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
package com.flaptor.search4j.crawler.modules;

import java.io.IOException;
import java.util.Set;

import org.apache.log4j.Logger;

import com.flaptor.search4j.crawler.UrlPatterns;
import com.flaptor.search4j.crawler.pagedb.Page;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;

/**
 * The PatternClassifier module keeps a large number of rules against 
 * which a string can be matched.
 * The rules are read from a file and are formed by a string prefix followed 
 * by categories to apply to documents matching that prefix.
 * 
 * @author Flaptor Development Team
 */
public class PatternClassifierModule extends ATrueFalseModule {

    private static Logger logger = Logger.getLogger(Execute.whoAmI());
    private UrlPatterns patterns ;


    /**
     * Class constructor. 
     * Reads the patterns file and builds the TrieTree that will be used to 
     * match urls.
     * @param moduleName
     *              The name of this module.
     * @param globalConfig
     *              The config that holds global values for all modules.
	 * @todo review the exception thrown on file problems.
     */
    public PatternClassifierModule(String moduleName,Config globalConfig) {
        super(moduleName, globalConfig);
        String filename = config.getString("patterns.file");
        if (null == filename || "".equals(filename)) {
            String error = "Invalid patterns.file filename.";
            error += " PatternClassifier can not work without a patterns file.";
            logger.error(error);
            // TODO check what kind of exception to throw
            throw new RuntimeException(error);
        }
        try {
            patterns = new UrlPatterns(filename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Boolean tfInternalProcess(FetchDocument doc) {
        Page page = doc.getPage();
        if (null == page) {
            logger.warn("Page is null. Ignoring document.");
            return false;
        }
        String url = page.getUrl();

        Set<String> foundCategories = patterns.getTokens(url);

        boolean foundSomething = false;
        for (String category: foundCategories) {
            doc.addCategory(category);
            foundSomething = true;
        }

        return foundSomething;
    }


}

