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
        import="com.flaptor.hounder.crawler.CacheBean"
        import="java.util.HashMap"
        import="java.util.HashSet"
        import="java.util.Map"
        import="java.util.List"
%>

<jsp:useBean id="cacheCalculatorBean" class="com.flaptor.hounder.classifier.CacheCalculatorBean" scope="session"/>
<jsp:useBean id="whoHasBean" class="com.flaptor.hounder.classifier.WhoHasBean" scope="session"/>

<% if (!cacheCalculatorBean.isInited() || !whoHasBean.isInited()) {
%>
    <jsp:forward page="config.jsp"/>
<% }
        // set the character encoding to use when interpreting request values
        request.setCharacterEncoding("utf-8");
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta HTTP-Equiv="Cache-Control" content="no-cache">
<head>
<title>(Re)Calculate probabilities file </title>
<script language="JavaScript">
	function applyToAll() {
	  var x;
  	  var all=document.getElementById('recalcAll');
	 <% 
	  String[] categories = cacheCalculatorBean.getCategoryList();
	  for (int i=0;i< categories.length; i++){
		String cat=categories[i];
	%>
		x=document.getElementsByName('recalc_<%=cat%>');
		x[0].checked=all.checked;
	<% } %>
	}
</script>
</head>

<body>
<jsp:include page="navbar.jsp"/>

<p>

<%
	for (String cat: cacheCalculatorBean.getCategoryList()){
		String calc = request.getParameter("recalc_"+cat);
		if (null != calc){
			cacheCalculatorBean.calculate(cat);
		}
	}	
	if (null != request.getParameter("updateWhoHas")){
	    whoHasBean.calculate();
	}	
%>
</p>
<p>
<form action="recalculateProbs.jsp" method="get">
	<table>
	<tr>
		<td> Update WhoHas? </td>
		<td><input type="checkbox" name="updateWhoHas" value="Update" /> </td>
		<td> WhoHas file was calculated on 
			<%= whoHasBean.getWhoHasFileDate().toString() %> 
		</td>		
	</tr>
	<tr>
		<td>Recalculate all probabilities</td>
		<td> <input type="checkbox" id="recalcAll"
			onclick="return applyToAll()" value="recalculate" /> </td>
	</tr>
	<tr></tr>	<tr></tr>	<tr></tr>
<%	
	String[] catList= cacheCalculatorBean.getCategoryList();
	for (String cat: catList){
%>
	<tr>
		<td>Recalculate <%= cat %> </td>
		<td><input type="checkbox" name="recalc_<%= cat	%>" value="recalculate" 
		onclick="all=document.getElementById('recalcAll'); all.checked=false" /> </td>
		<td><%= cat	%>.probabilites was calculated on 
			<%= cacheCalculatorBean.getProbabilitiesFileDate(cat).toString() %> 
		</td>
	</tr>
<%
	}
%>
	</table>
	<input type="submit" value="Submit">	
</form> 	
</p>

</body>
</html>
