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
import="com.flaptor.hounder.crawler.bean.CrawlerBean"
import="com.flaptor.hounder.crawler.bean.PatternFileBean"
import="com.flaptor.hounder.crawler.bean.PatternFileBean.PatternFileBeanException"
import="java.util.Set"
%>

<jsp:useBean id="configBean" class="com.flaptor.hounder.crawler.bean.ConfigBean" scope="session"/>

<%
// set the character encoding to use when interpreting request values 
request.setCharacterEncoding("utf-8");

String crawlerName = request.getParameter("crawlerName");
if (crawlerName == null) {
    crawlerName = "";
}
String url = request.getParameter("url");
String patternFileParam = request.getParameter("patternFileParam");
if (null == patternFileParam) {
    patternFileParam = "";
}

CrawlerBean crawler = configBean.getCrawlerBean(crawlerName);
PatternFileBean patternFile = crawler.getPatternFileBeans().get(patternFileParam);

boolean matches = false;
Set<String> tokens = null;
String errorMsg = null;
if (null != crawler && null != patternFile && null != url) {
    try {
        matches = patternFile.matches(url);
        if (matches) {
            tokens = patternFile.getTokens(url);
        }
    } catch (PatternFileBeanException e) {
        errorMsg = "Patterns are not correct. Go to hotspots.jsp and re-check them";
    }
} else {
    if (null != url ) {
        errorMsg = "Crawler or PatternFile not available";
    }
}

%> 

<% request.setAttribute("pageTitle", " verify URL - "+ crawlerName + " - " + patternFileParam); %>
<%@include file="top.include.jsp" %>

<% if (null != errorMsg) {%>
    <h2><%=errorMsg%></h2>
<%} else {%>
    <% if (null != url) { %>
        <h3><%=url%> is <%=matches? "" : "not " %> matched by patterns</h3>
        <% if (matches) { %>
            <h4> <% for (String token:tokens) {%> <%=token + " "%><%}%></h4>
        <% } //end if %>
    <% } //end if %>
<% } //end else %>

<form action="./verify_url.jsp">
    <input type="hidden" name="crawlerName" value="<%=crawlerName%>"/>
    <input type="hidden" name="patternFileParam" value="<%=patternFileParam%>"/>
    <table>
		<tr>
			<th colspan ="3"><%=crawlerName + " - " + patternFileParam%></th>
		</tr>
		<tr>
			<td>URL</td>
			<td>
		 	   <input type="text" name="url"/>
			</td>
			<td>
				<input type="submit" value="verify"/>
			</td>
		</tr>
	</table>
</form>


<%@include file="bottom.include.jsp" %>
