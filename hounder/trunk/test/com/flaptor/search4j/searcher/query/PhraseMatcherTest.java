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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;
import com.flaptor.util.TestUtils;

/**
 * @author Flaptor Development Team
 */
public class PhraseMatcherTest extends TestCase {
    PhraseMatcher pm = new PhraseMatcher();
    File dir ;

    public void setUp() throws IOException {
        dir = FileUtil.createTempDir("phraseMatcherTest", ".tmp");
    }
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void tearDown() {
        FileUtil.deleteFile(dir);
    }
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testBigFile() {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < 10000; ++i)
            list.add(TestUtils.randomText(4, 20).trim());
        pm.construct(list);
        for (int i = 0; i < 500; ++i) {
            Random rand = new Random();
            int subphrases = rand.nextInt(50);
            String query = "";
            String result = "";
            for (int j = 0; j < subphrases; ++j) {
                if (rand.nextInt(2) == 0) {
                    String randText = TestUtils.randomText(1, 20).trim();
                    query += " " + randText;
                    result += " " + randText;
                }
                String phrase = list.get(rand.nextInt(list.size()));
                query += " " + phrase;
                result += " \"" + phrase + "\"";
            }
            result = result.trim();
            query = query.trim();
            assertTrue(pm.recognize(query).equalsIgnoreCase(result));
        }
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testTao() throws IOException {
        String filePath = dir.getAbsolutePath() + File.separator + "taoteching.txt";
        TestUtils.writeFile(filePath, taoTeChing);
        pm.construct(new File(filePath));
        assertEquals("", pm.recognize(""));
        assertEquals(
                "I once met an old man who \"conceived of as having no name it is the originator of heaven\"",
                pm.recognize("I once met an old man who conceived of as having no name it is the originator of heaven"));
        assertEquals(
                "\"The Tao that can be trodden is not the enduring and\"",
                pm.recognize("The Tao that can be trodden is not the enduring and"));
        assertEquals(
                "desire always", 
                pm.recognize("desire always"));
        assertEquals(
                "But if desire us be",
                pm.recognize("But if desire us be"));
        assertEquals(
                "But if But if desire But if desire always within But if desire us be",
                pm.recognize("But if But if desire But if desire always within But if desire us be"));
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testList() {
        List<String> list = Arrays.asList(new String[] { 
            "yo soy marto",
            "voy a la casa de mono",
            "la casa",
            "soy de river plate"
        });
        pm.construct(list);

        assertEquals(
                "\"yo soy maRtO\" y voy a \"la CASA\" de pedro y \"soy de river plate\" desde chico",
                pm.recognize("yo soy maRtO y voy a la CASA de pedro y soy de river plate desde chico"));
        assertEquals(
                "voy a \"la casa\" \"la casa\" de voy a lo de mono voy a \"la casa\" de yo soy de soy de river yo soy de plate",
                pm.recognize("voy a la casa la casa de voy a lo de mono voy a la casa de yo soy de soy de river yo soy de plate"));
        assertEquals(
                "voy a la voy a la voy a casa de soy de la de river",
                pm.recognize("voy a la voy a la voy a casa de soy de la de river"));
    }

    String taoTeChing = "PART 1 \n\n\n\n\nThe Tao that can be trodden is not the enduring and\nunchanging Tao The name that can be named is not the enduring and\nunchanging name \n\n Conceived of as having no name it is the Originator of heaven\nand earth conceived of as having a name it is the Mother of all\nthings \n\nAlways without desire we must be found \n If its deep mystery we would sound \n But if desire always within us be \n Its outer fringe is all that we shall see \n\nUnder these two aspects it is really the same but as development\ntakes place it receives the different names Together we call them\nthe Mystery Where the Mystery is the deepest is the gate of all that\nis subtle and wonderful \n\n\nAll in the world know the beauty of the beautiful and in doing\nthis they have the idea of what ugliness is they all know the skill\nof the skilful and in doing this they have the idea of what the\nwant of skill is \n\nSo it is that existence and non-existence give birth the one to\n the idea of the other that difficulty and ease produce the one the\n";
}
