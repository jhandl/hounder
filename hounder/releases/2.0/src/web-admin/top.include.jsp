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
<%-- author Martin Massera --%>
<%@ page 
contentType="text/html; charset=utf-8"
pageEncoding="UTF-8"
%>

<%
// set the character encoding to use when interpreting request values 
request.setCharacterEncoding("utf-8");
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
	<meta HTTP-Equiv="Cache-Control" content="no-cache">
	<link rel="stylesheet" type="text/css" href="style.css" />
<head>
<title><%=request.getAttribute("pageTitle")%></title>
<link rel="icon" href="favicon.ico" type="image/x-icon">
<link rel="shortcut icon" href="favicon.ico" type="image/x-icon">
<link rel="stylesheet" type="text/css" href="style.css">
</head>

<body>
<div id="navbar">
	<a href="config.jsp">Config</a>
	<a href="index.jsp">Crawlers</a>
</div>
<div id="main">
	<div id="content">
		<center>