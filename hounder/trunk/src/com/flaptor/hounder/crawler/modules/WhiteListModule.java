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

import java.io.IOException;

import org.apache.log4j.Logger;

import com.flaptor.hounder.crawler.UrlPatterns;
import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;

/**
 * Sets the IS_HOTSPOT
 * @author rafa
 *
 */
public class WhiteListModule extends ATrueFalseModule {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    
    private UrlPatterns patterns; // list of grep patterns a url must match to become a hotspot.
    
	/**
	 * @todo UrlPatterns are not singleton, but should be. @link UrlPatterns
	 */
    public WhiteListModule (String name, Config globalConfig) throws IOException{
        super(name, globalConfig);
        patterns = new UrlPatterns(getModuleConfig().getString("whitelist.file"));
    }
    
    @Override
    public Boolean tfInternalProcess (FetchDocument doc) {
        Page page = doc.getPage();
        if (null == page) {
            logger.warn("Page is null. Ignoring document.");
            return null;
        }
        boolean patternMatched = false; // for debugging/logging
        if (patterns.match(page.getUrl())) {
            doc.setTag(IS_HOTSPOT_TAG);
            patternMatched= true;
        }
        logger.debug("  " + (patternMatched?"":"not ") + "hotspot: "+ page.getUrl());
        return patternMatched;
    }

}
