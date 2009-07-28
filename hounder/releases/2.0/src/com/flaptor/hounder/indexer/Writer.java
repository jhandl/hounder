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

import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Field;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import com.flaptor.util.Config;
import com.flaptor.util.Execute;

/**
 * This class implements a module that parses a DOM document and adds or removes
 * a document from a IndexWriteProvider. It expects the dom document to have the
 * following format:
 * 
 * <pre>
 *    &lt;documentAdd&gt;
 *    &lt;documentId&gt; (String) &lt;/documentId&gt;
 *    [&lt;boost &gt;(float)(default=1)&lt;/boost&gt;]
 *    ( &lt;field name=(string) stored=(bool) indexed=(bool) tokenized=(bool) [boost=(float)(default=1)]&gt; (text) &lt;/field&gt;)*
 *    &lt;/documentAdd&gt;
 *   
 *    OR
 *   
 *    &lt;documentDelete&gt;
 *    &lt;documentId&gt; (String) &lt;/documentId&gt;
 *    &lt;documentDelete&gt;
 * </pre>
 * 
 * 
 * Configuration strings:
 *      Writer.fields: a comma-separated list of names of fields
 *          which must be present in a document for it to be valid.
 *      docIdName: the name to be used for the documentId while storing it in the index.
 *      Writer.compressedFields: a comma separated lisd of name of field which will be stored
 *      	in the index in compressed form. Useful for long text fields.
 *      
 * @author Flaptor Development Team
 */

public class Writer extends AInternalModule {

    private final IndexWriteProvider iwp = indexer.getIndexManager();

    private String docIdName = null;
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    public Writer(final Indexer indexer) {
        super(indexer);
		Config config = Config.getConfig("indexer.properties");
		docIdName = config.getString("docIdName");
        if (docIdName.equals("")) {
            throw new IllegalArgumentException("The docIdName cannot be empty");
        }

    }

    /**
     * Implementation of the Module interface. Depending on the document
     * received, the Writer can insert or delete a document, or process it as a
     * command.
     * @see Writer
     */
    protected Document[] internalProcess(final Document doc) {
        Element root = doc.getRootElement();
        if (root.getName().equals("documentAdd")) {
            try {
                iwp.addDocument(DocumentConverter.getInstance().convert(doc));
            } catch (DocumentConverter.IllegalDocumentException e) {
                logger.error("Exception while converting this document to lucene. Check the document format. This document "
                    + "won't be added to the index.", e);
            }
        } else if (root.getName().equals("documentDelete")) {
            processDelete(root);
        } else {
            logger.error("Invalid format received");
        }
        return null;
    }

	private void processDelete(final Element e) {
        if ( logger.isEnabledFor(Level.DEBUG)) { 
            logger.debug("Processing delete");
        }
		Node node = e.selectSingleNode("documentId");
		if (null != node) {
			iwp.deleteDocument(node.getText());
		} else {
			logger.error("documentId node not found. Ignoring deletion.");
		}
	}

}
