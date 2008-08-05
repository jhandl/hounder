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
package com.flaptor.hounder.searcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.antlr.stringtemplate.StringTemplate;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.After;
import org.junit.Before;

import com.flaptor.hounder.indexer.Indexer;
import com.flaptor.hounder.searcher.group.NoGroup;
import com.flaptor.hounder.searcher.query.LazyParsedQuery;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.StringUtil;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * 
 * @author Rafael Horowitz
 *
 */
public class SnippetSearcherTest extends TestCase{

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    private static final String FRAG_SEP="...";
    private static final String PHRASE_BOUND="[.!\\-?]";

    private StringTemplate addTemplate = new StringTemplate("<documentAdd><documentId>$documentId$</documentId><field name=\"text\" stored=\"true\" indexed=\"true\" tokenized=\"true\">$text$</field><field name=\"group\" stored=\"true\" indexed=\"true\" tokenized=\"true\">$group$</field></documentAdd>");
    private int startPort = 30000;
    private ISearcher snippetSearcher;


    private Indexer indexer; 
    private List<String> tmpDirs;
    
    private Config searcherConfig= null;
    
    public SnippetSearcherTest(){
        super();
    }
    
    @Before
    public void setUp() throws Exception {
        tmpDirs = new ArrayList<String>(2);
        try {
            setUpSearcher();
            Execute.sleep(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() throws Exception {
        // request stops
        indexer.requestStop();
        // wait for them
        while (!indexer.isStopped()) {
            Execute.sleep(1000);
        }
        // delete temp dirs
        for (String tmpDir: tmpDirs) {
            FileUtil.deleteDir(tmpDir);
        }
    }
    
    private void setUpSearcher() throws Exception {    
        searcherConfig = Config.getConfig("searcher.properties");
        String tmpDir = com.flaptor.util.FileUtil.createTempDir("junit", ".tmp").getAbsolutePath();
        String searcherTmpDir = com.flaptor.util.FileUtil.createTempDir("junit", ".tmp").getAbsolutePath();

        Config indexerConfig = Config.getConfig("indexer.properties");
        Config.getConfig("common.properties").set("baseDir", tmpDir);
        Config.getConfig("common.properties").set("port.base", String.valueOf(startPort));
        // Configuration
        // indexer:
        indexerConfig.set("IndexManager.updateInterval", "2000");
        indexerConfig.set("IndexLibrary.remoteIndexUpdaters","127.0.0.1:"+String.valueOf(startPort));
        indexerConfig.set("Indexer.modules", "com.flaptor.hounder.indexer.Writer");
        indexerConfig.set("Indexer.fields", "text");
        indexerConfig.set("docIdName", "docId");
        indexerConfig.set("Indexer.maxQueueSize", "100");
        indexerConfig.set("IndexLibrary.rsyncAccessString","");
        indexerConfig.set("Writer.fields", "");
        indexerConfig.set("clustering.enable", "false");
        indexerConfig.set("Indexer.modules","com.flaptor.hounder.indexer.LoggerModule,com.flaptor.hounder.indexer.HtmlParser,com.flaptor.hounder.indexer.Writer");
        // searcher:
        searcherConfig.set("QueryParser.searchFields", "text");
        searcherConfig.set("QueryParser.searchFieldWeights", "1.0f");
        searcherConfig.set("QueryParser.nonTokenizedFields", "");
        searcherConfig.set("Searcher.workingDirPath", searcherTmpDir);
        searcherConfig.set("ReloadableIndexSearcher.minTimeBetweenIndexes", "1000");
        searcherConfig.set("ReloadableIndexSearcher.sleepTime", "1000");
        searcherConfig.set("compositeSearcher.useSnippetSearcher", "false");// we dont want it to be constructed using the default settings!!       
        searcherConfig.set("clustering.enable", "false");

        // do not delete this "snippetSearcher=new ..." line, or you will get
        // some error messages saying that the indexer can connect to the searcher
        int[] lenA={5};
        String[] fieldA={"text"};
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND , false);

        indexer = new Indexer();
        // ------ document-1 ---------------
        addTemplate.setAttribute("text", "This is a simple phrase");
        addTemplate.setAttribute("documentId", "doc-1");
        indexer.index(addTemplate.toString());
        addTemplate.reset();
        // ------ document-2 ---------------
        addTemplate.setAttribute("text", "This test will check! test segments and their separation. using different chars to separe the frags- chars and chars and chars");
        addTemplate.setAttribute("documentId", "doc-2");
        indexer.index(addTemplate.toString());
        addTemplate.reset();
        // ------ document-3 ---------------
        addTemplate.setAttribute("text", "has more. more more comes first. have has twice has");
        addTemplate.setAttribute("documentId", "doc-3");
        indexer.index(addTemplate.toString());
        addTemplate.reset();
        // ------ document-4 ---------------
        addTemplate.setAttribute("text", "no no no no. foo foo foo foo. bla bla surrounding bla bla. bar bar bar bar. fiz fiz fiz fiz");
        addTemplate.setAttribute("documentId", "doc-4");
        indexer.index(addTemplate.toString());
        addTemplate.reset();
        // ------ document-5 ---------------
        addTemplate.setAttribute("text", "same highlight same same. other5 highlight other5 other5");
        addTemplate.setAttribute("documentId", "doc-5");
        indexer.index(addTemplate.toString());
        addTemplate.reset();
        // ------ document-6 ---------------
        addTemplate.setAttribute("text", "same highlight same same. other6 highlight other6 other6");
        addTemplate.setAttribute("documentId", "doc-6");
        indexer.index(addTemplate.toString());
        addTemplate.reset();
        // ------ document-7 ---------------
        addTemplate.setAttribute("text", "two words");
        addTemplate.setAttribute("documentId", "doc-7");
        indexer.index(addTemplate.toString());
        addTemplate.reset();
        // ------ document-8 ---------------
        addTemplate.setAttribute("text", "last dot.");
        addTemplate.setAttribute("documentId", "doc-8");
        indexer.index(addTemplate.toString());
        addTemplate.reset();

        addTemplate.setAttribute("text", "word1 word2. word3 word4.  word5 word51 hlght word6. word7 word8. word9 word10. word11 hlght hlght word12. word13 word14. word15 hlght hlght hlght word16. word17.");
        addTemplate.setAttribute("documentId", "doc-9");
        indexer.index(addTemplate.toString());
        addTemplate.reset();
            
        tmpDirs.add(tmpDir);
        tmpDirs.add(searcherTmpDir);
    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testSurrounding() throws SearcherException {
        int[] lenA={40}; // >= "bla bla surrounding bla bla.".length()
        String[] fieldA={"text"};
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);

        GroupedSearchResults results;        
        results = snippetSearcher.search(new LazyParsedQuery("surrounding"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        org.apache.lucene.document.Document doc= results.getGroup(0).last().get(0);
        String content= StringUtil.nullToEmpty(doc.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "bla bla <B>surrounding</B> bla bla. bar bar bar bar.", content);
        

        lenA[0]=54; // lenA + "bar bar bar bar.".length()        
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);                
        results = snippetSearcher.search(new LazyParsedQuery("surrounding"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc= results.getGroup(0).last().get(0);
        content= StringUtil.nullToEmpty(doc.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "foo foo foo foo. bla bla <B>surrounding</B> bla bla. bar bar bar bar.", content);            

        lenA[0]=80;         
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);                
        results = snippetSearcher.search(new LazyParsedQuery("surrounding"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc= results.getGroup(0).last().get(0);
        content= StringUtil.nullToEmpty(doc.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "foo foo foo foo. bla bla <B>surrounding</B> bla bla. bar bar bar bar. fiz fiz fiz fiz", content);

        lenA[0]=95;         
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);                
        results = snippetSearcher.search(new LazyParsedQuery("surrounding"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc= results.getGroup(0).last().get(0);
        content= StringUtil.nullToEmpty(doc.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "no no no no. foo foo foo foo. bla bla <B>surrounding</B> bla bla. bar bar bar bar. fiz fiz fiz fiz", content);            

    }
    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testManyPhrases() throws SearcherException {
        int[] lenA={0};  
        String[] fieldA={"text"};
        GroupedSearchResults results;
        org.apache.lucene.document.Document doc9;
        String content9;

        lenA[0]=10;
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);
        results = snippetSearcher.search(new LazyParsedQuery("hlght"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc9= results.getGroup(0).last().get(0);
        content9= StringUtil.nullToEmpty(doc9.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "word15 <B>hlght</B> <B>hlght</B> <B>hlght</B> word16.", content9);

        lenA[0]=60;
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);
        results = snippetSearcher.search(new LazyParsedQuery("hlght"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc9= results.getGroup(0).last().get(0);
        content9= StringUtil.nullToEmpty(doc9.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "word11 <B>hlght</B> <B>hlght</B> word12. ... word15 <B>hlght</B> <B>hlght</B> <B>hlght</B> word16.", content9);

        lenA[0]=100;
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);
        results = snippetSearcher.search(new LazyParsedQuery("hlght"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc9= results.getGroup(0).last().get(0);
        content9= StringUtil.nullToEmpty(doc9.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "word5 word51 <B>hlght</B> word6. ... word11 <B>hlght</B> <B>hlght</B> word12. ... word15 <B>hlght</B> <B>hlght</B> <B>hlght</B> word16.", content9);

        // The tests below are algorithm dependent. Current algo starts filling with near sentences (to give some context
        // from left to right, adding 1 context sentence to the right of any existing hilighted fragment.
        // Then it do the same adding a context sentence to the left but starting from right to left.
        lenA[0]=135;
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);
        results = snippetSearcher.search(new LazyParsedQuery("hlght"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc9= results.getGroup(0).last().get(0);
        content9= StringUtil.nullToEmpty(doc9.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "word5 word51 <B>hlght</B> word6. word7 word8. ... word11 <B>hlght</B> <B>hlght</B> word12. ... word15 <B>hlght</B> <B>hlght</B> <B>hlght</B> word16.", content9);

        lenA[0]=145;
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);
        results = snippetSearcher.search(new LazyParsedQuery("hlght"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc9= results.getGroup(0).last().get(0);
        content9= StringUtil.nullToEmpty(doc9.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "word5 word51 <B>hlght</B> word6. word7 word8. ... word11 <B>hlght</B> <B>hlght</B> word12. word13 word14. word15 <B>hlght</B> <B>hlght</B> <B>hlght</B> word16.", content9);
        
        lenA[0]=155;
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);
        results = snippetSearcher.search(new LazyParsedQuery("hlght"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc9= results.getGroup(0).last().get(0);
        content9= StringUtil.nullToEmpty(doc9.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "word5 word51 <B>hlght</B> word6. word7 word8. ... word11 <B>hlght</B> <B>hlght</B> word12. word13 word14. word15 <B>hlght</B> <B>hlght</B> <B>hlght</B> word16. word17.", content9);
        
        lenA[0]=161;
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);
        results = snippetSearcher.search(new LazyParsedQuery("hlght"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc9= results.getGroup(0).last().get(0);
        content9= StringUtil.nullToEmpty(doc9.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "word5 word51 <B>hlght</B> word6. word7 word8. word9 word10. word11 <B>hlght</B> <B>hlght</B> word12. word13 word14. word15 <B>hlght</B> <B>hlght</B> <B>hlght</B> word16. word17.", content9);

        lenA[0]=175;
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);
        results = snippetSearcher.search(new LazyParsedQuery("hlght"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc9= results.getGroup(0).last().get(0);
        content9= StringUtil.nullToEmpty(doc9.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "word3 word4. word5 word51 <B>hlght</B> word6. word7 word8. word9 word10. word11 <B>hlght</B> <B>hlght</B> word12. word13 word14. word15 <B>hlght</B> <B>hlght</B> <B>hlght</B> word16. word17.", content9);

        lenA[0]=185;
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);
        results = snippetSearcher.search(new LazyParsedQuery("hlght"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc9= results.getGroup(0).last().get(0);
        content9= StringUtil.nullToEmpty(doc9.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "word1 word2. word3 word4. word5 word51 <B>hlght</B> word6. word7 word8. word9 word10. word11 <B>hlght</B> <B>hlght</B> word12. word13 word14. word15 <B>hlght</B> <B>hlght</B> <B>hlght</B> word16. word17.", content9);

        lenA[0]=500;
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);
        results = snippetSearcher.search(new LazyParsedQuery("hlght"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc9= results.getGroup(0).last().get(0);
        content9= StringUtil.nullToEmpty(doc9.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "word1 word2. word3 word4. word5 word51 <B>hlght</B> word6. word7 word8. word9 word10. word11 <B>hlght</B> <B>hlght</B> word12. word13 word14. word15 <B>hlght</B> <B>hlght</B> <B>hlght</B> word16. word17.", content9);


    }

    
    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testLastChar() throws SearcherException {
        int[] lenA={10}; // so we get 1 only snippet 
        String[] fieldA={"text"};
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);

        GroupedSearchResults results;        
        results = snippetSearcher.search(new LazyParsedQuery("last"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        org.apache.lucene.document.Document doc8= results.getGroup(0).last().get(0);
        String content8= StringUtil.nullToEmpty(doc8.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "<B>last</B> dot.", content8);
        
        results = snippetSearcher.search(new LazyParsedQuery("dot"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc8= results.getGroup(0).last().get(0);
        content8= StringUtil.nullToEmpty(doc8.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "last <B>dot</B>.", content8);         

    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testShortFragments() throws SearcherException {
        int[] lenA={10}; // so we get 1 only snippet 
        String[] fieldA={"text"};
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);

        GroupedSearchResults results;        
        results = snippetSearcher.search(new LazyParsedQuery("two"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        org.apache.lucene.document.Document doc7= results.getGroup(0).last().get(0);
        String content7= StringUtil.nullToEmpty(doc7.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "<B>two</B> words", content7);
        
        results = snippetSearcher.search(new LazyParsedQuery("words"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc7= results.getGroup(0).last().get(0);
        content7= StringUtil.nullToEmpty(doc7.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "two <B>words</B>", content7);                

    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testSameSnippet() throws SearcherException {
        int[] lenA={10}; // so we get 1 only snippet 
        String[] fieldA={"text"};
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);

        GroupedSearchResults results;        
        results = snippetSearcher.search(new LazyParsedQuery("highlight"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 2, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        org.apache.lucene.document.Document doc5= results.getGroup(0).last().get(0);
        org.apache.lucene.document.Document doc6= results.getGroup(1).last().get(0);
        String content5= StringUtil.nullToEmpty(doc5.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        String content6= StringUtil.nullToEmpty(doc6.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "same <B>highlight</B> same same.", content5);
        assertEquals("The returned snippet is not as expected", "other6 <B>highlight</B> other6 other6", content6);                
    }

    


    
    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testFragmentOrder() throws SearcherException {
        int[] lenA={22}; // <= "<B>more</B> <B>more</B> comes first.".length()
        String[] fieldA={"text"};
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenA, FRAG_SEP, PHRASE_BOUND, false);

        GroupedSearchResults results;        
        results = snippetSearcher.search(new LazyParsedQuery("more"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        org.apache.lucene.document.Document doc= results.getGroup(0).last().get(0);
        String content= StringUtil.nullToEmpty(doc.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "<B>more</B> <B>more</B> comes first.", content);
        
        // We should write the hilighted phrase, and then the surrounding ones
        int[] lenB={34}; // >= "have <B>has</B> twice <B>has</B>".length()+1        
        snippetSearcher= new SnippetSearcher(new CompositeSearcher(), fieldA, lenB, FRAG_SEP, PHRASE_BOUND, false);                
        results = snippetSearcher.search(new LazyParsedQuery("has"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc= results.getGroup(0).last().get(0);
        content= StringUtil.nullToEmpty(doc.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "more more comes first. " + "have <B>has</B> twice <B>has</B>", content);            

    }
        
    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testFragmentSeparation() throws SearcherException {
        GroupedSearchResults results;        
        results = snippetSearcher.search(new LazyParsedQuery("separation"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        org.apache.lucene.document.Document doc= results.getGroup(0).last().get(0);
        String content= StringUtil.nullToEmpty(doc.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "test segments and their <B>separation</B>.", content);            
        
        results = snippetSearcher.search(new LazyParsedQuery("test"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc= results.getGroup(0).last().get(0);
        content= StringUtil.nullToEmpty(doc.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "This <B>test</B> will check!", content);            

        results = snippetSearcher.search(new LazyParsedQuery("different"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc= results.getGroup(0).last().get(0);
        content= StringUtil.nullToEmpty(doc.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "using <B>different</B> chars to separe the frags-", content);
        
        results = snippetSearcher.search(new LazyParsedQuery("chars"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        doc= results.getGroup(0).last().get(0);
        content= StringUtil.nullToEmpty(doc.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();        
        assertEquals("The returned snippet is not as expected", "<B>chars</B> and <B>chars</B> and <B>chars</B>", content);        
    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testSimplePhrase() throws SearcherException {        
        GroupedSearchResults results;
        results = snippetSearcher.search(new LazyParsedQuery("phrase"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, results.groups());
        assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
        org.apache.lucene.document.Document doc= results.getGroup(0).last().get(0);
        String content= StringUtil.nullToEmpty(doc.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        assertEquals("The returned snippet is not as expected", "This is a simple <B>phrase</B>", content);            
    }

    class MultiThreadTestHlp extends Thread{
        public GroupedSearchResults results;
        public void run(){
            try {
                results = snippetSearcher.search(new LazyParsedQuery("phrase"), 0, 10, new NoGroup(), 1, null, null);
            } catch (SearcherException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }                    
    }

    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testMultiThread() throws SearcherException {
        MultiThreadTestHlp[] thrArr= new MultiThreadTestHlp[50];
        for (int i=0; i< thrArr.length; i++){
            thrArr[i]= new MultiThreadTestHlp();
        }
        for (int i=0; i< thrArr.length; i++){
            thrArr[i].start();
        }
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        for (int i=0; i< thrArr.length; i++){
            GroupedSearchResults results = thrArr[i].results;
            assertEquals("We get a bad number of groups", 1, results.groups());
            assertEquals("We get a bad number of results within the group", 1, results.getGroup(0).last().size());
            org.apache.lucene.document.Document doc= results.getGroup(0).last().get(0);
            String content= StringUtil.nullToEmpty(doc.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
            assertEquals("The returned snippet is not as expected", "This is a simple <B>phrase</B>", content);
        }
    }


    /**
     * This test a large file where the hilighted words are far from the begining
     * @throws SearcherException
     */
    @TestInfo(testType = TestInfo.TestType.INTEGRATION)
    public void testMercuryLifeDocument() throws SearcherException {
    
        // Index problematic document
        File docFile = new File("test/com/flaptor/hounder/searcher/mercurylife.html");
        
        String buf = "";
        try {
            buf = FileUtil.readFile(docFile);        }
        catch (Exception e) {
            logger.error("testMercuryLifeDocument:", e);
            fail(e.toString());
        }
        Document doc = DocumentHelper.createDocument();
        Element body = doc.addElement("documentAdd").addElement("body");
        body.addText(buf);
        doc.getRootElement().addElement("documentId").addText("mercury");
        indexer.index(doc);
        Execute.sleep(5000);

        GroupedSearchResults gsr = snippetSearcher.search(new LazyParsedQuery("diabetic ice cream"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals("We get a bad number of groups", 1, gsr.groups());
        assertEquals("We get a bad number of results within the group", 1, gsr.getGroup(0).last().size());
        
        org.apache.lucene.document.Document luceneDoc = gsr.getGroup(0).last().get(0);
        String content= StringUtil.nullToEmpty(luceneDoc.get(SnippetSearcher.SNIPPET_FIELDNAME_PREFIX + "text")).trim();
        //System.out.println("content: "+ content);
        assertTrue("Did not get a content of the desired size", content.length() > 0);
        assertTrue("Results has nothing highlighted: " + content, content.contains("<B>"));
    }
}
