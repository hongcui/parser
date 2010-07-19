package fna.webservices;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import fna.parsing.ApplicationUtilities;
import fna.webservices.beans.ScientificName;

public class WebServicesUtilities {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		new WebServicesUtilities().isName("Probolomyrmex");
	}
	
	public boolean isName(String text) {
		
		try {
			/* Checking in the HNS */
			URL hnsUrl = new URL(ApplicationUtilities.getProperty("HNS") +
					URLEncoder.encode(text, "UTF-8") );
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document doc = builder.parse(hnsUrl.openStream());
	        NodeList nodes = doc.getElementsByTagName("td");
	        System.out.println(nodes.getLength());
		} catch (Exception exe) {
			
		}


		return false;
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
