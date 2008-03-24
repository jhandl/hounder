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
package com.flaptor.hounder.searcher.filter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;


/**
 * Implement a Filter that is the rusult of ANDing a series of filters.
 * This class is "immutable", but only  after the first use of getLuceneFilter.
 * This class is <b>not</b> thread safe.
 * This filter is not cached on it's own, but the filters passed to it may be cached.
 * There would be little gain in caching this class, as it does't generate a lucene filter
 * with it's associated lengthy bitArray.
 */
@SuppressWarnings("serial")
public final class BooleanFilter extends AFilter implements Serializable {
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());
	private Set<AFilter> filters;
	private boolean used;
	private final Type type;

	/**
	 * Constructs an empty Anded Filter.
	 * The BooleanFilter constructed is empty and has to be filled up with calling to addFilter
	 * before using it.
	 */
	public BooleanFilter(final Type type) {
		if (null == type) {
			String msg = "type cannot be null";
			logger.error(msg);
			throw new IllegalArgumentException(msg);
		}
		this.type = type;
		filters = new HashSet<AFilter>();
		used = false;
	}

	/**
	 * Adds a filter to be anded.
	 * You can only add filters between construction and the first call to getLuceneFilter.
	 * @throws IllegalStateException if getLuceneFilter has been called for this anded filter.
	 */
	public BooleanFilter addFilter(final AFilter filter) {
		if (used) {
			String s = "addFilter: trying to add a filter after the initial use of the class.";
			logger.error(s);
			throw new IllegalStateException(s);
		}
		filters.add(filter);
		return this;
	}

	/**
	 * Implementation of the Filter interface.
	 * Calling this method will change the internal state of the class, preventing any further Filter
	 * addition.
	 */
	public org.apache.lucene.search.Filter getLuceneFilter() {
		used = true;
		List<org.apache.lucene.search.Filter> array = new ArrayList<org.apache.lucene.search.Filter>(filters.size());
		for (AFilter f : filters) {
			array.add(f.getLuceneFilter());
		}
		if (type == Type.AND) {
			return new org.apache.lucene.misc.ChainedFilter(array.toArray(new org.apache.lucene.search.Filter[filters.size()]), org.apache.lucene.misc.ChainedFilter.AND);
		} else {
			return new org.apache.lucene.misc.ChainedFilter(array.toArray(new org.apache.lucene.search.Filter[filters.size()]), org.apache.lucene.misc.ChainedFilter.OR);
		}
			
	}

	/**
	 * Semantic equality.
	 * Note that equals does not consider the state of the object, so 2 objects that are equal now may
	 * not be equal later.
	 */
	public boolean equals(final Object obj) {
		if (null == obj) {
			return false;
		}else if (! (obj.getClass().equals(BooleanFilter.class))) {
			return false;
		} else {
			return (type == ((BooleanFilter)obj).type) && filters.equals(((BooleanFilter)obj).filters);
		}
	}

	public int hashCode() {
		return filters.hashCode() ^ type.hashCode();
	}

	//--------------------------------------------------------------------------------
	//Internal classes
	public static enum Type {AND, OR};
} 
