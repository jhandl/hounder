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
package com.flaptor.hounder.cache;

import java.util.ArrayList;
import java.util.List;

import com.flaptor.clusterfest.monitoring.MonitorNodeDescriptor;
import com.flaptor.clusterfest.monitoring.NodeChecker;
import com.flaptor.clusterfest.monitoring.NodeState;
import com.flaptor.util.Statistics;

/**
 * Checker for cacheServer node type
 * 
 * @author Flaptor Development Team
 */
public class CacheServerChecker implements NodeChecker{

	public NodeChecker.Result checkNode(MonitorNodeDescriptor node, NodeState state) {
        Statistics statistics = (Statistics)state.getProperties().get("statistics");
	    
	    List<String> remarks = new ArrayList<String>();
	    remarks.add("This is a stub checker, please write a sanity checker for this type of node.");
	    return new Result(Sanity.GOOD, remarks);
	}
}
