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
import="com.flaptor.search4j.crawler.bean.CrawlerBean"
import="com.flaptor.search4j.crawler.bean.InjectorBean"
import="java.util.List"
import="java.util.ArrayList"
%>

<jsp:useBean id="configBean" class="com.flaptor.search4j.crawler.bean.ConfigBean" scope="session"/>

<%
// set the character encoding to use when interpreting request values 
request.setCharacterEncoding("utf-8");

String crawlerName = request.getParameter("crawlerName");
if (crawlerName == null) {
    crawlerName = "";
}
String command = request.getParameter("command");
if (command == null) {
    command = "";
}
String patternFileParam = request.getParameter("patternFileParam");
if (null == patternFileParam) {
    patternFileParam = "";
}

CrawlerBean crawler = configBean.getCrawlerBean(crawlerName);
InjectorBean injector = crawler.getInjectorBean();
List<String> list = null;
String errorMsg = null;
boolean success = false;
if (null != crawler && null != injector) {

    if ("write".equals(command)) {
    
        String urlCount = request.getParameter("urlCount");
        int count = Integer.parseInt(urlCount);
        list = new ArrayList<String>(count);
        for (int i = 0; i <= count ; i++) {
            String urli = request.getParameter("url"+i);
            if (null != urli && !"".equals(urli)) {
                list.add(urli);
            }
        }
        success = injector.injectURLs(list);
        
        if (!success) {
            errorMsg = injector.getErrorString();
        }
    } else {
        list = new ArrayList<String>();
    }

} else {
    errorMsg = "Crawler or Injector not available";
}

// Button to delete a row from a table.
String deleteRowButton = "<button type='button' onclick='this.parentNode.parentNode.parentNode.removeChild(this.parentNode.parentNode)'>delete</button>";
%> 

<% request.setAttribute("pageTitle", crawlerName + " - URL injection "); %>
<%@include file="top.include.jsp" %>

<script>
    var rowCount;

    function addRow() {
        var table = document.getElementById("urls");
        var element = document.createElement("tr");
        var url = document.createElement("td");
        url.innerHTML = "<input type=\"text\" name=\"url" + rowCount + "\"/>";
        var deleteButton = document.createElement("td");
        deleteButton.innerHTML = "<%=deleteRowButton%>";
        element.appendChild(url);
        element.appendChild(deleteButton);
        table.appendChild(element);
        rowCount++;
        document.getElementById("urlCount").value = rowCount;

    }
</script>

    <% if ("write".equals(command)) {
        if (success) { %>
            <h3>operation succeded</h3>
        <%} else { %>
            <h3><%=errorMsg%></h3>
        <%}
    }%>
    <form name="actions" action="inject.jsp" method="get">
        <input type="hidden" name="crawlerName" value="<%=crawlerName%>">
        <input id="urlCount" type="hidden" name="urlCount" value="<%=list.size() + 1%>">
    
        <table id="urls">
        <tr>
        	<th colspan="2"><%=crawlerName + " - URL injection" %></th>
        </tr>
        <%for (int i = 0; i < list.size(); i++) { %>
        <tr>
            <td>
                <input type="text" name="<%="url"+i%>" value="<%=list.get(i)%>">
            </td>                
            <td>
                <%=deleteRowButton%>
            </td>
        </tr>
        <%}%>
        <tr>
            <td>
                <input type="text" name="<%="url"+list.size()%>">
            </td>                
            <td>
                <%=deleteRowButton%>
            </td>
        </tr>
            
        <script> rowCount = <%=list.size() + 1 %></script>
        </table>

        <input type="submit" name="command" value="write" >
        <button onclick="addRow()" type="button">add URL</button>
        <input type="submit" name="command" value="cancel" >
    </form>

<%@include file="bottom.include.jsp" %>
