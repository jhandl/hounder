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
package com.flaptor.search4j.searcher;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.lucene.document.Field;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.flaptor.search4j.searcher.query.AQuery;
import com.flaptor.util.Config;
import com.flaptor.util.DomUtil;
import com.flaptor.util.StringUtil;

/**
 * @author Flaptor Development Team
 */
public class OpenSearch {

    private static final String XMLNS_A9_OPENSEARCH_1_0 = "http://a9.com/-/spec/opensearchrss/1.0/";
    private static final String XMLNS_SEARCH4J_OPENSEARCH_1_0 = "http://www.flaptor.com/opensearchrss/1.0/";

    private static final Set<String> fieldsToShow = new HashSet<String>();

    /**
     * OpenSearch standard, forces to have 3 fields: title, link, description
     * The name in the index of those fields might be different so we have to
     * map between the index-name and the opensearch name.
     * This field map the 'description' field
     */
    private static final String descriptionField;
    private static final String linkField;
    private static final String titleField;

    static {
        Config config = Config.getConfig("opensearch.properties");
        String[] fieldList = config.getStringArray("opensearch.show.search4j.fields");
        titleField = config.getString("opensearch.title.from.index.field");
        linkField = config.getString("opensearch.link.from.index.field");
        descriptionField = config.getString("opensearch.description.from.index.field");    	
        fieldsToShow.addAll(Arrays.asList(fieldList));
    }

    /**
     * Private empty default constructor to prevent inheritance and instantiation.
     */
    private OpenSearch() {}

    /**
     * Creates a OpenSearch's compatible DOM document.
     * The generated dom contains only valid xml characters (infringing chars are removed).
     * Compliant with OpenSearch 1.0 with most of the Nutch 0.8.1 extensions.
     * @param baseUrl the url of the webapp
     * @param htmlSearcher the name of the component (servlet/jsp) that returns the search results in an HTML page
     * @param opensearchSearcher the name of the component (servlet/jsp) that returns the search results in an OpenSearch RSS page
     * @param extraParams the parameters present in the request, not passed explicitly (such as sort, reverse, etc.)
     * @param queryString the query string, as entered by the user
     * @param start the offset of the first result
     * @param count the number of results requested (the actual number of results found may be smaller)
     * @param sr the SearchResults structure containing the result of performing the query
     * @return a DOM document
     * <br>An empty sr argument means that no results were found.
     */
    public static final Document buildDom_1_0(String baseUrl, String htmlSearcher, String opensearchSearcher, String extraParams, String queryString, int start, int count, GroupedSearchResults sr, int status, String statusMessage) {

        String encodedQuery = null;
        try {
            encodedQuery = URLEncoder.encode(queryString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Should never happen!
            encodedQuery = "";
        }
        Document dom = DocumentHelper.createDocument();
        Namespace opensearchNs = DocumentHelper.createNamespace("opensearch", XMLNS_A9_OPENSEARCH_1_0);
        Namespace search4jNs = DocumentHelper.createNamespace("search4j", XMLNS_SEARCH4J_OPENSEARCH_1_0);
        Element root = dom.addElement("rss").
        addAttribute("version", "2.0");
        root.add(opensearchNs);
        root.add(search4jNs);
        Element channel = root.addElement("channel");
        channel.addElement("title").addText("Search4j: " +DomUtil.filterXml(queryString));
        channel.addElement("link").addText(baseUrl + "/" + htmlSearcher +
                "?query=" +encodedQuery + "&start=" + start + extraParams);
        channel.addElement("description").addText("Search4j search results for query: " + DomUtil.filterXml(queryString));
        channel.addElement(QName.get("totalResults", opensearchNs)).addText(Integer.toString(sr.totalGroupsEstimation()));
        channel.addElement(QName.get("startIndex", opensearchNs)).addText(Integer.toString(start));
        channel.addElement(QName.get("itemsPerPage", opensearchNs)).addText(Integer.toString(count));
        channel.addElement(QName.get("query",search4jNs)).addText(DomUtil.filterXml(queryString));
        AQuery suggestedQuery = sr.getSuggestedQuery();
        if (null != suggestedQuery) {
            channel.addElement(QName.get("suggestedQuery",search4jNs)).addText(DomUtil.filterXml(suggestedQuery.toString()));
        }
        channel.addElement(QName.get("status",search4jNs)).addText(Integer.toString(status));
        channel.addElement(QName.get("statusDesc",search4jNs)).addText(statusMessage);
        if (sr.lastDocumentOffset() > 0) {
            channel.addElement(QName.get("nextPage",search4jNs)).addText(baseUrl + "/" + opensearchSearcher + 
                    "?query=" + encodedQuery + "&start=" + (sr.lastDocumentOffset()) + extraParams);
        }

        for (int i=0; i< sr.groups(); i++) {
            Vector<org.apache.lucene.document.Document> docs= sr.getGroup(i).last();
            Element parent= null;
            for (int j = 0; j < docs.size(); j++) {
                org.apache.lucene.document.Document doc = sr.getGroup(i).last().get(j);                
                if (0 == j) {// j=0 is head of group. j>0 is tail
                    parent= createAndAddElement(doc, channel, search4jNs);
                } else {
                    createAndAddElement(doc, parent, search4jNs);
                }

            }
        }
        return dom;
    }

    private static Element createAndAddElement( org.apache.lucene.document.Document doc, 
            Element parent, Namespace search4jNs){
        String url= StringUtil.nullToEmpty(doc.get(linkField)).trim();
        String description= StringUtil.nullToEmpty(doc.get(descriptionField)).trim();
        String title= StringUtil.nullToEmpty(doc.get(titleField)).trim();           
        if ("".equals(title)) {
            title=url;
        }            

        Element item = parent.addElement("item");            
        item.addElement("title").addText(DomUtil.filterXml(title));
        item.addElement("link").addText(DomUtil.filterXml(url));
        String desc = DomUtil.filterXml(description);
        item.addElement("description").addText(desc);

        for (Iterator iter = doc.getFields().iterator(); iter.hasNext(); ) {
            Field f = (Field) iter.next();
            if (fieldsToShow.contains(f.name())) {
                item.addElement(QName.get(f.name(),search4jNs)).addText(DomUtil.filterXml(f.stringValue()));
            }
        }
        return item;
    }
}

