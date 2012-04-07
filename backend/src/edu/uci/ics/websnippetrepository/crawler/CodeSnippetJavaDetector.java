package edu.uci.ics.websnippetrepository.crawler;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.uci.ics.websnippetrepository.ImportToDatabase;

public class CodeSnippetJavaDetector {
	
	private static HashSet<String> keywordSet = new HashSet<String>();
//	private static final String[] JAVA_KEYWORDS ={
//	    "abstract", "continue", "for", "new","switch","assert","default", "goto",
//	    "package", "synchronized", "boolean", "do", "if", "private", "this",
//	    "break", "double", "implements", "protected", "throw", "byte", "else",
//	    "import", "public", "throws", "case", "enum","instanceof",
//	    "return", "transient", "catch", "extends", "int", "short",
//	    "try", "char", "final", "interface", "static", "void",
//	    "class", "finally", "long", "strictfp", "volatile", "const",
//	    "float", "native", "super", "while"};

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws FileNotFoundException 
	 * @throws UnsupportedEncodingException 
	 */
	public static void main(String[] args) throws SQLException, FileNotFoundException, UnsupportedEncodingException {
		
		fillKeywordSet();
		
		String sql = "SELECT docid, orderid, data, keywordratio " +
						"FROM snippetdata " + 
						"WHERE type = 1 " +
						"ORDER BY docid ASC ;";

		int totalNumberOfTerms = 0;
		int numberOfMatchingKeywords = 0;
		
		PropertyConfigurator.configure("log4j.properties");
		Logger logger = Logger.getLogger(CodeSnippetJavaDetector.class);
				
		logger.debug("Getting First Connection");
		Connection firstConn = ImportToDatabase.getConnection(true);
		Statement firstStmt = firstConn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
  														java.sql.ResultSet.CONCUR_UPDATABLE);
		firstStmt.setFetchSize(Integer.MIN_VALUE);	//fetch row-by-row
		firstConn.setReadOnly(false);
		firstStmt.setQueryTimeout(28800);
		logger.debug("Start Querying First Connection");
		ResultSet rs = firstStmt.executeQuery(sql);
		
		int counter = 0;
		
		logger.debug("Start Getting ResultSet from First Connection");
		//start to go through the data
		while(rs.next()){
			String content=rs.getString("data");

			//reset counting values
			totalNumberOfTerms = 0;
			numberOfMatchingKeywords = 0;	
			Scanner eachTerm = new Scanner(content);
			eachTerm.useDelimiter("\\W+");

			// while read code snippet using scanner
		    while (eachTerm.hasNext()) {
		    	String term = eachTerm.next().trim();
		    	if (term != null && term.length()>0) {

		    		// increase the total number of terms
		    		totalNumberOfTerms++;
		    		// calculate pattern matching
		    		if (keywordSet.contains(term)){
		    			// if a term is java keyword, increase the number of matching
		    			numberOfMatchingKeywords++;
		    		}        
		    	}
		    }
		    eachTerm.close();

			// calculate the ratio of matched java keyword and total number of terms
			double ratio = 0.0;
			if (totalNumberOfTerms != 0){
				ratio = numberOfMatchingKeywords*1.0/totalNumberOfTerms;
			}	
			
			//update table
			rs.updateDouble("keywordratio", ratio);
			rs.updateRow();
			
			counter++;
			if(counter%100==0){
				System.gc();
				logger.debug(String.format("%4d|%4d|%4d|%.2f", counter, totalNumberOfTerms,numberOfMatchingKeywords,ratio));
			}
		}
		
		rs.close();

		firstStmt.close();		
		firstConn.close();
		
		logger.info("Total File Written = "+counter);
	}
	public static void fillKeywordSet(){
		Scanner scanner = new Scanner(CodeSnippetJavaDetector.class.getResourceAsStream("/edu/uci/ics/websnippetrepository/resources/javakeywords.txt"));
		while(scanner.hasNext()){
			keywordSet.add(scanner.next());
		}
		scanner.close();
	}
	
}
