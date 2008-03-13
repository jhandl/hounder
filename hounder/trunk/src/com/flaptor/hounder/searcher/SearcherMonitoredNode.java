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

import com.flaptor.clusterfest.monitoring.node.MonitoreableImplementation;
import com.flaptor.hounder.searcher.group.NoGroup;
import com.flaptor.hounder.searcher.query.LazyParsedQuery;
import com.flaptor.util.Pair;
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
		TrafficLimitingSearcher tls = searcher.getTrafficLimitingSearcher();
		if (null != tls) {
			setProperty("maxSimultaneousQueries", String.valueOf(tls.getMaxSimultaneousQueries()));
			setProperty("simultaneousQueries", String.valueOf(tls.getSimultaneousQueries()));
		}
		setProperty("queryStats", stats.getStats("searcherQuery"));
		setProperty("cacheHit", stats.getStats("cacheHit"));
		setProperty("cacheMiss", stats.getStats("cacheMiss"));
		setProperty("suggestQuery", stats.getStats("suggestQuery"));
        setProperty("responseTimes", stats.getStats("responseTimes"));
		try {
            searcher.search(new LazyParsedQuery("testing123"), 0, 1, new NoGroup(), 1, null,null);
            setProperty("searcherException", null);
        } catch (SearcherException e) {
            setProperty("searcherException", e.getMessage());
        }
		ArrayList<Pair<String,Float>> averageTimes = new ArrayList<Pair<String,Float>>();
		for (String eventName : stats.getEvents()) {
			if (eventName.startsWith("averageTimes_")) {
				averageTimes.add(new Pair<String,Float>(eventName.substring(13),stats.getLastPeriodStats(eventName).getAvg()));
			}
		}
		if (averageTimes.size() > 0) {
			setProperty("averageTimes",averageTimes);
		}
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
