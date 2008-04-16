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

import java.lang.reflect.Constructor;

import org.apache.log4j.Logger;
import org.dom4j.Document;

import com.flaptor.clusterfest.NodeListener;
import com.flaptor.clusterfest.action.ActionModule;
import com.flaptor.clusterfest.controlling.ControllerModule;
import com.flaptor.clusterfest.controlling.node.ControllableImplementation;
import com.flaptor.clusterfest.monitoring.MonitorModule;
import com.flaptor.hounder.indexer.clustering.IndexerActionReceiver;
import com.flaptor.util.AStoppableThread;
import com.flaptor.util.Config;
import com.flaptor.util.DocumentParser;
import com.flaptor.util.Execute;
import com.flaptor.util.PortUtil;
import com.flaptor.util.Queue;
import com.flaptor.util.RunningState;
import com.flaptor.util.Statistics;
import com.flaptor.util.Stoppable;


/**
 * This class implements the Hounder indexer. It parses the supplied String
 * into a DOM Document and sequentially processes it through a list of modules
 * supplied in the configuration file. Requests are received asynchronously and
 * sent to a queue for sequential indexing. If journaling is turned on, a 
 * &lt;transactionId&gt;(long)&lt;/transactionId&gt; element is added as a child of the
 * root element.
 * @author Flaptor Development Team
 */
public class Indexer implements IRmiIndexer, IIndexer, Stoppable {
    
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    
    //clusterfest
    private NodeListener clusteringListener;
    private IndexerMonitoredNode indexerMonitoredNode; 
    
    /**
     * Possible return values for the index method.
     */
    public static final int SUCCESS = 0;
    public static final int PARSE_ERROR = 1;
    public static final int RETRY_QUEUE_FULL = 2;
    public static final int FAILURE = 3; // INTERNAL ERROR

    // The input queue holds documents until they can be processes by the pipe.
    private static int maxQueueSize;
    private Queue<Document> queue;
    private QueueProcessorThread qpt;
    private DocumentParser parser = new DocumentParser();
    private ModulePipe pipe;
	private final IndexManager indexManager;
    
    //State related te the shutdown sequence.
    private RunningState state = RunningState.RUNNING;

    // Statistics instance, to report events, and constants
    private final Statistics statistics = Statistics.getStatistics();
    public static final String DOCUMENT_ENQUEUED = "Indexer.DocumentEnqueued";
    
    
    /**
     * Constructor.
     */
    public Indexer() {
    	
        Config config = Config.getConfig("indexer.properties");
        if (config.getBoolean("clustering.enable")) {
        	int port = PortUtil.getPort("clustering.rpc.indexer");
    		clusteringListener = new NodeListener(port, config);
    		indexerMonitoredNode = IndexerMonitoredNode.getInstance();
    		MonitorModule.addMonitorListener(clusteringListener, IndexerMonitoredNode.getInstance());
    		ControllerModule.addControllerListener(clusteringListener, new ControllableImplementation());
    		ActionModule.setActionReceiver(clusteringListener, new IndexerActionReceiver(this));
        }


		indexManager = new IndexManager(this);
        maxQueueSize = config.getInt("Indexer.maxQueueSize");
        logger.info("Maximum queue size set to " + maxQueueSize);
        // create all the modules requested in the configuration file AND
        // the special Writer
        // module at the end of the pipeline
        pipe = new ModulePipe();
        String modulesStr = config.getString("Indexer.modules");
        if (!modulesStr.equals("")) {
            String[] moduleStr = modulesStr.split(",");
            for (int i = 0; i < moduleStr.length; i++) {
                logger.info("creating " + moduleStr[i]);
                try {
					Class<?> theModule = Class.forName(moduleStr[i].trim());
					if (AInternalModule.class.isAssignableFrom(theModule)) {
						Constructor cons = theModule.getConstructor(new Class[]{Indexer.class});
						AInternalModule mod = (AInternalModule) cons.newInstance(this);
						pipe.addModule(mod);
					} else if (AModule.class.isAssignableFrom(theModule)) {
						pipe.addModule((AModule) theModule.newInstance());
					} else {
						String s = "constructor: A module specified is not an AModule nor an AInternalModule.";
						logger.error(s);
						throw new IllegalArgumentException(s);
					}
                } catch (Exception e) {//Handles 3 different exceptions
                    logger.fatal("Indexer constructor: " + e,e);
                    System.exit(-1);
                }
            }
        } else {
            logger.info("Module list is empty!");
        }
        logger.info("Modules ready");
        // Modules created
        

        queue = new Queue<Document>(maxQueueSize);
        qpt = new QueueProcessorThread();
        pipe.start();
        qpt.start();
        logger.info("Indexer ready");
    }

	/**
	 * Package protected method that returns the IndexManager.
	 * @return the IndexManager used by this index.
    */
	IndexManager getIndexManager() {
		return indexManager;
	}
    /**
     * Adds a document to the indexing queue.
     * @param doc the request Document
     * @return SUCCESS or RETRY_QUEUE_FULL
     * @throws IllegalStateException if the state of the indexer is not running.
     * @see com.flaptor.util.remote.XmlrpcServer
     */
    public int index(final Document doc) {
    // Changed from indexDom because there is no need to use the param type as part of the method name.
        logger.debug("indexDom: received data to index");
        if (state != RunningState.RUNNING) {
            String s = "indexDom: Trying to index a document but the Indexer is no longer running.";
            logger.error(s);
            throw new IllegalStateException(s);
        }
        if (queue.enqueueNoBlock(doc)) {
            statistics.notifyEventValue(DOCUMENT_ENQUEUED,1f);
            return SUCCESS;
        } else {
            statistics.notifyEventError(DOCUMENT_ENQUEUED);
            return RETRY_QUEUE_FULL;
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
        // this avoids parsing the document if the queue is already full
        // so it reduces the impact of a denial-of-service attack
        // (not necessarily malicious, it could be due to a bug in the
        // client)
        if (state != RunningState.RUNNING) {
            String s = "indexDom: Trying to index a document but the Indexer is no longer running.";
            logger.error(s);
            throw new IllegalStateException(s);
        }
        if (queue.isFull()) {
            return RETRY_QUEUE_FULL;
        } else {
            Document doc = parser.genDocument(text);
            if (null == doc) {
                return (PARSE_ERROR);                
            } else {
                return index(doc);
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
            state = RunningState.STOPPING;
            new StopperThread().start();
        } else {
            logger.warn("requestStop: stop requested while not running. Ignoring.");
        }
    }

    //----------------------------------------------------------------------------------------------------
    //Internal classes
    //----------------------------------------------------------------------------------------------------
    /**
     * This class continuously takes an element from the input queue and calls
     * process for it.
     * @see ModulePipe#push(Document doc)
     * @author spike
     */
    private class QueueProcessorThread extends AStoppableThread {
        private final long MAX_TIME_ASLEEP = 1000;
        
        /**
         * Default constructor.
         */
        public QueueProcessorThread() {
            thrd.setName("Indexer:QueueProcessorThread");
        }
        
        /**
         * Continuously checks for data on the queue and calls process with it.
         * Uses a blocking call with timeout, to make sure it can exit when
         * signaled to.
         * @see AStoppableThread#run()
         */
        public void run() {
            while (true) {
                Document doc = null;
                doc = (Document) queue.dequeueBlock(MAX_TIME_ASLEEP);
                //If I've been asked to stop and the queue is empty I can stop.
                if (signaledToStop && null == doc) {
                    break;
                }
                if (doc != null) {
                    logger.debug("Processing a request from the queue...");
                    pipe.push(doc);
                }
            }
            stopped = true;
        }
    }
    
    /**
     * This class handles the sequence of events that stops the Indexer.
     */
    private class StopperThread extends Thread {
        private final Logger logger = Logger.getLogger(StopperThread.class);
        
        public StopperThread() {
            super();
            setName("Indexer.StopperThread");
        }
        
        /**
         * Executes the Indexer shutdown sequence asynchronously
         * After that, sets the Indexer state to Stopped.
         */
        public void run() {
            logger.info("Beginning stop sequence.");
            assert(state == RunningState.STOPPING);
            logger.debug("Stopping QueueProcessorThread.");
            qpt.requestStop();
            while (!qpt.isStopped()) {
                Execute.sleep(20,logger);
            }
            logger.debug("QueueProccessorThread stopped.");
            logger.debug("Stopping the pipeline.");
            pipe.requestStop();
            while (!pipe.isStopped()) {
                Execute.sleep(20,logger);
            }
            logger.debug("Pipeline stopped.");
            logger.debug("Stopping the IndexManager.");
			indexManager.requestStop();
            while (!indexManager.isStopped()) {
                Execute.sleep(20,logger);
            }            
           
            // Stop clustering if it was enabled
            if (null != clusteringListener) {
                logger.debug("Stopping clustering listener");
                clusteringListener.requestStop();
                while (!clusteringListener.isStopped()) {
                    Execute.sleep(20,logger);
                }
                logger.debug("clustering listener stopped.");
            }            

            logger.debug("IndexManager stopped.");
            logger.info("Stop sequence finished.");
            state = RunningState.STOPPED;
        }
    }

    public NodeListener getClusteringListener() {
        return clusteringListener;
    }

    public IndexerMonitoredNode getIndexerMonitoredNode() {
        return indexerMonitoredNode;
    }
}
