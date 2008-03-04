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
public class TestMapper extends APageMapper {

    public TestMapper (Config config, int nodeCount) {
        super(config, nodeCount);
    }

    /**
     * Receives a URL with the form xxxxxxxN=M
     * If N==0, returns M
     * If N==1, returns 1-M
     * This is necessary for testing with two DPageDBs in the same JVM,
     * because the list of nodes has to be inverted for one of them.
     */
    public int mapPage (Page page) {
        String[] parts = page.getUrl().split("=");
        if (parts[0].endsWith("0")) {
            return Integer.parseInt(parts[1]);
        } else {
            return 1 - Integer.parseInt(parts[1]);
        }
    }

}

