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
package com.flaptor.hounder.loganalysis;

import com.flaptor.util.EmbeddedWebAppHTTPServer;
import com.flaptor.util.PortUtil;

/**
 * HTTP server for loganalysis 
 * 
 * @author Martin Massera
 */
public class HTTPLogAnalysisServer {

	public HTTPLogAnalysisServer(){
        String webappPath = this.getClass().getClassLoader().getResource("web-loganalysis").getPath();
		new EmbeddedWebAppHTTPServer(PortUtil.getPort("loganalysis.web"), webappPath, "/loganalysis"); 
	}
	
	public static void main(String[] args) {
		new HTTPLogAnalysisServer();
	}
}
