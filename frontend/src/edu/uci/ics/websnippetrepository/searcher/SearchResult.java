package edu.uci.ics.websnippetrepository.searcher;

import java.util.Set;

public class SearchResult {
	
	protected String queryString;
	
	protected String query_snippet_index;
	protected String query_document_index;
	
	protected Set<String> queryTerms;
	
	protected long timeused;
	
	protected int numSearchResults;
	
	protected SearchResultEntity[] searchResult;
	
	protected int startResultIndex;

	/** Formulated query string
	 * @return the queryString
	 */
	public String getQueryString() {
		return queryString;
	}

	/** Internally use
	 * @return the query_snippet_index
	 */
	public String getQuery_snippet_index() {
		return query_snippet_index;
	}

	/** Internally use
	 * @return the query_document_index
	 */
	public String getQuery_document_index() {
		return query_document_index;
	}

	/** set of terms in query, regardless of fields
	 * @return the queryTerms
	 */
	public Set<String> getQueryTerms() {
		return queryTerms;
	}

	/** time used for searching
	 * @return the timeused
	 */
	public long getTimeused() {
		return timeused;
	}

	/** total number search result
	 * @return the numReturnResults
	 */
	public int getNumSearchResults() {
		return numSearchResults;
	}

	/** This is filteredd search result after done paging.
	 * @return the searchResult
	 */
	public SearchResultEntity[] getSearchResult() {
		return searchResult;
	}

	/** Return starting index of search result;
	 * @return the startResultIndex
	 */
	public int getStartResultIndex() {
		return startResultIndex;
	}
	
}
