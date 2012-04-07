package edu.uci.ics.websnippetrepository.evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.uci.ics.websnippetrepository.ImportToDatabase;

public class GenerateSnippetFileForEvaluation {

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws FileNotFoundException 
	 * @throws UnsupportedEncodingException 
	 */
	public static void main(String[] args) throws SQLException, FileNotFoundException, UnsupportedEncodingException {
		String outFolder = "output/socket";
		String query = "socket";
		String sql = "select t2.docid,orderid,`type`, url, match(`data`) against ('"+query+"') as score " +
		"from snippetdata t2 " +
		"inner join document t1 on t1.docid=t2.docid " +
		"inner join docwithcode t3 on t1.docid=t3.docid " +
		"where " +
		"match(`data`) against ('"+query+"') " + 
		"group by t2.docid " +
		"having score = Max(score) " + 
		"order by score desc "+
		"limit 100;";
		
		int counter=0;
		
		PropertyConfigurator.configure("log4j.properties");
		Logger logger = Logger.getLogger(GenerateSnippetFileForEvaluation.class);
		
		//create output folder
		File outF = new File(outFolder);
		if(!outF.exists()){
			outF.mkdirs();
		}
		
		PrintWriter summaryWriter = new PrintWriter(String.format("%s/summary.csv", outFolder, counter),"UTF8");
		
		Connection firstConn = ImportToDatabase.getConnection(true);
		Statement stmt = firstConn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
  				java.sql.ResultSet.CONCUR_READ_ONLY);
		
		stmt.setFetchSize(Integer.MIN_VALUE);	//fetch row-by-row
		ResultSet rs = stmt.executeQuery(sql);
		
		Connection secondConn = ImportToDatabase.getConnection(true);
		PreparedStatement pstmt = secondConn.prepareStatement("SELECT orderid, type, `data` FROM snippetdata WHERE docid=? AND ?-2<=orderid AND orderid<=?+2 ;");
		
		//start to go through the data
		while(rs.next()){
			int docid = rs.getInt("docid");
			int orderid = rs.getInt("orderid");
			int type = rs.getInt("type");
			String url = rs.getString("url");
			
			logger.debug(String.format("%4d|%4d|%4d|%s", docid,orderid,type,url));
	
			//write summary for this result
			summaryWriter.println(docid+","+orderid+","+(type==ImportToDatabase.NODETYPE_SOURCECODE?"CODE":"TEXT")+","+url);
				
			//open file
			PrintWriter out = new PrintWriter(String.format("%s/snippetResult%04d.xml", outFolder, counter),"UTF8");
			
			//query to get text above code, code, and text below code
			pstmt.setInt(1, docid);
			pstmt.setInt(2, orderid);
			pstmt.setInt(3, orderid);
			ResultSet rs2 = pstmt.executeQuery();
			out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			out.println("<result>");
			out.println("<description>");
			out.println("<docid>"+docid+"</docid>");
			out.println("<orderid>"+orderid+"</orderid>");
			out.println("<type>"+(type==ImportToDatabase.NODETYPE_SOURCECODE?"CODE":"TEXT")+"</type>");
			out.println("<url>"+url+"</url>");
			out.println("</description>");
			while(rs2.next()){
				int orderid2 = rs2.getInt("orderid");
				int type2 = rs2.getInt("type");
				String typeStr = type2==ImportToDatabase.NODETYPE_SOURCECODE?"CODE":"TEXT";
				String snippet = rs2.getString("data");
				snippet = org.apache.commons.lang.StringEscapeUtils.escapeXml(snippet);
				
				logger.debug(String.format("--->%4d|%4s", orderid2,typeStr));
				
				out.println("<"+typeStr+" orderid=\""+orderid2+"\" >");
				out.println(snippet);
				out.println("</"+typeStr+">");
			}
			out.println("</result>");
			
			rs2.close();
			out.close();
			
			counter++;
		}
		
		firstConn.close();
		secondConn.close();
		
		summaryWriter.close();
		
		logger.info("Total File Written = "+counter);
	}

}
