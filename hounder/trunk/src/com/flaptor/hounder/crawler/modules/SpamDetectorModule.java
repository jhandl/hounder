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

import org.apache.log4j.Logger;

import com.flaptor.search4j.crawler.pagedb.Page;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;

/**
 * Tries to detect spam
 * @author Flaptor Development Team
 */
public class SpamDetectorModule extends ATrueFalseModule {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private int maxTitleLength = 0;
    
	/**
     * Get the module configuration.
	 */
    public SpamDetectorModule (String name, Config globalConfig) throws IOException {
        super(name, globalConfig);
        maxTitleLength = config.getInt("max.title.length");
    }
    
    //    @Override
    public Boolean tfInternalProcess (FetchDocument doc) {
        boolean spam = false;
        try {
            Page page = doc.getPage();
            String title = doc.getTitle();
            if (title.length() > maxTitleLength) {
                spam = true;
            }
        } catch (NullPointerException e) {
            logger.error(e,e);
        }
        return spam;
    }

}
