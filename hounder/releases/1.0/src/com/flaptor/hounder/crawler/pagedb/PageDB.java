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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.flaptor.hounder.crawler.PageDBTrimmer;
import com.flaptor.hounder.crawler.PageRank;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.sort.MergeSort;


/**
 * The PageDB implements a database of pages. It can be read in url order 
 * or in md5 order (md5 hash of the url), which is close to random order.
 * It also takes care of various statistics, like number of pages
 * 
 * @author Flaptor Development Team
 */
public class PageDB implements IPageStore {

    static Logger logger = Logger.getLogger(Execute.whoAmI());

    // open mode
    public static final int READ = 0x01;
    public static final int WRITE = 0x02;
    protected int mode;

    // open modifier, can be added to the open mode
    public static final int UNSORTED = 0x10; 
    public static final int APPEND = 0x20;

    // for use with setPriorityScope()
    public static final int ALL_PAGES = 1;
    public static final int FETCHED_PAGES = 2;

    // file names for the pagedb
    private static String unsortedFileName = "pages";
    private static String md5FileName = "pagesByMd5";

    // pagedb related files
    private String dirname = null;
    private ObjectOutputStream outputStream = null;
    private ObjectInputStream inputStream = null;

    // file buffer size
    private static final int BUFFERSIZE = 65535;

    // used to determine if the pages are being added in order
    private String maxHash = null;
    private boolean sorted = false;
    private boolean sortDisabled = false;

    // stats
    private long pagesRead = 0;
    private PageDBStats stats = null;
    private int priorityScope = FETCHED_PAGES;


    /**
     * Create a PageDB.
     * @param dirname the path to the pagedb directory.
     */
    public PageDB (String dirname) {
        this.dirname = dirname;
        // logger.debug("PAGEDB constructor1 "+dirname+" ("+new Throwable().getStackTrace()[1].getClassName()+")");
        stats = new PageDBStats(dirname);
    }

    /**
     * Open the pagedb for reading or writing.
     * @param mode (READ or WRITE) + (APPEND and/or UNSORTED)
     */
    public void open (int mode) throws IOException {
        File dir;
        this.mode = mode;
        int action = (mode & 0x0F);
        boolean append = (mode & APPEND) != 0;
        boolean sortDisabled = (mode & UNSORTED) != 0;
        switch (action) {
            case READ:
                // logger.debug("PAGEDB open read "+dirname+" ("+new Throwable().getStackTrace()[1].getClassName()+")");
                // At this point we don't know which db the user will open, so all we can do is check for the dir.
                dir = new File(dirname);
                if (!dir.exists()) {
                    throw new IOException("PageDB directory not found (" + dirname + ").");
                }
                stats.read();
                if (0 == stats.pageCount) {
                    logger.warn("Pagedb " + dirname + " has no pages");
                }
                pagesRead = 0;
                break;
            case WRITE:
                // logger.debug("PAGEDB open write "+dirname+" ("+new Throwable().getStackTrace()[1].getClassName()+")");
                dir = new File (dirname);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File md5File = new File(dir, md5FileName);
                boolean oldDB = false;
                if (md5File.exists()) {
                    md5File.renameTo(new File(dir, unsortedFileName));
                    oldDB = true;
                }
                outputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(dir, unsortedFileName), append), BUFFERSIZE));
                if (append && oldDB) {
                    stats.read();  // this is needed for the stats that can't be gathered from the files alone, like the cycle number
                    stats.gatherStatsFromPageDBFile(new PageSource(new File(dirname, unsortedFileName)));  // gather the rest of the data, including histogram
                    sorted = false;
                } else {
                    stats.init();
                    maxHash = "00000000000000000000000000000000";
                    sorted = true;
                }
                break;
            default:
                throw new IOException("Unknown open mode (" + mode + ").");
        }
    }

    /**
     * Sets the scope for the priority calculation to include all pages or only the fetched ones.
     * @param scope either ALL_PAGES of FETCHED_PAGES.
     */
    public void setPriorityScope (int scope) {
        priorityScope = scope;
    }

    /**
     * Write a page to the pagedb.
     * @param page Page that should be written to the pagedb.
     */
    public synchronized void addPage (Page page) throws IOException {
        logger.debug("PAGEDB addPage fetched=" +(page.getLastSuccess()>0)+" ("+page.getUrl()+")");
        if (null == outputStream) {
            throw new IOException("PageDB not open for writing.");
        }
        // write page to the file
        page.write(outputStream);
        outputStream.reset();
        // check for sorted addition
        if (sorted) {
            String hash = page.getUrlHash();
            sorted = (hash.compareTo(maxHash) >= 0); 
            maxHash = hash;
        }
        // accumulate stats
        stats.pageCount++;
        if (page.getLastAttempt() > 0) {
            if (page.getLastSuccess() > 0) {
                stats.fetchedPages++;
                stats.fetchedScore += page.getScore();
                if (priorityScope == FETCHED_PAGES) {
                    stats.priorityHistogram.addValue(page.getPriority());
                }
            } else {
                stats.failedPages++;
            }
        }
        if (priorityScope == ALL_PAGES) {
            stats.priorityHistogram.addValue(page.getPriority());
        }
        stats.scoreHistogram.addValue(page.getScore());
    }


    public Iterator<Page> iterator() {
        return new PageIterator();
    }

    protected class PageIterator implements Iterator<Page> {

        private ObjectInputStream inputStream;
        private long pagesRead, pageCount;

        public PageIterator() {
            pageCount = 0;
            File inputFile = new File(dirname, md5FileName);
            if (!inputFile.exists()) {
                logger.error("Sorted pages file not found. Maybe the pagedb has not been closed?");
            } else {
                try {
                    inputStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(inputFile), BUFFERSIZE));
                    if (!stats.initialized) {
                        stats.read();
                    }
                    pageCount = stats.pageCount;
                } catch (IOException e) {
                    logger.error("Could not open sorted pages file.", e);
                }
            }
            pagesRead = 0;
        }

        public boolean hasNext() {
            if (null == inputStream) {
                throw new IllegalStateException("Pagedb not open for reading.");
            }
            return (pagesRead < pageCount);
        }

        public Page next() {
            if (null == inputStream) {
                throw new IllegalStateException("Pagedb not open for reading.");
            }
            Page page = null;
            try {
                page = Page.read(inputStream);
                pagesRead++;
            } catch (IOException e) {
                logger.error("Reading PageDB: " + e, e);
            }
            return page;
        }

        public void remove() {
            throw new IllegalStateException("This operation is not implemented.");
        }

    }


    /**
     * Close the pagedb. 
     * It should be called both after reading and after writing a poagedb.
     */
    public void close () throws IOException {
        // logger.debug("PAGEDB close "+dirname+" ("+new Throwable().getStackTrace()[1].getClassName()+")");
        if (null != inputStream) {
            Execute.close(inputStream);
            inputStream = null;
        }
        if (null != outputStream) {
            Execute.close(outputStream);
            outputStream = null;
            sortPages();
            stats.write();
        }
    }


    // Transform the input file into two sorted files, one by url, the other one by md5.
    private void sortPages () throws IOException {
        File unsortedFile = new File (dirname, unsortedFileName);
        File md5File = new File (dirname, md5FileName);
        sortPages (unsortedFile, md5File);
        unsortedFile.delete();
    }

    // If the source file is already sorted, copy it; otherwise sort it.
    private void sortPages (File from, File to) throws IOException {
        if (sorted || sortDisabled) {
            from.renameTo(to);
        } else {
            MergeSort.sort (from, to, new PageInformation());
        }
    }

    // Auxiliary interface for inline functor object.
    private interface PageDataGetter {
        public Comparable getData (Page p);
    }

    // Merge sorted files aliminating duplicates and return the resulting file size.
    @SuppressWarnings("unchecked")
        private static void mergeSortedFiles (File orig1, File orig2, File dest, PageDataGetter dataGetter) throws IOException {
            ObjectInputStream in1= new ObjectInputStream(new BufferedInputStream(new FileInputStream(orig1), BUFFERSIZE));
            ObjectInputStream in2 = new ObjectInputStream(new BufferedInputStream(new FileInputStream(orig2), BUFFERSIZE));
            ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(dest), BUFFERSIZE));
            Comparable val1 = null;
            Comparable val2 = null;
            Comparable valout = null;
            Comparable valold = null;
            Page page1 = null;
            Page page2 = null;
            Page pageout = null;
            Page pageold = null;
            boolean eof1 = false;
            boolean eof2 = false;

            // read the first page from file 1
            try {
                page1 = Page.read(in1);
                val1 = dataGetter.getData(page1);
            } catch (EOFException e) {
                eof1 = true;
            }
            // read the first page from file 2
            try {
                page2 = Page.read(in2);
                val2 = dataGetter.getData(page2);
            } catch (EOFException e) {
                eof2 = true;
            }
            // while any of the files has unread data
            while (!eof1 || !eof2) {
                if (!eof2 && (eof1 || val1.compareTo(val2) >= 0)) {  
                    // if val1 >= val2, select page2 for output
                    pageout = page2;
                    valout = val2;
                    // and read a new page from file 2
                    try {
                        page2 = Page.read(in2);
                        val2 = dataGetter.getData(page2);
                    } catch (EOFException e) {
                        eof2 = true;
                    }
                } else {              
                    // if val1 < val2, select page1 for output
                    pageout = page1;
                    valout = val1;
                    // and read a new page from file 1
                    try {
                        page1 = Page.read(in1);
                        val1 = dataGetter.getData(page1);
                    } catch (EOFException e) {
                        eof1 = true;
                    }
                }
                // if the page selected for output is different from the last one
                if (null != valold && valout.compareTo(valold) != 0) {
                    // write that page to the output file
                    pageold.write(out);
                    out.reset();
                }
                pageold = pageout;
                valold = valout;
            }
            // the last page must be written
            if (null != pageold) {
                pageold.write(out);
                out.reset();
            }
            Execute.close(in1);
            Execute.close(in2);
            Execute.close(out);
        }


    /**
     * Merge this pageDB with another one.
     * @param orig1 the first origin pagedb to merge.
     * @param orig2 the second origin pagedb to merge.
     * @param dest the destination pagedb.
     */
    public static void merge (PageDB orig1, PageDB orig2, PageDB dest) throws IOException {
        orig1.open(READ);
        orig2.open(READ);
        dest.open(WRITE);
        dest.close();
        File in1, in2, out;
        PageDBStats stats = new PageDBStats(dest.getDir());
        stats.cycleCount = Math.max(orig1.getCycles(), orig2.getCycles());

        in1 = new File (orig1.getDir(), md5FileName);
        in2 = new File (orig2.getDir(), md5FileName);
        out = new File (dest.getDir(), md5FileName);
        mergeSortedFiles (in1, in2, out, new PageDataGetter() { public Comparable getData(Page p) {return p.getUrlHash();} });

        stats.gatherStatsFromPageDBFile(new PageSource(out));
        stats.write();
    }


    // Auxiliary class used to give the PageDBStats a means to access the pages in a PageDB file 
    // while keeping the PageDB file format knowledge within the PageDB class.
    protected static class PageSource {
        ObjectInputStream in;
        private File file;
        public PageSource (File file) {
            this.file = file;
        }
        // FIXME throws Exception
        public void open () throws FileNotFoundException, IOException {
            in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file), BUFFERSIZE));
        }
        public Page nextPage () throws IOException {
            Page p = Page.read(in);
            return p;
        }
        public void close () {
            Execute.close(in);
        }
    }

    // Reads pages from a file and counts them.
    private long countPages (File file) {
        long count = 0;
        try {
            PageDBStats lstats = new PageDBStats(null);
            lstats.gatherStatsFromPageDBFile(new PageSource(file));
            count = lstats.pageCount;
        } catch (FileNotFoundException e) {
            logger.error(e, e);
        } catch (IOException e) {
            logger.error(e, e);
        }
        return count;
    }


    /**
     * Repairs a pagedb that has been not been properly closed.
     */
    public void repair () throws IOException {
        File dir = new File (dirname);
        if (!dir.exists()) {
            throw new IOException("PageDB directory not found (" + dirname + ").");
        }
        // see if the pagedb has not been sorted or the sort was interrupted
        File unsortedFile = new File (dir, unsortedFileName);
        if (MergeSort.sortIsIncomplete(unsortedFile)) { 
            // prepare for proper close
            logger.info("Rebuilding with " + stats.pageCount + " documents...");
            sorted = false;
            sortPages();

            // read the stats file, in case we can obtain the cycle number
            // the rest of the stats will be recalculated
            stats.read();
            if (stats.cycleCount == -1) {  // stats file not there, so start over with cycle 0
                stats.cycleCount = 0;
            }
            // compute the fetch stats
            stats.gatherStatsFromPageDBFile(new PageSource(new File (dir, md5FileName)));
            // write the stats file
            stats.write();

        } else {
            // see if the data file is there
            File md5File = new File (dir, md5FileName);
            long md5Count = countPages(md5File); // count the pages in the md5 file
            stats.read(); // read the stats file

            if (md5Count == -1) { // main data file missing
                if (stats.pageCount == -1 && stats.cycleCount == -1) { // stats data file missing
                    logger.error("This is an empty directory. There is no pagedb to repair.");
                } else {
                    logger.error("All the page data in this pagedb is missing, it can't be repaired.");
                }
            } else {
                // main data file is ok
                if (stats.pageCount != md5Count) {  // the page count data is lost or wrong
                    stats.pageCount = md5Count;
                    logger.info("Page count data lost or inconsistent, will use " + stats.pageCount + ".");
                }
                if (stats.cycleCount == -1) {  // the cycle count data is lost or wrong
                    stats.cycleCount = 0;
                    logger.info("Cycle count data lost, will restart at 0.");
                }
                // compute the fetch stats
                stats.gatherStatsFromPageDBFile(new PageSource(md5File));
                // write the stats file
                stats.write();
            }
        }
    }

    /**
     * Returns the number of pages in the pagedb.
     * @return the number of pages in the pagedb.
     */
    public long getSize () {
        // logger.debug("PAGEDB getSize "+dirname+" ("+new Throwable().getStackTrace()[1].getClassName()+")");
        if (!stats.initialized) {
            stats.read();
        }
        return stats.pageCount;
    }

    /**
     * Returns the number of cycles the pagedb has gone through.
     * @return the number of cycles.
     */
    public long getCycles () {
        // logger.debug("PAGEDB getCycles "+dirname+" ("+new Throwable().getStackTrace()[1].getClassName()+")");
        if (!stats.initialized) {
            stats.read();
        }
        return stats.cycleCount;
    }

    /**
     * Returns the number of failed pages in the last cycle.
     * @return the number of failed pages in the last cycle.
     */
    public long getFailedSize () {
        // logger.debug("PAGEDB getFetchedSize "+dirname+" ("+new Throwable().getStackTrace()[1].getClassName()+")");
        if (!stats.initialized) {
            stats.read();
        }
        return stats.failedPages;
    }

    /**
     * Returns the number of fetched pages in the last cycle.
     * @return the number of fetched pages in the last cycle.
     */
    public long getFetchedSize () {
        // logger.debug("PAGEDB getFetchedSize "+dirname+" ("+new Throwable().getStackTrace()[1].getClassName()+")");
        if (!stats.initialized) {
            stats.read();
        }
        return stats.fetchedPages;
    }

    /**
     * Returns the sum of the score of all fetched pages in the pagedb.
     * @return the total score.
     */
    public float getFetchedScore () {
        // logger.debug("PAGEDB getFetchedScore "+dirname+" ("+new Throwable().getStackTrace()[1].getClassName()+")");
        if (!stats.initialized) {
            stats.read();
        }
        return stats.fetchedScore;
    }

    /**
     * Returns the value for the given percentile in the priority histogram.
     * @return the priority threshold for the given percentile.
     */
    public float getPriorityThreshold (int percentile) {
        // logger.debug("PAGEDB getPriorityThreshold "+dirname+" ("+new Throwable().getStackTrace()[1].getClassName()+")");
        return stats.getPriorityThreshold(percentile);
    }

    /**
     * Returns the value for the given percentile in the score histogram.
     * @return the score threshold for the given percentile.
     */
    public float getScoreThreshold (int percentile) {
        // logger.debug("PAGEDB getScoreThreshold "+dirname+" ("+new Throwable().getStackTrace()[1].getClassName()+")");
        return stats.getScoreThreshold(percentile);
    }


    /**
     * Tell the pagedb that it is part of the same cycle as anoter pagedb.
     */
    public void setSameCycleAs (PageDB otherdb) {
        // logger.debug("PAGEDB setSameCycleAs "+dirname+" <- "+otherdb.getDir()+" ("+new Throwable().getStackTrace()[1].getClassName()+")");
        stats.cycleCount = otherdb.getCycles();
        stats.write();
    }

    /**
     * Tell the pagedb that it is the next cycle of anoter pagedb.
     */
    public void setNextCycleOf (PageDB otherdb) {
        // logger.debug("PAGEDB setNextCycleOf "+dirname+" <- "+otherdb.getDir()+" + 1 ("+new Throwable().getStackTrace()[1].getClassName()+")");
        stats.cycleCount = otherdb.getCycles() + 1;
        stats.write();
    }

    /**
     * Returns the directory of the pagedb.
     * @return the directory of the pagedb.
     */
    public String getDir () {
        // logger.debug("PAGEDB getDir "+dirname+" ("+new Throwable().getStackTrace()[1].getClassName()+")");
        return dirname;
    }

    /**
     * Delete the pagedb.
     */
    public boolean deleteDir () {
        // logger.debug("PAGEDB deleteDir "+dirname+" ("+new Throwable().getStackTrace()[1].getClassName()+")");
        return FileUtil.deleteDir(dirname);
    }

    /**
     * Rename the pagedb.
     * @param newname new name for the pagedb.
     */
    public boolean rename (String newname) {
        // logger.debug("PAGEDB rename "+dirname+" ("+new Throwable().getStackTrace()[1].getClassName()+")");
        boolean ok = FileUtil.rename(dirname, newname);
        if (ok) {
            dirname = newname;
            stats.relocateTo(newname);
        }
        return ok;
    }





    private static void usage (String msg) {
        System.out.println();
        System.out.println(msg);
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  PageDB list <dir>                                (lists all pages)");
        System.out.println("  PageDB create <dir> <url-file>                   (creates a new pagedb)"); 
        System.out.println("  PageDB append <dir> <url-file>                   (appends to an existing pagedb)"); 
        System.out.println("  PageDB merge <dir-orig1> <dir-orig2> <dir-dest>  (merges two pagedbs, eliminating duplicates)"); 
        System.out.println("  PageDB repair <dir>                              (closes a pagedb that has not been properly closed)");
        System.out.println("  PageDB trim <dir-orig> <dir-dest>                (trims the pagedb according to crawler config)"); 
        System.out.println("  PageDB reset <dir-orig> <dif-dest>               (marks all pages as not fetched)"); 
        System.out.println();
        System.exit(1);
    }

    public static void main (String[] args) {
        try {
            String log4jConfigPath = com.flaptor.util.FileUtil.getFilePathFromClasspath("log4j.properties");
            if (null != log4jConfigPath) {
                PropertyConfigurator.configure(log4jConfigPath);
            } else {
                logger.warn("log4j.properties not found on classpath!");
            }

            if (args.length < 2) {
                usage("Not enough arguments.");
            }
            String cmd = args[0];

            if ("list".equals(cmd)) {

                System.err.println();
                String dir = args[1];
                PageDB pagedb = new PageDB(dir);
                pagedb.open(READ);
                for (Page page : pagedb) {
                    String url = page.getUrl();
                    int distance = page.getDistance();
                    int retries = page.getRetries();
                    long lastAttempt = page.getLastAttempt();
                    long lastSuccess = page.getLastSuccess();
                    float score = page.getScore();
                    float priority = page.getPriority();
                    String[] anchors = page.getAnchors();
                    System.out.println ("  Dist=" + distance + " Retr=" + retries + " LastAtt=" + lastAttempt + " LastSucc=" + lastSuccess + " Score=" + score + " Prior=" + priority + " URL=" + url + " Anchors(" + anchors.length + ")=" + java.util.Arrays.asList(anchors));
                }
                System.err.println();
                pagedb.close();

            } else if ("stats".equals(cmd)) {

                System.err.println();
                String dir = args[1];
                int[] distStats = new int[1000];
                int[] retryStats = new int[100];
                int maxDistance = 0;
                int maxRetries = 0;
                long size = 0;
                long fetched = 0;
                long failed = 0;
                float minPriority = Float.MAX_VALUE;
                float maxPriority = -Float.MAX_VALUE;
                float minScore = Float.MAX_VALUE;
                float maxScore = -Float.MAX_VALUE;
                PageDB pagedb = new PageDB(dir);
                pagedb.open(READ);
                for (Page page : pagedb) {
                    String url = page.getUrl();
                    int distance = page.getDistance();
                    int retries = page.getRetries();
                    float priority = page.getPriority();
                    float score = page.getScore();
                    if (page.getLastAttempt() > 0) {
                        if (page.getLastSuccess() > 0) {
                            if (priority > maxPriority) maxPriority = priority;
                            if (priority < minPriority) minPriority = priority;
                            if (score > maxScore) maxScore = score;
                            if (score < minScore) minScore = score;
                            fetched++;
                        } else {
                            failed++;
                        }
                    }
                    distStats[distance]++;
                    retryStats[retries]++;
                    if (distance > maxDistance) maxDistance = distance;
                    if (retries > maxRetries) maxRetries = retries;
                    size++;
                }
                System.err.println();
                System.err.println("  stat:  size="+pagedb.getSize()+"  fetched="+pagedb.getFetchedSize()+"  failed="+pagedb.getFailedSize()+"  cycles="+pagedb.getCycles());
                System.err.println("  real:  size="+size+"  fetched="+fetched+"  failed="+failed);
                System.err.println();
                for (int d=0; d<=maxDistance; d++) if (distStats[d] > 0) System.err.println("        distance "+d+": "+distStats[d]);
                System.err.println();
                for (int r=0; r<=maxRetries; r++) if (retryStats[r] > 0) System.err.println("        retries "+r+": "+retryStats[r]);
                System.err.println();
                float histMin, histMax;
                histMin = pagedb.stats.getPriorityThreshold(0);
                histMax = pagedb.stats.getPriorityThreshold(100);
                System.err.println("  Priority:");
                System.err.println("      hist:   min="+histMin+"  max="+histMax);
                System.err.println("      real:   min="+minPriority+"  max="+maxPriority);
                System.err.println();
                histMin = pagedb.stats.getScoreThreshold(0);
                histMax = pagedb.stats.getScoreThreshold(100);
                System.err.println("  Score:  (sum="+pagedb.getFetchedScore()+")");
                System.err.println("      hist:   min="+histMin+"  max="+histMax);
                System.err.println("      real:   min="+minScore+"  max="+maxScore);
                System.err.println();
                pagedb.close();

            } else if ("new".equals(cmd)) {

                if (args.length < 2) {
                    usage("Not enough arguments.");
                }
                String dir = args[1];
                PageDB pagedb = new PageDB(dir);
                pagedb.open(WRITE);
                pagedb.close();
                System.out.println("New empty pagedb created");

            } else if ("create".equals(cmd) || "append".equals(cmd)) {

                if (args.length < 3) {
                    usage("Not enough arguments.");
                }
                String dir = args[1];
                boolean append = "append".equals(cmd);
                File urls = new File(args[2]);
                if (!urls.exists()) {
                    usage("Urls file not found.");
                }
                boolean verbose = true;
                if (args.length == 4 && "-q".equals(args[3])) {
                    verbose = false;
                } 
                PageDB pagedb = new PageDB(dir);
                pagedb.open(WRITE + (append ? APPEND : 0));
                BufferedReader burls = new BufferedReader(new FileReader(urls));
                int count = 0;
                while (burls.ready()) {
                    String line = burls.readLine();
                    if (line.trim().length() > 0) {
                        Page page = new Page(line, 0f, 0f);
                        page.setDistance(0);
                        page.setRetries(0);
                        page.setLastAttempt(0L);
                        page.setLastSuccess(0L);
                        page.setScore(PageRank.INITIAL_SCORE);
                        pagedb.addPage(page);
                        count++;
                    }
                }
                if (verbose) System.out.println("Added " + count + " pages to the pagedb");
                Execute.close(burls);
                pagedb.close();

            } else if ("merge".equals(cmd)) {

                if (args.length < 4) {
                    usage("Not enough arguments.");
                }
                PageDB orig1 = new PageDB(args[1]);
                PageDB orig2 = new PageDB(args[2]);
                PageDB dest = new PageDB(args[3]);
                PageDB.merge (orig1, orig2, dest);

            } else if ("repair".equals(cmd)) {

                if (args.length < 2) {
                    usage("Not enough arguments.");
                }
                PageDB pagedb = new PageDB(args[1]);
                pagedb.repair();

            } else if ("trim".equals(cmd)) {

                if (args.length < 3) {
                    usage("Not enough arguments.");
                }
                PageDB orig = new PageDB(args[1]);
                PageDB dest = new PageDB(args[2]);
                long unfetched = orig.getSize() - orig.getFetchedSize();
                PageDBTrimmer trimmer = new PageDBTrimmer();
                trimmer.trimPageDB(orig, dest, unfetched);

            } else if ("reset".equals(cmd)) {

                if (args.length < 3) {
                    usage("Not enough arguments.");
                }
                PageDB orig = new PageDB(args[1]);
                PageDB dest = new PageDB(args[2]);
                orig.open(READ);
                dest.open(WRITE);
                for (Page page : orig) {
                    page.setLastAttempt(0L);
                    page.setLastSuccess(0L);
                    page.setRetries(0);
                    page.setPriority(0);
                    page.setScore(PageRank.INITIAL_SCORE);
                    dest.addPage(page);
                }
                orig.close();
                dest.close();

            } else if ("copy".equals(cmd)) {

                if (args.length < 3) {
                    usage("Not enough arguments.");
                }
                PageDB orig = new PageDB(args[1]);
                PageDB dest = new PageDB(args[2]);
                orig.open(READ);
                dest.open(WRITE);
                for (Page page : orig) {
                    dest.addPage(page);
                }
                orig.close();
                dest.close();

            } else {
                usage("Unknown command.");
            }
        } catch (Exception e) {
            logger.error("" + e, e);
        }
    }

}

