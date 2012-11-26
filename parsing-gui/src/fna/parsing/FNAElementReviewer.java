/**
 * 
 */
package fna.parsing;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;



/**
 * @author Hong Updates
 * 
 * used to review <TaxonIdentifier> elements of parsed FNA volumes for JSTOR
 *
 */
public class FNAElementReviewer {
	//private String elementname = "habitat";
	private String elementname = "global_distribution";
	private XPath xpath1= XPath.newInstance("//"+elementname);
	private static final Logger LOGGER = Logger.getLogger(FNAElementReviewer.class);  


	/**
	 * 
	 */
	public FNAElementReviewer(String xmldir) throws Exception{
		File xmlfolder = new File(xmldir);
		File[] xmls = xmlfolder.listFiles();
		for(File xml: xmls){
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(xml);
			Element root = doc.getRootElement();
			List<Element> elements = xpath1.selectNodes(root);
			System.out.println();
			System.out.println(xml.getName()+":");
			for(Element element: elements){
				System.out.println(elementname+": "+element.getTextTrim());										
			}
		}
	}

	/**
	 * recursively print the elements and content
	 * @param name
	 * @return
	 */
	private String printElement(Element name, String content) {
		List<Element> children = name.getChildren();
		if(children.size()==0){
			content += name.getName()+": "+name.getTextTrim()+"\n";
		}else{
			for(Element c: children){
				content += printElement(c, "");
			}
		}
		return content;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String xmldir = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V26-good\\target\\last";
		//String xmldir = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameReviewer\\finalnew\\V21_last_good\\reviewed_by_hong_tocheck_synonym";
		try{
			FNAElementReviewer fnr = new FNAElementReviewer(xmldir);
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
		}
	}

}
