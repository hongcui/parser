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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;

import org.apache.log4j.Logger;

import fna.parsing.ApplicationUtilities;
import fna.parsing.ParsingException;

public class VolumeMarkupDbAccessor {
	//public static String PRONOUN ="all|each|every|some|few|individual|both|other";
	//public static String NUMBERS = "zero|one|ones|first|two|second|three|third|thirds|four|fourth|fourths|quarter|five|fifth|fifths|six|sixth|sixths|seven|seventh|sevenths|eight|eighths|eighth|nine|ninths|ninth|tenths|tenth";
	//public static String CHARACTERS ="lengths|length|lengthed|width|widths|widthed|heights|height|character|characters|distribution|distributions|outline|outlines|profile|profiles|feature|features|form|forms|mechanism|mechanisms|nature|natures|shape|shapes|shaped|size|sizes|sized";
	//public static String PREPOSITION ="above|across|after|along|around|as|at|before|beneath|between|beyond|by|for|from|in|into|near|of|off|on|onto|out|outside|over|than|throughout|toward|towards|up|upward|with|without";
	//public static String CLUSTERSTRINGS = "group|groups|clusters|cluster|arrays|array|series|fascicles|fascicle|pairs|pair|rows|number|numbers|\\d+";
	//public static String SUBSTRUCTURESTRINGS = "part|parts|area|areas|portion|portions";
	//public static String STOP ="a|about|above|across|after|along|also|although|amp|an|and|are|as|at|be|because|become|becomes|becoming|been|before|being|beneath|between|beyond|but|by|ca|can|could|did|do|does|doing|done|for|from|had|has|have|hence|here|how|if|in|into|inside|inward|is|it|its|may|might|more|most|near|no|not|of|off|on|onto|or|out|outside|outward|over|should|so|than|that|the|then|there|these|this|those|throughout|to|toward|towards|up|upward|was|were|what|when|where|which|why|with|within|without|would";

	/**
	 * @param args
	 */
    private static final Logger LOGGER = Logger.getLogger(VolumeMarkupDbAccessor.class);
    private static String url = ApplicationUtilities.getProperty("database.url");
	private String tablePrefix = null ;
	private String glossarytable;
	private Connection conn = null;
   
    
    public VolumeMarkupDbAccessor(String dataPrefix, String glossarytable){
    	this.tablePrefix = dataPrefix;
    	this.glossarytable = glossarytable;
    	try {
    			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
    			conn = DriverManager.getConnection(url);
    	} catch (Exception e) {
    			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
    	}
    		
	
    }
	
    
    /**
     * display learned new structures in structures subtab in step 4 (perl markup) for curation.
     * 
     * @param tagList
     * @throws ParsingException
     * @throws SQLException
     */
    public ArrayList<String> structureTags4Curation(List <String> tagList) throws  ParsingException, SQLException {
    	
    	PreparedStatement stmt = null;
    	PreparedStatement stmtSentence=null;
    	ResultSet rs = null;
    	Statement stmt1 = null;
    	ResultSet rs1 = null;
    	ResultSet rs2 = null;
		try {
	 		String filter1 = "";
	 		String filter2 = "";
	 		String filter3 = "";
			stmt1 = conn.createStatement();
			rs1 = stmt1.executeQuery("show tables");
			while(rs1.next()){
				if(rs1.getString(1).compareToIgnoreCase(ApplicationUtilities.getProperty("NONEQTABLE"))==0){
					filter1 = " and tag not in (select word from "+ ApplicationUtilities.getProperty("NONEQTABLE")+") "; 
					filter2 = " and plural not in (select word from "+ ApplicationUtilities.getProperty("NONEQTABLE")+") " ;
					filter3 = " and word not in (select word from "+ ApplicationUtilities.getProperty("NONEQTABLE")+") ";
				}
			}

			String sql = "select distinct tag as structure from "+this.tablePrefix+"_sentence where tag != 'unknown' and tag is not null and tag not like '% %' " +
			filter1 +
			"union select distinct plural as structure from "+this.tablePrefix+"_singularplural"+","+ this.tablePrefix+"_sentence where singular=tag "+
			filter2 +
			"order by structure"; 		
			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();
			while (rs.next()) {
				String tag = rs.getString("structure");
				populateCurationList(tagList, tag); //select tags for curation, filter against the glossary
			}
			sql = "select distinct word from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+" where pos in ('p', 's', 'n') and saved_flag !='red' "+
			filter3+" order by word";
			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();
			while (rs.next()) {
				String tag = rs.getString("word");
				stmtSentence = conn.prepareStatement("select * from " + this.tablePrefix + "_sentence where sentence like '% " + tag + "%'");
				rs2 = stmtSentence.executeQuery();
				if (rs2.next()) {
					populateCurationList(tagList, tag); //select tags for curation, filter against the glossary
				}
			}
			return deduplicateSort(tagList);
		} catch (SQLException e) {
			//LOGGER.error("Couldn't update sentence table in VolumeMarkupDbAccessor:updateData", sqlexe);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			throw new ParsingException("Error Accessing the database" , e);
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}
			if(rs1!=null) rs1.close();
			if(stmt!=null) stmt.close();
			if(rs2!=null) rs2.close();
			if(stmtSentence!=null) stmtSentence.close();
		}
    }


	private ArrayList<String> deduplicateSort(List<String> tagList) {
		HashSet<String> set = new HashSet<String>(tagList);
		String[] sorted = set.toArray(new String[]{}); 
		Arrays.sort(sorted);
		ArrayList<String> results = new ArrayList<String>();
		for(int i=0; i<sorted.length; i++){
			results.add(sorted[i]);
		}
		return results;
	}
    
    
    /**
     * display unknown terms in morestructure/moredescriptor subtabs 
     * in step 4 (perl markup) for curation.
     * @param curationList
     * @throws ParsingException
     * @throws SQLException
     */
    public ArrayList<String> contentTerms4Curation(List <String> curationList, ArrayList<String> structures, ArrayList<String> characters) throws  ParsingException, SQLException {
     	PreparedStatement stmt = null;
    	ResultSet rs = null;
		Statement stmt1 = null;
		ResultSet rs1 = null;
	 	try {
	 		String filter = "";
			stmt1 = conn.createStatement();
			rs1 = stmt1.executeQuery("show tables");
			while(rs1.next()){
				if(rs1.getString(1).compareToIgnoreCase(ApplicationUtilities.getProperty("NONEQTABLE"))==0){
					filter = " and dhword not in (select word from "+ ApplicationUtilities.getProperty("NONEQTABLE")+") ";
				}
			}
	 		String sql = "select dhword from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("ALLWORDS")+
			//" where count>=3 and inbrackets=0 and dhword not like '%\\_%' and " +
			" where dhword not like '%\\_%' and " +		
			" dhword not in (select word from "+ this.tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+" where saved_flag='red')"+
			filter +
			" and dhword not in (select word from "+ this.tablePrefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+") order by dhword";
			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();
			while (rs.next()) {
				String word = rs.getString("dhword");
				if(!structures.contains(word) && !characters.contains(word)){
					populateCurationList(curationList, word);
				}
			}
			return this.deduplicateSort(curationList);
		} catch (SQLException e) {
			//LOGGER.error("Couldn't update sentence table in VolumeMarkupDbAccessor:contentTerms4Curation", sqlexe);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			throw new ParsingException("Error Accessing the database" , e);
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}		
			if(stmt1!=null) stmt1.close();
			if(rs1!=null) rs1.close();
		}
    }
    /**
     * if word in glossary, add it to wordroles
     * if not in glossary, add to curationList
     * @param curationList
     * @param word
     */
	private void populateCurationList(List<String> curationList, String word) {
		Statement stmt = null;
		ResultSet rs = null;
		try{	
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select category from "+this.glossarytable+" where term ='"+word+"'");
			if(rs.next()){
				String cat = rs.getString("category");
				if(cat.matches("("+ApplicationUtilities.getProperty("STRUCTURECATEGORYPATTERNINGLOSSARY")+")")){
					add2WordRolesTable(word, "op");
				}else{
					add2WordRolesTable(word, "c");
				}
			}else{
				curationList.add(word);
			}
		}catch(Exception e){
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
	}

	/**
	 * load descriptor subtab in step 4 (perl markup)
	 * @return
	 * @throws SQLException
	 */
	public ArrayList<String> descriptorTerms4Curation() throws SQLException {
		
		ArrayList<String> words = new ArrayList<String>();
		
		PreparedStatement stmt = null;
		PreparedStatement stmtSentence = null;
		ResultSet rset = null;
		Statement stmt1 =null;
		ResultSet rs1 = null;
		ResultSet rs2 = null;
		try {
	 		String filter = "";
		    stmt1 = conn.createStatement();
			rs1 = stmt1.executeQuery("show tables");
			while(rs1.next()){
				if(rs1.getString(1).compareToIgnoreCase(ApplicationUtilities.getProperty("NONEQTABLE"))==0){
					filter = " and word not in (select word from "+ ApplicationUtilities.getProperty("NONEQTABLE")+") ";
				}
			}
	 		rs1.close();
	 		stmt1.close();
	 		
	 		String sql = "select word from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+" where pos=? and saved_flag !='red' "+
	 				filter+"order by word";
			//stmt = conn.prepareStatement("select word from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+" where pos=? and word not in (select distinct term from "+this.glossarytable+")");
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, "b");
			rset = stmt.executeQuery();
			if (rset != null) {
				while(rset.next()){
					String word = rset.getString("word");
					stmtSentence = conn.prepareStatement("select * from " + this.tablePrefix + "_sentence where sentence like '% " + word + "%'");
				    rs2 = stmtSentence.executeQuery();
					if (rs2.next()) {
						populateDescriptorList(words, word);
					}
				}	
			}
			words = deduplicateSort(words);
		} catch (SQLException e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		} finally {
			if(rset != null) {
				rset.close();
			}
			
			if(stmt != null) {
				stmt.close();
			}
			if(rs1 != null) rs1.close();
			if(rs2 !=null) rs2.close();
			if(stmt1 != null) stmt.close();
			if(stmtSentence !=null) stmtSentence.close();
		}
	return words;	
	}
	
	/**
	 * w is put into words only if 
	 * it is a word
	 * it is not a pronoun, a stopword, a preposition, an adv (-ly), or -shaped
	 * it is not in the glossary
	 * 
	 * if it is in the glossary, get its role from glossary and save it in wordroles table.
	 * @param words
	 * @param w
	 */
	private void populateDescriptorList(ArrayList<String> words, String w) {
		if(w.matches(".*?\\w.*")){
				String wc = w;
				if(w.indexOf("-")>=0 || w.indexOf("_")>=0){
					String[] ws = w.split("[_-]");
					w = ws[ws.length-1];
				}
				Statement stmt = null;
				ResultSet rset = null;
				try {
					stmt = conn.createStatement();
					rset = stmt.executeQuery("select category from "+this.glossarytable+" where term ='"+w+"'");					 
					if(rset.next()){//in glossary
						String cat = rset.getString(1);
						if(cat.matches("\\b(STRUCTURE|FEATURE|SUBSTANCE|PLANT|nominative|structure)\\b")){
							add2WordRolesTable(wc, "os");
						}else{
							add2WordRolesTable(wc, "c");
						}
						
					}else{ //not in glossary
						words.add(wc);
					}
				} catch (SQLException e) {
					StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
				}finally{
					try{
						if(rset!=null) rset.close();
						if(stmt!=null) stmt.close();
					}catch(Exception e){
						StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
						LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
								System.getProperty("line.separator")
								+sw.toString());
					}
				}
		}
	}

	/**
	 * 
	 * @param w
	 * @param role
	 */
	private void add2WordRolesTable(String w, String role) {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select * from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+" where word='"+w+"' and semanticrole='"+role+"'");
			if(!rs.next()){
				stmt.execute("insert into "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+"(word, semanticrole) values ('"+w+"','"+role+"')");
			}
		} catch (SQLException e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}	finally{
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
	}


/**
 * called also by "load last project"	
 * @return
 * @throws SQLException
 */
public ArrayList<String> getSavedDescriptorWords() throws SQLException {
		
		ArrayList<String> words = new ArrayList<String>();
		
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			
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
			
		}
		
		return words;
	}


	public ArrayList<ArrayList> getUnSavedDescriptorWords() throws SQLException {
		
		ArrayList<String> words = new ArrayList<String>();
		ArrayList<String> flag = new ArrayList<String>();
		ArrayList<ArrayList> wordsAndFlag = new ArrayList<ArrayList>();
		
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
		
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
		} catch (SQLException e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		} finally {
			if(rset != null) {
				rset.close();
			}
			
			if(stmt != null) {
				stmt.close();
			}			
		}
		wordsAndFlag.add(words);
		wordsAndFlag.add(flag);
		return wordsAndFlag;
	}
	
	
	public void insertIntoHeuristicsTerms(String term, String type){
		Statement stmt = null;
		try{
			stmt = conn.createStatement();
			stmt.execute("insert into "+tablePrefix+"_"+ApplicationUtilities.getProperty("HEURISTICSTERMS")+"(term, type) values ('"+term+"','"+type+"')");
		}catch (Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}finally{
			try{
				if(stmt!=null) stmt.close();
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
						System.getProperty("line.separator")
						+sw.toString());
			}
		}
	}
	
	public ArrayList<String> retrieveOriginalSentences(){
		ArrayList<String> originals = new ArrayList<String>();
		Statement stmt = null;
		ResultSet rs = null;
		try{
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select originalsent from "+tablePrefix+"_"+ApplicationUtilities.getProperty("SENTENCETABLE"));
			while(rs.next()){
				originals.add(rs.getString("originalsent"));
			}
		}catch (Exception e){
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
		return originals;
	}
	
    public static void main(String[] args)throws Exception {
		// TODO Auto-generated method stub
		//System.out.println(DriverManager.getConnection(url));
	}
}
