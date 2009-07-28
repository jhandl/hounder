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

import java.util.Iterator;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;

import org.apache.lucene.document.Field;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.query.LazyParsedQuery;
import com.flaptor.util.DomUtil;

/**
 * @author Flaptor Development Team
 */
public class XmlResults {


    /**
     * Private empty default constructor to prevent inheritance and instantiation.
     */
    private XmlResults() {}

    /**
     * Creates an XML search results document.
     * @param sr the GroupedSearchResults structure containing the result of performing the query.
     * @param status the code returned by the searcher.
     * @param statusMessage the status description.
     * @return a DOM document.
     */
    public static final Document buildXml(GroupedSearchResults sr, int status, String statusMsg) {
        return buildXml(null,0,0,null,sr,status,statusMsg,null,null,null,null,null);
    }
    
    /**
     * Creates a XML search results document (verbose version).
     * The generated dom contains only valid xml characters (infringing chars are removed).
     * @param queryString the query string, as entered by the user
     * @param start the offset of the first result
     * @param count the number of results requested (the actual number of results found may be smaller)
     * @param orderBy the field by which the results are sorted
     * @param sr the GroupedSearchResults structure containing the result of performing the query
     * @param status the code returned by the searcher
     * @param statusMsg the status description
     * @param xsltUri the uri for the xslt used to process the xml on the client side, 
     *          or null if no client-side processing is needed
     * @param rangeField field for which a range filter will be applied, or null if no filter used.
     * @param rangeStart start value for the range filter.
     * @param rangeEnd end value for the range filter.
     * @param params a map of parameters sent to the searcher with the request.
     * @return a DOM document
     * <br>An empty sr argument means that no results were found.
     */
    public static final Document buildXml(String queryString, int start, int count, String orderBy, GroupedSearchResults sr, int status, String statusMsg, String xsltUri, String rangeField, String rangeStart, String rangeEnd, Map<String,String[]> params) {

        Document dom = DocumentHelper.createDocument();
        if (null != xsltUri) {
            Map<String,String> map = new HashMap<String,String>();
            map.put("type", "text/xsl");
            map.put("href", xsltUri);
            dom.addProcessingInstruction("xml-stylesheet", map);
        }
        Element root;
        Element group;
        root = dom.addElement("SearchResults");
        root.addElement("totalResults").addText(Integer.toString(sr.totalResults()));
        root.addElement("totalGroupsEstimation").addText(Integer.toString(sr.totalGroupsEstimation()));
        if (count > 0) { root.addElement("startIndex").addText(Integer.toString(start)); }
        if (count > 0) { root.addElement("itemsPerPage").addText(Integer.toString(count)); }
        if (null != orderBy) { root.addElement("orderBy").addText(DomUtil.filterXml(orderBy)); }
        if (null != queryString) { root.addElement("query").addText(DomUtil.filterXml(queryString)); }
        if (null != rangeField) {
            root.addElement("filter").
                    addAttribute("field",rangeField).
                    addAttribute("start",rangeStart).
                    addAttribute("end",rangeEnd);
        }
        if (null != params) {
            for (String key : params.keySet()) {
                if (null == root.selectSingleNode(key)) {
                    String val = params.get(key)[0];
                    root.addElement(key).addText(val);
                }
            }
        }
        AQuery suggestedQuery = sr.getSuggestedQuery();
        if (null != suggestedQuery) {
            root.addElement("suggestedQuery").addText(DomUtil.filterXml(((LazyParsedQuery)suggestedQuery).getQueryString()));
        }
        root.addElement("status").addText(Integer.toString(status));
        root.addElement("statusDesc").addText(statusMsg);

        for (int i=0; i<sr.groups(); i++) {
            String name = sr.getGroup(i).first();
            group = root.addElement("group").addAttribute("name", name);
            Vector<org.apache.lucene.document.Document> docs = sr.getGroup(i).last();
            for (int j = 0; j < docs.size(); j++) {
                org.apache.lucene.document.Document doc = sr.getGroup(i).last().get(j);                
                createAndAddElement(doc, group);
            }
        }
        return dom;
    }

    private static Element createAndAddElement(org.apache.lucene.document.Document doc, Element parent) {
        Element item = parent.addElement("result");            
        for (Iterator iter = doc.getFields().iterator(); iter.hasNext(); ) {
            Field f = (Field) iter.next();
            item.addElement(f.name()).addText(f.stringValue());
        }
        return item;
    }

}

