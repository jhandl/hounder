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

<%@page import="com.flaptor.util.Pair"%>
<%@page import="com.flaptor.clustering.monitoring.SystemProperties"%>
<%@page import="com.flaptor.clustering.Node"%>
<%@page import="com.flaptor.clustering.Cluster"%>
<%@page import="com.flaptor.hounder.crawler.clustering.*"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.Map"%>

<%
    Cluster cluster = Cluster.getInstance();
    int idx = Integer.parseInt(request.getParameter("node"));
    Node clusterNode = cluster.getNodes().get(idx);

    String action = request.getParameter("action");
    CrawlerControl crawlerControl = (CrawlerControl)cluster.getModule("crawlerControl");
    CrawlerControlNode node = (CrawlerControlNode)crawlerControl.getNode(clusterNode);

    request.setAttribute("pageTitle", "crawler control - " + clusterNode.getHost()+":"+clusterNode.getPort());
%>

<%@include file="/include.top.jsp" %>


<%if (!clusterNode.isReachable()) { %>
    <h2>Node unreachable!</h2>
<%
}%>

<%if (node == null) {%>
    <h2>This node is not a crawler.</h2>
<%  
}
else {%>
    hola mono<br/>
    <pre><%=crawlerControl.getBoostConfig(node) %></pre>
<%}
%>
<%@include file="/include.bottom.jsp" %>
