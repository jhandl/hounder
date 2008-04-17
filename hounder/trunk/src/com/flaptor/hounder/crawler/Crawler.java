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
package com.flaptor.hounder.crawler;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.flaptor.clusterfest.NodeListener;
import com.flaptor.clusterfest.controlling.ControllerModule;
import com.flaptor.clusterfest.controlling.node.ControllableImplementation;
import com.flaptor.clusterfest.monitoring.MonitorModule;
import com.flaptor.hounder.crawler.clustering.CrawlerControllableImplementation;
import com.flaptor.hounder.crawler.modules.CacheModule;
import com.flaptor.hounder.crawler.modules.CommandWithPageDB;
import com.flaptor.hounder.crawler.modules.DocumentCacheItem;
import com.flaptor.hounder.crawler.modules.IProcessorModule;
import com.flaptor.hounder.crawler.modules.ModulesManager;
import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.hounder.crawler.pagedb.PageDB;
import com.flaptor.hounder.crawler.pagedb.distributed.DPageDB;
import com.flaptor.hounder.crawler.pagedb.distributed.PageCatcher;
import com.flaptor.hounder.util.UrlFilter;
import com.flaptor.util.CloseableQueue;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.PortUtil;
import com.flaptor.util.cache.FileCache;

/**
 * This class implements Hounder's web crawler.
 * @author Flaptor Development Team
 */
public class Crawler {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private String pagedbDir; // the directory of the pagedb.
    private String injectedPagedbDir; // the directory of the injected pagedb.
    private int indexOptimizePeriod; // the number of cycles between index optimizations.
    private IFetcher fetcher; // the fetcher to be used.
    private CloseableQueue<FetchList> fetchlistQueue; // this queue holds fetch lists ready to be fetched.
    private CloseableQueue<FetchList> injectedFetchlistQueue; // this queue holds fetch lists ready to be fetched, that come from temporary page dbs.
    private CloseableQueue<FetchData> fetchdataQueue; // this queue holds fetched data ready to be processed.
    private long discoveryPages; // the number of pages in the discovery front after one cycle. TODO: should be in pagedb.
    private boolean cycleFinished; // if true, the cycle has finished.
    private Object cycleFinishedMonitor; // used for waking up the crawler when the cycle finished.
    private static StopMonitor stopMonitor; // the monitor for the stop file.
    private ModulesManager modules; // this holds the modules that will process the crawled pages.
    private boolean distributed; // if true, the underlying pagedb will be distributed.
    private PageCatcher pageCatcher = null;
    private NodeListener nodeListener;
    private static UrlFilter urlFilter;


    /**
     * Class initializer.
     * Does not impose a fetcher.
     */
    public Crawler () throws Exception {
        this(null);
    }


    /** 
     * Class initializer.
     * Accepts fetcher suggestoin.
     * @param fetcher the provided fetcher, or null if no specific fetcher provided.
     */
    public Crawler (IFetcher fetcher) throws Exception {
        this.fetcher = fetcher;
        Config config = Config.getConfig("crawler.properties");
        indexOptimizePeriod = config.getInt("index.optimize.period");
        pagedbDir = config.getString("pagedb.dir");
        injectedPagedbDir = config.getString("injected.pagedb.dir");
        distributed = config.getBoolean("pagedb.is.distributed");
        fetchlistQueue = new CloseableQueue<FetchList>(3); // max three fetchlists in the queue
        injectedFetchlistQueue = new CloseableQueue<FetchList>(); 
        fetchdataQueue = new CloseableQueue<FetchData>(3); //TODO: analyze if the fetchdata should be written to disk.
        cycleFinishedMonitor = new Object();
        stopMonitor = new StopMonitor("stop");
        modules = ModulesManager.getInstance();
        urlFilter = new UrlFilter();

    	if (config.getBoolean("clustering.enable")) {
        	int port = PortUtil.getPort("clustering.rpc.crawler");
    		nodeListener = new NodeListener(port, config);
    		MonitorModule.addMonitorListener(nodeListener, new CrawlerMonitoredNode(this));
    		ControllerModule.addControllerListener(nodeListener, new ControllableImplementation());
    		nodeListener.addModuleListener("crawlerControl", new CrawlerControllableImplementation());
    		nodeListener.start();
        }
    }


    /**
     * Returns true if the crawler is running.
     * @return true if the crawler is running.
     */
    public static boolean running () {
        return !StopMonitor.stopRequested();
    }

    // Stops the crawler
    private void stopCrawler () {
        StopMonitor.stop();
        if (null != pageCatcher) {
            pageCatcher.stop();
        }
/* 
    Commented out because it causes the crawler to hang at the end, should investigate more.
    If this is not used, an external stop request may not work because the fetchserver would be waiting for a fetchlist.
        fetchlistQueue.enqueueBlock(null); // this will stop threads that use null as a signal for stopping, like the fetchserver.
*/
    }


    /**
     * Filters urls based on nutch configuration file.
     * @param url to be filtered.
     * @return the same url if it passed the filter, or null if it didn't.
     */
    public static String urlFilter(String url) {
        String res;
        try {
            res = urlFilter.filter(url);
        } catch (Exception e) {
            res = null;
        }
        return res;
    }


    // Decides whether the index should be optimized after the last cycle
    private boolean shouldOptimizeAfterCycle (long cycle) {
        if (indexOptimizePeriod == 0) {
            return false;
        }
        return (cycle % indexOptimizePeriod == 0);
    }



    // This class implements the monitor for the fetchlist queue.
    // It keeps the fetchlist queue non-empty.
    private class FetchlistQueueMonitor extends Thread {

        private FetchlistFactory factory = null;
        private long skip;
        private long seen;

        public FetchlistQueueMonitor (PageDB oldPageDB, PageDB tmpPageDB, long skip) {
            try {
                factory = new FetchlistFactory(oldPageDB,tmpPageDB);
            } catch (IOException e) {
                logger.error(e,e);
            }
            this.setName("FetchlistQueueMonitor");
            this.skip = skip;
            seen = 0;
        }


        public void run () {
            FetchList fetchlist;
            boolean couldEnqueue;
            do {
                fetchlist = factory.getNextFetchlist();

                if (null == fetchlist) {
                    fetchlistQueue.close();
                    logger.debug("Enqueued last fetchlist of this cycle.");
                    break;
                } 
                // else
                seen += fetchlist.getSize();
                if (skip > seen) {
                    fetchlist.remove();
                } else {
                    couldEnqueue = false;
                    while (running() && !couldEnqueue && !fetchlistQueue.isClosed()) {
                        couldEnqueue = fetchlistQueue.enqueueBlock(fetchlist,100);
                    }
                }
            } while (running() /* && !fetchlist.isClosed()*/);

        }

    }


    // This class checks if there is an injected page db,
    // and if it is found it generates a fetchlist with its content.
    // It assumes that the pagedb passed to the constructor is open for 
    // writing, and may have problems if the new pagedb is detected
    // while closing tmp.
    // @todo check that this does not cause problems.
    private class InjectedFetchlistQueueMonitor extends Thread {
        private FetchlistFactory factory = null;
        private PageDB tmpPageDB = null;

        public InjectedFetchlistQueueMonitor (PageDB tmpPageDB) {
            this.tmpPageDB = tmpPageDB;
            this.setName("InjectedFetchlistQueueMonitor");
        }

        public void run() {
            while (running() && !cycleFinished) {

                File injectedPageDB = new File(injectedPagedbDir);
                // Wait for the new pagedb to appear
                while( !injectedPageDB.exists() && running() && !cycleFinished && (null != factory)) {
                    Execute.sleep(10000);
                }

                // There is a pagedb file, and the factory is doing nothing.
                if (injectedPageDB.exists() && (null == factory)) {
                    try {
                        PageDB pageDB = new PageDB(injectedPagedbDir);
                        pageDB.open(PageDB.READ);
                        factory = new FetchlistFactory(pageDB,tmpPageDB);

                        FetchList fetchlist = factory.getNextFetchlist();
                        while (null != fetchlist && running() && !cycleFinished) {
                            injectedFetchlistQueue.enqueueNoBlock(fetchlist);
                            fetchlist = factory.getNextFetchlist();
                        }

                        // if the pageDB is exhausted, we can delete it
                        if (null == fetchlist) {
                            // End with this pageDB
                            logger.debug("Injected pagedb exhausted. Deleting it");
                            pageDB.close();
                            pageDB.deleteDir();
                            // Discard previous factory
                            factory = null;
                        }
                    } catch (IOException e) {
                        logger.error(e,e);
                        logger.error("Shutting down InjectedFetchlistQueueMonitor. Injection capabilities disabled");
                        break;
                    }
                }
            }
        }
    }



    // This class implements the monitor for the fetchdata queue.
    private class FetchdataQueueMonitor extends Thread {
        private PageDB tmpPageDB;
        private PageDB oldPageDB;
        private FetchdataProcessor processor;
        public FetchdataQueueMonitor (PageDB oldPageDB, PageDB tmpPageDB, FetchdataProcessor processor) {
            this.tmpPageDB = tmpPageDB;
            this.oldPageDB = oldPageDB;
            this.processor = processor;
            discoveryPages = 0;
            this.setName("FetchdataQueueMonitor");
        }

        public void run () {

            FetchData fetchdata = null;
            do {
                boolean couldDequeue = false;
                while (!couldDequeue && !fetchdataQueue.isClosed() && running()) {
                    // Try to get a fetchlist from the queue with a 10s timeout
                    fetchdata = fetchdataQueue.dequeueBlock(100);
                    couldDequeue = (null != fetchdata);
                }

                if (couldDequeue) {
                    try {
                        discoveryPages += processor.processFetchdata(fetchdata, oldPageDB, tmpPageDB);
                    } catch (Exception e) {
                        logger.error(e,e);
                    }
                    fetchdata.remove();
                } else if (fetchdataQueue.isClosed()) { //could not dequeue
                    logger.debug("Processed last fetchdata from queue");
                    break;
                }
            } while (running());

            cycleFinished = true;
            synchronized(cycleFinishedMonitor) {
                cycleFinishedMonitor.notifyAll();
            }
        }

    }


    /**
     * Returns the next fetchlist to be fetched.
     * @return the next fetchlist.
     */
    public FetchList getFetchlist () throws Exception {
        FetchList fetchlist = injectedFetchlistQueue.dequeueNoBlock();
        boolean couldDequeue = (null != fetchlist);     

        if (couldDequeue) {
            logger.debug("Dequeued injected fetchlist");
            return fetchlist;
        }

        // else, dequeue from standard queue.
        while ((!couldDequeue) && running() && !fetchlistQueue.isClosed()){
            fetchlist = fetchlistQueue.dequeueBlock(100);
            couldDequeue = (null != fetchlist);
        }    

        if (couldDequeue && fetchlistQueue.isClosed()) {
            logger.debug("Dequeued the last fetchlist of this cycle.");
        }
        return fetchlist;
    }

    /**
     * Accepts fetched data to be processed.
     * @param fetchdata the data that has been fetched.
     */
    public void takeFetchdata (FetchData fetchdata) throws Exception {
        if (null == fetchdata) {
            fetchdataQueue.close();
            logger.debug("Closing fetchdataQueue. No more fetchdata will be received.");
            return;
        }
        // else, enqueue it
        boolean enqueued = false;
        while (!enqueued) {
            enqueued = fetchdataQueue.enqueueBlock(fetchdata,100);
        }
    }


    // When refreshing, the crawler must not generate a temporary or a new pagedb. 
    // This is a fake pagedb that cheats the crawler into believing it is creating theses pagedbs.
    private class NoPageDB extends PageDB {
        public NoPageDB() { super(""); }
        public NoPageDB(String filename) { super(filename); }
        public void open(int action) {}
        public void addPage(Page page) {}
        public Iterator<Page> getPages() throws IOException { return new NoPageIterator(); }
        public void close() {}
        public long getSize() { return 0; }
        public long getCycles() { return 0; }
        public long getFetchedSize() { return 0; }
        public float getPriorityThreshold(int percentile) { return 0f; }
        public void setSameCycleAs(PageDB otherdb) {}
        public void setNextCycleOf(PageDB otherdb) {}
        public String getDir() { return "/dev/null"; }
        public boolean deleteDir() { return true; }
        public boolean rename(String newname) { return true; }
        private class NoPageIterator extends PageIterator {
            public NoPageIterator() throws IOException {} 
            public boolean hasNext() { return false; }
        }
    }


    /**
     * Signals the start of a crawl cycle.
     * @param oldPageDB the pagedb from which the crawler will read pages during this cycle.
     */
    public void declareStartCycle (PageDB oldPageDB) {
        CommandWithPageDB cmd = new CommandWithPageDB("startCycle",oldPageDB);
        modules.applyCommand(cmd);
    }

    /**
     * Signals the end of a crawl cycle.
     * @param newPageDB the pagedb that the crawler has created during this cycle.
     */
    public void declareEndCycle (PageDB newPageDB) {
        CommandWithPageDB cmd = new CommandWithPageDB("endCycle",newPageDB);
        modules.applyCommand(cmd);
    }


    /**
     * Submits all fetched pages from the pagedb to the module pipe retrieving the page contents from the cache.
     * It needs to have the cache configured in the modules list.
     * If there is no cache, it falls back to the refetch method.
     */
    public void refresh (long skip) {
        boolean hasCache = false;
        IProcessorModule[] caches = modules.getModuleInstances("com.flaptor.hounder.crawler.modules.CacheModule");
        if (caches.length > 0) {
            CacheModule cacheModule = null;
            FileCache<DocumentCacheItem> fileCache = null;
            for (int i=0; i<caches.length && null == fileCache; i++) {
                cacheModule = (CacheModule)caches[i];
                fileCache = cacheModule.getPageCache();
            }
            if (null != fileCache) {
                try {
                    modules.unloadModule(cacheModule);
                    hasCache = true;
                    long seen = 0;
                    PageDB pagedb = new PageDB(pagedbDir);
                    long total = pagedb.getSize();
                    declareStartCycle(pagedb);
                    PageCache pageCache = new PageCache(pagedb, fileCache, skip);
                    FetchdataProcessor processor = new FetchdataProcessor();
                    processor.processFetchdata(pageCache, pagedb, new NoPageDB());
                    pagedb.close();
                    declareEndCycle(pagedb); // Note: normally this would be the newPageDB, but in a refresh there is no such thing.
                    modules.applyCommand("optimize");
                } catch (IOException e) {
                    logger.error(e,e);
                }
            }
        }
        if (!hasCache) {
            crawl(0,skip);
        }
        stopCrawler();
    }


    public void redistribute () {
        if (!distributed) {
            logger.error("Can't redistribute a non-distributed PageDB");
        } else {
            try {
                PageDB oldPageDB = new PageDB(pagedbDir);
                DPageDB newPageDB = new DPageDB(pagedbDir+".new");
                oldPageDB.open(PageDB.READ);
                newPageDB.open(DPageDB.WRITE + DPageDB.UNSORTED);
                long total = oldPageDB.getSize();
                long done = 0;
                for (Page page : oldPageDB) {
                    newPageDB.addPage(page);
                    if (++done % 10000 == 0) {
                        logger.info("Redistributed "+done+" of "+total+" pages.");
                    }
                }
                oldPageDB.close();
                newPageDB.close();
                String oldName = oldPageDB.getDir();
                PageDB tmpPageDB = new PageDB(pagedbDir+".tmp");
                tmpPageDB.deleteDir();
                if (attempt(oldPageDB.rename(tmpPageDB.getDir()), "renaming pagedb -> pagedb.tmp")) {
                    if (attempt(newPageDB.rename(oldName), "renaming pagedb.new -> pagedb")) {
                        if (attempt(tmpPageDB.deleteDir(), "deleting pagedb.tmp")) {
                            logger.info("Done redistributing.");
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Redistributing the pagedb", e);
            }
        }
        stopCrawler();
    }


    /** 
     * Does the actual crawl.
     * @param cycles -1 if the crawler should cycle repeatedly, N if it should cycle N times, 0 if it should only refresh without completing a cycle.
     */
    public void crawl (final int cycles) {
        crawl(cycles, 0);
    }

    /** 
     * Does the actual crawl.
     * @param cycles -1 if the crawler should cycle repeatedly, N if it should cycle N times, 0 if it should only refresh without completing a cycle.
     * @param skip the number of pages to skip from the pagedb in the first cycle. This is for resuming interrupted crawls.
     */
    public void crawl (final int cycles, long skip) {
        try {
            StopMonitor.reset();
            FetchdataProcessor processor = new FetchdataProcessor();
            if (distributed) {
                pageCatcher = new PageCatcher("catch");
            }
            int cyclesCounter = 0;
            final boolean createNewPageDB;
            if (cycles == 0 )
                createNewPageDB = false;
            else
                createNewPageDB = true;

            while (running()) {

                // start queues
                if (fetchlistQueue.isClosed()) fetchlistQueue.reset();
                if (fetchdataQueue.isClosed()) fetchdataQueue.reset();

                PageDB newPageDB = runSingleCrawlCicle(processor, createNewPageDB, skip);
                skip = 0;

                logger.info("Finished crawl cycle");

                cyclesCounter++;
                if (cycles == 0 || (cycles > 0 && cycles == cyclesCounter)) {
                    stopCrawler();
                }

                // TODO: this should be done by the Hounder indexer, not the crawler.
                if (running()) {
                    if (shouldOptimizeAfterCycle(newPageDB.getCycles())) {
                        processor.optimizeIndex();
                    }
                }
            }

        } catch (Exception e) {
            logger.error("ABORTING CRAWLER: " + e, e);
        }
        if (running()) {
            stopCrawler();
        }
        logger.info("Stopped.");
    }


    /**
     * Helper method for crawl().
     * Runs a single crawl cycle and returns the retulting PageDB
     * @param processor
     * @param createNewPageDB if set to true, a normal crawl cicle is performed, generating a
     *  new pagedb with the new crawled data (already sorted and deduped). If set to false, the
     *  crawel data is passed through the pipeline, but it is not stored in a new pageDB. This
     *  mode may be useful to process al the pages stored in a pagedb (for example to index
     *  them).
     * @param skip the number of pages to skip from the pagedb in the first cycle. This is for resuming interrupted crawls.
     * @return An updated pagedb. Sorted and deduped. In case the createNewPageDB param was set
     *  to false, the returned pageDB is empty.
     * @throws Exception
     * @throws IOException
     * @throws InterruptedException
     * @throws MalformedURLException
     */
    private PageDB runSingleCrawlCicle(FetchdataProcessor processor, boolean createNewPageDB, long skip) throws Exception, IOException, InterruptedException, MalformedURLException {
        // start the fetch server
        FetchServer fetchserver = new FetchServer(fetcher, this);
        fetchserver.start();

        // prepare the pagedbs
        PageDB oldPageDB = new PageDB(pagedbDir);
        PageDB tmpPageDB;
        PageDB newPageDB;
        if (createNewPageDB) {
            if (distributed) {
                tmpPageDB = new DPageDB(pagedbDir + ".tmp", pageCatcher);
            } else {
                tmpPageDB = new PageDB(pagedbDir + ".tmp");
            }
            newPageDB = new PageDB(pagedbDir + ".new");
        } else {
            tmpPageDB = new NoPageDB();
            newPageDB = new NoPageDB();
        }
        // delete any leftover dirs
        newPageDB.deleteDir();
        tmpPageDB.deleteDir();
        // prepare the new pagedb
        oldPageDB.open(PageDB.READ);
        tmpPageDB.open(PageDB.WRITE);
        tmpPageDB.setNextCycleOf(oldPageDB);

        logger.info("Starting crawl cycle " + oldPageDB.getCycles());
        declareStartCycle(oldPageDB);

        FetchlistQueueMonitor fetchlistFeeder = new FetchlistQueueMonitor(oldPageDB, tmpPageDB, skip);
        InjectedFetchlistQueueMonitor injectedFetchlistFeeder = new InjectedFetchlistQueueMonitor(tmpPageDB);
        FetchdataQueueMonitor fetchdataConsumer = new FetchdataQueueMonitor(oldPageDB, tmpPageDB, processor);
        cycleFinished = false;
        fetchlistFeeder.start();
        injectedFetchlistFeeder.start();
        fetchdataConsumer.start();

        // Wait until the fetchlist and fetchdata threads are done
        synchronized(cycleFinishedMonitor) {
            while (running() && !cycleFinished) {
                logger.debug("Waiting: running="+running()+" cycleFinished="+cycleFinished+" fetchList="+fetchlistQueue.size()+" injectedFetchList="+injectedFetchlistQueue.size()+" fetchData="+fetchdataQueue.size());
                cycleFinishedMonitor.wait(60000); // wake up every minute or when the cycle finishes
            }
        }
        logger.debug("Waiting no more: running="+running()+" cycleFinished="+cycleFinished+" fetchList="+fetchlistQueue.size()+" injectedFetchList="+injectedFetchlistQueue.size()+" fetchData="+fetchdataQueue.size());

        if (running()) {

            logger.debug("Closing old and temporary pagedbs");
            oldPageDB.close();
            tmpPageDB.close(); 
            logger.debug("Old and temporary pagedbs closed");

            if (createNewPageDB) {
                // dedup & trim
                new PageDBTrimmer().trimPageDB (tmpPageDB, newPageDB, discoveryPages);

                if (running()) {
                    // TODO overwrite old with new
                    boolean ok = false;
                    String oldName = oldPageDB.getDir();
                    if (attempt(tmpPageDB.deleteDir(), "deleting pagedb.tmp")) {
                        if (attempt(oldPageDB.rename(tmpPageDB.getDir()), "renaming pagedb -> pagedb.tmp")) {
                            if (attempt(newPageDB.rename(oldName), "renaming pagedb.new -> pagedb")) {
                                if (attempt(tmpPageDB.deleteDir(), "deleting pagedb.tmp (2)")) {
                                    ok = true;
                                }
                            }
                        }
                    }
                    if (ok) {
                        declareEndCycle(newPageDB);
                    } else {
                        stopCrawler();
                    }
                }
            }
        } else {
            logger.info("Stopping, not closing or trimming the temporary pagedbs");
        }
        return newPageDB;
    }

    // Helper method, allows to write cleaner code for situations
    // where several operations which may fail depend on each other.
    private boolean attempt (boolean ok, String msg) {
        if (!ok) {
            logger.error(msg);
        }
        return ok;
    }


    // Show help.
    private static void usage (String msg) {
        System.out.println();
        System.out.println(msg);
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  Crawler                (crawls one cycle)");
        System.out.println("  Crawler runforever     (crawls until stopped)");
        System.out.println("  Crawler cycles=<N>     (crawls N cycles)");
        System.out.println();
        System.exit(1);
    }


    /**
     * Starts the crawler. 
     */
    public static void main (String args[]) throws Exception { 
        String log4jConfigPath = com.flaptor.util.FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            logger.warn("log4j.properties not found on classpath!");
        }
        Config config = Config.getConfig("crawler.properties");
        String mode = config.getString("crawler.mode").toLowerCase();

        Crawler crawler = new Crawler();
        
        int skip = config.getInt("crawler.mode.skip"); 
        int cycles = config.getInt("crawler.mode.cycles");
        
        if (mode.equals("runforever")) {
        	crawler.crawl(-1, skip);
        } else if (mode.equals("cycles")) {
            crawler.crawl(config.getInt("crawler.mode.cycles"), skip);
        } else if (mode.equals("refresh")) {
            crawler.refresh(skip);
        } else if (mode.equals("redistribute")) {
            crawler.redistribute();
        }
    }
}
