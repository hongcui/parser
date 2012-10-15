/**
 * 
 */
package fna.parsing;

import java.io.File;
import java.util.List;

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
public class FNANameReviewer {
	private XPath xpath1= XPath.newInstance("//number");
	private XPath xpath2= XPath.newInstance("//TaxonIdentification");

	/**
	 * 
	 */
	public FNANameReviewer(String xmldir) throws Exception{
		File xmlfolder = new File(xmldir);
		File[] xmls = xmlfolder.listFiles();
		for(File xml: xmls){
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(xml);
			Element root = doc.getRootElement();
			List<Element> numbers = xpath1.selectNodes(root);
			List<Element> names = xpath2.selectNodes(root);
			if(numbers.size()>1) System.out.println(xml.getName()+" has more than 1 names");
			else{
				System.out.println();
				System.out.println(xml.getName()+":");
				if(numbers.size()>0) System.out.println("number = "+numbers.get(0).getTextNormalize());
				for(Element name: names){
					String status = name.getAttributeValue("Status");
					System.out.println(status +":\n"+printElement(name, ""));										
				}
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

		//String xmldir = "E:\\work_data\\ToReview\\V3-good\\target\\problematic\\runNameReviewer";

		//String xmldir = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameReviewer\\finalnew\\V21_last_good\\reviewed_by_hong_tocheck_synonym";
		try{
			FNANameReviewer fnr = new FNANameReviewer(xmldir);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
