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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.Pair;

/**
 * This class implements a parser for html documents contained in the body
 * element of the content. It searches for the "body" element in the second
 * level (child of the root element) and adds a field element containing all the
 * (html) readable information as a sibling to the original body element.
 * @author Flaptor Development Team
 */
public class HtmlParser extends AModule {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private final Set<Pair<String,String>> tags;
    private final boolean STORED;
    private final boolean INDEXED;
    private com.flaptor.search4j.util.HtmlParser parser;
    // List of extra fields to extract. useful to extract META tags, and such.
    private final List<String> extraFields;


    /**
     * Constructor.
     *
     * @todo the extra fields will use the same stored|indexed setting as defined
     * on HtmlParser.stored and HtmlParser.indexed. It would be nice to have the
     * posibility to optionally override it for each field.
     */
    public HtmlParser() {
        Config conf = Config.getConfig("indexer.properties");

        String[] inputTagNames = conf.getStringArray("HtmlParser.inputTagNames");
        String[] outputFieldNames= conf.getStringArray("HtmlParser.outputFieldNames");
        if (inputTagNames.length != outputFieldNames.length) {
            throw new IllegalArgumentException("Length of inputTagName list does not match length of outputFieldName list.");
        }
        tags = new HashSet<Pair<String, String>>();
        for (int i = 0; i < inputTagNames.length; i++) {
            tags.add(new Pair<String, String>(inputTagNames[i], outputFieldNames[i]));
        }

        String removedXPathElements = conf.getString("HtmlParser.removedXPath");
        String[] separatorTags = conf.getStringArray("HtmlParser.separatorTags");
        List<Pair<String,String>> extraFieldMapping = conf.getPairList("HtmlParser.extraFieldMapping");
        
        Map<String,String> mapping = new HashMap<String,String>();
        extraFields = new ArrayList<String>(extraFieldMapping.size());

        for (Pair<String,String> pair: extraFieldMapping) {
            mapping.put(pair.first(),pair.last());
            extraFields.add(pair.first());
        }
        
        parser = new com.flaptor.search4j.util.HtmlParser(removedXPathElements, separatorTags,mapping);

        STORED = conf.getBoolean("HtmlParser.stored");
        INDEXED = conf.getBoolean("HtmlParser.indexed");
        if (!(STORED || INDEXED)) {
            throw new IllegalArgumentException("constructor: both indexed an stored are set to false in the configuration.");
        }
    }

    /**
     * Parses the html document and extracts the indexable text.
     * 
     * @param doc
     *            the dom4j doc to process.
     * @return a single document, in wich some fields may have been added, using
     *         the info from the "body" field. The fields are added at the same
     *         level as the "body" and the original "body" is preserved.
     */
    public final Document[] internalProcess(final Document doc) {
        Document[] docs = { doc };
        try {
            for (Pair<String, String> tag : tags) {
                processTag(doc, tag.first(), tag.last());
            }
        } catch (Exception e) {
            logger.error("internalProcess: while running processTag:" + e.getMessage(),e);
            return null;
        }
        return docs;
    }

    /**
     * Parses a tag to produce a field.
     * @param doc the doc to modify
     * @throw exception on error, signaling the main method to return no document.
     */
    private void processTag(Document doc, final String tagName, final String fieldName) throws Exception {
        Node bodyElement = doc.selectSingleNode("/*/" + tagName);
        if (null == bodyElement) {
            logger.warn("Content element missing from document. I was expecting a '"+tagName+"'. Will not add '"+fieldName+"' field.");
            return;
        }

        Node destElement = doc.selectSingleNode("//field[@name='"+fieldName+"']");        
        if (null != destElement) {
            logger.warn("Parsed element '"+fieldName+"' already present in document. Will not overwrite.");
            return;
        }

        com.flaptor.search4j.util.HtmlParser.Output out = parser.parse("",bodyElement.getText());
        

        for (String field: extraFields) {
            String content = out.getField(field);
            if (null == content) {
                logger.debug("had document without " + field + " field. Continuing with other fields.");
                continue;
            }
            Element docField = DocumentHelper.createElement("field");
            docField.addText(content);
            docField.addAttribute("name",field);
            docField.addAttribute("indexed", Boolean.toString(INDEXED));
            docField.addAttribute("stored", Boolean.toString(STORED));
            docField.addAttribute("tokenized", "true");
            bodyElement.getParent().add(docField);
        } 


        String text = out.getText();
        Element field = DocumentHelper.createElement("field");
        field.addText(text);
        field.addAttribute("name", fieldName);
        field.addAttribute("indexed", Boolean.toString(INDEXED));
        field.addAttribute("stored", Boolean.toString(STORED));
        field.addAttribute("tokenized", "true");
        bodyElement.getParent().add(field);
    }

    public static void main(String[] args) throws Exception {
        AModule mod = new HtmlParser();
        mod.mainHelper(args);
    }

}
