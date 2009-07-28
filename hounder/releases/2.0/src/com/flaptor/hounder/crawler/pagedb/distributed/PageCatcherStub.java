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

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.logging.Level;
import org.apache.log4j.Logger;

import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.util.PortUtil;
import com.flaptor.util.remote.ARmiClientStub;
import com.flaptor.util.remote.ExponentialFallbackPolicy;
import com.flaptor.util.remote.RpcException;

/**
 * A client-side IRemotePageCatcher that connects with the server
 * via rmi.
 * 
 * @author Flaptor Development Team
 */
public class PageCatcherStub extends ARmiClientStub implements IRemotePageCatcher {
    private static Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());
    private IRemotePageCatcher remotePageCatcher = null;
    private static long CONNECTION_TIMEOUT = 5 * 60 * 1000L; // 5 min.
	/**
	 * Constructor.
	 */
    public PageCatcherStub(final int basePort, final String host) {
        super(PortUtil.getPort(basePort,"pagecatcher.rmi"), host, new ExponentialFallbackPolicy(),false);
        int catcherPort = PortUtil.getPort(basePort,"pagecatcher.rmi");
        logger.info("Creating PageCatcherStub for PageCatcher located at " + host + ":" + catcherPort);
        boolean connected = false;
        long start = System.currentTimeMillis();
        while (!connected) {
            try {
                if (super.checkConnection()) {
                    connected = true;
                    super.connectionSuccess();
                }
            } catch (RemoteException ex) { }
            if (!connected) {
                long now = System.currentTimeMillis();
                if (now-start > CONNECTION_TIMEOUT) {
                    logger.error("Failed to connect to remote PageCatcher at " + host + ":" + catcherPort);
                    break;
                }
            }
            try { Thread.sleep(1000); } catch (InterruptedException ex) { }
        }
    }
    
  
    /**
     * Adds a page to a remote pagedb catcher.
     * @param page the page to add.
     * @throws com.flaptor.util.remote.RpcException
     */
    public void addPage(Page page) throws RpcException {
        try {
            if (super.checkConnection()) {
                remotePageCatcher.addPage(page);
                super.connectionSuccess();
            } else {
                throw new RpcException("AddPage failed trying to connect to remote PageCatcher");
            }
        } catch (RemoteException e) {
            logger.error(e,e);
            super.connectionFailure();
            throw new RpcException(e);
        } catch (RpcException e) {
            logger.error(e,e);
            super.connectionFailure();
            throw e;
        }
    }

    //@Override
    protected void setRemote(Remote remote) {
        this.remotePageCatcher = (IRemotePageCatcher) remote;
    }

}
