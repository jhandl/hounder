package com.flaptor.hounder;

import java.io.Reader;

import org.apache.lucene.analysis.ISOLatin1AccentFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class LatinStandardAnalyzer extends StandardAnalyzer {

    public LatinStandardAnalyzer(String[] stopWords) {
        super(stopWords);
    }

    /** Constructs a {@link StandardTokenizer} filtered by a {@link
      StandardFilter}, a {@link LowerCaseFilter} and a {@link StopFilter}. */
    @Override
        public TokenStream tokenStream(String fieldName, Reader reader) {
            TokenStream result = super.tokenStream(fieldName, reader);
            result = new ISOLatin1AccentFilter(result);
            return result;
        }
}
