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
	
	import="com.flaptor.search4j.classifier.LearningBean"
	import="com.flaptor.search4j.classifier.util.StateEnum"
	import="com.flaptor.search4j.crawler.CacheBean"
    import="com.flaptor.util.Pair"	
	import="java.util.HashMap"
	import="java.util.Map"
	import="java.util.Set"
    import="java.util.List"
%>
<jsp:useBean id="learningBean" class="com.flaptor.search4j.classifier.LearningBean" scope="session"/>
<jsp:useBean id="whyBean" class="com.flaptor.search4j.classifier.WhyBean" scope="session"/>
<jsp:useBean id="cacheCalculatorBean" class="com.flaptor.search4j.classifier.CacheCalculatorBean" scope="session"/>

<% if (!whyBean.isInited() || !learningBean.isInited() ||
		!cacheCalculatorBean.isInited()) { %>
    <jsp:forward page="config.jsp"/>
<% } %>
<%
	whyBean.reloadProbabilities();
	// set the character encoding to use when interpreting request values 
	request.setCharacterEncoding("utf-8");		
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta HTTP-Equiv="Cache-Control" content="no-cache">
<head>
<title>Checks why a document (url) is classified the way it is</title>

<script language="JavaScript">
function commitChanges() {
	var str="";
	var x=document.getElementsByTagName("input");
	for (var i=0; i < x.length;i++) // >
	{ 
		var e=x[i];
		var v= e.value;
		var t= e.title;
		if (v > 0 && 1 > v) {
			if (0 == str.length ){ 
				str= t + "=" + v;
			} else {
				str=  str + " ; " + t + "=" + v;
			}
		}		
	}
	document.getElementById("newProbsValues").value=str;
	document.getElementById("probsForm").submit();
}
</script>
</head>

<body>
<jsp:include page="navbar.jsp"/>

<%
	String categoryName = request.getParameter("category");
	if (null == categoryName || 0 == categoryName.length()){
		categoryName= cacheCalculatorBean.getCategoryList()[0];
	}
	
	String np = request.getParameter("newProbsValues");
//	System.err.println("np=" + np);
	if (null != np){
		cacheCalculatorBean.setMyProbs(np, categoryName);
	}
	Map<String,Double> myProbsMap= cacheCalculatorBean.getMyProbabilities(categoryName);
	
	String url = (String)request.getParameter("url");
	Double score= whyBean.getScore(categoryName, url);
	if (null == score){
	  score = -1.0;
	}	
%>  
   	<form  id="probsForm" action="why.jsp?category=<%=categoryName%>" method="post">
		<input type="hidden" name="url" value="<%=url%>">
		<input id="newProbsValues" name="newProbsValues" type="hidden" value="">
		<input id="z" type="submit" value="Commit"  onclick="commitChanges()">
		<br>
		Category <b><%=categoryName%>: <%=score %></b><br>
		URL=<b><%=url%></b>
		<table border="1">
		<tr>
  			<td><b>Token</b></td>
  			<td><b>Value</b></td>
		</tr>
<%
		// The getProbabilitiesMap() returns the map with the real values 
		// ie: the computed values overriden by 'my.probabilities' values
		Map<String,Double> unsortedMap= whyBean.getProbabilitiesMap(categoryName,url);			
		List<Pair<Double,String>> probs= com.flaptor.search4j.classifier.util.ProbsUtils.getSortedMapByVal(unsortedMap);
			
		for (Pair<Double,String> pair : probs) {
			String token= pair.last();
			Double val= pair.first();
%>  
			<tr>
			<td><a href="whohas.jsp?token=<%=token%>"><%=token%></a></td>
  			<td><%=val%></td>
			</tr>  		
<%
		}
%>  	
		</table>	
		<input type="submit" value="Commit"  onclick="commitChanges()">
   	</form>				
<%

%>  	
  
</body>
</html>
