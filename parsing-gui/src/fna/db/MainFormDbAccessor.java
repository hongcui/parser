/**
 * MainFormDbAccessor.java
 *
 * Description : This performs all the database access needed by the MainForm
 * Version     : 1.0
 * @author     : Partha Pratim Sanyal
 * Created on  : Aug 29, 2009
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
import java.sql.Statement;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;


import fna.parsing.ApplicationUtilities;
import fna.parsing.MainForm;
import fna.parsing.ParsingException;



public class MainFormDbAccessor {


	private static final Logger LOGGER = Logger.getLogger(MainFormDbAccessor.class);
	
	 
	public static void main(String[] args) throws Exception {

		Connection conn = DriverManager.getConnection(url);
		System.out.println(conn);
		conn.close(); 
	}
	
	private static String url = ApplicationUtilities.getProperty("database.url");
	
	static {
		Statement stmt = null;
		Connection conn = null;
		try {
			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Couldn't find Class in MainFormDbAccessor" + e);
			e.printStackTrace();
		} 
	}
	
	public void removeMarkUpData(List <String> removedTags) throws ParsingException, SQLException {
		
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			
				//Class.forName(driverPath);
				conn = DriverManager.getConnection(url);
				String tablePrefix = MainForm.dataPrefixCombo.getText();
				String sql = "update "+tablePrefix+"_sentence set tag = 'unknown' where tag = ?";
				stmt = conn.prepareStatement(sql);
				
				for (String tag : removedTags) {
					stmt.setString(1, tag);
					stmt.executeUpdate();
				}
				


		} catch (SQLException sqlexe) {
			LOGGER.error("Couldn't update sentence table in MainFormDbAccessor:removeMarkUpData", sqlexe);
			sqlexe.printStackTrace();
			throw new ParsingException("Error Accessing the database" , sqlexe);
			
		} /*catch (ClassNotFoundException clexe) {
			LOGGER.error("Couldn't load the db Driver in MainFormDbAccessor:removeMarkUpData", clexe);
			throw new ParsingException("Couldn't load the db Driver" , clexe);
			
		} */finally {
			if (stmt != null) {
				stmt.close();
			}
			
			if (conn != null) {
				conn.close();
			}			
			
		}
	}
	
	public void loadTagsData(Combo tagListCombo, Combo modifierListCombo) throws ParsingException, SQLException {
		ResultSet rs = null;
		PreparedStatement stmt = null;
		PreparedStatement stmt_select = null;
		Connection conn = null;
		try {
			//Class.forName(driverPath);
			conn = DriverManager.getConnection(url);
			String tablePrefix = MainForm.dataPrefixCombo.getText();
			String sql = "select distinct tag from "+tablePrefix+"_sentence where tag != 'unknown' and tag is not null order by tag asc";
			stmt = conn.prepareStatement(sql);
			
			rs = stmt.executeQuery();
			while (rs.next()) {
				String tag = rs.getString("tag");
				tagListCombo.add(tag);
			}

			sql = "select distinct modifier from "+tablePrefix+"_sentence where modifier is not null order by modifier asc";
			stmt_select = conn.prepareStatement(sql);			
			rs = stmt_select.executeQuery();
			
			while (rs.next()) {
				String mod = rs.getString("modifier");
				modifierListCombo.add(mod);
			}
			

		} catch (SQLException exe) {
			LOGGER.error("Couldn't execute db query in MainFormDbAccessor:loadTagsData", exe);
			exe.printStackTrace();
			throw new ParsingException("Failed to execute the statement.", exe);
		} /*catch (ClassNotFoundException clex) {
			LOGGER.error("Couldn't load the db Driver in MainFormDbAccessor:loadTagsData", clex);
			throw new ParsingException("Couldn't load the db Driver" , clex);			
		}*/ finally {
			if (rs != null) {
				rs.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
			if (stmt_select != null) {
				stmt_select.close();
			}
			if (conn != null) {
				conn.close();
			}
			
		}
	}
	
	public void loadTagsTableData(Table tagTable) throws ParsingException, SQLException {
		
		PreparedStatement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		
		try {
			//Class.forName(driverPath);
			conn = DriverManager.getConnection(url);
			String tablePrefix = MainForm.dataPrefixCombo.getText();
			String sql = "select * from "+tablePrefix+"_sentence where tag = 'unknown' order by sentence";
			stmt = conn.prepareStatement(sql);
			
			int i = 0;
			rs = stmt.executeQuery();
			while (rs.next()) {
				String sentid = rs.getString("sentid");
				String tag = rs.getString("tag");
				String sentence = rs.getString("sentence");
				
				TableItem item = new TableItem(tagTable, SWT.NONE);
			    item.setText(new String[]{++i+"", sentid,"", tag, sentence});
			}

		} catch (SQLException exe) {
			LOGGER.error("Couldn't execute db query in MainFormDbAccessor:loadTagsTableData", exe);
			exe.printStackTrace();
			throw new ParsingException("Failed to execute the statement.", exe);
		} /*catch (ClassNotFoundException clex) {
			LOGGER.error("Couldn't load the db Driver in MainFormDbAccessor:loadTagsTableData", clex);
			throw new ParsingException("Couldn't load the db Driver" , clex);			
		}*/ finally {
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
	
	public void updateContextData(int sentid, StyledText contextStyledText) throws SQLException, ParsingException {
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		String min = "" + (sentid - 2);
		String max = "" + (sentid + 2);
		
		
		try {
			//Class.forName(driverPath);
			conn = DriverManager.getConnection(url);
			String tablePrefix = MainForm.dataPrefixCombo.getText();
			String sql = "select * from "+tablePrefix+"_sentence where sentid > ? and sentid < ?";
			stmt = conn.prepareStatement(sql);
			
			stmt.setString(1, min);
			stmt.setString(2, max);
			
			rs = stmt.executeQuery();
			while (rs.next()) {
				String sid = rs.getString("sentid");
				String tag = rs.getString("tag");
				String sentence = rs.getString("sentence");
				
				int start = contextStyledText.getText().length();
				
				contextStyledText.append(sentence + "\r\n");
				if (Integer.parseInt(sid) == sentid) {
					StyleRange styleRange = new StyleRange();
					styleRange.start = start;
					styleRange.length = sentence.length();
					styleRange.fontStyle = SWT.BOLD;
					// styleRange.foreground = display.getSystemColor(SWT.COLOR_BLUE);
					contextStyledText.setStyleRange(styleRange);
				}
			}

		} catch (SQLException exe) {
			LOGGER.error("Couldn't execute db query in MainFormDbAccessor:updateContextData", exe);
			exe.printStackTrace();
			throw new ParsingException("Failed to execute the statement.", exe);
		} /*catch (ClassNotFoundException clex) {
			LOGGER.error("Couldn't load the db Driver in MainFormDbAccessor:updateContextData", clex);
			throw new ParsingException("Couldn't load the db Driver" , clex);			
		}*/ finally {
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
	
	public void saveTagData(Table tagTable) throws ParsingException, SQLException{
		
		Connection conn = null;
		PreparedStatement stmt = null;
		PreparedStatement stmt_update = null;
		ResultSet rs = null;
		
		try {
			//Class.forName(driverPath);
			conn = DriverManager.getConnection(url);
			
			
			for (TableItem item : tagTable.getItems()) {
				String sentid = item.getText(1);
				String modifier = item.getText(2);
				String tag = item.getText(3);
				
				if (tag.equals("unknown"))
					continue;
				
				if(tag.equals("PART OF LAST SENTENCE")){//find tag of the last sentence
					String tablePrefix = MainForm.dataPrefixCombo.getText();
					String sql = "select tag from "+tablePrefix+"_sentence where sentid ="+(Integer.parseInt(sentid)-1);
					stmt = conn.prepareStatement(sql);
					rs = stmt.executeQuery();
					rs.next();
					tag = rs.getString("tag");
				}
				String tablePrefix = MainForm.dataPrefixCombo.getText();
				String sql = "update "+tablePrefix+"_sentence set modifier = ?, tag = ? where sentid = ?";
				stmt_update = conn.prepareStatement(sql);
				stmt_update.setString(1, modifier);
				stmt_update.setString(2, tag);
				stmt_update.setString(3, sentid);
				
				stmt_update.executeUpdate();
			}

		} catch (SQLException exe) {
			LOGGER.error("Couldn't execute db query in MainFormDbAccessor:saveTagData", exe);
			exe.printStackTrace();
			throw new ParsingException("Failed to execute the statement.", exe);
		} finally {
			if (rs != null) {
				rs.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
			if (stmt_update != null) {
				stmt_update.close();
			}
			
			if (conn != null) {
				conn.close();
			}
						
		}
	}
	
	public void datasetPrefixRetriever(List <String> datasetPrefixes) 
		throws ParsingException, SQLException{
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rset = null;
		
		String createprefixTable = "CREATE TABLE  if not exists datasetprefix (" +
				 "prefix varchar(20) NOT NULL DEFAULT '', "+
				  "time_last_accessed timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
				  "tab_general varchar(1) DEFAULT NULL, "+
				  "tab_segm varchar(1) DEFAULT NULL, "+
				  "tab_verf varchar(1) DEFAULT NULL, "+
				  "tab_trans varchar(1) DEFAULT NULL, "+
				  "tab_struct varchar(1) DEFAULT NULL, "+
				  "tab_unknown varchar(1) DEFAULT NULL, "+
				  "tab_finalm varchar(1) DEFAULT NULL, "+
				  "tab_gloss varchar(1) DEFAULT NULL, "+
				  "PRIMARY KEY (prefix) ) " ; 
		
		try {
			conn = DriverManager.getConnection(url);
			stmt = conn.createStatement();
			stmt.execute(createprefixTable);
			rset = stmt.executeQuery("select * from datasetprefix order by time_last_accessed desc");
			while (rset.next()) {
				datasetPrefixes.add(rset.getString("prefix"));
			}
		}catch (SQLException exe) {
			LOGGER.error("Couldn't execute db query in MainFormDbAccessor:datasetPrefixRetriever", exe);
			exe.printStackTrace();
			throw new ParsingException("Failed to execute the statement.", exe);
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
	
	public String getLastAccessedDataSet() 
	throws ParsingException, SQLException{
	
	Connection conn = null;
	Statement stmt = null;
	ResultSet rset = null;
	String recent = null;
	
	try {
		conn = DriverManager.getConnection(url);
		stmt = conn.createStatement();
		rset = stmt.executeQuery("select * from datasetprefix order by time_last_accessed desc");
		if (rset.next()) {
			recent =  rset.getString("prefix");
		}
	}catch (SQLException exe) {
		LOGGER.error("Couldn't execute db query in MainFormDbAccessor:datasetPrefixRetriever", exe);
		exe.printStackTrace();
		throw new ParsingException("Failed to execute the statement.", exe);
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
	
	return recent;
 }

	public void savePrefixData(String prefix) 
	throws ParsingException, SQLException{
	
	Connection conn = null;
	PreparedStatement stmt = null;
	ResultSet rset = null;
	
	try {
		if (!prefix.equals("")) {
			conn = DriverManager.getConnection(url);		
			stmt = conn.prepareStatement("insert into datasetprefix values ('"+ 
					prefix + "', current_timestamp, 0, 0, 0 ,0, 0, 0, 0, 0)");
			stmt.executeUpdate();
		}
	}catch (SQLException exe) {
		
		if (exe.getMessage().contains("key 'PRIMARY'")) {
			stmt = conn.prepareStatement("update datasetprefix set time_last_accessed = current_timestamp" +
					" where prefix = '" + prefix + "'" ); 
			stmt.executeUpdate();
		}
		if (!exe.getMessage().contains("key 'PRIMARY'")) {
			LOGGER.error("Couldn't execute db query in MainFormDbAccessor:savePrefixdata", exe);
			throw new ParsingException("Failed to execute the statement.", exe);
		}
		
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
	public void loadStatusOfMarkUp(boolean [] markUpStatus, String dataPrefix) 
		throws 	ParsingException, SQLException {
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			if(dataPrefix != null && !dataPrefix.equals("")) {
				conn = DriverManager.getConnection(url);
				stmt = conn.prepareStatement("select * from datasetprefix where prefix ='" + dataPrefix + "'");
				rset = stmt.executeQuery();
				if (rset != null && rset.next()) {
					
					/* Segmentation tab */
					if (rset.getInt("tab_segm") == 0) {
						markUpStatus[1] = false;
					} else {
						markUpStatus[1] = true;
					}
					
					/* Verification tab */
					if (rset.getInt("tab_verf") == 0) {
						markUpStatus[2] = false;
					} else {
						markUpStatus[2] = true;
					}
					
					/* Transformation tab */
					if (rset.getInt("tab_trans") == 0) {
						markUpStatus[3] = false;
					} else {
						markUpStatus[3] = true;
					}
					
					/* Structure Name Correction tab */
					if (rset.getInt("tab_struct") == 0) {
						markUpStatus[4] = false;
					} else {
						markUpStatus[4] = true;
					}
					
					/* Unknown removal tab */
					if (rset.getInt("tab_unknown") == 0) {
						markUpStatus[5] = false;
					} else {
						markUpStatus[5] = true;
					}
					
					/* Finalizer tab */
					if (rset.getInt("tab_finalm") == 0) {
						markUpStatus[6] = false;
					} else {
						markUpStatus[6] = true;
					}
					
					/* Glossary tab */
					if (rset.getInt("tab_gloss") == 0) {
						markUpStatus[7] = false;
					} else {
						markUpStatus[7] = true;
					}
					
					
				}
			}
		} catch (Exception exe) {
			LOGGER.error("Error loading saved status",exe);
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
	
	public void saveStatus(String tab, String prefix, boolean status) throws SQLException {
		
		Connection conn = null;
		PreparedStatement stmt = null;
		int tabStatus = 0;
		//Lookup
		{
			if (tab.equals(ApplicationUtilities.getProperty("tab.one.name"))) {
				tab = "tab_general";
			}
			if (tab.equals(ApplicationUtilities.getProperty("tab.two.name"))) {
				tab = "tab_segm";
			}
			if (tab.equals(ApplicationUtilities.getProperty("tab.three.name"))) {
				tab = "tab_verf";
			}
			if (tab.equals(ApplicationUtilities.getProperty("tab.four.name"))) {
				tab = "tab_trans";
			}
			if (tab.equals(ApplicationUtilities.getProperty("tab.five.name"))) {
				tab = "tab_struct";
			}
			if (tab.equals(ApplicationUtilities.getProperty("tab.six.name"))) {
				tab = "tab_unknown";
			}
			if (tab.equals(ApplicationUtilities.getProperty("tab.seven.name"))) {
				tab = "tab_finalm";
			}
			if (tab.equals(ApplicationUtilities.getProperty("tab.eight.name"))) {
				tab = "tab_gloss";
			}
		}
		
		 if (status == true) {
			 tabStatus = 1; 
		 } 
		
		try {
			conn = DriverManager.getConnection(url);
			stmt = conn.prepareStatement("update datasetprefix set " + tab + "= " + tabStatus + 
					" where prefix='" + prefix +"'");
			stmt.executeUpdate();
			
		} catch (Exception exe) {
			LOGGER.error("Unable to save status", exe);
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
