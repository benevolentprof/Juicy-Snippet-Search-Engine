package edu.uci.ics.websnippetrepository.crawler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.uci.ics.websnippetparser.report.Diff;
import edu.uci.ics.websnippetrepository.ImportToDatabase;

/**Using "diff" of all snippets in 2 pages in order to check duplication 
 * with 10% acceptable error differences.  
 * @author ptantiku
 * @deprecated Slow and Produces wrong result
 */
public class PageDuplicationChecker {

	static Logger logger; 
	
	/** percent of the breaking point whether two pages are the same */
	private static final double THRESHOLD_PERCENTAGE = 0.90;

	/**
	 * Query for all snippets from a specific document using docid
	 * @param docid
	 * @return ArrayList<String> object of all text&code snippets in the document.
	 */
	public static ArrayList<String> queryAllSnippetFromDocID(int docid){
		try {
			Connection conn = ImportToDatabase.getConnection(false);
			Statement stmt = conn.createStatement();
			
			ResultSet rs = stmt.executeQuery("SELECT data FROM snippetdata t1 " +
					"INNER JOIN docwithcode t2 ON t1.docid=t2.docid " +
					"WHERE t1.docid='"+docid+"' " +
					"ORDER BY t1.orderid ASC ");
			
			ArrayList<String> allSnippet = new ArrayList<String>();
			
			while(rs.next()){
				String snippet = rs.getString(1);
								
				allSnippet.add(snippet);
			}

			stmt.close();
			
			
			return allSnippet;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Breaks a snippet into lines
	 * @param str
	 * @return lines into a String array 
	 */
	public static String[] breakStringLines(String str){
		
		//clean the contiguous white spaces
		str = str.replaceAll("[ \t\\x0B\f]+", " ");
		
		//remove blank lines
		str = str.replaceAll("\n\\s*\n", "\n");
		
		String[] splitString = str.split("[\r\n]");
		return splitString;
	}
	
	/**
	 * Check whether 2 documents are the same using ratio of similar lines to all lines in documents.
	 * It compares snippet by snippet in line-level.  
	 * @param docid1  document id in database
	 * @param docid2  document id in database
	 * @return true if both document are the same
	 */
	public static boolean isDuplicate(int docid1, int docid2){

		
		ArrayList<String> document1 = queryAllSnippetFromDocID(docid1);
		ArrayList<String> document2 = queryAllSnippetFromDocID(docid2);
				
		int totalLinesInPage = 0;
		int totalSameLines = 0;
		
		//go through all snippet one-by-one in order
		for(int i=0;i<Math.min(document1.size(), document2.size());i++){
			
			//break each snippet into lines (and cleaning them)
			String[] linesSnippet1 = breakStringLines(document1.get(i));
			String[] linesSnippet2 = breakStringLines(document2.get(i));
			
			//do the diff and find how many lines are same
			Diff diffTool = new Diff(linesSnippet1, linesSnippet2);	
			Diff.summary summary = diffTool.diff_summary(false);
			//int numberOfLinesInSnippet1 = linesSnippet1.length;
			int numberOfLinesInSnippet2 = linesSnippet2.length; 
			
			int sameLines = numberOfLinesInSnippet2 - summary.added;
		
			totalLinesInPage += numberOfLinesInSnippet2;
			totalSameLines += sameLines;
			
		}
	
				
		//check percent of similarity of these pages, if it is above threshold , then it's duplicate. 
		if (totalSameLines*1.0 / totalLinesInPage >= THRESHOLD_PERCENTAGE)
			return true;
		else
			return false;
	}
	
	public static void checkDuplicationInDatabase(){
		try {
			int nextGroupID = 0;
			int nextDocID = 0;
			
			Connection conn = ImportToDatabase.getConnection(true);
			Connection conn2 = ImportToDatabase.getConnection(true);
			Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
			stmt.setFetchSize(Integer.MIN_VALUE);
			
			//first of all, get max number of groupid, and max docid for continuing process
			ResultSet rs = stmt.executeQuery("SELECT max(groupid),max(docid) FROM pagegroup");
			if(rs.next()==true){
				nextGroupID = rs.getInt(1)+1;
				nextDocID = rs.getInt(2)+1;
			}
			rs.close();
			
			//then query all the document id from database
			rs = stmt.executeQuery("SELECT docid FROM docwithcode WHERE docid>= "+nextDocID+" ORDER BY docid ASC");
			while(rs.next()){
				int docid = rs.getInt(1);
				
				//assigning current document to no group (default)
				int addToGroupID = -1; 
				
				//if it is first document, create new group
				if(nextGroupID == 0 ){
					addToGroupID = nextGroupID;
					nextGroupID ++;
				}
				else{
					//try query all grouped pages from database 
					Statement stmt2 = conn2.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
					//stmt2.setFetchSize(Integer.MIN_VALUE);
					ResultSet rs2 = stmt2.executeQuery("SELECT groupid, docid FROM pagegroup GROUP BY groupid");
					
					while(rs2.next()){
						int groupid = rs2.getInt(1);
						int docidInDB = rs2.getInt(2);
					
						//if this document matches with the one in this group,
						//mark this document to be added in this group and stop the loop
						if(isDuplicate(docid, docidInDB)){
							addToGroupID = groupid;
							break;
						}
					}
					
					//if no assigned groupid (no match with previous ones), create new group
					if(addToGroupID == -1){
						addToGroupID = nextGroupID;
						nextGroupID ++;
					}
					
					rs2.close();
					stmt2.close();
					
				}
				
				//try query all grouped pages from database 
				Statement updateStmt = conn2.createStatement();
				updateStmt.execute("INSERT INTO pagegroup(groupid,docid) VALUES ("+addToGroupID+","+docid+")");
				updateStmt.close();
				
				logger.debug("Assigning docID: "+docid+" ---> group: "+addToGroupID);
			}
			
			stmt.close();
			conn.close();
			conn2.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//setup LOG4J
		logger = Logger.getLogger(PageDuplicationChecker.class);
		PropertyConfigurator.configure("log4j.properties");
		
		checkDuplicationInDatabase();
		
	}

}
