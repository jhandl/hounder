<%-- 
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
--%>
<%@page import="com.flaptor.hounder.searcher.OpenSearchHandler"%>
<%@page import="com.flaptor.hounder.searcher.WebSearchUtil"%>
<%@page import="org.dom4j.Document"%>
<%@page import="com.flaptor.util.DomUtil"%>
<%@page import="org.dom4j.Element"%>
<%@page import="com.flaptor.hounder.cache.CachedVersionUtil"%>
<%@page import="java.util.Map"%>

<jsp:include page="head.jsp"/>
<%@ include file="pagination.jsp" %>
<%@ include file="result.jsp" %>
<%@ include file="query.jsp" %>

<%
addQueryBox(request,out);
if (request.getParameter("query")!= null) {      
	Document results = OpenSearchHandler.doQuery(request, WebSearchUtil.getSearcher());
	Element parentElement = results.getRootElement().element("channel");

    int status = Integer.parseInt(DomUtil.getElementText(parentElement, "status"));
    String statusDesc = DomUtil.getElementText(parentElement, "statusDesc");
	if (status == 0) {	    
	    printGoToLinks(parentElement, request, out);
	    out.write("<br/><br/>");
    	printResults(parentElement,out);
	    out.write("<br/><br/>");
	    addQueryBox(request,out);
	    printGoToLinks(parentElement, request, out);
    } else {
        out.write(statusDesc);
    }
}%>

<jsp:include page="bottom.jsp"/>
