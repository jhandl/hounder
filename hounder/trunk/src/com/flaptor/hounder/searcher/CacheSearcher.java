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

import org.apache.log4j.Logger;

import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.Cache;
import com.flaptor.util.Statistics;

/**
 * This searcher has a cache and a base searcher
 * If queries arent in the cache, it uses the base searcher to get results 
 * 
 * @author Martin Massera
 */
public class CacheSearcher implements ISearcher{
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());
    
    private ISearcher searcher;
    private Cache<QueryParams, GroupedSearchResults> cache;
    
    /**
     * Creates the searcher
     * @param searcher the base searcher
     * @param cache the cache to store and retrieve results
     */
    public CacheSearcher(ISearcher searcher, Cache<QueryParams, GroupedSearchResults> cache) {
        if (null == searcher) {
            throw new IllegalArgumentException("searcher cannot be null.");
        }
        if (null == cache) {
            throw new IllegalArgumentException("cache cannot be null.");
        }

        this.searcher = searcher;
        this.cache = cache;
    }

    public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup groupBy, int groupSize, AFilter afilter, ASort asort)  throws SearcherException{        
    	boolean hit = true;
    	long start = System.currentTimeMillis();
    	
    	//first, see if the query is in the cache
        QueryParams queryParams = new QueryParams(query,firstResult,count,groupBy,groupSize,afilter,asort);

        GroupedSearchResults res = cache.get(queryParams);

        if (res == null) { //if it is not in the cache
        	hit = false;
            res = queryParams.executeInSearcher(searcher);
            cache.put(queryParams, res);            //tell the cache to maybe store the results (we don't control whether it does it or not)
        }
        
        long end = System.currentTimeMillis();
        Statistics.getStatistics().notifyEventValue(hit ? "cacheHit" : "cacheMiss", (end-start)/1000.0f);

        return res;
    }
}
