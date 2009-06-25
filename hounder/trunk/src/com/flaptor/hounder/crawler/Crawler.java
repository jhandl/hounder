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
import com.flaptor.clusterfest.deploy.DeployListenerImplementation;
import com.flaptor.clusterfest.deploy.DeployModule;
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
import com.flaptor.util.FileUtil;
import com.flaptor.util.PortUtil;
import com.flaptor.util.cache.FileCache;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;

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
    private boolean cycleFinished; // if true, the cycle has finished.
    private Object cycleFinishedMonitor; // used for waking up the crawler when the cycle finished.
    private static StopMonitor stopMonitor; // the monitor for the stop file.
    private boolean distributed; // if true, the underlying pagedb will be distributed.
    private boolean starting; // if true, this is the first cycle since the crawler started.
    private long pagedbSize; // the size of the current pagedb.
    private boolean protectAgainstEmptyPageDB; // abort if the new pagedb would be empty.
    private CrawlerProgress progress; // the progress stats for the current crawl cycle
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
        protectAgainstEmptyPageDB = config.getBoolean("protect.against.empty.pagedb");
        starting = true;
        fetchlistQueue = new CloseableQueue<FetchList>(1); // max one fetchlists in the queue
        injectedFetchlistQueue = new CloseableQueue<FetchList>(); //TODO: put a limit, a large injectdb causes an OutOfMemoryError.
        fetchdataQueue = new CloseableQueue<FetchData>(1); //TODO: analyze if the fetchdata should be written to disk.
        cycleFinishedMonitor = new Object();
        stopMonitor = new StopMonitor("stop");
        urlFilter = new UrlFilter();

    	if (config.getBoolean("clustering.enable")) {
        	int port = PortUtil.getPort("clustering.rpc.crawler");
    		nodeListener = new NodeListener(port, config);
    		MonitorModule.addModuleListener(nodeListener, new CrawlerMonitoredNode(this));
    		ControllerModule.addModuleListener(nodeListener, new ControllableImplementation());
    		nodeListener.addModuleListener("crawlerControl", new CrawlerControllableImplementation());
            DeployModule.addModuleListener(nodeListener, new DeployListenerImplementation());
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
        if (null != nodeListener) {
            nodeListener.requestStop();
            while (!nodeListener.isStopped()) {
                Execute.sleep(100);
            }
        }
        ModulesManager.getInstance().close();
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

        public FetchlistQueueMonitor (PageDB oldPageDB, PageDB tmpPageDB, long skip) {
            try {
                factory = new FetchlistFactory(oldPageDB,tmpPageDB,progress);
            } catch (IOException e) {
                logger.error(e,e);
            }
            this.setName("FetchlistQueueMonitor");
            factory.skip(skip);
        }


        @Override
        public void run () {
            FetchList fetchlist;
            boolean couldEnqueue;
            do {
                fetchlist = factory.getNextFetchlist();

                if (null == fetchlist) {
                    fetchlistQueue.close();
                    logger.debug("No more fetchlists for this cycle.");
                    break;
                } 
                // else
                couldEnqueue = false;
                while (running() && !couldEnqueue && !fetchlistQueue.isClosed()) {
                    couldEnqueue = fetchlistQueue.enqueueBlock(fetchlist,100);
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

        @Override
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
                        factory = new FetchlistFactory(pageDB,tmpPageDB,progress);

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
                            pageDB.deleteDir(true);
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
            this.setName("FetchdataQueueMonitor");
        }

        @Override
        public void run () {
            FetchData fetchdata = null;
            do {
                boolean couldDequeue = false;
                while (!couldDequeue && !fetchdataQueue.isClosed() && running()) {
                    // Try to get a fetchlist from the queue with a small timeout
                    fetchdata = fetchdataQueue.dequeueBlock(100);
                    couldDequeue = (null != fetchdata);
                }

                if (couldDequeue) {
                    try {
                        long discoveredPages = processor.processFetchdata(fetchdata, oldPageDB, tmpPageDB);
                        progress.addProcessed(fetchdata.getSize());
                        progress.addDiscovered(discoveredPages);
                        progress.report();
                    } catch (Exception e) {
                        logger.error(e,e);
                    } catch (Throwable e) {
                        logger.fatal(e,e);
                        System.exit(-1);
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
        progress.addFetched(fetchdata.getSize());
        progress.report();
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
        ModulesManager.getInstance().applyCommand(cmd);
    }

    /**
     * Signals the end of a crawl cycle.
     * @param newPageDB the pagedb that the crawler has created during this cycle.
     */
    public void declareEndCycle (PageDB newPageDB) {
        CommandWithPageDB cmd = new CommandWithPageDB("endCycle",newPageDB);
        ModulesManager.getInstance().applyCommand(cmd);
    }


    /**
     * Submits all fetched pages from the pagedb to the module pipe retrieving the page contents from the cache.
     * It needs to have the cache configured in the modules list.
     * If there is no cache, it falls back to the refetch method.
     */
    public void refresh () {
        boolean hasCache = false;
        IProcessorModule[] caches = ModulesManager.getInstance().getModuleInstances("com.flaptor.hounder.crawler.modules.CacheModule");
        if (caches.length > 0) {
            CacheModule cacheModule = null;
            FileCache<DocumentCacheItem> fileCache = null;
            for (int i=0; i<caches.length && null == fileCache; i++) {
                cacheModule = (CacheModule)caches[i];
                fileCache = cacheModule.getPageCache();
            }
            if (null != fileCache) {
                try {
                    ModulesManager.getInstance().unloadModule(cacheModule);
                    hasCache = true;
                    long seen = 0;
                    PageDB pagedb = new PageDB(pagedbDir);
                    long total = pagedb.getSize();
                    declareStartCycle(pagedb);
                    PageCache pageCache = new PageCache(pagedb, fileCache);
                    FetchdataProcessor processor = new FetchdataProcessor();
                    processor.processFetchdata(pageCache, pagedb, new NoPageDB());
                    pagedb.close();
                    declareEndCycle(pagedb); // Note: normally this would be the newPageDB, but in a refresh there is no such thing.
                    ModulesManager.getInstance().applyCommand("optimize");
                } catch (IOException e) {
                    logger.error(e,e);
                }
            }
        }
        if (!hasCache) {
            crawl(0);
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
                tmpPageDB.deleteDir(false);
                if (attempt(oldPageDB.rename(tmpPageDB.getDir()), "renaming pagedb -> pagedb.tmp")) {
                    if (attempt(newPageDB.rename(oldName), "renaming pagedb.new -> pagedb")) {
                        if (attempt(tmpPageDB.deleteDir(false), "deleting pagedb.tmp")) {
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
     * Delete all temporary files and directories.
     * Usefull for cleanup after testing.
     */
    public void cleanup() {
        CrawlerProgress.cleanup();
        PageDB tmpPageDB;
        if (distributed) {
            tmpPageDB = new DPageDB(pagedbDir + ".tmp", pageCatcher);
        } else {
            tmpPageDB = new PageDB(pagedbDir + ".tmp");
        }
        PageDB newPageDB = new PageDB(pagedbDir + ".new");
        tmpPageDB.deleteDir(true);
        newPageDB.deleteDir(true);
    }

    /** 
     * Runs the crawl cycles.
     * @param cycles -1 if the crawler should cycle repeatedly, N if it should cycle N times, 0 if it should only refresh without completing a cycle.
     */
    public void crawl (final int cycles) {
        try {
            StopMonitor.reset();
            FetchdataProcessor processor = new FetchdataProcessor();
            if (distributed) {
                pageCatcher = new PageCatcher("catch");
            }
            int cyclesCounter = 0;
            boolean createNewPageDB = (cycles != 0);

            while (running()) {

                // start queues
                if (fetchlistQueue.isClosed()) fetchlistQueue.reset();
                if (fetchdataQueue.isClosed()) fetchdataQueue.reset();
                
                // run crawl cycle
                PageDB newPageDB = runSingleCrawlCycle(processor, createNewPageDB);

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
        stopCrawler();
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
    @SuppressWarnings("fallthrough")
    private PageDB runSingleCrawlCycle(FetchdataProcessor processor, boolean createNewPageDB) throws Exception, IOException, InterruptedException, MalformedURLException {
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
//                if (starting) {
//                    logger.info("Waiting for other nodes to start...");
//                    ((DPageDB)tmpPageDB).synch();
//                    logger.info("All nodes started");
//                    starting = false;
//                }
            } else {
                tmpPageDB = new PageDB(pagedbDir + ".tmp");
            }
            newPageDB = new PageDB(pagedbDir + ".new");
        } else {
            tmpPageDB = new NoPageDB();
            newPageDB = new NoPageDB();
        }
        // delete leftover new pagedb
        newPageDB.deleteDir(false);
        // prepare the new pagedb
        oldPageDB.open(PageDB.READ);

        // Crawl recovery attempt
        boolean skipFetch = false;
        long skip = 0;
        
        progress = CrawlerProgress.restartCrawlerProgress();
        if (null != progress) { 
            // the previous cycle was interrupted
            switch (progress.stage()) {
                case CrawlerProgress.START:
                case CrawlerProgress.STOP:
                    logger.info("Last crawler state is either before starting or after finishing, will start next cycle.");
                    progress = null;
                    break;
                case CrawlerProgress.FETCH:
                    if ((progress.cycle() == oldPageDB.getCycles() + 1)) {
                        skip = progress.processed();
                        logger.info("Crawler was interrupted while fetching at cycle "+progress.cycle()+", will continue current cycle skipping "+skip+" docs.");
                        tmpPageDB.open(PageDB.WRITE + PageDB.APPEND);
                    } else {
                        logger.info("Last crawler report inconsistent with pagedb state, will restart.");
                        progress = null;
                    }
                    break;
                case CrawlerProgress.SORT:
                    // fall through
                case CrawlerProgress.MERGE:
                    logger.info("Crawler was interrupted while sorting at cycle "+progress.cycle()+", will continue current cycle.");
                    tmpPageDB.open(PageDB.WRITE + PageDB.APPEND); // this will force a sort upon closing
                    // fall through
                case CrawlerProgress.TRIM:
                    if (progress.stage() == CrawlerProgress.TRIM) {
                        logger.info("Crawler was interrupted while trimming at cycle "+progress.cycle()+", will continue current cycle.");
                    }
                    skipFetch = true;
                    break;
                default:
                    logger.error("Unknown crawler state report, will restart.");
                    progress = null;
                    break;
            }
        }
        if (null == progress) { 
            // there was no interrupted previous cycle or it was inconsistent
            tmpPageDB.deleteDir(false);
            tmpPageDB.open(PageDB.WRITE);
            tmpPageDB.setNextCycleOf(oldPageDB);
            progress = new CrawlerProgress(tmpPageDB.getCycles());
        }
        
        tmpPageDB.setProgressHandler(progress);
        if (!skipFetch) {
            if (0 == skip) {
                progress.startFetch(oldPageDB.getSize(), oldPageDB.getFetchedSize());
                logger.info("Starting crawl cycle " + (oldPageDB.getCycles()+1));
            } else {
                logger.info("Continuing crawl cycle " + (oldPageDB.getCycles()+1));
            }
            declareStartCycle(oldPageDB);

            FetchlistQueueMonitor fetchlistFeeder = new FetchlistQueueMonitor(oldPageDB, tmpPageDB, skip);
            InjectedFetchlistQueueMonitor injectedFetchlistFeeder = new InjectedFetchlistQueueMonitor(tmpPageDB);
            FetchdataQueueMonitor fetchdataConsumer = new FetchdataQueueMonitor(oldPageDB, tmpPageDB, processor);
            cycleFinished = false;
            fetchlistFeeder.start();
            injectedFetchlistFeeder.start();
            fetchdataConsumer.start();

            // This is where the main thread spends its time while the crawl cycle takes place.
            // Wait until the fetchlist and fetchdata threads are done
            synchronized(cycleFinishedMonitor) {
                while (running() && !cycleFinished) {
                    logger.debug("Waiting: running="+running()+" cycleFinished="+cycleFinished+" fetchList="+fetchlistQueue.size()+" injectedFetchList="+injectedFetchlistQueue.size()+" fetchData="+fetchdataQueue.size());
                    cycleFinishedMonitor.wait(60000); // wake up every minute or when the cycle finishes
                }
            }
            logger.debug("Waiting no more: running="+running()+" cycleFinished="+cycleFinished+" fetchList="+fetchlistQueue.size()+" injectedFetchList="+injectedFetchlistQueue.size()+" fetchData="+fetchdataQueue.size());
        }
        
        if (running()) {
            progress.report();
            logger.debug("Closing old and temporary pagedbs");
            oldPageDB.close();
            tmpPageDB.close(); 
            logger.debug("Old and temporary pagedbs closed");

            if (createNewPageDB) {
                // dedup & trim
                new PageDBTrimmer().trimPageDB(tmpPageDB, newPageDB, progress);

                // check the trimmed pagedb size
                if (protectAgainstEmptyPageDB && newPageDB.getSize() == 0) {
                    logger.error("The new PageDB is empty, will stop the crawler before replacing the old PageDB. Please check the hotspots, modules and other settings before restarting.");
                    stopCrawler();
                }
                
                if (running()) {
                    boolean ok = false;
                    String oldName = oldPageDB.getDir();
                    if (attempt(tmpPageDB.deleteDir(false), "deleting pagedb.tmp")) {
                        if (attempt(oldPageDB.rename(tmpPageDB.getDir()), "renaming pagedb -> pagedb.tmp")) {
                            if (attempt(newPageDB.rename(oldName), "renaming pagedb.new -> pagedb")) {
                                if (attempt(tmpPageDB.deleteDir(false), "deleting pagedb.tmp (2)")) {
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
            progress.close();
        } else {
            oldPageDB.abort();
            tmpPageDB.abort();
            logger.info("Stopping, not closing or trimming the temporary pagedb");
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


    /**
     * Starts the crawler. 
     */
    public static void main (String args[]) throws Exception { 
        String log4jConfigPath = FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            logger.warn("log4j.properties not found on classpath!");
        }
        Config config = Config.getConfig("crawler.properties");
        String mode = config.getString("crawler.mode").toLowerCase();

        Crawler crawler = new Crawler();
        
        int cycles = config.getInt("crawler.mode.cycles");
        
        if (mode.equals("runforever")) {
        	crawler.crawl(-1);
        } else if (mode.equals("cycles")) {
            crawler.crawl(config.getInt("crawler.mode.cycles"));
        } else if (mode.equals("refresh")) {
            crawler.refresh();
        } else if (mode.equals("redistribute")) {
            crawler.redistribute();
        }
    }
}
