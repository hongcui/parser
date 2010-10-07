/**
 * 
 */
package fna.parsing.datacleaner;

import java.util.*;

import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * @author hongcui
 *
 */
public class CleanTime extends DataCleaner{

	/**
	 * 
	 */
	public CleanTime(String sourcedir, ArrayList<String> sourceElements, String outputElement, String outputdir) {
		super(sourcedir, sourceElements, outputElement, outputdir);
	}

	/*
	 * **************************************************************************************
	 * replace the content of each source element with its legal value
	 * replace the source element name with output element name
	 * 
	 * for flowering time, it is simple replacement
	 */
	protected Element clean(Element root){
		try{
			Iterator<String> it = this.sourceelements.iterator();
			while(it.hasNext()){
				String ename = it.next();
				List<Element> elements = XPath.selectNodes(root, "//"+ename);
				Iterator<Element> eit = elements.iterator();
				while(eit.hasNext()){
					Element e = eit.next();
					ArrayList<String> values = cleanText(e.getText());
					Element p = e.getParentElement();
					p.removeContent(e);
					Iterator<String> vit = values.iterator();
					while(vit.hasNext()){//if values is empty, no replacement is done, but the original element is removed
						Element ce = new Element(this.outputelement);
						ce.setText(vit.next());
						p.addContent(ce);
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return root;
	}



	/**
	 * ***************************************************************************************
	 */
	protected void collectLegalValues(){
		this.legalvalues.put("jan", "1");
		this.legalvalues.put("feb", "1");
		this.legalvalues.put("mar", "1");
		this.legalvalues.put("apr", "1");
		this.legalvalues.put("may", "1");
		this.legalvalues.put("jun", "1");
		this.legalvalues.put("jul", "1");
		this.legalvalues.put("aug", "1");
		this.legalvalues.put("sep", "1");
		this.legalvalues.put("oct", "1");
		this.legalvalues.put("nov", "1");
		this.legalvalues.put("dec", "1");
		this.legalvalues.put("spring", "1");
		this.legalvalues.put("summer", "1");
		this.legalvalues.put("fall", "1");
		this.legalvalues.put("winter", "1");
		this.legalvalues.put("year round", "1");
	} 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
