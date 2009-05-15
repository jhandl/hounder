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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import org.apache.nutch.protocol.Content;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.fetcher.Fetcher;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.util.NutchJob;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.conf.Configuration;

import com.flaptor.hounder.crawler.modules.FetchDocument;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import org.apache.nutch.metadata.Nutch;
import org.apache.nutch.parse.ParseText;
import org.apache.nutch.protocol.ProtocolStatus;

/**
 * This class implements a wrapper around the Nutch fetcher, 
 * to be used as a plugin in the Search4j Crawler.
 */
public class Nutch9Fetcher implements IFetcher {

    private static Logger logger = Logger.getLogger(Execute.whoAmI());
    private String segmentsDir;
    private int threads;
    private boolean keepUrl;
    private Fetcher fetcher;

    /**
     * Initialize the fetcher.
     */
    public Nutch9Fetcher() {
        Config config = Config.getConfig("nutchfetcher.properties");
        segmentsDir = config.getString("fetchlist.dir");
        keepUrl = config.getBoolean("keep.original.url.on.redirect");
        fetcher = new Fetcher();
        Configuration conf = new Configuration();
        // conf.addDefaultResource("crawl-tool.xml");
        conf.addDefaultResource("nutch-default.xml");
        conf.addDefaultResource("nutch-site.xml");
        JobConf job = new NutchJob(conf);
        threads = job.getInt("fetcher.threads.fetch", 10);
        fetcher.setConf(conf);
    }

    /**
     * Fetch the provided list of pages and produce a resulting list of fetched data.
     * @param fetchlist the list of pages to fetch.
     * @return a list of fetched pages.
     */
    public FetchData fetch(FetchList fetchlist) throws Exception {
        String segmentDir = buildSegment(fetchlist);
        fetcher.fetch(new Path(segmentDir),threads);
        FetchData fetchdata = buildFetchData(segmentDir, fetchlist);
        FileUtil.deleteDir(segmentDir);
        return fetchdata;
    }

    /**
     * Create a new empty segment.
     * @return the absolute path of the segment directory.
     */
    private synchronized String getNewSegmentDir() {
        String dirname = null;
        try {
            File dir;
            int suffix = 1;
            do {
                String datePrefix = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(System.currentTimeMillis()));
                dir = new File(segmentsDir, datePrefix + "-" + suffix);
                suffix++;
            } while (dir.exists());
            dir.mkdir();
            dirname = dir.getCanonicalPath();
        } catch (Exception e) {
            logger.error(e, e);
        }
        return dirname;
    }

    // Auxiliary progressable class.
    private class NoProgress implements org.apache.hadoop.util.Progressable {
        public void progress() {}
    }


    /**
     * Create a nutch fetchlist segment from the provided list of pages.
     * @param fetchlist the list of pages from which to build the segment.
     */
    private String buildSegment(FetchList fetchlist) throws IOException {
        // create the segment dir
        String segmentDir = getNewSegmentDir();
        Path output = new Path(segmentDir, CrawlDatum.GENERATE_DIR_NAME);
        JobConf job = new JobConf();
        job.setOutputPath(output);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(CrawlDatum.class);
        // job.setOutputFormat(SequenceFileOutputFormat.class);
        // job.setOutputKeyComparatorClass(HashComparator.class);
        RecordWriter writer = new SequenceFileOutputFormat().getRecordWriter(null,job,"fetcher",new NoProgress());
        for (com.flaptor.hounder.crawler.pagedb.Page page : fetchlist) {
            Text key = new Text(page.getUrl());
            CrawlDatum value = new CrawlDatum(); // TODO: try taking this line outside of the loop
            writer.write(key,value);
        }
        writer.close(null);
        return segmentDir;
    }


    /**
     * Determine if the fetch is successful.
     * @return true if the fetch is successful.
     */
    private static boolean success (SegmentRecord rec) {
        return (rec.status == CrawlDatum.STATUS_FETCH_SUCCESS);
    }

    /**
     * Determine if the fetch error is recoverable.
     * @return true if the fetch error is recoverable.
     */
    private static boolean recoverable (SegmentRecord rec) {
        return (rec.status != CrawlDatum.STATUS_FETCH_GONE);
    }

    /**
     * Determine if the fetch error is internal.
     * @return true if the fetch error is internal.
     */
    private static boolean internalError (SegmentRecord rec) {
        return (rec.protocol_code == ProtocolStatus.WOULDBLOCK 
                || rec.protocol_code == ProtocolStatus.BLOCKED);
    }


    /**
     * Build a FetchData from the nutch fetcher output.
     * @return The list of fetched documents. 
     */
    private FetchData buildFetchData(String segmentDir, FetchList fetchlist) {
        FetchData fetchdata = new FetchData();
        NutchSegment segment = new NutchSegment(segmentDir);
        ArrayList<SegmentRecord> unknownPages = new ArrayList<SegmentRecord>();
        HashMap<String,String> redirects = new HashMap<String,String>();
        com.flaptor.hounder.crawler.pagedb.Page page;
        for (SegmentRecord rec : segment) {
            // Get original page from the fetchlist
            page = fetchlist.getPage(rec.origurl);
            if (null == page) {
                // This is the destination page of a redirect. 
                // At this point, we don't know this url and have no info on 
                // the original url, so we store the fetched data for later use.
                unknownPages.add(rec);
            } else {
                if (!rec.origurl.equals(rec.newurl)) {
                    // This is the original page of a redirect.
                    // At this point, we don't hace the fetched data, but we do 
                    // have the original and the new url, so we store the mapping.
                    redirects.put(rec.newurl, rec.origurl);
                } else {
                    // This is a non-redirected page.
                    fetchdata.addDoc(new FetchDocument(page, rec.origurl, rec.content, rec.header, success(rec), recoverable(rec), internalError(rec), true));
                }
            }
        }
        // Now go through the redirects.
        for (SegmentRecord rec : unknownPages) {
            FetchDocument doc = null;
            if (redirects.containsKey(rec.newurl)) {
                rec.origurl = redirects.get(rec.newurl);
                page = fetchlist.getPage(rec.origurl);
                if (null != page) {
                    // Override URL with fetched URL if the fetcher is configured to do so
                    if (!keepUrl) {
                        try {
                            page.setUrl(rec.newurl);
                        } catch (MalformedURLException e) {
                            logger.debug("Malformed redirect url. Keeping original url.",e);
                        }
                    }
                    // finally we could reconstruct the redirect and can now store the page.
                    doc = new FetchDocument(page, rec.origurl, rec.content, rec.header, success(rec), recoverable(rec), internalError(rec), true);
                }
                if (null != doc) {
                    fetchdata.addDoc(doc);
                } else {
                    logger.error("Unknown page fetched. This is a bug in Nutch9Fetcher.");
                }
            }
        }
        return fetchdata;
    }


    private class SegmentRecord {
        public String origurl;
        public String newurl;
        public byte status;
        public int protocol_code;
        public Map<String,String> header;
        public byte[] content;
    }


    private class NutchSegment implements Iterable<SegmentRecord> {
        String segmentDir;

        public NutchSegment(String segmentDir) {
            this.segmentDir = segmentDir;
        }

        public Iterator<SegmentRecord> iterator() {
            return new SegmentIterator(segmentDir);
        }


        private class SegmentIterator implements Iterator<SegmentRecord> {
            private int FetchlistData = 0;
            private int StatusData = 1;
            private int ContentData = 2;
            private ArrayList<ArrayList<Path>> parts;
            private SequenceFile.Reader[] readers = null;
            private boolean hasnext;
            private SegmentRecord currRec;
            private Configuration conf;

            public SegmentIterator(String segmentDir) {
                conf = new Configuration();
                conf.addDefaultResource("nutch-default.xml");
                conf.addDefaultResource("nutch-site.xml");
                parts = new ArrayList<ArrayList<Path>>();
                parts.add(FetchlistData, getParts(segmentDir, CrawlDatum.GENERATE_DIR_NAME));
                parts.add(StatusData, getParts(segmentDir, CrawlDatum.FETCH_DIR_NAME));
                parts.add(ContentData, getParts(segmentDir, Content.DIR_NAME));
                readers = new SequenceFile.Reader[4];
                hasnext = true;
                logger.debug("ITERATOR INIT");
                advance();
            }

            public boolean hasNext() {
                if (logger.isDebugEnabled()) {
                    logger.debug("ITERATOR HAS_NEXT? "+hasnext);
                }
                return hasnext;
            }

            public SegmentRecord next() {
                if (!hasnext) return null;
                if (logger.isDebugEnabled()) {
                    logger.debug("ITERATOR NEXT "+currRec.origurl);
                }
                SegmentRecord rec = currRec;
                advance();
                return rec;
            }

            private ArrayList<Path> getParts(String segmentDir, String name) {
                File segment = new File(segmentDir, name);
                ArrayList<Path> partList = new ArrayList<Path>();
                if (null != segment && null != segment.listFiles()) {
                    for (File part : segment.listFiles()) {
                        if (part.getName().startsWith("part-")) {
                            File target;
                            if (part.isFile()) {
                                target = part;
                            } else {
                                target = new File(part,"data");
                            }
                            partList.add(new Path(target.getAbsolutePath()));
                        }
                    }
                }
                return partList;
            }

            private boolean advanceReader(int i) {
                if (null == readers[i]) {
                    if (parts.get(i).size() > 0) {
                        try {
                            readers[i] = new SequenceFile.Reader(FileSystem.get(conf), parts.get(i).remove(0), conf);
                        } catch (IOException e) {
                            logger.error("Reading a nutch segment",e);
                            hasnext = false;
                        }
                    } else {
                        hasnext = false;
                    }
                }
                return hasnext;
            }

            private void closeReader(int i) {
                Execute.close(readers[i]);
                readers[i] = null;
            }

            private void advance() {
                currRec = new SegmentRecord();
                if (!hasnext) return;

                logger.debug("  ADVANCING...");
                try {

                    logger.debug("   Status...");
                    while (advanceReader(StatusData)) {
                        logger.debug("     got reader");
                        Text key = new Text();
                        CrawlDatum value = new CrawlDatum();
                        if (readers[StatusData].next(key, value)) {
    //                        value.getFetchInterval();
                            currRec.status = value.getStatus();
                            ProtocolStatus pstatus = (ProtocolStatus) value.getMetaData().get(Nutch.WRITABLE_PROTO_STATUS_KEY);
                            currRec.protocol_code = (null == pstatus) ? 0 : pstatus.getCode();
                            if (logger.isDebugEnabled()) {
                                logger.debug("       STATUS OF "+key.toString()+": "+currRec.status+" "+CrawlDatum.getStatusName(currRec.status)+" (code "+currRec.protocol_code+")");
                            }
                            break;
                        } else {
                            closeReader(StatusData);
                        }
                    }

                    logger.debug("   Content...");
                    while (advanceReader(ContentData)) {
                        logger.debug("     got reader");
                        Text key = new Text();
                        Content value = new Content();
                        if (readers[ContentData].next(key, value)) { 
                            if (logger.isDebugEnabled()) {
                                logger.debug("       CONTENT OF "+key.toString()+":");
                            }
                            currRec.origurl = value.getUrl();
                            currRec.newurl = value.getBaseUrl();
                            currRec.content = value.getContent();
                            Metadata metadata = value.getMetadata();
                            currRec.header = new HashMap<String,String>();
                            for (String name : metadata.names()) {
                                String data = metadata.get(name);
                                currRec.header.put(name.toLowerCase(),data);
                            }
                            currRec.header.remove("nutch.content.digest");
                            currRec.header.remove("nutch.crawl.score");
                            currRec.header.remove("nutch.segment.name");
                            if (currRec.status == CrawlDatum.STATUS_FETCH_REDIR_PERM
                                    || currRec.status == CrawlDatum.STATUS_FETCH_REDIR_TEMP) {
                                currRec.newurl = currRec.header.get("location");
                            }
                            if (logger.isDebugEnabled()) {
                                logger.debug("         origurl: "+currRec.origurl);
                                logger.debug("         new url: "+currRec.newurl);
                                logger.debug("         content: "+currRec.content.length+" bytes");
                                logger.debug("         metadat: "+currRec.header);
                            }
                            break;
                        } else {
                            closeReader(ContentData);
                        }
                    }

                } catch (IOException e) {
                    logger.error("Reading data from a nutch segment",e);
                    hasnext = false;
                }
            }

            public void remove() {
                throw new IllegalStateException("This operation is not implemented.");
            }
        }

    }


}

