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
package com.flaptor.search4j.indexer;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class CommandsModuleTest extends TestCase {
    private CommandsModule mod;

    @Override
    public void setUp() {
        mod = new CommandsModule(null);
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testAcceptNonNull() {
        Document document = DocumentHelper.createDocument();
		Element root = document.addElement("documentDelete");
		root.addElement("documentId").addText("111");

		try {
			mod.process(document);
		} catch (Exception e) {
			assertTrue(false);
		}
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
	public void testRejectNull() {
		try {
			mod.process(null);
			assertTrue(false);
		} catch (IllegalArgumentException e) {
			//Being here is soooo goood.
		}
	}
}
