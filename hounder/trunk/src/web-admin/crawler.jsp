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
<%@ page 
import="com.flaptor.search4j.crawler.bean.CrawlerBean"
import="com.flaptor.search4j.crawler.bean.PatternFileBean"
import="com.flaptor.search4j.crawler.bean.CrawlerStatusBean"
import="com.flaptor.search4j.crawler.bean.PageDBExportBean"
import="com.flaptor.util.Config"
import="java.util.Map"
import="java.util.Map.Entry"
%>

<jsp:useBean id="configBean" class="com.flaptor.search4j.crawler.bean.ConfigBean" scope="session"/>

<%
//set the character encoding to use when interpreting request values 
request.setCharacterEncoding("utf-8");

String crawlerName = request.getParameter("crawlerName");
if (crawlerName == null) {
    crawlerName = "";
}
String command = request.getParameter("command");
if (command == null) {
    command = "";
}


CrawlerBean crawler = configBean.getCrawlerBean(crawlerName);
boolean success = true;
String errorMessage = null;
if (null != crawler) {
    if ("stop".equals(command)) crawler.stop();
    if ("start".equals(command)) crawler.start();
    if ("export".equals(command)) {
        PageDBExportBean peBean = crawler.getPageDBExportBean();
        if (null != peBean) {
            success = peBean.export();
            if (!success) errorMessage = peBean.getErrorString();
        }
    }

}
%> 

<% request.setAttribute("pageTitle", crawlerName); %>
<%@include file="top.include.jsp" %>

    <% if (null == crawler) {%>
        <%=crawlerName + ": No such Crawler."%>
    <%} else {%>       
		<% if (!success) { %>
     		<h2>Could not make pagedb copy: <%=errorMessage%></h2>
		<%}%>
        <table>
        	<tr>
        		<th colspan="2"><%=crawlerName%></th>
        	</tr>       
        	<tr>
        		<td>Status:</td>
        		<td><%= crawler.status().getStatus()%></td>
        	</tr>
        	<tr>
        		<td>Actions:</td>
            <%  Map<String,PatternFileBean> mapping = crawler.getPatternFileBeans();
                for (Entry<String,PatternFileBean> entry: mapping.entrySet()) {            
            %>
                <td><a href="hotspots.jsp?crawlerName=<%=crawlerName%>&patternFileParam=<%=entry.getKey()%>&command=list"><%=entry.getKey()%></a>
                </td>
            <%  }%>
        	</tr>
        	<tr>
        		<td></td>
                <td><a href="inject.jsp?crawlerName=<%=crawlerName%>">Inject URLs</a></td>
        	</tr>
        </table>
    <form name="actions" action="crawler.jsp" method="get">
	    <input type="hidden" name="crawlerName" value="<%=crawlerName%>">
		<%  CrawlerStatusBean csb = crawler.status();
	    	if (CrawlerStatusBean.state.STOPPED == csb.getStatus()) { %>
		      <input type="submit" name="command" value="start" text="START">
        	<% } else if (CrawlerStatusBean.state.RUNNING == csb.getStatus()) { %>
		      <input type="submit" name="command" value="stop" text="STOP">
	        <%}%>
        <%  // always
    	    PageDBExportBean peBean = crawler.getPageDBExportBean();
	        if (null != peBean) {%>
    	    <input type="submit" name="command" value="export" text="EXPORT">
        <%}%>
	</form>
        

   <%} // end else%>

<%@include file="bottom.include.jsp" %>
 