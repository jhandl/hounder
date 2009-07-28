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
import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.dom4j.Document;

import com.google.common.base.Preconditions;

import com.flaptor.clusterfest.NodeListener;
import com.flaptor.clusterfest.action.ActionModule;
import com.flaptor.clusterfest.controlling.ControllerModule;
import com.flaptor.clusterfest.controlling.node.ControllableImplementation;
import com.flaptor.clusterfest.deploy.DeployListenerImplementation;
import com.flaptor.clusterfest.deploy.DeployModule;
import com.flaptor.clusterfest.monitoring.MonitorModule;
import com.flaptor.hounder.indexer.clustering.IndexerActionReceiver;
import com.flaptor.hounder.indexer.util.Hash;
import com.flaptor.hounder.IndexDescriptor;
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
 */
public class BatchMultiIndexer implements IRmiIndexer, IIndexer, Stoppable {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    private RunningState state = RunningState.RUNNING;
    private final int clusterSize;
    private final BatchIndexManager[] indexManagers;
    private final Hash hashFunction;
    private final String docIdName;
    private final File outputDirectory;
    private DocumentParser parser = new DocumentParser();

    public BatchMultiIndexer(int clusterSize, File outputDirectory, String fragmentName) {
        Preconditions.checkArgument(clusterSize > 0, "Invalid clusterSize.");
        Preconditions.checkArgument(null != outputDirectory && outputDirectory.exists() && outputDirectory.isDirectory(), "check the output directory.");
        this.clusterSize = clusterSize;
        this.outputDirectory = outputDirectory;
        hashFunction = new Hash(clusterSize);
        indexManagers = new BatchIndexManager[clusterSize];
        for (int i = 0; i < clusterSize; i++) {
            IndexDescriptor id = new IndexDescriptor(clusterSize, i , fragmentName);
            indexManagers[i] = new BatchIndexManager(id, new File(outputDirectory, "index-" + String.valueOf(i)));
        }
        //Some of the configuration is taken from the config system
        Config config = Config.getConfig("indexer.properties");
        docIdName = config.getString("docIdName");
    }

    public IndexerReturnCode index(final Document doc) {
        // Changed from indexDom because there is no need to use the param type as part of the method name.
        logger.debug("index(Dom): received data to index");
        if (state != RunningState.RUNNING) {
            String s = "index(Dom): Trying to index a document but the Indexer is no longer running.";
            logger.error(s);
            throw new IllegalStateException(s);
        }

        org.apache.lucene.document.Document ldoc;
        try {
            ldoc = DocumentConverter.getInstance().convert(doc);
        } catch (DocumentConverter.IllegalDocumentException e) {
            return IndexerReturnCode.PARSE_ERROR;
        }
        String docId = ldoc.get(docIdName);
        if (null == docId) {
            logger.error("No documentId specified. Ignoring addition.");
            return IndexerReturnCode.PARSE_ERROR;
        }

        int targetFragment = hashFunction.hash(docId);
        try {
            indexManagers[targetFragment].addDocument(ldoc);
        } catch (IOException e) {
            logger.error("Exception while trying to add a document to index number " + targetFragment, e);
            return IndexerReturnCode.FAILURE;
        }
        return IndexerReturnCode.SUCCESS;
    }

    public IndexerReturnCode index(final String text) {
        if (state != RunningState.RUNNING) {
            String s = "index: Trying to index a document but the Indexer is no longer running.";
            logger.error(s);
            throw new IllegalStateException(s);
        }
        Document doc = parser.genDocument(text);
        if (null == doc) {
            return (IndexerReturnCode.PARSE_ERROR);
        } else {
            return index(doc);
        }
    }

    @Override
        public boolean isStopped() {
            return (state == RunningState.STOPPED);
        }

    @Override
        public void requestStop() {
            if (state == RunningState.RUNNING) {
                state = RunningState.STOPPING;
                new StopperThread().start();
            } else {
                logger.warn("requestStop: stop requested while not running. Ignoring.");
            }
        }

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
            for (BatchIndexManager im : indexManagers) {
                im.requestStop();
            }
            for (BatchIndexManager im : indexManagers) {
                while (!im.isStopped()) {
                    com.flaptor.util.Execute.sleep(50);
                }
            }
            logger.info("Stop sequence finished.");
            state = RunningState.STOPPED;
        }
    }

    public static void printUsage() {
        System.out.println("Usage: \n"+
                    "BatchMultiIndexer clusterSize outputDirectory fragmentName basePort");
    }

    public static void main(final String[] args) throws Exception {
        if (4 != args.length) {
            printUsage();
            System.exit(-1);
        }
        int clusterSize = Integer.valueOf(args[0]).intValue();
        File outputDirectory = new File(args[1]);
        String fragmentName = args[2];
        int basePort = Integer.valueOf(args[3]).intValue();

        outputDirectory.mkdir();
        System.out.println("Creating the BatchMultiIndexer");
        BatchMultiIndexer bmi = new BatchMultiIndexer(clusterSize, outputDirectory, fragmentName);
        Runtime.getRuntime().addShutdownHook( new ShutdownHook(bmi));
        System.out.println("Exporting via rpc");
        Config.getConfig("common.properties").set("basePort", String.valueOf(basePort));
        new MultipleRpcIndexer(bmi, true, true);
    }

    private static class ShutdownHook extends Thread {
        private final Stoppable toStop;

        public ShutdownHook(Stoppable toStop) {
            this.toStop = toStop;
        }

        @Override
        public void run() {
            System.err.println("Shutdown catched. Trying to close cleanly.");
            toStop.requestStop();
            while (!toStop.isStopped()) {
                com.flaptor.util.Execute.sleep(50);
            }
            System.err.println("Shutdown sequence completed.");
        }
    }
}
