/**
 * 
 */
package fna.db;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import fna.parsing.ApplicationUtilities;

/**
 * @author hongcui
 *
 */
@SuppressWarnings({ "unchecked" })
public class Type4TransformerDbAccessor {

	/**
	 * @param args
	 */
    private static final Logger LOGGER = Logger.getLogger(VolumeTransformerDbAccess.class);
    private static String url = ApplicationUtilities.getProperty("database.url");
    private static Connection conn = null;
    //private static String prefix = "fna";
    private String prefix = null;
    private String tablename = null;
    
	static {
		try {
			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Couldn't find Class in Type3PreMarkupDbAccessor" + e);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}
	
	/**
	 * 
	 */
	public Type4TransformerDbAccessor(String tablename, String prefix) {
		this.tablename = tablename;
		this.prefix = prefix;
		Statement stmt =null;
		try{
			conn = DriverManager.getConnection(url);
			stmt = conn.createStatement();
			stmt.execute("drop table if exists "+prefix+"_"+tablename);
			stmt.execute("create table if not exists "+prefix+"_"+tablename+" (ofilename varchar(100) NOT NULL, nfilename varchar(100), primary key(ofilename))");			
		}catch(Exception e){
			LOGGER.error("Type4TransformerDbAccessor error:" + e);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}	finally{
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
	
	
	public void addRecords(Hashtable files){
		Statement stmt = null;
		try{
			conn = DriverManager.getConnection(url);
			stmt = conn.createStatement();
			Enumeration<String> en = files.keys();
			while(en.hasMoreElements()){
				String filename = (String)en.nextElement();
				String newfn = (String)files.get(filename);
				stmt.execute("insert into "+prefix+"_"+tablename+" values (\""+filename+"\", \""+newfn+"\")");
			}
		}catch(Exception e){
			LOGGER.error("Type3PreMarkupDbAccessor error:" + e);
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
	
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
