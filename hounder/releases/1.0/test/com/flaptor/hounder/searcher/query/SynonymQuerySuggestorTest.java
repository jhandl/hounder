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
import java.util.Vector;

import org.apache.lucene.document.Document;

import com.flaptor.hounder.searcher.GroupedSearchResults;
import com.flaptor.hounder.searcher.ISearcher;
import com.flaptor.hounder.searcher.SuggestQuerySearcher;
import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.group.NoGroup;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.Config;
import com.flaptor.util.FileUtil;
import com.flaptor.util.Pair;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;
import com.flaptor.util.TestUtils;

/**
 * @author Flaptor Development Team
 */
public class SynonymQuerySuggestorTest extends TestCase {

    private File dir = FileUtil.createTempDir("SynonymQuerySuggestorTest", ".tmp");
    private ISearcher baseSearcher;
    private ISearcher suggestSearcher;
    private final float factor = 0.5f;
    
    @Override
    protected void setUp() throws Exception {
        String synonymFile = dir.getAbsolutePath()+File.separator+"synonyms.txt";
        TestUtils.writeFile(synonymFile,"foo=house,bar\nfoo1=bar1\nfoos=bar,bar1,bar2\ndont=cant,wont");

        Config config = Config.getConfig("searcher.properties");
        config.set("QueryParser.searchFields", "content");
        config.set("QueryParser.searchFieldWeights", "1.0f");
        config.set("QueryParser.synonymFile",synonymFile);

        
        baseSearcher = new SearcherStub();
        suggestSearcher = new SuggestQuerySearcher(baseSearcher,new SynonymQuerySuggestor(new File(synonymFile)),100,factor);
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        FileUtil.deleteFile(dir);
    }


    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testSuggests() throws Exception {
        AQuery query = new LazyParsedQuery("foo");
        // a foo query should return no results
        GroupedSearchResults gsr = suggestSearcher.search(query,0,10,new NoGroup(),1,null,null);
        assertTrue(gsr.getSuggestedQuery().toString().contains("bar"));
    }    

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testDoesNotSuggest() throws Exception {
        AQuery query = new LazyParsedQuery("dont give results");
        GroupedSearchResults gsr = suggestSearcher.search(query,0,10,new NoGroup(),1,null,null);
        assertTrue(null == gsr.getSuggestedQuery());
    }


    //---------------
    // PRIVATE CLASSES

    private class SearcherStub implements ISearcher {
    

        // every query, that does not contain "bar", will return empty
        public GroupedSearchResults search(AQuery query, int firstResult, int groupCount, AGroup groupBy, int groupSize,AFilter filter, ASort sort) throws com.flaptor.hounder.searcher.SearcherException {
       
            if (!query.toString().contains("bar")) {
                return new GroupedSearchResults();
            }
            // else
            Vector<Pair<String,Vector<Document>>> vec = new Vector<Pair<String,Vector<Document>>>();
            Vector<Vector<Float>> scores = new Vector<Vector<Float>>();
            for (int i = 0; i < 10; i++) {
                Vector<Document> doc = new Vector<Document>();
                doc.add(new Document());
                vec.add(new Pair<String,Vector<Document>>("",doc));
                Vector<Float> score = new Vector<Float>();
                score.add(new Float(10-i));
                scores.add(score);
            }
            return new GroupedSearchResults(vec,1000,0,11,scores);
            
        }
    }

}
