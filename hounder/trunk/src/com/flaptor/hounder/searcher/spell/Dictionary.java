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
package com.flaptor.hounder.searcher.spell;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;

import com.flaptor.hist4j.AdaptiveHistogram;
import com.flaptor.util.Pair;

/**
 * @author Flaptor Development Team
 */
public class Dictionary implements Iterable<Pair<String,Float>>{

    IndexReader reader;
    String field;

    public Dictionary (IndexReader reader,String field) {
        this.reader = reader;
        this.field = field;
    }
    
    public final Iterator<Pair<String,Float>> iterator() {
        return new LuceneIterator();
      }


    private class LuceneIterator implements Iterator<Pair<String,Float>> {
        
        private TermEnum termEnum;
        private int threshold;
        private int maxFreq;
        private boolean has_next_called;
        private Pair<String,Float> actualPair;
        private int seen;

        public LuceneIterator() {
            seen = 0;
            try {
                int terms = 0;
                maxFreq = 0;
                AdaptiveHistogram hist = new AdaptiveHistogram();
                termEnum = reader.terms(new Term(field,""));

                while (termEnum.next()) {
                    hist.addValue(termEnum.docFreq());
                    terms++;
                    maxFreq = Math.max(maxFreq,termEnum.docFreq());
                }

                threshold = (int)hist.getValueForPercentile(90);

                termEnum = reader.terms(new Term(field,""));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public Pair<String,Float> next() {
            if (!has_next_called) {
                hasNext();
            }
            has_next_called = false;
            return (actualPair != null) ? actualPair : null;
        }

        public boolean hasNext() {
            has_next_called = true;
            try {
                // if there is still words
                if (!termEnum.next()) {
                    actualPair = null;
                    return false;
                }
                seen++;
                boolean found = true;
                boolean ended = false;
                while (termEnum.docFreq() < threshold) {
                    ended = !termEnum.next();
                    seen++;
                    if (ended) break;
                }

                if (0 == (seen % 100 )) {
                    System.out.println("Seen " + seen + " terms");
                }
                
                if (ended) {
                    actualPair = null;                    
                    return false;
                }

                //  if the next word are in the field
                Term term = termEnum.term();
                String fieldt = term.field();
                if (fieldt != field) {
                    actualPair = null;
                    return false;
                } else {
                    actualPair = new Pair<String,Float>(term.text(),calculateBoost(termEnum.docFreq()));
                }
                return true;
            } catch (IOException ex) {
                ex.printStackTrace();
                return false;
            } 

        }

        public void remove(){}

        
        private Float calculateBoost(int freq) {
            // linear
            return new Float(freq - threshold + 1);
        }
    }
}
