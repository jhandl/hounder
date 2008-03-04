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

import com.flaptor.clustering.Node;
import com.flaptor.clustering.NodeUnreachableException;
import com.flaptor.clustering.modules.ModuleNode;

/**
 * Node for crawler control module
 * 
 * @author Martin Massera
 */
public class CrawlerControlNode implements ModuleNode {
	private Node node;
	private CrawlerControllable crawlerControllable;
	
	public CrawlerControlNode(Node node) {
		super();
		this.node = node;
		crawlerControllable = CrawlerControl.getCrawlerControllableProxy(node.getXmlrpcClient());
	}

	public String getBoostConfig() throws NodeUnreachableException {
		try{
			return crawlerControllable.getBoostConfig();
		}
		catch (Exception e) {
			node.setUnreachable(e); //this throws an exception
			return null;
		}
	}
}
