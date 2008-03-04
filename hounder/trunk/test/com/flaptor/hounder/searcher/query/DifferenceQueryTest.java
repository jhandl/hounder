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
package com.flaptor.hounder.searcher.query;

import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class DifferenceQueryTest extends TestCase {

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testEqualsObject() {
        DummyQuery d1 = new DummyQuery(1f);
        DummyQuery d2 = new DummyQuery(2f);
        DifferenceQuery a1 = new DifferenceQuery(d1, d2);
        DifferenceQuery a2 = new DifferenceQuery(d2, d1);
        DifferenceQuery a3 = new DifferenceQuery(d2, d1);
        DifferenceQuery a4 = new DifferenceQuery(d1, d1);
        assertEquals(a2, a3);
        assertFalse("Testing for non commutativeness",a1.equals(a2));
        assertFalse(a3.equals(a4));
        assertFalse(a1.equals(d1));
    }
    //-----------------------------------------------------------
    //Private class
    private class DummyQuery extends AQuery {
        public DummyQuery() {
            super();
        }
        
        public DummyQuery(float f) {
            super(f);
        }
        
        public org.apache.lucene.search.Query getLuceneQuery() {
            return null;
        }
    }
}
