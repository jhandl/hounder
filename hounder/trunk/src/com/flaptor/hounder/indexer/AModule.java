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
package com.flaptor.search4j.indexer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.dom4j.Document;

import com.flaptor.util.DocumentParser;
import com.flaptor.util.DomUtil;
import com.flaptor.util.IOUtil;
import com.flaptor.util.RunningState;
import com.flaptor.util.Stoppable;



/**
 * This interface defines a module in the indexer pipeline. This module does not
 * know its order in the sequence, or where it's output will go. All it can do
 * is take the provided document and return zero, one, or more documents that
 * may be based on it. The most common scenario is a module that returns the
 * provided document with some new fields in it, or a filter that returns most
 * documents untouched but consumes the rest.
 * @author Flaptor Development Team
 */
public abstract class AModule implements Stoppable{

    private static final Logger logger = Logger.getLogger(AModule.class);
    protected RunningState state = RunningState.RUNNING;

    /**
     * Processes a document.
     * This is the method to call.
     * @param doc the input dom4j Document. Must not be null
     * @return an array of dom4j Documents, may be 0
     * @throws IllegalStateException if the module is stopped.
	 * @throws IllegalArgumentException if the document is null.
     */
    public final Document[] process(final Document doc) {
        if (state != RunningState.RUNNING) {
            String s = "process: proccess called, but the module is stopping or stopped.";
            logger.fatal(s);
            throw new IllegalStateException(s);
        }
		if (null == doc) {
			throw new IllegalArgumentException("doc must not be null.");
		}
        return internalProcess(doc);
    }
    
    /**
     * Every subclass must implement here the functionality it provides.
     * @param doc the input dom4j Document
     * @return an array of dom4j Documents, may be 0
     */
    protected abstract Document[] internalProcess(Document doc);
    
    /**
     * Returns true if the module is stopped.
     */
    public final boolean isStopped() {
        return state == RunningState.STOPPED;
    }
    
    /**
     * Signals the AModule to stop.
     * A typical AModule has no internal state and thus can me stopped at any
     * time, but subclasses may have to do many thing to flush their internal
     * state.
     * In any case this method must return immediatly and the caller must check
     * the return value of isStopped() to know when it is really stopped.
     * If this method is overridden, the subclass must remember to set the state
     * to STOPPED before ending, otherwise the system will hang waiting for it to
     * stop.
     */
    public void requestStop() {
        state = RunningState.STOPPED;
    }

   
    /**
     * Helper method for main on subclasses.
     * Gets a filename as parameter, and processes it with the
     * concrete module, sending output to stdout.
     *
     * Subclasses should do this as main :
     *
     *
     * <pre>
     *  public static void main(String[] args) {
     *      AModule mod = new ConcreteModule();
     *      mod.mainHelper(args);
     *  }
     * </pre>
     *
     *
     * @param file Xml file to process with this module
     */
    public void mainHelper(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: com.flaptor.search4j.indexer.AModule <file>");
            System.exit(1);
        }
        File ifile = new File(args[0]);
        InputStream fis = new FileInputStream(ifile);
        String content = IOUtil.readAll(fis);
        Document doc = new DocumentParser().genDocument(content);
        Document[] output = process(doc);

        for (Document d: output) {
            System.out.println(DomUtil.domToString(d));
            System.out.println(" ");
        }
    }

}
