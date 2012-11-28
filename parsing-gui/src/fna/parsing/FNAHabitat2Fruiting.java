package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 * used to fix cases like:
 *  <habitat>fruiting late spring–early summer (jul–sep). marshes</habitat>
    <habitat>streamsides</habitat>
    <habitat>ditches</habitat>
 =>
     <phenology_fruiting>fruiting late spring–early summer (jul–sep).</phenology_fruiting> <habitat>marshes</habitat>
    <habitat>streamsides</habitat>
    <habitat>ditches</habitat>
 * 
 * @author Hong Updates
 *
 */
public class FNAHabitat2Fruiting {
	private static final Logger LOGGER = Logger.getLogger(FNAHabitat2Fruiting.class);  

	public static void main(String[] args) {
		String xmldir = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V23-good\\target\\last";		
		try{
			File dir = new File(xmldir);
			File[] files = dir.listFiles();
			int count = 0;
			for(File file: files){
				SAXBuilder builder = new SAXBuilder();
				Document doc = builder.build(file);
				Element treatment = doc.getRootElement();
				List<Element> habitats = treatment.getChildren("habitat");
				int index = -1;
				for(Element habitat : habitats){
					String text = habitat.getTextTrim();
					if(text.startsWith("fruiting") || text.startsWith("Fruiting")){
						System.out.println(count+":"+file.getName()+": "+text);
						index = treatment.indexOf(habitat);
						int cut = text.indexOf(".") + 1;
						if(cut <= 0) cut = text.length();
						String phfr = text.substring(0, cut);
						String newhab = text.replace(phfr, "").trim();
						habitat.detach();
						Element phfre = new Element("phenology_insertion");
						FloweringTimeParser4FNA ftpf = new FloweringTimeParser4FNA(phfre, phfr, "phenology_fruiting");
						ftpf.parse();
						List<Element> phfrs = phfre.getChildren();
						for(int i = 0; i < phfrs.size();){
							Element e = phfrs.get(i);
							e.detach();
							treatment.addContent(index++,e);
						}
						if(newhab.length()>0){
							Element newhabe = new Element("habitat");
							newhabe.setText(newhab);
							treatment.addContent(index, newhabe);
						}
						count++;
						break;
					}									
				}				
				treatment.detach();
				Document newdoc = new Document(treatment);
				BufferedOutputStream out = new BufferedOutputStream(
						new FileOutputStream(file));
				XMLOutputter outputter = new XMLOutputter();
				outputter.output(newdoc, out);
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}
}