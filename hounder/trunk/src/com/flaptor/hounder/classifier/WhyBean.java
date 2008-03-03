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
package com.flaptor.search4j.classifier;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.flaptor.search4j.classifier.bayes.BayesClassifier;
import com.flaptor.search4j.classifier.util.DocumentParser;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.cache.FileCache;


/**
 * Used to know why a url was classified the way it was.
 * @author rafa
 *
 */
public class WhyBean extends TrainingBean {

    private static final Logger LOGGER = Logger.getLogger(Execute.whoAmI());
    
    private Map<String, BayesClassifier> classifiers= new HashMap<String, BayesClassifier>();
    private int maxTuple;
    private FileCache<String>  cache;
    
    public WhyBean(){
        
    }
    
    public boolean reloadProbabilities(){
        for (String cat: config.getCategoryList()){
            classifiers.put(cat, new BayesClassifier(config.getBaseDir(), cat));
        }
        return true;
    }
    
    public boolean initialize(ConfigBean config){
        inited= (super.initialize(config) && reloadProbabilities());
        Config cfg= Config.getConfig("classifier.properties"); 
        maxTuple= cfg.getInt("document.parser.tuples.size");
        cache = new FileCache<String> (config.getCacheDir() + "/text"); // TODO: softcode /text
        return inited;
    }
    
 

    /**
     * Given a document,and a category return its score: the probability
     *  of that document to belongs to that category.
     * @param url the document
     * @param cat the category
     * @return
     * @throws NullPointerException if the category doesn't exist
     */    
    public synchronized Double getScore(String cat, String url)  {
        String item=cache.getItem(url);
        if (null==item){
            LOGGER.warn("Page " + url + "is not in cache");
            return null;
        }
        Map<String,int[]> par=DocumentParser.parse(item, maxTuple);
        double val= classifiers.get(cat).classify(par);
        return val;        
        
    }
    
    @SuppressWarnings("unused")
    private void debugPrintMap(Map<String,Double> mp){
        System.err.println("*****************************************");
        for (String key: mp.keySet()){
            System.err.println(key + "=>" + mp.get(key));
        }
        System.err.println("*****************************************");        
    }

    /**
     * Return a map of {token => value}, showing for each token on the given
     * url, what is its probability value for the given category.
     * @param category
     * @param url
     * @return
     */
    public Map<String,Double> getProbabilitiesMap(String category, String url) {
        String item=cache.getItem(url);
        if (null==item){
            LOGGER.warn("Page " + url + "is not in cache");
            return null;
        }
        Map<String,int[]> par=DocumentParser.parse(item, maxTuple);
        return  classifiers.get(category).getProbabilitiesMap(par);        
    }
    
    
    public static void main(String [] args){
        WhyBean wb= new WhyBean();
        String cacheDir= args[0];
        String baseDir= args[1];
        String categ= args[2];
        String[] categories= categ.split(",");
        ConfigBean bu= new ConfigBean();
        bu.initialize(categories, cacheDir, baseDir, null, -1);
        wb.initialize(bu);
        String url= args[3];
        for (String cat: categories){
            wb.getScore(url, cat);
            wb.getProbabilitiesMap(cat, url);            
        }
    }
}
