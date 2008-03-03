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
package com.flaptor.search4j.classifier.bayes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.flaptor.search4j.classifier.util.DocumentParser;
import com.flaptor.search4j.classifier.util.TokenCounterPersistence;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.sort.MergeSort;
import com.flaptor.util.sort.RecordReader;
import com.flaptor.util.sort.RecordWriter;

/**
 * This class implements the bayes classifier formulae.
 * There are two differents stages of the filter.
 *
 * Traning: <br />
 *   Use multiple calls to addData() to feed the classfier with document tokens, 
 *   explicitly specifying if the document belongs to the category or not.<br />
 *
 *   Call computeProbabilities() to calculate the probability of each token to belong
 *   to the category or not, and to persist the new data to disk, making it available
 *   to start classifying new documents.<br />
 *
 * @todo load some (now hardcoded parameters) from the configuration.
 * TODO: NOT THREAD SAFE: WHoHas is static
 * @author Flaptor Development Team
 */
public class BayesCalculator {
    private static final Logger logger= Logger.getLogger(Execute.whoAmI());
    // The following thresholds are additional constraints to the terms to have assigned a probability different than the default.
    // These values are the minimum number of ocurrencies of the term in each of the following cases:
    // If the term appears only in documents that don't belong to the category, it must appear in more than MIN_REQ_NON_CATEGORIZED docs.
    // If the term appears only in documents that belong to the category, it must appear in more than MIN_REQ_CATEGORIZED docs.
    // If the term appears in documents that belong to the category and in documents that don't, it must appear in more than MIN_REQ_BOTH docs.

    private final int MIN_REQ_NON_CATEGORIZED;
    private final int MIN_REQ_CATEGORIZED;
    private final int MIN_REQ_BOTH;
    private final float IGNORE_TERM_PROBABILITY_FROM;
    private final float IGNORE_TERM_PROBABILITY_TO;

    private final String TEMP_DIR;
    private final String CAT_TOK_FILE_UNSORTED;
    private final String CAT_TOK_FILE_SORTED;
    

    private final int MAX_TUPLE_SZ;
    
    // Name of the category.
    private final String categoryName;
    // Name of the directory that will hold the probabilities and counter files.
    private final String dataDir;    

    private long categoryDocumentCount = 0L;
    private long nonCategoryDocumentCount = 0L;

    // Count of the token ocurrencies for the categorized documents.
    private RecordWriter tokenCountersFile;
    
    private Map<String, Double> myProbabilities;

    /**
     * When the Bayesian filter was trained the documents were parsed and 
     * tokenized by ({@link DocumentParser#parse(String, int)} to calculate the
     * probability of each token.
     * When the probabilities files are used to decide on a document,
     * there is no point in tokenizing more than when the probabilities file
     * was created. Ie: if we tokenized in tuples of 1, 2 and 3 words, there
     * is no point in tokenizing in tuples of 4 or more words, as no such token
     * will be in the probabilities file. 
     * The solution is to save the used maxTupleSize number in the probabilities
     * file.
     * 
     * @see BayesClassifier#getMaxTuple()
     */
    public static final String MAX_TUPLE_SIZE="__MAX_TUPLE_SIZE__";
    
    public BayesCalculator(String categoryName, Config cfg, int maxTuple) throws IOException {
        this(".", categoryName, cfg, maxTuple);        
    }

    public BayesCalculator(String dir, String categoryName, Config cfg, int maxTuple) throws IOException {
        this.MAX_TUPLE_SZ= maxTuple;
        MIN_REQ_NON_CATEGORIZED= cfg.getInt("bayes.calculator.min.required.non.categorized");
        MIN_REQ_CATEGORIZED= cfg.getInt("bayes.calculator.min.required.categorized");
        MIN_REQ_BOTH= cfg.getInt("bayes.calculator.min.required.both");
        TEMP_DIR= cfg.getString("bayes.calculator.tmp.dir");
        try {
            FileUtil.createOrGetDir(TEMP_DIR, true, true);
        } catch (IOException e) {
            logger.error("Cant create directory " + TEMP_DIR, e);
            throw e;
        }

        IGNORE_TERM_PROBABILITY_FROM= cfg.getFloat("bayes.calculator.ignore.term.probability.from");
        IGNORE_TERM_PROBABILITY_TO= cfg.getFloat("bayes.calculator.ignore.term.probability.to");
        CAT_TOK_FILE_UNSORTED= TEMP_DIR + "/tokenCountUnsorted";
        CAT_TOK_FILE_SORTED= TEMP_DIR + "/tokenCountSorted";
        FileUtil.deleteFile(CAT_TOK_FILE_UNSORTED);
        FileUtil.deleteFile(CAT_TOK_FILE_SORTED);

        this.categoryName = categoryName;
        dataDir = dir;
        myProbabilities = PersistenceManager.readProbabilitiesFromFile(dataDir,
                categoryName+".my.probabilities");
        try {
            tokenCountersFile= new TokenCounterPersistence().newRecordWriter( 
                    new File(CAT_TOK_FILE_UNSORTED));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw (e);
        }
    }

    /**
     * Returns the date of the last modification of the .probabilities file
     * @param dir
     * @param categoryName
     * @return
     */
    public static Date getProbabilitiesFileDate(String dir, String categoryName){
        File f= new File(dir,categoryName+".probabilities");
        long t= f.lastModified();
        return new Date(t);
    }
    
    
    /**
     * Adds the token counters corresponding to a document categorized or 
     * non-categorized.
     * @param documentTokenCount the map with the tokens of the document and 
     * their respective number of ocurrencies
     * @param belongsToCategory true if the document belongs to the category,
     * false otherwise
     * @throws IOException 
     * note the Map<String,int[]> was intended to be Map<String,int>, however
     * java doesnt allow to insert primitives into a Map. Because the Integer
     * are inmutables the (ugly) solution is to use an int[] of size 1. 
     */
    public void addData(Map<String,int[]>documentTokenCount, 
            boolean belongsToCategory, String url) throws IOException {
        Set<String> tokens = documentTokenCount.keySet();
        /*
         * In previous versions, we added the data to a Map(token->TokenCounter) 
         * in memory. However such aproach took too much RAM and was discarded.
         * In this version we add the data to a file that is lately transformed 
         * into a Map. see #computeTokenCounter
         */
        TokenCounterPersistence.TCRecord tcr;
        if (belongsToCategory) {
            categoryDocumentCount++;
            for (String token : tokens) {
                tcr= new TokenCounterPersistence().newRecord(token, 
                        documentTokenCount.get(token)[0], 0);
                tokenCountersFile.writeRecord(tcr);
            }
        } else {
            nonCategoryDocumentCount++;
            for (String token : tokens) {
                tcr= new TokenCounterPersistence().newRecord(token, 0, 
                        documentTokenCount.get(token)[0]);
                tokenCountersFile.writeRecord(tcr);
            }            
        }
    }


    
   
    /**
     * In previous versions, we added the data to a Map(token->TokenCounter) 
     * in memory. However such aproach consumes too much RAM and was discarded.
     * In this version we add the data to a file that is lately transformed 
     * into a Map.
     * The addCount method, adds a line 'token catVal nonCatVal' to a file, for 
     * each token. 
     * That file is then sorted in the computeTokenCounter() method.
     * After that the file is "folded" (all the lines having the same token
     * are summed).
     * Finally the probability of each token is calculated. If it's !=0 and
     * !=UNKNOWN_TOKEN_PROBABILITY, its added to the probabilities Map. 
     * 
     */
    private void computeTokenCounter(Map<String, Double> tokenProbabilities)
    throws IOException{
        logger.info("Closing categoryTokenCount...");        
        tokenCountersFile.close();        
        logger.info("Closing categoryTokenCount.... done");
        logger.info("Sorting categoryTokenCount....");
        File beforeSort= new File(CAT_TOK_FILE_UNSORTED);
        File afterSort= new File(CAT_TOK_FILE_SORTED);
        TokenCounterPersistence tcp= new TokenCounterPersistence();
        MergeSort.sort(beforeSort, afterSort, tcp);
        logger.info("Sorting categoryTokenCount.... done");

        logger.info("Folding categoryTokenCount.... ");
        // Now saves all this data to a FileCache        
        RecordReader tcrr= tcp.newRecordReader(afterSort);                
        TokenCounter catTc= new TokenCounter();
        TokenCounter nonCatTc= new TokenCounter();
        TokenCounterPersistence.TCRecord tcr= (TokenCounterPersistence.TCRecord) tcrr.readRecord();
        if (null==tcrr) return;
        catTc.update(tcr.getCatVal());
        nonCatTc.update(tcr.getNonCatVal());
        String prevToken= tcr.getToken();        
        while (null != (tcr= (TokenCounterPersistence.TCRecord) tcrr.readRecord())){
            if (tcr.getToken().equals(prevToken)){
                catTc.update(tcr.getCatVal());
                nonCatTc.update(tcr.getNonCatVal());
            } else {
                computeProbs(prevToken, catTc, nonCatTc, tokenProbabilities);
                catTc= new TokenCounter();
                nonCatTc= new TokenCounter();
                catTc.update(tcr.getCatVal());
                nonCatTc.update(tcr.getNonCatVal());
                prevToken= tcr.getToken();
            }
        }
        computeProbs(prevToken, catTc, nonCatTc, tokenProbabilities);
        logger.info("Folding categoryTokenCount.... done");
    }
    
    private void computeProbs( String token, TokenCounter catTC,  
            TokenCounter nonCatTC, Map<String, Double> tokenProbabilities){
        /* System.err.println("calculating " + token + " cat: " + catTC.getCount()
         + ", " + catTC.getCountUnique() + ". noncat: " + nonCatTC.getCount() +
         ", " + nonCatTC.getCountUnique());*/

        double probability = 0;
        // If the counters for the category are null, the token doesn't belong any document in the category.
        // Thus, its probability is MIN_PROBABILITY.
        if (0 == catTC.getCountUnique()) {
            if (nonCatTC.getCountUnique() > MIN_REQ_NON_CATEGORIZED) { // Rule determined by heruistic analisys
                probability = BayesProbabilities.CATEGORY_MIN_PROBABILITY;
            }
        // If the counters for the non-category are null, the token only appears on categorized documents.
        // Thus, its probability is MAX_PROBABILITY.
        } else if (0 == nonCatTC.getCountUnique()) {
            if (catTC.getCountUnique() > MIN_REQ_CATEGORIZED) { // Rule determined by heruistic analisys
                probability = BayesProbabilities.CATEGORY_MAX_PROBABILITY;
            }                    
        // If none of counters are null, the token appears in both types of documents.
        // Thus, I need to calculate the probability.
        } else {
            if (catTC.getCountUnique() + nonCatTC.getCountUnique() > MIN_REQ_BOTH) { // Rule determined by heuristic analisys
                double categoryFreq = (double)catTC.getCountUnique()/(double)categoryDocumentCount;
                double nonCategoryFreq= (double)nonCatTC.getCountUnique()/(double)nonCategoryDocumentCount;
                probability = categoryFreq / (categoryFreq + nonCategoryFreq);
            }
        }
        if (probability > 0 && 
                !(IGNORE_TERM_PROBABILITY_FROM <= probability   &&
                probability <= IGNORE_TERM_PROBABILITY_TO)){
            tokenProbabilities.put(token, Double.valueOf(probability));            
        }        
    }
    
    /**
     * Computes the probabilities of the whole set of tokens.
     * It writes the following data to disk: <br />
     * <ul>
     *   <li>Counters for the category documents
     *   <li>Counters for the non-category documents
     *   <li>Token probabilities
     * </ul>
     * @throws IOException 
     */
    public void computeProbabilities() throws IOException {
        Map<String, Double> tokenProbabilities = new HashMap<String, Double>();
        computeTokenCounter(tokenProbabilities);
        setMaxTupleSize(tokenProbabilities, MAX_TUPLE_SZ);
        logger.info("Computing probabilities.... done");
        PersistenceManager.writeProbabilitiesToFile(dataDir, 
                categoryName+".probabilities", tokenProbabilities);
    }

    private void setMaxTupleSize(Map<String, Double> probs, int mt){
        probs.put(BayesCalculator.MAX_TUPLE_SIZE, new Double(mt));
    }
    
    public void updateMyProbabilities(String key, Double value, boolean flush){
        myProbabilities.put(key, value);
        if (flush){
            PersistenceManager.writeProbabilitiesToFile(dataDir, 
                    categoryName+".my.probabilities", myProbabilities);
        }
    }
    
    public Map<String,Double> getMyProbabilities(){
        return myProbabilities;
    }            

    public static void printUsageAndExit() {
        System.err.println("Usage: BayesCalculator remove_tokens <category> -f <terms file>");
        System.err.println("Usage: BayesCalculator add_tokens <category> -f <terms file>");
        System.err.println("Usage: BayesCalculator convert <category>");
        System.err.println("The <category>.probabilities file is assumed to be in this (.) directory");
        System.err.println("The <terms file> should contain a list of tokens:values, " +
        		"one per line as:\n token1:0.3\n token2:0.1\n token3:0.2");
        System.err.println("Convert: The old .probabilities file, didn't include" +
        		" the MAX_TUPLE_SIZE value, as the 'tuples' where allways size 1." +
        		"Use 'convert', to automatically load an old-probabilities-file," +
        		" and add the MAX_TUPLE_SIZE value (=1) to avoid getting a " +
        		"warning with old files.");
        System.err.println("You can use the PersistenceManager " +
        		"(com.flaptor.search4j.classifier.bayes.PersistenceManager)" +
        		" to print the contents of a .probabilities file");
        System.exit(-1);
    }

    public static void removeTokens(String category, Set<String>tokens) {
        Map<String,Double> probabilities = PersistenceManager.readProbabilitiesFromFile(null, category+".probabilities");
        probabilities.keySet().removeAll(tokens);
        PersistenceManager.writeProbabilitiesToFile(null, category+".probabilities", probabilities);
    }

    public static void addTokens(String category, Set<String>tokens) {
        Map<String,Double> probabilities = PersistenceManager.readProbabilitiesFromFile(null, category+".probabilities");
        for (String line : tokens) {
            String[] parsedLine = line.split(":");
            probabilities.put(parsedLine[0], Double.valueOf(parsedLine[1]));
        }
        PersistenceManager.writeProbabilitiesToFile(null, category+".probabilities", probabilities);
    }
    
    
    /** 
     * The old probabilities file, didn't included the MAX_TUPLE_SIZE value, as
     * the 'tuples' where allways size 1. 
     * This method, loads an old-probabilities-file, and adds the MAX_TUPLE_SIZE
     * value (=1) to avoid getting a warning with old files.
     * @param category
     */
    public static void convert(String category) {
        Map<String,Double> probabilities = PersistenceManager.readProbabilitiesFromFile(null, category+".probabilities");
        if (probabilities.containsKey(MAX_TUPLE_SIZE)){
            Double val= probabilities.get(MAX_TUPLE_SIZE);
            logger.warn("The file already has MAX_TUPLE_SIZE defined to " + val + ". Doing nothing.");            
            return;
        }
        Set<String> tokens = new HashSet<String>();
        tokens.add(MAX_TUPLE_SIZE + ":1");
        addTokens(category, tokens);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUsageAndExit();
        }

        if ("remove_tokens".equals(args[0]) && (args.length == 4) && "-f".equals(args[2])) {
            Set<String> tokens = new HashSet<String>();
            FileUtil.fileToSet(null, args[3], tokens);
            removeTokens(args[1], tokens);
        } else if ("add_tokens".equals(args[0]) && (args.length == 4) && "-g".equals(args[2])) {
            Set<String> tokens = new HashSet<String>();
            FileUtil.fileToSet(null, args[3], tokens);
            addTokens(args[1], tokens);
        } else if ("convert".equals(args[0]) && (args.length == 2)) {
            convert(args[1]);                 
        } else {
            System.err.println("Invalid parameter");
            printUsageAndExit();
        }

    }
}

