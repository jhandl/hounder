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

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;

/**
 * Represents de mathematical difference (lt - rt).
 * @author Flaptor Development Team
 */
public final class DifferenceQuery extends ABinaryOperator implements Serializable {

    public DifferenceQuery(final AQuery lt, final AQuery rt) {
        super(lt, rt);
    }

    public DifferenceQuery(final float boost, final AQuery lt, final AQuery rt) {
        super(boost, lt, rt);
    }

    public org.apache.lucene.search.Query getLuceneQuery() {
        BooleanQuery bq = new BooleanQuery();
        bq.add(leftTerm.getLuceneQuery(), BooleanClause.Occur.MUST);
        bq.add(rightTerm.getLuceneQuery(), BooleanClause.Occur.MUST_NOT);
        bq.setBoost(boost);
        return bq;
    }
    
    @Override
    public String toString() {
        return "( " + leftTerm.toString() + " ) - ( " + rightTerm.toString() + " );boost: " + boost;
    }

    /**
     * Implements a very simple equality.
     * a - b != b - a
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
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode()
    {
        int hash = 17;
        hash = 3 * hash + leftTerm.hashCode();
        hash = 3 * hash + rightTerm.hashCode();
        hash = hash ^ super.hashCode();
        return hash;
    } 
}
