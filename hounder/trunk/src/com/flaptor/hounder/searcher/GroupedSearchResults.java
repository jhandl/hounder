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
package com.flaptor.hounder.searcher;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;

import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.util.Pair;

/**
 * The result of a grouped search.
 * Contains the number of total matches of a search, the documents requested, and the scores of them.
 * @author Flaptor Development Team
 */
public final class GroupedSearchResults implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI()); 
	private final int count;

    // Which is the offset of the last checked document of this vector.
    // Useful for pagination.
    private final int offset;
	private final Vector<Pair<String,Vector<Document>>> results;
	private final Vector<Vector<Float>> scores;
    private AQuery suggestedQuery = null;
    private long responseTime = 0;

	/**
	 * Construct a new empty GroupedSearchResults.
	 *
	 */
	public GroupedSearchResults() {
		results = new Vector<Pair<String,Vector<Document>>>();
		scores = new Vector<Vector<Float>>();
		count = 0;
        offset = 0;
	}
    
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Found ")
            .append(results.size())
            .append(" results.");
        int i = 0;
        for (Pair<String,Vector<Document>> group: results) {
            buf.append("Group: ")
               .append(group.first())
               .append("\n");
                
            for (Document d : group.last()) {
                int j = 0;
                buf.append(d)
                    .append(", with score ")
                    .append(scores.elementAt(i).elementAt(j))
                    .append('\n');
                j++;
            }
            i++;
        }
        return buf.toString();
    }

	/**
	 * Constructs a SearchResults based on a vector of documents.
	 * @param documents the Vector of documents. This vector is stored internally and should not be
	 *      modified externally after calling this constructor.
	 * @param totalDocuments the size of ALL the documents resulting from the search. This parameter generally
	 *      is greater than the size of the vector and must not be lower.
	 * @param scores the scores of the documents in the first vector.
	 */
	public GroupedSearchResults(final Vector<Pair<String,Vector<Document>>> documents, final int totalDocuments, final int first, final int offset, final Vector<Vector<Float>> scores) {
		if ((null == documents) || (null == scores)) {
			String s = "The documents or the scores are null";
			logger.error(s);
			throw new IllegalArgumentException(s);            
		}


        // This check does not make sense. Maybe deep check?
        /*
		if (totalDocuments < documents.size()) {
			String s = "The total amount of documents does not match the size of the document vector (" + documents.size() + ")";
			logger.error(s);
			throw new IllegalArgumentException(s);
		}
		if (documents.size() != scores.size()) {
			String s = "The scores count (" + scores.size() + ") does not match the size of the document vector (" + documents.size() + ")";
			logger.error(s);
			throw new IllegalArgumentException(s);
		}
        */
		results = documents;
        float groupRatio = (documents.size() == 0) ? 0 : ((offset - first) / documents.size() );
		count = (0 == groupRatio) ? 0 : (int)(((float)totalDocuments )/ groupRatio);

        // IF the latest offset is the total of documents, there
        // are no more documents to check. use -1 as flag
        this.offset = (offset == totalDocuments ) ? -1 : offset;
		this.scores = scores;
	}

	/**
	 * Returns the number of document groups stored in this class.
	 * @return the number of document groups stored in this class
	 */
	public int groups() {
		return results.size();
	}

	/**
	 * Returns the total number of documents that resulted from the search that
	 * generated this result set. This number is generally greater than the 
     * number of documents stored in this object.
     *
     * This number does not take into consideration groups. It is global.
     *
	 * @return the total number of documents that matched the original query
	 */
	public int totalGroupsEstimation() {
		return count;
	}


    /**
     * Returns the offset (for the query) of the last document checked to 
     * generate this GroupedSearchResults.
     *
     * It is the last document checked to generate this object, but that 
     * document is not necesarilly in this object.
     */
    public int lastDocumentOffset() {
        return offset;
    }


	public Pair<String,Vector<Document>> getGroup(final int i) {
		return results.get(i);
	}

	public Vector<Float> getGroupScore(final int i) {
		return scores.get(i);
	}

    public void setSuggestedQuery(AQuery suggestedQuery) {
        this.suggestedQuery = suggestedQuery;
    }

    public AQuery getSuggestedQuery() {
        return suggestedQuery;
    }
    
    public void setResponseTime(long responseTime) {
    	this.responseTime = responseTime;
    }
    
    public long getResponseTime() {
    	return responseTime;
    }
    
}
