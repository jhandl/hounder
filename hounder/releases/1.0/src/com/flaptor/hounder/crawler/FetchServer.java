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
package com.flaptor.hounder.crawler;

import org.apache.log4j.Logger;

import com.flaptor.util.Config;
import com.flaptor.util.Execute;


/**
 * This class implements a fetch server.
 * @todo synchronize the threads
 * @todo add comments
 * @author Flaptor Development Team
 */
public class FetchServer {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private Thread serverThread;
    private IFetcher fetcher;
    private Crawler crawler;


    /** 
     * Class initializer. The fetchserver works with a provided fetcher.
     * @param fetcher the provided fetcher, null if it should be read from the config.
     * @param crawler the crawler that will provide fetchlists and consume fetchdata.
     */
    public FetchServer (IFetcher fetcher, Crawler crawler) throws Exception {
        if (null == fetcher) {
            Config config = Config.getConfig("crawler.properties");
            String className = config.getString("fetcher.plugin");
            fetcher = (IFetcher)Class.forName(className).getConstructor(new Class[]{}).newInstance(new Object[]{});
        }
        this.fetcher = fetcher;
        this.crawler = crawler;
        serverThread = new ServerThread();
        serverThread.setDaemon(true);
    }


    // This thread handles the fetcher.
    private class ServerThread extends Thread {
        public void run () {
            FetchList fetchlist = null;
            FetchData fetchdata = null;
            boolean continueAfterException;
            do {
                continueAfterException= false;
                try {
                    fetchlist = crawler.getFetchlist();
                    fetchdata = null;
                    if (null != fetchlist) {
                        fetchdata = fetcher.fetch(fetchlist);
                    } else {
                        logger.debug("Got null fetchlist");
                    }
                    crawler.takeFetchdata(fetchdata);
                } catch (Exception e) {
                    logger.error(e,e);
                    continueAfterException= true;
                }
            } while ((continueAfterException || (null != fetchdata)) && Crawler.running());
            logger.info("Local fetcher is done. ("+((null==fetchdata)?"fetchdata==null":"crawler.running()==false")+")");
        }
    }

    /**
     * Start the fetch server.
     */
    public void start () {
        serverThread.start();
    }

}

