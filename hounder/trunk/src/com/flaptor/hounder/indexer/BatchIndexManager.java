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
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

import com.flaptor.hounder.Index;
import com.flaptor.hounder.IndexDescriptor;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.RunningState;
import com.flaptor.util.Stoppable;

public final class BatchIndexManager implements Stoppable {
	private static final Logger logger = Logger.getLogger(Execute.whoAmI());


//    private Indexer indexer;

	//Lucene related variables.
    private final Index index;
    private IndexWriter writer;
	private String docIdName = null;

    private AtomicLong nextAddId= new AtomicLong();

	//State related te the shutdown sequence.
	private volatile RunningState state = RunningState.RUNNING;


	/**
	 * Default constructor.
     * Takes the required information from the configuration file.
     *
     * Configuration strings:
     *      docIdName:
     *          the string to use as identifier of the document id while
     *          storing it in the index.
	 * @throws IllegalArgumentException
     *          when there are problems with the arguments taken from the
	 * 		    configuration file.
	 * @throws IllegalStateException
     *          when the filesystem is not ready to be set up (permissions,
	 * 		    space?)
	 */
	public BatchIndexManager(IndexDescriptor indexDescriptor, File indexDirectory) {
        index = Index.createIndex(indexDirectory);
        index.setIndexDescriptor(indexDescriptor);
        writer= index.getWriter();
        Config config = Config.getConfig("indexer.properties");
		docIdName = config.getString("docIdName");
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
	 * @throws IllegalStateException if state is not RUNNING.
	 */
	public void addDocument(final Document doc) throws IOException {
		checkRunningState();
		String docId = doc.get(docIdName);
		if (null == docId) {
			logger.error("No documentId specified. Ignoring addition.");
			return;
		}
		Long addId = nextAddId.incrementAndGet();
        doc.add(new Field("AddId", addId.toString(), Field.Store.YES, Field.Index.UN_TOKENIZED));
        if ( logger.isEnabledFor(Level.DEBUG)) {
            logger.debug("Adding document with AddId=" + addId + " and docId=" + docId);
        }
        writer.addDocument(doc);
	}

    @Override
	public void requestStop() {
		state = RunningState.STOPPING;
        com.flaptor.util.Execute.close(writer, logger);
		state = RunningState.STOPPED;
	}

    @Override
	public boolean isStopped() {
		return state == RunningState.STOPPED;
	}

}
