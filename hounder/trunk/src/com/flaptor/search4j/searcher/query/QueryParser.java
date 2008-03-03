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
package com.flaptor.search4j.searcher.query;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.BooleanClause;

import com.flaptor.util.Config;
import com.flaptor.util.Pair;

/**
 * Generates an AQuery from a String.
 * This class basically uses Lucene's query parser, but adds the extra functionality of specifying
 * the fields and weights of the non-field prefixed terms in the query via the configuration file.
 * The QueryParser is thread-safe.
 * @author Flaptor Development Team
 */
public class QueryParser implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI()); 
    private static final Config searcherConfig = Config.getConfig("searcher.properties");
    private static final Config commonConfig = Config.getConfig("common.properties");
	protected Analyzer analyzer = null;
	protected Pair<?,?> fieldsAndWeights[] = null;
    protected PhraseMatcher phraseMatcher = null;

	/**
	  Constructor.
	  Takes many configuration parameters from the config.
	 */
	public QueryParser() {
		analyzer = createAnalyzer();   
        phraseMatcher = createPhraseMatcher();
        
        // Queryparser config
        String fields[] = searcherConfig.getString("QueryParser.searchFields").split(",");
        String weights[] = searcherConfig.getString("QueryParser.searchFieldWeights").split(",");
        if (fields.length != weights.length) {
            String m = "searchFields and searchFieldsWeights length do not match. Please fix the config file.";
            logger.fatal(m);
            throw new IllegalArgumentException(m);
        }
        fieldsAndWeights = new Pair<?,?>[fields.length];
        logger.info("Using the following field(s) and weight(s)");
        for (int i=0; i < fields.length; i++) {
            fieldsAndWeights[i] = new Pair<String,Float>(fields[i], Float.parseFloat(weights[i]));
            logger.info(fieldsAndWeights[i]);
        }
	}

	/**
	* Returs a new created analyzer to be used for parsing.
	* Helper method to be used by the constructor. It create the analyzer that tokenizes
	* only the fields to be tokenized.
	*/
	private Analyzer createAnalyzer() {
        final String[] stopwords = commonConfig.getStringArray("stopwords");
        StandardAnalyzer stdAnalyzer = new StandardAnalyzer(stopwords);
		PerFieldAnalyzerWrapper retval = new PerFieldAnalyzerWrapper(stdAnalyzer);
		String[] nonTokenizedField = searcherConfig.getStringArray("QueryParser.nonTokenizedFields");
        String[] synonymFields = searcherConfig.getStringArray("QueryParser.synonymFields");
        String synonymFile = searcherConfig.getString("QueryParser.synonymFile");

	    KeywordAnalyzer keywordAnalyzer = new KeywordAnalyzer();
		for (String field : nonTokenizedField) {
		    logger.info("createAnalyzer: adding field to be skipped during tokenization: " + field);
			retval.addAnalyzer(field, keywordAnalyzer);
		}
	
        if (synonymFields.length > 0 ) {
            SynonymAnalyzer synonymAnalyzer = new SynonymAnalyzer(stdAnalyzer,synonymFile);
            for (String field : synonymFields) {
                logger.info("createAnalyzer: adding field to be expanded with synonyms: " + field);
                retval.addAnalyzer(field, synonymAnalyzer);
            }
        }
        return retval;

    }

    /**
     * it creates the phraseMatcher from the file specified in searcher.query.phrasesFile
     * if blank, it recognizes no phrases
     * 
     * @return the newly created phraseMatcher
     */
    private PhraseMatcher createPhraseMatcher() {
        PhraseMatcher pm = new PhraseMatcher();
        String phrasesFilePath = searcherConfig.getString("QueryParser.phrasesFile").trim();
        if (phrasesFilePath.equals("")) pm.construct(new ArrayList<String>());
        else {
            try {

                pm.construct(new File(phrasesFilePath));
            } catch (IOException e) {
                logger.error("error while constructing phraseMatcher", e);
                pm.construct(new ArrayList<String>());
            }
        }
        return pm;
    }


    /**
     * Returns a query from a string.
     * @see org.apache.lucene.queryParser.QueryParser for the format of the input string.
     * @throws IllegalArgumentException if the string can not be parsed.
     */
    public org.apache.lucene.search.Query parse(String queryStr) {
        //the parser chokes on null
        if (null == queryStr) {
            String s = "query string cannot be null";
            logger.error(s);
            throw new NullPointerException(s);
        }

        queryStr = matchPhrases(queryStr);

        org.apache.lucene.search.Query q[];
        org.apache.lucene.search.BooleanQuery bq = new org.apache.lucene.search.BooleanQuery();

        try {
            q = new org.apache.lucene.search.Query[fieldsAndWeights.length];
            for (int i = 0; i < fieldsAndWeights.length ; i++) {
                org.apache.lucene.queryParser.QueryParser qp = 
                    new org.apache.lucene.queryParser.QueryParser((String)fieldsAndWeights[i].first(), analyzer);
                qp.setDefaultOperator(org.apache.lucene.queryParser.QueryParser.Operator.AND);
                logger.debug("parse: parsing query: " + queryStr + " on field " + fieldsAndWeights[i].first());
                q[i] = qp.parse(queryStr);
                q[i].setBoost(((Float)fieldsAndWeights[i].last()).floatValue());
                bq.add(q[i], BooleanClause.Occur.SHOULD);
            }
        } catch (org.apache.lucene.queryParser.ParseException e) {
            String s = "parse: lucene could not parse query: " + queryStr + ", message: " + e.getMessage();
            logger.error(s);
            throw new IllegalArgumentException(s);
        }
        //if there's just 1 default field, the boolean query will have just one term, and it will be an extra layer
        // that add nothing, so I remove it.
        if (bq.getClauses().length == 1) {
            return q[0];
        } else {
            return bq;
        }
    }

    private String matchPhrases(String query) {
        StringBuffer newQuery = new StringBuffer();
        int pos = -1;
        boolean inQuotes = false;
        while (true) {
            int start = pos + 1;
            pos = query.indexOf('"', start);

            String subquery = (pos == -1) ? query.substring(start) : query.substring(start, pos);
            if (!inQuotes) {
                newQuery.append(phraseMatcher.recognize(subquery).trim());
            } else {
                newQuery.append(subquery);
            }
            if (pos==-1) break;

            if (!inQuotes) newQuery.append(" ");
            newQuery.append("\"");
            if (inQuotes)  newQuery.append(" ");
            inQuotes = !inQuotes;
        }
        return newQuery.toString().trim();
    }



    public TokenStream tokenStream(String arg0, Reader arg1) {
        return analyzer.tokenStream(arg0, arg1);
    }
}

