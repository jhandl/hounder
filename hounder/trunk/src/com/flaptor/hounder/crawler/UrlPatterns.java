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
 * The expressions are read from a file and have one of the folowing formats:
 *
 * 1. prefix 
 * 2. prefix | regex
 * 3. prefix || tokens
 * 4. prefix | regex || tokens
 *
 * 5. *
 * 6. * | regex || tokens
 * 7. * || tokens
 *
 *
 * and are parsed as follows:
 *
 * 1. exact prefix match, no tokens associated
 * 2. if prefix matches, the ending of the string is matched against regex, 
 *    no tokens associated
 * 3. just prefix match, with tokens associated
 * 4. same as 2, but associating tokens
 * 5. everything matches, no tokens
 * 6. everything matches, regex is ignored, tokens associated
 * 7. everything matches, tokens associated
 *
 * "tokens" is a comma separated list of keywords associated with the 
 * prefix/regex of that line
 * 
 * A string matches the UrlPatterns class if it matches any given prefix that
 * was not followed by a regular expression, or if some inicial part of it 
 * matches any given prefix, and the rest matches the regular expression that followed.
 *
 * For example, if the file contains the lines "abc" and "xyz | [0-9]+" (without the quotes),
 * then the strings "abc", "xyz3" and "xyz42" would match, 
 * but "ab", "abc3" or "xyzjk" would not.
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
                try { sleep(checkDelay*1000); } catch (Exception e) {/* ingnore */}
            }
        }

        // Stop the monitor.
        public void stopMonitoring() {
            running = false;
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
        boolean matchAll = false;
        if (null != filename) {
            try {
                File hotspotFile = new File(filename);
                if (hotspotFile.exists()) {
                    BufferedReader reader = new BufferedReader(new FileReader(hotspotFile));
                    while (reader.ready()) {
                        String line = reader.readLine();
                        if (line.length() > 0 && line.charAt(0) != '#') {  // ignore empty lines and comments
                            if (line.startsWith("*")) {  // if any line is "*", all previous or following lines are ignored and any line will match
                                matchAll = true;
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
                    reader.close();
                } else {
                    throw new IllegalArgumentException("Couldn't find user urls file " +filename);
                }
            } catch (IOException e) {
                String msg="Reading hostpots file (" + filename + "): " + e;
                logger.error(msg,e);
                throw new IllegalArgumentException(msg,e);
            }
        }
        return matchAll;
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


    // Just a pattern, without tokens
    private class OnlyPattern extends PatternRule{
        private Pattern pattern;

        public OnlyPattern(Pattern pattern) {
            this.pattern = pattern;
        }
        public OnlyPattern(String expr) {
            this.pattern = Pattern.compile(expr, Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);  // compile the pattern 
        }

        public boolean matches(String suffix) {
            return pattern.matcher(suffix).matches();
        }

        public PatternRule join(PatternRule other) {
            if (!canJoin(other)) 
                throw new IllegalArgumentException(toString() +" can not join " + other.toString());


            String expr = "(" + this.pattern.toString()+ ")|(" + ((OnlyPattern)other).pattern.toString() + ")";           
            return new OnlyPattern(expr);
        }
    }

    // just tokens, using .* as regex (matches always returns true)
    private class OnlyTokens extends PatternRule{
        private Set<String> tokens;

        public OnlyTokens(String[] tokenArray) {
            tokens = new HashSet<String>(tokenArray.length,1);
            for (String token: tokenArray){
                tokens.add(token);
            }
        }

        public OnlyTokens(Set<String> tokenSet) {
            tokens = new HashSet<String>(tokenSet);
        }

        public PatternRule join(PatternRule other) {
            if (!canJoin(other)) 
                throw new IllegalArgumentException(toString() +" can not join " + other.toString());

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

        public Set<String> getTokens(){
            return tokens;
        }
    }

    // Different tokens for different patterns
    private class PatternAndTokens extends PatternRule{
        private Set<String> tokens;
        private Pattern pattern;

        public PatternAndTokens(String expr,String[] tokenArray){
            this(Pattern.compile(expr,Pattern.CASE_INSENSITIVE | Pattern.COMMENTS),tokenArray);
        }
        
        public PatternAndTokens(String expr,Set<String> tokenSet){
            this(Pattern.compile(expr,Pattern.CASE_INSENSITIVE | Pattern.COMMENTS),tokenSet);
        }

        public PatternAndTokens(Pattern pattern, String[] tokenArray) {
            // construct a hashset that will be full (no append here)
            this.tokens = new HashSet<String>(tokenArray.length,1);
            this.pattern = pattern;

            for (String token: tokenArray) {
                tokens.add(token);
            }
        }

        public PatternAndTokens(Pattern pattern, Set<String> tokenSet) {
            this.pattern = pattern;
            this.tokens = new HashSet<String>(tokenSet);
        }

        public boolean canJoin(PatternRule other) {
            return super.canJoin(other) && this.tokens.equals(((PatternAndTokens)other).tokens);
        }

        public PatternRule join(PatternRule other){
            if (!canJoin(other)) 
                throw new IllegalArgumentException(toString() +" can not join " + other.toString());

            String expr = "(" + this.pattern.toString()+ ")|(" + ((PatternAndTokens)other).pattern.toString() + ")";           
            Pattern regex = Pattern.compile(expr, Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);  // compile the pattern 
            // this.tokens equals other.tokens, so there is no problem on return this tokens
            return new PatternAndTokens(regex,this.tokens);
        }

        public boolean matches(String suffix) {
            return pattern.matcher(suffix).matches();
        }

        public Set<String> getTokens() {
            return tokens;
        }
    }



    // string before first | is assumed to be prefix
    // string after || is assumed to be tokens
    // string between first | and || is assumed to be pattern.
    private Pair<String,PatternRule> getPatternRule(String line) {
        if (line.matches(".*\\|\\|.*")) {
            String[] parts = line.split("\\|\\|",2);
            String[] tokens = parts[1].split(",");
            String[] prefix = parts[0].split("\\|",2);

            if (prefix.length == 1 ) {
                logger.debug("only tokens: " + line);
                return new Pair<String,PatternRule>(prefix[0].toLowerCase().trim(),new OnlyTokens(tokens));
            } else { // length == 2
                logger.debug("pattern and tokens: " + line);
                return new Pair<String,PatternRule>(prefix[0].toLowerCase().trim(),new PatternAndTokens(prefix[1].toLowerCase().trim(),tokens));
            }
        } else { // does not match ||, only pattern
            String[] parts = line.split("\\|",2);
            String pattern = "";
            String prefix = parts[0].toLowerCase().trim();
            if (parts.length == 1) {
                pattern = "^$";
            } else {
                pattern = parts[1].toLowerCase().trim();
            }
            logger.debug("only pattern: " + line + " pattern: "+pattern);
            return new Pair<String,PatternRule>(prefix,new OnlyPattern(pattern));
        }
    }

    private void parseDefaultTokens(String line) {
        String[] parts = line.split("\\|\\|",2);
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

