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

import java.util.Map;

import org.apache.log4j.Logger;

import com.flaptor.hounder.classifier.bayes.MultiClassifier;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;



/**
 *  This module makes bayesian classification of documents, using a
 *  Multiclassifier. Classifies every document it gets, unless it 
 *  has any tag on passThroughTags. Does not flag documents as Hotspots.
 *
 *  In order to flag classified documents as hotspots, use another module
 *  that takes into consideration what this module classified.
 *  
 * @author Flaptor Development Team
 */
public class BayesianClassifierModule extends AThresholdModule {

    
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    private int textLengthLimit; // the maximum allowed page text length.
    private final String[] categories; // the names of the categories.
    private final MultiClassifier multiClassifier; // classifier that defines if the page is a hotspot and to which category it belongs.
    private final float categoryThreshold; // threshold to add category to document
    
    public BayesianClassifierModule (String moduleName, Config globalConfig) {   
        super(moduleName, globalConfig);
        textLengthLimit = globalConfig.getInt("page.text.max.length");
        Config mdlConfig = getModuleConfig();
        String categoryList = mdlConfig.getString("categories");        
        if ((categoryList != null) && !"".equals(categoryList.trim())) {
            categories = categoryList.split(",");
            double unknownTermsProbability = mdlConfig.getFloat("unknown.terms.probability");
            categoryThreshold   = mdlConfig.getFloat("category.score.threshold");            
            multiClassifier = new MultiClassifier(categories, unknownTermsProbability);
        } else { // categories is null or ""
            // It makes no sense to have a BayesianClassifierModule that 
            // has no categories. Someone misplaced this module in the modules
            // manager, or forgot to set categories. Either way, fail.
            logger.warn("Categories not found. This module is useless configured this way.");
            throw new IllegalArgumentException("Classifier does not have categories. Something in the configuration is wrong");
        }

    }
    
    
    /**
	 * @todo review exception handling.
	 * @fixme there're problems with 2 bayer modules.
	 */
    @Override
    protected Double tInternalProcess (FetchDocument doc) {
        Double overallScore= null;
        String text = doc.getText(textLengthLimit);
        if (null == text) {
            logger.warn("Document has no parsed text. Ignoring this document.");
            return null;
        }

        // Determine if the page is a hotspot by using the bayesian classifier
        Map<String,Double> classification = null;
        // Constructor makes sure that the classifier is not null.
        // this check does not make much sense
        if (multiClassifier != null) {
            // Classify the page using filters
            classification = multiClassifier.getNamedScores(text);
            // Check which categories deserve to go to document categories
            for (String category: categories) {
                Double score = classification.get(category);
                if (score >= categoryThreshold){
                    doc.addCategory(category); 
                }
            }
            // Check if the document should be tagged as hotspot
            overallScore = classification.get(MultiClassifier.OVERALL_SCORE);
            
            try {
                // FIXME: WHAT IF THERE ARE 2 BAYES MODULES? 
                doc.addAttribute(CLASSIFICATION_OBJECT, classification);
            } catch (Exception e) {
                logger.error("Classification data already set by some other module",e); // TODO: should re-throw?
            }
        }
        return overallScore;
    }

}
