package edu.uci.ics.websnippetrepository.test;

import java.io.ByteArrayInputStream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import edu.uci.ics.websnippetparser.algorithms.CodeExtractor;


public class TestSnippetParser {

	public static void main(String[] args) {
		
		CodeExtractor myCode = new CodeExtractor(4);
		//myCode.parseFile("http://java.sun.com/docs/books/tutorial/java/nutsandbolts/for.html");
		myCode.parseFile("http://www.javadeveloper.co.in/java-example/java-hashmap-example.html");
		
		String docString = myCode.getDoc();		
		try {
			XPath xPath = XPathFactory.newInstance().newXPath();
			XPathExpression nodeXPathExpr = xPath.compile("/codesnippets/text | /codesnippets/sourcecode");
			InputSource inputSource = new InputSource(new ByteArrayInputStream(docString.getBytes()));
			
			NodeList allNodes = (NodeList) nodeXPathExpr.evaluate(inputSource,XPathConstants.NODESET);
			
			int nodeNumber = allNodes.getLength();
			System.out.println("Text Found:: "+nodeNumber+" nodes.");
			System.out.println("========================================");
			
			for(int i=0;i<nodeNumber;i++){
				Node node = allNodes.item(i);
				
				String content = node.getTextContent();
				//content = StringEscapeUtils.unescapeHtml(content).trim();
				System.out.println(node+":--->:"+node.getNodeName());
				if(node.getTextContent()!=null)
					System.out.println("--->"+content);
				System.out.println("========================================");
			}
			
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		
	}


}
