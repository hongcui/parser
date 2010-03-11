/**
 * 
 */
package fna.parsing;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * @author Hong Updates
 *
 */
public class HabitatParser4FNA extends EnumerativeElementParser{
	
	public HabitatParser4FNA(Element parent, String text, String enutag){
		super(parent, text, enutag);
	}
	
	public Element parse(){
		return parent;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
