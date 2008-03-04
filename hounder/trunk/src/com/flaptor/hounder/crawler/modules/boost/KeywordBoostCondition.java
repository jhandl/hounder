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
package com.flaptor.hounder.crawler.modules.boost;

import java.util.HashSet;
import java.util.Set;

import com.flaptor.hounder.crawler.modules.FetchDocument;
import com.flaptor.util.Config;


/**
 * This boost condition is true if any one of the specified keywords is present in a document.
 * 
 * @author Flaptor Development Team
 */
public class KeywordBoostCondition extends ABoostCondition {

    // the list of keywords
    private Set<String> keywords = new HashSet<String>();

    /**
     * Read and store the keywords from the config file.
     * @param config the configuration file.
     */
    public KeywordBoostCondition (Config config) {
        super(config);
        String[] keywordList = config.getStringArray("condition.keywords");
        for (String keyword: keywordList) {
            keywords.add(keyword);
        }
    }


    /** 
     * Check the document tokens, and if any of them is in the keywords set, return true.
     * @param doc the fetched document.
     */
    public boolean eval (FetchDocument doc){
        String text = doc.getText();
        if (null != text) {
            String[] tokens = text.split(" ");
            for (String token: tokens) {
                if (keywords.contains(token)) {
                    return true;
                }
            }
        }
        return false;
    }

}

