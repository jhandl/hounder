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
package com.flaptor.hounder.indexer;

import java.io.File;

import org.apache.log4j.Logger;
import org.dom4j.Document;

import com.flaptor.util.Config;
import com.flaptor.util.DocumentParser;
import com.flaptor.util.Execute;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class HtmlParserTest extends TestCase {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    HtmlParser parser;
    
    @Override
    public void setUp() {
        Config indexerConfig = Config.getConfig("indexer.properties");
        indexerConfig.set("HtmlParser.inputTagNames","body");
        indexerConfig.set("HtmlParser.outputFieldNames","text");
        indexerConfig.set("HtmlParser.removedXPath","");
        indexerConfig.set("HtmlParser.separatorTags","");
        indexerConfig.set("HtmlParser.stored","true");
        indexerConfig.set("HtmlParser.indexed","true");
        parser = new HtmlParser();
    }

    @Override
    public void tearDown() {
    }
    
    @TestInfo (testType = TestInfo.TestType.UNIT)
    public void testDontDie() {

        // The parser throws an exception, caused by an stack overflow.
        // ignore it.
        filterOutput("processTag:null");
        File docFile = new File("test/com/flaptor/hounder/indexer/killerDoc.xml");
        
        Config conf = Config.getConfig("indexer.properties");
        String[] inputTagNames = conf.getStringArray("HtmlParser.inputTagNames");
        boolean flag= false;
        for (String s: inputTagNames){
            if ("body".equals(s)){
                flag=true;
                break;
            }
        }
        assertTrue("The killerDoc contains a tag 'body', but 'body' is not in " +
        		"the list of tags to parse (see HtmlParser.inputTagNames on indexer.properties)", flag);
        
        StringBuffer buf = new StringBuffer();
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(docFile));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                buf.append(line + " ");
            }
        }
        catch (Exception e) {
            logger.error("testDontDie:", e);
            fail(e.toString());
        }
                
        Document doc = new DocumentParser().genDocument(buf.toString());
        parser.process(doc);
    }
}
