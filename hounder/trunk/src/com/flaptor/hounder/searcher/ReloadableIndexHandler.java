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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;

import com.flaptor.hounder.Index;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.group.AResultsGrouper;
import com.flaptor.hounder.searcher.group.NoGroup;
import com.flaptor.hounder.searcher.group.TopDocsDocumentProvider;
import com.flaptor.hounder.searcher.payload.SimilarityForwarder;
import com.flaptor.util.Cache;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.Pair;
import com.flaptor.util.RunningState;
import com.flaptor.util.Statistics;
import com.flaptor.util.Stoppable;

/**
 * Automatically detects index changes, providing required
 * <code>IndexSearcher</code> functionality on the newest index.
 * @author Flaptor Development Team
 */
final class ReloadableIndexHandler implements Stoppable {

	private static final Logger logger = Logger.getLogger(Execute.whoAmI());
	private static final Config config = Config.getConfig("searcher.properties");
	private static final Statistics statistics = Statistics.getStatistics();

	protected final int maxOffset;
	protected final int maxHitsPerPage;
    protected final int slackFactor;

    // The similarity to use when compairing documents. It is very useful for boosting on searchtime.
    private final org.apache.lucene.search.Similarity similarity;

    //State related te the shutdown sequence.
    private RunningState state = RunningState.RUNNING;

    //we need this handle to clear the cache when we reload the index
    private List<Cache> caches = new ArrayList<Cache>();

    private IndexLibrary library;
    AtomicReference<IndexRepository> currentIndexRepository = new AtomicReference<IndexRepository>(null);


    // Sample one every N queries
    private int querySamplePeriod = 0;
    private int queriesInPeriod = 0;
    private int savedQueryindex = 0;
    private int numberOfSavedQueries = 0;
    private SavedQuery[] savedQueries = null;


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

        similarity = new SimilarityForwarder();
        querySamplePeriod = config.getInt("Searcher.query.sample.period");
        numberOfSavedQueries = config.getInt("Searcher.query.sample.size");
        savedQueries = new SavedQuery[numberOfSavedQueries];

        //FIXME: This is a potential but unlikely race condition, as we are publishing "this" to another thread
        //	before the object is fully constructed (the constructor hasn't ended yet).
        library = new IndexLibrary(this);
    }



	/**
	 * Determine if the time has come to save another query
	 * as a sample for future use for re-heating the searcher.
	 * @return true if it is time to take another query sample.
	 */
	private boolean shouldSaveQuery() {
		boolean should = false;
		if (querySamplePeriod > 0) {
			queriesInPeriod++;
			if (queriesInPeriod >= querySamplePeriod) {
				queriesInPeriod = 0;
				should = true;
			}
		}
		return should;
	}

	/**
	 * Save a query for future use in re-heating the searcher after a new index has been loaded.
	 */
	private void saveQuery(Query query, Filter filter, Sort sort, int offset, int groupCount, AGroup groupBy, int groupSize) {
		if (savedQueryindex >= numberOfSavedQueries) savedQueryindex = 0;
	    savedQueries[savedQueryindex] = new SavedQuery(query, filter, sort, offset, groupCount, groupBy, groupSize);
	    savedQueryindex++;
	}

	/**
	 * This method will be called whenever a new index is read.
	 * The purpose of this is to make a search to cause the new index to be used for the first
	 * time and avoid a user the honor of waiting for the searcher to load and prepare its
	 * internal (lazy) data structures.
	 */
	public void executeSavedQueries() {
        if (null != savedQueries && null != currentIndexRepository.get()) {
        	int savePeriod = querySamplePeriod;
        	querySamplePeriod = 0;
        	for (SavedQuery query : savedQueries) {
        		if (null != query) {
			    	query.replay();
        		}
        	}
        	querySamplePeriod = savePeriod;
		}
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
	public Pair<GroupedSearchResults, Query> search(final Query query, final Filter filter, final Sort sort, final int offset, final int groupCount, AGroup groupBy, int groupSize) throws IOException, NoIndexActiveException, SearchTimeoutException, SearcherException {
		IndexRepository ir = currentIndexRepository.get();
		if (null == ir) {
			throw new NoIndexActiveException();
		}
		IndexSearcher searcher = ir.getIndexSearcher();
		try {
			if (shouldSaveQuery()) {
				saveQuery(query, filter, sort, offset, groupCount, groupBy, groupSize);
			}

			TopDocs tdocs;
			long startTime = System.currentTimeMillis();
			if (null == sort) { //Lucene's documentation is not clear about whether search may take a null sort, so...
				tdocs= searcher.search(query,filter, offset + groupSize * groupCount * slackFactor);
			} else {
				tdocs= searcher.search(query,filter, offset + groupSize * groupCount * slackFactor, sort);
			}
			statistics.notifyEventValue("lucene work time", System.currentTimeMillis() - startTime);

			// Exceptions are thrown to upper layer, but if we got here, assume everything is ok.
			Query rewrittenQuery = searcher.rewrite(query);
			GroupedSearchResults results = pageResults(tdocs, searcher, offset, groupCount, groupBy, groupSize);
			if (null == results) {
				throw new RuntimeException("GroupedSearchResults is NULL");
			}
			return(new Pair<GroupedSearchResults, Query>(results, rewrittenQuery));
		} catch (IOException e) {
			statistics.notifyEventError("lucene work time");
			throw e;
		} finally {
			ir.releaseIndexSearcher(searcher);
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
        if (null == groupBy ) {
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
    public int getNumDocs() throws NoIndexActiveException, SearchTimeoutException, SearcherException {
		IndexRepository ir = currentIndexRepository.get();
		if (null == ir) {
			throw new NoIndexActiveException();
		}
		IndexSearcher searcher = ir.getIndexSearcher();
        try {
            return searcher.getIndexReader().numDocs();
        } finally {
        	ir.releaseIndexSearcher(searcher);
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
     * Sets the new index searcher.
     * There should be only one thread doing this, synchronize changes just in
     * case.
     * @return the index reference for log records.
     * @throws IOException
     *             if there are too many indexes open and there is no room for
     *             the new one. Should never happen if used properly.
     */
    public void setNewIndex(final Index newIndex) throws SearcherException {
    	//first, I want to serialize (just in case) all calls to this method. For that I'll use
    	//currentIndexRepository as a semaphore. Note that the advantage of AtomicReference is not
    	//lost since currentIndexRepository users (search and getNumDocs) don't sync against it.
    	synchronized (currentIndexRepository) {
	    	IndexRepository newRepository = new IndexRepository(newIndex);
            IndexRepository oldIndexRepository = currentIndexRepository.get();
	    	currentIndexRepository.set(newRepository);
	        //invalidate the cache, if any
	        clearCaches();
	        //extensive index warming, if enabled.
	 		executeSavedQueries();
            if (null != oldIndexRepository) {
                oldIndexRepository.close();
            }
    	}
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
     * @inheritDoc
     */
    public boolean isStopped() {
        return (state == RunningState.STOPPED);
    }

    /**
     * After this call, all further attempts to use the index methods (@link index , @link indexDom )
     * will throw an IllegalStateException.
     * @inheritDoc
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


            logger.info("Closing current index...");
        	synchronized (currentIndexRepository) {
        		IndexRepository current = currentIndexRepository.getAndSet(null);
        		Execute.close(current, logger);
        	}
        	logger.info("All indexes closed.");


        	logger.info("Stop sequence finished.");
            state = RunningState.STOPPED;
        }
    }

    private class IndexRepository {
    	private static final int POOL_SIZE = 6;
        private final Index index;
    	BlockingQueue<IndexSearcher> searcherPool;
        private final long timeout;

    	public IndexRepository(final Index index) throws SearcherException {
            this.index = index;
    	    Config conf = Config.getConfig("searcher.properties");
    	    if (conf.getBoolean("compositeSearcher.useTrafficLimiting")) {
    		    timeout = conf.getInt("searcher.trafficLimiting.maxTimeInQueue");
    		    if (timeout < 0L) {
    		        throw new IllegalArgumentException("timeout is a negative number (" + timeout + ")");
    		    }
    		    logger.info("IndexRepository constructor: setting the max time to wait for an available index searcher to " + timeout);
    		} else {
    		    timeout = 0L;
    		    logger.info(" IndexRepository constructor: setting the max time to wait for an available index searcher to infinity.");
    		}
    	    preheatIndex(index);
    		searcherPool = new ArrayBlockingQueue<IndexSearcher>(POOL_SIZE, true);
    		for (int i = 0; i < POOL_SIZE; i++) {
    			IndexSearcher is = new IndexSearcher(index.getReader());
    			is.setSimilarity(similarity);
    			searcherPool.add(is);
    		}
    	}

    	private void preheatIndex(final Index index) throws SearcherException {
	    	IndexReader reader = index.getReader();
	    	try {
	    		reader.terms(); // for heating the index.
	    	} catch (IOException e) {
	    		throw new SearcherException(e);
	    	}
    	}

    	IndexSearcher getIndexSearcher() throws SearcherException {
            IndexSearcher retVal;
            if (logger.isDebugEnabled()) {
                logger.debug("getIndexSearcher: remaining capacity is " + searcherPool.remainingCapacity() + " , with a max size of " + POOL_SIZE);
            }
            try {
    		    if (timeout == 0L) {
    		        retVal = searcherPool.take();
    		    } else {
    		        retVal = searcherPool.poll(timeout, TimeUnit.MILLISECONDS);
    		    }
    		    if (null == retVal) {
    	            if (logger.isDebugEnabled()) {
    	                logger.debug("getIndexSearcher: (retVal is null) remaining capacity is " + searcherPool.remainingCapacity() + " , with a max size of " + POOL_SIZE);
    	            }
    		        throw new SearchTimeoutException(timeout ,"timeout exceeded while waiting for an indexSearcher");
    		    }
    		} catch (InterruptedException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("getIndexSearcher: (interrupted while waiting) remaining capacity is " + searcherPool.remainingCapacity() + " , with a max size of " + POOL_SIZE);
                }
    			throw new SearcherException(e);
    		}
    		return retVal;
    	}

    	void releaseIndexSearcher(IndexSearcher s) {
            if (logger.isDebugEnabled()) {
                logger.debug("releaseIndexSearcher: remaining capacity is " + searcherPool.remainingCapacity() + " , with a max size of " + POOL_SIZE);
            }
    		if (!searcherPool.offer(s)) {
                logger.debug("release: remaining capacity is " + searcherPool.remainingCapacity() + " , with a max size of " + POOL_SIZE);
                logger.fatal("releaseIndexSearcher: could not return an indexSearcher to the queue");
                System.exit(-1);
    		}
    	}

    	public void close() {
    		for (int i = 0; i < POOL_SIZE; i++) {
    		    boolean done= false;
    		    while (!done){
    		        try {
    		            IndexReader cir = searcherPool.take().getIndexReader();
    		            cir.close();
    		            done=true;
    		        } catch (IOException e) {
    		            logger.error("Exception while closing an indexReader.", e);
    		            done=true;
    		        } catch (InterruptedException e) {
    		            done=false;
    		            logger.error("InterruptedException while closing an indexReader.", e);
    		        }
    		    }
    		}
            library.discardIndex(index);
    	}
    }

    private class SavedQuery {
    	Query query;
    	Filter filter;
    	Sort sort;
    	int offset;
    	int groupCount;
    	AGroup groupBy;
    	int groupSize;
    	public SavedQuery(Query query, Filter filter, Sort sort, int offset, int groupCount, AGroup groupBy, int groupSize) {
    		this.query = query;
    		this.filter = filter;
    		this.sort = sort;
    		this.offset = offset;
    		this.groupCount = groupCount;
    		this.groupBy = groupBy;
    		this.groupSize = groupSize;
    	}
    	public void replay() {
    		try {
				search(query, filter, sort, offset, groupCount, groupBy, groupSize);
			} catch (Exception e) {
				// ignore.
			}
    	}
    }

}
