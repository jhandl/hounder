/*
Copyright 2008 Flaptor (flaptor.com) 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License.
*/
package com.flaptor.hounder.crawler.pagedb.distributed;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.hounder.crawler.pagedb.PageDB;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.NetUtil;
import com.flaptor.util.remote.ConnectionException;


/**
 * The DPageDB implements one node in a cluster of PageDBs. 
 * It behaves as a normal PageDB to the outside.
 * @author Flaptor Development Team
 */
public class DPageDB extends PageDB {

    static Logger logger = Logger.getLogger(Execute.whoAmI());
    private PageDistributor distributor = null;
    private PageCatcher pageCatcher = null;
    private boolean catcherIsLocal = false;


    /**
     * Create a DPageDB.
     */
    public DPageDB (String dirname) {
        this(dirname, new PageCatcher(dirname));
        catcherIsLocal = true;
    }

    /**
     * Create a DPageDB with a provided page catcher.
     * @param dirname the path to the pagedb directory.
     * @param catcher the provided PageCatcher.
     */
    public DPageDB (String dirname, PageCatcher catcher) {
        super(dirname);
        Config config = Config.getConfig("crawler.properties");
        ArrayList<NodeAddress> nodes = getNodeList(config);
        NodeAddress localNode = getLocalNode(nodes);
        APageMapper mapper = getPageMapper(config, nodes.size());
        pageCatcher = catcher;
        pageCatcher.start(localNode);
        distributor = new PageDistributor(nodes, localNode, mapper);
    }

    // Parse the configured list of nodes and return a map of IPs and port numbers.
    private ArrayList<NodeAddress> getNodeList (Config config) {
        ArrayList<NodeAddress> nodeList = new ArrayList<NodeAddress>();
        String[] nodeSpecs = config.getStringArray("pagedb.node.list");
        if (null == nodeSpecs || 0 == nodeSpecs.length) {
            throw new RuntimeException("No list of nodes defined for the distributed PageDB");
        } 
        for (String spec : nodeSpecs) {
            String[] parts = spec.split(":");
            String ip = parts[0].trim();
            Integer port = null;
            if (parts.length > 1) {
                port = Integer.parseInt(parts[1].trim());
            }
            nodeList.add(new NodeAddress(ip, port));
        }
        return nodeList;
    }


    // Return the configured page mapper.
    private APageMapper getPageMapper (Config config, int numberOfNodes) {
        String[] parts = config.getStringArray("pagedb.node.mapper");
        if (null == parts || 0 == parts.length) {
            throw new RuntimeException("No mapper defined for the distributed PageDB");
        } 
        String mapperClass = parts[0].trim();
        Config mapperConfig = config;
        if (parts.length > 1) {
            String mapperName = parts[1].trim();
            mapperConfig = Config.getConfig(mapperName + "Mapper.properties");
        }
        APageMapper mapper;
        try {
            mapper = (APageMapper)Class.forName(mapperClass).getConstructor(new Class[]{Config.class, Integer.TYPE}).newInstance(new Object[]{mapperConfig, numberOfNodes});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return mapper;
    }

    // Return the IP and port of the local node, matching each node with the local network interfaces.
    public NodeAddress getLocalNode (ArrayList<NodeAddress> nodes) {
        NodeAddress localNode = null;
        boolean found = false;
        try {
            for (String localIP : NetUtil.getLocalIPs()) {
                for (NodeAddress node : nodes) {
                    String remoteIP = node.getIP();
                    Integer remotePort = node.getPort();
                    if (!found && localIP.equals(remoteIP)) {
                        localNode = node;
                        found = true;
                    }
                }
            }
        } catch (java.net.SocketException e) {
            throw new RuntimeException("Looking for the local IP among the list of provided distributed PageDB addresses", e);
        }
        if (!found) {
            throw new RuntimeException("The local host is not among the list of nodes for the distributed PageDB");
        }
        return localNode;
    }

    /**
     * Open the pagedb.
     * @param action @see com.flaptor.hounder.crawler.pagedb.PageDB#open(int)
     */
    public void open (int action) throws IOException {
        super.open(action);
    }


    /**
     * Write a page to the pagedb.
     * @param page Page that should be written to the pagedb.
     */
    public synchronized void addPage (Page page) throws IOException {
        boolean sent = false;
        page.setLocal(true);
        try {
            IRemotePageCatcher catcher = distributor.getCatcher(page);
            if (!distributor.catcherIsLocal(catcher)) {
                logger.debug("DPAGEDB Sending page "+page.getUrl()+" to remote catcher");
                catcher.addPage(page);
                sent = true;
            }
        } catch (ConnectionException e) {
            logger.error("Attempting to send a page to a remote PageCatcher",e);
            page.setLocal(false);
        }
        if (!sent) {
            super.addPage(page);
        }
    }

    /**
     * Close the distributed pagedb.
     */
    public void close () throws IOException {
        if ((mode & 0x0F) != READ) {
            logger.info("DPageDB ready for close. Will merge "+pageCatcher.catches()+" catched pages.");
            if (pageCatcher.catches() > 0) {
                PageDB db = pageCatcher.getCatch();
                db.open(READ);
                for (Page page : db) {
                    page.setLocal(true);
                    super.addPage (page);
                }
                Execute.close(db);
                db.deleteDir();
            }
            if (catcherIsLocal) {
                pageCatcher.stop();
            }
        }
        logger.info("DPageDB closing local PageDB.");
        super.close();
        logger.info("DPageDB closed.");
    }


}


