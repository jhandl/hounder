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
package com.flaptor.search4j.crawler.modules;

import java.util.HashSet;

import org.apache.log4j.Logger;

import com.flaptor.util.Config;
import com.flaptor.util.Execute;


/**
 * Abstract Processor Module. It provides config loading capabilities,
 * as well as passThroughOnTags loading. 
 *
 * It requires that config files have pass.through.on.tags and
 * pass.through.on.missing.tags  defined.
 * If it is not the case, it will complain and never ignore a tag.
 * 
 * @author Flaptor Development Team
 */
public abstract class AProcessorModule implements IProcessorModule {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    
    /**
     * If a doc {@link #process(FetchDocument)} cames with some of the tags 
     * defined here, the document will be ignored and returned as is. 
     */
    protected HashSet<String> passThroughOnTags;
    /**
     * If a doc {@link #process(FetchDocument)} cames without some of the tags 
     * defined here, the document will be ignored and returned as is. 
     */
    protected HashSet<String> passThroughOnMissingTags;

    protected final Config config;      // The config for the module.
    protected final String moduleName;  // The name of the module.

    protected final String IS_HOTSPOT_TAG;
    protected final String EMIT_DOC_TAG;


    /**
     * Instantiates an AProcessorModule, with the given name.
     * The given name will be used to load a configuration file, that
     * is named nameModule.properties.
     *
     * @param name
     *              The name of the module. It will be used to load
     *              configuration.
     * 
     * @throws IllegalArgumentException 
     *              if the module name is "" or null, or there is no
     *              name.properties file.
     */
    public AProcessorModule (String name, Config globalConfig) {
        if (null == name || "".equals(name)) {
            throw new IllegalArgumentException("Illegal module name");
        }
        IS_HOTSPOT_TAG = globalConfig.getString("hotspot.tag");
        EMIT_DOC_TAG = globalConfig.getString("emitdoc.tag");
        
        moduleName = name;
        config = Config.getConfig(name + "Module.properties");

        // Set pass through on tags. read from config
        passThroughOnTags = loadTags("pass.through.on.tags");        
        // Set pass through on missing tags. read from config
        passThroughOnMissingTags = loadTags("pass.through.on.missing.tags");
    }


    private final boolean ignoreDocument (final FetchDocument doc){
        for (String tag : passThroughOnTags){
            if (doc.hasTag(tag)){
                return true;
            }            
        }
        for (String tag : passThroughOnMissingTags){
            if (tag.length() < 1){// if empty, tag="";
                continue;
            }
            if (!doc.hasTag(tag)){
                return true;
            }            
        }
        return false;
    }

    public void process (FetchDocument doc) {
        if (ignoreDocument(doc)){
            return;
        }
        internalProcess(doc);
    }

    /**
     * Process a document
     * @param doc the document to process 
     */
    protected abstract void internalProcess (FetchDocument doc);


    /**
     * Gets a multivalued variable from the config, splits it and returns
	 * the values in a hashset.
	 * It also prints to the logger the name of each tag.
     * @param strTag the name of the configuration variable.
     * @return the values of the configuration variable, splitted by the
	 * "," character.
     */
    protected HashSet<String> loadTags(String strTag) {
        HashSet<String> set= new HashSet<String>();
        String[] tags;
        tags= config.getStringArray(strTag);
        logger.debug(strTag + ": " + tags);        
        for (String tag: tags) {
            tag = tag.trim();
            if (tag.length() > 0) {
                logger.debug("Adding tag: " + tag);
                set.add(tag);
            }
        }
        return set;
    }

    /**
     * @see IProcessorModule#applyCommand(Object)
     */
    // While is Object, will create a ICommand when necessary
    public void applyCommand(Object command){
        // Usually modules will do nothing with the commands
        // If a module knows some commands, just override this method
    }


    protected String getModuleName() {
        return moduleName;
    }
    
    @Override
    public String toString() {
        return getClass().getName() + ":" + getModuleName();
    }
}
