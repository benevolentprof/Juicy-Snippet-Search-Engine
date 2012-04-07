// Modified Version of HTMLParser SiteCapturer 
// modified by Ptantiku 

package edu.uci.ics.websnippetrepository.crawler;


import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.parserapplications.SiteCapturer;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.util.EncodingChangeException;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import edu.uci.ics.websnippetrepository.ImportToDatabase;

/**
 * Save a web site locally.
 * Illustrative program to save a web site contents locally.
 * It was created to demonstrate URL rewriting in it's simplest form.
 * It uses customized tags in the NodeFactory to alter the URLs.
 * This program has a number of limitations:
 * <ul>
 * <li>it doesn't capture forms, this would involve too many assumptions</li>
 * <li>it doesn't capture script references, so funky onMouseOver and other
 * non-static content will not be faithfully reproduced</li>
 * <li>it doesn't handle style sheets</li>
 * <li>it doesn't dig into attributes that might reference resources, so
 * for example, background images won't necessarily be captured</li>
 * <li>worst of all, it gets confused when a URL both has content and is
 * the prefix for other content,
 * i.e. http://whatever.com/top and http://whatever.com/top/sub.html both
 * yield content, since this cannot be faithfully replicated to a static
 * directory structure (this happens a lot with servlet based sites)</li>
 *</ul>
 */
public class CodeSiteCapturer extends SiteCapturer
{
    
	static final Logger logger = Logger.getLogger(CodeSiteCapturer.class);
	
	String rootURL;
	
    /**
     * Create a web site capturer.
     */
    public CodeSiteCapturer ()
    {
        super();
        setCaptureResources(false);	//code only, no resources
        setTarget("");	//no need to set downloaded file location
    }
    
    public void setSource(String url){
    	super.setSource(url);
    	
    	if(url.lastIndexOf('/')==-1)
    		rootURL=url.toLowerCase();
    	else
    		rootURL=url.substring(0,url.lastIndexOf('/')).toLowerCase();
    	
    	logger.debug(">>>>Root URL = "+rootURL);
    }
    
    /**
     * Determine that the link should be add to crawling queue or not
     */
    protected boolean isToBeCaptured (String link) {
    	boolean shouldCapture =
                (link.toLowerCase().startsWith (rootURL) 
               // && (-1 == link.indexOf ("?")) 
                && (-1 == link.indexOf ("#"))
                && (link.indexOf("www.")==link.lastIndexOf("www."))
                && (link.indexOf("http://")==link.lastIndexOf("http://")));
    	
    	if (shouldCapture)
    		logger.debug("(q="+mPages.size()+",d="+mFinished.size()+") adding "+link);
    	
    	return shouldCapture;
    } 
    
    /**
     * Process a single page.
     * @param filter The filter to apply to the collected nodes.
     * @exception ParserException If a parse error occurs.
     */
    @SuppressWarnings("unchecked")
    protected void process (NodeFilter filter)
        throws
            ParserException
    {
        String url;
        int bookmark;
        NodeList list;
        NodeList robots;
        MetaTag robot;
        String content;

        // get the next URL and add it to the done pile
        url = (String)mPages.remove (0);
        mFinished.add (url);

        logger.info("(q="+mPages.size()+",d="+mFinished.size()+") URL-->"+url);
        
        try
        {
            bookmark = mPages.size ();
            // fetch the page and gather the list of nodes
            mParser.setURL (url);
            try
            {
                list = new NodeList ();
                for (NodeIterator e = mParser.elements (); e.hasMoreNodes (); )
                    list.add (e.nextNode ()); // URL conversion occurs in the tags
            }
            catch (EncodingChangeException ece)
            {
                // try again with the encoding now set correctly
                // hopefully mPages, mImages, mCopied and mFinished won't be corrupted
                mParser.reset ();
                list = new NodeList ();
                for (NodeIterator e = mParser.elements (); e.hasMoreNodes (); )
                    list.add (e.nextNode ());
            }

            // handle robots meta tag according to http://www.robotstxt.org/wc/meta-user.html
            // <meta name="robots" content="index,follow" />
            // <meta name="robots" content="noindex,nofollow" />
            robots = list.extractAllNodesThatMatch (
                new AndFilter (
                    new NodeClassFilter (MetaTag.class),
                    new HasAttributeFilter ("name", "robots")), true);
            if (0 != robots.size ())
            {
                robot = (MetaTag)robots.elementAt (0);
                content = robot.getAttribute ("content").toLowerCase ();
                if ((-1 != content.indexOf ("none")) || (-1 != content.indexOf ("nofollow")))
                    // reset mPages
                    for (int i = bookmark; i < mPages.size (); i++)
                        mPages.remove (i);
                if ((-1 != content.indexOf ("none")) || (-1 != content.indexOf ("noindex")))
                    return;
            }
    
            if (null != filter)
                list.keepAllNodesThatMatch (filter, true);

            
            
            //Export this HTML Page
            exportHTMLPage(url,list);
            
        }
        catch (ParserException pe)
        {
            String message;
            
            // this exception handling is suboptimal,
            // but it recognizes resources that aren't text/html
            message = pe.getMessage ();
            if ((null != message) && (message.endsWith ("does not contain text")))
            {
                if (!mCopied.contains (url))
                    if (!mImages.contains (url))
                        mImages.add (url);
                mFinished.remove (url);
            }
            else
                throw pe;
        }
    }
   

	/**
     * Perform the capture.
     */
    @SuppressWarnings("unchecked")
	public void capture ()
    {
       
        mPages.clear ();
        mPages.add (getSource ());
        while (0 != mPages.size ())
            try{
                process (getFilter ());
            } catch (ParserException pe) {
            	logger.error("Parser Error. "+pe.getMessage());
            }
    }
    
    public void resetURLList(){
    	//clear finished page
    	mFinished.clear();
    	mPages.clear();
    }
    
    /**
     * Export current webpage to Database
     * @param url	URL of the HTML
     * @param list	list of Node in HTML
     */
    protected void exportHTMLPage(String url, NodeList list) {
    	
    	//join all nodes in list to restore HTML documents
    	StringBuilder strBuilder = new StringBuilder();
    	for (int i=0; i<list.size(); i++){
    		strBuilder.append(list.elementAt(i).toHtml());
    	}
    	    	
    	try {
			ImportToDatabase.importHTMLDocumentToDatabase(url, strBuilder.toString());
		} catch (SQLException e) {
			logger.error("Error while importing "+url+" to database."+e.getMessage());
		}
    	    	
	}

}
