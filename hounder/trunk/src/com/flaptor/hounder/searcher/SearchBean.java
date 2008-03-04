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
package com.flaptor.hounder.searcher;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.query.LazyParsedQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.Config;

/**
 * Implements the search functionality.
 * @author Flaptor Development Team
 */
public class SearchBean {

    private final ISearcher searcher;
    private boolean inited = false;

	/**
	 * Constructor.
	 */
	public SearchBean() {
        // TODO: Fix access to config file. (Config.init("xxxx"))
        searcher = new CompositeSearcher();
	}

	public final GroupedSearchResults search(final String queryStr, final int firstResult, final int count,  final AFilter filter, final ASort sort, final AGroup group, final int groupSize)  throws SearcherException{
		return searcher.search(new LazyParsedQuery(queryStr), firstResult, count, group, groupSize, filter, sort);
	}


    public synchronized final void init(String[] classpath) {
        if (!inited) {
            try {
                URL[] urls = new URL[classpath.length];
                for (int i = 0; i < classpath.length; i++) {
                    urls[i] = new URL("file://"+classpath[i]);
                }
                URLClassLoader loader = new URLClassLoader(urls);
                Config config = Config.getConfig("searcher.properties",loader);
                inited = true;
            } catch (MalformedURLException e) {
                System.err.println(e);
            }
        }
    }

}

