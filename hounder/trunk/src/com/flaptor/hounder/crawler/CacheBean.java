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
package com.flaptor.search4j.crawler;

import java.io.File;
import java.util.Set;

import org.apache.log4j.Logger;

import com.flaptor.search4j.crawler.modules.FetchDocument;
import com.flaptor.search4j.crawler.pagedb.Page;
import com.flaptor.util.Execute;
import com.flaptor.util.cache.FileCache;

/**
 * @author Flaptor Development Team
 */
public class CacheBean {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private FileCache<String> textCache;
    private boolean shouldFetch;

    private boolean inited = false;

    /**
     * Ctor. 
     * You must call initialize before using this class' methods
     */
    public CacheBean() {
    }

    /**
     * 
     * @return true iff this bean has been already initialized
     */
    public boolean isInited() {
        return inited;
    }

    /**
     * Initializes this bean
     * 
     * @param cacheBaseDir The directory where the cache is.
     * @param shouldFetch 
     *      Sets what happend when a page required by the methods  
     *          {@link #getPage(String)} and {@link #getText(String)} is not in 
     *          the cache.
     *      If false, and the page is not in the cache, "" will be 
     *          returned
     *      If true, and a page is not in the cache, the page will be 
     *          first fetched, then saved into the cache and then returned.
     * 
     * @return
     */
    public boolean initialize(String cacheBaseDir, boolean shouldFetch) {
        String cacheT= cacheBaseDir + File.separator + "text"; // TODO: softcode "/text"
        textCache = new FileCache<String>(cacheT);
        inited = true;
        this.shouldFetch= shouldFetch;

        return inited;        
    }
    
    /**
     * The same as initialize(cacheBaseDir, false)
     * @param cacheBaseDir
     * @return
     */
    @Deprecated
    public boolean initialize(String cacheBaseDir /*, false*/) {
        return initialize(cacheBaseDir, false);
    }

    /**
     * Given a url, fetches it and saves it to the cache (text and page cache)
     * @param url
     * @return true iff the page was succesfully fetched and saved.
     */
    private IFetcher fetcher = new NutchFetcher();
    
    private boolean fetchPage(String url){
        float page_similarity_threshold=0f;
        float score= 0f;
        int textLengthLimit= 50000;
        try {
            Page page= new Page(url, score, page_similarity_threshold);
            FetchList fetchlist = new FetchList();
            fetchlist.addPage(page);
            fetchlist.close();
            FetchData fetchdata = fetcher.fetch(fetchlist);
            boolean fetched = false;
            for(FetchDocument doc: fetchdata) {
                fetched = true;
                String txt = doc.getText(textLengthLimit);
                textCache.addItem(url, txt);
            }
            if (!fetched){                
                throw new RuntimeException("Unable to fetch page: " + url);
            }
        } catch (Exception e) {
            logger.error(e,e);
            return false;
        }
        return true;
    }
    
    public boolean fetchPages(Set<String> urls){
        float page_similarity_threshold=0f;
        float score= 0f;
        int textLengthLimit= 50000;
        try {
            FetchList fetchlist = new FetchList();
            for (String url: urls){
                Page page= new Page(url, score, page_similarity_threshold);                
                fetchlist.addPage(page);
            }
            fetchlist.close();
            FetchData fetchdata = fetcher.fetch(fetchlist);
            for (FetchDocument doc : fetchdata) {
                String txt = doc.getText(textLengthLimit);
                String url = doc.getPage().getUrl();
                textCache.addItem(url, txt);
                logger.debug("Fetched " + url);
            }
        } catch (Exception e) {
            logger.error(e,e);
            return false;
        }
        return true;
    }


    /**
     * Returns the text in the cache, for the given key
     * (ie the parsed text of an html page) 
     * @param key
     * @return
     */
    public String getText(String key) {
        return getText(key,false);
    }

    /**
     * Returns the data for the given key (ie: url) in the text-cache 
     * If there is no such key in the cache and shouldFetch is true, the
     * page is fetched, then stored in the cache, then the requested data
     * is return;
     * If there is no such key in the cache and shouldFetch is false, "" is
     * returned.
     * If any error, "" is returned.
     * @param key
     * @return the data on the text-che for the given key.
     */
     public String getText(String key, boolean refetch) {      
        String data = textCache.getItem(key);
        if (refetch || (null == data && shouldFetch) ){
            logger.debug("Start fetching");
            System.err.println("Start fetching " + key);
            if (!fetchPage(key)){
                logger.warn("Couldn't fetch page: " + key);
            }
            data = textCache.getItem(key);
            logger.debug("Fetching done:" + key);            
        } else {
            logger.debug("Not fetching, already in cache");
        }
       return (data != null) ? data : "";
    }
    
    public boolean isInTextCache(String key){
        return (null != textCache.getItem(key));
    }


    /**
     * Returns the page in the cache, for the given key
     * (ie a full html page) 
     * @param key
     * @return
     *
    public String getPage(String key) {
        String pg= getData(pageCache, key, false) ;
        return pg;
    }*/



    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: CacheBean <dir> <fetch|nofetch> <url>");
            return;
        }
        CacheBean bean = new CacheBean();
        bean.initialize(args[0], "fetch".equalsIgnoreCase(args[1]));;
        String url=args[2];
        System.err.println(new Boolean(bean.isInTextCache(url)));
        System.out.println("Page: ");               
        //System.out.println(bean.getPage(url));
        System.err.println(new Boolean(bean.isInTextCache(url)));
        System.out.println("Text: ");
        System.out.println(bean.getText(url));
        System.err.println(new Boolean(bean.isInTextCache(url)));
    }

}

