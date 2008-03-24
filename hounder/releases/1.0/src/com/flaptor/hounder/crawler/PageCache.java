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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.flaptor.hounder.crawler.modules.DocumentCacheItem;
import com.flaptor.hounder.crawler.modules.FetchDocument;
import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.hounder.crawler.pagedb.PageDB;
import com.flaptor.util.Execute;
import com.flaptor.util.cache.FileCache;

/**
 * @author Flaptor Development Team
 */
public class PageCache implements Iterable<FetchDocument>{

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private PageDB pagedb;
    private FileCache<DocumentCacheItem> cache;
    private long skip;
    private long seen;

    public PageCache (PageDB pagedb, FileCache<DocumentCacheItem> cache) {
        this(pagedb, cache, 0);
    }

    public PageCache (PageDB pagedb, FileCache<DocumentCacheItem> cache, long skip) {
        this.pagedb = pagedb;
        this.cache = cache;
        this.skip = skip;
        seen = 0;
    }

    public Iterator<FetchDocument> iterator () {
        try {
            return new PageCacheIterator();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public class PageCacheIterator implements Iterator<FetchDocument> {
        
        Iterator<Page> pages;
        FetchDocument doc = null;
        
        public PageCacheIterator () throws IOException {
            pages = pagedb.iterator();
            advance();
        }

        private void advance () {
            doc = null;
            while (null == doc && pages.hasNext()) {
                Page page = pages.next();
                String url = page.getUrl();
                if (cache.hasItem(url)) {
                    seen++;
                    if (seen > skip) {
                        byte[] content = cache.getItem(url).getContent();
                        Map<String,String> header = new HashMap<String,String>(); // this info is lost, it should be stored in the cache along with the page contents.
                        boolean success = true;
                        boolean recoverable = true;
                        boolean changed = false;
                        doc = new FetchDocument(page, url, content, header, success, recoverable, changed);
                    } else if (seen % 10000 == 0) {
                        logger.info("Skipped "+seen+" pages...");
                    }
                }
            }
        }

        public synchronized boolean hasNext () {
            return (null != doc);
        }


        public synchronized FetchDocument next () {
            FetchDocument ret = doc;
            if (null == doc) {  
                throw new NoSuchElementException("No more pages in the pagedb and cache");
            }
            advance();
            return ret;
        }

        public void remove () {
            throw new IllegalStateException("The remove operation is not implemented for PageCache");
        }

    }

}

