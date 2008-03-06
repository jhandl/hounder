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
package com.flaptor.hounder.classifier.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import com.flaptor.util.Execute;
import com.flaptor.hounder.util.TokenUtil;


/**
 * Helper class, it provides a convenience method to parse (i.e.: tokenize and filter) the content of a document 
 * and count the number of ocurrencies of each token.
 *
 * This implementation uses the Lucene's StandardAnalyzer to generate the list of terms.
 * Every term is lowercased and standard stop words are filtered out.
 * This implementation is thread-safe, as it creates a new analyzer instance for each document.
 *
 * @todo The tokenizer should be configurable in a properties file
 * @todo allow the analyzer to use to be configurable (decouple from lucene?)
 * @todo verify if it's necessary to create a new analyzer each time
 * @author Flaptor Development Team
 */
public class DocumentParser {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    /** Private empty default constructor to prevent inheritance and instantiation */
    private DocumentParser() {}
    
    
    /**
     * Transforms the document in an array of tokens and counts the number of 
     * ocurrencies of each token.
     * @param doc the document represented as a string
     * @param maxTuple If maxTuple>1 then tuples of 1..maxTuples will be return.
     *  Ie if the document is "t1 t2 t3 t4" and maxTuple=2, then the returned
     *  map will contain values for each fo the following: t1, t2, t1_t2, t2_t3 
     *  If maxTuple <1 then maxTuple=1.
     * @return a map that binds each token with the count of ocurrencies within
     *  the document
     * @see {@link TupleTokenizer}{@link #parse(String, int)}
     * The map should be '<String,int>'. But int can't be inserted to a Map, and
     * Integer is unmodifiable. So this awful hack uses an int[] to be able to
     * add an int and change it's value easily during the calculation.
     */         
    public static Map<String,int[]> parse(String doc, int maxTuple){

        // TODO: Use Integer instead int[].
        Map<String,int[]> tokenCount = new HashMap<String, int[]>();

        // TODO: Decouple from lucene, allow the analyzer to be configurable.
        // TODO: Verifiy that it is necessary to create a new analyzer instance each time.
        Analyzer analyzer = new StandardAnalyzer();
        Reader docReader = new StringReader(doc);
        TokenStream tokenStream = analyzer.tokenStream(null, docReader);

        Token token = null;
        
        try {
            if (1 < maxTuple ){
                tokenStream= new TupleTokenizer(tokenStream, maxTuple);
            }
            while ((token = tokenStream.next()) != null) {
                String term = TokenUtil.termText(token);
                int[] count = tokenCount.get(term);
                if (count == null) {
                    count = new int[] { 0 };
                    tokenCount.put(term, count);
                } else {
                    count[0]++;
                }
            }
        } catch (IOException e) {
            System.err.println("parse: couldn't parse document " +e);
        } finally {
            try {
                tokenStream.close();
            } catch (IOException e) {
                System.err.println("close: " +e);
            }
        }

        return tokenCount;
    }

}

