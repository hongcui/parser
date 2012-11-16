/**
 * 
 */
package fna.parsing;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.TreeSet;

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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
		}		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try{
		Connection conn = null;
		if(conn == null){
			Class.forName("com.mysql.jdbc.Driver");
		    String URL = "jdbc:mysql://localhost/markedupdatasets?user=termsuser&password=termspassword";
			conn = DriverManager.getConnection(URL);
		}
		String transformeddir = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameReviewer\\CompleteReviewed\\v19_hong_reviewed_final";
		String outputtablename = "fnav19_taxonames";
		String volume = "fnav19";
		TaxonNameCollector tnc = new TaxonNameCollector(conn, transformeddir, outputtablename, volume);
		tnc.collect();
		}catch(Exception e){
			e.printStackTrace();
		}

	}


	protected void collect4TaxonX() {
		// TODO Auto-generated method stub
		
	}

}
