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
package com.flaptor.hounder.searcher.query;

import java.io.Serializable;

/**
 * This is an AQuery that makes Payload weight in the score formula.
 * To use Payloads, Hounder creates a Term on the document that 
 * has the field and the term equal, and associates a payload to it.
 *
 * So, when using payloads, that translates into a lucene BoostingTermQuery
 * with a Term(fieldName,fieldName).
 *
 * Usage:
 * <pre>
 *      AQuery q1 = new LazyParsedQuery("this is my query");
 *      AQuery q2 = new PayloadQuery("date");
 *
 *      AQuery finalQuery = new AndQuery(q1,q2);
 *
 *      searcher.search(finalQuery,...);
 * </pre>
 *
 * Note that using an AndQuery will make documents without a payload on field "fieldName"
 * be ommitted on the results.
 *
 *
 * Payloads then will be processed by the similarity assigned to "fieldName" 
 * on SimilarityForwarder.
 *
 * @author Flaptor Development Team
 */
public class PayloadQuery extends AQuery {
    private final String field;

    /**
     * Creates a PayloadQuery that will use the payload stored in field
     * as parameter to a Similarity (and that will affect its score)
     *
     * @param field
     *          The name of the field that has the payload.
     */
    public PayloadQuery(String field) {
        this(1f,field);
    }
    
    /**
     * Advanced constructor.
     * Receives a boost to apply to the query.
     * @see PayloadQuery(String)
     *
     * @param boost the boost to apply to this query. It must be > 0. The score of the query will be
     *      calculated as the score of the unboosted query multiplied by the boost.
     * @throws IllegalArgumentException boost <= 0
     */   
    public PayloadQuery(final float boost,final String field) {
        super(boost);
        this.field = field;
    }
    
    /**
     * @return a lucene query.
     * @see org.apache.lucene.search.Query
     */
    public org.apache.lucene.search.Query getLuceneQuery(){
        return new org.apache.lucene.search.payloads.BoostingTermQuery(new org.apache.lucene.index.Term(field,field));
    }

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
        PayloadQuery q = (PayloadQuery) obj;
        return boost == q.boost && field.equals(q.field);
    }
    
    @Override
    public int hashCode() {
        return Float.floatToIntBits(boost) ^ field.hashCode();
    }

}
