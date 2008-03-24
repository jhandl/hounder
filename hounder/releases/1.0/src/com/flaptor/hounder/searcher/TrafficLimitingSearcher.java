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
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;

import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.sort.ASort;

/**
 * This class limits the number of simultaneous queries 
 * and manages the waiting queue size according to throughput 
 * estimates.
 * 
 * @author Martin Massera
 */
public class TrafficLimitingSearcher implements ISearcher {
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());

    private int maxSimultaneousQueries;
    private int maxTimeInQueue;
    private int maxQueueSize; 
    private Queue<QueryInQueue> queue = new LinkedList<QueryInQueue>();
    private Integer queriesInProgress = new Integer(0);
    
    private int queueThreshold = 5; // if the threshold is exceeded start calculating the throughput
    private int queryCount = 0; // for counting and making statistics 
    
    final private ISearcher baseSearcher;

    
    /**
     * Creates a TrafficLimitingSearcher
     *
     * @param baseSearcher the base searcher that executes the searches
     * @param maxSimultaneousQueries maximum number of simultaneous queries
     * @param maxTimeInQueue maximum time of a query spent in the queue (in ms) 
     * (NOTE: enforcement is on queuing based on throuhgput estimate)
     */
    public TrafficLimitingSearcher(ISearcher baseSearcher, int maxSimultaneousQueries, int maxTimeInQueue) {
        if (null == baseSearcher) {
            throw new IllegalArgumentException("baseSearcher cannot be null.");
        }
        this.baseSearcher = baseSearcher;
        this.maxSimultaneousQueries = maxSimultaneousQueries;
        this.maxTimeInQueue = maxTimeInQueue;
        maxQueueSize = queueThreshold + maxSimultaneousQueries * 2;

        new Timer().schedule(new QueueSizeCalculator(), 0, 1000);
    }

    public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup group, int groupSize, AFilter filter, ASort sort)  throws SearcherException{
        
        QueryParams qparams = new QueryParams(query, firstResult, count, group, groupSize, filter, sort);
        
        boolean doQuery = false;
        synchronized (queriesInProgress) { //see if there is place to do the query
            if (queriesInProgress < maxSimultaneousQueries) {
                queriesInProgress = queriesInProgress + 1;
                doQuery = true;
            }
        }
        GroupedSearchResults res = null;
        if (doQuery) { //if there is place do the query
        	res = executeSearchAndPoll(qparams);
        } else { //if there is no place for executing now
            QueryInQueue queryInQueue = new QueryInQueue(qparams);

            // if there is place in the queue, put it in the queue
            // otherwise discard it
            synchronized (queue)
            {
                if (queue.size() < maxQueueSize) {
                    queue.add(queryInQueue);
                } else {
                    logger.info("TrafficLimitingSearcher: dropping a query, the query queue reached the maximum of " + maxQueueSize + " queries");
                    // TODO There should be some way in the architecture to say why the search didnt work
                    throw new RuntimeException("The search was discarded by TrafficLimitingSearcher - there is no place in the queue");
                }
            }
            
            
            synchronized (queryInQueue) { //and wait
            	boolean exit = false;
            	while (!exit) {
	                try {
	                    queryInQueue.wait();
	                    exit = true;
	                } catch (InterruptedException t) { //this query is counted, if there is an error decrement queries in progress 
	                    logger.warn("interrupted while waiting", t);
	                }
            	}
            }
            if (queryInQueue.isExecuteNotDiscard()) {
            	res = executeSearchAndPoll(qparams);
            } else {
                /** @todo There should be some way in the architecture to say why the search didnt work */
                throw new RuntimeException("The search was discarded by TrafficLimitingSearcher - aborted query");
            }
        }
        if (null == res) throw new SearcherException("GroupedSearchResults is NULL");
        return res;
    }
    
    private GroupedSearchResults executeSearchAndPoll(QueryParams qparams)  throws SearcherException{
        try {
            GroupedSearchResults results = qparams.executeInSearcher(baseSearcher);
            queryCount++; 
            if (null == results) {
                logger.debug("returning null GroupedSearchResults");
            } else {
                logger.debug("returning good GroupedSearchResults");
            }
            return results; 
        } finally {
            synchronized (queriesInProgress)
            {
                QueryInQueue nextQuery; 
                synchronized (queue) {
                    nextQuery = queue.poll(); 
                }
                if (nextQuery != null) { //if there is a next query, dont decrement queriesInProgress
                    synchronized (nextQuery) {
                        nextQuery.notify();
                    }
                } else {
                    queriesInProgress = queriesInProgress - 1;                    
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

    private static class QueryInQueue{
        private boolean executeNotDiscard = true;
        private QueryParams qparams;
        
        public QueryInQueue(QueryParams qparams) {
            this.qparams = qparams;
        }
        public boolean isExecuteNotDiscard() {return executeNotDiscard;}
        public void setExecuteNotDiscard(boolean executeNotDiscard) {this.executeNotDiscard = executeNotDiscard;}
        public QueryParams getQparams() {return qparams;}
    }
    
    /**
     * timer task that calculates the throughput. The throughput is calculated
     * whenever there is enough traffic (the queue is over the queue threshold)
     */
    private class QueueSizeCalculator extends TimerTask {
        final private static int SAMPLES = 10;
        private Queue<Integer> queryCounts = new ArrayBlockingQueue<Integer>(SAMPLES); 
        public void run() {
            if (queue.size() > queueThreshold) { 
                if (queryCounts.size() == SAMPLES) queryCounts.poll();
                queryCounts.add(queryCount);
    
                float throughput = 0;
                for (int count : queryCounts) {
                    throughput += count;
                }
                throughput /= queryCounts.size();           
    
                maxQueueSize = queueThreshold + (int) (throughput * (maxTimeInQueue/1000.0f));
//                System.out.println(maxQueueSize + " " + throughput);
            }
            queryCount = 0;
        }
    }    
}
