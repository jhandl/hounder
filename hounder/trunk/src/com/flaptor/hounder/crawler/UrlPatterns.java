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
package com.flaptor.hounder.crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.flaptor.util.Pair;
import com.flaptor.util.TrieTree;



/**
 * The UrlPatterns class keeps a large number of expressions against which a string can be matched.
 * The expressions are read from a file and must comply with the folowing format:
 *
 *    &lt;prefix&gt; [ | &lt;pattern&gt; ] [ || &lt;anti-pattern&gt; ] [ ||| &lt;tokens&gt; ]
 *
 * The following forms are valid:
 *
 *    1. prefix
 *    2. prefix ||| tokens
 *    3. prefix | pattern
 *    4. prefix | pattern ||| tokens
 *    5. prefix | pattern || anti-pattern
 *    6. prefix | pattern || anti-pattern ||| tokens
 *    7. *
 *
 * "tokens" is a comma separated list of keywords that are associated with the 
 * prefix/regex of that line. The "*" prefix matches everything. If such a line 
 * is included, everying else in the file is ignored.
 * 
 * A string matches the UrlPattern if it matches any given prefix that
 * was not followed by a regular expression, or if some inicial part of it 
 * matches any given prefix, and the rest matches the pattern that followed 
 * and does not contain anything that matches the anti-pattern, if specified.
 * Note that while the pattern needs to match the url after the prefix in its 
 * entirety, the anti-pattern can match any substring of it. 
 *
 * For example, if the file contains the following lines:
 *    abc
 *    jkl | [0-9]+
 *    xyz | [a-z0-9/]+ || pag[0-9]
 * then the strings "abc", "jkl123" and "xyz/foo/bar" would match, 
 * but "ab", "abc3", "jkl", "jklx", "xyz/foo/pag3/bar" and "mno" would not.
 *
 * @author Flaptor Development Team
 */
public class UrlPatterns {

    private TrieTree<Vector<PatternRule>> patterns = null;
    private Set<String> defaultTokens;
    private boolean matchAll = false;
    private static Logger logger = Logger.getLogger(Execute.whoAmI());
    private long checkDelay = 10; // ten seconds
    private long minTimeBetweenFiles = 60; // one minute
    private FileMonitor monitor = null;


    /**
     * Class constructor. 
     * Reads the patterns file and builds the TrieTree that will be used to match urls.
     * @param filename the patterns file name.
     */
    public UrlPatterns (String filename) throws IOException {
        patterns = new TrieTree<Vector<PatternRule>>();
        defaultTokens = new HashSet<String>();
        if ( ! (new File(filename)).exists()) {
            String altname = FileUtil.getFilePathFromClasspath(filename);
            if (null == altname) {
                throw new IOException("File not found: "+filename);
            } else {
               filename = altname;
            }
        }
        matchAll = readPatterns (filename, patterns);
        monitor = new FileMonitor(filename);
        monitor.setDaemon(true);
        monitor.start();
    }


    /**
     * Alternative class constructor.
     * @param filename the patterns file name.
     * @param checkDelay the time it takes to check for changes in the patterns file, in seconds.
     */
    public UrlPatterns (String filename, int checkDelay) throws IOException {
        this(filename);
        this.checkDelay = checkDelay;
    } 
    
    // Monitor a file, and reload it if its timestamp changed.
    private class FileMonitor extends Thread {

        private String filename = null;
        private long lastTimestamp = 0;
        private volatile boolean running = false;

        // Initialize the monitor class.
        public FileMonitor (String filename) {
            this.filename = filename;
            lastTimestamp = System.currentTimeMillis();
        }

        // Start monitoring the file.
        public void run() {
            Thread.currentThread().setName("UrlPattern.FileMonitor("+filename+")");
            running = true;
            while (running) {
                File file = new File(filename);
                long now = System.currentTimeMillis();
                if ((now - lastTimestamp) > minTimeBetweenFiles * 1000) {
                    long timestamp = file.lastModified();
                    if (timestamp > lastTimestamp) {
                        logger.info("New patterns file detected ("+filename+")");
                        try {
                            TrieTree<Vector<PatternRule>> newPatterns = new TrieTree<Vector<PatternRule>>();
                            boolean newMatchAll = readPatterns(filename, newPatterns);
                            switchPatterns(newPatterns, newMatchAll);
                            lastTimestamp = timestamp;
                            logger.info("Patterns updated from " + filename);
                        } catch (Exception e) {
                            logger.error("Couldn't read new patterns file ("+filename+"): ", e);
                        }
                    }
                }
                try { sleep(checkDelay*1000); } catch (Exception e) {/* ignore */}
            }
        }

        // Stop the monitor.
        public void stopMonitoring() {
            running = false;
            interrupt();
        }

    }


    /**
     * Close the url patterns and the monitor.
     */
    public void close () {
        monitor.stopMonitoring();
    }

    // Once a new pattern file has been read, the new data can replace the old data.
    private synchronized void switchPatterns (TrieTree<Vector<PatternRule>> newPatterns, boolean newMatchAll) {
        patterns = newPatterns;
        matchAll = newMatchAll;
    }

    // Read the patterns from a file.
    private boolean readPatterns (String filename, TrieTree<Vector<PatternRule>> patterns) {
        boolean newMatchAll = false;
        if (null != filename) {
        	BufferedReader reader = null;
            try {
                File hotspotFile = new File(filename);
                if (hotspotFile.exists()) {
                    reader = new BufferedReader(new FileReader(hotspotFile));
                    while (reader.ready()) {
                        String line = reader.readLine();
                        if (line.length() > 0 && line.charAt(0) != '#') {  // ignore empty lines and comments
                            if (line.startsWith("*")) {  // if any line is "*", all previous or following lines are ignored and any line will match
                                newMatchAll = true;
                                parseDefaultTokens(line);
                                continue;
                            }
                            Pair<String,PatternRule> pair = getPatternRule(line);
                            
                            Vector<PatternRule> stored = patterns.get(pair.first());
                            if (null == stored) {
                                stored = new Vector<PatternRule>();
                                stored.add(pair.last());
                                patterns.put(pair.first(),stored);
                            } else { // there was something for that prefix
                                boolean joined = false;
                                for (int i = 0 ; i < stored.size() && !joined ; i++) {
                                    if (stored.get(i).canJoin(pair.last())) {
                                        stored.set(i,stored.get(i).join(pair.last()));
                                        joined = true;
                                    }
                                }
                                if (!joined) {
                                    stored.add(pair.last());
                                }
                            }
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Couldn't find user urls file " +filename);
                }
            } catch (IOException e) {
                String msg="Reading hostpots file (" + filename + "): " + e;
                logger.error(msg,e);
                throw new IllegalArgumentException(msg,e);
            } finally {
            	Execute.close(reader);
            }
        }
        return newMatchAll;
    }

    /**
     * Matches a line against the stored patterns.
     * @param line the line to match against the stored patterns.
     * @return true if there is a match, false otherwise.
     */
    public synchronized boolean match (String line) {
        boolean matches = false;
        if (matchAll) {  // if the wildcard "*" was used in the patterns file, everything matches
            matches = true;
        } else {
            line = line.toLowerCase();
            Vector<TrieTree<Vector<PatternRule>>.PartialMatch<Vector<PatternRule>>> partial = patterns.getPartialMatches(line);  // get all the patterns for this prefix
            for (int i = 0; i < partial.size() && ! matches; i++) {  // go through each pattern and try to match the rest of the line
                int pos = partial.elementAt(i).getPosition();  // pos is the place in the line where the prefix ends
                String suffix = line.substring(pos + 1);
                for (PatternRule rule : partial.elementAt(i).getValue()) {
                    if (rule.matches(suffix)) {
                        matches = true;
                        break;
                    }
                }
            }
        }
        return matches;
    }

    /**
     * Indicates whether the patterns file matches all urls or only a selection.
     * @return true if all pages match, false otherwise.
     */
    public synchronized boolean matchAll () {
        return matchAll;
    }


    /**
     * Returns the tokens that the url matches, an empty set if there is a match but no token is specified, and null if there is no match.
     */
    public synchronized Set<String> getTokens(String line) {
        boolean matches = false;
        Set<String> tokens = new HashSet<String>(defaultTokens);

        line = line.toLowerCase();
        Vector<TrieTree<Vector<PatternRule>>.PartialMatch<Vector<PatternRule>>> partial = patterns.getPartialMatches(line);  // get all the patterns for this prefix
        for (int i = 0; i < partial.size() ; i++) {  // go through each pattern and try to match the rest of the line
            int pos = partial.elementAt(i).getPosition();  // pos is the place in the line where the prefix ends
            String suffix = line.substring(pos + 1);
            for (PatternRule rule : partial.elementAt(i).getValue()) {
                if (rule.matches(suffix)) {
                    tokens.addAll(rule.getTokens());
                    matches = true;
                }
            }
        }
        if (!matches && !matchAll) tokens = null;
        return tokens;
    }



    // INTERNAL CLASSES, stored in trietree vectors
    private abstract class PatternRule {
        public boolean canJoin(PatternRule other) {
            return this.getClass().equals(other.getClass());
        }
       
        public Set<String> getTokens() {
            return new HashSet<String>(0);
        }

        public abstract boolean matches(String url);
        public abstract PatternRule join (PatternRule rule);
    }

    
    // No Pattern, no tokens.
    private class EmptyRule extends PatternRule {
        public boolean matches(String url) { return (url.length() == 0); }
        public PatternRule join(PatternRule rule) { return this; }
    }


    // Just a pattern, without tokens
    private class OnlyPattern extends PatternRule{
        private Pattern pattern, antiPattern;

        public OnlyPattern(Pattern pattern, Pattern antiPattern) {
            this.pattern = pattern;
            this.antiPattern = antiPattern;
        }
        public OnlyPattern(Pattern pattern) {
            this(pattern,null);
        }
        public OnlyPattern(String expr1, String expr2) {
            this.pattern = Pattern.compile(expr1, Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);
            if (null != expr2 && expr2.trim().length() == 0) { expr2 = null; }
            this.antiPattern = (null==expr2) ? null : Pattern.compile(".*"+expr2+".*", Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);
        }
        public OnlyPattern(String expr) {
            this(expr,null);
        }

        public boolean matches(String suffix) {
            boolean match = pattern.matcher(suffix).matches();
            if (match && null != antiPattern && antiPattern.matcher(suffix).matches()) {
                match = false;
            }
            return match;
        }
        
        @Override
        public boolean canJoin(PatternRule other) {
            // true if same class and (both antiPatterns are null (can't be same instance) or are equal)
            boolean can = super.canJoin(other) && (
                    ((null == this.antiPattern && null == ((OnlyPattern)other).antiPattern))
                    || ((null != this.antiPattern && null != ((OnlyPattern)other).antiPattern) 
                        && (this.antiPattern.toString().equals(((OnlyPattern)other).antiPattern.toString()))));
            return can;
        }

        public PatternRule join(PatternRule other) {
            if (!canJoin(other)) {
                throw new IllegalArgumentException(toString() +" can not join " + other.toString());
            }

            String expr1 = "(" + this.pattern.toString()+ ")|(" + ((OnlyPattern)other).pattern.toString() + ")";  
            String expr2 = null;
            if (null != this.antiPattern) { // they must both be equal (see canJoin)
                expr2 = this.antiPattern.toString();
            }
            return new OnlyPattern(expr1,expr2);
        }
    }

    // just tokens, using .* as regex (matches always returns true)
    private class OnlyTokens extends PatternRule{
        private Set<String> tokens;

        public OnlyTokens(String[] tokenArray) {
            tokens = new HashSet<String>(tokenArray.length,1);
            for (String token : tokenArray){
                tokens.add(token.trim());
            }
        }

        public OnlyTokens(Set<String> tokenSet) {
            tokens = new HashSet<String>(tokenSet);
        }

        public PatternRule join(PatternRule other) {
            if (!canJoin(other)) {
                throw new IllegalArgumentException(toString() +" can not join " + other.toString());
            }
            Set<String> otherTokens = ((OnlyTokens)other).tokens;
            Set<String> newSet = new HashSet<String>(this.tokens.size() + otherTokens.size(),1);
            newSet.addAll(this.tokens);
            newSet.addAll(otherTokens);
            return new OnlyTokens(newSet);
        }

        // .*, just for prefix match
        public boolean matches(String suffix) {
            return true;
        }

        @Override
        public Set<String> getTokens(){
            return tokens;
        }
    }

    // Different tokens for different patterns
    private class PatternAndTokens extends PatternRule {
        private OnlyPattern pattern;
        private OnlyTokens tokens;

        public PatternAndTokens(String expr1, String expr2, String[] tokenArray) {
            pattern = new OnlyPattern(expr1, expr2);
            tokens = new OnlyTokens(tokenArray);
        }
        
        public PatternAndTokens(String expr1, String expr2, Set<String> tokenSet) {
            pattern = new OnlyPattern(expr1, expr2);
            tokens = new OnlyTokens(tokenSet);
        }

        private PatternAndTokens(OnlyPattern pattern, OnlyTokens tokens) {
            this.pattern = pattern;
            this.tokens = tokens;
        }
        
        @Override
        public boolean canJoin(PatternRule other) {
            return super.canJoin(other) 
                    && pattern.canJoin(((PatternAndTokens)other).pattern) 
                    && tokens.canJoin(((PatternAndTokens)other).tokens);
        }

        public PatternRule join(PatternRule other){
            if (!canJoin(other)) {
                throw new IllegalArgumentException(toString() +" can not join " + other.toString());
            }
            OnlyPattern p = (OnlyPattern)pattern.join(((PatternAndTokens)other).pattern);
            OnlyTokens t = (OnlyTokens)tokens.join(((PatternAndTokens)other).tokens);
            return new PatternAndTokens(p,t);
        }

        public boolean matches(String suffix) {
            return pattern.matches(suffix);
        }

        @Override
        public Set<String> getTokens() {
            return tokens.getTokens();
        }
    }

    

    // PatternRule syntax:  
    //   <prefix> | <pattern> || <antiPattern> ||| <token> [, <token>] ... 
    // Any part may be missing.
    
    private Pair<String,PatternRule> getPatternRule(String line) {
        line = line.trim();
        int[] x = new int[5];
        x[0] = -1;
        x[1] = line.indexOf("|");
        x[2] = line.indexOf("||");
        x[3] = line.indexOf("|||");
        x[4] = line.length();
        if (x[1] == x[2] || x[1] == x[3]) { x[1] = -1; }
        if (x[2] == x[3]) { x[2] = -1; }
        int fst;
        for (fst=3; fst > 0; fst--) { if (x[fst]!=-1) { break; } }
        boolean hasPrefix = ((x[fst] > 0) || (fst==0 && line.length() > 0));
        boolean hasPattern = (x[1] >= 0);
        boolean hasAntiPattern = (x[2] > x[1]);
        boolean hasTokens = (x[3] > x[2]);
        for (int i=3; i>0; i--) { if (x[i] == -1) { x[i] = x[i+1]; } }
        String prefix = "";
        String pattern = null;
        String antiPattern = null;
        String[] tokens = null;
        if (hasPrefix) { prefix = line.substring(0,x[1]).toLowerCase().trim(); }
        if (hasPattern) { pattern = line.substring(x[1]+1,x[2]).toLowerCase().trim(); }
        if (hasAntiPattern) { antiPattern = line.substring(x[2]+2,x[3]).toLowerCase().trim(); }
        if (hasTokens) { tokens = line.substring(x[3]+3).split(","); }
        
        Pair<String,PatternRule> out;
        if (hasPattern && hasTokens) {
            out = new Pair<String,PatternRule>(prefix,new PatternAndTokens(pattern,antiPattern,tokens));
        } else if (hasPattern) {
            out = new Pair<String,PatternRule>(prefix,new OnlyPattern(pattern,antiPattern));
        } else if (hasTokens) {
            out = new Pair<String,PatternRule>(prefix,new OnlyTokens(tokens));
        } else { // only prefix
            out = new Pair<String,PatternRule>(prefix,new EmptyRule());
        }
        return out;
    }

    private void parseDefaultTokens(String line) {
        String[] parts = line.split("\\|\\|\\|",2);
        if (parts.length == 2 ) {
            String[] tokens = parts[1].trim().split(",");
            for (String token:tokens) {
                defaultTokens.add(token);
            }
        }
    }



    private static void usage (String msg) {
        System.out.println();
        System.out.println(msg);
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  UrlPattern filename url");
        System.out.println();
        System.exit(1);
    }

    // For testing
    public static void main (String[] args) throws IOException {
        String log4jConfigPath = com.flaptor.util.FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            logger.warn("log4j.properties not found on classpath!");
        }
        if (args.length < 2) {
            usage("Not enough arguments.");
        }
        String filename = args[0];
        String url = args[1];
        UrlPatterns pat = new UrlPatterns(filename);
        System.out.println(pat.match(url)?"match":"no match");
        System.out.println("tokens: " + pat.getTokens(url));
    }


}

