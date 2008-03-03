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
package com.flaptor.search4j.searcher;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.flaptor.search4j.cluster.MultiSearcher;
import com.flaptor.search4j.searcher.filter.AFilter;
import com.flaptor.search4j.searcher.group.AGroup;
import com.flaptor.search4j.searcher.query.AQuery;
import com.flaptor.search4j.searcher.sort.ASort;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.PortUtil;
import com.flaptor.util.remote.RmiServer;
import com.flaptor.util.remote.WebServer;
import com.flaptor.util.remote.XmlrpcServer;

/**
 * Searcher exposed by several RPC interfaces 
 * @author Flaptor Development Team
 */
public class MultipleRpcSearcher implements ISearcher {
	public static final String XMLRPC_CONTEXT = null;
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private final ISearcher baseSearcher;
    private RmiSearcherWrapper rmiSearcherWrapper;
    
    public MultipleRpcSearcher(ISearcher baseSearcher, boolean rmi, boolean xmlrpc, boolean openSearch, boolean web) {
        this.baseSearcher = baseSearcher;
        if (rmi) {
            int port = PortUtil.getPort("searcher.rmi");
            logger.info("MultipleRpcSearcher constructor: starting rmi searcher on port " + port);
            rmiSearcherWrapper = new RmiSearcherWrapper(baseSearcher);
            RmiServer server = new RmiServer(port);
            server.addHandler(RmiServer.DEFAULT_SERVICE_NAME, rmiSearcherWrapper);
            server.start();
        }
        if (xmlrpc) {
            int port = PortUtil.getPort("searcher.xml");
            logger.info("MultipleRpcSearcher constructor: starting xmlRpc searcher on port " + port);
            XmlrpcServer server = new XmlrpcServer(port);
            server.addHandler(XMLRPC_CONTEXT, new XmlSearcher(baseSearcher));
            server.start();
        }
        if (openSearch || web) {
            int webServerPort = PortUtil.getPort("searcher.webOpenSearch");
            WebServer server = new WebServer(webServerPort); 

            if (openSearch) {
                logger.info("MultipleRpcSearcher constructor: starting OpenSearch searcher on port " + webServerPort + " context /opensearch/");
                server.addHandler("/opensearch", new OpenSearchHandler(baseSearcher));
            }
            if (web) {
                logger.info("MultipleRpcSearcher constructor: starting web searcher on port " + webServerPort  + " context /websearch/");
                WebSearchUtil.setSearcher(baseSearcher);
                String webappPath = this.getClass().getClassLoader().getResource("web-searcher").getPath();
                server.addWebAppHandler("/websearch", webappPath);
            }
   			try {server.start(); } catch (Exception e) {throw new RuntimeException(e);}
        }
    }
    
    public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup group, int groupSize, AFilter filter, ASort sort)  throws SearcherException{
        return baseSearcher.search(query, firstResult, count, group, groupSize, filter, sort);
    }

    public static void main(String[] args) {

        String log4jConfigPath = FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            logger.warn("log4j.properties not found in classpath! Reload disabled.");
        }

        Config conf = Config.getConfig("searcher.properties");
        ISearcher baseSearcher = conf.getBoolean("searcher.isMultiSearcher") ? new MultiSearcher() : new CompositeSearcher();
        new MultipleRpcSearcher(baseSearcher,conf.getBoolean("rmiInterface"), conf.getBoolean("xmlInterface"), conf.getBoolean("openSearchInterface"), conf.getBoolean("webInterface"));
    }
}
