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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;
import java.util.Arrays;

import org.apache.log4j.PropertyConfigurator;

import com.flaptor.util.FileUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;
import com.flaptor.util.TestUtils;
import com.flaptor.util.TextSignature;

/**
 * @author Flaptor Development Team
 */
public class PageTest extends TestCase {

    Random rnd;
    String tmpDir;
    

    public void setUp() {
        String log4jConfigPath = com.flaptor.util.FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            System.err.println("log4j.properties not found on classpath!");
        }
        tmpDir = FileUtil.createTempDir("crawlertest",".tmp").getAbsolutePath();
        rnd = new Random(System.currentTimeMillis());
    }

    public void tearDown() {
        FileUtil.deleteDir(tmpDir);
    }


    public static Page randomPage () {
        Random rnd = new Random(System.currentTimeMillis());
        Page page = null;
        boolean ok = false;
        while (!ok) {
            try {
                page = new Page("",0.1f+rnd.nextFloat());
                page.setUrl("http://"+TestUtils.randomText(2,4).trim().replace(' ','.')+"/"+TestUtils.randomText(0,4).replace(' ','/'));
                page.setDistance(rnd.nextInt(100));
                page.setRetries(rnd.nextInt(10));
                page.setPriority(rnd.nextFloat());
                page.setLastAttempt(Math.abs(rnd.nextLong()));
                long succ = (rnd.nextFloat() < 0.7f) ? Math.abs(rnd.nextLong()) : 0;
                page.setLastSuccess(succ);
                if (succ > 0) {
                    page.setLastChange(Math.abs(rnd.nextLong()));
                    page.setSignature(new TextSignature(TestUtils.randomText(100,100)));
                    page.setScore(rnd.nextFloat());
                    page.setEmitted(rnd.nextBoolean());
                    page.setLocal(rnd.nextBoolean());
                    page.setNumInlinks(rnd.nextInt(20));
                }
                page.setAnchors(TestUtils.randomText(0,5).split(" "));
                page.setParents(TestUtils.randomText(0,5).split(" "));
                ok = true;
            } catch (Exception e) { System.err.println(e); }
        }
        return page;
    }

    private boolean identical(Page one, Page two) {
        if (null == one && null == two) return true;
        if (null == one || null == two) { System.out.println("null"); return false; }
        if (!one.getUrl().equals(two.getUrl())) { System.out.println("url"); return false; }
        if (one.getScore() != two.getScore()) { System.out.println("score"); return false; }
        if (one.getPriority() != two.getPriority()) { System.out.println("priority"); return false; }
        if (one.getDistance() != two.getDistance()) { System.out.println("distance"); return false; }
        if (one.getRetries() != two.getRetries()) { System.out.println("retries"); return false; }
        if (one.getLastAttempt() != two.getLastAttempt()) { System.out.println("lastAttempt"); return false; }
        if (one.getLastSuccess() != two.getLastSuccess()) { System.out.println("lastSuccess"); return false; }
        if (one.getLastChange() != two.getLastChange()) { System.out.println("lastChange"); return false; }
        if (one.isLocal() != two.isLocal()) { System.out.println("isLocal"); return false; }
        if (one.isEmitted() != two.isEmitted()) { System.out.println("emitted"); return false; }
        if (one.getNumInlinks() != two.getNumInlinks()) { System.out.println("numInlinks"); return false; }
        if (!one.getUrlHash().equals(two.getUrlHash())) { System.out.println("urlhash"); return false; }
        String[] anchors1 = one.getAnchors();
        String[] anchors2 = two.getAnchors();
        Arrays.sort(anchors1);
        Arrays.sort(anchors2);
        if (!Arrays.equals(anchors1,anchors2)) { System.out.println("anchors"); return false; }
        String[] parents1 = one.getAnchors();
        String[] parents2 = two.getAnchors();
        Arrays.sort(parents1);
        Arrays.sort(parents2);
        if (!Arrays.equals(parents1,parents2)) { System.out.println("parents"); return false; }
        if (!one.getSignature().equals(two.getSignature())) { System.out.println("signature"); return false; }
        return true;
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testPageSerialization() throws Exception {
        for (int i=0; i<1000; i++) {
            ByteArrayOutputStream buff = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(buff);
            Page page1 = randomPage();
            out.writeObject(page1);
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buff.toByteArray()));
            Page page2 = (Page)in.readObject();
            if (!identical(page1,page2)) {
                System.out.println("Before: "+page1+"\nAfter: "+page2);
                fail("The page was changed by the serialization / deserialization process");
            }
        }
    }

    private long freeMem() {
        try {
            int max = Integer.MAX_VALUE - 1;
            int oneMeg = 1024 * 1024;
            byte[][] fill = new byte[max][];
            for (int i=0; i<max; i++) {
                fill[i] = new byte[oneMeg];
            }
        } catch (OutOfMemoryError e) {
            Runtime.getRuntime().gc();
        }
        return Runtime.getRuntime().freeMemory();
    }

    private float megas(long bytes) {
        return Math.round(bytes/1024/102.4f)/10.0f;
    }

    public void xtestMemoryLeak() throws Exception {
        int size = 100000;
        long start, pre, end;
        File file = new File(tmpDir, "pages");

        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
        start = freeMem();
        for (int i=0; i<size; i++) {
            Page page = randomPage();
            page.write(out);
            out.reset();
        }
        end = freeMem();
        out.close();
        assertTrue("The serialization process has a memory leak", end > start * 0.9);

        ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
        start = freeMem();
        for (int i=0; i<size; i++) {
            Page page = Page.read(in);
        }
        end = freeMem();
        in.close();
        assertTrue("The deserealization process has a memory leak", end > start * 0.9);
    }


}

