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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import fna.parsing.ApplicationUtilities;
import fna.parsing.ParsingException;

public class VolumeMarkupDbAccessor {

	/**
	 * @param args
	 */
    private static final Logger LOGGER = Logger.getLogger(VolumeMarkupDbAccessor.class);
    private static String url = ApplicationUtilities.getProperty("database.url");
	private String tablePrefix = null ;
	private String glossarytable;
    static {
		try {
			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Couldn't find Class in MainFormDbAccessor" + e);
			e.printStackTrace();
		}
	}
    
    public VolumeMarkupDbAccessor(String dataPrefix, String glossarytable){
    	this.tablePrefix = dataPrefix;
    	this.glossarytable = glossarytable;
    }
	
    //public VolumeMarkupDbAccessor(){}
    
    public void updateData(List <String> tagList) throws  ParsingException, SQLException {
	 
	 Connection conn = null;
	 PreparedStatement stmt = null;
	 ResultSet rs = null;
	 
		try {
			conn = DriverManager.getConnection(url);
			//Populate Structure Table Hong TODO 5/23/11
			String sql = "select distinct tag from "+this.tablePrefix+"_sentence where tag != 'unknown' and tag is not null and tag not like '% %' and tag not in (select distinct term from "+this.glossarytable+")";
			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();
			
			while (rs.next()) {
				String tag = rs.getString("tag");
				tagList.add(tag);
			}

		} catch (SQLException sqlexe) {
			LOGGER.error("Couldn't update sentence table in VolumeMarkupDbAccessor:updateData", sqlexe);
			sqlexe.printStackTrace();
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
	/**
	 * no longer used
	 * @return
	 * @throws SQLException
	 */
	public ArrayList<String> getDescriptorWords() throws SQLException {
		
		ArrayList<String> words = new ArrayList<String>();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			conn = DriverManager.getConnection(url);
			stmt = conn.prepareStatement("select word from "+this.tablePrefix+"_wordpos4parser where pos=? and word not in (select distinct term from "+this.glossarytable+")");
			stmt.setString(1, "b");
			rset = stmt.executeQuery();
			if (rset != null) {
				while(rset.next()){
					words.add(rset.getString("word"));
				}	
			}			
		} catch (SQLException exe) {
			LOGGER.error("Error in getting words as descriptors: " +
					"mainFormDbAccessor.getDescriptorWords", exe);
		} finally {
			if(rset != null) {
				rset.close();
			}
			
			if(stmt != null) {
				stmt.close();
			}
			
			if(conn != null){
				conn.close();
			}
		}
		
		return words;
	}
	
	
	
public ArrayList<String> getSavedDescriptorWords() throws SQLException {
		
		ArrayList<String> words = new ArrayList<String>();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			conn = DriverManager.getConnection(url);
			//Populate descriptor Hong TODO 5/23/11
			//stmt = conn.prepareStatement("select word from "+this.tablePrefix+"_wordpos4parser where pos=? and word not in (select distinct term from "+this.glossarytable+") and saved_flag not in ('green','red')");
			stmt = conn.prepareStatement("select word from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+" where pos=? and word not in (select word from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+") and saved_flag not in ('red')");
			stmt.setString(1, "b");
			rset = stmt.executeQuery();
			if (rset != null) {
				while(rset.next()){
					words.add(rset.getString("word"));
				}	
			}			
		} catch (SQLException exe) {
			LOGGER.error("Error in getting words as descriptors: " +
					"mainFormDbAccessor.getDescriptorWords", exe);
		} finally {
			if(rset != null) {
				rset.close();
			}
			
			if(stmt != null) {
				stmt.close();
			}
			
			if(conn != null){
				conn.close();
			}
		}
		
		return words;
	}
    public static void main(String[] args)throws Exception {
		// TODO Auto-generated method stub
		//System.out.println(DriverManager.getConnection(url));
	}

	public ArrayList<ArrayList> getUnSavedDescriptorWords() throws SQLException {
		
		ArrayList<String> words = new ArrayList<String>();
		ArrayList<String> flag = new ArrayList<String>();
		ArrayList<ArrayList> wordsAndFlag = new ArrayList<ArrayList>();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			conn = DriverManager.getConnection(url);
			//Populate descriptor Hong TODO 5/23/11
			//stmt = conn.prepareStatement("select word from "+this.tablePrefix+"_wordpos4parser where pos=? and word not in (select distinct term from "+this.glossarytable+") and saved_flag not in ('green','red')");
			stmt = conn.prepareStatement("select word,saved_flag from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+" where pos=? and word not in (select word from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+")");
			stmt.setString(1, "b");
			rset = stmt.executeQuery();
			if (rset != null) {
				while(rset.next()){
					words.add(rset.getString("word"));
					flag.add(rset.getString("saved_flag"));
				}	
			}			
		} catch (SQLException exe) {
			LOGGER.error("Error in getting words as descriptors: " +
					"mainFormDbAccessor.getDescriptorWords", exe);
		} finally {
			if(rset != null) {
				rset.close();
			}
			
			if(stmt != null) {
				stmt.close();
			}
			
			if(conn != null){
				conn.close();
			}
		}
		wordsAndFlag.add(words);
		wordsAndFlag.add(flag);
		return wordsAndFlag;
	}
}
