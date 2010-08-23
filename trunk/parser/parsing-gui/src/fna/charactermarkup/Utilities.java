/**
 * 
 */
package fna.charactermarkup;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;

import java.util.Hashtable;

import fna.parsing.state.StateCollector;
import fna.parsing.state.WordNetWrapper;
import java.util.ArrayList;
/**
 * @author hongcui
 *
 */
public class Utilities {
	public static String or = "_or_";
	
	public static boolean isNoun(String word, ArrayList<String> nouns){
		word = word.replaceAll("[<>{}\\]\\[]", "");
		if(!word.matches(".*?[a-z]+.*")){
			return false;
		}
		if(word.matches("\\b("+StateCollector.stop+")\\b")){
			return false;
		}
		if(nouns.contains(word)){
			return true;
		}
		WordNetWrapper wnw = new WordNetWrapper(word);
		String pos = wnw.mostlikelyPOS();
		if(pos != null){
			if(pos.compareTo("noun") == 0){
				nouns.add(word);
				return true;
			}
		}
		return false;

	}
	
	public static boolean isVerb(String word, ArrayList<String> verbs) {
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
			return false;

	}
	
	public static boolean isAdv(String word, ArrayList<String> adverbs) {
		word = word.replaceAll("[<>{}\\]\\[]", "").trim();
		if(!word.matches(".*?[a-z]+.*")){
			return false;
		}
		if(word.matches("\\b("+StateCollector.stop+")\\b")){
			return false;
		}
		if(adverbs.contains(word)){
			return true;
		}
		if(word.compareTo("moreorless")==0){
			return true;
		}
		WordNetWrapper wnw = new WordNetWrapper(word);
		String pos = wnw.mostlikelyPOS();
		if(pos != null){
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
		return false;
	}
	
	/**
	 * 5-{merous}
	 * @param w
	 * @return null if not found
	 */
	public static String lookupCharacter(String w, Connection conn, Hashtable<String, String> characterhash) {
		
		w = w.replaceAll("[{}<>]", "").replaceAll("\\d+[–-]", "_").replaceAll("–", "-");//"5-merous" =>_merous
		String wc = w;
		String ch = characterhash.get(w);
		if(ch != null){
			return ch;
		}else{
			ch = "";
			if(w.endsWith("shaped")){
				return "shape";
			}
			if(w.indexOf('-')>0){
				String[] ws = w.split("-+");
				w = ws[ws.length-1];
			}
			try{
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select category from fnaglossaryfixed where term ='"+w+"'");
				while(rs.next()){
					String cat = rs.getString("category");
					if(! ch.matches(".*?(^|_)"+cat+"(_|$).*")){
						ch += rs.getString("category")+"_or_";
					}
				}
				rs.close();
				stmt.close();
				if(ch.length()>0){
					ch = ch.replaceFirst(Utilities.or+"$", "");
					characterhash.put(wc, ch);
					return ch;
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return null;
	}

}
