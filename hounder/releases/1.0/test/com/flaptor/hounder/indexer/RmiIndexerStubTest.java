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

import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * Tests the rmi client-side (stub).
 * @author Flaptor Development Team
 */
public class RmiIndexerStubTest extends TestCase {

    private MultipleRpcIndexer indexer;
    private static final int BASE_PORT = 47000;
    private static final int OFFSET_RMI = 0;

    @Override
    protected void setUp() {
        Config common = Config.getConfig("common.properties");
        common.set("port.base", "" + BASE_PORT);
        common.set("port.offset.indexer.rmi", "" + OFFSET_RMI);
        common.set("baseDir", com.flaptor.util.FileUtil.createTempDir("RmiIndexerStubTest", ".tmp").getAbsolutePath());
        Config config = Config.getConfig("indexer.properties");
        config.set("IndexManager.updateInterval", "10000");
        config.set("Indexer.maxQueueSize", "100");
        config.set("Indexer.modules", "com.flaptor.hounder.indexer.Writer");
        config.set("IndexLibrary.remoteIndexUpdaters", "");
        config.set("IndexLibrary.rsyncAccessString", "");
        config.set("clustering.enable","no"); //We are not testing clusterfest here.
        config.set("Writer.fields","");
		
        IIndexer baseIndexer = new Indexer();
        indexer = new MultipleRpcIndexer(baseIndexer,true, false);

    }

    @Override
    protected void tearDown() {
        if (null != indexer) {
            indexer.requestStop();
            while (!indexer.isStopped()) {
                Execute.sleep(20);
            }
        }
    }


    @TestInfo(testType = TestInfo.TestType.INTEGRATION,
            requiresPort = {BASE_PORT + OFFSET_RMI})
    public void testIndexingFailOnUnparsable() {
        RmiIndexerStub indexerStub = new RmiIndexerStub(BASE_PORT, "localhost");
        int retVal = -1;
        try {
            retVal = indexerStub.index("this text is not valid xml");
        } catch (Exception e) {
            fail("catched exception while indexing" + e);
        }
        assertEquals(Indexer.PARSE_ERROR, retVal);
    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION,
            requiresPort = {BASE_PORT + OFFSET_RMI})
    public void testIndexingSuccesOnAdd() {
        RmiIndexerStub indexerStub = new RmiIndexerStub(BASE_PORT, "localhost");
        int retVal = -1;
        try {
            retVal = indexerStub.index("<documentAdd><documentId>1</documentId></documentAdd>");
        } catch (Exception e) {
            fail("catched exception while indexing" + e);
        }
        assertEquals(Indexer.SUCCESS, retVal);
    }

}
