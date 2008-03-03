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
package com.flaptor.search4j.searcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;

import com.flaptor.search4j.Index;
import com.flaptor.search4j.searcher.group.AGroup;
import com.flaptor.search4j.searcher.group.AResultsGrouper;
import com.flaptor.search4j.searcher.group.NoGroup;
import com.flaptor.search4j.searcher.group.TopDocsDocumentProvider;
import com.flaptor.search4j.searcher.payload.DatePayloadScorer;
import com.flaptor.util.Cache;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.Pair;
import com.flaptor.util.RunningState;
import com.flaptor.util.Stoppable;

/**
 * Automatically detects index changes, providing required
 * <code>IndexSearcher</code> functionality on the newest index. It checks
 * periodically for new indexes. After a new index is found, it waits 30 seconds
 * to check again. Search operations are always completed, regardless the index
 * is changing.
 * @author Flaptor Development Team
 */
final class ReloadableIndexHandler implements Stoppable {

	private static final Logger logger = Logger.getLogger(Execute.whoAmI());
	private static final Config config = Config.getConfig("searcher.properties");
	
	/** when an old index is replaced by a new one, the queries pending on the old index
	 * are still processed on it. Because there could be many index changes in a row,
	 * we can keep a few old indexes for a little while until they service their queries.
	 */
	private static final int MAX_CONCURRENT_SEARCHERS=5;
	protected final int maxOffset;
	protected final int maxHitsPerPage;
    protected final int slackFactor;

    //State related te the shutdown sequence.
    private RunningState state = RunningState.RUNNING;

    //we need this handle to clear the cache when we reload the index
    private List<Cache> caches = new ArrayList<Cache>();

    private IndexLibrary library;

    private int size;

    // The following objects are shared by searcher threads and by readindex thread.
    // search() method, used by searcher threads, modifies inUse[] and usageCount[],
    // and uses currentIndexId and indexSearchersPool[].
    Pair<Index, IndexSearcher>[] indexSearchersPool;

    int[] usageCount;
    int[] inUse;
    boolean[] available;
    long[] startTimestamp;
    int currentIndexId;
    int[] errCount;
    int[] okCount;

    // The similarity to use when compairing documents.
    // It is very useful for boosting on searchtime.
    private org.apache.lucene.search.Similarity similarity;


    /**
     * Constructor.
     * @fixme there's a potential but unlikely race condition, as we're publishing "this" to onother thread before
     *	the object is fully constructed (the constructor hasn't ended yet)
     * @todo Try to wait at least until first read, so first queres aren't missed.
     *	 Right now, Searcher is instantiated before the index is accessed, and the first queries after a startup
     *	return 0 results. This shouldn't be an issue, though, as it only happens when the application is started.
     *	The index switchings are handled properly, no queries are lost.
     *
     */
    @SuppressWarnings("unchecked")
    public ReloadableIndexHandler() {
        maxOffset = config.getInt("ReloadableIndexSearcher.maxOffset");
        maxHitsPerPage = config.getInt("ReloadableIndexSearcher.maxHitsPerPage");
        slackFactor = config.getInt("ReloadableIndexSearcher.lookupLimit"); 
        size = MAX_CONCURRENT_SEARCHERS;
        indexSearchersPool = new Pair[size];
        usageCount = new int[size]; 
        inUse = new int[size];
        available = new boolean[size];
        startTimestamp = new long[size];
        errCount = new int[size];
        okCount = new int[size];
        
        for (int i = 0; i < size; i++) {
            indexSearchersPool[i] = null;
            usageCount[i] = 0;
            inUse[i] = 0;
            available[i] = true;
            startTimestamp[i] = 0;
        }

        currentIndexId = -1;

        String whichSimilarity = config.getString("Searcher.payloadScorer");
        if ("date".equals(whichSimilarity.trim().toLowerCase())) {
            similarity = new DatePayloadScorer();
            logger.debug("using DatePayloadScorer as similarity");
        } else {
            similarity = new org.apache.lucene.search.DefaultSimilarity();
        }


        library = new IndexLibrary(this);
        //FIXME: This is a potential but unlikely race condition, as we are publishing "this" to another thread
        //	before the object is fully constructed (the constructor hasn't ended yet).




    }


    /**
     * Executes <code>IndexSearch.search(Query query, Filter filter, Sort sort)</code> on current index in a
     * thread safe way.
     *
     * @param query the lucene Query to use for this search.
     * @param filter the lucene filter to use.
     * @param sort the lucene sort to use.
     * @return a Pair of <SearchResults, Query>.
     *  The SearchResults object is the result of the query. If the search returned less document than those required,
     *	it returs as many as there are. The scores returned in the SearchResults are not normalized, which is important to
     *	allow to compare result from different searchers (see the SearchCluster design).
     *  The Query is the same input query, but rewritten. This expands several kinds of queries into term queries. It
     *  is usefull to generate highlighting of snippets, but can be ignored if not.
     * @throws NoIndexActiveException if there is no index active or if <code>IndexSearcher.search()</code> failed.
     * See #org.apache.lucene.search.IndexSearcher for details.
     */
    public Pair<GroupedSearchResults, Query> search(final Query query, final Filter filter, final Sort sort, final int offset, final int groupCount, AGroup groupBy, int groupSize) throws IOException, NoIndexActiveException {
        // Only need to synchronize access to counters. The search operation can be left outside, as it is thread-safe.
        // indexId is a local variable, and its indexSearcher is already marked 'in-use'.
        // Lock is obtained over 'inUse' array to separate of new index setting sync. process.
        int indexId = -1;
        boolean ok = false;
        synchronized(inUse) {
            if (currentIndexId == -1) {
                throw new NoIndexActiveException();
            }
            indexId = currentIndexId;

            inUse[indexId]++;
            usageCount[indexId]++;
        }
        try {
            org.apache.lucene.search.Searcher searcher = indexSearchersPool[indexId].last();
            TopDocs tdocs;
            if (null == sort) { //Lucene's documentation is not clear about whether search may take a null sort, so...
                tdocs= searcher.search(query,filter, offset + groupSize * groupCount * slackFactor);
            } else {
                tdocs= searcher.search(query,filter, offset + groupSize * groupCount * slackFactor, sort);
            }

            // Exceptions are thrown to upper layer, but if we got here, assume everything is ok.
            ok = true;
            Query rewrittenQuery = searcher.rewrite(query);
            GroupedSearchResults results = pageResults(tdocs, searcher, offset, groupCount, groupBy, groupSize);
            if (null == results) {
            	throw new RuntimeException("GroupedSearchResults is NULL");
            }
            return(new Pair<GroupedSearchResults, Query>(results, rewrittenQuery));

        } finally {
            synchronized(inUse) {
                inUse[indexId]--;
                if (ok) { okCount[indexId] ++; } // keep counting after having decided
                else { errCount[indexId] ++; }   // what to do with the index for stats
            }
        }
    }

    /**
	  Fetches the actual documents for the requested interval.
	  The results returned by lucene do not know anything about paging. This method
	  takes care of that.
	  @param tdocs the TopDocs, returned by a low level search api.
	  @param searcher the seacher over which the search that returned tdocs was issued.
	  @param offset the zero base position of the first result to return
	  @param hitsPerPage the number of documents to return, starting from offset.
	  @return a SearchResults object, containing the un-normalized (real) score information.
     */
    private void checkQueryParameters(final TopDocs tdocs, final int offset, int hitsPerPage) { 

        //check for weird parameters
        if (offset > tdocs.totalHits) {
            String s = "user requested offset " + offset + " which is higher than the number of hits: " + tdocs.totalHits;
            logger.error(s);
            throw new IllegalArgumentException(s);
        }
        if (offset > maxOffset) {
            String s = "user requested offset " + offset + " which is higher than the maximum allowed of " + maxOffset;
            logger.error(s);
            throw new IllegalArgumentException(s);
        }

        if (hitsPerPage > maxHitsPerPage) {
            logger.warn("filterHits: the hitsPerPage passed is greater than the max. allowed. Using the max.");
            hitsPerPage = maxHitsPerPage;
        }
    }



    private GroupedSearchResults pageResults (final TopDocs tdocs, final org.apache.lucene.search.Searcher searcher, final int offset, int groupCount, AGroup groupBy, int groupSize) {

        checkQueryParameters(tdocs, offset, groupCount);


        AResultsGrouper grouper;
        if (null ==groupBy ) {
            grouper = (new NoGroup()).getGrouper(new TopDocsDocumentProvider(tdocs,searcher));
        } else {
            grouper = groupBy.getGrouper(new TopDocsDocumentProvider(tdocs,searcher));
        }
        return grouper.group(groupCount,groupSize,offset);
    }




    /**
     * Returns the number of documents in the current index. Note that the index may change at any
     * time, so the returned value should be taken as a hint on the actual size of the index.
     *
     * @return The number of (undeleted) documents in the active index.
     * @throws NoIndexActiveException if there is no index active.
     */
    public int getNumDocs() throws NoIndexActiveException {
        // Only need to synchronize access to counters. The search operation can be left outside, as it is thread-safe.
        // indexId is a local variable, and its indexSearcher is already marked 'in-use'.
        // Lock is obtained over 'inUse' array to separate of new index setting sync. process.
        int indexId = -1;
        boolean ok = false;
        synchronized(inUse) {
            if (currentIndexId == -1) {
                throw new NoIndexActiveException();
            }
            indexId = currentIndexId;

            inUse[indexId]++;
            usageCount[indexId]++;
        }
        try {
            int retVal = indexSearchersPool[indexId].last().getIndexReader().numDocs();
            // Exceptions are thrown to upper layer, but if we got here, assume everything is ok.
            ok = true;
            return retVal;
        } finally {
            synchronized(inUse) {
                inUse[indexId]--;
                if (ok) { okCount[indexId] ++; } // keep counting after having decided
                else { errCount[indexId] ++; }   // what to do with the index for stats
            }
        }
    }

    /**
     * Sets the handle to result caches to be cleared
     * when a new index is loaded. This is necessary because the owner of
     * the cache does not know when this happens, and the cache 
     * needs to be invalidated.
     */
    public void setCaches(List<Cache> caches) {
        this.caches = caches;
    }

    /**
     * Adds the handle to a result cache to be cleared
     * when a new index is loaded. This is necessary because the owner of
     * the cache does not know when this happens, and the cache 
     * needs to be invalidated.
     */
    public void addCache(Cache cache) {
        caches.add(cache);
    }

    /**
     * Iterates over all index searchers in the pool until first available.
     * @return first index id available.
     */
    private int getAvailableIndexId() {

        int newIndexId = -1;
        for (int i = 0; i < available.length; i++) {
            if (available[i]) {
                newIndexId = i;
                break;
            }
        }
        return newIndexId;
    }

    /**
     * Sets the new index searcher.
     * There should be only one thread doing this, synchronize changes just in
     * case.
     * @return the index reference for log records.
     * @throws IOException
     *             if there are too many indexes open and there is no room for
     *             the new one. Should never happen if used properly.
     */
    public int setNewIndex(final Index newIndex) throws IOException {

        IndexSearcher newIndexSearcher = new IndexSearcher(newIndex.getReader());
        newIndexSearcher.setSimilarity(similarity);
        int newIndexId = getAvailableIndexId();
        if (-1 == newIndexId) {
            // Should never happen, though, as we checked before calling this
            // method.
            throw new IOException("Too many indexes in use");
        }
        indexSearchersPool[newIndexId] = new Pair<Index, IndexSearcher>(newIndex, newIndexSearcher);
        startTimestamp[newIndexId] = System.currentTimeMillis();
        available[newIndexId] = false;
        inUse[newIndexId] = 0;
        usageCount[newIndexId] = 0;
        errCount[newIndexId] = 0;
        okCount[newIndexId] = 0;

        // This part must be synchronized with searchers, so the same object is
        // used to get the lock.
        // See search() method.
        synchronized (inUse) {
            currentIndexId = newIndexId;
        }

        //invalidate the cache, if any
        clearCaches();

        return (currentIndexId);
    }

    /**
     * Clear the caches.
     * Invalidates all the caches, after printing statistics about their hitrate.
     */
    private void clearCaches() {
        if (null != caches) {
            for (int i = 0; i < caches.size(); i++) {
                Cache cache = caches.get(i);
                logger.info("Cache " + i + ": hit ratio since last clear is " + cache.getRecentHitRatio() + ", historic is " + cache.getHitRatio());
                cache.clear();
            }
        }
    }

    /**
     * Returns false if there is at least one place to hold a new index.
     */
    public boolean isFull() {
        return (-1 == getAvailableIndexId());
    }

    /**
     * Closes all open but not-in-use indexes.
     * 
     */
    public void flush() {
        for (int i = 0; i < size; i++) {
            // Must be synchronized with searchers.
            synchronized (inUse) {
                if (i == currentIndexId)
                    continue;
                if ((!available[i]) && (inUse[i] == 0)) {
                    if (indexSearchersPool[i] != null) {
                        library.discardIndex(indexSearchersPool[i].first());
                        indexSearchersPool[i] = null; // It can now be gc'ed.
                    }
                    logger.info("Index " +i+ " dropped, in use for " +
                            ((System.currentTimeMillis() - startTimestamp[i])/60000)+ " minutes, with " +
                            usageCount[i]+ " uses (errors: " +errCount[i]+ ", success: " +okCount[i]+ ")");
                    inUse[i] = 0;
                    usageCount[i] = 0;
                    errCount[i] = 0;
                    okCount[i] = 0;
                    available[i] = true;
                }
            }
        }
    }


    /**
     * @inheritDoc
     */
    public boolean isStopped() {
        return (state == RunningState.STOPPED);
    }

    /**
     * @inheritDoc
     * After this call, all further attempts to use the index methods (@link index , @link indexDom )
     * will throw an IllegalStateException.
     */
    public void requestStop() {
        if (state == RunningState.RUNNING) {
            state = RunningState.STOPPING;
            new StopperThread().start();
        } else {
            logger.warn("requestStop: stop requested while not running. Ignoring.");
        }
    }


    //--------------------------------------------------------------------------------------------------------
    //Internal classes
    //--------------------------------------------------------------------------------------------------------
    /**
     * This class handles the sequence of events that stops the ReloadableIndexSearcher.
     */
    private class StopperThread extends Thread {
        private final Logger logger = Logger.getLogger(StopperThread.class);

        public StopperThread() {
            super();
            setName("ReloadableIndexSearcher.StopperThread");
        }

        /**
         * Executes the ReloadableIndexSearcher shutdown sequence asynchronously
         * After that, sets the ReloadableIndexSearcher state to Stopped.
         */
        public void run() {
            logger.info("Beginning stop sequence.");
            assert(state == RunningState.STOPPING);
            logger.info("Stopping IndexLibrary...");
            library.requestStop();
            while (!library.isStopped()) {
                Execute.sleep(20);
            }
            logger.info("IndexLibrary stopped.");
            logger.info("Closing open indexes...");
            //FIXME: if there're many indexes open with long queries, this may fail.
            int localCurrentIndexId = -1;
            synchronized (inUse) {
                localCurrentIndexId = currentIndexId;
                currentIndexId = -1;
            }
            if (localCurrentIndexId != -1) {
                Execute.close(indexSearchersPool[localCurrentIndexId].first());
                Execute.close(indexSearchersPool[localCurrentIndexId].last());
                indexSearchersPool[localCurrentIndexId] = null; // It can now be gc'ed.
            }
            flush();
            logger.info("All indexes closed.");
            logger.info("Stop sequence finished.");
            state = RunningState.STOPPED;
        }
    }

}
