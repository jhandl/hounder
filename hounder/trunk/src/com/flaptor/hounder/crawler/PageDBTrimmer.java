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

import org.apache.log4j.Logger;

import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.hounder.crawler.pagedb.PageDB;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.TextSignature;



/**
 * This class takes the pagedb that has been produced by the fetch cycles, 
 * which contains pages and outbound links found during the fetch stage,
 * and produces a new pagedb for the next crawl cycle, with no repetitions 
 * and containing only those pages that should be kept according to the 
 * configuration.
 *
 * @todo: All pages disposed here will not be "de-emmitted" (for example deleted from the index).
 * @author Flaptor Development Team
 */
public class PageDBTrimmer {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private UrlPatterns hotspots; // list of grep patterns a url must match to become a hotspot.
    private long discoveryFrontSize; // the number of pages to fetch outside of the hotspot vicinity.
    private boolean stocasticDiscoveryFrontLine; // if true, discovery pages are randomly selected to form the discovery front.
    private int cyclesBetweenDiscoveryWaves; // the number of cycles before the discovery wave is renewed.
    private static int maxDistance; // the distance (in jumps) the crawler will venture from the known hotspots in search for more hotspots.
    private static int[] maxRetries; // number of times a page will be allowed to fail before dropping it.
    private long pagedbSizeLimit; // number of pages that the pagedb cannot exceed.
    static { init(true); }

    /** 
     * Class initializer.
     * Uses the default fetcher and indexer.
     */
    public PageDBTrimmer () throws IOException {
        init(false);
        Config config = Config.getConfig("crawler.properties");
        maxDistance = config.getInt("max.distance");
        hotspots = new UrlPatterns(config.getString("hotspot.file"));
        discoveryFrontSize = config.getLong("discovery.front.size");
        stocasticDiscoveryFrontLine = config.getBoolean("discovery.front.stocastic");
        cyclesBetweenDiscoveryWaves = config.getInt("cycles.between.discovery.waves");
        pagedbSizeLimit = config.getInt("pagedb.size.limit");
    }


    private static void init (boolean echo) {
        maxDistance = Config.getConfig("crawler.properties").getInt("max.distance");
        String[] retries = Config.getConfig("crawler.properties").getString("max.retries").split(",");
        if (retries.length > maxDistance+1 && echo) {
            logger.warn("There are retry limits defined beyond that of maxDistance, they will be ignored");
        }
        if (retries.length == 0 && echo) {
            logger.error("There are no retry limits defined");
        }
        maxRetries = new int[maxDistance+1];
        for (int i=0; i<=maxDistance; i++) {
            if (i < retries.length) {
                maxRetries[i] = Integer.valueOf(retries[i]);
            } else {
                maxRetries[i] = maxRetries[i-1]; // if there are fewer retries defined, repeat the last value.
            }
        }
    }


    /**
	 * Change the page's priority to reflect the latest events (fetched, age, contents change)
     * @todo this should be elsewhere...
	 */
    private void updatePriority (Page page) {
        long now = System.currentTimeMillis();
        long timeSinceLastAttempt = now - page.getLastAttempt();
        long timeSinceLastSuccess = now - page.getLastSuccess();
        long timeSinceLastChange = now - page.getLastChange();
        long timeStatic = timeSinceLastChange - timeSinceLastSuccess;
        long timeDead = timeSinceLastSuccess - timeSinceLastAttempt;
        if (timeDead > timeStatic) {
            timeStatic = timeDead;
        }

        float priority = (timeSinceLastSuccess - timeStatic * 0.4f - timeDead * 2.0f) / 10000f;

// System.out.println(page.getUrl()+"   timeSinceLastSuccess="+timeSinceLastSuccess+" timeStatic="+timeStatic+" timeDead="+timeDead+"    priority="+priority);
        page.setPriority(priority);
    }



    /**
     * Check if the page should be deleted because of too many retries.
     * It must be deleted if it did fail too many times for its distance, as long as
     * there are no links to this page that would bring it back after it was deleted.
     * @param page the page to be analized.
     */
    public static boolean tooManyRetries (Page page) {
        int retries = page.getRetries();
        int distance = page.getDistance();
        int inlinks = page.getNumInlinks();
        if (distance >= maxRetries.length) return true;
        return retries > maxRetries[distance] && inlinks == 0;
    }



    /**
     * This class implements the pagedb filtering criteria.
     * A page is ok to write only if it doesn't exceed the distance and retry thresholds, 
     * or if it is an unfetched discovery page and the quota for discovery pages 
     * has not been exceeded.
     */
    private class PageFilter {

        private int maxDistance;
        private int[] maxRetries;
        private long dbFetchedSize;
        private long discoveryFrontSize;
        private long availableDiscoveryPages;
        private long discoveryPagesWritten;
        private long unfetchedPagesWritten;
        private float discoveryFrontRatio;
        private java.util.Random rnd;

        // Constructor
        public PageFilter (int maxDistance, int[] maxRetries, long dbFetchedSize, long discoveryFrontSize, long availableDiscoveryPages) {
            this.maxDistance = maxDistance;
            this.maxRetries = maxRetries;
            this.dbFetchedSize = dbFetchedSize;
            this.discoveryFrontSize = discoveryFrontSize;
            this.availableDiscoveryPages = availableDiscoveryPages;
            if (availableDiscoveryPages > 0) {
                if (stocasticDiscoveryFrontLine) {
                    discoveryFrontRatio = (float)discoveryFrontSize / (float)availableDiscoveryPages;
                } else {
                    discoveryFrontRatio = 1.0f;
                }
            } else {
                discoveryFrontRatio = 1.0f;
            }
            discoveryPagesWritten = 0;
            unfetchedPagesWritten = 0;
            rnd = new java.util.Random(System.currentTimeMillis());
        }

        // Determine if a page if ok to write.
        public boolean shouldWrite (PageDB pagedb, Page page) throws IOException {
            boolean okToWrite = false;
            String motive = null; // this will hold an explanation if the page is rejected, for debugging
            String url = page.getUrl();
            long lastSuccess = page.getLastSuccess();

            if (lastSuccess > 0 || pagedbSizeLimit == 0 || dbFetchedSize + unfetchedPagesWritten <= pagedbSizeLimit) { 
                // the number of pages written is within the imposed limit

                // get the page properties
                int distance = page.getDistance();
                int retries = page.getRetries();
                float score = page.getScore();
                boolean addAll = (discoveryFrontRatio >= 1.0f);

                // If the cycle number of the destination pagedb is a multiple of the wave period, we must start a new wave.
                // We add (or substract, depending on which side of the equation we are) 1 because the dest pagedb has the 
                // cycle number already incremented, we never get to see cycle number 0 at this point.
                boolean startingNewDiscoveryWave;
                if (0 == cyclesBetweenDiscoveryWaves) {
                    startingNewDiscoveryWave = (pagedb.getCycles() == maxDistance+1); 
                } else {
                    startingNewDiscoveryWave = ((pagedb.getCycles()-maxDistance-1) % cyclesBetweenDiscoveryWaves == 0); 
                }

                if (distance <= maxDistance) { // the page is within the allowed distance from a hotspot
                    if (!tooManyRetries(page)) { // the page has not exceeded the number of retries for its distance and it has no inlinks.
                        if (distance > 0 || hotspots.match(url)) { // if the page was a hotspot, it still is (needed for trimmer.main)
                            if (Crawler.urlFilter(url) != null) { // matches url regex filter file
                                okToWrite = true;
                            } else motive = "discarded by url regex file";
                        } else motive = "no longer a hotspot";
                    } else motive = "too many retries "+retries+">"+maxRetries[distance]+" and no inlinks";
                } else { // it is outside of the hostpot vicinity
                    if (discoveryFrontSize > 0) { // there is a discovery frontier defined
                        if (lastSuccess == 0L) { // it has not yet been fetched
                            if (retries == 0) { // it didn't fail (the goal is to travel the web fast, if a page fails, we move on)
                                if (startingNewDiscoveryWave ? distance == maxDistance + 1 : distance > maxDistance + 1) { 
                                    // if we are starting a new wave, fetch the pages just outside of the vicinity (the wave birthline)
                                    // otherwise, we only fetch pages that are not in the wave birthline (so we don't create a new wave with each cycle)
                                    if (discoveryPagesWritten < discoveryFrontSize) { // there is room for more discovery pages
                                        if (addAll || rnd.nextFloat() <= discoveryFrontRatio) { // this page has K/N chances of getting in (K=max, N=available)
                                            if (Crawler.urlFilter(url) != null) { // matches url regex filter file
                                                okToWrite = true;
                                                discoveryPagesWritten++;
                                            } else motive = "discarded by url regex file";
                                        } else motive = "discovery page wasn't lucky enough to make it to the front";
                                    } else motive = "too many discovery pages written "+discoveryPagesWritten+">"+discoveryFrontSize;
                                } else motive = startingNewDiscoveryWave ? "not at the birthline (dist="+(maxDistance+1)+") when we want to start a new wave" 
                                    : "at the birthline (dist="+(maxDistance+1)+") when we don't want to start a new wave";
                            } else motive = "too many retries "+retries+">"+maxRetries[maxDistance];
                        } else motive = "discovery page already fetched";
                    } else motive = "too distant from a hotspot and no discovery front allowed "+distance+">"+maxDistance;
                }

                if (0 == lastSuccess && okToWrite) {
                    unfetchedPagesWritten++;
                }

            } else motive = "PageDB size limit reached";

            if (okToWrite) {
                logger.debug("      OK to write page " + url);
            } else {
                logger.debug("      NOT OK to write page " + url + ", " + motive);
            }
            return okToWrite;
        }

    }

    /**
     * Read the origin pagedb and write a new destination pagedb getting 
     * rid of duplicates, pages that are too distant or unresponsive.
     * @param origPageDB the intput pagedb.
     * @param destPageDB the output pagedb that will hold the trimmed result.
     * @param availableDiscoveryPages the number of discovery pages present in the input pagedb.
     */ 
    public void trimPageDB (PageDB origPageDB, PageDB destPageDB, long availableDiscoveryPages) throws IOException, MalformedURLException {
        Page bestPage;
        bestPage = new Page("",0);
        bestPage.setDistance(0);
        bestPage.setRetries(0);
        bestPage.setLastAttempt(0);
        bestPage.setLastSuccess(0);
        bestPage.setLastChange(0);
        bestPage.setPriority(-Float.MAX_VALUE);
        bestPage.setEmitted(false);
        bestPage.setSignature(new TextSignature(""));
        boolean unfetched = false;
        int inlinks = 0;


        logger.debug("Trimming the pagedb");
        origPageDB.open(PageDB.READ);
        destPageDB.open(PageDB.WRITE + PageDB.UNSORTED);
        destPageDB.setSameCycleAs(origPageDB);
        long dbSize = origPageDB.getSize();
        long dbFetchedSize = origPageDB.getFetchedSize();
        logger.debug("  cycle="+origPageDB.getCycles()+" size="+dbSize);

        PageRank pageRank = new PageRank(dbSize);
        PageFilter pageFilter = new PageFilter(maxDistance, maxRetries, dbFetchedSize, discoveryFrontSize, availableDiscoveryPages);

        // This code produces one page for each block of same-url pages.
        // The produced page has the best properties of the block,
        // Unfetched pages in the block contribute to the pagerank of the 
        // resulting page. If there are no unfetched pages in the block, 
        // the fetched page is simply copied.

        for (Page page : origPageDB) {

            if (!Crawler.running()) break;

            // get data for this page
            String url = page.getUrl();
            logger.debug("  reading page " + url);
            long lastAttempt = page.getLastAttempt();
            long lastSuccess = page.getLastSuccess();
            long lastChange = page.getLastChange();

            if (url.equals(bestPage.getUrl())) { // both pages have the same url
                logger.debug("    same page, will keep reading.");

                // add the anchor to the list of anchors of this page
                bestPage.addAnchors(page.getAnchors());
                // add the urls of the pages linking to this one
                bestPage.addParents(page.getParents());

                // if this page has not been fetched, mark the block as unfetched, 
                // add its score to the block score and count it as an incomming link
                if (lastSuccess == 0L) {
                    unfetched = true;
                    pageRank.addContribution(page.getScore());
                    inlinks++;
                }

                // keep the shortest distance
                int distance = page.getDistance();
                if (distance < bestPage.getDistance()) { 
                    bestPage.setDistance(distance);
                }

                // keep the latest fetch
                if (lastAttempt > bestPage.getLastAttempt()) { 
                    bestPage.setLastAttempt(lastAttempt);
                }

                // keep the latest success
                if (lastSuccess > bestPage.getLastSuccess()) { 
                    bestPage.setLastSuccess(lastSuccess);
                }

                // keep the latest change
                if (lastChange > bestPage.getLastChange()) { 
                    bestPage.setLastChange(lastChange);
                }

                // keep the least retries
                int retries = page.getRetries();
                if (lastSuccess < lastAttempt || lastSuccess == 0) { 
                    // if this page has not been successfuly fetched keep the most retries 
                    // (one will be for the actual attempt, the rest will be unattempted links)
                    if (retries > bestPage.getRetries()) {
                        bestPage.setRetries(retries);
                    }
                }

                // keep the old priority, hash and emitted
                if (lastSuccess > 0) {
                    bestPage.setSignature(page.getSignature());
                    bestPage.setEmitted(page.isEmitted());
                    bestPage.setPriority(page.getPriority());
                }

            } else { // The page is not a duplicate

                if (bestPage.getUrl().length() > 0) { // if this is not the first page, write the best of the last similar pages
                    logger.debug("    new page, will write previous one: " + bestPage.getUrl());
                    bestPage.setNumInlinks(inlinks);
                    if (unfetched) bestPage.setScore(pageRank.getPageScore());
                    if (pageFilter.shouldWrite (destPageDB, bestPage)) {
                        updatePriority(bestPage);
                        destPageDB.addPage(bestPage);
                    }
                } 
                // this is a new page, record its properties
                bestPage.setUrl(page.getUrl());
                bestPage.setDistance(page.getDistance());
                bestPage.setLastAttempt(page.getLastAttempt());
                bestPage.setLastSuccess(page.getLastSuccess());
                bestPage.setLastChange(page.getLastChange());
                bestPage.setRetries(page.getRetries());
                bestPage.setAnchors(page.getAnchors());
                bestPage.setParents(page.getParents());
                bestPage.setScore(page.getScore());
                bestPage.setPriority(page.getPriority());
                bestPage.setSignature(page.getSignature());
                bestPage.setEmitted(page.isEmitted());
                unfetched = (bestPage.getLastSuccess() == 0L);
                inlinks = 0;
                pageRank.reset();
                if (unfetched) {
                    pageRank.addContribution(bestPage.getScore());
                    inlinks++;
                }
            }
        }
        if (bestPage.getUrl().length() > 0) { // if the orig pagedb is not empty, write the best of the last similar pages
            logger.debug("    pagedb is over, will write last one: " + bestPage.getUrl());
            bestPage.setNumInlinks(inlinks);
            if (unfetched) bestPage.setScore(pageRank.getPageScore());
            if (pageFilter.shouldWrite (destPageDB, bestPage)) {
                updatePriority(bestPage);
                destPageDB.addPage(bestPage);
            }
        }
        if (Crawler.running()) { // don't waste time closing temporary pagedbs if the system is being stopped
            origPageDB.close();
            destPageDB.close();
        }
        hotspots.close();
    }


}

