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

import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * A very simple for the LoggerModule.
 * Tests that it doesn't change the data as it passes
 * through it.
 * @author spike
 */
public class LoggerModuleTest extends TestCase {
    private LoggerModule mod;

    /**
     * @see TestCase#setUp()
     */
    public void setUp() {
        mod = new LoggerModule();
    }

    /**
     * @see TestCase#tearDown()
     */
    public void tearDown() {
        mod = null;
    }

    /**
     * A trivial test: checks that it send exactly one document.
     */
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testSize() {
        Document document = DocumentHelper.createDocument();
        Document[] returnedDocs = mod.process(document);
        assertEquals("The module didn't return 1 document", 1,
                returnedDocs.length);
    }

    /**
     * Tests that the (only) returned document has the same data as the one
     * processed.
     */
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testEquality() {
        // we create a dom document.
        // This code was shamelessly cut&pasted from the dom4j quick start
        // guide.
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("root");

        root.addElement("author").addAttribute("name", "James").addAttribute(
                "location", "UK").addText("James Strachan");

        root.addElement("author").addAttribute("name", "Bob").addAttribute(
                "location", "US").addText("Bob McWhirter");
        // now that we have something to process, we compare the origial version
        // with
        // the processed one.
        // As document has no equals method, we compare the xml representation.
        String original = document.asXML();
        Document pdoc = mod.process(document)[0];
        String modified = pdoc.asXML();
        assertEquals(
                "The returned document is different from the processed one",
                original, modified);
    }
}
