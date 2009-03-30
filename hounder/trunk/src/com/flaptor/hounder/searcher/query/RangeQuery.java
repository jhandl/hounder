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
import org.apache.lucene.search.ConstantScoreRangeQuery;

/**
 * This is an AQuery to filter for a range of values.
 *
 * @author Flaptor Development Team
 */
public class RangeQuery extends AQuery {
    private static final long serialVersionUID = 1L;
    private final String field;
    private final String start;
    private final String end;
    private final boolean includeStart;
    private final boolean includeEnd;

    /**
     * Creates a RangeQuery with the given parameters
     */
    public RangeQuery(String field, String start, String end, boolean includeStart, boolean includeEnd) {
        super(1.0f);
        this.field = field;
        this.start = start;
        this.end = end;
        this.includeStart = includeStart;
        this.includeEnd = includeEnd;
    }
    
    /**
     * @return a lucene query.
     * @see org.apache.lucene.search.Query
     */
    public org.apache.lucene.search.Query getLuceneQuery() {
        return new ConstantScoreRangeQuery(field,start,end,includeStart,includeEnd);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if((obj == null) || (obj.getClass() != this.getClass()))
            return false;
        RangeQuery q = (RangeQuery) obj;
        return boost == q.boost 
                && field.equals(q.field)
                && start.equals(q.start)
                && end.equals(q.end)
                && includeStart == q.includeStart
                && includeEnd == q.includeEnd;
    }
    
    @Override
    public int hashCode() {
        return Float.floatToIntBits(boost) 
                ^ field.hashCode() 
                ^ start.hashCode() 
                ^ end.hashCode() 
                ^ Boolean.valueOf(includeStart).hashCode() 
                ^ Boolean.valueOf(includeEnd).hashCode();
    }

}
