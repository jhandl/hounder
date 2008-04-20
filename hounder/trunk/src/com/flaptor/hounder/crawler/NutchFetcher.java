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
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.nutch.db.Page;
import org.apache.nutch.fetcher.FetcherOutput;
import org.apache.nutch.fs.NutchFileSystem;
import org.apache.nutch.io.ArrayFile;
import org.apache.nutch.net.BasicUrlNormalizer;
import org.apache.nutch.pagedb.FetchListEntry;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.parse.ParseStatus;
import org.apache.nutch.protocol.Content;
import org.apache.nutch.protocol.ProtocolStatus;

import com.flaptor.hounder.crawler.modules.FetchDocument;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;

/**
 * This class implements a wrapper around the Nutch fetcher, 
 * to be used as a plugin in the Hounder Crawler.
 * @author Flaptor Development Team
 */
public class NutchFetcher implements IFetcher {

    private static Logger logger = Logger.getLogger(Execute.whoAmI());
    private static BasicUrlNormalizer urlNormalizer;
    private String segmentsDir;
    private boolean keepUrl;

    /**
     * Initialize the fetcher.
     */
    public NutchFetcher() {
        Config config = Config.getConfig("nutchfetcher.properties");
        segmentsDir = config.getString("fetchlist.dir");
        keepUrl = config.getBoolean("keep.original.url.on.redirect");
        urlNormalizer = new BasicUrlNormalizer();
    }

    /**
     * Fetch the provided list of pages and produce a resulting list of fetched data.
     * @param fetchlist the list of pages to fetch.
     * @return a list of fetched pages.
     */
    public FetchData fetch(FetchList fetchlist) throws Exception {
        String segmentDir = buildSegment(fetchlist);
        try{
        	org.apache.nutch.fetcher.Fetcher.main(new String[] { segmentDir } );
        } catch (Throwable t) {
        	logger.error(t,t);
        	throw new Exception(t);
        }
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

    /**
     * Create a nutch fetchlist segment from the provided list of pages.
     * @param fetchlist the list of pages from which to build the segment.
     */
    private String buildSegment(FetchList fetchlist) throws IOException {
        // create the segment dir
        String segmentDir = getNewSegmentDir();
        File file = new File(segmentDir, FetchListEntry.DIR_NAME);
        NutchFileSystem lfs = org.apache.nutch.fs.NutchFileSystem.getNamed("local");
        ArrayFile.Writer af = new ArrayFile.Writer(lfs, file.getPath(), FetchListEntry.class);
        // Write the pages to the segment, saving the url in the anchors list for redirect following.
        // This trick is needed because nutch does not report redirect linkbacks.
        for (com.flaptor.hounder.crawler.pagedb.Page page : fetchlist) {
            try {
                Page pg = new Page(page.getUrl(), page.getScore());
                FetchListEntry fle = new FetchListEntry(true, pg, new String[]{page.getUrl()});
                af.append(fle);
            } catch (MalformedURLException e) {
                logger.warn("MalformedURLException adding ["+page.getUrl()+"] to the nutch fetchlist segment.");
            }
        }
        // close the segment
        Execute.close(af);
        return segmentDir;
    }

    /**
     * Tell if the fetch was successful for this record.
     * @return true if it was successful, false otherwise.
     */
    private static boolean success (FetcherOutput fo) {
        ProtocolStatus protStatus = fo.getProtocolStatus();
        return protStatus.isSuccess();
    }

    /**
     * Determine if the fetch error is recoverable.
     * @return true if the fetch error is recoverable.
     */
    private static boolean recoverable (FetcherOutput fo) {
        boolean canRecover = true;
        ProtocolStatus protStatus = fo.getProtocolStatus();
        int protCode = protStatus.getCode();
        switch (protCode) {
            case ProtocolStatus.GONE:
            case ProtocolStatus.MOVED:
            case ProtocolStatus.TEMP_MOVED:
            case ProtocolStatus.NOTFOUND:
            case ProtocolStatus.ACCESS_DENIED:
            case ProtocolStatus.ROBOTS_DENIED:
            case ProtocolStatus.REDIR_EXCEEDED:
                canRecover = false;
                break;
            case ProtocolStatus.EXCEPTION:
                String[] args = protStatus.getArgs();
                if (args.length > 0) {
                    if (args[0].endsWith("404")) {
                        canRecover = false;
                    }
                }
                break;
        }
        return canRecover;
    }

    /**
     * Obtain the redirected url, or null if there is no redirect.
     * @return null if no redirect, the new url if there is a redirect.
     */
    private String getRedirect(FetcherOutput fo, ParseData pd) {
        String newurl = null; // assume no redirect
        ProtocolStatus protStatus = fo.getProtocolStatus();
        switch (protStatus.getCode()) {
            case ProtocolStatus.SUCCESS:
                ParseStatus parseStatus = pd.getStatus();
                if (null != parseStatus && parseStatus.getMinorCode() == ParseStatus.SUCCESS_REDIRECT) {
                    newurl = parseStatus.getMessage();
                }
                break;
            case ProtocolStatus.MOVED:
            case ProtocolStatus.TEMP_MOVED:
                newurl = protStatus.getMessage();
                break;
        }
        if (null != newurl) {
            try {
                newurl = urlNormalizer.normalize(newurl);
            } catch (MalformedURLException e) {
                logger.warn("MalformedURLException trying to normalize the redirect url ["+newurl+"]");
            }
        }
        return newurl;
    }


    /**
     * Build a FetchData from the nutch fetcher output.
     * @return The list of fetched documents. 
     */
    private FetchData buildFetchData(String segmentDir, FetchList fetchlist) {
        FetchData fetchdata = new FetchData();
        SegmentRecord sr = new SegmentRecord(segmentDir);
        HashMap<String,com.flaptor.hounder.crawler.pagedb.Page> redirs = new HashMap<String,com.flaptor.hounder.crawler.pagedb.Page>();
        while (sr.next()) {
            FetcherOutput fo = sr.getFetcherOutput();
            ParseData pd = sr.getParseData();
            Content cnt = sr.getContent();
            FetchListEntry fle = fo.getFetchListEntry();
            String url = sr.getFetchedUrl();
            if (null == url) {
                logger.warn("The fetched url is null. Skipping page.");
                continue;
            }
            com.flaptor.hounder.crawler.pagedb.Page page = null;
            // reconstruct old page url form the anchors data array
            String origurl;
            String[] anchors = fle.getAnchors();
            if (anchors.length == 1) {
                // This page was stored in the segment by us.
                origurl = anchors[0];
                page = fetchlist.getPage(origurl);
                if (null == page) {
                    // This should never happen.
                    logger.error("Page not recognized ("+origurl+"). Skipping.");
                    continue;
                }
                // Lets see if this page has a redirect.
                String newurl = getRedirect(fo,pd);
                if (null != newurl) {
                    // Redirect, store new url and the original page for later use.
                    redirs.put(newurl, page);
                    continue;
                } // if null (meaning no redirect), simply emit.
            } else {
                // The page has no stored origurl. It was added by the fetcher, as a redir destination.
                // At this point, we should have seen the redir pointing to this page, so we retrieve it.
                page = redirs.get(url);
                if (null == page) {
                    // The destination page of a redirect was written before the starting page.
                    // According to the nutch 0.7.2 fetcher code, this is impossible.
                    logger.error("Page not recognized ("+url+"), redirect came before original page in segment. Skipping.");
                    continue;
                }
                origurl = page.getUrl();
                if (!keepUrl) {
                	// Override original url with redirected url.
                	try {
                		page.setUrl(url);
                	} catch (MalformedURLException e) {
                		logger.warn("Malformed redirect url ("+url+"). Skipping.");
                		continue;
                	}
                }
            }

            boolean changed = true; // nutch ignores the server change status of the page.
            boolean success = success(fo);
            boolean recoverable = recoverable(fo);

            // parse java.util.Properties, inserting lowercased keys on header map.
            Map<String,String> header = new HashMap<String,String>();
            Map<Object,Object> props = cnt.getMetadata();
            for (Object key: props.keySet()) {
            	String value = (String)props.get(key);
            	header.put(((String)key).toLowerCase(),value);
            }

            FetchDocument doc = new FetchDocument(page,
            		url,
            		cnt.getContent(),
            		header,
            		success,
            		recoverable,
            		changed);

            fetchdata.addDoc(doc);

        }
        sr.close();
        return fetchdata;
    }


}

