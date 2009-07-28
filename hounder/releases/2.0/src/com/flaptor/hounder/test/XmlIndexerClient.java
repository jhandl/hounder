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
package com.flaptor.hounder.test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import com.flaptor.util.remote.XmlrpcClient;

/**
 * A client to an xml rpc Indexer.
 * Useful for testing, and as a starting point for custom Xmlrpc searcher clients. 
 * It's also a convenient way to send commands to the indexer using the main method.
 * @see com.flaptor.hounder.indexer.Indexer#index(String)
 * @see com.flaptor.hounder.indexer.Writer
 * @see com.flaptor.hounder.indexer.CommandsModule
 * @author Flaptor Development Team
 */
public class XmlIndexerClient {

    private final XmlrpcClient xmlrpc;


    /**
     * Constructor.
     * @param host the host where the XmlSearcher is running.
     * @param port the port where the XmlSearcher is running on.
     */
    public XmlIndexerClient(final String host, final int port) {
        final String url = "http://" + host + ":" + port;
        try {
            xmlrpc = new XmlrpcClient(new URL(url));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid url: " + url
                    + ". Check host and port parameters.");
        }
    }

    private static void printUsage() {
        System.out.println("usage: XmlIndexerClient host port file");
    }
    
    public static void main(final String args[]) {
        if (args.length != 3) {
            printUsage();
            return;
        }
        XmlIndexerClient client = new XmlIndexerClient(args[0], Integer.parseInt(args[1]));
        client.send(new File(args[2]));
    }


    /**
     * Sends the content of a single file (not a directory) to the indexer.
     * @param file the file which contents to send.
     */
    private void sendFile(final File file) {
        System.out.println("sending : " + file);
        StringBuffer buf = new StringBuffer();
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                buf.append(line + " ");
            }

            String toSend = buf.toString();
            send(toSend);
        }
        catch (Exception e) {
            System.err.println(e);
        }
    }

    public void send(final String message) {
        try {
            System.out.println("sending : " + message);
            System.out.println(xmlrpc.execute("indexer", "index", new Object[]{message}).toString());
        }
        catch (Exception e) {
            System.err.println(e);
        }
    }
    
    /**
     * Recursively sends a directory or sends a single file.
     * @param file a file or directory. If it's a file, it sends it's content. If it's a directory
     *      it traverses all it's subdirectories (and subdirs. of subdirs...) and send all files
     *      in them.
     */
    public void send(final File file) {
        if (file.isDirectory()) {
            String[] children = file.list();
            for (int i=0; i<children.length; i++) {
                send(new File(file, children[i]));
            }
        }
        else {
            sendFile(file);
        }
    }

}
