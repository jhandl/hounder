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
package com.flaptor.hounder.crawler;

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

import com.flaptor.hounder.crawler.pagedb.IPageStore;
import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.util.Execute;

/**
 * This class stores a list of pages.
 * This implementation stores the pages in RAM. If that proves problematic, 
 * it can be changed to store them in files, for example using a PageDB.
 * @author Flaptor Development Team
 */
public class FetchList implements IPageStore {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private LinkedHashMap<String,Page> list;

    /** 
     * Initialize the class.
     * @todo provide the expected size so the arraylist can be reserved.
     */
    public FetchList () {
        list = new LinkedHashMap<String,Page>();
    }

    /**
     * Add a page to the fetchlist.
     * @param page the page to add.
     */
    public void addPage(Page page) {
        list.put(page.getUrl(), page);
    }

    /**
     * Get a Page from the store.
     * @param url the url of the stored page.
     * @return the stored page that matches the given url, or null if there is no page stored with that url.
     */
    public Page getPage(String url) {
        return list.get(url);
    }

    /**
     * Close the fetchlist.
     */
    public void close() {
        // there is no close in this implementation.
    }

    /**
     * Get the fetchlist size.
     */
    public int getSize() {
        return list.size();
    }

    /**
     * Iterates the fetchdata.
     */
    public Iterator<Page> iterator () {
        return list.values().iterator();
    }

    /**
     * Free resources
     */
    public void remove() {
        list.clear();
        list = null;
    }

}

