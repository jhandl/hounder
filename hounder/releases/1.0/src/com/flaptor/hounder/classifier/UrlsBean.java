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
package com.flaptor.hounder.classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.flaptor.hounder.crawler.CacheBean;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;

/**
 * This bean is in charge of the 'urls' file. Other Beans should not modify it.
 * @author rafa
 *
 */
public class UrlsBean extends TrainingBean {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private CacheBean cache;
    /**
     * STOPPING: A stop request was sent (Probably needs to wait until finishing current cycle).
     * STOPPED: The bgfetcher was not started, or was started and stoped
     * FINISHED: The bgFetcher finished fetching all the URLs
     * RUNNING: The bgFetcher is running
     */
    public enum BGFetcherStatus {STOPPING, STOPPED, FINISHED, RUNNING};
    private BGFetcherStatus bgfStatus= BGFetcherStatus.STOPPED;
    
    public UrlsBean() {
    }
    
    public boolean initialize(ConfigBean config, CacheBean cache) {
        super.initialize(config);
        this.cache= cache;
        inited= true;
        return inited;
    }
    
    public List<String> getUrls(){
        return getUrls(config);       
    }
        
    /**
     * Given a url, returns true iff the url was alreaady fetched and is in the
     *  cache.
     */
    public boolean getUrlState(String url){
        return cache.isInTextCache(url);
    }
    
    public List<String> getUrls(String url){
        List<String> inputData= new ArrayList<String>();
        String ftl= config.getBaseDir()+File.separator+config.getUrlFile();
        if (!FileUtil.fileToList(null, ftl,inputData)) {
            throw new IllegalStateException("Couldn't load file " + ftl);
        }
        return inputData;
    }
    
    public static List<String> getUrls(ConfigBean config){
        List<String> inputData= new ArrayList<String>();
        String ftl= config.getBaseDir()+File.separator+config.getUrlFile();
        if (!FileUtil.fileToList(null, ftl,inputData)) {
            throw new IllegalStateException("Couldn't load file " + ftl);
        }
        for (int i=0; i< inputData.size(); i++){
            String ln= inputData.get(i);
            if (ln.matches("^\\s*$")){
                inputData.remove(i);
                logger.warn("Ignoring empty URL from line " + i + 
                        " on file " + ftl);
                /*throw new IllegalArgumentException("The url at line " + i 
                        + " of " + ftl + " is invalid (empty)");*/
            }
        }
        return inputData;
    }

    public void startBGFetcher(boolean refetch){        
        Set<String> urls = new HashSet<String>(getUrls());
        CacheBGFetcher bgFetch= new CacheBGFetcher(urls, refetch);
        bgfStatus= BGFetcherStatus.RUNNING;
        bgFetch.start();
    }
    
    public void stopBGFetcher(){        
        bgfStatus= BGFetcherStatus.STOPPING;
    }

    public BGFetcherStatus getBGFetcherStatus() {
        return bgfStatus;
    }

    /**
     * This class reads the urls file, and start fetching the urls to the cache
     * @author rafa
     *
     */
    private class CacheBGFetcher extends Thread{
        private final int FETCHLIST_SIZE= 5000;
        private  Set<String> urls;
        private boolean refetch;
        
        public CacheBGFetcher(Set<String> urls, boolean refetch){
            this.urls = urls;
            this.refetch= refetch;
        }

        public void run(){
            bgfStatus= BGFetcherStatus.RUNNING;
            int i=0;
            Set<String> subSet= new HashSet<String>();
            logger.info("Going to fetch 1st " + FETCHLIST_SIZE);
            for (String url:urls){                
                if (refetch || !cache.isInTextCache(url)){
                    logger.debug("************* Adding to fetchlist (" + i + "):" + url);
                    subSet.add(url);
                    i= i+1;
                }  else {
                    logger.debug("*******Already in cache: not adding to fetchlist (" + i + "):" + url);
                }
                if (BGFetcherStatus.STOPPING == bgfStatus){
                    logger.info("Stoping fetcher.");
                    bgfStatus= BGFetcherStatus.STOPPED;
                    return;
                }
                if (FETCHLIST_SIZE == i){
                    cache.fetchPages(subSet);
                    subSet.clear();
                    i=0;
                    logger.info(" 5000 fetched. Going to fetch next 5000");
                }
                if (BGFetcherStatus.STOPPING == bgfStatus){
                    logger.info("Stoping fetcher.");
                    bgfStatus= BGFetcherStatus.STOPPED;
                    return;
                }
            }
            logger.info("Going to fetch last list");
            if (0 < subSet.size()){
                cache.fetchPages(subSet);
            }
            logger.info("Last list fetched.");
            bgfStatus= BGFetcherStatus.FINISHED;
        }
    }

    public static void main (String[] args){
        ConfigBean config= new ConfigBean();
        CacheBean cacheB= new CacheBean();
        String cacheDir= args[1];
        String baseDir=args[2];
        String urlFile=args[3];
        boolean refetch= false;
        if (args.length == 5 && args[4].equalsIgnoreCase("refetch")){
        	refetch=true;
            logger.debug("Forcing refetch");
        } else {
            logger.debug("Not forcing refetch");
        }
        config.initialize(null, cacheDir, baseDir, urlFile, -1);
        cacheB.initialize(config.getCacheDir(), true);
        UrlsBean ub= new UrlsBean();
        ub.initialize(config,cacheB);
        ub.startBGFetcher(refetch);        
    }
}
