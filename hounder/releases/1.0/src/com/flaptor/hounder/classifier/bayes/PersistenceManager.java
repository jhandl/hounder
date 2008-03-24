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
package com.flaptor.hounder.classifier.bayes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.flaptor.util.Execute;


/**
 * @author Flaptor Development Team
 */
public final class PersistenceManager {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    
    /** To avoid instantiation and inheritance */
    private PersistenceManager() {}

    /**
     * Writes the map of token probabilities to disk.
     * @param dirname the name of the directory that will hold the file
     * @param filename the name of the file (< category name >.probabilities)
     * @param map the object to persist
     */
    public static void writeProbabilitiesToFile(String dirname, String filename, Map<String,Double> map) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(dirname,filename))));
            oos.writeObject(map);
            oos.flush();
        } catch (IOException e) {
            System.err.println("write: " +e);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    System.err.println("close: " +e);
                }
            }
        }
    }

    /**
     * Writes the map of token counters to disk.
     * @param dirname the name of the directory that will hold the file
     * @param filename the name of the file (< category name >.[non-]categorized)
     * @param map the object to persist
     */
    static void writeCountersToFile(String dirname, String filename, Map<String,TokenCounter> map) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(dirname,filename))));
            oos.writeObject(map);
            oos.flush();
        } catch (IOException e) {
            System.err.println("write: " +e);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    System.err.println("close: " +e);
                }
            }
        }
    }

    
    static void writeWhoHasToFile(String dirname, String filename, Map<String,Set<String>> map) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(dirname,filename))));
            oos.writeObject(map);
            oos.flush();
        } catch (IOException e) {
            System.err.println("write: " +e);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    System.err.println("close: " +e);
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public static Map<String,Set<String>> readWhoHasFromFile(String dirname, String filename) {
        Map<String,Set<String>> map = null;
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(dirname,filename))));
            map = (Map<String,Set<String>>)ois.readObject();
        } catch (FileNotFoundException e) {
            //System.err.println("read: " +e);
            String msg= "WhoHas file " + dirname + "/" + filename + 
            " not found, creating one.";
            logger.debug(msg);            
            map= new HashMap<String,Set<String>>();
        } catch (InvalidClassException e) {
            System.err.println("read: invalid object version " +e);
            throw new IllegalArgumentException("Invalid version");
        } catch (IOException e) {
            System.err.println("read: " +e);
            throw new IllegalArgumentException("I/O Error");
        } catch (ClassNotFoundException e) {
            System.err.println("read: " +e);
            map = new HashMap<String,Set<String>>();
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    System.err.println("close: " +e);
                }
            }
        }
        return map;
    }

    
    /**
     * Reads a token probabilities map from disk.
     * @param dirname the name of the directory holding the file
     * @param filename the name of the file (< category name >.probabilities)
     * @return the map read
     */
    @SuppressWarnings("unchecked")
    public static Map<String,Double> readProbabilitiesFromFile(String dirname, String filename) {
        Map<String,Double> map = null;
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(dirname,filename))));
            map = (Map<String,Double>)ois.readObject();
        } catch (FileNotFoundException e) {
            //System.err.println("read: " +e);
            String msg= "Probabilities file " + dirname + "/" + filename + 
            " not found, creating one.";
            logger.debug(msg);
            map= new HashMap<String,Double>();
        } catch (InvalidClassException e) {
            System.err.println("read: invalid object version " +e);
            throw new IllegalArgumentException("Invalid version");
        } catch (IOException e) {
            System.err.println("read: " +e);
            throw new IllegalArgumentException("I/O Error");
        } catch (ClassNotFoundException e) {
            System.err.println("read: " +e);
            map = new HashMap<String,Double>();
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    System.err.println("close: " +e);
                }
            }
        }
        return map;
    }

    /**
     * Reads a token counter map from disk.
     * @param dirname the name of the directory holding the file
     * @param filename the name of the file (< category name >.[non-]categorized)
     * @return the map read
     */
    @SuppressWarnings("unchecked")
    static Map<String,TokenCounter> readCountersFromFile(String dirname,String filename) {
        Map<String,TokenCounter> map = null;
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(dirname,filename))));
            map = (Map<String,TokenCounter>)ois.readObject();
        } catch (IOException e) {
            System.err.println("read: " +e);
            map = new HashMap<String,TokenCounter>();
        } catch (ClassNotFoundException e) {
            System.err.println("read: " +e);
            map = new HashMap<String,TokenCounter>();
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    System.err.println("close: " +e);
                }
            }
        }
        return map;
    }

    /**
     * Writes the content of the counters map to a file in a human readable way.
     * @param map the counters map
     * @param dirname the name of the directory that will hold the file
     * @param filename the name of the file (< category name >.counters-dump)
     */
    static void dumpCountersToDisk(Map<String,TokenCounter> map, String dirname, String filename) throws IOException {
        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(new File(dirname, filename)));
        try {
            dumpCountersToStream(map,fos);
        } finally {
            if (fos != null) {
                try { fos.close(); }
                catch (Exception e) { System.err.println("close: " +e); }
            }
        }
    }

    /**
     * Writes the content of the counters map to a file in a human readable way.
     * @param map the counters map
     * @param fos the output stream
     */
    static void dumpCountersToStream(Map<String,TokenCounter> map, OutputStream fos) throws IOException {
        fos.write((Integer.toString(map.keySet().size()) + " terms found.").getBytes("UTF-8"));
        for (String token : map.keySet()) {
            fos.write((token + ": " + map.get(token)+"\n").getBytes("UTF-8"));
        }
    }

    /**
     * Writes the content of the probabilities map to a file in a human readable way.
     * @param map the probabilities map
     * @param dirname the name of the directory that will hold the file
     * @param filename the name of the file (< category name >.counters-dump)
     */
    static void dumpProbabilitiesToDisk(Map<String,Double> map, String dirname, String filename) throws IOException {
        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(new File(dirname, filename)));
        try {
            dumpProbabilitiesToStream(map,fos);
        } finally {
            if (fos != null) {
                try { fos.close(); }
                catch (Exception e) { System.err.println("close: " +e); }
            }
        }
    }

    /**
     * Writes the content of the counters map to a file in a human readable way.
     * @param map the counters map
     * @param fos the output stream
     */
    static void dumpProbabilitiesToStream(Map<String,Double> map, OutputStream fos) throws IOException {
        for (String token : map.keySet()) {
            fos.write((token + ":" + map.get(token)+"\n").getBytes("UTF-8"));
        }
    }

    /**
     * Dumps the content of a counters or probabilities file.
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: PersistenceManager < filename >");
            return;
        }
        if (args[0].endsWith(".probabilities")) {
            Map<String,Double> probabilities = PersistenceManager.readProbabilitiesFromFile(".", args[0]);
            PersistenceManager.dumpProbabilitiesToStream(probabilities,System.out);
        } else if (args[0].endsWith(".categorized") || args[0].endsWith(".non-categorized")) {
            Map<String,TokenCounter> counters = PersistenceManager.readCountersFromFile(".", args[0]);
            PersistenceManager.dumpCountersToStream(counters,System.out);
        } else {
            System.err.println("Invalid parameter");
            System.err.println("Usage: MultiCalculator < filename >");
            System.err.println("Dont forget the filename extension. The filename" +
            		" should be <category>.probabilities or " +
            		"<category>.categorized or <category>.non-categorized");
        }
    }

}

