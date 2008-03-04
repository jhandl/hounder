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
package com.flaptor.hounder.crawler.pagedb.distributed;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Random;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.hounder.crawler.pagedb.PageDB;
import com.flaptor.hounder.crawler.pagedb.PageTest;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class DPageDBTest extends TestCase {

    static final int port1 = 1160;
    static final int port2 = 1161;

    static Logger logger = Logger.getLogger(Execute.whoAmI());
    Random rnd = null;
    PrintStream stdOut;
    PrintStream stdErr;
    String tmpDir;
    Config config;

    public void setUp() throws IOException {
        filterOutputRegex("[0-9 ]*(parsing|Using|impl:|not including|logging|No FS|Plugins|status|[a-z\\.]* = ).*");
        String log4jConfigPath = com.flaptor.util.FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            System.err.println("log4j.properties not found on classpath!");
        }
        tmpDir = FileUtil.createTempDir("dpagedbtest",".tmp").getAbsolutePath();
        stdOut = System.out;
        stdErr = System.err;
        rnd = new Random(System.currentTimeMillis());
        config = Config.getConfig("crawler.properties");
       
        Config.getConfig("common.properties").set("port.offset.pagecatcher.rmi","70");
    }

    public void tearDown() {
        System.setOut(stdOut);
        System.setErr(stdErr);
        FileUtil.deleteDir(tmpDir);
    }


    String[] dbNames = new String[2];
    DPageDB[] dbs = new DPageDB[2];
    volatile int done = 0;

    private class DBCreator extends Thread {
        int id;
        PageCatcher catcher;
        public DBCreator (int id, PageCatcher catcher) {
            this.id = id;
            this.catcher = catcher;
        }
        public void run () {
            try {
                if (null == catcher) {
                    dbs[id] = new DPageDB(dbNames[id]);
                } else {
                    dbs[id] = new DPageDB(dbNames[id], catcher);
                }
                dbs[id].open(DPageDB.WRITE);
                synchronized (dbs) {
                    done++;
                }
            } catch (Exception e) { logger.error(e,e); }
        }
    }

    private class DBCloser extends Thread {
        int id;
        public DBCloser (int id) {
            this.id = id;
        }
        public void run () {
            try {
                dbs[id].close();
                synchronized (dbs) {
                    done++;
                }
            } catch (IOException e) { throw new RuntimeException(e); }
        }
    }

    private void runTwoDPageDBs (PageCatcher[] catchers) throws Exception {
        for (int tst=0; tst<2; tst++) {
            config.set("pagedb.node.mapper", "com.flaptor.hounder.crawler.pagedb.distributed.TestMapper");
            config.set("pagedb.node.list", "127.0.0.1:1090, 127.0.0.1:1091");
            dbs[0] = null;
            dbs[1] = null;
            dbNames[0] = tmpDir+"/db0";
            dbNames[1] = tmpDir+"/db1";
            done = 0;
            new DBCreator(0,catchers[0]).start();
            Execute.sleep(1000);
            config.set("pagedb.node.list", "127.0.0.1:1091, 127.0.0.1:1090");
            new DBCreator(1,catchers[1]).start();
            while (done < 2) Execute.sleep(100);

            Page page00 = PageTest.randomPage();
            Page page01 = PageTest.randomPage();
            Page page10 = PageTest.randomPage();
            Page page11 = PageTest.randomPage();

            page00.setUrl("http://example-"+tst+".com/test0=0");
            page01.setUrl("http://example-"+tst+".com/test0=1");
            page10.setUrl("http://example-"+tst+".com/test1=0");
            page11.setUrl("http://example-"+tst+".com/test1=1");

            dbs[0].addPage(page00);
            dbs[0].addPage(page01);
            dbs[1].addPage(page10);
            dbs[1].addPage(page11);

            Execute.sleep(1000);

            done = 0;
            new DBCloser(0).start();
            new DBCloser(1).start();
            while (done < 2) Execute.sleep(100);

            PageDB db = new PageDB(dbNames[0]);
            db.open(DPageDB.READ);
            Iterator<Page> pages = db.iterator();
            assertTrue("The first distributed pagedb should have two pages, yet it has none.", pages.hasNext());
            Page page1 = pages.next();
            assertTrue("One of the pages in the first distributed pagedb is not expected: "+page1.getUrl(), page1.equals(page00) || page1.equals(page10));
            assertTrue("The first distributed pagedb should have two pages, yet it has one.", pages.hasNext());
            Page page2 = pages.next();
            assertTrue("One of the pages in the first distributed pagedb is not expected: "+page2.getUrl(), page2.equals(page00) || page2.equals(page10));
            assertFalse("One of the pages in the first distributed pagedb has been cloned:"+page1.getUrl(), page1.equals(page2)); 
            assertFalse("The first distributed pagedb should have two pages, yet it has more.", pages.hasNext());
            db.close();

            db = new PageDB(dbNames[1]);
            db.open(DPageDB.READ);
            pages = db.iterator();
            assertTrue("The second distributed pagedb should have two pages, yet it has none.", pages.hasNext());
            page1 = pages.next();
            assertTrue("One of the pages in the second distributed pagedb is not expected: "+page1.getUrl(), page1.equals(page01) || page1.equals(page11));
            assertTrue("The second distributed pagedb should have two pages, yet it has one.", pages.hasNext());
            page2 = pages.next();
            assertTrue("One of the pages in the second distributed pagedb is not expected: "+page2.getUrl(), page2.equals(page01) || page2.equals(page11));
            assertFalse("One of the pages in the second distributed pagedb has been cloned:"+page1.getUrl(), page1.equals(page2)); 
            assertFalse("The second pagedb should have two pages, yet it has more.", pages.hasNext());
            db.close();
        }
    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION,
                requiresPort = {port1,port2})
    public void testIndependentDPageDB () throws Exception {
        runTwoDPageDBs(new PageCatcher[]{null,null});
    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION,
                requiresPort = {1090,1091})
    public void xtestDependentDPageDB () throws Exception {
        PageCatcher catcher0 = new PageCatcher(tmpDir+"/catch0");
        PageCatcher catcher1 = new PageCatcher(tmpDir+"/catch1");
        runTwoDPageDBs(new PageCatcher[]{catcher0,catcher1});
    }

}
