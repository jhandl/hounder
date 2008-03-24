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
import="java.util.List"
import="java.util.ArrayList"

%>

<jsp:useBean id="configBean" class="com.flaptor.hounder.crawler.bean.ConfigBean" scope="session"/>

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

String errorMsg = null;
List<List<String>> list = null;
List<List<String>> failureList = null;
CrawlerBean crawler = configBean.getCrawlerBean(crawlerName);
if (null == crawler) {
    errorMsg = "Crawler "+crawlerName+" not available";
} else {
    PatternFileBean patternFile = crawler.getPatternFileBeans().get(patternFileParam);
    if (null == patternFile) {
        errorMsg = "PatternFile "+patternFileParam+" not available";
    } else {
        if ("list".equals(command)) list = patternFile.getPatterns();
        if ("write".equals(command)) {

            try {
                String patternCount = request.getParameter("patternCount");
                int count = Integer.parseInt(patternCount);
                list = new ArrayList<List<String>>(count);
                for (int i = 0; i < count ; i++) {
                    String prefixi = request.getParameter("prefix"+i);
                    String patterni = request.getParameter("pattern"+i);
                    String tokensi = request.getParameter("tokens"+i);
                    if (null != prefixi && !"".equals(prefixi)) {
                        List<String> line = new ArrayList<String>(3);
                        line.add(prefixi);
                        line.add(patterni);
                        line.add(tokensi);
                        list.add(line);
                    }
                }
                patternFile.writeToFile(list);
            } catch (PatternFileBeanException e){
                errorMsg = "Some patterns are wrong.";
                failureList = e.getList();
                list = failureList;
            } catch (Exception e) {
                errorMsg = e.getMessage();
                System.out.println(e);
            }
        }
    }
}



// Button to delete a row from a table.
String deleteRowButton = "<button onclick='this.parentNode.parentNode.parentNode.removeChild(this.parentNode.parentNode); return false' value='delete'>delete</button>";

%> 

<% request.setAttribute("pageTitle", crawlerName + " - " + patternFileParam); %>
<%@include file="top.include.jsp" %>

<script>
var rowCount;

function addRow() {
    var table = document.getElementById("patterns");
    var element = document.createElement("tr");
    var prefix = document.createElement("td");
    prefix.innerHTML = "<input type=\"text\" name=\"prefix" + rowCount + "\" size=\"40\" />";
    var pattern = document.createElement("td");
    pattern.innerHTML = "<input type=\"text\" name=\"pattern" + rowCount + "\" size=\"20\" />";
    var tokens = document.createElement("td");
    tokens.innerHTML = "<input type=\"text\" name=\"tokens" + rowCount + "\" size=\"20\" />";
    var deleteButton = document.createElement("td");
    deleteButton.innerHTML = "<%=deleteRowButton%>";
    element.appendChild(prefix);
    element.appendChild(pattern);
    element.appendChild(tokens);
    <% if (null != failureList) {%>
        var patternError = document.createElement("td");
        patternError.innerHTML = "<span></span>";
        element.appendChild(patternError);
        <%}%>
            element.appendChild(deleteButton);
        table.appendChild(element);
        rowCount++;
        document.getElementById("patternCount").value = rowCount;

}
</script>

<% if (null != errorMsg && null == failureList) {%>
    <h2><%=errorMsg%></h2>
        <%} else {%>
            <form name="actions" action="hotspots.jsp" method="post">
            <input type="hidden" name="crawlerName" value="<%=crawlerName%>">
            <input type="hidden" name="patternFileParam" value="<%=patternFileParam%>">
            <input id="patternCount" type="hidden" name="patternCount" value="<%=list.size()%>">
    
            <table id="patterns">
            <tr>
            	<th colspan="4"><%=crawlerName + " - " + patternFileParam%></th>
            </tr>

            <% for (int i = 0; i < list.size(); i++){ 
                List<String> line = list.get(i);
            %>
            <tr>
                <td>
                    <input type="text" name="<%="prefix"+i%>" value="<%=line.get(0)%>" size="40">
               </td>
                <td>
                    <input type="text" name="<%="pattern"+i%>" value="<%=line.get(1)%>" size="20">
                </td>
                <td>
                    <input type="text" name="<%="tokens"+i%>" value="<%=line.get(2)%>" size="20">
                </td>
                <% if (null != failureList) {%>
                <td>
                    <span class="patternError"><%=line.size() > 3 ? line.get(3): ""%></span>
                </td>
                <%}%>
                <td>
                    <%=deleteRowButton%>
                </td>
            </tr>
            <%}%>
            <script> rowCount = <%=list.size()%></script>
            </table>
            <button onclick="addRow(); return false">add rule</button>
            <input type="submit" name="command" value="list" text="CANCEL">
            <input type="submit" name="command" value="write" text="WRITE">

        </form>

   <%} // end else %>

   <a href="./verify_url.jsp?crawlerName=<%=crawlerName%>&patternFileParam=<%=patternFileParam%>" target="_blank">Verify url</a>

<%@include file="bottom.include.jsp" %>
