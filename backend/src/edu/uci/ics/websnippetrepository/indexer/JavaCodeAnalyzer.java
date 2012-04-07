package edu.uci.ics.websnippetrepository.indexer;

import java.io.Reader;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseTokenizer;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;

/**
 * Analyzer for indexing Java code for Lucene.
 * Code has been adapted from 
 * http://onjava.com/pub/a/onjava/2006/01/18/using-lucene-to-search-java-source.html?page=2
 * 
 * English Stopword list in resources came from 
 * http://www.ranks.nl/resources/stopwords.html
 * 
 * Java keywords came from 
 * http://java.sun.com/docs/books/tutorial/java/nutsandbolts/_keywords.html
 * 
 * @author ptantiku
 *
 */
public class JavaCodeAnalyzer extends Analyzer{
	
	private static Set<Object> javaKeywords;
	private static Set<Object> englishStopwords;

	public JavaCodeAnalyzer(){
		super();
		if(javaKeywords==null || englishStopwords==null)
			loadKeywords();
		
	}
	
	/**
	 * Load Java keywords and English stop words from resources to create set of stopwords for Lucene. 
	 */
	private static void loadKeywords(){
		LinkedList<String> wordList = new LinkedList<String>();
		
		//load java keywords
		Scanner scanner = new Scanner(JavaCodeAnalyzer.class.getResourceAsStream("/edu/uci/ics/websnippetrepository/resources/javakeywords.txt"));
		while(scanner.hasNext()){
			wordList.add(scanner.next());
		}
		scanner.close();
		javaKeywords = StopFilter.makeStopSet(wordList, true);
		
		wordList.clear();
		//load English stop words
		scanner = new Scanner(JavaCodeAnalyzer.class.getResourceAsStream("/edu/uci/ics/websnippetrepository/resources/englishstopwords.txt"));
		while(scanner.hasNext()){
			wordList.add(scanner.next());
		}
		scanner.close();
		englishStopwords = StopFilter.makeStopSet(wordList, true);
	}

	@Override
	/**
	 * Analyze the input data and remove stop words or java keywords
	 */
	public TokenStream tokenStream(String fieldName, Reader reader) {
		
		 if (fieldName.equals("code")){
	        return   new StopFilter(false,		//if it's code, remove JavaKeyword
	        			new LowerCaseTokenizer(reader),javaKeywords);
		} else if (fieldName.equals("text") ||
				   fieldName.equals("title") || 
				   fieldName.equals("content") || 
				   fieldName.equals("comment")){
	        return   new PorterStemFilter(		//remove common morphological and inflexional endings.
	        			new StopFilter(false,	//new StopFilter, without record the positions of the words removed
    						new LowerCaseTokenizer(reader),englishStopwords));
		} else {	//just for default case
			return new StopFilter(false,	//new StopFilter, without record the positions of the words removed
					new LowerCaseTokenizer(reader),englishStopwords);
		}
	}
}
