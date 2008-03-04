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
package com.flaptor.hounder.classifier;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.flaptor.hounder.classifier.bayes.BayesCalculator;
import com.flaptor.hounder.classifier.bayes.BayesClassifier;
import com.flaptor.hounder.classifier.bayes.PersistenceManager;
import com.flaptor.hounder.classifier.util.DocumentParser;
import com.flaptor.hounder.classifier.util.ProbsUtils;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.cache.FileCache;

/**
 * For a given category, reads the list of URLs that match that category 
 * (cat_included_urls) and those that don't (cat_notIncluded_urls).
 * It then parses each document and uses a BayesCalculator to calculate the
 * probability of each token.
 * Finally it writes the probability file for the given category
 * Also updates the .my.probabilities file for the given category.
 *
 * @author Flaptor Development Team
 */
public class CacheCalculatorBean extends TrainingBean{

    private static final Logger LOGGER = Logger.getLogger(Execute.whoAmI());

    private List<String> includedUrlsList; 
    private List<String> notIncludedUrlsList;
    private Config cfg; 
    private int maxTuple;
    private FileCache<String>  cache;

    public CacheCalculatorBean(){        
    }
    
    public boolean initialize(ConfigBean config) {
        if (!super.initialize(config)){
            return false;
        }
        cfg= Config.getConfig("classifier.properties"); 
        maxTuple= cfg.getInt("document.parser.tuples.size");
        inited= true;
        cache = new FileCache<String> (config.getCacheDir() + "/text"); // TODO: softcode /text
        return inited;
    }


    /**
     * Loads the data from cat_included_urls/cat_notIncluded_urls 
     */
    void loadIncludedNotIncludedUrls(String catName){
        includedUrlsList= ProbsUtils.loadUrlsList(config.getBaseDir(), catName, ProbsUtils.INCLUDED);
        notIncludedUrlsList= ProbsUtils.loadUrlsList(config.getBaseDir(), catName, ProbsUtils.NOT_INCLUDED);
    }

    public Date getProbabilitiesFileDate(String categoryName){
        return BayesCalculator.getProbabilitiesFileDate(config.getBaseDir(), categoryName);        
    }
    
    public Map<String,Double> readProbabilities(String categoryName) {
        Map<String,Double> probs= PersistenceManager.readProbabilitiesFromFile(config.getBaseDir(),
                categoryName+".probabilities"); // TODO softcode it
        return probs;
    }
        
    /**
     * Calculates the probabilities for the given cat, and writes to the disc 
     * 'cat'.probabilities file
     * For the given 'cat', loads the files cat_SUFFIX_INCLUDED and 
     * cat_SUFFIX_NOT_INCLUDED, calculates the cat.probabilities 
     * and writes the cat.probabilities file
     * @param catName
     * @throws IOException 
     */
    public void calculate(String catName) throws IOException{
        loadIncludedNotIncludedUrls(catName);
        BayesCalculator calculator= new BayesCalculator(config.getBaseDir(), catName, cfg, maxTuple);        
        for (String url: includedUrlsList){
            String item=cache.getItem(url);
            if (null==item){
                LOGGER.warn("Page " + url + "is in included for " + catName + " but not in cache");
                continue;
            }
            calculator.addData(DocumentParser.parse(item, maxTuple),true, url);
        }

        for (String url: notIncludedUrlsList){
            String item=cache.getItem(url);
            if (null==item){
                LOGGER.warn("Page " + url + "is in notIncluded for " + catName + " but not in cache");                
                continue;
            }            
            calculator.addData(DocumentParser.parse(item, maxTuple),false, url);
        }
        calculator.computeProbabilities(); //and save the .probabilities to disk
    }

    public void setMyProbs(String newProbs, String catName) throws IOException{
        BayesCalculator calculator= new BayesCalculator(config.getBaseDir(), catName, cfg, maxTuple);
        String[] tokVals= newProbs.split("\\s+;\\s+");
        String key=null;
        Double val=null;
        for (String tv: tokVals){
            String []s= tv.split("=");
            key= s[0];
            val= Double.valueOf(s[1]);
            LOGGER.warn("Writing " + key + " = " + val);
            calculator.updateMyProbabilities(key, val, false); //false ~ noflush            
        }
        LOGGER.warn("Writing " + key + " = " + val);
        calculator.updateMyProbabilities(key, val, true); // true ~ flush        
    }

    public Map<String,Double> getMyProbabilities(String catName) throws IOException{
        BayesCalculator calculator= new BayesCalculator(config.getBaseDir(), catName, cfg, maxTuple);
        return calculator.getMyProbabilities();
    }


    /**
     * Verify the bayesian guesses.
     * Takes the urls in cat_included and cat_not_included and checks what the
     * bayesian classifier say.
     * @param catId: the id of the cat (it's position on the catOk array).
     * @return a map String => Set as follow
     *     'uinc_cinc' : list of urls that both the user and the classifier say are included
     *     'unot_cnot' : list of urls that both the user and the classifier say are not included
     *     'uinc_cnot' : list of urls that the user marked as included but the classifier say are not included
     *     'unot_cinc' : list of urls that the user marked as not included but the classifier say are included     
     */
    public Map<String,Map<String, Double>> verify(String catName) throws UnsupportedEncodingException{
        return verify(catName, true);
    }
    
    public Map<String,Map<String, Double>> verify(String catName, boolean loadMaps) 
    throws UnsupportedEncodingException{
        if (loadMaps) loadIncludedNotIncludedUrls(catName);
        BayesClassifier classifier= new BayesClassifier(config.getBaseDir(), catName);
        if (classifier.isProbabilitiesFileEmpty()){
            return null;
        }

        Map<String,Map<String, Double>> mp= new HashMap<String,Map<String, Double>>();        
        mp.put("uinc_cinc", new HashMap<String, Double>());
        mp.put("unot_cnot", new HashMap<String, Double>());
        mp.put("uinc_cnot", new HashMap<String, Double>());
        mp.put("unot_cinc", new HashMap<String, Double>());
        
        // traverse the list of included urls and check what the classifier say
        for (String url: includedUrlsList){
            String item=cache.getItem(url);
            if (null==item){
                LOGGER.warn("Page " + url + "is in included for " + catName + " but not in cache");
                continue;
            }
            double classifierScore = classifier.classify(DocumentParser.parse(item, classifier.getMaxTuple()));
            boolean classifierIncluded = (classifierScore > 0.5);
            addToIncNotIncMap(mp, url, true, classifierIncluded, classifierScore);            
        }
        // traverse the list of not included urls and check what the classifier say
        for (String url: notIncludedUrlsList){
            String item=cache.getItem(url);
            if (null==item){
                LOGGER.warn("Page " + url + "is in notIncluded for " + catName + " but not in cache");
                continue;
            }
            double classifierScore = classifier.classify(DocumentParser.parse(item, classifier.getMaxTuple()));
            boolean classifierIncluded = (classifierScore > 0.5);
            addToIncNotIncMap(mp, url, false, classifierIncluded, classifierScore);            
        }
        return mp;
    }


    private void addToIncNotIncMap(Map<String,Map<String, Double>> mp, String url, 
            boolean userIncluded, boolean classifierIncluded, double classifierScore){
        Map<String, Double> map;
        if (userIncluded){
            if (classifierIncluded){
                map= mp.get("uinc_cinc");
            } else {
                map= mp.get("uinc_cnot");
            }
        } else {
            if (classifierIncluded){
                map= mp.get("unot_cinc");
            } else {
                map= mp.get("unot_cnot");
            }                    
        }
        map.put(url, classifierScore);
    }

    
    public static void main(String[] args) {
        String [] categoriesName = null;
        String cacheDirName = null;
        CacheCalculatorBean cc= null;        

        cacheDirName= args[0];
        categoriesName= args[1].split(",");        
        cc= new CacheCalculatorBean();

        ConfigBean bu= new ConfigBean();
        bu.initialize(categoriesName, cacheDirName, ".", null, -1);
        cc.initialize(bu);
        for (String cat: categoriesName){
            try {
                cc.calculate(cat);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }                
}

