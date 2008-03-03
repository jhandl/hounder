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

import com.flaptor.search4j.crawler.pagedb.Page;
import com.flaptor.util.Config;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;
import com.flaptor.util.TestUtils;
import com.flaptor.util.Execute;

/**
 * @author Flaptor Development Team
 */
public class AThresholdModuleTest extends TestCase {

    private static final String POS_PROP= "Module.properties";
    private static final String THRESH_PROP= "threshold";
    private String tmpConfig = "tmp.properties";
    private Config globalConfig;

    protected void setUp() throws Exception {
        super.setUp();

        TestUtils.setConfig(tmpConfig,
                "page.text.max.length=250\n" +
                "hotspot.tag=TAG_IS_HOTSPOT\n" +
                "emitdoc.tag=TAG_IS_INDEXABLE");
        globalConfig = TestUtils.getConfig();

        String thresh="threshold.value = 0.50\n" +
        "on.below.threshold.set.tags=below_tag_set\n" +
        "on.below.threshold.unset.tags=below_tag_unset\n" +
        "on.above.threshold.set.tags=below_tag_set\n" +
        "on.above.threshold.unset.tags=below_tag_unset\n"+
        "pass.through.on.tags=\n" +
        "pass.through.on.missing.tags=\n";
        TestUtils.writeFile(THRESH_PROP + POS_PROP,thresh);

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        FileUtil.deleteFile(THRESH_PROP + POS_PROP);
        FileUtil.deleteFile(tmpConfig);
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testBelow() throws Exception {        
        FetchDocument doc= new FetchDocument(new Page("",1f));        
        MockThresholdModule thMod= new MockThresholdModule(THRESH_PROP, globalConfig);

        thMod.setReturnedValue(thMod.thresholdValue - 0.1); // below
        for (String tag : thMod.onBelowUnsetTag){
            if (tag.length() < 1){// if empty, tag="";
                continue;
            }
            doc.setTag(tag);
        }
        for (String tag : thMod.onBelowSetTags){
            if (tag.length() < 1){// if empty, tag="";
                continue;
            }
            doc.delTag(tag);
        }        
        thMod.internalProcess(doc);
        for (String tag : thMod.onBelowUnsetTag){
            assertFalse(doc.hasTag(tag));
        }
        for (String tag : thMod.onBelowSetTags){
            assertTrue(doc.hasTag(tag));
        }               
        
        // check nothing happens if seting/unseting tag twice
        thMod.internalProcess(doc);
        for (String tag : thMod.onBelowUnsetTag){
            assertFalse(doc.hasTag(tag));
        }
        for (String tag : thMod.onBelowSetTags){
            assertTrue(doc.hasTag(tag));
        }               

        
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testAbove() throws Exception {        
        FetchDocument doc= new FetchDocument(new Page("",1f));        
        MockThresholdModule thMod= new MockThresholdModule(THRESH_PROP, globalConfig);

        thMod.setReturnedValue(thMod.thresholdValue + 0.1); // above
        for (String tag : thMod.onAboveUnsetTag){
            if (tag.length() < 1){// if empty, tag="";
                continue;
            }
            doc.setTag(tag);
        }
        for (String tag : thMod.onAboveSetTag){
            if (tag.length() < 1){// if empty, tag="";
                continue;
            }
            doc.delTag(tag);
        }        
        thMod.internalProcess(doc);
        for (String tag : thMod.onAboveUnsetTag){
            assertFalse(doc.hasTag(tag));
        }
        for (String tag : thMod.onAboveSetTag){
            assertTrue(doc.hasTag(tag));
        }        
        
        // check nothing happens if seting/unseting tag twice
        thMod.internalProcess(doc);
        for (String tag : thMod.onAboveUnsetTag){
            assertFalse(doc.hasTag(tag));
        }
        for (String tag : thMod.onAboveSetTag){
            assertTrue(doc.hasTag(tag));
        }        

    }
}
