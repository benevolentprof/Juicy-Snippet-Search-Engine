package edu.uci.ics.websnippetrepository.indexer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import edu.uci.ics.websnippetrepository.ImportToDatabase;

public class ClosestParagraphExtractor {

	/**
	 * MAXIMUM snippet length according to this link
	 * http://www.sagerock.com/blog/title-tag-meta-description-length/
	 */
	public static final int MAX_SNIPPET_LENGTH = 160;	
	
	
	public static void extractingParagraphBefore() throws SQLException{
		
		/* create connection */
		Connection conn = ImportToDatabase.getConnection(true);
		Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
		
		/* query randomly 5 codesnippet's texts */
		ResultSet rs = stmt.executeQuery("SELECT codesnippetid,`text`,paragraphbefore FROM codesnippet");
		//ResultSet rs = stmt.executeQuery("SELECT codesnippetid,`text` FROM codesnippet WHERE codesnippetid=94392");
		
		int count=0;
		
		while(rs.next()){
			//int codesnippetid = rs.getInt("codesnippetid");
			String text = rs.getString("text");
						
			//clean blank lines
			String paragraph = text.replaceAll("\r\n","\n");
			paragraph = text.replaceAll("\r","\n");
			paragraph = text.replaceAll("\n[ \t]*\n","\n\n");

			//remove trailing with blank lines
			int endingWithoutNewLine = paragraph.length()-1;
			while(endingWithoutNewLine>0 && !Character.isLetterOrDigit(paragraph.charAt(endingWithoutNewLine)) ){
				endingWithoutNewLine--;
			}
			
			paragraph = paragraph.substring(0, endingWithoutNewLine+1);
			
			//find the first new line from end of text
			int startPoint = paragraph.lastIndexOf("\n\n");
			
			if(startPoint > -1){
				paragraph = paragraph.substring(startPoint+2);
			}
			
			//remove all new lines
			paragraph = paragraph.replaceAll("\\s+", " ");
			
			if (paragraph.length()>MAX_SNIPPET_LENGTH){
				paragraph = paragraph.substring(0,MAX_SNIPPET_LENGTH);
				
				//find last non-word character from the max length position, and remove
				paragraph = paragraph.replaceFirst("\\w+$", "");
			}
			
			paragraph = paragraph.trim();
			
			/*System.out.println("==========="+codesnippetid+"===========");
			System.out.println("==========="+text+"===========");
			System.out.println("==========="+paragraph+"===========");
			*/
			
			rs.updateString("paragraphbefore",paragraph);
			rs.updateRow();
			
			System.out.println("DONE: "+(count++));
		}
		
		
		conn.close();
	}
	
	
	public static void main(String[] args) throws SQLException {
		extractingParagraphBefore();
	}

}
