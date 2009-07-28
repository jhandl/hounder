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
package com.flaptor.hounder;

import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class IndexDescriptorTest extends TestCase {

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testValuesReturned() {
		IndexDescriptor id = new IndexDescriptor(2, 0, "");
		assertEquals(2, id.getTotalNumberOfNodes());
		assertEquals(0, id.getNodeNumber());

		id = new IndexDescriptor(3, 2, "");
		assertEquals(3, id.getTotalNumberOfNodes());
		assertEquals(2, id.getNodeNumber());
	}

    @TestInfo(testType = TestInfo.TestType.UNIT)
	public void testConstructorValidation() {
		boolean error = true;
		try {
			new IndexDescriptor(0, 0, "" );
		} catch (IllegalArgumentException e ) {
			error = false;
		}
		assertFalse(error);

		error = true;
		try {
			new IndexDescriptor(1, -1, "");
		} catch (IllegalArgumentException e ) {
			error = false;
		}
		assertFalse(error);
	}

    @TestInfo(testType = TestInfo.TestType.UNIT)
	public void testStringCostructor() {
		IndexDescriptor id = new IndexDescriptor("2of4@myCluster");
		assertEquals(id.getTotalNumberOfNodes(), 4);
		assertEquals(id.getNodeNumber(), 2);
		assertEquals(id.getClusterName(), "myCluster");
	}
}
