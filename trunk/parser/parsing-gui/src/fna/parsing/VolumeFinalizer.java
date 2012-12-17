/**
 * $Id$
 */
package fna.parsing;

import java.io.*;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import org.jdom.Comment;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

//import conversion.DescriptionParser;
//import conversion.TaxonCharacterMatrix;
//import dao.FilenameTaxonDao;

//import taxonomy.ITaxon;
//import taxonomy.SubTaxonException;
//import taxonomy.TaxonHierarchy;
//import taxonomy.TaxonRank;

import fna.charactermarkup.SentenceChunker4StanfordParser;
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
    private static boolean standalone = false;
    //standalone set to true if running from the stanfordparser.java. Also have to set the standalonefolder to the current folder that is processed.
    //standalone set to false when running from the interface.
	private static String standalonefolder = "C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\FNAv7Limnanthaceae";

    //private static String standalonefolder = "C:\\temp\\DEMO\\demo-folders\\FNA-v19-excerpt";
    //private static String standalonefolder = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\phenoscape-fish-source";
    //private static String standalonefolder = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\V4";
    //private static String standalonefolder = "C:\\Users\\mohankrishna89\\Desktop\\Ant Work\\Plazi_8405_tx";
    
	private Text finalLog;
    private Display display;
    
    public VolumeFinalizer(ProcessListener listener, Text finalLog, String dataPrefix, Connection conn, String glossaryPrefix, Display display) {
        /*glossary = Registry.ConfigurationDirectory + "FNAGloss.txt"; // TODO
        */
        if(!standalone) this.listener = listener;
    	if(!standalone) this.finalLog = finalLog;
    	if(!standalone) this.display = display;
    	this.dataPrefix = dataPrefix;
        this.conn = conn;
        this.glossaryPrefix = glossaryPrefix;
    }
    

	
	public void run () {
		//if(!standalone) listener.setProgressBarVisible(true);
		try{
			outputFinal();
			if(MainForm.createMatrix) createMatrix();
		}catch(Exception e){
			this.showOutputMessage(e.toString());
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
        //check for final result errors
        File finalFileList= null;
        File transformedFileList = null;
        if(!standalone){
        	finalFileList = new File(Registry.TargetDirectory+"\\final\\");
        	transformedFileList = new File(Registry.TargetDirectory+"\\transformed\\");
        }
        if(standalone){
        	finalFileList = new File(this.standalonefolder+"\\final\\");
        	transformedFileList = new File(this.standalonefolder+"\\transformed\\");
        }
        if(finalFileList.list().length == 0)
        {
            //this.popupMessage("No file is output");
    		if(!standalone) this.showOutputMessage("System terminates with errors. No files has been annotated.");
        }
        if(finalFileList.list().length != transformedFileList.list().length)
        {
            //this.popupMessage("No file is output");
    		if(!standalone) this.showOutputMessage("Number of files does not match between transformed and final folders. You should check all files with descriptions are annotated.");
        }
        //if(!standalone) listener.setProgressBarVisible(false);
        
        else//Should add the code to generate the files and uploading and executing it here in the else loop.
        {
       		if(!standalone){
       			this.showOutputMessage("System is done with annotating files.");
       			this.showOutputMessage("The annotated files are saved in "+Registry.TargetDirectory+"final\\");
       		}
        }
    }
    private void createMatrix() {
    	//check file names to determine top and lower taxon rank
  		if(!standalone){
   			this.showOutputMessage("\n System is generating a taxon-character matrix from annotated files");
      	}
  	
  		//generate filename2taxon table
  		if(this.dataPrefix.contains("treatise")){
  			FileName2TaxonTreatise fntf = new FileName2TaxonTreatise(Registry.SourceDirectory, conn, dataPrefix);
  			fntf.createFilename2taxonTable();
  			fntf.populateFilename2TaxonTable();
  		}
  		if(this.dataPrefix.contains("fna")){
  			FileName2TaxonFNA fntf = new FileName2TaxonFNA(Registry.SourceDirectory, conn, dataPrefix);
  			fntf.createFilename2taxonTable();
  			fntf.populateFilename2TaxonTable();
  		}
  		
		//generate configuration files to SDD
  		createDatabasePropertiesFile();
  		createDescriptionPropertiesFile();
  		
  		//start SDD process
    	/*Object[] rankrange = getTaxonRankRange();
		TaxonHierarchy th = makeHierarchyTwoLevel((String)rankrange[0], (TaxonRank)rankrange[1], (TaxonRank)rankrange[2]);
		th.printSimple();
		TaxonCharacterMatrix matrix = new TaxonCharacterMatrix(th);
		matrix.generateMatrixFile(Registry.TargetDirectory+rankrange[0]+"matrix.txt");
  		if(!standalone){
   			this.showOutputMessage("The taxon-character matrix is saved in "+Registry.TargetDirectory+rankrange[0]+"matrix.txt");
   		}*/
	}

    /**
     * jaxb.annotation.directory = /bin/annotationSchema/jaxb
	   input.path = C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\TreatisePartO\\target\\final
     */
    private void createDescriptionPropertiesFile() {
    	StringBuffer sb = new StringBuffer();
    	sb.append("jaxb.annotation.directory = /bin/annotationSchema/jaxb"+System.getProperty("line.separator"));
    	sb.append("input.path = ");
    	sb.append((Registry.TargetDirectory+"final/").replaceAll("\\", "/")+System.getProperty("line.separator"));
    	try{
    		//C:\Documents and Settings\Hong Updates\workspace\parsing-gui
    		//C:\Documents and Settings\Hong Updates\workspace\SDD\src\dao\database.properties
     		String outputfile = System.getProperty("user.dir").replaceFirst("parsing-gui$", "")+"SDD\\src\\conversion\\description.properties";   		
    		FileWriter fstream = new FileWriter(outputfile);
    		BufferedWriter out = new BufferedWriter(fstream);
    		out.write(sb.toString());
    		out.close();
    	}catch (Exception e){//Catch exception if any
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
    	}    		  		
	}



	/**
     *  user = root
		password = root
		driver = com.mysql.jdbc.Driver
		url = jdbc:mysql://localhost:3306/
		singular-plural-database = markedupdatasets
		singular-plural-table-name = treatise_o_test_singularplural
		filename-database = markedupdatasets
		filename-table-name = treatise_o_test_filename2taxon
     */
    private void createDatabasePropertiesFile() {
    	StringBuffer sb = new StringBuffer();
    	sb.append("user = ");
    	sb.append(ApplicationUtilities.getProperty("database.username")+System.getProperty("line.separator"));
    	sb.append("password = ");
    	sb.append(ApplicationUtilities.getProperty("database.password")+System.getProperty("line.separator"));
    	sb.append("driver = ");
    	sb.append(ApplicationUtilities.getProperty("database.driverPath")+System.getProperty("line.separator"));
    	sb.append("url = ");
    	sb.append("jdbc:mysql://localhost:3306/"+System.getProperty("line.separator"));
    	sb.append("singular-plural-database = ");
    	sb.append(ApplicationUtilities.getProperty("database.name")+System.getProperty("line.separator"));
    	sb.append("singular-plural-table-name = ");
    	sb.append(this.dataPrefix+"_singularplural"+System.getProperty("line.separator"));    	
    	sb.append("filename-database = ");
    	sb.append(ApplicationUtilities.getProperty("database.name")+System.getProperty("line.separator"));
    	sb.append("filename-table-name = ");
    	sb.append(this.dataPrefix+"_filename2taxon");
    	
    	try{
    		
    		//C:\Documents and Settings\Hong Updates\workspace\parsing-gui
    		//C:\Documents and Settings\Hong Updates\workspace\SDD\src\dao\database.properties
    		String outputfile = System.getProperty("user.dir").replaceFirst("parsing-gui$", "")+"SDD\\src\\dao\\database.properties";   		
   		  	FileWriter fstream = new FileWriter(outputfile);
   		  	BufferedWriter out = new BufferedWriter(fstream);
   		  	out.write(sb.toString());
   		  	out.close();
    	}catch (Exception e){//Catch exception if any
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
    	}
    		  
	}



	/**
     * assume file names indicate the sequence in original description
     * assume all file names belong to the same highest rank.
     * @return object[0] taxon name of the taxon with the highest rank, 
     *         object[1] the highest rank
     *         object[2] the lowest rank
     */
	/*private Object[] getTaxonRankRange() {
		ArrayList<String> ranks = new ArrayList<String>();
		ranks.add("domain");
		ranks.add("kingdom");
		ranks.add("phylum");
		ranks.add("subphylum");
		ranks.add("superdivision");
		ranks.add("division");
		ranks.add("subdivision");
		ranks.add("superclass");
		ranks.add("class");
		ranks.add("subclass");
		ranks.add("superorder");
		ranks.add("order");
		ranks.add("suborder");
		ranks.add("superfamily");
		ranks.add("family");
		ranks.add("subfamily");
		ranks.add("tribe");
		ranks.add("subtribe");
		ranks.add("genus");
		ranks.add("subgenus");
		ranks.add("section");
		ranks.add("subsection");
		ranks.add("species");
		ranks.add("subspecies");
		ranks.add("variety");
		
		
		
		Object[] results = new Object[3];
		File[] files = (new File(Registry.TargetDirectory+"final")).listFiles();
		int[] filenames = new int[files.length];
		//from 1.xml 10.xml, 100.xml ...
		
		int i = 0;
		for(File file: files){
			filenames[i++]= Integer.parseInt(file.getName().replace(".xml", ""));
		}
		//to 1, 2, 3, 4
		Arrays.sort(filenames); 
		
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = null;
			//look for the highest rank
			for(String rank: ranks){
				String q ="select `"+rank+"` from "+this.dataPrefix+"_filename2taxon where length(`"+rank+"`)>0";
				rs = stmt.executeQuery(q);
				if(rs.next()){
					results[0] = rs.getString(1);
					results[1] = TaxonRank.valueOf(rank.toUpperCase());
					break; //found highest rank
				}
			}
			//look for the lowest rank
			for(i = ranks.size()-1; i >=0; i--){
				rs = stmt.executeQuery("select `"+ranks.get(i)+"` from "+this.dataPrefix+"_filename2taxon where length(`"+ranks.get(i)+"`)>0");
				if(rs.next()){
					results[2] = TaxonRank.valueOf(ranks.get(i).toUpperCase());;
					break; //found lower rank
				}
			}		
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
		return results;
	}*/
    /*
	private TaxonHierarchy makeHierarchyTwoLevel(String topName, TaxonRank topRank, TaxonRank bottomRank) {
		FilenameTaxonDao dao = new FilenameTaxonDao();
		System.out.println("Making hierarchy for family: " + topName);
		DescriptionParser parser = new DescriptionParser(topName, topRank);
		ITaxon taxon = parser.parseTaxon();
		String rankTop = dao.getFilenameForDescription(topRank, topName);
		List<String> rankLower = dao.getFilenamesForManyDescriptions(topRank, topName, bottomRank);
		rankLower.remove(rankTop);
		TaxonHierarchy h = new TaxonHierarchy(taxon);
		DescriptionParser bottomParser;
		for(String s : rankLower) {
			String bottomName = dao.getTaxonValues(s).get(bottomRank.toString().toLowerCase());
			System.out.println("Bottom name: " + bottomName);
			bottomParser = new DescriptionParser(bottomName, bottomRank);
			ITaxon bottomTaxon = bottomParser.parseTaxon();
			try {
				h.addSubTaxon(bottomTaxon);
			}
			catch (SubTaxonException e) {
				//System.out.println(e.getMessage());
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			}
		}
		return h;
	}
	*/
	/**
     * stanford parser
     * @throws ParsingException
     */

    public void outputFinal() throws Exception {
    	//if(!standalone)  listener.progress(10);
    	if(!standalone) this.showOutputMessage("System is starting finalization step [could take hours]...");
    	
		//updateGlossary(); //active this later 4/2011
		
		String posedfile = Registry.TargetDirectory+"/"+this.dataPrefix + "_"+ApplicationUtilities.getProperty("POSED");
		String parsedfile =Registry.TargetDirectory+"/"+this.dataPrefix + "_"+ApplicationUtilities.getProperty("PARSED");
		String database = ApplicationUtilities.getProperty("database.name");
		
		/*
		String posedfile = "FNAv19posedsentences.txt";		
		String parsedfile = "FNAv19parsedsentences.txt";
		String database = "annotationevaluation";
		String postable = "wordpos4parser";
		*/
		String glosstable = this.glossaryPrefix;
		

		String transformeddir = Registry.TargetDirectory+"\\transformed\\";
		//Used to collect taxon names for Taxon X ant documents like Plazi_8538_pyr_mad_tx1- Use if type 4 else use Hong's
		/*if(MainForm.type.contentEquals("type4")){
			TaxonNameCollector tnc = new TaxonNameCollector4TaxonX(conn, transformeddir, dataPrefix+"_taxonnames", dataPrefix);
			tnc.collect4TaxonX();
		}
		else{
			TaxonNameCollector tnc = new TaxonNameCollector(conn, transformeddir, dataPrefix+"_taxonnames", dataPrefix);
			tnc.collect();
		}*/


		StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, this.dataPrefix,glosstable, false);
		if(!standalone) this.showOutputMessage("System is POS-tagging sentences...");
		sp.POSTagging();
		//if(!standalone) listener.progress(50);
		if(!standalone) this.showOutputMessage("System is syntactic-parsing sentences...");		
		sp.parsing();
		//if(!standalone) listener.progress(80);
		if(!standalone) this.showOutputMessage("System is annotating sentences...");
		sp.extracting();
		//if(!standalone) listener.progress(100);
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
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
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
            StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
            //LOGGER.error("VolumeFinalizer : Failed to output the final result.", e);
            throw new ParsingException("Failed to output the final result.", e);
        }
    }

	public static Element getBaseRoot(String fileindex, int order){
		File source = null;
		if(!standalone)  source = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty("TRANSFORMED"));	
		if(standalone)  source = new File(standalonefolder+"\\target\\transformed"); 
		int total = source.listFiles().length;
		try {
			SAXBuilder builder = new SAXBuilder();
			System.out.println("finalizing "+fileindex);
			
			//if(!standalone) listener.progress(40+(order*60/total));
			File file = new File(source, fileindex + ".xml");
			Document doc = builder.build(file);
			
			Element root = doc.getRootElement();
			root.detach();
			return root;
		}catch (Exception e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			//LOGGER.error("VolumeFinalizer : Failed to output the final result.", e);
			throw new ParsingException("Failed to output the final result.", e);
		}
	}


	/**
	 * called by StanfordParser.extracting()
	 * @param root
	 * @param fileindex
	 * @param targetstring
	 */
	public static void outputFinalXML(Element root, String fileindex, String targetstring) {
		File target = null;
		if(!standalone) target = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty(targetstring));
		if(standalone) target = new File(standalonefolder+"\\target\\final");
		File result = new File(target, fileindex + ".xml");
		Comment comment = new Comment("produced by "+VolumeFinalizer.version+System.getProperty("line.separator"));
		//Comment comment = null;
		ParsingUtil.outputXML(root, result, comment);
		if(!standalone) listener.info("" + fileindex, result.getPath(), "");
	}

	public ArrayList<String> replaceWithAnnotated(Learn2Parse cl, String xpath, String targetstring, boolean flatten) {
		File source = null;
		File target = null;
		ArrayList<String> parsed = new ArrayList<String>();
		if(!standalone)  source = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty("TRANSFORMED"));
		if(standalone)  source = new File(standalonefolder+"\\target\\transformed"); 
		int total = source.listFiles().length;
		if(!standalone)  target = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty(targetstring));
		if(standalone)  target = new File(standalonefolder+"\\target\\final");
		try {
			SAXBuilder builder = new SAXBuilder();
			for (int count = 1; count <= total; count++) {
				if(!standalone) this.showOutputMessage("System is finalizing "+count+"...");
				System.out.println("finalizing "+count);
				//if(!standalone) listener.progress(40+(count*60/total));
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

				if(!standalone){
					listener.info("" + count, result.getPath(), "");
				}
				parsed.add(result.getPath());
			}
        } catch (Exception e) {
            StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
            //LOGGER.error("VolumeFinalizer : Failed to output the final result.", e);
            throw new ParsingException("Failed to output the final result.", e);
        }
		return parsed;
    }
	
	/**
	 * files with a description size = 0 will not get written to final copy. they need to be copied from transformed folder to final folder
	 */
	public static void copyFilesWithoutDescriptions2FinalFolder() {
		File source = null;
		File target = null;
		File description = null;
		if(!standalone)  source = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty("TRANSFORMED"));
		if(standalone)  source = new File(standalonefolder+"\\target\\transformed"); 
		if(!standalone)  target = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty("FINAL"));
		if(standalone)  target = new File(standalonefolder+"\\target\\final");
		if(!standalone)  description = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty("DESCRIPTIONS"));
		if(standalone)  description = new File(standalonefolder+"\\target\\descriptions");
		//find in description files have a size of zero
		ArrayList<String> files4copy = new ArrayList<String>();
		File[] des = description.listFiles();
		for(File df: des){
			if(df.length()==0){
				files4copy.add(df.getName());
			}
		}
		//for each file in files4copy, copy from transformed to final
		try{
			SAXBuilder builder = new SAXBuilder();
			for(String f: files4copy){
				f = f.replaceFirst("txt$", "xml");
				Document trans = builder.build(new File(source, f));
				Element root = trans.getRootElement();
				root.detach();
				ParsingUtil.outputXML(root, new File(target, f) ,null);
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
		
	}
    protected void resetOutputMessage() {
		display.syncExec(new Runnable() {
			public void run() {
				if(finalLog!=null) finalLog.setText("");
			}
		});
	}
    
	protected void showOutputMessage(final String message) {
		display.syncExec(new Runnable() {
			public void run() {
				if(finalLog!=null) finalLog.append(message+"\n");
			}
		});
	}
	 
	protected void popupMessage(final String message){
		display.syncExec(new Runnable() {
			public void run() {
				ApplicationUtilities.showPopUpWindow(message, "Error",SWT.ERROR);
			}
		});
	}
    
    public static void main (String [] args) {
        /*String database="annotationevaluation";
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
            StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
        }
        VolumeFinalizer vf = new VolumeFinalizer(null, "fnav19", conn, "fnaglossaryfixed");
        vf.start();*/
    	//Don't forget to delete the comment of the following line
    	VolumeFinalizer.copyFilesWithoutDescriptions2FinalFolder();
    	
    	//mohan checking code to create a text file provided a dataset prefix and a dataprefix are given
    	//createTextFile("oldtext", "datasetprefix");
    	//Mohan code to create a datadump of term_category and sentence tables based on dataprefix 
    	//dumpFiles("treatise_o");
    }
    

    	
    	



}
