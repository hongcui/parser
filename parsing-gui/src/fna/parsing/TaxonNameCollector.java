/**
 * 
 */
package fna.parsing;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * @author Hong Updates
 * This class gathers taxon names from Transformed FNA descriptions
 * The taxon names are then saved in a database table 
 * One of use of the names is to support fine-grained annotation of 
 * exceptions (e.g "leaves red (green in species s)") in FNA
 */
public class TaxonNameCollector {
	private String volume;
	private File transformeddir;
	private String outputtablename;
	private Connection conn;
	private PreparedStatement insert;
	private TreeSet<String> names = new TreeSet<String>();
	 private static final Logger LOGGER = Logger.getLogger(TaxonNameCollector.class);  

	/**
	 * 
	 */
	public TaxonNameCollector(Connection conn, String transformeddir, String outputtablename, String volume) throws Exception {
		this.transformeddir = new File(transformeddir);
		this.outputtablename = outputtablename;
		this.conn = conn;
		this.volume = volume;
		Statement st = conn.createStatement();
		st.execute("drop table if exists "+this.outputtablename);
		PreparedStatement stmt = conn.prepareStatement("create table if not exists "+this.outputtablename+" (nameid MEDIUMINT not null auto_increment primary key, name varchar(200), source varchar(50))");
		stmt.execute();
		this.insert = conn.prepareStatement("insert into "+this.outputtablename+"(name, source) values (?, ?)");
	}

	
	public void collect(){
		try{
			File[] xmlfiles = this.transformeddir.listFiles();
			for(File xmlfile: xmlfiles){
				collectNames(xmlfile);
			}
			saveNames();
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}
	
	private void saveNames() {
		try{
			for(String name: names){
				if(name.length() > 200) name = name.substring(0, name.indexOf(" "));//each name should be 1-word long.
				insert.setString(1, name);	
				insert.setString(2, this.volume);
				insert.execute();
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}	
		
	}

	@SuppressWarnings("unchecked")
	private void collectNames(File xmlfile) {
		try{
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(xmlfile);
			Element root = doc.getRootElement();
			List<Element> names = XPath.selectNodes(root, "//*[ends-with(name(), '_name')]");
			addNames(names);
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
		
	}

	/**
	 * if this.outputtablename exists, add the names to it
	 * else create a table
	 * @param names
	 * @param nametype
	 */
	private void addNames(List<Element> names) {
		try{
			for(Element name: names){
				String namerank = name.getName();
				String namestr = name.getTextTrim();
				if(namestr.contains("conserved")){
					System.out.println();
				}
				if(!namerank.contains("common") && !namerank.contains("conserved")&& !(namestr.length()==2 && namestr.endsWith(".")))this.names.add(namestr);
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Connection conn = null;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
			    String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
			}
			String transformeddir = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameReviewer\\CompleteReviewed\\v19_hong_reviewed_final";
			String outputtablename = "fnav19_"+ApplicationUtilities.getProperty("TAXONNAMES");
			String volume = "fnav19";
			TaxonNameCollector tnc = new TaxonNameCollector(conn, transformeddir, outputtablename, volume);
			tnc.collect();
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}finally{
			try{
				conn.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}

	}


	protected void collect4TaxonX() {
		// TODO Auto-generated method stub
		
	}

}
