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

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.flaptor.util.Config;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author spike
 */
public class FieldFormatCheckerModuleTest extends TestCase {
    private FieldFormatCheckerModule mod;

    /**
     * @see TestCase#setUp()
     */
    public void setUp() {
        Config config = Config.getConfig("indexer.properties");
        config.set("FieldFormatChecker.fields","date:long");
        mod = new FieldFormatCheckerModule();
        filterOutput("Dropping");
    }

    /**
     * @see TestCase#tearDown()
     */
    public void tearDown() {
        mod = null;
        unfilterOutput();
    }

    /**
     */
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testDropsNonParseable() {
        Document dom = DocumentHelper.createDocument();
        Element rootElement = DocumentHelper.createElement("documentAdd");
        dom.setRootElement(rootElement);


        
        Element field = DocumentHelper.createElement("field");
        field.addAttribute("name","date")
             .addText("1234aoe");

        rootElement.add(field);

        Document[] docs = mod.process(dom);

        assertEquals(docs.length,0);
    }


    /**
     */
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testDropsMissingField() {
        Document dom = DocumentHelper.createDocument();
        Element rootElement = DocumentHelper.createElement("documentAdd");
        dom.setRootElement(rootElement);

        Element field = DocumentHelper.createElement("field");
        field.addAttribute("name","not-date")
             .addText("1234");

        rootElement.add(field);

        Document[] docs = mod.process(dom);

        assertEquals(docs.length,0);
    }


    /**
     */
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testPassesValidDocument() {
        Document dom = DocumentHelper.createDocument();
        Element rootElement = DocumentHelper.createElement("documentAdd");
        dom.setRootElement(rootElement);

        Element field = DocumentHelper.createElement("field");
        field.addAttribute("name","date")
             .addText("1234");

        rootElement.add(field);

        Document[] docs = mod.process(dom);

        assertEquals(docs.length,1);
    }

    /**
     */
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testPassesNonAdd() {
        Document dom = DocumentHelper.createDocument();
        Element rootElement = DocumentHelper.createElement("documentDelete");
        dom.setRootElement(rootElement);

        Element field = DocumentHelper.createElement("field");
        field.addAttribute("name","date")
             .addText("1234aoeuaoeu");

        rootElement.add(field);

        Document[] docs = mod.process(dom);

        assertEquals(docs.length,1);
    }


}
