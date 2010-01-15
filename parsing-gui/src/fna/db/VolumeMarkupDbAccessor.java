/**
 * VolumeMarkupDbAccessor.java
 *
 * Description : This performs all the database access needed by the VolumeMarkup
 * Version     : 1.0
 * @author     : Partha Pratim Sanyal
 * Created on  : September 11, 2009
 *
 * Modification History :
 * Date   | Version  |  Author  | Comments
 *
 * Confidentiality Notice :
 * This software is the confidential and,
 * proprietary information of The University of Arizona.
 */

package fna.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import fna.parsing.ApplicationUtilities;
import fna.parsing.ParsingException;
import fna.parsing.Registry;

public class VolumeMarkupDbAccessor {

	/**
	 * @param args
	 */
    private static final Logger LOGGER = Logger.getLogger(VolumeMarkupDbAccessor.class);
    private static String url = ApplicationUtilities.getProperty("database.url");
	static {
		try {
			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Couldn't find Class in MainFormDbAccessor" + e);
			e.printStackTrace();
		}
	}
	public static void main(String[] args)throws Exception {
		// TODO Auto-generated method stub
		//System.out.println(DriverManager.getConnection(url));
	}
	
 public void updateData(List <String> tagList) throws  ParsingException, SQLException {
	 
	 Connection conn = null;
	 PreparedStatement stmt = null;
	 ResultSet rs = null;
	 
		try {
			conn = DriverManager.getConnection(url);
			
			String sql = "select distinct tag from sentence where tag != 'unknown' and tag is not null";
			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();
			
			while (rs.next()) {
				String tag = rs.getString("tag");
				tagList.add(tag);
			}

		} catch (SQLException sqlexe) {
			LOGGER.error("Couldn't update sentence table in VolumeMarkupDbAccessor:updateData", sqlexe);
			throw new ParsingException("Error Accessing the database" , sqlexe);
			
		} finally {
			
			if (rs != null) {
				rs.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
			if (conn != null) {
				conn.close();
			}			
			
		}
	 
 }
}
