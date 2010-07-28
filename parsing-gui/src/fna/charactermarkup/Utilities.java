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

/**
 * @author hongcui
 *
 */
public class Utilities {
	
	public static boolean isAdv(String word) {
		word = word.replaceAll("[<>{}\\]\\[]", "").trim();
		if(!word.matches(".*?[a-z]+.*")){
			return false;
		}
		if(word.matches("\\b("+StateCollector.stop+")\\b")){
			return false;
		}
		WordNetWrapper wnw = new WordNetWrapper(word);
		String pos = wnw.mostlikelyPOS();
		if(pos != null){
			return pos.compareTo("adv") == 0;
		}else{
			return word.endsWith("ly");
		}
	}
	
	/**
	 * 
	 * @param w
	 * @return null if not found
	 */
	public static String lookupCharacter(String w, Connection conn, Hashtable<String, String> characterhash) {
		
		w = w.replaceAll("[{}<>]", "");
		String wc = w;
		String ch = characterhash.get(w);
		if(ch != null){
			return ch;
		}else{
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
					ch = rs.getString("category")+"/";
				}
				if(ch != null){
					ch = ch.replaceFirst("/$", "");
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
