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
<jsp:useBean id="configBean" class="com.flaptor.search4j.crawler.bean.ConfigBean" scope="session"/>

<%
//set the character encoding to use when interpreting request values 
request.setCharacterEncoding("utf-8");

String baseDirParam = request.getParameter("baseDir");
if (baseDirParam == null) {
    baseDirParam = "";
}
String libDirParam = request.getParameter("libDir");
if (libDirParam == null) {
    libDirParam = "";
}

String errorMsg = "";
String submitParam = request.getParameter("submit");
boolean inited = false;
if ("CONFIG".equals(submitParam)) {
    inited= configBean.initialize(baseDirParam,libDirParam);
    if (!inited) {
        errorMsg = "There was an error initializing the system";
    }
}

%>

<% if (inited) { %>
    <jsp:forward page="index.jsp"/>
<% } %> 

<% request.setAttribute("pageTitle", errorMsg); %>
<%@include file="top.include.jsp" %>

<%=errorMsg%>

<form name="config" action="config.jsp" method="get">
<table>
<tr>
  <th colspan="2">Config</th>
</tr>
<tr>
  <td>Crawler Base Dir:</td>
  <td><input name="baseDir" type="text" value="<%=baseDirParam%>" size="100"></td>
</tr>
<tr>
  <td>Crawler Lib Dir:</td>
  <td><input name="libDir" type="text" value="<%=libDirParam%>" size="100"></td>
</tr>
<tr>
  <td colspan="2"><input name="submit" type="submit" value="CONFIG" text="CONFIG"></td>
</tr>
</table>
</form>

<%@include file="bottom.include.jsp" %>
