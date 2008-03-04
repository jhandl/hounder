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
package com.flaptor.hounder.searcher.spell;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * @author Flaptor Development Team
 */
public class DidYouMeanIndexer {

	public void createSpellIndex(String field,
			Directory originalIndexDirectory,
			Directory spellIndexDirectory) throws IOException {
		
		IndexReader indexReader = null;
		try {
			indexReader = IndexReader.open(originalIndexDirectory);
			Dictionary dictionary = new Dictionary(indexReader, field);
			SpellChecker spellChecker = new SpellChecker(spellIndexDirectory);
			spellChecker.indexDictionary(dictionary);
		} finally {
			if (indexReader != null) {
				indexReader.close();
			}
		}
	}

    public static void main(String[] args) throws IOException {

        String usage = "DidYouMeanIndexer <field> <origIndex> <spellIndex>";

        if (args.length < 3) {
            System.out.println(usage);
            System.exit(-1);
        }


        String field = args[0];
        String origIndex = args[1];
        String spellIndex = args[2];

        DidYouMeanIndexer indexer = new DidYouMeanIndexer();
        FSDirectory origDir = FSDirectory.getDirectory(origIndex);
        FSDirectory spellDir = FSDirectory.getDirectory(spellIndex);

        // Call intern() on field to work around bug in LuceneDictionary
        // WTF?
        indexer.createSpellIndex(field.intern(), origDir, spellDir);		
    }

}
