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
package com.flaptor.hounder.crawler.modules;

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
 * <li>on.below.threshold.set.tags </li>
 * <li>on.below.threshold.unset.tags</li>
 * <li>on.above.threshold.set.tags</li>
 * <li>on.above.threshold.unset.tags</li>
 * </ul>
 * If it is not the case, it will complain and never ignore a tag.
 * 
 * @author Flaptor Development Team
 */
public abstract class AThresholdModule extends AProcessorModule {

    static final Logger logger = Logger.getLogger(Execute.whoAmI());
    
    /**
     * Stores the tags stated in on.below.threshold.set.tags
     */
    protected HashSet<String> onBelowSetTags;

    /**
     * Stores the tags stated in on.below.threshold.unset.tags
     */
    protected HashSet<String> onBelowUnsetTag;

    /**
     * Stores the tags stated in on.above.threshold.set.tags
     */
    protected HashSet<String> onAboveSetTag;

    /**
     * Stores the tags stated in on.above.threshold.unset.tags
     */
    protected HashSet<String> onAboveUnsetTag;

    protected Double thresholdValue;
    
   
    public AThresholdModule(String name, Config globalConfig) {
        super(name, globalConfig);
        if (null == name || "".equals(name)) {
            throw new IllegalArgumentException("Illegal module name");
        }
        thresholdValue=  new Double (getModuleConfig().getFloat("threshold.value"));
        onBelowSetTags=    loadTags("on.below.threshold.set.tags");
        onBelowUnsetTag=  loadTags("on.below.threshold.unset.tags");
        onAboveSetTag=   loadTags("on.above.threshold.set.tags");
        onAboveUnsetTag= loadTags("on.above.threshold.unset.tags");
    }

    @Override
    protected final void internalProcess(FetchDocument doc) {
        Double res= tInternalProcess(doc);
        if (null == res){
            return;
        }
        HashSet<String> setTags;
        HashSet<String> unSetTags;
        if (res < thresholdValue){
            setTags= onBelowSetTags;
            unSetTags= onBelowUnsetTag;
        } else { // res >= threshold
            setTags= onAboveSetTag;
            unSetTags= onAboveUnsetTag;                        
        }

        for (String tag : setTags){
            if (tag.length() < 1){// if empty, tag="";
                continue;
            }
            try {
                doc.setTag(tag);
            } catch (Exception e) {                
                logger.error(e);
                throw new RuntimeException(e);
            }
        }
        for (String tag : unSetTags){
            if (tag.length() < 1){// if empty, tag="";
                continue;
            }
            doc.delTag(tag);
        }
        try { // we add an atribute to the doc, indicatin the score
            doc.addAttribute(getScoreAttributeName(), res);
        } catch (Exception e) {
            logger.warn(e,e);
        }
    }
        
    /**
     * The attribute name is the one returned by
     * this method. The attributed indicated the value calculated by the
     * AThresholdModule implementation.
     * @return
     */
    private String getScoreAttributeName(){
        return getModuleName() + "_MODULE_RESULTING_SCORE";
    }
    
    
    /**
     * Processes a document. Tags will be added/remove/untouched to the document 
     * according to the returned value (Long,null), the threshold and the config
     * file.
     * @param doc the document to process
     * @return A long that will be compared to the threshold.
     *          If higher (or equal), the tags on.above.* will be added/deleted
     *          If lower, the tags  on.below.* will be added/deleted
     *         null: no tag will be added/removed
     * The tags are the defined in the config file
     */
    abstract protected Double tInternalProcess(FetchDocument doc);
    

}
