 /* $Id$ */
/**
 * 
 */
package fna.charactermarkup;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;

import fna.parsing.ApplicationUtilities;
import fna.parsing.state.StateCollector;
import fna.parsing.state.WordNetWrapper;
import java.util.ArrayList;
import java.util.regex.*;

import org.apache.log4j.Logger;
/**
 * @author hongcui
 *
 */
@SuppressWarnings({ "unused" })
public class Utilities {
	public static String or = "_or_";
	
	public static Hashtable<String, String> singulars = new Hashtable<String, String>();
	public static Hashtable<String, String> plurals = new Hashtable<String, String>();
	public static ArrayList<String> sureVerbs = new ArrayList<String>();
	public static ArrayList<String> sureAdvs = new ArrayList<String>();
	public static ArrayList<String> partOfPrepPhrase = new ArrayList<String>();
	public static ArrayList<String> notSureVerbs = new ArrayList<String>();
	public static ArrayList<String> notSureAdvs = new ArrayList<String>();
	public static ArrayList<String> notPartOfPrepPhrase = new ArrayList<String>();
	private static final Logger LOGGER = Logger.getLogger(Utilities.class);
	public static boolean debug = false;
	public static boolean debugPOS = true;
	//special cases
	/**
	 * word must be a verb if
	 * 1. its pos is "verb" only, or
	 * 2. "does not" word
	 * 3. has "verb" pos and seen patterns (word "a/the", or word prep <organ>) and not seen pattern (word \w+ly$). 
	 * @param word
	 * @param conn
	 * @return
	 */
	public static boolean mustBeVerb(String word, Connection conn, String prefix){
		if(sureVerbs.contains(word)) return true;
		if(notSureVerbs.contains(word)) return false;
		WordNetWrapper wnw = new WordNetWrapper(word);
		boolean v = wnw.isV();
		if(!wnw.isAdj() && !wnw.isAdv() && !wnw.isN() && v){
			sureVerbs.add(word);
			if(debugPOS) System.out.println(word+" is sureVerb");
			return true;
		}
		try{
			Statement stmt = conn.createStatement();
			String q = "select * from "+prefix+"_"+ApplicationUtilities.getProperty("SENTENCETABLE")+" " +
					"where originalsent like '%does not "+word+"%'";
			ResultSet rs = stmt.executeQuery(q);
			if(rs.next()){
				sureVerbs.add(word);
				if(debugPOS) System.out.println(word+" is sureVerb");
				return true;
			}
			if(v){
				q = "select * from "+prefix+"_"+ApplicationUtilities.getProperty("HEURISTICNOUNS")+" " +
						"where word = '"+word+"'";
				rs = stmt.executeQuery(q);
				if(rs.next()){
					notSureVerbs.add(word);
					return false;
				}
				
				q = "select * from "+prefix+"_"+ApplicationUtilities.getProperty("SENTENCETABLE")+" " +
						"where sentence rlike '(^| )"+word+" +[-a-z_]+ly$'";
				rs = stmt.executeQuery(q);
				if(rs.next()){
					notSureVerbs.add(word);
					return false;
				}
				
				q = "select sentence from "+prefix+"_"+ApplicationUtilities.getProperty("SENTENCETABLE")+" " +
						"where sentence rlike '(^| )"+word+" (a|an|the) '";
				rs = stmt.executeQuery(q);
				if(rs.next()){
					sureVerbs.add(word);
					if(debugPOS) System.out.println(word+" is sureVerb");
					return true;
				}
				
				if(word.endsWith("ed") || word.endsWith("ing")){
					q = "select sentence from "+prefix+"_"+ApplicationUtilities.getProperty("SENTENCETABLE")+" " +
							"where sentence rlike '(^| )"+word+" '";
					rs = stmt.executeQuery(q);
					while(rs.next()){
						String sent = rs.getString("sentence");
						String preps = ChunkedSentence.prepositions;
						Pattern p = Pattern.compile("\\b"+word+"\\b(?: (?:"+preps+")) +(\\S+)");
						Matcher m = p.matcher(sent);
						while(m.find()){
							String term = m.group(1);
							if(term.matches("(a|an|the|some|any|this|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth)")){
								sureVerbs.add(word);
								if(debugPOS) System.out.println(word+" is sureVerb");
								return true;
							}else if(isOrgan(term, conn, prefix)){
								sureVerbs.add(word);
								if(debugPOS) System.out.println(word+" is sureVerb");
								return true;
							}
						}		
					}
				}
			}			
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
		}
		notSureVerbs.add(word);
		return false;
	}
	
	private static boolean isOrgan(String term, Connection conn, String tablePrefix) {
		try{
			Statement stmt = conn.createStatement();
			String wordrolesable = tablePrefix+ "_"+ApplicationUtilities.getProperty("WORDROLESTABLE");		
			ResultSet rs = stmt.executeQuery("select word from "+wordrolesable+" where semanticrole in ('os', 'op') and word='"+term+"'");		
			if(rs.next()){
				if(debugPOS) System.out.println(term+" is an organ");
				return true;
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
		}
		return false;
	}

	public static boolean mustBeAdv(String word){
		if(sureAdvs.contains(word)) return true;
		if(notSureAdvs.contains(word)) return false;
		WordNetWrapper wnw = new WordNetWrapper(word);
		if(!wnw.isAdj() && wnw.isAdv() && !wnw.isN() && !wnw.isV()){
			sureAdvs.add(word);
			if(debugPOS) System.out.println(word+" is sureAdv");
			return true;
		}
		notSureAdvs.add(word);
		return false;
	}
	
	public static boolean partOfPrepPhrase(String word, Connection conn, String prefix){
		if(partOfPrepPhrase.contains(word)) return true;
		if(notPartOfPrepPhrase.contains(word)) return true;
		if(isOrgan(word, conn, prefix)){
			notPartOfPrepPhrase.add(word);
			return false;
		}
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select sentence from "+prefix+"_"+ApplicationUtilities.getProperty("SENTENCETABLE")+" " +
					"where originalsent rlike ' ("+ChunkedSentence.prepositions+") "+word+" ("+ChunkedSentence.prepositions+")'");
			boolean select = true;
			boolean exist = false;
			while(rs.next()){
				exist = true;
				if(rs.getString("sentence").matches(".*?\\bfrom "+word+" to\\b.*")) select = false;
			}
			if(exist && select){
				partOfPrepPhrase.add(word);
				if(debugPOS) System.out.println(word+" is partOfPrepPhrase");
				return true;
			}			
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
		}
		notPartOfPrepPhrase.add(word);
		return false;
	}
	
	public static boolean isPlural(String t) {
		t = t.replaceAll("\\W", "");
		if(t.matches("\\b(series|species|fruit)\\b")){
			return true;
		}
		if(t.compareTo(toSingular(t))!=0){
			return true;
		}
		return false;
	}

	public static String toSingular(String word){
		String s = "";
		word = word.toLowerCase().replaceAll("\\W", "");
		//check cache
		singulars.put("axis", "axis");
		singulars.put("axes", "axis");
		singulars.put("bases", "base");
		singulars.put("boss", "boss");
		singulars.put("buttress", "buttress");
		singulars.put("callus", "callus");
		singulars.put("frons", "frons");
		singulars.put("grooves", "groove");
		singulars.put("interstices", "interstice");
		singulars.put("lens", "len");
		singulars.put("media", "media");
		singulars.put("midnerves", "midnerve");
		singulars.put("process", "process");
		singulars.put("series", "series");
		singulars.put("species", "species");
		singulars.put("teeth", "tooth");
		singulars.put("valves", "valve");
		
		plurals.put("axis", "axes");
		plurals.put("base", "bases");		
		plurals.put("groove", "grooves");
		plurals.put("interstice", "interstices");
		plurals.put("len", "lens");
		plurals.put("media", "media");
		plurals.put("midnerve", "midnerves");
		plurals.put("tooth", "teeth");
		plurals.put("valve", "valves");
		plurals.put("boss", "bosses");
		plurals.put("buttress", "buttresses");
		plurals.put("callus", "calluses");
		plurals.put("frons", "fronses");
		plurals.put("process", "processes");
		plurals.put("series", "series");
		plurals.put("species", "species");

		s = singulars.get(word);
		if(s!=null) return s;
		
		//adverbs
		if(word.matches("[a-z]{3,}ly")){
			singulars.put(word, word);
			plurals.put(word, word);
			return word;
		}
		
		String wordcopy = word;
		wordcopy = checkWN4Singular(wordcopy);
		if(wordcopy != null && wordcopy.length()==0){
			return word;
		}else if(wordcopy!=null){
			singulars.put(word, wordcopy);
			if(!wordcopy.equals(word)) plurals.put(wordcopy, word);
			if(debug) System.out.println("["+word+"]'s singular is "+wordcopy);
			return wordcopy;
		}else{//word not in wn
		
			Pattern p1 = Pattern.compile("(.*?[^aeiou])ies$");
			Pattern p2 = Pattern.compile("(.*?)i$");
			Pattern p3 = Pattern.compile("(.*?)ia$");
			Pattern p4 = Pattern.compile("(.*?(x|ch|sh|ss))es$");
			Pattern p5 = Pattern.compile("(.*?)ves$");
			Pattern p6 = Pattern.compile("(.*?)ices$");
			Pattern p7 = Pattern.compile("(.*?a)e$");
			Pattern p75 = Pattern.compile("(.*?)us$");
			Pattern p8 = Pattern.compile("(.*?)s$");
			//Pattern p9 = Pattern.compile("(.*?[^aeiou])a$");
			
			Matcher m1 = p1.matcher(word);
			Matcher m2 = p2.matcher(word);
			Matcher m3 = p3.matcher(word);
			Matcher m4 = p4.matcher(word);
			Matcher m5 = p5.matcher(word);
			Matcher m6 = p6.matcher(word);
			Matcher m7 = p7.matcher(word);
			Matcher m75 = p75.matcher(word);
			Matcher m8 = p8.matcher(word);
			//Matcher m9 = p9.matcher(word);
			
			if(m1.matches()){
			  s = m1.group(1)+"y";
			}else if(m2.matches()){
			  s = m2.group(1)+"us";
			}else if(m3.matches()){
			  s = m3.group(1)+"ium";
			}else if(m4.matches()){
			  s = m4.group(1);
			}else if(m5.matches()){
			  s = m5.group(1)+"f";
			}else if(m6.matches()){
			  s = m6.group(1)+"ex";
			}else if(m7.matches()){
			  s = m7.group(1);
			}else if(m75.matches()){
			  s = word;
			}else if(m8.matches()){
			  s = m8.group(1);
			}//else if(m9.matches()){
			//  s = m9.group(1)+"um";
			//}
		  
		  if(s != null){
			if(debug) System.out.println("["+word+"]'s singular is "+s);
			singulars.put(word, s);
			if(!s.equals(word)) plurals.put(s, word);
			return s;
		  }
		}
		if(debug) System.out.println("["+word+"]'s singular is "+word);
		return word;
	}
	
	///////////////////////////////////////////////////////////////////////

	public static String checkWN(String cmdtext){
		try{
	 	  		Runtime r = Runtime.getRuntime();	
		  		Process proc = r.exec(cmdtext);
			    ArrayList<String> errors = new ArrayList<String>();
		  	    ArrayList<String> outputs = new ArrayList<String>();
		  
	            // any error message?
	            //StreamGobbler errorGobbler = new 
	                //StreamGobblerWordNet(proc.getErrorStream(), "ERROR", errors, outputs);            
	            
	            // any output?
	            StreamGobbler outputGobbler = new 
	                StreamGobblerWordNet(proc.getInputStream(), "OUTPUT", errors, outputs);
	                
	            // kick them off
	            //errorGobbler.start();
	            outputGobbler.start();
	                                    
	            // any error???
	            int exitVal = proc.waitFor();
	            //System.out.println("ExitValue: " + exitVal);

	            StringBuffer sb = new StringBuffer();
	            for(int i = 0; i<outputs.size(); i++){
	            	//sb.append(errors.get(i)+" ");
	            	sb.append(outputs.get(i)+" ");
	            }
	            return sb.toString();
				
		  	}catch(Exception e){
		  		StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
		  	}
		  	return "";
	}
	////////////////////////////////////////////////////////////////////////
		
	/**
	 * return null : word not in WN
	 * return ""   : word is not a noun or is singular
	 * return aword: word is a pl and singular form is returned
	 */
	public static String checkWN4Singular(String word){
		
		String result = checkWN("wn "+word+" -over");
		if (result.length()==0){//word not in WN
			return null;
		}
		//found word in WN:
		String t = "";
		Pattern p = Pattern.compile("(.*?)Overview of noun (\\w+) (.*)");
		Matcher m = p.matcher(result);
		while(m.matches()){
			 t += m.group(2)+" ";
			 result = m.group(3);
			 m = p.matcher(result);
		}
		if (t.length() ==0){//word is not a noun
			return "";
		} 
		String[] ts = t.trim().split("\\s+"); //if multiple singulars (bases =>basis and base, pick the first one
		for(int i = 0; i<ts.length; i++){
			if(ts[i].compareTo(word)!=0){//find a singular form
				return ts[i];
			}
		}
		return "";//original is a singular
	}
	 

	public static boolean isNoun(String word, ArrayList<String> nouns, ArrayList<String> notnouns){
		word = word.trim();
		if(word.indexOf(' ')>0) return false;
		word = word.replaceAll("[<>{}\\]\\[]", "");
		if(!word.matches(".*?[a-z]+.*")){
			notnouns.add(word);
			return false;
		}
		if(word.matches("\\b("+StateCollector.stop+")\\b")){
			notnouns.add(word);
			return false;
		}
		if(nouns.contains(word)){
			return true;
		}
		
		if(notnouns.contains(word)){
			return false;
		}
		WordNetWrapper wnw = new WordNetWrapper(word);
		String pos = wnw.mostlikelyPOS();
		if(pos != null){
			if(pos.compareTo("noun") == 0){
				nouns.add(word);
				return true;
			}
		}
		notnouns.add(word);
		return false;

	}
	
	public static boolean isVerb(String word, ArrayList<String> verbs, ArrayList<String> notverbs) {
		word = word.replaceAll("[<>{}\\]\\[]", "").trim();
		if(!word.matches(".*?[a-z]+.*")){
			return false;
		}
		if(word.matches("\\b("+StateCollector.stop+")\\b")){
			return false;
		}
		if(verbs.contains(word)){
			return true;
		}
		if(notverbs.contains(word)){
			return false;
		}
		WordNetWrapper wnw = new WordNetWrapper(word);
		String pos = wnw.mostlikelyPOS();
		if(pos != null){
			if(pos.compareTo("verb") == 0){
				verbs.add(word);
				return true;
			}else{
				if(wnw.isV() && word.endsWith("ed")){
					verbs.add(word);
					return true;
				}
			}
		}
		notverbs.add(word);
		return false;

	}
	
	public static boolean isAdv(String word, ArrayList<String> adverbs, ArrayList<String> notadverbs) {
		word = word.replaceAll("[<>{}\\]\\[()\\d+-]", "").trim();
		if(word.matches("(not|at-?least|throughout|much)")){
			return true;
		}
		if(word.matches("in.*?(profile|view)")){//covers in-dorsal-view, in-profile
			return true;
		}
		/*mohan code to make as-long-as an adverb*/
		if(word.matches("aslongas")){//covers as-long-as
			return true;
		}
		/*End mohan code*/
		if(word.compareTo("moreorless")==0){
			return true;
		}
		if(word.compareTo("becoming")==0){
			return true;
		}
		if(word.compareTo("±")==0){
			return true;
		}
		if(!word.matches(".*?[a-z]+.*")){
			notadverbs.add(word);
			return false;
		}
		if(word.matches("\\b("+StateCollector.stop+")\\b")){
			notadverbs.add(word);
			return false;
		}
		if(adverbs.contains(word)){
			return true;
		}
		if(notadverbs.contains(word)){
			return false;
		}
		

		WordNetWrapper wnw = new WordNetWrapper(word);
		String pos = wnw.mostlikelyPOS();
		if(pos != null && pos.length()>0){
			if(pos.compareTo("adv") == 0){
				adverbs.add(word);
				return true;
			}
		}else{
			if(word.endsWith("ly")){
				adverbs.add(word);
				return true;
			}
		}
		notadverbs.add(word);
		return false;
	}
	
	/**
	 * 5-{merous}
	 * changed from return a string to an array of two strings nov 14, 2012
	 * @param w
	 * @return null if not found; string[2]: string[0]: chara1_or_chara2, string[1]: preferedterm_chara1, perferredterm_chara2
	 */
	public static String[] lookupCharacter(String w, Connection conn, Hashtable<String, String[]> characterhash, String glosstable, String prefix) {
		if(w.trim().length()==0) return null;
		if(w.indexOf(" ")>0) w = w.substring(w.lastIndexOf(" ")+1).trim();
		w = w.replaceAll("[{}<>()]", "").replaceAll("\\d+[–-]", "_").replaceAll("[–_]", "-")./*replaceAll(" ", "").*/replaceAll("_+", "_");//"(3-)5-merous" =>_merous
		w = w.replaceFirst(".*?_(?=[a-z]+$)", ""); //_or_ribbed
		String wc = w;
		String[] ch = characterhash.get(w);
		if(ch != null){
			return ch;
		}else{
			ch = null;
			if(w.endsWith("shaped")){
				//return "shape";
				w = w.replaceFirst("shaped", "-shaped");
			}

			if(w.indexOf('-')>0){
				String[] ws = w.split("-+");
				w = ws[ws.length-1];
			}
			ch = lookup(w, conn, characterhash, glosstable, wc, prefix);
			if(ch == null && wc.indexOf('-')>0){//pani_culiform
				ch = lookup(wc.replaceAll("-", ""), conn, characterhash, glosstable, wc, prefix);
			}
		}
		return ch;
	}
	
	/*
	 	public static String lookupCharacter(String w, Connection conn, Hashtable<String, String> characterhash, String glosstable, String prefix) {
		if(w.trim().length()==0) return null;
		if(w.indexOf(" ")>0) w = w.substring(w.lastIndexOf(" ")+1).trim();
		w = w.replaceAll("[{}<>()]", "").replaceAll("\\d+[–-]", "_").replaceAll("[–_]", "-").replaceAll("_+", "_");//"(3-)5-merous" =>_merous
		w = w.replaceFirst(".*?_(?=[a-z]+$)", ""); //_or_ribbed
		String wc = w;
		String ch = characterhash.get(w);
		if(ch != null){
			return ch;
		}else{
			ch = "";
			if(w.endsWith("shaped")){
				return "shape";
			}
			//-tipped, thin-edged, white-edged, etc.
			if(w.endsWith("ed")&& w.contains("-") && Utilities.isNounVerb(w.substring(w.lastIndexOf("-")))){
				return "architecture";
			}
			if(w.indexOf('-')>0){
				String[] ws = w.split("-+");
				w = ws[ws.length-1];
			}
			ch = lookup(w, conn, characterhash, glosstable, wc, prefix);
			if(ch == null && wc.indexOf('-')>0){//pani_culiform
				ch = lookup(wc.replaceAll("-", ""), conn, characterhash, glosstable, wc, prefix);
			}
		}
		return ch;
	}*/

	
	private static boolean isNounVerb(String word) {
		WordNetWrapper wnw = new WordNetWrapper(word);
		if(wnw.formchange() && wnw.isAdj()){return true;}
		return false;
	}

	/**
	 * changed from return a string to an array of two strings nov 14, 2012
	 * @param w
	 * @param conn
	 * @param characterhash
	 * @param glosstable
	 * @param wc
	 * @param prefix
	 * @return null if not found; string[2]: string[0]: chara1_or_chara2, string[1]: preferedterm_chara1, perferredterm_chara2
	 */
	private static String[] lookup(String w, Connection conn,
			Hashtable<String, String[]> characterhash, String glosstable,
			String wc, String prefix) {
		String ch ="";
		String syn = "";
		String[] result = new String[2];
		HashSet<String> chs = new HashSet<String>();
		HashSet<String> syns = new HashSet<String>();
		try{
			Statement stmt = conn.createStatement();
			//check glossarytable
			ResultSet rs = stmt.executeQuery("select term, category, hasSyn from "+glosstable+" where term rlike '^"+w+"(_[0-9])?$' or term rlike '_"+w+"(_[0-9])?$' order by category");
			while(rs.next()){
				String term = rs.getString("term");
				String cat = rs.getString("category");
				chs.add(cat);
				int hassyn = rs.getInt("hasSyn");
				if(hassyn == 1){
					Statement stmt1 = conn.createStatement();
					ResultSet rs1 = stmt1.executeQuery("select term, synonym from "+glosstable.replace("fixed", "syns") + 
							" where synonym ='"+term+"'");
					while(rs1.next()){
						syns.add(rs1.getString("term")+"_"+cat); //find preferred term
					}					
				}

				//if(! ch.matches(".*?(^|_)"+cat+"(_|$).*")){
				//	ch += rs.getString("category").trim().replaceAll("\\s+", "_")+"_or_";
				//}
			}
			//check _term_category table, terms in the table may have number suffix such as linear_1, linear_2, 
			String q = "select term, category, hasSyn from "+prefix+"_term_category where term rlike '^"+w+"(_[0-9])?$' and category !='structure' order by category";
			rs = stmt.executeQuery(q);
			while(rs.next()){
				String term = rs.getString("term");
				String cat = rs.getString("category");
				chs.add(cat);
				int hassyn = rs.getInt("hasSyn");
				if(hassyn == 1){
					Statement stmt1 = conn.createStatement();
					ResultSet rs1 = stmt1.executeQuery("select term, synonym from "+prefix+ "_syns" + 
							" where synonym ='"+term+"'");
					while(rs1.next()){
						syns.add(rs1.getString("term")+"_"+cat); //find preferred term
					}					
				}

				//if(! ch.matches(".*?(^|_)"+cat+"(_|$).*")){
				//	ch += rs.getString("decision").trim().replaceAll("\\s+", "_")+"_or_";
				//}
			}			
			rs.close();
			stmt.close();
			String[] charas = chs.toArray(new String[]{});
			String[] synonyms = syns.toArray(new String[]{});
 			Arrays.sort(charas);
 	
 			
 			for(String character: charas){
 				ch += character.replaceAll("\\s+", "_")+"_or_";
 			}
			if(ch.length()>0){
				ch = ch.replaceFirst(Utilities.or+"$", "");				
			}else{
				return null;
			}
			
 			for(String synonym: synonyms){
 				syn += synonym+",";
 			}
 			result[0] = ch;
 			result[1] = syn;
			
			characterhash.put(wc, result);
			return result;
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
		}
		return null;
	}
	/*private static String lookup(String w, Connection conn,
			Hashtable<String, String> characterhash, String glosstable,
			String wc, String prefix) {
		String ch ="";
		HashSet<String> chs = new HashSet<String>();
		try{
			Statement stmt = conn.createStatement();
			//check glossarytable
			ResultSet rs = stmt.executeQuery("select distinct category from "+glosstable+" where term = '"+w+"' or term ='_"+w+"' order by category");
			while(rs.next()){
				String cat = rs.getString("category");
				chs.add(cat);
				//if(! ch.matches(".*?(^|_)"+cat+"(_|$).*")){
				//	ch += rs.getString("category").trim().replaceAll("\\s+", "_")+"_or_";
				//}
			}
			//check _term_category table, terms in the table may have number suffix such as linear_1, linear_2, 
			String q = "select distinct category from "+prefix+"_term_category where term rlike '"+w+"(_[0-9])?' and category !='structure' order by category";
			rs = stmt.executeQuery(q);
			while(rs.next()){
				String cat = rs.getString("category");
				chs.add(cat);
				//if(! ch.matches(".*?(^|_)"+cat+"(_|$).*")){
				//	ch += rs.getString("decision").trim().replaceAll("\\s+", "_")+"_or_";
				//}
			}			
			rs.close();
			stmt.close();
			String[] charas = chs.toArray(new String[]{});
 			Arrays.sort(charas);
 			
 			for(String character: charas){
 				ch += character.replaceAll("\\s+", "_")+"_or_";
 			}
			if(ch.length()>0){
				ch = ch.replaceFirst(Utilities.or+"$", "");
				characterhash.put(wc, ch);
				return ch;
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
		}
		return null;
	}*/
	
	/**
	 * 
	 * @param term
	 * @param conn
	 * @param glosstable
	 * @return
	 */
	public static boolean inGlossary(String term, Connection conn, String glosstable, String prefix) {
		term = term.replaceAll(".*[_-]", "");
		String termcopy = term;
		term = term.replaceFirst("(semi|sub|un)", "");
		boolean in = false;
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select term, category from "+glosstable+" where term ='"+term+"'");
			if(rs.next()){
				String cat = rs.getString("category");
				in = true;
				//nov 14, 2012, Hong: didn't understand why the term need to be inserted to term_category table here
				//Hong: checked and found that it should not be inserted at all.
				//Statement stmt1 = conn.createStatement();
				//stmt1.execute("insert into "+prefix+"_term_category (term, category) values ('"+termcopy+"', '"+cat+"')");
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
		}
		return in;
	}
	
	public static String plural(String b) {
		return Utilities.plurals.get(b);
	}

	/**
	 * break text into correct tokens: 
	 * @param text: that is {often} {concealed} r[p[by] o[(trichomes)]];
	 * @return
	 */
	public static ArrayList<String> breakText(String text) {
		ArrayList<String> tokens = new ArrayList<String>();
		String[] words = text.split("\\s+");
		String t = "";
		int left = 0;
		for(int i = 0; i<words.length; i++){
			String w = words[i];
			if(w.indexOf("[")<0 && w.indexOf("]")<0 && left==0){
				if(!w.matches("\\b(this|have|that|may|be|which|where|when)\\b")){tokens.add(w);};
			}else{
				left += w.replaceAll("[^\\[]", "").length();
				left -= w.replaceAll("[^\\]]", "").length();
				t += w+" ";
				if(left==0){
					tokens.add(t.trim());
					t = "";
				}
			}
		}
		return tokens;
	}
	
	public static String threeingSentence(String str) {
		//hide the numbers in count list: {count~list~9~or~less~} <fin> <rays>
		ArrayList<String> lists = new ArrayList<String>();
		str = hideLists(str, lists);		
		//threeing
		str = str.replaceAll("(?<=\\d)-(?=\\{)", " - "); //this is need to keep "-" in 5-{merous} after 3ed (3-{merous} and not 3 {merous}) 
		//Pattern pattern3 = Pattern.compile("[\\d]+[\\-\\–]+[\\d]+");
		//Pattern pattern3 = Pattern.compile(NumericalHandler.numberpattern);
		//Pattern pattern4 = Pattern.compile("(?<!(ca[\\s]?|diam[\\s]?))([\\d]?[\\s]?\\.[\\s]?[\\d]+[\\s]?[\\–\\-]+[\\s]?[\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]+[\\s]?[\\–\\-]+[\\s]?[\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]/[\\d][\\s]?[\\–\\-][\\s]?[\\d]/[\\d])|(?<!(ca[\\s]?|diam[\\s]?))([\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]/[\\d])");
		//Pattern pattern5 = Pattern.compile("[\\d±\\+\\–\\-\\—°²:½/¼\"“”\\_´\\×µ%\\*\\{\\}\\[\\]=]+");
		//Pattern pattern5 = Pattern.compile("[\\d\\+°²½/¼\"“”´\\×µ%\\*]+(?!~[a-z])");
		Pattern pattern5 = Pattern.compile("[\\d\\+°²½/¼\"“”´\\×µ%\\*]+(?![a-z])"); //single numbers, not including individual "-", would turn 3-branched to 3 branched 
		//Pattern pattern6 = Pattern.compile("([\\s]*0[\\s]*)+(?!~[a-z])"); //condense multiple 0s.
		Pattern pattern6 = Pattern.compile("(?<=\\s)[0\\s]+(?=\\s)");
		//Pattern pattern5 = Pattern.compile("((?<!(/|(\\.[\\s]?)))[\\d]+[\\-\\–]+[\\d]+(?!([\\–\\-]+/|([\\s]?\\.))))|((?<!(\\{|/))[\\d]+(?!(\\}|/)))");
         //[\\d±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×\\s,µ%\\*\\{\\}\\[\\]=(<\\{)(\\}>)]+
		Pattern pattern7 = Pattern.compile("[(\\[]\\s*\\d+\\s*[)\\]]"); // deal with ( 2 ), (23) is dealt with by NumericalHandler.numberpattern
		
		Matcher	 matcher1 = NumericalHandler.numberpattern.matcher(str);
        str = matcher1.replaceAll("0");
		matcher1.reset();
         
         /*matcher1 = pattern4.matcher(str);
         str = matcher1.replaceAll("0");
         matcher1.reset();*/
         
         matcher1 = pattern5.matcher(str);//single numbers
         str = matcher1.replaceAll("0");
         matcher1.reset();
         
         /* should not remove space around 0, because: pollen 70-80% 3-porate should keep 2 separate numbers: 70-80% and 3-porate
		* 
         String scptemp = str;
         matcher1 = pattern6.matcher(str);//remove space around 0
         str = matcher1.replaceAll("0");
         if(!scptemp.equals(str)){
		   System.out.println();
         }
         matcher1.reset();*/
         
         matcher1 = pattern7.matcher(str);//added for (2)
         str = matcher1.replaceAll("0");
         matcher1.reset();
         //further normalization
         
         
         //3 -{many} or 3- {many}=> {3-many}
         str = str.replaceAll("0\\s*-\\s*", "0-").replaceAll("0(?!~[a-z])", "3").replaceAll("3\\s*[–-]\\{", "{3-").replaceAll("±(?!~[a-z])","{moreorless}").replaceAll("±","moreorless"); //stanford parser gives different results on 0 and other numbers.
         
         //2-or-{3-lobed} => {2-or-3-lobed}
         str = str.replaceAll("(?<=-(to|or)-)\\{", "").replaceAll("[^\\{]\\b(?=3-(to|or)-3\\S+\\})", " {");
		
         //unhide count list
         str = unCountLists(str, lists);
         return str;
	}
	
    /*public static int hasUnmatchedBracket(String text, String lbracket, String rbracket) {
    	int count = 0;
    	String[] tokens = text.split("\\s+");
    	for(String t: tokens){
    		if(t.equals(lbracket)) count++;
    		if(t.equals(rbracket)) count--;
    	}
		return count;
    }*/

    public static int hasUnmatchedBracket(String text, String lbracket, String rbracket) {
    	if(lbracket.equals("[")) lbracket = "\\[";
    	if(lbracket.equals("]")) lbracket = "\\]";
    	
    	int left = text.replaceAll("[^"+lbracket+"]", "").length();
    	int right = text.replaceAll("[^"+rbracket+"]", "").length();
    	if(left > right) return 1;
    	if(left < right) return -1;
		return 0;
	}
    
    public static boolean hasUnmatchedBrackets(String text) {
    	//String[] lbrackets = new String[]{"\\[", "(", "{"};
    	//String[] rbrackets = new String[]{"\\]", ")", "}"};
    	String[] lbrackets = new String[]{"\\[", "("};
    	String[] rbrackets = new String[]{"\\]", ")"};
    	for(int i = 0; i<lbrackets.length; i++){
    		int left1 = text.replaceAll("[^"+lbrackets[i]+"]", "").length();
    		int right1 = text.replaceAll("[^"+rbrackets[i]+"]", "").length();
    		if(left1!=right1) return true;
    	}
		return false;
	}
    
    /**
     * if bracket is left, then refresh the index of a new positive count
     * if bracket is right, return the first index with a negative count
     * @param bracket
     * @param str
     * @return index of unmatched bracket in str
     */
	public static int indexOfunmatched(char bracket, String str) {
		int cnt = 0;
		char l = '('; char r=')';
		switch(bracket){
			case '(':  l = '('; r =')'; break;
			case '[': l = '['; r =']'; break;
			case ')':  l = '('; r =')'; break;
			case ']': l = '['; r =']'; break;
		}		
		
		if(bracket == r){
			for(int i = 0; i < str.length(); i++) {
			    if(str.charAt(i)== l){
			    	cnt++;
			    }else if(str.charAt(i) == r){
			    	cnt--; 
			    }			
			    if(cnt<0) return i; //first index with negative count
			}
		}

		if(bracket == l){
			int index = -1;
			for(int i = 0; i < str.length(); i++) {
			    if(str.charAt(i)== l){
			    	cnt++;
			    	index = i;
			    }else if(str.charAt(i) == r){
			    	cnt--; 
			    }			
			    if(cnt==0) index = -1; //first index with negative count
			}
			return index;
		}
		return -1;
	}
	
	/**
	 * hide lists such as
	 * {upper} {pharyngeal} <tooth> <plates_4_and_5>
	 * count~list~2~to~4
	 * so the numbers will not be turned into 3.
	 * @param str
	 * @param countlists
	 * @return
	 */
	private static String hideLists(String str,
			ArrayList<String> lists) {
		if(str.contains("count~list~") || str.matches(".*?<\\S+_\\d.*")){
			String newstr = "";
			String[] tokens = str.split("\\s+");
			int count = 0;
			for(String t: tokens){
				if(t.indexOf("count~list~")>=0 || t.matches("<\\S+_\\d.*")){
					newstr +="# ";
					lists.add(t);
					count++;
				}else{
					newstr +=t+" ";
				}
			}			
			return newstr.trim();
		}else{
			return str;
		}
	}

	private static String unCountLists(String str, ArrayList<String> lists) {
		if(str.contains("#")){
			String newstr = "";
			String[] tokens = str.split("\\s+");
			int count = 0;
			for(String t: tokens){
				if(t.contains("#")){
					newstr += lists.get(count)+" ";
					count++;
				}else{
					newstr +=t+" ";
				}
			}
			return newstr.trim();
		}else{
			return str;
		}
	}

	
	/**remove all bracketed text such as "leaves large (or small as in abc)"
	 * do not remove brackets that are part of numerical expression : 2-6 (-10)
	 * @param str: "leaves large (or small as in abc)"
	 * @return: "leaves large"
	 */
		public static String handleBrackets(String str) {
			//remove nested brackets left by pl such as (petioles (2-)4-8 cm)
			//String p1 ="\\([^()]*?[a-zA-Z][^()]*?\\)";
			//String p2 = "\\[[^\\]\\[]*?[a-zA-Z][^\\]\\[]*?\\]";
			//String p3 = "\\{[^{}]*?[a-zA-Z][^{}]*?\\}";				
			if(str.matches(".*?\\(.*?[a-zA-Z].*?\\).*") || str.matches(".*?\\[.*?[a-zA-Z].*?\\].*")){ 
				String[] pretokens = str.split("\\s+");
				str = Utilities.threeingSentence(str);
				String[] tokens = str.split("\\s+");
				StringBuffer bracketfree = new StringBuffer();
				boolean inbracket = false;
				for(int i=0; i<tokens.length; i++){
					if(tokens[i].matches("[(\\[].*")){
						inbracket = true;
					}
					if(!inbracket){
						if(tokens[i].compareTo("3")==0){
							bracketfree.append(pretokens[i]+" ");
						}else{
							bracketfree.append(tokens[i]+" ");
						}
					}												
					if(tokens[i].matches(".*[)\\]]")){
						inbracket = false;
					}
				}
				str = bracketfree.toString().trim();
				if(str.matches(".*?\\(\\s+?\\s+\\).*")){//2n=20( ? ), 30 => 2n=20?, 30
					str = str.replaceAll("\\(\\s+?\\s+\\)", "?");
				}
				//str = str.replaceAll(p1, "").replaceAll(p2, "").replaceAll("\\s+", " ").trim();					
			}
			return str;
		}


	public static void main(String[] argv){
		//Utilities.lookupCharacter(w, conn, characterhash)
		//System.out.println(Utilities.isNoun(",", new ArrayList<String>()));
		//System.out.println(Utilities.plural("disc"));
		//System.out.println(Utilities.isAdv("much", new ArrayList<String>()));
		System.out.println(Utilities.indexOfunmatched(']', "2-]5-20[-30+]"));
	}
}
