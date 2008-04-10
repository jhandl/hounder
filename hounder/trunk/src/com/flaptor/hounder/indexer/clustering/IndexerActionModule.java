package com.flaptor.hounder.indexer.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.flaptor.clusterfest.ClusterManager;
import com.flaptor.clusterfest.ModuleUtil;
import com.flaptor.clusterfest.NodeDescriptor;
import com.flaptor.clusterfest.WebModule;
import com.flaptor.clusterfest.action.ActionModule;
import com.flaptor.clusterfest.action.ActionNodeDescriptor;
import com.flaptor.util.Pair;
import com.flaptor.util.remote.WebServer;

public class IndexerActionModule extends ActionModule implements WebModule {

    public List<Pair<String, String>> getSelectedNodesActions() {
        List<Pair<String, String>> actions = new ArrayList<Pair<String,String>>();
        if (hasReachableNodes()) {
            actions.add(new Pair<String, String>("indexer.checkpoint","checkpoint"));
            actions.add(new Pair<String, String>("indexer.optimize", "optimize"));
            actions.add(new Pair<String, String>("indexer.close", "close"));
        }
        return actions;
    }
    public String selectedNodesAction(String action, List<NodeDescriptor> nodes, HttpServletRequest request) {
        action = action.replace("indexer.", "");
        List<Pair<NodeDescriptor,Throwable>> errors = sendAction(nodes, action, null);
        String ret;
        if (errors.size() == 0) {
            ret = action + " sent correctly to selected indexers";
        } else {
            ret = action + " generated the following errors:<br/><ul>";
            for (Pair<NodeDescriptor,Throwable> error : errors) {
                ret += "<li><strong>" + error.first().getHost() + ":" + error.first().getPort() + "</strong>: " + error.last().getMessage() + "</li>";
            }
            ret += "</ul>";
        }
        for (NodeDescriptor node : nodes){
            ClusterManager.getInstance().updateAllInfo(node);
        }
        return ret;
    } 
    public String getNodeHTML(NodeDescriptor node, int nodeNum) {
        if (isRegistered(node) && node.isReachable()) {
            return 
            "<a href=\"?action=indexer.checkpoint&idx="+nodeNum + "\">checkpoint</a> " +     
            "<a href=\"?action=indexer.optimize&idx="+nodeNum + "\">optimize</a> " +     
            "<a href=\"?action=indexer.close&idx="+nodeNum + "\">close</a>";     
        }else return null;
    }
    public List<String> getActions() {
        return Arrays.asList(new String[]{"indexer.checkpoint","indexer.optimize", "indexer.close"});
    }
    public String action(String action, HttpServletRequest request) {
        NodeDescriptor node = ModuleUtil.getNodeFromRequest(request);
        return selectedNodesAction(action, Arrays.asList(new NodeDescriptor[]{node}), request);
    }
    public String doPage(String page, HttpServletRequest request, HttpServletResponse response) {return null;}
    public String getModuleHTML() {return null;}
    public List<String> getPages() {return null;}
    public void setup(WebServer server) {}
}
