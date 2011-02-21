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

import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.xpath.XPath;

import fna.parsing.Learn2Parse;
import fna.parsing.VolumeFinalizer;
import fna.parsing.state.SentenceOrganStateMarker;


/**
 * @author hongcui
 *
 */
public class StanfordParser implements Learn2Parse, SyntacticParser{
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "root";
	static protected String password = "root";
	protected int count = 0;
	private File posedfile = null;
	private File parsedfile = null;
	private String POSTaggedSentence = "POSedSentence";
	private POSTagger4StanfordParser tagger = null;
	private String tableprefix = null;
	private String postable = null;
	private String glosstable = null;
	//private SentenceOrganStateMarker sosm = null;
	//private Hashtable sentmapping = new Hashtable();
	private boolean finalize = false;
	private boolean debug = true;
	/**
	 * 
	 */
	public StanfordParser(String posedfile, String parsedfile, String database, String tableprefix, String postable, String glosstable) {
		// TODO Auto-generated constructor stub
		this.posedfile = new File(posedfile); 
		this.parsedfile = new File(parsedfile);
		this.database = database;
		this.tableprefix = tableprefix;
		this.postable = postable;
		this.glosstable = glosstable;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists "+this.tableprefix+"_"+this.POSTaggedSentence+"(source varchar(100) NOT NULL, posedsent TEXT, PRIMARY KEY(source))");
				stmt.execute("delete from "+this.tableprefix+"_"+this.POSTaggedSentence);
				stmt.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		tagger = new POSTagger4StanfordParser(conn, this.tableprefix, postable, glosstable);
	}
	
	public void POSTagging(){
		try{
			Statement stmt = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			FileOutputStream ostream = new FileOutputStream(posedfile); 
			PrintStream out = new PrintStream( ostream );
			
			//ResultSet rs = stmt.executeQuery("select * from newsegments");
			//stmt.execute("alter table markedsentence add rmarkedsent text");
			ResultSet rs = stmt.executeQuery("select source, markedsent from "+this.tableprefix+"_markedsentence order by (source+0) ");////sort as numbers
			//ResultSet rs = stmt.executeQuery("select * from markedsentence order by source ");
			int count = 1;
			while(rs.next()){
				//str=rs.getString(3);
				String src = rs.getString("source");
				String str = rs.getString("markedsent");
				//TODO: may need to fix "_"
				str = tagger.POSTag(str, src);
	       		//stmt2.execute("insert into marked_pos values('"+rs.getString(1)+"','"+rs.getString(2)+"','"+str+"')");
	       		stmt2.execute("insert into "+this.tableprefix+"_"+this.POSTaggedSentence+" values('"+rs.getString(1)+"','"+str+"')");
	       		//System.out.println(str);
	       		out.println(str);
	       		count++;
	       		//sentmapping.put(""+count, src);
			}
			stmt2.close();
			rs.close();
			out.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * direct the parsed result to text file parsedfile
	 * 
	 * this does not work. Work only from commandline. 
	 */
	public void parsing(){
		PrintStream out = null;
		Pattern ptn = Pattern.compile("^Parsing \\[sent\\. (\\d+) len\\. \\d+\\]:");
	  	try{
	  		FileOutputStream ostream = new FileOutputStream(parsedfile); 
			out = new PrintStream( ostream );	
 	  		Runtime r = Runtime.getRuntime();
	  		//Process p = r.exec("cmd /c stanfordparser.bat");
 	  		//String cmdtext = "stanfordparser.bat >"+this.parsedfile+" 2<&1";
 	  		//String cmdtext = "cmd /c stanfordparser.bat";	  		
	  		String cmdtext = "java -mx900m -cp \"C:/stanford-parser-2010-08-20/stanford-parser.jar;\" edu.stanford.nlp.parser.lexparser.LexicalizedParser " +
	  				"-sentences newline -tokenized -tagSeparator / C:/stanford-parser-2010-08-20/englishPCFG.ser.gz  "+
	  				this.posedfile;
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

			//format
            if(headings.size()+1 != trees.size()+1){
            	System.err.println("Error reading parsing results");
            	System.exit(2);
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
	  		e.printStackTrace();
	  	}
	  	//out.close();
	}
	
	
	public void extracting(){
    	try{
	       //String test="(ROOT  (S (NP      (NP (NN body) (NN ovoid))      (, ,)      (NP        (NP (CD 2-4))        (PP (IN x)          (NP            (NP (CD 1-1.5) (NN mm))            (, ,)            (ADJP (RB not) (JJ winged)))))      (, ,))    (VP (VBZ woolly))    (. .)))";
	       // test="(ROOT  (NP    (NP      (NP (NNP Ray))      (ADJP (JJ laminae)        (NP (CD 6))))    (: -)    (NP      (NP        (NP (CD 7) (NNS x))        (NP (CD 2/CD-32) (NN mm)))      (, ,)      (PP (IN with)        (NP (CD 2))))    (: -)    (NP      (NP (CD 5) (NNS hairs))      (PP (IN inside)        (NP          (NP (NN opening))          (PP (IN of)            (NP (NN tube))))))    (. .)))";
	       // test="(S (NP (NP (NN margins) (UCP (NP (JJ entire)) (, ,) (ADJP (JJ dentate)) (, ,) (ADJP (RB pinnately) (JJ lobed)) (, ,) (CC or) (NP (JJ pinnatifid) (NN pinnately))) (NN compound)) (, ,) (NP (JJ spiny)) (, ,)) (VP (JJ tipped) (PP (IN with) (NP (NNS tendrils)))) (. .))";
	        FileInputStream istream = new FileInputStream(this.parsedfile); 
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(istream));
			String line = "";
			String text = "";
			int i = 0;
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select source, rmarkedsent from "+this.tableprefix+"_markedsentence order by (source+0)"); //+0 so sort as numbers
			//ResultSet rs = stmt.executeQuery("select source, markedsent from markedsentence order by source");
			Pattern ptn = Pattern.compile("^Parsing \\[sent\\. (\\d+) len\\. \\d+\\]:(.*)");
			Matcher m = null;
			Tree2XML t2x = null;
			Document doc = null;
			CharacterAnnotatorChunked cac = null;
			String pdescID ="";
			int order = 0+0;
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
					//if(i != 359 && i !=484 && i!=517 && i!=549 && i != 1264 && i!=1515 && i!=1613 && i !=1782 && i !=2501 && i !=2793 && i!=4798 && i!=9243 && i!=10993 && i!=12449 && text.startsWith("(ROOT")){
					//if(i !=2793 && text.startsWith("(ROOT")){//FNAv19
					//if(i != 1156 && text.startsWith("(ROOT")){//treatiseh
					if(/*i != 2468 && i != 3237 &&i != 9555 && i != 9504 &&*/ i !=4061 && text.startsWith("(ROOT")){//bhl	
					text = text.replaceAll("(?<=[A-Z])\\$ ", "S ");
					t2x = new Tree2XML(text);
					doc = t2x.xml();
					//Document doccp = (Document)doc.clone();
					if(rs.relative(i)){
						String sent = rs.getString("rmarkedsent");
						String src = rs.getString("source");
						String thisdescID = src.replaceFirst("-\\d+$", "");//1.txtp436_1.txt-0's descriptionID is 1.txtp436_1.txt
						//int thisfileindex = Integer.parseInt(src.replaceFirst("\\.txt.*$", ""));
						String thisfileindex = src.replaceFirst("\\.txt.*$", "");
						if(finalize){
							if(baseroot ==null){
								order++;
								baseroot = VolumeFinalizer.getBaseRoot(thisfileindex, order);
							}
						}
						//System.out.println(sent);
						if(!sent.matches(".*?\\b/\\b.*") &&!sent.matches(".*?\\b2s\\b.*") &&!sent.matches(".*?× .*") &&!sent.matches(".*?\\+×.*")){//TODO: until the hyphen problems are fix, do not extract from those sentences
							if(!sent.matches(".*?[;\\.]\\s*$")){
								sent = sent+" .";
							}
							//sent = normalizeSpacesRoundNumbers(sent);
							sent = sent.replaceAll("<\\{?times\\}?>", "times");
							SentenceChunker4StanfordParser ex = new SentenceChunker4StanfordParser(i, doc, sent, conn, glosstable);
							ChunkedSentence cs = ex.chunkIt();
							
							//System.out.print("["+src+"]:");
							if(this.debug){
								System.out.println();
								System.out.println(i+"["+src+"]: "+cs.toString());

							}
							cac = new CharacterAnnotatorChunked(conn, this.tableprefix, glosstable);
							//Element statement = cac.annotate(src.replaceAll("^\\d+\\.txt", ""), src, cs); //src: 100.txt-18
							Element statement = cac.annotate(src, src, cs); //src: 100.txt-18
							
							if(finalize){
								if(thisdescID.compareTo(pdescID)!=0){
									if(description.getChildren().size()!=0){ //not empty
										//plug description in XML document
										//write the XML to final
										//call MainForm to display
										//VolumeFinalizer.replaceWithAnnotated(description, count, "/treatment/description", "FINAL", false);
										
										placeDescription(description, pdescID, baseroot);
										description = new Element("description");
										if(this.debug){
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
							description.addContent(statement);
							pdescID = thisdescID;
							pfileindex = thisfileindex;
						}
						rs.relative(i*-1); //reset the pointer
					}
					}
					text = "";
					i = 0;
				}
			}
			if(finalize){
				placeDescription(description, pdescID, baseroot);
				VolumeFinalizer.outputFinalXML(baseroot, pfileindex, "FINAL");		
			}
			rs.close();
    	}catch (Exception e){
    		//System.err.println(e);
			e.printStackTrace();
        }
    }

	public static String normalizeSpacesRoundNumbers(String sent) {
		sent = ratio2number(sent);//bhl
		sent = sent.replaceAll("(?<=\\d)\\s+(?=\\d)", "-"); //bhl: two numbers connected by a space
		sent = sent.replaceAll("at least", "at-least");
		sent = sent.replaceAll("2\\s*n\\s*=", "2n=");
		sent = sent.replaceAll("2\\s*x\\s*=", "2x=");
		sent = sent.replaceAll("n\\s*=", "n=");
		sent = sent.replaceAll("x\\s*=", "x=");
		
		sent = sent.replaceAll("[–—-]", "-").replaceAll(",", " , ").replaceAll(";", " ; ").replaceAll(":", " : ").replaceAll("\\.", " . ").replaceAll("\\[", " [ ").replaceAll("\\]", " ] ").replaceAll("\\(", " ( ").replaceAll("\\)", " ) ").replaceAll("\\s+", " ").trim();
		
		if(sent.matches(".*?[nx]=.*")){
			sent = sent.replaceAll("(?<=\\d)\\s*,\\s*(?=\\d)", ","); //remove spaces around , for chromosome only so numericalHandler.numericalPattern can "3" them into one 3. Other "," connecting two numbers needs spaces to avoid being "3"-ed (fruits 10, 3 of them large) 
		}
		sent = sent.replaceAll("\\b(?<=\\d+) \\. (?=\\d+)\\b", ".");//2 . 5 => 2.5
		sent = sent.replaceAll("(?<=\\d)\\.(?=\\d[nx]=)", " . "); //pappi 0.2n=12
		
		//sent = sent.replaceAll("(?<=\\d)\\s+/\\s+(?=\\d)", "/"); // 1 / 2 => 1/2
		//sent = sent.replaceAll("(?<=[\\d()\\[\\]])\\s+[–—-]\\s+(?=[\\d()\\[\\]])", "-"); // 1 - 2 => 1-2
		//sent = sent.replaceAll("(?<=[\\d])\\s+[–—-]\\s+(?=[\\d])", "-"); // 1 - 2 => 1-2
		Pattern p = Pattern.compile("(.*?)(\\d*)\\s+\\[\\s+([ –—+\\d\\.,?×/-]+)\\s+\\]\\s+(\\d*)(.*)");  //4-25 [ -60 ] => 4-25[-60]. ? is for chromosome count
		Matcher m = p.matcher(sent);
		while(m.matches()){
			sent = m.group(1)+ (m.group(2).length()>0? m.group(2):" ")+"["+m.group(3).replaceAll("\\s*[–—-]\\s*", "-")+"]"+(m.group(4).length()>0? m.group(4):" ")+m.group(5);
			m = p.matcher(sent);
		}
		////keep the space after the first (, so ( 3-15 mm) will not become 3-15mm ) in POSTagger.
		p = Pattern.compile("(.*?)(\\d*)\\s+\\(\\s+([ –—+\\d\\.,?×/-]+)\\s+\\)\\s+(\\d*)(.*)");  //4-25 ( -60 ) => 4-25(-60)
		m = p.matcher(sent);
		while(m.matches()){
			sent = m.group(1)+ (m.group(2).length()>0? m.group(2):" ")+"("+m.group(3).replaceAll("\\s*[–—-]\\s*", "-")+")"+(m.group(4).length()>0? m.group(4):" ")+m.group(5);
			m = p.matcher(sent);
		}
		sent = sent.replaceAll("\\s+/\\s+", "/"); //and/or 1/2
		sent = sent.replaceAll("\\s+×\\s+", "×");
		sent = sent.replaceAll("\\s*\\+\\s*", "+"); // 1 + => 1+
		sent = sent.replaceAll("(?<![\\d()\\]\\[×-])\\+", " +");
		sent = sent.replaceAll("\\+(?![\\d()\\]\\[×-])", "+ ");
		sent = sent.replaceAll("(?<=(\\d))\\s*\\?\\s*(?=[\\d)\\]])", "?"); // (0? )
		sent = sent.replaceAll("\\s*-\\s*", "-"); // 1 - 2 => 1-2, 4 - {merous} => 4-{merous}
		/*if(sent.indexOf(" -{")>=0){//1–2-{pinnately} or -{palmately} {lobed} => {1–2-pinnately-or-palmately} {lobed}
			sent = sent.replaceAll("\\s+or\\s+-\\{", "-or-").replaceAll("\\s+to\\s+-\\{", "-to-").replaceAll("\\s+-\\{", "-{");
		}*/
		return sent;
	}
	
	public static String ratio2number(String sent){
		String small = "\\b(?:one|two|three|four|five|six|seven|eight|nine)\\b";
		String big = "\\b(?:half|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth)s?\\b";
		//ratio
		Pattern ptn = Pattern.compile("(.*?)("+small+"\\s*-?_?\\s*"+big+")(.*)");
		Matcher m = ptn.matcher(sent);
		while(m.matches()){
			String ratio = m.group(2);
			ratio = toRatio(ratio);
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
		// TODO Auto-generated method stub
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
	/**
	 * @param args
	 */
	public static void main(String[] args) {
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
		//StanfordParser.normalizeSpacesRoundNumbers(text);
		//String text = "ovary more than two to three-fourths to one half superior. ";
		//System.out.println(StanfordParser.ratio2number(text));
		/*String posedfile = "FNAv19posedsentences.txt";
		String parsedfile = "FNAv19parsedsentences.txt";
		String database = "fnav19_benchmark";
		StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, "fnav19", "wordpos4parser", "fnaglossaryfixed");
		*/
		
		/*String posedfile = "Treatisehposedsentences.txt";
		String parsedfile = "Treatisehparsedsentences.txt";
		String database = "treatiseh_benchmark";
		StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, "treatiseh", "wordpos4parser", "treatisehglossaryfixed");*/
		
		String posedfile = "BHLposedsentences.txt";
		String parsedfile = "BHLparsedsentences.txt";
		String database = "bhl_benchmark";
		StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, "bhl_clean", "wordpos4parser", "fnabhlglossaryfixed");
		sp.POSTagging();
		sp.parsing();
		sp.extracting();
	}
}
