package edu.uci.ics.websnippetrepository.indexer;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SimpleFSDirectory;
import org.htmlparser.Parser;
import org.htmlparser.beans.StringBean;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.util.ParserException;

import edu.uci.ics.websnippetrepository.ImportToDatabase;

/**
 * Index HTML documents from database which is stored in table "htmlstorage".
 * This code is using HTML parser from SourceForge's HTMLParser.
 * 
 * @author ptantiku
 * 
 */
public class HTMLDocumentIndexer {
	
	public static final String INDEX_DIR = "html_indexes";
	
	private static Logger logger;
	
	/**
	 * Load HTML documents from database and do indexing.
	 * 
	 * @param args
	 * @throws SQLException 
	 */
	public static void main(String[] args) {
		
		//setup Log4J
		PropertyConfigurator.configure("log4j.properties");
		logger = Logger.getLogger(HTMLDocumentIndexer.class);
		
		try {
			File indexDir = new File(INDEX_DIR);
			IndexWriter writer = new IndexWriter(
									new SimpleFSDirectory(indexDir), 
									new JavaCodeAnalyzer(), 
									true,
									IndexWriter.MaxFieldLength.UNLIMITED);
			
			indexHTMLDocument(writer);
			writer.optimize();
			writer.close();
		} catch (CorruptIndexException e) {		//when index is corrupt
			e.printStackTrace();
		} catch (LockObtainFailedException e) {	//when other writer is using the index
			e.printStackTrace();
		} catch (IOException e) {	//when directory can't be read/written
			e.printStackTrace();
		} catch (SQLException e) {	//when Database error occurs
			e.printStackTrace();
		} catch (InterruptedException e) {	//when Lucene's htmlparser gets interupted
			e.printStackTrace();
		} catch (ParserException e) {		//when SourceForge's HTMLParser gets error
			e.printStackTrace();
		}
		
	}

	private static void indexHTMLDocument(IndexWriter writer) throws SQLException, IOException, InterruptedException, ParserException {
		//Creating Database Connection
		Connection conn = ImportToDatabase.getConnection(true);
		Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		stmt.setFetchSize(Integer.MIN_VALUE);	//fetch row-by-row
		
		String allHTMLDataSQL = "SELECT t1.docid, url, title, html, groupid, groupcnt " +
									"FROM document t1 " +
									"INNER JOIN htmlstorage t2 ON t1.docid=t2.docid " +
									"INNER JOIN (SELECT groupid,min(docid) as docid,count(*) as groupcnt FROM pagegroup GROUP BY groupid) t3 ON t1.docid=t3.docid " +
									"ORDER BY t1.docid ASC;";
		
		//Query all HTML documents
		logger.info("Start querying for HTML documents");
		int counter=0;
		ResultSet rs = stmt.executeQuery(allHTMLDataSQL);
		while(rs.next()){
			int docid = rs.getInt("docid");
			String url = rs.getString("url");
			String title = StringEscapeUtils.unescapeHtml(rs.getString("title"));
			String html = rs.getString("html");
			int groupid = rs.getInt("groupid");
			int groupcnt = rs.getInt("groupcnt");
			
			logger.debug(String.format("docid:%5d|%s", docid, url));
			
			// make a new, empty document
		    Document doc = new Document();
		 
		    //initialize SourceForge's HTMLParser
		    StringBean stringExtractor = new StringBean();
		    Lexer lexer = new Lexer(html); 
		    Parser p = new Parser(lexer);
		    p.visitAllNodesWith(stringExtractor);
		    String content = stringExtractor.getStrings();
		    
		    
		    if(content!=null && content.trim().length()>0){
		    	//trim and unescape HTML format
		    	content = StringEscapeUtils.unescapeHtml(content).trim();
		    	
		    	//Add docid for future reference
		    	doc.add(new Field("docid", String.valueOf(docid),Field.Store.YES,Field.Index.NO));
		    
			    //Add the url, it will be indexed(searchable), but not be tokenized into words
			    doc.add(new Field("url", url, Field.Store.YES, Field.Index.NO));
			    
			    // Add the title as a field that it can be searched and that is stored.
			    doc.add(new Field("title", title, Field.Store.YES, Field.Index.ANALYZED));
			    
			    // Add the tag-stripped contents as a Reader-valued Text field so it will
			    // get tokenized and indexed.
			    doc.add(new Field("content", content, Field.Store.YES, Field.Index.ANALYZED));
			    
			    //add duplication info of the page
			    doc.add(new Field("groupid", String.valueOf(groupid), Field.Store.YES, Field.Index.NO));
			    doc.add(new Field("groupcnt", String.valueOf(groupcnt), Field.Store.YES, Field.Index.NO));
			    
			    
			    //write document into index
				writer.addDocument(doc);	
				counter++;
		    }
		}
		logger.info("All HTML documents parsing: DONE (total="+counter+")");
		
		conn.close();
	}

}
