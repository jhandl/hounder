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
package com.flaptor.hounder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.MergeScheduler;
import org.apache.lucene.store.FSDirectory;

import com.flaptor.util.CommandUtil;
import com.flaptor.util.Config;
import com.flaptor.util.Execute;

/**
 * Contains indexed data (basically a lucene index + some metadata).
 * This class provides methods to make a lightweight copy of an index and 
 * to store and retrieve metadata (Properties) directly in the index. 
 * This class is not thread safe. Use it from a single thread. The writers or readers
 * this class provides, may be thread safe. See lucene's documentation.
 * Configuration strings:
 *      luceneMergeFactor: defaults to 10. See Lucene's documentation.
 *      luceneMinMergeDocs: defaults to 1000. See Lucene's documentation.
 *      luceneMaxMergeDocs: defaults to 10000. See Lucene's documentation.
 *      
 * @author Flaptor Development Team
 */
public class Index {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    protected volatile State state = State.closed;
    protected final File path;
    private FSDirectory directory;
    private Analyzer analyzer;
    private final Properties properties;
    
    private final MergeScheduler mergeScheduler;
    private final MergePolicy mergePolicy;
    private final double smallSegmentSize;
    private IndexDescriptor indexDescriptor;

    /**
     * Constructs an Index object to access the index data on disk.
     * @param path the abstract path to the directory containing the
     *  index data.
     */
    public Index(final File path) {
        this(path, false);
    }

    /**
     * Closes the index, ensuring that all data is flushed to disk.
     * It is, however, the responsability of the caller to ensure that
     * all writers and reader are closed before making this call.
     */
    public void close(){
        state = State.closed;
        writeProperties();
        directory.close();
    }

    /**
     * Completely erases an index.
     * It deletes all data from disk. Calling this method closes the index.
     */
    public void eraseFromDisk() {
        logger.info("Deleting index located at " + path);
        close();
        com.flaptor.util.FileUtil.deleteDir(path);
    }

    public boolean exists() {
        return path.exists();
    }

    /**
     * Creates a new, empty index.
     * @param path
     * @return
     */
    public static Index createIndex(final File path) {
        return new Index(path, true);
    }

    /**
     * Gets a writer to modify the index.
     * @return a lucene IndexWriter. The caller has to remember closing the
     * writer it gets.
     * @throws RuntTimeException if there was a problem opening the writer.
     * @throws IllegalStateException if the index has been closed.
     */
    public IndexWriter getWriter() {
        if (state == State.closed) {
            throw new IllegalStateException("Can't get writer: the index is closed.");
        }
        try {
            IndexWriter writer = new IndexWriter(directory, analyzer, false);
            writer.setMergePolicy(mergePolicy);
            writer.setMergeScheduler(mergeScheduler);
            writer.setMaxBufferedDocs(IndexWriter.DISABLE_AUTO_FLUSH);
            writer.setRAMBufferSizeMB(smallSegmentSize);
            return writer;
        } catch (IOException e) {
            logger.fatal(e);
            throw new RuntimeException(e);
        }

    }

    /**
     * Gets a reader to read/erase over the index.
     * @return a lucene IndexReader. The caller has to remember closing it after use.
     * @throws IllegalStateException if the index has been closed.
     */
    public IndexReader getReader() {
        if (state == State.closed) {
            throw new IllegalStateException("Can't get reader: the index is closed.");
        }
        try {
            return IndexReader.open(directory);
        } catch (IOException e) {
            logger.error("Error while getting the reader.", e);
            throw new RuntimeException("Error while getting the reader.");
        }

    }

    /**
     * Creates a new index copying this one's data.
     * ALL WRITERS AND READERS MUST BE CLOSED BEFORE CALLING THIS METHOD.
     * It is not necessary to close this index itself. Internally it will be
     * closed, and reopened after the copy is performed, so the original
     * index can be used after making the copy.
     * Internally, it optimizes the operation by making a hard-link copy
     * of lucene's files.
     * @return
     */
    public Index copyIndex(final File pathOfNewIndex) {
        if (pathOfNewIndex.exists()) {
            throw new IllegalArgumentException(pathOfNewIndex.getAbsolutePath() + " exists. I can't create a new index there.");
        }
        writeProperties();
        com.flaptor.util.Execute.close(directory, logger);
        makeHardLinkCopy(path, pathOfNewIndex);
        setUpDirectory(false);
        Index retValue = new Index(pathOfNewIndex);
        retValue.setIndexDescriptor(indexDescriptor);
        return retValue;
    }

    /**
     * Stores metadata in this index.
     */
    public void setIndexProperty(final String key, final String value) {
        properties.setProperty(key, value);
    }

    /**
     * Retrieves metadata stored in this index.
     * Returns null if the key has not been defined.
     */
    public String getIndexProperty(final String key) {
        return properties.getProperty(key);
    }


    /**
     * Returns the size of the index, in bytes
     */
    public Long length() {
        return path.length();
    }


    public IndexDescriptor getIndexDescriptor() {
        return indexDescriptor;
    }

    public void setIndexDescriptor(IndexDescriptor descriptor) {
        this.indexDescriptor = descriptor;
        this.indexDescriptor.setLocalPath(path.getAbsolutePath());
    }


    //---------------------------------------------------------------------------------
    //Private methods
    private Index(final File path, boolean create) {
        if (!create) {
            if (!path.exists()) {
                throw new IllegalArgumentException("The path passed to the costructor doesn't exist.");
            }
            if (!path.isDirectory()) {
                throw new IllegalArgumentException("The path passed to the constructor is not a directory");
            }
        } else {
            if (path.exists()) {
                throw new IllegalArgumentException("Cannot create index on " + path.getAbsolutePath() + ". Path exists.");
            }
        }
        this.path = path;
        properties = new Properties();
        if (!create) {
            File propFile = new File(path.getAbsolutePath() + File.separator + "index.properties");
            if (!propFile.exists()) {
                throw new IllegalArgumentException("Cannot find the properties inside the index. Is the index corrupted?.");
            }
            if (!propFile.isFile()) {
                throw new IllegalArgumentException("There's no file named index.properties in the index. Maybe it is a directory?");
            }
            InputStream is = null;
            try {
                is = new FileInputStream(propFile);
                properties.load(is);
                indexDescriptor = new IndexDescriptor(properties.get("indexDescriptor").toString());
            } catch (IOException e) { 
                logger.error("Exception while trying to load index.properties for index at " + path.getAbsolutePath(), e);
            } catch (NullPointerException e) {
                logger.error("There is no index descriptor on " + propFile.getName() + ". using default. Exception caused by " + e.getMessage(), e);
                indexDescriptor = IndexDescriptor.defaultDescriptor();
            }finally {
                com.flaptor.util.Execute.close(is, logger);
            }
        }
        Config config = Config.getConfig("common.properties");

        failOnLegacyParameters(config);
        
        ConcurrentMergeScheduler cms = new ConcurrentMergeScheduler();
        cms.setMaxThreadCount(12);
        cms.setMergeThreadPriority(Thread.MIN_PRIORITY);
        this.mergeScheduler = cms;
        
        LogByteSizeMergePolicy mp = new LogByteSizeMergePolicy();
        smallSegmentSize = config.getFloat("Index.smallSegmentSizeMB");
        mp.setMinMergeMB(smallSegmentSize);
        mp.setMergeFactor(config.getInt("Index.mergeFactor"));
        mergePolicy = mp;
        
        
        
        createAnalyzer();
        setUpDirectory(create);
    }
    
    /**
     * This is to force users to update the configuration files.
     * All this code can be removed after 1May2008.
     * @param config
     * @throws IllegalArgumentException if some of the old configuration
     * variables is found.
     */
    void failOnLegacyParameters(Config config) {
    	int ok = 0;
    	try {
    		config.getInt("IndexManager.luceneMergeFactor");
    	} catch (IllegalStateException e) {
    		ok++;
    	}
    	try {
    		config.getInt("IndexManager.luceneMinMergeDocs");
    	} catch (IllegalStateException e) {
    		ok++;
    	}
    	try {
    		config.getInt("IndexManager.luceneMaxMergeDocs");
    	} catch (IllegalStateException e) {
    		ok++;
    	}
    	if (ok < 3) {
    		throw new IllegalArgumentException("the configuration variables IndexManager.luceneMergeFactor," +
    				" IndexManager.luceneMinMergeDocs and IndexManager.luceneMaxMergeDocs are obsolete. Please" +
    				" update the configuration files.");
    	}
    }
    
    /**
     * Flushes the metadata stored in properties to disk.
     */
    private void writeProperties() {
        File propFile = new File(path.getAbsolutePath() + File.separator + "index.properties");
        OutputStream os = null;
        try {
            os = new FileOutputStream(propFile, false); //do not append, overwrite.
            // Persist index descriptor on index.properties, if not null
            if (null == indexDescriptor) {
                logger.warn("Writing index properties on " + propFile.getAbsolutePath() + " without index descriptor");
            } else  {
                properties.setProperty("indexDescriptor",indexDescriptor.toString());   
            }
            properties.store(os, "This file is automatically generated while saving the index. Do not edit it by hand.");
        } catch (IOException e) { 
            logger.error("Exception while trying to store index.properties for index at " + path.getAbsolutePath(), e);
        }finally {
            com.flaptor.util.Execute.close(os, logger);
        }
    }

    /**
     * Creates a new directory or opens an existing one.
     * @param create true to create a new directory, false otherwise.
     */
    protected void setUpDirectory(final boolean create) {
        try {
            directory = FSDirectory.getDirectory(path);
            if (create) {
                new IndexWriter(directory, analyzer, create).close();
                writeProperties();
            } else { 
                try {
                    // there must be an index there .. if there is not
                    // an index, it will throw an IOException
                    IndexReader reader = IndexReader.open(directory);
                } catch (IOException e) {
                    logger.error("Could not open IndexReader: " + e,e);
                    throw new IllegalArgumentException(e);
                }
            }
            state = State.open;
        } catch (Exception e) {
            logger.fatal("could not open index on " + path.getAbsolutePath(),e);
            throw new RuntimeException(e);
        }
    }

    private void createAnalyzer() {
        Config config = Config.getConfig("common.properties");
        String[] stopwords = config.getString("stopwords").split(",");
        logger.info("Using the following stopwords: " + java.util.Arrays.toString(stopwords));
        analyzer = new StandardAnalyzer(stopwords);
    }

    /**
     * Makes a lightweight copy of the source directory to the destination.
     * This implementation executes the command
     * <pre>cp -lpr [source] [destination]</pre>.
     * @param source the absolute path of the source directory. Must exist.
     * @param destination the absolute path of the destination directory
     */
    protected static void makeHardLinkCopy(final File source, final File destination) {
        String command = "cp -lpr " + source.getAbsolutePath() + " " + destination.getAbsolutePath();
        logger.debug("makeHardLinkCopy: \"" + command + "\"");
        CommandUtil.execute(command, null, logger);

        // copy index.properties, do not link it
        command = "rm " + destination.getAbsolutePath() + File.separator + "index.properties ";
        logger.debug("makeHardLinkCopy: removing index.properties link: \"" + command + "\"");
        CommandUtil.execute(command, null, logger);

        command = "cp " + source.getAbsolutePath() + File.separator + "index.properties " + destination.getAbsolutePath();
        logger.debug("makeHardLinkCopy: copying index.properties: \"" + command + "\"");
        CommandUtil.execute(command, null, logger);
    }


    protected Index() {
        throw new UnsupportedOperationException("");
    }

    //----------------------------------------------------------------------------
    //Internal classes
    protected static enum State {opening, open, closed};

}
