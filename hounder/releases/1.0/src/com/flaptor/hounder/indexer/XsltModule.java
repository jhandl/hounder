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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.junit.Before;
import org.junit.Test;

import com.flaptor.util.Config;
import com.flaptor.util.DocumentParser;
import com.flaptor.util.DomUtil;
import com.flaptor.util.Execute;
import com.flaptor.util.TestUtils;

/**
 * Transforms a document based on a xsl transformation.
 * Reads the transformation descriptor from a file. Set the configuration
 * variable "XsltModule.file".
 * @author Flaptor Development Team
 */
public class XsltModule extends AModule {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private final Transformer transformer;
    
    public XsltModule() {
        Config conf = Config.getConfig("indexer.properties");
        String xsltFileName = conf.getString("XsltModule.file");
        try {
            transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(xsltFileName));
        } catch (TransformerConfigurationException e) {
            logger.error("constructor: error creating the transformer.");
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    protected Document[] internalProcess(Document doc) {
        if (logger.isDebugEnabled()) {
            logger.debug("About to transform: " + DomUtil.domToString(doc));
        }

        DocumentSource source = new DocumentSource(doc);
        DocumentResult result = new DocumentResult();
        try {
            synchronized(transformer) {
                transformer.transform(source, result);
            }
        } catch (TransformerException e) {
            logger.error("internalProcess: exception while transforming document.", e);
            logger.error("offending document was: " + DomUtil.domToString(doc));
            return null;
        }
        
        // retrieve resultant document
        doc = result.getDocument();

        if (logger.isDebugEnabled()) {
            logger.debug("Transformed: " + DomUtil.domToString(doc));
        }
        
        
        return new Document[] {doc};
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        AModule mod = new XsltModule();
        mod.mainHelper(args);
    }

    
    
    //-----------------------------------------------------------------------------------
    //Testing
    @Before
    public void setUpConfig() throws Exception {
        File file = TestUtils.createTempFileWithContent("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" +
                "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
                    "<xsl:template match=\"/foo\">" +
                        "<newFoo>" +
                            "<text><xsl:value-of select=\"bar\"/></text>" +
                        "</newFoo>" +
                    "</xsl:template>" +
                "</xsl:stylesheet>");
        Config conf = Config.getConfig("indexer.properties");
        conf.set("XsltModulFile", file.getAbsolutePath());
    }
    
    @Test
    public void foo() {
        Document input = new DocumentParser().genDocument("<foo>" +
                "<bar>some text</bar>" +
                "</foo>");
        Document[] output = internalProcess(input);
        assertThat(output.length, is(1));
        Node node = output[0].selectSingleNode("/newFoo/text");
        assertThat(node, notNullValue());
        assertThat(node.getText(), is("some text"));
    }
}
