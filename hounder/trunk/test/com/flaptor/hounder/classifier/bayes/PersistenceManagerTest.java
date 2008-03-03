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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class PersistenceManagerTest extends TestCase {

    private String dirname;
    private String category;
    private Map<String,Double> probabilities;
    private Map<String,TokenCounter> counters;
    
    /**
     * Creates both probabilities and counters maps.
     */
    public void setUp() {
        dirname = FileUtil.createTempDir("persistenceManagerTest", ".tmp").getAbsolutePath();
        category = "test_category";
        probabilities = new HashMap<String,Double>();
        Random random = new Random(System.currentTimeMillis());
        int limit = random.nextInt(1000) + 100;
        for (int i=0; i<limit; i++) {
            probabilities.put("PrEFiX"+Integer.toString(i),Double.valueOf(random.nextDouble()));
        }
        counters = new HashMap<String,TokenCounter>();
        limit = random.nextInt(1000) + 100;
        for (int i=0; i<limit; i++) {
            TokenCounter counter = new TokenCounter();
            counter.update(1+random.nextInt(100));
            counters.put("pRefIx"+Integer.toString(i),counter);
        }
        
    }

    /**
     * Cleans probabilities and counters maps.
     */
    public void tearDown() { 
        probabilities.clear();
        probabilities = null;
        counters.clear();
        counters = null;
        FileUtil.deleteDir(dirname);
    }

    /**
     * Tests that a counters map is written to disk in a recoverable way.
     */
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testProbabilitiesWriteAndRecover() {

        String filename = category+"-probabilities";

        if (!deleteFile(dirname, filename)) {
            throw new IllegalStateException("Error deleting the test file");
        }

        File f = new File(dirname,filename);
        assertFalse("There's a map file, it shouldn't be!", f.exists());
        PersistenceManager.writeProbabilitiesToFile(dirname, filename, probabilities);
        assertTrue("There's no map file!", f.exists());
        Map<String,Double> mapRead = PersistenceManager.readProbabilitiesFromFile(dirname, filename);
        assertTrue("Written map is different to read map", probabilities.equals(mapRead));

    }

    /**
     * Tests that a counters map is written to disk in a recoverable way.
     */
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testCountersWriteAndRecover() {

        String filename = category+"-categorized";

        if (!deleteFile(dirname, filename)) {
            throw new IllegalStateException("Error deleting the test file");
        }

        File f = new File(dirname,filename);
        assertFalse("There's a map file, it shouldn't be!", f.exists());
        PersistenceManager.writeCountersToFile(dirname, filename, counters);
        assertTrue("There's no map file!", f.exists());
        Map<String,TokenCounter> mapRead = PersistenceManager.readCountersFromFile(dirname, filename);
        assertTrue("Written map is different to read map", counters.equals(mapRead));

    }

    /**
     * Deletes a file if it exists.
     * @param dirname directory name
     * @param filename file name
     * @return true if the file didn't exist or if it existed and was deleted
     */
    private boolean deleteFile(String dirname, String filename) {
        File f = new File(dirname,filename);
        return (!f.exists() || (f.exists() && f.delete()));
    }

}

