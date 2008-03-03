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
package com.flaptor.search4j.searcher.payload;


import org.apache.lucene.index.Payload;

/**
 * Encodes and decodes longs as/from payloads.
 * @author Flaptor Development Team
 */
public class LongPayload {

    private Long lPayload;
    private byte[] bPayload;

    public LongPayload(Long value) {
        lPayload = value;
        bPayload = getBytes(value);
    }

    public LongPayload(byte[] value) {
        if (value.length < 8) throw new IllegalArgumentException("byte[] has to be 8 bytes long. Argument is " + value.length + " bytes long.");
        bPayload = value;
        lPayload = getLong(value);
    }



    public Payload asPayload() {
        return new Payload(bPayload);
    }
    public Long asLong() {
        return lPayload;
    }


    public boolean equals(Object other) {
        if (null == other) return false;
        if (!other.getClass().equals(this.getClass())) return false;

        LongPayload otherPayload = (LongPayload)other;
        if (!this.lPayload.equals(otherPayload.lPayload)) return false;
        
        // we do not check the arrays, as the long is the representation of the array
        return true;
        
    }



    /** HELPER METHODS */
    private static long getLong(byte[] data) {
        byte[] data1 = new byte[4];
        byte[] data2 = new byte[4];
        System.arraycopy(data, 0, data1, 0, 4);
        System.arraycopy(data, 4, data2, 0, 4);
        return ((long)(getInt(data1)) << 32) + (getInt(data2) & 0xFFFFFFFFL);
    }
    private static byte[] getBytes(long value) {
        byte[] temp = new byte[8];
        temp[0] = (byte)((value >>> 56) & 0xFF);
        temp[1] = (byte)((value >>> 48) & 0xFF);
        temp[2] = (byte)((value >>> 40) & 0xFF);
        temp[3] = (byte)((value >>> 32) & 0xFF);
        temp[4] = (byte)((value >>> 24) & 0xFF);
        temp[5] = (byte)((value >>> 16) & 0xFF);
        temp[6] = (byte)((value >>>  8) & 0xFF);
        temp[7] = (byte)((value >>>  0) & 0xFF);
        return temp;
    }
    private static int getInt(byte[] data) {
        int w, x, y, z;
        w = data[0];
        x = data[1];
        y = data[2];
        z = data[3];
        if (w < 0) w+=256;
        if (x < 0) x+=256;
        if (y < 0) y+=256;
        if (z < 0) z+=256;
        return ((w << 24) + (x <<16) + (y <<8) + (z <<0));
    }

}
