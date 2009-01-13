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

import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import org.apache.log4j.Logger;

/**
 * Keeps stats about the progress of the crawler and reports them to a file.
 * @author jorge
 */
public class CrawlerProgress {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private long cycle;
    private long tosee;
    private long tofetch;
    private long tosort;
    private long tomerge;
    private long totrim;
    private long seen;
    private long fetched;
    private long processed;
    private long sorted;
    private long merged;
    private long trimmed;
    private long now;
    private long[] startTime;
    private long[] endTime;
    private File reportFile;
    private File baseFile;
    private int stage;
    private final static int START = 0;
    private final static int FETCH = 1;
    private final static int SORT = 2;
    private final static int MERGE = 3;
    private final static int TRIM = 4;
    private final static int STOP = 5;

    public CrawlerProgress(long cycle) {
        this.cycle = cycle;
        stage = 0;
        tosee = 0;
        seen = 0;
        fetched = 0;
        processed = 0;
        sorted = 0;
        trimmed = 0;
        Config config = Config.getConfig("crawler.properties");
        String baseFileName = config.getString("progress.report.filename");
        baseFile = new File(baseFileName);
        reportFile = new File(baseFileName+"."+cycle);
        startTime = new long[5];
        startTime[START] = System.currentTimeMillis(); 
        endTime = new long[5];
    }
    
    public void startFetch(long max, long known) {
        Config config = Config.getConfig("crawler.properties");
        int refetchPercent = config.getInt("priority.percentile.to.fetch");
        tosee = max > 0 ? max : 1;
        tofetch = (max-known)+known*refetchPercent/100;
        stage = FETCH;
        startTime[stage] = System.currentTimeMillis(); 
    }

    public void startSort(long max) {
        tosort = max > 0 ? max : 1;
        stage = SORT;
        startTime[stage] = System.currentTimeMillis(); 
        endTime[stage-1] = startTime[stage];
    }

    public void startMerge(long max) {
        tomerge = max > 0 ? max : 1;
        stage = MERGE;
        startTime[stage] = System.currentTimeMillis(); 
        endTime[stage-1] = startTime[stage];
    }

    public void startTrim(long max) {
        totrim = max > 0 ? max : 1;
        stage = TRIM;
        startTime[stage] = System.currentTimeMillis(); 
        endTime[stage-1] = startTime[stage];
    }

    private void stop() {
        stage = STOP;
        endTime[stage-1] = System.currentTimeMillis();
        report();
    }


    public void addSeen(long seen) {
        this.seen += seen;
    }
    
    public void addFetched(long fetched) {
        this.fetched += fetched;
        if (this.fetched > tofetch) tofetch = this.fetched;
    }
    
    public void addProcessed(long processed) {
        this.processed += processed;
    }
    
    public void addSorted(long sorted) {
        this.sorted += sorted;
    }
    
    public void addMerged(long merged) {
        this.merged += merged;
    }
    
    public void addTrimmed(long trimmed) {
        this.trimmed += trimmed;
    }
    

    private String formatTime(long time, boolean absolute) {
        if (time < 0) {
            return "unknown";
        } else {
            if (absolute) {
                return new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(time);
            } else {
                long seconds = time / 1000;
                long minutes = seconds / 60;
                seconds -= minutes * 60;
                long hours = minutes / 60;
                minutes -= hours * 60;
                long days = hours / 24;
                hours -= days * 24;
                return (days > 0 ? days + " days, " : "") + 
                       (hours > 0 ? hours + " hours, " : "") + 
                       minutes + " minutes";
            }
        }
    }
    
    public void report() {
        if (stage > STOP) return;
        now = System.currentTimeMillis();
        long elapsed = now - startTime[START];
        if (0 == elapsed) { elapsed = 1; }
        BufferedWriter buf = null;
        try {
            buf = new BufferedWriter(new FileWriter(reportFile));
            buf.write("Cycle: "+cycle+"                                                            ");
            buf.newLine();
            buf.write("Start: "+formatTime(startTime[START],true)+"                                ");
            buf.newLine();
            buf.write("Now:   "+formatTime(now,true));
            buf.newLine();
            buf.write("Elapsed: "+formatTime(elapsed,false)+"                               ");
            buf.newLine();
            buf.write("PageDB: "+tosee+" docs (fetching "+tofetch+")                                 ");
            buf.newLine();
            showFetch(buf);
            showSort(buf);
            showMerge(buf);
            showTrim(buf);
            buf.write("                                            ");
            buf.newLine();
        } catch (Exception ex) {
            logger.error("While writing to the crawler progress report file:", ex);
        } finally {
            Execute.close(buf);
        }
    }

    private void showFetch(BufferedWriter buf) throws IOException {
        if (stage == FETCH) {
            buf.write("Fetch: ");
            buf.write("seen " + seen + " (" + (100 * seen / tosee) + "%) - ");
            buf.write("fetched " + fetched + " (" + (100 * fetched / tofetch) + "%) - ");
            buf.write("processed " + processed + " (" + (100 * processed / tofetch) + "%)");
            buf.write("                                            ");
            buf.newLine();
            showProgress(buf,processed,tofetch);
        } else if (stage > FETCH) {
            buf.write("Fetch: ");
            showFinished(buf, processed, FETCH);
        }
    }

    private void showSort(BufferedWriter buf) throws IOException {
        if (stage == SORT) {
            buf.write("Sort:  ");
            buf.write(sorted + " (" + (100 * sorted / tosort) + "%)");
            buf.write("                                            ");
            buf.newLine();
            showProgress(buf,sorted,tosort);
        } else if (stage > SORT) {
            buf.write("Sort:  ");
            showFinished(buf, sorted, SORT);
        }
    }

    private void showMerge(BufferedWriter buf) throws IOException {
        if (stage == MERGE) {
            buf.write("Merge: ");
            buf.write(merged + " (" + (100 * merged / tomerge) + "%)");
            buf.write("                                            ");
            buf.newLine();
            showProgress(buf,merged,tomerge);
        } else if (stage > MERGE) {
            buf.write("Merge: ");
            showFinished(buf, merged, MERGE);
        }
    }
    
    private void showTrim(BufferedWriter buf) throws IOException {
        if (stage == TRIM) {
            buf.write("Trim:  ");
            buf.write(trimmed + " (" + (100 * trimmed / totrim) + "%)");
            buf.write("                                            ");
            buf.newLine();
            showProgress(buf,trimmed,totrim);
        } else if (stage > TRIM) {
            buf.write("Trim:  ");
            showFinished(buf, trimmed, TRIM);
        }
    }
    
    
    private void showProgress(BufferedWriter buf, long current, long max) throws IOException {
        long elapsed = now - startTime[stage];
        if (0 == elapsed) { elapsed = 1; }
        long remaining = (current > 0) ? ((max * elapsed) / current) - elapsed : -1;
        float rate = ((10000L * current) / elapsed) / 10.0f;
        buf.write("         Elapsed: "+formatTime(elapsed,false)+"                               ");
        buf.newLine();
        buf.write("         Remaining: "+formatTime(remaining,false)+"                           ");
        buf.newLine();
        buf.write("         Rate: "+rate+" docs/s"+"                                             ");
        buf.newLine();
    }

    private void showFinished(BufferedWriter buf, long max, int stage) throws IOException {
        long elapsed = endTime[stage] - startTime[stage];
        if (0 == elapsed) { elapsed = 1; }
        float rate = ((10000L * max) / elapsed) / 10.0f;
        buf.write(max+" docs in "+formatTime(elapsed,false)+" ("+rate+" docs/s)");
        buf.write("                                            ");
        buf.newLine();
    }

    public void close() {
        stop();
        if (FileUtil.copyFile(reportFile,baseFile,true)) {
            FileUtil.deleteFile(reportFile.getAbsolutePath());
        }
    }
    
}
