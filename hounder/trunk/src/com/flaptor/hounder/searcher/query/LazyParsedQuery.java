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

/**
 * @author Flaptor Development Team
 */
public class LazyParsedQuery extends AQuery {

        private org.apache.lucene.search.Query lq = null;
        private final String queryStr;

        public LazyParsedQuery(String queryString) {
            if (null == queryString) {
                throw new IllegalArgumentException("queryString cannot be null.");
            }
            queryStr = queryString;
        }

        public  org.apache.lucene.search.Query getLuceneQuery() {
            if (null == lq) {
                lq = new QueryParser().parse(queryStr);
            }
            return lq;
        }

        public String getQueryString() {
            return queryStr;
        }
        
        @Override
            public int hashCode() {
                return queryStr.hashCode();
            }

        @Override
            public boolean equals(Object obj) {
                if(this == obj)
                    return true;
                if((obj == null) || (obj.getClass() != this.getClass()))
                    return false;
                return queryStr.equals(((LazyParsedQuery)obj).queryStr);
            }
    }
