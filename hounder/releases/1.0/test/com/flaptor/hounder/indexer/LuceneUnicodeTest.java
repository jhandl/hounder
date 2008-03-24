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
package com.flaptor.hounder.indexer;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;

import com.flaptor.util.Execute;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;


/**
 * Tests the compatibility of lucene with unicode. This tests doesn't have
 * anything to do with the code we write, but as lucene is the fundation for all
 * the rest is important to know that's handling the java subset of unicode more
 * or less correctly.
 * @author Flaptor Development Team
 */
public class LuceneUnicodeTest extends TestCase {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    private IndexWriter writer = null;

    private File dir;

    @Override
    public void setUp() {
        dir = com.flaptor.util.FileUtil.createTempDir("test", ".tmp"); 
        try {
            writer = new IndexWriter(dir, new StandardAnalyzer(), true);
        } catch (Exception e) {
            assertTrue(false);
        }
    }

    @Override
    public void tearDown() {
        try {
            writer.close();
        } catch (IOException e) {
            fail("IOException caught. This is not normal... " + e);
        }
        writer = null;
        dir.delete();
    }

    /**
     * The only Test case.
     * Generates a bunch of interesting test strings, indexes them and then
     * gets them from the index to compare with the original.
     */
    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testStoredContent() {
        try {
            String testString = getTestString();
            logger.debug("Using test string: " + testString);
            Document doc = new Document();
            doc.add(new Field("field1", testString, Field.Store.YES,
                    Field.Index.UN_TOKENIZED));
            writer.addDocument(doc);
            writer.optimize();
            writer.close();
            IndexReader reader = IndexReader.open(dir);
            Document doc2 = reader.document(0);
            String recoveredString = doc2.get("field1");
            logger.debug("Recovered String: " + recoveredString);
            assertTrue("Strings do not match", testString
                    .equals(recoveredString));
        } catch (Exception e) {
            logger.error("Exception caught:" + e);
            assertTrue("exception", false);
        }
    }

    @TestInfo(testType = TestInfo.TestType.UNIT)
    public void testIndexedContent() {
        try {
            String testString = "otorrinolaring\u00f3logo";
            logger.debug("Using test string: " + testString);
            Document doc = new Document();
            doc.add(new Field("field1", testString, Field.Store.YES,
                    Field.Index.TOKENIZED));
            writer.addDocument(doc);
            writer.optimize();
            writer.close();
            IndexReader reader = IndexReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader); 
            Document doc2 = searcher.search(new TermQuery(new Term("field1", testString))).doc(0);
            String recoveredString = doc2.get("field1");
            logger.debug("Recovered String: " + recoveredString);
            assertTrue("Strings do not match", testString
                    .equals(recoveredString));
        } catch (Exception e) {
            logger.error("Exception caught:" + e);
            assertTrue("exception", false);
        }
    }

    /**
     * Generates the interesting string to test.
     * @return A string with almast all the unicode characters.
     */
    private String getTestString() {
        StringBuffer buf = new StringBuffer();
        for (char i = 0; i < 0xffff; i++) {
            buf.append(i);
        }
        return buf.toString();
    }
}
