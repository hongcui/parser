/**
 * 
 */
package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 * @author Hong Updates
 * part of habitats is mixed with elevation
 * ... <phenology_fruiting>nov</phenology_fruiting><elevation>emergent shorelines; 0–1500 m;</elevation>
 * fix them for JSTOR
 */
public class FNAElevation {
	static Pattern elevation = Pattern.compile("\\d+.*? m;?");
	 private static final Logger LOGGER = Logger.getLogger(FNAElevation.class);  


	/**
	 * 
	 */
	public FNAElevation() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String xmldir = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameReviewer\\CompleteReviewed\\v26_hong_reviewed_final_synrank_adjusted";		
		try{
			File dir = new File(xmldir);
			File[] files = dir.listFiles();
			int count = 0;
			for(File file: files){
				SAXBuilder builder = new SAXBuilder();
				Document doc = builder.build(file);
				Element treatment = doc.getRootElement();
				Element ele = treatment.getChild("elevation");	
				if(ele == null) continue;
				String text = ele.getTextTrim();
				Matcher m = elevation.matcher(text);
				if(m.find()){
					int index = text.indexOf(";");
					if(index> 0 && index != text.length()-1){ //found habitat
						String habitat = text.substring(0, index+1);
						String elestr = text.replace(habitat, "").trim();
						ele.setText(elestr);
						Element hab = null;
						if(habitat.equals("introduced;")) hab = new Element("introduced");
						else if(habitat.contains("of conservation concern")) hab = new Element("conservation");
						else if(habitat.contains("of conser-vation concern")) hab = new Element("conservation");
						else if(habitat.contains("of conservation consern")) hab = new Element("conservation");
						else hab = new Element("habitat");
						hab.setText(habitat);
						int loc = treatment.indexOf(ele);
						treatment.addContent(loc, hab);
						count++;
						System.out.println(habitat+" is habitat ["+file.getName()+"]");
					}						
				}
					
				BufferedOutputStream out = new BufferedOutputStream(
						new FileOutputStream(file));
				XMLOutputter outputter = new XMLOutputter();
				outputter.output(doc, out);
			}
			System.out.println(count+" files fixed");
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}


	}

}
