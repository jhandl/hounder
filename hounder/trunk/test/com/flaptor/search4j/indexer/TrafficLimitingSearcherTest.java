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
package com.flaptor.search4j.indexer;

import java.util.Random;

import com.flaptor.search4j.searcher.GroupedSearchResults;
import com.flaptor.search4j.searcher.ISearcher;
import com.flaptor.search4j.searcher.TrafficLimitingSearcher;
import com.flaptor.search4j.searcher.filter.AFilter;
import com.flaptor.search4j.searcher.group.AGroup;
import com.flaptor.search4j.searcher.query.AQuery;
import com.flaptor.search4j.searcher.sort.ASort;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;
import com.flaptor.util.ThreadUtil;

/**
 * @author Flaptor Development Team
 */
public class TrafficLimitingSearcherTest extends TestCase {
       
    private static class WaitingSearcher implements ISearcher {
        public boolean random = false;
        public int queriesInProgress = 0;
        public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup group, int groupSize, AFilter filter, ASort sort) {
            synchronized(this) {queriesInProgress++;}
            ThreadUtil.sleep(random ? new Random().nextInt(1000) : 1000);
            synchronized(this) {queriesInProgress--;return new GroupedSearchResults();}
        }
    }

    private int queriesDone = 0;
    
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testMultithreadedFixed() throws InterruptedException {
        multithreaded(false);
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testMultithreadedRandom() throws InterruptedException {
        multithreaded(true);        
    }
    
    private void multithreaded(boolean random) throws InterruptedException {
    
        int NUM_THREADS = 1000;
        WaitingSearcher baseSearcher = new WaitingSearcher();
        final TrafficLimitingSearcher searcher = new TrafficLimitingSearcher(baseSearcher, 10, 1000);
        baseSearcher.random = random;
        for (int i =0; i < NUM_THREADS; ++i) {
            try {Thread.sleep(10);} catch (InterruptedException e) {}
            new Thread() {
                public void run() {
//                    long t0 = System.currentTimeMillis();
                    try {
                        searcher.search(null, 0, 1, null, 1, null, null);
                    } catch (Throwable t) {
                        assertTrue(t.getMessage().contains("TrafficLimitingSearcher"));
                    } finally {
                        queriesDone++;
                    }
//                    long tf = System.currentTimeMillis();
//                    System.out.println(tf - t0);
                }
            }.start();
        }

        while (queriesDone < NUM_THREADS) {
            assertTrue(baseSearcher.queriesInProgress <= searcher.getMaxSimultaneousQueries());
            if (baseSearcher.queriesInProgress > searcher.getMaxSimultaneousQueries()){
                fail(baseSearcher.queriesInProgress + " queries, should be <=" + searcher.getMaxSimultaneousQueries());
            }
            Thread.sleep(50);
        }
    }
}
