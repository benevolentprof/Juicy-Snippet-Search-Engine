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

import edu.uci.ics.websnippetrepository.ImportToDatabase;
import edu.uci.ics.websnippetrepository.evaluation.GenerateSnippetFileForEvaluation;

public class JavaCodeIndexer {

	public static final String INDEX_DIR = "indexes";
	
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
		
		String allSnippetDataSQL = "SELECT t1.codesnippetid, t1.docid, url, title, code, text, paragraphbefore, samesnippetlist, samesnippetcount " +
										"FROM codesnippet t1 " +
										"INNER JOIN snippetgroup t2 " +
										"ON t1.codesnippetid=t2.codesnippetid ";
		
		stmt.setFetchSize(Integer.MIN_VALUE);	//fetch row-by-row
		ResultSet rs = stmt.executeQuery(allSnippetDataSQL);
		
		int snippetid,docid;
		String textSnippet=null;
		String codeSnippet=null;
		String url=null;
		String title=null;
		String paragraph=null;
		String samesnippetlist;
		int samesnippetcount=1;
		
		int counter=0;
		
		while(rs.next()){
			snippetid = rs.getInt("codesnippetid");
			docid = rs.getInt("docid");
			url = rs.getString("url");
			title = StringEscapeUtils.unescapeHtml(rs.getString("title"));
			codeSnippet = rs.getString("code");
			textSnippet = StringEscapeUtils.unescapeHtml(rs.getString("text"));
			paragraph = rs.getString("paragraphbefore");;
			samesnippetlist = rs.getString("samesnippetlist");
			samesnippetcount = rs.getInt("samesnippetcount");
			
			
			//create document to store in the index
			Document document = new Document();
			document.add(new Field("snippetid",String.valueOf(snippetid), Field.Store.YES, Field.Index.NO));
			document.add(new Field("docid",String.valueOf(docid), Field.Store.YES, Field.Index.NO));
			document.add(new Field("url", url, Field.Store.YES, Field.Index.NO ));
			document.add(new Field("title", title, Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("code", codeSnippet, Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("text", textSnippet, Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("paragraph", paragraph, Field.Store.YES, Field.Index.ANALYZED));
			document.add(new Field("samesnippetlist", samesnippetlist, Field.Store.YES, Field.Index.NO));
			document.add(new Field("samesnippetcount", String.valueOf(samesnippetcount), Field.Store.YES, Field.Index.NO));
						
			//parse java code
			addCodeKeywordIntoDocument(document, codeSnippet);
			
			logger.debug(String.format("snippet:%5d|docid:%5d|url:%s",snippetid,docid,url));
			writer.addDocument(document);
			
			counter++;
			if(counter%100==0)
				System.gc();
		}
		
		rs.close();
		conn.close();
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
