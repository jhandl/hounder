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
import="java.util.Map"

%>

<jsp:useBean id="cacheBean" class="com.flaptor.search4j.crawler.CacheBean" scope="session"/>
<jsp:useBean id="learningBean" class="com.flaptor.search4j.classifier.LearningBean" scope="session"/>


<% if (!cacheBean.isInited() || !learningBean.isInited()) { %>
    <jsp:forward page="config.jsp"/>
<% }
// set the character encoding to use when interpreting request values 
request.setCharacterEncoding("utf-8");

String[] CATEGORIES = learningBean.getCategoryList();
int NUM_CATEGORIES=CATEGORIES.length;
int urlCount = learningBean.getUrlCount();

boolean showUrl = "ON".equals(request.getParameter("showUrl"));

// Parameter processing
// We can get the page url or the page urlid (it's pos in the urls file)
String url = (String)request.getParameter("url");
String urlIdParam = (String)request.getParameter("urlId");

int urlId = 0;
String LAST_VIEWED_URL_COOKIE="lastUrlId";
Cookie[] cookies= request.getCookies();
for(int i=0; i<cookies.length; i++) {
	Cookie cookie = cookies[i];
	if (LAST_VIEWED_URL_COOKIE.equals(cookie.getName())){
		urlId= new Integer(cookie.getValue());
		break;
    }
}
if (url != null) {
	urlId = learningBean.getUrlId(url);
} else if (urlIdParam != null) {
    urlId = Integer.parseInt(urlIdParam);
    url = learningBean.getUrl(urlId);
} else {
    url = learningBean.getUrl(urlId);
}

boolean havePrev = (urlId > 0);
boolean haveNext = (urlId < urlCount-1);

String submitParam = (String)request.getParameter("submit");
if ("CLASSIFY".equals(submitParam)) {
	for (String cat : CATEGORIES) {
		String param= (String) request.getParameter("radio_" + cat);
		if (null == param || param.equalsIgnoreCase("unknown")){
            learningBean.markAsUnknown(cat,url);
		} else if (param.equalsIgnoreCase("included")) {
            learningBean.addToCategory(cat,url);
        } else if (param.equalsIgnoreCase("not_included")) {
            learningBean.removeFromCategory(cat,url);
        } else {
        	System.err.println("WARN: no status for cat " +cat+ ", url=" + url);
        }
    }    
    learningBean.saveData();
    if (haveNext) {
        urlId = urlId + 1;
    }
    Cookie lastUrl = new Cookie(LAST_VIEWED_URL_COOKIE, new Integer(urlId).toString());
	lastUrl.setMaxAge(Integer.MAX_VALUE);
	response.addCookie(lastUrl);
} else if ("CALCULATE".equals(submitParam)) {
    learningBean.saveData();
    urlId = 0;
}
// Have to recompute, as urlId may have changed.
url = learningBean.getUrl(urlId);
havePrev = (urlId > 0);
haveNext = (urlId < urlCount -1);
String urlText = cacheBean.getText(url);
Map<String,StateEnum> stateMap = learningBean.getStates(url);

%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta HTTP-Equiv="Cache-Control" content="no-cache">
<head>
<title><%=url%></title>
<link rel="icon" href="favicon.ico" type="image/x-icon">
<link rel="shortcut icon" href="favicon.ico" type="image/x-icon">
<link rel="stylesheet" type="text/css" href="style.css">
<script language="JavaScript">
function updateUrl(a) {
	var checkbox=document.getElementById('showUrl');
	if (checkbox.checked) {
		a.href = a.href + '&showUrl=' + checkbox.value;
	}
	return true;
}
function refreshMyWindow() {
	var newUrl='classify.jsp?urlId=<%=urlId%>';
	var checkbox=document.getElementById('showUrl');
	if (checkbox.checked) {
		newUrl += '&showUrl=' + checkbox.value;
	}
	window.location.href = newUrl;
}


function applyToAll(what) {
  var x;
  <% for (int i=0;i< CATEGORIES.length; i++){
	String cat=CATEGORIES[i];%>
	x=document.getElementsByName('radio_<%=cat%>');
	if (what == "yes") {x[0].checked=true} else { x[0].checked=false };
	if (what == "no")  {x[1].checked=true} else { x[1].checked=false };
	if (what == "unk") {x[2].checked=true} else { x[2].checked=false };
<% } %>
}



</script>
</head>

<body>

<jsp:include page="navbar.jsp"/>

<center>

<table border="0">
<tr>

<% // PREV BUTTON %>
  <td align="center" style="width:100px;">
<% if (havePrev) { %>
    <a href="classify.jsp?urlId=<%=(urlId - 1)%>" onclick="return updateUrl(this)">Prev</a>
<% } else { %>
    Prev
<% } %>
  </td>

<% // Category tagging %>
  <td>
  <form name="classify" action="classify.jsp" method="get">
	  <input name="urlId" type="hidden" value="<%=urlId%>">
	  <table border="1" cellspacing="0" style="font-family: Arial, Helvetica; font-size: 10px;" bgcolor="#E0E0FF">
		  <tr>
		  <% for (int i=0;i< CATEGORIES.length; i++){
			String cat=CATEGORIES[i];%>
			  <td>
			    <table style="font-family: Arial, Helvetica; font-size: 10px;" >
				    <tr>
				      <td rowspan="3" align="right"><font size="+1"><%=cat%></font></td>
				      <td align="right">Yes
				      	<input name="radio_<%=cat%>" type="radio" value="included" 
				      		<%=(stateMap.get(cat) == StateEnum.INCLUDED) ? "checked" : ""%>>
				     </td>
				    </tr>
				    <tr>
				      <td align="right">No
				      	<input name="radio_<%=cat%>" type="radio" value="not_included"
				      	<%=(stateMap.get(cat) == StateEnum.REMOVED) ? "checked" : ""%>>
				      </td>
				    </tr>
				    <tr>      
				      <td align="right">Unknown
				      	<input name="radio_<%=cat%>" type="radio" value="unknown"
				      	<%=(stateMap.get(cat) == StateEnum.UNKNOWN) ? "checked" : ""%>>
				      </td>      
				    </tr>
			    </table>
			  </td>
		<% if( i%5==1){%></tr><tr> <%}%>
		  <% } %>
		  </tr>
	  </table>
	  </td>
	  <td>
	  <input name="submit" type="submit" value="CLASSIFY" text="CLASSIFY">
  </form>
  </td>

  <td align="center" style="width:100px;">
    <input type="button" onclick="return applyToAll('yes')" value="Yes All">
    <input type="button" onclick="return applyToAll('no')"  value="No All">
    <input type="button" onclick="return applyToAll('unk')" value="Unknown All">
  </td>

  <td align="center" style="width:100px;">
<% if (haveNext) { %>
    <a href="classify.jsp?urlId=<%=(urlId + 1)%>" onclick="return updateUrl(this)">Next</a>
<% } else { %>
    Next
<% } %>
  </td>


</tr>
</table>

<table width="100%" border="0" cellspacing="0">
  <tr style="font-size:12px;font-family:Arial,Helvetica;" bgcolor="#B0B0FF">
    <td  width="1%" align="left" nowrap>
    
    
<%
	String[] catList= learningBean.getCategoryList();
%>
	</br>
	Show why:
<%	
	for (String cat: catList){
%>	
   	<a href="why.jsp?category=<%= cat%>&url=<%=(url)%>" ><%= cat %></a>
<%
	}
%>    
    </td>
    
    
    <td align="center">
        <a href="<%=url%>" target="_blank"><%=url%></a>
    </td>
    <td width="1%" align="right" nowrap>
      <input type="checkbox" name="showUrl" id="showUrl" <%=(showUrl ? "checked" : "")%> value="ON" onchange="refreshMyWindow()">Show page</input>
    </td>
  </tr>
</table>

<table width="100%" border="0" cellspacing="0" style="margin-bottom:1% ! important;">
  <tr>
    <td>
      <textarea name="desc" type="text" rows="15" style="width: 100%; margin:0 ! important; padding:0 ! important;">
        <%=urlText%>
      </textarea>
    </td>
  </tr>
</table>
<table width="100%" border="1" cellspacing="0">
  <tr>
    <td style="height:410px;">
<% if (showUrl) { %>
        <iframe name="iframe" src="<%=url%>" frameborder="0" marginwidth="0" marginheight="0" scrolling="auto" height="100%" width="100%">
        </iframe> 
<% } %>
    </td>
  </tr>
</table>

</div>

</center>
</body>
</html>

