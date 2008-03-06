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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;

import com.flaptor.hounder.Index;
import com.flaptor.hounder.IndexDescriptor;
import com.flaptor.hounder.indexer.util.Hash;
import com.flaptor.util.AStoppableThread;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.RunningState;
import com.flaptor.util.Stoppable;

/**
 * This class manages a lucene directory.
 * It allows to simple use theIndexWriteProvider functionality,
 * without knowing the detail of the index. It transparently
 * manages the closing and opening of the index.<p>
 * Configuration strings:
 *      docIdName: the name of lucene's field storing the documentId.
 *          @see Writer
 *      workingDirPath: no default. The path of the directory to use to store
 *          the working index and the copies.
 *      updateInterval: no default. The number of milliseconds to wait between
 *          index checkpoints. If it is 0, checkpoints are disabled.
 * @author Flaptor Development Team
 */
public final class IndexManager implements IndexWriteProvider, Stoppable {
	private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    private final Config config;
    //This contains the information about the index over wich we are writing.
    private final IndexDescriptor indexDescriptor;
    //This function determines if the documents should be indexed in this
    //index segment. Useful for cluster installations only.
    private final Hash hashFunction;


	//Lucene related variables.
    private Index workIndex;
    private IndexWriter writer;
    private UpdaterThread ut;
	private String docIdName = null;
	private boolean optimizeScheduled = false;
	
	private Hashtable<String, Long> lastOperation;
    private long nextAddId;

	//State related te the shutdown sequence.
	private volatile RunningState state = RunningState.RUNNING;

	//FileSystem related variables
    private IndexLibrary library;
	
    private final File workingDirectory;
	private final String workingDirPath;
	private final File indexDirectory;


    private final String LAST_CHECKPOINT = "lastCheckpoint";
    private final String LAST_OPTIMIZE = "lastOptimization";

	/**
	 * Default constructor. 
     * Takes the required information from the configuration file. 
     *
     * Configuration strings: 
     *      docIdName: 
     *          the string to use as identifier of the document id while 
     *          storing it in the index. 
     *      updateInterval: 
     *          no default. The number of milliseconds to wait between
     *          index checkpoints. If it is 0, checkpoints are disabled.
     *
	 * @throws IllegalArgumentException 
     *          when there are problems with the arguments taken from the
	 * 		    configuration file.
	 * @throws IllegalStateException 
     *          when the filesystem is not ready to be set up (permissions,
	 * 		    space?)
	 */
	public IndexManager() {
        config = Config.getConfig("indexer.properties");
        workingDirPath = Config.getConfig("common.properties").getString("baseDir")  
                         + File.separator 
                         + config.getString("indexer.dir") 
                         + File.separator 
                         + "indexes";
      
		lastOperation = new Hashtable<String, Long>();
        workingDirectory = new File(workingDirPath);
		indexDirectory = new File(workingDirectory, "index");
        indexDescriptor = new IndexDescriptor(config.getString("IndexManager.indexDescriptor"));
        library = new IndexLibrary();


		setBaseDirectory();
		setIndexDirectory();
		setLatestCopyAsIndex();
        hashFunction = new Hash(indexDescriptor.getTotalNumberOfNodes());
		docIdName = config.getString("docIdName");

		ut = new UpdaterThread();
		logger.debug("Starting UpdaterThread.");
		ut.start();
	}

	/**
	 * Sets the latest copy as the working index.
	 * Constructor helper method.
	 * If there are copies in the library, it copies the latest copy as the 
     * working index. Otherwise it starts a new index.
	 */
	private void setLatestCopyAsIndex() {
        String latestCopy = library.getLatestDirectoryCopy();
        if (null == latestCopy) {
			//FIXME: we have to check wether the index directory contains a valid index
			// and in that case, the app should terminate, instead of erasing the existent
			//(but probably invalid) index.
            logger.info("constructor: no copy found. Creating new index. Index will serve " + indexDescriptor.toString());
            workIndex = Index.createIndex(indexDirectory);
            workIndex.setIndexDescriptor(indexDescriptor);
            nextAddId = 1;
            openWriter();
        } else {
            logger.info("constructor: Using latest copy as index.");
            String indexPath = indexDirectory.getAbsolutePath();
            com.flaptor.util.FileUtil.deleteDir(indexPath);
            Index latestCopyIndex = new Index(new File(latestCopy));
            workIndex = latestCopyIndex.copyIndex(indexDirectory);
            IndexDescriptor foundIndexDescriptor = workIndex.getIndexDescriptor();
            if (!indexDescriptor.equals(foundIndexDescriptor)) {
                String s = "This indexer is supposed to serve: " + indexDescriptor.toString() + ", but the index" +
                    " found is: " + foundIndexDescriptor.toString();
                logger.fatal(s);
                throw new IllegalStateException(s);
            }
            nextAddId = findLargestAddId() + 1;
            openWriter();
        }
	}

	/**
	 * Sets up the index directory.
	 * Constructor helper method.
	 * Verifies the existence or creates the index directory inside the working
     * directory.
     *
	 * @throws IllegalArgumentException 
     *      if the index directory is not a directory, but a regular file.
	 * @throws IllegalStateException 
     *      if there is a permissions problem with the directory.
	 */
	private void setIndexDirectory() {
        if (indexDirectory.exists()) {
            if (!indexDirectory.isDirectory()) {
                String s = "Index present, but not a directory.";
                logger.error(s);
                throw new IllegalArgumentException(s);
            }
        }
	}

	/**
	 * Sets up the working directory.
	 * Constructor helper method.
	 * Helper method for the constructor. Reads from the configuration the 
     * workingDirPath, checks for it's existence, creates it if necessary, 
     * and returns.
     *
	 * @throws IllegalArgumentException 
     *      if the workingDirPath in the configuration points to an existent, 
     *      non directory file.
	 * @throws IllegalStateException 
     *      if there're problems with permissions with the working directory, 
     *      or if that exception floats from the File opperation performed.
	 */
	private void setBaseDirectory() {
        if (workingDirectory.exists()) {
            if (!workingDirectory.isDirectory()) {
                String s = "setBaseDirectory: the path " + workingDirPath + " point to a file.";
                logger.error(s);
                throw new IllegalArgumentException(s);
            }
            logger.info("Using existing working directory.");
        } else {
            logger.info("Working directory does not exist. Creating it.");
            workingDirectory.mkdir();
        }
        if (!workingDirectory.canRead()) {
            String s = "Don't have read permission over the working directory(" + workingDirectory.getAbsolutePath() + ").";
            logger.error(s);
            throw new IllegalStateException(s);
        }
        if (!workingDirectory.canWrite()) {
            String s = "Don't have write permission over the working directory(" + workingDirectory.getAbsolutePath() + ").";
            logger.error(s);
            throw new IllegalStateException(s);
        }
	}

	/**
	 * Searchs all the index to find the largest AddId.
	 * @return the largest AddId found in the index.
	 */
	private long findLargestAddId() {
		long max = 1;
		IndexReader reader = null;
		try {
			reader = workIndex.getReader();
			int num = reader.maxDoc();
			for (int i = 0; i < num; i++) {
				if (!reader.isDeleted(i)) {
					String val = reader.document(i).get("AddId");
					if (null != val) {
						long n = new Long(val).longValue();
						if (max < n) {
							max = n;
						}
					}
				}
			}
		} catch (IOException e) {
			logger.fatal("Could not read from the index to get the last AddId." + e);
			throw new RuntimeException("Error reading the index when looking for initial AddId.", e);
		} finally {
			Execute.close(reader, logger);
		}
		logger.debug("Largest AddId found: " + max);
		return max;
	}

	/**
	 * Returns a monotonically increasing Long. The initial value is 1 if no
	 * initial index is provided, or the last stored DocumentId +1 if an index
	 * is supplied.
	 * @return the next AddId to use.
	 */
	private Long generateAddId() {
        Long retval = new Long(nextAddId);
        nextAddId++;
		return retval;
	}

	/**
	 * Checks that the runningState is RUNNING.
	 * This is a simple helper method used to check that the running state 
     * is RUNNING.
	 * if it's not, it throws an exception.
	 * Using this method makes many methods much more readable.
     *
	 * @throws IllegalStateException if state is not RUNNING.
	 */
	private void checkRunningState() {
		if (state != RunningState.RUNNING) {
			String s = "checkRunningState: state is " + state;
			logger.error("s");
			throw new IllegalStateException(s);
		}
	}

	/**
	 *@inheritDoc
	 * @throws IllegalStateException if state is not RUNNING.
	 */
	public synchronized void addDocument(final Document doc) {
		checkRunningState();
		String docId = doc.get(docIdName);
		if (null == docId) {
			logger.error("No documentId specified. Ignoring addition.");
			return;
		}
        if (!verifySegment(docId)) {
            // error logged on verifySegment
            return;
        }
		Long addId = generateAddId();
        doc.add(new Field("AddId", addId.toString(), Field.Store.YES, Field.Index.UN_TOKENIZED));
		try {
            if ( logger.isEnabledFor(Level.DEBUG)) { 
                logger.debug("Adding document with AddId=" + addId + " and docId=" + docId);
            }
			writer.addDocument(doc);
			lastOperation.put(docId, addId);
		} catch (IOException e) {
			logger.error(e);
		}
	}

	/**
	 *@inheritDoc
	 * @throws IllegalStateException if state is not RUNNING.
	 */
	public synchronized void deleteDocument(final String id) {
		checkRunningState();
		lastOperation.put(id, new Long(0));
	}

    private boolean verifySegment(String docId) {
        // if there is only one segment, the documents have
        // to be in this segment
        if (1 == indexDescriptor.getTotalNumberOfNodes()) {
            return true;
        }
        // otherwise, check that the document hashes to this node.
        int nodeNumber = indexDescriptor.getNodeNumber();
        int hash = hashFunction.hash(docId);
        if (nodeNumber!= hash) {
            String error = "Trying to add a document that does not hash to this indexer."
                        + " Maybe the MultiIndexer configuration is wrong (indexers are mixed)"
                        + " or some client is indexing here directly. Unfortunately, I can not"
                        + " tell the difference."
                        + " This indexer handles node " + indexDescriptor.getNodeNumber() + "("+nodeNumber+")"
                        + " and got a document for node " + hashFunction.hash(docId) + "(" + hash + ")."
                        + " documentId is \"" + docId + "\"";
            logger.error(error);
            return false;
        }
        // no error found. 
        return true;
    }


	/**
	 * Performs the deletes and remove duplicates from the index.
	 */
	private synchronized void applyDeletes() {
		IndexReader reader = null;
		IndexSearcher searcher = null;
		try {
			reader = IndexReader.open(indexDirectory);
            Set<Integer> documentsToDelete = new HashSet<Integer>();
			Enumeration keysEnum = lastOperation.keys();
            //First we collect the lucene ids of document to be deleted.
			while (keysEnum.hasMoreElements()) {
                searcher = new IndexSearcher(reader);
				String key = (String) keysEnum.nextElement();
				// if the last operation is a delete lastAddition will be 0 and we'll find no match in the index.
				//This way, all the documents with that DocumentId will be erased.
                String lastAddition = String.valueOf((Long) (lastOperation.get(key)));
                if ( logger.isEnabledFor(Level.DEBUG)) { 
				    logger.debug("Applying deletes: searching " + docIdName	+ " = [" + key + "]");
                }
				Hits hits = searcher.search(new TermQuery(new Term(docIdName, key)));
                if ( logger.isEnabledFor(Level.DEBUG)) { 
                    logger.debug("Applying deletes: found matches: " + hits.length());
                }
                logger.debug("HITS LENGTH: " + hits.length());
				for (int i = 0; i < hits.length(); i++) {
					String addId = hits.doc(i).get("AddId");
					if (!lastAddition.equals(addId)) {
                        if ( logger.isEnabledFor(Level.DEBUG)) { 
                            logger.debug("Applying deletes: deleting AddId:" + addId);
                        }
                        documentsToDelete.add(hits.id(i));
					}
				}
			}
            //Now we have all lucene's ids of documents to be deleted and we can
            //proceed with the actual deletion.
            for (Integer i : documentsToDelete) {
                reader.deleteDocument(i);
            }
            
		} catch (IOException e) {
			logger.fatal("applyDeletes: IOException caught.", e);
            throw new RuntimeException(e);
		} finally {
			if (searcher != null) {
				try {
					searcher.close();
				} catch (Exception e) {
					String s = "applyDeletes: Couldn't close searcher, nothing I can do about it" + e;
					logger.error(s);
					throw new IllegalStateException(s);					
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {
                    logger.warn("Couldn't close reader, nothing I can do about it", e);
				}
			}
		}

		lastOperation.clear();
	}

	/**
	 * Opens the writer.
	 * @throws RuntTimeException if there was a problem opening the writer.
	 */
	private void openWriter() {
        writer = workIndex.getWriter();
	}

	/**
	 * Closes the writer.
	 */
	private void closeWriter() {
		com.flaptor.util.Execute.close(writer, logger);
	}

	/**
	 * Closes the writer to be sure that all changes are in the directory and
	 * then calls the shell command that makes a copy of it. Used to make a copy
	 * of the directory while it's in a consistent state. If there is an index
	 * optimization scheduled, it'll be performed here.
	 * @throws IllegasStateException if the index copy couldn't be made.
	 * @throws RuntimeException if there was a problem opening the index.
	 */
	public synchronized void makeDirectoryCheckpoint() {
		closeWriter();
		applyDeletes();
		if (optimizeScheduled) {
			logger.info("Beginning index optimization..");
			try {
                // make sure that the writer is open to optimize.
                openWriter();
				// If optimize() fails, optimization signal keeps schedules.
				writer.optimize();
                workIndex.setIndexProperty("lastOptimization", String.valueOf(System.currentTimeMillis()));
                IndexerMonitoredNode.getInstance().setProperty(LAST_OPTIMIZE,System.currentTimeMillis());
				optimizeScheduled = false;
				logger.info("Index optimization complete.");
			} catch (IOException e) {
				logger.fatal("Exception caught while trying to optimize the index. ", e);
                throw new RuntimeException(e);
			}
            finally {
                // anyway, we need to close the writer after trying to optimize
		        closeWriter();
            }
		}

        workIndex.setIndexProperty("lastCheckpoint", String.valueOf(System.currentTimeMillis()));
        IndexerMonitoredNode.getInstance().setProperty(LAST_CHECKPOINT,System.currentTimeMillis());


        // add index to persistent library. 
        // it will push the index if needed.
        boolean addedToLibrary = library.addIndex(workIndex);

        if (!addedToLibrary) {
            logger.fatal("Could not add index to library.");
            throw new RuntimeException("Could not add index to library.");
        }

		openWriter();
	}

	/**
	 * Schedules an optimization to be done during the next makeDirectoryCheckpoint.
	 */
	public void scheduleOptimize() {
		checkRunningState();
		optimizeScheduled = true;
	}

	/**
	 * @inheritDoc
	 */
	public void requestStop() {
		state = RunningState.STOPPING;
		new StopperThread().start();
	}

	/**
	 * @inheritDoc
	 */
	public boolean isStopped() {
		return state == RunningState.STOPPED;        
	}

	//----------------------------------------------------------------------------------------------------
	//Internal classes
	//----------------------------------------------------------------------------------------------------
	/**
	 * Periodically calls makeDirectoryCheckpoint. The interval is determined by
	 * updateInterval. Setting the update interval to 0, disables the calls.
	 * @todo review exception handling
	 */
	private class UpdaterThread extends AStoppableThread {
		private final int updateInterval;

		/**
		 * Default constructor.
		 */
		public UpdaterThread() {
			thrd.setName("IndexManager:UpdaterThread");
			updateInterval = config.getInt("IndexManager.updateInterval");
		}

		/**
		 * Runs the thread. Should no be called directly. Use "start()" insted.
		 */
		public void run() {
			if (updateInterval == 0) {
				logger.warn("updateInterval is set to 0. No automatic index checkpoints will be performed. You'll have to do it manually.");
                stopped = true;
				return;
			}

			while (!signaledToStop) {
				sleep(updateInterval);
				logger.info("Calling makeDirectoryCheckpoint()");
				try {
					makeDirectoryCheckpoint();
				} catch (Exception e) {
					//We can be here because there was a problem doing the copy (a case that may be solved by
					// the operator, or because there was a problem opening the index (probably unsolvable).
					//TODO: there should be a set of specific exceptions to separate the 2 cases.
					logger.error("Exception caught in the UpdaterThread", e);
                    System.exit(-1);
				}
			}
			stopped = true;
		}
	}

	/**
	 * This class handles the shutdown.
	 * It closes the index and then sets the IndexManager's state to stopped.
	 * This thread is necessary to make the IndexManager's stop call non blocking.
	 */
	private class StopperThread extends Thread {
		private final Logger logger = Logger.getLogger(StopperThread.class);

		public StopperThread() {
			super();
			setName("IndexManager.StopperThread");
		}

		/**
		 * Executes asincronously the Indexer shutdown secuence.
		 * After that, sets the Indexer state to Stopped.
		 */
		public void run() {
			logger.info("Stopping.");
			assert(state == RunningState.STOPPING);
            logger.debug("Stopping UpdaterThread...");
			ut.requestStop();
			while (!ut.isStopped()) {
				try {
					sleep(20);
				} catch (InterruptedException e) {
					logger.warn("Interrupted while sleeping.");
				}
			}
			logger.debug("UpdaterThread stopped.");
			try {
				makeDirectoryCheckpoint();
				closeWriter();
			} catch (Exception e) {
				logger.error("Catched exception while executing shutdown sequence. I'll set the state to closed, but something went really wrong.\n", e);
			}
			logger.debug("Stopping IndexLibrary...");
            library.requestStop();
            while (!library.isStopped()) {
                Execute.sleep(20);
            }
			logger.debug("IndexLibrary stopped.");
			logger.info("Stopped.");
			state = RunningState.STOPPED;
		}
	}
}
