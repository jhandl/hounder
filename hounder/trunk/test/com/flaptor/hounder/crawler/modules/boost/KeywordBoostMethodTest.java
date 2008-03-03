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
package com.flaptor.search4j.crawler.modules.boost;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import com.flaptor.search4j.crawler.modules.FetchDocument;
import com.flaptor.search4j.crawler.pagedb.Page;
import com.flaptor.util.Config;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class KeywordBoostMethodTest extends TestCase {

    private KeywordBoostMethod kbm;
    private String field;
    private String keyword;
    private int times;


    public void setUp() throws IOException{
        filterOutputRegex("[0-9 ]*(parsing|Using|impl:|not including|logging|No FS|Plugins|status|[a-z\\.]* = ).*");

        field = "text_field";
        keyword = "text_keyword";
        times = 10;

        // Hack, as this property file is always there .. 
        Config config = Config.getConfig("common.properties");
        config.set("method.keyword.applyto.field",field);
        config.set("method.keyword.patterns.file","kbm.patterns");

        com.flaptor.util.TestUtils.writeFile("kbm.patterns","*||"+keyword);


        //config.set("value.constant","1000");
        kbm = new KeywordBoostMethod(config);
    }

    public void tearDown() {
        File file = new File("kbm.patterns");
        if (file.exists()) {
            file.delete();
        }
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testApplyBoost() throws MalformedURLException {
        Page page = new Page("http://test.flaptor.com",1f);
        FetchDocument doc = new FetchDocument(page);
        kbm.applyBoost(doc,times);

        String text = doc.getIndexableAttribute(field).toString();
        int found = 0;
        for (String value : text.split(" ")) {
            if (value.equals(keyword)){
                found++;
            }
        }
        assertTrue(times == found);
    }

}
