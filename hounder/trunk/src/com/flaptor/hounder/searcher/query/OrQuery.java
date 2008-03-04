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

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;

/**
 * @author Flaptor Development Team
 */
public final class OrQuery extends ABinaryOperator implements Serializable {

    public OrQuery(final AQuery lt, final AQuery rt) {
        super(lt, rt);
    }

    public OrQuery(final float boost, final AQuery lt, final AQuery rt) {
        super(boost, lt, rt);
    }
    
    public org.apache.lucene.search.Query getLuceneQuery() {
        BooleanQuery bq = new BooleanQuery();
        if (leftTerm instanceof OrQuery) {
        	for (BooleanClause bc : ((BooleanQuery)leftTerm.getLuceneQuery()).getClauses()) {
        		bq.add(bc);
        	}
        } else {
        	bq.add(leftTerm.getLuceneQuery(), BooleanClause.Occur.SHOULD);
        }
        if (rightTerm instanceof OrQuery) {
        	for (BooleanClause bc : ((BooleanQuery)rightTerm.getLuceneQuery()).getClauses()) {
        		bq.add(bc);
        	}        	
        } else {
        	bq.add(rightTerm.getLuceneQuery(), BooleanClause.Occur.SHOULD);
        }
        bq.setBoost(boost);
        return bq;
    }
    
    @Override
    public String toString() {
        return "( " + leftTerm.toString() + " ) OR ( " + rightTerm.toString() + " );boost: " + boost;
    }

    /**
     * Implements a very simple equality that considers a OR b = b OR a
     */
    @Override
    public boolean equals(final Object obj)
    {
        if(this == obj)
            return true;
        if((obj == null) || (obj.getClass() != this.getClass()))
            return false;
        ABinaryOperator bq = (ABinaryOperator) obj;
        if (boost != bq.boost) {
            return false;
        }
        if (leftTerm.equals(bq.leftTerm) && rightTerm.equals(bq.rightTerm)) {
            return true;
        } else if (leftTerm.equals(bq.rightTerm) && rightTerm.equals(bq.leftTerm)) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode()
    {
        int hash = super.hashCode() ^ leftTerm.hashCode() ^ rightTerm.hashCode();
        return hash;
    } 
}
