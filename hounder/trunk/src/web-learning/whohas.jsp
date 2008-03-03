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
        import="java.util.HashMap"
        import="java.util.HashSet"
        import="java.util.Set"
        import="java.util.Map"
        import="java.util.List"
        import="java.util.Iterator"
%>

<jsp:useBean id="whoHasBean" class="com.flaptor.search4j.classifier.WhoHasBean" scope="session"/>

<% if (!whoHasBean.isInited()) {
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
<title>Show which URLs have some token</title>

</head>

<body>
<jsp:include page="navbar.jsp"/>

<%
	String token = (String)request.getParameter("token");
	Set<String> urls= whoHasBean.getWhoHas(token);
    StringBuffer ur= new StringBuffer();	
%>  
		<table border="1">
		<tr>
  			<td><b>Token</b></td>
  			<td><b>URLS</b></td>
		</tr>
		<tr>
	        <td><%=token%></td>		
<%
	if (null == urls){
%>
   		    <td>No url has it</td>    	        
<%			
	} else {
	    Iterator<String> it=  urls.iterator();
     	while (it.hasNext()){
     		String u= it.next();
	       	ur.append("<a href=\"classify.jsp?url=" + u + "\">" + u 
	       	+ "</a> </br> ");
    	}
    }
%>		
            <td><%=ur%></td>
        </tr>  
    </table>
</body>
</html>
