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
package com.flaptor.hounder.indexer;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dom4j.Document;

import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.PortUtil;
import com.flaptor.util.remote.RmiServer;
import com.flaptor.util.remote.XmlrpcServer;

/**
 * @author Flaptor Development Team
 */
public class MultipleRpcIndexer implements IIndexer {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private final IIndexer baseIndexer;
    RmiServer rmiServer = null;
    XmlrpcServer xmlrpcServer = null;
    
    public MultipleRpcIndexer(IIndexer indexer, boolean rmi, boolean xmlrpc) {
        this.baseIndexer = indexer;
        if (rmi) {
        	int port = PortUtil.getPort("indexer.rmi");
            logger.info("MultipleRpcIndexer constructor: starting rmi indexer on port " + port);
            rmiServer = new RmiServer(port);
            rmiServer.addHandler(RmiServer.DEFAULT_SERVICE_NAME, (IRmiIndexer)baseIndexer);
            rmiServer.start();
        }
        if (xmlrpc) {
        	int port = PortUtil.getPort("indexer.xml");
            logger.info("MultipleRpcIndexer constructor: starting xmlRpc indexer on port " + port);
            xmlrpcServer = new XmlrpcServer(port);
            xmlrpcServer.addHandler(null, baseIndexer);
            xmlrpcServer.start();
        }
    }
    
    public int index(Document doc) {
        return baseIndexer.index(doc);
    }

    public int index(String text) {
        return baseIndexer.index(text);
    }

    public boolean isStopped() {
        return ( rmiStopped()
                && xmlrpcStopped()
                && baseIndexer.isStopped() );
    }

    private boolean rmiStopped() {
        if (null == rmiServer) {
            return true;
        } else {
            return rmiServer.isStopped();
        }
    }

    private boolean xmlrpcStopped() {
        if (null == xmlrpcServer) {
            return true;
        } else {
            return xmlrpcServer.isStopped();
        }
    }

    public void requestStop() {
        new StopperThread().start();
    }

    public static void main(String[] args) {
        String log4jConfigPath = FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            logger.warn("log4j.properties not found in classpath! Reload disabled.");
        }
        
        Config conf = Config.getConfig("indexer.properties");
        IIndexer indexer = conf.getBoolean("isMultiIndexer") ? new MultiIndexer() : new Indexer();
        new MultipleRpcIndexer(indexer,conf.getBoolean("rmiInterface"), conf.getBoolean("xmlInterface"));
    }

    private class StopperThread extends Thread {
        public StopperThread() {
            setName("MultipleRpcIndexer-StopperThread");
        }

        @Override
        public void run() {
            if (null != rmiServer) {
                logger.debug("stopping rmi server...");
                rmiServer.requestStop();
                while (!rmiServer.isStopped()) {
                    Execute.sleep(20);
                }
                logger.debug("rmi server stopped.");
            }
            if (null != xmlrpcServer) {
                logger.debug("stopping xmlrpc server...");
                xmlrpcServer.requestStop();
                while (! xmlrpcServer.isStopped()) {
                    Execute.sleep(20);
                }
                logger.debug("xmlrpc server stopped.");
            }
            logger.debug("stopping indexer...");
            baseIndexer.requestStop();
            while (!baseIndexer.isStopped()) {
                Execute.sleep(20);
            }
            logger.debug("indexer stopped. MultipleRpcIndexer stopped.");
        }
    }
}
