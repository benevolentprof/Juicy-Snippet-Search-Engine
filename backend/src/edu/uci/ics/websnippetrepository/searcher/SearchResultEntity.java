package edu.uci.ics.websnippetrepository.searcher;

public class SearchResultEntity {
	private int snippetid;
	private int docid;
	private String url;
	private String title;
	private String code;
	private String text;
	private double score;
	
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
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
}
