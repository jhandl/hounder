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
package com.flaptor.search4j.searcher.filter;

import java.io.Serializable;

import com.flaptor.util.Config;
import com.flaptor.util.LRUCache;

/**
 * Represents an abstraction of a resultset Filter.
 * Configuration strings:
 * 		Filter.cacheSize the number of SearchResults to cache.
 * @author Flaptor Development Team
 */
public abstract class AFilter implements Serializable {
	/**
	 * Cache for LUCENE filters.
	 * The s4j filters that return a auto-cached, expensive to calculate, lucene filter,
	 * should search the cache for an existing equivalent filter before generating a new one.
	 * LRUCache is thread-safe (@see com.flaptor.search4j.utils.LRUCache)
	 * IMPORTANT: note that the static field in java are not serialized, so in an rmi transfer
	 * the cache won't travel with the object.
	 */
	private volatile static LRUCache<AFilter, org.apache.lucene.search.Filter> filterCache = null; 

	/**
	 * Returns the cache to cache lucene filters.
	 * The reason filterCache is not initialized simply in a static way is to delay as much as possible
	 * the use of the configuration file. While using RMI, on the client side it makes no sense to use
	 * the cache and using it forces to have the configuration file where it's size is stored.
	 */
	protected static final LRUCache<AFilter, org.apache.lucene.search.Filter> getFilterCache() {
		if (null == filterCache) {
			synchronized (AFilter.class) {
				//While waiting for synchronization, another thread may have set this variable.
				if (null == filterCache) {
					filterCache = new LRUCache<AFilter, org.apache.lucene.search.Filter>(Config.getConfig("searcher.properties").getInt("Filter.cacheSize"));
				}
			}
		}
		//Here I know it the filter cache is not null.
		return filterCache;
	}	

	public abstract org.apache.lucene.search.Filter getLuceneFilter();
}

