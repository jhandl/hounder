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

import org.apache.log4j.Logger;

import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.PortUtil;
import com.flaptor.util.cache.FileCache;
import com.flaptor.util.remote.RmiServer;

/**
 * @author Flaptor Development Team
 */
public class CacheModule extends AProcessorModule {

    private static Logger logger = Logger.getLogger(Execute.whoAmI());

    private FileCache<String> textCache = null; // cache for the parsed text of the fetched pages.
    private FileCache<DocumentCacheItem> pageCache = null; // cache for the original contents of the fetched pages.
    private int textLengthLimit; // the maximum allowed page text length.
    private RmiServer cacheRmiServer;   
    
    public CacheModule (String moduleName, Config globalConfig) {
        super(moduleName, globalConfig);
        Config mdlConfig = getModuleConfig();
        String textDir = mdlConfig.getString("text.cache.dir");
        if (textDir.length() > 0) {
            textCache = new FileCache<String>(textDir);
        }
        String pageDir = mdlConfig.getString("page.cache.dir");
        if (pageDir.length() > 0) {
            pageCache = new FileCache<DocumentCacheItem>(pageDir);
        }
        textLengthLimit = globalConfig.getInt("page.text.max.length");
        
        cacheRmiServer = new RmiServer(PortUtil.getPort("crawler.pageCache.rmi")); 
        cacheRmiServer.addHandler(RmiServer.DEFAULT_SERVICE_NAME, pageCache);

        cacheRmiServer.start();
    }

    public FileCache<String> getTextCache () {
        return textCache;
    }
    
    public FileCache<DocumentCacheItem> getPageCache () {
        return pageCache;
    }
    
    protected String getKey(FetchDocument doc){
        return doc.getPage().getUrl();
    }
    
    @Override
    protected void internalProcess (FetchDocument doc) {    
        Page page = doc.getPage();
        if (null == page) {
            logger.warn("Fetchdata does not have a page");
            return;
        }
        String text = getDocumentText(doc);
        if (null == text) {
            logger.warn("Document does not have parsed text.");
            return;
        }
        DocumentCacheItem item = new DocumentCacheItem(doc.getContent(), doc.getMimeType());
        if (null == item) {
            logger.warn("Document does not have the original content.");
            return;
        }
        
        if (null != textCache) {
            textCache.addItem(getKey(doc), text);
        }
        if (null != pageCache) {
            pageCache.addItem(getKey(doc), item);
        }
    }

	protected String getDocumentText(FetchDocument doc) {
		return doc.getText(textLengthLimit);
	}

}
