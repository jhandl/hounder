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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.log4j.PropertyConfigurator;

import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.hounder.crawler.pagedb.PageDB;
import com.flaptor.hounder.crawler.pagedb.PageTest;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class PageCatcherTest extends TestCase {

    Random rnd = null;
    PrintStream stdOut;
    PrintStream stdErr;
    String tmpDir;

    public void setUp() throws IOException {
        String log4jConfigPath = com.flaptor.util.FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            System.err.println("log4j.properties not found on classpath!");
        }
        tmpDir = FileUtil.createTempDir("dpagedbtest",".tmp").getAbsolutePath();
        stdOut = System.out;
        stdErr = System.err;
        try {
            System.setErr(new PrintStream(new File(tmpDir+"/test_stderr")));
        } catch (Exception e) {}
        rnd = new Random(System.currentTimeMillis());
    }

    public void tearDown() {
        System.setOut(stdOut);
        System.setErr(stdErr);
        FileUtil.deleteDir(tmpDir);
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testCatcher () throws Exception {
        TestMapper mapper = new TestMapper(null, 0);
        NodeAddress node = new NodeAddress("127.0.0.1",1099);
        ArrayList<NodeAddress> nodes = new ArrayList<NodeAddress>();
        nodes.add(node);
        PageCatcher localCatcher = new PageCatcher(tmpDir+"/catchdb");
        localCatcher.start(node);
        PageDistributor distributor = new PageDistributor(nodes, node, mapper);
        Page page1 = PageTest.randomPage();
        page1.setUrl("http://example.com/test0=0");
        IRemotePageCatcher stubCatcher= distributor.getCatcher(page1);
        stubCatcher.addPage(page1);
        PageDB db = localCatcher.getCatch();
        db.open(PageDB.READ);
        Iterator<Page> pages = db.iterator();
        assertTrue("The page sent through rmi did not survive the adventure.", pages.hasNext());
        Page page2 = pages.next();
        assertTrue("The page has been changed by the trip through rmi:\n  1: "+page1+"\n  2: "+page2, page1.equals(page2));
        assertFalse("Sent one page through rmi and more than one came out the other end.", pages.hasNext());
        db.close();
    }

}
