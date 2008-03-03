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

import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Element;

import com.flaptor.search4j.crawler.pagedb.Page;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;

/**
 * @author Flaptor Development Team
 */
public class IndexerWithHistoryModule extends IndexerModule {

	private static final Logger logger = Logger.getLogger(Execute.whoAmI());
	
    public IndexerWithHistoryModule(String moduleName, Config globalConfig) {
        super(moduleName, globalConfig);
    }
    
    public static String getKey(Page page){
        String url= page.getUrl();
        long time= page.getLastAttempt();
        return time + "_" + url;        
    }
    
    @Override
    protected String getDocumentId(Page page) {
        return getKey(page);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void internalProcess(FetchDocument doc) {

        Page page = doc.getPage();
        if (page == null) {
            logger.warn("Page is null. Ignoring this document.");
            return;
        }

        if (doc.hasTag(EMIT_DOC_TAG)) {

        	super.internalProcess(doc);

        } 
        
        /*
         *  Even when the page was already indexed and has lost its tag,
         *  the IndexerWithWithHistory needs not to delete it because 
         *  it keeps the page's history.
         */        
    }
    
    @Override
    protected void addAdditionalFields(Element root, Page page,
    		Map<String, Double> boostMap) {
       	super.addAdditionalFields(root, page, boostMap);
       	
       	long crawlTimestamp = page.getLastAttempt();
       	
       	addField(root, "crawlTimestamp", String.valueOf(crawlTimestamp), true, true, false, boostMap);
    }

}
