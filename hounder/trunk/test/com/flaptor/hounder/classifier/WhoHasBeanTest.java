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
package com.flaptor.search4j.classifier;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class WhoHasBeanTest extends TestCase {
    private File tmpDir; 
    public WhoHasBeanTest(String arg0) {
        super(arg0);
    }

    protected void setUp() throws Exception {
        tmpDir= FileUtil.createTempDir("whoHasBeanTest", ".tmp");
        super.setUp();
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        FileUtil.deleteDir(tmpDir);
    }

    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testWhoHas() throws IOException {
        Map<String, int[]> docTkn= new HashMap<String, int[]>();;
        WhoHasBean whb= new WhoHasBean();
        whb.initialize("/whoHasUnsorted", "/whoHasSorted", 
                tmpDir.getAbsolutePath() + "/whoHasDone", tmpDir.getAbsolutePath());
        

        String IN_1="in_url_1";
        String IN_2="in_url_2";
        String IN_3="in_url_3";
        String IN_12="in_url_1_and_2";
        String IN_123="in_url_1_and_2_and_3";
        
        String URL_1="url1";
        String URL_2="url2";
        String URL_3="url3";
        
        HashSet<String> whoHasin1= new HashSet<String>();
        whoHasin1.add(URL_1);
        HashSet<String> whoHasin2= new HashSet<String>();
        whoHasin2.add(URL_2);
        HashSet<String> whoHasin3= new HashSet<String>();
        whoHasin3.add(URL_3);
        HashSet<String> whoHasin12= new HashSet<String>();
        whoHasin12.add(URL_1);whoHasin12.add(URL_2);
        HashSet<String> whoHasin123= new HashSet<String>();
        whoHasin123.add(URL_1);whoHasin123.add(URL_2);whoHasin123.add(URL_3);
        
        docTkn.clear();
        docTkn.put(IN_1, new int[]{1});
        docTkn.put(IN_12, new int[]{1});        
        docTkn.put(IN_123, new int[]{1});
        whb.addData(docTkn, "url1");

        docTkn.clear();    
        docTkn.put(IN_2, new int[]{1});
        docTkn.put(IN_12, new int[]{1});
        docTkn.put(IN_123, new int[]{1});
        whb.addData(docTkn, "url2");

        docTkn.clear();    
        docTkn.put(IN_3, new int[]{1});
        docTkn.put(IN_123, new int[]{1});
        whb.addData(docTkn, "url3");
        
        whb.computeWhoHas();

        assertEquals(whoHasin1, whb.getWhoHas(IN_1));
        assertEquals(whoHasin2, whb.getWhoHas(IN_2));
        assertEquals(whoHasin3, whb.getWhoHas(IN_3));
        assertEquals(whoHasin12, whb.getWhoHas(IN_12));
        assertEquals(whoHasin123, whb.getWhoHas(IN_123));
        Set<String> empty= whb.getWhoHas("unexisting_url_fajsdfkjas");
        assertTrue(empty.isEmpty());
    }


}
