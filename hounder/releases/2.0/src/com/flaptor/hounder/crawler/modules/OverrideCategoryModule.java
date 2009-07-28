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

import java.util.Set;

import org.apache.log4j.Logger;

import com.flaptor.util.Config;
import com.flaptor.util.Execute;

/**
 *  This module can override delete categories, if 2 categories are exclussive.
 *  In case there is an order of categories, lets says catA,catB,catC, and 
 *  a document appears that has catA and catC, only catA will "survive" this 
 *  module. If the document had catA, catB and catD, only catA and catD will
 *  survive.
 *
 * @author Flaptor Development Team
 */
public class OverrideCategoryModule extends AProcessorModule {

    private static Logger logger = Logger.getLogger(Execute.whoAmI());
    private final String[] overridableCategories;

    public OverrideCategoryModule(String moduleName, Config globalConfig) {
        super(moduleName, globalConfig);
        overridableCategories = getModuleConfig().getStringArray("categories.order");
        if (0 == overridableCategories.length) {
            String error = "Instantiated without categories to override";
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void internalProcess (FetchDocument doc) {

        // keep a copy of document categories.
        Set<String> categories = doc.getCategories();

        for (String category: overridableCategories){
            // if there are no categories, or just one, skip
            if (categories.size() < 2) {
                break;
            }
            if (categories.contains(category) && doc.removeCategory(category)) {
                if( logger.isDebugEnabled()) {
                    logger.debug("Deleted category \"" + category + "\" from document " + doc.getPage().getUrl());
                }
                // also, remove from local copy
                categories.remove(category);
            }
        }
    }

}
