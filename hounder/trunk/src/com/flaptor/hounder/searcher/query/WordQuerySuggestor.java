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
package com.flaptor.hounder.searcher.query;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.store.FSDirectory;

import com.flaptor.hounder.searcher.spell.SpellChecker;


/**
 * Suggests queries based on a SpellChecker, created
 * from a dictionary
 *  
 *  @author Martin Massera
 */
public class WordQuerySuggestor extends AQuerySuggestor {
    
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());
    
    /**
     * Creates a WordQuerySuggestor, based on a SpellChecker,
     * from the given dictionary directory
     *
     * @param dictionaryDir
     *          A directory that contains an N-Gram index, for
     *          the underlying SpellChecker
     */
    public WordQuerySuggestor(File dictionaryDir) throws IOException{
        super(new SpellChecker(FSDirectory.getDirectory(dictionaryDir)));
    }
}
