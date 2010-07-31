package fna.webservices;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.RemarkNode;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.nodes.TextNode;

import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserFeedback;
//import org.htmlparser.util.

import fna.parsing.ApplicationUtilities;
import fna.webservices.beans.ScientificName;

public class WebServicesUtilities {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String text = "Probolomyrmecinae Perrault"; //"Probolomyrmecini";
		System.out.println("\n" + text + " is "+ 
				(new WebServicesUtilities().isName(text) == true ? "present " : "absent ") 
				+"in " + ApplicationUtilities.getProperty("HNS") );
	}
	
	public boolean isName(String text) {
		
		try {
			/* Checking in the HNS */
		
			if(checkHNSServer(text)) {
				return true;
			}
			/* Checking in Zoobank */

			if(checkZoobankServer(text)) {
				return true;
			}
		} catch (Exception exe) {
			exe.printStackTrace();
		}


		return false;
	}

	private boolean checkZoobankServer(String text) throws Exception {
		 Parser parser = new Parser(ApplicationUtilities.getProperty("ZOOBANK") 
				 + URLEncoder.encode(text));
		 TagNameFilter filter = new TagNameFilter ("SPAN");
		 NodeList list = parser.parse (filter);
		 boolean found = false;
		 
		 for (int i = 5; i< list.size(); i++ ) {
			 found = processMyNodes(text, list.elementAt(i));
			 if(found) {
				 break;
			 }
		 } 
		 return found;
	}
	private boolean checkHNSServer(String text) throws Exception {
		
		 Parser parser = new Parser(ApplicationUtilities.getProperty("HNS") + text);
		 TagNameFilter filter = new TagNameFilter ("TR");
		 NodeList list = parser.parse (filter);
		 boolean found = false;
		 
		 for (int i = 1; i< list.size(); i++ ) {
			 found = processMyNodes(text, list.elementAt(i));
			 if(found) {
				 break;
			 }
		 } 
		 return found;
	}
	 private boolean processMyNodes (String text, Node node) throws Exception
	 {
		 boolean returnValue = false;
	     if (node instanceof TextNode) {
	         // downcast to TextNode
	         TextNode name = (TextNode)node;
	         // do whatever processing you want with the text
	         System.out.print (" " + name.getText ());
	         if (name.getText ().contains(text)) {
	        	 returnValue = true;
	        	 return returnValue;
	         }
	     }
	     else if (node instanceof TagNode) {
	         // downcast to TagNode
	         TagNode tag = (TagNode)node;
	         NodeList nl = tag.getChildren ();
	         if (null != nl)
	             for (NodeIterator i = nl.elements (); i.hasMoreNodes(); ){
	                 returnValue = processMyNodes (text, i.nextNode ());
	    	         if(returnValue) {
	    	        	 break;
	    	         }
	             }

	     }
	     return returnValue;
	 }
	public boolean isName(String text, String source) {
		return false;
	}
	
	public ScientificName getNameInfo(String name, String src) {
		return null;
	}
	
	public String annotateNames(String segment, String source) {
		return null;
	}
	
	public ArrayList<String> names(String segment, String source){
		return null;
	}
	
	public ArrayList<String> servers(){
		return null;
	}
}
