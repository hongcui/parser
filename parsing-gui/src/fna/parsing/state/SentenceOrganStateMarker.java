/**
 * 
 */
package fna.parsing.state;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author hongcui
 *
 */
public class SentenceOrganStateMarker {
	private Hashtable<String, String> sentences = new Hashtable<String, String>();
	private String username="termsuser";
	private String password ="termspassword";
	private Connection conn = null;
	private boolean marked = false;
	/**
	 * 
	 */
	public SentenceOrganStateMarker(String database) {
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists markedsentence (source varchar(100) NOT NULL PRIMARY KEY, markedsent varchar(2000))");
				ResultSet rs = stmt.executeQuery("select * from markedsentence");
				if(rs.next()){this.marked = true;}
				stmt.execute("update sentence set charsegment =''");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	protected Hashtable markSentences(){
		if(this.marked){
			loadMarked();
		}else{
			mark();
		}
		return sentences;
	}

	protected void loadMarked() {
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select source, markedsent from markedsentence");
			while(rs.next()){
				String source = (String)rs.getString("source");
				String taggedsent = (String)rs.getString("markedsent"); 
				sentences.put(source, taggedsent); //do this in addClause
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * mark [o] and [c]: organs and characters
	 */
	protected void mark(){
		Hashtable adjnounsent = new Hashtable();
		ArrayList adjnouns = new ArrayList();
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select source, originalsent from sentence");
			while(rs.next()){
				String source = rs.getString("source");
				String sent = rs.getString("originalsent");
				sentences.put(source, sent);
			}
			
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT distinct modifier FROM sentence s where modifier != \"\" and tag like \"[%\"");
			while(rs.next()){
				adjnouns.add(rs.getString("modifier"));
			}
			
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT source, modifier FROM sentence s where modifier != \"\" and tag like \"[%\"");
			while(rs.next()){
				String modifier = rs.getString(2).replaceAll("\\[.*?\\]", "").trim();
				adjnounsent.put(rs.getString("source"), modifier);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		String organnames = collectOrganNames();
		String statenames = collectStateNames();
		
		Enumeration<String> en = sentences.keys();
		while(en.hasMoreElements()){
			String source = en.nextElement();
			String sent = (String)sentences.get(source); 
			String taggedsent = markthis(source, sent, organnames, "<", ">");
			taggedsent = markthis(source, taggedsent, statenames, "{", "}");
			taggedsent = taggedsent.replaceAll("[<{]or[}>]", "or"); //make sure to/or are left untagged
			taggedsent = taggedsent.replaceAll("[<{]to[}>]", "to");
			if(adjnounsent.containsKey(source)){
				taggedsent = fixAdjNouns(adjnouns, (String)adjnounsent.get(source), taggedsent);
			}
			sentences.put(source, taggedsent); 
			try{
				Statement stmt1 = conn.createStatement();
				stmt1.execute("insert into markedsentence values('"+source+"', '"+taggedsent+"')");
			}catch(Exception e){
				e.printStackTrace();
			}
			System.out.println(source+" marked");
		}

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

	protected String markthis(String source, String sent, String parts, String leftmark, String rightmark) {
		//remove ()
		sent = sent.replaceAll("\\(.*?\\)", "");
		sent = sent.replaceAll("(?<=\\w)\\s+(?=[,.;:])", "");

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
		tsent = tsent.replaceAll("\\}-\\{", "_"); // => {oblong}-{ovate} :  {oblong_ovate}
		tsent = tsent.replaceAll("\\s*,\\s*", " , ");
		tsent = tsent.replaceAll("\\s*\\.\\s*", " . ");
				
		return tsent;
	}
	
	protected String collectStateNames(){
		String statestring = "";
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select word from wordroles where semanticrole ='c'");
			while(rs.next()){
				statestring += "|"+rs.getString("word"); 
			}
		}catch (Exception e){
				e.printStackTrace();
		}
		return statestring.replaceFirst("\\|", "").replaceAll("\\|+", "|");
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
		
		rs = stmt.executeQuery("select modifier, tag from sentence where tag  like '[%]'"); //inner [tepal]
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
		SentenceOrganStateMarker sosm = new SentenceOrganStateMarker("fnav19_benchmark");
		sosm.markSentences();

	}

}
