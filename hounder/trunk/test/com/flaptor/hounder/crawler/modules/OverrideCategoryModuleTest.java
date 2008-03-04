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

import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.util.Config;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class OverrideCategoryModuleTest extends TestCase {

    private Config globalConfig;
    private Config moduleConfig;
    private String moduleName;

    public void setUp() {
        globalConfig = Config.getConfig("crawler.properties");
        moduleName = "override";
        moduleConfig = Config.getConfig(moduleName + "Module.properties");
    }
    
    public void tearDown() {
    }
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testDeletesEveryCategory() throws Exception {

        moduleConfig.set("categories.order","sites,forums");
        AProcessorModule module = new OverrideCategoryModule(moduleName,globalConfig);
        FetchDocument doc = new FetchDocument(new Page("",1f));
        doc.addCategory("sites");
        doc.addCategory("forums");
        doc.addCategory("nutrition");
        module.process(doc);

        assertTrue("categories where " + doc.getCategories() + ". expected nutrition.",doc.getCategories().size() == 1);
        assertTrue("categories where " + doc.getCategories() + ". expected nutrition.",doc.getCategories().contains("nutrition"));
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testLeavesMostImportant() throws Exception {
        moduleConfig.set("categories.order","sites,forums");
        AProcessorModule module = new OverrideCategoryModule(moduleName,globalConfig);
        FetchDocument doc = new FetchDocument(new Page("",1f));
        doc.addCategory("sites");
        doc.addCategory("forums");
        module.process(doc);
        
        assertTrue("categories where " + doc.getCategories() + ". expected forums.",doc.getCategories().size() == 1);
        assertTrue("categories where " + doc.getCategories() + ". expected forums.",doc.getCategories().contains("forums"));
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testLeavesOnlyCategory() throws Exception {
        moduleConfig.set("categories.order","sites,forums");
        AProcessorModule module = new OverrideCategoryModule(moduleName,globalConfig);
        FetchDocument doc = new FetchDocument(new Page("",1f));
        doc.addCategory("sites");
        module.process(doc);
        
        assertTrue("categories where " + doc.getCategories() + ". expected sites.",doc.getCategories().size() == 1);
        assertTrue("categories where " + doc.getCategories() + ". expected sites.",doc.getCategories().contains("sites"));
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testDoesNothing() throws Exception {
        moduleConfig.set("categories.order","sites,forums");
        AProcessorModule module = new OverrideCategoryModule(moduleName,globalConfig);
        FetchDocument doc = new FetchDocument(new Page("",1f));
        doc.addCategory("keep");
        module.process(doc);
        
        assertTrue("categories where " + doc.getCategories() + ". expected keep.",doc.getCategories().size() == 1);
        assertTrue("categories where " + doc.getCategories() + ". expected keep.",doc.getCategories().contains("keep"));
    }
}
