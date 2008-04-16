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
package com.flaptor.hounder.searcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.antlr.stringtemplate.StringTemplate;

import com.flaptor.hounder.cluster.MultiSearcher;
import com.flaptor.hounder.searcher.CompositeSearcher;
import com.flaptor.hounder.indexer.Indexer;
import com.flaptor.hounder.searcher.group.NoGroup;
import com.flaptor.hounder.searcher.group.StoredFieldGroup;
import com.flaptor.hounder.searcher.query.LazyParsedQuery;
import com.flaptor.hounder.searcher.query.MatchAllQuery;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.Execution;
import com.flaptor.util.FileUtil;
import com.flaptor.util.MultiExecutor;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * Test for MultiSearcher
 * 
 * @author Martin Massera
 */
public class MultiSearcherTest extends TestCase {

    private StringTemplate addTemplate = new StringTemplate("<documentAdd><documentId>$documentId$</documentId><field name=\"content\" stored=\"true\" indexed=\"true\" tokenized=\"true\">$content$</field><field name=\"group\" stored=\"true\" indexed=\"true\" tokenized=\"true\">$group$</field></documentAdd>");
    private int startPort = 30000;
    private int numServers = 3;
    private int docsPerSearcher = 10;
    private int docsPerGroup = 2;
    
    private List<ISearcher> searchers; 
    private List<Indexer> indexers;
    private List<String> tmpDirs;
    private MultiExecutor<Void> executor = new MultiExecutor<Void>(10, "testMultiSearcher");
    
    public void setUp() {
        Config.getConfig("searcher.properties").set("multiSearcher.workerThreads", "10");
        indexers = new ArrayList<Indexer>(numServers);
        searchers = new ArrayList<ISearcher>(numServers);
        tmpDirs = new ArrayList<String>(numServers * 2);
    } 

    public void tearDown() {
        // request stops
        for (Indexer indexer: indexers) {
            indexer.requestStop();
        }
        // wait for them
        for (Indexer indexer: indexers) {
            while (!indexer.isStopped()) {
                Execute.sleep(30);
            }
        }
        // delete temp dirs
        for (String tmpDir: tmpDirs) {
            FileUtil.deleteDir(tmpDir);
        }
    }

    private void setUpSearcher(int numServer) throws Exception {
   	
    	Config searcherConfig = Config.getConfig("searcher.properties");
        String tmpDir = com.flaptor.util.FileUtil.createTempDir("junit", ".tmp").getAbsolutePath();
        String searcherTmpDir = com.flaptor.util.FileUtil.createTempDir("junit", ".tmp").getAbsolutePath();

        int basePort = getBasePort(numServer);
        Config indexerConfig = Config.getConfig("indexer.properties");
        Config.getConfig("common.properties").set("baseDir", tmpDir);
        Config.getConfig("common.properties").set("port.base", String.valueOf(basePort));
        // Configuration
        // indexer:
        indexerConfig.set("IndexManager.updateInterval", "1000");
        indexerConfig.set("IndexLibrary.remoteIndexUpdaters","127.0.0.1:"+String.valueOf(basePort));
        indexerConfig.set("Indexer.modules", "com.flaptor.hounder.indexer.Writer");
        indexerConfig.set("Indexer.fields", "content");
        indexerConfig.set("docIdName", "docId");
        indexerConfig.set("Indexer.maxQueueSize", "100");
        indexerConfig.set("IndexLibrary.rsyncAccessString","");
        indexerConfig.set("Writer.fields", "");
        indexerConfig.set("clustering.enable", "false");
        // searcher:
        searcherConfig.set("QueryParser.searchFields", "content");
        searcherConfig.set("QueryParser.searchFieldWeights", "1.0f");
        searcherConfig.set("QueryParser.nonTokenizedFields", "");
        searcherConfig.set("Searcher.workingDirPath", searcherTmpDir);
        searcherConfig.set("ReloadableIndexSearcher.minTimeBetweenIndexes", "1000");
        searcherConfig.set("ReloadableIndexSearcher.sleepTime", "1000");
        searcherConfig.set("Searcher.generateSnippets", "false");        
		searcherConfig.set("clustering.enable", "false");

        Indexer indexer = new Indexer();
        for (int i = 0; i < docsPerSearcher ; i++) {
            addTemplate.setAttribute("content", "content " + numServer);
            addTemplate.setAttribute("documentId", String.valueOf(i) + "-" + String.valueOf(numServer));
            addTemplate.setAttribute("group", "group"+String.valueOf(i/docsPerGroup));
            indexer.index(addTemplate.toString());
            addTemplate.reset();
        }
        indexers.add(indexer);

        ISearcher baseSearcher = new CompositeSearcher();
        ISearcher searcher = new MultipleRpcSearcher(baseSearcher,true/*rmi*/,false,false,false);
        searchers.add(searcher);

        tmpDirs.add(tmpDir);
        tmpDirs.add(searcherTmpDir);
    }

    private int getBasePort(int numServer) {
        return startPort + 1000 * numServer;
    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION,
            requiresPort = {30000})
//just test that there are no exceptions searching through rmi
    public void testRmiSearch() throws Exception {
        int basePort = getBasePort(0);
        setUpSearcher(0);
        String hosts = "127.0.0.1:" + (basePort);
        Thread.sleep(2000);
        RmiSearcherStub stub = new RmiSearcherStub(getBasePort(0), "127.0.0.1");
        GroupedSearchResults results = stub.search(new LazyParsedQuery("0"), 0, numServers, new NoGroup(), 1, null, null);
        assertEquals(docsPerSearcher, results.totalGroupsEstimation());
    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION,
            requiresPort = {30000, 31000})
    public void testSearchNoGroup() throws Exception {
        String hosts = "";
        for (int i = 0; i < numServers; ++i) {
            int basePort = getBasePort(i);
            setUpSearcher(i);
            hosts += "127.0.0.1:" + (basePort);
            if (i < numServers - 1) hosts += ",";
        }
        Thread.sleep(2000);

        for (int i = 0; i < numServers; ++i) {
            //first check the searchers
            GroupedSearchResults results = searchers.get(i).search(new LazyParsedQuery(String.valueOf(i)), 0, numServers, new NoGroup(), 1, null, null); 
            assertEquals(docsPerSearcher, results.totalGroupsEstimation());

            //check the searchers through RMI
            RmiSearcherStub stub = new RmiSearcherStub(getBasePort(i), "127.0.0.1"); 
            results = stub.search(new LazyParsedQuery(String.valueOf(i)), 0, numServers, new NoGroup(), 1, null, null); 
            assertEquals(docsPerSearcher, results.totalGroupsEstimation());
        }

        //now check through multiSearcher
        Config.getConfig("searcher.properties").set("searcher.isMultiSearcher", "true");
        Config.getConfig("multiSearcher.properties").set("multiSearcher.hosts", hosts);
        doQueries(false);
        doQueries(true);
    }

    private void doQueries(boolean withComposite)  throws SearcherException {
        final ISearcher multiSearcher = withComposite ? new CompositeSearcher() : new MultiSearcher();

        Execution<Void> execution = new Execution<Void>();
        GroupedSearchResults results = multiSearcher.search(new LazyParsedQuery("content"), 0, numServers, new NoGroup(), 1, null, null);
        assertEquals(numServers * docsPerSearcher, results.totalGroupsEstimation());
        for (int times = 0; times < 50; times++){
            for (int i = 0; i < numServers; ++i) {
                final int numSearcher= i;
                execution.addTask(new Callable<Void>() {
                    public Void call() throws Exception {
                        GroupedSearchResults results = multiSearcher.search(new LazyParsedQuery(String.valueOf(numSearcher)), 0, numServers, new NoGroup(), 1, null, null);
                        if (docsPerSearcher != results.totalGroupsEstimation()) throw new Exception("not equals");
                        return null;
                    }
                });
            }
        }
        executor.addExecution(execution);
        execution.waitFor();
        assertEquals(0, execution.getProblems().size());
    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION,
            requiresPort = {30000, 31000,32000})
    public void testSearchGroups() throws Exception {
    
        Config.getConfig("searcher.properties").set("searcher.isMultiSearcher", "false");
        String hosts = "";
        for (int i = 0; i < numServers; ++i) {
            int basePort = getBasePort(i);
            setUpSearcher(i);
            hosts += "127.0.0.1:" + (basePort);
            if (i < numServers - 1) hosts += ",";
        }
        Thread.sleep(2000);

        
        //now check through multiSearcher

        Config.getConfig("multiSearcher.properties").set("multiSearcher.hosts", hosts);

        final ISearcher multiSearcher = new MultiSearcher();
        Execution<Void> execution = new Execution<Void>();
        for (int times = 0; times < 50; times++) {
            execution.addTask(new Callable<Void>() {
                public Void call() throws Exception {
                    GroupedSearchResults gsr = multiSearcher.search(new MatchAllQuery(),0,docsPerSearcher*numServers,new StoredFieldGroup("group"),docsPerGroup,null,null);
                    
                    assertEquals("Not the same count of groups.", docsPerSearcher / docsPerGroup, gsr.groups());
                    
                    for (int i = 0; i < gsr.groups(); i++) {
                        if (docsPerGroup != gsr.getGroup(i).last().size()) {
                            throw new Exception("Wrong count of results per group.");
                        }
                    }
                    return null;
                }
            });
        }
        executor.addExecution(execution);
        execution.waitFor();
        assertEquals(0, execution.getProblems().size());
    }
}
