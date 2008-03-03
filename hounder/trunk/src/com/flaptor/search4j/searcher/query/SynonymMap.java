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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * SynonymMap extends org.apache.lucene.index.memory.SynonymMap, to provide
 * a way to use synonyms with a more user-friendly input.
 *
 * It uses an inputstream, with a format parseable by java.util.Properties,
 * where every key is associated with a list of tokens, separated by comma.
 * The key, list pairs are stored in a HashMap.
 *
 * When asked for synonyms for a given word, the synonyms are retrieved from
 * the HashMap, using the word as key.
 *
 * @author Flaptor Development Team
 */
public class SynonymMap extends org.apache.lucene.index.memory.SynonymMap implements WordSuggestor {

    private Map<String,String[]> map = new HashMap<String,String[]>();

    public SynonymMap (InputStream is) throws IOException {
        super(new ByteArrayInputStream(new byte[0]));
        Properties p = new Properties();
        p.load(is);
        for (Object key: p.keySet()) {
            map.put((String)key,((String)p.get(key)).trim().split(","));
            //p.remove(key); // CAN NOT REMOVE BECAUSE of java.util.ConcurrentModificationException
        }
        p = null;
    }

    public String[] getSynonyms(String word) {
        String[] synonyms = map.get(word);
        if (null == synonyms) {
            synonyms = new String[0];
        }

        return synonyms;
    }

    public String toString() {
        return map.toString();
    }


    public String[] suggestWords(String word) {
        return getSynonyms(word);
    }
}
