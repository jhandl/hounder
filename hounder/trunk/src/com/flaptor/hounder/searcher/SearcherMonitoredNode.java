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

import java.util.ArrayList;
import java.util.List;

import com.flaptor.clustering.monitoring.nodes.MonitoreableImplementation;
import com.flaptor.util.Statistics;
import com.flaptor.util.ThreadUtil;

/**
 * implementation of MonitoredNode for monitoring a searcher
 * 
 * @author Martin Massera
 */
public class SearcherMonitoredNode extends MonitoreableImplementation {

	CompositeSearcher searcher;
	
	public SearcherMonitoredNode(CompositeSearcher searcher) {
		this.searcher = searcher;
	}

	public void updateProperties() {
		super.updateProperties();
		Statistics stats = Statistics.getStatistics();
		setProperty("maxSimultaneousQueries", String.valueOf(searcher.getTrafficLimitingSearcher().getMaxSimultaneousQueries()));
		setProperty("simultaneousQueries", String.valueOf(searcher.getTrafficLimitingSearcher().getSimultaneousQueries()));
		setProperty("queryStats", stats.getStats("searcherQuery"));
		setProperty("cacheHit", stats.getStats("cacheHit"));
		setProperty("cacheMiss", stats.getStats("cacheMiss"));
		setProperty("suggestQuery", stats.getStats("suggestQuery"));
	}
	
	public static List<String> getThreadNames() {
		List<String> ret = new ArrayList<String>();
		for (Thread t : ThreadUtil.getLiveThreads()) {
			ret.add(t.getName());
		}
		return ret;
	}

	public void updateProperty(String property) {
		//update all for now
		updateProperties();
	}
}
