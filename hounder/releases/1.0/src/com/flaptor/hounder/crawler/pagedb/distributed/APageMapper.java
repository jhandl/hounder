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
 * A page mapper maps pages to node numbers. 
 * This is the way a Distributed PageDB distributes the pages among its DPageDB instances.
 * @author Flaptor Development Team
 */
public abstract class APageMapper {

    protected int nodeCount; // the number of nodes participating in the Distributed PageDB.

    /**
     * Construct a new page mapper.
     * @param config the configuration file for this mapper.
     * @param nodeCount the number of nodes participating in the Distributed PageDB.
     */
    public APageMapper (Config config, int nodeCount) {
        this.nodeCount = nodeCount;
    }

    /**
     * Given a page, applies some heuristics to map it to one of the nodes.
     * @param page the page that needs to be mapped.
     * @return the number of the node to which this page is mapped.
     */
    public abstract int mapPage (Page page);


    /**
     * Helper method for testing the map function, to be called from the subclass main() method.
     * @param mapperClass the class calling this method.
     * @param args the main() args.
     */
    public static void test (Class<?> mapperClass, String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println ("\nMissing arguments.\n\nUsage:\n  "+mapperClass.getName()+" <nodeCount> <url>\n");
            System.exit(-1);
        }
        int nodes = Integer.parseInt(args[0]);
        Page page = new Page(args[1],0);
        APageMapper mapper = (APageMapper)mapperClass.getConstructor(new Class<?>[]{Config.class, Integer.TYPE}).newInstance(new Object[]{null,nodes});
        System.out.println("  "+args[1]+" -> "+mapper.mapPage(page));
    }
    
}

