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
import org.dom4j.Node;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.flaptor.util.DomUtil;
import com.flaptor.util.Execute;
import com.flaptor.util.Config;
import com.flaptor.util.Pair;

/**
 * A Module to ensure that certain fields have a specific format.
 * This Module can help ensure that sorting will not have NumberFormatExceptions
 * on runtime.
 *
 * TODO add more field types. At this moment, we are just verifying long
 *  
 *
 * @author Flaptor Development Team
 */
public final class FieldFormatCheckerModule extends AModule {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    private Set<String> longFields;
    // TODO insert more sets here


    public FieldFormatCheckerModule() {
        Config config = Config.getConfig("indexer.properties");
        List<Pair<String,String>> pairList = config.getPairList("FieldFormatChecker.fields");

        longFields = new HashSet<String>();

        for (Pair<String,String> pair: pairList) {
            String type = pair.last();
            if ("long".equalsIgnoreCase(type)) {
                longFields.add(pair.first());
                continue;
            }

            // TODO insert more field types here

            // if no one handled this type, WARN
            logger.warn("there is no checker for type: " + type + " . Ignoring " + pair );
        }    
    }




    /**
     * Processes the document. Takes the xml document, prints it to the logger,
     * and returns the same document.
     */
    protected Document[] internalProcess(final Document doc) {

        // check that this is a documentAdd
        // otherwise, skip.
        Node root = doc.getRootElement();
        if (!root.getName().equals("documentAdd")) return new Document[]{doc};

       
        for (String longField: longFields) {
            Node node = doc.selectSingleNode("//field[@name='"+longField+"']");
            if (null == node) {
                logger.error("Document lacks field " + longField +". Dropping document. ");
                if ( logger.isDebugEnabled()){
                    logger.debug(DomUtil.domToString(doc) + " lacks field " + longField);
                }
                return new Document[0];
            }

            String text = node.getText();
            try { 
                Long.parseLong(text);
            } catch (NumberFormatException e) {
                logger.error("Document has field " + longField + ", but it is not parseable as Long. Dropping document");
                if ( logger.isDebugEnabled()){
                    logger.debug(DomUtil.domToString(doc) + " contains field " + longField + " but it is not parseable as Long.");
                }
                return new Document[0];
            }
        }

        // TODO insert more field type checks here
        Document[] docs = {doc};
        return docs;
    }


    public static void main(String[] args) throws Exception {
        AModule mod = new FieldFormatCheckerModule();
        mod.mainHelper(args);
    }
}
