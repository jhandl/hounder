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
package com.flaptor.hounder.crawler.pagedb.distributed;

import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.util.Config;

/**
 * @author Flaptor Development Team
 */
public class UrlHashMapper extends APageMapper {

    public UrlHashMapper (Config config, int nodeCount) {
        super(config, nodeCount);
    }

    public int mapPage (Page page) {
        int hash = Math.abs(page.getUrlHash().hashCode());
        return hash % nodeCount;
    }

    public static void main (String[] args) throws Exception {
        APageMapper.test(UrlHashMapper.class, args);
    }
    
}

