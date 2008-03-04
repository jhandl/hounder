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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.search.DefaultSimilarity;

import com.flaptor.util.Config;
import com.flaptor.util.Pair;


/**
 *  This class makes no sense until we use lucene > 2.2
 *
 * @author Flaptor Development Team
 */
public class SimilarityForwarder extends DefaultSimilarity {

    private final Map<String,PayloadScorer> scorers;
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());

    public SimilarityForwarder(){
        this(Config.getConfig("searcher.properties"));
    }


    public SimilarityForwarder(Config config) {
        scorers = new HashMap<String,PayloadScorer>();
        List<Pair<String,String>> pairs = config.getPairList("SimilarityForwarder.scorers");
        try { 
            for (Pair<String,String> pair: pairs) {

                PayloadScorer scorer = (PayloadScorer)Class.forName(pair.last()).newInstance();

                if (scorers.containsKey(pair.first())) {
                    PayloadScorer oldScorer = scorers.get(pair.first());
                    if (oldScorer.equals(scorer)) {
                        logger.warn("constructor: field " + pair.first() + " already defined. Ignoring second definition.");
                    } else {
                        String error = "constructor: field " + pair.first() + " had " + 
                            oldScorer.getClass().getName() + " associated,"+
                            "but it is redefined to have " +pair.last() + 
                            " associated. I can not tell which definition "+
                            "is ok, so I will fail.";
                        logger.error(error);
                        throw new IllegalStateException(error);
                    }
                }

                // otherwise, just put it to the map
                scorers.put(pair.first(),scorer);
            }
        } catch (Exception e) {
            logger.error("constructor: while creating PayloadScorers: ",e);
            throw new RuntimeException(e);
        }
    }


    public float scorePayload(String fieldName, byte[] payload, int offset, int length) {

        PayloadScorer scorer = scorers.get(fieldName);
        if (null == scorer) {
            return super.scorePayload(/*fieldName,*/ payload, offset, length);
        } else {
            // Lucene gives an array that is larger than the length parameter.
            // trim it.
            // TODO maybe this logic should be inside an abstract PayloadScorer?
            byte[] trimmedPayload = new byte[length];
            System.arraycopy(payload,offset,trimmedPayload,0,length);
            return scorer.scorePayload(trimmedPayload);
        }
    } 
}
