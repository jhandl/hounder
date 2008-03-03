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

import org.apache.log4j.Logger;
import org.apache.nutch.analysis.lang.LanguageIdentifier;

import com.flaptor.util.Config;
import com.flaptor.util.Execute;

/**
 * Process a document checking it's language. Adds a TAG_LANGUAGE_lang tag
 * to the FetcDocument (ie: TAG_LANGUAGE_es)
 * @author rafa
 *
 */
public class LanguageDetectionModule extends AProcessorModule {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private static LanguageIdentifier langIdentif= LanguageIdentifier.getInstance();

    public static final String LANGUAGE_TAG="TAG_LANGUAGE_";

    public LanguageDetectionModule(String name, Config globalConfig) {
        super(name, globalConfig);
    }


    @Override
    protected void internalProcess(FetchDocument doc) {;
        synchronized (this) {
            String lang= langIdentif.identify(doc.getText());
            try {
                doc.addTag(LANGUAGE_TAG + lang);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
