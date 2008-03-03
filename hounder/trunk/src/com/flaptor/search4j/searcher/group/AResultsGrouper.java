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
package com.flaptor.search4j.searcher.group;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;

import com.flaptor.search4j.searcher.GroupedSearchResults;
import com.flaptor.util.Execute;
import com.flaptor.util.Pair;



/**
 * A Results Grouper is an object that can turn lucene TopDocs
 * into GroupedSearchResults.
 *
 * This class has all the logic to page results, construct the
 * GroupedSearchResults, and denormalize Document scores if
 * needed.
 *
 * Classes that extend this class must implement findGroup(Doc),
 * that is in which position of the vector the result should be
 * inserted into.
 *
 * @author Flaptor Development Team
 */
public abstract class AResultsGrouper {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());    

    // Groups of documents
    protected Vector<Pair<String,Vector<Document>>> docs = new Vector<Pair<String,Vector<Document>>>();
    // Scores associated with documents
    protected Vector<Vector<Float>> scores = new Vector<Vector<Float>>();
    private final DocumentProvider provider;


    public AResultsGrouper(DocumentProvider provider){
        this.provider = provider;
    }



    /**
     * Groups the TopDocs given in the constructor.
     *
     * @param groupCount
     *          Max number of groups to generate. Less groups than this
     *          parameter will be returned only if available groups are
     *          less than the parameter.
     * @param groupSize
     *          Max number of elements a group can have. If there are more than
     *          this parameter, only the first <i>groupSize</i> are kept.
     * @param offset
     *          What position on the TopDocs is the first to be checked. 
     *          Documents on TopDocs with index less tan <i>offset</i> will not
     *          be taken into consideration.
     *
     * @return GroupedSearchResults
     *
     */
    public GroupedSearchResults group(int groupCount, int groupSize, int offset) {

        int i = offset;
        try { 

             boolean denormalize;
             if (provider.getMaxScore() > 1) {
                 denormalize= true;
             } else {
                denormalize= false;
             }

            for (int j = 0, limit = provider.length(); j < groupCount  && i < limit ; i++) {


                Document doc = provider.getDocument(i);
                Pair<String,Integer> pos = findGroup(doc);

                // Check if the group exists already, or it has to be created
                if (docs.size() == pos.last()) {
                    docs.add(new Pair<String,Vector<Document>>(pos.first(),new Vector<Document>())); 
                    scores.add(new Vector<Float>());
                    // as there is a new group, count it
                    j++;
                }

                // Now, add the document on pos if there is room for it.
                if (docs.get(pos.last()).last().size() < groupSize) {
                    docs.get(pos.last()).last().add(doc);
                    if (denormalize) {
                        //logger.debug("Not Denormalizing: " + tdocs.scoreDocs[i].score);
                        scores.get(pos.last()).add(new Float(provider.getScore(i)));
                    } else {
                        //logger.debug("Denormalizing: " + tdocs.scoreDocs[i].score * tdocs.getMaxScore());
                        scores.get(pos.last()).add(new Float(provider.getScore(i) * provider.getMaxScore()));
                    }
                } // else, discard it
            }
        } catch (java.io.IOException e) {
            String s = "group: error while getting documents from the topdocs: " + e.getMessage() ;
            logger.error(s,e);
            throw new RuntimeException(e);
        }
        return new GroupedSearchResults(docs,provider.totalHits()/*tdocs.totalHits*/,offset,i,scores);
    }


    // This method has to be implemented on subclasses.
    // Given a document, the subclass has to determine in which position
    // this doc belongs, and which "label" it has.
    protected abstract Pair<String,Integer> findGroup(Document doc);
}
