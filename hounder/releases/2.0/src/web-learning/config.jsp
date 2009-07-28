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
import="com.flaptor.hounder.crawler.CacheBean"

%>

<jsp:useBean id="configBean" class="com.flaptor.hounder.classifier.ConfigBean" scope="session"/>
<jsp:useBean id="cacheBean" class="com.flaptor.hounder.crawler.CacheBean" scope="session"/>
<jsp:useBean id="learningBean" class="com.flaptor.hounder.classifier.LearningBean" scope="session"/>
<jsp:useBean id="cacheCalculatorBean" class="com.flaptor.hounder.classifier.CacheCalculatorBean" scope="session"/>
<jsp:useBean id="whoHasBean" class="com.flaptor.hounder.classifier.WhoHasBean" scope="session"/>
<jsp:useBean id="whyBean" class="com.flaptor.hounder.classifier.WhyBean" scope="session"/>
<jsp:useBean id="urlsBean" class="com.flaptor.hounder.classifier.UrlsBean" scope="session"/>

<%

// set the character encoding to use when interpreting request values 
request.setCharacterEncoding("utf-8");

String baseDirParam = request.getParameter("baseDir");
if (baseDirParam == null) {
    baseDirParam = "";
}
String categoriesParam = request.getParameter("categories");
String[] categories = null;
if (categoriesParam != null) {
    categories = categoriesParam.split(",");
} else {
    categoriesParam = "";
}
String urlFileParam = request.getParameter("urlFile");
if (urlFileParam == null) {
    urlFileParam = "";
}
String cacheBaseDirParam = request.getParameter("cacheBaseDir");
if (cacheBaseDirParam == null) {
    cacheBaseDirParam = "";
}

String errorMsg = "";
String submitParam = request.getParameter("submit");
boolean inited = false;
if ("CONFIG".equals(submitParam)) {
    inited= configBean.initialize(categories, cacheBaseDirParam, baseDirParam, 
            urlFileParam, 0.5);
    inited = inited && 
    		 cacheBean.initialize(cacheBaseDirParam, true) &&    		      		     		  
    		 learningBean.initialize(configBean) && 
    		 cacheCalculatorBean.initialize(configBean) &&
    		 whyBean.initialize(configBean) &&
    		 whoHasBean.initialize(configBean) &&
     		 urlsBean.initialize(configBean,cacheBean);
    if (!inited) {
        errorMsg = "There was an error initializing the system";
    }
}

%>

<% if (inited) { %>
    <jsp:forward page="classify.jsp"/>
<% } %> 


<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta HTTP-Equiv="Cache-Control" content="no-cache">
<head>
<title>
<%=errorMsg%>
</title>
</head>
<link rel="icon" href="favicon.ico" type="image/x-icon">
<link rel="shortcut icon" href="favicon.ico" type="image/x-icon">
<link rel="stylesheet" type="text/css" href="style.css">
</head>
<body>

<center>
<%=errorMsg%>

<form name="config" action="config.jsp" method="get">
<table border="1">
<tr>
  <td>Learning Base Dir:</td>
  <td><input name="baseDir" type="text" value="<%=baseDirParam%>" size="100"></td>
</tr>
<tr>
  <td>Categories:</td>
  <td><input name="categories" type="text" value="<%=categoriesParam%>" size="100"></td>
</tr>
<tr>
  <td>Url File:</td>
  <td><input name="urlFile" type="text" value="<%=urlFileParam%>" size="100"></td>
</tr>
<tr>
  <td>Cache Base Dir:</td>
  <td><input name="cacheBaseDir" type="text" value="<%=cacheBaseDirParam%>" size="100"></td>
</tr>
<tr>
  <td colspan="2"><input name="submit" type="submit" value="CONFIG" text="CONFIG"></td>
</tr>
</table>
</form>
</center>
</body>
</html>

