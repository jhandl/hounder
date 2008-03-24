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
package com.flaptor.hounder.searcher.payload;

import org.apache.lucene.search.DefaultSimilarity;
import org.apache.log4j.Logger;

/**
 * A Scorer that scores payloads depending the date
 * they contain, and the current date. 
 *
 * That date is determined at runtime, when a query is
 * performed.
 * @author Flaptor Development Team
 */
public class DatePayloadScorer extends DefaultSimilarity implements PayloadScorer {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());

    public float scorePayload(byte[] payload) {
        if (payload.length == 0) return 1f;
        try {

            long date = new LongPayload(payload).asLong() / 1000 ;
            long now = System.currentTimeMillis() / 1000;

            long diff = Math.abs(now - date);
            float ret = 1.0f;

            if (diff < 3600*24*30) {
                ret = 5.0f; //last month
            }

            if (diff < 3600*24*7) {
                ret = 10.0f; //last week
            }

            if (diff < 3600*48) {
                ret = 20.0f; // last two days
            }

            if (diff < 3600*24) {
                ret = 30.0f; //last day
            }

            if (diff < 3600*12) {
                ret = 40.0f; //last 12 hours
            }

            if (diff < 7200) {
                ret = 60.0f; //last two hours
            }

            if (diff < 3600) {
                ret = 100.0f;
            }

            return ret;

        } catch (Exception e) {
            logger.error("scorePayload: " + e.getMessage(),e);
            return 1f;
        }
    }

    @Override
    public float scorePayload(String fieldName, byte[] payload, int offset, int length) {
         // ignore offset and length parameters... Is this ok?
         // also ignore fieldname, as we should have been called by
         // a SimilarityForwarder
        return scorePayload(payload);
    }
}
