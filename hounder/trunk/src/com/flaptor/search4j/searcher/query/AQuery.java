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
 * The base for all s4j queries.
 * Simply states that a s4j query can return a lucene query, although this is of no concern for
 * the application that use s4j.
 * NOTE: all class derived from AQuery should be immutable. (convention to simplify things later)
 * @author Flaptor Development Team
 */
public abstract class AQuery implements Serializable {
    protected final float boost;
    
    /**
     * Default constructor.
     * Sets the boost to 1f
     */
    protected AQuery() {
        boost = 1f;
    }
    
    /**
     * Advanced constructor.
     * Receives a boost to apply to the query.
     * @param boost the boost to apply to this query. It must be > 0. The score of the query will be
     *      calculated as the score of the unboosted query multiplied by the boost.
     * @throws IllegalArgumentException boost <= 0
     */   
    protected AQuery(final float boost) {
        if (boost <=0 ) {
            throw new IllegalArgumentException("Boost must be > 0");
        }
        this.boost = boost;
    }
    
    /**
     * Abstract method that returns a lucene query that represents this s4j query.
     * @return a lucene query.
     * @see org.apache.lucene.search.Query
     */
    public abstract org.apache.lucene.search.Query getLuceneQuery();

    /**
     * At this level, 2 AQueries are equal if they are members of the same class and if they
     * have the same boost.
     * This method should be overriden by AQuery subclasses, but can be called by them
     * for convenience. 
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if((obj == null) || (obj.getClass() != this.getClass()))
            return false;
        AQuery q = (AQuery) obj;
        return boost == q.boost;
    }
    
    @Override
    public int hashCode() {
        return Float.floatToIntBits(boost);
    }

    public String toString() {
        return getLuceneQuery().toString();
    }
}
