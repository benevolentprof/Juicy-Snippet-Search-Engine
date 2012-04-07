package edu.uci.ics.websnippetrepository.indexer;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

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
import edu.uci.ics.websnippetrepository.evaluation.GenerateSnippetFileForEvaluation;
/**
 * Code Snippet Indexer for creating an index of code snippet table for later search.
 * This file is similar to JavaCodeIndexer.java but this file attempt to combine 
 * snippet index and html index together into one index.
 *  
 * @author ptantiku
 *
 */
public class CodeSnippetIndexer {

	public static final String INDEX_DIR = "snippetindex";
	
	private static Logger logger;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configure("log4j.properties");
		logger = Logger.getLogger(GenerateSnippetFileForEvaluation.class);
		
		try {
			File indexDir = new File(INDEX_DIR);
			IndexWriter writer = new IndexWriter(
									new SimpleFSDirectory(indexDir), 
									new JavaCodeAnalyzer(), 
									true,
									IndexWriter.MaxFieldLength.UNLIMITED);
			
			indexCodeSnippet(writer);
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
		}
		
	}


	public static void indexCodeSnippet(IndexWriter writer) throws SQLException, IOException {
		//first query code snippet and its text related  from database
		Connection conn = ImportToDatabase.getConnection(true);
		Statement stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
  				java.sql.ResultSet.CONCUR_READ_ONLY);
		
		//first query code snippet and its text related  from database
		Connection conn2 = ImportToDatabase.getConnection(true);
		Statement stmt2 = conn2.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
  				java.sql.ResultSet.CONCUR_READ_ONLY);
		
		String allSnippetDataSQL = "SELECT t1.codesnippetid, t1.docid, url, title, code, text " +
										"FROM codesnippet t1 " +
										"INNER JOIN docwithcode t2 ON t1.docid=t2.docid " +
										"ORDER BY t1.docid ASC ";
		
		stmt.setFetchSize(Integer.MIN_VALUE);	//fetch row-by-row
		ResultSet rs = stmt.executeQuery(allSnippetDataSQL);
		
		int snippetid,docid;
		String textSnippet=null;
		String codeSnippet=null;
		String url=null;
		String title=null;
		
		int currentDocid = -1;
		String content="";
		
		int counter=0;
		
		while(rs.next()){
			snippetid = rs.getInt("codesnippetid");
			docid = rs.getInt("docid");
			url = rs.getString("url");
			title = StringEscapeUtils.unescapeHtml(rs.getString("title"));
			codeSnippet = rs.getString("code");
			textSnippet = StringEscapeUtils.unescapeHtml(rs.getString("text"));
			
			//create document to store in the index
			Document document = new Document();
			document.add(new Field("snippetid",String.valueOf(snippetid), Field.Store.YES, Field.Index.NO));
			document.add(new Field("docid",String.valueOf(docid), Field.Store.YES, Field.Index.NO));
			document.add(new Field("url", url, Field.Store.YES, Field.Index.NO ));
			document.add(new Field("title", title, Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("code", codeSnippet, Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("text", textSnippet, Field.Store.YES, Field.Index.ANALYZED));
						
			//parse java code
			addCodeKeywordIntoDocument(document, codeSnippet);
			
			//query for htmlstorage if docid is new
			if (docid!=currentDocid){
				currentDocid = docid;
				ResultSet rs2 = stmt2.executeQuery("SELECT html FROM htmlstorage WHERE docid="+docid);
				rs2.next();
				String html = rs2.getString("html");
				rs2.close();
				
				//initialize SourceForge's HTMLParser
			    StringBean stringExtractor = new StringBean();
			    Lexer lexer = new Lexer(html); 
			    Parser p = new Parser(lexer);
			    try {
					p.visitAllNodesWith(stringExtractor);
				} catch (ParserException e) {
					logger.error("Error when trying to extract HTML from docid="+docid,e);
				}
			    content = stringExtractor.getStrings();
			    
			    if(content!=null && content.length()>0)
			    	content = StringEscapeUtils.unescapeHtml(content);
			    else
			    	content = "";
			}
			
			//add whole page content into index
			document.add(new Field("content", content, Field.Store.NO, Field.Index.ANALYZED));
			
			logger.debug(String.format("snippet:%5d|docid:%5d|url:%s",snippetid,docid,url));
			writer.addDocument(document);
			
			counter++;
			if(counter%100==0)
				System.gc();
		}
		
		rs.close();
		conn.close();
		conn2.close();
	}
	
	public static void addCodeKeywordIntoDocument(Document document,String codeSnippet){
		//parse java code into separated categories
		JavaASTVisitor javaVisitor = JavaCodeParser.parseJavaCode(codeSnippet);
		
		//create storage for splitted words (from using camel-case splitter)
		LinkedList<String> splitWords = new LinkedList<String>();
		
		//package
		if(javaVisitor.packageStr!=null && javaVisitor.packageStr.trim().length()>0);
			document.add(new Field("package",javaVisitor.packageStr.toLowerCase(),Field.Store.NO,Field.Index.NOT_ANALYZED));
		
		//imports
		for(String iter:javaVisitor.importsList)
			document.add(new Field("import",iter.toLowerCase(),Field.Store.NO,Field.Index.NOT_ANALYZED));
		
		//class declared
		for(String iter:javaVisitor.classesList){
			document.add(new Field("class",iter.toLowerCase(),Field.Store.NO,Field.Index.NOT_ANALYZED));
			splitWords.addAll(JavaCodeParser.camelcaseSplit(iter));
		}
		
		//class extends/implements
		for(String iter:javaVisitor.extendsList){
			document.add(new Field("extends",iter.toLowerCase(),Field.Store.NO,Field.Index.NOT_ANALYZED));
			splitWords.addAll(JavaCodeParser.camelcaseSplit(iter));
		}
		
		//class used
		for(String iter:javaVisitor.classesUsedList){
			document.add(new Field("classused",iter.toLowerCase(),Field.Store.NO,Field.Index.NOT_ANALYZED));
			splitWords.addAll(JavaCodeParser.camelcaseSplit(iter));
		}
		
		//method declare
		for(String iter:javaVisitor.methodsList){
			document.add(new Field("method",iter.toLowerCase(),Field.Store.NO,Field.Index.NOT_ANALYZED));
			splitWords.addAll(JavaCodeParser.camelcaseSplit(iter));
		}
		
		//method return
		for(String iter:javaVisitor.returnsList){
			document.add(new Field("return",iter.toLowerCase(),Field.Store.NO,Field.Index.NOT_ANALYZED));
			splitWords.addAll(JavaCodeParser.camelcaseSplit(iter));
		}
		
		//method used
		for(String iter:javaVisitor.methodsCalledList){
			document.add(new Field("methodused",iter.toLowerCase(),Field.Store.NO,Field.Index.NOT_ANALYZED));
			splitWords.addAll(JavaCodeParser.camelcaseSplit(iter));
		}
		
		//variable declare
		for(String iter:javaVisitor.variablesList){
			if(iter.length()>1){	//ignore single-character variable
				document.add(new Field("variable",iter.toLowerCase(),Field.Store.NO,Field.Index.NOT_ANALYZED));
				splitWords.addAll(JavaCodeParser.camelcaseSplit(iter));
			}
		}
		
		//comment
		for(String iter:javaVisitor.commentsList){
			document.add(new Field("comment",StringEscapeUtils.unescapeHtml(iter),Field.Store.NO,Field.Index.ANALYZED));
		}
		
		//finally all the splitted words
		for(String iter:splitWords){
			document.add(new Field("splitword",iter.toLowerCase(),Field.Store.NO,Field.Index.NOT_ANALYZED));
		}
	}

}
