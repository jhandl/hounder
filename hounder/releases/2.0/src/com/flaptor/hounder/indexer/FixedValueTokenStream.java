package com.flaptor.hounder.indexer;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;

import com.flaptor.hounder.searcher.payload.LongPayload;



/**
 * A TokenStream that returns only one token, with a fixed value, and a long as payload.
 * @author Flaptor Development Team
 */
public class FixedValueTokenStream extends TokenStream {

    private boolean ended;
    private final long payload;
    private final char[] value;

    /**
     * @param value
     *          The string value for the only Token this TokenStream will generate.
     * @param payload
     *          The payload for the only Token this TokenStream will generate.
     */
    public FixedValueTokenStream(String value, long payload){
        this.ended = false;
        this.value = value.toCharArray();
        this.payload = payload;
    }


    /**
     * Use FixedValueTokenStream.next(Token) instead.
     * @throws UnsupportedOperationException always.
    **/
    @Override
    @Deprecated
    public Token next() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return 
     *      A Token (with the value specified at the constructor) and the payload
     *      specified at the constructor, iff it is the first time this method is
     *      called. Every further call to this method will return null.
     * 
     */
    @Override
    public Token next(Token token) {
        if (this.ended) return null;
        this.ended = true;
        token.reinit(value, 0, value.length, 0, value.length -1);
        LongPayload lPayload = new LongPayload(payload);
        token.setPayload(lPayload.asPayload());
        return token;
    }
}
