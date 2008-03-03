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
package com.flaptor.search4j.searcher.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.flaptor.search4j.searcher.GroupedSearchResults;
import com.flaptor.search4j.searcher.sort.ASort;
import com.flaptor.search4j.searcher.sort.FieldSort;
import com.flaptor.util.Pair;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * @author Flaptor Development Team
 */
public class GroupedSearchResultsDocumentProviderTest extends TestCase {


    // As there was a bug of IndexOutOfBoundsException when every GroupedSearchResults
    // on the list for the GroupedSearchResultsDocumentProvider constructor was empty,
    // this test checks that that bug is not present anymore
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testEmptyResults() throws Exception {
        GroupedSearchResults gsr1 = new GroupedSearchResults();
        GroupedSearchResults gsr2 = new GroupedSearchResults();

        List<GroupedSearchResults> list = new ArrayList<GroupedSearchResults>();
        list.add(gsr1);
        list.add(gsr2);

        GroupedSearchResultsDocumentProvider provider = new GroupedSearchResultsDocumentProvider(list,new com.flaptor.search4j.searcher.sort.ScoreSort());

        assertEquals(provider.length(),0);
        assertEquals(provider.totalHits(),0);
        assertEquals(provider.getMaxScore(),0f);
        
        

        try {
            provider.getDocument(1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            assertTrue(true);
        }
        
        try {
            provider.getScore(1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            assertTrue(true);
        }

    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testSortDocs() throws Exception {

        ASort sort = new FieldSort(false,"int",FieldSort.OrderType.INT);
        Vector<Pair<String,Vector<Document>>> docs = new Vector<Pair<String,Vector<Document>>>();
        Vector<Vector<Float>> scores = new Vector<Vector<Float>>();
        Vector<GroupedSearchResults> gsrs = new Vector<GroupedSearchResults>();

        for (int j = 0; j < 3 ; j++) {
            docs = new Vector<Pair<String,Vector<Document>>>();
            scores = new Vector<Vector<Float>>();
            for (int i = 0; i < 10; i++) {
                Document doc = new Document();
                Field field = new Field("int",String.valueOf(10*j + i),Field.Store.YES,Field.Index.TOKENIZED);
                doc.add(field);
                Vector<Document> group = new Vector<Document>();
                group.add(doc);
                docs.add(new Pair<String,Vector<Document>>(String.valueOf(10*j+i),group));
                Vector<Float> score = new Vector<Float>();
                score.add(1f);
                scores.add(score);
            }
            GroupedSearchResults gsr = new GroupedSearchResults(docs,10,0,10,scores);
            gsrs.add(gsr);
        }

        java.util.Collections.shuffle(gsrs);
        GroupedSearchResultsDocumentProvider provider = new GroupedSearchResultsDocumentProvider(gsrs,sort);

        int min = -1;
        for (int i = 0; i < provider.length(); i++) {
            int found = Integer.parseInt(provider.getDocument(i).get("int"));
            assertTrue("found: " + found + " - min: "+min, found > min);
            min = found;
        }

    }


}
