package edu.uci.ics.websnippetrepository.crawler;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.uci.ics.websnippetrepository.ImportToDatabase;

public class HTMLCrawler {
	
	public static final String[] URL_LIST = {
		"http://java.sun.com/docs/books/tutorial/",
		"http://learnola.com/",	//------6 pages of tutorial*
		"http://www.zetcode.com/",		//----5 categories of tutorial pages
		"http://forum.codecall.net/java-tutorials",
		"http://www.dickbaldwin.com/java/",
		"http://www.learn-java-tutorial.com/",
		"http://www.developer.com/java/",
		"http://pages.cpsc.ucalgary.ca/~kremer/tutorials/Java/",
		"http://www.beginner-tutorials.com/java-tutorials.php",
		"http://www.javabeginner.com",
		"http://www.javacoffeebreak.com/",  
		"http://www.cafeaulait.org/javatutorial.html",
		"http://www.javaworld.com/",
		"http://en.wikiversity.org/wiki/Java_Tutorial",
		"http://leepoint.net/notes-java/index.html",
		"http://www.javafaq.nu/java-example.html",
		"http://www.java-tips.org",
		"http://www.java2s.com/Tutorial/Java/CatalogJava.htm",
		"http://www.java2s.com/Code/Java/CatalogJava.htm",
		"http://www.java2s.com/Article/Java/CatalogJava.htm",
		"http://www.java2s.com/Code/JavaAPI/CatalogJavaAPI.htm",
		"http://www.java2s.com/Product/Java/GUI-Tools/CatalogGUI-Tools.htm",
		"http://www.tech-recipes.com/category/computer-programming/java-programming/",
		"http://www.exampledepot.com/egs/",
		"http://www.devdaily.com/java/",
		"http://www.roseindia.net/java/",
		"http://en.wikibooks.org/wiki/Java_Programming/",
		"http://www.codetoad.com/java/",
		"http://danzig.jct.ac.il/java_class/",
		"http://www.java-samples.com/showtitles.php?category=Java&start=1",
		"http://www.algolist.net/Algorithms/",
		"http://www.javapractices.com/",
		"http://home.cogeco.ca/~ve3ll/jatutor0.htm",
	};
	
	static final Logger logger = Logger.getLogger(HTMLCrawler.class);
	
	public static void main(String[] args) throws Exception {
		
		PropertyConfigurator.configure("log4j.properties");
		
		CodeSiteCapturer capturer = new CodeSiteCapturer();
		for(int i=URL_LIST.length-1;i<URL_LIST.length;i++){
			String url = URL_LIST[i];
			capturer.resetURLList();
			System.gc();
			capturer.setSource(url);
			capturer.capture();
		}
		
		ImportToDatabase.closeConnection();
	}

}
