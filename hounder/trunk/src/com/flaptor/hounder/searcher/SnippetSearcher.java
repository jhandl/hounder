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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.NullFragmenter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;

import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.query.QueryParser;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.Config;
import com.flaptor.util.Statistics;

/**
 * This searcher has a base searcher, and adds snippets to it's results
 * 
 * @author Rafael Horowitz
 */
public class SnippetSearcher implements ISearcher{
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());

    private final ISearcher searcher;
    private final QueryParser queryParser;

    /**
     * The generated snippet field will be SNIPPET_FIELDNAME_PREFIX<field_name>
     * ie: the snippet for the field 'title' will be in 
     * SNIPPET_FIELDNAME_PREFIX + "title"
     * 
     */
    public static final String SNIPPET_FIELDNAME_PREFIX = "snippet_";



    /** Name of the field contained the stored data used to generate the snippets */
    private String[] snippetOfFields = null;    
    /** Returned text max length (in chars) */  
    private int[] snippetsLength;
    /** String used to glue different chunks in snippets */
    private String fragmentSeparator;

    /**
     *  Boolean indicating if we have to return a substring when Lucene 
     *  generates an empty snippet 
     */
    private boolean emptySnippetsAllowed;


    /**
     * Performance: When creating a new StringBuilder define its capacity to...
     */
    private static final int STRING_BUILDER_INITIAL_CAPACITY=16;

    /**
     * Regexp used to split words
     */
    private static final String WORD_BOUNDARY_REGEX = "\\s+";
    private static final Pattern WORD_BOUNDARY_REGEX_PATTERN= Pattern.compile(WORD_BOUNDARY_REGEX);

    /**
     * Strings used to mark the highlighted term.
     */
    private static final String HIGHLIGHTER_PREFIX = "<B>";
    private static final String HIGHLIGHTER_SUFFIX = "</B>";

    /**
     * Comparators used to sort the Fragment array by the fragment score or position
     */
    private final Comparator<Fragment> COMPARE_BY_SCORE= new CompareByScore();
    private final Comparator<Fragment> COMPARE_BY_POS= new CompareByPos();

    /**
     * Null fragmenter. Used to avoid lucene of fragmenting the text
     */
    private static final Fragmenter NULL_FRAGMENTER=new NullFragmenter();

    /**
     * Patterns used to match the beginning/end of a phrase (with a hilighted 
     * token)
     * Usually "[.?!\\-]"  etc. Marks that a new fragment begins
     */
    private final Pattern fragmentPat;
    private final Pattern prefixPattern;
    private final Pattern posfixPattern;

    /**
     * If an highlighted phrase is too short or was already seen, we return this
     * value. Note that phrases below this threshold probably will not be shown
     * It means: the phrase has a token (or more) highlighted, however the
     * phrase is not good enough.
     */
    private static final float BAD_SNIPPET_THRESHOLD= (float) 0.1;

    /**
     * Creates the searcher
     * @param searcher the base searcher
     */
    public SnippetSearcher(ISearcher searcher, Config config) {
        this(searcher,config.getStringArray("Searcher.snippetOfFields"),
                config.getIntArray("Searcher.snippetLength"),
                config.getString("Searcher.snippetFragmentSeparator"),
                config.getString("Searcher.snippetFragmentBoundary"),
                config.getBoolean("Searcher.emptySnippetsAllowed"));        
    }

    /**
     * Ctor used by the unit tests. Can also be used by others.
     * @param searcher
     * @param snippetOfFields
     * @param snippetsLength
     * @param fragmentSeparator
     * @param fragmentBoundary
     * @param emptySnippetsAllowed
     */
    public SnippetSearcher(ISearcher searcher, String[] snippetOfFields, 
            int[] snippetsLength,
            String fragmentSeparator, String fragmentBoundary,
            boolean emptySnippetsAllowed) {

        if (null == searcher) {
            throw new IllegalArgumentException("searcher cannot be null.");
        }
        this.searcher = searcher;
        this.queryParser = new QueryParser();

        if ( 0 == snippetOfFields.length){
            String msg= "Creating a SnippetSearcher, but Searcher.snippetOfFields is empty";
            logger.error(msg);//          throw new IllegalArgumentException("msg");
        }
        this.snippetOfFields = snippetOfFields;

        if (snippetsLength.length  != snippetOfFields.length ){
            String msg= "Error in configuration: snippetLength and snippetOfFields have different component size";
            logger.error(msg);
            throw new IllegalArgumentException(msg);            
        }

        this.snippetsLength = snippetsLength;
        this.fragmentSeparator = fragmentSeparator;
        //this.fragmentBoundary = fragmentBoundary;
        this.emptySnippetsAllowed = emptySnippetsAllowed;

        this.prefixPattern= Pattern.compile("^" + fragmentBoundary + "?" + HIGHLIGHTER_PREFIX + ".*");
        this.posfixPattern= Pattern.compile(".*" + HIGHLIGHTER_SUFFIX + fragmentBoundary + "?$");

        this.fragmentPat= Pattern.compile( "^.*(" + fragmentBoundary+ ")+" + "$");
    }



    public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup groupBy, int groupSize, AFilter afilter, ASort asort)  throws SearcherException{        
        GroupedSearchResults res = searcher.search(query, firstResult, count, groupBy, groupSize, afilter, asort);
        long start=0;
        long end=0;
        try {
            start= System.currentTimeMillis();
            addSnippets(res, query.getLuceneQuery());
            end = System.currentTimeMillis();
            Statistics.getStatistics().notifyEventValue("SnippetSearcher", (end-start)/1000.0f);
        } catch (IOException e) {
            end = System.currentTimeMillis();
            logger.warn(e);
            Statistics.getStatistics().notifyEventValue("SnippetSearcherException", (end-start)/1000.0f);
        }        
        return res;
    }

    /**
     * Add snippets to the search-results. It adds a new field 
     * SNIPPET_FIELDNAME_PREFIX_field with the snippet for each field
     */
    private void addSnippets (GroupedSearchResults res,  
            org.apache.lucene.search.Query query) throws IOException {    

        Formatter simpleHtmlFormatter= new SimpleHTMLFormatter(HIGHLIGHTER_PREFIX, HIGHLIGHTER_SUFFIX);
        for (int i= 0; i < snippetOfFields.length; i++) {
            String fieldToSnippet = snippetOfFields[i];   
            int snippetLength= snippetsLength[i];
            QueryScorer scorer= new QueryScorer(query, fieldToSnippet);
            addSnippets(res, fieldToSnippet, snippetLength, scorer, simpleHtmlFormatter);
        }
    }



    /**
     * Adds snippets to the search results.
     * How stuff works:
     * For each 'group g' in provided GroupedSearchResults:
     *   For each result in 'g':
     *     Use the lucene highlighter to get the terms highlighted on the required field.
     *     Then call getSnippet(...) to get the resulting snippet
     */    
    private void addSnippets (GroupedSearchResults res, String snippetOfField,
            int snippetLength, QueryScorer scorer, Formatter simpleHtmlFormatter) throws IOException {       

        Highlighter highlighter = new Highlighter(simpleHtmlFormatter, scorer);
        highlighter.setTextFragmenter(NULL_FRAGMENTER);
        highlighter.setMaxDocBytesToAnalyze(Integer.MAX_VALUE); // make sure the whole text will be analyzed
        // Here we store every seen phrase. It is used to give less score to
        // recurrying phrases
        Set<String> usedSnippets= new HashSet<String>();

        for (int j = 0; j < res.groups() ; j++) {  // for each group
            Vector<Document> resDocs = res.getGroup(j).last();
            int docsLen= resDocs.size();
            for (int i = 0; i < docsLen; i++) { // for each document on that group               
                Document doc = resDocs.get(i); // get the document i
                String text = doc.get(snippetOfField);  // text to be snippeted
                if (null == text){
                    logger.warn("Asked to snippet an unexisting field: " + snippetOfField );
                    continue;
                }

                TokenStream tokenStream = queryParser.tokenStream(snippetOfField, new StringReader(text));               
                TextFragment[] fragments = highlighter.getBestTextFragments(tokenStream, text, false, 1);

                String result= null;
                if ( null != fragments  && 0 < fragments.length) {
                    result= getSnippet(fragments[0].toString(), snippetLength, scorer, usedSnippets);
                }

                if ( null == result || 0 == result.length() ){ // 
                    if(emptySnippetsAllowed) {
                        result= "";
                    } else {
                        result= text.substring(0,Math.min(text.length(), snippetLength));
                    }
                }
                String snippetF= SNIPPET_FIELDNAME_PREFIX + snippetOfField;
                doc.add(new Field(snippetF, result.toString(), Field.Store.YES, Field.Index.NO));
            }
        }
    }




    /**
     * 
     * How stuff works:
     *   Given a highlighted text:
     *      Split it in phrases (using the Matcher fragMatcher).
     *      For each such phrase, get it's score (using getScore(...)
     *      Create new fragment for each phrase and it's score on the text       
     *   Returns all the fragments.
     *   
     * @return all the fragments. Never returns null
     */
    private ArrayList<Fragment> getPhrases(String wholeText, 
            Set<String> usedSnippets){        
        
        ArrayList<Fragment> phrases= new ArrayList<Fragment>();
        StringBuilder phrase= new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
        String[] tokens= WORD_BOUNDARY_REGEX_PATTERN.split(wholeText);
        int newFragPos=0;
        int from= 0;     
        Matcher fragMatcher= fragmentPat.matcher("");
        Matcher prefixMatcher= prefixPattern.matcher(""); 
        Matcher postfixMatcher= posfixPattern.matcher("");

        for (int i=0; i< tokens.length; i++){
            String token= tokens[i]; 
            fragMatcher.reset(token);              
            if (fragMatcher.matches()){
                phrase.append(token);
                String fragmentTxt= phrase.toString();                
                float score= getScore(fragmentTxt, usedSnippets, tokens, from, 
                        i+1, prefixMatcher, postfixMatcher);               
                Fragment frag= new Fragment(fragmentTxt, newFragPos++, score);
                phrases.add(frag);
                phrase= new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
                from= i+1;               
            } else {
                phrase.append(token);
                phrase.append(' ');
            }
        }        
        if (0 < phrase.length()){
            String txt= phrase.toString();
            float score= getScore(txt, usedSnippets, tokens, from, 
                    tokens.length, prefixMatcher, postfixMatcher);
            Fragment frag= new Fragment(phrase.toString(), newFragPos++, score);
            phrases.add(frag);
        }       
        return phrases;
    }



    /**
     * Given a fragment, calculate and returns its score.
     * @param fragmentTxt the fragment to score
     * @param usedSnippets a Set having previously snippeted phrases. It's used
     * to give less score to repeating phrases
     * 
     * @param wholeText For performance. see bellow
     * @param from For performance. see bellow
     * @param to For performance. see bellow
     * @param mPre For performance. see bellow
     * @param mPos For performance. see bellow
     * 
     * @note The params 'wholetext', 'from' and 'to' are used for performance
     * improvement only. Instead of spliting the fragmentTxt param
     * (String[] tokens= WORD_BOUNDARY_REGEX_PATTERN.split(fragmentTxt);), we
     * make use of the already created array having all the tokens and use it
     * within the passed boundaries.
     * 
     * @note 'mPre' and 'mPos' are used to match the start/end of a phrase.
     * Are passed as parameter so they are not created each time this method
     * is called. 
     * 
     * How stuff works:
     *   If the phrase (fragmentTxt) was already seen anywhere in the current 
     *     {@link GroupedSearchResults}, then return BAD_SNIPPET_THRESHOLD. 
     *   Else
     *     Add the text to the Set of known phrases (usedSnippets)
     *     Split the phrase into tokens (actually, to make it faster, use the 
     *       wholeText array which includes all the tokens of the fragmentTxt).
     *     For each such token, if it's an highlighed one (mPre and mPos match)
     *       then add a point to the phrase score.
     *     If the phrase is too short (3 or less tokens) 
     *       return BAD_SNIPPET_THRESHOLD
     *     Return the total phrase score.
     * 
     * @return the phrase score.
     */
    private float getScore(String fragmentTxt, Set<String> usedSnippets,
            String[] wholeText, int from, int to, Matcher mPre, Matcher mPos){
        // We want to avoid showing many results with the same snippet. Usually
        // it means we are showing some menu or site's page common header.
        if (usedSnippets.contains(fragmentTxt)){
            return BAD_SNIPPET_THRESHOLD;
        } // don't "else usedSnippets.add(text)" here because the phrase might 
        //  not be highlighted at all. We do that below

        //String[] tokens= WORD_BOUNDARY_REGEX_PATTERN.split(fragmentTxt);
        float res=0;

        //for (String token: tokens){
        for (int i= from; i< to; i++){
            String token= wholeText[i];
            mPre.reset(token);            
            if (mPre.matches()){ // doing 2 'if' instead of '&&' to avoid unecessary 
                // regexp reset/compilation and make it faster
                mPos.reset(token);
                if (mPos.matches()){
                    ++res;
                }
            }
        }        
        if (res > 0){ // if we are here the phrase is highlighted and new for us
            usedSnippets.add(fragmentTxt);
        }
//      if (tokens.length < 4){ //If threre are a few words only, return 0.1 or less
        if (to-from < 4){ //If threre are a few words only, return 0.1 or less
            res= Math.min(BAD_SNIPPET_THRESHOLD, res);
        }
        return  res;        
    }

    /**
     * Internal class used to sore a phrase (posibly hilighted), its score 
     * and posiction
     *
     */
    class Fragment {
        String text;
        float score;
        int pos;
        boolean useIt;

        public String getText() {
            return text;
        }
        public float getScore() {
            return score;
        }
        public int getPos() {
            return pos;
        }
        public Fragment(String text, int pos, float score){
            this.text= text;
            this.pos=pos;
            this.score= score;
            useIt=false;
        }
        public boolean isUseIt() {
            return useIt;
        }
        public void setUseIt(boolean useIt) {
            this.useIt = useIt;
        }
    }


    /**
     * Internal class used to sort an array of Fragments by the Fragment score
     */
    class CompareByScore implements Comparator<Fragment>{
        public int compare(Fragment o1, Fragment o2) {
            if (o1.getScore() == o2.getScore()) return 0;
            return o1.getScore() < o2.getScore()? 1 : -1;
        }        
    }

    /**
     * Internal class used to sort an array of Fragments by the Fragment index
     */
    class CompareByPos implements Comparator<Fragment>{
        public int compare(Fragment o1, Fragment o2) {
            if (o1.getPos() == o2.getPos()) return 0;
            return o1.getPos() > o2.getPos()? 1 : -1;
        }        
    }


    /**
     *  Selects which fragments to use. The fragments are selected by their 
     *  score, if necessary add surrounding fragments to the selected ones to
     *  fill the required length.
     *  @return a list having the indexes/pos of the selected fragments
     *  
     *  Assumes The frags are ordered by score (higher score first).
     *  Note that the frags order is altered by this method: frags is returned
     *  ordered by pos
     *  
     *  How stuff works:
     *    As the fragment list is ordered by their score, select the necessary 
     *       N first fragments (all with score > BAD_SNIPPET_THRESHOLD) until 
     *       the sum of their lenghts is >= requiredLength.
     *       If the length is >= requiredLength return the list of fragments.
     *    If all the "good" (score > BAD_SNIPPET_THRESHOLD) fragments were
     *       selected but the requiredLength was not filled:
     *       For each fragment, add surrounding fragments (forward/backward) 
     *       until the lenght is filled or there are no fragments. 
     *    Sort the list of (ids of) fragments and return it.
     *    
     *  
     */
    private List<Integer> selectFragments(ArrayList<Fragment> frags, int requiredLength ){
        int totalLength=0;
        // phrasesToUse holds the pos of the phrases to show
        LinkedList<Integer> phrasesToUse= new LinkedList<Integer>();
        for (Fragment frag: frags){
            if (totalLength >= requiredLength){
                Collections.sort(phrasesToUse);
                Collections.sort(frags, COMPARE_BY_POS);
                return phrasesToUse;
            }
            if (BAD_SNIPPET_THRESHOLD >= frag.getScore()){
                break;
            }
            frag.setUseIt(true);
            totalLength += frag.getText().length();
            phrasesToUse.add(frag.pos);
        }
        if (0 == phrasesToUse.size()){
            Collections.sort(frags, COMPARE_BY_POS);
            return phrasesToUse;   
        }
        // Here we have marked the fragments with score > BAD_SNIPPET_THRESHOLD 
        // to be used. Probably we will need to add some more fragments also, 
        //to return the required length        

        // Avoid an infinite loop. If we enter the 'continue' all the time and 
        //  there is no change forward nor backward
        boolean flagInfiniteLoopFrwrd= false; 
        boolean flagInfiniteLoopBackwrd= false;
        Collections.sort(frags, COMPARE_BY_POS);
        Collections.sort(phrasesToUse);
        while (totalLength < requiredLength  && !(flagInfiniteLoopBackwrd && flagInfiniteLoopFrwrd )){            
            flagInfiniteLoopFrwrd= true;
            flagInfiniteLoopBackwrd= true;                
            
            ListIterator<Integer> phrasesIterator = phrasesToUse.listIterator();
            int curr= phrasesToUse.getFirst();
            while (phrasesIterator.hasNext()){
                Integer next= phrasesIterator.next();
                if (curr == next){
                    curr ++;
                } else {
                    phrasesIterator.previous();// move one backwards
                    totalLength= addFragment(frags, curr, totalLength);
                    phrasesIterator.add(curr);
                    flagInfiniteLoopFrwrd= false;
                    curr= next;
                    if (totalLength >= requiredLength){
                        break;
                    }
                }
            }
            if (totalLength < requiredLength && curr < frags.size()){
                totalLength= addFragment(frags, curr, totalLength);
                phrasesIterator.add(curr);
                flagInfiniteLoopFrwrd= false;                
            }
            
            phrasesIterator = phrasesToUse.listIterator(phrasesToUse.size());
            curr= phrasesToUse.getLast();
            while (phrasesIterator.hasPrevious() && totalLength < requiredLength){
                Integer prev= phrasesIterator.previous();
                if (curr == prev){
                    curr --;
                } else {
                    phrasesIterator.next();
                    totalLength= addFragment(frags, curr, totalLength);
                    phrasesIterator.add(curr);
                    phrasesIterator.previous();
                    flagInfiniteLoopBackwrd= false;
                    curr= prev;
                }
            }
            if (totalLength < requiredLength && curr >= 0){
                //phrasesIterator.next();
                totalLength= addFragment(frags, curr, totalLength);                
                phrasesIterator.add(curr);
                flagInfiniteLoopBackwrd= false;                
            }
        }
        //Collections.sort(phrasesToUse);
        return phrasesToUse;
    }
    

/**
 * 
 * @param frags
 * @param requiredLength
 * @return
 * @deprecated Use selectFragments instead.
 */
private List<Integer> selectFragments2(ArrayList<Fragment> frags, int requiredLength ){
    int totalLength=0;
    // phrasesToUse holds the pos of the phrases to show
    ArrayList<Integer> phrasesToUse= new ArrayList<Integer>();
    for (Fragment frag: frags){
        if (BAD_SNIPPET_THRESHOLD >= frag.getScore()){
            break;
        }
        if (totalLength >= requiredLength){
            Collections.sort(phrasesToUse);
            Collections.sort(frags, COMPARE_BY_POS);
            return phrasesToUse;
        }
        frag.setUseIt(true);
        totalLength += frag.getText().length();
        phrasesToUse.add(frag.pos);
    }
    if (0 == phrasesToUse.size()){
        Collections.sort(frags, COMPARE_BY_POS);
        return phrasesToUse;   
    }
    // Here we have marked the fragments with score > BAD_SNIPPET_THRESHOLD 
    // to be used. Probably we will need to add some more fragments also, 
    //to return the required length        
    int nextPhraseNumber=0;
    boolean forward= false;
    Collections.sort(frags, COMPARE_BY_POS);
    // Avoid an infinite loop. If we enter the 'continue' all the time and 
    //  there is no change forward nor backward
    boolean flagInfiniteLoopFrwrd= false; 
    boolean flagInfiniteLoopBackwrd= false;
    while (totalLength < requiredLength  && !(flagInfiniteLoopBackwrd && flagInfiniteLoopFrwrd )){            
        ArrayList<Integer> tmp= new ArrayList<Integer>(phrasesToUse);
        forward= !forward;
        Collections.sort(phrasesToUse);
        if (forward){
            flagInfiniteLoopFrwrd= true;
        } else {
            flagInfiniteLoopBackwrd= true;
        }            
        int size= phrasesToUse.size();
        for (int i=0; i < size; i++){
            
            nextPhraseNumber= getNext(phrasesToUse, i, forward);
            if (nextPhraseNumber < 0 || nextPhraseNumber >= frags.size()){
                continue;
            }
            Fragment frag= frags.get(nextPhraseNumber);
            if (frag.useIt){       
                logger.warn("Adding an already added fragment. Should not happen");
                continue;
            }
            frag.setUseIt(true);
            totalLength += frag.getText().length();
            tmp.add(nextPhraseNumber);
            phrasesToUse.add(nextPhraseNumber);
            if (forward){
                flagInfiniteLoopFrwrd= false;
            } else {
                flagInfiniteLoopBackwrd= false;
            }
            if (totalLength >= requiredLength){
                break;
            }
        }
        phrasesToUse= tmp;            
    }
    Collections.sort(phrasesToUse);
    return phrasesToUse;
}
/**
 * Receives a list of (ordered) integers and returns the 1st integer 
 * not in the list, starting at 'pos' forwad or backwards 
 * @param lst
 * @param pos
 * @param forward
 * @return -1 if pos is outOfBounds 
 */
private int getNext(List<Integer> lst, int pos, boolean forward){        
    if (pos <0 || pos >= lst.size()){
        return -1;
    }
    int next;
    if (forward){
        next= lst.get(pos) + 1;
        for (int j=pos + 1; j< lst.size(); j++){
            if (lst.get(j)==next){
                ++next;
            } else {
                break;
            }
        }
    } else {
        next= lst.get(pos);
        for (int j=pos; j>= 0; j--){
            if (lst.get(j)==next){
                --next;
            } else {
                break;
            }
        }
    }
    return next;
}


    private int addFragment(ArrayList<Fragment> frags, int curr, int totalLength){
        Fragment frag= frags.get(curr);
        if (frag.useIt){       
            logger.warn("Adding an already added fragment. Should not happen");
            return totalLength;
        }
        frag.setUseIt(true);
        return totalLength + frag.getText().length();            
    }
    

    /**
     * 
     * Select some random fragments to show, till the requiredLength is filled.
     * @param frags
     * @param requiredlength
     * @return
     * 
     *  Note that the frags order is altered by this method: frags is returned
     *  ordered by pos
     *  
     */
    private List<Integer> selectRandomFragments(ArrayList<Fragment> frags, int requiredlength ){
        int mid= frags.size()/2;
        int totLength=0;
        List<Integer> fragsToUse= new ArrayList<Integer>();
        for (int i=mid; totLength < requiredlength && i < frags.size(); i++){
            Fragment frag= frags.get(i);
            frag.setUseIt(true);
            totLength += frag.getText().length();
            fragsToUse.add(frag.pos);
        }
        // if necessary, we use also backward
        for (int i=mid; totLength < requiredlength && i >= 0; i--){
            Fragment frag= frags.get(i);
            if (frag.useIt){
                continue;
            }
            frag.setUseIt(true);
            totLength += frag.getText().length();
            fragsToUse.add(frag.pos);
        }
        Collections.sort(fragsToUse);
        Collections.sort(frags, COMPARE_BY_POS);
        return fragsToUse;
    }


    /**
     * Returns a snippet of the text
     * 
     * How stuff works:
     *   1- Use the getPhrases() to get the text splited in phrases, highlighted 
     *   and scored. (Fragment class)
     *   2- If none of the Fragments is good enough (score > BAD_SNIPPET_THRESHOLD)
     *     then use selectRandomFragments() to select some random fragments 
     *      Else use selectFragments() to select the best fragments
     *   3- Concatenate the fragments (adding '..' if necessary) and return it. 
     */
    private String getSnippet(String text, int requiredlength, 
            QueryScorer scorer, Set<String> usedSnippets){               

        ArrayList<Fragment> frags= getPhrases(text, usedSnippets);       
        Collections.sort(frags, COMPARE_BY_SCORE);

        if (0 == frags.size() || 0 == frags.get(0).getScore() ){
            // if there is no highlighted word ...
            return "";
        }

        // if there are only bad fragments we fill it with random frags
        List<Integer> fragsToUse= (frags.get(0).getScore() <= BAD_SNIPPET_THRESHOLD) ? 
                selectRandomFragments(frags, requiredlength) : selectFragments(frags, requiredlength);

        StringBuilder res= new StringBuilder(STRING_BUILDER_INITIAL_CAPACITY);
        // here we have selected all the snippet fragments. They are already 
        // orderd by pos.

        //We only want to add the separator between fragments, not before the
        //first one. This flag is for that.
        boolean firstTimeFlag= true;
        for (Integer i: fragsToUse) {
            Fragment frag = frags.get(i);
            if (!frag.isUseIt()) {
                logger.warn("Adding a not to shown phrase. Should not happen");
                //continue;
            }
            if (firstTimeFlag || i == 0 || frags.get(i-1).useIt){  // do not add '...'
                firstTimeFlag= false; // before 1st fragment or between 2 consecutives fragments
            } else {
                res.append(fragmentSeparator);
                res.append(' ');
            }
            res.append(frag.getText()); // adding a space after each phrase.
            res.append(' ');
        }        
        return res.toString();
    }    
}
