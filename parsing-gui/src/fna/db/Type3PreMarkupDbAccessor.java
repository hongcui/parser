
package fna.db;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import fna.parsing.ApplicationUtilities;

@SuppressWarnings("unchecked")
public class Type3PreMarkupDbAccessor {

	/**
	 * @param args
	 */
    private static final Logger LOGGER = Logger.getLogger(VolumeTransformerDbAccess.class);
    private static String url = ApplicationUtilities.getProperty("database.url");
    private static Connection conn = null;
    //private static String prefix = "fna";
    @SuppressWarnings("unused")
	private String prefix = null;
    
	static {
		try {
			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Couldn't find Class in Type3PreMarkupDbAccessor" + e);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}
	

	public Type3PreMarkupDbAccessor(){
		
	}
	
	public ArrayList selectRecords(String select, String from, String where){
		ArrayList<String> results = new ArrayList<String>();
		Statement stmt = null;
		ResultSet rs = null;
		try{
			conn = DriverManager.getConnection(url);
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select "+select+" from "+from+" where "+where);
			String[] temp = select.split("\\s*,\\s*");
			while(rs.next()){
				String row = "";
				for(int i = 1; i<=temp.length; i++){
					row += rs.getString(i)+"###";
				}
				row = row.replaceFirst("###$", "");
				if(row.indexOf("###")>=0){
					row += " "; //so "" value attached to the end of row can be recognized
				}
				results.add(row);
			}
		}catch(Exception e){
			LOGGER.error("Type3PreMarkupDbAccessor error:" + e);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}finally{
			try{
				if(rs!=null) rs.close();
				if(stmt!=null) stmt.close();
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
						System.getProperty("line.separator")
						+sw.toString());
			}
		}		
		return results;
	}
	
	


	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		System.out.println(DriverManager.getConnection(url));

	}

}
