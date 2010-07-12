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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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

import fna.parsing.ApplicationUtilities;
import fna.parsing.Registry;

public class GNILookUp {

	private static Properties properties = null;
	private static FileInputStream fstream = null;
	private static Set<Object> tagKeys = null;
	private static final Logger LOGGER = Logger.getLogger(GNILookUp.class);
	private String source1 = "D:\\FNA\\FNAV19\\target\\transformed";
	private String gniURL = "http://gni.globalnames.org/name_strings.xml?search_term=";
	private String destination1 = "D:\\FNA\\FNAV19\\target\\name-tagged\\";
	private ArrayList <String> tags ;
	private HashMap<String, String> lsidMap;
	private ArrayList<String> dictionary;
	
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
			createDictionary();
			for (File file : files) {
				tags = new ArrayList<String>();
				lsidMap = new HashMap<String, String>();
				
				readTags(new File(source+"\\100.xml"));
				writeMarkUp(new File(source+"\\100.xml"), destination1);
				saveNames();
				//remove this break later - now taste for one file
				break;
			}
		} catch (Exception exe){
			exe.printStackTrace();
			LOGGER.error("Error in write getTagNames ", exe);
		}

		
	}
	
	/**
	 * This method will create a dictionary of common English 
	 * words in memory to save the Web services invocation for every word.
	 * @throws Exception
	 */
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
	
	/**
	 * This method will mark the file with lsids
	 * @param file
	 * @param destination
	 */
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
	        		String nodeName = nodes.item(i).getNodeName();
		        	boolean hasTag = tags.contains(nodeName);
		        	
		        	if (hasTag) { 
		        		String nodeValue = nodes.item(i).getFirstChild().getNodeValue();
		        		if (lsidMap.get(nodeValue) != null) {
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
	
	/**
	 * This method reads the file information into memory and also checks for lsid and valid Sceintific names.
	 * @param file
	 */
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
	
	/**
	 * This Method will extract the LSID
	 * @param tagValue
	 * @param url
	 * @return
	 * @throws Exception
	 */
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
	
	/**
	 * This method cleans the String from clutter
	 * @param description
	 * @return
	 */
	private static String clean(String description) {
		Pattern p = Pattern.compile("[\\W\\d\\s\\e]+");
		String [] a = p.split(description);
		StringBuffer sb = new StringBuffer();
		for (String s : a) {
			sb.append(s+" ");
		}
		return sb.toString().trim();		
	}
	
	private void saveNames(){
		
		PreparedStatement stmt = null;
		Connection conn = null;
		
		try {
			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
			conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
			stmt = conn.prepareStatement("insert into gnilsids(name, lsid, source) values(?,?,?)");
			
			Set <String> keys = lsidMap.keySet();
    		for (String name : keys) {
    			stmt.setString(1, name);
    			stmt.setString(2, lsidMap.get(name));
    			stmt.setString(3, "gni");
    			try {
    				stmt.execute();
    			} catch (Exception exe) {
    				if (!exe.getMessage().contains("Duplicate")) {
    					exe.printStackTrace();
    					LOGGER.error("Excepion in saveNames ", exe);
    				}
    			}
    			
    		}
    		
		} catch(Exception exe) {
			exe.printStackTrace();
			LOGGER.error("Excepion in saveNames ", exe);
		}
	}
}
