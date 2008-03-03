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
package com.flaptor.search4j.indexer.util;

import java.util.Random;

import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class HashTest extends TestCase {

    @TestInfo(testType = TestInfo.TestType.UNIT)
	public void testInvalidConstructorWithZero() {
		try {
			new Hash(0);
			fail();
		} catch (IllegalArgumentException e) {
			//This is ok
		}
	}

    @TestInfo(testType = TestInfo.TestType.UNIT)
	public void testInvalidConstructorWithNegativeOB() {
		try {
			new Hash(0);
			fail();
		} catch (IllegalArgumentException e) {
			//This is ok
		}
	}
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testDistribution() {
        Hash f = new Hash(2);
        int counter = 0;
        Random rand = new Random(System.currentTimeMillis());
        
        for (int i =0 ; i < 1000; i++) {
            byte[] bytes = new byte[Math.abs(rand.nextInt()) % 255];
            if (0 == f.hash(new String(bytes))) {
                counter++;
            }
        }
        assertTrue("Imbalanced hash. counter was " + counter, (counter > 450) && (counter <550));
    }
}

