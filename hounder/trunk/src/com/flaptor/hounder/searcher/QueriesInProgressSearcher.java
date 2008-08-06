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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.Statistics;

/**
 * This class keeps track of the queries in progress, and avoids sending the
 * same query more than once.
 * Once a query is running, all queries with the same arguments are not send
 * to the base searcher, but they are internally stored until the result of
 * the first one returns.
 *
 * 
 * @author Martin Massera, Spike.
 */
public class QueriesInProgressSearcher implements ISearcher {
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());

    private Statistics stats = Statistics.getStatistics();

    final private ISearcher baseSearcher;
    Map<QueryParams, QueryResults> inProgress = new HashMap<QueryParams, QueryResults>();

    public QueriesInProgressSearcher(ISearcher baseSearcher) {
        if (null == baseSearcher) {
            throw new IllegalArgumentException("baseSearcher cannot be null.");
        }
        this.baseSearcher = baseSearcher;
    }

    public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup group, int groupSize, AFilter filter, ASort sort)  throws SearcherException{
        QueryParams params = new QueryParams(query, firstResult, count, group, groupSize, filter, sort);
        boolean execute;
        QueryResults results;
        synchronized (inProgress) {
            if (!inProgress.containsKey(params)) {
                inProgress.put(params, new QueryResults());
                execute = true;
            } else {
                execute = false;
            }
            results = inProgress.get(params);
        }

        GroupedSearchResults retvalue = null;
        if (execute) {
            stats.notifyEventValue("mergedQueries", 0);
            SearcherException searcherException = null;
        	RuntimeException runtimeException = null;
            try {
                retvalue = baseSearcher.search(query, firstResult, count, group, groupSize, filter, sort);
            } catch (SearcherException e) {
                searcherException = e;
            } catch (RuntimeException e) {
                runtimeException = e;
            }

            synchronized (inProgress) {
                inProgress.remove(params);
            }
            synchronized (results) {
                results.setResults(retvalue, searcherException, runtimeException);
                results.notifyAll();
            }
        } else {
            stats.notifyEventValue("mergedQueries", 1);
            synchronized (results) {
            	while (!results.isValid()) {
                    try {
                        results.wait();
                    } catch (InterruptedException e) {
                        logger.error("Interrupted while sleeping", e);
                    }
                }
            }
        }
    	if (results.getResults() != null) return results.getResults(); 
        else {
        	if (results.getSearcherException() != null) throw results.getSearcherException();
        	else throw results.getRuntimeException();
        }
    }

    private static class QueryResults {
        private GroupedSearchResults results = null;
        private SearcherException searcherException = null;
        private RuntimeException runtimeException = null;
        private boolean valid = false;
      
        public GroupedSearchResults getResults() {
            return results;
        }
        
        public SearcherException getSearcherException() {
            return searcherException;
        }

        public RuntimeException getRuntimeException() {
            return runtimeException;
        }

        public boolean isValid() {
            return valid;
        }

        public void setResults(GroupedSearchResults results, SearcherException searcherException, RuntimeException runtimeException) {
            this.results = results;
            this.searcherException = searcherException;
            this.runtimeException = runtimeException;
            valid = true;
        }
    }
}
