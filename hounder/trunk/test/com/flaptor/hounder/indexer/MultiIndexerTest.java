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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;

import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;


/**
 * @author Flaptor Development Team
 */
public class MultiIndexerTest extends TestCase {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
	private List<String> tmpDirs = null;
    private List<IIndexer> rpcIndexers = null;
    private List<IIndexer> baseIndexers = null;
    private static int numIndexers = 2;
    private static int documents = 100;
    private static int basePort = 30000;

	@Override
    public void setUp() {
        filterOutput("the webserver never freed up");
	    
        tmpDirs = new ArrayList<String>(numIndexers);
        rpcIndexers = new ArrayList<IIndexer>(numIndexers);
        baseIndexers = new ArrayList<IIndexer>(numIndexers);
        String hosts = "";
        for (int i = 0; i < numIndexers ; i++) {
            setUpIndexer(i);
            hosts += "127.0.0.1:"+genPort(i)+",";
        }
        hosts = hosts.substring(0,hosts.length()-1);
        Config config = Config.getConfig("multiIndexer.properties");
        config.set("multiIndexer.useXslt","no");
        config.set("indexer.hosts",hosts);
        Config.getConfig("common.properties").set("port.base", String.valueOf(genPort(numIndexers + 1 )));
        Config.getConfig("indexer.properties").set("clustering.enable","false");
    }


    public void setUpIndexer(int i){
		String tmpDir = com.flaptor.util.FileUtil.createTempDir("junit", ".tmp").getAbsolutePath();
        com.flaptor.util.FileUtil.deleteDir(tmpDir);
        tmpDirs.add(tmpDir);
        Config.getConfig("common.properties").set("baseDir", tmpDir);
        Config.getConfig("common.properties").set("port.base", String.valueOf(genPort(i)));
        
        Config config = Config.getConfig("indexer.properties");
        //config.set("Indexer.modules", "");
        config.set("Writer.fields", "content");
        config.set("Indexer.maxQueueSize", "100");
		config.set("docIdName", "docId");
        config.set("IndexManager.updateInterval", "2000");
        config.set("IndexLibrary.remoteIndexUpdaters","");
        config.set("IndexLibrary.rsyncAccessString","");
        config.set("IndexManager.indexDescriptor",i+"of"+numIndexers+"@defaultCluster");
        config.set("Indexer.modules","com.flaptor.hounder.indexer.CommandsModule,com.flaptor.hounder.indexer.LoggerModule,com.flaptor.hounder.indexer.Writer");
       
        IIndexer baseIndexer = new Indexer();
        IIndexer indexer = new MultipleRpcIndexer(baseIndexer,true/*rmi*/,false/*xmlrpc*/);
        rpcIndexers.add(indexer);
        baseIndexers.add(baseIndexer);
    }



	@Override
	public void tearDown() {
        // request stop on every indexer
        for (IIndexer indexer: rpcIndexers) {
            indexer.requestStop();
        }
        // wait for every indexer to stop
        for (IIndexer indexer: rpcIndexers) {
            while (!indexer.isStopped()) {
                Execute.sleep(1000);
            }
        }

        // delete temporary directories.
        for (String tmpDir: tmpDirs) {
    		com.flaptor.util.FileUtil.deleteDir(tmpDir);
        }
        tmpDirs = new ArrayList<String>();
        unfilterOutput();
	}

    
    @TestInfo(testType = TestInfo.TestType.UNIT)
//        ,requiresPort = {30000, 31000})
    public void testMultiIndexer() throws Exception {
        MultiIndexer multiIndexer = new MultiIndexer(); 
    
        for (int i = 0; i < documents ; i++) {
            String document = "<documentAdd><documentId>doc"+i+"</documentId><field name=\"content\" stored=\"true\" indexed=\"true\" tokenized=\"true\">content "+i+"</field></documentAdd>";
            multiIndexer.index(document);
        }
        Execute.sleep(5000);
        multiIndexer.requestStop();

        int totalDocumentsFound = 0;
        int minDocsFound = documents / (2 * numIndexers);
        for (String tmpDir: tmpDirs) {
            String sep = java.io.File.separator;
            IndexReader reader = IndexReader.open(tmpDir + sep + "indexer" + sep + "indexes" + sep + "index");
            int docsFound = reader.maxDoc();
            reader.close();
            assertTrue("too few documents indexed. Found " + docsFound + ", expected at least" +minDocsFound , docsFound > minDocsFound);
            totalDocumentsFound += docsFound;
        }

        assertEquals("Did not index every document.", totalDocumentsFound,documents);
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
//        ,requiresPort = {30000, 31000})
    public void testMixedNodesFails() throws Exception{
        // Mix up hosts
        Config config = Config.getConfig("multiIndexer.properties");
        String[] hosts = config.getStringArray("indexer.hosts");
        StringBuffer sb = new StringBuffer();
        for (int i = hosts.length - 1 ; i >= 0; i--) {
            sb.append(hosts[i]);
            sb.append(",");
        }
        config.set("indexer.hosts",sb.substring(0,sb.length()-1));


        // filter output to avoid logging expected errors
        super.filterOutputRegex("Unfortunately");

        MultiIndexer multiIndexer = new MultiIndexer();
        for (int i = 0; i < documents ; i++) {
            String document = "<documentAdd><documentId>doc"+i+"</documentId><field name=\"content\" stored=\"true\" indexed=\"true\" tokenized=\"true\">content "+i+"</field></documentAdd>";
            multiIndexer.index(document);
        }
        Execute.sleep(5000);
        multiIndexer.requestStop();
        
       
        // check that every index is empty
        for (String tmpDir: tmpDirs) {
            String sep = java.io.File.separator;
            IndexReader reader = IndexReader.open(tmpDir + sep + "indexer" + sep + "indexes" + sep + "index");
            assertEquals("indexed " + reader.maxDoc() + " documents on mixed indexer. Mixed indexers should not index anything.",0,reader.maxDoc());
            reader.close();
        }
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
//        ,requiresPort = {30000, 31000})
    public void testCommands() throws Exception {
        String command = "<command name='close'/>";
        MultiIndexer multiIndexer = new MultiIndexer();
        int retValue = multiIndexer.index(command);

        assertEquals("Indexing command did not succeed.",Indexer.SUCCESS,retValue);

        Execute.sleep(5000);

        for (IIndexer indexer: baseIndexers) {
            assertTrue("indexer " + indexer + " is not stopped.", indexer.isStopped());
        } 
    } 


    private static int genPort(int i) {
        return basePort + 1000 * i;
    }
}
