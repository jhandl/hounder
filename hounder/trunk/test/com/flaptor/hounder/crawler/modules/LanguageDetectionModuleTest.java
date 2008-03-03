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
public class LanguageDetectionModuleTest extends TestCase {

    private static final String POS_PROP= "Module.properties";
    private static final String MOD_NAME="lang";

    private static final String SP_TEXT1="Bienvenido al 8vo. Encuentro Linux ";            
    private static final String SP_TEXT2="Si desea recibir la edici\u00f3n digital";
    private static final String SP_TEXT3="Sin acentos edicion comunicacion";

    private static final String EN_TEXT1="Welcome to the Linux world expo";            
    private static final String EN_TEXT2="Would you like a cup of tea";
    private static final String EN_TEXT3="He's celebrating with his family";

    private String tmpConfig = "tmp.properties";
    private LanguageDetectionModule ldm;

    public LanguageDetectionModuleTest() throws IOException{
    }
    
    public void setUp() throws Exception {
        filterOutputRegex("[0-9 ]*(parsing|Language).*");
        String mockDo="pass.through.on.tags= \n" +
                      "pass.through.on.missing.tags=";
        TestUtils.setConfig(tmpConfig,
                "page.text.max.length=250\n" +
                "hotspot.tag=TAG_IS_HOTSPOT\n" +
                "emitdoc.tag=TAG_IS_INDEXABLE");
        Config cfg = TestUtils.getConfig();

        //TestUtils.writeFile(MOCK_LANG + POS_PROP, mockDo);
        TestUtils.writeFile(MOD_NAME + POS_PROP, mockDo);
        ldm= new LanguageDetectionModule(MOD_NAME, cfg);
    }
    
    public void tearDown() {
        FileUtil.deleteFile(MOD_NAME + POS_PROP);
        FileUtil.deleteFile(tmpConfig);
    }
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testSpanish() throws Exception {
        FetchDocument doc;
        String lang= LanguageDetectionModule.LANGUAGE_TAG+"es"; 
        doc= new FetchDocument(new Page("",1f), null, SP_TEXT1, null, null, null, null,
                true, false, true);
        assertEquals(0,doc.getTags().size());
        ldm.process(doc);
        assertEquals(1,doc.getTags().size());
        assertTrue(doc.getTags().contains(lang));
        
        doc= new FetchDocument(new Page("",1f), null, SP_TEXT2, null, null, null, null,
                true, false, true);
        assertEquals(0,doc.getTags().size());
        ldm.process(doc);
        assertEquals(1,doc.getTags().size());
        assertTrue(doc.getTags().contains(lang));

        doc= new FetchDocument(new Page("",1f), null, SP_TEXT3, null, null, null, null,
                true, false, true);
        assertEquals(0,doc.getTags().size());
        ldm.process(doc);
        assertEquals(1,doc.getTags().size());
        assertTrue(doc.getTags().contains(lang));
    }
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testEnglish() throws Exception {
        FetchDocument doc;
        String lang= LanguageDetectionModule.LANGUAGE_TAG+"en";
        doc= new FetchDocument(new Page("",1f), null, EN_TEXT1, null, null, null, null,
                true, false, true);
        assertEquals(0,doc.getTags().size());
        ldm.process(doc);
        assertEquals(1,doc.getTags().size());
        assertTrue(doc.getTags().contains(lang));
        
        doc= new FetchDocument(new Page("",1f), null, EN_TEXT2, null, null, null, null,
                true, false, true);
        assertEquals(0,doc.getTags().size());
        ldm.process(doc);
        assertEquals(1,doc.getTags().size());
        assertTrue(doc.getTags().contains(lang));

        doc= new FetchDocument(new Page("",1f), null, EN_TEXT3, null, null, null, null,
                true, false, true);
        assertEquals(0,doc.getTags().size());
        ldm.process(doc);
        assertEquals(1,doc.getTags().size());
        assertTrue(doc.getTags().contains(lang));

    }


}
