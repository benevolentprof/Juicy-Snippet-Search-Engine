<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="edu.uci.ics.websnippetrepository.searcher.WebSearcher"%>
<%@ page import="edu.uci.ics.websnippetrepository.searcher.SearchResultEntity"%>
<%@ page import="edu.uci.ics.websnippetrepository.searcher.SearchResult"%>

<%@page import="java.io.IOException"%>
<%@page import="org.apache.lucene.queryParser.ParseException"%>

<%@page import="java.util.Set"%>
<%@page import="java.util.Iterator"%><html>
<head>
	<title>Prototype Code Search</title>
	<script type="text/javascript"
		src="http://ajax.googleapis.com/ajax/libs/jquery/1.4/jquery.min.js"></script>
	
	<link rel="stylesheet" type="text/css" href="style.css"> 
	<script type="text/javascript" src="script.js"></script>
</head>
<body>
	<h3>Please enter your search query</h3>
	<form action="index.jsp" method="get">
		<input type="text" size="50" style="width:300px" name="txtQuery" value="<%if (request.getParameter("txtQuery")!=null) out.print(request.getParameter("txtQuery")); %>" />
		<input type="submit" value="Submit" style="width:80px;margin: 0px 5px 0px 5px;"/>	
		<BR><a href="javascript:;" id="advFeaturesToggleButton"  class="normaltext">Toggle Advance Search Features</a><BR>
		<DIV class="advanceSearchBox" id="advanceFeatures" style="display: none;">
			<table border="0">
			<TR><TD class="normaltext"> package: </TD><TD><INPUT type="text" name="package"> </TD></TR>
			<TR><TD class="normaltext"> import: </TD><TD><INPUT type="text" name="import"> </TD></TR>
			<TR><TD class="normaltext"> class: </TD><TD><INPUT type="text" name="class"> </TD></TR>
			<TR><TD class="normaltext"> class used: </TD><TD><INPUT type="text" name="classUsed"> </TD></TR>
			<TR><TD class="normaltext"> extends/implements: </TD><TD><INPUT type="text" name="extends"> </TD> </TR>
			<TR><TD class="normaltext"> method: </TD><TD><INPUT type="text" name="method"> </TD></TR>
			<TR><TD class="normaltext"> method used: </TD><TD><INPUT type="text" name="methodUsed"> </TD></TR>
			<TR><TD class="normaltext"> return: </TD><TD><INPUT type="text" name="return"> </TD></TR>
			<TR><TD class="normaltext"> variable: </TD><TD><INPUT type="text" name="variable"> </TD></TR>
			<TR><TD class="normaltext"> comment: </TD><TD><INPUT type="text" name="comment"> </TD></TR>		
			</table>
		</DIV>
	</form>

<%
    if ((request.getParameter("txtQuery") != null && request.getParameter("txtQuery").length() > 1)||
		(request.getParameter("package") != null && request.getParameter("package").length() > 1)||	
		(request.getParameter("import") != null && request.getParameter("import").length() > 1)||	
		(request.getParameter("class") != null && request.getParameter("class").length() > 1)||	
		(request.getParameter("classUsed") != null && request.getParameter("classUsed").length() > 1)||	
		(request.getParameter("extends") != null && request.getParameter("extends").length() > 1)||	
		(request.getParameter("method") != null && request.getParameter("method").length() > 1)||	
		(request.getParameter("methodUsed") != null && request.getParameter("methodUsed").length() > 1)||	
		(request.getParameter("return") != null && request.getParameter("return").length() > 1)||	
		(request.getParameter("variable") != null && request.getParameter("variable").length() > 1)||	
		(request.getParameter("comment") != null && request.getParameter("comment").length() > 1)){
%>
	<DIV style="width:700px;">
	<HR >         
	  
<%
		
		WebSearcher ws = new WebSearcher();
		String query = request.getParameter("txtQuery"); 
		
		String[] optionArr = new String[10];
		
		optionArr[0] = (request.getParameter("package")!= null)? request.getParameter("package"):"";
		optionArr[1] = (request.getParameter("import")!= null)? request.getParameter("import"):"";
		optionArr[2] = (request.getParameter("class")!= null)? request.getParameter("class"):""; 
		optionArr[3] = (request.getParameter("classUsed")!= null)? request.getParameter("classUsed"):"";
		optionArr[4] = (request.getParameter("extends")!= null)? request.getParameter("class"):"";
		optionArr[5] = (request.getParameter("method")!= null)? request.getParameter("method"):""; 
		optionArr[6] = (request.getParameter("methodUsed")!= null)? request.getParameter("methodUsed"):""; 
		optionArr[7] = (request.getParameter("return")!= null)? request.getParameter("return"):""; 
		optionArr[8] = (request.getParameter("variable")!= null)? request.getParameter("variable"):""; 
		optionArr[9] = (request.getParameter("comment")!= null)? request.getParameter("comment"):"";
				
		try{
			SearchResult searchResult = null;
			if (request.getParameter("start")==null)
				searchResult = ws.search(query, optionArr);
			else
				searchResult = ws.search(query, optionArr, Integer.parseInt(request.getParameter("start")), WebSearcher.MAX_SHOWN_RESULTS );
			int hits = searchResult.getNumSearchResults();
%>	
	<span class="normaltext" style="text-align:right; float:right;">
		<%	
			String prevURL="", nextURL="";
			boolean hasPrevURL=false, hasNextURL=false;
		
			if (searchResult.getStartResultIndex() != 0){ 
				hasPrevURL = true;
				prevURL = request.getRequestURL()+"?"+request.getQueryString();
				if(prevURL.lastIndexOf("start=")!=-1)
					prevURL = prevURL.replaceAll("start=[^&]*","start="+(searchResult.getStartResultIndex()-WebSearcher.MAX_SHOWN_RESULTS));
				else
					prevURL = prevURL + "&start=" + (searchResult.getStartResultIndex()-WebSearcher.MAX_SHOWN_RESULTS);
		%>
		
			<a href="<%=prevURL %>">&lt; PREV</a> |  	
			<% }
		%>
		showing <%=searchResult.getStartResultIndex()+1 %> - <%=searchResult.getStartResultIndex()+searchResult.getSearchResult().length %> from <%=hits%> (in <%=searchResult.getTimeused() %> ms)
		<%	if (searchResult.getStartResultIndex()+WebSearcher.MAX_SHOWN_RESULTS < hits){
				hasNextURL = true;
				nextURL = request.getRequestURL()+"?"+request.getQueryString();
				if(nextURL.lastIndexOf("start=")!=-1)
					nextURL = nextURL.replaceAll("start=[^&]*","start="+(searchResult.getStartResultIndex()+WebSearcher.MAX_SHOWN_RESULTS));
				else
					nextURL = nextURL + "&start=" + (searchResult.getStartResultIndex()+WebSearcher.MAX_SHOWN_RESULTS);
		%>
			| <a href="<%=nextURL %>">NEXT &gt;</a>  	
			<% }
		%> 
		<br>
		<a id="ToggleHighlightButton" href="#">Toggle Highlight</a>
	</span>
	<span class="normaltext"style="text-align:left; font-size:16px; font-weight:bold; ">Results for '<%=request.getParameter("txtQuery")%>'</span>  
	<div class="urlLink">
		<!--  Query String: <% /*=searchResult.getQueryString()*/ %><br/>
		Query String for Snippet : <% /*=searchResult.getQuery_snippet_index()*/ %><br/>
		Query String for Document: <% /*=searchResult.getQuery_document_index()*/ %><br/>
		URL: <% /*= request.getRequestURL() */ %><br/>
		URL: <% /*= request.getQueryString()*/ %><br/>  -->
		<script language="javascript">
			var queryterms = [<%
				Set queryTerms = searchResult.getQueryTerms();
				for(Iterator i = queryTerms.iterator();i.hasNext(); ){
					String term = (String)i.next();
					if(i.hasNext())
						out.println("\""+term+"\", ");
					else
						out.print("\""+term+"\"");
				}
			%>];
		</script>
		<script type="text/javascript" src="highlight.js"></script>
	</div>
	<HR >
	</DIV>
<% 			
			SearchResultEntity[] returnResults = searchResult.getSearchResult(); 
			for (int i=0; i<returnResults.length ; i++){
				SearchResultEntity resultEntity = returnResults[i];
				int snippetid = resultEntity.getSnippetid();
				String title = resultEntity.getTitle();
				String url = resultEntity.getUrl();
				String text = resultEntity.getText();
				String code = resultEntity.getCode();
				String paragraph = resultEntity.getParagraph();
				double score = resultEntity.getScore();
				int	 samesnippetcount = resultEntity.getSamesnippetcount();
				
%>
				<DIV class="resultBox">
				<A class="resultHeader" href="<%=url %>"><%=snippetid %> : <%=title %></A>
				<DIV class="textStyle"><%=paragraph %></DIV>
				<PRE class="codeStyle" id="codeStyle"><%=code %></PRE>
				<HR>
				<a class="urlLink" href="<%=url %>"><%=url %></a><BR>
				
				<% if (samesnippetcount>1) { 
						out.println("<a href=\"seemore.jsp?snippetid="+snippetid+"\" >+ see more "+samesnippetcount+" versions.</a>");
					 } %>
				
				</DIV>
<% 
			}	//end for loop for result entries
			
			out.println("<span class=\"normaltext\" style=\"text-align:right; float:right;\">");
			if (hasPrevURL){ 
				out.println("<a href=\""+prevURL+"\">&lt; PREV</a> | ");
			}
			
			out.println("showing "+(searchResult.getStartResultIndex()+1)+" - "+(searchResult.getStartResultIndex()+searchResult.getSearchResult().length)+
					" from "+hits+" (in "+searchResult.getTimeused() +" ms)");
			
			if (hasNextURL){ 
				out.println("| <a href=\""+nextURL+"\">NEXT &gt;</a>");
			}
			out.println("</span>"); 
		}catch(ParseException e){
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}
%>
	<!-- lightbox -->
	<div class="lightbox" id="lightbox" style="display:none;"></div>
	<div class="databox" id="databox" >
		<a class="closebox" id="closebox"></a>
		<div class="databoxHeader">
			<span class="databoxHeaderTitle">CODE </span>
			<span class="databoxHeaderTitle2" id="databoxHeaderWord">abcd</span>
		</div>
		<pre id="codeBigScreen">
			void main(String[] args){
				System.out.println("abcd");
			}
		</pre>
		<a class="urlLinkBigScreen" id="urlLinkBigScreen" href="#">abcd</a>
		<input class="copyCodeButton" id="copyCodeButton" type="button" onclick="javascript:selectCode(this);" value="select code">
	</div>
	
	<!-- Open Popup -->
	<input type="button" value="open title-only result" onclick="window.open('<%="resulttitle.jsp?"+request.getQueryString() %>','titleOnlyResultWindow','width=1000,height=500,menubar=no,status=no,location=no,toolbar=no,scrollbars=yes');"/>
<%
	}
%>
	
</body>
</html>
