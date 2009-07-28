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
<%@page
import="com.flaptor.hounder.crawler.bean.CrawlerBean"
%>

<jsp:useBean id="configBean" class="com.flaptor.hounder.crawler.bean.ConfigBean" scope="session"/>
<%
// set the character encoding to use when interpreting request values 
request.setCharacterEncoding("utf-8");

 if (!configBean.isInited()) { %>
    <jsp:forward page="config.jsp"/>
<% }
String[] crawlers = configBean.getCrawlerNames();
%>

<% request.setAttribute("pageTitle", "Crawlers"); %>
<%@include file="top.include.jsp" %>

<table>
   	<tr>
   		<th>Crawler</th>
   		<th>Status</th>
	</tr>
    <% for (String crawlerName: crawlers) {%>
    <tr>
        <td><a href="crawler.jsp?crawlerName=<%=crawlerName%>"><%=crawlerName%></a></td>
        <td><%  CrawlerBean bean = configBean.getCrawlerBean(crawlerName);
                if (null == bean) { %>error<% }
                else %> <%=bean.status().getStatus()%></td>
    </tr>
    <% } // end for %>
</table>

<%@include file="bottom.include.jsp" %>
