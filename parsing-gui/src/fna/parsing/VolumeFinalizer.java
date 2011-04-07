/**
 * $Id$
 */
package fna.parsing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import fna.charactermarkup.StanfordParser;

/**
 * @author chunshui
 */
@SuppressWarnings({ "unchecked", "static-access" })
public class VolumeFinalizer extends Thread {
	//glossary established in VolumeDehyphenizer
	//private String glossary;

	private static ProcessListener listener;
	private String dataPrefix;
	private static final Logger LOGGER = Logger.getLogger(VolumeFinalizer.class);
	private Connection conn = null;
	private String glossaryPrefix;
	
	public VolumeFinalizer(ProcessListener listener, String dataPrefix, Connection conn, String glossaryPrefix) {
		//glossary = Registry.ConfigurationDirectory + "FNAGloss.txt"; // TODO
		this.listener = listener;
		this.dataPrefix = dataPrefix;
		this.conn = conn;
		this.glossaryPrefix = glossaryPrefix;
	}
	
	public static void main (String [] args) {
		
	}
	
	public void run () {
		listener.setProgressBarVisible(true);
		outputFinal();
		listener.setProgressBarVisible(false);
	}
	/**
	 * stanford parser
	 * @throws ParsingException
	 */
	public void outputFinal() throws ParsingException {

		listener.progress(20);
		updateGlossary();
		String posedfile = Registry.TargetDirectory+"/"+this.dataPrefix + "_"+ApplicationUtilities.getProperty("POSED");
		String parsedfile =Registry.TargetDirectory+"/"+this.dataPrefix + "_"+ApplicationUtilities.getProperty("PARSED");
		String database = ApplicationUtilities.getProperty("database.name");
		String postable = ApplicationUtilities.getProperty("POSTABLE");
		//String glosstable = this.dataPrefix + "_"+ApplicationUtilities.getProperty("GLOSSTABLE");
		String glosstable = this.glossaryPrefix;
		StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, this.dataPrefix,postable,glosstable);
		sp.POSTagging();
		sp.parsing();
		sp.extracting();
		listener.progress(40);
		//CharacterLearner cl = new CharacterLearner(ApplicationUtilities.getProperty("database.name")
		//		/*+ "_corpus"*/, dataPrefix);
		//cl.markupCharState();
		// output final records
		// read in treatments, replace description with what cl output
		//replaceWithAnnotated(cl, "treatment/description", "FINAL", false);
	}
	
	/**
	 * 2008 version 
	 */
	/*public void outputFinal() throws ParsingException {

		listener.progress(20);

		CharacterLearner cl = new CharacterLearner(ApplicationUtilities.getProperty("database.name"), dataPrefix);
		listener.progress(40);

		cl.markupCharState();
		// output final records
		// read in treatments, replace description with what cl output
		replaceWithAnnotated(cl, "treatment/description", "FINAL", false);
	}*/
	/**
	 * add new character/state to glossary
	 */
	private void updateGlossary() {
		try{
			Statement stmt = conn.createStatement();
			String q ="select distinct term, decision from "+this.dataPrefix+"_group_decisions as t1, "+this.dataPrefix+"_grouped_terms as t2 where term not in (select term from fnaglossary) and t1.groupId=t2.groupId";
			ResultSet rs = stmt.executeQuery(q);
			Statement stmt1 = conn.createStatement();
			while(rs.next()){				
				String q1 = "insert into fnaglossary (term, category, status) values ('"+rs.getString("term")+"', '"+rs.getString("decision")+"', 'learned')";
				stmt1.execute(q1);
			}
			stmt1.close();
			rs.close();
			stmt.close();						
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	/**
	 * 2010
	 * @param adescription: character-annotated
	 * @param baseroot:root element of the base XML
	 * @param xpath
	 */
	public static void replaceWithAnnotated(Element adescription, Element baseroot, String xpath) {

		try {
			//Element root = doc.getRootElement();	
			//List<Element> content = adescription.getContent();
			//get the element index of the unannotated counterpart 	
			Element description = (Element) XPath.selectSingleNode(
					baseroot, xpath);	
			int index=-1;
			Element parent = description;
			if(description!=null){
			 parent= description.getParentElement();
			//int index = baseroot.indexOf(description);
			 index= parent.indexOf(description);
			}
			// replace
			if (index >= 0) {
				//System.out.println(fileindex+".xml has "+xpath+" element replaced");
				//baseroot.setContent(index, adescription);
				parent.setContent(index, adescription);
			}
			baseroot.detach();
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("VolumeFinalizer : Failed to output the final result.", e);
			throw new ParsingException("Failed to output the final result.", e);
		}
	}

	/*public static void replaceWithAnnotated(Element adescription, Document doc, String descId, String xpath, boolean flatten) {
		try{
			Element root = doc.getRootElement();	
			List<Element> content = adescription.getContent();
			//get the element index of the unannotated counterpart 	
			Element description = (Element) XPath.selectSingleNode(
					root, xpath);			
			int index = root.indexOf(description);
			
			// replace
			if (index >= 0) {
				//System.out.println(fileindex+".xml has "+xpath+" element replaced");
				root.setContent(index, adescription);
			}
			root.detach();
			
			//outputFinalXML(target, root);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("VolumeFinalizer : Failed to output the final result.", e);
			throw new ParsingException("Failed to output the final result.", e);
		}
	}*/
	//public static Element getBaseRoot(int fileindex){
	public static Element getBaseRoot(String fileindex, int order){
		File source = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty("TRANSFORMED"));	
		//File source = new File("C:\\DATA\\FNA-v19\\target\\transformed"); 
		int total = source.listFiles().length;
		try {
			SAXBuilder builder = new SAXBuilder();
			System.out.println("finalizing "+fileindex);
			
			listener.progress(40+(order*60/total));
			File file = new File(source, fileindex + ".xml");
			Document doc = builder.build(file);
			
			Element root = doc.getRootElement();
			root.detach();
			return root;
		}catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("VolumeFinalizer : Failed to output the final result.", e);
			throw new ParsingException("Failed to output the final result.", e);
		}
	}

	//public static void outputFinalXML(Element root, int fileindex, String targetstring) {
	public static void outputFinalXML(Element root, String fileindex, String targetstring) {
		
		File target = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty(targetstring));
		//File target = new File("C:\\DATA\\FNA-v19\\target\\final");
		File result = new File(target, fileindex + ".xml");
		ParsingUtil.outputXML(root, result);
		listener.info("" + fileindex, result.getPath(), "");//TODO: test 3/19/10 
	}


	public void replaceWithAnnotated(Learn2Parse cl, String xpath, String targetstring, boolean flatten) {
		File source = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty("TRANSFORMED"));
		int total = source.listFiles().length;

		File target = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty(targetstring));

		try {

			SAXBuilder builder = new SAXBuilder();
			for (int count = 1; count <= total; count++) {
				System.out.println("finalizing "+count);
				listener.progress(40+(count*60/total));
				File file = new File(source, count + ".xml");
				Document doc = builder.build(file);
				Element root = doc.getRootElement();	
				//create xml from annotated text 2008 version
				ArrayList<String> elementstrs = (ArrayList<String>)cl.getMarkedDescription(count + ".txt");	
				Iterator<String> it = elementstrs.iterator();
				ArrayList<Element> content = new ArrayList<Element>();
				while(it.hasNext()){
					String descXML =(String)it.next();
					if (descXML != null && !descXML.equals("")) {
						doc = builder.build(new ByteArrayInputStream(
								descXML.getBytes("UTF-8")));
						Element descript = doc.getRootElement(); // marked-up
						descript.detach();
						content.add(descript);
					}
				}
				//get the element index of the unannotated counterpart 	
				Element description = (Element) XPath.selectSingleNode(
						root, xpath);			
				int index = root.indexOf(description);
				
				// replace
				if (index >= 0) {
					System.out.println(count+".xml has "+xpath+" element replaced");
					root.setContent(index, content);
				}
				
				root.detach();
				
				File result = new File(target, count + ".xml");
				ParsingUtil.outputXML(root, result);

				listener.info("" + count, result.getPath(), "");//TODO: test 3/19/10 
			}

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("VolumeFinalizer : Failed to output the final result.", e);
			throw new ParsingException("Failed to output the final result.", e);
		}
	}
}
