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
package com.flaptor.search4j.cluster;

import java.io.File;

import org.apache.log4j.Logger;

import com.flaptor.search4j.searcher.CacheSearcher;
import com.flaptor.search4j.searcher.GroupedSearchResults;
import com.flaptor.search4j.searcher.ISearcher;
import com.flaptor.search4j.searcher.QueriesInProgressSearcher;
import com.flaptor.search4j.searcher.QueryParams;
import com.flaptor.search4j.searcher.SearcherException;
import com.flaptor.search4j.searcher.SuggestQuerySearcher;
import com.flaptor.search4j.searcher.TrafficLimitingSearcher;
import com.flaptor.search4j.searcher.filter.AFilter;
import com.flaptor.search4j.searcher.group.AGroup;
import com.flaptor.search4j.searcher.query.AQuery;
import com.flaptor.search4j.searcher.query.WordQuerySuggestor;
import com.flaptor.search4j.searcher.sort.ASort;
import com.flaptor.util.Cache;
import com.flaptor.util.Config;
import com.flaptor.util.TimeoutLRUCache;

/**
 * The composition of Searchers for MultiSearcher
 * 
 * @author Martin Massera
 */
public class CompositeMultiSearcher implements ISearcher {
    
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());

    private ISearcher searcher;
    private MultiSearcher baseMultiSearcher;
    
    public CompositeMultiSearcher() {

        Config searcherConfig = Config.getConfig("multiSearcher.properties");
        baseMultiSearcher = new MultiSearcher();

        searcher = baseMultiSearcher;

        if (searcherConfig.getBoolean("compositeMultiSearcher.useTrafficLimiting")) {
            int maxSimultaneousQueries = searcherConfig.getInt("multiSearcher.trafficLimiting.maxSimultaneousQueries");
            int maxTimeInQueue = searcherConfig.getInt("multiSearcher.trafficLimiting.maxTimeInQueue");

            searcher = new TrafficLimitingSearcher(searcher, maxSimultaneousQueries, maxTimeInQueue);
        }
        if (searcherConfig.getBoolean("compositeMultiSearcher.useQueriesInProgress")) {
            searcher = new QueriesInProgressSearcher(searcher);
        }
        if (searcherConfig.getBoolean("compositeMultiSearcher.useCache")) {
            //TODO make this configurable, element timeout set at 1 minute
            Cache<QueryParams, GroupedSearchResults> cache = new TimeoutLRUCache<QueryParams, GroupedSearchResults>(500, 60000);
            searcher = new CacheSearcher(searcher, cache);
        }
        if (searcherConfig.getBoolean("compositeMultiSearcher.useSuggestQuery")) {
            try {
                searcher = new SuggestQuerySearcher(searcher, 
                        new WordQuerySuggestor(new File(searcherConfig.getString("multiSearcher.suggestQuerySearcher.dictionaryDir"))),
                        100,//XXX FIXME hardcoded value
                        searcherConfig.getFloat("multiSearcher.suggestQuerySearcher.factor"));
            } catch (java.io.IOException e) {
                logger.error("While creating SuggestQuerySearcher:" + e,e);
                throw new RuntimeException(e.getMessage(),e);
            }
        }
    }
    
    public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup group, int groupSize, AFilter filter, ASort sort) throws SearcherException {
    	return searcher.search(query, firstResult, count, group, groupSize, filter, sort);
    }
}
