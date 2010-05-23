package fna.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import fna.beans.ContextBean;
import fna.beans.TermsDataBean;
import fna.parsing.ApplicationUtilities;
import fna.parsing.MainForm;

public class CharacterStateDBAccess {

	/**
	 * @param args
	 */
	private static final Logger LOGGER = Logger.getLogger(CharacterStateDBAccess.class);
	
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
		Connection conn = DriverManager.getConnection(url);
		System.out.println(conn);

	}
	
	public void getDecisionCategory(ArrayList<String> decisions) throws SQLException {
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			
				conn = DriverManager.getConnection(url);
				String tablePrefix = MainForm.dataPrefixCombo.getText();
				
				String sql = "SELECT distinct category FROM " + tablePrefix+
						"_character order by category";
				stmt = conn.prepareStatement(sql);
				
				rset = stmt.executeQuery();
				while(rset.next()) {
					decisions.add(rset.getString(1));
				}
				
				
		} catch (Exception exe) {
			LOGGER.error("Couldn't execute db query in CharacterStateDBAccess:getDecisionCategory", exe);
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
	
	public ArrayList<TermsDataBean> getTerms(String group) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		group = group.substring(group.indexOf("_")+1);
		ArrayList<TermsDataBean > coOccurrences = new ArrayList<TermsDataBean>();
		try {
			
			conn = DriverManager.getConnection(url);
			String tablePrefix = MainForm.dataPrefixCombo.getText();
			String sql = "select * from " + tablePrefix +"_grouped_foc_terms " +
					"where groupId=" + group+ " order by frequency desc";
			
			pstmt = conn.prepareStatement(sql);
			rset = pstmt.executeQuery();
			while(rset.next()) {
				TermsDataBean  tbean = new TermsDataBean();
				tbean.setGroupId(rset.getInt("groupId"));
				tbean.setTerm1(rset.getString("term"));
				tbean.setTerm2(rset.getString("cooccurTerm"));
				tbean.setFrequency(rset.getInt("frequency"));
				String files = rset.getString("sourceFiles");
				String [] sourceFiles = files.split(",");
				tbean.setSourceFiles(sourceFiles);
				tbean.setKeep(rset.getString("keep"));
				coOccurrences.add(tbean);
			}
			
			
		} catch (Exception exe) {
			LOGGER.error("Couldn't execute db query in CharacterStateDBAccess:getTerms", exe);
			exe.printStackTrace();
			
		} finally {
			if (rset != null) {
				rset.close();
			}
			
			if (pstmt != null) {
				pstmt.close();
			}
			
			if (conn != null) {
				conn.close();
			}
			
		}
		return coOccurrences;
		
	}
	
	public ArrayList<ContextBean> getContext(String [] sourceFiles) throws Exception {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		ArrayList<ContextBean> contexts = new ArrayList<ContextBean>();
		String sql = "SELECT source, originalsent FROM "+ 
			MainForm.dataPrefixCombo.getText().trim() +"_sentence where source in (";
		for (String source : sourceFiles) {
			sql += "'" + source + "',";
		}
		
		sql = sql.substring(0, sql.lastIndexOf(",")) + ")";
		try {
			conn = DriverManager.getConnection(url);
			pstmt = conn.prepareStatement(sql);
			rset = pstmt.executeQuery();
			
			while(rset.next()){
				ContextBean cbean = new ContextBean(rset.getString("source"), rset.getString("originalsent"));
				contexts.add(cbean);
			}
						
		} catch (Exception exe) {
			LOGGER.error("Couldn't execute db query in CharacterStateDBAccess:getTerms", exe);
			exe.printStackTrace();
			
		} finally {
			if (rset != null) {
				rset.close();
			}
			
			if (pstmt != null) {
				pstmt.close();
			}
			
			if (conn != null) {
				conn.close();
			}
			
		}
		return contexts;
	}
	
	public boolean saveTerms(ArrayList<TermsDataBean> terms) throws SQLException {
		
		Connection conn = null;
		PreparedStatement pstmt = null; 
		String sql = "delete from " + MainForm.dataPrefixCombo.getText().trim() +"_grouped_foc_terms where groupId=?";
		try {
			conn = DriverManager.getConnection(url);
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, terms.get(1).getGroupId());
			pstmt.execute();
			
			sql = "insert into " + MainForm.dataPrefixCombo.getText().trim() +"_grouped_foc_terms values(?,?,?,?,?,?)";
			
			pstmt = conn.prepareStatement(sql);
			
			for (TermsDataBean tbean : terms) {
				pstmt.setInt(1, tbean.getGroupId());
				pstmt.setString(2, tbean.getTerm1());
				pstmt.setString(3, tbean.getTerm2());
				pstmt.setInt(4, tbean.getFrequency());
				pstmt.setString(5, tbean.getKeep());
				
				String [] files = tbean.getSourceFiles();
				String sourceFile = "";
				for (String file : files) {
					sourceFile += file + ",";
				}
				sourceFile = sourceFile.substring(0, sourceFile.lastIndexOf(","));
				
				pstmt.setString(6, sourceFile);
				pstmt.addBatch();
			}
			
			pstmt.executeBatch();
			
		} catch (Exception exe) {
			LOGGER.error("Couldn't execute db query in CharacterStateDBAccess:saveTerms", exe);
			exe.printStackTrace();
			
		} finally {
			
			if (pstmt != null) {
				pstmt.close();
			}
			
			if (conn != null) {
				conn.close();
			}
			
		}
		
		return true;
	}
	
	public String getDecision(int groupId) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		String decision = "";
		ResultSet rset = null;
		String sql = "select decision from " + MainForm.dataPrefixCombo.getText().trim() +"_group_decisions where groupId=?" ;
		try {
			conn = DriverManager.getConnection(url);
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, groupId);
			rset = pstmt.executeQuery();
			if(rset.next()) {
				decision = rset.getString(1);
			}
		} catch (Exception exe) {
			LOGGER.error("Couldn't execute db query in CharacterStateDBAccess:getDecision", exe);
			exe.printStackTrace();
		} finally {
			if (rset != null) {
				rset.close();
			}
			if (pstmt != null) {
				pstmt.close();
			}
			
			if (conn != null) {
				conn.close();
			}
			
		}
		
		return decision;
		
	}
	
	public ArrayList<String> getProcessedGroups() throws SQLException {
		ArrayList<String> processedGroups = new ArrayList<String>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "select groupId from " + MainForm.dataPrefixCombo.getText().trim() +"_group_decisions order by groupId";
		ResultSet rset = null;
		try {
			conn = DriverManager.getConnection(url);
			pstmt = conn.prepareStatement(sql);
			rset = pstmt.executeQuery();
			
			while (rset.next()){
				processedGroups.add("Group_"+rset.getInt(1));
			}
			
		} catch (Exception exe) {
			LOGGER.error("Couldn't execute db query in CharacterStateDBAccess:getProcessedGroups", exe);
			exe.printStackTrace();
		} finally {
			if (rset != null) {
				rset.close();
			}
			if (pstmt != null) {
				pstmt.close();
			}
			
			if (conn != null) {
				conn.close();
			}
			
		}
		return processedGroups;
	}
	
	public boolean saveDecision(int groupId, String decision) throws SQLException {
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "delete from " + MainForm.dataPrefixCombo.getText().trim() +"_group_decisions where groupId=?" ;
		try {
			
			/*Delete existing information */
			conn = DriverManager.getConnection(url);
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, groupId);
			pstmt.execute();
			
			/* Insert the new decision */
			sql = "insert into " + MainForm.dataPrefixCombo.getText().trim() +"_group_decisions values (?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, groupId);
			pstmt.setString(2, decision);
			pstmt.execute();
			
		} catch (Exception exe) {
			LOGGER.error("Couldn't execute db query in CharacterStateDBAccess:saveDecision", exe);
			exe.printStackTrace();
		} finally {
			if (pstmt != null) {
				pstmt.close();
			}
			
			if (conn != null) {
				conn.close();
			}
			
		}
		return true;
	}

}
