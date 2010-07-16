package fna.charactermarkup;

import java.io.*;

import java.util.regex.*;
import java.util.ArrayList;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;


/**
 * @author hongcui
 *
 */
public class Tree2XML {
    private String test=null;
    private String str = "";
    //private static PrintWriter out; 
    /**
     * 
     */
    public Tree2XML(String test) {
        // TODO Auto-generated constructor stub
        this.test = test;
    }
    
    public Document xml(){
        if(test==null){
            return null;
        }
        //out.println();
        //out.println(test);
        String xml = "";
        //step 1: turn all ( to <
        test = test.replaceAll("\\(", "<");
        //System.out.println(test);
        //step 2: turn those ) that are after a <, but without another < in between, to />. Regexp: <[^<]*?\)
        Pattern p = Pattern.compile("(.*?<[^<]*?)(\\))(.*)");
        Matcher m = p.matcher(test);
        while(m.matches()){
            xml += m.group(1)+"/>";
            test = m.group(3);
            m = p.matcher(test);
        }
        xml+=test;
        //System.out.println(xml);
        //step 3: process remaining ) one by one
        p = Pattern.compile("(.*?\\))(.*)");
        m = p.matcher(xml);
        while(m.matches()){
            String part = m.group(1);
            part = process(part);
            xml = part+m.group(2);
           // System.out.println(xml);
            m = p.matcher(xml);
        }
        
        xml = format(xml);
      //  System.out.println(xml);
		Document doc =null;
		try {
		     SAXBuilder builder = new SAXBuilder();
		     ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
		     doc = builder.build(bais);
		    } catch (Exception e) {
		      System.out.print("Problem parsing the xml: \n" + e.toString());
		}
		return doc;
    }
    
    
    /**
     * <NN Heads/> will become
     * <NN text="Heads"/>
     * @param xml
     */
    private String format(String xml) {
        // TODO Auto-generated method stub
        String r = "";
        int count = 0;
        xml = xml.replaceAll("<[^A-Z/]", "<PUNCT");
        Pattern p = Pattern.compile("(.*?)<([^<]*?) ([^<]*?)/>(.*)");
        Matcher m = p.matcher(xml);
        while(m.matches()){
            r += m.group(1);
            r +="<"+m.group(2)+" id=\""+count+"\" text=\""+m.group(3)+"\"/>";
            xml = m.group(4);
            m = p.matcher(xml);
            count++;
        }
        r +=xml;
        return r;
    }

    /**
     * 
     * @param part looks like: 
     * a) <S    <NP      <NP <NN Heads/> <JJ many/>)     or 
     * b) <S    <NP      <NP> <NN Heads/> <JJ many/> </NP> )
     * @return: 
     * a) <S    <NP      <NP> <NN Heads/> <JJ many/> </NP> or
     * b) <S    <NP>      <NP> <NN Heads/> <JJ many/> </NP> </NP> 
     */

    private String process(String part) {
        String result = "";
        part = part.trim().replaceFirst("\\)$", "").replaceAll("\\s+", " ").trim();
        String cp = part;
        
        part = part.replaceAll("<[^<]*?/>", "");
        Pattern p = Pattern.compile("(.*?)<([A-Z]+)>\\s*</\\2>(.*)");
        Matcher m = p.matcher(part);
        while(m.matches()){
            part = m.group(1)+m.group(3);
            m = p.matcher(part);
        }
        part = part.trim();
        if(part.lastIndexOf("<") < 0){
        	return cp;
        }
        String tag = part.substring(part.lastIndexOf("<"));
        cp = cp.replaceAll(tag+"( |$)", tag+"* ");
        int index = cp.lastIndexOf('*');
        result = cp.substring(0, index)+">"+cp.substring(index+1)+"</"+tag.replaceFirst("<","")+">";
        result = result.replaceAll("\\*", "");        
        return result;
    }

 
    
 /*   private void processxml(Document root) {
    	str = "";
    	NodeList noun = root.getElementsByTagName("NP");
    	if( noun.getLength() != 0){
    		Node node = noun.item(0);
			if (node.hasChildNodes()){
				processchildnn(node);
			}
			else{
				if (node.getAttributes()!= null){
					str = str + node.getAttributes().getNamedItem("text").getNodeValue()+" ";
				}
			}
    	}
    	NodeList verbid = root.getElementsByTagName("VP");
		if( verbid.getLength() != 0){
			System.out.println(verbid.getLength());
			out.print(str+" / ");
			//System.out.println(verbid.item(0).getFirstChild().getTextContent());
			for(int i = 0; i < verbid.getLength(); i++){
				Node node = verbid.item(i);
				if (node.hasChildNodes()){
					processchilds(node);
					out.print(" / ");
				}
				else{
					if (node.getAttributes()!= null){
						out.println(node.getAttributes().getNamedItem("text").getNodeValue());
					}
				}
			}
		}
    }
    
    private void processchildnn(Node node) {
    	
		NodeList childid = node.getChildNodes();
		for(int j = 0; j < childid.getLength(); j++){
			Node nodes = childid.item(j);
			if (nodes.hasChildNodes())
				processchildnn(nodes);
			else{
				if (nodes.getAttributes()!= null && nodes.getNodeName() == "NN"){
					str = str + nodes.getAttributes().getNamedItem("text").getNodeValue()+" ";
				}
			}
		}
		
    }
    
    private void processchilds(Node node) {
    	
			NodeList childid = node.getChildNodes();
			for(int j = 0; j < childid.getLength(); j++){
				Node nodes = childid.item(j);
				if (nodes.hasChildNodes())
					processchilds(nodes);
				else{
					if (nodes.getAttributes()!= null){
						out.print(nodes.getAttributes().getNamedItem("text").getNodeValue()+" ");
					}
				}
			}
			
    }*/
		    
    /**
     * @param args
     */
    public static void main(String[] args) {
    }

}
