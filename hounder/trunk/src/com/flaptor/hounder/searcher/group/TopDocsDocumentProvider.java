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
package com.flaptor.hounder.searcher.group;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;

/**
 * @author Flaptor Development Team
 */
public class TopDocsDocumentProvider implements DocumentProvider {

    private final TopDocs tdocs;
    private final Searcher searcher;

    public TopDocsDocumentProvider( TopDocs tdocs, Searcher searcher) {
        this.searcher = searcher;
        this.tdocs = tdocs;
    }

    public int totalHits() {
        return tdocs.totalHits;
    }

    public Document getDocument(int i) throws IOException {
        return searcher.doc(tdocs.scoreDocs[i].doc);
    }

    public float getScore(int i) {
        return tdocs.scoreDocs[i].score;
    }

    public float getMaxScore() {
        return tdocs.getMaxScore();
    }

    public int length() {
        return tdocs.scoreDocs.length;
    }
}
