package edu.uci.ics.websnippetrepository.searcher;

/**
 * Object for storing each search result entry
 * @author ptantiku
 *
 */
public class SearchResultEntity {
	private int snippetid;
	private int docid;
	private String url;
	private String title;
	private String code;
	private String text;
	private String paragraph;
	private double score;
	private int samesnippetcount;
	
	public int getSnippetid() {
		return snippetid;
	}
	public void setSnippetid(int snippetid) {
		this.snippetid = snippetid;
	}	
	public int getDocid() {
		return docid;
	}
	public void setDocid(int docid) {
		this.docid = docid;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}	
	public String getTitle(){
		return title;
	}
	public void setTitle(String title){
		this.title = title;
	}	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getParagraph() {
		return paragraph;
	}
	public void setParagraph(String paragraph) {
		this.paragraph = paragraph;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public int getSamesnippetcount() {
		return samesnippetcount;
	}
	public void setSamesnippetcount(int samesnippetcount) {
		this.samesnippetcount = samesnippetcount;
	}
}
