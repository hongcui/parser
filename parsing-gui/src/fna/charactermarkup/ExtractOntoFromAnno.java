 /* $Id$ */
/**
 * 
 */
package fna.charactermarkup;

import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Connection;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

import org.apache.log4j.Logger;

import fna.parsing.ApplicationUtilities;

/**
 * @author hongcui
 *
 */
@SuppressWarnings({  "unused" })
public class ExtractOntoFromAnno {
	Hashtable<String, String> onto = new Hashtable<String, String>();
	Connection conn = null;
	String ontoname = null;
	private static final Logger LOGGER = Logger.getLogger(ExtractOntoFromAnno.class);
	/**
	 * this class extract term/category pairs from GoldenGate annotated files
	 */
	public ExtractOntoFromAnno(String folder, String database, String ontoname, String username, String password) {
		this.ontoname = ontoname;
		Statement stmt = null;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password+"&connectTimeout=0&socketTimeout=0&autoReconnect=true";
				conn = DriverManager.getConnection(URL);
				stmt = conn.createStatement();
				stmt.execute("create table if not exists "+ontoname+" (id int NOT NULL auto_increment, term varchar(150), category varchar(150), remark varchar(150), primary key(id))");
				//stmt.execute("delete from antglossary");
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}finally{
			try{
				if(stmt!=null) stmt.close();
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
						System.getProperty("line.separator")
						+sw.toString());
			}
		}
		File[] files = new File(folder).listFiles();
		for(int i = 0; i<files.length; i++){
			extractFrom(files[i]);
		}
		dump2database();
	
	}

	
	private void dump2database() {
		Statement stmt = null;
		try{
			stmt = conn.createStatement();
			Enumeration<String> en = this.onto.keys();
			while(en.hasMoreElements()){
				String term = en.nextElement();
				String cat = this.onto.get(term);
				stmt = conn.createStatement();
				stmt.execute("insert into "+ontoname+" (term, category) values ('"+term+"','"+cat+"')");
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}finally{
			try{
				if(stmt!=null) stmt.close();
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
						System.getProperty("line.separator")
						+sw.toString());
			}
		}
		
	}


	private void extractFrom(File file) {
		try{
			FileInputStream fstream = new FileInputStream(file);
		    DataInputStream in = new DataInputStream(fstream);
		    BufferedReader br = new BufferedReader(new InputStreamReader(in));
		    
		    String strLine;
		    Pattern p = Pattern.compile(".*?normTerm=(\\S+) type=(\\S+?)(>.*)");
		    while ((strLine = br.readLine()) != null)   {
		    	Matcher m = p.matcher(strLine);
		    	while(m.matches()){
		    		add(m.group(1).replaceAll("\\W", " ").trim(), m.group(2).replaceAll("\\W", " ").trim());
		    		m = p.matcher(m.group(3));
		    	}
		    }
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}
		



	private void add(String term, String cat) {
		String category = (String)this.onto.get(term);
		if(category == null){
			this.onto.put(term, cat);
		}else if(!category.matches(".*?\\b"+cat+"\\b.*")){
			this.onto.put(term, category+"#"+cat);
		}
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//String folder = "X:\\DATA\\Plazi\\fromOSU\\Annotated_ants_from_Charuwat";
		/*String folder = "X:\\DATA\\Plazi\\fromOSU\\compAnnotated-ants_No02";
		String database = "manual_annotation";
		String username = "root";
		String password = "root";
		String ontoname = "antglossary";*/
		
		String folder = "X:\\DATA\\Treatise\\annotatedAtYale\\annotateddescriptions";
		String database = "manual_annotation";
		String username = "root";
		String password = "root";
		String ontoname = "brochiopodglossary";
		ExtractOntoFromAnno eofa = new ExtractOntoFromAnno(folder, database, ontoname, username, password);
	}

}
