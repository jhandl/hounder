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

import java.util.ArrayList;
import java.util.Random;

import com.flaptor.hist4j.AdaptiveHistogram;

/**
 * @author Flaptor Development Team
 */
public class SimPriority {

    private class Page {
        private int id;
        public float priority = 0f;
        public long lastAttempt = Long.MAX_VALUE;
        public long lastSuccess = Long.MAX_VALUE;
        public long lastChange = Long.MAX_VALUE;
        public float probSuccess = 0f;
        public float probChange = 0f;
        public int attempts = 0;
        public int retries = 0;
        public int changes = 0;
        public Page (int id) { 
            this.id = id;
//            probSuccess = 0.9f + 0.1f * rnd.nextFloat();
            probSuccess = 1;
            probChange = rnd.nextFloat();
//            probChange = 0;
        }
        public long getLastAttempt() {return lastAttempt;}
        public long getLastSuccess() {return lastSuccess;}
        public long getLastChange() {return lastChange;}
        public void setPriority(float p) {priority = p;};
        public void show (boolean mark) {
            System.out.println("Page "+id+": "+(mark?"*":" ")+" retries="+retries+", changes="+changes+", attempts="+attempts+", priority="+Math.round(priority*100f)/100f);
        }
    }


    private ArrayList<Page> pagedb;
    private int maxPages;
    private Random rnd;

    private AdaptiveHistogram histogram;

    private float priorityThreshold = 0;


    public SimPriority (int maxPages) {
        this.maxPages = maxPages;
        rnd = new Random(System.currentTimeMillis());
        pagedb = new ArrayList<Page>();
        for (int i = 0; i < maxPages; i++) {
            Page page = new Page(i);
            pagedb.add(page);
        }
        histogram = new AdaptiveHistogram();
pagedb.get(0).probSuccess = 0.0f;
pagedb.get(0).probChange = 1.0f;
    }

    private void updatePriority (Page page) {
        long now = System.currentTimeMillis();
        long timeSinceLastAttempt = now - page.getLastAttempt();
        long timeSinceLastSuccess = now - page.getLastSuccess();
        long timeSinceLastChange = now - page.getLastChange();
        float timeStatic = timeSinceLastChange - timeSinceLastSuccess;
        float timeDead = timeSinceLastSuccess - timeSinceLastAttempt;

        float priority = timeSinceLastSuccess - timeStatic * 0.4f - timeDead * 2.0f;

        page.setPriority(priority);
    }

    private boolean shouldFetch (Page page) {
        return (page.priority >= priorityThreshold);
    }

    private void selectPagesToFetch (ArrayList<Page> db, ArrayList<Page> toFetch, ArrayList<Page> toPass) {
        for (int i = 0; i < db.size(); i++) {
            Page page = db.get(i);
            if (shouldFetch(page)) {
                toFetch.add(page);
            } else {
                toPass.add(page);
            }
        }
    }

    private void fetch (ArrayList<Page> fetchlist) {
        for (int i = 0; i < fetchlist.size(); i++) {
            long now = System.currentTimeMillis();
            Page page = fetchlist.get(i);
            page.attempts++;
            page.lastAttempt = now;
            if (rnd.nextFloat() <= page.probSuccess || page.attempts == 1) {
                page.lastSuccess = now;
                if (rnd.nextFloat() <= page.probChange || page.attempts == 1) {
                    page.lastChange = now;
                    page.changes++;
                }
            } else {
                page.retries++;
            }
        }
        try { Thread.sleep(rnd.nextInt(10)); } catch (Exception e) {}
    }

    public void crawl () {
        ArrayList<Page> fetchlist = new ArrayList<Page>();
        ArrayList<Page> newDB = new ArrayList<Page>();
        selectPagesToFetch(pagedb, fetchlist, newDB);

        fetch(fetchlist);

        histogram.reset();

        Page page;
        for (int i = 0; i < pagedb.size(); i++) {
            page = pagedb.get(i);
            updatePriority(page);
        }

        priorityThreshold = histogram.getValueForPercentile(90);
//        System.out.print("Threshold=" +priorityThreshold+" ");

        page = pagedb.get(0);
        page.show(fetchlist.contains(page));
    }

    public void show() {
        System.out.println("--------- threshold = " + priorityThreshold);
        for (int i = 0; i < pagedb.size(); i++) {
            Page page = pagedb.get(i);
            page.show(page.priority >= priorityThreshold);
        }
    }

    public static void main (String[] arg) {
        SimPriority sim = new SimPriority(10);
        for (int i = 0; i < 1000; i++) {
            sim.crawl();
        }
//        sim.show();
    }

}

