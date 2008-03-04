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
public class AQueryTest extends TestCase {

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testEqualsObject() {
        DummyQuery q1 = new DummyQuery();
        assertEquals("Testing the same object", q1, q1);
        
        DummyQuery q2 = new DummyQuery();
        assertEquals("Testing two different but default created objects", q1, q2);
        
        DummyQuery q3 = new DummyQuery(2f);
        DummyQuery q4 = new DummyQuery(2f);
        assertEquals("Testing 2 non default created object with the same boost to be equal", q3, q4);
        
        DummyQuery q5 = new DummyQuery(0.5f);
        assertFalse("Testing 2 non defoult created objects with different boost to be different", q5.equals(q4));
        
        assertFalse("Testing equals not to fail with null", q5.equals(null));
        
        assertFalse("Testing equals to return false with a different class", q5.equals(new Float(1f)));
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
