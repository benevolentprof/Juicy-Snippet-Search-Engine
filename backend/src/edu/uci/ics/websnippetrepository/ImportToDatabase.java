package edu.uci.ics.websnippetrepository;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


public class ImportToDatabase {

	public static final int NODETYPE_TEXT = 0;
	public static final int NODETYPE_SOURCECODE = 1;
	protected static Connection conn;
	
	private static synchronized void setup(){
		try {
			conn=getConnection(true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public ImportToDatabase(){
		try{
			if(conn==null || conn.isClosed()){
				setup();
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
	}
	
	public static Connection getConnection(boolean requestNewConnection) throws SQLException{
		if(requestNewConnection){
			Connection newConn = null;
		
			try {
				Class.forName("com.mysql.jdbc.Driver").newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			newConn = DriverManager.getConnection(
			"jdbc:mysql://dorami.ics.uci.edu/codesnippet?useUnicode=yes&characterEncoding=utf8&user=coderep&password=C0deRep?");
			newConn.setAutoCommit(true);
		
			return newConn;
		}
		else{
			if(conn==null || conn.isClosed()){
				setup();
			}
			return conn;
		}
		
	}
	
	public static boolean isConnectionEstablished(){
		if(conn==null)
			return false;
		else
			try {
				return !conn.isClosed();
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
	}
	
	public static synchronized void importXMLToDatabase(String url, String xmlDocString) throws SQLException{
		
		try {
			
			//test connection, if not existed, create new
			if(!isConnectionEstablished())
				setup();
			
			//start parsing the XML document
			XPath xPath = XPathFactory.newInstance().newXPath();
			XPathExpression nodeXPathExpr = xPath.compile("/codesnippets/text | /codesnippets/sourcecode");
			InputSource inputSource = new InputSource(new ByteArrayInputStream(xmlDocString.getBytes()));
			
			//get all nodes, both <text> and <sourcecode>
			NodeList allNodes = (NodeList) nodeXPathExpr.evaluate(inputSource,XPathConstants.NODESET);
			
			//import new document to "document" table.
			conn.setAutoCommit(false);
			System.out.println("::processing url: "+url);
			PreparedStatement pstmt = conn.prepareStatement("INSERT INTO document(url,created) VALUES (?,now());", PreparedStatement.RETURN_GENERATED_KEYS);
			pstmt.setString(1, url);
			pstmt.execute();
			ResultSet keyResult = pstmt.getGeneratedKeys();
			keyResult.next();
			int docID = keyResult.getInt(1);
			System.out.println("::document imported with docid: "+docID);
			pstmt.close();
						
			//import each xml node into "snippetdata" table
			pstmt = conn.prepareStatement("INSERT INTO snippetdata(docid,orderid,type,data) VALUES (?,?,?,?);");
					
			for(int i=0;i<allNodes.getLength();i++){
				Node node = allNodes.item(i);
				int nodeType = node.getNodeName().equals("text")?
								ImportToDatabase.NODETYPE_TEXT:
								ImportToDatabase.NODETYPE_SOURCECODE;
				String content = node.getTextContent().trim();
				content = StringEscapeUtils.unescapeHtml(content);
				System.out.printf("::::order: %d | %-10s | \"%s...\" ",i,node.getNodeName(),(content.length()<=20?content:content.substring(0, 20)));
				pstmt.clearParameters();
				pstmt.setInt(1, docID);
				pstmt.setInt(2, i);
				pstmt.setInt(3, nodeType);
				pstmt.setString(4, content);
				pstmt.executeUpdate();
				System.out.println(" ----> imported.");
			}
			
			//commit
			conn.commit();
			
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			conn.rollback();
			
		} catch (SQLException e) {
			e.printStackTrace();
			conn.rollback();
			throw(e);
		} finally {
			conn.setAutoCommit(true);
		}
	}
	
	
	public static synchronized void importHTMLDocumentToDatabase(String url, String htmlDocString) throws SQLException{
		
		//test connection, if not existed, create new
		if(!isConnectionEstablished())
			setup();
		
		conn.setAutoCommit(false);
		PreparedStatement pstmt = conn.prepareStatement("INSERT INTO document(url,created) VALUES (?,now());", PreparedStatement.RETURN_GENERATED_KEYS);
		pstmt.setString(1, url);
		pstmt.execute();
		
		ResultSet keyResult = pstmt.getGeneratedKeys();
		keyResult.next();
		int docID = keyResult.getInt(1);
		
		pstmt = conn.prepareStatement("INSERT INTO htmlstorage(docid,html) VALUES (?,?);");
		pstmt.setInt(1, docID);
		pstmt.setString(2, htmlDocString);
		pstmt.execute();
		
		conn.commit();
		conn.setAutoCommit(true);
	}
	
	public static void importSnippetToDatabase(int docid, int orderid, int snippetType, String snippetString) throws SQLException{
		//test connection, if not existed, create new
		if(!isConnectionEstablished())
			setup();
		
		PreparedStatement pstmt = conn.prepareStatement("INSERT INTO snippetdata(docid,orderid,type,data) VALUES (?,?,?,?);");
		pstmt.setInt(1, docid);
		pstmt.setInt(2, orderid);
		pstmt.setInt(3, snippetType);
		pstmt.setString(4, snippetString);
		pstmt.execute();
		
	}
	
	
	public static void closeConnection() throws SQLException{
		if(conn!=null && !conn.isClosed())
			conn.close();
	}
	
}
