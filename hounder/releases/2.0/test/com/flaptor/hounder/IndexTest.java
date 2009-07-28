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
package com.flaptor.hounder;

import java.io.File;

import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class IndexTest extends TestCase {
    File dir, indexDir;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        filterOutput("There is no index descriptor on index.properties. using default");
        dir = FileUtil.createTempDir("junit", ".tmp");
        indexDir = new File(dir.getAbsolutePath() + File.separator + "index");
    }
    
    @Override
    protected void tearDown() {
        unfilterOutput();
        FileUtil.deleteDir(dir);
    }
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testStoreAndLoad() {
        Index index = Index.createIndex(indexDir);
        index.close();
        
        index = new Index(indexDir);
    }
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testMetadataStore() {
        Index index = Index.createIndex(indexDir);
        index.setIndexProperty("foo", "bar");
        index.close();
        index = new Index(indexDir);
        assertEquals("bar", index.getIndexProperty("foo"));
    }

}
