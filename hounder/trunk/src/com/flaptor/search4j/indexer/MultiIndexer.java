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
package com.flaptor.search4j.indexer;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;

import com.flaptor.search4j.indexer.util.Hash;
import com.flaptor.util.Config;
import com.flaptor.util.DocumentParser;
import com.flaptor.util.DomUtil;
import com.flaptor.util.Execute;
import com.flaptor.util.Pair;
import com.flaptor.util.PortUtil;
import com.flaptor.util.RunningState;


/**
 * This class implements the search4j multi indexer. It recieves documents
 * to index and relays them to one of a number of indexers according to a 
 * pluggable function of the received document, for example the hash of the
 * url. It is important that this hash function splits the incomming stream
 * of documents evenly so each indexer will end up with a similarly sized
 * index, otherwise the search result scores will not be compatible.
 * @author Flaptor Development Team
 */
public class MultiIndexer implements IRmiIndexer, IIndexer {
    
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private RunningState state = RunningState.RUNNING;
    private ArrayList<IRemoteIndexer> indexers = new ArrayList<IRemoteIndexer>();
    private DocumentParser parser = new DocumentParser();
    private Hash hashFunction = null;
    private static final int RETRY_LIMIT = 30;

    
    /**
     * Constructor.
     */
    public MultiIndexer() {

        // get the indexer stubs.
        Config config = Config.getConfig("multiIndexer.properties");
        String[] hosts = config.getStringArray("indexer.hosts");
        for (int i = 0; i < hosts.length; i++) {
        	Pair<String, Integer> host = PortUtil.parseHost(hosts[i]);
            indexers.add(new RmiIndexerStub(host.last(), host.first()));
        }

        hashFunction = new Hash(indexers.size());

    }


    /**
     * Adds a document to the indexing queue.
     * @param doc the request Document
     * @return SUCCESS, RETRY_QUEUE_FULL.
     * @throws IllegalStateException if the state of the indexer is not running.
     * @see com.flaptor.util.remote.XmlrpcServer
     */
    public int index(final Document doc) {
        if (state != RunningState.RUNNING) {
            throw new IllegalStateException("index: Trying to index a document but the MultiIndexer is no longer running.");
        }


        // check commands
        Node command = doc.selectSingleNode("/command");
        if (null != command) {
            return processCommand(doc);
        }

        // else, it may be a document to index / delete
        // get the documentId.
        Node node = doc.selectSingleNode("//documentId");
        if (null == node) {
            logger.error("Document missing documentId. Will not index it.");
            logger.error("Document was : " + DomUtil.domToString(doc));
            return Indexer.FAILURE;
        }
        
        // get the target indexer.
        int target = hashFunction.hash(node.getText());
        // send the document to the target indexer.
        try {
            IRemoteIndexer indexer = indexers.get(target);
            logger.debug("Sending " + node.getText() + " to indexer " + target);
            return indexer.index(doc);
        } catch (com.flaptor.util.remote.ConnectionException e) {
            logger.error("Connection failed: ",e);
            return Indexer.FAILURE;
        }
    }


    /**
     * Adds a document to the indexing queue.
     * @param text the request in xml formatted text
     * @return SUCCESS, RETRY_QUEUE_FULL or PARSE_ERROR
     * @throws IllegalStateException if the indexer is not in the "running" state.
     * @see com.flaptor.util.remote.XmlrpcServer
     */
    public int index(final String text) {
        Document doc = parser.genDocument(text);
        if (null == doc) {
            return (Indexer.PARSE_ERROR);
        } else {
            return index(doc);
        }
    }



    private int processCommand(final Document doc) {
        Node commandNode = doc.selectSingleNode("/command");
        Node attNode = commandNode.selectSingleNode("@node");
        
        if (null == attNode) { // so it is for all nodes

            for (IRemoteIndexer indexer: indexers) {
                boolean accepted = false;
                int retries = 0;

                // try until accepted, or too much retries.
                while (!accepted && retries < RETRY_LIMIT ) {
                    try {
                        int retValue = indexer.index(doc);
                        if (Indexer.SUCCESS == retValue) {
                            accepted = true;
                        } else {
                            retries++;
                            Execute.sleep(10*1000); // wait 10 seconds before retry
                        }
                    } catch (com.flaptor.util.remote.ConnectionException e ) {
                        logger.error("processCommand: Connection failed: " + e.getMessage(),e);
                    }
                }
                // if could not send to indexer
                if (!accepted) {
                    logger.error("processCommand: tried " + RETRY_LIMIT + " times to index command, but failed on node " + indexer.toString() + ". Will not continue with other indexers.");
                    return Indexer.FAILURE;

                }
            }
            // in case no one returned Indexer.FAILURE 
            return Indexer.SUCCESS;
        } else { // specific node

            try {
                int indexerNumber = Integer.parseInt(attNode.getText());
                if (indexerNumber < indexers.size() && indexerNumber >=0) {
                    IRemoteIndexer indexer = indexers.get(indexerNumber);
                    try {
                        return indexer.index(doc);
                    } catch (com.flaptor.util.remote.ConnectionException e) {
                        logger.error("processCommand: while sending command to single node " + indexer.toString() + " " + e.getMessage(),e);
                        return Indexer.FAILURE;
                    }
                } else {
                    logger.error("processCommand: received command for node number out of indexers range. Received " + indexerNumber + " and have " + indexers.size() + " indexers. Ignoring command.");
                    return Indexer.FAILURE;
                }
            } catch (NumberFormatException e) {
                logger.error("processCommand: can not parse node number: " + e,e);
                return Indexer.PARSE_ERROR;
            }
        }
    }


    /**
     * @inheritDoc
     */
    public boolean isStopped() {
        return (state == RunningState.STOPPED);
    }


    /**
     * @inheritDoc
     * After this call, all further attempts to use the index methods (@link index , @link indexDom )
     * will throw an IllegalStateException.
     */
    public void requestStop() {
        if (state == RunningState.RUNNING) {
            state = RunningState.STOPPED;
        } else {
            logger.warn("requestStop: stop requested while not running. Ignoring.");
        }
    }


}
