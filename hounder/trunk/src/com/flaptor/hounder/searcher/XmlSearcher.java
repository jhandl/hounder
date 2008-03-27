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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.filter.BooleanFilter;
import com.flaptor.hounder.searcher.filter.RangeFilter;
import com.flaptor.hounder.searcher.filter.ValueFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.group.NoGroup;
import com.flaptor.hounder.searcher.group.StoredFieldGroup;
import com.flaptor.hounder.searcher.group.TextSignatureGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.query.LazyParsedQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.hounder.searcher.sort.FieldSort;
import com.flaptor.hounder.searcher.sort.ScoreSort;
import com.flaptor.util.Execute;
import com.flaptor.util.Pair;

/**
 * This class implements a searcher that works within an xmlrpc server by converting
 * the results into a portable structure.
 * @author Flaptor Development Team
 */
public class XmlSearcher {

	private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    private final ISearcher searcher;
    
    /**
     * Constructor.
     * Creates a new CompositeSearcher to use.
     */
    public XmlSearcher() {
        searcher = new CompositeSearcher();
    }
    
    /**
     * Constructor.
     * Uses the provided searcher.
     * @param s the searcher to use.
     */
    public XmlSearcher(ISearcher s) {
        if (null == s) {
            throw new IllegalArgumentException("baseSearcher cannot be null.");
        }
        searcher = s;
    }
    
    
	/**
	 * This method returns the search results packed in a Vector, so that the xml server
	 * can understand them and return them to the client.
	 * @param queryStr the query.
	 * @param firstResult the number of the first document to return. The first document is the number 0.
	 * @param count how many documents to return.
     * @param dedupBy 
     *          a Vector of Strings, representing the group. The Vector can be
     *          null, indicating no dedup, or a 2 String Vector, with the first 
     *          component being either "STORED" or "SIGNATURE". The second 
     *          component is the field to use to dedup (to check for equality
     *          or similarity, respectively). 
     *          The vector can also be a one element vector, only if "SIGNATURE" 
     *          is the element stored, and in that case a Signature dedup will
     *          occur, over the "text" field (default).
     *          
	 * @param filters a Vector of Vectors of Strings representing the filters. The outer Vector represents the
	 * 		several filters to be 'and'ed to build the final flter. The inner Vectors may contain 2 or 3 Strings.
	 * 		If the Vector contains 2 Strings, it represents a value filter, where the field is the first String
	 * 		and the value the 2nd. If it contains 3 Strings, it's interpreted as a RangeFilter as (Field, from,
	 * 		to). Both a null parameter and a zero length list are valid and mean no filtering.
	 * @param sort a vector of vectors of Strings. The inner vectors all have to have exactly 3 Strings. The
	 * 		the first one is the name of the field. The reserved string "SORT-BY-RELEVANCE" means that the search
	 *		relevance will be used for the sort. The second either "true" or "false" and is meant to reverse
	 * 		the sort order. The last one is either "INT", "STRING" or "FLOAT", and indicates the type of sort
	 *		(ie: numeric or diccionary order) to perform.
	 * 		On the outer vector, both a null parameter and a zero length list are valid and mean the default
	 * 		sort (relevance) will be used.
	 */

    /**
     * Performs a search, grouping results instead of ignoring "duplicates". 
     * Expects same parameters as xmlsearch that dedups.
     * Groups have at most groupSize elements.
     *
	 * @param queryStr 
     *          the query.
	 * @param firstResult 
     *          the number of the first document to return. 
     *          The first document is the number 0.
	 * @param count 
     *          how many documents to return.
     * @param dedupBy 
     *          a Vector of Strings, representing the group. The Vector can be
     *          null, indicating no dedup, or a 2 String Vector, with the first 
     *          component being either "STORED" or "SIGNATURE". The second 
     *          component is the field to use to dedup (to check for equality
     *          or similarity, respectively). 
     *          The vector can also be a one element vector, only if "SIGNATURE" 
     *          is the element stored, and in that case a Signature dedup will
     *          occur, over the "text" field (default).
     *          
	 * @param filters 
     *          a Vector of Vectors of Strings representing the filters. 
     *          The outer Vector represents the several filters to be 
     *          'and'ed to build the final flter. The inner Vectors may 
     *          contain 2 or 3 Strings.
	 * 		    If the Vector contains 2 Strings, it represents a value 
     * 		    filter, where the field is the first String and the value 
     * 		    the 2nd. If it contains 3 Strings, it's interpreted as a 
     * 		    RangeFilter as (Field, from,to). Both a null parameter and 
     * 		    a zero length list are valid and mean no filtering.
	 * @param sort 
     *          a vector of vectors of Strings. The inner vectors all have 
     *          to have exactly 3 Strings. The first one is the name of the 
     *          field. The reserved string "SORT-BY-RELEVANCE" means that the 
     *          search relevance will be used for the sort. The second either
     *          "true" or "false" and is meant to reverse the sort order. 
     *          The last one is either "INT", "STRING" or "FLOAT", and 
     *          indicates the type of sort (ie: numeric or diccionary order) 
     *          to perform.
	 * 		    On the outer vector, both a null parameter and a zero length 
     * 		    list are valid and mean the default sort (relevance) will be 
     * 		    used.
     *
     * @param groupSize
     *          the max allowed size of a group. If more results than 
     *          groupSize are found, only the first groupSize are kept,
     *          and the rest is discarded.
     *
     * @return  a Vector representation of GroupedSearchResults, 
     *          or a vector with a message about the exception thrown.
     * @see groupedSearchResultsToVector(GroupedSearchResults)
     *
     *
     */
    public Vector xmlsearch(final String queryStr, final int firstResult, final int groupCount, final Vector groupBy, final int groupSize,final Vector filters, final Vector sort) {
		logger.debug("Search request received in xmlsearch: query \"" + queryStr + "\", first result " + firstResult + ", groupCount " + groupCount);
		String message = "error: ";
		try {
            AQuery query = new LazyParsedQuery(queryStr);
			GroupedSearchResults gsr = searcher.search(query, firstResult, groupCount, generateGroup(groupBy),groupSize, generateFilters(filters), generateSort(sort));
			logger.debug("Ready to return response: " + gsr);
			return groupedSearchResultsToVector(gsr);
		} catch (SearcherException e) {
			message += "searcher exception - " + e.getMessage();
			logger.warn("Searcher exception", e);
		}
		catch (IllegalArgumentException e) {
			message += "Search called with illegal arguments - " + e.getMessage();
			logger.warn("Search called with illegal arguments. Returning null.", e);
		} catch (Throwable t) {
			message += "unexpected exception  - " + t.getMessage();
			logger.error("Unknown exception called. Returning null.", t);
		}
		Vector<String> ret = new Vector<String>();
		ret.add(message);
		return ret;
	}





	/**
	 * Returns a Hounder sort from the Vector representation of filters.
	 * @param sortVec a vector of vectors of Strings. The inner vectors all have to have exacltly 3 integers. The
	 * 		the first one is the name of the field. The reserved string "SORT-BY-RELEVANCE" means that the search
	 *		relevance will be used for the sort. The second either "true" or "false" and is meant to reverse
	 * 		the sort order. The last one is either "INT", "STRING" or "FLOAT", and indicates the type of sort
	 *		(ie: numeric or diccionary order) to perform.
	 * 		On the outer vector, both a null parameter and a zero length list are valid and mean the default
	 * 		sort (relevance) will be used.
	 */
	protected ASort generateSort(final Vector sortVec) {
		if (null == sortVec || 0 == sortVec.size()) {
			return null;
		} else {
			ASort lastSort = null;
			for (int i = sortVec.size() - 1 ; i >=  0; i--) {
				lastSort = generateSingleSort((Vector) sortVec.elementAt(i), lastSort);
			}
			return lastSort;
		}
	}

	/**
	 * Helper method for generateSort.
	 */
	private ASort generateSingleSort(final Vector vec, final ASort subSort) {
		String name = (String) vec.elementAt(0);
		ASort sort;
		if (name.equals("SORT-BY-RELEVANCE")) {
			if (null == subSort) {
				sort = new ScoreSort();
			} else {
				sort = new ScoreSort(subSort);
			}
		} else {
			String reverse = (String) vec.elementAt(1);
			String typeStr = (String) vec.elementAt(2);
			FieldSort.OrderType type = FieldSort.OrderType.STRING;
			if (typeStr.equals("FLOAT")) {
				type = FieldSort.OrderType.FLOAT;
			} else if (typeStr.equals("INT")) {
				type = FieldSort.OrderType.INT;
			} else if (typeStr.equals("LONG")) {
				type = FieldSort.OrderType.LONG;
			}
			if (null == subSort) {
				sort = new FieldSort(reverse.equals("true"), name, type);
			} else {
				sort = new FieldSort(reverse.equals("true"), name, type, subSort);
			}
		}
		return sort;
	}


    /**
     * Returns a Hounder group from a vector representation of a group.
     *
     * The valid formats are 
     *
     * [ "STORED", "FIELDNAME"], ["SIGNATURE"] and [ "SIGNATURE","FIELDNAME"].
     *
     *
     * If parameter does not match any of this formats, no grouping is made
     */
    private AGroup generateGroup(final Vector vec) {
        if (null == vec || vec.size() == 0) 
            return new NoGroup();


        try {
            if ("SIGNATURE".equals((String)vec.get(0))) {
                if (vec.size() == 2 ) {
                    String field = (String)vec.get(1);
                    return new TextSignatureGroup(field);
                } else {
                    return new TextSignatureGroup();
                }
            } else if ("STORED".equals((String)vec.get(0))){
                if (vec.size() != 2) {
                    throw new IllegalArgumentException("Wrong parameter count: \"STORED FIELDNAME\" needed. ");
                }
                return new StoredFieldGroup((String)vec.get(1));
            } else {
                throw new IllegalArgumentException("Supported arguments are \"null\", [ \"STORED\", \"FIELDNAME\"], [\"SIGNATURE\"] and [ \"SIGNATURE\",\"FIELDNAME\"].");
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Supported arguments are \"null\", [ \"STORED\", \"FIELDNAME\"], [\"SIGNATURE\"] and [ \"SIGNATURE\",\"FIELDNAME\"].");
        }

    }



	/**
	 * Returns a Hounder filter from the Vector representation of filters.
	 * @param filters a Vector of Vector of string representing the filters. The outer Vector represents the
	 * several filters to be 'and'ed to build the final flter. The inner lists may contain 2 or 3 Strings.
	 * If the Vector contains 2 Strings, it represents a value filter, where the field is the first String
	 * and the value the 2nd. If it contains 3 Strings, it's interpreted as a RangeFilter as (Field, from,
	 * to). Both a null parameter and a zero length list are valid and mean no filtering.
	 * @return A Hounder filter, or null.
	 */
	protected AFilter generateFilters(final Vector filters) {
		if (null == filters) {
			return null;
		} else {
			BooleanFilter andedFilter = new BooleanFilter(BooleanFilter.Type.AND);
			int filterCounter = 0;
			for (Iterator iter = filters.iterator(); iter.hasNext();) {
				List filter;
				try {
					filter = (Vector) iter.next();
				} catch (ClassCastException e) {
					String s = "generateFilters: the outer list contains an object that is not a list." + e;
					logger.error(s);
					throw new IllegalArgumentException(s);
				}
				int stringCounter = 0;
				String[] s = new String[3];
				for (Iterator iter2 = filter.iterator(); iter2.hasNext();) {
					try {
						s[stringCounter] = (String) iter2.next();
					} catch (ClassCastException e) {
						String m = "generateFilters: the inner list " + filterCounter + " contains a non String object at position " + stringCounter;
						logger.error(m);
						throw new IllegalArgumentException(m);
					}
					stringCounter++;
					if (stringCounter > 2) {
						break;
					}
				}
				if (stringCounter == 2) {
					andedFilter.addFilter(new ValueFilter(s[0], s[1]));
				} else if (stringCounter == 3) {
					andedFilter.addFilter(new RangeFilter(s[0], s[1], s[2]));
				} else {
					String m = "generateFilters: inner list number " + filterCounter + " contains " + stringCounter + " strings.";
					logger.error(m);
					throw new IllegalArgumentException(m);
				}
				filterCounter++;
			}
			if (filterCounter == 0) {
				return null;
			} else {
				return andedFilter;
			}
		}
	}

	/**
	 * This method returns a vector representation of the results.
	 * @param s the GroupedSearchResults to convert to a vector.
	 * @return a vector representation of the search Results. The format is:
	 * first element: the number of documents in this set
	 * second element: the estimation of the total number of groups matching the query (totalGroupsEstimation)
     * third: the offset of the last document checked. useful to know offset for next query, and -1 if there are no more results.
	 * fourth: the total number of hits matching the query(no matter grouping)
	 * fifth: suggested query
     * sixth and onwards : a vector of the names and values of each field in the document, stored as map&lt;field,value&gt;
	 * This method is meant to be used by the xmlrpc server, which has a limited repertoire of objects that it can return.
	 **/
    public static final Vector groupedSearchResultsToVector(final GroupedSearchResults gsr){
        Vector<Object> v = new Vector<Object>(2 + gsr.groups());
        v.add(gsr.groups());
        v.add(gsr.totalGroupsEstimation());
        v.add(gsr.lastDocumentOffset());
        v.add(gsr.totalResults());
        v.add(""); // TODO put suggested queries here, if available

        for (int i = 0; i < gsr.groups(); i++) {
            Pair<String,Vector<Document>> group = gsr.getGroup(i);
            Vector<Object> vGroup = new Vector<Object>(group.last().size() + 1);
            vGroup.add(group.first());
            for (Document d: group.last()) {
                Hashtable<String,String> fh = new Hashtable<String,String>();
                for (Iterator iter = d.getFields().iterator(); iter.hasNext();) {
                    Field f = (Field) iter.next();
                    fh.put(f.name(),f.stringValue());
                }
                vGroup.add(fh);
            }
            v.add(vGroup);
        }
        return v;
    }
}
