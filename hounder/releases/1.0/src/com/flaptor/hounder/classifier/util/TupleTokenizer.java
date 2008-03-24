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
import java.util.Vector;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;

import com.flaptor.hounder.util.TokenUtil;

/**
 * @author rafa
 *
 */
public class TupleTokenizer extends TokenStream {

    private final Vector<Token> tokens= new Vector<Token>();
    private int index=0;
    private int increment=1;
    private final int MAX_INCREMENT;

    /**
     * @throws IOException 
     * @param ts the TokenStream to wrap
     * @param maxTuple If maxTuple>1 then tuples of 1..maxTuples will be return.
     *  Ie if the document is "t1 t2 t3 t4" and maxTuple=2, then the return wil
     *  be "t1 t2 t1_t2 t2_t3"
     * 
     */
    public TupleTokenizer(TokenStream ts, int maxTuples ) throws IOException {

        MAX_INCREMENT= maxTuples;
        Token tk;
        while ((tk = ts.next()) != null) {
            tokens.add(tk);
        }        
    }

    private Token mergeTokens(Token t1, Token t2){
        if (null == t1) return t2;
        return new Token(TokenUtil.termText(t1)+"_"+TokenUtil.termText(t2), 
                t1.startOffset(), t2.endOffset());
    }
    /* (non-Javadoc)
     * @see org.apache.lucene.analysis.TokenStream#next()
     */
    @Override
    public Token next() throws IOException {
        if (0==tokens.size()){
            return null;
        }
        Token res= null;
        if (1 == increment){
            res= tokens.get(index);
            index++;
            if (index == tokens.size()){
                index=0;
                increment ++;
            }
            return res;
        }
        if (increment > MAX_INCREMENT){
            return null;
        }

        for (int i= index; i< increment+index && i<tokens.size(); i++){
            res= mergeTokens(res,tokens.get(i));            
        }
        if (index + increment == tokens.size()){
            index=0;
            increment ++;
        } else {
            index++;
        }
        return res;
    }
    
    

}
