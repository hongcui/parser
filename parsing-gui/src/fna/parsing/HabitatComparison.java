package fna.parsing;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * compare <discussion> elements in xml files of two folders
 * @author Hong Updates
 *
 */
public class HabitatComparison {
	 private static final Logger LOGGER = Logger.getLogger(HabitatComparison.class);  

	public static void main(String[] args){
		String folder1 = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V21-good\\target\\last";
		String folder2 = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameReviewer\\finalnew\\V21_last_good\\reviewed_by_hong_tocheck_synonym";
		File F1 = new File(folder1);
		File F2 = new File(folder2);
		File[] files1 = F1.listFiles();
		int count = 0;
		try{
			for(File file1 : files1){
				SAXBuilder builder = new SAXBuilder();
				Document doc1 = builder.build(file1);
				Element treatment1 = doc1.getRootElement();
				Document doc2 = builder.build(new File(F2, file1.getName()));
				Element treatment2 = doc2.getRootElement();
				List<Element> habitats1 = treatment1.getChildren("habitat");
				List<Element> habitats2 = treatment2.getChildren("habitat");
				if(habitats1.size() != habitats2.size()){
					System.out.println(file1.getName());
					count++;
				}else{
					for(int i = 0; i < habitats1.size(); i++){
						Element habitat1 = habitats1.get(i);
						Element habitat2 = habitats2.get(i);
						if(!habitat1.getTextTrim().equals(habitat2.getTextTrim())){
							System.out.println(file1.getName());
							count++;
							break;
						}
					}
				}
				
				
			}
			System.out.println("total:"+count);
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}
}