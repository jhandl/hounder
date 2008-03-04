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

import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.Statistics;

/**
 * A searcher for taking query times
 * 
 * @author Martin Massera
 */
public class StatisticSearcher implements ISearcher {

	private ISearcher searcher;
	private String eventName; 
	
	
	public StatisticSearcher(ISearcher searcher, String eventPrefix) {
		this.searcher = searcher;
		this.eventName = eventPrefix + "Query";
	}

	public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup group, int groupSize, AFilter filter, ASort sort)  throws SearcherException{
		long start = System.currentTimeMillis(); 
		boolean success = false;
		GroupedSearchResults results = null;
		try {
			results = searcher.search(query, firstResult, count, group, groupSize, filter, sort);
			success = true;
		} finally {
			if (success) { 
				long end = System.currentTimeMillis();
				Statistics.getStatistics().notifyEventValue(eventName, (end - start)/1000.0f);
			} else {
				Statistics.getStatistics().notifyEventError(eventName);
			}
		}
		if (null == results) throw new SearcherException("GroupedSearchResults is NULL");
		return results;		
	}

}
