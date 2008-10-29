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
package com.flaptor.hounder.searcher;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.Statistics;

/**
 * This class limits the number of simultaneous queries
 * and manages the waiting queue size according to throughput
 * estimates.
 *
 * The waiting time in queue is enforced using 2 different algorithms:
 * <dl>
 *  <dt> Early drop </dt>
 *      <dd>Calculates the throughput, and based on that calculates how many queries
 *      can be answered before <code>maxTimeInQueue</code>. It then drops from the
 *      queue all the queries that will not be answered</dd>
 *  <dt> Late drop</dt>
 *      <dd>Right after the query arrives, we log the arriving time. Right
 *      before executing the query, we check how long the query was in this queue.
 *      If it was more than <code>maxTimeInQueue</code> it will be discarded. </dd>
 *  </dl>
 * @author Martin Massera, Spike
 * @author rafa
 */
public class TrafficLimitingSearcher implements ISearcher {
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());

    private final Statistics stats = Statistics.getStatistics();
    final private ISearcher baseSearcher;

    private final int maxTimeInQueue;
    private final int maxSimultaneousQueries;
    private volatile int maxQueueSize;

    private int queriesInProgress = 0;

    private static final int QUEUE_THRESHOLD = 5; // if the threshold is exceeded start calculating the throughput

    private int queryCount = 0; // for counting and making statistics
    private final Object queryCountMutex = new Object();


    private final Semaphore sem;
    private AtomicInteger queueSize= new AtomicInteger(0);

    /**
     * Creates a TrafficLimitingSearcher
     *
     * @param baseSearcher the base searcher that executes the searches
     * @param maxSimultaneousQueries maximum number of simultaneous queries
     * @param maxTimeInQueue maximum time of a query spent in the queue.
     * The restriction is enforced twice:
     * <ol>
     *   <li>  The time is checked when the query arrives, by calculating the
     * time will pass in the queue based on throuhgput estimation. </li>
     *   <li> The time is checked right before executing the query.</li>
     * </ol>
     */
    public TrafficLimitingSearcher(ISearcher baseSearcher, int maxSimultaneousQueries,
            int maxTimeInQueue) {
        if (null == baseSearcher) {
            throw new IllegalArgumentException("baseSearcher cannot be null.");
        }
        this.baseSearcher = baseSearcher;
        this.maxSimultaneousQueries = maxSimultaneousQueries;
        this.maxQueueSize = QUEUE_THRESHOLD + maxSimultaneousQueries * 2;
        this.sem= new Semaphore(maxSimultaneousQueries, true);
        this.maxTimeInQueue = maxTimeInQueue;
        new Timer().schedule(new QueueSizeCalculator(), 0, 1000);
    }

    public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup group, int groupSize, AFilter filter, ASort sort)  throws SearcherException{
        long enqueuedTime= System.currentTimeMillis();
        int qs= queueSize.incrementAndGet();
        if (qs > maxQueueSize){  // if there is not enough space in queue, drop it
            queueSize.decrementAndGet();
            logger.info("TrafficLimitingSearcher: dropping a query, the query queue reached " +
                    "the maximum of " + maxQueueSize + " queries." );
            //TODO:change the SearchTimeoutException class to reflect the reality of an early drop.
            throw new SearchTimeoutException(-1 ,"The search was discarded by TrafficLimitingSearcher - " +
            		"there is no place in the queue (earlyDrop)");
        }

        try { // Wait here, till one of the maxSimultaneousQueries permissions is available
            sem.acquire();
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting in q", e);
            throw new SearcherException("The search was discarded by TrafficLimitingSearcher - interrupted while waiting in queue");
        } finally{
            queueSize.decrementAndGet();
        }

        stats.notifyEventValue("queriesInProgress", maxSimultaneousQueries - sem.availablePermits());

        long now = System.currentTimeMillis();
        if (now - enqueuedTime > maxTimeInQueue) {
            sem.release();
            throw new SearchTimeoutException(maxTimeInQueue ,"The search was discarded by TrafficLimitingSearcher - " +
            		"the query was too much time on queue (lateDrop). " +
                    "maxTimeInQ: " + maxTimeInQueue+", in: " +  enqueuedTime +
                    ", now: " + now + " --> " + (now - enqueuedTime));
        }
        synchronized (queryCountMutex) {
            queryCount++;
        }
        try {
            GroupedSearchResults results = baseSearcher.search(query, firstResult, count, group, groupSize, filter, sort);
            return results;
        } finally {
            sem.release();
        }

    }

    /**
     * timer task that calculates the throughput. The throughput is calculated
     * whenever there is enough traffic (the queue is over the queue threshold)
     */
    private class QueueSizeCalculator extends TimerTask {
        final private static int SAMPLES = 10;
        private Queue<Integer> queryCounts = new LinkedList<Integer>();
        private int totalSum=0;
        private long lastRun= System.currentTimeMillis();
        public void run() {
            if (queueSize.get() > QUEUE_THRESHOLD) {
                while (queryCounts.size() >= SAMPLES){
                    Integer val = queryCounts.remove();
                    totalSum -= val.intValue();
                }
                float windowTime;
                int qC;
                synchronized (queryCountMutex) {
                    windowTime= System.currentTimeMillis() - lastRun;
                    qC= queryCount;
                    queryCount = 0;
                    lastRun= System.currentTimeMillis();
                }
                totalSum += qC;
                queryCounts.add(qC);

                float throughput = 0; // how many queries we served
                throughput = totalSum /  queryCounts.size();

                maxQueueSize = Math.max(QUEUE_THRESHOLD +1, (int) ( maxTimeInQueue * (throughput/windowTime)));
            } else {
                synchronized (queryCountMutex) {
                    queryCount = 0;
                    lastRun= System.currentTimeMillis();
                }
            }
        }
    }


    /**
     * @return the maximum number of simultaneous queries
     */
    public int getMaxSimultaneousQueries() {
        return maxSimultaneousQueries;
    }

    /**
     * @return the number of queries in progress
     */
    public int getSimultaneousQueries() {
        return queriesInProgress;
    }

    @Override
    public void requestStop() {
        baseSearcher.requestStop();
    }

    @Override
    public boolean isStopped() {
        return baseSearcher.isStopped();
    }
}
