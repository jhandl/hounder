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

    private HashSet<String> requiredFields;   // mandatory fields for a document
    private HashSet<String> compressedFields; // mandatory fields for a document
    private HashSet<String> requiredPayloads; // mandatory fields for a document

    private String docIdName = null;

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    public Writer(final Indexer indexer) {
        super(indexer);
		Config config = Config.getConfig("indexer.properties");
		docIdName = config.getString("docIdName");
        if (docIdName.equals("")) {
            throw new IllegalArgumentException("The docIdName cannot be empty");
        }

		requiredFields = new HashSet<String>();
		compressedFields = new HashSet<String>();
		requiredPayloads = new HashSet<String>();

		String[] fields = config.getStringArray("Writer.compressedFields");
		for (int j = 0; j < fields.length; j++) {
			compressedFields.add(fields[j]);
			logger.info("The field \"" + fields[j] + "\" will be stored compressed in the index.");
			if (fields[j].equals(docIdName)) {
				logger.warn("Asked to compress the documentId field. It won't be compressed.");
			}
		}

		fields = config.getStringArray("Writer.fields");
		for (int j = 0; j < fields.length; j++) {
			requiredFields.add(fields[j]);
			logger.info("The field \"" + fields[j] + "\" will be checked for in every document.");
		}

        String[] payloads = config.getStringArray("Writer.payloads");
        for (String payload: payloads) {
            if ("".equals(payload)) {
                throw new IllegalArgumentException("\"\" can not be a payload.");
            }
            requiredPayloads.add(payload);
            logger.info("The payload \"" + payload + "\" will be checked for in every document.");
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
            processAdd(root);
        } else if (root.getName().equals("documentDelete")) {
            processDelete(root);
        } else {
            logger.error("Invalid format received");
        }
        return null;
    }

	/**
	 * @todo refactor this method, is too long
	 */
    private void processAdd(final Element e) {
        // TODO: This method is too long, refactor.
        logger.debug("Processing Add");

		float documentBoost;
		Node node = e.selectSingleNode("boost");
		if (null == node) {
			documentBoost = 1.0F;
		} else {
			documentBoost = Float.parseFloat(node.getText());
            if ( logger.isEnabledFor(Level.DEBUG)) { 
                logger.debug("Using non-default document boost of " + documentBoost);
            }
		}
        if (Float.isNaN(documentBoost) || Float.isInfinite(documentBoost) || documentBoost <= 0) {
            logger.error("Document with invalid boost received. Ignoring addition.");
            return;
        }

        org.apache.lucene.document.Document ldoc = new org.apache.lucene.document.Document();
        ldoc.setBoost(documentBoost);
		
		// For comparison with the required fields we keep track of the added
		// fields.
		HashSet<String> providedFields = new HashSet<String>();

		//First, we add the documentId as a field under the name provided in the configuration (docIdName)
        node = e.selectSingleNode("documentId");
		if (null == node) {
			logger.error("Document missing documentId. Cannot index it.");
			return;
		}
		String docIdText = node.getText();
		//now we add the documentId as another field, using the name provided in the configuration (docIdName)
		Field lfield = new Field(docIdName, docIdText, Field.Store.YES, Field.Index.UN_TOKENIZED);
		ldoc.add(lfield);
		providedFields.add(docIdName);
        if ( logger.isEnabledFor(Level.DEBUG)) { 
            logger.debug("Writer - adding documentId field:" + docIdName + ", index: true, store: true, token: false, text: "
				+ docIdText);
        }
		// Now we add the regular fields
		for (Iterator iter = e.elementIterator("field"); iter.hasNext();) {
			Element field = (Element) iter.next();
			String fieldName, storedS, indexedS, tokenizedS, boostS, fieldText;
			boolean stored, tokenized, indexed;
			float boost = 1;

			fieldName = field.valueOf("@name");
			if (fieldName.equals("")) {
				logger.error("Field without name. Ignoring add.");
				return;
			}

			//There cannot be a field with the name used to store the documentId (docIdName)
			//as it would collide with the documentId per se when saved to the lucene index.
			fieldText = field.getText();
			if (fieldName.equals(docIdName)) {
				logger.error("This document contains a field with the same name as the configured name to save the documentId( "
						+ docIdName + "). Cannot index this document.");
				return;
			}

			storedS = field.valueOf("@stored");
			if (storedS.equals("")) {
				logger.error("Field without stored attribute. Ignoring add");
				return;
			}
			stored = Boolean.valueOf(storedS);

			indexedS = field.valueOf("@indexed");
			if (indexedS.equals("")) {
				logger.error("Field without indexed attribute. Ignoring add.");
				return;
			}
			indexed = Boolean.valueOf(indexedS);
			//Lucene complains of an unindexed unstored field with a runtime exception
			//and it makes no sense anyway
			if (!(indexed || stored)) {
				logger.error("processAdd: unindexed unstored field \"" + fieldName + "\". Ignoring add.");
				return;
			}

			tokenizedS = field.valueOf("@tokenized");
			if (tokenizedS.equals("")) {
				logger.error("Field without tokenized attribute. Ignoring add.");
				return;
			}
			tokenized = Boolean.valueOf(tokenizedS);

			boostS = field.valueOf("@boost");
			if (!boostS.equals("")) {
				try {
					boost = new Float(boostS).floatValue();
				} catch (NumberFormatException exception) {
					logger.error("Error in input format while adding document.", exception);
					return;
				}
			}

			// Now we add the fields. Depending on the parameter stored, indexed
			// and tokenized we call a different field constructor.
			lfield = null;
			Field.Index indexType = (indexed ? (tokenized ? Field.Index.TOKENIZED : Field.Index.UN_TOKENIZED) : Field.Index.NO);
			Field.Store storeType;
			if (!stored) {
				storeType = Field.Store.NO;
			} else {
				if (compressedFields.contains(fieldName)) {
					storeType = Field.Store.COMPRESS;
				} else {
					storeType = Field.Store.YES;
				}
			}
			lfield = new Field(fieldName, fieldText, storeType, indexType);

			lfield.setBoost(boost);
			providedFields.add(fieldName); // for later comparison with the required fields

			ldoc.add(lfield);
            if ( logger.isEnabledFor(Level.DEBUG)) { 
                logger.debug("Writer - adding field:" + fieldName + ", index:" + indexed + ", store:" + stored + ", token:" + tokenized
                        + " ,boost: " + boost + ", text: " + fieldText);
            }
		} // for  (field iterator)

       

        HashSet<String> providedPayloads = new HashSet<String>();
		// Now we add the payloads
		for (Iterator iter = e.elementIterator("payload"); iter.hasNext();) {
			Element payload = (Element) iter.next();
			
            String payloadName = payload.valueOf("@name");
			if (payloadName.equals("")) {
				logger.error("Payload without name. Ignoring add.");
				return;
			}
            providedPayloads.add(payloadName);
            try { 
                Long payloadValue = Long.parseLong(payload.getText());
                ldoc.add(new Field(payloadName,new FixedValueTokenStream(payloadName,payloadValue)));
                logger.debug("Adding payload \""+payloadName+"\" to document \"" + docIdText + "\" with value " + payloadValue);
            } catch (NumberFormatException nfe) {
                logger.error("Writer - while parsing Long payload: " + nfe.getMessage() + " ignoring add." ,nfe);
                return;
            }
        }


		// no we test for the presence of the required fields
		if (providedFields.containsAll(requiredFields) && providedPayloads.containsAll(requiredPayloads)) {
			iwp.addDocument(ldoc);
		} else {
            StringBuffer sb = new StringBuffer();
			sb.append("Document with missing required fields or payloads. Ignoring addition.\n");
            sb.append("Provided fields are: \n");
			for (String field : providedFields ) {
				sb.append(field + "\n");
			}
			sb.append("The fields required are: \n");
			for (String field : requiredFields ) {
				sb.append(field + "\n");
			}

            sb.append("Provided payloads are: \n");
            for (String payload: providedPayloads) {
				sb.append(payload + "\n");
            }
            sb.append("Required payloads are: \n");
            for (String payload: requiredPayloads) {
				sb.append(payload + "\n");
            }
            logger.error(sb.toString());
		}
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
