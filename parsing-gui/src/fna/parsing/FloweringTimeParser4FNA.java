/**
 * 
 */
package fna.parsing;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import java.util.*;

/**
 * @author Hong Updates
 *
 */
public class FloweringTimeParser4FNA extends EnumerativeElementParser{
	static Hashtable<String, String> monthmapping = new Hashtable<String, String>();
		
		
	
	public FloweringTimeParser4FNA(Element parent, String text, String enutag){
		super(parent, text, enutag);
		monthmapping.put("jan", "winter");
		monthmapping.put("feb", "winter");
		monthmapping.put("mar", "spring");
		monthmapping.put("apr", "spring");
		monthmapping.put("may", "spring");
		monthmapping.put("jun", "summer");
		monthmapping.put("jul", "summer");
		monthmapping.put("aug", "summer");
		monthmapping.put("sep", "fall");
		monthmapping.put("oct", "fall");
		monthmapping.put("nov", "fall");
		monthmapping.put("dec", "winter");
	}
	
	public Element parse(){
		text = text.toLowerCase().replaceFirst("flowering\\s+", "");
		String[] months = text.split("[;,.]");
		for(int i = 0; i<months.length; i++){
			String month = months[i].trim();
			if(month.compareTo("") !=0){
				Element enuelement = new Element(enutag);
				enuelement.setText(month);
				parent.addContent(enuelement);
				String season = this.monthmapping.get(month.toLowerCase());
				enuelement = new Element(enutag);
				enuelement.setText(season);
				parent.addContent(enuelement);				
			}		
		}
		return parent;	
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
