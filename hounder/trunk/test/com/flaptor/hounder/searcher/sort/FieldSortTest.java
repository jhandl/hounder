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
package com.flaptor.hounder.searcher.sort;

import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class FieldSortTest extends TestCase {

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testEqualsAndHashCode() {
        FieldSort f1 = new FieldSort(false, "foo", FieldSort.OrderType.INT);
        assertEquals("testing equals against itself", f1, f1);
        assertEquals("testing hashCode against itself", f1.hashCode(), f1.hashCode());
        
        FieldSort f2 = new FieldSort(false, "foo", FieldSort.OrderType.INT);
        assertEquals("testing equals between equivalent instances", f1, f2);
        assertEquals("testing hashCode between equivalent instances", f1.hashCode(), f2.hashCode());
    }

}
