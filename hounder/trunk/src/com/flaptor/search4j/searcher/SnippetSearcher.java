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
package com.flaptor.search4j.searcher;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.NullFragmenter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;

import com.flaptor.search4j.searcher.filter.AFilter;
import com.flaptor.search4j.searcher.group.AGroup;
import com.flaptor.search4j.searcher.query.AQuery;
import com.flaptor.search4j.searcher.query.QueryParser;
import com.flaptor.search4j.searcher.sort.ASort;
import com.flaptor.util.Config;
import com.flaptor.util.Statistics;

/**
 * This searcher has a base searcher, and add snippets to it's results
 * 
 * @author Rafael Horowitz
 */
public class SnippetSearcher implements ISearcher{
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());

    private final ISearcher searcher;
    private final QueryParser queryParser;

    /**
     * The gerated snippet field will be SNIPPET_FIELDNAME_PREFIX_<field_name>
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
    /** Usually "[.?!\\-]"  etc. Marks that a new fragment begins */
    private String fragmentBoundary; 

    /* Boolean indicating if we have to return a substring
         when Lucene generates an empty snippet */
    private boolean emptySnippetsAllowed;

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
     * Ctor used by the unittsets. Can also be used by others.
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
        this.fragmentBoundary = fragmentBoundary;
        this.emptySnippetsAllowed = emptySnippetsAllowed;
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

    private void addSnippets (GroupedSearchResults res,  
            org.apache.lucene.search.Query query) throws IOException {
        for (int i = 0; i < snippetOfFields.length; i++) {
            String fieldToSnippet = snippetOfFields[i];   
            int snippetLength= snippetsLength[i];
            addSnippets(res, fieldToSnippet, snippetLength, query);
        }
    }

   
    /**
     * Adds snippets to the search results.
     */
    private void addSnippets (GroupedSearchResults res, String snippetOfField,
            int snippetLength, org.apache.lucene.search.Query query) throws IOException {
        QueryScorer scorer= new QueryScorer(query,snippetOfField);
        Formatter simpleHtml= 
            new SimpleHTMLFormatter(HIGHLIGHTER_PREFIX, HIGHLIGHTER_SUFFIX);
        Highlighter highlighter = new Highlighter(simpleHtml, scorer);
        highlighter.setTextFragmenter(new NullFragmenter());
        
        Set<String> usedSnippets= new HashSet<String>();
        for (int j = 0; j < res.groups() ; j++) {  // for each group
            for (int i = 0; i < res.getGroup(j).last().size(); i++) { // for each document on that group
                Document doc = res.getGroup(j).last().get(i); // get the document i
                String text = doc.get(snippetOfField);  // text to be snippeted
                if (null == text){
                    logger.warn("Asked to snippet an unexisting field: " + snippetOfField );
                    continue;
                }
                highlighter.setMaxDocBytesToAnalyze(text.length()); // make sure the whole text will be analyzed
                TokenStream tokenStream = queryParser.tokenStream(snippetOfField, new StringReader(text));
                TextFragment[] fragments = highlighter.getBestTextFragments(tokenStream, text, false, 1);
               
                String result= null;
                if ( null != fragments  && 0 < fragments.length) {
                    result= getSnippet(fragments[0].toString(), snippetLength, query, scorer, usedSnippets);
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
                logger.debug("Added " + snippetF + "=" + result);
            }
        }
    }

    private static final String WORD_BOUNDARY_REGEX = "\\s+";
    private static final String HIGHLIGHTER_PREFIX = "<B>";
    private static final String HIGHLIGHTER_SUFFIX = "</B>";

    /**
     * 
     * @param text
     * @param query
     * @param scorer
     * @return never null
     */
    private Vector<Fragment> getPhrases(String text,  
            org.apache.lucene.search.Query query, QueryScorer scorer,
            Set<String> usedSnippets){ 
        Vector<Fragment> phrases= new Vector<Fragment>();
        StringBuffer sb= new StringBuffer();
        String[] tokens= text.split(WORD_BOUNDARY_REGEX);
        int pos=0;
        for (int i=0; i< tokens.length; i++){
            String token= tokens[i]; 
            String regex= "^.*(" + fragmentBoundary+ ")+" + "$";
            if (token.matches(regex)){                
                sb.append(token);
                String txt= sb.toString();                
                float score= getScore(txt, query, scorer, usedSnippets);
                Fragment frag= new Fragment(txt, pos++, score);
                phrases.add(frag);
                sb= new StringBuffer();
            } else {
                sb.append(token);
                sb.append(' ');
            }
        }
        if (0 < sb.length()){
            String txt= sb.toString();
            float score= getScore(txt, query, scorer, usedSnippets);
            Fragment frag= new Fragment(sb.toString(), pos++, score);
            phrases.add(frag);
        }
        return phrases;
    }
    
    /**
     * If an highlighted phrase is too short or was already seen, we return this
     * value. Note that phrases below this threshold probably will not be shown
     */
    private static final float BAD_SNIPPET_THRESHOLD= (float) 0.1;
    
    protected float getScore(String text,  org.apache.lucene.search.Query query,
            QueryScorer scorer, Set<String> usedSnippets){
        // We want to avoid showing many results with the same snippet. Usually
        // it means we are showing some menu or site's page common header.
        if (usedSnippets.contains(text)){
            return BAD_SNIPPET_THRESHOLD;
        } // don't "else usedSnippets.add(text)" here because the phrase might 
        //  not be a highlighted at all. We do that below
                  
        String[] tokens= text.split(WORD_BOUNDARY_REGEX);
        float res=0;
        int score=0;
               
        for (String token: tokens){
//            if (token.startsWith(HIGHLIGHTER_PREFIX) && token.endsWith(HIGHLIGHTER_SUFFIX)){
            if (token.matches("^" + fragmentBoundary + "?" + HIGHLIGHTER_PREFIX + ".*") 
                    && token.matches(".*" + HIGHLIGHTER_SUFFIX + fragmentBoundary + "?$")){
                score++;
            }
        }        
        res= (float) score;
        if (res > 0){ // if we are here the phrase is highlighted and new for us
            usedSnippets.add(text);
        }
        if (tokens.length < 4){ //If threre are a few words only, return 0.1 or less
            res= Math.min(BAD_SNIPPET_THRESHOLD, res);
        }
        if (logger.isDebugEnabled()){
            logger.debug("Scorer= " + res  + ", " +text );
        }
        return  res;        
    }

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
    

    class CompareByScore implements Comparator<Fragment>{
        public int compare(Fragment o1, Fragment o2) {
            if (o1.getScore() == o2.getScore()) return 0;
            return o1.getScore() < o2.getScore()? 1 : -1;
        }        
    }

    class CompareByPos implements Comparator<Fragment>{
        public int compare(Fragment o1, Fragment o2) {
            if (o1.getPos() == o2.getPos()) return 0;
            return o1.getPos() > o2.getPos()? 1 : -1;
        }        
    }

    /**
     * returns the 1st integer not in the list, starting at 'pos' forwad or backwards 
     * @param lst
     * @param pos
     * @param forward
     * @return -1 if pos is outOfBounds 
     */
    private int getNext(List<Integer> lst, int pos, boolean forward){        
        if (pos <0 || pos >= lst.size()){
            return -1;
        }
        int prev= lst.get(pos);
        if (forward){
            for (int i=pos; i< lst.size(); i++){
                if (lst.get(i)==prev){
                    ++prev;
                } else {
                    break;
                }
            }
        } else {
            for (int i=pos; i>= 0; i--){
                if (lst.get(i)==prev){
                    --prev;
                } else {
                    break;
                }
            }
        }
        return prev;
    }

    /**
     *  Selects which fragments to use
     *  returns a list having the indexes/pos of the selected fragments
     */
    private List<Integer> selectFragments(Vector<Fragment> frags, int requiredlength ){
        int totLength=0;
        // useIt holds the pos of the phrases to show
        List<Integer> useIt= new ArrayList<Integer>();
        for (Fragment frag: frags){
            if (totLength >= requiredlength || BAD_SNIPPET_THRESHOLD >= frag.getScore()){ 
                break;
            }
            frag.setUseIt(true);
            totLength += frag.getText().length();
            useIt.add(frag.pos);
        }
        if (0 == useIt.size()){
         return useIt;   
        }
        // Here we have marked the fragments with score > BAD_SNIPPET_THRESHOLD 
        // to be used. Probably we will need to add some more fragments also, 
        //to return the required lenght        
        int nextPosToShow=0;
        boolean forward= false;
        Collections.sort(frags, new CompareByPos());
        // Avoid an infinite loop. If we enter the 'continue' all the time and 
        //  there is no change forward nor backward
        boolean flagInfiniteLoopFrwrd= false; 
        boolean flagInfiniteLoopBackwrd= false;
        while (totLength < requiredlength  && !(flagInfiniteLoopBackwrd && flagInfiniteLoopFrwrd )){            
            List<Integer> tmp= new ArrayList<Integer>(useIt);
            forward= !forward;
            Collections.sort(useIt);
            if (forward){
                flagInfiniteLoopFrwrd= true;
            } else {
                flagInfiniteLoopBackwrd= true;
            }            

            for (int i=0; i < useIt.size(); i++){
                //j = forward? Math.max(j, shownPos): Math.min(j, i);
                nextPosToShow= getNext(useIt, i, forward);
                if (nextPosToShow < 0 || nextPosToShow >= frags.size()){
                    continue;
                }
                Fragment frag= frags.get(nextPosToShow);
                if (frag.useIt){       
                    logger.warn("Adding an already added fragment. Should not happen");
                    continue;
                }
                frag.setUseIt(true);
                totLength += frag.getText().length();
                tmp.add(nextPosToShow);
                if (forward){
                    flagInfiniteLoopFrwrd= false;
                } else {
                    flagInfiniteLoopBackwrd= false;
                }
                if (totLength >= requiredlength){
                    break;
                }
            }
            useIt= tmp;            
        }
        Collections.sort(useIt);
        return useIt;
    }
    
    private List<Integer> selectRandomFragments(Vector<Fragment> frags, int requiredlength ){
        int mid= frags.size()/2;
        int totLength=0;
        List<Integer> useIt= new ArrayList<Integer>();
        for (int i=mid; totLength < requiredlength && i < frags.size(); i++){
            Fragment frag= frags.get(i);
            frag.setUseIt(true);
            totLength += frag.getText().length();
            useIt.add(frag.pos);
        }
        // if necessary, we use also backward
        for (int i=mid; totLength < requiredlength && i >= 0; i--){
            Fragment frag= frags.get(i);
            frag.setUseIt(true);
            totLength += frag.getText().length();
            useIt.add(frag.pos);
        }
        Collections.sort(useIt);
        return useIt;
    }
    
    private String getSnippet(String text, int requiredlength, 
            org.apache.lucene.search.Query query, QueryScorer scorer, 
            Set<String> usedSnippets){        
        Vector<Fragment> frags= getPhrases(text, query, scorer, usedSnippets); 
        Collections.sort(frags, new CompareByScore());
        if (0 == frags.size() || 0 == frags.elementAt(0).getScore() ){
            // if there is no highlighted word ...
            return "";
        }
        // if there are only bad fragments we fill it with random frags
        List<Integer> useIt= (frags.elementAt(0).getScore() <= BAD_SNIPPET_THRESHOLD) ?
                selectRandomFragments(frags, requiredlength):
                selectFragments(frags, requiredlength);
        
        StringBuffer res= new StringBuffer();
        // here we have selected all the snippet fragments. Now let put them in
        // order.
        Collections.sort(frags, new CompareByPos());
        //We only want to add the separator between fragments, not before the
        //first one. This flag is for that.
        boolean firstTimeFlag= true;
        for (Integer i: useIt) {
            Fragment frag = frags.elementAt(i);
            if (!frag.isUseIt()) {
                logger.warn("Adding a not to shown phrase. Should not happen");
                //continue;
            }
            if (firstTimeFlag || i == 0 || frags.elementAt(i-1).useIt){  // do not add '...'
                firstTimeFlag= false; // before 1st fragment or between 2 consecutives fragments
            } else {
                res.append(fragmentSeparator + " ");
            }
            res.append(frag.getText() + " "); // adding a space after each phrase.
        }
        return res.toString();
    }    
}
