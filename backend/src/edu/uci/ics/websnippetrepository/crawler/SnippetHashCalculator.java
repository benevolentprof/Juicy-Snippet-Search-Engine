package edu.uci.ics.websnippetrepository.crawler;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.uci.ics.websnippetrepository.ImportToDatabase;
/** Create MD5 hashcode for code snippets and update the database
 * 
 * @author ptantiku
 */
public class SnippetHashCalculator {

	
	/**
	 * Calculate MD5 Hash for a code snippet 
	 * @param codeSnippet
	 * @return
	 */
	public static String calculateHash(String codeSnippet){
		MessageDigest m;
		try {
			m = MessageDigest.getInstance("MD5");
			
			//clean code snippet by removing all whitespace and newline
			codeSnippet = codeSnippet.replaceAll("\\s", "");
			byte[] data = codeSnippet.getBytes("UTF-8"); 
			m.update(data,0,data.length);
			BigInteger i = new BigInteger(1,m.digest());
			return String.format("%1$032X", i);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static void updateAllCodeSnippetHashInDatabase(){
		//setup LOG4J
		Logger logger = Logger.getLogger(SnippetHashCalculator.class);
		PropertyConfigurator.configure("log4j.properties");
			
		try {
			
			//setup connection and statement for database
			Connection conn = ImportToDatabase.getConnection(true);
			conn.setAutoCommit(true);
			
			Statement stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
									java.sql.ResultSet.CONCUR_UPDATABLE);
			stmt.setFetchSize(Integer.MIN_VALUE);	//fetch row-by-row
			conn.setReadOnly(false);
			stmt.setQueryTimeout(28800);
		
			ResultSet rs = stmt.executeQuery("SELECT codesnippetid,code,hash FROM codesnippet");
			
			int codeSnippetID;
			String codeSnippet;
			String hash;
			
			while(rs.next()){
				
				//get each document
				codeSnippetID = rs.getInt(1);
				codeSnippet = rs.getString(2);
				logger.debug("Processing CodeSnippetID "+codeSnippetID);
				
				//calculate hash and add it to database
				hash = calculateHash(codeSnippet);
				//stmt2.execute("UPDATE codesnippet SET hash='"+hash+"' WHERE codesnippetid='"+codeSnippetID+"'");
				
				//update table
				rs.updateString("hash", hash);
				rs.updateRow();
			}
			
			conn.close();
		} catch (SQLException e) {
			logger.error("Error Try To Query From The Database.",e);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
/*		String s;
		s = "This is a test.";
		System.out.println(s+" --> "+calculateHash(s));

		s = "This is a test";
		System.out.println(s+" --> "+calculateHash(s));
		
		s = "This is a   test.";
		System.out.println(s+" --> "+calculateHash(s));
		
		s = "This is   \t a \n test.";
		System.out.println(s+" --> "+calculateHash(s));
		
		s = "This is a test.";
		System.out.println(s+" --> "+calculateHash(s));
*/		
		updateAllCodeSnippetHashInDatabase();
		
	}

}
