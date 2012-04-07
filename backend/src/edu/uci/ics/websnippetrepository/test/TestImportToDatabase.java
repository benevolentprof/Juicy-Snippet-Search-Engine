package edu.uci.ics.websnippetrepository.test;


import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import edu.uci.ics.websnippetparser.algorithms.CodeExtractor;
import edu.uci.ics.websnippetrepository.ImportToDatabase;

public class TestImportToDatabase {

	private static String url="http://java.sun.com/docs/books/tutorial/java/nutsandbolts/for.html";
	private static String xmlDocString;
	private static Connection conn;
	private static int expectedNodeCount;
	private static boolean firstRun=true;
	private static int docID;
	
	@BeforeClass
	public static void setUpOnce() throws Exception {
		
		
		//extract code snippet from a page into single XML document string.
		CodeExtractor myCode = new CodeExtractor(1);
		myCode.parseFile(url);
		xmlDocString = myCode.getDoc();		
		
		//setup xPart
		XPath xPath = XPathFactory.newInstance().newXPath();
		XPathExpression nodeXPathExpr = xPath.compile("/codesnippets/text | /codesnippets/sourcecode");
		InputSource inputSource = new InputSource(new ByteArrayInputStream(xmlDocString.getBytes()));
		NodeList allNodes = (NodeList) nodeXPathExpr.evaluate(inputSource,XPathConstants.NODESET);
		expectedNodeCount = allNodes.getLength();
		
		//setup Mysql connection
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(
			"jdbc:mysql://localhost/codesnippet?user=root&password=w,ji^h");
			conn.setAutoCommit(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}

	@Test
	public void testCheckConnection(){
		if(!firstRun){
			Assert.assertNotNull("When it's not first run, Connection must not be null.",ImportToDatabase.isConnectionEstablished());
		}else{
			firstRun=true;
			Assert.assertTrue("First Run", true);
		}
	}
	
	@Test
	public void testImportXMLToDatabase(){
		
		//Run the import method
		try{
			ImportToDatabase.importXMLToDatabase(url, xmlDocString);
		}catch(SQLException e){
			e.printStackTrace();
			Assert.fail("SQL Exception occurs");
		}
	}
	
	@Test
	public void testCheckImportedData(){
		//Test the result
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT docid,url,created FROM document WHERE url='"+TestImportToDatabase.url+"'");
			
			Assert.assertTrue("Import to Document Table Success",rs!=null && rs.next());
			docID = rs.getInt(1);
			String url = rs.getString(2);
			Date createdDate = rs.getDate(3);
			Assert.assertEquals("Confirm imported data(URL).",TestImportToDatabase.url, url);
			System.out.println(docID+" | "+url+" | "+createdDate.toString());
			rs.close();
			
			rs = stmt.executeQuery("SELECT count(*) FROM snippetdata WHERE docid="+docID);
			rs.next();
			int actualCount= rs.getInt(1);
			Assert.assertEquals("Check Number of Nodes",expectedNodeCount, actualCount);
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	@AfterClass
	public static void tearDownOnce() throws Exception {
		ImportToDatabase.closeConnection();
		
		//clean up what just imported.
		Statement stmt = conn.createStatement();
		stmt.executeUpdate("DELETE FROM snippetdata WHERE docid="+docID+";");
		stmt.executeUpdate("DELETE FROM document WHERE docid="+docID+";");
		stmt.close();
		conn.close();
	}

}
