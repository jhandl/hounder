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
<%@page import="java.util.Map"%>
<%@page import="org.dom4j.Element"%>
<%@page import="com.flaptor.util.DomUtil"%>

<%!
String getGoToLink(int pageNr, int totalResults, int itemsPerPage, 
        Map<String,String[]> params, String requestURL, String caption){    
    pageNr= pageNr <0 ? 0: pageNr;
    if (pageNr < 0) {
        pageNr =0;
    }
    if (pageNr * itemsPerPage > totalResults){
        pageNr= totalResults/itemsPerPage;
    }
    StringBuffer gotoHref= new StringBuffer(requestURL);
    boolean flag= true;
    for (String key: params.keySet()){
        if ("start".equalsIgnoreCase(key)) continue;
        String val= params.get(key)[0];
        if (flag){
            flag= false;
            gotoHref.append("?"+key+"="+val);
	        continue;
        }
        gotoHref.append("&"+key+"="+val);
    }
    String res= "<a href=\"" + gotoHref + "&start=" + pageNr * itemsPerPage + "\">" + caption + "</a>";
	return res;    
}
%>

<%!
void printGoToLinks(Element parentElement, HttpServletRequest request, 
        java.io.Writer out) throws java.io.IOException{
    int totalResults = Integer.parseInt(DomUtil.getElementText(parentElement, "totalResults"));
    int startIndex = Integer.parseInt(DomUtil.getElementText(parentElement, "startIndex"));
    int itemsPerPage = Integer.parseInt(DomUtil.getElementText(parentElement, "itemsPerPage"));
    
    String urlR= request.getRequestURL().toString();
    Map<String,String[]> params= request.getParameterMap();
    
    int currentPage= startIndex/itemsPerPage;
    int minPage=0;
    int maxPage= totalResults/itemsPerPage;    
	
    if (totalResults > minPage && itemsPerPage < totalResults) { 
    	int showToI= (startIndex + itemsPerPage < totalResults) ? (startIndex + itemsPerPage) : (totalResults);
    	String showTo= new Integer(showToI).toString();
        out.write ("Showing " + new Integer(startIndex + 1).toString() + " to " + showTo);
        out.write (" out of " +  totalResults + " results " + "<br/>"); 
        if (currentPage > minPage) {
           out.write(getGoToLink(currentPage-1, totalResults, itemsPerPage, params, urlR, "previous"));
        } else {
            out.write("previous ");
        }
        int gotoLinks=5;
        int startLink= Math.max(minPage, currentPage-gotoLinks);
        int endLinks= Math.min(maxPage,startLink + 2*gotoLinks);
        
        for (int p = startLink; p <= endLinks; p++) {
        	if (currentPage == p) {
        	    out.write(new Integer(p+1).toString()); // p+1: because index start from 0 but we show as starting from 1
        	}else { 
        	    String pg= new Integer(p+1).toString() ;
        	    out.write(" ");
        	    out.write(getGoToLink(p, totalResults, itemsPerPage, params, urlR, pg));
        	    out.write(" ");
        	} 	
        }
        if (currentPage < maxPage) { 
        	out.write(getGoToLink(currentPage+1, totalResults, itemsPerPage, params, urlR, " next"));
        } else {
            out.write(" next");
        }
    }
}
%>
