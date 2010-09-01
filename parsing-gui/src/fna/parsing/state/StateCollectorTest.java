package fna.parsing.state;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
/**
 * DO NOT treat a list of states, such as imbricate, lanceolate or ovate because it is common for an author to enumerate different characters in a list
 * Treat only states connected by or/to, such as /{elliptic} to {oblong}/ or {ovate}, {glabrous} or /{villous} to {tomentose}/, clasping or short_decurrent,  
 * Watch out for "adv or/to adv state" pattern, such as {thinly} to {densely} {arachnoid_tomentose}
 * Watch out for preposition to: reduced to
 * 
 * {distalmost} {linear} to {narrowly} {elliptic} , {bractlike} , {spinulose} to {irregularly} {dentate} or {shallowly} {lobed} .

 * 
 * @author hongcui
 *
 */
public class StateCollectorTest extends StateCollector {

		
	public StateCollectorTest(Connection conn, String tableprefix) {
		super(conn, tableprefix);
		//statematrix.save2MySQL(database, "termsuser", "termspassword");
		
	}
	
	public StateCollectorTest(Connection conn, String tableprefix, ArrayList<String> knownstates) {
		super(conn, tableprefix, knownstates);
		
	}

	public void saveStates(){
		statematrix.save2MySQL(this.conn, this.tableprefix, "termsuser", "termspassword");
	}
	
	public void grouping4GraphML(){
		statematrix.Grouping();
		statematrix.output2GraphML();
	}
			
	/**
	 * rely on {c} /[o] and to/or 
	 * add to the statematrix
	 */
	protected void parseSentence(String source, String sent){
		String scopy = sent;
		Pattern p = Pattern.compile("\\b(to|or)\\b");
		Matcher m = p.matcher(sent);
		if(m.find()){
			System.out.println("from sent ["+source+"]:"+sent);
			
			//Pattern p1 = Pattern.compile("((?:\\{\\w+}\\s)+|\\s*(,|or|to)\\s*)+\\s*(to|or|nor)\\s*(?:\\{\\w+\\}\\s)+");
			Pattern p1 = Pattern.compile("(?:\\{\\w+}\\s)+\\s*(or|to)\\s*(?:\\{\\w+\\}\\s*)+");
			//Pattern p1 = Pattern.compile("(?:(?:\\{\\w+}\\s)+\\s*(or|to)\\s*)+(?:\\{\\w+\\}\\s*)+");
			Matcher m1 = p1.matcher(sent);
			while(m1.find()){
				String matched = sent.substring(m1.start(), m1.end());
				String mstring = matched;
				boolean endofseg = false;
				int end = m1.end() + 5 > sent.length()? sent.length() : m1.end()+5;
				String follow = sent.substring(m1.end(), end);
				if(follow.matches("\\s*[,;\\.:].*") ){
					endofseg = true;
				}
				matched = matched.toLowerCase();
				//sent = sent.substring(m1.end()); take from after (or|to) instead
				sent = sent.substring(m1.end(1)+1); //3 for "or|to "
				matched = matched.replaceFirst("^[\\s,]*", "").replaceAll("[{}]", "");
				matched = split(matched, endofseg);
				if(matched.length() > 0 && ! mstring.matches(".*?(ed|ing)}.*? to .*")){ //ignore "reduced to", but take "reduced or"
					add2matrix(matched, source);
					System.out.println("\t====::"+matched); //deal with two "to"/"or" in one match: {distalmost} {linear} to {narrowly} {elliptic} , {bractlike} , {spinulose} to {irregularly} {dentate} or {shallowly} {lobed} .
				}
				m1 = p1.matcher(sent);
			}
		}
	return;	
	}
	
	/*1) thinly to/or densely arachnoid_tomentose} : leave this alone: no need to capture degrees.
	 *2) distalmost linear to/or narrowly elliptic
	 * @param if endofseg is true, take the last adj for the last segment
	 * @return
	 */
	private String split(String conjunction, boolean endofseg){
		String[] terms = conjunction.split("\\s+(to|or)\\s+");
		String csv = "";
		int count = 0;
		System.out.println("########### from :"+conjunction);
		
		int size = terms.length;
		int i = 0;
		//all but the last term: save the last non-adv word from each term
		for(i = 0; i < terms.length-1; i++){
			terms[i] = terms[i].trim();
			String[] parts = terms[i].split("\\s+");
			//if(parts.length > 1){
			//	System.out.println("########### from :"+conjunction);
			//}
			for(int j = parts.length-1; j >=0; j--){
				if(!isAdv(parts[j])){ //save the last non-adv word from each term
						csv += ","+parts[j];
						count++;
						break;
				}
			}
		}
		//the last term: save the first non-adv word
		String[] parts = terms[i].split("\\s+");
		if(!endofseg){
			for(int j = 0; j <parts.length; j++){
				if(!isAdv(parts[j])){ //save the first non-adv word for the last term
						csv += ","+parts[j];
						count++;
						break;
				}
			}
		}else{
			for(int j = parts.length-1; j >=0; j--){
				if(!isAdv(parts[j])){ //save the first non-adv word for the last term
						csv += ","+parts[j];
						count++;
						break;
				}
			}
		}
		if(count > 1){//at least two states in a conjunction
			csv = csv.replaceFirst("^[\\s,]*", "").replaceFirst("[\\s,]*$", "");
			return csv;
		}
		return "";
	}
	
	protected boolean isAdv(String word){
		//access WordNet for answer
		String wordc = word;
		
		word = word.replaceFirst("ly$", "");
		if(word.compareTo(wordc) != 0){
			WordNetWrapper wnw1 = new WordNetWrapper(word);
			WordNetWrapper wnw2 = new WordNetWrapper(word+"e");
			if(wnw1.isAdj() || wnw2.isAdv()){
				System.out.println(wordc + " is an adv");
				return true;
			}
		}
		
		WordNetWrapper wnw = new WordNetWrapper(wordc);
		//if(wnw.isAdv() && !wnw.isAdj()){
		if(wnw.mostlikelyPOS() !=null && wnw.mostlikelyPOS().compareTo("adv") == 0){
			System.out.println(word + " is an adv");
			return true;
		}
		
		
		return false;
	}
	
	/*
	 * refined is a list of format: a,b,c,d
	 */
	protected void add2matrix(String refined, String source){
		String[] alist = refined.split(",");
		for(int i = 0; i<alist.length; i++){
	    	for(int j = i+1; j<alist.length; j++){
	    		int score = 1; //absent or erect
	    		State s1 = statematrix.getStateByName(alist[i]);
	    		State s2 = statematrix.getStateByName(alist[j]);
	    		s1 = s1 == null? new State(alist[i]) : s1;
	    		s2 = s2 == null? new State(alist[j]) : s2;
	    		statematrix.addPair(s1, s2, score, source);
	    	}
	    }	
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		/*StateCollectorTest sct = new StateCollectorTest("onto_foc_corpus"); //using learned semanticroles only
		sct.collect("onto_foc_corpus");
		sct.saveStates("onto_foc_corpus");
		*/
		//to use the result from unsupervisedclausemarkup, change wordpos table to wordroles (word, semanticroles) where semanticroles in (c, os, op)
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
		StateCollectorTest sct = new StateCollectorTest(conn, "fnav19"); /*using learned semanticroles only*/
		sct.collect();
		sct.saveStates();
		sct.grouping4GraphML();
	}

}
