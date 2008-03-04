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

import com.flaptor.util.Execute;

/**
 * This is a simfetcher that after ending the fetch, waits a bit.
 * Useful to test things that need the fetcher to spend time fetching.
 * 
 * @author Flaptor Development Team
 */
public class SimSlowFetcher extends SimFetcher {

    private long waitTime = 0;

    public SimSlowFetcher (SimWeb web, long waitTime) {
        super(web);
        this.waitTime = waitTime;
    }

    public FetchData fetch (FetchList fetchlist) throws Exception {
        Execute.sleep(waitTime);
        return super.fetch(fetchlist);
    }

}

