package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.morph.WordnetStemmer;
import edu.smu.tspell.wordnet.*;
import edu.smu.tspell.wordnet.impl.file.Morphology;
import edu.smu.tspell.wordnet.impl.file.WordFormLookup;

public class FNAPostprocessArchitecture {
	protected static final Logger LOGGER = Logger.getLogger(FileName2Taxon.class);
	private String wordnetPath;
	private Connection conn;
	public Connection connectDB(String database) {	
		conn = null;
		if(conn == null){
			try{
				Class.forName("com.mysql.jdbc.Driver");
			    String URL = ApplicationUtilities.getProperty("database.url");
			    URL = "jdbc:mysql://localhost/"+database+URL.substring(URL.indexOf("?"));
			   	//String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			}
		}
		return conn;
	}
	
	public void closeDB() {	
		if(conn != null){
			try{
				conn.close();
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			}
		}
	}
	
	public void setWordnetPath(String wordnetPath) {	
		this.wordnetPath = wordnetPath;
	}
	
	public List<String> getTermListForCategory(String category) {
		List<String> res = new LinkedList<String>();
		Statement stmt=null;
		try {
			stmt = conn.createStatement();
			ResultSet resultSet = stmt.executeQuery("select term from fnaglossaryfixed where category = '"+category +"'");

			String term;
			while(resultSet.next()) { 
				term=resultSet.getString("term"); 
				res.add(term);
			}

		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			LOGGER.error(ApplicationUtilities
					.getProperty("CharaParser.version")
					+ System.getProperty("line.separator") + sw.toString());
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (Exception e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities
						.getProperty("CharaParser.version")
						+ System.getProperty("line.separator") + sw.toString());
			}
		}
		return res;

	}
	
	public String getCategoryForGivenTerm(String term) {
		String category=null;
		Statement stmt=null;
		try {
			stmt = conn.createStatement();
			ResultSet resultSet = stmt.executeQuery("select category from fnaglossaryfixed where term = '"+term +"'");
			if(resultSet.next()) { 
				category=resultSet.getString("category"); 
			}

		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			LOGGER.error(ApplicationUtilities
					.getProperty("CharaParser.version")
					+ System.getProperty("line.separator") + sw.toString());
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (Exception e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities
						.getProperty("CharaParser.version")
						+ System.getProperty("line.separator") + sw.toString());
			}
		}
		return category;

	}
	
	public boolean isStructure(String term) {
		boolean res=false;
		Statement stmt=null;
		try {
			stmt = conn.createStatement();
			ResultSet resultSet = stmt.executeQuery("select category from fnaglossaryfixed where term = '"+term +"'");
			if(resultSet.next()) { 
				if (resultSet.getString("category").equals("structure")){
					res = true;
				}; 
			}

		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			LOGGER.error(ApplicationUtilities
					.getProperty("CharaParser.version")
					+ System.getProperty("line.separator") + sw.toString());
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (Exception e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities
						.getProperty("CharaParser.version")
						+ System.getProperty("line.separator") + sw.toString());
			}
		}
		return res;

	}
	
	//Use worknet interface to get the original form of a term
	public String[] getOriginalVerbForm(String term) {
		String[] ret=null;		
		System.setProperty("wordnet.database.dir", wordnetPath);		

		Dictionary dictionary = new Dictionary(new File(wordnetPath));
		try {
			dictionary.open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		WordnetStemmer stemmer = new WordnetStemmer(dictionary);
		List<String> stems = stemmer.findStems(term, edu.mit.jwi.item.POS.VERB);
		ret = new String[stems.size()];
		int i = 0;
		for(String stem : stems){
			ret[i] = stem;
			i++;
		}
		dictionary.close();

		return ret;
	}
	
	//Use worknet interface to get the original form of a term
	public String[] getOriginalAdjForm(String term) {
		String[] ret=null;		
		System.setProperty("wordnet.database.dir", wordnetPath);		

		Dictionary dictionary = new Dictionary(new File(wordnetPath));
		try {
			dictionary.open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		WordnetStemmer stemmer = new WordnetStemmer(dictionary);
		List<String> stems = stemmer.findStems(term, edu.mit.jwi.item.POS.ADJECTIVE);
		if (stems.size() > 0) {
			ret = new String[stems.size()];
			int i = 0;
			for (String stem : stems) {
				ret[i] = stem;
				i++;
			}
		}
		dictionary.close();
		
		return ret;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		FNAPostprocessArchitecture ppa = new FNAPostprocessArchitecture();		
		ppa.setWordnetPath("C:\\Program Files (x86)\\WordNet\\2.1\\dict");
		ppa.connectDB("markedupdatasets");
		
		String path = "C:\\Users\\jingliu5\\UFLwork\\Charaparser\\V8\\target\\final"; //
		String outPath = "C:\\Users\\jingliu5\\UFLwork\\Charaparser\\V8\\target\\final_NA";

		String volume = "V3_";
		FileOutputStream fout = null;// FileWriter

		String file;
		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();
		File outputdir = new File(outPath);
		if (!outputdir.exists())
			outputdir.mkdir();

		List<String> architectureList = ppa.getTermListForCategory("architecture");
//		List<String> structureList = getTermListForCategory(conn,"structure");
		HashMap architectureMap = new HashMap();
		HashMap architectureContraintMap = new HashMap();

		for (String archi:architectureList){
			String constraint = "";
			String structure = "";
			String[] structureCandidates=null;
			if (archi.endsWith("ed")) {
				if (archi.indexOf("-") > 0) {
					constraint = archi.substring(0, archi.indexOf("-"));
					if (constraint.equals("one")||constraint.equals("two")||constraint.equals("single")||constraint.equals("many"))
						constraint="count";
					structure = archi.substring(archi.indexOf("-") + 1);
				}else
					structure = archi;
				structureCandidates = ppa.getOriginalVerbForm(structure);				
			}
			if (structureCandidates!=null) {
				for(int i = 0;i<structureCandidates.length;i++){
					structure = structureCandidates[i];
					if (ppa.isStructure(structure)) {
						architectureMap.put(archi, structure);
						if (!constraint.equals(""))
							architectureContraintMap.put(archi, constraint);
						break;
					}
				}	

			}
		}

		
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				file = listOfFiles[i].getName();
				System.out.println(file);
				File xmlFile = new File(path + "\\" + file);
				Document document;

				try {
					XPath xpath = XPath
							.newInstance("//character[@name='architecture']");
					SAXBuilder builder = new SAXBuilder();
					Document doc = builder.build(xmlFile);
					Element root = doc.getRootElement();

					List<Element> list = xpath.selectNodes(root);
					for (Element character : list) {
						String architecture = character
								.getAttributeValue("value");
						String archiStructure = (String)architectureMap.get(architecture);
						String constraint = (String)architectureContraintMap.get(architecture);
						String modifier = character.getAttributeValue("modifier");
						if (archiStructure!=null) {
							Element structure = character.getParentElement();
							Element statement = structure.getParentElement();
							structure.removeContent(character);
							Element newStructure = new Element("structure");
							String id = structure.getAttributeValue("id");
							newStructure.setAttribute("id", id + "_1");
							newStructure.setAttribute("name", archiStructure);
							if (constraint!=null){  //architecture has a constraint
								if(constraint.equals("count")){  //constraint is count
									Element newCharacter = new Element("character");
									String count;
									String countStr = architecture.substring(0, architecture.indexOf("_"));
									if (countStr.equals("1")||countStr.equals("one")||countStr.equals("single"))
										count = "1";
									else if (countStr.equals("2")||countStr.equals("two"))
										count = "2";
									else  
										count = countStr;
									newCharacter.setAttribute("name","count");
									newCharacter.setAttribute("value",count);
									newStructure.addContent(newCharacter);
								}else{    
									newStructure.setAttribute("constraint", architecture.substring(0, architecture.indexOf("_")));
								}
							}
							if (modifier!=null){  //architecture has a modifier
								System.out.println(modifier);
								String[] originalMod = ppa.getOriginalAdjForm(modifier);
								if (originalMod != null) {
									String category = ppa
											.getCategoryForGivenTerm(originalMod[0]);
									Element newCharacter = new Element(
											"character");
									newCharacter.setAttribute("name", category);
									newCharacter.setAttribute("value",
											originalMod[0]);
									newStructure.addContent(newCharacter);
								}
							}
							Element newRelation = new Element("relation");
							newRelation.setAttribute("id", "r_" + id);
							newRelation.setAttribute("name", "part_of");
							newRelation.setAttribute("from", id + "_1");
							newRelation.setAttribute("to", id);
							statement.addContent(newStructure);
							statement.addContent(newRelation);
						}
					}

					File outfile = new File(outPath + "\\" + file);
					fout = new FileOutputStream(outfile);// FileWriter
					// if file doesnt exists, then create it
					if (!outfile.exists()) {
						outfile.createNewFile();
					}
					Comment comment = new Comment(
							"produced by FNAPostprocessArchitecture.java");					
					XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
					outputter.output(doc, fout);

				} catch (IOException | JDOMException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					if (fout != null) {
						try {
							fout.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

			}
		}
		ppa.closeDB();
	}

}
