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

/**
 * This interface defines a fetcher that can be pugged into the crawler.
 * Its only funtion is to take a list of pages that need to be fetched, 
 * fetch those pages from the internet, and produce a corresponding list
 * of fetched data.
 * @author Flaptor Development Team
 */
public interface IFetcher {

    /**
     * Takes a FetchList, fetches the pages, and produces a FetchData.
     */
    public FetchData fetch(FetchList list) throws Exception;

}

