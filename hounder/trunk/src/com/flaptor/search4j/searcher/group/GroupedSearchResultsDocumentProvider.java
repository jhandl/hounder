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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.document.Document;

import com.flaptor.search4j.searcher.GroupedSearchResults;
import com.flaptor.search4j.searcher.sort.ASort;
import com.flaptor.search4j.searcher.sort.FieldSort;
import com.flaptor.util.CollectionsUtil;
import com.flaptor.util.Pair;

/**
 * @author Flaptor Development Team
 */
public class GroupedSearchResultsDocumentProvider implements DocumentProvider {

    private static Comparator<Pair<Document,Float>> COMPARATOR = new DocumentPairComparator();

    private final List<GroupedSearchResults> gsr;
    private final List<Pair<Document,Float>> plainDocs;
    private final int totalHits;
    private final ASort sort;




    public GroupedSearchResultsDocumentProvider (List<GroupedSearchResults> gsr, ASort sort){
        this.gsr = gsr;
        this.sort = sort;
        int countedHits = 0;
        List<List<Pair<Document,Float>>> lists = new ArrayList<List<Pair<Document,Float>>>(gsr.size());
        List<Pair<Document,Float>> overflowList = new ArrayList<Pair<Document,Float>>();
        for (int i = 0; i < gsr.size() ; i++) { 
            List<Pair<Document,Float>> list = new ArrayList<Pair<Document,Float>>();
            for (int j = 0; j < gsr.get(i).groups(); j++) {
                List<Document> docList = gsr.get(i).getGroup(j).last();
                List<Float> scoreList = gsr.get(i).getGroupScore(j);
                if (0 == docList.size()) continue;

                list.add(new Pair<Document,Float>(docList.get(0),scoreList.get(0)));
                // push second results from groups into overflowList
                for (int k = 1; k < docList.size(); k++) {
                    overflowList.add(new Pair<Document,Float>(docList.get(k),scoreList.get(k)));
                }
            }
            countedHits += gsr.get(i).totalGroupsEstimation();
            lists.add(list);
        }
        this.totalHits = countedHits;



        Comparator<Pair<Document,Float>> comparator = null;
        // only fieldsort performs a good getComparator
        if (sort instanceof FieldSort) {
            comparator = new DocumentComparatorWrapper(sort.getComparator());
        } else {
            comparator = COMPARATOR;
        }
            
        // sort overflowList
        Collections.sort(overflowList,Collections.reverseOrder(comparator));
        lists.add(overflowList);

        // merge into plainDocs
        plainDocs = CollectionsUtil.mergeLists(lists,Collections.reverseOrder(comparator));
       
    }

    public int length() {
        return this.plainDocs.size();
    }


    public int totalHits() {
        return this.totalHits;
    }


    public Document getDocument(int i) {
        return this.plainDocs.get(i).first();
    }
    public float getScore(int i) {
        return this.plainDocs.get(i).last();
    }

    public float getMaxScore(){
        if (0 == plainDocs.size() ) {
            return 0;
        }
        return this.plainDocs.get(0).last();
    }
    
    
    
    private static class DocumentPairComparator implements Comparator<Pair<Document,Float>> {
    
        public int compare(Pair<Document,Float> p1, Pair<Document,Float> p2) {
            if (null == p1 && null == p2) return 0;
            if (null == p1 ) return 1;
            if (null == p2 ) return -1;
            return p1.last().compareTo(p2.last());
        }
    }

    // This class is useful to compare when FieldSort is used.
    private static class DocumentComparatorWrapper implements Comparator<Pair<Document,Float>> {

        private final Comparator<Document> comparator;

        private DocumentComparatorWrapper(Comparator<Document> comparator) {
            this.comparator = comparator;
        }

        public int compare(Pair<Document,Float> p1, Pair<Document,Float> p2) {
            if (null == p1 && null == p2) return 0;
            if (null == p1 ) return 1;
            if (null == p2 ) return -1;
            return comparator.compare(p1.first(),p2.first());
        }
    }

}
