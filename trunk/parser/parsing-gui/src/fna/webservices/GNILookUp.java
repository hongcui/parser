package fna.webservices;

/**
 * @author Partha Pratim Sanyal (ppsanyal@email.arizona.edu)
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.commons.lang.StringEscapeUtils;

import fna.parsing.Registry;

public class GNILookUp {

	/**
	 * @param args
	 */
	private static Properties properties = null;
	private static FileInputStream fstream = null;
	private static Set<Object> tagKeys = null;
	private static final Logger LOGGER = Logger.getLogger(GNILookUp.class);
	static {
		try {
			fstream = new FileInputStream(System.getProperty("user.dir")+
					"\\markuptags.properties");
			properties = new Properties();
			properties.load(fstream);
			tagKeys = properties.keySet();
		} catch (FileNotFoundException e) {
			LOGGER.error("couldn't open file in GNILookUp static block", e);
		} catch (IOException e) {
			LOGGER.error("couldn't open file in GNILookUp static block", e);
			e.printStackTrace();
		} 
	}
	private String source1 = "D:\\FNA\\FNAV19\\target\\transformed";
	private String gniURL = "http://gni.globalnames.org/name_strings.xml?search_term=";
	private String destination1 = "D:\\FNA\\FNAV19\\target\\name-tagged\\";
	private ArrayList <String> tags ;
	private HashMap<String, String> lsidMap;
	private ArrayList<String> dictionary;
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		new GNILookUp().getTagNames(null, null);

	}
	
	public void getTagNames(String source, String destination) {
		source = source1;
		destination = destination1;
		
		File transformedDirectory  = new File(source1);
		File [] files = transformedDirectory.listFiles();
		/* The grand for loop */
		try {
			for (File file : files) {
				tags = new ArrayList<String>();
				lsidMap = new HashMap<String, String>();
				createDictionary();
				readTags(file);
				writeMarkUp(file, destination1);
				//remove this break later - now taste for one file
				break;
			}
		} catch (Exception exe){
			exe.printStackTrace();
			LOGGER.error("Error in write getTagNames ", exe);
		}

		
	}
	
	private void createDictionary() throws Exception {
		BufferedReader in = null;
		try {
		    in = new BufferedReader(new FileReader("dictionary.txt"));
		    String word;
		    dictionary = new ArrayList<String>();
		    while ((word = in.readLine()) != null) {
		        dictionary.add(word.toLowerCase());
		    }
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("Error in write createDictionary ", e);
		} finally {
			in.close();
		}
	}
	private void writeMarkUp(File file, String destination) {
		try {
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document doc = builder.parse(file);
	        doc.getDocumentElement().normalize();
	        
	        TransformerFactory tFactory = TransformerFactory.newInstance();
	        Transformer tFormer = tFactory.newTransformer();
	        
	        Element root = doc.getDocumentElement();
	        NodeList nodes = root.getChildNodes();
	        for (int i = 0 ; i <nodes.getLength(); i++) {
	        	
	        	if (!nodes.item(i).getNodeName().equalsIgnoreCase
	        			(properties.getProperty("discussion"))) {
		        	boolean hasTag = tags.contains(nodes.item(i).getNodeName());
		        	if (hasTag) {  		
		        		
		        		String nodeValue = nodes.item(i).getFirstChild().getNodeValue();
		        		nodes.item(i).getFirstChild().setNodeValue("");
		        		Element elem = doc.createElement("name");
		        		elem.setAttribute("lsid", lsidMap.get(nodeValue));
		        		elem.setAttribute("src", "gni");
		        		elem.appendChild(doc.createTextNode(nodeValue));
		        		nodes.item(i).appendChild(elem);
		        		tags.remove(nodes.item(i).getNodeName());
		        		System.out.println("Written lsid string tag for " + nodeValue);
		        		LOGGER.info("Written lsid string tag for " + nodeValue);
		        	}
	        	} else {
	        		String original = nodes.item(i).getFirstChild().getNodeValue();
	        		String discussion = 
	        			clean(nodes.item(i).getFirstChild().getNodeValue());
	        		
	        		System.out.println("Processing the following discussion : "+ discussion);
	        		LOGGER.info("Processing the following discussion : "+ discussion);
	        		
	        		Scanner sc = new Scanner(discussion);
	        		while (sc.hasNext()) {
	        			String word = sc.next();
	        			if (lsidMap.get(word) != null) {
			        		Element elem = doc.createElement("name");
			        		elem.setAttribute("lsid", lsidMap.get(word));
			        		elem.setAttribute("src", "gni");
			        		elem.appendChild(doc.createTextNode(word));
			        		nodes.item(i).appendChild(elem);
	        			}
	        		}
	        		
/*	        		Set <String> keys = lsidMap.keySet();
	        		for (String name : keys) {
	        			discussion = 
	        				StringEscapeUtils.unescapeXml(
	        						discussion.replace(name, "<name lsid=\"" +lsidMap.get(name) + 
	        								"\" src=\"gni\"gt"+name+"lt/name gt"));
	        		}*/

	        		//nodes.item(i).getFirstChild().setNodeValue(discussion);
	        		tags.remove(properties.getProperty("discussion"));
	        	}

	        	
/*	        	if (nodes.item(i).getNodeName().equals(taglist[0])) {
	        		nodes.item(i).getFirstChild().setNodeValue("partha" + 
	        				nodes.item(i).getFirstChild().getNodeValue());
	        	}
	        	System.out.println(nodes.item(i).getNodeName());
	        	if (nodes.item(i).getFirstChild() != null)
	        	System.out.println(nodes.item(i).getFirstChild().getNodeValue());*/
	        }
	        FileOutputStream flt = new FileOutputStream
	        (new File(destination+file.getName()));
	        OutputStreamWriter out = new OutputStreamWriter(flt);
	        Source source = new DOMSource(doc);
	        StreamResult result = new StreamResult(out);
	        tFormer.transform(source, result); 
		}
		catch (Exception e) {
			System.out.println("Error: " + e);
			e.printStackTrace();
			LOGGER.error("Error in write MarkUp ", e);
		}
	}
	
	private void readTags(File file) {
		
		try {
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document doc = builder.parse(file);
			for (Object obj : tagKeys) {
				String tagName = properties.getProperty((String)obj);
				if (!tagName.equals(properties.getProperty("discussion"))) {
					NodeList nodes = doc.getElementsByTagName(tagName);
					if (nodes.getLength() != 0){
						tags.add(tagName);
					}
					Element element = (Element)nodes.item(0);
					if (element != null) {
						String tagValue = element.getFirstChild().getNodeValue();
						String LSID = extractLSID(clean(tagValue), gniURL); 
						if (!LSID.equals("")) {
							lsidMap.put(tagValue, LSID);
							System.out.println("Name found : " + tagValue);
							LOGGER.info("Name found : " + tagValue);
						}
					}
				} else {
					NodeList nodes = doc.getElementsByTagName(tagName);
					if (nodes.getLength() != 0){
						tags.add(tagName);
					}
					int nodeLength = nodes.getLength();
					for (int i = 0 ; i < nodeLength; i++) {
						Element element = (Element)nodes.item(i);
						if (element != null) {
							String tagValue = element.getFirstChild().getNodeValue();
							String discussion = clean(tagValue);
							String [] words = discussion.split(" ");
							for (String word : words) {
								if (lsidMap.get(word) == null && 
										!dictionary.contains(word.toLowerCase())){
									String LSID = extractLSID(clean(word), gniURL); 
									if (!LSID.equals("")) {
										lsidMap.put(word, LSID);
										System.out.println("Name found : " + word);
										LOGGER.info("Name found : " + word);
									}
								}
							}
						}

					}
				}

				
			}
			System.out.println(lsidMap);
			LOGGER.info("LSIDMAP " + lsidMap);
			System.out.println(tags);
			LOGGER.info("Tags " + tags);
			
		} catch (Exception exe) {
			exe.printStackTrace();
			LOGGER.error("Error in write readTags ", exe);
		}


	}
	
	private String extractLSID(String tagValue, String url) throws Exception {
		URL gniUrl = new URL(url+tagValue);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(gniUrl.openStream());
        NodeList nodes = doc.getElementsByTagName("name");
        String LSID = "";
        int nodeLength = nodes.getLength();
        for (int i = 0; i< nodeLength;  i++) {
        	Element element = (Element)nodes.item(i);
        	String nodeValue = element.getFirstChild().getNodeValue();
        	if (nodeValue.equalsIgnoreCase(tagValue)) {
        		LSID = element.getNextSibling().getNextSibling().getNextSibling()
				.getNextSibling().getFirstChild().getNodeValue();
        		break;
        		
        	}
        }
        
        return LSID;
	}
	
	private static String clean(String description) {
		Pattern p = Pattern.compile("[\\W\\d\\s\\e]+");
		String [] a = p.split(description);
		StringBuffer sb = new StringBuffer();
		for (String s : a) {
			sb.append(s+" ");
		}
		return sb.toString().trim();		
	}
}
