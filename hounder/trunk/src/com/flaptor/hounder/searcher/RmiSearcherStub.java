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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import com.google.common.base.Preconditions;

import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.group.NoGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.query.LazyParsedQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.PortUtil;
import com.flaptor.util.remote.ARmiClientStub;
import com.flaptor.util.remote.ExponentialFallbackPolicy;
import com.flaptor.util.remote.IRetryPolicy;
import com.flaptor.util.remote.RpcException;

/**
 * A client-side IRemoteSearcher that connects with the server
 * via rmi.
 * @author Flaptor Development Team
 */
public class RmiSearcherStub extends ARmiClientStub implements IRemoteSearcher {
    private static Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());
    private IRmiSearcher remoteSearcher = null;

    private final String host;
    private final int port;

    private AtomicInteger requestsInProgress = new AtomicInteger(0);
    private final int maxSimultaneousRequests;
    private final static int DEFAULT_MAX_SIMULTANEOUS_REQUESTS = 30;

    /**
     * Ctor with ExponentialFallbackPolicy
     * @param basePort
     * @param host
     */
    public RmiSearcherStub(final int basePort, final String host) {
    	this(basePort, host, new ExponentialFallbackPolicy());
    }

   	public RmiSearcherStub(final int basePort, final String host, IRetryPolicy policy) {
        this (basePort, host, policy, DEFAULT_MAX_SIMULTANEOUS_REQUESTS);
    }

   	public RmiSearcherStub(final int basePort, final String host, IRetryPolicy policy, int maxSimultaneousRequests) {
        super(PortUtil.getPort(basePort,"searcher.rmi"), host, policy);
        Preconditions.checkArgument(maxSimultaneousRequests > 0, "RmiSearcherStub.init: maxSimultaneousRequests must be > 0");
        Preconditions.checkArgument(basePort > 0, "RmiSearcherStub.init: basePort must be > 0");
        Preconditions.checkNotNull(host, "RmiSearcherStub.init: host cannot be null");
        this.maxSimultaneousRequests = maxSimultaneousRequests;
        this.port = basePort;
        this.host = host;
        logger.info("Creating RmiSearcherStub for Searcher located at " + host + ":" + PortUtil.getPort(basePort,"searcher.rmi"));
    }

    public String getTextualIdentifier() {
        return (host + ':' + port);
    }

    public GroupedSearchResults search(AQuery query, int firstResult, int count,  AGroup groupBy, int groupSize, AFilter filter, ASort sort) throws RpcException {
        int requests = requestsInProgress.incrementAndGet();
        try {
            if ( requests > maxSimultaneousRequests) {
                throw new RpcException("There are too many requests in progress( current: " + requests + ", max: " + maxSimultaneousRequests
                        + "). Server call skipped.");
            }
            try {
                if (super.checkConnection()) {
                    GroupedSearchResults res = remoteSearcher.search(query, firstResult, count, groupBy, groupSize,filter, sort);
                    super.connectionSuccess();
                    return res;
                } else {
                    throw new RpcException("The recconection policy requested not to contact the server. Server call skipped.");
                }
            } catch (RemoteException e) {
                logger.error("search: exception caught.", e);
                super.connectionFailure();
                throw new RpcException(e);
            }
        } finally {
            requestsInProgress.decrementAndGet();
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
