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


import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;

import com.flaptor.util.Execute;

/**
 * This class implements a module that captures commands and executes them.
 * The commands have the following format:
 * <pre>
 *   &lt; command name=(string) / &gt;
 * </pre>
 * where the command name may be "optimize" to schedule an index optimization;
 * , "close" to close the app cleanly or "checkpoint" to flush the index to disk.
 * 
 * @author Flaptor Development Team
 */
public class CommandsModule extends AInternalModule {
	private static final Logger logger = Logger.getLogger(Execute.whoAmI());

	/**
	 * Constructor.
	 * @param indexer a reference to the indexer that contains this module.
	 */
	public CommandsModule(final Indexer indexer) {
		super(indexer);
	}

	/**
	 * @param doc a non null document to process
	 */
	public Document[] internalProcess(final Document doc) {
		Element root = doc.getRootElement();
		if (root.getName().equals("command")) {
			String name = root.attributeValue("name");
			if (null == name) {
				logger.error("Invalid command: no name set. Ignoring it.");
			} else {
				if (name.equals("optimize")) {
					optimize();    
				} else if (name.equals("close")) {
					close();
				} else if (name.equals("checkpoint")) {
					checkpoint();
				} else {
					logger.error("Unknown command received. Ignoring it.");
				}
			}
			Document[] docs = {};
			return docs;
		} else {
			Document[] docs = {doc};
			return docs;
		}
	}

	private void optimize() {
		logger.info("Optimize command received.");
		indexer.getIndexManager().scheduleOptimize();
	}

	private void close() {
		logger.info("Close command received.");
		indexer.requestStop();
	}

	private void checkpoint() {
		logger.info("Checkpoint command received.");
		indexer.getIndexManager().makeDirectoryCheckpoint();
	}
}

