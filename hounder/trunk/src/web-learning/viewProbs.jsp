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
        import="com.flaptor.search4j.classifier.bayes.PersistenceManager"
        import="com.flaptor.util.Pair"	
        import="java.util.HashMap"
        import="java.util.HashSet"
        import="java.util.Map"
        import="java.util.List"
%>

<jsp:useBean id="cacheCalculatorBean" class="com.flaptor.search4j.classifier.CacheCalculatorBean" scope="session"/>
<jsp:useBean id="whoHasBean" class="com.flaptor.search4j.classifier.WhoHasBean" scope="session"/>

<% if (!cacheCalculatorBean.isInited()) {
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
<title>Show probabilities file</title>

<script language="JavaScript">
function commitChanges() {
	var str="";
	document.getElementById("newVal1").title=document.getElementById("newToken1").value;
	document.getElementById("newVal2").title=document.getElementById("newToken2").value;
	document.getElementById("newVal3").title=document.getElementById("newToken3").value;
	document.getElementById("newVal4").title=document.getElementById("newToken4").value;	
	var x=document.getElementsByTagName("input");
	for (var i=0; i < x.length;i++) // >
	{ 
		var e=x[i];
		var v= e.value;
		var t= e.title;
		if (0 == t.length) continue;
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
	System.err.println("np=" + np);
	if (null != np){
		cacheCalculatorBean.setMyProbs(np, categoryName);
	}
	
	Map<String,Double> unsortedMap= cacheCalculatorBean.readProbabilities(categoryName);
	List<Pair<Double,String>> probs= com.flaptor.search4j.classifier.util.ProbsUtils.getSortedMapByVal(unsortedMap);	
	Map<String,Double> myProbsMap= cacheCalculatorBean.getMyProbabilities(categoryName);
%>
	<br></br>
	Show probabilities file for category <b><%= categoryName %></b>
	<br></br>
   	<form  id="probsForm" action="viewProbs.jsp?category=<%=categoryName%>" method="post" enctype="utf-8">
		<input id="newProbsValues" name="newProbsValues" type="hidden" value="">
		<input id="z" type="submit" value="Commit"  onclick="commitChanges()">

	</br></br>
    <table border="1">
    	<tr> <td>Add up to 4 tokens and their values here </td></tr>
    	<tr>            
            <td><b>New Token</b></td>
            <td>New Value</td>
        </tr>    	
        <tr>
    		<td> <input id="newToken1" type="text" value=""></td> <td><input id="newVal1" type="text" value=""> </td>
        </tr>
        <tr>
    		<td> <input id="newToken2" type="text" value=""></td> <td><input id="newVal2" type="text" value=""> </td>
        </tr>
        <tr>
    		<td> <input id="newToken3" type="text" value=""></td> <td><input id="newVal3" type="text" value=""> </td>
        </tr>
        <tr>
    		<td> <input id="newToken4" type="text" value=""></td> <td><input id="newVal4" type="text" value=""> </td>
        </tr>
        
        <tr>            
            <td><b>Token</b></td>
            <td>Value</td>
            <td>Modify tokens value</td>                        
        </tr>

<%
	int i=0;
	for (Pair<Double,String> pair : probs) {
		String token= pair.last();
		Double value= pair.first();
		if ("__MAX_TUPLE_SIZE__".equals(token))
			 continue;
		Double myVal= myProbsMap.get(token);
		

%>	
		<tr>
			<td><a href="whohas.jsp?token=<%=token%>"><%= token %></a></td>
			<td><%= value %></td>
			<td><input type="text" value="<%=null == myVal? "": myVal %>" size="5" title="<%=token%>" ></td>			
		</tr>
<%	
		// we delete known tokens from myProbsMap. THis way the tokens that left
		// in myProbsMap are the one added by the user
		myProbsMap.remove(token);
	}
%>

        <tr>            
            <td><b>Tokens added by the user</b></td>
            <td>Value</td>
            <td>Modify tokens value</td>            
        </tr>

<%
	List<Pair<Double,String>> myProbs= com.flaptor.search4j.classifier.util.ProbsUtils.getSortedMapByVal(myProbsMap);
	for (Pair<Double,String> pair : myProbs) {
		String token= pair.last();
		Double value= pair.first();
%>	
		<tr>
			<td><a href="whohas.jsp?token=<%=token%>"><%= token %></a></td>
			<td><%= value%></td>
			<td><input type="text" value="<%=value %>" size="5" title="<%=token%>" ></td>			
		</tr>
<%	
	}
%>

	   </table>

		<input type="submit" value="Commit"  onclick="commitChanges()">
   	</form>		
	   
</body>
</html>
