/**
 * 
 */
package fna.parsing;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Hashtable;

import org.apache.log4j.Logger;

/**
 * @author Hong Updates
 * This class use some sort of input to generate a database table call filename2taxon
 */
public abstract class FileName2Taxon {
	protected String inputfilepath;
	protected String database;
	protected String dataprefix;
	protected Connection conn;
	protected static final Logger LOGGER = Logger.getLogger(FileName2Taxon.class);
	protected Hashtable<String, String> values = new Hashtable<String, String>();
	
	
	
	/**
	 * 
	 */
	public FileName2Taxon(String inputfilepath, String database, String prefix) {
		values.put("filename", "");
		values.put("hasdescription", "");
		values.put("family", "");
		values.put("subfamily", "");
		values.put("tribe", "");
		values.put("subtribe", "");
		values.put("genus", "");
		values.put("subgenus", "");
		values.put("section", "");
		values.put("subsection", "");
		values.put("species", "");
		values.put("subspecies", "");
		values.put("variety", "");
		
		
		this.inputfilepath = inputfilepath;
		this.database = database;
		this.dataprefix = prefix;
		if(conn == null){
			try{
				Class.forName("com.mysql.jdbc.Driver");
			    String URL = "jdbc:mysql://localhost/"+database+"?user=termsuser&password=termspassword";
				//String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			}
		}
	}

	protected void populateFilename2TaxonTable(){
		File[] xmls = (new File(this.inputfilepath)).listFiles();
		int size = xmls.length;
		//must be in the original order in the original volume.
		for(int i = 1; i <= size; i++){
			System.out.println(i+".xml");
			populateFilename2TaxonTableUsing(new File(this.inputfilepath, i+".xml"));
		}
	}
	
	protected abstract void populateFilename2TaxonTableUsing(File xml);
	public void insertIntoFilename2TaxonTable(){
		String filename = values.get("filename");
		int hasdescription = Integer.parseInt(values.get("hasdescription"));
		String family = values.get("family");
		String subfamily = values.get("subfamily");
		String tribe = values.get("tribe");
		String subtribe = values.get("subtribe");
		String genus = values.get("genus");
		String subgenus = values.get("subgenus");
		String section = values.get("section");
		String subsection = values.get("subsection");
		String species = values.get("species");
		String subspecies = values.get("subspecies");
		String variety = values.get("variety");				
		
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("insert into "+dataprefix+"_filename2taxon values (" +
					"'"+filename+"'," +hasdescription+",'" +family+"','" +subfamily+"','" 
					+tribe+"','" +subtribe+"','" + genus+"','" +subgenus+"','" +section+"','" +subsection+"','" 
					+species+"','" +subspecies+"','" +variety+"')");

			stmt.close();
			
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
		
	}
	public void createFilename2taxonTable() {
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("drop table if exists "+dataprefix+"_filename2taxon");
			stmt.execute("create table if not exists "+dataprefix+"_filename2taxon (" +
					"`filename` varchar(10) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL," +
					"`hasdescription` tinyint(1) DEFAULT 0," +
					"`family` varchar(250) CHARACTER SET utf8 COLLATE utf8_bin," +
					"`subfamily` varchar(250) CHARACTER SET utf8 COLLATE utf8_bin ," +
					"`tribe` varchar(250) CHARACTER SET utf8 COLLATE utf8_bin ," +
					"`subtribe` varchar(250) CHARACTER SET utf8 COLLATE utf8_bin ," +
					"`genus` varchar(250) CHARACTER SET utf8 COLLATE utf8_bin ," +
					"`subgenus` varchar(250) CHARACTER SET utf8 COLLATE utf8_bin ," +
					" `section` varchar(250) CHARACTER SET utf8 COLLATE utf8_bin ," +
					"`subsection` varchar(250) CHARACTER SET utf8 COLLATE utf8_bin ," +
					" `species` varchar(250) CHARACTER SET utf8 COLLATE utf8_bin ," +
					"`subspecies` varchar(250) CHARACTER SET utf8 COLLATE utf8_bin ," +
					"`variety` varchar(250) CHARACTER SET utf8 COLLATE utf8_bin) " +
					" DEFAULT CHARSET=utf8;");
			stmt.close();
			
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
