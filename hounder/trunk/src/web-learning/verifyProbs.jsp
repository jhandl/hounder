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
        import="java.util.HashMap"
        import="java.util.HashSet"
        import="java.util.Set"        
        import="java.util.Map"
        import="java.util.List"
%>

<jsp:useBean id="cacheCalculatorBean" class="com.flaptor.search4j.classifier.CacheCalculatorBean" scope="session"/>

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
<title>Verify probabilities file performance</title>

</head>

<body>
<jsp:include page="navbar.jsp"/>

<p>

<%
	String categoryName = request.getParameter("category");
	if (null == categoryName || 0 == categoryName.length()){
		categoryName= cacheCalculatorBean.getCategoryList()[0];
	}	
%>	
	<h3> <%= categoryName %>.probabilites was calculated on 
	<%= cacheCalculatorBean.getProbabilitiesFileDate(categoryName).toString() %> </br>
	</h3>
</p>
    <table border="1">
		<tr>
        	<b>
            <td>Category</td>
            <td>OK</td>
            <td>BAD</td>
            <td>TOT</td>
            <td>OK_P</td>
            <td>BAD_P</td>
            <td>true positives</td>
            <td>false negatives</td>
            <td>false positives</td>
            <td>true negatives</td>                        
            </b>
        </tr>


<%
    String[] userClas= {"uinc_cinc", "uinc_cnot", "unot_cinc", "unot_cnot"};
    Map<String,Map<String,Double>> matches= cacheCalculatorBean.verify(categoryName);
    if (null == matches)  {
%>
		<h4> Category <%= categoryName %> is empty </h4>	    
<%	    
    } else {
        int uinc_cinc= matches.get("uinc_cinc").size();
        int uinc_cnot= matches.get("uinc_cnot").size();
        int unot_cinc= matches.get("unot_cinc").size();
        int unot_cnot= matches.get("unot_cnot").size();
        int ok=uinc_cinc + unot_cnot;
        int bad=uinc_cnot + unot_cinc;
        int tot= bad+ok;
%>
        <tr>
            <td><a href="#<%=categoryName%>" class="altlink"><%=categoryName%></a></td>
            <td><%=ok%></td>
            <td><%=bad%></td>
            <td><%=tot%></td>
            <td><%=(float)ok/(float)tot%></td>
            <td><%=(float)bad/(float)tot%></td>
            <td><a href="#<%=categoryName%>_uinc_cinc" class="altlink"><%=uinc_cinc%></a></td>
            <td><a href="#<%=categoryName%>_uinc_cnot" class="altlink"><%=uinc_cnot%></a></td>
            <td><a href="#<%=categoryName%>_unot_cinc" class="altlink"><%=unot_cinc%></a></td> 
            <td><a href="#<%=categoryName%>_unot_cnot" class="altlink"><%=unot_cnot%></a></td>
         </tr>
<%
	} // else
%>
        </table>

        <table border="1">
                <tr>
                        <b>
                        <td>URL</td>
                        <td>Why?</td>
                        <td>Reclassify</td>                        
                        </b>
                </tr>
<%
	    if (null != matches){		    	
		  for (String ucla: userClas){
%>
	    	<tr> 
<%			
			if (ucla.equalsIgnoreCase("uinc_cinc")) { %>		    	
		    	<td bgcolor="#00FF00"> <a name="<%=categoryName%>_<%=ucla%>" </a>
		    		<b>Results below were
	  				marked as included by the user and the learning machine 
	  				(true positives) </b> <%=categoryName%> </td>
<%	 		} else if (ucla.equalsIgnoreCase("uinc_cnot")) { %>
		    	<td bgcolor="#ff0000"> <a name="<%=categoryName%>_<%=ucla%>" </a>
		    	<b>Results below were
					marked as included by the user but not by the learning machine
					(false negatives) </b> <%=categoryName%> </td>
<% 			} else if (ucla.equalsIgnoreCase("unot_cinc")) { %>		
		    	<td bgcolor="#ff0000"> <a name="<%=categoryName%>_<%=ucla%>" </a>
			    	<b>Results below were   
					marked as not included by the user but included by the learning
					machine  (false positives) </b> <%=categoryName%> </td>
<% 			} else if (ucla.equalsIgnoreCase("unot_cnot")) { %>		   
		    	<td bgcolor="#00FF00"> <a name="<%=categoryName%>_<%=ucla%>" </a>
		    		<b>Results below were   
					marked as not included by the user and by the learning machine 
					(true negatives) </b> <%=categoryName%> </td>
<% 			} %>
		    	<td> </td>
		    	<td> </td>
	    	</tr>
<%	    
	    	Map<String,Double> ucUrlsValMap= matches.get(ucla);
	    	Set<String> ucUrlsSet= ucUrlsValMap.keySet();	    	
	    	
            for (String urlPage: ucUrlsSet) {
%>
                <tr>
                        <td><%=urlPage%></td>
                        <td><a href="why.jsp?category=<%=categoryName%>&url=<%=urlPage%>">
		                        <%= ucUrlsValMap.get(urlPage) %></a></td>
                        <td><a href="classify.jsp?url=<%=urlPage%>">Reclassify</a></td>
                </tr>
<%
			} // for (String urlPage: ucUrlsList
			} // 		  for (String ucla: userClas){
			}// 	    if (null != matches){		    	
%>
        </table>

</body>
</html>
