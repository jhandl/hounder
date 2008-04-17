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

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.antlr.stringtemplate.StringTemplate;
import org.apache.log4j.Logger;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

import com.flaptor.clusterfest.NodeListener;
import com.flaptor.clusterfest.controlling.ControllerModule;
import com.flaptor.clusterfest.controlling.node.ControllableImplementation;
import com.flaptor.clusterfest.monitoring.MonitorModule;
import com.flaptor.hounder.crawler.modules.DocumentCacheItem;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.Pair;
import com.flaptor.util.PortUtil;
import com.flaptor.util.Statistics;
import com.flaptor.util.cache.MultiCache;

/**
 * HTTP server for the content system, that uses a multiCache 
 * @author Flaptor Development Team
 */
class HttpCacheServer {
    
    private static Logger logger = Logger.getLogger(Execute.whoAmI());
	
	public HttpCacheServer() {

        Server server = new Server(PortUtil.getPort("cacheServer.http"));        
        server.setHandler(new CacheHandler());
        try {
        	server.start();
        } catch (Throwable t) {
        	throw new RuntimeException(t);
        }
	}
	
    /**
     * Handles the requests checking that there is a parameter URL
     * and then seeks that URL in the multiCache
     */
    public static class CacheHandler extends AbstractHandler {
        
        private MultiCache<DocumentCacheItem> multiCache;
        private NodeListener nodeListener;

        StringTemplate frameTemplate;
        StringTemplate notFoundTemplate;
        
        public CacheHandler() {
            Config config = Config.getConfig("multiCache.properties");
            frameTemplate = new StringTemplate(config.getString("HTTPCacheServer.frameTemplate"));
            notFoundTemplate = new StringTemplate(config.getString("HTTPCacheServer.notFound"));

        	if (config.getBoolean("clustering.enable")) {
            	int port = PortUtil.getPort("clustering.rpc.cacheServer");
        		nodeListener = new NodeListener(port, config);
        		MonitorModule.addMonitorListener(nodeListener, new CacheServerMonitoredNode(this));
        		ControllerModule.addControllerListener(nodeListener, new ControllableImplementation());
        		nodeListener.start();
            }

        	List<Pair<String, Integer>> caches = new ArrayList<Pair<String, Integer>>();
        	for (String host : config.getStringArray("multiCache.hosts")) {
        	    caches.add(PortUtil.parseHost(host, "crawler.pageCache.rmi"));
        	}
        	multiCache = new MultiCache<DocumentCacheItem>(caches, config.getLong("multiCache.timeout"), config.getInt("multiCache.workerThreads"));
        }

        
        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
        	logger.info("request " + request.getRequestURL());
            String URL = request.getParameter("URL");
            if (URL == null ) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                ((Request)request).setHandled(true);
                return;
            }
            URL = URLDecoder.decode(URL, "UTF-8");
            
            DocumentCacheItem doc = multiCache.getItem(URL);
            if (doc != null) {
                response.setContentType(doc.getMimeType());
                
                if (doc.getMimeType().equals("text/html")) {
                    StringTemplate template = frameTemplate.getInstanceOf();
                    template.setAttribute("url", URL);
                    response.getOutputStream().write(template.toString().getBytes());
                }
                response.getOutputStream().write(doc.getContent());
            } else {
            	StringTemplate template = notFoundTemplate.getInstanceOf();
                template.setAttribute("url", URL);
                response.getOutputStream().write(template.toString().getBytes());
//                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            
            ((Request)request).setHandled(true);
        }        
    }    

    public static void main(String[] args) {
    	new HttpCacheServer();
    }
}
 
