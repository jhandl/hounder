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
	contentType="text/html; charset=utf-8"
	pageEncoding="UTF-8"
	
	import="com.flaptor.hounder.classifier.LearningBean"
	import="com.flaptor.hounder.classifier.util.StateEnum"
	import="com.flaptor.hounder.classifier.UrlsBean.BGFetcherStatus"
	import="com.flaptor.hounder.crawler.CacheBean"
	import="java.util.HashMap"
	import="java.util.Map"
	import="java.util.List"
%>

<jsp:useBean id="urlsBean" class="com.flaptor.hounder.classifier.UrlsBean" scope="session"/>

<% if (!urlsBean.isInited()) { %>
    <jsp:forward page="config.jsp"/>
<% } %>
<%
	// set the character encoding to use when interpreting request values 
	request.setCharacterEncoding("utf-8");		
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta HTTP-Equiv="Cache-Control" content="no-cache">
<head>
<title>Show the URLS in the training tool</title>

</head>

<body>
<jsp:include page="navbar.jsp"/>

<%
	String reqStat= request.getParameter("bgfReqStat");
	if (reqStat != null){
		if ("Start".equals(reqStat)){
			String rf= request.getParameter("refetch");	
			boolean refetch= false;
			if (rf != null) refetch=true;			
			urlsBean.startBGFetcher(refetch);
		} else if ("Stop".equals(reqStat)){
			urlsBean.stopBGFetcher();
		} else {
%>		
		</br> Ignoring parameter bgfReqStat=<%= reqStat%>		 </br>
<%		}
	}
%>

</br> Using cache at: <%=urlsBean.getCacheDir()%></br>
	  Using baseDir : <%=urlsBean.getBaseDir()%></br>
	  Using urls file (baseDir/): <%=urlsBean.getUrlFile()%></br>

	<form action="urls.jsp" method="get">
<%
	BGFetcherStatus bgfStat= urlsBean.getBGFetcherStatus();
%>	
	Current BG-Fetcher status is <%= bgfStat %> 
	<input type="submit" value="Refresh status">	
<%
	if (BGFetcherStatus.RUNNING == bgfStat) {
%>		
		<input type="submit" name="bgfReqStat" value="Stop">	
<% } else { %>
		</br>Refetch pages even if in cache<input type="checkbox" name="refetch" value="refetch"/>
		<input type="submit" name="bgfReqStat" value="Start">			
<% } %>		
	</form>


		<table border="1">
		<tr>
  			<td><b>Id</b></td>
  			<td><b>Cached?</b></td>
			<td><b>Classify</b></td>
  			<td><b>Url</b></td>			
		</tr>
<%
		List<String> urls= urlsBean.getUrls();	
		Integer i= new Integer(0);
		for (String url:urls){	
			String status= (urlsBean.getUrlState(url)? "true":"false");
%>  
			<tr>
				<td><%=i%></td>			
	  			<td><%=status%></td>
	  			<td><a href="classify.jsp?url=<%=url%>">Classify</a></td>
				<td><%=url%></td>
			</tr>  		
<%
		i= i+1;
		}
%>  	
		</table>	
  
</body>
</html>
