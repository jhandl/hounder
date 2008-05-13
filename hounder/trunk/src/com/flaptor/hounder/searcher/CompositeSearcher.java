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

import java.io.File;

import org.apache.log4j.Logger;

import com.flaptor.clusterfest.NodeListener;
import com.flaptor.clusterfest.controlling.ControllerModule;
import com.flaptor.clusterfest.controlling.node.ControllableImplementation;
import com.flaptor.clusterfest.deploy.DeployListenerImplementation;
import com.flaptor.clusterfest.deploy.DeployModule;
import com.flaptor.clusterfest.monitoring.MonitorModule;
import com.flaptor.hounder.cluster.MultiSearcher;
import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.query.SynonymQuerySuggestor;
import com.flaptor.hounder.searcher.query.WordQuerySuggestor;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.Cache;
import com.flaptor.util.Config;
import com.flaptor.util.LRUCache;
import com.flaptor.util.PortUtil;
import com.flaptor.util.Statistics;

/**
 * The composition of Searchers in the order we are interested:
 * Statistics -> SuggestQuery -> Cache -> Queries in Progress -> 
 * TrafficLimiting -> Snippet -> Searcher
 * 
 * @author Martin Massera
 */
public class CompositeSearcher implements ISearcher {
    
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());

    private ISearcher searcher;
    private ISearcher baseSearcher;
    private NodeListener nodeListener;
    private TrafficLimitingSearcher trafficLimitingSearcher;
    
    public CompositeSearcher() {
        this(Config.getConfig("searcher.properties"));
    }
    
    public CompositeSearcher(Config searcherConfig) {

        // start clustering first.
    	if (searcherConfig.getBoolean("clustering.enable")) {
        	int port = PortUtil.getPort("clustering.rpc.searcher");
        	if (searcherConfig.getBoolean("searcher.isMultiSearcher")) {
        	    searcherConfig.set("clustering.node.type", "multisearcher");
        	} else {
        	    searcherConfig.set("clustering.node.type", "searcher");
        	}
        	nodeListener = new NodeListener(port, searcherConfig);
    		MonitorModule.addModuleListener(nodeListener, new SearcherMonitoredNode(this));
    		ControllerModule.addModuleListener(nodeListener, new ControllableImplementation());
            DeployModule.addModuleListener(nodeListener, new DeployListenerImplementation());
    		nodeListener.start();
        }

    	if (searcherConfig.getBoolean("searcher.isMultiSearcher")) {
    		baseSearcher = new MultiSearcher();
    	} else {
    		baseSearcher = new Searcher();
    	}
        searcher = baseSearcher;

        if (searcherConfig.getBoolean("compositeSearcher.useSnippetSearcher")) {
            searcher= new SnippetSearcher(searcher,searcherConfig);
        }
        if (searcherConfig.getBoolean("compositeSearcher.useTrafficLimiting")) {
            int maxSimultaneousQueries = searcherConfig.getInt("searcher.trafficLimiting.maxSimultaneousQueries");
            int maxTimeInQueue = searcherConfig.getInt("searcher.trafficLimiting.maxTimeInQueue");
            trafficLimitingSearcher = new TrafficLimitingSearcher(searcher, maxSimultaneousQueries, maxTimeInQueue);
            searcher = trafficLimitingSearcher;
        }
        if (searcherConfig.getBoolean("compositeSearcher.useQueriesInProgress")) {
            searcher = new QueriesInProgressSearcher(searcher);
        }
        
        if (searcherConfig.getBoolean("compositeSearcher.useCache") && !searcherConfig.getBoolean("searcher.isMultiSearcher")) {
        //if it's a multiSearcher, nobody will ever flush the cache
            Cache<QueryParams, GroupedSearchResults> cache = new LRUCache<QueryParams, GroupedSearchResults>(500); //XXX TODO: make this configurable
            if (baseSearcher instanceof Searcher) {
            	((Searcher)baseSearcher).addCache(cache);
            }
            searcher = new CacheSearcher(searcher, cache); 
        }
        if (searcherConfig.getBoolean("compositeSearcher.useSynonymSuggestQuery")) {
            try {
                searcher = new SuggestQuerySearcher(searcher,
                            new SynonymQuerySuggestor(new File(searcherConfig.getString("searcher.suggestQuerySearcher.synonymFile"))),
                            100,
                            searcherConfig.getFloat("searcher.suggestQuerySearcher.factor"));
            } catch (java.io.IOException e) {
                logger.error("While creating WordQuerySuggestor:"+e,e);
                throw new RuntimeException(e.getMessage(),e);
         
           }
        }
        if (searcherConfig.getBoolean("compositeSearcher.useSpellCheckSuggestQuery")) {
            try {
                searcher = new SuggestQuerySearcher(searcher, 
                        new WordQuerySuggestor(new File(searcherConfig.getString("searcher.suggestQuerySearcher.dictionaryDir"))),
                        100,//XXX FIXME hardcoded value
                        searcherConfig.getFloat("searcher.suggestQuerySearcher.factor"));
            } catch (java.io.IOException e) {
                logger.error("While creating WordQuerySuggestor:"+e,e);
                throw new RuntimeException(e.getMessage(),e);
            }
        }
    	searcher = new StatisticSearcher(searcher);
    }
    
    public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup group, int groupSize, AFilter filter, ASort sort) throws SearcherException{
    	if (logger.isDebugEnabled()) {
    		logger.debug("received query: "+  query + ",from: " + firstResult + ", requesting: " + count + ", grouping by:"
    				+ group + ", with groupsize: " + groupSize + ", filtering by: " + filter + ", sorting by: " + sort);
    	}
    	return searcher.search(query, firstResult, count, group, groupSize, filter, sort);
    }
    
    public ISearcher getBaseSearcher() {
        return baseSearcher;
    }
    
    public TrafficLimitingSearcher getTrafficLimitingSearcher() {
    	return trafficLimitingSearcher;
    }
}
