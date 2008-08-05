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
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.lucene.document.Document;
import org.apache.lucene.store.FSDirectory;

import com.flaptor.hounder.searcher.CacheSearcher;
import com.flaptor.hounder.searcher.CompositeSearcher;
import com.flaptor.hounder.searcher.GroupedSearchResults;
import com.flaptor.hounder.searcher.ISearcher;
import com.flaptor.hounder.searcher.NoIndexActiveException;
import com.flaptor.hounder.searcher.QueryParams;
import com.flaptor.hounder.searcher.Searcher;
import com.flaptor.hounder.searcher.SearcherException;
import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.filter.ValueFilter;
import com.flaptor.hounder.searcher.group.NoGroup;
import com.flaptor.hounder.searcher.group.StoredFieldGroup;
import com.flaptor.hounder.searcher.query.AndQuery;
import com.flaptor.hounder.searcher.query.LazyParsedQuery;
import com.flaptor.hounder.searcher.query.PayloadQuery;
import com.flaptor.hounder.searcher.query.MatchAllQuery;
import com.flaptor.hounder.searcher.query.TermQuery;
import com.flaptor.hounder.searcher.query.WordQuerySuggestor;
import com.flaptor.hounder.searcher.sort.FieldSort;
import com.flaptor.hounder.searcher.spell.DidYouMeanIndexer;
import com.flaptor.util.Cache;
import com.flaptor.util.CommandUtil;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.LRUCache;
import com.flaptor.util.Pair;
import com.flaptor.util.TestCase;
import com.flaptor.util.TestInfo;

/**
 * Tests the complete process from indexing to searching.
 * This test case is somhow high-level: it creates an Indexer and
 * a Searcher, indexes something, allows the normal rsync mechanism
 * to do it's magic and then searches to find the document.
 * @author spike
 */
public class IndexerSearcherTest extends TestCase {
	private static Logger logger = null;


	private String addA = "<documentAdd><documentId>a</documentId><field name=\"content\" stored=\"true\" indexed=\"true\" tokenized=\"true\">contentA</field></documentAdd>";
	private String deleteA = "<documentDelete><documentId>a</documentId></documentDelete>";

	private String addB = "<documentAdd><documentId>b</documentId><field name=\"content\" stored=\"true\" indexed=\"true\" tokenized=\"true\">contentB</field></documentAdd>";
	private String deleteB = "<documentDelete><documentId>b</documentId></documentDelete>";

	private String addC = "<documentAdd><documentId>c</documentId><field name=\"content\" stored=\"true\" indexed=\"true\" tokenized=\"true\">contentC</field></documentAdd>";
	private String deleteC = "<documentDelete><documentId>c</documentId></documentDelete>";

	private String addCb = "<documentAdd><documentId>cb</documentId><boost>10</boost><field name=\"content\" stored=\"true\" indexed=\"true\" tokenized=\"true\">contentC</field></documentAdd>";
	private String deleteCb = "<documentDelete><documentId>cb</documentId></documentDelete>";


	private String group1a = "<documentAdd><documentId>group1a</documentId><field name=\"content\" stored=\"true\" indexed=\"true\" tokenized=\"true\">a</field><field name=\"group\" stored=\"true\" indexed=\"true\" tokenized=\"true\">group1</field></documentAdd>";
	private String group1b = "<documentAdd><documentId>group1b</documentId><field name=\"content\" stored=\"true\" indexed=\"true\" tokenized=\"true\">b</field><field name=\"group\" stored=\"true\" indexed=\"true\" tokenized=\"true\">group1</field></documentAdd>";
	private String group2a = "<documentAdd><documentId>group2a</documentId><field name=\"content\" stored=\"true\" indexed=\"true\" tokenized=\"true\">a</field><field name=\"group\" stored=\"true\" indexed=\"true\" tokenized=\"true\">group2</field></documentAdd>";
	private String group2b = "<documentAdd><documentId>group2b</documentId><field name=\"content\" stored=\"true\" indexed=\"true\" tokenized=\"true\">b</field><field name=\"group\" stored=\"true\" indexed=\"true\" tokenized=\"true\">group2</field></documentAdd>";
	private String group3a = "<documentAdd><documentId>group3a</documentId><field name=\"content\" stored=\"true\" indexed=\"true\" tokenized=\"true\">a</field><field name=\"group\" stored=\"true\" indexed=\"true\" tokenized=\"true\">group3</field></documentAdd>";



    // boosted by 10, but no payload.
    private String addSmallPayload = "<documentAdd><documentId>small</documentId><boost>10</boost><field name=\"content\" stored=\"true\" indexed=\"true\" tokenized=\"true\">contentA</field><payload name=\"payload\">0</payload></documentAdd>";
    // not boosted, but payload will make it go up
    private String addBigPayload = "<documentAdd><documentId>big</documentId><field name=\"content\" stored=\"true\" indexed=\"true\" tokenized=\"true\">contentA</field><payload name=\"payload\">"+System.currentTimeMillis()+"</payload></documentAdd>";

	private Indexer indexer = null;

    private Searcher baseSearcher = null;
    private CompositeSearcher searcher = null;
    private ISearcher cacheSearcher = null;
    private Cache<QueryParams, GroupedSearchResults> cache = null; 
    private static final NoGroup noGroup = new NoGroup();
    

    private String tmpDir = null;


	@Override
	public void setUp() {
        String log4jConfigPath = com.flaptor.util.FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            System.err.println("log4j.properties not found on classpath!");
        }
	    logger = Logger.getLogger(Execute.whoAmI());
        filterOutput("Exception while trying to store index.properties");
		tmpDir = com.flaptor.util.FileUtil.createTempDir("junit", ".tmp").getAbsolutePath();
		// Configuration
        Config.getConfig("common.properties").set("baseDir", tmpDir);
        Config.getConfig("common.properties").set("port.base", "10000");
		// indexer:
		Config indexerConfig = Config.getConfig("indexer.properties");
		indexerConfig.set("IndexManager.updateInterval", "2000");
        indexerConfig.set("IndexLibrary.remoteIndexUpdaters","localhost:10000");
        indexerConfig.set("IndexLibrary.rsyncAccessString","");
		indexerConfig.set("Indexer.modules", "com.flaptor.hounder.indexer.Writer");
		indexerConfig.set("Indexer.fields", "content");
		indexerConfig.set("docIdName", "docId");
		indexerConfig.set("Indexer.maxQueueSize", "100");
		indexerConfig.set("Writer.fields","");
		indexerConfig.set("clustering.enable","false");

		// searcher:
		Config searcherConfig = Config.getConfig("searcher.properties");
		searcherConfig.set("QueryParser.searchFields", "content");
		searcherConfig.set("QueryParser.searchFieldWeights", "1.0f");
		searcherConfig.set("QueryParser.nonTokenizedFields", "");
		searcherConfig.set("ReloadableIndexSearcher.minTimeBetweenIndexes", "1000");
		searcherConfig.set("ReloadableIndexSearcher.sleepTime", "1000");
		searcherConfig.set("clustering.enable", "false");
        searcherConfig.set("SimilarityForwarder.scorers","");
		
		//TODO see why it crashes if snippets are set
		searcherConfig.set("Searcher.generateSnippets", "false");

        indexer = new Indexer();
        searcher = new CompositeSearcher();
        baseSearcher = (Searcher)searcher.getBaseSearcher();       
        
        cache = new LRUCache<QueryParams, GroupedSearchResults>(500);
        cacheSearcher = new CacheSearcher(baseSearcher, cache); //a cacheSearcher that uses the same baseSearcher
        baseSearcher.addCache(cache);
	}

    private void stopIndexer() {
        if (null != indexer) {
            indexer.requestStop();
            while (!indexer.isStopped()) {
                Execute.sleep(50);
            }
            indexer = null;
        }
    }

    private void stopSearcher() {
        if (null != baseSearcher) {
            baseSearcher.close();
            baseSearcher = null;
            searcher = null;
        }
    }


    @Override
        public void tearDown() {
            stopIndexer();
            stopSearcher();
            com.flaptor.util.FileUtil.deleteDir(tmpDir);
            unfilterOutput();
        }

    @TestInfo(testType = TestInfo.TestType.SYSTEM,
            requiresPort = {10000, 10001, 10010, 10011, 10012})
    public void xtestQuerySuggest() throws IOException, SearcherException{
        indexer.index(addC);
        indexer.index(addB);
        indexer.index(addA);
        Execute.sleep(5000);

        FSDirectory origDir = FSDirectory.getDirectory(tmpDir + File.separator + "indexer" + File.separator + "indexes" + File.separator + "index");
        FSDirectory spellDir = FSDirectory.getDirectory(tmpDir + File.separator + "spell");
        new DidYouMeanIndexer().createSpellIndex("content".intern(), origDir, spellDir);
        WordQuerySuggestor suggestor = new WordQuerySuggestor(spellDir.getFile());

        assertNotNull(suggestor.suggest(new LazyParsedQuery("content")));
        assertNotNull(suggestor.suggest(new LazyParsedQuery("contenta")));
        assertNotNull(suggestor.suggest(new LazyParsedQuery("contetb")));

        Config searcherConfig = Config.getConfig("searcher.properties");
        searcherConfig.set("compositeSearcher.useSpellCheckSuggestQuery", "true");
        searcherConfig.set("searcher.suggestQuerySearcher.dictionaryDir", spellDir.getFile().getAbsolutePath());
        searcher = new CompositeSearcher();
        
        GroupedSearchResults res = searcher.search(new LazyParsedQuery("contentb"), 0, 10, null, 20, null, null);
        assertEquals(1, res.totalGroupsEstimation());
        res = searcher.search(new LazyParsedQuery("content"), 0, 10, null, 20, null, null);
        assertEquals(0, res.totalGroupsEstimation());
        assertNotNull(res.getSuggestedQuery());
        res = searcher.search(res.getSuggestedQuery(), 0, 10, null, 20, null, null);
        assertEquals(1, res.totalGroupsEstimation());

        searcherConfig.set("compositeSearcher.useSuggestQuery", "false");
    }
    

	/**
	 * Test that the boost factor provided while indexing has relevance
	 * while searching.
	 */
    @TestInfo(testType = TestInfo.TestType.SYSTEM,
            requiresPort = {10000, 10001, 10010, 10011, 10012})
	public void testIndexingDocumentBoost()  throws SearcherException{
		indexer.index(addC);
		indexer.index(addCb);
		Execute.sleep(5000);
		GroupedSearchResults sr = searcher.search(new TermQuery("content", "contentc"), 0, 10,noGroup, 1, null, null);
		assertEquals(2, sr.totalGroupsEstimation());
		Document d1 = sr.getGroup(0).last().elementAt(0);
		assertEquals("cb", d1.get("docId"));
		Document d2 = sr.getGroup(1).last().elementAt(0);
		assertEquals("c", d2.get("docId"));
	}

	/**
	 * Tests that you index 1 document, got 1 document,
	 * index onother, get another...
	 */
    @TestInfo(testType = TestInfo.TestType.SYSTEM,
            requiresPort = {10000, 10001, 10010, 10011, 10012})
	public void testWhatYouAddIsWhatYouGet()  throws SearcherException{
		Execute.sleep(5000);
		GroupedSearchResults sr = searcher.search(new TermQuery("content", "contenta"), 0, 10, noGroup, 1, null, null);
		assertEquals(0, sr.totalGroupsEstimation());
		Execute.sleep(5000);
		sr = searcher.search(new TermQuery("content", "contentb"), 0, 10, noGroup, 1, null, null);
		assertEquals(0, sr.totalGroupsEstimation());

		indexer.index(addA);
		Execute.sleep(5000);
		sr = searcher.search(new TermQuery("content", "contenta"), 0, 10, noGroup,1, null, null);
		assertEquals(1, sr.totalGroupsEstimation());
		sr = searcher.search(new TermQuery("content", "contentb"), 0, 10, noGroup, 1, null, null);
		assertEquals(0, sr.totalGroupsEstimation());

		indexer.index(addB);
		Execute.sleep(5000);
		sr = searcher.search(new TermQuery("content", "contenta"), 0, 10, noGroup, 1, null, null);
		assertEquals(1, sr.totalGroupsEstimation());
		sr = searcher.search(new TermQuery("content", "contentb"), 0, 10, noGroup, 1, null, null);
		assertEquals(1, sr.totalGroupsEstimation());

	}

	/**
	 * Tests filters that do not mach any document, filter them all.
	 */
    @TestInfo(testType = TestInfo.TestType.SYSTEM,
            requiresPort = {10000, 10001, 10010, 10011, 10012})
	public void testNonMatchingFilters()  throws SearcherException{
		indexer.index(addA);
		indexer.index(addB);
		Execute.sleep(5000);

		AFilter filter = new ValueFilter("nonExistentField", "noValue");
		GroupedSearchResults sr = searcher.search(new MatchAllQuery(), 0, 10, noGroup, 1, filter, null);
		assertEquals(0, sr.totalGroupsEstimation());
	}


	/**
	 * Tests a value filter that matches some documents.
	 */
    @TestInfo(testType = TestInfo.TestType.SYSTEM,
            requiresPort = {10000, 10001, 10010, 10011, 10012})
	public void testValueFilter()  throws SearcherException{
		indexer.index(addA);
		indexer.index(addB);
		Execute.sleep(5000);
		GroupedSearchResults sr = searcher.search(new MatchAllQuery(), 0, 10, noGroup, 1, null, null);
		assertEquals(2, sr.totalGroupsEstimation());

		AFilter filter = new ValueFilter("content", "contenta");
		sr = searcher.search(new MatchAllQuery(), 0, 10, noGroup, 1, filter, null);
		assertEquals(1, sr.totalGroupsEstimation());
	}


    @TestInfo(testType = TestInfo.TestType.SYSTEM,
            requiresPort = {10000, 10001, 10010, 10011, 10012})
    public void testGroupedResults()  throws SearcherException{
        indexer.index(group1a);
        indexer.index(group1b);
        indexer.index(group2a);
        indexer.index(group2b);
        indexer.index(group3a);
		Execute.sleep(5000);
        GroupedSearchResults gsr = searcher.search(new MatchAllQuery(),0,3,new StoredFieldGroup("group"),2,null,new FieldSort(false,"group",FieldSort.OrderType.STRING));

        assertEquals(3,gsr.groups());
        assertEquals(2,gsr.getGroup(0).last().size());
        assertEquals(2,gsr.getGroup(1).last().size());
        assertEquals(1,gsr.getGroup(2).last().size());
    }

    @TestInfo(testType = TestInfo.TestType.SYSTEM,
            requiresPort = {10000, 10001, 10010, 10011, 10012})
    public void testDedupedResults()  throws SearcherException{
        indexer.index(group1a);
        indexer.index(group1b);
        indexer.index(group2a);
        indexer.index(group2b);
        indexer.index(group3a);
		Execute.sleep(5000);
        GroupedSearchResults sr = searcher.search(new MatchAllQuery(),0,3,new StoredFieldGroup("group"), 5,null,null);

        assertEquals(3,sr.groups());
    }

    @TestInfo(testType = TestInfo.TestType.SYSTEM,
            requiresPort = {10000, 10001, 10010, 10011, 10012})
    public void testCache()  throws SearcherException{
        indexer.index(group1a);
        indexer.index(group1b);
        indexer.index(group2a);
        indexer.index(group2b);
        indexer.index(group3a);
        Execute.sleep(5000);

        GroupedSearchResults newGsr, gsr;
        float newHitRatio, hitRatio;
        
        newGsr = cacheSearcher.search(new MatchAllQuery(),0,3,new StoredFieldGroup("group"),2,null,new FieldSort(false,"group",FieldSort.OrderType.STRING));
        newHitRatio = cache.getHitRatio();
        for (int i = 0; i < 20; ++i) {
            gsr = newGsr;
            hitRatio = newHitRatio;

            newGsr = cacheSearcher.search(new MatchAllQuery(),0,3,new StoredFieldGroup("group"),2,null,new FieldSort(false,"group",FieldSort.OrderType.STRING));
            newHitRatio = cache.getHitRatio();
            
            assertTrue(gsr == newGsr);
            assertTrue(hitRatio < newHitRatio);
        }
    }

    private File waitForIndex(String indexesDir, int indexNum) {
        String[] indexes = null;
        boolean arrived = false;
        while (!arrived) {
            Execute.sleep(100);
            indexes = (new File(indexesDir)).list();
            arrived = (indexes.length > indexNum);
        }
        Arrays.sort(indexes);
        File indexDir = new File(indexesDir, indexes[indexNum]);
        boolean finished = false;
        while (!finished) {
            Execute.sleep(100);
            finished = (new File(indexDir,"index.properties")).exists();
        }
        return indexDir;
    }

    @TestInfo(testType = TestInfo.TestType.SYSTEM,
            requiresPort = {10000, 10001, 10010, 10011, 10012})
    public void testSearcherStartup() throws Exception {
        File wd = new File(tmpDir);
        ArrayList<Pair<String,String>> indexes = new ArrayList<Pair<String,String>>(); // first=index_name last=copy_name

        // get the first valid index
        indexer.index(addA);
        File validIndexDir1 = waitForIndex(tmpDir+"/searcher/indexes",0);
        indexes.add(new Pair<String,String>(validIndexDir1.getAbsolutePath(),tmpDir+"/validIndex1"));
        CommandUtil.execute("cp -lr "+indexes.get(0).first()+" "+indexes.get(0).last(), wd); 

        // get an invalid index
        indexer.index(addA);
        File invalidIndexDir = waitForIndex(tmpDir+"/searcher/indexes",1);
        indexes.add(new Pair<String,String>(invalidIndexDir.getAbsolutePath(),tmpDir+"/invalidIndex"));
        CommandUtil.execute("cp -lr "+indexes.get(1).first()+" "+indexes.get(1).last(), wd); 

        // get the second valid index
        indexer.index(addA);
        File validIndexDir2 = waitForIndex(tmpDir+"/searcher/indexes",2);
        indexes.add(new Pair<String,String>(validIndexDir2.getAbsolutePath(),tmpDir+"/validIndex2"));
        CommandUtil.execute("cp -lr "+indexes.get(2).first()+" "+indexes.get(2).last(), wd); 

        // stop everything.
        stopIndexer();
        stopSearcher();

        new File(tmpDir+"/invalidIndex","index.properties").delete();

        // try all combinations of index presence and cleanup flag
        for (int b=0; b<16; b++) {
            trySearcherStartup(indexes, new boolean[] {(b&1)>0, (b&2)>0, (b&4)>0}, (b&8)>0, wd);
        }
    }

    private void trySearcherStartup (ArrayList<Pair<String,String>> indexes, boolean[] indexUse, boolean cleanup, File wd) throws Exception {
        File indexesDir = new File(tmpDir,"/searcher/indexes");
        FileUtil.deleteDir(indexesDir);
        indexesDir.mkdir();
        Config searcherConfig = Config.getConfig("searcher.properties");
        searcherConfig.set("IndexLibrary.cleanupOnStartup",cleanup?"true":"false");
        for (int i = 0; i<3; i++) { 
            if (indexUse[i]) {
                Execute.sleep(1010); // needed to ensure the order of the last modification time of the index dirs (has to be greater than 1 sec).
                CommandUtil.execute("cp -lr "+indexes.get(i).last()+" "+indexes.get(i).first(), wd); 
                (new File(indexes.get(i).first())).setLastModified(System.currentTimeMillis());
            }
        }
        boolean validIndexPresent = (indexUse[0] || indexUse[2]);
        boolean invalidIndexPresent = indexUse[1];


        if (invalidIndexPresent) filterOutput("Cannot find the properties inside the index");
        searcher = new CompositeSearcher();
        baseSearcher = (Searcher)searcher.getBaseSearcher();
        unfilterOutput();

        if (!validIndexPresent) filterOutput("No indexId active");
        try {
            GroupedSearchResults sr = searcher.search(new TermQuery("content", "contenta"), 0, 10, noGroup, 1, null, null);
            if (!validIndexPresent) fail("Search on an invalid index did not fail as expected");
        } catch (NoIndexActiveException e) {
            if (validIndexPresent) throw e;
        }
        unfilterOutput();

        for (int i = 0; i<3; i++) { 
            if (indexUse[i]) {
                File indexDir = new File(indexes.get(i).first());
                if (cleanup) {
                    if (i==0 && indexUse[2]) {
                        assertFalse("Oldest index has not been deleted when cleanUp=true",indexDir.exists());
                    }
                    if (i==2 && indexUse[0]) {
                        assertTrue("Newest index has been deleted when cleanUp=true",indexDir.exists());
                    }
                    if (i==1 && validIndexPresent) {
                        assertFalse("Invalid index has not been deleted when cleanUp=true and valid index present",indexDir.exists());
                    }
                    if (i==1 && !validIndexPresent) {
                        assertTrue("Invalid index has been deleted when cleanUp=true and no valid index present",indexDir.exists());
                    }
                } else {
                    assertTrue("Index has been deleted even when cleanUp=false",indexDir.exists());
                }
            }
        }

        stopSearcher();
    }



    @TestInfo(testType = TestInfo.TestType.SYSTEM,
            requiresPort = {10000, 10001, 10010, 10011, 10012})
    public void testPayloads() throws Exception{
        Config searcherConfig = Config.getConfig("searcher.properties");

        indexer.index(addSmallPayload);
        indexer.index(addBigPayload);

        Execute.sleep(5000);


        // perform query
        GroupedSearchResults gsr = searcher.search(new TermQuery("content", "contenta"), 0, 10, new NoGroup(), 1, null, null);
        assertEquals(gsr.groups(),2);

        // check that small payload is first, if we do not use payloads.
        assertEquals(gsr.getGroup(0).last().get(0).get("docId"),"small");
        assertEquals(gsr.getGroup(1).last().get(0).get("docId"),"big");

       
        // override config, so payloads are used
        searcherConfig.set("SimilarityForwarder.scorers","payload:com.flaptor.hounder.searcher.payload.DatePayloadScorer");
   
        // restart searcher
        searcher = new CompositeSearcher();
        Execute.sleep(5000);


        // perform query
        gsr = searcher.search(new AndQuery(new TermQuery("content", "contenta"),new PayloadQuery("payload")), 0, 10, new NoGroup(), 1, null, null);
        assertEquals(gsr.groups(),2);

        // check that now, using payloads, big payload is first
        assertEquals(gsr.getGroup(0).last().get(0).get("docId"),"big");
        assertEquals(gsr.getGroup(1).last().get(0).get("docId"),"small");


    }

}
