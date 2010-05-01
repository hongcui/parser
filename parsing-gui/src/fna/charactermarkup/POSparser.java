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
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.trees.tregex.*;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;


import fna.parsing.ProcessListener;


public class POSparser {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "root";
	static protected String password = "";
	private ProcessListener listener;

	public POSparser() {
		// TODO Auto-generated constructor stub
	}

	public POSparser(String database) {
		// TODO Auto-generated constructor stub
		collect(database);
	}
	
	protected void collect(String database){
		POSparser.database = database;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists marked_pos (sentid MEDIUMINT NOT NULL, source varchar(100) NOT NULL, markedsent TEXT, PRIMARY KEY(sentid))");
				stmt.execute("delete from marked_pos");
				parse_pos();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	protected void parse_pos(){
		try
		{
		/*	Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			Statement stmt3 = conn.createStatement();
			FileOutputStream ostream = new FileOutputStream("F:\\UA\\RA\\Stanford-Parser\\stanford-parser-2010-02-26\\seginput.txt"); 
			PrintStream out = new PrintStream( ostream );
			String str;
			ResultSet rs = stmt.executeQuery("select * from newsegments");
        	while(rs.next()){
        		str=rs.getString(3);
        		str = reversecondense(str);
            	str = plaintextextractor(str);
            	Pattern pattern1 = Pattern.compile("_");
                // Replace all occurrences of pattern in input
                Matcher matcher1 = pattern1.matcher(str);
                str = matcher1.replaceAll(" ");
                matcher1.reset();
                Pattern pattern2 = Pattern.compile(":");
                // Replace all occurrences of pattern in input
                matcher1 = pattern2.matcher(str);
                str = matcher1.replaceAll(",");
                matcher1.reset();
                
                Pattern pattern3 = Pattern.compile("[\\d]+[\\-\\�]+[\\d]+");
   				Pattern pattern4 = Pattern.compile("(?<!(ca[\\s]?|diam[\\s]?))([\\d]?[\\s]?\\.[\\s]?[\\d]+[\\s]?[\\�\\-]+[\\s]?[\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]+[\\s]?[\\�\\-]+[\\s]?[\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]/[\\d][\\s]?[\\�\\-][\\s]?[\\d]/[\\d])|(?<!(ca[\\s]?|diam[\\s]?))([\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]/[\\d])");
   				Pattern pattern5 = Pattern.compile("[\\d�\\+\\�\\-\\���:�/�\"��\\_�\\׵%\\*\\{\\}\\[\\]=]+");
   				Pattern pattern6 = Pattern.compile("(0[\\s]*)+");
   				//Pattern pattern5 = Pattern.compile("((?<!(/|(\\.[\\s]?)))[\\d]+[\\-\\�]+[\\d]+(?!([\\�\\-]+/|([\\s]?\\.))))|((?<!(\\{|/))[\\d]+(?!(\\}|/)))");
                //[\\d�\\+\\�\\-\\��.�:�/�\"��\\_;x�\\�\\s,�%\\*\\{\\}\\[\\]=(<\\{)(\\}>)]+
   				matcher1 = pattern3.matcher(str);
                str = matcher1.replaceAll("0");
                matcher1.reset();
                
                matcher1 = pattern4.matcher(str);
                str = matcher1.replaceAll("0");
                matcher1.reset();
                
                matcher1 = pattern5.matcher(str);
                str = matcher1.replaceAll("0");
                matcher1.reset();
                
                matcher1 = pattern6.matcher(str);
                str = matcher1.replaceAll("0 ");
                matcher1.reset();
                
                StringBuffer sb = new StringBuffer();
                Pattern pattern7 = Pattern.compile("[a-zA-Z]+");
                matcher1 = pattern7.matcher(str);
            	while ( matcher1.find()){
            		String pos = matcher1.group(0);
            		//System.out.println(pos);
            		ResultSet rs1 = stmt1.executeQuery("select * from wordpos where word='"+pos+"'");
            		if(rs1.next()){
            			if(rs1.getString(2).contains("p")|rs1.getString(2).contains("s")){
            				System.out.println(rs1.getString(2));
            				matcher1.appendReplacement(sb, pos+"/NN");
            			}
            			else if(rs1.getString(2).contains("b")){
            				System.out.println(rs1.getString(2));
            				matcher1.appendReplacement(sb, pos+"/JJ");
            			}
            		}
            	}
            	matcher1.appendTail(sb);
        		str=sb.toString();
        		matcher1.reset();
        		out.println(str);
        		stmt2.execute("insert into marked_pos values('"+rs.getString(1)+"','"+rs.getString(2)+"','"+str+"')");
        	}
        	out.close();
        	
        	Runtime r = Runtime.getRuntime();
			Process p = r.exec("cmd /c lexparserPOS.bat");
        	
        	FileInputStream istream = new FileInputStream("F:\\UA\\RA\\Code\\parsing-gui-charactermarkup\\finaloutput.txt"); 
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(istream));
        	String s = "";
			while ((s=stdInput.readLine())!=null){
				System.out.println(s);
				TreeReader ptr = new PennTreeReader(stdInput);
				Tree t = ptr.readTree();
				System.out.println(t);
				TregexPattern pattern = TregexPattern.compile("__");
				TregexMatcher matcher = pattern.matcher(t);
				while(matcher.find()){
						//Tree tr = matcher.getNode("");
						//TreePrint tp = new TreePrint("penn");
						//tp.printTree(tr);
						System.out.println("hello");
				}
				matcher.reset();
        	}*/
			
			FileInputStream istream = new FileInputStream("F:\\UA\\RA\\Code\\parsing-gui-charactermarkup\\finaloutput.txt"); 
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(istream));
        	String s = "";
        	String str = "";
        	int ct = 0;
			while ((s=stdInput.readLine())!=null){
				//System.out.println(s);
				if(!s.contains("(ROOT")){
					str = str.concat(s);
				}					
				else{
					ct++;
					if(ct!=1){
						System.out.println(ct);
						System.out.println(str);
						extractrelation(str);
						str = "";
					}
					//System.out.println(s);
				}
        	}
			
		}catch (Exception e){
    		//System.err.println(e);
			e.printStackTrace();
        }
	}
	
	protected void extractrelation (String str) {
		Pattern pattern = Pattern.compile("\\(VP[()\\w\\s]+\\(PP[\\s]\\([\\w\\s]+\\)");
        Matcher matcher = pattern.matcher(str);
        while(matcher.find()){
        	System.out.println(str.substring(matcher.start(), matcher.end()));
        }
        matcher.reset();
	}

	protected String reversecondense(String str) { 
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
		Pattern pattern11 = Pattern.compile("[{][\\w�\\+\\�\\-\\.:=/\\_]+[}]");
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
		Pattern pattern1 = Pattern.compile("[\\w�\\+\\�\\-\\���\\.:=/\\s�\"�;x�\\�\\*�%��\\_,]+");
    	Matcher matcher = pattern1.matcher(str);
    	while ( matcher.find()){
    		int i=matcher.start();
    		int j=matcher.end();
    		str1=str1.concat(str.subSequence(i,j).toString());
    	}
    	matcher.reset();
    	return(str1);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new POSparser("fnav19_benchmark");
	}

}
