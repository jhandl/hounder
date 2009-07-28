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

import com.flaptor.clusterfest.monitoring.MonitorNodeDescriptor;
import com.flaptor.clusterfest.monitoring.NodeChecker;
import com.flaptor.clusterfest.monitoring.NodeState;
import com.flaptor.util.Statistics;

/**
 * Checker for searcher node type
 * 
 * @author Martin Massera
 */
public class SearcherChecker implements NodeChecker{

    public NodeChecker.Result checkNode(MonitorNodeDescriptor node, NodeState state) {
        Statistics statistics = (Statistics)state.getProperties().get("statistics");

        List<String> remarks = new ArrayList<String>();
        Sanity sanity = Sanity.GOOD;
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
