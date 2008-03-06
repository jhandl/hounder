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

import org.apache.log4j.Logger;

import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.util.PortUtil;
import com.flaptor.util.remote.ARmiClientStub;
import com.flaptor.util.remote.RpcException;
import com.flaptor.util.remote.ExponentialFallbackPolicy;

/**
 * A client-side IRemotePageCatcher that connects with the server
 * via rmi.
 * 
 * @author Flaptor Development Team
 */
public class PageCatcherStub extends ARmiClientStub implements IRemotePageCatcher {
    private static Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());
    private IRemotePageCatcher remotePageCatcher = null;
    
	/**
	 * @todo the searcher should be able to specify the connection policy.
	 */
    public PageCatcherStub(final int basePort, final String host) {
        super(PortUtil.getPort(basePort,"pagecatcher.rmi"), host, new ExponentialFallbackPolicy(),true);
        logger.info("Creating PageCatcherStub for PageCatcher located at " + host + ":" + PortUtil.getPort(basePort,"pagecatcher.rmi"));
    }
    
    public void addPage(Page page) throws  RpcException{
        try {
            super.checkConnection();
            remotePageCatcher.addPage(page);
            super.connectionSuccess();
        } catch (RemoteException e) {
            logger.error(e,e);
            super.connectionFailure();
            throw new RpcException(e);
        }
    }

    //@Override
    protected void setRemote(Remote remote) {
        this.remotePageCatcher = (IRemotePageCatcher) remote;
    }

}
