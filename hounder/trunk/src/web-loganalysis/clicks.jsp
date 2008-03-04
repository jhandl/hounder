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
List<String> tags = Arrays.asList(WebAppUtil.getParameterSessionValues(request, "scope", new String[0]));
String urlSiteFilter= WebAppUtil.getParameterSessionValue(request, "url_filter","");

String dateFormat= "dd/MM/yyyy";
SimpleDateFormat dateParser = new SimpleDateFormat(dateFormat); 
Date from = startDate.length() > 0 ? dateParser.parse(startDate) : null;
Date to = endDate.length() > 0 ? dateParser.parse(endDate) : null;
%>

<center>
<form action="clicks.jsp" method="post"> 
    <table>
        <tr>
            <th align="left"> Reports: </th>
            <th align="left"> Narrow by: </th>
            <th align="left"> Optional (for Clicks report only): </th>
        </tr>
    	<tr>
		    <td>
			    <table>
			        <%
			        String[] reportNames= {"clicks", "queries"};
			        for (String reportName: reportNames) {%>
			            <tr>
			            	<td>
			            		<input type="submit" name="report" value="<%=reportName%>"></input>
			            	</td>
			            </tr>
			        <% } // end for %>       
			    </table>
			</td>
			<td>
			    <table>
			        <tr><td> Dates (<%= dateFormat %>)</td></tr>
			        <tr><td> From: <input type="text" name="start_date" value="<%=startDate%>"></input> </td></tr>
			        <tr><td> To: <input type="text" name="end_date" value="<%=endDate%>"></input> </td></tr>
				</table>
			</td>
			<td>
				<table>								
			        <% for (String typeName: reportBean.listTagTypes()) {%>
			         <tr>
			         	<td> 
			             <%=typeName%>: 
			             <select name="scope">
				              <option value="">all</option>
				              <% for (String tagName: reportBean.listTags(typeName)) {%>
				                  <option value="<%=tagName%>" <%=tags.contains(tagName) ? "selected=\"true\"" :"" %>><%=tagName%></option>
				              <% } // end for %>
			             </select>
			         	</td>
			         </tr>
			        <% } // end for %>
			        <tr><td>
			        Url filter: <input type="text" name="url_filter" value="http://" />
			        </td></tr>
			    </table>
	        </td>
	    </tr>
	</table>

</form>
</center>


<%!
void printQueries(ArrayList<Pair<Integer,String>> data, 
        JspWriter out) throws java.io.IOException{ 
    out.println("<table>");
    for (Pair<Integer,String> item : data) {
        String line = item.last();
        Integer val = item.first();
        out.println("<tr><td>" + val +"</td><td>" + line + "</td></tr>");
	}
}

void printClicks(ArrayList<Pair<Integer,String>> data, 
        JspWriter out) throws java.io.IOException{ 
    
    out.println("<table>");
	String lastLink="";
	boolean firstTimeFlag= true;
    int clicked=0;
    int shown=0;
    for (Pair<Integer,String> item : data) {
        String link = item.last();
        Integer val = item.first();
        if (lastLink.equals(link)){        	
        	if (val > 0){
        	    clicked++;
       		} else {
       			shown++;
       		}
        } else {
			if (firstTimeFlag){ 
			    out.println("<tr><td> Shown <td> Clicked </td><td> URL </td> <td> Ratio </td></tr>"); 
        		firstTimeFlag= false;
        	} else { 
        	    out.println("<tr><td>" +Integer.toString(shown) + "</td>" +
        			"<td>" + Integer.toString(clicked) + "</td>" +
        			"<td>" + lastLink + "</td>" +
        			"<td>" + Float.toString(clicked/(float)(shown)) + "</td></tr>");
      		}
      		lastLink= link;
      		shown= 0;
      		clicked= 0;
        	if (val > 0){
        	    clicked++;
       		} else {
       			shown++;
       		}
        } // else
    } // for
		     
    out.println("<tr><td>" + Integer.toString(shown) + "</td>" +
		"<td>" + Integer.toString(clicked) + "</td>" +
		"<td>" + lastLink + "</td>" +
		"<td>" + Float.toString(clicked/(float)(shown)) + "</td></tr>");
    out.println("</table>");
}
%>

<%
	ArrayList<Pair<Integer,String>> data = null;
	String report = request.getParameter("report");
	if (report != null) {
		if ("clicks".equals(report)) {
	    	data = Report.getClicks(from, to, urlSiteFilter, tags);
			if (null != data) {%>
	    		<b> Results for <%=report%>: </b> <%
	    		printClicks(data,out);
			}	    	
		} else if ("queries".equals(report)) {
	    	data = Report.mostSearchedQueries(Integer.MAX_VALUE, from, to, tags);
			if (null != data) {%>
	    		<b> Results for <%=report%>: </b> <%
	    		printQueries(data,out);
			}
		}	
	}// if (report != null) 
%>


<%@include file="/include.bottom.jsp" %>




