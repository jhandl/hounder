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
package com.flaptor.hounder.searcher;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.group.NoGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.query.LazyParsedQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.PortUtil;
import com.flaptor.util.remote.ARmiClientStub;
import com.flaptor.util.remote.RpcException;
import com.flaptor.util.remote.ExponentialFallbackPolicy;

/**
 * A client-side IRemoteSearcher that connects with the server
 * via rmi.
 * @author Flaptor Development Team
 */
public class RmiSearcherStub extends ARmiClientStub implements IRemoteSearcher {
    private static Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());
    private IRmiSearcher remoteSearcher = null;
    
	/**
	 * @todo the searcher should be able to specify the connection policy.
	 */
    public RmiSearcherStub(final int basePort, final String host) {
        super(PortUtil.getPort(basePort,"searcher.rmi"), host, new ExponentialFallbackPolicy());
        logger.info("Creating RmiSearcherStub for Searcher located at " + host + ":" + PortUtil.getPort(basePort,"searcher.rmi"));
    }
    
	public GroupedSearchResults search(AQuery query, int firstResult, int count,  AGroup groupBy, int groupSize, AFilter filter, ASort sort) throws RpcException {
        try { 
            super.checkConnection();
            GroupedSearchResults res = remoteSearcher.search(query, firstResult, count, groupBy, groupSize,filter, sort);
            super.connectionSuccess();
            return res;
        } catch (RemoteException e) {
            logger.error(e,e);
            super.connectionFailure();
            throw new RpcException(e);
        }
    }


    //@Override
    protected void setRemote(Remote remote) {
        this.remoteSearcher = (IRmiSearcher) remote;
    }


    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            printUsage();
            System.exit(-1);
        }
        RmiSearcherStub searcher = new RmiSearcherStub(Integer.parseInt(args[1]), args[0]);
        AQuery query = new LazyParsedQuery(args[2]);
        System.out.println( searcher.search(query, Integer.parseInt(args[3]), Integer.parseInt(args[4]), new NoGroup(), 1, null, null));
    }

    private static void printUsage() {
        System.out.println("Usage:\n\t RmiSearcherStub host basePort query firstResult numberOfResults");
    }

}
