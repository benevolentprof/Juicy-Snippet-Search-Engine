package edu.uci.ics.websnippetrepository.searcher;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
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

public class WebSearcher {

	/*------Configuration------*/
	public static final String DOC_INDEX_DIR = "html_indexes";
	public static final String SNIPPET_INDEX_DIR = "indexes";
	public static final int MAX_SHOWN_RESULTS = 10;
	public static final HashMap<String,Float> BOOST_TABLE = new HashMap<String, Float>();
	public static final String[] SNIPPET_FIELDS = new String[]{"title","text","code"}; 
	static{
		BOOST_TABLE.put("title",(float) 1.0);
		BOOST_TABLE.put("text", (float) 1.0);
		BOOST_TABLE.put("code", (float) 1.0);
	}
	public static final int MAX_FETCH_DOCUMENT = 1000;
	public static final int MAX_FETCH_SNIPPET  = 1000;
	public static final int MAX_LENGTH_DISPLAY_TEXT = 50;
	public static final int MAX_LENGTH_DISPLAY_CODE = 50;
	/*------End of Configuration------*/
	
	/** do highlight on search result or not */
	private boolean doHighLight;
	
	private IndexSearcher docSearcher;
	private IndexSearcher searcher;

	private QueryParser queryDocParser;
	private MultiFieldQueryParser queryParser;
	
	/**
	 * Initialization this searcher
	 */
	public WebSearcher() throws IOException {
		//initialize
		//initialize Lucene search system
		File docIndexDir = new File(DOC_INDEX_DIR);
		File snippetIndexDir = new File(SNIPPET_INDEX_DIR); 
		
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
		
		//initialize html index searcher
		Directory docfsDir = FSDirectory.open(docIndexDir);
		docSearcher = new IndexSearcher(docfsDir, true);
				
		//initialize index searcher
		Directory fsDir = FSDirectory.open(snippetIndexDir);
		searcher = new IndexSearcher(fsDir, true);
		
		queryDocParser = new QueryParser(Version.LUCENE_30, "content", analyzer);
		queryParser = new MultiFieldQueryParser(Version.LUCENE_30, SNIPPET_FIELDS, analyzer, BOOST_TABLE);
		
	}
	
	/**
	 * Search method using for free-text search query
	 * @param query		free-text search query
	 * @return	Snippet search result in form of array of SearchResultEntity
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public SearchResultEntity[] search(String queryStr) throws ParseException, IOException{
		
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
		SearchResultEntity[] resultsArray = new SearchResultEntity[snippetResultArray.length];
		int count=0;
		for(ScoreDoc iter:snippetResultArray){
			Document document = searcher.doc(iter.doc);
			int snippetid = Integer.parseInt(document.get("snippetid"));
			int docid = Integer.parseInt(document.get("docid"));
			double score = iter.score;
			String url = document.get("url");
			String title = document.get("title");
			String text = document.get("text");
			String code = document.get("code");
			
			StringBuilder shownText = new StringBuilder(StringEscapeUtils.escapeHtml(text));
			StringBuilder shownCode = new StringBuilder(code);
			
			//Find position to highlight
			if(doHighLight){
				Set<Term> queryTerms = new HashSet<Term>();
				query.extractTerms(queryTerms);
				Set<String> queryTermSet = new HashSet<String>(); //reduce duplicated term
				for(Term t:queryTerms)
					queryTermSet.add(t.text().toLowerCase());
				
				for(String term:queryTermSet){
					int startIndex=0;
					int foundIndex=0;
					while(-1==(foundIndex=shownText.indexOf(term, startIndex))){
						shownText.insert(foundIndex, "<em>");
						shownText.insert(foundIndex+term.length()+4, "</em>");
						startIndex = foundIndex+term.length()+8;
					}
					
					startIndex=0;
					foundIndex=0;
					while(-1==(foundIndex=shownCode.indexOf(term, startIndex))){
						shownCode.insert(foundIndex, "<em>");
						shownCode.insert(foundIndex+term.length()+4, "</em>");
						startIndex = foundIndex+term.length()+8;
					}
						
				}
			}
			
			
			//encapsulate all fields into one result 
			SearchResultEntity result = new SearchResultEntity();
			result.setSnippetid(snippetid);
			result.setDocid(docid);
			result.setUrl(url);
			result.setTitle(StringEscapeUtils.escapeHtml(title));
			result.setCode(shownCode.toString());
			result.setText(shownText.toString());
			result.setScore(score);
			
			//set one result into array of results
			resultsArray[count] = result;
			
			count++;
		}
		
		return resultsArray;
	}
	
	/**
	 * Search method using for free-text search query, plus code specific query
	 * such as class:[terms], method:[terms] and variable:[terms]. 
	 * @param query		free-text search query
	 * @param codeSpecificQuery  array of code specific query
	 * 		order by:
	 * 			<li>declared class name</li>
	 * 			<li>referenced class name</li>
	 * 			<li>extended/implemented class name<li>
	 * 			<li>declared method name</li>
	 * 			<li>referenced method name</li>
	 * 			<li>return type</li>
	 * 			<li>variable name</li>
	 * 			<li>comment</li>
	 * 			
	 * @return	Snippet search result in form of array of SearchResultEntity
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public SearchResultEntity[] search(String query,String[] codeSpecificQuery) throws ParseException, IOException{
		
		if (codeSpecificQuery==null || codeSpecificQuery.length==0)
			return search(query);
		
		StringBuilder queryBuilder = new StringBuilder(query);
		switch(codeSpecificQuery.length){
			case 8: queryBuilder.append(" comment:("+codeSpecificQuery[7]+")");
			case 7: queryBuilder.append(" variable:("+codeSpecificQuery[6]+")");
			case 6: queryBuilder.append(" return:("+codeSpecificQuery[5]+")");
			case 5: queryBuilder.append(" methodused:("+codeSpecificQuery[4]+")");
			case 4: queryBuilder.append(" method:("+codeSpecificQuery[3]+")");
			case 3: queryBuilder.append(" extends:("+codeSpecificQuery[2]+")");
			case 2: queryBuilder.append(" classused:("+codeSpecificQuery[1]+")");
			case 1: queryBuilder.append(" class:("+codeSpecificQuery[0]+")");
			default:
				//do nothing
				System.err.println("Lenght of String Array does not match");
		}
				
		return search(queryBuilder.toString());
	}
	
	
	/**
	 * Set whether the search results should be highlight(default is off).
	 * @param doHighlight
	 */
	public void setShowHighlight(boolean doHighlight){
		this.doHighLight = doHighlight;
	}
}

