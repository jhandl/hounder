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

import java.util.Vector;

import org.apache.log4j.Logger;
import org.dom4j.Document;

import com.flaptor.util.AStoppableThread;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.Queue;

/**
 * Implements a process pipe for modules. Each module receives a document and
 * can return the same document or a changed one, or many documents, or none at
 * all. Each document produced by a module is stored in a queue which is used to
 * feed the next module down the pipe. An external process must add the modules
 * to the pipe, start the pipe thread and feed it new documents. The last module
 * should return no documents.
 * @author Flaptor Development Team
 */
public class ModulePipe extends AStoppableThread {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private Vector<AModule> modules = new Vector<AModule>();
    private Vector<Queue<Document>> queues = new Vector<Queue<Document>>();
    private int maxQueueSize;
    private final long SLEEP_TIME_WHEN_NOTHING_TO_DO = 50;
    /**
     * Once the pipe is set to run, adding modules is no longer allowed.
     * This variable keeps track of that for that reason.
     */
    private boolean alreadyProcessing = false;


    /**
     * Default constructor.
     */
    public ModulePipe() {
        thrd.setName(ModulePipe.class.toString());
        maxQueueSize = Config.getConfig("indexer.properties").getInt("ModulePipe.inter_module_queue_size");
        logger.info("Using an inter Modules queue of maximum size :" + maxQueueSize);
    }

    /**
     * Add a module to the end pipe.
     * @param mod the module to add. Must not be null.
     */
    public void addModule(final AModule mod) {
        if (alreadyProcessing) {
            String s = "addModule: trying to add a module while already processing.";
            logger.error(s);
            throw new IllegalArgumentException(s);            
        }
        if (null != mod) {
            modules.add(mod);
            queues.add(new Queue<Document>(maxQueueSize));
        } else {
            String s = "addModule: module can't be null";
            logger.error(s);
            throw new IllegalArgumentException(s);
        }
    }

    /**
     * Feed the pipe a new document to be processed by the modules.
     * Blocking call: waits until there's space in the firsn queue.
     * @param doc the dom4j Document to enqueue.
     * @throws IllegalStateException if there's no module en the pipe or if the thread
     *      is not running.
     */
    public void push(final Document doc) {
        if (signaledToStop) {
            String s = "write: trying to write a document while not stopping or stopped.";
            logger.error(s);
            throw new IllegalStateException(s);
        }
        
        if (queues.size() > 0) {
            Queue<Document> q = queues.get(0);
            q.enqueueBlock(doc); // We block until there is space in the queue.
        } else {
            String s = "write: no modules in the pipe";
            logger.error(s);
            throw new IllegalStateException(s);
        }
    }

    /**
     * Run the pipe.
     */
    /*
	 Warning, if a module produces more documents in a single process that the size of the intermediate queues, the extras will be dropped
	 logging an error
	*/
    public void run() {
		try {
			while (true) {
				boolean somethingFound = false;
				int i = queues.size() - 1;
				while (i >= 0) {
					Queue<Document> currQueue = queues.get(i);
					// We will process just the first document found. Read below.
					if (currQueue.size() > 0) {
						somethingFound = true;
						AModule currModule = modules.get(i);
						logger.debug("run: doc found in queue " + i + ", calling module " + currModule);
						Document inputDoc = (Document) currQueue.dequeueNoBlock();// I'm the only comsumer to this queue, so I should never receive a null.
						Document[] outputDocs = currModule.process(inputDoc);
						Queue<Document> nextQueue = (i + 1 < queues.size()) ? queues.get(i + 1) : null;
						if (null != outputDocs) {
							logger.debug("run: module " + currModule + " returned " + outputDocs.length + " docs");
							if (null != nextQueue) {
								for (int j = 0; j < outputDocs.length; j++) {
									if (!nextQueue.enqueueNoBlock(outputDocs[j])) {
										logger.error("Intermediate queue " + i + " full. Droping document.");
									}
								}
							} else {
								logger.error("run: the last module in the pipe is returning documents although it should consume them.");
							}
						}

						// This is the odd part, if we processed something, we re-start looking from the next queue (if there is one).
						// This keeps all the intermediate queues as empty as posibble, avoiding drops.
						if (i < (queues.size() - 1)) {
							i++;
						}//if we're already at the last queue, we reprocess it again (i = i)
					} else { // if nothing was found we proceed to the previous queue
						i--;
					}
				} // while
				if (!somethingFound) {
					if (!signaledToStop) {
						sleep(SLEEP_TIME_WHEN_NOTHING_TO_DO);
					} else {
						stopAllModules();
						stopped = true;
						return;
					}
				}

			}
		} catch (Exception e) { //This should never, ever happen.
			logger.fatal("run: exception caught in run loop.", e);
			System.exit(-1);
		}
    }
    
    /**
     * For all modules in the pipe requests a stop and waits until it stops.
     * 
     */
    private void stopAllModules() {
        logger.debug("Stopping the pipeline.");
        AModule module;
        for (int i = 0; i < modules.size(); i++) {
            logger.info("Requesting a stop to module " + i);
            modules.get(i).requestStop();
        }
        for (int i=0; i < modules.size(); i++) {
            module = modules.get(i);
            while (!module.isStopped()) {
                try {
                    sleep(1000); 
                } catch (Exception e) { /* Ignore */ }
                if (!module.isStopped()) {
                    logger.warn("Waiting for module " + i + " to stop...");
                }              
            }
            logger.info("Module " + i + " stopped");
        }
        logger.debug("Pipeline stopped");
    }

}
