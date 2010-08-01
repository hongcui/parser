package fna.webservices;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.nodes.TextNode;

import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;

import fna.parsing.ApplicationUtilities;
import fna.webservices.beans.ScientificName;

public class WebServicesUtilities {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		String text = "Probolomyrmecinae"; //"Probolomyrmecini";
		System.out.println("\n" + text + " is "+ 
				(new WebServicesUtilities().isName(text) == true ? "present " : "absent ") 
				+"in " + ApplicationUtilities.getProperty("HNS") );
	}
	
	/**
	 * This function tells if "text" is a name recognized by any name server
	 * @param text
	 * @return
	 */
	public boolean isName(String text) {
		
		try {
			/* Checking in the HNS */
		    if (text.contains(" ")) {
				if(checkHNSServer(text.substring(0, text.indexOf(" ")))) {
					return true;
				}
		    } else {
				if(checkHNSServer(text)) {
					return true;
				}
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
			 found = processMyNodes(text, list.elementAt(i), false);
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
			 found = processMyNodes(text, list.elementAt(i), false);
			 if(found) {
				 break;
			 }
		 } 
		 return found;
	}
	 protected static boolean processMyNodes (String text, Node node, boolean exact) throws Exception
	 {
		 boolean returnValue = false;
	     if (node instanceof TextNode) {
	         // downcast to TextNode
	         TextNode name = (TextNode)node;
	         // do whatever processing you want with the text
	         //System.out.print (" " + name.getText ());
	         if (exact) {
		         if (name.getText ().equalsIgnoreCase(text)) {
		        	 returnValue = true;
		        	 return returnValue;
		         }
	         } else {
		         if (name.getText ().contains(text)) {
		        	 returnValue = true;
		        	 return returnValue;
		         }
	         }

	     }
	     else if (node instanceof TagNode) {
	         // downcast to TagNode
	         TagNode tag = (TagNode)node;
	         NodeList nl = tag.getChildren ();
	         if (null != nl)
	             for (NodeIterator i = nl.elements (); i.hasMoreNodes(); ){
	                 returnValue = processMyNodes (text, i.nextNode (), exact);
	    	         if(returnValue) {
	    	        	 break;
	    	         }
	             }

	     }
	     return returnValue;
	 }
	public boolean isName(String text, String source) {
		
		try {
			if(source.equalsIgnoreCase("HNS")){
				return checkHNSServer(text);
			}
			
			if(source.equalsIgnoreCase("ZOOBANK")) {
				return checkZoobankServer(text);
			}
		} catch (Exception exe){
			exe.printStackTrace();
		}

		return false;
	}
	
	public ScientificName getNameInfo(String name, String src) {
		return null;
	}
	
	/**
	 * This function annotates a text segment using the src name server 
	 * @param segment
	 * @param source
	 * @return
	 */
	public String annotateNames(String segment, String source) {
		return null;
	}
	
	/**
	 * This function returns a list of names mentioned in segment in its 
	 * original order that are recognized by src name server.
	 * @param segment
	 * @param source
	 * @return
	 */
	public ArrayList<String> names(String segment, String source){
		return null;
	}
	
	/**
	 * This function returns all name servers the class knows how to access
	 * @return
	 */
	public HashMap <String, String> servers(){
		
		HashMap <String, String> servers = new HashMap<String, String>();
		
		servers.put("HNS", ApplicationUtilities.getProperty("HNS"));
		servers.put("ZOOBANK", ApplicationUtilities.getProperty("ZOOBANK"));
		servers.put("FISHBASE", ApplicationUtilities.getProperty("FISHBASE"));
		servers.put("Index-Fungorum", ApplicationUtilities.getProperty("Index-Fungorum"));
		servers.put("Geonames", ApplicationUtilities.getProperty("Geonames"));
		servers.put("FallingRain", ApplicationUtilities.getProperty("FallingRain"));		
		
		return servers;
	}
}
