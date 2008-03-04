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

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.util.Execute;


/**
 * The page distributor maps pages to physical DPageDB nodes.
 * It uses the provided page mapper to map a page to a node number.
 * TODO: add state info for each dpagedb node
 * @author Flaptor Development Team
 */
public class PageDistributor {

    static Logger logger = Logger.getLogger(Execute.whoAmI());
    private APageMapper mapper;
    private IRemotePageCatcher[] catchers; 
    private IRemotePageCatcher localCatcher = null;


    /**
     * Initializes the class by taking the list of node ip addresses and constructing a list of remote page catchers.
     * @param nodeIPs array of IP addresses for the remote page catchers.
     * @param mapper a page mapper that selects which node handles a page.
     */
    public PageDistributor (ArrayList<NodeAddress> nodes, NodeAddress localNode, APageMapper mapper) {
        this.mapper = mapper;
        catchers = new IRemotePageCatcher[nodes.size()];

        // Get a remote page catcher for each provided node ip
        int nodeID = 0;
        for (NodeAddress node : nodes) {
            IRemotePageCatcher catcher = new PageCatcherStub(node.getPort(),node.getIP());
            if (null != catcher) {
                catchers[nodeID++] = catcher;
                if (node.equals(localNode)) {
                    localCatcher = catcher;
                }
            } else {
                throw new RuntimeException("Could not create PageCatcherStub for " + node);
            } 
           /* 
            boolean connected = false;
            while (!connected) {
                //IRemotePageCatcher catcher = (IRemotePageCatcher) RmiUtil.getRemoteService(node.getIP(), node.getPort(), RmiServer.DEFAULT_SERVICE_NAME);//TODO replace this line with a STUB
                if (null != catcher) {
                    catchers[nodeID++] = catcher;
                    if (node.equals(localNode)) {
                        localCatcher = catcher;
                    }
                    connected = true;
                } else {
                    Execute.sleep(1000);
                }
            }
            */
        }
        if (null == localCatcher) {
            throw new RuntimeException("Local address not found among the distributed PageDB nodes");
        }
    }

    // map pages to pagedb nodes
    public IRemotePageCatcher getCatcher (Page page) {
        int owner = mapper.mapPage(page);
        return catchers[owner];
    }

    public boolean catcherIsLocal (IRemotePageCatcher catcher) {
        return localCatcher.equals(catcher);
    }

}


