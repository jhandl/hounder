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
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.flaptor.hounder.crawler.pagedb.IPageStore;
import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.hounder.crawler.pagedb.PageDB;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;



/**
 * This class produces fetchlists
 * @author Flaptor Development Team
 */
public class FetchlistFactory {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private int fetchlistSize; // the number of web pages to fetch per fetch cycle.
    private int priorityPercentileToFetch; // the percent of pages that will be fetched on each cycle, based on their priority.
    private float priorityThreshold; // if a page has a priority lower than this, it will not be fetched.
    private PageDB pageSourceDB;
    private PageDB pageDestDB;
    private Iterator<Page> pages;


    /** 
     * Class initializer.
     * Prepares the fetchlist factory to work on a new set of pagedbs with a new threshold.
     */
    public FetchlistFactory (PageDB pageSource, PageDB pageDest) throws IOException {
        Config config = Config.getConfig("crawler.properties");
        fetchlistSize = config.getInt("fetchlist.size");
        priorityPercentileToFetch = config.getInt("priority.percentile.to.fetch");
        pageSourceDB = pageSource;
        pageDestDB = pageDest;
        priorityThreshold = pageSourceDB.getPriorityThreshold(100-priorityPercentileToFetch);
        pages = pageSourceDB.iterator();
    }

    /**
     * Returns a new fetchlist.
     * @return a new fetchlist, or null if the sourcePageDB is exhausted.
     */
    public FetchList getNextFetchlist () {
        FetchList fetchlist = null;
        if (pages.hasNext()) {
            fetchlist = new FetchList();
            selectPagesToFetch(fetchlist, pageDestDB);
            fetchlist.close();
        }
        return fetchlist;
    }

    // Decides if a page should be fetched, based on the crawler configuration and page status and information.
    private boolean shouldFetch (Page page) {
        boolean unfetched = (page.getLastSuccess() == 0L) && (page.getRetries() < 3);
        boolean highPriority = (page.getPriority() >= priorityThreshold);
        boolean should = highPriority || unfetched;
        if (logger.isDebugEnabled()) {
          logger.debug("Should Fetch: "+(should?"yes":"no")+"  unfetched="+(unfetched?"yes":"no")+" (tries="+page.getRetries()+")  highPriority="+(highPriority?"yes":"no")+" ("+page.getPriority()+(highPriority?">=":"<")+priorityThreshold+")");
        }
        return should;
    }

    // Split the page enumeration in two groups: one to fetch, one to pass
    private void selectPagesToFetch (IPageStore toFetch, IPageStore toPass) {
        logger.debug("Selecting pages for the fetchlist");
        int pagesAdded = 0;
        while (pagesAdded < fetchlistSize && pages.hasNext()) {
            Page page = pages.next(); 

            // if it is a valid web page (not an image, etc), it is considered, otherwise it is discarded
            if (null == Crawler.urlFilter(page.getUrl())) {
                logger.debug("  discarding page " + page.getUrl() + ", url filters consider it not interesting");
            } else {
                // if the page should be fetched, it goes to the fetchlist, otherwise it is passed
                boolean added = false;
                if (shouldFetch(page)) {
                    try {
                        logger.debug("  adding page " + page.getUrl());
                        toFetch.addPage(page);
                        pagesAdded++;
                        added = true;
                    } catch (IOException e) {
                        logger.error("Trying to add a page to the IPageStore " + toFetch + ": " + e, e);
                    }
                }
                if (!added && null != toPass) {
                    try {
                        logger.debug("  passing page " + page.getUrl());
                        toPass.addPage(page);
                    } catch (IOException e) {
                        logger.error("Trying to add a page to the IPageStore " + toPass + ": " + e, e);
                    }
                }
            }
        }
    }

}

