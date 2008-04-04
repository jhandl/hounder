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
package com.flaptor.hounder.indexer;

import java.util.ArrayList;
import java.util.List;

import com.flaptor.clusterfest.monitoring.MonitorNodeDescriptor;
import com.flaptor.clusterfest.monitoring.NodeChecker;
import com.flaptor.clusterfest.monitoring.NodeState;

/**
 * Checker for indexer node type
 * @author Flaptor Development Team
 */
public class IndexerChecker implements NodeChecker{

    @SuppressWarnings("unchecked")
    public NodeChecker.Result checkNode(MonitorNodeDescriptor node, NodeState state) {
        Sanity sanity = Sanity.GOOD;
        List<String> remarks = new ArrayList<String>();
        
        List<String> indexUpdateProblems = (List<String>)state.getProperties().get("indexUpdateProblems");
        if (indexUpdateProblems != null && indexUpdateProblems.size() > 0) {
            sanity = Sanity.BAD;
            for (String problem: indexUpdateProblems) {
                remarks.add(problem);
            }
        }
        return new Result(sanity, remarks);
    }
}
