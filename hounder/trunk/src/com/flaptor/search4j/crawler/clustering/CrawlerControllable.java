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
package com.flaptor.search4j.crawler.clustering;

/**
 * RPC interface for crawler control clusterfest module
 * @author Flaptor Development Team
 */
public interface CrawlerControllable {
	/**
	 * pinging method for determining if the node is crawler controllable
	 * @return true
	 */
	public boolean ping() throws Exception;
	
	public String getBoostConfig() throws Exception;
}
