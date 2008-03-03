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
 * Abstract Processor Module. It provides tagging/untagging facilities according
 * to the result of the tfInternalProcess.
 *
 * It requires that config files to have the following tags defined:
 * <ul> 
 * <li>on.true.set.tags </li>
 * <li>on.true.unset.tags</li>
 * <li>on.false.set.tags</li>
 * <li>on.false.unset.tags</li>
 * </ul>
 * If it is not the cse, it will complain and never ignore a tag.
 * 
 * @author Flaptor Development Team
 */
public abstract class ATrueFalseModule extends AProcessorModule {

    static final Logger logger = Logger.getLogger(Execute.whoAmI());
    
    /**
     * Stores the tags stated in on.true.set.tags
     */
    protected HashSet<String> onTrueSetTags;

    /**
     * Stores the tags stated in on.true.unset.tags
     */
    protected HashSet<String> onTrueUnSetTags;

    /**
     * Stores the tags stated in on.false.set.tags
     */
    protected HashSet<String> onFalseSetTags;

    /**
     * Stores the tags stated in on.false.unset.tags
     */
    protected HashSet<String> onFalseUnSetTags;

    
    public ATrueFalseModule(String name, Config globalConfig) {
        super(name, globalConfig);
        if (null == name || "".equals(name)) {
            throw new IllegalArgumentException("Illegal module name");
        }
        onTrueSetTags=    loadTags("on.true.set.tags");
        onTrueUnSetTags=  loadTags("on.true.unset.tags");
        onFalseSetTags=   loadTags("on.false.set.tags");
        onFalseUnSetTags= loadTags("on.false.unset.tags");
    }

    @Override
    protected final void internalProcess(FetchDocument doc) {
        Boolean res= tfInternalProcess(doc);
        if (null == res){
            return;
        }
        HashSet<String> setTags;
        HashSet<String> unSetTags;
        if (res.booleanValue()){// true
            setTags= onTrueSetTags;
            unSetTags= onTrueUnSetTags;
        } else { //false
            setTags= onFalseSetTags;
            unSetTags= onFalseUnSetTags;                        
        }

        for (String tag : setTags){
            if (tag.length() < 1){// if empty, tag="";
                continue;
            }
            try {
                if (!doc.hasTag(tag)){
                    doc.addTag(tag);
                }
            } catch (Exception e) {                
                logger.error(e);
                throw new RuntimeException(e);
            }
        }
        for (String tag : unSetTags){
            if (tag.length() < 1){// if empty, tag="";
                continue;
            }
            if (doc.hasTag(tag)){
                doc.delTag(tag);
            }
        }

    }
    
    /**
     * Processes a document. Tags will be added/remove/untouched to the document 
     * according to the returned value (true,false,null) and the config file.
     * @param doc the document to process
     * @return true: The tags stated in on.true.set.tags will be set (added)
     *               The tags stated in on.true.unset.tags will be unset (removed)
     *         false: The tags stated in on.false.set.tags will be set (added)
     *                The tags stated in on.false.unset.tags will be unset (removed)
     *         null: no tag will be added/removed
     * The tags are the defined in the config file
     */
    abstract protected Boolean tfInternalProcess(FetchDocument doc);
    

}
