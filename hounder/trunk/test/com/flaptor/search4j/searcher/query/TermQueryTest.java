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
package com.flaptor.search4j.searcher.query;

import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class TermQueryTest extends TestCase {

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testEqualsObject() {
        TermQuery t1 = new TermQuery("foo", "bar");
        assertEquals("testing against itself", t1, t1);
        
        TermQuery t2 = new TermQuery("foo", "bar");
        assertEquals(t1, t2);
        
        TermQuery t3 = new TermQuery(1f, "", "");
        TermQuery t4 = new TermQuery(2f, "", "");
        assertFalse(t3.equals(t4));       
    }

}
