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

import com.flaptor.util.Config;
import com.flaptor.util.Execute;

/**
 * @author Flaptor Development Team
 */
public class TimeAttributesModule extends AProcessorModule {

    private static Logger logger = Logger.getLogger(Execute.whoAmI());
    private boolean modifiedDay =       false;
    private boolean modifiedHour =      false;
    private boolean modifiedMinute =    false;
    private boolean modifiedLong =      false;
    private boolean fetchedDay =        false;
    private boolean fetchedHour =       false;
    private boolean fetchedMinute =     false;
    private boolean fetchedLong =       false;

    public TimeAttributesModule (String moduleName, Config globalConfig) {
        super(moduleName, globalConfig);
        modifiedDay     = config.getBoolean("modification.day");
        modifiedHour    = config.getBoolean("modification.hour");
        modifiedMinute  = config.getBoolean("modification.minute");
        modifiedLong    = config.getBoolean("modification.long");
        fetchedDay      = config.getBoolean("fetch.day");
        fetchedHour     = config.getBoolean("fetch.hour");
        fetchedMinute   = config.getBoolean("fetch.minute");
        fetchedLong     = config.getBoolean("fetch.long");
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void internalProcess (FetchDocument doc) {

        String lastModified = doc.getLastModified();
        if (null == lastModified ) { 
            if (modifiedDay || modifiedHour || modifiedMinute || modifiedLong) {
                logger.debug("Page " + doc.getPage().getUrl() + " has null last-modified header. Modification data is not available." );
            }
        } else {
            long modified = Long.parseLong(lastModified);
            if (modifiedDay) doc.setIndexableAttribute("modifiedDay",modified/86400000L);
            if (modifiedHour) doc.setIndexableAttribute("modifiedHour",modified/3600000L);
            if (modifiedMinute) doc.setIndexableAttribute("modifiedMinute",modified/60000L);
            if (modifiedLong) doc.setIndexableAttribute("modifiedLong",modified);
        }

        String lastUpdated = doc.getLastUpdated();
        long fetched = Long.parseLong(lastUpdated);

        if (fetchedDay) doc.setIndexableAttribute("fetchedDay",fetched/86400000L);
        if (fetchedHour) doc.setIndexableAttribute("fetchedHour",fetched/3600000L);
        if (fetchedMinute) doc.setIndexableAttribute("fetchedMinute",fetched/60000L);
        if (fetchedLong) doc.setIndexableAttribute("fetchedLong",fetched);


    }

}
