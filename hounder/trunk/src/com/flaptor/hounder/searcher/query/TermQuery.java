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

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;

/**
 * Query that searches for a specific term in a specific field.
 * The term passed to the constructor does not suffer any modifications, no case conversions,
 * no tokenization. So great care has to be taken to be sure the term passed is consistent with
 * the tokenization made at index time.
 * @author spike
 *
 */
public final class TermQuery extends AQuery implements Serializable {
    private final String field;
    private final String term;
    
    /**
     * Basic constructor.
     * @param field the field where to look the term in. Must not be null.
     * @param term the term to search for. Must not be null.
     * @throws IllegalArgumentException if term or field are null.
     */
    public TermQuery(final String field, final String term) {
        super();
        if (null == field) throw new IllegalArgumentException("constructor: field must not be null.");
        if (null == term) throw new IllegalArgumentException("constructor: term must not be null.");
        this.field = field;
        this.term = term;
    }

    /**
     * Advanced constructor.
     * Receives a boost to apply to the query.
     * @param boost the boost to apply to this query. It must be > 0. The score of the query will be
     *      calculated as the score of the unboosted query multiplied by the boost.
     * @param field the field where to look the term in. Must not be null.
     * @param term the term to search for. Must not be null.
     * @throws IllegalArgumentException if term or field are null or if boost <= 0
     */
    public TermQuery(final float boost, final String field, final String term) {
        super(boost);
        if (null == field) throw new IllegalArgumentException("constructor: field must not be null.");
        if (null == term) throw new IllegalArgumentException("constructor: term must not be null.");
        this.field = field;
        this.term = term;
    }
    
    @Override
    public Query getLuceneQuery() {
        org.apache.lucene.search.TermQuery luceneQuery = new org.apache.lucene.search.TermQuery(new Term(field, term));
        luceneQuery.setBoost(boost);
        return luceneQuery;
    }

    @Override
    public String toString() {
        return "field: " + field + "; term: " + term + ";boost:" + boost;
    }
    
    @Override
    public boolean equals(final Object obj)
    {
        if(this == obj)
            return true;
        if((obj == null) || (obj.getClass() != this.getClass()))
            return false;
        TermQuery tq = (TermQuery) obj;
        return field.equals(tq.field) && term.equals(tq.term) && boost == tq.boost;
    }
    
    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 17 * hash + field.hashCode();
        hash = 17 * hash + term.hashCode();
        hash = hash ^ super.hashCode();
        return hash;
    } 
}
