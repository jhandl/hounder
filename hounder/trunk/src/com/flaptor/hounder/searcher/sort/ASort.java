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
package com.flaptor.search4j.searcher.sort;

import java.util.List;
import java.util.Comparator;

import org.apache.lucene.search.SortField;
import org.apache.lucene.document.Document;

import com.flaptor.util.Cache;
import com.flaptor.util.Config;
import com.flaptor.util.LRUCache;

/**
 * ResultSet sort criteria.
 * @author Flaptor Development Team
 */
public abstract class ASort implements java.io.Serializable {
    private static Cache<ASort, org.apache.lucene.search.Sort> cache = new LRUCache<ASort, org.apache.lucene.search.Sort>(Config.getConfig("searcher.properties").getInt("Sort.cacheSize"));
	
    protected final Boolean reversed;
	
    /**The base sort is a sort used to decide between documents when this sort can't
     * separate them. May be null.
    */
    protected final ASort baseSort;

	/**
	 * Constructor.
     * Creates an ASort with no base sort.
	 * @param reversed if true, the sort order is reversed.
	 */
	protected ASort(final boolean reversed) {
		this(reversed, null);
	}

	/**
	 * Constructor.
	 * As this class and all derived from it are supposed to be immutable, there's no chance to generate a sort
	 * which contains itself in the base sort, a situation that would have lead to infinite loop when trying to
	 * generate the lucene sort.
	 * @param reversed if true, the sort order is reversed.
	 * @param baseSort a sort to use in case of two documents that are equally ranqued according
	 * 		to this Sort.
	 */
	protected ASort(final boolean reversed, final ASort baseSort) {
		this.reversed = Boolean.valueOf(reversed);
		this.baseSort = baseSort;
	}


	/**
	 * Returns a lucene's sort representing this s4j filter.
	 * It will first try to find an equivalent lucene sort already cached in the SortCache. If
	 * none is found, a new one is created and cached.
	 * @return a lucene sort for this s4j sort.
	 */
	final public org.apache.lucene.search.Sort getLuceneSort() {
		org.apache.lucene.search.Sort sort = cache.get(this);
		if (null != sort) {
			return sort;
		} else {
			sort = generateNewLuceneSort();
			cache.put(this, sort);
			return sort;
		}
	}

	/**
	 * Returns a newly generated lucene sort that represents this s4j sort.
	 * @return a new lucene sort representing a this s4j sort.
	 */
	final org.apache.lucene.search.Sort generateNewLuceneSort() {
        return new org.apache.lucene.search.Sort(getSortFields().toArray(new SortField[0]));
    }
    
	@Override
	public boolean equals(final Object obj)
	{
	    if(this == obj)
	        return true;
	    if((obj == null) || (obj.getClass() != this.getClass()))
	        return false;
        ASort s = (ASort) obj;
        if (s.reversed != reversed) {
            return false;
        }
        if ((null == baseSort) && (null == s.baseSort)) {
            return true;
        } else if ((null != baseSort) && (null != s.baseSort)) {
            return baseSort.equals(obj);
        } else {
            return false;
        }
	}
    
	@Override
	public int hashCode()
	{
	    int hash = 7;
	    hash = 31 * hash + reversed.hashCode();
        if (null != baseSort) {
            hash = 31 * hash + baseSort.hashCode();
        }
	    return hash;
	}
	
    protected abstract List<SortField> getSortFields();

    public abstract Comparator<Document> getComparator();
}

