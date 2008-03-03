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
package com.flaptor.search4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;

import com.flaptor.util.Execute;



/**
 * An in-memory representation of a index, containing multiple 
 * Indexes inside.
 * 
 *  This class is useful for cases like handling a big index with
 *  lots of documents, and a very small index with news, or
 *  documents that are updated very frequently.
 * 
 * @author dbuthay
 *
 */
public class MultiIndex extends Index {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    private IndexReader reader = null;
    private List<Index> children = null;


    // Useful for composing indexes
    private MultiIndex(File path,List<Index> children) {
    	super(path);
    	this.children = children;
    	this.reader = getReader();
    }



    protected void setUpDirectory(final boolean create) {
        if (create) {
            logger.error("Asked to create a directory. I'm a MultiIndex and I can not write!");
            throw new UnsupportedOperationException("MultiIndex can not create a directory.");
        }
        
        state = State.opening;
        getReader();
        state = State.open;
    }


    /**
     * Copies this MultiIndex to a new Location. Internally, copies all its
     * children (that may be MultiIndexes as well) and writes the properties
     * pointing to the new locations of childs.
     * 
     * @return	
     * 		A MultiIndex, copied from this MultiIndex.
     */
    @Override
    public Index copyIndex(File pathOfNewIndex) {
    	if (pathOfNewIndex.exists()) {
            throw new IllegalArgumentException(pathOfNewIndex.getAbsolutePath() + " exists. I can't create a new index there.");
        }
    
    	pathOfNewIndex.mkdirs();
    	List<Index> newChildren = new ArrayList<Index>(children.size());
    	for (Index child: children) {
    		File newChildPath = new File(pathOfNewIndex,child.path.getName());
    		newChildren.add(child.copyIndex(newChildPath));
    	}
    	
    	writeProperties(pathOfNewIndex, children);
    	return new MultiIndex(pathOfNewIndex,newChildren);
    }


    
    /**
     * Gets a reader for this MultiIndex. Internally, gets readers for its 
     * children, and generates a {@link MultiReader} with them
     *  
     * @return
     * 		A {@link MultiReader} that can read on every of this index children.
     */
    @Override
    public IndexReader getReader() {
        if (state == State.closed) {
            throw new IllegalStateException("Can't get reader: the index is closed.");
        }
        
        // Special case, that will happen when using super(File) and before
        // children is assigned. in this case we return null, but will have
        // to call getReader() after assigning children.
        // THIS IS A HORRIBLE HACK
        if (null == children) {
        	logger.warn("First creation of a MultiIndex. Will have to return a null reader until I know my children.");
        	return null;
        }

        try {
            IndexReader[] readers = new IndexReader[children.size()];
            for (int i = 0; i < readers.length; i++) {
                readers[i] = children.get(i).getReader();
            }
            reader = new MultiReader(readers);
            return reader;
        } catch (IOException e) {
            logger.error(e,e);
            throw new RuntimeException("Error while getting reader: " + e.getMessage(),e);
        }
    }


    /**
     * Writes properties of this index on a file.
     * It gets an IndexDescriptor using {@link IndexDescriptor#getMultiDescriptor(String[])}},
     * and writes it to a properties file, called "index.properties" located at
     * parameter path.
     * 
     * @param path
     * 			The path where to write the generated properties file.
     * @param indexes
     * 			The indexes that will be represented in this properties file.
     */
    private static void writeProperties(File path, List<Index> indexes) {
        File propFile = new File(path.getAbsolutePath() + File.separator + "index.properties");
        OutputStream os = null;
        Properties prop = new Properties();
        String[] clusterNames = new String[indexes.size()];
        for (int i = 0; i < indexes.size() ; i++) {
        	if (null != indexes.get(i).getIndexDescriptor()) {
        		clusterNames[i] = indexes.get(i).getIndexDescriptor().getClusterName();
        		prop.setProperty(indexes.get(i).getIndexDescriptor().toString(), indexes.get(i).path.getAbsolutePath());
        	} else {
        		clusterNames[i] = null;
        	}
        }
        IndexDescriptor indexDescriptor = IndexDescriptor.getMultiDescriptor(clusterNames);
        
        try {
            os = new FileOutputStream(propFile, false); //do not append, overwrite.
            // Persist index descriptor on index.properties, if not null
            if (null == indexDescriptor) {
                logger.warn("Writing index properties on " + propFile.getAbsolutePath() + " without index descriptor");
            } else  {
                prop.setProperty("indexDescriptor",indexDescriptor.toString());   
            }
            prop.store(os, "This file is automatically generated while saving the index. Do not edit it by hand.");
        } catch (IOException e) { 
            logger.error("Exception while trying to store index.properties for index at " + path.getAbsolutePath(), e);
        }finally {
            com.flaptor.util.Execute.close(os, logger);
        }
    }

    
    /**
     * Closes this MultiIndex. Internally calls {@link MultiReader#close()},
     * that closes all its children {@link IndexReader}s.
     */
    @Override
    public void close() {
    	this.state = State.closed;
    	writeProperties(path, children);
    	Execute.close(this.reader,logger);
    }

    
    /**
     * Gets a MultiIndex, located at the parameter File, that is the 
     * composition of the indexes passed as parameter. This is the proper way 
     * to get a MultiIndex, that wraps a bunch of Indexes (no matter
     * if they are MultiIndex or Index). 
     * 
     * @param newIndexPath
     * 			The path where the MultiIndex will be created. It has to exist
     * 			in order to create the MultiIndex.
     * @param indexes
     * 			A Index[] containing the indexes to compose
     * @return
     * 			a MultiIndex, that is the composition of the parameter indexes.
     */
    public static MultiIndex compose(File newIndexPath, Index[] indexes) {
        if (!newIndexPath.exists()) {
            logger.error("Asked to generate an index in a path that does not exist.");
            throw new RuntimeException("Asked to generate an index in a path that does not exist.");
        }

        // Copy every index on indexes, so we make sure those copies do not get erased.
        List<Index> children = new ArrayList<Index>(indexes.length);
        for (Index index: indexes) {
            File destination = new File (newIndexPath,index.path.getName());
            children.add(index.copyIndex(destination));
        }
        writeProperties(newIndexPath,children);
        return new MultiIndex(newIndexPath,children);
    }

    
    
    // hide default constructor
    private MultiIndex() {
    }

}
