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
import java.util.Vector;

/**
 * A filter that filters document not matching a specific value for a specific field.
 * The ValueFilter is a filter created with a value and a field. Only the documents that have that field, and that the value
 * matches the provided one will pass.
 * @author Flaptor Development Team
 */
@SuppressWarnings("serial")
public class ValueFilter extends AFilter implements Serializable {
	private final String value;
	private final String field; 

	public ValueFilter(final String field, final String value) {
		this.value = value;
		this.field = field;
	}

	/**
	 * Returns a lucene filter that implements this filter.
	 * Tries to get it from the filterCache.
	 */
	public org.apache.lucene.search.Filter getLuceneFilter() {
		org.apache.lucene.search.Filter filter = AFilter.getFilterCache().get(this);
		if (null != filter) {
			return filter;
		} else {
			filter = generateNewLuceneFilter();
			AFilter.getFilterCache().put(this, filter);
			return filter;
		}

	}

	/**
	 Generates the underlying lucene filter for this Hounder filter.
	*/
	private org.apache.lucene.search.Filter generateNewLuceneFilter() {
		return new org.apache.lucene.search.CachingWrapperFilter(new org.apache.lucene.search.QueryWrapperFilter(new org.apache.lucene.search.TermQuery(new org.apache.lucene.index.Term(field, value))));
	}

	/**
	 @inheritDoc
	 Two Value filters are equal if both the value and the field match.
	*/
	public boolean equals(final Object obj) {
		if (null == obj) {
			return false;
		} else if (!(obj.getClass().equals(ValueFilter.class))) {
			return false;
		} else {
			ValueFilter o = (ValueFilter)obj;
			return (field.equals(o.field) && value.equals(o.value));
		}
	}

	/**
	 @inheritDoc
	*/
	public int hashCode() {
		Vector<Object> vec = new Vector<Object>(2);
		vec.add(field);
		vec.add(value);
		return vec.hashCode();
	}

}

