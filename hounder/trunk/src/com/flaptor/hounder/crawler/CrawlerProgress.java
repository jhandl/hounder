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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import org.apache.log4j.Logger;

/**
 * Keeps stats about the progress of the crawler and reports them to a file.
 * @author jorge
 */
public class CrawlerProgress implements Serializable {

    private static final long serialVersionUID = 1L;
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
    private long discovered;
    private long sorted;
    private long merged;
    private long trimmed;
    private long now;
    private long totalDeadTime;
    private long[] startTime;
    private long[] endTime;
    private long[] deadTime;
    private File reportFile;
    private File baseFile;
    private File binaryFile;
    private int stage;
    public final static int START = 0;
    public final static int FETCH = 1;
    public final static int SORT = 2;
    public final static int MERGE = 3;
    public final static int TRIM = 4;
    public final static int STOP = 5;

  
    /**
     * Constructor. Before reporting progress, a stage has to be started.
     * @param cycle the current crawler cycle.
     */
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
        binaryFile = new File(baseFileName+"-b."+cycle);
        startTime = new long[6];
        startTime[START] = System.currentTimeMillis(); 
        endTime = new long[6];
        deadTime = new long[6];
        totalDeadTime = 0;
    }

    /**
     * Default constructor for deserialization.
     */
    protected CrawlerProgress() {
    }
    
    
    
    /**
     * Marks the start of the fetch stage.
     * @param max number of pages in the old pagedb.
     * @param known number of known pages in the old pagedb.
     */
    public void startFetch(long max, long known) {
        Config config = Config.getConfig("crawler.properties");
        int refetchPercent = config.getInt("priority.percentile.to.fetch");
        tosee = max > 0 ? max : 1;
        tofetch = (max-known)+known*refetchPercent/100;
        if (0 == tofetch) tofetch = 1;
        stage = FETCH;
        startTime[stage] = System.currentTimeMillis(); 
        deadTime[stage] = 0;
    }

    /**
     * Marks the start of the sort stage.
     * @param max Number of records to sort.
     */
    public void startSort(long max) {
        tosort = max > 0 ? max : 1;
        stage = SORT;
        startTime[stage] = System.currentTimeMillis(); 
        deadTime[stage] = 0;
        endTime[stage-1] = startTime[stage];
    }

    /**
     * Marks the start of the merge stage.
     * @param max number of records to merge.
     */
    public void startMerge(long max) {
        tomerge = max > 0 ? max : 1;
        stage = MERGE;
        startTime[stage] = System.currentTimeMillis(); 
        deadTime[stage] = 0;
        endTime[stage-1] = startTime[stage];
    }

    /**
     * Marks the start of the trim stage.
     * @param max number of pages in the pagedb.tmp.
     */
    public void startTrim(long max) {
        totrim = max > 0 ? max : 1;
        stage = TRIM;
        startTime[stage] = System.currentTimeMillis(); 
        deadTime[stage] = 0;
        endTime[stage-1] = startTime[stage];
    }

    /**
     * Marks the end of the crawl cycle.
     */
    private void stop() {
        stage = STOP;
        endTime[stage-1] = System.currentTimeMillis();
        report();
    }

    /**
     * Adds the number of pages that have been read from the old pagedb 
     * since the start of the fetch stage or last call to this method.
     * @param seen
     */
    public void addSeen(long seen) {
        this.seen += seen;
    }
    
    /**
     * Adds the number of pages that have been fetched
     * since the start of the fetch stage or last call to this method.
     * @param fetched
     */
    public void addFetched(long fetched) {
        this.fetched += fetched;
        if (this.fetched > tofetch) tofetch = this.fetched;
    }

    /**
     * Adds the number of pages that have been processed
     * since the start of the fetch stage or last call to this method.
     * @param processed
     */
    public void addProcessed(long processed) {
        this.processed += processed;
    }

    /**
     * Adds the number of pages discovered that where not seen before
     * since the start of the fetch stage or last call to this method.
     * @param discovered
     */
    public void addDiscovered(long discovered) {
        this.discovered += discovered;
    }
    
    /**
     * Adds the number of pages that have been sorted
     * since the start of the sort stage or last call to this method.
     * @param sorted
     */
    public void addSorted(long sorted) {
        this.sorted += sorted;
    }

    /**
     * Adds the number of pages that have been merged
     * since the start of the merge stage or last call to this method.
     * @param merged
     */
    public void addMerged(long merged) {
        this.merged += merged;
    }

    /**
     * Adds the number of pages that have been trimmed
     * since the start of the trim stage or last call to this method.
     * @param trimmed
     */
    public void addTrimmed(long trimmed) {
        this.trimmed += trimmed;
    }
    
    /**
     * Get the current crawl cycle.
     * @return the current crawl cycle.
     */
    public long cycle() {
        return cycle;
    }
    
    /**
     * Get the current stage.
     * @return the current stage.
     */
    public int stage() {
       return stage;
    }
    
    /**
     * Get the current number of processed documents.
     * @return the current number of processed documents.
     */
    public long processed() {
        return processed;
    }

    /**
     * Get the curent number of discovered documents.
     * @return the curent number of discovered documents.
     */
    public long discovered() {
        return discovered;
    }
    
    /**
     * Convertes a period of time from milliseconds to a readable format
     * @param time milliseconds representing the period of time.
     * @param absolute if true converts to "YYYY.MM.DD HH.MM.SS" format, 
     *          if false converts to "X days, Y hours, Z minutes" format
     * @return the formated string.
     */
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
    
    /**
     * Updates the report file.
     */
    public void report() {
        if (stage < START || stage > STOP) return;
        now = System.currentTimeMillis();
        writeCrawlerProgress();
        long elapsed = now - startTime[START] - totalDeadTime;
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

    /**
     * Produces the status for the fetch stage.
     * @param buf the output buffer.
     * @throws java.io.IOException if there is a problem writing to the buffer.
     */
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

    /**
     * Produces the status for the sort stage.
     * @param buf the output buffer.
     * @throws java.io.IOException if there is a problem writing to the buffer.
     */
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

    /**
     * Produces the status for the merge stage.
     * @param buf the output buffer.
     * @throws java.io.IOException if there is a problem writing to the buffer.
     */
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
    
    /**
     * Produces the status for the trim stage.
     * @param buf the output buffer.
     * @throws java.io.IOException if there is a problem writing to the buffer.
     */
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
    
    /**
     * Produces the status info for a stage that is in progress.
     * @param buf the output buffer.
     * @throws java.io.IOException if there is a problem writing to the buffer.
     */    
    private void showProgress(BufferedWriter buf, long current, long max) throws IOException {
        long elapsed = now - startTime[stage] - deadTime[stage];
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

    /**
     * Produces the status info for a stage that has already finished.
     * @param buf the output buffer.
     * @param max the number of records processed.
     * @param stage the stage to which this info belongs.
     * @throws java.io.IOException if there is a problem writing to the buffer.
     */
    private void showFinished(BufferedWriter buf, long max, int stage) throws IOException {
        long elapsed = endTime[stage] - startTime[stage] - deadTime[stage];
        if (0 == elapsed) { elapsed = 1; }
        float rate = ((10000L * max) / elapsed) / 10.0f;
        buf.write(max+" docs in "+formatTime(elapsed,false)+" ("+rate+" docs/s)");
        buf.write("                                            ");
        buf.newLine();
    }

    /**
     * Closes the progress file for the current cycle and appends it 
     * to the end of the general progress file.
     */
    public void close() {
        stop();
        if (FileUtil.copyFile(reportFile,baseFile,true)) {
            FileUtil.deleteFile(reportFile.getAbsolutePath());
            FileUtil.deleteFile(binaryFile.getAbsolutePath());
        }
    }

    
    /* Persistence management */
    
    /**
     * Write a binary version of the progress report.
     */
    private void writeCrawlerProgress() {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(baseFile.getAbsolutePath()+"-b."+cycle));
            out.writeObject(this);
        } catch (Exception e) {
            logger.error("Reading binary crawler progress file: ",e);
        } finally {
            Execute.close(out);
        }
    }

    /**
     * Restore a CrawlerProgress instance from the latest progress report file.
     * If there is no binary progress report file, null is returned.
     * @return a CrawlerProgress instance, or null if there is no binary report file.
     */
    public static CrawlerProgress readCrawlerProgress() {
        CrawlerProgress cp = null;
        Config config = Config.getConfig("crawler.properties");
        String baseFileName = config.getString("progress.report.filename");
        int cycle = latestReportedCycle(baseFileName);
        if (cycle > 0) {
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(new FileInputStream(baseFileName+"-b."+cycle));
                cp = (CrawlerProgress)in.readObject();
            } catch (Exception e) {
                logger.error("Reading binary crawler progress file: ",e);
            } finally {
                Execute.close(in);
            }
        }
        return cp;
    }
    
    /**
     * Restores a CrawlerProgress instance from the latest progress report file
     * and compensate for the time elapsed since it was last updated.
     * If there is no binary progress report file, null is returned.
     * @return a CrawlerProgress instance, or null if there is no binary report file.
     */
    public static CrawlerProgress restartCrawlerProgress() {
        CrawlerProgress cp = readCrawlerProgress();
        if (null != cp) {
            long delta = System.currentTimeMillis() - cp.now;
            cp.deadTime[cp.stage] += delta;
            cp.totalDeadTime += delta;
            if (cp.stage == FETCH) { cp.seen = 0; }
        }
        return cp;
    }
    
    /**
     * Filter for binary progress file spec.
     */
    private static class Filter implements FilenameFilter {
        String basename;
        public Filter(String name) {
            basename = name;
        }
        public boolean accept(File dir, String name) { 
            return name.startsWith(basename);
        }
    }
    
    /**
     * Read the current crawler cycle from the latest report file.
     * If there is not progress report file, 0 es returned.
     * @return the current cycle, or 0 if there is no report file.
     */
    private static int latestReportedCycle(String baseName) {
        int cycle = 0;
        try {
            int minCycle = Integer.MAX_VALUE;
            File baseFile = new File(".",baseName);
            File dir = baseFile.getParentFile();
            for (File file : dir.listFiles(new Filter(baseFile.getName()+"-b."))) {
                String[] parts = file.getName().split("\\.");
                int val = Integer.parseInt(parts[parts.length-1]);
                if (val < minCycle) { minCycle = val; }
            }
            if (minCycle < Integer.MAX_VALUE) { cycle = minCycle; }
        } catch (Exception e) {
            logger.warn("While trying to read the cycle number from the crawler progress report file: ", e);
        }
        return cycle;
    }

}
