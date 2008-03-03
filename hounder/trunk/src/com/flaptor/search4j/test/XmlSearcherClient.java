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
package com.flaptor.search4j.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;

import com.flaptor.search4j.searcher.ISearcher;
import com.flaptor.util.remote.XmlrpcClient;

/**
 * A client to the RpcSearcher. Useful for testing, and as a starting point for
 * custom Xmlrpc searcher clients. It "main" method allows to issue queries from
 * the console.
 * 
 * @see com.flaptor.search4j.searcher.XmlSearcher
 * @author Flaptor Development Team
 */
public final class XmlSearcherClient {
    private final XmlrpcClient xmlrpc;

    /**
     * Constructor.
     * @param host the host where the XmlSearcher is running.
     * @param port the port where the XmlSearcher is running on.
     * @throws IllegalArgumentException
     *             if the host/port combination cannot generate a valid url.
     */
    public XmlSearcherClient(final String host, final int port) {
        final String url = "http://" + host + ":" + port;
        try {
            xmlrpc = new XmlrpcClient(new URL(url));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid url: " + url
                    + ". Check host and port parameters.");
        }
    }

    /**
     * A very basic search method. It doesn't allow to specify filters or sorts.
     * 
     * @param query
     *            the string of the query.
     * @see com.flaptor.search4j.searcher.query.QueryParser for the format of
     *      the input string.
     * @param start
     *            the first document of the resultset to return.
     * @param count
     *            the number of documents to return.
     * @return the returned resultset.
     * @throws XmlRpcException 
     * @throws UnsupportedOperationException 
     * @throws XmlRpcFault 
     * @see com.flaptor.search4j.searcher.XmlSearcher for the format of the
     *      returned data.
     */
    public Vector<?> search(final String query, final int start, final int count) throws XmlRpcException {
        Vector<Object> params = new Vector<Object>();
        Vector<?> result = (Vector<?>) xmlrpc.execute("searcher", "xmlsearch", new Object[]{query, start, count});
        return result;
    }

    private static void printUsage() {
        System.out.println("XmlSearcherClient host port query first count");
    }

    public static void main(final String args[]) throws Exception {
        if (args.length != 5) {
            printUsage();
            return;
        }
        XmlSearcherClient client = new XmlSearcherClient(args[0], Integer.parseInt(args[1]));
        Vector<?> result = client.search(args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        System.out.print("showing " + (Integer) result.elementAt(0) + " results ");
        System.out.println("out of " + (Integer) result.elementAt(1) + " total matches\n");
        for (int i = 2; i < result.size(); i++) {
            Vector<?> doc = (Vector<?>) result.elementAt(i);
            for (int j = 0; j < doc.size(); j++) {
                Vector<?> field = (Vector<?>) doc.elementAt(j);
                System.out.println((String) field.elementAt(0) + ": "
                        + (String) field.elementAt(1));
            }
        }

    }

}
