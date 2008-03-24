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
package com.flaptor.hounder.indexer.util;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

import com.flaptor.hounder.Index;
import com.flaptor.hounder.IndexDescriptor;

/**
 * This class implements a mechanism to split a given index into N indexes, each one containing
 * approximately size/N documents. The index is first optimized, so a backup should be made
 * if the index needs to be kept as is.
 *
 * @todo index descriptors are all the same, as indexes are copied with hardlinks.
 * @author Flaptor Development Team
 * 
 */
public final class SplitIndex {

    private static Vector<String> toOptimize;
    private static int threadsRunning = 0;

    /**
     * This thread is a worked that reads an index from a queue and optimizes it.
     */
    static class OptimizerThread extends Thread {
        public void run() {
            while(toOptimize.size() > 0) {
                IndexWriter iw = null;
                try {
                    synchronized(toOptimize) {
                        if (toOptimize.size() == 0) break;
                        String writer = (String)toOptimize.elementAt(0);
                        toOptimize.remove(0);
                        iw = new IndexWriter(writer, new org.apache.lucene.analysis.standard.StandardAnalyzer(), false);
                    }
                    System.out.println("starting optimization of " + iw.getDirectory() + " at " + new Date());
                    iw.setMaxBufferedDocs(1000);
                    iw.optimize();
                    iw.close();
                    System.out.println("finished optimization of " + iw.getDirectory() + " at " + new Date());
                }
                catch(IOException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
            threadsRunning--;
        }

    }


    /**
     * This method takes the following parameters:
     * - index: the path to the index to be split.
     * - numIndexes: how many indexes to divide it into.
     * - destDir: the directory where they will be created.
     * - hashfield: the field to use as an input to the hash function
     *   that will decide what index to put a document in (must be a stored field)
     */
    public static void main(String args[]) {
        if (args.length != 4) {
            System.err.println("usage: SplitIndex index numIndexes destDir hashfield");
            return;
        }


        String index = args[0];
        int numIndexes = Integer.parseInt(args[1]);
        Hash hashFunction = new Hash(numIndexes);
        String destDir = args[2];
        String hashfield = args[3];
        IndexReader ir[] = new IndexReader[numIndexes];
        String indexNames[] = new String[numIndexes];
        int numDocs[] = new int[numIndexes];
        try {
            for (int i = 0; i < numIndexes; i++) {
                indexNames[i] = replicateIndex(index, destDir, i);
                ir[i] = IndexReader.open(indexNames[i]);
            }

            int n = ir[0].maxDoc();
            int deleted = 0;
            for (int i = 0; i < n ; i++) {
                if (ir[0].isDeleted(i)) {
                    deleted++;
                    continue;
                }
                Document doc = ir[0].document(i);
                //see what index this document hashes to and remove it from all the others.
                String hashKey = doc.get(hashfield);
                int toIndex = hashFunction.hash(hashKey);
                numDocs[toIndex]++;
                for (int j = 0; j < numIndexes; j++) {
                    if (j != toIndex) {
                        ir[j].deleteDocument(i);
                    }
                }
                if (i % 10000 == 0) {
                    System.out.println(i + " documents seen, " + deleted + ", deleted. ");
                }
            }
            toOptimize = new Vector<String>();
            for (int i = 0; i < numIndexes; i++) {
                System.out.println("index " + i + " has " + numDocs[i] + " docs.");
                ir[i].close();
                System.out.println("optimizing index " + indexNames[i]);
                toOptimize.add(indexNames[i]);
            }
            for (int i = 0; i < numIndexes; i++) {
                System.out.println("starting optimizer thread " + i);
                new OptimizerThread().start();
                threadsRunning++;
            }
            while (threadsRunning > 0) {
                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e) {
                    System.out.println(e);
                }
            }
            // Write index descriptors
            for (int i = 0; i < numIndexes; i++) {
                Index idx = new Index(new File(indexNames[i]));
                IndexDescriptor idxDescriptor = new IndexDescriptor(numIndexes,i,"defaultCluster");
                idx.setIndexDescriptor(idxDescriptor);
                idx.setIndexProperty("lastOptimization",String.valueOf(System.currentTimeMillis()));
                idx.close();
            }
            
            
            System.out.println("done at " + new Date());
        }
        catch (IOException e) {
            System.err.println("problem splitting indexes: " + e);
            System.err.println("check output directory and perform manual cleanup");
            return;
        }
    }

    /**
     * Copies the index as a hard link, to destDir/index_[indexNumber].
     * XXX TODO: parameterize the copy command so it's not Unix-specific
     */
    private static String replicateIndex(String index, String destDir, int indexNumber) throws IOException {
        String newIndex = destDir + "/index_" + indexNumber;
        String command = "cp -lr " + index + " " + newIndex;
        try {
            Runtime.getRuntime().exec(command).waitFor();
        }
        catch (InterruptedException e) {
            throw new IOException(e.getMessage());
        }
        return newIndex;
    }
}

