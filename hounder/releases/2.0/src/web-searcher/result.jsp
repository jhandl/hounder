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
<%@page import="com.flaptor.hounder.cache.CachedVersionUtil"%>
<%@page import="java.util.List"%>
<%@page import="org.dom4j.Element"%>
<%@page import="com.flaptor.util.DomUtil"%>

<%!void formatResult(String url, String title, String desc,
            java.io.Writer out, boolean groupHead) throws java.io.IOException {
        if (groupHead) {
            out.write("<div class=\"result\">"); // head of group
        } else {
            out.write("<div class=\"result_tail\">"); // tail
        }
        out.write("<span class=\"res_title\">");
        out.write("<a href=\"" + url + "\">" + title + "</a><br/>");
        out.write("</span>");
        out.write("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">");
        out.write("<tbody>");
        out.write("<tr>");
        out.write("<td>");
        out.write("<font size=\"-1\">");
        out.write("<span class=\"res_desc\">");
        out.write(desc + "</br>");
        out.write("</span>");
        out.write("<span class=\"res_url\">");
        out.write(url);
        out.write("</span>");
        if (CachedVersionUtil.showCachedVersion(url)) {
            out.write("<nobr>");
            out.write(" <a class=\"res_cached\" href=\""
                    + CachedVersionUtil.getCachedVersionURL(url)
                    + "\">cached version</a>");
            out.write("</nobr>");
        }
        out.write("</font>");
        out.write("</td>");
        out.write("</tr>");
        out.write("</tbody>");
        out.write("</table>");
        out.write("</div>");//result
    }

    void printResults(Element parentElement, java.io.Writer out)
            throws java.io.IOException {
        out.write("<div class=\"results\">");
        for (Element e : (List<Element>) parentElement.elements("item")) {
            String url = DomUtil.getElementText(e, "link");
            String title = DomUtil.getElementText(e, "title");
            String desc = DomUtil.getElementText(e, "description");
            formatResult(url, title, desc, out, true);
            out.write("<br/>");
            for (Element ee : (List<Element>) e.elements("item")) {
                url = DomUtil.getElementText(ee, "link");
                title = DomUtil.getElementText(ee, "title");
                desc = DomUtil.getElementText(ee, "description");
                formatResult(url, title, desc, out, false);
                out.write("<br/>");            
            }            
        }
        out.write("</div>");
    }%>
