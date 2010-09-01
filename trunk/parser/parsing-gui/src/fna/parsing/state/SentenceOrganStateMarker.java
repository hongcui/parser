/**
 * 
 */
package fna.parsing.state;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fna.charactermarkup.*;

/**
 * @author hongcui
 *
 */
public class SentenceOrganStateMarker {
	private Hashtable<String, String> sentences = new Hashtable<String, String>();
	private Connection conn = null;
	private boolean marked = false;
	private int fixedcount  =0;
	
	private Hashtable<String, String> adjnounsent = null;
	private String adjnounslist = "";
	private String organnames = null;
	private String statenames = null;
	private String tableprefix = null;
	/**
	 * 
	 */
	public SentenceOrganStateMarker(Connection conn, String tableprefix) {
		this.tableprefix = tableprefix;
		this.conn = conn;
		try{
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists "+this.tableprefix+"_markedsentence (source varchar(100) NOT NULL Primary Key, markedsent text, rmarkedsent text)");
				stmt.execute("delete from "+this.tableprefix+"_markedsentence");
				//ResultSet rs = stmt.executeQuery("select * from "+this.tableprefix+"_markedsentence");
				//if(rs.next()){this.marked = true;}
				stmt.execute("update "+this.tableprefix+"_sentence set charsegment =''");
		}catch(Exception e){
			e.printStackTrace();
		}

		//preparing...
		this.adjnounsent = new Hashtable(); //source ->adjnoun (e.g. inner)
		ArrayList<String> adjnouns = new ArrayList<String>();//all adjnouns
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select source, tag, originalsent from "+this.tableprefix+"_sentence");
			while(rs.next()){
				String source = rs.getString("source");
				String sent = rs.getString("tag")+"##"+rs.getString("originalsent");
				sentences.put(source, sent);
			}
			
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT distinct modifier FROM "+this.tableprefix+"_sentence s where modifier != \"\" and tag like \"[%\"");
			while(rs.next()){
				String modifier = rs.getString(1).replaceAll("\\[.*?\\]", "").trim();
				adjnouns.add(modifier);
			}
			
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT source, tag, modifier FROM "+this.tableprefix+"_sentence s where modifier != \"\" and tag like \"[%\"");
			while(rs.next()){
				String modifier = rs.getString(2).replaceAll("\\[.*?\\]", "").trim();
				adjnounsent.put(rs.getString("tag"), modifier);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		Collections.sort(adjnouns);
		for(int i = adjnouns.size()-1; i>=0; i--){
			this.adjnounslist +=adjnouns.get(i)+"|";
		}
		this.adjnounslist = "[<{]*"+this.adjnounslist.replaceFirst("\\|$", "").replaceAll("\\|+", "|").replaceAll("\\|", "[}>]*|[<{]*").replaceAll(" ", "[}>]* [<{]*")+"[}>]*";
		this.organnames = collectOrganNames();
		this.statenames = collectStateNames();
	}
	
	protected Hashtable markSentences(){
		if(this.marked){
			loadMarked();
		}else{
			Enumeration<String> en = sentences.keys();
			while(en.hasMoreElements()){
				String source = en.nextElement();
				String sent = (String)sentences.get(source); 
				String tag = sent.substring(0, sent.indexOf("##"));
				sent = sent.replace(tag+"##", "").trim();
				String taggedsent = markASentence(source, tag.trim(), sent);
				sentences.put(source, taggedsent); 
				try{
					Statement stmt1 = conn.createStatement();
					stmt1.execute("insert into "+this.tableprefix+"_markedsentence (source, markedsent) values('"+source+"', '"+taggedsent+"')");
				}catch(Exception e){
					e.printStackTrace();
				}
				//System.out.println(source+" marked");
			}
		}
		return sentences;
	}

	protected void loadMarked() {
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select source, markedsent from "+this.tableprefix+"_markedsentence");
			while(rs.next()){
				String source = (String)rs.getString("source");
				String taggedsent = (String)rs.getString("markedsent"); 
				sentences.put(source, taggedsent); //do this in addClause
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public String markASentence(String source, String tag, String sent) {
		String taggedsent = markthis(source, sent, organnames, "<", ">");
		taggedsent = markthis(source, taggedsent, statenames, "{", "}");
		taggedsent = taggedsent.replaceAll("[<{]or[}>]", "or"); //make sure to/or are left untagged
		taggedsent = taggedsent.replaceAll("[<{]to[}>]", "to");
		//taggedsent = fixInner(adjnounslist, taggedsent);
		//if(adjnounsent.containsKey(source) || taggedsent.matches(".*? of [<{]*\\b(?:"+adjnounslist+")\\b[}>]*.*")){
		if((adjnounsent.containsKey(tag) && taggedsent.matches(".*?[<{]*\\b(?:"+adjnounslist+")\\b[}>]*.*")) || taggedsent.matches(".*? of [<{]*\\b(?:"+adjnounslist+")\\b[}>]*.*")){
			taggedsent = fixInner(taggedsent);
		//	taggedsent = fixAdjNouns(adjnouns, (String)adjnounsent.get(source), taggedsent);
		}
 		return taggedsent;
	}
	
	/**
	 * mark Inner as organ for sent such as inner red.
	 * @param adjnouns
	 * @param taggedsent
	 * @return
	 */
	private String fixInner(String taggedsent) {
		String fixed = "";
		boolean needfix = false;
		boolean changed = true;
		//Pattern p =Pattern.compile("(.*?)(\\s*(?:[ <{]*\\b(?:"+adjnounslist+")\\b[}> ]*)+\\s*)(.*)");
		Pattern p =Pattern.compile("(.*?)((?:^| )(?:(?:\\{|<\\{)*\\b(?:"+adjnounslist+")\\b(?:\\}>|\\})*) )(.*)");
		Matcher m = p.matcher(taggedsent);
		
		while(m.matches() && changed){
			changed = false;
			String before = m.group(1);
			String inner = m.group(2);
			String after = m.group(3);
			if(!before.trim().endsWith(">") &&!after.trim().startsWith("<")){//mark inner as organ
				inner = inner.trim().replaceAll("[<{}>]", "").replaceAll("\\s+", "> <").replaceAll("<and>", "and").replaceAll("<or>", "or");
				inner = "<"+inner+">";
				needfix = true;
				changed = true;
			}
			//fixed +=before+" "+inner+" ";
			fixed = before+" "+inner+" "+after; //{outer} {pistillate}
			//taggedsent = after;
			//m = p.matcher(taggedsent);
			m = p.matcher(fixed);
		}
		//fixed +=taggedsent;
		if(needfix){
			System.out.println("fixed "+fixedcount+": "+fixed);
			fixedcount++;
		}
		return fixed.replaceAll("\\s+", " ");
	}

	/**
	 * retag {caline} 10 to <caline> 10 when an adjnoun does not follow an organ or proceeds  an organ.
	 * @param adjnouns
	 * @param taggedsent
	 * @return
	 */
	private String fixAdjNouns(ArrayList adjnouns, String adjnoun, String taggedsent) {
		adjnoun = adjnoun.replaceAll("\\s+", "\\\\W+");
		taggedsent = Pattern.compile("[<{]*\\b"+adjnoun+"\\b[}>]*", Pattern.CASE_INSENSITIVE).matcher(taggedsent).replaceFirst("<"+adjnoun+">").replaceAll("W\\+", "> <").replaceAll("<and>", "and").replaceAll("<or>", "or");
		return taggedsent;
	}

	public static String markthis(String source, String sent, String parts, String leftmark, String rightmark) {
		//remove ()
		sent = sent.replaceAll("\\(.*?\\)", "");
		sent = sent.replaceAll("(?<=\\w)\\s+(?=[,\\.;:])", "");

		sent = sent.replaceAll("_", "-");
		
		Pattern tagsp = Pattern.compile("(.*?)\\b("+parts+")\\b(.*)", Pattern.CASE_INSENSITIVE);
		String taggedsent = "";
		Matcher m = tagsp.matcher(sent);
		while(m.matches()){
			taggedsent += m.group(1)+leftmark+m.group(2)+rightmark;
			sent = m.group(3);
			m = tagsp.matcher(sent);
		}
		taggedsent +=sent;
		
		String tsent = "";
		Pattern p = Pattern.compile("(.*\\}-)(\\w+)(.*)");
		m = p.matcher(taggedsent);
		while(m.matches()){
			tsent += m.group(1)+"{"+m.group(2)+"}";
			taggedsent = m.group(3);
			m = p.matcher(taggedsent);			
		}
		tsent +=taggedsent;
		tsent = tsent.replaceAll("\\}-\\{", "-"); // => {oblong}-{ovate} :  {oblong-ovate}
		tsent = tsent.replaceAll("\\s*,\\s*", " , ");
		tsent = tsent.replaceAll("\\s*\\.\\s*", " . ");
		tsent = tsent.replaceAll("\\s*;\\s*", " ; ");
		tsent = tsent.replaceAll("\\s*:\\s*", " : ");
		tsent = tsent.replaceAll("\\s*\\]\\s*", " ] ");
		tsent = tsent.replaceAll("\\s*\\[\\s*", " [ ");
		//tsent = tsent.replaceAll("\\s*\\)\\s*", " ) ");
		//tsent = tsent.replaceAll("\\s*\\(\\s*", " ( ");
		tsent = tsent.replaceAll("\\s+", " ").trim();		
		return tsent;
	}
	
	protected String collectStateNames(){
		String statestring = "";
		try{
			Statement stmt = conn.createStatement();
			//ResultSet rs = stmt.executeQuery("select word from "+this.tableprefix+"_wordpos where pos ='b'");
			ResultSet rs = stmt.executeQuery("select word from wordroles where semanticrole ='c'");
			while(rs.next()){
				String w = rs.getString("word");
				if(!w.matches("\\W+") && !w.matches("("+ChunkedSentence.stop+")") &&!w.matches("("+ChunkedSentence.prepositions+")")){
					statestring += "|"+ w; 
				}
			}
		}catch (Exception e){
				e.printStackTrace();
		}
		return statestring.replaceAll("_", "|").replaceAll("\\b(and|or|to)\\b", "").replaceFirst("\\|", "").replaceAll("\\|+", "|");
	}
	
	protected String collectOrganNames(){
		StringBuffer tags = new StringBuffer();
		try{
		Statement stmt = conn.createStatement();
		//organNameFromGloss(tags, stmt);
		organNameFromSentences(tags, stmt);
		organNameFromPlNouns(tags, stmt);
	
		tags = tags.replace(tags.lastIndexOf("|"), tags.lastIndexOf("|")+1, "");
		
		}catch(Exception e){
			e.printStackTrace();
		}
		return tags.toString().replaceAll("\\|+", "|");
	}
	

	protected void organNameFromPlNouns(StringBuffer tags, Statement stmt)
			throws SQLException {
		ResultSet rs;
		//rs = stmt.executeQuery("select word from "+this.tableprefix+"_wordpos where pos in ('p', 's', 'n')");
		rs = stmt.executeQuery("select word from wordroles where semanticrole in ('op', 'os')");
		while(rs.next()){
			tags.append(rs.getString("word").trim()+"|");
		}
	}
	
	protected void organNameFromSentences(StringBuffer tags, Statement stmt)
			throws SQLException {
		ResultSet rs;
		/*rs = stmt.executeQuery("select distinct tag from sentence where tag not like '% %'");
		while(rs.next()){
			String tag = rs.getString("tag");
			if(tag == null || tag.indexOf("[")>=0|| tags.indexOf("|"+tag+"|") >= 0){continue;}
			tags.append(tag+"|");
		}*/
		
		rs = stmt.executeQuery("select modifier, tag from "+this.tableprefix+"_sentence where tag  like '[%]'"); //inner [tepal]
		while(rs.next()){
			String m = rs.getString("modifier");
			m = m.replaceAll("\\[^\\[*\\]", ""); 
			if(m.compareTo("")!= 0){
				String tag = null;
				if(m.lastIndexOf(" ")<0){
					tag = m;
				}else{
					tag = m.substring(m.lastIndexOf(" ")+1); //last word from modifier
				}
				if(tag == null ||tag.indexOf("[")>=0|| tags.indexOf("|"+tag+"|") >= 0 || tag.indexOf("[")>=0 || tag.matches(".*?(\\d|"+StateCollector.stop+").*")){continue;}
				tags.append(tag+"|");
			}
		}
	}
	
	protected void organNameFromGloss(StringBuffer tags, Statement stmt)
			throws SQLException {
		ResultSet rs = stmt.executeQuery("select distinct term from fnaglossary where category in ('STRUCTURE', 'FEATURE', 'SUBSTANCE', 'PLANT')");
		while(rs.next()){
			String tag = rs.getString("term");
			if(tag == null){continue;}
			tags.append(tag+"|");
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Connection conn = null;
		String database="";
		String username="";
		String password="";
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		SentenceOrganStateMarker sosm = new SentenceOrganStateMarker(conn, "fnav19");
		sosm.markSentences();

	}

}
