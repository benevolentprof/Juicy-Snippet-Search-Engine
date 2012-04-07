package edu.uci.ics.websnippetrepository.crawler;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import edu.uci.ics.websnippetparser.algorithms.CodeExtractor;
import edu.uci.ics.websnippetrepository.ImportToDatabase;


/**
 * Extract all HTML documents stored in the database into several snippets, 
 * and then upload them back to the database.
 * @author ptantiku
 *
 */
public class SnippetExtractor {
	
	static final Logger logger = Logger.getLogger(SnippetExtractor.class);

	public static void main(String[] args) {
		
		//setup
		PropertyConfigurator.configure("log4j.properties");
		int docid=0;
		String htmlString;
		String xmlDocString;
		
		
		//setup CodeExtractor
		CodeExtractor myCode = new CodeExtractor(4);
		
		//setup xPart
		XPath xPath;
		XPathExpression nodeXPathExpr;
		try {
			xPath = XPathFactory.newInstance().newXPath();
			nodeXPathExpr = xPath.compile("/codesnippets/text | /codesnippets/sourcecode");
		} catch (XPathExpressionException e) {
			logger.fatal("Cannot Create XPATH",e);
			return;
		}
		
				
		try {
			Connection fetchingConn = ImportToDatabase.getConnection(true);
			Connection updatingConn = ImportToDatabase.getConnection(false);
			updatingConn.setAutoCommit(false);
			
			Statement stmt = fetchingConn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
		              				java.sql.ResultSet.CONCUR_READ_ONLY);
			stmt.setFetchSize(Integer.MIN_VALUE);	//fetch row-by-row
		
			ResultSet rs = stmt.executeQuery("SELECT docid,html FROM htmlstorage");
			while(rs.next()){
				
				try {
					//get each document
					docid = rs.getInt(1);
					htmlString = rs.getString(2);
					logger.debug("Processing DocID "+docid);
					
					//extract code snippet from a page into single XML document string.
					myCode.parseDocumentString(htmlString);
					xmlDocString = myCode.getDoc();
					
					//split the XML into snippets
					InputSource inputSource = new InputSource(new ByteArrayInputStream(xmlDocString.getBytes()));
					NodeList allNodes = (NodeList) nodeXPathExpr.evaluate(inputSource,XPathConstants.NODESET);
					int nodeCount = allNodes.getLength();
					
					//for each snippet
					for(int i=0;i<nodeCount;i++){
						Node node = allNodes.item(i);
						int nodeType = node.getNodeName().equals("text")?
										ImportToDatabase.NODETYPE_TEXT:
										ImportToDatabase.NODETYPE_SOURCECODE;
						String content = node.getTextContent();
						content = StringEscapeUtils.unescapeHtml(content).trim();
						logger.debug(String.format("%5d | %3d | %-10s | \"%s...\" ",docid,i,node.getNodeName(),(content.length()<=20?content:content.substring(0, 20))));
						
						ImportToDatabase.importSnippetToDatabase(docid, i, nodeType, content);
					}
					
					updatingConn.commit();
				} catch (XPathExpressionException e) {
					logger.error("XPath fails: docid="+docid,e);e.printStackTrace();
				} catch (DOMException e) {
					logger.error("XPath fails: docid="+docid,e);e.printStackTrace();
				} catch (SQLException e) {
					logger.error("Fail to SQL import snippets from document:"+docid,e);
				}
			}
			
			updatingConn.setAutoCommit(true);
			
			updatingConn.close();
			fetchingConn.close();
		} catch (SQLException e) {
			logger.error("Error Try To Query From The Database.",e);
		}
		
	}

}
