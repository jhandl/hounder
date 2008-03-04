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

import com.flaptor.hounder.searcher.GroupedSearchResults;
import com.flaptor.hounder.searcher.ISearcher;
import com.flaptor.hounder.searcher.QueriesInProgressSearcher;
import com.flaptor.hounder.searcher.SearcherException;
import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;
/**
 * @author Flaptor Development Team
 */
public class QueriesInProgressTest extends TestCase {
       
    private static class WaitingSearcher implements ISearcher {

        public int queriesInProgress = 0;
        public volatile boolean answer = false; 
        
        public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup group, int groupSize, AFilter filter, ASort sort) {
            synchronized(this) {queriesInProgress++;}
            while (!answer) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
            }
            synchronized(this) {
                queriesInProgress--;
                return new GroupedSearchResults();
            }
        }       
    }
    
    private int queriesInProgress = 0;

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testMultithreaded() throws InterruptedException{
    
        int NUM_THREADS = 100;
        WaitingSearcher baseSearcher = new WaitingSearcher();
        final ISearcher searcher = new QueriesInProgressSearcher(baseSearcher);
        
        final Object lock = new Object();
        for (int i =0; i < NUM_THREADS; ++i) {
            new Thread() {
                public void run() {
                    synchronized (lock) {queriesInProgress++;}
                    try {
						searcher.search(null, 0, 1, null, 1, null, null);
					} catch (SearcherException e) {
						e.printStackTrace();
						fail();
					}
                    synchronized (lock) {queriesInProgress--;}
                }
            }.start();
        }
        Thread.sleep(2000);
        assertEquals(NUM_THREADS, queriesInProgress);
        assertEquals(1, baseSearcher.queriesInProgress);
        baseSearcher.answer = true;
        Thread.sleep(2000);
        assertEquals(queriesInProgress, 0);
    }

}
