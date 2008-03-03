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
package com.flaptor.search4j.searcher.query;

import java.io.Serializable;


/**
 * An AQuery composed of 2 AQueries joined by a binary operatior.
 * This class simple adds the notion of a left operand and a right operand, and it's used to
 * implement ands, ors, -, etc.
 * @author Flaptor Development Team
 */
abstract class ABinaryOperator extends AQuery implements Serializable {
    protected final AQuery leftTerm;
    protected final AQuery rightTerm;
    
    /**
     * Constructor that uses the default boost for AQuery.
     * @see AQuery#AQuery()
     * @param lt the left term (operand)
     * @param rt the right term (operand)
     */
    ABinaryOperator(final AQuery lt, final AQuery rt) {
        super();
        if (null == lt) throw new IllegalArgumentException("constructor: left term must not be null");
        if (null == rt) throw new IllegalArgumentException("constructor: right term must not be null");
        leftTerm = lt;
        rightTerm = rt;        
    }
    
    /**
     * Full constructor. Allows to assign an arbitrary boost value.
     * @param boost the boost value for this AQuery (not for the right nor left operand,
     *      but for all this query). Must be > 0
     * @param lt the left term (operand)
     * @param rt the right term (operand)
     * @throws IllegalArgumentException if lt or rt are null or if boost <= 0
     */
    ABinaryOperator(final float boost, final AQuery lt, final AQuery rt) {
        super(boost);
        if (null == lt) throw new IllegalArgumentException("constructor: left term must not be null");
        if (null == rt) throw new IllegalArgumentException("constructor: right term must not be null");
        leftTerm = lt;
        rightTerm = rt;
    }
    
}
