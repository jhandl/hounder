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
public class OrQueryTest extends TestCase {

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testHashCode() {
        DummyQuery d1 = new DummyQuery(1f);
        DummyQuery d2 = new DummyQuery(2f);
        OrQuery o1 = new OrQuery(d1, d2);
        OrQuery o2 = new OrQuery(d2, d1);
        
        assertEquals(o1.hashCode(), o2.hashCode());
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testEqualsObject() {
        DummyQuery d1 = new DummyQuery(1f);
        DummyQuery d2 = new DummyQuery(2f);
        OrQuery o1 = new OrQuery(d1, d2);
        OrQuery o2 = new OrQuery(d2, d1);
        OrQuery o3 = new OrQuery(d2, d1);
        OrQuery o4 = new OrQuery(d1, d1);
        assertEquals(o2, o3);
        assertEquals("Testing for commutativeness",o1, o2); //Commutative
        assertFalse(o4.equals(o1));
        assertFalse(o4.equals(o2));
        assertFalse(o4.equals(o3));
        assertFalse("Testing against an instance of other class", o4.equals(d1));
    }
    
    //-----------------------------------------------------------
    //Private class
    private class DummyQuery extends AQuery {

        private static final long serialVersionUID = 1L;

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
