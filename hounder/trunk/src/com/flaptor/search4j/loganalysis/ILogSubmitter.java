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
package com.flaptor.search4j.loganalysis;

import java.util.Date;
import java.util.List;

/**
 * Interface for logging queries and results for later analysis
 * 
 * @author Martin Massera
 */
public interface ILogSubmitter {

	/**
	 * log a query
	 * 
	 * @param query the query string
	 * @param queryResults the number of results
	 * @param time the moment of the query
	 * @param userIP the ip of querying user
	 * @param firstResults the list of the first results, do not inform here if they are clicked or not
	 * @return the query id
	 */
	public long submitQuery(String query, int queryResults, Date time, String userIP, List<Result> firstResults); 
	
	/**
	 * log a clicked result of a query
	 * 
	 * @param queryId the query it belongs to, previously logged using reportQuery
	 * @param url the url of the result
	 * @param distance the position in the results list, irrespective of pagination
	 * @param clicked true if it was clicked
	 * @param tags
	 */
	public void submitClickedResult(long queryId, Result result);


    /**
     * This inner class represents a search result that can be logged.
     */
    public class Result {

        private String url;
        private int distance;
        private boolean clicked;
        private List<String> tags;

        /**
         * Constructor.
         * @param url the url of the search result.
         * @param distance the position of this result within the search result list, irrespective of pagination.
         * @param clicked true if the result was clicked by the user.
         * @param tags a list of tags associated with the result.
         */
        public Result(String url, int distance, boolean clicked, List<String> tags) {
            super();
            this.url = url;
            this.distance = distance;
            this.clicked = clicked;
            this.tags = tags;
        }

        /** 
         * Getter for the result url.
         * @return the result url.
         */
        public String getUrl() {
            return url;
        }

        /** 
         * Getter for the result distance.
         * @return the result distance.
         */
        public int getDistance() {
            return distance;
        }

        /** 
         * Getter for the clicked attribute.
         * @return the clicked attribute.
         */
        public boolean isClicked() {
            return clicked;
        }

        /** 
         * Getter for the result tags.
         * @return the result tags.
         */
        public List<String> getTags() {
            return tags;
        }

    }

}
