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
package com.flaptor.search4j.cache;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.flaptor.util.Config;
import com.flaptor.util.Pair;
import com.flaptor.util.PortUtil;

/**
 * Utility class that gives you the URL of the cached version or a URL
 * To be used by various frontends
 * 
 * @author Flaptor Development Team
 */
public class CachedVersionUtil {
	
    private static final boolean showCachedVersionLink;
    private static final String cacheServerURL; 
    
    static {
    	Config searcherConfig = Config.getConfig("searcher.properties");
    	showCachedVersionLink = searcherConfig.getBoolean("searcher.cachedVersion.showLink"); 
    	Pair<String,Integer> cacheServer = PortUtil.parseHost(searcherConfig.getString("searcher.cachedVersion.cacheServerHost"), "cacheServer.http");
    	cacheServerURL ="http://"+ cacheServer.first() + ":" + cacheServer.last() + "/?URL=";
    }
	
    /**
     * @return true if the URL has a cached version
     * 
     * TODO now it doesnt really look if the URL has a cached version 
     */
    public static boolean showCachedVersion(String url) {
    	return showCachedVersionLink;
    }

    /**
     * @return the URL of the cached version (of the param url) in the cache server
     */
    public static String getCachedVersionURL(String url) {
        String encodedURL; 
        try {
            encodedURL = URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Should never happen!
        	encodedURL = "";
        }
    	return cacheServerURL+encodedURL;
    }
}
