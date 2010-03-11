/**
 * 
 */
package fna.parsing;

import org.jdom.Element;

/**
 * @author Hong Updates
 *
 */
public class DistributionParser4FNA extends EnumerativeElementParser {

	/**
	 * @param parent
	 * @param text
	 */
	public DistributionParser4FNA(Element parent, String text, String enutag) {
		super(parent, text, enutag);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see fna.parsing.ElementParser#parse()
	 */
	@Override
	protected Element parse() {
		String[] areas = text.split("[;,.]");
		for(int i = 0; i<areas.length; i++){
			String area = areas[i].trim();
			if(area.compareTo("") !=0){
				Element enuelement = new Element(enutag);
				enuelement.setText(area);
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
