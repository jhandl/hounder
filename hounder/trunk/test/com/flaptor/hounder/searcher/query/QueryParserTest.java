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

import java.io.File;

import com.flaptor.util.Config;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;
import com.flaptor.util.TestUtils;

/**
 * @author Flaptor Development Team
 */
public class QueryParserTest extends TestCase {

    File dir = FileUtil.createTempDir("queryParserTest", ".tmp");

    QueryParser qp;
    
    protected void setUp() throws Exception {
        String phrasesFile = dir.getAbsolutePath()+File.separator+"phrases.txt";
        TestUtils.writeFile(phrasesFile, "yo soy marto\nvoy a la casa de mono\nla casa\nsoy de river plate\n");
        String synonymFile = dir.getAbsolutePath()+File.separator+"synonyms.txt";
        TestUtils.writeFile(synonymFile,"foo=bar\nfoo1=bar1\nfoos=bar,bar1,bar2");

        Config config = Config.getConfig("searcher.properties");
        config.set("QueryParser.searchFields", "content");
        config.set("QueryParser.searchFieldWeights", "1.0f");
        config.set("searcher.query.phrasesFile", phrasesFile);
        config.set("QueryParser.synonymFields","text");
        config.set("QueryParser.synonymFile",synonymFile);

        
        qp = new QueryParser();
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        FileUtil.deleteFile(dir);
    }

    
//commented because the method is not public
//    public void testPhraseMatcher() {
//        assertEquals(
//                qp.matchPhrases("hola yo soy marto como estas"),
//                "hola \"yo soy marto\" como estas");
//        assertEquals(
//                qp.matchPhrases("hola \"yo soy marto\" como estas"),
//                "hola \"yo soy marto\" como estas");
//        assertEquals(
//                qp.matchPhrases("\"hola yo soy marto como\" estas"),
//                "\"hola yo soy marto como\" estas");
//        assertEquals(
//                qp.matchPhrases("hola \"\"yo soy marto"),
//                "hola \"\" \"yo soy marto\"");
//        assertEquals(
//                qp.matchPhrases("hola \"yo soy marto"),
//                "hola \"yo soy marto");
//        assertEquals(
//                qp.matchPhrases("yo soy marto AND yo soy marto"),
//                "\"yo soy marto\" AND \"yo soy marto\"");
//        assertEquals(
//                qp.matchPhrases("yo soy marto,yo soy marto"),
//                "yo soy marto,yo soy marto");
//        assertEquals(
//                qp.matchPhrases("(yo soy marto)"),
//                "(yo soy marto)");
//    }
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testTermQuery() {
        assertEquals(qp.parse("foo:bar"), new TermQuery("foo", "bar").getLuceneQuery());
        assertEquals(qp.parse("bar"), new TermQuery("content", "bar").getLuceneQuery());
    }
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testOrQuery() {
        AQuery q = new OrQuery(new TermQuery("foo", "bar"), new TermQuery("foo2", "bar2"));
        String qs1 = "foo:bar OR foo2:bar2";
        String qs2 = "foo2:bar2 OR foo:bar";

        assertTrue(
                qp.parse(qs1).toString().equals(q.getLuceneQuery().toString())
                ||
                qp.parse(qs2).toString().equals(q.getLuceneQuery().toString()));
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testAndQuery() {
        AQuery q = new AndQuery(new TermQuery("foo", "bar"), new TermQuery("foo2", "bar2"));
        String qs1 = "foo:bar foo2:bar2";
        String qs2 = "foo2:bar2 foo:bar";

        assertTrue(
                qp.parse(qs1).toString().equals(q.getLuceneQuery().toString())
                ||
                qp.parse(qs2).toString().equals(q.getLuceneQuery().toString()));
    }
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testUnbalancedParenthesis() {
        boolean fail = true;
        filterOutput("lucene could not parse query: (foo,");
        try {
            qp.parse("(foo");
        } catch (IllegalArgumentException e) {
            fail = false;
        }
        unfilterOutput();
        assertFalse("parser didn't throw an exception with unbalanced parenthesis", fail);
    }
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testEqualityOfGeneratedQueries() {
        assertFalse(qp.parse("foo").equals(qp.parse("bar")));
        
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testSynonymQueries() {
        AQuery query = new LazyParsedQuery("text:foo");
        assertTrue(query.toString() + " does not contain text:bar", query.toString().matches(".*text:bar.*"));
        assertFalse(query.toString() + " contains text:bar1 and is not supposed to",query.toString().matches(".*text:bar1.*"));
        assertTrue(query.toString() + " does not contain text:foo, and it was there before the query was expanded",query.toString().matches(".*foo.*"));
        
        
        query = new LazyParsedQuery("text:foos");
        assertTrue(query.toString() + " does not contain text:bar1", query.toString().matches(".*bar1.*"));
        assertTrue(query.toString() + " does not contain text:bar2", query.toString().matches(".*bar2.*"));
        assertTrue(query.toString() + " does not contain text:bar", query.toString().matches(".*bar .*"));
        assertTrue(query.toString() + " does not contain text:foos, and it was there before the query was expanded",query.toString().matches(".*foos.*"));


        query = new LazyParsedQuery("title:foo");
        assertFalse(query.toString() + " has been expanded, but does not contain expandable fields ( title != text)", query.toString().matches(".*bar.*"));
    }

    public static void main(String[] args) throws Exception {
        QueryParserTest t = new QueryParserTest();
        t.setUp();
        t.testEqualityOfGeneratedQueries();
        t.testAndQuery();
        t.testOrQuery();
        t.testTermQuery();
        t.testUnbalancedParenthesis();
    }

}
