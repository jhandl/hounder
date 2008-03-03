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
 * @author Flaptor Development Team
 */
public final class SimplePhraseQuery extends AQuery implements Serializable {
    private final String field;
    private final String[] terms;
    
    public SimplePhraseQuery(final String field, final String[] terms) {
        super();
        if (null == field) throw new IllegalArgumentException("constructor: field must not be null.");
        if (null == terms) throw new IllegalArgumentException("constructor: terms must not be null.");
        if (terms.length == 0) throw new IllegalArgumentException("constructor: terms must have at least one document.");
        this.field = field;
        //I'm going to make a defensive copy of terms, since array is not immutable.
        this.terms = new String[terms.length];
        for (int i = 0; i < terms.length; i++) {
            if (null == terms[i]) throw new IllegalArgumentException("constructor: term number " + i + " is null.");
            this.terms[i] = terms[i];
        }
    }

    public SimplePhraseQuery(final float boost, final String field, final String[] terms) {
        super(boost);
        if (null == field) throw new IllegalArgumentException("constructor: field must not be null.");
        if (null == terms) throw new IllegalArgumentException("constructor: terms must not be null.");
        if (terms.length == 0) throw new IllegalArgumentException("constructor: terms must have at least one document.");
        this.field = field;
        //I'm going to make a defensive copy of terms, since array is not immutable.
        this.terms = new String[terms.length];
        for (int i = 0; i < terms.length; i++) {
            if (null == terms[i]) throw new IllegalArgumentException("constructor: term number " + i + " is null.");
            this.terms[i] = terms[i];
        }
    }

    @Override
    public org.apache.lucene.search.Query getLuceneQuery() {
        org.apache.lucene.search.PhraseQuery pq = new org.apache.lucene.search.PhraseQuery();
        for (int i = 0; i < terms.length; i++) {
            pq.add(new org.apache.lucene.index.Term(field, terms[i]));
        }
        pq.setBoost(boost);
        return pq;
    }

    @Override
    public String toString() {
        return "field: " + field + "; terms: " + terms + ";boost: " + boost;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if(this == obj)
            return true;
        if((obj == null) || (obj.getClass() != this.getClass()))
            return false;
        SimplePhraseQuery spq = (SimplePhraseQuery) obj;
        if (boost != spq.boost) {
            return false;
        }
        if (!field.equals(spq.field)) {
            return false;
        } else if (spq.terms.length != terms.length) {
            return false;
        } else {
            for (int i = 0; i < terms.length; i++) {
                if (!spq.terms[i].equals(terms[i])) {
                    return false;
                }
            }
            return true;
        }
    }
    
    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 17 * hash + field.hashCode();
        hash = 17 * hash + terms.length;
        for (int i = 0; i < terms.length; i++) {
            hash = 17 * hash + terms[i].hashCode();
        }
        hash = hash ^ super.hashCode();
        return hash;
    } 
}
