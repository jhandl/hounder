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

import com.flaptor.hounder.searcher.payload.*;
import com.flaptor.util.Config;
import com.flaptor.util.QuadCurve;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.apache.log4j.Logger;
import org.apache.lucene.search.DefaultSimilarity;

/**
 * A Scorer that scores payloads depending on the numeric value of a payload. 
 * The difference with simply boosting the document at index time is that
 * the weight of this payload can changed on the fly without having to reindex.
 *
 * @author Flaptor Development Team
 */
public class ScalarPayloadScorer extends DefaultSimilarity implements PayloadScorer {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());
    private float weight;
    private long maxValue;
    private String name;

    public ScalarPayloadScorer(String fieldName) {
        name = fieldName;
        Config config = Config.getConfig("searcher.properties");
        weight = config.getFloat(fieldName+".payload.weight");
        maxValue = config.getLong(fieldName+".payload.max.value");
        if (maxValue <= 0) maxValue = 1;
    }


    public float scorePayload(byte[] payload) {
        float boost = 1f;
        if (payload.length >= 0) {
            try {
                long value = new LongPayload(payload).asLong();
System.out.print("PAYLOAD("+name+")="+value);
                value = Math.min(value,maxValue);
                boost = 1f+weight*value/maxValue;
System.out.println("  boost="+boost);
            } catch (Exception e) {
                logger.error("scorePayload: ",e);
            }
        }
        return boost;
    }

    @Override
    public float scorePayload(String fieldName, byte[] payload, int offset, int length) {
         // ignore offset and length parameters... Is this ok?
         // also ignore fieldname, as we should have been called by
         // a SimilarityForwarder
        return scorePayload(payload);
    }
}
