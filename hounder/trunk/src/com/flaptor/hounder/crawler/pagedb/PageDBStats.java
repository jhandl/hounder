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
package com.flaptor.hounder.crawler.pagedb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.flaptor.hist4j.AdaptiveHistogram;
import com.flaptor.util.Execute;


/**
 * This class represents the statistics for a PageDB.
 * It can to write the stats to a file and read it from a file,
 * gather the stats from a page source and calculate the 
 * priority threshold for a given percentile of the data values.
 * 
 * @author Flaptor Development Team
 */
public class PageDBStats {

    private static final int VERSION = 4;
    private String dir = null;
    public boolean initialized = false;
    public long pageCount = -1;
    public long cycleCount = -1;
    public long failedPages = -1;
    public long fetchedPages = -1;
    public float fetchedScore = -1;
    public AdaptiveHistogram priorityHistogram = null;
    public AdaptiveHistogram scoreHistogram = null;
    private int sampleSize = 20;
    private float[] prioritySamples = null;
    private float[] scoreSamples = null;
    private static String statsFileName = "stats";
    private static String sizeFileName = "size";
    private static String cyclesFileName = "cycles";
    static Logger logger = Logger.getLogger(Execute.whoAmI());


    /**
     * Class constructor.
     * @param dir the directory where the stats file should reside.
     */
    public PageDBStats (String dir) {
        this.dir = dir;
    }

    /**
     * Init data.
     */
    public void init() {
        pageCount = 0;
        cycleCount = 0;
        failedPages = 0;
        fetchedPages = 0;
        fetchedScore = 0;
        initialized = true;
        priorityHistogram = new AdaptiveHistogram();
        scoreHistogram = new AdaptiveHistogram();
    }

    /**
     * Writes the stats to a file.
     */
    public void write () {
        BufferedWriter buf = null;
        File file = new File(dir, statsFileName);
        try {
            buf = new BufferedWriter(new FileWriter(file));
            buf.write(String.valueOf(VERSION));
            buf.newLine();
            buf.write("pages="+String.valueOf(pageCount));
            buf.newLine();
            buf.write("cycles="+String.valueOf(cycleCount));
            buf.newLine();
            buf.write("fetched="+String.valueOf(fetchedPages));
            buf.newLine();
            buf.write("failed="+String.valueOf(failedPages));
            buf.newLine();
            buf.write("score="+String.valueOf(fetchedScore));
            buf.newLine();
            writeSamples(buf, priorityHistogram, "priority");
            writeSamples(buf, scoreHistogram, "score");
        } catch (Exception e) {
            logger.error("Writing data file " + file + ": " + e, e);
        } finally {
            Execute.close(buf);
        }
    }


    // read a line, either a version <4 value-only line or a >=4 "variable=value" line.
    private String readValue(BufferedReader buf) throws IOException {
        String line = buf.readLine();
        String[] parts = line.split("=");
        if (parts.length > 1) {
            line = parts[1];
        }
        return line.trim();
    }


    /**
     * Reads the stats from a file.
     */
    public void read () {
        BufferedReader buf = null;
        File file = new File(dir, statsFileName);
        if (file.exists()) {
            try {
                buf = new BufferedReader(new FileReader(file));
                int version = Integer.parseInt(buf.readLine());
                if (version > VERSION) {
                    logger.error("Pagedb Stats file version mismatch: max known version is "+VERSION+", found version " + version);
                } else {
                    pageCount = Long.parseLong(readValue(buf));
                    cycleCount = Long.parseLong(readValue(buf));
                    fetchedPages = Long.parseLong(readValue(buf));
                    if (version >= 3) {
                        failedPages = Long.parseLong(readValue(buf));
                    }
                    fetchedScore = Float.parseFloat(readValue(buf));
                    prioritySamples = readSamples(buf);
                    if (version >= 2) {
                        scoreSamples = readSamples(buf);
                    }
                }
            } catch (Exception e) {
                logger.error("Reading data file " + file + ": " + e, e);
            } finally {
                Execute.close(buf);
            }
        } else {
            pageCount = readNumberFromFile(new File(dir, sizeFileName));
            cycleCount = readNumberFromFile(new File(dir, cyclesFileName));
        }
        initialized = true;
    }

    // write histogram samples to a file.
    private void writeSamples (BufferedWriter writer, AdaptiveHistogram histogram, String name) throws IOException {
        writer.write(name+" histogram: sampleSize="+String.valueOf(sampleSize));
        writer.newLine();
        for (int s = 0; s <= sampleSize; s++) {
            int p = (s * 100) / sampleSize; 
            float value = 0;
            if (null != histogram) {
                value = histogram.getValueForPercentile(p);
            } else if (null != prioritySamples) {
                value = prioritySamples[s];
            }
            writer.write(" sample"+s+"="+String.valueOf(value));
            writer.newLine();
        }
    }

    // read histogram samples from a file
    private float[] readSamples (BufferedReader reader) throws IOException {
        sampleSize = Integer.parseInt(readValue(reader));
        float[] samples = new float[sampleSize+1];
        for (int s = 0; s <= sampleSize; s++) {
            samples[s] = Float.parseFloat(readValue(reader));
        }
        return samples;
    }

    // Reads a number from a file. This is for reading old pagedbs that don't have one stats file.
    private static long readNumberFromFile (File file) {
        long data = -1;
        BufferedReader buf = null;
        try {
            buf = new BufferedReader(new FileReader(file));
            data = Long.parseLong(buf.readLine());
        } catch (Exception e) {
            logger.warn("Reading data file " + file + ": " + e, e);
        } finally {
            Execute.close(buf);
        }
        return data;
    }

    /**
     * Reads pages from a page source and calculates the stats.
     * @param pageSource the class that provides a way to get pages from which to gather stats.
     * @throws FileNotFoundException if the provided page source can't find the page file.
     */
    public void gatherStatsFromPageDBFile (PageDB.PageSource pageSource) throws FileNotFoundException, IOException {
        if (!initialized) {
            init();
        } else {
            pageCount = 0;
            failedPages = 0;
            fetchedPages = 0;
            fetchedScore = 0;
            priorityHistogram = new AdaptiveHistogram();
            scoreHistogram = new AdaptiveHistogram();
        }
        pageSource.open();
        while (true) {
            try {
                Page page = pageSource.nextPage();
                pageCount++;
                if (page.getLastAttempt() > 0) {
                    if (page.getLastSuccess() > 0) {
                        fetchedPages++;
                        fetchedScore += page.getScore();
                    } else {
                        failedPages++;
                    }
                }
                priorityHistogram.addValue(page.getPriority());
                scoreHistogram.addValue(page.getScore());
            } catch (Exception e) {
                break;
            }
        }
        pageSource.close();
    }

    // Gets the histogram threshold for a given percentile.
    public float getHistogramThreshold (int percentile, AdaptiveHistogram histogram, float[] samples) {
        float threshold = 0;
        if (null != samples) {
            float i = (float)(percentile * sampleSize) / 100f;
            int i1 = (int)Math.floor(i);
            if (i == i1) {
                threshold = samples[i1];
            } else {
                int i2 = (int)Math.ceil(i);
                threshold = samples[i1] + (samples[i2] - samples[i1]) * (i - i1);
            }
        } else if (null != histogram) {
            threshold = histogram.getValueForPercentile(percentile);
        }
        return threshold;
    }

    /**
     * Gets the priority threshold for a given percentile.
     * @param percentile the percent of values that should fall below the requested value.
     * @return the value for which the given percent of values fall below.
     */
    public float getPriorityThreshold (int percentile) {
        return getHistogramThreshold (percentile, priorityHistogram, prioritySamples);
    }

    /**
     * Gets the priority threshold for a given percentile.
     * @param percentile the percent of values that should fall below the requested value.
     * @return the value for which the given percent of values fall below.
     */
    public float getScoreThreshold (int percentile) {
        return getHistogramThreshold (percentile, scoreHistogram, scoreSamples);
    }

    /**
     * Relocates the stats file withing a new pagedb directory.
     */
    public void relocateTo (String newDir) {
        dir = newDir;
    }

    /** Return a tring representation of this object */
    public String toString () {
        return "Stats:  size="+pageCount+"  cycles="+cycleCount+"failedSize="+failedPages+"  fetchedSize="+fetchedPages+"  fetchedScore="+fetchedScore;
    }

}


