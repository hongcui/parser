package fna.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import fna.beans.DescriptionBean;
import fna.beans.ExpressionBean;
import fna.beans.NomenclatureBean;
import fna.beans.SectionBean;
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
			HashMap <Integer, ExpressionBean> expressions, DescriptionBean descriptionBean, 
			SpecialBean special, HashMap <String, Text> abbreviations) throws SQLException {
		

		PreparedStatement pstmt = null;
		Connection conn = null;
		boolean success = false;
		
		try {
			conn = DriverManager.getConnection(url);
			/* Insert the data from the first tab 
			 * use TextBean
			 * */
			pstmt = conn.prepareStatement("delete from configtype2text");
			pstmt.execute();
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
			
			/* Save Nomenclature tab now - Use nomenclatures*/
			pstmt = conn.prepareStatement("delete from nomenclatures");
			pstmt.execute();
			query = "insert into nomenclatures (nameLabel, _yes, _no, description, _type) values (?,?,?,?,?)";
			Set <Integer> keys = nomenclatures.keySet();
			String type1 = ApplicationUtilities.getProperty("Type1");
			String type2 = ApplicationUtilities.getProperty("Type2");
			String type3 = ApplicationUtilities.getProperty("Type3");
			pstmt = conn.prepareStatement(query);
			for (Integer i : keys) {
				NomenclatureBean nBean = nomenclatures.get(i);
				pstmt.setString(1, nBean.getLabel().getText());
				pstmt.setString(2, nBean.getYesRadioButton().getSelection()?"Y":"N");
				pstmt.setString(3, nBean.getNoRadioButton().getSelection()?"Y":"N");
				pstmt.setString(4, nBean.getDescription().getText());
				int offset = i.intValue()%3;
				switch(offset) {
				 case 0 : pstmt.setString(5, type1);
				 		  break;
				 case 1 : pstmt.setString(5, type2);
				 	      break;
				 case 2 : pstmt.setString(5, type3);
		 	      		  break;
				}
				
				pstmt.execute();
			}
			
			/* Save the data in Expression tab - use expressions*/
			pstmt = conn.prepareStatement("delete from expressions");
			pstmt.execute();
			query = "insert into expressions (_label, description) values (?,?)";
			keys = expressions.keySet();
			pstmt = conn.prepareStatement(query);
			for (Integer i : keys) {
				ExpressionBean expBean = expressions.get(i);
				pstmt.setString(1, expBean.getLabel().getText());
				pstmt.setString(2, expBean.getText().getText());
				pstmt.execute();
			}
			
			/*Save morphological descriptions  - use descriptionBean */
			pstmt = conn.prepareStatement("delete from morpdesc");
			pstmt.execute();
			
			query = "insert into morpdesc values(?,?)";
			pstmt = conn.prepareStatement(query);			
			pstmt.setString(1, descriptionBean.getYesButton().getSelection()?"Y":"N");
			pstmt.setString(2, descriptionBean.getOtherInfo().getText());			
			pstmt.execute();
			
			pstmt = conn.prepareStatement("delete from descriptions");
			pstmt.execute();
			
			query = "insert into descriptions (_order, section, start_token, end_token, embedded_token) values(?,?,?,?,?)";
			pstmt = conn.prepareStatement(query);
			HashMap <Integer, SectionBean> descriptions = descriptionBean.getSections();
			keys = descriptions.keySet();
			
			for(Integer i : keys) {
				SectionBean  secBean = descriptions.get(i);
				pstmt.setString(1, secBean.getOrder().getText());
				pstmt.setString(2, secBean.getSection().getText());
				pstmt.setString(3, secBean.getStartTokens().getText());
				pstmt.setString(4, secBean.getEndTokens().getText());
				pstmt.setString(5, secBean.getEmbeddedTokens().getText());
				pstmt.execute();
			}
			
			/* Save Special tab data - use SpecialBean */
			pstmt = conn.prepareStatement("delete from specialsection");
			pstmt.execute();
			
			query = "insert into specialsection(hasGlossary,glossaryHeading," +
					"hasReference,referenceHeading) values (?,?,?,?)";
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, special.getFirstButton().getSelection()?"Y":"N");
			pstmt.setString(2, special.getFirstText().getText());
			pstmt.setString(3, special.getSecondButton().getSelection()?"Y":"N");
			pstmt.setString(4, special.getSecondText().getText());
			pstmt.execute();
			
			/* Save the abbreviations tab data - use abbreviations */
			pstmt = conn.prepareStatement("delete from abbreviations");
			pstmt.execute();
			
			query = "insert into abbreviations (_label, abbreviation) values(?,?)";
			
			pstmt = conn.prepareStatement(query);
			Set <String> keySet = abbreviations.keySet();
			for (String name: keySet) {
				pstmt.setString(1, name);
				pstmt.setString(2, abbreviations.get(name).getText());
				pstmt.execute();
			}
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
