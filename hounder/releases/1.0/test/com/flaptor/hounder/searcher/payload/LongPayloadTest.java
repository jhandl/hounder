package com.flaptor.hounder.searcher.payload;

import org.apache.lucene.index.Payload;

import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;


/**
 *  Test encoding/decoding Long as byte[] payloads 
 * @author Flaptor Development Team
 */
public class LongPayloadTest extends TestCase {

    @TestInfo (testType = TestInfo.TestType.UNIT)
    public void testEncodeDecode() {
    
        long[] values = new long[]{Long.MAX_VALUE,Long.MIN_VALUE,0,1,-1,10,-10,128,-128,127,-127,255,-255,256,-256};

        for (Long value: values) {
        
            LongPayload lPayload = new LongPayload(value);
            Payload payload = lPayload.asPayload();

            LongPayload bPayload = new LongPayload(payload.toByteArray());

            assertEquals(lPayload,bPayload);
            assertEquals(value,bPayload.asLong());
        }
    }

}
