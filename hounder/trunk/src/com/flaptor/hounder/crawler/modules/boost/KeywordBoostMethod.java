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

import java.io.IOException;
import java.util.Set;

import com.flaptor.hounder.crawler.UrlPatterns;
import com.flaptor.hounder.crawler.modules.FetchDocument;
import com.flaptor.util.Config;
import com.flaptor.util.FileUtil;

/**
 * @author Flaptor Development Team
 */
public class KeywordBoostMethod extends ABoostMethod{

    private final String field;
    private UrlPatterns patterns;

    public KeywordBoostMethod(Config config){
        super(config);
        field = config.getString("method.keyword.applyto.field");
        String filename = config.getString("method.keyword.patterns.file");
        String filepath = FileUtil.getFilePathFromClasspath(filename);
        try {
            patterns = new UrlPatterns(filepath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void applyBoost(FetchDocument doc, double value) {

        String indexableFieldContent = (String)doc.getIndexableAttribute(field);
        String url = doc.getPage().getUrl();

        Set<String> keywords = patterns.getTokens(url);

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < (int) value; i++ ) {
            for (String keywordValue: keywords) {
                sb.append(" " + keywordValue + " " );
            }
        }

        doc.setIndexableAttribute(field, indexableFieldContent + sb.toString());
    }
}
