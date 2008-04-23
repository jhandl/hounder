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
import com.flaptor.util.FileUtil;

/**
 * Matches the url of the page against a url pattern file
 * @author rafa, jorge
 */
public class MatchUrlModule extends ATrueFalseModule {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    
    private UrlPatterns patterns; // list of grep patterns a url must match to become a hotspot.
    
	/**
	 * @todo UrlPatterns are not singleton, but should be. @link UrlPatterns
	 */
    public MatchUrlModule (String name, Config globalConfig) throws IOException{
        super(name, globalConfig);
        String filename = getModuleConfig().getString("url.pattern.file");
        String filepath = FileUtil.getFilePathFromClasspath(filename);
        patterns = new UrlPatterns(filepath); // TODO: hotspots should be singleton, otherwise we have two copies in ram.
    }
    
    @Override
    public Boolean tfInternalProcess (FetchDocument doc) {
        Page page = doc.getPage();
        if (null == page) {
            logger.warn("Page is null. Ignoring document.");
            return null;
        }
        return patterns.match(page.getUrl());
    }

}
