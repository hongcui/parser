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
public class StanfordParser implements Learn2Parse{
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "root";
	static protected String password = "root";
	protected int count = 0;
	private File posedfile = null;
	private File parsedfile = null;
	private String POSTaggedSentence = "POSedSentence";
	private MyPOSTagger tagger = null;
	private String tableprefix = null;
	//private SentenceOrganStateMarker sosm = null;
	//private Hashtable sentmapping = new Hashtable();
	private boolean debug = true;
	/**
	 * 
	 */
	public StanfordParser(String posedfile, String parsedfile, String database, String tableprefix) {
		// TODO Auto-generated constructor stub
		this.posedfile = new File(posedfile); 
		this.parsedfile = new File(parsedfile);
		this.database = database;
		this.tableprefix = tableprefix;
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
		tagger = new MyPOSTagger(conn, this.tableprefix);
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


	  		//Process p = r.exec("cmd /c cd \"C:\\Program Files\\stanford-parser-2010-02-26\"");
	  		//String command = "cmd /c java -mx900m -cp \"stanford-parser.jar;\" edu.stanford.nlp.parser.lexparser.LexicalizedParser -sentences newline -tokenized -tagSeparator / englishPCFG.ser.gz \""+this.posedfile.getAbsolutePath()+"\" > "+this.parsedfile.getAbsolutePath();
	  		//p = r.exec(command);
	  		
	  		
	  	    /*BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));			
			BufferedReader errInput = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			int h = 1;
			int t = 1;
			// read the errors from the command
			String e = "";
			while ((e = errInput.readLine()) != null) {
				System.out.println(e);
				if(e.startsWith("Parsing [sent.")){
            		headings.add(e);
            		System.out.println(h+" add heading: "+e);
            		h++;
            	}
			}
			// read the output from the command
			String s = "";
			StringBuffer sb = new StringBuffer();
			while ((s = stdInput.readLine()) != null) {
				System.out.println(s);
				if(s.startsWith("(ROOT") || s.startsWith("Sentence too long")){
        			if(sb.toString().trim().length()>0){
        				trees.add(sb.toString());
        				System.out.println(t+" add tree: "+sb.toString());
        				t++;
        			}
        			sb = new StringBuffer();
        			sb.append(s+System.getProperty("line.separator"));
        		}else if(s.matches("^\\s*\\(.*")){
        			sb.append(s+System.getProperty("line.separator"));
        		}
			}
			trees.add(sb.toString());
			*/
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
			int pfileindex = 0;
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
					if(i != 359 && i !=484 && i != 1264 && i !=1782 && text.startsWith("(ROOT")){
					text = text.replaceAll("(?<=[A-Z])\\$ ", "S ");
					t2x = new Tree2XML(text);
					doc = t2x.xml();
					//Document doccp = (Document)doc.clone();
					if(rs.relative(i)){
						String sent = rs.getString("rmarkedsent");
						String src = rs.getString("source");
						String thisdescID = src.replaceFirst("-\\d+$", "");//1.txtp436_1.txt-0's descriptionID is 1.txtp436_1.txt
						int thisfileindex = Integer.parseInt(src.replaceFirst("\\.txt.*$", ""));
						if(baseroot ==null){
							baseroot = VolumeFinalizer.getBaseRoot(thisfileindex);
						}
						
						//System.out.println(sent);
						if(!sent.matches(".*?\\b/\\b.*") &&!sent.matches(".*?\\b2s\\b.*")){//TODO: until the hyphen problems are fix, do not extract from those sentences
							if(!sent.matches(".*?[;\\.]\\s*$")){
								sent = sent+".";
							}
							sent = sent.replaceAll(",", " , ").replaceAll(";", " ; ").replaceAll(":", " : ").replaceAll("\\.", " . ").replaceAll("\\[", " [ ").replaceAll("\\]", " ] ").replaceAll("\\s+", " ").trim();
							
							SentenceChunker ex = new SentenceChunker(i, doc, sent, conn);
							ChunkedSentence cs = ex.chunkIt();
							if(this.debug){
								System.out.println();
								System.out.println(i+": "+cs.toString());
							}
							cac = new CharacterAnnotatorChunked(conn, this.tableprefix);
							//Element statement = cac.annotate(src.replaceAll("^\\d+\\.txt", ""), src, cs); //src: 100.txt-18
							Element statement = cac.annotate(src, src, cs); //src: 100.txt-18
							
							if(thisdescID.compareTo(pdescID)!=0){
								if(description.getChildren().size()!=0){ //not empty
									//plug description in XML document
									//write the XML to final
									//call MainForm to display
									//VolumeFinalizer.replaceWithAnnotated(description, count, "/treatment/description", "FINAL", false);
									
									placeDescription(description, pdescID, baseroot);
									description = new Element("description");
									if(this.debug){
										System.out.println(count+".xml written");
									}
								}
							}
							
							if(thisfileindex != pfileindex){
								if(pfileindex !=0){
									VolumeFinalizer.outputFinalXML(baseroot, pfileindex, "FINAL");
									baseroot = VolumeFinalizer.getBaseRoot(thisfileindex);
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
			placeDescription(description, pdescID, baseroot);
			VolumeFinalizer.outputFinalXML(baseroot, pfileindex, "FINAL");
			//VolumeFinalizer.replaceWithAnnotated(description, count, "/treatment/description", "FINAL", false);
			/*if(text.trim().compareTo("") != 0){ //last tree
				text = text.replaceAll("(?<=[A-Z])\\$ ", "S ");
				t2x = new Tree2XML(text);
				doc = t2x.xml();
				//Document doccp = (Document)doc.clone();
				if(rs.relative(i)){
					String sent = rs.getString("rmarkedsent");
					String src = rs.getString("source");
					//System.out.println(sent);
					if(!sent.matches(".*?[_–-]\\s*[<{a-z].*") && !sent.matches(".*?\\band/or\\b.*") &&!sent.matches(".*?\\b2s\\b.*")){//TODO: until the hyphen problems are fix, do not extract from those sentences
						sent = sent.replaceAll(",", " , ").replaceAll(";", " ; ").replaceAll(":", " : ").replaceAll("\\.", " . ").replaceAll("\\[", " [ ").replaceAll("\\]", " ] ").replaceAll("\\s+", " ");
						SentenceChunker ex = new SentenceChunker(i, doc, sent, conn);
						ChunkedSentence cs = ex.chunkIt();
						if(this.debug){
							System.out.println();
							System.out.println(i+": "+cs.toString());
						}
						cac = new CharacterAnnotatorChunked(conn, this.tableprefix);
						Element statement = cac.annotate(Integer.parseInt(src.replaceAll("^\\d+\\.txt-", ""))+1, src, cs);
						description.addContent(statement);
						//plug description in XML document
						//write the XML to final
						//call MainForm to display
						int filecount = Integer.parseInt(src.replaceFirst("\\.txt-\\.*$", ""));
						VolumeFinalizer.replaceWithAnnotated(description, filecount, "/treatment/description", "FINAL", false);
					}
				}
			}*/
			
			rs.close();
    	}catch (Exception e){
    		//System.err.println(e);
			e.printStackTrace();
        }
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

	/*protected String reversecondense(String str) { 
		StringBuffer sb2 = new StringBuffer();
    	Pattern pattern12 = Pattern.compile("<[a-zA-Z_ ]+>");
    	Matcher matcher = pattern12.matcher(str);
    	while ( matcher.find()){
    		int k=matcher.start()+1;
			int l=matcher.end()-1;
			String org=str.subSequence(k,l).toString();
			if(org.contains("_has_")){
				String org1=org.subSequence(0,org.indexOf("_")).toString();
				String org2=org.subSequence(org.lastIndexOf("_")+1,org.length()).toString();
				matcher.appendReplacement(sb2, "<"+org1+"> <"+org2+">");
			}
    	}
    	matcher.appendTail(sb2);
		str=sb2.toString();
		matcher.reset();
    	StringBuffer sb1 = new StringBuffer();
		Pattern pattern11 = Pattern.compile("[{][\\w±\\+\\–\\-\\.:=/\\_]+[}]");
		matcher = pattern11.matcher(str);
		while ( matcher.find()){
			int k=matcher.start()+1;
			int l=matcher.end()-1;
			String state=str.subSequence(k,l).toString();
			Pattern pattern13 = Pattern.compile("_");
			Matcher matcher1 = pattern13.matcher(state);
			state = matcher1.replaceAll("} or {");
			matcher1.reset();
			matcher.appendReplacement(sb1, state);
		}
		matcher.appendTail(sb1);
		str=sb1.toString();
		matcher.reset();
		return(str);
	}
	
	protected String plaintextextractor(String str) {
		String str1 = "";
		Pattern pattern1 = Pattern.compile("[\\w±\\+\\–\\-\\—°²\\.:=/\\s½\"¼;x´\\×\\*µ%“”\\_,]+");
    	Matcher matcher = pattern1.matcher(str);
    	while ( matcher.find()){
    		int i=matcher.start();
    		int j=matcher.end();
    		str1=str1.concat(str.subSequence(i,j).toString());
    	}
    	matcher.reset();
    	return(str1);
	}*/
	
	
	public ArrayList<String> getMarkedDescription(String source){
		return null;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String posedfile = "FNAv19posedsentences.txt";
		String parsedfile = "FNAv19parsedsentences.txt";
		String database = "fnav19_benchmark";
		StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, "fnav19");
		//sp.POSTagging();
		//sp.parsing();
		sp.extracting();
	}
}
