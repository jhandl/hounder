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
    private long max;
    private long seen;
    private long tofetch;
    private long fetched;
    private long processed;
    private long startTime;
    private File reportFile;
    private File baseFile;

    public CrawlerProgress(long cycle, long max, long known) {
        this.cycle = cycle;
        this.max = max;
        seen = 0;
        fetched = 0;
        processed = 0;
        startTime = System.currentTimeMillis(); 
        Config config = Config.getConfig("crawler.properties");
        int refetchPercent = config.getInt("priority.percentile.to.fetch");
        tofetch = (max-known)+known*refetchPercent/100;
        String baseFileName = config.getString("progress.report.filename");
        baseFile = new File(baseFileName);
        reportFile = new File(baseFileName+"."+cycle);
    }

    public void addSeen(long seen) {
        this.seen += seen;
    }
    
    public void addFetched(long fetched) {
        this.fetched += fetched;
    }
    
    public void addProcessed(long processed) {
        this.processed += processed;
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
        long now = System.currentTimeMillis();
        long elapsed = now - startTime;
        if (0 == elapsed) { elapsed = 1; }
        long remaining = -1;
        if (processed > 0) {
            remaining = ((tofetch * elapsed) / processed) - elapsed;
        } else if (fetched > 0) {
            remaining = ((tofetch * elapsed) / fetched) - elapsed;
        }
        float rate = ((10000L * processed) / elapsed) / 10.0f;
        BufferedWriter buf = null;
        try {
            buf = new BufferedWriter(new FileWriter(reportFile));
            buf.write("Cycle: " + cycle);
            buf.newLine();
            buf.write("Pagedb size: " + max + " (to fetch "+tofetch+")");
            buf.newLine();
            buf.write("Seen: " + seen + " (" + (100 * seen / max) + "%)");
            buf.newLine();
            buf.write("Fetched: " + fetched + " (" + (100 * fetched / tofetch) + "%)");
            buf.newLine();
            buf.write("Processed: " + processed + " (" + (100 * processed / tofetch) + "%)");
            buf.newLine();
            buf.write("Rate: " + rate + " docs/s");
            buf.newLine();
            buf.write("Start: " + formatTime(startTime,true));
            buf.newLine();
            buf.write("Now: " + formatTime(now,true));
            buf.newLine();
            buf.write("Elapsed: " + formatTime(elapsed,false));
            buf.newLine();
            buf.write("Remaining: " + formatTime(remaining,false));
            buf.newLine();
            buf.newLine();
        } catch (IOException ex) {
            logger.error("While writing to the crawler progress report file:", ex);
        } finally {
            Execute.close(buf);
        }
    }
    
    public void close() {
        if (FileUtil.copyFile(reportFile,baseFile,true)) {
            FileUtil.deleteFile(reportFile.getAbsolutePath());
        }
    }
    
}
