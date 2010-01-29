package fna.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Combo;

import fna.parsing.ApplicationUtilities;

public class ConfigurationDbAccessor {

	/**
	 * @param args
	 */
	private static final Logger LOGGER = Logger.getLogger(ConfigurationDbAccessor.class);
	private static String url = ApplicationUtilities.getProperty("database.url");
	
	static {
		try {
			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Couldn't find Class in ConfigurationDbAccessor" + e);
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	
	public void retrieveTagDetails(List <String> tags) throws Exception {
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rset = null;
		int i = 0;
		try {
			conn = DriverManager.getConnection(url);
			stmt = conn.createStatement();
			rset = stmt.executeQuery("SELECT tagname FROM configtags c order by tagname;");
			while(rset.next()) {
				tags.add(rset.getString("tagname"));
			}
			
			
		} catch (Exception exe) {
			
			LOGGER.error("Couldn't retrieve Tag Details in ConfigurationDbAccessor" + exe);
			exe.printStackTrace();
		} finally {
			if (rset != null) {
				rset.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}

			if (conn != null) {
				conn.close();
			}
		}
		
	}
	
	public void saveSemanticTagDetails(HashMap <Integer, Combo> comboFields) throws SQLException{
		Connection conn = null;
		Statement stmt = null;
		
		int count = comboFields.size();
		try {
			conn = DriverManager.getConnection(url);
			stmt = conn.createStatement();
			for (int i= 1; i<= count; i++) {
				Integer key = new Integer(i);
				Combo combo = comboFields.get(key);
				String tagName = combo.getText().trim();
				try {
					if(!tagName.equals("")) {
						stmt.executeUpdate("insert into configtags (tagname, marker) values " +
								"('" + tagName + "', 'U');");
					}
				} catch (Exception exe) {
					LOGGER.error("Couldn't insert into configtags", exe);
					exe.printStackTrace();
				}
			}
			
		} catch (Exception exe) {
			LOGGER.error("Couldn't insert Tag Details in ConfigurationDbAccessor:saveSemanticTagDetails" + exe);
			exe.printStackTrace();
		} finally {

			if (stmt != null) {
				stmt.close();
			}

			if (conn != null) {
				conn.close();
			}
		}

	}

}
