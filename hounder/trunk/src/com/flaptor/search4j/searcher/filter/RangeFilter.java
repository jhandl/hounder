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
import java.util.ArrayList;
import java.util.List;

/**
 * A filter that let pass document within a range of values.
 * The RangeFilter is a Filter constructed with a field name and 2 values. Only the document containing the specified field and whose values
 * are within the 2 provided values pass. The range is inclusive.\
 * @author Flaptor Development Team
 */
@SuppressWarnings("serial")
public class RangeFilter extends AFilter implements Serializable {
	private final String field;
	private final String from;
	private final String to;

	/**
	  Constructor.
	  @param field the name of the field.
	  @param from the minimum value that will be allowed (inclusive)
	  @param to the maximum value that will be allowed (inclusive)
	 */
	public RangeFilter(final String field, final String from, final String to) {
		this.field = field;
		this.from = from;
		this.to = to;
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
	  Generates a new lucene filter.
	  @return a new lucene filter matching this search4j filter.
	 */
	private org.apache.lucene.search.Filter generateNewLuceneFilter() {
		return new org.apache.lucene.search.CachingWrapperFilter(new org.apache.lucene.search.RangeFilter(field, from, to, true, true));
	}

	/**
	  @inheritDoc
	  Two RangeFilters are equal if they have the same field, the same start value (from) and the same end value (to).
	 */
	public boolean equals(final Object obj) {
		if (null == obj) {
			return false;
		}else if (!(obj.getClass().equals(RangeFilter.class))) {
			return false;
		} else {
			RangeFilter o = (RangeFilter)obj;
			return (field.equals(o.field) && from.equals(o.from) && to.equals(o.to));
		}
	}

	/**
	  @inheritDoc
	 */
	public int hashCode() {
		List<String> list = new ArrayList<String>(3);
		list.add(field);
		list.add(from);
		list.add(to);
		return list.hashCode();
	}

}
