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

import java.rmi.registry.LocateRegistry;

import com.flaptor.util.Config;
import com.flaptor.util.PortUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;
import com.flaptor.util.remote.RmiServer;

/**
 * Tests the Indexer through an rmi connection directly (without
 * using the client stub.
 * @author Flaptor Development Team
 */
public class RmiIndexerTest extends TestCase {
	private String tmpDir = null;
    
    @Override
    public void setUp() {
		tmpDir = com.flaptor.util.FileUtil.createTempDir("RmiIndexerStubTest", ".tmp").getAbsolutePath();
		Config commonConfig = Config.getConfig("common.properties");
        commonConfig.set("baseDir", tmpDir);

        Config config = Config.getConfig("indexer.properties");
        config.set("Indexer.modules", "com.flaptor.hounder.indexer.Writer");
        config.set("IndexManager.updateInterval", "1000");
        config.set("Indexer.maxQueueSize", "100");
        config.set("IndexLibrary.remoteIndexUpdaters", "");
        config.set("IndexLibrary.rsyncAccessString", "");
        config.set("clustering.enable","false");
    }

	@Override
	public void tearDown() {
		com.flaptor.util.FileUtil.deleteDir(tmpDir);
	}
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testConnection() throws Exception {
        int port = PortUtil.getPort("indexer.rmi");
        IIndexer baseIndexer = new Indexer();
        IIndexer indexer = new MultipleRpcIndexer(baseIndexer,true, false);
        try {
            LocateRegistry.getRegistry(port).lookup(RmiServer.DEFAULT_SERVICE_NAME);
        } catch (Exception e) {
            fail("Exception caught while looking up RmiServer. " + e.toString());
        }
        indexer.requestStop();
        while (!indexer.isStopped()) {
            Thread.sleep(20);
        }
    }
    
}
