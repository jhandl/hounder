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

import com.flaptor.util.Config;
import com.flaptor.util.QuadCurve;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.apache.log4j.Logger;
import org.apache.lucene.search.DefaultSimilarity;

/**
 * A Scorer that scores payloads depending the date
 * they contain, and the current date. 
 *
 * That date is determined at runtime, when a query is
 * performed.
 * @author Flaptor Development Team
 */
public class ShortDatePayloadScorer extends DefaultSimilarity implements PayloadScorer {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());
    private static Calendar cal;
    private static float weight, damp;
    static {
        cal = new GregorianCalendar();
        weight = Config.getConfig("searcher.properties").getFloat("short.date.payload.weight");
        damp = Config.getConfig("searcher.properties").getFloat("short.date.payload.damp");
    }


    public float scorePayload(byte[] payload) {
        float boost = 1f;
        if (payload.length >= 0) {
            try {
                long shortDate = new LongPayload(payload).asLong();
                int year = (int)(shortDate/10000);
                shortDate -= year*10000;
                int month = (int)(shortDate/100);
                shortDate -= month*100;
                int day = (int)shortDate;
                cal.clear();
                cal.set(year,month-1,day);
                long date = cal.getTimeInMillis();
                long now = System.currentTimeMillis();
                float diff = Math.abs(now - date)/(1000L*60*60*24);  // days
                if (diff+damp == 0) diff += 0.1f;
                boost = 1f+(weight*damp)/(diff+damp);
System.out.println("Paylaod: "+year+"/"+month+"/"+day+"  now="+now+"  date="+date+"  days="+diff+"   ret:"+boost);
            } catch (Exception e) {
                logger.error("scorePayload: " + e.getMessage(),e);
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
