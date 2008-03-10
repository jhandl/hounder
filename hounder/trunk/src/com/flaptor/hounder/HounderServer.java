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

package com.flaptor.hounder;

import org.apache.log4j.Logger;

import com.flaptor.hounder.indexer.Indexer;
import com.flaptor.hounder.searcher.CompositeSearcher;
import com.flaptor.hounder.searcher.Searcher;
import com.flaptor.util.Execute;
import com.flaptor.util.remote.XmlrpcServer;

/**
 * @author Flaptor Development Team
 * @deprecated start searcher, indexer and crawler separately
 */
public class HounderServer {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    private static final int DEFAULT_INDEXER_PORT = 9521;
    private static final int DEFAULT_SEARCHER_PORT = 9522;

    public static void main(String[] args) {

        String log4jConfigPath = com.flaptor.util.FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
		    org.apache.log4j.PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            logger.warn("log4j.properties not found on classpath!");
        }

        Indexer indexer = null;
        CompositeSearcher searcher = null;

        try {
            int indexerPort = DEFAULT_INDEXER_PORT;
            int searcherPort = DEFAULT_SEARCHER_PORT;
            if (args.length == 2) {
                try {
                    indexerPort = Integer.valueOf(args[0]).intValue();
                    searcherPort = Integer.valueOf(args[1]).intValue();
                } catch (NumberFormatException e) {
                    logger.error("Invalid parameters " +e);
                    printUsage();
                    return;
                }
            } else if (args.length != 0) {
                printUsage();
                return;
            }

            indexer = new Indexer();
            XmlrpcServer indexerServer = new XmlrpcServer(indexerPort);
            indexerServer.addHandler("indexer", indexer);
            
            searcher = new CompositeSearcher();
            XmlrpcServer searcherServer = new XmlrpcServer(searcherPort);
            searcherServer.addHandler("searcher", searcher);

            indexerServer.start();
            logger.info("Indexer started on port " +Integer.toString(indexerPort));
            searcherServer.start();
            logger.info("Searcher started on port " +Integer.toString(indexerPort));

        } catch (Exception e) {
            if (indexer != null) {
                indexer.requestStop();
            }
            if (searcher != null) {
                ((Searcher)searcher.getBaseSearcher()).close();
            }
            while (!indexer.isStopped()) {
                Execute.sleep(10);
            }
        }
    }

    private static void printUsage() {
        logger.error("Usage: HounderServer [ <indexer port> <searcher port> ]");
    }

}

