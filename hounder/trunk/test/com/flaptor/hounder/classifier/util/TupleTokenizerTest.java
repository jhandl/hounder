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

import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * @author Flaptor Development Team
 */
public class TupleTokenizerTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testNext() throws IOException {
        TupleTokenizer tt= new TupleTokenizer(new MockTokenStrem(),3);
        assertEquals("t1", tt.next().termText());
        assertEquals("t2", tt.next().termText());
        assertEquals("t3", tt.next().termText());
        assertEquals("t4", tt.next().termText());
        assertEquals("t5", tt.next().termText());
        assertEquals("t6", tt.next().termText());
        assertEquals("t7", tt.next().termText());
        assertEquals("t8", tt.next().termText());
        assertEquals("t9", tt.next().termText());
        assertEquals("t10", tt.next().termText());

        assertEquals("t1_t2", tt.next().termText());
        assertEquals("t2_t3", tt.next().termText());
        assertEquals("t3_t4", tt.next().termText());
        assertEquals("t4_t5", tt.next().termText());
        assertEquals("t5_t6", tt.next().termText());
        assertEquals("t6_t7", tt.next().termText());
        assertEquals("t7_t8", tt.next().termText());
        assertEquals("t8_t9", tt.next().termText());
        assertEquals("t9_t10", tt.next().termText());
        
        assertEquals("t1_t2_t3", tt.next().termText());
        assertEquals("t2_t3_t4", tt.next().termText());
        assertEquals("t3_t4_t5", tt.next().termText());
        assertEquals("t4_t5_t6", tt.next().termText());
        assertEquals("t5_t6_t7", tt.next().termText());
        assertEquals("t6_t7_t8", tt.next().termText());
        assertEquals("t7_t8_t9", tt.next().termText());
        assertEquals("t8_t9_t10", tt.next().termText());
        
        assertNull(tt.next());
    }

    private class MockTokenStrem extends TokenStream{
        private Vector<String> v= new Vector<String>();
        private int index=0;

        public MockTokenStrem(){
            v.add("t1");
            v.add("t2");
            v.add("t3");
            v.add("t4");
            v.add("t5");
            v.add("t6");
            v.add("t7");
            v.add("t8");
            v.add("t9");
            v.add("t10");
        }

        @Override
        public Token next() throws IOException {
            if (index == v.size()) return null;
            return new Token(v.get(index++),index*2,index*2+2);
        }        
    }
}
