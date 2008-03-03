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
import org.antlr.stringtemplate.StringTemplate;

import com.flaptor.search4j.Index;
import com.flaptor.search4j.MultiIndex;
import com.flaptor.search4j.searcher.IndexUpdatesListener;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class IndexComposerTest extends TestCase {

    private StringTemplate addTemplate = new StringTemplate("<documentAdd><documentId>a</documentId><field name=\"content\" stored=\"true\" indexed=\"true\" tokenized=\"true\">$content$</field></documentAdd>");
    private int numServers = 2;
    private String tmpDir;
    private ArrayList<Indexer> indexers;

    protected void setUp() throws Exception{
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < numServers; i++) {
        //    setUpIndexer(i);
            sb.append("cluster"+i + ",");
        }
        Config.getConfig("common.properties").set("port.base","10000");
        Config composerConfig = Config.getConfig("composer.properties");
        tmpDir = com.flaptor.util.FileUtil.createTempDir("junit", ".tmp").getAbsolutePath();
        Config.getConfig("common.properties").set("baseDir", tmpDir);
        composerConfig.set("IndexComposer.clusters",sb.substring(0,sb.length() - 1));
        composerConfig.set("IndexComposer.baseDir",tmpDir);

        Config.getConfig("indexer.properties").set("Writer.fields", "");
        Config.getConfig("indexer.properties").set("clustering.enable","false");

        indexers = new ArrayList<Indexer>();
    }
    
    protected void tearDown() throws Exception{
        super.tearDown();
        FileUtil.deleteDir(tmpDir);
    }


    private void setUpIndexer(int numServer) throws Exception {
        String tmpDir = com.flaptor.util.FileUtil.createTempDir("junit", ".tmp").getAbsolutePath();
        Config indexerConfig = Config.getConfig("indexer.properties");
        // Configuration
        // indexer:
        Config.getConfig("common.properties").set("baseDir", tmpDir);
        indexerConfig.set("IndexManager.updateInterval", "1000");
        indexerConfig.set("IndexManager.indexDescriptor", "0of1@cluster"+numServer);
        indexerConfig.set("IndexLibrary.remoteIndexUpdaters","127.0.0.1:10000");
        indexerConfig.set("Indexer.modules", "com.flaptor.search4j.indexer.Writer");
        indexerConfig.set("Indexer.fields", "content");
        indexerConfig.set("docIdName", "docId");
        indexerConfig.set("Indexer.maxQueueSize", "100");
        indexerConfig.set("IndexLibrary.rsyncAccessString","");

        Indexer indexer = new Indexer();
        addTemplate.setAttribute("content", "content " + numServer);
        indexer.index(addTemplate.toString());        

        indexers.add(indexer);
    }
   

    private void stopIndexers() {
        for (Indexer indexer : indexers) {
            indexer.requestStop();
        }
        for (Indexer indexer : indexers) {
            while (!indexer.isStopped()) {
                Execute.sleep(30);
            }
        }
        indexers.clear();
    }


    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testMerge() throws Exception{
        
        Config searcherConfig = Config.getConfig("searcher.properties");
        searcherConfig.set("searcher.dir", "searcher");
        IndexComposer composer = new IndexComposer();
        new IndexUpdatesListener(composer); // dont keep reference
        
        for (int i = 0; i < numServers; i++) {
            setUpIndexer(i);
        }

        Execute.sleep(3000);
        Index index = composer.getCurrentIndex();

        if (index instanceof MultiIndex) {
            assertTrue(true);
        } else {
            assertTrue(false);
        }

        stopIndexers();
    }   
}
