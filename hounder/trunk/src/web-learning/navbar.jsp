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
<jsp:useBean id="cacheCalculatorBean" class="com.flaptor.search4j.classifier.CacheCalculatorBean" scope="session"/>

<div name="navbar">
	<a href="config.jsp">Config</a>
    <a href="classify.jsp">Classify</a>	
	<a href="urls.jsp">Urls</a>
<!-- 	<a href="why.jsp">Why?</a>  needs a URL  -->
<!-- 	<a href="whohas.jsp">Who has a token?</a> needs a URL  -->
<!--	<a href="viewProbs.jsp">View Probs?</a> -->
<!--	<a href="verifyProbs.jsp">Verify Probs?</a>	 -->
	<a href="recalculateProbs.jsp">Re-Calculate probs</a>
<%
	String[] catList= cacheCalculatorBean.getCategoryList();
%>
	</br>
	View probabilities file for category:
<%	
	for (String cat: catList){
%>	
	 <a href="viewProbs.jsp?category=<%= cat%>"><%= cat%></a>
<%
	}
%>
	</br>
	Verify probabilities file for category:
<%	
	for (String cat: catList){
%>	
	 <a href="verifyProbs.jsp?category=<%= cat%>"><%= cat%></a>
<%
	}
%>
	</br>
	<br>
	---------------------------------------------------------------------------
	</br></br></br>
</div>
