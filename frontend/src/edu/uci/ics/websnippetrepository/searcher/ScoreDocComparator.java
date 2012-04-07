package edu.uci.ics.websnippetrepository.searcher;


import java.util.Comparator;
import org.apache.lucene.search.ScoreDoc;

public class ScoreDocComparator implements Comparator<ScoreDoc> {

	@Override
	public int compare(ScoreDoc arg0, ScoreDoc arg1) {
		return - Double.compare(arg0.score, arg1.score);	//reverse normal comparator because high score goes top
	}


}
