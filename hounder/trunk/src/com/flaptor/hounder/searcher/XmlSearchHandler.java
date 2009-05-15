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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;


import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.io.DocumentSource;
import org.mortbay.jetty.handler.AbstractHandler;

import com.flaptor.hounder.searcher.filter.BooleanFilter;
import com.flaptor.hounder.searcher.filter.ValueFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.group.NoGroup;
import com.flaptor.hounder.searcher.group.StoredFieldGroup;
import com.flaptor.hounder.searcher.group.TextSignatureGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.query.AndQuery;
import com.flaptor.hounder.searcher.query.LazyParsedQuery;
import com.flaptor.hounder.searcher.query.PayloadQuery;
import com.flaptor.hounder.searcher.query.RangeQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.hounder.searcher.sort.FieldSort;
import com.flaptor.hounder.searcher.sort.ScoreSort;
import com.flaptor.util.Config;
import com.flaptor.util.DomUtil;
import com.flaptor.util.Execute;
import com.flaptor.util.Pair;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


/**
 * Handler for OpenSearch http queries.
 * It instantiates a Searcher and a QueryParser, and handles http queries.
 * @author Flaptor Development Team
 */
public class XmlSearchHandler extends AbstractHandler {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private final ISearcher searcher;
    private Map<String, Pair<Transformer, String>> transformMap;
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss");
    
    /**
     * Constructor.
     * Internally constructs a new CompositeSearcher to search.
     */
    public XmlSearchHandler() {
        this(new CompositeSearcher());
    }
    
    /**
     * Constructor.
     * It gets form the configuration file (searcher.properties) the variable "XmlSearchHandler.transformMap"
     * and interprets it as a comma separated list of semicolon separated triads. Each triad has (urlPath,
     * contentType, xsltFilePath).
     * @param s the base searcher to use.
     */
    public XmlSearchHandler(ISearcher searcher) {
        if (null == searcher) {
            throw new RuntimeException("OpenSearchHandler constructor: base searcher cannot be null.");
        }
        this.searcher = searcher;
        Config config = Config.getConfig("searcher.properties");
        transformMap = new HashMap<String, Pair<Transformer, String>>();
        String[] mappings = config.getStringArray("xmlsearch.transformMap");
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
        TransformerFactory tFactory = TransformerFactory.newInstance();
        for ( String mapping : mappings) {
            String[] parameters = mapping.split(";");
            if (parameters.length != 3) {
                throw new IllegalArgumentException("Invalid format in XmlSearchHandler.transformMap.");
            }
            String subPath = parameters[0].trim();
            String contentType = parameters[1].trim();
            String xmltPath = parameters[2].trim();
            Transformer transformer;
            try {
                transformer = tFactory.newTransformer(new StreamSource(xmltPath));
            } catch (TransformerConfigurationException e) {
                logger.error("constructor: compiling the xsl transformation.");
                throw new IllegalArgumentException(e);
            }
            transformMap.put(subPath, new Pair<Transformer, String>(transformer, contentType));
        }
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
     * xsltUri
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
        int minHitsPerPage=3;
        int maxHitsPerPage=50;
        int maxOffset=1000;       // Max number of hit the results page can start with
        // parameters:
        // query (string)    the query string
        // start (int)       the offset of the first result
        // hitsPerPage (int) the number of results to be returned
        // orderBy (string)  the order in which to return the results: <field>:(int|long|float)[:reverse]
        // tz (int)	         the timezone for displaying the date
        
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
                String parts[] = sortingCriterion.split(":");
                String sortField = parts[0];
                FieldSort.OrderType orderType = FieldSort.OrderType.STRING;
                boolean reverse = false;
                for (int p = 1; p < parts.length; p++) {
                	String part = parts[p].toLowerCase();
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
        
        // Range filter
        String rangeParam = getParameter(params,"range");
        String rangeField = null;
        String rangeStart = null;
        String rangeEnd = null;
        if (null != rangeParam) {
            String parts[] = rangeParam.split(":");
            if (parts.length > 1) {
                rangeField = parts[0];
                String limits[] = parts[1].split("-");
                if (limits.length > 1) {
                    rangeStart = limits[0];
                    rangeEnd = limits[1];
                }
            }
        }

        // Date filter
        String pastParam = getParameter(params,"past");
        if (null != pastParam) {
            String parts[] = pastParam.split(":");
            if (parts.length > 2) {
                rangeField = parts[0];
                int days = Integer.parseInt(parts[1]);
                SimpleDateFormat dateFormatter = new SimpleDateFormat(parts[2]);
                Calendar cal = new GregorianCalendar();
                rangeEnd = dateFormatter.format(cal.getTime());
                cal.add(Calendar.DAY_OF_YEAR, -days);
                rangeStart = dateFormatter.format(cal.getTime());
            }
        }




        // Group (uni-valued)
        String groupParam = getParameter(params,"groupBy");
        AGroup group = new NoGroup();
        if (groupParam != null) {
            if (groupParam.equals("signature")) {
                group = new TextSignatureGroup("text");
            } else {
                group = new StoredFieldGroup(groupParam);
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

        // Timezone (uni-valued)
        int timezone = 0;
        String tzParam = getParameter(params, "tz");
        if (tzParam != null) {
            try {
                timezone = Integer.parseInt(tzParam);
            } catch (Exception e) {
                logger.warn("Error parsing timezone", e);        		
            }
        }

        // Payloads
        String payloadParam = getParameter(params,"payload");
        String[] payloadFields = null;
        if (null != payloadParam) {
            payloadFields = payloadParam.split(",");
        }


        //If useXsltStr is null, it means we should not include the directive to transform the
        //xml with an xslt
        String xsltUri = getParameter(params, "xsltUri");

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
        if (tzParam != null) {
            extraParams.append("&tz=");
            extraParams.append(tzParam);
        }

        GroupedSearchResults sr = null;
        int status = 0;
        String statusMessage = "OK";
        try {
            AQuery query = new LazyParsedQuery(queryString);
            if (null != payloadFields) {
                for (String fieldName : payloadFields) {
                    query = new AndQuery(query, new PayloadQuery(fieldName));
                }
            }
            if (null != rangeField) {
                query = new AndQuery(query, new RangeQuery(rangeField,rangeStart,rangeEnd,true,true));
            }
            sr = searcher.search(query, start, hitsPerPage, group, groupSize, andFilter, sort);
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
        System.out.println("Q "+formatter.format(new Date())+" "+sr.totalResults()+" "+queryString);
 
        Document dom = XmlResults.buildXml(queryString, start, hitsPerPage, orderByParam, sr, status, statusMessage, xsltUri, rangeField, rangeStart, rangeEnd, params);
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
     * xsltUri
     * raw true|false
     * this method is a merge of search-base.jsp, opensearch.jsp and http://docs.codehaus.org/display/JETTY/Embedding+Jetty
     */
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
        response.setCharacterEncoding("utf-8");
        PrintWriter pw = response.getWriter();

        Document originalDom = doQuery(request, searcher);
        originalDom.getRootElement()
                .addAttribute("SearchEngine","Hounder (hounder.org)")
                .addAttribute("DevelopedBy","Flaptor (flaptor.com)");

        @SuppressWarnings("unchecked")
        String rawStr = getParameter(request.getParameterMap(), "raw");
        if (Boolean.parseBoolean(rawStr) || transformMap.isEmpty()) {
            response.setContentType("text/xml");
            String openSearchResults = DomUtil.domToString(originalDom);
            pw.print(openSearchResults);
            pw.flush();
        } else {
            Pair<Transformer, String> value = transformMap.get(request.getPathInfo());
//System.out.println("XML HANDLE: path="+request.getPathInfo()+"  value="+value);
            if (null == value) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "There's no xslt to serve this context.");
                return;
            }
            Transformer transformer = value.first();
            String contentType = value.last();

            response.setContentType(contentType);
            try {
                synchronized(transformer) {
                    transformer.transform(new DocumentSource(originalDom), new StreamResult(pw));
                }
            } catch (TransformerException e) {
                logger.error("internalProcess: exception while transforming document. (set error level to debug to see the offending document)", e);
                logger.debug("offending document was: " + DomUtil.domToString(originalDom));
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing internal xslt.");
                return;
            }
        }
    }
}
