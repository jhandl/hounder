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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.flaptor.hounder.crawler.modules.CommandWithDoc;
import com.flaptor.hounder.crawler.modules.FetchDocument;
import com.flaptor.hounder.crawler.modules.ModulesManager;
import com.flaptor.hounder.crawler.pagedb.Link;
import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.hounder.crawler.pagedb.PageDB;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;



/**
 * This class processes the results of a fetch.
 * @author Flaptor Development Team
 */
public class FetchdataProcessor {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private int maxDistance; // the distance (in jumps) the crawler will venture from the known hotspots in search for more hotspots.
    private UrlPatterns hotspots; // list of grep patterns a url must match to become a hotspot.
    private ModulesManager modules; // this holds the modules that will process the crawled pages.
    private boolean recordParents; // if true the urls of the pages linking to each page will be recorded.
    private final String IS_HOTSPOT; // the tag used to mark a doc as a hotspot.
    private ExecutorService pool;
    private final int workerCount;
    private long discoveryPages;

    /** 
     * Class initializer.
     */
    public FetchdataProcessor () throws IOException {
        Config config = Config.getConfig("crawler.properties");
        maxDistance = config.getInt("max.distance");
        IS_HOTSPOT = config.getString("hotspot.tag");
        hotspots = new UrlPatterns(config.getString("hotspot.file"));
        recordParents = config.getBoolean("record.parents");
        modules = ModulesManager.getInstance();

        int cpus = Runtime.getRuntime().availableProcessors();
        workerCount = (int)Math.ceil(cpus * config.getFloat("workers.per.cpu"));
        logger.info("FetchDocument processor using "+workerCount+" workers on "+cpus+" cpus");
    }

    private /*synchronized */void newDiscoveryPage () {
        discoveryPages++;
    }

    public void optimizeIndex (){
        // Forward command to modules manager.
        modules.applyCommand("optimize");
    }


    /** 
     * Go through the fetched data, index it if needed, and store the outlinks for the next cycle.
     * Return the number of available discovery front pages.
     *
     * @todo check if the presence of a frontline should be checked 
     * before setting the distance of hotspot outlinks to 0.
     *
     * @todo: If a page no longer matches the nutch regex files, a delete command will not be sent to 
     * the modules pipe. Also, if the UrlPattern files (hotspots, blacklist, whitelist, etc) are changed 
     * during the crawl cycle so that a page is no longer a hotspot, all pages that were considered 
     * hotspots earlier in the cycle will be dumped without sending a delete command to the document pipe.
     */
    public synchronized long processFetchdata (Iterable<FetchDocument> fetchdata, PageDB oldPageDB, PageDB newPageDB) throws IOException {
        // Create new pool to process.
        int queueLen = workerCount * 10;
        ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(queueLen);
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
        pool = new ThreadPoolExecutor(queueLen,queueLen,0,TimeUnit.SECONDS,queue,handler);
        // set discoveryPages to 0, as there is a new "batch"
        discoveryPages = 0;
        logger.debug("Processing the fetched data");

        Iterator<FetchDocument> iter = fetchdata.iterator();
        int submitted = 0;


        // Feed the thread pool queue.
        while (iter.hasNext() && Crawler.running()) {
            FetchDocument doc = iter.next();
            Runnable processorJob = new ProcessorJob(doc,oldPageDB,newPageDB);
            pool.execute(processorJob);
            submitted++;
        }

        // So the fetchdata ended, or the crawler is no longer
        // running.
        if (Crawler.running()) {
            pool.shutdown();
            while (!pool.isTerminated()) {
                Execute.sleep(50);
            }
        } else {
            // wait no more
            pool.shutdownNow();
            logger.debug("Ending process because crawler is no longer running.");
        }

        return discoveryPages;
    }
        



    private class ProcessorJob implements Runnable {

        private final FetchDocument doc;
        private final PageDB oldPageDB;
        private final PageDB newPageDB;

        public ProcessorJob (FetchDocument doc,PageDB oldPageDB, PageDB newPageDB) {
            this.doc = doc;
            this.oldPageDB = oldPageDB;
            this.newPageDB = newPageDB;
        }


        public void run () {

            try {
                boolean wasHotspot = false;
                Page page = doc.getPage();
                String pageurl = page.getUrl();

                Link[] links = doc.getLinks();
                boolean success = doc.success() && (doc.getText().length() > 0 || links.length > 0 || page.getAnchors().length > 0);

                if (!success) { // the page could not be fetched

                    logger.debug("  page " + pageurl + " could not be fetched");
                    boolean keep = false;
                    if (doc.recoverable()) {
                        page.setRetries(page.getRetries() + 1);
                        if (!PageDBTrimmer.tooManyRetries(page)) {
                            keep = true;
                        }
                    } else {
                        logger.debug("  discarding page " + pageurl);
                    }

                    if (keep) {
                        // if the page is to be kept, store it
                        newPageDB.addPage(page);
                    } else {
                        // otherwise announce it to the modules so they can take appropiate action
                        CommandWithDoc cmd = new CommandWithDoc("delete", doc);
                        modules.applyCommand(cmd);
                    }

                } else { // the page could be fetched.

                    page.setRetries(0); // the page has been successfully fetched, so no retries.
                    if (page.getLastSuccess() == 0) {
                        page.setLastChange(page.getLastAttempt()); // first fetch is considered a change
                    }
                    page.setLastSuccess(page.getLastAttempt()); // this is the time of the last successful fetch: now

                    if (hotspots.match(pageurl)) {
                        doc.setTag(IS_HOTSPOT);
                        wasHotspot = true;
                    }

                    // send it to modules manager
                    modules.process(doc);

                    // Now add the page's outlinks to the next pagedb, 
                    // so they can be fetched in the next cycle
                    if (links.length == 0) {
                        // We need to avoid dangling nodes. 
                        // A simple way is to add a link to itself
                        links = new Link[1];
                        links[0] = new Link(pageurl, "");
                    }
                    for (Link link : links) {
                        try {
                            if (! (page.getDistance() > maxDistance && pageurl.equals(link.getUrl()))) { // dont add self-links in a discovery front page
                                if (Crawler.urlFilter(link.getUrl()) != null) { // if the url is a valid web page (not an image, etc)
                                    logger.debug("    Adding link to " + link + " to the pagedb");
                                    Page child = new Page(link.getUrl(), 1.0f);
                                    child.setRetries(0);
                                    child.setLastAttempt(0L);
                                    child.setLastSuccess(0L);
                                    if (recordParents) child.addParent(pageurl);
                                    child.addAnchor(link.getAnchor()); // at this point it can only be one anchor
                                    child.setScore(PageRank.parentContribution(page.getScore(), links.length));
                                    // unless the child is a hotspot, it is removed from the fetched page by 1 level
                                    child.setDistance(page.getDistance() + 1);

                                    if ( ! hotspots.matchAll() && (maxDistance == 0)) {
                                        // If hotspots is "*", all links are set at distance>0 and the trimmer 
                                        // will keep those that make it into the front line.
                                        // If hotspots restricts the crawl and maxDistance is 0, we want to make 
                                        // sure the distance is 0 when the child is a hotspot, so it will not be 
                                        // dropped by the trimmer.
                                        // TODO: check if this is true, or if the presence of a frontline should 
                                        // be checked before setting distances to 0.
                                        if (hotspots.match(link.getUrl())) {
                                            child.setDistance(0);
                                            logger.debug("    child hotspot: "+link);
                                        } else {
                                            logger.debug("    child not hotspot: "+link);
                                        }

                                    }
                                    newPageDB.addPage(child); 
                                    if (child.getDistance() > maxDistance) {
                                        newDiscoveryPage();
                                    }
                                } else {
                                    logger.debug("    Dropping uninteresting url " + link);
                                }
                            }
                        } catch (MalformedURLException e) {
                            logger.warn("Processing page outlinks: " + e, e);
                        }
                    }
                    

                    if (doc.hasTag(IS_HOTSPOT)) {
                        page.setDistance(0); // the distance of this page to a hotspot is 0.
                    } else {
                        // if this page is not a hotspot (either by the regex file or because the modules said so)
                        // we place this page at the discovery front line. If any other page links to it, it will 
                        // reaquire its original distance during the trimm process. Otherwise, it really doesn't 
                        // matter what its original distance was.
                        page.setDistance(maxDistance + 2); // +1 would be the birthline
                    }
                    logger.debug("  Adding fetched page " + pageurl + " to the pagedb");
                    newPageDB.addPage(page);
                }

            } catch (IOException e) {
                logger.error(e,e);
                throw new RuntimeException(e);
            }
        }
    }


}

