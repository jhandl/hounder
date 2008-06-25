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
package com.flaptor.hounder.crawler.modules;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.hounder.crawler.pagedb.PageDB;
import com.flaptor.hounder.indexer.IRemoteIndexer;
import com.flaptor.hounder.indexer.Indexer;
import com.flaptor.hounder.indexer.MockIndexer;
import com.flaptor.hounder.indexer.RmiIndexerStub;
import com.flaptor.util.Config;
import com.flaptor.util.DomUtil;
import com.flaptor.util.Execute;
import com.flaptor.util.Pair;
import com.flaptor.util.PortUtil;
import com.flaptor.util.QuadCurve;


/**
 * Indexer Module for FetchdataProcessor.
 *
 *  @todo check how to give better error messages. Those logged now can not 
 *  help to identify the problematic document.
 *  
 * @author Flaptor Development Team
 */
public class IndexerModule extends AProcessorModule {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private int textLengthLimit; // the maximum allowed page text length.
    private int titleLengthLimit; // the maximum allowed page title length.
    private int indexerBusyRetryTime; // time in seconds between retries when the indexer is busy.
    private int categoryBoostDamp; // the amount of damping for the categoryBoost value in the boost formula.
    private int pagerankBoostDamp; // the amount of damping for the pagerankBoost value in the boost formula.
    private int logBoostDamp; // the amount of damping for the log value in the boost formula.
    private int freshnessBoostDamp; // the amount of damping for the freshnessBoost value in the boost formula.
    private QuadCurve freshnessCurve; // the curve that describes the amount of boost for any given freshness.
    private IRemoteIndexer indexer; // the Hounder indexer.
    private String crawlName; // the name of the crawl, added to the index so searches can be restricted to the results of this crawler.
    private float[] scoreThreshold; // the values for the (0 to 100 step 10) percentiles in the page score histogram.
    private HashSet hostStopWords; // the parts of web host names that are not interesting, like www.
    private boolean sendContent; //  if true the page content will be sent to the indexer in a <body> tag.
    

    public IndexerModule (String moduleName, Config globalConfig) {
        super(moduleName, globalConfig);
        textLengthLimit = globalConfig.getInt("page.text.max.length");
        titleLengthLimit = globalConfig.getInt("page.title.max.length");
        Config mdlConfig = getModuleConfig();
        indexerBusyRetryTime = mdlConfig.getInt("indexer.busy.retry.time");
        crawlName = globalConfig.getString("crawler.name");

        categoryBoostDamp = weightToDamp(mdlConfig.getFloat("category.boost.weight"));
        pagerankBoostDamp = weightToDamp(mdlConfig.getFloat("pagerank.boost.weight"));
        logBoostDamp = weightToDamp(mdlConfig.getFloat("log.boost.weight"));
        freshnessBoostDamp = weightToDamp(mdlConfig.getFloat("freshness.boost.weight"));
        prepareFreshnessBoost(mdlConfig.getString("freshness.times"));
        hostStopWords = new HashSet<String>(Arrays.asList(mdlConfig.getStringArray("host.stopwords")));
        sendContent = mdlConfig.getBoolean("send.content.to.indexer");

        // instantiate the indexer.
        if (mdlConfig.getBoolean("use.mock.indexer")) {
            logger.warn("Using a mock indexer. This should be used only for testing.");
            this.indexer = new MockIndexer();
        } else {
        	Pair<String, Integer> host = PortUtil.parseHost(mdlConfig.getString("remoteRmiIndexer.host"), "indexer.rmi");
            this.indexer = new RmiIndexerStub(host.last(), host.first());
        }        
    }


    /**
     * @todo the pageDB is checked for null, but should be checked for
     *	fetchedSize
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void internalProcess(FetchDocument doc) {

        Page page = doc.getPage();
        if (null == page) {
            logger.warn("Page is null. Ignoring this document.");
            return;
        }
        if (logger.isDebugEnabled()) { 
            logger.debug("Doc has tags: "+doc.getTags().toString());
        } 
        if (doc.hasTag(EMIT_DOC_TAG)) {
            addToIndex(doc);
        } else {
            if (page.isEmitted()) {
                deleteFromIndex(page);
            }
        }
    }


    // Delete a page from the index
    private void deleteFromIndex (Page page) {
        org.dom4j.Document dom = DocumentHelper.createDocument();
        Element root = dom.addElement("documentDelete");

        root.addElement("documentId").addText(getDocumentId(page));
        try {
            while (indexer.index(dom) == Indexer.RETRY_QUEUE_FULL) {
                try {
                    Thread.sleep(indexerBusyRetryTime*1000);
                } catch (InterruptedException e) {
                    logger.debug("Sleep interrupted: " + e, e);
                }
            }
            page.setEmitted(false);
        } catch (Exception e) {
            logger.error(e,e);
        }
    }


    // Calcuate the category boost. TODO: range?
    private float calculateCategoryBoost (Map<String,Object> attr) {
        Double categoryBoost = (Double)attr.get("categroy_boost");
        if (null == categoryBoost) {
            categoryBoost = 1d;
        }
        return categoryBoost.floatValue();
    }


    // Calcuate the pagerank boost, range 0.1 - 10.
    private float calculatePagerankBoost (Page page) {
        // page rank 
        float score = page.getScore();
        int bucket = 0;
        for (; bucket < scoreThreshold.length && scoreThreshold[bucket] <= score; bucket++);
        float pagerankBoost = bucket-1;
        if (bucket < scoreThreshold.length) {
            float bucketSpan = (scoreThreshold[bucket] - scoreThreshold[bucket-1]);
            if (bucketSpan > 0) {
                pagerankBoost += (score - scoreThreshold[bucket-1]) / bucketSpan;
            }
            if (pagerankBoost < 0.1f) pagerankBoost = 0.1f;
        }
        return pagerankBoost;
    }


    // Calcuate the log(inlinks) boost, range 0.1 - 10 for up to 20000 inlinks
    private float calculateLogBoost (Page page) {
        // log(inlinks) boost 
        float logBoost = 0.1f;
        int inlinks = page.getNumInlinks();
        if (inlinks > 1) {
            logBoost = (float)Math.log(inlinks);
        }
        return logBoost;
    }


    // Prepare the freshness boost.
    private void prepareFreshnessBoost (String timesDef) {
        double t1=0, t2=0, t3=0;
        boolean ok = false;
        try {
            Scanner times = new Scanner(timesDef).useDelimiter(",");
            t1 = (double)times.nextInt();
            t2 = (double)times.nextInt();
            t3 = (double)times.nextInt();
            if (t1 == t2 || t1 == t3 || t2 == t3) {
                logger.error("The times defined for the freshness boost cannot be the same (currently set to "+t1+","+t2+","+t3+")");
            } else if (t1 > t2 || t1 > t3 || t2 > t3) {
                logger.error("The times defined for the freshness boost must be in growing sequence (currently set to"+t1+","+t2+","+t3+")");
            } else {
                ok = true;
            }
        } catch (Exception e) {
            logger.error("The three different times (in days) must be defined for the freshness boost: max, normal and min (example: 0,7,90 for now, a week, and 3 months)", e);
        }
        if (!ok) {
            t1 = 0; // now
            t2 = 7; // one week
            t3 = 90; // three months
            logger.warn("The following times will be used for the freshness boost: "+t1+","+t2+","+t3+" days");
        }
        freshnessCurve = new QuadCurve(t1,10,t2,1,t3,0.1);
    }


    // Calcuate the freshness boost. TODO: range?
    private float calculateFreshnessBoost (Page page) {
        final long MILLIS_IN_A_DAY = 24*60*60*1000L;
        long daysSinceLastChange = (System.currentTimeMillis() - page.getLastChange()) / MILLIS_IN_A_DAY;
        if (0 == daysSinceLastChange) daysSinceLastChange = 1;
        return (float)freshnessCurve.getY(daysSinceLastChange);
    }


    // Convert from a weight value in the [0.0, 1.0] range to a damp value in the corresponding [10, 0] range.
    // This is a convenience so that the user can think in terms of weight (0 = no weight at all, 1 = full weight),
    // but the program needs the damp factor (0 = no damp, 10 = full damp). 
    // @see factor()
    private int weightToDamp (float weight) {
        return (int)(10.0 * (1.0f - weight));
    }


    // Damp the influence of a value in the boost formula.
    // If damp == 0, the value is returned unchanged, so the value retains its full influence.
    // If damp == 10, the value is disregarded and 1.0 is returned, so the value has no influence at all.
    // If damp is in the [1,9] range, the damp-power-of-two root of the value is returned, which gets closer to 1.0 as damp increases.
    private float factor (String name, float value, int damp) {
        if (value < 0.1f || value > 15f) {
            logger.warn(name+" boost value out of range! ("+value+")");
            value = (value < 0.1f) ? 0.1f : 15f;

        }
        if (damp >= 10) return 1.0f;
        for (int i = 0; i < damp; i++) {
            value = (float)Math.sqrt(value);
        }
        return value;
    }


    /**
     * Polymorphic method for deciding how to compose a documentId 
     * from a page
     * 
     * @param page
     * @return 
     */
    protected String getDocumentId(Page page) {
        return page.getUrl();
    }


    // Add a page to the index
    @SuppressWarnings("unchecked")
    protected void addToIndex (FetchDocument doc) {

        byte[] content = doc.getContent();
        if (0 == content.length) {
            logger.warn("Page has no data. Ignoring this document.");
            return;
        }

        Set<String> categories = doc.getCategories();
        Map<String,Object> attributes = doc.getAttributes();
        Map<String,Object> indexableAttributes = doc.getIndexableAttributes();

        // build xml doc
        org.dom4j.Document dom = DocumentHelper.createDocument();
        Element root = dom.addElement("documentAdd");
        Page page = doc.getPage();
        String text = doc.getText();
        String url = page.getUrl();
        String host = getHost(url);
        String title = doc.getTitle(titleLengthLimit);
        String tokenizedHost = tokenizeHost(host);
        String anchorText = getAnchorText(page);

        float categoryBoost = calculateCategoryBoost(attributes);
        float pagerankBoost = calculatePagerankBoost(page);
        float logBoost = calculateLogBoost(page);
        float freshnessBoost = calculateFreshnessBoost(page);

        // add overall score
        float f1 = factor("category",categoryBoost,categoryBoostDamp);
        float f2 = factor("pagerank",pagerankBoost,pagerankBoostDamp);
        float f3 = factor("log",logBoost,logBoostDamp);
        float f4 = factor("freshness",freshnessBoost,freshnessBoostDamp);
        float f5 = ((Double)attributes.get("boost")).floatValue(); // as calculated by the boost module, or 1.0 if no boost module is defined.
        float boost = f1 * f2 * f3 * f4 * f5;

        // System.out.println("BOOST url=["+url+"]  category="+f1+" ("+categoryBoost+":"+categoryBoostDamp+")  pagerank="+f2+" ("+pagerankBoost+":"+pagerankBoostDamp+")  log="+f3+" ("+logBoost+":"+logBoostDamp+")  freshness="+f4+" ("+freshnessBoost+":"+freshnessBoostDamp+") moduleBoost="+f5+"  Boost="+boost);

        if (boost < 1e-6) {
            logger.warn("Boost too low! ("+boost+")  category="+f1+" ("+categoryBoost+":"+categoryBoostDamp+")  pagerank="+f2+" ("+pagerankBoost+":"+pagerankBoostDamp+")  log="+f3+" ("+logBoost+":"+logBoostDamp+")  freshness="+f4+" ("+freshnessBoost+":"+freshnessBoostDamp+") moduleBoost="+f5);
        }

        root.addElement("boost").addText(String.valueOf(boost));
        root.addElement("documentId").addText(getDocumentId(page));

        Map<String,Double> boostMap = (Map<String,Double>)attributes.get("field_boost");

        // add the search fields
        addField(root, "url", url, true, true, true, boostMap);
        addField(root, "site", host, true, true, false, boostMap);
        addField(root, "tokenizedHost", tokenizedHost, false, true, true, boostMap);
        addField(root, "title", title, true, true, true, boostMap);
        addField(root, "text", text, true, true, true, boostMap);
        addField(root, "anchor", anchorText, false, true, true, boostMap);
        addField(root, "crawl", crawlName, false, true, true, boostMap);

        if (sendContent) {
            addBody(root,doc,content);
        }

        // for debugging only
        //addField(root, "boostinfo", "category="+f1+" ("+categoryBoost+":"+categoryBoostDamp+")  pagerank="+f2+" ("+pagerankBoost+":"+pagerankBoostDamp+")  log="+f3+" ("+logBoost+":"+logBoostDamp+")  freshness="+f4+" ("+freshnessBoost+":"+freshnessBoostDamp+") moduleBoost="+f5+"  Boost="+boost, true, false, false, null);

        addAdditionalFields(root, page, boostMap);

        // Adding metainfo from attributes
        Set<Entry<String,Object>> attributeSet = indexableAttributes.entrySet();
        for (Entry<String,Object> attribute : attributeSet) {
            addField(root, attribute.getKey(), attribute.getValue() == null ? "" : attribute.getValue().toString(), true, true, true, boostMap);
        }

        StringBuffer assignedCategories = new StringBuffer();
        if (null != categories) {
            // iterate through the classes the page belongs to add each category and its score
            for (Iterator<String> iter = categories.iterator(); iter.hasNext();) {
                assignedCategories.append(iter.next());
                assignedCategories.append(" ");

                // repeat the field times proportional to the score (this is a way to boost the document by category);
                //for (int rep = 0; rep < score*10; rep++) {
                //    addField(root, "categoryBoost", categ, false, true, false);
                //}
            }
            addField(root, "categories", assignedCategories.toString().trim(), true, true, true, boostMap);
        }

        if (logger.isDebugEnabled()) { 
            logger.debug("Indexing dom: " + DomUtil.domToString(dom));
        }
        // Send the document to the indexer. If the queue is full, wait and retry.
        try {
            while (indexer.index(dom) == Indexer.RETRY_QUEUE_FULL) {
                try { 
                    Thread.sleep(indexerBusyRetryTime*1000); 
                } catch (InterruptedException e) {
                    logger.debug("Sleep interrupted: " + e, e); 
                }
            }
            page.setEmitted(true);
        } catch (Exception e) {
            logger.error(e,e);
        }
    }


    /**
     * Intended for extension.
     * Any subclass of IndexerModule should override this method to add any additional field it needs.
     *    
     */
    protected void addAdditionalFields(Element root, Page page,
            Map<String, Double> boostMap) {
        //Intended for extension.
    }


    /**
     * Adds a new field to the <code>doc</code> Element. 
     * 
     * @param doc the element to add the field to
     * @param name the name of the field
     * @param value the String value for the field
     * @param stored true iif should be stored
     * @param indexed true iif should be indexed
     * @param tokenized true iif should be tokenized
     * @param boostMap map containing the boosts for each field name
     */
    protected final void addField (Element doc, String name, String value, boolean stored, boolean indexed, boolean tokenized, Map<String,Double> boostMap) {
        Double boost = 1.0d;
        if (null != boostMap && boostMap.containsKey(name)) {
            boost = boostMap.get(name);
        }
        doc.addElement("field")
            .addAttribute("name", name)
            .addAttribute("stored", Boolean.toString(stored))
            .addAttribute("indexed", Boolean.toString(indexed))
            .addAttribute("tokenized", Boolean.toString(tokenized))
            .addAttribute("boost", boost.toString())
            .addText(value);
    }


    protected final void addBody(Element doc, FetchDocument fetchDoc, byte[] bytes) {
        String encoding = null;
        // find charset. http headers usually have a Content-Type line, but
        // as it may not be in the same case, all headers are stored lowercased.
        // Content-Type lines contain mime-type and charset, separated by ;
        // for example
        // Content-Type: text/html; charset=UTF-8
        if (fetchDoc.getHeader().containsKey("content-type")) {
            String[] tokens = fetchDoc.getHeader().get("content-type").split(";");
            for (String token: tokens) {
                if (token.toLowerCase().contains("charset") && token.contains("=")){
                    encoding = token.split("=")[1].trim().toUpperCase();
                    break;
                }
            }
        }
        // if not found, use default encoding
        if (null == encoding) {
            encoding = java.nio.charset.Charset.defaultCharset().name();
        }

        try {
            doc.addElement("body").addText(new String(bytes,encoding));
        } catch(java.io.UnsupportedEncodingException e) {
            logger.error("while adding body: ",e); 
        } 
    }


    // Extract the host part of the url
    private String getHost (String url) {
        String host;
        try {
            host = new URI(url).getHost();
            if (null == host) {
                host = "";
            }
            if (0 == host.trim().length()) {
                logger.warn("Null or empty host ("+url+")");
            }
        } catch (URISyntaxException e) {
            logger.warn("Invalid url ("+url+")");
            host = "";
        }
        return host;
    }


    // Separate a host name into its parts
    private String tokenizeHost (String host) {
        String tokenizedHost;
        if (0 == host.trim().length()) {
            tokenizedHost="";
        } else {
            String[] hostParts = host.split("\\.");
            StringBuffer buf = new StringBuffer();

            // strip the common parts away
            for (String part : hostParts) {
                if (!hostStopWords.contains(part)) {
                    buf.append(part);
                    buf.append(" ");
                }
            }

            // add the normalized domain (sans subdomain)
            int keep = (hostParts[hostParts.length-1].length() == 2) ? 3 : 2;
            keep = Math.min(keep, hostParts.length);
            for (int i = hostParts.length-keep; i < hostParts.length; i++) {
                buf.append(hostParts[i]);
                buf.append(".");
            }

            tokenizedHost = buf.toString();
        }
        return tokenizedHost;
    }


    // Return a string with all the anchors
    private String getAnchorText (Page page) {
        StringBuffer anchorText = new StringBuffer();
        String[] anchors = page.getAnchors();
        for (int i=0; i<anchors.length; i++) {
            anchorText.append(" ");
            anchorText.append(anchors[i]);
        }
        return anchorText.toString();
    }


    public void applyCommand(Object command){
        if ("optimize".equals(command.toString())) {
            logger.info("optimize requested.");
            try {
                org.dom4j.Document dom = DocumentHelper.createDocument();
                dom.addElement("command").addAttribute("name", "optimize");
                indexer.index(dom);
            } catch (Exception e) {
                logger.error(e,e);
            }
        } else if ("delete".equals(command.toString())) {
            FetchDocument doc = ((CommandWithDoc)command).getDoc();
            Page page = doc.getPage();
            deleteFromIndex(page);
        } else if ("startCycle".equals(command.toString())) {
            PageDB pagedb = ((CommandWithPageDB)command).getPageDB();
            scoreThreshold = new float[11];
            for (int i = 0; i < scoreThreshold.length; i++) {
                scoreThreshold[i] = pagedb.getScoreThreshold(i*10);
            }
        }
    }

}
