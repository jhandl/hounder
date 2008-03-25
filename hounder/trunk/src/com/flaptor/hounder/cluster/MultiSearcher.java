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
package com.flaptor.hounder.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.flaptor.hounder.searcher.GroupedSearchResults;
import com.flaptor.hounder.searcher.IRemoteSearcher;
import com.flaptor.hounder.searcher.ISearcher;
import com.flaptor.hounder.searcher.QueryParams;
import com.flaptor.hounder.searcher.RmiSearcherStub;
import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.group.AResultsGrouper;
import com.flaptor.hounder.searcher.group.GroupedSearchResultsDocumentProvider;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.CallableWithId;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.Execution;
import com.flaptor.util.MultiExecutor;
import com.flaptor.util.Pair;
import com.flaptor.util.PortUtil;
import com.flaptor.util.Statistics;
import com.flaptor.util.Execution.Results;

/**
 * This class implements a server that sends a query to a number of searchers
 * (possibly remote), waits for the results and merges them.
 * 
 * @author Martin Massera
 */
public class MultiSearcher implements ISearcher {

    private static Logger logger = Logger.getLogger(Execute.whoAmI());

    private List<IRemoteSearcher> searchers = new ArrayList<IRemoteSearcher>();
    private MultiExecutor<GroupedSearchResults> multiQueryExecutor;
    private List<String> searcherIPs = new ArrayList<String>();
    private long timeout;
    
    public MultiSearcher() {
        Config config = Config.getConfig("multiSearcher.properties");
        String[] hosts = config.getStringArray("multiSearcher.hosts");
        
        for (int i = 0; i < hosts.length; i++) {
            Pair<String, Integer> host = PortUtil.parseHost(hosts[i]);
            searchers.add(new RmiSearcherStub(host.last(), host.first()));
            searcherIPs.add(host.first());
        }
        timeout = config.getLong("multiSearcher.timeout");
        multiQueryExecutor = new MultiExecutor<GroupedSearchResults>(config.getInt("multiSearcher.workerThreads"), "multiSearcher");
    }

    /**
     * Runs the query against all searchers and returns a Vector representing the result set.
     *
     * To resolve the query, each searcher is asked for firstResult + count results.
     * Then, all the results are "merged" this is "real" list of results.
     * With this list, the first <i>firstResult</i> are discarded, and the <i>count</i>
     * results on the head are returned.
     *
     */
    public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup group, int groupSize, AFilter filter, ASort sort) {

        final QueryParams queryParams = new QueryParams(query, 0, firstResult + count, group, 1, filter, sort);
        
        Execution<GroupedSearchResults> execution= new Execution<GroupedSearchResults>();
        for (int i = 0; i < searchers.size(); ++i) {
        	final int numSearcher = i;
        	final IRemoteSearcher searcher = searchers.get(numSearcher);
            execution.getTaskQueue().add(new CallableWithId<GroupedSearchResults, Integer>(numSearcher) {
                public GroupedSearchResults call() throws Exception {
                    return queryParams.executeInRemoteSearcher(searcher);
                }
            });
        }
        multiQueryExecutor.addExecution(execution);

        long start = System.currentTimeMillis();
        long now = start;
        while(true) {
            synchronized(execution) {
                if (execution.getResultsList().size() == searchers.size()) break;
                
                now = System.currentTimeMillis();
                long toWait = timeout - (now - start);
                if (toWait > 0) {
                    try {
                        execution.wait(toWait);
                    } catch (InterruptedException e) {
                        logger.warn("multiqueryExecutor: interrupted while waiting for the responses to return. I will continue...");
                    }
                } else {
                    logger.warn("timeout of some searchers");
                    break;
                }
            }
        }
        
        //a treeMap for sorting values according to the searcher number
        Map<Integer, GroupedSearchResults> goodResultsMap = new TreeMap<Integer, GroupedSearchResults>();
        List<GroupedSearchResults> goodResults = new ArrayList<GroupedSearchResults>();
        //List<Results<GroupedSearchResults>> badResults = new ArrayList<Results<GroupedSearchResults>>();
        int badResults = 0;
        
        int totalDocuments = 0;
        //we take a snapshot of the results
        //other results may come after the timeout, a change in the size of the result set could cause problems
        synchronized(execution) {
            execution.forget();
            for (Results<GroupedSearchResults> result : execution.getResultsList()) {
                int numSearcher = ((CallableWithId<GroupedSearchResults, Integer>)result.getTask()).getId();
                if (result.isFinishedOk()) {
                	GroupedSearchResults gsr = result.getResults();
                	goodResultsMap.put(numSearcher, gsr);
                    totalDocuments += gsr.totalGroupsEstimation();
                    // gather stats from the uni-searchers
                	Statistics.getStatistics().notifyEventValue("averageTimes_"+searcherIPs.get(numSearcher), gsr.getResponseTime());
                } else {
                    badResults++;
                    logger.warn("Exception from remote searcher " +  numSearcher, result.getException());
                }
            }
        }
        
		
        //move (sorted) entries to a list
        for (Map.Entry<Integer, GroupedSearchResults> entry : goodResultsMap.entrySet()) {
        	goodResults.add(entry.getValue());
        }
        int resultsSize = goodResults.size(); 
        logger.debug("obtained " + totalDocuments + " documents in "+ resultsSize + " good responses and " +  badResults + " exceptions in " + (now - start) + " ms ");

        if (goodResults.size() == 0) {
            logger.warn("No good results - " + badResults + " exceptions");
            return new GroupedSearchResults();
        }

        //done collecting results, either because we have results from
        //all the searchers or because we timed out
        //now we generate a result vector with the top results of each set

        AResultsGrouper grouper = group.getGrouper(new GroupedSearchResultsDocumentProvider(goodResults,sort));
        return grouper.group(count,groupSize,firstResult);
    }
    
}
