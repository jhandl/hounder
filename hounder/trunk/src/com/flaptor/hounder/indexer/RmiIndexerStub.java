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
package com.flaptor.hounder.indexer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.rmi.Remote;
import java.rmi.RemoteException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.flaptor.util.PortUtil;
import com.flaptor.util.remote.ARmiClientStub;
import com.flaptor.util.remote.AlwaysRetryPolicy;
import com.flaptor.util.remote.RpcException;

/**
 * A client-side IRemoteIndexer that connects with the server
 * via rmi.
 * @author Flaptor Development Team
 */
public class RmiIndexerStub extends ARmiClientStub implements IRemoteIndexer {
    private static Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());
    private IRmiIndexer remoteIndexer = null;
    
    /**
     * Constructor.
     * @param basePort the base port where the remote instance of Hounder is running.
     * @param host
     */
    public RmiIndexerStub(final int basePort, final String host) {
        super(PortUtil.getPort(basePort,"indexer.rmi"), host, new AlwaysRetryPolicy());
    }


    //@Override
    protected void setRemote(Remote remote) {
        this.remoteIndexer = (IRmiIndexer) remote;
    }


    public String toString() {
        return "RmiIndexerStub for indexer running at " + super.toString();
    }


    public int index(Document doc) throws RpcException {
        try {
            super.checkConnection();
            int res = remoteIndexer.index(doc);
            super.connectionSuccess();
            return res;
        } catch (RemoteException e) {
            logger.error(e,e);
            super.connectionFailure();
            throw new RpcException(e);
        }
    }

    public int index(String text) throws RpcException {
        try {
            super.checkConnection();
            int res = remoteIndexer.index(text);
            super.connectionSuccess();
            return res;
        } catch (RemoteException e) {
            logger.error(e,e);
            super.connectionFailure();
            throw new RpcException(e);
        }
    }



    // TODO when indexer supports exceptions instead of error codes,
    // IndexerException.toString() will be much nicer.
    private static String errorCodeToString(int error) {
        switch(error) {
            case Indexer.SUCCESS:           return "SUCCESS";
            case Indexer.PARSE_ERROR:       return "PARSE_ERROR";
            case Indexer.RETRY_QUEUE_FULL:  return "RETRY_QUEUE_FULL";
            case Indexer.FAILURE:           return "FAILURE";
        }
        return "UNKNOWN_ERROR";
    }


    @SuppressWarnings("static-access")
    private static Options getOptions() {
        Option host = OptionBuilder
            .withArgName("hostName")
            .hasArg()
            .withDescription("the hostname where the indexer is running")
            .isRequired()
            .withLongOpt("host")
            .create("h");
        Option port = OptionBuilder
            .withArgName("basePort")
            .hasArg()
            .withDescription("the basePort where the indexer is running")
            .isRequired()
            .withType(PatternOptionBuilder.NUMBER_VALUE)
            .withLongOpt("port")
            .create("p");
        Option delUrl = OptionBuilder
            .withArgName("url")
            .hasArg()
            .withDescription("a url to delete on the indexer")
            .withLongOpt("deleteUrl")
            .create("du");

        Option delFile = OptionBuilder
            .withArgName("url-file")
            .hasArg()
            .withDescription("a file containing urls to delete on the indexer")
            .withLongOpt("deleteFile")
            .create("df");

        Option optimize = new Option("o","optimize", false, "send optimize command to indexer" );
        Option checkpoint = new Option("c","checkpoint", false, "send checkpoint (and push) command to indexer" );
        Option stop = new Option("s","stop", false, "send stop command to indexer" );

        Options options = new Options();
        options.addOption(host);
        options.addOption(port);
        options.addOption(delUrl);
        options.addOption(delFile);
        options.addOption(optimize);
        options.addOption(checkpoint);
        options.addOption(stop);

        return options;
    }



    private static Document generateDeleteDocument(String url) {
        org.dom4j.Document dom = DocumentHelper.createDocument();
        Element root = dom.addElement("documentDelete");
        root.addElement("documentId").addText(url);
        return dom;
    }

    private static Document generateCommandDocument(String command) {
        org.dom4j.Document dom = DocumentHelper.createDocument();
        Element root = dom.addElement("command");
        root.addAttribute("name",command);
        return dom;
    }

    private static void indexOrFail(RmiIndexerStub stub,Document dom, String error) throws Exception {
        int ret = stub.index(dom);
        if (ret != Indexer.SUCCESS) {
            System.out.println(error + errorCodeToString(ret));
            System.exit(1);
        }
    }


    public static void main (String[] args) {


        // create the parser
        CommandLineParser parser = new PosixParser();
        CommandLine line = null;
        Options options = getOptions();
        try {
            // parse the command line arguments
            line = parser.parse( options, args );
        } catch( ParseException exp ) {
            // oops, something went wrong
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "RmiIndexerStub -h <hostName> -p <basePort> [options] ", options);
            System.exit(1);
        }




        boolean doOptimize = line.hasOption("optimize");
        boolean doCheckpoint = line.hasOption("checkpoint");
        boolean doStop = line.hasOption("stop");
        Integer port = ((Long)line.getOptionObject("port")).intValue();
        String host = line.getOptionValue("host");


        try {
            RmiIndexerStub stub = new RmiIndexerStub(port,host);


            if (line.hasOption("deleteUrl")) {
                String url = line.getOptionValue("deleteUrl");
                Document dom = generateDeleteDocument(url);
                indexOrFail(stub,dom,"Could not delete " + url);
                System.out.println("delete " + url + " command accepted by indexer");
            }

            if (line.hasOption("deleteFile")) {

                BufferedReader reader = new BufferedReader(new FileReader(line.getOptionValue("deleteFile")));
                while (reader.ready()) {
                    String url = reader.readLine();
                    if (url.length() > 0 && url.charAt(0) != '#') {  // ignore empty lines and comments
                        Document dom = generateDeleteDocument(url);
                        indexOrFail(stub,dom,"Could not delete " + url);
                        System.out.println("delete " + url + " command accepted by indexer");
                    }
                }
                reader.close();
            }


            if (doOptimize) { 
                Document dom = generateCommandDocument("optimize");
                indexOrFail(stub,dom,"Could not send optimize command.");
                System.out.println("optimize command accepted by indexer");
            }

            if (doCheckpoint) {
                Document dom = generateCommandDocument("checkpoint");
                indexOrFail(stub,dom,"Could not send checkpoint command.");
                System.out.println("checkpoint command accepted by indexer");

            } 
            if (doStop) {
                Document dom = generateCommandDocument("close");
                indexOrFail(stub,dom,"Could not send stop command.");
                System.out.println("stop command accepted by indexer");
            } 
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
