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

import org.apache.log4j.Logger;
import org.dom4j.Document;

import com.flaptor.util.DomUtil;
import com.flaptor.util.Execute;

/**
 * This class implements a module that logs the xml document that passes through
 * it. The data is logged to the info level. The received document is sent
 * unmodified
 * Implementation note: this module could be not thread-safe. Use with caution.
 * @author Flaptor Development Team
 */
public final class LoggerModule extends AModule {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    /**
     * Processes the document. Takes the xml document, prints it to the logger,
     * and returns the same document.
     */
    protected Document[] internalProcess(final Document doc) {
        logger.info(DomUtil.domToString(doc));
        Document[] docs = {doc};
        return docs;
    }


    public static void main(String[] args) throws Exception {
        AModule mod = new LoggerModule();
        mod.mainHelper(args);
    }
}
