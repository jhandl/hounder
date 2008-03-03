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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.flaptor.util.Config;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;
import com.flaptor.util.TestUtils;

/**
 * @author Flaptor Development Team
 */
public class BayesCalculatorTest extends TestCase {
    private String CONFIG_FILE;
    private String dirName;
    BayesCalculator bc;
    Map<String, int[]> docTknCat;
    Map<String, int[]> docTknNonCat;
    static final String CATEGORY_NAME="testcat";
    
    public BayesCalculatorTest(String arg0) throws FileNotFoundException, IOException {
        super(arg0);
    }

    protected void setUp() throws Exception {
        super.setUp();
        dirName=  FileUtil.createTempDir("bayesCalculatorTest", ".tmp").getAbsolutePath();
        docTknCat= new HashMap<String, int[]>();
        docTknNonCat= new HashMap<String, int[]>();
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        FileUtil.deleteFile(CONFIG_FILE);
        FileUtil.deleteDir(dirName);
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testProbs() throws IOException {
        CONFIG_FILE= "tmp.bayes.calculator.test.config";
        FileUtil.deleteFile(CONFIG_FILE);
        String cfg="bayes.calculator.min.required.non.categorized=0\n" +
        "bayes.calculator.min.required.categorized=0\n" +
        "bayes.calculator.min.required.both=0\n" +
        "bayes.calculator.ignore.term.probability.from=0.43\n"+
        "bayes.calculator.ignore.term.probability.to=0.44\n"+
        "bayes.calculator.tmp.dir=" +  dirName + "\n";
        TestUtils.writeFile(CONFIG_FILE, cfg);
        Config config=  Config.getConfig(CONFIG_FILE);
        bc= new BayesCalculator(dirName, CATEGORY_NAME, config, 1);

        /* Adding the following 
           tokens   timesCat    timesNonCat
            strA    4            0
            strB    3            1
            strC    2            2
            strD    1            3
            strE    0            4
          */ 
        docTknCat.put("strA", new int[]{1});
        docTknNonCat.put("strE", new int[]{1});
        for (int i=0; i< 4; i++){
            bc.addData(docTknCat, true, "caturl");
            bc.addData(docTknNonCat, false, "noncaturl");
        }
        docTknCat.clear();
        docTknNonCat.clear();

        docTknCat.put("strB", new int[]{1});
        docTknNonCat.put("strD", new int[]{1});
        for (int i=0; i< 3; i++){
            bc.addData(docTknCat, true, "caturl");
            bc.addData(docTknNonCat, false, "noncaturl");
        }
        docTknCat.clear();
        docTknNonCat.clear();

        docTknCat.put("strC", new int[]{1});
        docTknNonCat.put("strC", new int[]{1});        
        for (int i=0; i< 2; i++){
            bc.addData(docTknCat, true, "caturl");
            bc.addData(docTknNonCat, false, "noncaturl");
        }
        docTknCat.clear();
        docTknNonCat.clear();

        docTknCat.put("strD", new int[]{1});
        docTknNonCat.put("strB", new int[]{1});        
        for (int i=0; i< 1; i++){
            bc.addData(docTknCat, true, "caturl");
            bc.addData(docTknNonCat, false, "noncaturl");
        }
        docTknCat.clear();
        docTknNonCat.clear();
       
        
        
        bc.computeProbabilities();
        Map<String,Double> probs= PersistenceManager.readProbabilitiesFromFile(dirName, CATEGORY_NAME+".probabilities" );
//        printProbs(probs);
        assertEquals(BayesProbabilities.CATEGORY_MAX_PROBABILITY, probs.get("strA"));
        assertTrue(0.751> probs.get("strB") && 0.749 < probs.get("strB")); // round problems
        assertEquals(0.5, probs.get("strC"));
        assertEquals(0.25, probs.get("strD"));
        assertEquals(BayesProbabilities.CATEGORY_MIN_PROBABILITY, probs.get("strE"));
        
    }
    
    @SuppressWarnings("unused")
    private void printProbs(Map<String,Double> probs){
        System.err.println("********* PRINTING PROBS **********");
        SortedSet<String> s= new TreeSet<String>(probs.keySet());
        for (String tkn: s){
            System.err.println(tkn + ":" + probs.get(tkn));
        }                
        System.err.println("*********** DONE ***************");
    }

    // Checks that the *.min.required.* are used
    @TestInfo(testType = TestInfo.TestType.UNIT)
   public void testMinRequired() throws IOException {
        CONFIG_FILE= "tmp.bayes.calculator.test2.config";
        FileUtil.deleteFile(CONFIG_FILE);
        String cfg="bayes.calculator.min.required.non.categorized=2\n" +
        "bayes.calculator.min.required.categorized=2\n" +
        "bayes.calculator.min.required.both=5\n" +
        "bayes.calculator.ignore.term.probability.from=0.43\n"+
        "bayes.calculator.ignore.term.probability.to=0.44\n"+
        "bayes.calculator.tmp.dir=" +  dirName + "\n";
        TestUtils.writeFile(CONFIG_FILE, cfg);
        Config config=  Config.getConfig(CONFIG_FILE);
        bc= new BayesCalculator(dirName, CATEGORY_NAME, config, 1);
        
        docTknCat.put("not_enough_cat", new int[]{1});
        docTknNonCat.put("not_enough_non_cat", new int[]{1});
        for (int i=0; i< 2; i++){
            bc.addData(docTknCat, true, "caturl");
            bc.addData(docTknNonCat, false, "noncaturl");
        }
        docTknCat.clear();
        docTknNonCat.clear();

        docTknCat.put("not_enough_both", new int[]{1});
        bc.addData(docTknCat, true, "caturl");
        docTknNonCat.put("not_enough_both", new int[]{1});
        for (int i=0; i< 3; i++){
            bc.addData(docTknNonCat, false, "noncaturl");
        }
        docTknCat.clear();
        docTknNonCat.clear();

        docTknCat.put("enough_cat", new int[]{1});
        docTknNonCat.put("enough_non_cat", new int[]{1});
        for (int i=0; i< 3; i++){
            bc.addData(docTknCat, true, "caturl");
            bc.addData(docTknNonCat, false, "noncaturl");
        }
        docTknCat.clear();
        docTknNonCat.clear();
        
        docTknCat.put("enough_both", new int[]{1});
        for (int i=0; i< 4; i++){
            bc.addData(docTknCat, true, "caturl");
        }
        docTknNonCat.put("enough_both", new int[]{1});
        for (int i=0; i< 2; i++){
            bc.addData(docTknNonCat, false, "noncaturl");
        }
        docTknCat.clear();
        docTknNonCat.clear();

        bc.computeProbabilities();
        Map<String,Double> probs= PersistenceManager.readProbabilitiesFromFile(dirName, CATEGORY_NAME+".probabilities" );
        
        //printProbs(probs);
        assertNull(probs.get("not_enough_cat"));
        assertNull(probs.get("not_enough_non_cat"));
        assertNull(probs.get("not_enough_both"));
        assertNotNull(probs.get("enough_cat"));
        assertNotNull(probs.get("enough_non_cat"));
        assertNotNull(probs.get("enough_both"));
    }
}
