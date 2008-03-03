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
package com.flaptor.search4j.crawler.pagedb;

import java.io.File;
import java.io.PrintStream;
import java.util.Date;
import java.util.Random;

import org.apache.log4j.PropertyConfigurator;

import com.flaptor.search4j.crawler.PageRank;
import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class PageDBTest extends TestCase {

    PageDB pagedb;
    Random rnd;
    PrintStream origOut;
    

    public void setUp() {
        String log4jConfigPath = com.flaptor.util.FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            System.err.println("log4j.properties not found on classpath!");
        }
        rnd = new Random(new Date().getTime());
        origOut = System.err;
        try {
            System.setErr(new PrintStream(new File("/tmp/test_stderr")));
        } catch (Exception e) {}
    }

    public void tearDown() {
        System.setErr(origOut);
        FileUtil.deleteDir("/tmp/testdb");
    }

    private void create() throws Exception {
        pagedb = new PageDB("/tmp/testdb");
        pagedb.open(PageDB.WRITE);
        pagedb.setPriorityScope(PageDB.ALL_PAGES);
    }

    private void open() throws Exception {
        pagedb.open(PageDB.READ);
    }

    private void close() throws Exception {
//System.out.print("TIME: ");
//        long start = System.currentTimeMillis();
        pagedb.close();
//        long stop = System.currentTimeMillis();
//System.out.println((stop-start)+" millis");
    }

    private void del() {
        boolean ok = pagedb.deleteDir();
        assertTrue("couldn't delete pagedb dir", ok);
    }

    private void add (String str) throws Exception {
        String url = "http://" + str + ".com";
        Page page = new Page(url, 0f);
        page.setDistance(0);
        page.setRetries(0);
        page.setLastAttempt(0L);
        page.setLastSuccess(0L);
        page.setScore(PageRank.INITIAL_SCORE);
        page.setPriority(rnd.nextFloat());
        pagedb.addPage(page);
    }

    private String randomString() {
        StringBuffer buf = new StringBuffer();
        int len = 30 + rnd.nextInt(30);
        for (int i=0; i<len; i++) {
            char c = (char) ('a' + rnd.nextInt('z'-'a'));
            buf.append(c);
        }
        return buf.toString();
    }

    private String numToString(long val) {
        String pad = "000000000000000000";
        String str = String.valueOf(val);
        return pad.substring(0,pad.length()-str.length()) + str;
    }

    private void build(int size, boolean ordered) throws Exception {
// System.out.println("SIZE="+size);
        long t1 = new Date().getTime();
        create();
        String url = randomString();
        for (int i=1; i<=size; i++) {
            if (ordered) {
                add(url + numToString(i));
            } else {
                add(url);
                url = randomString();
            }
        }
        close();
        checkSize(size);
        checkOrder();
        checkPriority();
        del();
    }

    private void checkSize (long size) throws Exception {
        long n = pagedb.getSize();
        assertTrue("PageDB should be " + size + " pages long, but is " + n + " pages long", n == size);
    }

    private void checkOrder() throws Exception {
        open();
        String maxMd5 = "";
        for (Page page : pagedb) {
            String md5 = page.getUrlHash();
            assertTrue ("Pages out of MD5 order", md5.compareTo(maxMd5) >= 0);
            maxMd5 = md5;
        }
        close();
    }

    private void checkPriority() throws Exception {
        open();
        int size = (int)pagedb.getSize();
        if (size > 100) {
            float[] prio = new float[size];
            int i = 0;
            for (Page page : pagedb) {
                prio[i++] = page.getPriority();
            }
            java.util.Arrays.sort(prio);
            for (i = 0; i < size; i+=size/100) {
                int per = (i * 100) / size;
                float val = pagedb.getPriorityThreshold(per);
                float ref = prio[i];
                assertTrue("The priority threshold returned by the pagedb does not match the real data", Math.abs(val-ref) < 0.05f);
            }
        }
        close();
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testPageDB() throws Exception {
        boolean sorted = true;
        build(1,sorted);
        build(1000,sorted);
    }


}

