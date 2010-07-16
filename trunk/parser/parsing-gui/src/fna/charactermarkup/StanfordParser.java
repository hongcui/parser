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

import fna.parsing.state.SentenceOrganStateMarker;


/**
 * @author hongcui
 *
 */
public class StanfordParser {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "root";
	static protected String password = "root";
	protected int count = 0;
	private File posedfile = null;
	private File parsedfile = null;
	private String POSTaggedSentence = "POSedSentence";
	private MyPOSTagger tagger = null;
	private SentenceOrganStateMarker sosm = null;
	//private Hashtable sentmapping = new Hashtable();
	private boolean debug = true;
	/**
	 * 
	 */
	public StanfordParser(String posedfile, String parsedfile, String database) {
		// TODO Auto-generated constructor stub
		this.posedfile = new File(posedfile); 
		this.parsedfile = new File(parsedfile);
		this.database = database;
		this.sosm = new SentenceOrganStateMarker(this.database);
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				//stmt.execute("create table if not exists marked_pos (sentid MEDIUMINT NOT NULL, source varchar(100) NOT NULL, markedsent TEXT, PRIMARY KEY(sentid))");
				stmt.execute("create table if not exists "+this.POSTaggedSentence+"(source varchar(100) NOT NULL, posedsent TEXT, PRIMARY KEY(source))");
				//stmt.execute("delete from "+this.POSTaggedSentence);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		tagger = new MyPOSTagger(database);
	}
	
	public void POSTagging(){
		try{
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			Statement stmt3 = conn.createStatement();
			FileOutputStream ostream = new FileOutputStream(posedfile); 
			PrintStream out = new PrintStream( ostream );
			
			//ResultSet rs = stmt.executeQuery("select * from newsegments");
			ResultSet rs = stmt.executeQuery("select * from markedsentence order by source");
			int count = 1;
			while(rs.next()){
				//str=rs.getString(3);
				String src = rs.getString(1);
				String str = rs.getString(2);
				//TODO: may need to fix "_"
				str = tagger.POSTag(str);
	       		//stmt2.execute("insert into marked_pos values('"+rs.getString(1)+"','"+rs.getString(2)+"','"+str+"')");
	       		stmt2.execute("insert into "+this.POSTaggedSentence+" values('"+rs.getString(1)+"','"+str+"')");
	       		//System.out.println("POSed sentence "+rs.getString(1)+ " inserted");
	       		out.println(str);
	       		count++;
	       		//sentmapping.put(""+count, src);
			}
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
	  		Process p = r.exec("cmd /c stanfordparser.bat >"+this.parsedfile+" 2<&1");
	  		//Process p = r.exec("cmd /c cd \"C:\\Program Files\\stanford-parser-2010-02-26\"");
	  		//String command = "cmd /c java -mx900m -cp \"stanford-parser.jar;\" edu.stanford.nlp.parser.lexparser.LexicalizedParser -sentences newline -tokenized -tagSeparator / englishPCFG.ser.gz \""+this.posedfile.getAbsolutePath()+"\" > "+this.parsedfile.getAbsolutePath();
	  		//p = r.exec(command);
	  		
	  		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			BufferedReader errInput = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			
			// read the errors from the command
			String e = "";
			while ((e = errInput.readLine()) != null) {
				System.out.println(e);
				//Matcher m = ptn.matcher(e);
				//if(m.matches()){
				//	e = "["+(String) sentmapping.get(m.group(1))+"]";
				//}
				//out.println(e);
			}
			// read the output from the command
			String s = "";
			while ((s = stdInput.readLine()) != null) {
				System.out.println(s);
				//Matcher m = ptn.matcher(s);
				//if(m.matches()){
				//	s = "["+(String) sentmapping.get(m.group(1))+"]";
				//}
				//out.println(s);
				
			}
			
			
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
			ArrayList docs = new ArrayList();
			String line = "";
			String text = "";
			int i = 0;
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select source, markedsent from markedsentence order by source");
			Pattern ptn = Pattern.compile("^Parsing \\[sent\\. (\\d+) len\\. \\d+\\]:(.*)");
			while ((line = stdInput.readLine())!=null){
				if(line.startsWith("Loading") || line.startsWith("X:") || line.startsWith("Parsing file")|| line.startsWith("Parsed") ){continue;}
				if(line.trim().length()>1){
					Matcher m = ptn.matcher(line);
					if(m.matches()){
						i = Integer.parseInt((String)m.group(1));
					}else{
						text += line.replace(System.getProperty("line.separator"), ""); 
					}
				}else{
					text = text.replaceAll("(?<=[A-Z])\\$ ", "S ");
					Tree2XML t2x = new Tree2XML(text);
					Document doc = t2x.xml();
					Document doccp = (Document)doc.clone();
					if(rs.relative(i)){
						String sent = rs.getString("markedsent");
						String src = rs.getString("source");
						//System.out.println(sent);
						if(!sent.matches(".*?[_–-]\\s*[<{a-z].*") && !sent.matches(".*?\\band/or\\b.*") &&!sent.matches(".*?\\b2s\\b.*")){//TODO: until the hyphen problems are fix, do not extract from those sentences
							sent = sent.replaceAll(",", " , ").replaceAll(";", " ; ").replaceAll(":", " : ").replaceAll("\\.", " . ").replaceAll("\\[", " [ ").replaceAll("\\]", " ] ").replaceAll("\\s+", " ");
							SentenceChunker ex = new SentenceChunker(i, doc, sent);
							ChunkedSentence cs = ex.chunkIt();
							if(this.debug){
								System.out.println();
								System.out.println(i+": "+cs.toString());
							}
							//CharacterAnnotatorChunked cac = new CharacterAnnotatorChunked(i, src, cs, doccp, this.sosm);
							//Element statement = cac.annotate();
						}
						rs.relative(i*-1); //reset the pointer
					}
					text = "";
					i = 0;
				}
			}
			if(text.trim().compareTo("") != 0){ //last tree
				text = text.replaceAll("(?<=[A-Z])\\$ ", "S ");
				Tree2XML t2x = new Tree2XML(text);
				Document doc = t2x.xml();
				Document doccp = (Document)doc.clone();
				if(rs.relative(i)){
					String sent = rs.getString("markedsent");
					String src = rs.getString("source");
					//System.out.println(sent);
					if(!sent.matches(".*?[_–-]\\s*[<{a-z].*") && !sent.matches(".*?\\band/or\\b.*") &&!sent.matches(".*?\\b2s\\b.*")){//TODO: until the hyphen problems are fix, do not extract from those sentences
						sent = sent.replaceAll(",", " , ").replaceAll(";", " ; ").replaceAll(":", " : ").replaceAll("\\.", " . ").replaceAll("\\[", " [ ").replaceAll("\\]", " ] ").replaceAll("\\s+", " ");
						SentenceChunker ex = new SentenceChunker(i, doc, sent);
						ChunkedSentence cs = ex.chunkIt();
						if(this.debug){
							System.out.println();
							System.out.println(i+": "+cs.toString());
						}
						//CharacterAnnotatorChunked cac = new CharacterAnnotatorChunked(i, src, cs, doccp, this.sosm);
						//Element statement = cac.annotate();
					}
				}
			}
    	}catch (Exception e){
    		//System.err.println(e);
			e.printStackTrace();
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
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String posedfile = "Copy of FNAv19posedsentences1.txt";
		String parsedfile = "Copy of FNAv19parsedsentences1.txt";
		String database = "fnav19_benchmark";
		StanfordParser sp = new StanfordParser(posedfile, parsedfile, database);
		//sp.POSTagging();
		//sp.parsing();
		sp.extracting();
	}
}
