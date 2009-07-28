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
 * This class converts a hounder's document add dom to a lucene document.
 */

public class DocumentConverter {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    private static class LazyHolder {
        private static final DocumentConverter documentConverter = new DocumentConverter();
    }

    public static DocumentConverter getInstance() {
        return LazyHolder.documentConverter;
    }


    private HashSet<String> requiredFields;   // mandatory fields for a document
    private HashSet<String> compressedFields; // mandatory fields for a document
    private HashSet<String> requiredPayloads; // mandatory fields for a document

    private String docIdName = null;


    

    private DocumentConverter() {
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
     *Converts a hounder's add document into a lucene document.
     *This method is thread safe.
     *@throws IllegalArgumentException if the document is malformed, if it's not an add
     *if it does not contain the required fields, etc.
    */
    public org.apache.lucene.document.Document convert(final Document doc) throws IllegalDocumentException {
        Element root = doc.getRootElement();
        if (root.getName().equals("documentAdd")) {
            return processAdd(root);
        } else {
            throw new IllegalDocumentException("This is not an add document.");
        }
    }

	/**
	 * @todo refactor this method, is too long
	 */
    private org.apache.lucene.document.Document processAdd(final Element e) throws IllegalDocumentException {
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
            throw new IllegalDocumentException("Document with invalid boost (" + documentBoost + ") received.");
        }

        org.apache.lucene.document.Document ldoc = new org.apache.lucene.document.Document();
        ldoc.setBoost(documentBoost);
		
		// For comparison with the required fields we keep track of the added
		// fields.
		HashSet<String> providedFields = new HashSet<String>();

		//First, we add the documentId as a field under the name provided in the configuration (docIdName)
        node = e.selectSingleNode("documentId");
		if (null == node) {
			throw new IllegalDocumentException("Add document missing documentId.");
		}
		String docIdText = node.getText();
		//now we add the documentId as another field, using the name provided in the configuration (docIdName)
		Field lfield = new Field(docIdName, docIdText, Field.Store.YES, Field.Index.NOT_ANALYZED);
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
				throw new IllegalDocumentException("Field without name.");
			}

			//There cannot be a field with the name used to store the documentId (docIdName)
			//as it would collide with the documentId per se when saved to the lucene index.
			fieldText = field.getText();
			if (fieldName.equals(docIdName)) {
				throw new IllegalDocumentException("This document contains a field with the same name as the configured name "
                        + "to save the documentId( " + docIdName + ").");
			}

			storedS = field.valueOf("@stored");
			if (storedS.equals("")) {
				throw new IllegalDocumentException("Field without stored attribute.");
			}
			stored = Boolean.valueOf(storedS);

			indexedS = field.valueOf("@indexed");
			if (indexedS.equals("")) {
				throw new IllegalDocumentException("Field without indexed attribute.");
			}
			indexed = Boolean.valueOf(indexedS);
			//Lucene complains of an unindexed unstored field with a runtime exception
			//and it makes no sense anyway
			if (!(indexed || stored)) {
				throw new IllegalDocumentException("processAdd: unindexed unstored field \"" + fieldName + "\".");
			}

			tokenizedS = field.valueOf("@tokenized");
			if (tokenizedS.equals("")) {
				throw new IllegalDocumentException("Field without tokenized attribute.");
			}
			tokenized = Boolean.valueOf(tokenizedS);

			boostS = field.valueOf("@boost");
			if (!boostS.equals("")) {
				try {
					boost = new Float(boostS).floatValue();
				} catch (NumberFormatException exception) {
					throw new IllegalDocumentException("Unparsable boost value (" + boostS + ") for field  \"" + fieldName + "\".");
				}
			}

			// Now we add the fields. Depending on the parameter stored, indexed
			// and tokenized we call a different field constructor.
			lfield = null;
			Field.Index indexType = (indexed ? (tokenized ? Field.Index.ANALYZED : Field.Index.NOT_ANALYZED) : Field.Index.NO);
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
				throw new IllegalDocumentException("Payload without name.");
			}
            providedPayloads.add(payloadName);
            try { 
                Long payloadValue = Long.parseLong(payload.getText());
                ldoc.add(new Field(payloadName,new FixedValueTokenStream(payloadName,payloadValue)));
                logger.debug("Adding payload \""+payloadName+"\" to document \"" + docIdText + "\" with value " + payloadValue);
            } catch (NumberFormatException nfe) {
                throw new IllegalDocumentException("Writer - while parsing Long payload \""+payloadName+"\": " + nfe.getMessage());
            }
        }


		// no we test for the presence of the required fields
		if (!providedFields.containsAll(requiredFields) || !providedPayloads.containsAll(requiredPayloads)) {
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
            throw new IllegalDocumentException(sb.toString());
		}
        return ldoc;
	}

    public static class IllegalDocumentException extends Exception {
        public IllegalDocumentException(String message) {
            super(message);
        }
    }
}
