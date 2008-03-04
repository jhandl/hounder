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
package com.flaptor.hounder.crawler.modules.boost;

import com.flaptor.hounder.crawler.modules.FetchDocument;
import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.util.Config;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class ConstantBoostValueTest extends TestCase {

    private ConstantBoostValue cbv;

    public void setUp() {
        Config config = Config.getEmptyConfig();
        config.set("value.constant","1000");
        cbv = new ConstantBoostValue(config);
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testHasValue() throws Exception {
        FetchDocument doc = new FetchDocument(new Page("",1f));
        assertTrue(cbv.hasValue(doc));
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testGetValue() throws Exception {
        FetchDocument doc = new FetchDocument(new Page("",1f));
        assertTrue(new Double(1000).equals(cbv.getValue(doc)));
    }

}
