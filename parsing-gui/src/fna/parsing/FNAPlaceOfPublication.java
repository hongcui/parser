/**
 * 
 */
package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

/**
 * @author Hong Updates
 * This class moves treatement/place_of_publication to treatment/TaxonIdentification/place_of_publication
 */
public class FNAPlaceOfPublication {

	/**
	 * 
	 */
	public FNAPlaceOfPublication() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 * @throws Exception 
	 * @throws JDOMException 
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		XPath poppath = XPath.newInstance("./place_of_publication"); 
		String[] vs = new String[]{"3", "4", "5", "7", "8", "19", "20", "21", "23", "26", "27"};
		String basedir = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameReviewer\\CompleteReviewed\\beforeStep2\\V";
		for(String v : vs){
			System.out.println("V"+v);
			String dir = basedir+v;
		
			File xmldir = new File(dir);
			File[] xmls = xmldir.listFiles();
			for(File f : xmls){
				SAXBuilder builder = new SAXBuilder();
				Document doc = builder.build(f);
				Element treatment = doc.getRootElement();
				List<Element> pops = poppath.selectNodes(treatment);
				List<Element> children = treatment.getChildren();
				for(Element pop : pops){
					int p = treatment.indexOf(pop);
					Element ti = null;
					do{
						p = p - 1;
						if(p>=0) ti = children.get(p);
					}while(ti!=null && !ti.getName().equals("TaxonIdentification"));
					
					if(ti != null){
						pop.detach();
						ti.addContent(pop);
						System.out.println(f.getName()+ " fixed");
					}else{
						System.out.println(f.getName()+ " to be checked");
					}
				}
				
				treatment.detach();
				doc = new Document(treatment);
				BufferedOutputStream out = new BufferedOutputStream(
						new FileOutputStream(f));
				XMLOutputter outputter = new XMLOutputter();
				outputter.output(doc, out);
			}
			
		}
	}

}
