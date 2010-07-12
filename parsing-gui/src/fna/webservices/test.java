package fna.webservices;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
/*		String x = "This, is a &&^%$$^$sentence. \"containing\"; <some/ ; [punctuation]> :marks!+" +
				"was trying to&* ,,..??++*figure%$# ##out if@ this%%$678^ !!really ?works??";
		Pattern p = Pattern.compile("[\\W\\d\\s\\e]+");
		String [] a = p.split(x);
		for (String s : a) {
				System.out.print(s+" ");
		}
		System.out.println(a.length);*/
		try {
		//URL url = new URL("http://plazi2.cs.umb.edu:8080/OmniFAT/find_names");
/*			
		    String discussion = "Caryophyllaceae includes 54 locally endemic genera " +
		    		"(many of them in the eastern Mediterranean region of Europe, " +
		    		"Asia, and Africa), cultivated taxa (especially Dianthus, " +
		    		"Gypsophila, and Silene), and weedy taxa (mostly from Eurasia). " +
		    		"Of the 37 genera in the flora area, 15 are entirely non-native: " +
		    		"Agrostemma, Corrigiola, Gypsophila, Holosteum, Lepyrodiclis, " +
		    		"Moenchia, Myosoton, Petrorhagia, Polycarpaea, Polycarpon, " +
		    		"Saponaria, Scleranthus, Spergula, Vaccaria, and Velezia.";
		
			
			try {
			    // Construct data
			    String data = URLEncoder.encode("document_text", "UTF-8") 
			    + "=" + URLEncoder.encode(discussion, "UTF-8");
			    data += "&" + URLEncoder.encode("omni_fat_instance", "UTF-8") 
			    + "=" + URLEncoder.encode("Botany.web", "UTF-8");

			    // Send data
			    URL url = new URL("http://plazi2.cs.umb.edu:8080/OmniFAT/find_names");
			    URLConnection conn = url.openConnection();
			    conn.setDoOutput(true);
			    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			    wr.write(data);
			    wr.flush();

			    // Get the response
			    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			    String line;
			    while ((line = rd.readLine()) != null) {
			        // Process line...
			    	System.out.println(line);
			    }
			    wr.close();
			    rd.close();
			    
			    
			} catch (Exception e) {
			} */
			
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document doc = builder.parse("D:\\FNA\\FNAV19\\target\\transformed\\1.xml");
	        doc.getDocumentElement().normalize();
	        
	        TransformerFactory tFactory = TransformerFactory.newInstance();
	        Transformer tFormer = tFactory.newTransformer();
	        
	        Element root = doc.getDocumentElement();
	        System.out.println(root.getNodeName() + root.getNodeValue());
	        NodeList nodes = root.getChildNodes();
	        String [] taglist = {"family_name", "discussion"};
	        for (int i = 0 ; i <nodes.getLength(); i++) {
	        	//Element el = (Element);
	        	if (nodes.item(i).getNodeName().equals(taglist[0])) {
/*	        		nodes.item(i).getFirstChild().setNodeValue("partha" + 
	        				nodes.item(i).getFirstChild().getNodeValue());*/
	        		
	        		String nodeValue = nodes.item(i).getFirstChild().getNodeValue();
	        		nodes.item(i).getFirstChild().setNodeValue("");
	        		Element elem = doc.createElement("name");
	        		elem.setAttribute("lsid", "bla bla");
	        		elem.setAttribute("src", "gni");
	        		elem.appendChild(doc.createTextNode(nodeValue));
	        		nodes.item(i).appendChild(elem);
	        	}
/*	        	System.out.println(nodes.item(i).getNodeName());
	        	if (nodes.item(i).getFirstChild() != null)
	        	System.out.println(nodes.item(i).getFirstChild().getNodeValue());*/
	        }
	        FileOutputStream flt = new FileOutputStream
	        (new File("D:\\FNA\\FNAV19\\target\\name-tagged\\1.xml"));
	        OutputStreamWriter out = new OutputStreamWriter(flt);
	        Source source = new DOMSource(doc);
	        StreamResult result = new StreamResult(out);
	        tFormer.transform(source, result); 
		} catch (Exception exe){
			exe.printStackTrace();
		}

	}

}
