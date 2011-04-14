/**
 * $Id$
 */
package fna.parsing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import fna.charactermarkup.StanfordParser;
import fna.parsing.state.SentenceOrganStateMarker;

/**
 * @author chunshui
 */
@SuppressWarnings({ "unchecked", "static-access" })
public class VolumeFinalizer extends Thread {
	/*glossary established in VolumeDehyphenizer
	private String glossary;*/

	private static ProcessListener listener;
	private String dataPrefix;
	private static final Logger LOGGER = Logger.getLogger(VolumeFinalizer.class);
	private Connection conn = null;
	private String glossaryPrefix;
	private static String version="$Id$";
	
	public VolumeFinalizer(ProcessListener listener, String dataPrefix, Connection conn, String glossaryPrefix) {
		/*glossary = Registry.ConfigurationDirectory + "FNAGloss.txt"; // TODO
		*/
		this.listener = listener;
		this.dataPrefix = dataPrefix;
		this.conn = conn;
		this.glossaryPrefix = glossaryPrefix;
	}
	

	
	public void run () {
		//listener.setProgressBarVisible(true);
		outputFinal();
		//listener.setProgressBarVisible(false);
	}
	/**
	 * stanford parser
	 * @throws ParsingException
	 */
	public void outputFinal() throws ParsingException {

		//listener.progress(20);
		//updateGlossary(); //active this later 4/2011
		/*
		String posedfile = Registry.TargetDirectory+"/"+this.dataPrefix + "_"+ApplicationUtilities.getProperty("POSED");
		String parsedfile =Registry.TargetDirectory+"/"+this.dataPrefix + "_"+ApplicationUtilities.getProperty("PARSED");
		String database = ApplicationUtilities.getProperty("database.name");
		String postable = ApplicationUtilities.getProperty("POSTABLE");
		*/
		String posedfile = "FNAv19posedsentences.txt";		
		String parsedfile = "FNAv19parsedsentences.txt";
		String database = "annotationevaluation";
		String postable = "wordpos4parser";
		String glosstable = this.glossaryPrefix;
		//SentenceOrganStateMarker sosm = new SentenceOrganStateMarker(conn, "fnav19", "fnaglossaryfixed", true);
		//sosm.markSentences();
		StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, this.dataPrefix,postable,glosstable);
		//sp.POSTagging();
		//sp.parsing();
		sp.extracting();
		//listener.progress(40);
	}
	
	
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
			//get the element index of the unannotated counterpart 	
			Element description = (Element) XPath.selectSingleNode(
					baseroot, xpath);	
			int index=-1;
			Element parent = description;
			if(description!=null){
			 parent= description.getParentElement();
			 index= parent.indexOf(description);
			}
			// replace
			if (index >= 0) {
				//System.out.println(fileindex+".xml has "+xpath+" element replaced");
				parent.setContent(index, adescription);
			}
			baseroot.detach();
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("VolumeFinalizer : Failed to output the final result.", e);
			throw new ParsingException("Failed to output the final result.", e);
		}
	}

	public static Element getBaseRoot(String fileindex, int order){
		//File source = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty("TRANSFORMED"));	
		File source = new File("C:\\DATA\\FNA-v19\\target\\transformed"); 
		int total = source.listFiles().length;
		try {
			SAXBuilder builder = new SAXBuilder();
			System.out.println("finalizing "+fileindex);
			
			//listener.progress(40+(order*60/total));
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

	
	public static void outputFinalXML(Element root, String fileindex, String targetstring) {
		
		//File target = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty(targetstring));
		File target = new File("C:\\DATA\\FNA-v19\\target\\final");
		File result = new File(target, fileindex + ".xml");
		Comment comment = new Comment("produced by "+VolumeFinalizer.version+System.getProperty("line.separator"));
		//Comment comment = null;
		ParsingUtil.outputXML(root, result, comment);
		//listener.info("" + fileindex, result.getPath(), "");//TODO: test 3/19/10 
	}


	public void replaceWithAnnotated(Learn2Parse cl, String xpath, String targetstring, boolean flatten) {
		//File source = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty("TRANSFORMED"));
		File source = new File("C:\\DATA\\FNA-v19\\target\\transformed"); 
		int total = source.listFiles().length;

		//File target = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty(targetstring));
		File target = new File("C:\\DATA\\FNA-v19\\target\\final");
		try {

			SAXBuilder builder = new SAXBuilder();
			for (int count = 1; count <= total; count++) {
				System.out.println("finalizing "+count);
				//listener.progress(40+(count*60/total));
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
				ParsingUtil.outputXML(root, result, null);

				//listener.info("" + count, result.getPath(), "");//TODO: test 3/19/10 
			}

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("VolumeFinalizer : Failed to output the final result.", e);
			throw new ParsingException("Failed to output the final result.", e);
		}
	}
	
	public static void main (String [] args) {
		String database="annotationevaluation";
		String username="root";
		String password="root";
		Connection conn = null;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		VolumeFinalizer vf = new VolumeFinalizer(null, "fnav19", conn, "fnaglossaryfixed");
		vf.start();
	}
}
