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

import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

/**
 * This class implements a mechanism to merge several indexes into one.
 * The destination index will be created, or overwritten if it exists.
 * @author Flaptor Development Team
 */
public class MergeIndexes {
	public static void main(String args[]) {
		if (args.length < 3) {
			System.err.println("Usage: MergeIndexes outputIndex index1 index2 [... indexN]");
			System.exit(-1);
		}
		try {
			IndexWriter iw = new IndexWriter(args[0], new StandardAnalyzer(), true);
            iw.setMaxBufferedDocs(1000);
			IndexReader readers[] = new IndexReader[args.length -1];
			for (int i = 0; i < args.length - 1; i++) {
				readers[i] = IndexReader.open(args[i+1]);
			}
			iw.addIndexes(readers);
			iw.optimize();
			iw.close();
		}
		catch (IOException e) {
			System.err.println(e);
		}
	}
}
