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


import com.flaptor.util.Config;
import com.flaptor.util.DomUtil;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;

import com.flaptor.util.Execute;
import java.util.Arrays;
import java.util.HashSet;
import org.dom4j.DocumentHelper;

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
public class SanitizerModule extends AModule {
	private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private static com.flaptor.util.HtmlParser htmlParser = new com.flaptor.util.HtmlParser();
    private HashSet<String> htmlFields,xmlFields,accentFields,allFields;
    private String xpath;
    
	/**
	 * Constructor.
	 * @param indexer a reference to the indexer that contains this module.
	 */
	public SanitizerModule() {
        Config config = Config.getConfig("indexer.properties");
        xpath = config.getString("SanitizerModule.XPath");
        htmlFields = new HashSet<String>(Arrays.asList(config.getStringArray("SanitizerModule.html")));
        xmlFields = new HashSet<String>(Arrays.asList(config.getStringArray("SanitizerModule.xml")));
        accentFields = new HashSet<String>(Arrays.asList(config.getStringArray("SanitizerModule.accents")));
        allFields = new HashSet<String>();
        allFields.addAll(htmlFields);
        allFields.addAll(xmlFields);
        allFields.addAll(accentFields);
	}

	/**
	 * @param doc a non null document to process
	 */
	public Document[] internalProcess(final Document doc) {
		Element root = doc.getRootElement();
		if (null != root) {
            for (String name : allFields) {
            	Element elem = (Element)root.selectSingleNode(xpath.replace("$", name));
                if (null != elem) {
                    try {
                        String text = elem.getText();
                        
                        if (htmlFields.contains(name)) {
                            text = htmlParser.parse("internal document", text).getText();
                        }
                        
                        if (xmlFields.contains(name)) {
                            text = DomUtil.filterXml(text);
                        }
                        
                        if (accentFields.contains(name)) {
                            text = filterAccents(text);
                        }
                        
                        elem.setText(text);
                    } catch (Exception e) {
                        logger.warn("Sanitizing field "+name,e);
                    }
                }
            }
        }
        Document[] docs = {doc};
        return docs;
	}

    private String filterAccents(String text) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case 'á':
                case 'Á':
                    c = 'a';
                    break;
                case 'é':
                case 'É':
                    c = 'e';
                    break;
                case 'í':
                case 'Í':
                    c = 'i';
                    break;
                case 'ó':
                case 'Ó':
                    c = 'o';
                    break;
                case 'ú':
                case 'Ú':
                    c = 'u';
                    break;
                case 'ñ':
                case 'Ñ':
                    c = 'n';
                    break;
            }
            buf.append(c);
        }
        return buf.toString();
    }
    
    public static void main(String[] args) {
        String text = args[0];
        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("documentAdd");
        root.addElement("text")
            .addText(text);
        root.addElement("field")
            .addAttribute("name", "text")
            .addAttribute("indexed", "true")
            .addAttribute("stored", "true")
            .addAttribute("tokenized", "true")
            .addText(text);
        SanitizerModule mod = new SanitizerModule();
        Document[] docs = mod.internalProcess(doc);
        for (Document d : docs) {
            System.out.println(DomUtil.domToString(d));
        }
    }
}

