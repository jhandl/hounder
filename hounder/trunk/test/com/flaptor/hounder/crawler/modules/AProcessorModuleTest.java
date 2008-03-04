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
package com.flaptor.hounder.crawler.modules;

import java.io.IOException;
import java.util.Set;

import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;
import com.flaptor.util.TestUtils;

/**
 * @author Flaptor Development Team
 */
public class AProcessorModuleTest extends TestCase {

    public AProcessorModuleTest(String arg0) {
        super(arg0);
    }

    private static final String POS_PROP= "Module.properties";
    private static final String MOCK_DO="mock";
    private static final String MOCK_PASS="mockPassOnTag";
    private static final String MOCK_MISS="mockPassOnMissingTag";
    private String tmpConfig = "tmp.properties";

    
    public void setUp() throws IOException {
        TestUtils.setConfig(tmpConfig,
                "page.text.max.length=250\n" +
                "hotspot.tag=TAG_IS_HOTSPOT\n" +
                "emitdoc.tag=TAG_IS_INDEXABLE");

        String passOn="pass.through.on.tags=tag_test_pass_through \n" +
                    "pass.through.on.missing.tags= \n";
        TestUtils.writeFile(MOCK_PASS + POS_PROP, passOn);

        String passOnMissing="pass.through.on.tags= \n" +
                "pass.through.on.missing.tags=tag_this_tag_will_not_be_defined";
        TestUtils.writeFile(MOCK_MISS + POS_PROP, passOnMissing);
        
        String mockDo="pass.through.on.tags= \n" +
                      "pass.through.on.missing.tags=";
        TestUtils.writeFile(MOCK_DO + POS_PROP, mockDo);
    }
    
    public void tearDown() {
        FileUtil.deleteFile(MOCK_DO + POS_PROP);
        FileUtil.deleteFile(MOCK_PASS + POS_PROP);
        FileUtil.deleteFile(MOCK_MISS + POS_PROP);
        FileUtil.deleteFile(tmpConfig);
    }
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testPassThroughOnTags() throws Exception {
        FetchDocument doc= new FetchDocument(new Page("",1f));
        
        MockPassOnTagModule mm1= new MockPassOnTagModule(MOCK_PASS);        
        Set<String> passT= mm1.getPassThroughOnTags();
        assertTrue(passT.size() > 0); // else we are checking nothing
        for (String t: passT){
            doc.addTag(t);
        }        
        mm1.process(doc);
        
        doc= new FetchDocument(new Page("",1f));
        MockPassOnTagMissingModule mm2= new MockPassOnTagMissingModule(MOCK_MISS);        
        // Make sure, this tag is not defined. Else will not passThrough, and 
        // the test will fail:
        passT= mm2.getPassThroughOnMissingTags();
        assertTrue(passT.size() > 0); // else we are checking nothing
        for (String t: passT){
            doc.delTag(t);
        }
        mm2.process(doc);
                

        MockModule mm3= new MockModule(MOCK_DO);
        doc= new FetchDocument(new Page("",1f));
        // Make sure, this tag is not defined. Else the test will be false
        doc.delTag(MockModule.TAG_MOCK3_INTERNAL_PROCESS_WAS_CALLED);
        assertFalse(doc.hasTag(MockModule.TAG_MOCK3_INTERNAL_PROCESS_WAS_CALLED));
        mm3.process(doc);
        assertTrue(doc.hasTag(MockModule.TAG_MOCK3_INTERNAL_PROCESS_WAS_CALLED));

    }

}
