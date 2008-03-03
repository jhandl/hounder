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
package com.flaptor.search4j.crawler;

import java.util.Iterator;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import com.flaptor.search4j.crawler.modules.FetchDocument;
import com.flaptor.util.Execute;


/**
 * This class implements a list of fetch documents.
 * This implementation stores the documents in RAM. If that proves problematic,
 * it can be changed to store them in files.
 * @author Flaptor Development Team
 */
public class FetchData implements Iterable<FetchDocument> {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private ArrayList<FetchDocument> list;
    

    /** 
     * Initialize the class.
     */
    public FetchData () {
        list = new ArrayList<FetchDocument>();
    }


    /**
     * Add a fetch document to the fetchdata.
     * @param doc the fetch document to add.
     */
    public void addDoc(FetchDocument doc) {
        list.add(doc);
    }


    /**
     * Close the fetchdata.
     */
    public void close() {
        // there is no close in this implementation.
    }


    /**
     * Get the fetchdata size.
     */
    public int getSize() {
        return list.size();
    }


    /**
     * Iterates the fetchdata.
     */
    public Iterator<FetchDocument> iterator () {
        return list.iterator();
    }

    public void remove() {
        // free resources
        list.clear();
        list = null;
    }

}

