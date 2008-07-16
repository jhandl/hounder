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

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.payloads.BoostingTermQuery;

import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.group.NoGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.query.LazyParsedQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.Cache;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.Pair;


/**
 * Implements the search functionality.
 * Provides a series of "search" methods to obtain a SearchResults from the most up to date index.
 * Configuration variables:
 *      generateSnippets: boolean. True to generate snippets. If true, the next parameters must
 *          be set.
 *      snippetOfField: the name of the field containing the text used to generate the scripts.
 *      snippetFieldName: the name of the artificial field generated to return the hightlighted
 *          text (the snippets per se)
 *      snippetFragmentCount: number of chunks or fragments used in snippets.
 *      snippetFragmentSeparator: string used to glue different chunks in snippets.
 * 		
 * @author Flaptor Development Team
 */
/** @todo change the name of this class */
public class Searcher implements ISearcher {
	private static final Logger logger = Logger.getLogger(Execute.whoAmI()); 
    private static final Config config = Config.getConfig("searcher.properties");

	protected ReloadableIndexHandler ris = null;


	/**
	 * Constructor.
	 * Takes many configuration parameters from the config.
	 * @todo make the size of the hitsCache configurable
	 */
	public Searcher() {
		ris = new ReloadableIndexHandler();
    }

    /**
     * from ReloadableIndexHandler.addCache:
     * 
     * Adds the handle to a result cache to be cleared
     * when a new index is loaded. This is necessary because the owner of
     * the cache does not know when this happens, and the cache 
     * needs to be invalidated.
     * 
     */
    public void addCache(Cache<QueryParams, GroupedSearchResults> cache) {
        ris.addCache(cache);
    }

    /**
     * Advanced search method.
     * parameters:
     * @param query the query
     * @param firstResult which result to start from, used for pagination (0 is the first)
     * @param count how many results to retrieve
     * @param filter the filter to use @see Filter
     * @param sort a Hounder Sort. If null, the default relevance sort will be used.
     * @throws NullPointerException if the queryStr is null.
     * @throws IllegalArgumentException if the queryStr cannot be parsed correctly.
     */
    public final GroupedSearchResults search(final AQuery query, final int firstResult, final int count,  AGroup groupBy, int groupSize, final AFilter filter, final ASort sort) throws SearcherException {
        return searchImpl(query, firstResult, count, filter, (null == sort ?  null : sort.getLuceneSort()),groupBy,groupSize);
    }


    /**
     * Implementation of main search method.
     * This method performs the search over the cache and, in the case the cache doesn't have the required SearchResults,
     *	perfoms a new search calling the ReloadableIndexSearcher and stores the results in the cache.
     * @param query the query
     * @param firstResult which result to start from, used for pagination (0 is the first)
     * @param count how many results to retrieve
     * @param filter the hounder filter to filter the resultSet for.
     * @param sort the lucene's sort to use. May be null for default solt (relevance)
     * @throws NullPointerException if the queryStr is null.
     * @throws IllegalArgumentException if the queryStr cannot be parsed correctly.
     *
     * @todo documents without payloads will NOT match queries. if there is an index with documents without payload, they will NEVER 
     *      match a query.
     *
     */
    private GroupedSearchResults searchImpl(final AQuery query, final int firstResult, final int count, final AFilter afilter, final org.apache.lucene.search.Sort sort, AGroup groupBy, int groupSize) throws SearcherException{

        org.apache.lucene.search.Filter filter = null;
        if (null != afilter) {
            filter = afilter.getLuceneFilter();
        }

        GroupedSearchResults res = null;
        //it wasn't in the cache, we go ahead with the query
        try {


            // Construct boosting query with payloads
            Query luceneQuery = query.getLuceneQuery();

            Pair<GroupedSearchResults, org.apache.lucene.search.Query> resultPair = ris.search(luceneQuery, filter, sort, firstResult, count,groupBy,groupSize);
            res = resultPair.first();
            if (null == res) {
                throw new SearcherException("GroupedSearchResults is NULL");
            }
            return res;
        } catch (IOException e) {
            //something bad happened to the index on disk, nothing we can do
            logger.error(e.getMessage());
            throw new IllegalStateException(e);
        }
    }


    /**
     * Call this method just before releasing the object for GC.
     */
    public void close() {
        logger.info("close: stopping the ReloadableIndexHandler.");
        ris.requestStop();
        while (!ris.isStopped()) {
            Execute.sleep(20);
        }
        logger.info("close: ReloadableIndexHandler stopped.");
    }

    //main method 
    public static void main (final String args[]) throws Exception {
        String log4jConfigPath = com.flaptor.util.FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            org.apache.log4j.PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            logger.warn("log4j.properties not found on classpath!");
        }
        //boolean runForEver = false;
        Searcher ls = new Searcher();
        try {
            System.out.println("searching " + args[1] + " on index " + args[0]);
            try {
                Thread.sleep(2000); // Awful hack to wait until first index is loaded.
            } catch (Exception e) {
                System.err.println(e);
            }

            GroupedSearchResults res = ls.search(new LazyParsedQuery(args[1]), new Integer(args[2]).intValue(), new Integer(args[3]).intValue(), new NoGroup(), 1, null, null);
            System.out.println("Obtained " + res.groups() + " results, showing a page of " + res.totalGroupsEstimation() + " results");
            for (int i = 0; i < res.totalGroupsEstimation(); i++) { 
                System.out.println("Result #" + (i+1) + ", fields:");
            }
            System.out.println("DEBUG: stopping threads");
        } finally {
            ls.close();
        }
    }
}
