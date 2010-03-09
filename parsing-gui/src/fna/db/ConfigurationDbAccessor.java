package fna.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import fna.beans.ExpressionBean;
import fna.beans.NomenclatureBean;
import fna.beans.SpecialBean;
import fna.beans.TextBean;
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
	
	public void saveSemanticTagDetails(HashMap <Integer, Combo> comboFields, 
			HashMap <Integer, Button> checkFields) throws SQLException{
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
				Button button = checkFields.get(key);
				boolean checked = button.getSelection();
				if(!tagName.equals("")) {
					if(checked) {
						stmt.executeUpdate("insert into configtags (tagname, marker, startStyle) values " +
								"('" + tagName + "', 'U', 'Y');");
					} else {
						stmt.executeUpdate("insert into configtags (tagname, marker) values " +
								"('" + tagName + "', 'U');");
					}

				}
			}

		} catch (Exception exe) {
			if(!exe.getMessage().contains("Duplicate entry")) {
			 LOGGER.error("Couldn't insert Tag Details in ConfigurationDbAccessor:saveSemanticTagDetails" + exe);
			 exe.printStackTrace();
			}
		} finally {

			if (stmt != null) {
				stmt.close();
			}

			if (conn != null) {
				conn.close();
			}
		}

	}
	
	
	public void saveParagraphTagDetails(String...paragraphs) throws SQLException{
		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = DriverManager.getConnection(url);
			stmt = conn.prepareStatement("insert into ocrstartparagraph(paragraph) values (?)");

			for (String para : paragraphs) {
				if(!para.trim().equals("") && !para.trim().equals("\r")) {
					stmt.setString(1, para);
					stmt.executeUpdate();
				}
			}

		} catch (Exception exe) {
			 LOGGER.error("Couldn't insert paragraph Details in ConfigurationDbAccessor:saveParagraphTagDetails" + exe);
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
	
	public boolean saveType2Details(TextBean textBean, HashMap <Integer, NomenclatureBean> nomenclatures, 
			HashMap <Integer, ExpressionBean> expressions, HashMap <Integer, Text> descriptions, HashMap <Integer, Label> sections, 
			SpecialBean special, HashMap <String, Text> abbreviations) throws SQLException {
		

		PreparedStatement pstmt = null;
		Connection conn = null;
		boolean success = false;
		
		try {
			conn = DriverManager.getConnection(url);
			//Insert the data from the first tab
			
			String query = "insert into configtype2text (firstpara, leadingIntend, spacing, avglength, pgNoForm," +
					"capitalized, allcapital, sectionheading, hasfooter, hasHeader, footerToken, headertoken) " +
					"values (?,?,?,?,?,?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, textBean.getFirstPara().getText());
			pstmt.setString(2, textBean.getLeadingIndentation().getText());
			pstmt.setString(3, textBean.getSpacing().getText());
			pstmt.setString(4, textBean.getEstimatedLength().getText());
			pstmt.setString(5, textBean.getPageNumberFormsText().getText());
			pstmt.setString(6, textBean.getSectionHeadingsCapButton().getSelection()?"Y":"N");
			pstmt.setString(7, textBean.getSectionHeadingsAllCapButton().getSelection()?"Y":"N");
			pstmt.setString(8, textBean.getSectionHeadingsText().getText());
			SpecialBean  splBean = textBean.getFooterHeaderBean();
			pstmt.setString(9, splBean.getFirstButton().getSelection()?"Y":"N");
			pstmt.setString(10, splBean.getSecondButton().getSelection()?"Y":"N");
			pstmt.setString(11, splBean.getFirstText().getText());
			pstmt.setString(12, splBean.getSecondText().getText());			
			pstmt.execute();
			success = true;
			
		} catch (SQLException exe) {
			LOGGER.error("Couldn't insert type2 Details in ConfigurationDbAccessor:saveType2Details" + exe);
			exe.printStackTrace();
			
		} finally {
			if (pstmt != null) {
				pstmt.close();
			} 
			if (conn != null) {
				conn.close();
			}
		}
		
		
		
		return success;
	}
	
	public boolean retrieveType2Details(TextBean textBean, HashMap <Integer, NomenclatureBean> nomenclatures, 
			HashMap <Integer, ExpressionBean> expressions, HashMap <Integer, Text> descriptions, HashMap <Integer, Label> sections, 
			SpecialBean special, HashMap <String, Text> abbreviations) throws SQLException {
		return true;
	}

}
