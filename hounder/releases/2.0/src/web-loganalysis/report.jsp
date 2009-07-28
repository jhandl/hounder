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
<%@page import="com.flaptor.util.*"%>
<%@page import="java.util.*"%>
<%@page import="com.flaptor.hounder.loganalysis.*"%>

<%@page import="java.text.SimpleDateFormat"%>
<jsp:useBean id="reportBean" class="com.flaptor.hounder.loganalysis.ReportFrontend" scope="session"/>
<%request.setAttribute("pageTitle", "Hounder Log Analysis");%>
<%@include file="include.top.jsp"%>

<%
String startDate = WebAppUtil.getParameterSessionValue(request, "start_date", "01/10/2007").trim();
String endDate = WebAppUtil.getParameterSessionValue(request, "end_date", "01/11/2007").trim();
List<String> tags = Arrays.asList(WebAppUtil.getParameterSessionValues(request, "category", new String[0]));
String topN = WebAppUtil.getParameterSessionValue(request, "topN", "10");

SimpleDateFormat dateParser = new SimpleDateFormat("dd/MM/yyyy"); 
Date from = startDate.length() > 0 ? dateParser.parse(startDate) : null;
Date to = endDate.length() > 0 ? dateParser.parse(endDate) : null;
%>

<center>
<form action="report.jsp" method="post"> 
    <table>
        <tr>
            <th align="left"> Reports: </th>
            <th align="left"> Narrow by: </th>
        </tr>
    <tr>
	    <td>
		    <table>
		        <% for (String reportName: reportBean.listReports()) {%>
		            <tr><td>
		            <input type="submit" name="report" value="<%=reportName%>"></input>
		            </td></tr>
		        <% } // end for %>       
		        <tr><td>
		            Show top <input type="text" name="topN" value="<%=topN%>"></input>
		        </td></tr>
		    </table>
		</td>
		<td>
		    <table>
		        <tr><td> Dates </td></tr>
		        <tr><td> From: <input type="text" name="start_date" value="<%=startDate%>"></input> </td></tr>
		        <tr><td> To: <input type="text" name="end_date" value="<%=endDate%>"></input> </td></tr>
		
		        <% for (String typeName: reportBean.listTagTypes()) {%>
		         <tr><td> 
		             <%=typeName%>: 
		             <select name="category">
		              <option value=""></option>
		              <% for (String tagName: reportBean.listTags(typeName)) {%>
		                  <option value="<%=tagName%>" <%=tags.contains(tagName) ? "selected=\"true\"" :"" %>><%=tagName%></option>
		              <% } // end for %>
		             </select>
		         <td></tr>
		        <% } // end for %>
		    </table>
        </td>
    </tr>
</table>

</form>
</center>

<%
ArrayList<Pair<Integer,String>> data = null;
String report = request.getParameter("report");
if (report != null) {
	int topn = Integer.parseInt(topN);
	if ("Most Searched Queries".equals(report)) {
	    data = Report.mostSearchedQueries(topn, from, to, tags);
	} else if ("Most Unsuccessful Queries".equals(report)) {
	    data = Report.mostUnsuccessfulQueries(topn, from, to, tags);
	} else if ("Worst Placed Results".equals(report)) {
	    data = Report.worstPlacedResults(topn, from, to, tags);
	} else if ("Best Placed Spam".equals(report)) {
	    data = Report.bestPlacedSpam(topn, from, to, tags);
	} else if ("Most Common Query Sequences".equals(report)) {
	    data = Report.mostCommonQuerySequences(topn, from, to, tags);
	}
	
	if (null != data) {%> 
	    <b> Results for <%=report%>: </b>
	    <table>
        <%for (Pair<Integer,String> item : data) {
	        String line = item.last();%>
            <tr><td><%=line%></td></tr>
        <%}
	}
}
%>


<%@include file="/include.bottom.jsp" %>




