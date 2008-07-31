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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.flaptor.hounder.crawler.pagedb.Page;
import com.flaptor.util.Pair;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;

/**
 * Matches the contents of the page against a list of words stored in a file
 * If the file contains any of the words, it returns true. Othrwise it returns false.
 * @author jorge
 */
public class WordFilterModule extends ATrueFalseModule {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private PhraseMatcher phrases; // list of words or phrases any of which a page must match to become a hotspot.
    private FileMonitor monitor = null;
    private long checkDelay = 10; // ten seconds
    private long minTimeBetweenFiles = 60; // one minute
  
    
	/**
	 * Create a WordFilterModule and start the file monitor. 
	 */
    public WordFilterModule (String name, Config globalConfig) throws IOException{
        super(name, globalConfig);
        String filename = getModuleConfig().getString("word.list.file");
        String filepath = FileUtil.getFilePathFromClasspath(filename);
        phrases = readFile(filepath);
        monitor = new FileMonitor(filepath);
        monitor.setDaemon(true);
        monitor.start();
    }
    
    /**
	 * Read the file of filter words.
	 */
    private PhraseMatcher readFile(String filename) {
    	PhraseMatcher phrases = new PhraseMatcher();
    	if (null != filename) {
            try {
                File phraseFile = new File(filename);
                if (phraseFile.exists()) {
                    BufferedReader reader = new BufferedReader(new FileReader(phraseFile));
                    while (reader.ready()) {
                    	String line = reader.readLine().toLowerCase();
                        phrases.add(line);
                    }
                }
            } catch (IOException e) {
            	logger.error("Reading word filter file.", e);
            }
    	}
    	return phrases;
    }
    
    /**
     * This class matches phrases against texts.
     * The phrases are stored as Lisp cons-cells. 
     * For example, the phrases "one", "two three", "two four"
     * are stored as follows:
     *  { (one, null);
     *    (two, { (three, null);
     *            (four, null)
     *          }
     *  }
     */
    private class PhraseMatcher {

    	/** Recursive definition of a phrase store */
    	private HashMap<String,PhraseMatcher> cons;

    	/** Constructor */
    	public PhraseMatcher() {
    		cons = new HashMap<String,PhraseMatcher>();
    	}
    	
    	/**
    	 * Recursively adds a phrase.
    	 * @param str the phrase to add.
    	 */
    	public void add(String str) {
    		String[] parts = str.split("\\s",2);
    		if (parts.length > 0) {
    			String word = parts[0];
    			if (!cons.containsKey(word)) {
    				cons.put(word,null);
    			}
    			if (parts.length > 1) {
    				String rest = parts[1];
        			PhraseMatcher matcher = cons.get(word);
        			if (null == matcher) {
        				matcher = new PhraseMatcher();
        				cons.put(word, matcher);
        			}
    				matcher.add(rest);
    			}
    		}
    	}
    	
    	/**
    	 * Recursively tries to match a given word array against the stored phrases.
    	 * @param words The array of words
    	 * @param start The starting position in the word array.
    	 * @return true if a match is found.
    	 */
    	private boolean matchWords(String[] words, int start) {
            boolean found = false;
            for (int i = start; i < words.length; i++) {
            	if (cons.containsKey(words[i])) {
            		PhraseMatcher rest = cons.get(words[i]);
            		if (null == rest || rest.matchWords(words,i+1)) {
            			found = true;
            			break;
            		}
            	}
            }
            return found;
    	}

    	/**
    	 * Tries to match a text against the stored phrases.
    	 * @param text the text to match.
    	 * @return true if a match is found.
    	 */
    	public boolean matchText(String text) {
            return matchWords(text.split("\\s"), 0);
    	}

    }
    
    
    // Once a new words file has been read, the new data can replace the old data.
    private synchronized void switchWordSets (PhraseMatcher newWords) {
        phrases = newWords;
    }


    /**
     * Check the parsed text against the list of filter words. 
     * If any word matches, return true, otherwise return false. 
     */
    @Override
    public Boolean tfInternalProcess (FetchDocument doc) {
        Page page = doc.getPage();
        if (null == page) {
            logger.warn("Page is null. Ignoring document.");
            return null;
        }
        return phrases.matchText(doc.getText().toLowerCase());
    }
    
   
    /**
     * Monitor a file, and reload it if its timestamp changed.
     */
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
                            PhraseMatcher newWords = readFile(filename);
                            switchWordSets(newWords);
                            lastTimestamp = timestamp;
                            logger.info("Filter words updated from " + filename);
                        } catch (Exception e) {
                            logger.error("Couldn't read new words file ("+filename+"): ", e);
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
    
}
