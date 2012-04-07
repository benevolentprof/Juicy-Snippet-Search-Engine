package edu.uci.ics.websnippetrepository.searcher;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import edu.uci.ics.websnippetrepository.indexer.JavaCodeAnalyzer;

public class ConsoleSearcher {

	private static final String DOC_INDEX_DIR = "html_indexes";
	private static final String SNIPPET_INDEX_DIR = "indexes";
	
	private static final int MAX_SHOWN_RESULTS = 10;
	private static final HashMap<String,Float> BOOST_TABLE = new HashMap<String, Float>();
	private static final String[] SNIPPET_FIELDS = new String[]{"title","text","code"}; 
	
	static{
		BOOST_TABLE.put("title",(float) 1.0);
		BOOST_TABLE.put("text", (float) 1.0);
		BOOST_TABLE.put("code", (float) 1.0);
	}
	
	private static final int MAX_FETCH_DOCUMENT = 1000;
	private static final int MAX_FETCH_SNIPPET  = 1000;
	
	private static final int MAX_LENGTH_DISPLAY_TEXT = 50;
	private static final int MAX_LENGTH_DISPLAY_CODE = 50;
	
	
	
	public static void main(String[] args) throws IOException, ParseException{
	
		//initialize Lucene search system
		File docIndexDir = new File(DOC_INDEX_DIR);
		File snippetIndexDir = new File(SNIPPET_INDEX_DIR); 
		
		//initialize html index searcher
		Directory docfsDir = FSDirectory.open(docIndexDir);
		IndexSearcher docSearcher = new IndexSearcher(docfsDir, true);
				
		//initialize index searcher
		Directory fsDir = FSDirectory.open(snippetIndexDir);
		IndexSearcher searcher = new IndexSearcher(fsDir, true);
		
		//initialize query analyzer with JavaCodeAnalyzer as default analyzer (for removing all stop-words)
		PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new JavaCodeAnalyzer());
		
		//setup analyzer for code-specific search, e.g. for package,imports,classes, etc.
		KeywordAnalyzer casesensitiveAnalyzer = new KeywordAnalyzer();	
		analyzer.addAnalyzer("package", casesensitiveAnalyzer);
		analyzer.addAnalyzer("import", casesensitiveAnalyzer);
		analyzer.addAnalyzer("class", casesensitiveAnalyzer);
		analyzer.addAnalyzer("classused", casesensitiveAnalyzer);
		analyzer.addAnalyzer("method", casesensitiveAnalyzer);
		analyzer.addAnalyzer("methodused", casesensitiveAnalyzer);
		analyzer.addAnalyzer("return", casesensitiveAnalyzer);
		analyzer.addAnalyzer("variable", casesensitiveAnalyzer);
		
		//initializer Query Parser
		MultiFieldQueryParser queryParser = new MultiFieldQueryParser(Version.LUCENE_30, SNIPPET_FIELDS, analyzer, BOOST_TABLE);
		QueryParser queryDocParser = new QueryParser(Version.LUCENE_30, "content", analyzer);
		
		//start running search engine
		Scanner scanner = new Scanner(System.in);
		System.out.print("Search:>");
		while(scanner.hasNextLine()){
			String queryStr = scanner.nextLine().trim();
			
			if(queryStr.equalsIgnoreCase("quit") || queryStr.equalsIgnoreCase("exit"))
				break;
			
			//initialize Query Parser
			Query query = queryParser.parse(queryStr);
			System.out.println("Snippet  QUERY: "+query.toString());
			
			String queryStrForDoc = queryStr.replaceAll("\\w+:", "");	//remove field specific query
			Query queryDoc = queryDocParser.parse(queryStrForDoc);
			System.out.println("Document QUERY: "+queryDoc.toString());
			
			//searching
			long startTime = System.currentTimeMillis();
			TopDocs resultSnippets = searcher.search(query, MAX_FETCH_SNIPPET);
			TopDocs resultDocs = docSearcher.search(queryDoc, MAX_FETCH_DOCUMENT);
			long endTime = System.currentTimeMillis();
			
			//setup document table for calculate weighting
			HashMap<Integer, Double> docScore = new HashMap<Integer, Double>();
			System.out.printf("Found %d documents in %d millisec\n",resultDocs.totalHits, endTime-startTime);
			System.out.println("Start weighting score...");
			double maxScore = resultDocs.getMaxScore();
			for(ScoreDoc iter:resultDocs.scoreDocs){
				Document document = docSearcher.doc(iter.doc);
				double score = iter.score;
				int docid = Integer.parseInt(document.get("docid"));
				docScore.put(docid, score/maxScore);
			}
			
			//update score in snippet results by multiply them with its document score
			ScoreDoc[] snippetResultArray = resultSnippets.scoreDocs;
			maxScore = resultSnippets.getMaxScore();
			for(ScoreDoc iter:snippetResultArray){
				Document document = searcher.doc(iter.doc);
				double score = iter.score / maxScore;
				int docid = Integer.parseInt(document.get("docid"));
				
				if(docScore.containsKey(docid)){
					iter.score = (float) (docScore.get(docid) * score);
				}	
				else
					iter.score = 0;
			}
			
			Arrays.sort(snippetResultArray, new ScoreDocComparator());
			
			
			//Showing Snippet Results
			System.out.printf("Found %d snippets in %d millisec\n", resultSnippets.totalHits, endTime-startTime);
			int count=0;
			for(ScoreDoc iter:snippetResultArray){
				Document document = searcher.doc(iter.doc);
				int snippetid = Integer.parseInt(document.get("snippetid"));
				double score = iter.score;
				String url = document.get("url");
				String text = document.get("text").replaceAll("[\\r\\n]", "");
				String code = document.get("code").replaceAll("[\\r\\n]", "");
				System.out.printf("%d|||snippetid:%5d|score:%.3f|url:%s\n",(count+1),snippetid,score,url); 
				
				//Find position to highlight
				Set<Term> queryTerms = new HashSet<Term>();
				query.extractTerms(queryTerms);
				Set<String> queryTermSet = new HashSet<String>(); //reduce duplicated term
				for(Term t:queryTerms)
					queryTermSet.add(t.text().toLowerCase());
				
				int minPosText = Integer.MAX_VALUE;
				int minPosCode = Integer.MAX_VALUE;
				for(String term:queryTermSet){
					int newPosText = text.toLowerCase().indexOf(term);
					int newPosCode = code.toLowerCase().indexOf(term);
					if(newPosText!=-1 && minPosText>newPosText)
						minPosText = newPosText;
					
					if(newPosCode!=-1 && minPosCode>newPosCode)
						minPosCode = newPosCode;
						
				}
				
				String shownText = text;
				if(minPosText!=Integer.MAX_VALUE)
					shownText = text.substring(minPosText);
				if(shownText.length()>MAX_LENGTH_DISPLAY_TEXT)
					shownText = shownText.substring(0,50);
				
				String shownCode = code;
				if(minPosCode!=Integer.MAX_VALUE)
					shownCode = code.substring(minPosCode);
				if(shownCode.length()>MAX_LENGTH_DISPLAY_CODE)
					shownCode = shownCode.substring(0,50);
				
				//printing results
				System.out.println("---------------------------------------------");
				System.out.println(shownText);
				System.out.println("---------------------------------------------");
				System.out.println(shownCode);
				System.out.println("=============================================");
				count++;
				if(count>=MAX_SHOWN_RESULTS)
					break;
			}
			
			
			System.out.print("Search:>");
		}
	}

}
