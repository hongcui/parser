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
import java.util.regex.*;
/**
 * @author hongcui
 *
 */
public class Utilities {
	public static String or = "_or_";
	
	public static Hashtable<String, String> singulars = new Hashtable<String, String>();
	public static boolean debug = false;
	//special cases



	public static String toSingular(String word){
		String s = "";
		word = word.toLowerCase().replaceAll("\\W", "");
		//check cache
		singulars.put("valves", "valve");
		singulars.put("media", "media");
		singulars.put("frons", "frons");
		singulars.put("species", "species");
		singulars.put("teeth", "tooth");
		singulars.put("bases", "base");
		singulars.put("series", "series");
		s = singulars.get(word);
		if(s!=null) return s;
		
		//adverbs
		if(word.matches("[a-z]{3,}ly")){
			singulars.put(word, word);
			return word;
		}
		
		String wordcopy = word;
		wordcopy = checkWN4Singular(wordcopy);
		if(wordcopy != null && wordcopy.length()==0){
			return word;
		}else if(wordcopy!=null){
			singulars.put(word, wordcopy);
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
			Pattern p8 = Pattern.compile("(.*?)s$");
			
			Matcher m1 = p1.matcher(word);
			Matcher m2 = p2.matcher(word);
			Matcher m3 = p3.matcher(word);
			Matcher m4 = p4.matcher(word);
			Matcher m5 = p5.matcher(word);
			Matcher m6 = p6.matcher(word);
			Matcher m7 = p7.matcher(word);
			Matcher m8 = p8.matcher(word);
		
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
			}else if(m8.matches()){
			  s = m8.group(1);
			}
		  
		  if(s != null){
			if(debug) System.out.println("["+word+"]'s singular is "+s);
			singulars.put(word, s);
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
	            StreamGobbler errorGobbler = new 
	                StreamGobbler(proc.getErrorStream(), "ERROR", errors, outputs);            
	            
	            // any output?
	            StreamGobbler outputGobbler = new 
	                StreamGobbler(proc.getInputStream(), "OUTPUT", errors, outputs);
	                
	            // kick them off
	            errorGobbler.start();
	            outputGobbler.start();
	                                    
	            // any error???
	            int exitVal = proc.waitFor();
	            //System.out.println("ExitValue: " + exitVal);

	            StringBuffer sb = new StringBuffer();
	            for(int i = 0; i<errors.size(); i++){
	            	sb.append(errors.get(i)+" ");
	            	sb.append(outputs.get(i)+" ");
	            }
	            return sb.toString();
				
		  	}catch(Exception e){
		  		e.printStackTrace();
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
		Pattern p = Pattern.compile("Overview of noun (\\w+) (.*)");
		Matcher m = p.matcher(result);
		while(m.matches()){
			 t += m.group(1)+" ";
			 result = m.group(2);
			 m = p.matcher(result);
		}
		if (t.length() ==0){//word is not a noun
			return "";
		} 
		String[] ts = t.trim().split("\\s+"); //if multiple singulars (bases =>basis and base, pick the first one
		for(int i = 0; i<ts.length; i++){
			if(ts[i].compareTo(word)==0){//the original word is a singular
				return "";
			}
		}
		return ts[0];
	}
	 

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
		word = word.replaceAll("[<>{}\\]\\[()\\d+-]", "").trim();
		if(word.matches("not")){
			return true;
		}
		if(word.compareTo("±")==0){
			return true;
		}
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
		return false;
	}
	
	/**
	 * 5-{merous}
	 * @param w
	 * @return null if not found
	 */
	public static String lookupCharacter(String w, Connection conn, Hashtable<String, String> characterhash) {
		
		w = w.replaceAll("[{}<>()]", "").replaceAll("\\d+[–-]", "_").replaceAll("–", "-").replaceAll(" ", "").replaceAll("_+", "_");//"(3-)5-merous" =>_merous
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
			if(w.indexOf('-')>0){
				String[] ws = w.split("-+");
				w = ws[ws.length-1];
			}
			try{
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select distinct category from fnaglossaryfixed where term = '"+w+"' or term ='_"+w+"'");
				while(rs.next()){
					String cat = rs.getString("category");
					if(! ch.matches(".*?(^|_)"+cat+"(_|$).*")){
						ch += rs.getString("category").trim().replaceAll("\\s+", "_")+"_or_";
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
	
	public static void main(String[] argv){
		//Utilities.lookupCharacter(w, conn, characterhash)
		System.out.println(Utilities.toSingular("margins"));
	}

}
