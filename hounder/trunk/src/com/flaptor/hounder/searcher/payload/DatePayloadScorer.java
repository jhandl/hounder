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
package com.flaptor.search4j.searcher.payload;

import java.util.Date;

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

    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());

    public float scorePayload(byte[] payload) {
        if (payload.length == 0) return 1f;
        try {

            long date = new LongPayload(payload).asLong() / 1000 ;
            long now = System.currentTimeMillis() / 1000;

            float ret = 1.0f;

            if (now - date < 3600*24*30) {
                ret = 5.0f; //last month
            }

            if (now - date < 3600*24*7) {
                ret = 10.0f; //last week
            }

            if (now - date < 3600*48) {
                ret = 20.0f; // last two days
            }

            if (now - date < 3600*24) {
                ret = 30.0f; //last day
            }

            if (now - date < 3600*12) {
                ret = 40.0f; //last 12 hours
            }

            if (now - date < 7200) {
                ret = 60.0f; //last two hours
            }

            if (now - date < 3600) {
                ret = 100.0f;
            }

            return ret;

        } catch (Exception e) {
            logger.error("scorePayload: " + e.getMessage(),e);
            return 1f;
        }
    }

    @Override
    public float scorePayload(byte[] payload, int offset, int length) {
        // ignore offset and length parameters... Is this ok?
        return scorePayload(payload);
    }
}
