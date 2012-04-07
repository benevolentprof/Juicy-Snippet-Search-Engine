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
	public static String DOC_INDEX_DIR = "html_indexes";
	public static String SNIPPET_INDEX_DIR = "indexes";
	public static final int MAX_SHOWN_RESULTS = 10;
	public static final HashMap<String,Float> BOOST_TABLE = new HashMap<String, Float>();
	public static final String[] SNIPPET_FIELDS = new String[]{
			"title",
			//"text",
			"paragraph",
			"code"}; 
	static{
		BOOST_TABLE.put("title",(float) 1.0);
		//BOOST_TABLE.put("text", (float) 1.0);
		BOOST_TABLE.put("paragraph", (float) 1.0);
		BOOST_TABLE.put("code", (float) 1.0);
	}
	public static final int MAX_FETCH_DOCUMENT = 1000;
	public static final int MAX_FETCH_SNIPPET  = 1000;
	public static final int MAX_LENGTH_DISPLAY_TEXT = 50;
	public static final int MAX_LENGTH_DISPLAY_CODE = 50;
	/*------End of Configuration------*/
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
	 * @param startResultIndex	starting index of the result to display (default=0)
	 * @param numResultShown	number of result will be shown
	 * @return	Snippet search result in form of a class of Search Result
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public SearchResult search(String queryStr, int startResultIndex, int numResultShown) throws ParseException, IOException{
		
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
		
		//SORT!
		Arrays.sort(snippetResultArray, new ScoreDocComparator());
		
		//Showing Snippet Results
		System.out.printf("Found %d snippets in %d millisec\n", resultSnippets.totalHits, endTime-startTime);
		int numberOfResult = snippetResultArray.length;
		
		//list all terms in query, this block will be used for text highlighting
		Set<Term> queryTerms = new HashSet<Term>();
		query.extractTerms(queryTerms);	
		Set<String> queryTermSet = new HashSet<String>(); //reduce duplicated term
		for(Term t:queryTerms){
			queryTermSet.add(t.text().toLowerCase());	
		}
		
		//Creating an entity of the whole result set
		SearchResult searchResult = new SearchResult();
		searchResult.queryString = queryStr;
		searchResult.query_snippet_index = query.toString();
		searchResult.query_document_index = queryDoc.toString();
		searchResult.numSearchResults = numberOfResult;
		searchResult.timeused = endTime-startTime;
		searchResult.queryTerms = queryTermSet;
		searchResult.startResultIndex = startResultIndex;
		
		//start processing for results
		int resultSize = (numberOfResult-(startResultIndex+numResultShown))>=0?numResultShown:(numberOfResult-(startResultIndex));
		SearchResultEntity[] resultsArray = new SearchResultEntity[resultSize];
		int count=0;
		for(int i=startResultIndex;i<numberOfResult && i<startResultIndex+numResultShown; i++){
			ScoreDoc iter = snippetResultArray[i];
			Document document = searcher.doc(iter.doc);
			int snippetid = Integer.parseInt(document.get("snippetid"));
			int docid = Integer.parseInt(document.get("docid"));
			double score = iter.score;
			String url = document.get("url");
			String title = document.get("title");
			String text = document.get("text");
			String code = document.get("code");
			String paragraph = document.get("paragraph");
			int samesnippetcount = Integer.parseInt(document.get("samesnippetcount"));
			
			//pre-format code & text snippet
			//remove all blank lines from code snippet
			code = code.replaceAll("[\r\n]([ \t]*[\r\n])+","\n");
						
			//HTML escaping all string 
			title = StringEscapeUtils.escapeHtml(title);
			text = StringEscapeUtils.escapeHtml(text);
			paragraph = StringEscapeUtils.escapeHtml(paragraph);
			
			//encapsulate all fields into one result 
			SearchResultEntity result = new SearchResultEntity();
			result.setSnippetid(snippetid);
			result.setDocid(docid);
			result.setUrl(url);
			result.setTitle(title);
			result.setCode(code);
			result.setText(text);
			result.setParagraph(paragraph);
			result.setScore(score);
			result.setSamesnippetcount(samesnippetcount);
			
			//set one result into array of results
			resultsArray[count] = result;
			
			count++;
		}
		
		close();
		
		//set SearchResultEntity to SearchResult
		searchResult.searchResult = resultsArray;
		
		return searchResult;
	}
	
	/**
	 * Search method using for free-text search query, plus code specific query
	 * such as class:[terms], method:[terms] and variable:[terms]. 
	 * @param query		free-text search query
	 * @param codeSpecificQuery  array of code specific query
	 * 		order by:
	 * 			<li>package</li>
	 * 			<li>import</li>
	 * 			<li>declared class name</li>
	 * 			<li>referenced class name</li>
	 * 			<li>extended/implemented class name<li>
	 * 			<li>declared method name</li>
	 * 			<li>referenced method name</li>
	 * 			<li>return type</li>
	 * 			<li>variable name</li>
	 * 			<li>comment</li>
	 * @param startResultIndex	starting index of the result to display (default=0)
	 * @param numResultShown	number of result will be shown		
	 * @return	Snippet search result in form of SearchResult
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public SearchResult search(String query,String[] codeSpecificQuery, int startResultIndex, int numResultShown) throws ParseException, IOException{
		
		if (codeSpecificQuery==null || codeSpecificQuery.length==0)
			return search(query, startResultIndex, numResultShown);
		
		StringBuilder queryBuilder = new StringBuilder(query);
		switch(codeSpecificQuery.length){
			case 10:if(codeSpecificQuery[9]!=null&&codeSpecificQuery[9].trim().length()>0)
						queryBuilder.append(" comment:("	+codeSpecificQuery[9]+")");
			case 9: if(codeSpecificQuery[8]!=null&&codeSpecificQuery[8].trim().length()>0)
						queryBuilder.append(" variable:("	+codeSpecificQuery[8]+")");
			case 8: if(codeSpecificQuery[7]!=null&&codeSpecificQuery[7].trim().length()>0)
						queryBuilder.append(" return:("		+codeSpecificQuery[7]+")");
			case 7: if(codeSpecificQuery[6]!=null&&codeSpecificQuery[6].trim().length()>0)
						queryBuilder.append(" methodused:("	+codeSpecificQuery[6]+")");
			case 6: if(codeSpecificQuery[5]!=null&&codeSpecificQuery[5].trim().length()>0)
						queryBuilder.append(" method:("		+codeSpecificQuery[5]+")");
			case 5: if(codeSpecificQuery[4]!=null&&codeSpecificQuery[4].trim().length()>0)
						queryBuilder.append(" extends:("	+codeSpecificQuery[4]+")");
			case 4: if(codeSpecificQuery[3]!=null&&codeSpecificQuery[3].trim().length()>0)
						queryBuilder.append(" classused:("	+codeSpecificQuery[3]+")");
			case 3: if(codeSpecificQuery[2]!=null&&codeSpecificQuery[2].trim().length()>0)
						queryBuilder.append(" class:("		+codeSpecificQuery[2]+")");
			case 2: if(codeSpecificQuery[1]!=null&&codeSpecificQuery[1].trim().length()>0)
						queryBuilder.append(" import:("		+codeSpecificQuery[1]+")");
			case 1: if(codeSpecificQuery[0]!=null&&codeSpecificQuery[0].trim().length()>0)
						queryBuilder.append(" package:("	+codeSpecificQuery[0]+")");
					break;
			default:
				//do nothing
				System.err.println("Lenght of String Array does not match");
		}
				
		return search(queryBuilder.toString(), startResultIndex, numResultShown);
	}
	
	/**
	 * Search method using for free-text search query
	 * @param query		free-text search query
	 * @return	Snippet search result in form of SearchResult
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public SearchResult search(String queryStr) throws ParseException, IOException{
		return search(queryStr,0,WebSearcher.MAX_SHOWN_RESULTS);
	}
	
	/**
	 * Search method using for free-text search query, plus code specific query
	 * such as class:[terms], method:[terms] and variable:[terms]. 
	 * @param query		free-text search query
	 * @param codeSpecificQuery  array of code specific query
	 * 		order by:
	 * 			<li>package</li>
	 * 			<li>import</li>
	 * 			<li>declared class name</li>
	 * 			<li>referenced class name</li>
	 * 			<li>extended/implemented class name<li>
	 * 			<li>declared method name</li>
	 * 			<li>referenced method name</li>
	 * 			<li>return type</li>
	 * 			<li>variable name</li>
	 * 			<li>comment</li>
	 * 			
	 * @return	Snippet search result in form of SearchResult
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public SearchResult search(String query,String[] codeSpecificQuery) throws ParseException, IOException{
		return search(query,codeSpecificQuery,0,WebSearcher.MAX_SHOWN_RESULTS);
	}
		
	/**
	 * Close every files as finishing search.
	 */
	public void close(){
		try {
			docSearcher.close();
			searcher.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

