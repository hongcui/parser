package fna.charactermarkup;

import java.io.*;

import java.util.regex.*;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;


/**
 * @author hongcui
 *
 */
public class Tree2XML {
    private String test=null;
    private String str = "";
    private static PrintWriter out; 
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
        out.println();
        out.println(test);
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
        xml = xml.replaceAll("<[^A-Z/]", "<PUNCT");
        Pattern p = Pattern.compile("(.*?)<([^<]*?) ([^<]*?)/>(.*)");
        Matcher m = p.matcher(xml);
        while(m.matches()){
            r += m.group(1);
            r +="<"+m.group(2)+" text=\""+m.group(3)+"\"/>";
            xml = m.group(4);
            m = p.matcher(xml);
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
        // TODO Auto-generated method stub
    	try{
	        String test="(ROOT  (S (NP      (NP (NN body) (NN ovoid))      (, ,)      (NP        (NP (CD 2-4))        (PP (IN x)          (NP            (NP (CD 1-1.5) (NN mm))            (, ,)            (ADJP (RB not) (JJ winged)))))      (, ,))    (VP (VBZ woolly))    (. .)))";
	       // test="(ROOT  (NP    (NP      (NP (NNP Ray))      (ADJP (JJ laminae)        (NP (CD 6))))    (: -)    (NP      (NP        (NP (CD 7) (NNS x))        (NP (CD 2/CD-32) (NN mm)))      (, ,)      (PP (IN with)        (NP (CD 2))))    (: -)    (NP      (NP (CD 5) (NNS hairs))      (PP (IN inside)        (NP          (NP (NN opening))          (PP (IN of)            (NP (NN tube))))))    (. .)))";
	       // test="(S (NP (NP (NN margins) (UCP (NP (JJ entire)) (, ,) (ADJP (JJ dentate)) (, ,) (ADJP (RB pinnately) (JJ lobed)) (, ,) (CC or) (NP (JJ pinnatifid) (NN pinnately))) (NN compound)) (, ,) (NP (JJ spiny)) (, ,)) (VP (JJ tipped) (PP (IN with) (NP (NNS tendrils)))) (. .))";
	        FileInputStream istream = new FileInputStream("F:\\UA\\RA\\Code\\parsing-gui-charactermarkup\\onelineoutput.txt"); 
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(istream));
			FileOutputStream ostream = new FileOutputStream("F:\\UA\\RA\\Code\\parsing-gui-charactermarkup\\finalrelationanalysis.txt"); 
			out = new PrintWriter(ostream);
			while ((test = stdInput.readLine())!=null){
				Tree2XML t2x = new Tree2XML(test);
		        Document doc = t2x.xml();
		        out.println(doc);
		        
			}
    	}catch (Exception e){
    		//System.err.println(e);
			e.printStackTrace();
        }
    }

}
