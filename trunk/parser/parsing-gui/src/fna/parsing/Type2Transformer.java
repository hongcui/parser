/**
 * 
 */
package fna.parsing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import fna.db.Type4TransformerDbAccessor;

/**
 * @author Hong Updates
 * For extracted XML formated descriptions from Treatise 
 */
public class Type2Transformer extends Thread {

	//private File source =new File(Registry.SourceDirectory); //a folder of text documents to be annotated
	private File source = new File(Registry.SourceDirectory);
	//File target = new File(Registry.TargetDirectory);

	//File target = new File("Z:\\DATA\\Plazi\\2ndFetchFromPlazi\\target-taxonX-ants-trash");
	//private String tableprefix = "plazi_ants";

	//target folder
	File target = new File(Registry.TargetDirectory);
	//private String tableprefix = "plazi_ants";
	

	private XMLOutputter outputter = null;
	// this is the dataprfix from general tab
	private String dataprefix = null;
	private ProcessListener listener;
	protected static final Logger LOGGER = Logger.getLogger(Type3Transformer.class);
	/**
	 * @param listener
	 * @param dataprefix
	 */
	public Type2Transformer(ProcessListener listener, String dataprefix) {
		this.listener = listener;
		this.dataprefix = dataprefix;
	}

	public void run(){
		listener.setProgressBarVisible(true);
		transform();
		listener.setProgressBarVisible(false);
	}
	/**
	 * just take the content of <description>s out and save them in the target folder 
	 */
	public void transform(){
		try{
			File[] files =  source.listFiles();
			SAXBuilder builder = new SAXBuilder();
			listener.progress(1);
			int total = files.length;
			
			for(int i = 0; i<total; i++){
				File f = files[i];		
				Document doc = builder.build(f);
				Element root = doc.getRootElement();
				
				Element descrp = (Element)XPath.selectSingleNode(root, "/treatment/description");
				String text = descrp.getTextNormalize();
				writeDescription2Descriptions(text,f.getName().replaceAll("xml$", "txt") ); //record the position for each paragraph.
				listener.progress((i+1)*100/total);
				listener.info((i++)+"", f.getName());
			}	
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void writeDescription2Descriptions(String textNormalize, String fn) {
		try {
			File file = new File(target+"/descriptions", fn);
			
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(textNormalize);
			out.close(); // don't forget to close the output stream!!!
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("Failed to output text file in Type2Transformer:outputDescriptionText", e);
			throw new ParsingException("Failed to output text file.", e);
		}		
	}
	/**
	 * 
	 */
	public Type2Transformer() {
		// TODO Auto-generated constructor stub
	}

}
