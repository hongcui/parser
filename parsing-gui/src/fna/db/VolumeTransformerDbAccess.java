/**
 * VolumeTransformerDbAccess.java
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import fna.parsing.ApplicationUtilities;
import fna.parsing.MainForm;
import fna.parsing.ParsingException;

public class VolumeTransformerDbAccess {

	/**
	 * @param args
	 */
    private static final Logger LOGGER = Logger.getLogger(VolumeTransformerDbAccess.class);
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
	
	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		System.out.println(DriverManager.getConnection(url));

	}
	
	public void add2TaxonTable(String taxonnumber, String name, String rank, int index) throws SQLException, ParsingException{
		
		Connection conn = null;
		Statement stmt = null;
		
		//System.out.println("add to taxon table:"+name+" "+rank+" "+index);
		if(name == null || name.compareTo("") == 0){
			return;
		}
		//dataset specific taxonTable Name
		String taxonTableName = MainForm.dataPrefixCombo.getText().trim()+"_"
		+ ApplicationUtilities.getProperty("taxontable");
		try{
			conn = DriverManager.getConnection(url);
			stmt = conn.createStatement();
			stmt.execute("insert into "+taxonTableName+" values ('"+taxonnumber+"', '"+name+"', '"+rank+"', '"+index+"')");
			
		}catch (SQLException sqlexe) {
			LOGGER.error("database access error in VolumeTransformerDbAccess:add2TaxonTable", sqlexe);
			sqlexe.printStackTrace();
			throw new ParsingException("Error Accessing the database" , sqlexe);
			
		} finally {
			
			
			if (stmt != null) {
				stmt.close();
			}
			
			if (conn != null) {
				conn.close();
			}			
			
		}

	}
	
	public void add2AuthorTable(String authority) throws SQLException, ParsingException{
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		// dataset specific author table name
		String authorTableName = MainForm.dataPrefixCombo.getText().trim()+"_"
									+ ApplicationUtilities.getProperty("authortable");
		
		try{
			conn = DriverManager.getConnection(url);
			stmt = conn.createStatement();
			
			rs = stmt.executeQuery("select authority from " +authorTableName + 
					"  where authority = '"+authority+"'");
			if(!rs.next()){
				stmt.execute("insert into " +authorTableName +"  values ('"+authority+"')");
			}
		} catch (SQLException sqlexe) {
			LOGGER.error("Couldn't update in VolumeTransformerDbAccess:add2AuthorTable", sqlexe);
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
	
	public void add2PublicationTable(String  publication) throws SQLException, ParsingException{
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		String publicationTableName = MainForm.dataPrefixCombo.getText().trim()+ "_"
										+ ApplicationUtilities.getProperty("publicationtable");
		
		publication = publication.replaceFirst("\\d.*", "");
		try{
			conn = DriverManager.getConnection(url);
			stmt = conn.createStatement();
			
			rs = stmt.executeQuery("select publication from " + 
					publicationTableName + " where publication = '"+publication+"'");
			if(!rs.next()){
				stmt.execute("insert into " + 
					publicationTableName + "  values ('"+publication+"')");
			}
		} catch (SQLException sqlexe) {
			LOGGER.error("Couldn't update in VolumeTransformerDbAccess:add2AuthorTable", sqlexe);
			throw new ParsingException("Error Accessing the database" , sqlexe);
			
		}  finally {
			
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
