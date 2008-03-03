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
package com.flaptor.search4j.classifier.bayes;

import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class TokenCounterTest extends TestCase {

    private TokenCounter counter_1_24x5;
    private TokenCounter counter_2_24x5;
    private TokenCounter counter_3_24x2;
    private TokenCounter counter_4_15x5;
    private TokenCounter counter_5_0x0;
    private TokenCounter counter_6_0x0;

    /**
     * Sets four counters up.
     * counter_1_24x5 has 5 documents with a total of 24 ocurrencies.
     * counter_2_24x5 has 5 documents with a total of 24 ocurrencies.
     * counter_3_24x2 has 2 documents with a total of 24 ocurrencies.
     * counter_4_15x5 has 5 documents with a total of 15 ocurrencies.
     * counter_5_0x0 has no documents.
     * counter_6_0x0 has no documents.
     */
    public void setUp() {
        counter_1_24x5 = new TokenCounter();
        counter_2_24x5 = new TokenCounter();
        counter_3_24x2 = new TokenCounter();
        counter_4_15x5 = new TokenCounter();
        counter_5_0x0 = new TokenCounter();
        counter_6_0x0 = new TokenCounter();

        counter_1_24x5.update(1);
        counter_1_24x5.update(2);
        counter_1_24x5.update(5);
        counter_1_24x5.update(7);
        counter_1_24x5.update(9); // Total = 1+2+5+7+9 = 24, 5 unique.
        counter_2_24x5.update(1);
        counter_2_24x5.update(1);
        counter_2_24x5.update(1);
        counter_2_24x5.update(1);
        counter_2_24x5.update(20); // Total = 1+1+1+1+20 = 24, 5 unique.
        counter_3_24x2.update(1);
        counter_3_24x2.update(23); // Total = 1 + 23 = 24, 2 unique.
        counter_4_15x5.update(1);
        counter_4_15x5.update(2);
        counter_4_15x5.update(3);
        counter_4_15x5.update(4);
        counter_4_15x5.update(5); // Total = 1+2+3+4+5 = 15, 5 unique.
    }

    /**
     * Cleans the counters.
     */
    public void tearDown() {
        counter_1_24x5 = null;
        counter_2_24x5 = null;
        counter_3_24x2 = null;
        counter_4_15x5 = null;
        counter_5_0x0 = null;
        counter_6_0x0 = null;
    }

    /**
     * Tests that two counters are equal if have the same number of updates and the same total count.
     * Also tests reflexibility and symmetry.
     */
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testEquals() {
        assertTrue("counter_1_24x5 and counter_2_24x5 should be equal", counter_1_24x5.equals(counter_2_24x5));
        assertTrue("counter_2_24x5 and counter_1_24x5 should be equal", counter_2_24x5.equals(counter_1_24x5));
        assertTrue("counter_1_24x5 should be equal to itself", counter_1_24x5.equals(counter_1_24x5));
        assertFalse("counter_1_24x5 and counter_3_24x2 should be different", counter_1_24x5.equals(counter_3_24x2));
        assertFalse("counter_3_24x2 and counter_1_24x5 should be different", counter_3_24x2.equals(counter_1_24x5));
        assertFalse("counter_2_24x5 and counter_4_15x5 should be different", counter_2_24x5.equals(counter_4_15x5));
        assertFalse("counter_4_15x5 and counter_2_24x5 should be different", counter_4_15x5.equals(counter_2_24x5));
        assertTrue("counter_5_0x0 and counter_6_0x0 should be equals", counter_5_0x0.equals(counter_6_0x0));
        assertTrue("counter_6_0x0 and counter_5_0x0 should be equals", counter_6_0x0.equals(counter_5_0x0));
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testGetCount() {
        assertTrue("counter_1_24x5 should have count = 24, but it returns " +counter_1_24x5.getCount()+ " instead.", counter_1_24x5.getCount() == 24);
        assertTrue("counter_2_24x5 should have count = 24, but it returns " +counter_2_24x5.getCount()+ " instead.", counter_2_24x5.getCount() == 24);
        assertTrue("counter_3_24x2 should have count = 24, but it returns " +counter_3_24x2.getCount()+ " instead.", counter_3_24x2.getCount() == 24);
        assertTrue("counter_4_15x5 should have count = 15, but it returns " +counter_4_15x5.getCount()+ " instead.", counter_4_15x5.getCount() == 15);
        assertTrue("counter_5_0x0 should have count = 0, but it returns " +counter_5_0x0.getCount()+ " instead.", counter_5_0x0.getCount() == 0);
        assertTrue("counter_6_0x0 should have count = 0, but it returns " +counter_6_0x0.getCount()+ " instead.", counter_6_0x0.getCount() == 0);
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testGetCountUnique() {
        assertTrue("counter_1_24x5 should have countUnique = 5, but it returns " +counter_1_24x5.getCountUnique()+ " instead.", 
                counter_1_24x5.getCountUnique() == 5);
        assertTrue("counter_2_24x5 should have countUnique = 5, but it returns " +counter_2_24x5.getCountUnique()+ " instead.", 
                counter_2_24x5.getCountUnique() == 5);
        assertTrue("counter_3_24x2 should have countUnique = 2, but it returns " +counter_3_24x2.getCountUnique()+ " instead.", 
                counter_3_24x2.getCountUnique() == 2);
        assertTrue("counter_4_15x5 should have countUnique = 5, but it returns " +counter_4_15x5.getCountUnique()+ " instead.", 
                counter_4_15x5.getCountUnique() == 5);
        assertTrue("counter_5_0x0 should have countUnique = 0, but it returns " +counter_5_0x0.getCountUnique()+ " instead.", 
                counter_5_0x0.getCountUnique() == 0);
        assertTrue("counter_6_0x0 should have countUnique = 0, but it returns " +counter_6_0x0.getCountUnique()+ " instead.", 
                counter_6_0x0.getCountUnique() == 0);
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testHashCode() {
        assertTrue("counter_1_24x5 and counter_2_24x5 should have the same hashCode", counter_1_24x5.hashCode() == counter_2_24x5.hashCode());
        assertFalse("counter_1_24x5 and counter_3_24x2 should have different hashCodes", counter_1_24x5.hashCode() == counter_3_24x2.hashCode());
        assertFalse("counter_3_24x2 and counter_1_24x5 should have different hashCodes", counter_3_24x2.hashCode() == counter_1_24x5.hashCode());
        assertFalse("counter_2_24x5 and counter_4_15x5 should have different hashCodes", counter_2_24x5.hashCode() == counter_4_15x5.hashCode());
        assertTrue("counter_5_0x0 and counter_6_0x0 should have the same hashCode", counter_5_0x0.hashCode() == counter_6_0x0.hashCode());
    }

}

