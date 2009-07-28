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

import java.util.Random;

import com.flaptor.hounder.searcher.GroupedSearchResults;
import com.flaptor.hounder.searcher.ISearcher;
import com.flaptor.hounder.searcher.SearcherException;
import com.flaptor.hounder.searcher.TrafficLimitingSearcher;
import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;
import com.flaptor.util.ThreadUtil;

/**
 * @author Flaptor Development Team
 */
public class TrafficLimitingSearcherTest extends TestCase {

    private static class WaitingSearcher implements ISearcher {
        public boolean random;
        private int waitingTime;
        public int queriesInProgress = 0;
        private volatile boolean isRunning = true;

        public WaitingSearcher() {
            this(1000, false);
        }

        public WaitingSearcher(int waitingTime,boolean random){
            this.waitingTime=  waitingTime;
            this.random= random;
        }

        @Override
        public void requestStop() {
            isRunning = false;
        }

        @Override
        public boolean isStopped() {
            return !isRunning;
        }

        public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup group, int groupSize, AFilter filter, ASort sort) {
            synchronized(this) {queriesInProgress++;}
            ThreadUtil.sleep(random ? new Random().nextInt(waitingTime) : waitingTime);
            synchronized(this) {queriesInProgress--;return new GroupedSearchResults();}
        }
    }

    private static class AlwaysFailSearcher implements ISearcher {
        private volatile boolean isRunning = true;
        public AlwaysFailSearcher(){
        };


        @Override
        public void requestStop() {
            isRunning = false;
        }

        @Override
        public boolean isStopped() {
            return !isRunning;
        }

        public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup group, int groupSize, AFilter filter, ASort sort) throws SearcherException {
            throw new SearcherException("I always fail");
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

    static int lateDropQueries=0;
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testLateDrop() throws InterruptedException {
        WaitingSearcher baseSearcher = new WaitingSearcher(300, false);
        final TrafficLimitingSearcher searcher = new TrafficLimitingSearcher(baseSearcher, 1, 100);
        final int NUM_THREADS=5;
        lateDropQueries=0;
        for (int i =0; i < NUM_THREADS; ++i) {
            try {Thread.sleep(10);} catch (InterruptedException e) {}
            new Thread() {
                public void run() {
//                  long t0 = System.currentTimeMillis();
                    try {
                        searcher.search(null, 0, 1, null, 1, null, null);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        assertTrue(t.getMessage().contains("lateDrop"));
                        lateDropQueries++;
                    } finally {
                        queriesDone++;
                    }
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
        assertEquals(NUM_THREADS -1 , lateDropQueries);
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
//                  long t0 = System.currentTimeMillis();
                    try {
                        searcher.search(null, 0, 1, null, 1, null, null);
                    } catch (Throwable t) {
                        assertTrue(t.getMessage().contains("TrafficLimitingSearcher"));
                    } finally {
                        queriesDone++;
                    }
//                  long tf = System.currentTimeMillis();
//                  System.out.println(tf - t0);
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





    boolean semaphorePassed = false;
    /**
     * Tests that queries that throw an Exception on baseSearcher release their semaphore.
     */
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testReleaseSemaphoreOnBaseException() {
        // how many simultaneous queries to support
        int slots = 5;
        AlwaysFailSearcher baseSearcher = new AlwaysFailSearcher();
        final TrafficLimitingSearcher searcher = new TrafficLimitingSearcher(baseSearcher,slots,1/*no timeout*/);
        for (int i = 0; i < slots +1 ; i++){
            try {
                searcher.search(null, 0, 1, null, 1, null, null);
                fail(); // should not happen, as AlwaysFailSearcher will throw a SearcherException
            }  catch (SearcherException se){
                // do nothing .. 
            } 
        }

        // now, try with another query. If the slots were not released, it should fail with TrafficLimitingSearcher Exception.
        // if it failed with "I always fail", it is ok.
        Thread searchThread  = new Thread() {
            public void run() {
                try {
                    searcher.search(null, 0, 1, null, 1, null, null);
                } catch (Throwable t) {
                    assertFalse(t.getMessage().contains("TrafficLimitingSearcher"));
                    assertTrue(t.getMessage().contains("I always fail"));
                    semaphorePassed = true;
                }
            }
        };
        searchThread.setDaemon(true);
        searchThread.start();

        com.flaptor.util.Execute.sleep(5000);
        if (!semaphorePassed) {
            fail("query got stuck waiting for semaphore");
        }
    }


}
