
package fna.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import fna.parsing.ApplicationUtilities;
import fna.parsing.MainForm;
import fna.parsing.ParsingException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;

public class Type3PreMarkupDbAccessor {

	/**
	 * @param args
	 */
    private static final Logger LOGGER = Logger.getLogger(VolumeTransformerDbAccess.class);
    private static String url = ApplicationUtilities.getProperty("database.url");
    private static Connection conn = null;
    //private static String prefix = "fna";
    private String prefix = null;
    
	static {
		try {
			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Couldn't find Class in Type3PreMarkupDbAccessor" + e);
			e.printStackTrace();
		}
	}
	

	public Type3PreMarkupDbAccessor(){
		
	}
	
	public ArrayList selectRecords(String select, String from, String where){
		ArrayList<String> results = new ArrayList<String>();
		try{
			conn = DriverManager.getConnection(url);
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select "+select+" from "+from+" where "+where);
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
			e.printStackTrace();
		}		
		return results;
	}
	
	


	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		System.out.println(DriverManager.getConnection(url));

	}

}
