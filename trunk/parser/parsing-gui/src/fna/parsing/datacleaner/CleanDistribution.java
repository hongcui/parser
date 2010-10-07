/**
 * 
 */
package fna.parsing.datacleaner;

import java.util.*;

import org.jdom.Element;
import org.jdom.xpath.XPath;

/**
 * merge all kinds of distribution to outputelement/"general_distribution"
 * also parse out introduced, and cultivated from sourceelements(including elevation) 
 * outputelement, elevation, introduced, and cultivated should be child nodes of DISTRIBUTION, sibling nodes of ecological_info.
 * keep only capitalized words
 * @author hongcui
 *
 */
public class CleanDistribution extends DataCleaner {
	private String directions="c|e|w|s|n|ec|wc|sc|nc|nw|ne|sw|se|central|east|west|south|north|eastcentral|westcentral|southcentral|northcentral|northwest|northeast|southwest|southeast";
	
	/**
	 * sourceElements should also include xx_distribution, "elevation"
	 */
	public CleanDistribution(String sourcedir, ArrayList<String> sourceElements, String outputElement, String outputdir) {
		super(sourcedir, sourceElements, outputElement, outputdir);
		this.legalvalues.put("northern hemisphere", "1");
		this.legalvalues.put("southern hemisphere", "1");
		this.legalvalues.put("worldwide", "1");
	}

	protected Element clean(Element root){
		try{
			Element gdistribution = new Element("distribution");
			root.addContent(gdistribution);
			Iterator<String> it = this.sourceelements.iterator();
			while(it.hasNext()){
				String ename = it.next();
				List<Element> elements = XPath.selectNodes(root, "//"+ename);
				Iterator<Element> eit = elements.iterator();
				while(eit.hasNext()){
					Element e = eit.next();
					String text = e.getText();
					int type = 0;
					if(text.trim().toLowerCase().startsWith("introduced")){
						type = 1;
					}else if(text.trim().toLowerCase().startsWith("cultivat")){
						type = 2;
					}
					if(ename.endsWith("elevation")){
						type = 3;
					}
					ArrayList<String> values = cleanText(text);
					Element p = e.getParentElement();					
					p.removeContent(e);
					Iterator<String> vit = values.iterator();
					while(vit.hasNext()){//if values is empty, no replacement is done, but the original element is removed
						Element ce = null;
						String t = vit.next();
						switch (type){
						case 1: ce = new Element("introduced");break;
						case 2: ce = new Element("cultivated");break;
						case 3: ce = new Element("elevation");
								t = t.replaceFirst("^[^-\\(\\d]+", "").replaceAll("–", "-").replaceAll("[)(]", "").replaceAll("-\\d+-", "-");
								break;
						default:ce = new Element(this.outputelement);
						}
						
						ce.setText(t);
						gdistribution.addContent(ce);
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return root;
	}


	protected void collectLegalValues(){
		Iterator<String> it = this.sourcecontent.iterator();
		while(it.hasNext()){
			String text = it.next();
			text = text.replaceAll("\\b[a-z]+\\b", "*").replaceAll("\\p{Punct}", ""); //remove all words that are not capitalized
			String[] values = text.split("\\*+");
			for(int i = 0; i<values.length; i++){
				String v = this.legalvalues.get(values[i]);
				if(v==null) this.legalvalues.put(values[i], "1");
			}
		}
	} 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
