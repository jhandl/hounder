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
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.PropertyConfigurator;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;

import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.hounder.crawler.pagedb.PageDB;
import com.flaptor.hounder.crawler.pagedb.PageTest;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;
import com.flaptor.util.TestUtils;
import com.flaptor.util.remote.WebServer;

/**
 * @author Flaptor Development Team
 */
public class CrawlerTest extends TestCase {

    Random rnd = null;
    PrintStream stdOut;
    PrintStream stdErr;
    Config config;
    String tmpDir;

    boolean testingPageRetention = true;
    boolean testingLinkFollowing = true;
    boolean testingRandomWebs = true;
    boolean testingVerticalCrawl = true;
    boolean testingRandomCrawl = true;
    boolean testingFailedFetcher = true;
    boolean testingPriorityCrawl = true;
    boolean testingPageRank = true;
    boolean testingPageDBInjection = true;

    int repetitions = 10;

    public void setUp() throws IOException {
        String log4jConfigPath = com.flaptor.util.FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            System.err.println("log4j.properties not found on classpath!");
        }
        rnd = new Random(System.currentTimeMillis());
        tmpDir = FileUtil.createTempDir("crawlertest",".tmp").getAbsolutePath();
        stdOut = System.out;
        stdErr = System.err;
        try {
            System.setErr(new PrintStream(new File(tmpDir+"/test_stderr")));
        } catch (Exception e) {}

        config = Config.getConfig("crawler.properties");
        config.set("pagedb.dir", tmpDir+"/testdb");
        config.set("injected.pagedb.dir", tmpDir+"/injectdb");
        config.set("fetchlist.dir", tmpDir+"/testsegments");
        config.set("hotspot.file", tmpDir+"/testhotspot");
        config.set("fetchlist.size", "10");
        config.set("discovery.front.size", "0");
        config.set("discovery.front.stocastic", "false");
        config.set("cycles.between.discovery.waves", "0");
        config.set("priority.percentile.to.fetch", "100");
        config.set("max.retries", "5");
        config.set("categories", "");
        config.set("modules", "com.flaptor.hounder.crawler.modules.WhiteListModule,whitelist|com.flaptor.hounder.crawler.modules.IndexerModule,indexer");
        config.set("hotspot.tag","TAG_IS_HOTSPOT");
        config.set("emitdoc.tag","TAG_IS_INDEXABLE");
        config.set("page.similarity.threshold","1");
        config.set("pagedb.is.distributed","false");
        config.set("clustering.enable","false");

        TestUtils.writeFile(tmpDir+"/testhotspot", "*");

        Config indexerModuleConfig = Config.getConfig("indexerModule.properties");
        indexerModuleConfig.set("use.mock.indexer","yes");
        Config whitelistModuleConfig = Config.getConfig("whitelistModule.properties");
        // USE SAME FILE AS FOR HOTSPOTS .. EVERYTHING IS INDEXED
        whitelistModuleConfig.set("whitelist.file",tmpDir+"/testhotspot");
        whitelistModuleConfig.set("on.true.set.tags","TAG_IS_INDEXABLE");

    }

    public void tearDown() {
        System.setOut(stdOut);
        System.setErr(stdErr);
        FileUtil.deleteDir(tmpDir);
    }

    

    private void preparePageDB (SimWeb web, int startPage) throws IOException {
        preparePageDB(web,startPage,"/testdb");
    }

    private void prepareInjectedPageDB (SimWeb web, int startPage) throws IOException{
        preparePageDB(web,startPage,"/tmpinjectdb");
        FileUtil.rename(tmpDir+"/tmpinjectdb", tmpDir+"/injectdb");
    }

    private void preparePageDB (SimWeb web, int startPage, String dirname) throws IOException{
        TestUtils.writeFile(tmpDir+"/testurls", SimWeb.pageToUrl(startPage));
        PageDB.main(new String[] {"create", tmpDir+dirname, tmpDir+"/testurls", "-q"});
    }

    private void preparePageDB (SimWeb web) throws IOException {
        int startPage = web.getStartPage();
        assertTrue("Couldn't build a connected web", startPage>=0);
        preparePageDB (web, startPage);
    }


    private long pageDBSize () throws IOException {
        PageDB db = new PageDB(tmpDir+"/testdb");
        long size = db.getSize();
        db.close();
        return size;
    }

    private Set<String> pageDBlist () throws Exception {
        PageDB db = new PageDB(tmpDir+"/testdb");
        db.open(PageDB.READ);
        Set<String> res = new HashSet<String>();
        for (Page page : db) {
            String url = page.getUrl();
            String[] part = url.split("[0-9]");
            int start = part[0].length();
            int end = url.length() - part[part.length-1].length();
            String id = url.substring(start,end);
            res.add(id);
        }
        db.close();
        return res;
    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION,
        requiresPort = {8086})
    public void testPageRetention () throws Exception {
        if (!testingPageRetention) return;
        System.out.println("...Testing page retention");
        String url = "http://localhost:8086/test.html";
        TestUtils.writeFile(tmpDir+"/testhotspot", url);
        config.set("max.distance", "0");
        config.set("discovery.front.size", "0");
        config.set("keep.original.url.on.redirect", "true");

        TestUtils.writeFile(tmpDir+"/web/test.html", "<html><head><title>title</title></head><body>"+TestUtils.randomText(25,25)+"</body></html>");
        WebServer server = new WebServer(8086);
        server.addResourceHandler("/", tmpDir+"/web");
        server.start();

        Page out, in;
        in = PageTest.randomPage();
        in.setUrl(url);

        PageDB db = new PageDB(tmpDir+"/testdb");
        db.open(PageDB.WRITE);
        db.addPage(in);
        db.close();

        Crawler crawler = new Crawler();

        int tries = 0;
        int maxTries = 10;
        do {
            tries++;

            crawler.crawl(1);

            db.open(PageDB.READ);
            Iterator<Page> pages = db.iterator();
            assertTrue("The crawler lost or discarded the test page", pages.hasNext());
            out = pages.next();
            assertFalse("The crawler has more than the test page", pages.hasNext());
            db.close();
        } while (out.getRetries() > 0 && tries <= maxTries);

        server.requestStop();
        while (! server.isStopped()) {
            Execute.sleep(20);
        }

        assertTrue("Test page url changed", in.getUrl().equals(out.getUrl()));
        assertTrue("Test hotspot page distance is not 0", out.getDistance() == 0);
        assertTrue("Test page retries is not 0", out.getRetries() == 0);
        assertTrue("Test page fetch time is off by more than one minute", Math.abs(System.currentTimeMillis() - out.getLastAttempt()) <= 1000*60*60);
        assertTrue("Test page success time is off by more than one minute", Math.abs(System.currentTimeMillis() - out.getLastSuccess()) <= 1000*60*60);
        assertTrue("Test page change time is off by more than one minute", Math.abs(System.currentTimeMillis() - out.getLastChange()) <= 1000*60*60);
        assertTrue("Test page score changed", in.getScore() == out.getScore());
        assertTrue("Test page url hash changed", in.getUrlHash().equals(out.getUrlHash()));
        assertTrue("Test page content hash did not change", ! in.getSignature().equals(out.getSignature()));
        assertTrue("Test page doesn't know it has been emitted", out.isEmitted());
        String[] anchorsIn = in.getAnchors();
        String[] anchorsOut = out.getAnchors();
        Arrays.sort(anchorsIn);
        Arrays.sort(anchorsOut);
        assertTrue("Test page anchors changed (in=["+Arrays.toString(anchorsIn)+"] out=["+Arrays.toString(anchorsOut)+"])", Arrays.equals(anchorsIn, anchorsOut));
    }


    @TestInfo(testType = TestInfo.TestType.SYSTEM,
            requiresPort = {8086})
    public void testLinkFollowing () throws Exception {
        if (!testingLinkFollowing) return;
        System.out.println("...Testing link following");
        String url = "http://localhost:8086/one.html";
        TestUtils.writeFile(tmpDir+"/testhotspot", "http:// | .*");
        config.set("max.distance", "0");
        config.set("discovery.front.size", "0");
        config.set("keep.original.url.on.redirect", "true");

        TestUtils.writeFile(tmpDir+"/web/one.html", "<a href='page two.html?a <b> c#d'>two</a>");
        TestUtils.writeFile(tmpDir+"/web/page two.html", "content");
        WebServer server = new WebServer(8086);
        server.addResourceHandler("/", tmpDir+"/web");
        server.start();

        Page in, one, two;
        in = PageTest.randomPage();
        in.setUrl(url);

        PageDB db = new PageDB(tmpDir+"/testdb");
        db.open(PageDB.WRITE);
        db.addPage(in);
        db.close();

        Crawler crawler = new Crawler();

        crawler.crawl(2);

        db.open(PageDB.READ);
        Iterator<Page> pages = db.iterator();
        assertTrue("The crawler lost or discarded all test pages", pages.hasNext());
        one = pages.next();
        assertTrue("The crawler lost or discarded the second test page", pages.hasNext());
        two = pages.next();
        assertFalse("The crawler has more than two pages", pages.hasNext());
        db.close();

        server.requestStop();
        while (!server.isStopped()) {
            Execute.sleep(20);
        }

        assertTrue("Failed in fetching both test pages", (one.getLastSuccess() > 0) && (two.getLastSuccess() > 0));

    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testRandomWebs () throws Exception {
        if (!testingRandomWebs) return;
        System.out.println("...Testing random webs");
        int tests = repetitions;
        for (int test=1; test<=tests; test++) {
            int size = 2+rnd.nextInt(15);
            config.set("discovery.front.size", String.valueOf(size));
            SimWeb web = new SimWeb(size);
            web.randomLinks();
            preparePageDB(web);
            SimFetcher fetcher = new SimFetcher(web);
            Crawler crawler = new Crawler(fetcher);
            long dbsize = 0;
            long reached = 0;
            boolean completed = false;
            int cycles = size;
            for (int c=0; c<cycles && !completed; c++) {
                crawler.crawl(1);
                dbsize = pageDBSize();
                reached = web.countReached();
                if (reached == dbsize && dbsize == size) { // all the web pages are in the db and have been fetched
                    completed = true;
                }
            }
            assertTrue("The crawl did not reach the " + size + " pages after " + cycles + " cycles", reached == dbsize && dbsize == size);
        }
    }

    private void runVerticalCrawl (String name, int[] links, int frontSize, int waveFreq, int[] expectedReach, int[] hotspots) throws Exception {
        System.out.println("...Testing vertical crawl: "+name);
        String hotspotSpec = "";
        if (hotspots.length > 0 && hotspots[0] == -1) {
            hotspotSpec = "*";
        } else {
            String sep = "";
            for (int page : hotspots) {
                hotspotSpec += sep + "http://page." + page + ".test.com/";
                sep="\n";
            }
        }
        TestUtils.writeFile(tmpDir+"/testhotspot", hotspotSpec);
        int size = expectedReach.length;
        for (int distance=0; distance<size; distance++) {
            config.set("max.distance", String.valueOf(distance));
            config.set("discovery.front.size", String.valueOf(frontSize));
            config.set("discovery.front.stocastic", "false");
            config.set("cycles.between.discovery.waves", String.valueOf(waveFreq));
// System.out.println("distance="+distance);
            SimWeb web = new SimWeb(size);
            SimFetcher fetcher = new SimFetcher(web);
            Crawler crawler = new Crawler(fetcher);
            if (null != links) {
                for (int i=0; i<links.length-1; i+=2) {
                    web.addLink(links[i], links[i+1]);
                }
            }
// web.show();
            preparePageDB(web, 0);
            crawler.crawl(size);
            long reached = pageDBSize();
            int expected = expectedReach[distance];
// System.out.println("reached "+reached+" expected "+expected);
            assertFalse("Crawl stoped short of reaching the limit of the hotspot neighborhood ("+reached+"<"+expected+")", reached < expected);
            assertFalse("Crawl reached a page outside of the hotspot neighborhood ("+reached+">"+expected+")", reached > expected);
        }
    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testVerticalCrawl () throws Exception {
        if (!testingVerticalCrawl) return;
        runVerticalCrawl("MatchNone", new int[] {0,1,1,2,2,3,3,4}, 0, 0, new int[] {0,0,0,0,0}, new int[] {});
        runVerticalCrawl("MatchOne", new int[] {0,1,1,2,2,3,3,4}, 0, 0, new int[] {1,2,3,4,5}, new int[] {0});
        runVerticalCrawl("MatchTwo", new int[] {0,1,1,2,2,3,3,4}, 0, 0, new int[] {2,3,4,5,5}, new int[] {0,1});
        runVerticalCrawl("MatchAll_NoWave", new int[] {0,1,1,2,2,3,3,4}, 0, 0, new int[] {1,4,5,5,5}, new int[] {-1});
        runVerticalCrawl("MatchAll_OneWave_Size1", new int[] {0,1,1,2,2,3,3,4}, 1, 0, new int[] {5,5,5,5,5}, new int[] {-1});
        runVerticalCrawl("MatchOne_TwoPaths", new int[] {0,1,0,2,1,2,2,3,3,4}, 0, 0, new int[] {1,3,4,5,5}, new int[] {0});
        runVerticalCrawl("MatchTwo_OneWave_Size1", new int[] {0,1,1,2,2,3,3,4}, 1, 0, new int[] {2,3,4,5,5}, new int[] {0,4});
        runVerticalCrawl("MatchTwo_TwoWaves_Size1_Freq3", new int[] {0,1,1,2,2,3,3,4}, 1, 3, new int[] {3,4,4,5,5}, new int[] {0,4});
        runVerticalCrawl("MatchThree_Branch_OneWave_Size1", new int[] {0,1,1,2,1,4,2,3,4,5}, 1, 0, new int[] {2,3,5,6,6,6}, new int[] {0,3,5});
        runVerticalCrawl("MatchThree_Branch_OneWave_Size2", new int[] {0,1,1,2,1,4,2,3,4,5}, 2, 0, new int[] {3,4,6,6,6,6}, new int[] {0,3,5});
        runVerticalCrawl("MatchThree_Branch_TwoWaves_Size2_Freq3", new int[] {0,1,1,2,1,4,2,3,4,5}, 2, 3, new int[] {3,4,6,6,6,6}, new int[] {0,3,5});
        runVerticalCrawl("MatchThree_Branch_TwoWaves_Size2_Freq4", new int[] {0,1,1,2,1,4,2,3,4,5}, 2, 4, new int[] {5,6,6,6,6,6}, new int[] {0,3,5});
    }

    private void runRandomCrawl (int size, int frontSize, int iterations) throws Exception {
        TestUtils.writeFile(tmpDir+"/testhotspot", "http://page.0.test.com/");
        config.set("max.distance", "0");
        config.set("discovery.front.size", String.valueOf(frontSize));
        config.set("discovery.front.stocastic", "true");
        config.set("cycles.between.discovery.waves", "0");
        Set<String> reachedSet = new HashSet<String>();
        for (int iter=0; iter<iterations; iter++) {
            SimWeb web = new SimWeb(size);
            SimFetcher fetcher = new SimFetcher(web);
            Crawler crawler = new Crawler(fetcher);
            web.fullLinks();
            preparePageDB(web, 0);
            crawler.crawl(1);
            reachedSet.addAll(pageDBlist());
        }
        assertTrue("The random walk did not reach different nodes each time", reachedSet.size() > frontSize + 1);
        assertTrue("The random walk did not reach most of the web", reachedSet.size() > size * 0.75);
    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testRandomCrawl () throws Exception {
        if (!testingRandomCrawl) return;
        System.out.println("...Testing random crawl");
        runRandomCrawl(100, 10, 30);
    }

    private int getPageDBFetchedCount (long[] fetchAttempts) throws Exception {
        int changed = 0;
        PageDB db = new PageDB(tmpDir+"/testdb");
        for (Page page : db) {
            int num = page.getUrl().charAt(26)-'0';
            long attempt = page.getLastAttempt();
            if (attempt != fetchAttempts[num]) {
                fetchAttempts[num] = attempt;
                changed++;
            }
        }
        db.close();
        return changed;
    }

    private float[] getPageDBPriorities () throws Exception {
        PageDB db = new PageDB(tmpDir+"/testdb");
        float[] pri = new float[(int)db.getSize()+1];
        for (Page page : db) {
            int num = page.getUrl().charAt(26)-'0';
            pri[num] = page.getPriority();
        }
        db.close();
        return pri;
    }


    boolean testingTimeout = false;
    int timeoutPage = 0;

    private class myResourceHandler extends ResourceHandler {
        public void handle (String target, javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response, int dispatch) throws IOException, javax.servlet.ServletException {
            if (testingTimeout && target.equals("/page"+timeoutPage+".htm")) {
                response.sendError(javax.servlet.http.HttpServletResponse.SC_REQUEST_TIMEOUT);
            } else {
                super.handle(target, request, response, dispatch);
            }
        }
    }


    @TestInfo(testType = TestInfo.TestType.SYSTEM,
            requiresPort = {8086})
    public void testPriorityCrawl () throws Exception {
        if (!testingPriorityCrawl) return;

        final int TEST_PAGES = 6;
        final int DEAD_PAGE = 6;
        final int CHANGED_PAGE = 5;
        final int PERCENTILE = 50;

        // prepare the pagedb
        TestUtils.writeFile(tmpDir+"/testurls", "http://localhost:8086/page1.htm");
        PageDB.main(new String[] {"create", tmpDir+"/testdb", tmpDir+"/testurls", "-q"});

        // prepare the pages
        for (int i = 1; i <= TEST_PAGES; i++) {
            int next = i % TEST_PAGES + 1;
            TestUtils.writeFile(tmpDir+"/testweb/page"+i+".htm", TestUtils.randomText(5,10)+"<a href='page"+next+".htm'>page"+next+"</a>");
        }

        //FIXME: jetty doesn't stop right after calling stop. Use com.flaptor.webserver instead.
        System.out.println("FIXME:  jetty doesn't stop right after calling stop. Use com.flaptor.webserver instead.");
        // run the web server
        Server server = new Server(8086);
        ResourceHandler resource_handler = new myResourceHandler();
        resource_handler.setResourceBase(tmpDir+"/testweb");
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {resource_handler, new DefaultHandler()});
        server.setHandler(handlers);
        server.start();

        // prepare the crawler
        TestUtils.writeFile(tmpDir+"/testhotspot", "*");
        config.set("max.distance", "0");
        config.set("max.retries", "2");
        config.set("discovery.front.size", "1");
        config.set("discovery.front.stocastic", "false");
        config.set("overall.score.threshold", "0.5");
        config.set("priority.percentile.to.fetch", String.valueOf(PERCENTILE));
        Crawler crawler = new Crawler();

        // run crawl cycles to get all pages
        System.out.println("...Testing priority crawl: partial crawl");
        do {
            crawler.crawl(1);
        } while (pageDBSize() < TEST_PAGES);
        crawler.crawl(1); // get the last links

        // verify that the stipulated proportion of pages are being fetched
        long[] attempts = new long[TEST_PAGES+1];
        getPageDBFetchedCount(attempts); // init the attempts array for later comparison
        for (int i = 0; i < 4; i++) {
            crawler.crawl(1); // run one crawl cycle
            int num = getPageDBFetchedCount(attempts); // count how many pages changed their lastAttempt date
            assertTrue("More than the expected proportion of pages has been fetched", num <= (TEST_PAGES * PERCENTILE / 100) + 1);
            assertTrue("Less than the expected proportion of pages has been fetched", num >= (TEST_PAGES * PERCENTILE / 100) - 1);
        }


        config.set("priority.percentile.to.fetch", "100");
        crawler = new Crawler();

        System.out.println("...Testing priority crawl: dead page");

        // cause a retry
        timeoutPage = DEAD_PAGE;
        testingTimeout = true;

        // run another batch of crawl cycles
        crawler.crawl(4);

        // verify that the dead page has the lowest priority
        float[] pri = getPageDBPriorities();
        for (int i = 1; i <= pageDBSize(); i++) {
            assertTrue("Dead page has higher priority than working page", pri[i] >= pri[DEAD_PAGE]);
        }

        System.out.println("...Testing priority crawl: changed page");
        // cause a change
        TestUtils.writeFile(tmpDir+"/testweb/page"+CHANGED_PAGE+".htm", TestUtils.randomText(5,10)+"<a href='page1.htm'>page1</a>");

        // run another crawl cycle
        crawler.crawl(1);

        // verify that page 4 has the highest priority
        pri = getPageDBPriorities();
        for (int i = 1; i <= pageDBSize(); i++) {
            assertTrue("Changed page has lower priority than static page", pri[i] <= pri[CHANGED_PAGE]);
        }

        server.stop();
    }


    private float[] getPageDBScores (SimWeb web) throws Exception {
        PageDB db = new PageDB(tmpDir+"/testdb");
        float[] score = new float[(int)db.getSize()];
        for (Page page : db) {
            int num = SimWeb.urlToPage(page.getUrl());
            score[num] = page.getScore();
        }
        db.close();
        return score;
    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testPageRank () throws Exception {
        if (!testingPageRank) return;
        System.out.println("...Testing PageRank");

        config.set("max.distance", "0");
        config.set("priority.percentile.to.fetch", "100");
        TestUtils.writeFile(tmpDir+"/testhotspot", "*");
        int tests = 5;
        for (int test = 0; test < tests; test++) {
            int size = 5 + rnd.nextInt(10);
            config.set("discovery.front.size", String.valueOf(size));
            SimWeb web = new SimWeb(size);
            SimFetcher fetcher = new SimFetcher(web);
            Crawler crawler = new Crawler(fetcher);
            for (int to = 0; to < size; to++) {
                for (int from = 0; from < to; from++) {
                    web.addLink(from, to);
                }
                web.addLink(to, to);
            }
            preparePageDB(web, 0);
            crawler.crawl(size*2);
            float[] score = getPageDBScores(web);
            for (int i = 1; i < size; i++) {
                assertTrue("Page with fewer inlinks has a higher score than page with more inlinks", score[i] > score[i-1]);
            }
        }
    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testPageDBInjection() throws Exception{
        if (!testingPageDBInjection) return;

        System.out.println("...Testing pagedb injection");
        int size = 25 + rnd.nextInt(10);
        int cycles = 2;
        config.set("discovery.front.size","35");
        SimWeb web = new SimWeb(size);
        SimSlowFetcher fetcher = new SimSlowFetcher(web,2000);
        for (int i = 1; i < size - cycles; i++ ) {
            web.addLink(0,i);
        }

        preparePageDB(web,0);
        Crawler crawler = new Crawler(fetcher);
        crawler.crawl(1);

        for (int i = 0; i < cycles; i++ ) {
            InjecterThread injecter = new InjecterThread(web,size -1 -i,1000);
            injecter.start();
            crawler.crawl(1);
        }
        assertTrue("injected pages not reached on multiple cycles. Reached="+web.countReached()+ "expected="+size , web.countReached() == size);


        web = new SimWeb(size);
        fetcher = new SimSlowFetcher(web,5000);
        for (int i = 1; i < size - cycles; i++ ) {
            web.addLink(0,i);
        }

        preparePageDB(web,0);
        crawler = new Crawler(fetcher);
        crawler.crawl(1);

        for (int i = 0; i < cycles; i++ ) {
            InjecterThread injecter = new InjecterThread(web,size -1 -i, 1000*i);
            injecter.start();
        }
        crawler.crawl(1);
        assertTrue("injected pages not reached on single cycle with multiple injections. Reached="+web.countReached()+ "expected="+size , web.countReached() == size);


    }

    private class InjecterThread extends TestThread {
        private SimWeb web = null;
        private int startPage = 0;
        private long waitTime = 0;

        public InjecterThread(SimWeb web, int startPage, long waitTime) {
            this.web = web;
            this.startPage = startPage;
            this.waitTime = waitTime;
        }

        public void runTest() {
            try {
                Execute.sleep(waitTime);
                prepareInjectedPageDB(web,startPage );
            } catch (IOException e){
                System.out.println(e);
            }
        }
        public void kill() {};
    }

}

