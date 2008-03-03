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


import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.flaptor.util.Config;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class IndexManagerTest extends TestCase {
    private String tmpDir;
    private IndexManager man;
    
    @Override
    public void setUp() {
        tmpDir = com.flaptor.util.FileUtil.createTempDir("junit", ".tmp").getAbsolutePath();
        // Configuration
        // common
        Config.getConfig("common.properties").set("baseDir", tmpDir);
        // indexer:
        Config config = Config.getConfig("indexer.properties");
        config.set("Indexer.fields", "content");
        config.set("docIdName", "docId");
        config.set("IndexManager.updateInterval", "1000");
        config.set("IndexLibrary.remoteIndexUpdaters","");
        config.set("IndexLibrary.rsyncAccessString","");
        man = new IndexManager();
    }

    @Override
    public void tearDown() {
        man.requestStop();
        while( !man.isStopped()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {}
        }
        com.flaptor.util.FileUtil.deleteDir(tmpDir);
    }

    private Document createField() {
        Document doc = new Document();
        doc.add(new Field("docId", "theOnlyDocId", Field.Store.YES, Field.Index.UN_TOKENIZED));
        return doc;
    }
    
    /**
     * There was a bug lurking from early 2006 to late 2007 that showed up with this test.
     * The problem was caused by deleting the documents while still using the hits. As hits
     * is lazy, this caused an ArrayOutOfBounds exception on sometimes.
     */
    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testDoNotCrackWithManyUpdates() {
        for (int i = 0; i < 101; i++) {
            man.addDocument(createField());
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}
    }

}
