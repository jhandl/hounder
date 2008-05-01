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
package com.flaptor.hounder.cluster;

import java.util.ArrayList;
import java.util.List;

import com.flaptor.clusterfest.monitoring.MonitorNodeDescriptor;
import com.flaptor.clusterfest.monitoring.NodeChecker;
import com.flaptor.clusterfest.monitoring.NodeState;
import com.flaptor.util.Statistics;
import com.flaptor.util.Statistics.EventStats;

/**
 * Checker for multisearcher node type.
 * Verifies that there where no errors with some of the unisearchers during the
 * last period of the Statistics object.
 * 
 * @author Spike
 */
public class MultiSearcherChecker implements NodeChecker{

    public NodeChecker.Result checkNode(MonitorNodeDescriptor node, NodeState state) {
        List<String> remarks = new ArrayList<String>();
        Sanity sanity = Sanity.GOOD;
        
        Statistics stats = (Statistics) state.getProperties().get("statistics");
        if (null == stats) {
        	remarks.add("No statistics to check.");
        	sanity = Sanity.BAD;
        } else {
        	for (String eventName : stats.getEvents()) {
        		if (eventName.startsWith("averageTimes_")) {
        			EventStats es = stats.getLastPeriodStats(eventName);
        			if (es.numErrors > 0L) {
        				remarks.add("Errors reported for event: " + eventName + '.');
        				sanity = Sanity.BAD;
        			}
        		}
        	}
        }
        
        String exception = (String)state.getProperties().get("searcherException");
        if (exception != null) {
            sanity = Sanity.BAD;
            if (exception.contains("No indexId active")) {
                remarks.add("No index active");
            } else {
                remarks.add("Searcher throwing exception: " + exception);
            }
        }
        return new Result(sanity, remarks);
    }
}
