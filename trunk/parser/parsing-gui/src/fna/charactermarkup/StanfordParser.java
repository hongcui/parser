 /* $Id$ */
/**
 * 
 */
package fna.charactermarkup;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.*;
import java.io.File;
import java.util.regex.*;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import fna.parsing.ApplicationUtilities;
import fna.parsing.Learn2Parse;
import fna.parsing.Registry;
import fna.parsing.TaxonNameCollector;
import fna.parsing.VolumeFinalizer;
import fna.parsing.state.SentenceOrganStateMarker;


/**
 * @author hongcui
 * updates April 2011
 * 	1. annual etc. => life_style
	2. added may_be_the_same relation for structures with the same name and constraint.
	3. to 60 m => marked as a range value with to_value="60" to_unit="m"
	4. separate two types of constraints for structure element (see below)
	5. removed empty values
	6. put <text> element as the first element in output
 */
@SuppressWarnings({ "unused","static-access" })
public class StanfordParser implements Learn2Parse, SyntacticParser{
	private static final Logger LOGGER = Logger.getLogger(StanfordParser.class);
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "root";
	//static protected String password = "root";
	static protected String password = "root";
	//protected int count = 0;
	static private int allchunks = 0;
	static private int discoveredchunks = 0;
	//private static Pattern numbergroup = Pattern.compile("(.*?)([()\\[\\]\\-\\–\\d\\.×x\\+°²½/¼\\*/%\\?]*?[½/¼\\d][()\\[\\]\\-\\–\\d\\.,?×x\\+°²½/¼\\*/%\\?]{1,}(?![a-z{}]))(.*)"); //added , and ? for chromosome counts, used {1, } to include single digit expressions such as [rarely 0]
	private static Pattern numbergroup = Pattern.compile("(.*?)([()\\[\\]\\-\\–\\d\\.×x\\+²½/¼\\*/%\\?]*?[½/¼\\d]?[()\\[\\]\\-\\–\\d\\.,?×x\\+²½/¼\\*/%\\?]{1,}(?![a-z{}]))(.*)"); //added , and ? for chromosome counts, used {1, } to include single digit expressions such as [rarely 0]
	private static boolean printNormalizeBrackets = false;

	private File posedfile = null;
	private File parsedfile = null;
	private String POSTaggedSentence = "POSedSentence";
	private POSTagger4StanfordParser tagger = null;
	private String tableprefix = null;
	private String glosstable = null;
	public static String lifestyle = "";
	public static String characters = "";
	//private SentenceOrganStateMarker sosm = null;
	//private Hashtable sentmapping = new Hashtable();

	private boolean finalize = false;
	//private boolean finalize = true;//set true when running config else set false.

	//private boolean debug = true;
	private boolean printSent = true;
	private boolean printProgress = false;
	private boolean evaluation = false;
	public static String newline = System.getProperty("line.separator");
	String output = Registry.TargetDirectory+"HumanReadable.txt";
	//String output = "C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\TreatisePartO\\target\\HumanReadable.txt";
	private File outfile = new File(output);

	/**
	 * 
	 */
	public StanfordParser(String posedfile, String parsedfile, String database, String tableprefix, String glosstable, boolean evaluation) {
		// TODO Auto-generated constructor stub
		if(outfile.exists()) outfile.delete();
		this.posedfile = new File(posedfile); 
		this.parsedfile = new File(parsedfile);
		this.database = database;
		this.tableprefix = tableprefix;
		this.glosstable = glosstable;
		this.evaluation = evaluation;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
			    String URL = "jdbc:mysql://localhost/"+database+"?user=termsuser&password=termspassword";
				//String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
			}
			Statement stmt = conn.createStatement();
			stmt.execute("create table if not exists "+this.tableprefix+"_"+this.POSTaggedSentence+"(source varchar(100) NOT NULL, posedsent TEXT, PRIMARY KEY(source))");
			stmt.execute("delete from "+this.tableprefix+"_"+this.POSTaggedSentence);	
			
			try{
				//collect life_style terms
				stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select distinct term from "+this.glosstable+ " where category='life_style'");
				while(rs.next()){
					this.lifestyle += rs.getString(1)+"|";
				}
				this.lifestyle = lifestyle.replaceFirst("\\|$", "");
				
				rs = stmt.executeQuery("select distinct term from "+this.glosstable+ " where category='character'");
				while(rs.next()){
					this.characters += rs.getString(1)+"|";
				}
				this.characters = characters.replaceFirst("\\|$", "");
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			}
			stmt.close();
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
		tagger = new POSTagger4StanfordParser(conn, this.tableprefix, glosstable);
	}
	
	public void POSTagging() throws Exception{
		try{
  			Statement stmt = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			FileOutputStream ostream = new FileOutputStream(posedfile); 
			PrintStream out = new PrintStream( ostream );
			
			//ResultSet rs = stmt.executeQuery("select * from newsegments");
			//stmt.execute("alter table markedsentence add rmarkedsent text");

			//ResultSet rs = stmt.executeQuery("select source, sentence from "+this.tableprefix+"_sentence order by sentid");// order by (source+0) ");////sort as numbers
			ResultSet rs = stmt.executeQuery("select source, markedsent from "+this.tableprefix+"_markedsentence order by sentid");// order by (source+0) ");////sort as numbers
			int count = 1;
			while(rs.next()){
				String src = rs.getString(1);
				String str = rs.getString(2);
				//TODO: may need to fix "_"
				//if(src.compareTo("232.txt-0")!=0) continue;
				try{
					str = tagger.POSTag(str, src);
				}catch(Exception e){
					StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
					LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
							System.getProperty("line.separator")+ "source:"+ src + System.getProperty("line.separator")+
							sw.toString());
				}
	       		stmt2.execute("insert into "+this.tableprefix+"_"+this.POSTaggedSentence+" values('"+rs.getString(1)+"','"+str+"')");
	       		out.println(str);
	       		count++;
	       		
			}
			stmt2.close();
			rs.close();
			out.close();
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			//throw e;
		}
	}

	/**
	 * direct the parsed result to the text file parsedfile
	 */
	public void parsing() throws Exception{
		PrintStream out = null;
		Pattern ptn = Pattern.compile("^Parsing \\[sent\\. (\\d+) len\\. \\d+\\]:");
	  	try{
	  		FileOutputStream ostream = new FileOutputStream(parsedfile); 
			out = new PrintStream( ostream );	
 	  		Runtime r = Runtime.getRuntime();
	  		//Process p = r.exec("cmd /c stanfordparser.bat");
 	  		//String cmdtext = "stanfordparser.bat >"+this.parsedfile+" 2<&1";
 	  		//String cmdtext = "cmd /c stanfordparser.bat";	
 	  		//String parserJarfilePath = ApplicationUtilities.getProperty("stanford.parser.jar"); 
	  		//String englishPCFGpath = ApplicationUtilities.getProperty("englishPCFG");
	  		String parserJarfilePath="lib\\stanford-parser.jar";
	  		String englishPCFGpath ="lib\\englishPCFG.ser.gz";
 	  		String cmdtext = "java -mx900m -cp "+parserJarfilePath+" edu.stanford.nlp.parser.lexparser.LexicalizedParser " +
	  				"-sentences newline -tokenized -tagSeparator / "+englishPCFGpath+" \""+
	  				this.posedfile+"\"";
 	  		System.out.println("parser path::"+cmdtext);
 	  		
	  		Process proc = r.exec(cmdtext);
          
		    ArrayList<String> headings = new ArrayList<String>();
	  	    ArrayList<String> trees = new ArrayList<String>();
	  
            // any error message?
            StreamGobbler errorGobbler = new 
                StreamGobbler(proc.getErrorStream(), "ERROR", headings, trees);            
            
            // any output?
            StreamGobbler outputGobbler = new 
                StreamGobbler(proc.getInputStream(), "OUTPUT", headings, trees);
                
            // kick them off
            errorGobbler.start();
            outputGobbler.start();
                                    
            // any error???
            int exitVal = proc.waitFor();
            System.out.println("ExitValue: " + exitVal);
            
           while(errorGobbler.isAlive() || outputGobbler.isAlive()){}

			//format
            if(headings.size() != trees.size()){
            	throw new Exception("Error reading Stanford parsing results. Parsing error. System terminates.");
            }
            StringBuffer sb = new StringBuffer();
            for(int i = 0; i<headings.size(); i++){
            	sb.append(headings.get(i)+System.getProperty("line.separator"));
            	sb.append(trees.get(i)+System.getProperty("line.separator"));
            }
            PrintWriter pw = new PrintWriter(new FileOutputStream(this.parsedfile));
            pw.print(sb.toString());
            pw.flush();
            pw.close();			
	  	}catch(Exception e){
	  		StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
	  		//throw e;
	  	}
	  	//out.close();
	}
	
	
	public void extracting() throws Exception{
    	try{
	        FileInputStream istream = new FileInputStream(this.parsedfile); 
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(istream));
			String line = "";
			String text = "";
			int i = 0;
			Statement stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery("select source, rmarkedsent from "+this.tableprefix+"_markedsentence order by sentid");//(source+0)"); //+0 so sort as numbers
			//ResultSet rs = stmt.executeQuery("select source, sentence from "+this.tableprefix+"_sentence order by sentid");//(source+0)"); //+0 so sort as numbers

			Pattern ptn = Pattern.compile("^Parsing \\[sent\\. (\\d+) len\\. \\d+\\]:(.*)");
			Matcher m = null;
			Tree2XML t2x = null;
			Document doc = null;
			CharacterAnnotatorChunked cac = new CharacterAnnotatorChunked(conn, this.tableprefix, glosstable, this.evaluation);
			SentenceChunker4StanfordParser ex = null;
			Element statement = null;
			ChunkedSentence cs = null;
			String pdescID ="";
			int order = 0;
			//int pfileindex = 0;
			String pfileindex = "";
			Element baseroot = null;
			Element description = new Element("description");
			while ((line = stdInput.readLine())!=null){
				if(line.startsWith("Loading") || line.startsWith("X:") || line.startsWith("Parsing file")|| line.startsWith("Parsed") ){continue;}
				if(line.trim().length()>1){
					m = ptn.matcher(line);
					if(m.matches()){
						i = Integer.parseInt((String)m.group(1));
					}else{
						text += line.replace(System.getProperty("line.separator"), ""); 
					}
				}else{
					if(text.startsWith("(ROOT")){//treatiseh
					text = text.replaceAll("(?<=[A-Z])\\$ ", "S ");
					text = text.replaceAll("&", "and");
					t2x = new Tree2XML(text);
					doc = t2x.xml();
					//Document doccp = (Document)doc.clone();
					String src = "";
					if(rs.relative(i)){
						try{ //if exception is thrown at any of the following steps, move on the next sentence.
						String sent = rs.getString(2);
						src = rs.getString(1);
						String thisdescID = src.replaceFirst("-\\d+$", "");//1.txtp436_1.txt-0's descriptionID is 1.txtp436_1.txt
						//int thisfileindex = Integer.parseInt(src.replaceFirst("\\.txt.*$", ""));
						String thisfileindex = src.replaceFirst("\\.txt.*$", "");
						if(finalize){
							if(baseroot ==null){
								order++;
								baseroot = VolumeFinalizer.getBaseRoot(thisfileindex, order);
							}
						}
						//sent = this.normalizeSpacesRoundNumbers(sent);
							if(!sent.matches(".*? [;\\.]\\s*$")){//at 30x. => at 30x. .
								sent = sent+" .";
							}
							sent = sent.replaceAll("<\\{?times\\}?>", "times");
							sent = sent.replaceAll("<\\{?diam\\}?>", "diam");
							sent = sent.replaceAll("<\\{?diams\\}?>", "diams");

							ex = new SentenceChunker4StanfordParser(i, doc, sent, src, this.tableprefix, conn, glosstable/*, SentenceOrganStateMarker.taxonnamepattern1, SentenceOrganStateMarker.taxonnamepattern2*/);

							cs = ex.chunkIt();
							//System.out.print("["+src+"]:");
							if(this.printSent){
								System.out.println();
								System.out.println(i+"["+src+"]: "+cs.toString());

							}
							
							if(finalize){
								if(thisdescID.compareTo(pdescID)!=0){
									if(description.getChildren().size()!=0){ //not empty
										//plug description in XML document
										//write the XML to final
										//call MainForm to display
										//VolumeFinalizer.replaceWithAnnotated(description, count, "/treatment/description", "FINAL", false);
										
										placeDescription(description, pdescID, baseroot);
										cac.reset();
										description = new Element("description");
										if(this.printProgress){
											System.out.println(pfileindex+".xml written");
										}
									}
								}
								
								//if(thisfileindex != pfileindex){
								if(thisfileindex.compareTo(pfileindex) !=0){
									//if(pfileindex !=0){
									if(pfileindex.length() !=0){
										order++;
										VolumeFinalizer.outputFinalXML(baseroot, pfileindex, "FINAL");
										baseroot = VolumeFinalizer.getBaseRoot(thisfileindex, order);
									}								
								}
							}
							//if(statement!=null) description.addContent(statement);
							statement = cac.annotate(src, src, cs); //src: 100.txt-18

							//print a human readable file 
							//String newline = System.getProperty("line.separator");
							//String humanreadable = i+"["+src+"]: "+cs.toString()+newline;
							String humanreadable = getHumanReadableText(statement)+newline+newline;
							  try{
								  // Create file 
								  FileWriter fstream = new FileWriter(outfile, true);
								  BufferedWriter out = new BufferedWriter(fstream);
								  out.write(humanreadable);
								  //Close the output stream
								  out.close();
							}catch (Exception e){//Catch exception if any
								StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
							}

							if(statement!=null) description.addContent(statement);

							pdescID = thisdescID;
							pfileindex = thisfileindex;
						}catch(Exception e){
							StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
							LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
									System.getProperty("line.separator")+ "source:"+ src + System.getProperty("line.separator")+
									sw.toString());
						}
						rs.relative(i*-1); //reset the pointer
						}
					}
					text = "";
					i = 0;
				}
			}//end while
			if(finalize){ //output the last sentence
				placeDescription(description, pdescID, baseroot);
				VolumeFinalizer.outputFinalXML(baseroot, pfileindex, "FINAL");		
			}
			rs.close();
    	}catch (Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			//throw e;
        }
    	//if(finalize) VolumeFinalizer.copyFilesWithoutDescriptions2FinalFolder();
    }


	/**
	 * turn statement in xml format to a text format
	 */
	@SuppressWarnings("unchecked")
	private String getHumanReadableText(Element statement) {
		
		List<Element> structures = statement.getChildren("structure");
		Hashtable<String, Element> id2elements = new Hashtable<String, Element>();
		StringBuffer sb = new StringBuffer();
		sb.append(statement.getAttributeValue("id")+":"+statement.getChildText("text")+newline);
		StringBuffer rsb = new StringBuffer();
		for(Element structure : structures){
			String structurename = (structure.getAttribute("constraint")!=null? structure.getAttributeValue("constraint")+" " : "")+structure.getAttributeValue("name");
			sb.append(newline+structurename+":"+newline);
			List<Element> characters = structure.getChildren("character");
			for(Element character : characters){
				sb.append("\t");
				List<Attribute> attributes = character.getAttributes();
				for(Attribute attribute: attributes){
					if(attribute.getName().matches("name")){
						sb.append(attribute.getValue()+":");
					}else if(attribute.getName().matches("value")){
						sb.append(attribute.getValue()+"; ");
					}else if(attribute.getName().compareTo("constraintid")==0){//do nothing
					}else{
						sb.append(attribute.getName()+":"+attribute.getValue()+"; ");
					}
				}
				sb.append(newline);
			}
			String structid = structure.getAttributeValue("id");
			try{
				XPath relationid = XPath.newInstance(".//relation[@from='"+structid+"']");
				List<Element> relations = relationid.selectNodes(statement);
				for(Element relation : relations){
					sb.append("\t");
					sb.append((relation.getAttribute("negation")!=null && relation.getAttributeValue("negation").compareTo("false")==0? "" : "not "));
					sb.append(relation.getAttributeValue("name")+":");
					String toid = relation.getAttributeValue("to");
					String toname = ((Element)XPath.selectSingleNode(statement, ".//structure[@id='"+toid+"']")).getAttributeValue("name");
					sb.append(toname);					
				}
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
						System.getProperty("line.separator")+sw.toString());
			}
		}
		return sb.toString();
	}

	public static String normalizeSpacesRoundNumbers(String sent) {
		sent = ratio2number(sent);//bhl
		sent = sent.replaceAll("(?<=\\d)\\s*/\\s*(?=\\d)", "/");
		sent = sent.replaceAll("(?<=\\d)\\s+(?=\\d)", "-"); //bhl: two numbers connected by a space
		sent = sent.replaceAll("<?\\{?\\btwice\\b\\}?>?", "2 times");
		sent = sent.replaceAll("<?\\{?\\bthrice\\b\\}?>?", "3 times");
		sent = sent.replaceAll("2\\s*n\\s*=", "2n=");
		sent = sent.replaceAll("2\\s*x\\s*=", "2x=");
		sent = sent.replaceAll("n\\s*=", "n=");
		sent = sent.replaceAll("x\\s*=", "x=");

		//sent = sent.replaceAll("[–—-]", "-").replaceAll(",", " , ").replaceAll(";", " ; ").replaceAll(":", " : ").replaceAll("\\.", " . ").replaceAll("\\[", " [ ").replaceAll("\\]", " ] ").replaceAll("\\(", " ( ").replaceAll("\\)", " ) ").replaceAll("\\s+", " ").trim();
		sent = sent.replaceAll("[~–—-]", "-").replaceAll("°", " ° ").replaceAll(",", " , ").replaceAll(";", " ; ").replaceAll(":", " : ").replaceAll("\\.", " . ").replaceAll("\\s+", " ").trim();
		sent = sent.replaceAll("(?<=\\d) (?=\\?)", ""); //deals especially x=[9 ? , 13] 12, 19 cases
		sent = sent.replaceAll("(?<=\\?) (?=,)", "");
		if(sent.matches(".*?[nx]=.*")){
			sent = sent.replaceAll("(?<=[\\d?])\\s*,\\s*(?=\\d)", ","); //remove spaces around , for chromosome only so numericalHandler.numericalPattern can "3" them into one 3. Other "," connecting two numbers needs spaces to avoid being "3"-ed (fruits 10, 3 of them large) 
		}
		sent = sent.replaceAll("\\b(?<=\\d+) \\. (?=\\d+)\\b", ".");//2 . 5 => 2.5
		sent = sent.replaceAll("(?<=\\d)\\.(?=\\d[nx]=)", " . "); //pappi 0.2n=12
		
		
		//sent = sent.replaceAll("(?<=\\d)\\s+/\\s+(?=\\d)", "/"); // 1 / 2 => 1/2
		//sent = sent.replaceAll("(?<=[\\d()\\[\\]])\\s+[–—-]\\s+(?=[\\d()\\[\\]])", "-"); // 1 - 2 => 1-2
		//sent = sent.replaceAll("(?<=[\\d])\\s+[–—-]\\s+(?=[\\d])", "-"); // 1 - 2 => 1-2
		
		//4-25 [ -60 ] => 4-25[-60]: this works only because "(text)" have already been removed from sentence in perl program
		sent = sent.replaceAll("\\(\\s+(?=[\\d\\+\\-%])", "("). //"( 4" => "(4"
		replaceAll("(?<=[\\d\\+\\-%])\\s+\\((?!\\s?[{<a-zA-Z])", "("). //" 4 (" => "4("
		replaceAll("(?<![a-zA-Z}>]\\s?)\\)\\s+(?=[\\d\\+\\-%])", ")"). //") 4" => ")4"
		replaceAll("(?<=[\\d\\+\\-%])\\s+\\)", ")"). //"4 )" => "4)"
		replaceAll("\\((?=\\d+-\\{)", "( "); //except for ( 4-{angled} )
		
		sent = sent.replaceAll("\\[\\s+(?=[\\d\\+\\-%])", "["). //"[ 4" => "[4", not [ -subpalmately ]
		replaceAll("(?<=[\\d\\+\\-%])\\s+\\[(?!\\s?[{<a-zA-Z])", "["). //" 4 [" => "4["
		replaceAll("(?<![a-zA-Z}>]\\s?)\\]\\s+(?=[\\d\\+\\-%])", "]"). //"] 4" => "]4"
		replaceAll("(?<=[\\d\\+\\-%])\\s+\\]", "]"). //"4 ]" => "4]"
		replaceAll("\\[(?=\\d+-\\{)", "[ "); //except for [ 4-{angled} ]
		
		/*Pattern p = Pattern.compile("(.*?)(\\d*)\\s+\\[\\s+([ –—+\\d\\.,?×/-]+)\\s+\\]\\s+(\\d*)(.*)");  //4-25 [ -60 ] => 4-25[-60]. ? is for chromosome count
		Matcher m = p.matcher(sent);
		while(m.matches()){
			sent = m.group(1)+ (m.group(2).length()>0? m.group(2):" ")+"["+m.group(3).replaceAll("\\s*[–—-]\\s*", "-")+"]"+(m.group(4).length()>0? m.group(4):" ")+m.group(5);
			m = p.matcher(sent);
		}
		////keep the space after the first (, so ( 3-15 mm) will not become 3-15mm ) in POSTagger.
		p = Pattern.compile("(.*?)(\\d*)\\s+\\(\\s+([ –—+\\d\\.,?×/-]+)\\s+\\)\\s+(\\d*)(.*)");  //4-25 ( -60 ) => 4-25(-60)
		//p = Pattern.compile("(.*?)(\\d*)\\s*\\(\\s*([ –—+\\d\\.,?×/-]+)\\s*\\)\\s*(\\d*)(.*)");  //4-25 ( -60 ) => 4-25(-60)
		m = p.matcher(sent);
		while(m.matches()){
			sent = m.group(1)+ (m.group(2).length()>0? m.group(2):" ")+"("+m.group(3).replaceAll("\\s*[–—-]\\s*", "-")+")"+(m.group(4).length()>0? m.group(4):" ")+m.group(5);
			m = p.matcher(sent);
		}*/
		
		sent = sent.replaceAll("\\s+/\\s+", "/"); //and/or 1/2
		sent = sent.replaceAll("\\s+×\\s+", "×");
		sent = sent.replaceAll("\\s*\\+\\s*", "+"); // 1 + => 1+
		sent = sent.replaceAll("(?<![\\d()\\]\\[×-])\\+", " +");
		sent = sent.replaceAll("\\+(?![\\d()\\]\\[×-])", "+ ");
		sent = sent.replaceAll("(?<=(\\d))\\s*\\?\\s*(?=[\\d)\\]])", "?"); // (0? )
		sent = sent.replaceAll("\\s*-\\s*", "-"); // 1 - 2 => 1-2, 4 - {merous} => 4-{merous}
		sent = sent.replaceAll("(?<=[\\d\\+-][\\)\\]])\\s+(?=[\\(\\[][\\d-])", "");//2(–3) [–6]  ??
		//%,°, and ×
		sent = sent.replaceAll("(?<![a-z])\\s+%", "%").replaceAll("(?<![a-z])\\s+°", "°").replaceAll("(?<![a-z ])\\s*×\\s*(?![ a-z])", "×");
		/*if(sent.indexOf(" -{")>=0){//1–2-{pinnately} or -{palmately} {lobed} => {1–2-pinnately-or-palmately} {lobed}
			sent = sent.replaceAll("\\s+or\\s+-\\{", "-or-").replaceAll("\\s+to\\s+-\\{", "-to-").replaceAll("\\s+-\\{", "-{");
		}*/
		//mohan code 11/9/2011 to replace (?) by nothing
		sent = sent.replaceAll("\\(\\s*\\?\\s*\\)","");
		//end mohan code
	
		//make sure brackets that are not part of a numerical expression are separated from the expression by a space
		if(sent.contains("(") || sent.contains(")")) sent = normalizeBrackets(sent, '(');
		if(sent.contains("[") || sent.contains("]")) sent = normalizeBrackets(sent, '[');
		
		sent = sent.replaceAll("\\[(?=-[a-z])", "[ ");//[-subpalmately ] => [ -subpalmately ]
		sent = sent.replaceAll("\\((?=-[a-z])", "( ");//[-subpalmately ] => [ -subpalmately ]
		return sent;
	}

	private static String normalizeBrackets(String sent, char bracket) {
		char l ='('; char r=')';
		switch (bracket){
			case '(': l = '('; r=')'; break;
			case '[': l = '['; r=']'; break;
		}
		//boolean changed = false;
		String sentorig = sent;
		String fixed = "";
		Matcher matcher = numbergroup.matcher(sent);
		while(matcher.matches()){
			String num = matcher.group(2);
			if(Utilities.hasUnmatchedBracket(num, ""+l, ""+r)>0){ //has an extra (
				int index = Utilities.indexOfunmatched(l, num);
				if(index==0) {//move ( to group(2)
					fixed += matcher.group(1)+l+" "+num.replaceFirst("\\"+l, "");
					sent = matcher.group(3);
				}else if(index == num.length()-1){ //move ( to group(3)
					fixed += matcher.group(1)+num.replaceFirst("\\"+l+"$", "");
					sent = " "+l+matcher.group(3);
				}else{//the extra ( is in the middle of the num expression, then either find the matching ) in group 3 or split the num at the (
					if(matcher.group(3).startsWith(" "+r)){//find the matching ), attach it to group(2)
						fixed += matcher.group(1)+matcher.group(2)+r;
						sent = matcher.group(3).replaceFirst("\\s*\\"+r, "");
					}else{//move text from the extra ( on to group(3)
						fixed += matcher.group(1)+matcher.group(2).substring(0, index);
						sent =" "+l+" "+matcher.group(2).substring(index+1)+matcher.group(3);
					}					
				}
			}else if(Utilities.hasUnmatchedBracket(num, ""+l, ""+r)<0){ //has an extra )
				int index = Utilities.indexOfunmatched(r, num);
				if(index==0) {//move ) to group(1)
					fixed += matcher.group(1)+r+" "+num.replaceFirst("\\"+r, "");
					sent = matcher.group(3);
				}else if(index == num.length()-1){ //move ) to group(3)
					fixed += matcher.group(1)+num.replaceFirst("\\"+r+"$", "");
					sent = " "+r+matcher.group(3);
				}else{//the extra ) is in the middle of the num expression, then either find the matching ( in group(1) or split the num at the )
					if(matcher.group(1).endsWith(l+" ")){//find the matching (, attach it to group(2)
						fixed += matcher.group(1).replaceFirst("\\"+l+"\\s*$", "")+"("+matcher.group(2);
						sent = matcher.group(3);
					}else{//move text from the extra ) on to group(1)
						fixed += matcher.group(1)+matcher.group(2).substring(0, index-1)+" "+r+" "+ matcher.group(2).substring(index+1);
						sent = matcher.group(3);
					}					
				}
			}else{
				fixed += matcher.group(1)+matcher.group(2);
				sent = matcher.group(3);
			}
			matcher = numbergroup.matcher(sent);
		}
		fixed +=sent;
		if(printNormalizeBrackets  && !fixed.equals(sentorig)){
			System.out.println("orig : "+sentorig);
			System.out.println("fixed: "+fixed);
		}
		return fixed.replaceAll("\\s+", " ");
	}
	


	public static String ratio2number(String sent){
		String small = "\\b(?:one|two|three|four|five|six|seven|eight|nine)";
		String big = "(?:half|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth)s?\\b";
		//ratio
		Pattern ptn = Pattern.compile("(.*?)("+small+"\\s*-?_?\\s*"+big+")(.*)");
		Matcher m = ptn.matcher(sent);
		while(m.matches()){
			String ratio = m.group(2);
			ratio = toRatio(ratio.replaceAll("_", "-"));
			sent = m.group(1)+ratio+m.group(3);
			m = ptn.matcher(sent);
		}
		//number
		small = "\\b(?:two|three|four|five|six|seven|eight|nine)\\b";
		ptn = Pattern.compile("(.*?)("+small+")(.*)");
		m = ptn.matcher(sent);
		while(m.matches()){
			String number = m.group(2);
			number = toNumber(number);
			sent = m.group(1)+number+m.group(3);
			m = ptn.matcher(sent);
		}
		sent = sent.replaceAll("(?<=\\d)\\s*to\\s*(?=\\d)", "-");
		return sent;
	}

	public static String toNumber(String ratio){
		ratio = ratio.replaceAll("\\btwo\\b", "2");
		ratio = ratio.replaceAll("\\bthree\\b", "3");
		ratio = ratio.replaceAll("\\bfour\\b", "4");
		ratio = ratio.replaceAll("\\bfive\\b", "5");
		ratio = ratio.replaceAll("\\bsix\\b", "6");
		ratio = ratio.replaceAll("\\bseven\\b", "7");
		ratio = ratio.replaceAll("\\beight\\b", "8");
		ratio = ratio.replaceAll("\\bnine\\b", "9");
		return ratio;
	}
	
	public static String toRatio(String ratio){
		ratio = ratio.replaceAll("\\bone\\b", "1/");
		ratio = ratio.replaceAll("\\btwo\\b", "2/");
		ratio = ratio.replaceAll("\\bthree\\b", "3/");
		ratio = ratio.replaceAll("\\bfour\\b", "4/");
		ratio = ratio.replaceAll("\\bfive\\b", "5/");
		ratio = ratio.replaceAll("\\bsix\\b", "6/");
		ratio = ratio.replaceAll("\\bseven\\b", "7/");
		ratio = ratio.replaceAll("\\beight\\b", "8/");
		ratio = ratio.replaceAll("\\bnine\\b", "9/");
		ratio = ratio.replaceAll("\\bhalf\\b", "2");
		ratio = ratio.replaceAll("\\bthirds?\\b", "3");
		ratio = ratio.replaceAll("\\bfourths?\\b", "4");
		ratio = ratio.replaceAll("\\bfifths?\\b", "5");
		ratio = ratio.replaceAll("\\bsixthths?\\b", "6");
		ratio = ratio.replaceAll("\\bsevenths?\\b", "7");
		ratio = ratio.replaceAll("\\beighths?\\b", "8");
		ratio = ratio.replaceAll("\\bninths?\\b", "9");
		ratio = ratio.replaceAll("\\btenths?\\b", "10");
		ratio = ratio.replaceAll("-", "").replaceAll("\\s", "");
		return ratio;
	}

	/**
	 * depending on the type of descriptionID, call different replaceWithAnnotated methods in VolumeFinalizer
	 * @param description
	 * @param dID: may be 1.txt or 1.txtp436_1.txt
	 */
	
	private void placeDescription(Element description, String dID, Element baseroot) {
		/*validate description, incomplete
		SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", true);
		builder.setFeature("http://apache.org/xml/features/validation/schema", true);
		builder.setProperty(
				  "http://apache.org/xml/properties/schema/external-schemaLocation",
				  "http://biosemantics.googlecode.com/svn/trunk/characterStatements/ characterAnnotationSchema.xsd");
		builder.build(description);*/
		
		if(dID.indexOf(".txtp")>=0){
			String pid = dID.replaceFirst("\\.txt$", "");
			VolumeFinalizer.replaceWithAnnotated(description, baseroot, ".//description[@pid=\""+pid+"\"]");			
		}else{
			VolumeFinalizer.replaceWithAnnotated(description, baseroot, ".//description");
		}
		
	}
	
	
	public ArrayList<String> getMarkedDescription(String source){
		return null;
	}
	
	public static void countChunks(int all, int discovered){
		StanfordParser.allchunks += all;
		StanfordParser.discoveredchunks += discovered;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try{
		//String text=", ( 2 – ) 2 . 5 – 3 . 5 ( – 4 ) × ( 1 . 5 – ) 2 – 3 ( – 4 ) {cm} ";
		//String text="blades ± obovate , [ 0 ] ( 1 – ) 2 – 3 - pinnately lobed , ultimate margins dentate , ";
		//String text="cypselae 9 – 18 mm , bodies terete or narrowly conic to obconic , 5 – 9 mm , beaks 3 – 10 mm , lengths ( 1 / 2 – ) 2 times bodies ;";
		//String text="2 n = 24 , 40 , [ 16 , 32 ] .";
		//String text = "x= 9 ( 18 ? ) .";
		//String text = "2 n = 24 , 40 , [ 16 , 32 ] .";
		//String text = "blades broadly elliptic or ovate to lanceolate , 6 – 12 ( – 18 + ) cm × 30 – 80 ( – 120 + ) mm , both faces sparsely pilose to hirsute .";
		//String text = "blades either linear to lanceolate and not lobed , 10 – 20 ( – 38 ) cm × 6 – 10 mm , or oblanceolate to oblong and pinnately lobed , 10 – 20 cm × 25 – 50 mm , or both ;";
		//String text = " often 2 - , 3 - , or 5 - ribbed ;";
		//<involucres> {shape~list~ovoid~to~broadly~cylindric~or~campanulate} , (2-)2.5-3.5(-4)×(1.5-)2-3(-4) {cm} , {thinly} {arachnoid} .
		//String text = "<branchlets> {slender} , 4-{sided} , {separated} from {one} another , 0 . 8 - 1 mm . {thick} , the ultimate ones 1 . 5 - 2 {cm} . {long} ;";
		//String text = "laminae 6 17 cm . long , 2 - 7 cm . broad , lanceolate to narrowly oblong or elliptic_oblong , abruptly and narrowly acuminate , obtuse to acute at the base , margin entire , the lamina drying stiffly chartaceous to subcoriaceous , smooth on both surfaces , essentially glabrous and the midvein prominent above , glabrous to sparsely puberulent beneath , the 8 to 18 pairs of major secondary veins prominent beneath and usually loop_connected near the margin , microscopic globose_capitate or oblongoid_capitate hairs usually present on the lower surface , clear or orange distally .";
		//String text = "<inflorescences> {terminal} and {axillary} , {terminal} usually occupying {distal} 1 / 5 – 1 / 3 of <stem> , rather {lax} , {interrupted} in {proximal} 1 / 2 , or almost to top , usually narrowly {paniculate} .";
		/*String text = "<petals> {lavender} or {white} , {often} {spatulate} , sometimes {oblanceolate} or {obovate} , 6 – 9 ( – 10 ) × ( 1 . 5 – ) 2 – 3 ( – 3 . 5 ) mm , <margins> not {crisped} , <claw> strongly {differentiated} from <blade> , ( {slender} , 2 – 3 . 5 ( – 4 ) mm , {narrowest} at <base> ) ;";
		text = StanfordParser.normalizeSpacesRoundNumbers(text);
		String str = Utilities.threeingSentence(text);
		String p1 ="\\([^()]*?[a-zA-Z][^()]*?\\)";
  		String p2 = "\\[[^\\]\\[]*?[a-zA-Z][^\\]\\[]*?\\]";
  		//String p3 = "\\{[^{}]*?[a-zA-Z][^{}]*?\\}";				
		if(str.matches(".*?"+p1+".*") || str.matches(".*?"+p2+".*")){ 
			str = Utilities.threeingSentence(str);
			str = str.replaceAll(p1, "").replaceAll(p2, "").replaceAll("\\s+", " ").trim();					
		}*/
		//String text = "ovary more than two to three-fourths to one half superior. ";
		//System.out.println(StanfordParser.ratio2number(text));
		//String text="<pollen> 70-100% 3-{porate} , {mean} 25 um .";
		//String text="x = [ 9 ? , 13 , 15 ] 17 , 18 , 19 .";
		//String text="<stamens> 2 ( – 3 ) [ – 6 ] , {exserted} ";
		//String text="<pappi> {persistent} , of {many} <scales> in {several} <series> , {distinct} , {narrow} [ rarely 0 ] .";
		//String text = "<blades> {obovate} to {oblanceolate} or {spatulate} , ( 1 – ) 2 ( – 3 ) -{pinnately} [ -subpalmately ] {lobed} ( {primary} <lobes> often pectinately {divided} ) , {ultimate} <margins> {serrate} or {entire} , <faces> ± {villous} to {sericeous} [ {glabrescent} , {glabrate} ] .";
		//String text = " with ( 1 or ) 2 or more <teeth> , <apex> {acute} or {acutish} , or sometimes {rounded} , <faces> not {appendaged} , only <apex> {foliaceous} .";
		//System.out.println(StanfordParser.normalizeSpacesRoundNumbers(text));
		
		

		//*fna
		//String database = "annotationevaluation";
		//String posedfile = "FNAv19posedsentences.txt";
		//String parsedfile = "FNAv19parsedsentences.txt";

		//StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, "fnav19", "fnaglossaryfixed", false);

		
		//*treatiseh
		//String posedfile = "Treatisehposedsentences.txt";
		//String parsedfile = "Treatisehparsedsentences.txt";
		//StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, "treatiseh", "wordpos4parser", "treatisehglossaryfixed");
		
		
		/*String database = "markedupdatasets";
		String posedfile = "C:\\Users\\mohankrishna89\\Desktop\\Ant Work\\Plazi_21207_gg2_tx\\target\\plazi_21207_gg2_tx_parsedsentences.txt";
		String parsedfile = "C:\\Users\\mohankrishna89\\Desktop\\Ant Work\\Plazi_21207_gg2_tx\\target\\plazi_21207_gg2_tx_posedsentences.txt";
		StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, "plazi_21207_gg2_tx", "antglossaryfixed", false);*/
		
		

		//String posedfile = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\v5\\target\\fnav5_posedsentences.txt";
		//String parsedfile ="C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\v5\\target\\fnav5_parsedsentences.txt";

			
		//String posedfile = "C:\\temp\\DEMO\\demo-folders\\taxonX-ants_description\\target\\taxon_ants_posedsentences.txt";
		//String parsedfile="C:\\temp\\DEMO\\demo-folders\\taxonX-ants_description\\target\\taxon_ants_parsedsentences.txt";
		//String posedfile="C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\plaziantfirst\\target\\plazi_ant_first_posedsentences.txt";
		//String parsedfile="C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\plaziantfirst\\target\\plazi_ant_first_parsedsentences.txt";
		//String posedfile="C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\phenoscape-fish-source\\target\\pheno_fish_posedsentences.txt";
		//String parsedfile="C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\phenoscape-fish-source\\target\\pheno_fish_parsedsentences.txt";
		


		String database = "markedupdatasets";
		String posedfile = "E:\\Data\\Diatom\\target\\donat_test_posedsentences.txt";
		String parsedfile = "E:\\Data\\Diatom\\target\\donat_test_parsedsentences.txt";
		String transformedir = "E:\\Data\\Diatom\\transformed";
		String prefix = "diatom_test";
		/*String posedfile = "C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\DonatAnts\\target\\donat_test_posedsentences.txt";
		String parsedfile = "C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\DonatAnts\\target\\donat_test_parsedsentences.txt";
		String transformedir = "C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\DonatAnts\\transformed";
		String prefix = "donat_test";*/

		/*String posedfile = "C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\TreatisePartO\\target\\treatise_o_test_posedsentences.txt";
		String parsedfile = "C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\TreatisePartO\\target\\treatise_o_test_parsedsentences.txt";
		String transformedir = "C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\TreatisePartO\\transformed";
		String prefix = "treatise_o_test";*/
		
		/*String posedfile = "C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\FNAv7Limnanthaceae\\target\\fnav7_test_posedsentences.txt";
		String parsedfile = "C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\FNAv7Limnanthaceae\\target\\fnav7_test_parsedsentences.txt";
		String transformedir = "C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\FNAv7Limnanthaceae\\transformed";
		String prefix = "fnav7_test";*/
		
		/*String posedfile = "C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\FNAv5Caryophyllaceae\\target\\fnav5_test_posedsentences.txt";
		String parsedfile = "C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\FNAv5Caryophyllaceae\\target\\fnav5_test_parsedsentences.txt";
		String transformedir = "C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\FNAv5Caryophyllaceae\\transformed";
		String prefix = "fnav5_test";*/
		
		/*String posedfile = "C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\FNAv8Ericaceae\\target\\fnav8_test_posedsentences.txt";
		String parsedfile = "C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\FNAv8Ericaceae\\target\\fnav8_test_parsedsentences.txt";
		String transformedir = "C:\\Documents and Settings\\Hong Updates\\Desktop\\2012BiosemanticsWorkshopTest\\FNAv8Ericaceae\\transformed";
		String prefix = "fnav8_test";*/
		
		/*String posedfile = "C:\\temp\\DEMO\\demo-folders\\FNA-v19-excerpt\\target\\fnav19_excerpt_posedsentences.txt";
		String parsedfile = "C:\\temp\\DEMO\\demo-folders\\FNA-v19-excerpt\\target\\fnav19_excerpt_parsedsentences.txt";
		String transformeddir = "C:\\temp\\DEMO\\demo-folders\\FNA-v19-excerpt\\target\\transformed";
		String prefix = "fnav19_excerpt";*/
		/*String posedfile = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\fnav2\\target\\fnav2_posedsentences.txt";
		String parsedfile ="C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\fnav2\\target\\fnav2_parsedsentences.txt";
		String prefix = "fnav2"; //should be volume name
		String transformeddir = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\fnav2\\target\\transformed";
		 */
		

		/*String posedfile = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\V4\\target\\fnav4n_posedsentences.txt";
		String parsedfile ="C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\V4\\target\\fnav4n_parsedsentences.txt";
		String prefix = "fnav4n"; //should be volume name
		String transformeddir = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\V4\\target\\transformed";
		*/
		


		StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, prefix, "diatomglossaryfixed", false);
		//StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, prefix, "fnaglossaryfixed", false);
		//StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, prefix, "antglossaryfixed", false);


		//sp.POSTagging();
		//sp.parsing();
		sp.extracting();
		//System.out.println("total chunks: "+StanfordParser.allchunks);
		//System.out.println("discovered chunks: "+StanfordParser.discoveredchunks);
		}catch (Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}
}
