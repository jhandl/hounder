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
package com.flaptor.search4j.crawler.modules.boost;

import java.io.IOException;
import java.util.Set;

import org.apache.log4j.Logger;

import com.flaptor.search4j.crawler.UrlPatterns;
import com.flaptor.search4j.crawler.modules.FetchDocument;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;

/**
 * This boost condition returns true if the document url matches a pattern file.
 * @author Flaptor Development Team
 */
public class UrlPatternBoostCondition extends ABoostCondition {

    private static Logger logger = Logger.getLogger(Execute.whoAmI());
    private UrlPatterns patterns;
    private String urlForValue = null;
    private double currValue;

    /**
     * Load the url patterns specified in the configuration.
     * @param config the configuration file.
     */
    public UrlPatternBoostCondition (Config config) {
        super(config);
        String filename = config.getString("condition.url.patterns.file");
        String filepath = FileUtil.getFilePathFromClasspath(filename);
        try {
            patterns = new UrlPatterns(filepath);
        } catch (IOException e) {   
            throw new RuntimeException(e);
        }
    }

    /**
     * If the document url matches any of the patterns, return true.
     * @param doc the fetched document.
     */
    public boolean eval (FetchDocument doc) {
        return patterns.match(doc.getPage().getUrl());
    }

    /**
     * returns true if the url pattern mathing this doc specifies a value.
     * @param doc the fetched document.
     * @return true if the url pattern mathing this doc specifies a value.
     */
    public boolean hasValue (FetchDocument doc) { 
        return (getValue(doc) >= 0);
    }

    /**
     * Returns the value specified in the pattern that matches this doc's url.
     * @param doc the fetched document.
     * @return the value specified in the pattern that matches this doc's url.
     */
    public double getValue (FetchDocument doc) {
        String url = doc.getPage().getUrl();
        if (null == urlForValue || !urlForValue.equals(url)) {
            Set<String> values = patterns.getTokens(doc.getPage().getUrl());
            currValue = -1;
            for (String value: values) {
                try {
                    double doubleValue = Float.parseFloat(value);
                    currValue = Math.max(currValue,doubleValue);
                } catch (NumberFormatException e) {
                    logger.error("Can not parse double from UrlPatterns!",e);
                    // continue
                }
            }
            if (-1 != currValue) {
                urlForValue = url;
            }
        }
        return currValue;
    }

}

