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
package com.flaptor.hounder.crawler.clustering;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.flaptor.clusterfest.AbstractModule;
import com.flaptor.clusterfest.NodeDescriptor;
import com.flaptor.clusterfest.NodeUnreachableException;
import com.flaptor.clusterfest.WebModule;
import com.flaptor.util.Pair;
import com.flaptor.util.remote.NoSuchRpcMethodException;
import com.flaptor.util.remote.WebServer;
import com.flaptor.util.remote.XmlrpcClient;

/**
 * Clusterfest Module for controlling crawlers
 * 
 * @author Martin Massera
 */
public class CrawlerControlModule extends AbstractModule<CrawlerControlNode> implements WebModule {

	@Override
	protected CrawlerControlNode createModuleNode(NodeDescriptor node) {
		return new CrawlerControlNode(node);
	}

	@Override
	public boolean shouldRegister(NodeDescriptor node) throws NodeUnreachableException {
		try {
			boolean ret = getCrawlerControllableProxy(node.getXmlrpcClient()).ping();
			return ret;
		} catch (NoSuchRpcMethodException e) {
			return false;
		} catch (Exception e) {
			throw new NodeUnreachableException(e, node);
		}
	}
    @Override
    protected void notifyModuleNode(CrawlerControlNode node) {
        //do nothing
    }
	
	public String getBoostConfig(CrawlerControlNode node) {
		try {
			return node.getBoostConfig();
		} catch (NodeUnreachableException e) {
			return null;
		}
	}
	
	/**
	 * @param xmlrpcClient
	 * @return a proxy of crawlerControllable that uses that xmlrpcClient
	 */
	public static CrawlerControllable getCrawlerControllableProxy(XmlrpcClient xmlrpcClient) {
		return (CrawlerControllable)XmlrpcClient.proxy("crawlerControl", CrawlerControllable.class, xmlrpcClient);	
	}
	public String action(String action, HttpServletRequest request) {
		return null;
	}
	public List<String> getActions() {
		return new ArrayList<String>();
	}
	public String getModuleHTML() {
		return null;
	}
	public String getNodeHTML(NodeDescriptor node, int nodeNum) {
		if (isRegistered(node)) return "<a href=\"crawlerControl.jsp?node="+nodeNum+"\">crawler</a>";
		else return null;
	}
	public void setup(WebServer server) {
	}

    public List<Pair<String, String>> getSelectedNodesActions() {
        return new ArrayList<Pair<String,String>>();
    }

    public String selectedNodesAction(String action, List<NodeDescriptor> nodes, HttpServletRequest request) {
        return null;
    }

    public String doPage(String page, HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    public List<String> getPages() {
        return new ArrayList<String>();
    }
}
