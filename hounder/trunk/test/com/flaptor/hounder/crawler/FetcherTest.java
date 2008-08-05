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
import java.util.HashMap;
import java.util.Random;

import org.apache.log4j.PropertyConfigurator;

import com.flaptor.hounder.crawler.modules.FetchDocument;
import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.hounder.crawler.pagedb.PageTest;
import com.flaptor.util.Config;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestUtils;
import com.flaptor.util.remote.WebServer;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class FetcherTest extends TestCase {

    Random rnd = null;
    String tmpDir;
    PrintStream stdErr;


    public void setUp() throws IOException {
        String log4jConfigPath = com.flaptor.util.FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            System.err.println("log4j.properties not found on classpath!");
        }
        rnd = new Random(System.currentTimeMillis());
        tmpDir = FileUtil.createTempDir("crawlertest",".tmp").getAbsolutePath();
        stdErr = System.err;
        try {
            System.setErr(new PrintStream(new File(tmpDir+"/test_stderr")));
        } catch (Exception e) {}
    }

    public void tearDown() {
        System.setErr(stdErr);
        FileUtil.deleteDir(tmpDir);
    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION,
            requiresPort = {8086})
    public void testFetcher() throws Exception {
        int size = 10;

        // Build the configured fetcher
        Config config = Config.getConfig("crawler.properties");
        String className = config.getString("fetcher.plugin");
        IFetcher fetcher = (IFetcher)Class.forName(className).getConstructor(new Class[]{}).newInstance(new Object[]{});

        // Create web pages and add them to a fetchlist
        FetchList fetchlist = new FetchList();
        HashMap<String,String> testPages = new HashMap<String,String>();
        for (int i=1; i<=size; i++) { 
            // Create the file
            String url = "http://localhost:8086/test-"+i+".html";
            String text = TestUtils.randomText(25,25);
            TestUtils.writeFile(tmpDir+"/web/test-"+i+".html", text);
            // Add the page to the fetchlist
            Page page = PageTest.randomPage();
            page.setUrl(url);
            fetchlist.addPage(page);
            // Store the data for later recall
            testPages.put(url,text);
        }

        // Start the web server
        WebServer server = new WebServer(8086);
        server.addResourceHandler("/", tmpDir+"/web");
        server.start();

        // Run the fetcher
        FetchData fetchdata = fetcher.fetch(fetchlist);

        // Stop the web server
        server.requestStop();

        // Chech that the page has been fetched
        for (FetchDocument doc : fetchdata) {
            String url = doc.getPage().getUrl();
            assertTrue("Failed to fetch page "+url, doc.success());
            assertTrue("Page "+url+" has been fetched twice", testPages.containsKey(url));
            String text = testPages.remove(url).trim();
            String fetched = (new String(doc.getContent())).trim();
            assertTrue("Fetcher didn't retrieve the correct content for page "+url, text.equals(fetched));
        }
        assertTrue("Fetcher missed "+testPages.size()+" pages: "+testPages.keySet(), testPages.size() == 0);

    }

}

