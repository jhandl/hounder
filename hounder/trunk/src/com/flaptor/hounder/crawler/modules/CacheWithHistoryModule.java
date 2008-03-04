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
package com.flaptor.hounder.crawler.modules;

import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.cache.FileCache;

/** 
 * Is a CacheModule, but pages are stored with URL+DATE as key.
 * This way when a page changes, the old one is still saved.
 * @author rafa
 *
 */
public class CacheWithHistoryModule extends CacheModule {
    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(Execute.whoAmI());
    
    /**
     * We store in disk, for each URL which dates we have cached it.
     * The first long is the timestamp of the crawling.
     * The second is the actual caching time closest previous to the crawler.
     * IE: if a page was crawled 5 times and changed only at the 4th crawl, we will have:
     *      [123, 123] 
     *      [128, 123]
     *      [133, 123]
     *      [138, 138]
     *      [143, 138]
     *  
     */
    private FileCache<SortedMap<Long, Long>> dateCache = null;
    

    public CacheWithHistoryModule (String moduleName, Config globalConfig) {
        super(moduleName, globalConfig);
        String dateDir = config.getString("date.cache.dir");
        dateCache= new FileCache<SortedMap<Long,Long>>(dateDir);
    }
    
    @Override
    protected String getKey(FetchDocument doc){
        return IndexerWithHistoryModule.getKey(doc.getPage());
    }
    
    /**
     * If the page has not changed since last time we saw it, we still save the
     * date
     * @param doc 
     */
    protected void addToKnownDates(FetchDocument doc){        
        long now = doc.getPage().getLastAttempt();
        String url = doc.getPage().getUrl();
        SortedMap<Long, Long> item = dateCache.getItem(url);
        if ( null == item){
            item = new TreeMap<Long, Long>();
        }
        if (doc.pageTextChanged()){
            item.put(now, now);
            
        } else {
        	Long prevCache = item.get(item.lastKey());
            item.put(now, prevCache);
        }
       dateCache.addItem(url, item); 
    }
    
    @Override
    protected void internalProcess (FetchDocument doc) {
        if (doc.pageTextChanged()){
            super.internalProcess(doc);
        }
        addToKnownDates(doc);
    }
    
    @Override
    protected String getDocumentText(FetchDocument doc) {
    	// we cache the entire document
    	return doc.getText();
    }
    
}
