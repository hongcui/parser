package fna.parsing.character;


import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource; 
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ManipulateGraphML {

	/**
	 * @param args
	 */
	public static void main(String [] args) {
		String group = "Group1.xml";
		
		GraphNode graphNode = deleteNode("rhizome", group);
		restoreNode(graphNode, group);
		
	}
	 
	public static GraphNode deleteNode(String nodeName, String groupName) {
		
		GraphNode graphNode = new GraphNode(nodeName);	
	    try{
	    	
	      //File location has to be put somewhere
	      String xmlFile = "D:/FNA/FNAV19/target/co-occurrence/" + groupName;
	      File file = new File(xmlFile);
	      String remElement = "node";
	      if (file.exists()){
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document doc = builder.parse(xmlFile);
	        TransformerFactory tFactory = TransformerFactory.newInstance();
	        Transformer tFormer = tFactory.newTransformer();
	        NodeList nodes = doc.getElementsByTagName(remElement);
	        
	        for (int i = 0 ; i < nodes.getLength(); i++) {
	        	Element el = (Element)nodes.item(i);
	        	String nodeKeyValue =  el.getElementsByTagName("data").item(0).getFirstChild().getNodeValue();
	            
	            if (nodeKeyValue.equals(nodeName)) {
	            	String id = el.getAttribute("id");
	            	graphNode.setNodeNumber(id);
		            NodeList nl = doc.getElementsByTagName("edge");
		            int size = nl.getLength();
		            for (int j=0; j< size; j++) {
		            	Element edgeElement = (Element) nl.item(j);
		            	String source = edgeElement.getAttribute("source");
		            	String target = edgeElement.getAttribute("target");
		            	if (source.equals(id) ||  target.equals(id)) {
		            		System.out.println("Source : " + source + ", target : " + target);
		            		edgeElement.getParentNode().removeChild(edgeElement);
		            		graphNode.getEdges().add(source + "," + target);
		            		doc.normalize();
		            		j = -1;
		            		size = doc.getElementsByTagName("edge").getLength();
		            		
		            	}
		            	
		            }
	            		            	
	            	el.getParentNode().removeChild(el);
	            	doc.normalize();
	            	break;
	            }
	        	
	        }
	        
	        doc.normalize();
	        Source source = new DOMSource(doc);
	        Result dest = new StreamResult(new File(xmlFile));
	        tFormer.transform(source, dest);

	      }
	      else{
	        System.out.println("File not found!");
	      }
	    }
	    catch (Exception e){
	      e.printStackTrace();
	      System.exit(0);
	    }
	    
	    return graphNode;
	  }
	
	public static boolean restoreNode(GraphNode graphNode, String groupName) {
		
			/*File path hardcoding has to be removed */
	      String xmlFile = "D:/FNA/FNAV19/target/co-occurrence/" + groupName;
	      File file = new File(xmlFile);
	      String remElement = "node";
	      try {
		      if (file.exists()){
			        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			        DocumentBuilder builder = factory.newDocumentBuilder();
			        Document doc = builder.parse(xmlFile);
			        TransformerFactory tFactory = TransformerFactory.newInstance();
			        Transformer tFormer = tFactory.newTransformer();
			        
			        /* Add the parent graph-node */
			        Node graph = doc.getElementsByTagName("graph").item(0);
			        
			        Element element = doc.createElement(remElement);
			        element.setAttribute("id", graphNode.getNodeNumber());
			        Element childElement = doc.createElement("data");
			        childElement.setAttribute("key", "name");
			        childElement.setTextContent(graphNode.getNodeName());
			        element.appendChild(childElement);
			        graph.appendChild(element);
			        doc.normalize();
			        
			        /* Add the edges now */
			        ArrayList<String> edges = graphNode.getEdges();
			        for (String edgeGroup : edges) {
			        	String [] boundaries = edgeGroup.split(",");
			        	Element edge = doc.createElement("edge");
			        	edge.setAttribute("source", boundaries[0]);
			        	edge.setAttribute("target", boundaries[1]);
			        	graph.appendChild(edge);
			        	doc.normalize();
			        }
			        
			        Source source = new DOMSource(doc);
			        Result dest = new StreamResult(new File(xmlFile));
			        tFormer.transform(source, dest);
			        
		      }
	      } catch(Exception exe){
	    	 exe.printStackTrace(); 
	      }

		
		return true;
	}
	
}
