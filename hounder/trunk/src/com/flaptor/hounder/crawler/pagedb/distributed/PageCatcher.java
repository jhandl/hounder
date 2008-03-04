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
package com.flaptor.hounder.crawler.pagedb.distributed;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.hounder.crawler.pagedb.PageDB;
import com.flaptor.util.Execute;
import com.flaptor.util.remote.ConnectionException;
import com.flaptor.util.remote.RmiServer;



/** 
 * The PageCatcher implements an RMI service that acts as a remote PageDB,
 * for other crawlers to send pages to. These pages will be catched into
 * a local pagedb and served to the local DPageDB upon request for merging.
 * @author Flaptor Development Team
 */
public class PageCatcher implements IRemotePageCatcher {

    static Logger logger = Logger.getLogger(Execute.whoAmI());
    private String catchDir = null;
    private PageDB catchdb = null;
    private int port = 0;
    private boolean running = false;
    private RmiServer server = null;

    /**
     * Create a PageCatcher.
     * @param catchDirBase path to the directory of the pagedg that will hold the catched pages.
     */
    public PageCatcher (String catchDir) {
        this.catchDir = catchDir + ".catch";
    }

    /**
     * Start de page catcher by adding it to the rmi registry
     * @param node the local node.
     */
    public void start (NodeAddress node) {
        if (!running) {
            newCatcher();
            exportCatcher(node.getPort());
            running = true;
        }
    }

    // Publish this catcher on the RMI registry.
    private void exportCatcher(int basePort) {
        try {
            this.port = com.flaptor.util.PortUtil.getPort(basePort,"pagecatcher.rmi");
            this.server = new RmiServer(this.port);
            this.server.addHandler(RmiServer.DEFAULT_SERVICE_NAME,this);
            this.server.start();
            //RmiUtil.registerLocalService(port,"PageCatcher",this);
        } catch (Exception e) {
            logger.error(e,e);
            throw new RuntimeException("Registering the PageCatcher", e);
        }
    }

    /**
     * Stop the catcher by unregistering it.
     */
    public void stop () {
        if (running) {
            this.server.requestStop();
            while (!this.server.isStopped()) {Execute.sleep(20);}
        } else {
            throw new IllegalStateException("Tried to stop a catcher that has not been started");
        }
    }

    /**
     * Add pages sent by remote dpagedb via RMI.
     * @param page the page sent by a remote dpagedb.
     */
    public void addPage (Page page) throws ConnectionException {
        if (running) {
            logger.debug("Receieved remote page "+page.getUrl());
            addCatch(page);
        } else {
            throw new IllegalStateException("Tried to add a page to a catcher that has not been started");
        }
    }

    /**
     * Return the number of pages catched.
     * @return the number of pages catched.
     */
    public long catches () {
        long catchSize = 0;
        if (running) {
            catchSize = catchdb.getSize();
        } else {
            throw new IllegalStateException("Tried to get the catch from a catcher that has not been started");
        }
        return catchSize;
    }

    // start a new pagedb for storing catched pages.
    private void newCatcher () {
        // TODO: using WRITE without APPEND means that if the crawler is restarted, any catched pages will be lost;
        //       but append doesn't work with adding the timestamp to the name. Yet if the name remains the same for 
        //       append to work, we can't return the old catchdb and immediately open the new one...
        try {
            String dirname = catchDir + ".tmp";
            catchdb = new PageDB(dirname);
            catchdb.open(PageDB.WRITE + PageDB.APPEND + PageDB.UNSORTED);
        } catch (IOException e) {
            logger.error(e,e);
            throw new RuntimeException("Creating a new PageCatcher pagedb", e);
        }
    }

    // add a catched page to the pagedb.
    private synchronized void addCatch (Page page) {
        try {
            catchdb.addPage(page);
        } catch (IOException e) {
            logger.error(e,e);
            throw new RuntimeException("Adding page to PageCatcher", e);
        }
    }


    /**
     * Return the pagedb with the catched pages and switch to a new one.
     * @return the pagedb with the catched pages.
     */
    public synchronized PageDB getCatch () {
        Execute.close(catchdb);
        catchdb.rename(catchDir);
        PageDB tmp = catchdb;
        newCatcher();
        return tmp;
    }

}


