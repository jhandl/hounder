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

/**
 * Helper class to hold some magic numbers and constants needed by the bayes filter.
 * @author Flaptor Development Team
 * 
 */
public final class BayesProbabilities {

    // Values determined by heuristic analisys
    public static final double CATEGORY_MIN_PROBABILITY = 0.01;
    public static final double CATEGORY_MAX_PROBABILITY = 0.99;
    // There's an optimization made if CATEGORY_MIN_PROBABILITY + CATEGORY_MAX_PROBABILITY sums ONE!
    public static final boolean MIN_PLUS_MAX_EQUALS_ONE = (1 == CATEGORY_MIN_PROBABILITY + CATEGORY_MAX_PROBABILITY);

    // Default probability value for not yet known terms.
    public static final double CATEGORY_DEFAULT_PROBABILITY = 0.5;
    // There's an optimization made if CATEGORY_DEFAULT_PROBABILITY equals 0.5!
    public static final boolean DEFAULT_EQUALS_0_5 = (0.5 == CATEGORY_DEFAULT_PROBABILITY);


    /**
     * Private empty default constructor to prevent inheritance and instantiation.
     */
    private BayesProbabilities() {}

}

