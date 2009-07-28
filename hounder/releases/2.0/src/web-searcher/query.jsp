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
<%!
void addQueryBox(HttpServletRequest request, java.io.Writer out) throws java.io.IOException{
	out.write("<form method=\"GET\" action=\"\">");
	out.write("		query: ");	
	String q=request.getParameter("query");
	if (null != q){
		out.write("		<input type=\"text\" name=\"query\" value=\"" + q + "\"/>");
	} else {
	    out.write("		<input type=\"text\" name=\"query\" value=\"\"/>");
	}
	out.write("		<input type=\"submit\"/>");
	out.write("</form>");	    
}
%>
