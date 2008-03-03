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
package com.flaptor.search4j.searcher;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.search.highlight.Fragmenter;

/**
 * @author Flaptor Development Team
 */
public class PhraseMatchingFragmenter implements Fragmenter {
    private static Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());

    private final int maxFragmentSize;
    private String originalText = null;
    private int lastOffset = 0;
    // Usually ". ? !"  etc. Marks that a new fragment begins
    private String[] fragmentBoundary={ ".", "?", "!", "\n" };

    
    public PhraseMatchingFragmenter(int maxFragmentSize, String[] fragmentBoundary) {
        this.maxFragmentSize = maxFragmentSize;
        this.fragmentBoundary= fragmentBoundary;
    }
    
    public PhraseMatchingFragmenter(int maxFragmentSize) {
        this.maxFragmentSize = maxFragmentSize;
    }

    
    public void start(String originalText) {
        this.originalText = originalText;
        logger.debug("text set to: " + originalText);
    }
    
    public boolean isNewFragment(Token token) {
        boolean isNewFrag= lineBreaker(lastOffset, token.startOffset());
        logger.debug("token: " + token.termText());
        if (isNewFrag) logger.debug("BREAK!");
        lastOffset = token.endOffset();
        return isNewFrag;
    }
    
    /**
     * 
     * @param start inclusive
     * @param end exclusive
     * @return
     */
    private boolean lineBreaker(int start, int end) {
        logger.debug("start:" + start + ", end: " + end);
        if (start >= end || start < 0) return false;
        String s = originalText.substring(start, end);
        logger.debug("analizing substring: " + s);
        for (String fb: fragmentBoundary){         
            if (-1 < s.indexOf(fb)){
                return true;
            }
        }
        return false;
    }    
}
