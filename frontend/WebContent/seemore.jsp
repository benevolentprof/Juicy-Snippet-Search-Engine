<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@page import="java.sql.Connection"%>
<%@page import="java.sql.DriverManager"%>
<%@page import="java.sql.Statement"%>
<%@page import="java.sql.ResultSet"%>
<%
	String dbUrl = "jdbc:mysql://localhost/codesnippet?useUnicode=yes&characterEncoding=utf8&user=root&password=w,ji^h";
%>


<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>See More</title>
	<link rel="stylesheet" type="text/css" href="style.css"> 
	<script type="text/javascript" src="script.js"></script>
</head>
<body>
	<h1> See More </h1>

	<HR>
<%
	if(request.getParameter("snippetid") != null && request.getParameter("snippetid").length()>=1) {
		
		int snippetid = Integer.parseInt(request.getParameter("snippetid"));
		Class.forName("com.mysql.jdbc.Driver");
		Connection con = DriverManager.getConnection (dbUrl);
		Statement stmt = con.createStatement();
		
		String query = 	"SELECT * FROM codesnippet " + 
					   	"WHERE FIND_IN_SET( " +
					   				"codesnippetid," +
			 						"(SELECT samesnippetlist FROM snippetgroup WHERE codesnippetid = "+snippetid+")" +
			 					")>0";
		ResultSet rs = stmt.executeQuery(query);
		
		while(rs.next()){
			int resultSnippetid = rs.getInt("codesnippetid");
			String title = rs.getString("title");
			String url = rs.getString("url");
			String text = rs.getString("text");
			String code = rs.getString("code");
%>
	<DIV class="resultBox">
		<A class="resultHeader" href="<%=url %>"><%=title %></A>
		<DIV class="textStyle"><%=text %></DIV>
		<PRE class="codeStyle"><%=code %></PRE>
		<HR>
		<input class="copyCodeButton" type="button" onclick="javascript:selectCode(this);" value="select code">
		<a class="urlLink" href="<%=url %>"><%=url %></a><BR>
	</DIV>
<%	
		}
		
		rs.close();
		con.close();
	}
		
		
%>
</body>
</html>