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

import org.apache.log4j.Logger;

import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class IndexerTest extends TestCase {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
	private String tmpDir = null;

	@Override
    public void setUp() {
		tmpDir = com.flaptor.util.FileUtil.createTempDir("junit", ".tmp").getAbsolutePath();
        com.flaptor.util.FileUtil.deleteDir(tmpDir);
        Config config = Config.getConfig("indexer.properties");
        // Configuration
        // indexer:
        Config.getConfig("common.properties").set("baseDir", tmpDir);
        config.set("Indexer.modules", "");
        config.set("Indexer.fields", "content");
        config.set("Indexer.maxQueueSize", "100");
		config.set("docIdName", "docId");
        config.set("IndexManager.updateInterval", "0");
        config.set("IndexLibrary.remoteIndexUpdaters","");
        config.set("IndexLibrary.rsyncAccessString","");
        config.set("clustering.enable","false");
    }

	@Override
	public void tearDown() {
		com.flaptor.util.FileUtil.deleteDir(tmpDir);
	}

    
    /**
     * Tests that the indexer stops after some time.
     * It doesn't test that the shutdows is clean.
     */
    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testShutdown() {
        Indexer indexer = new Indexer();
        assertFalse(indexer.isStopped());
        indexer.requestStop();
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            logger.error("testShutdown: interrupted while sleeping.");
        }
        assertTrue(indexer.isStopped());
    }
    
    
}
