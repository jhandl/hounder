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
package com.flaptor.hounder.classifier.bayes;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.flaptor.util.Execute;

/**
 * This class implements a bayesian classifier for one single category.
 * The BayesClassifier's internal state is created at construction time, being immutable from that time.
 * So, this implementation is thread safe, i.e., it allows multiple concurrent calls to classify method.
 *
 * There is no way to update the probabilities values on the fly.
 * An external mechanism must handle reloads, creating a new BayesClassifier and replacing the old instance with the new one.
 * @author Flaptor Development Team
 */
public final class BayesClassifier {

    private static final Logger LOGGER = Logger.getLogger(Execute.whoAmI());

    // Default probability value for not yet known terms.
    public static final double CATEGORY_DEFAULT_PROBABILITY = 0.5;

    private final Map<String,Double> probabilitiesMap;
    private final String categoryName;
    private final double unknownTermsProbability;

    // If false, consider each token once.  If true, multiply the token probability by the number of ocurrencies of the token.
    private final boolean useTokenMultiplicity;

    
    /**
     * Constructs a new bayesian classifier.
     * @param categoryName the name of the category (used to create the name)
     */
    public BayesClassifier(String categoryName) {
        this(".", categoryName);
    }

    /**
     * Constructs a new bayesian classifier.
     * @param dataDir the name of the base directory
     * @param categoryName the name of the category (used to create the name)
     * @todo right now the parameter useTokenMultiplicity is hardcoded to false. Make configurable.
     */
    public BayesClassifier(String dataDir, String categoryName) {
        this(dataDir, categoryName, CATEGORY_DEFAULT_PROBABILITY);
    }

    /**
     * Constructs a new bayesian classifier.
     * @param dataDir the name of the base directory
     * @param categoryName the name of the category (used to create the name)
     * @param unknownTermsProbability the probability value used for unknown (non classified) terms
	 * @todo right now the parameter useTokenMultiplicity is hardcoded to false. Make configurable.
     */
    public BayesClassifier(String dataDir, String categoryName, double unknownTermsProbability) {
        // TODO: make it configurable
        useTokenMultiplicity = false;
        this.categoryName = categoryName;
        if (unknownTermsProbability < 0){
            this.unknownTermsProbability= CATEGORY_DEFAULT_PROBABILITY;
        } else {
            this.unknownTermsProbability = unknownTermsProbability;
        }
        Map<String,Double> myProbs=PersistenceManager.readProbabilitiesFromFile(dataDir, categoryName+".my.probabilities");        
        probabilitiesMap = PersistenceManager.readProbabilitiesFromFile(dataDir, categoryName+".probabilities");
        if (0 < myProbs.size()){
            probabilitiesMap.putAll(myProbs);
        }
        
    }
    
    
    public int getMaxTuple(){
        if (!probabilitiesMap.containsKey(BayesCalculator.MAX_TUPLE_SIZE)){
            LOGGER.info("No MAX_TUPLE_SIZE found. Using 1 ");
            return 1;
        }
        return probabilitiesMap.get(BayesCalculator.MAX_TUPLE_SIZE).intValue();        
    }
    
    public boolean isProbabilitiesFileEmpty(){
        return probabilitiesMap.isEmpty();
    }
    /**
     * Classifies a document, indicating the calculated confidence level the 
     * document belongs to the category with.
     * @param doc the document token data, as generated by parseDocument.
     * @return the confidence level of the document
     */
    public double classify(Map<String,int[]> doc) {
        double matchProbability = 1;
        double noMatchProbability = 1;
        int totalCount = 0;
        int matchCount = 0;
        int noMatchCount = 0;
        int unknownCount = 0;
        int i=0;
        for (String token : doc.keySet()) {
            Double probabilityRef = probabilitiesMap.get(token);
            double probability;
            if (useTokenMultiplicity) {
                totalCount += doc.get(token)[0];
            } else {
                totalCount ++;
            }
            // Not yet known terms have a default probability.
            if (probabilityRef == null) {
                unknownCount ++; // For debugging
                probability = unknownTermsProbability;
                if (LOGGER.isDebugEnabled()) LOGGER.debug("Unknown Token (" +categoryName+ "): " +token+ ", Prob: " +probability);
            } else {
                probability = probabilityRef.doubleValue();
                if (LOGGER.isDebugEnabled()) LOGGER.debug("Known Token (" +categoryName+ "): " +token+ ", Prob: " +probability);
                if (BayesProbabilities.MIN_PLUS_MAX_EQUALS_ONE) {
                    if (probability == BayesProbabilities.CATEGORY_MAX_PROBABILITY) {
                        if (useTokenMultiplicity) {
                            matchCount += doc.get(token)[0];
                        } else {
                            matchCount ++;
                        }
                        continue;
                    } else if (probability == BayesProbabilities.CATEGORY_MIN_PROBABILITY) {
                        if (useTokenMultiplicity) {
                            noMatchCount += doc.get(token)[0];
                        } else {
                            noMatchCount ++;
                        }
                        continue;
                    }
                }
            }
            if (useTokenMultiplicity) {
                probability = Math.pow(probability,(double)doc.get(token)[0]);
            }
            // Heuristic value, just to keep precision.
            if (i == 8) {
                i = 0;
                matchProbability *= 1024;
                noMatchProbability *= 1024;
            } else {
                i++;
            }
            matchProbability *= probability;
            noMatchProbability *= (1-probability);
        }
        int matchToNoMatchDifference = matchCount - noMatchCount;
        if (matchToNoMatchDifference != 0) {
            double remainingProbability;
            int remainingFactors;
            if (matchToNoMatchDifference > 0) {
                remainingFactors = matchToNoMatchDifference;
                remainingProbability = BayesProbabilities.CATEGORY_MAX_PROBABILITY;
            } else {
                remainingFactors = -matchToNoMatchDifference;
                remainingProbability = BayesProbabilities.CATEGORY_MIN_PROBABILITY;
            }
            matchProbability *= Math.pow(remainingProbability,(double)remainingFactors);
            noMatchProbability *= Math.pow((1-remainingProbability),(double)remainingFactors);
        }

        LOGGER.debug("unknown terms: " +unknownCount+ ", matching only terms: " +matchCount+ ", non-matching only terms: " +noMatchCount + "total terms: " +totalCount);
        LOGGER.debug("matchProbability (" +categoryName+"): " +matchProbability);
        LOGGER.debug("noMatchProbability (" +categoryName+"): " +noMatchProbability);
        double docMatchProbability = matchProbability/(matchProbability + noMatchProbability);
        LOGGER.debug("docMatchProbability (" +categoryName+ "): " +docMatchProbability);

        return docMatchProbability;
    }

    /**
     * Returns a map stating for each token in the received map, the probability
     * of taht token (the computed values overriden by 'my.probabilities' 
     * values)
     * @param doc
     * @return
     */
    public Map<String,Double> getProbabilitiesMap(Map<String, int[]> doc) {
        Map<String,Double> probMap = new HashMap<String,Double>();
        for (String token : doc.keySet()) {
            Double prob = probabilitiesMap.get(token);
            if (prob == null) {
                prob = Double.valueOf(unknownTermsProbability);
            }
            probMap.put(token, prob);
        }
        return probMap;
    }
    
    public Map<String,Double> getInternals() {
        return probabilitiesMap;
    }

}

