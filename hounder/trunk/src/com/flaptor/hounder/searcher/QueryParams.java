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
package com.flaptor.search4j.searcher;

import java.io.Serializable;
import java.util.Vector;

import com.flaptor.search4j.searcher.filter.AFilter;
import com.flaptor.search4j.searcher.group.AGroup;
import com.flaptor.search4j.searcher.query.AQuery;
import com.flaptor.search4j.searcher.sort.ASort;
import com.flaptor.util.remote.ConnectionException;

/**
 * class that stores the query parameters
 * 
 * @author Martin Massera
 */
public class QueryParams implements Serializable {
    private final Vector<Object> params = new Vector<Object>(7);

    
    /**@todo AFILTER y ASORT tienen equals y hashcode??? */ 
    public QueryParams(AQuery query, int firstResult, int count, AGroup group, int groupSize, AFilter filter, ASort sort) {
        params.add(query);
        params.add(firstResult);
        params.add(count);
        params.add(group);
        params.add(groupSize);
        params.add(filter);
        params.add(sort);
    }

    @Override
    public boolean equals(Object o) {
        if (o ==null) return false; 
        if (!QueryParams.class.equals(o.getClass())) {
            return false;
        }
        QueryParams other = (QueryParams) o;
        return params.equals(other.params);
    }

    @Override
    public int hashCode() {
        return params.hashCode();
    }
    
    /**
     * @return the vector containing all params, useful for storing the query in collections 
     */
    Vector<Object> getParamVector() {
        return params;
    }
    
    /**
     * executes the query with the given params
     * @param searcher the seacher to execute the query in
     * @return results of the query from that searcher
     */
    public GroupedSearchResults executeInSearcher(ISearcher searcher) throws SearcherException{
        return searcher.search(
                (AQuery)params.get(0),
                (Integer)params.get(1),
                (Integer)params.get(2),
                (AGroup)params.get(3),
                (Integer)params.get(4),
                (AFilter)params.get(5),
                (ASort)params.get(6));
    }

    /**
     * executes the query with the given params in a remote searcher
     * @param searcher the remote seacher to execute the query in
     * @return results of the query from that searcher
     * @throws ConnectionException 
     */
    public GroupedSearchResults executeInRemoteSearcher(IRemoteSearcher searcher) throws ConnectionException {
        return searcher.search(
                (AQuery)params.get(0),
                (Integer)params.get(1),
                (Integer)params.get(2),
                (AGroup)params.get(3),
                (Integer)params.get(4),
                (AFilter)params.get(5),
                (ASort)params.get(6));
    }
}

