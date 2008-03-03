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
package com.flaptor.search4j.crawler.modules;

import java.io.IOException;
import java.util.Set;

import com.flaptor.search4j.crawler.pagedb.Page;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;
import com.flaptor.util.TestUtils;
import com.flaptor.util.Execute;


/**
 * @author Flaptor Development Team
 */
public class ATrueFalseModuleTest extends TestCase {

    private static final String POS_PROP= "Module.properties";
    private static final String MOCK_TRUE="mockTrue";
    private static final String MOCK_FALSE="mockFalse";
    private String tmpConfig = "tmp.properties";

    
    public void setUp() throws IOException {
        TestUtils.setConfig(tmpConfig,
                "page.text.max.length=250\n" +
                "hotspot.tag=TAG_IS_HOTSPOT\n" +
                "emitdoc.tag=TAG_IS_INDEXABLE");

        String trueMock="on.true.set.tags=tag_true_set \n" +
            "on.true.unset.tags=tag_true_unset \n"+
            "on.false.set.tags= \n"+
            "on.false.unset.tags= \n" +
            "pass.through.on.tags= \n" +
            "pass.through.on.missing.tags=\n";
        TestUtils.writeFile(MOCK_TRUE + POS_PROP, trueMock);
        
        String falseMock="on.true.set.tags= \n" +
        "on.true.unset.tags=\n"+
        "on.false.set.tags=tag_false_set \n"+
        "on.false.unset.tags=tag_false_unset \n" +
        "pass.through.on.tags= \n" +
        "pass.through.on.missing.tags=\n";
        TestUtils.writeFile(MOCK_FALSE + POS_PROP, falseMock);
    }
    
    public void tearDown() {
        FileUtil.deleteFile(MOCK_TRUE + POS_PROP);
        FileUtil.deleteFile(MOCK_FALSE + POS_PROP);
        FileUtil.deleteFile(tmpConfig);
    }
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testTrueInternalProcess() throws Exception {
        MockTrueModule trueModule= new MockTrueModule(MOCK_TRUE);
        FetchDocument doc= new FetchDocument(new Page("",1f));
        assertFalse(doc.hasTag(MockTrueModule.TAG_MOCKTRUE_INTERNAL_PROCESS_WAS_CALLED));
        Set<String> setT= trueModule.getSetTag();
        Set<String> unsetT= trueModule.getUnsetTag();

        for (String t: setT){
            doc.delTag(t);
        }
        for (String t: unsetT){
            doc.addTag(t);
        }

        assertTrue(setT.size() > 0); // else we are checking nothing
        assertTrue(unsetT.size() > 0); // else we are checking nothing
        trueModule.process(doc);
        assertTrue(doc.hasTag(MockTrueModule.TAG_MOCKTRUE_INTERNAL_PROCESS_WAS_CALLED));
        assertTrue(setT.size() > 0); // else we are checking nothing
        assertTrue(unsetT.size() > 0); // else we are checking nothing

        for (String t: setT){
            assertTrue(doc.hasTag(t));
        }
        for (String t: unsetT){
            assertFalse(doc.hasTag(t));
        }        
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testFalseInternalProcess() throws Exception {
        MockFalseModule module= new MockFalseModule(MOCK_FALSE);
        FetchDocument doc= new FetchDocument(new Page("",1f));
        assertFalse(doc.hasTag(MockFalseModule.TAG_MOCKFALSE_INTERNAL_PROCESS_WAS_CALLED));
        Set<String> setT= module.getSetTag();
        Set<String> unsetT= module.getUnsetTag();
        for (String t: setT){
            doc.delTag(t);
        }
        for (String t: unsetT){
            doc.addTag(t);
        }

        assertTrue(setT.size() > 0); // else we are checking nothing
        assertTrue(unsetT.size() > 0); // else we are checking nothing
        module.process(doc);
        assertTrue(doc.hasTag(MockFalseModule.TAG_MOCKFALSE_INTERNAL_PROCESS_WAS_CALLED));

        for (String t: setT){
            assertTrue(doc.hasTag(t));
        }
        for (String t: unsetT){
            assertFalse(doc.hasTag(t));
        }        
    }

}
