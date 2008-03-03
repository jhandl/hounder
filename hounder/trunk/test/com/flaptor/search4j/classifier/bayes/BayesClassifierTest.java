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

import java.util.HashMap;
import java.util.Map;

import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class BayesClassifierTest extends TestCase {
    private String dirName;
    
    public BayesClassifierTest(String arg0) {
        super(arg0);
    }

    protected void setUp() throws Exception {
        super.setUp();
        dirName=  FileUtil.createTempDir("bayesClassifierTest", ".tmp").getAbsolutePath();
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        FileUtil.deleteDir(dirName);
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testClassify() {        
        String CAT_NAME= "bct";
        String PROBS_FILE= CAT_NAME + ".probabilities";
        String MY_PROBS_FILE= CAT_NAME + ".my.probabilities";
        String TOK1= "tok1"; Double TOK1_PROB= 0.93;
        String TOK2= "tok2"; Double TOK2_PROB= 0.39;
        String TOK3= "tok3"; Double TOK3_PROB= 0.87;
        String TOK4= "tok4"; Double TOK4_PROB= 0.53;
        String TOK5= "tok5"; Double TOK5_PROB= 0.01;
        Double UNKNOWN_TERM_PROB= 0.02;
        
        
        Map<String,Double> probs= new HashMap<String, Double>();
        probs.put(TOK1, TOK1_PROB - 0.3);
        probs.put(TOK2, TOK2_PROB);
        probs.put(TOK3, TOK3_PROB);        
        PersistenceManager.writeProbabilitiesToFile(dirName, PROBS_FILE, probs);
        
        Map<String,Double> myProbs= new HashMap<String, Double>();
        myProbs.put(TOK1, TOK1_PROB);
        myProbs.put(TOK4, TOK4_PROB);
        myProbs.put(TOK5, TOK5_PROB);                
        PersistenceManager.writeProbabilitiesToFile(dirName, MY_PROBS_FILE, myProbs);
        
        BayesClassifier bc= new BayesClassifier(dirName, CAT_NAME, UNKNOWN_TERM_PROB);
        Map<String,Double> resProb=  bc.getInternals();
        
        assertEquals(TOK1_PROB, resProb.get(TOK1));
        assertEquals(TOK2_PROB, resProb.get(TOK2));
        assertEquals(TOK3_PROB, resProb.get(TOK3));
        assertEquals(TOK4_PROB, resProb.get(TOK4));
        assertEquals(TOK5_PROB, resProb.get(TOK5));        
        assertEquals(5, resProb.size());
/*        
        Map<String, int[]> doc= new HashMap<String, int[]>();
        doc.put(TOK1, new int[] {1});
        doc.put(TOK2, new int[] {2});
        doc.put("untok", new int[]{1});
        doc.put("untok2", new int[]{1});
        Double res= bc.classify(doc);
        
        Double match=TOK1_PROB * TOK2_PROB;
        Double noMatch= UNKNOWN_TERM_PROB * UNKNOWN_TERM_PROB;
        Double reqRes= match/ (match+ noMatch);
        System.err.println("noM=" + noMatch);
        System.err.println("mat=" + match);
        System.err.println("res=" + reqRes);
        assertEquals(reqRes, res);
*/      
        
    }

}
