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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.mortbay.jetty.handler.AbstractHandler;

import com.flaptor.hounder.searcher.filter.BooleanFilter;
import com.flaptor.hounder.searcher.filter.ValueFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.group.NoGroup;
import com.flaptor.hounder.searcher.group.StoredFieldGroup;
import com.flaptor.hounder.searcher.group.TextSignatureGroup;
import com.flaptor.hounder.searcher.query.LazyParsedQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.hounder.searcher.sort.FieldSort;
import com.flaptor.hounder.searcher.sort.ScoreSort;
import com.flaptor.util.DomUtil;
import com.flaptor.util.Execute;


/**
 * Handler for OpenSearch http queries.
 * It instantiates a Searcher and a QueryParser, and handles http queries.
 * @author Flaptor Development Team
 */
public class OpenSearchHandler extends AbstractHandler {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private final ISearcher searcher;


    /**
     * Constructor.
     * Internally constructs a new CompositeSearcher to search.
     */
    public OpenSearchHandler() {
        searcher = new CompositeSearcher();
    }
    
    /**
     * Constructor.
     * @param s a searcher to use.
     */
    public OpenSearchHandler(ISearcher s) {
        if (null == s) {
            throw new RuntimeException("OpenSearchHandler constructor: base searcher cannot be null.");
        }
        searcher = s;
    }

    /**
	 * Executes the query from the request parameters and returns the XML document
	 * 
     * Request can have the following parameters:
     *
     * query
     * start
     * hitsPerPage
     * categories
     * site
     * group = < "site" | "signature">
     * orderBy
     * crawl
     *
     */
    @SuppressWarnings("unchecked")
    public static Document doQuery(HttpServletRequest request, ISearcher searcher) throws UnsupportedEncodingException {
       return doQuery(request, searcher, request.getParameterMap());        
    }
    
    /**
     * The params is a map to String[], but we know the array has length 1, so
     * this method returns the first value for the key in the map, or null. 
     * Used to avoid checking converting String[] to String.
     * @param params
     * @param key
     * @return
     */
    private static String getParameter(Map<String,String[]> params, String key){
        if (params.containsKey(key)){
            String val=params.get(key)[0];
            //System.err.println("found " + key + "=" + val);
            return val;
        }
        //System.err.println(key + " not found ");
        return null;
    }
    
    /**
     * Similar to {@link #doQuery(HttpServletRequest, ISearcher)} but the 
     * parameters are not taken from request.getParameterMap; they are taken 
     * from the params argument.
     * This is necessary to allow a JSP modify the arguments without modifying
     * the request object (the parameters map on request in unmodifialble).
     * @param request
     * @param searcher
     * @param params
     * @return
     * @throws UnsupportedEncodingException
     */
    public static Document doQuery(HttpServletRequest request, ISearcher searcher, Map<String,String[]> params) throws UnsupportedEncodingException {
        request.setCharacterEncoding("utf-8");
        
        // Parameter processing
        int minHitsPerPage=5;
        int maxHitsPerPage=50;
        int maxOffset=1000;       // Max number of hit the results page can start with
        // parameters:
        // query (string)    the query string
        // start (int)       the offset of the first result
        // hitsPerPage (int) the number of results to be returned
        // Query String
        String queryString = getParameter(params,"query");
        if ((null == queryString) || (queryString.trim().equals(""))) {
            queryString = "";
        }
        // First hit to display
        int start = 0;        // Default value
        String startParam = getParameter(params, "start");
        if (null != startParam) {
            try {
                start = Integer.parseInt(startParam);
                if (start < 0) start = 0;
                if (start > maxOffset) start = maxOffset;
            } catch (java.lang.NumberFormatException e) {
                // ignore garbage
            }
        }

        // Number of hits to display
        int hitsPerPage = 10;
        String hitsPerPageParam = getParameter(params,"hitsPerPage");
        if (null != hitsPerPageParam) {
            try {
                hitsPerPage = Integer.parseInt(hitsPerPageParam);
                if (hitsPerPage < minHitsPerPage) hitsPerPage = minHitsPerPage;
                if (hitsPerPage > maxHitsPerPage) hitsPerPage = maxHitsPerPage;
            } catch (java.lang.NumberFormatException e) {
                //ignore garbage
            }
        }

        // orderBy param
        ASort sort = null;
        String orderByParam = getParameter(params,"orderBy");
        if ((orderByParam != null) && !"".equals(orderByParam)) {
            String[] sortingCriteria = orderByParam.split(",");
            sort = new ScoreSort();
            for (int i = (sortingCriteria.length-1); i >= 0; i--) {
                String sortingCriterion = sortingCriteria[i];
                String parts[] = sortingCriterion.toLowerCase().split(":");
                String sortField = parts[0];
                FieldSort.OrderType orderType = FieldSort.OrderType.STRING;
                boolean reverse = false;
                for (int p = 1; p < parts.length; p++) {
                	String part = parts[p];
                	if ("reverse".equals(part) || "reversed".equals(part)) {
                		reverse = true;
                		continue;
                	}
                	if ("int".equals(part)) {
                		orderType = FieldSort.OrderType.INT;
                		continue;
                	}
                	if ("long".equals(part)) {
                		orderType = FieldSort.OrderType.LONG;
                		continue;
                	}
                    if ("float".equals(part)) {
                		orderType = FieldSort.OrderType.FLOAT;
                		continue;
                	}
                }
                if ("score".equals(sortField)) {
                    sort = new ScoreSort();
                } else {
                    sort = new FieldSort(reverse, sortField, orderType, sort);
                }
            }
        }

        // Filtering

        // Categories (multi-valued)
        String[] categoriesParams = params.get("categories");
        BooleanFilter andFilter = null;
        if (categoriesParams != null) {
            andFilter = new BooleanFilter(BooleanFilter.Type.AND);
            for (String categoriesParam : categoriesParams) {
                String[] oredCategories = categoriesParam.split(",");
                BooleanFilter orFilter = new BooleanFilter(BooleanFilter.Type.OR);
                for (String oredCategory : oredCategories) {
                    orFilter.addFilter(new ValueFilter("categories", oredCategory));
                }
                andFilter.addFilter(orFilter);
            }
        }

        // Site (uni-valued)
        String siteParam = getParameter(params,"site");
        if (siteParam != null) {
            if (andFilter == null) {
                andFilter = new BooleanFilter(BooleanFilter.Type.AND);
            }
            andFilter.addFilter(new ValueFilter("site", siteParam));
        }

        // Crawl (multi-valued)
        String[] crawlParams = params.get("crawl");
        if (crawlParams != null) {
            if (andFilter == null) {
                andFilter = new BooleanFilter(BooleanFilter.Type.AND);
            }
            for (String crawlParam : crawlParams) {
                String[] oredCrawls = crawlParam.split(",");
                BooleanFilter orFilter = new BooleanFilter(BooleanFilter.Type.OR);
                for (String oredCrawl : oredCrawls) {
                    orFilter.addFilter(new ValueFilter("crawl", oredCrawl));
                }
                andFilter.addFilter(orFilter);
            }
        }

        // Group (uni-valued)
        String groupParam = getParameter(params,"group");
        AGroup group = new NoGroup();
        if (groupParam != null) {
            if (groupParam.equals("site")) {
                group = new StoredFieldGroup("site");
            } else if (groupParam.equals("signature")) {
                group = new TextSignatureGroup("text");
            }
        }
        int groupSize=1;
        String groupSizeParam = getParameter(params,"group_size");
        if (groupSizeParam != null) {
            try{
                groupSize= Integer.parseInt(groupSizeParam);
            }catch (Exception e) {
                logger.warn("Error parsing group_size", e);
                groupSize=1;
            }
        } 

        String requestUrl = request.getRequestURL().toString();
        String baseUrl = requestUrl.substring(0,requestUrl.lastIndexOf('/'));
        StringBuffer extraParams = new StringBuffer();
        extraParams.append("&hitsPerPage=");
        extraParams.append(hitsPerPage);
        if (orderByParam != null) {
            extraParams.append("&orderBy=");
            extraParams.append(orderByParam);
        }
        if (categoriesParams != null) {
            for (String p : categoriesParams) {
                extraParams.append("&categories=");
                extraParams.append(p);
            }
        }
        if (siteParam != null) {
            extraParams.append("&site=");
            extraParams.append(siteParam);
        }
        if (crawlParams != null) {
            for (String p : crawlParams) {
                extraParams.append("&crawl=");
                extraParams.append(p);
            }
        }

        GroupedSearchResults sr = null;
        int status = 0;
        String statusMessage = "OK";
        try {
            sr = searcher.search(new LazyParsedQuery(queryString), start, hitsPerPage, group, groupSize, andFilter, sort);
        } catch (SearcherException e) {
            logger.error("SEARCHING",e);
            status = 200;
            statusMessage = e.getMessage();
        	sr = new GroupedSearchResults();
        } catch (RuntimeException e) {
            logger.error("SEARCHING",e);
            status = 100;
            statusMessage = "Internal error in OpenSearchHandler: " +e.getMessage();
            sr = new GroupedSearchResults();
        }

        Document dom = OpenSearch.buildDom_1_0(baseUrl, "search.jsp", "opensearch.jsp", extraParams.toString(), queryString, start, hitsPerPage, sr, status, statusMessage);
        return dom;
    }
    

    /**
     * Request can have the following parameters:
     *
     * query
     * start
     * hitsPerPage
     * categories
     * site
     * group = < "site" | "signature">
     * orderBy
     * crawl
     *
     * this method is a merge of search-base.jsp, opensearch.jsp and http://docs.codehaus.org/display/JETTY/Embedding+Jetty
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
        Document dom = doQuery(request, searcher);
        String openSearchResults = DomUtil.domToString(dom);
        response.setContentType("text/xml");
//        response.setContentLength(openSearchResults.getBytes().length); <-- failed and was replaced in version 2721
//        response.setContentLength(openSearchResults.length()); <-- failed and was commented out in version 4408
        response.setCharacterEncoding("utf-8");
        PrintWriter pw = response.getWriter();
        pw.print(openSearchResults);
        pw.flush();
    }
}
