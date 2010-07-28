/**
 * 
 */
package fna.charactermarkup;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author hongcui
 *
 */
public class MyPOSTagger {
	static protected Connection conn = null;
	static protected String username = "root";
	static protected String password = "root";
	static public String numberpattern = "[ ()\\[\\]\\-\\\\d\\.×\\+°˛Ŋ/ŧ\\*/%]{4,}";
	private ArrayList<String> chunkedtokens = null;
	private ArrayList<String> charactertokensReversed = null;
	public static Hashtable<String, String> characterhash = new Hashtable<String, String>();

	/**
	 * 
	 */
	public MyPOSTagger(Connection conn) {
		this.conn = conn;
	}
	
	private void lookupCharacters(String str) {
		if(str.trim().length() ==0){
			return;
		}
		this.chunkedtokens = new ArrayList<String>(Arrays.asList(str.split("\\s+")));
		this.charactertokensReversed = new ArrayList<String>();
		boolean save = false;
		ArrayList<String> saved = new ArrayList<String>();
		boolean ambiguous = false;
		ArrayList<String> amb = new ArrayList<String>();
		for(int i = this.chunkedtokens.size()-1; i>=0; i--){
			String word = this.chunkedtokens.get(i);
			if(word.indexOf('{')>=0 && word.indexOf('<')<0){
				String ch = Utilities.lookupCharacter(word, conn, this.characterhash); //remember the char for this word (this word is a word before (to|or|\\W)
				if(ch==null){
					this.charactertokensReversed.add(word.replaceAll("[{}]", "")); //
				}else{
					this.charactertokensReversed.add(ch); //color
					if(ch.indexOf("/")>0){
						ambiguous = true;
						amb.add(this.chunkedtokens.size()-1-i+"");
					}
					if(save){
						save(saved, this.chunkedtokens.size()-1-i, ch); 
					}
					save = false;
				}
			}else if (word.indexOf('<')>=0){
				this.charactertokensReversed.add("#");
			}else if(word.matches("(to|or)")){
				this.charactertokensReversed.add("@"); //to|or|,
				save = true;
			}else if(word.matches("\\W")){
				this.charactertokensReversed.add(word); //,;.
				save = true;
			}else{
				this.charactertokensReversed.add("%");
			}
		}
		
		//deal with a/b characters
		if(ambiguous){
			Iterator<String> it = amb.iterator();
			while(it.hasNext()){
				int i = Integer.parseInt(it.next());
				Pattern p = Pattern.compile("("+this.charactertokensReversed.get(i).replaceAll("/", "|")+")");
				Matcher m = p.matcher(lastSaved(saved, i));
				if(m.matches()){
					this.charactertokensReversed.set(i, m.group(1));
				}else{
					m = p.matcher(nextSaved(saved, i));
					if(m.matches()){
						this.charactertokensReversed.set(i, m.group(1));
					}
				}
			}
		}
	}
	
	private String lastSaved(ArrayList<String> saved, int index){
		for(int i = index-1; i >=0; i--){
			if(saved.get(i).trim().length()>0){
				return saved.get(i);
			}
		}
		return "";
	}
	
	private String nextSaved(ArrayList<String> saved, int index){
		for(int i = index+1; i <saved.size(); i++){
			if(saved.get(i).trim().length()>0){
				return saved.get(i);
			}
		}
		return "";
	}
	
	
	
	private void save(ArrayList<String> saved, int index, String ch){
		while(saved.size()<=index){
			saved.add("");
		}
		saved.set(index, ch);
	}
	/**
	 * put a list of states of the same character connected by to/or in a chunk
	 * color, color, or color
	 * color or color to color
	 * 
	 * {color-blue-to-red}
	 * 
	 */
	private String normalizeCharacterLists(){
		//charactertokens.toString
		String list = "";
		String result = "";
		for(int i = this.charactertokensReversed.size() -1; i>=0; i--){
			list+=this.charactertokensReversed.get(i)+" ";
		}
		list = list.trim()+" "; //need to have a trailing space
		
		//pattern match: collect state one by one
		int base = 0;
		Pattern pt = Pattern.compile("(.*?\\b)(([0-9a-z-]+ly )*([a-z-]+ )+([@,;\\.] )+\\s*)(([a-z-]+ )*(\\4)+[@,;\\.%].*)");//
		Matcher mt = pt.matcher(list);
		while(mt.matches()){
			int start = (mt.group(1).trim()+" a").trim().split("\\s+").length+base-1; //"".split(" ") == 1
			String l = mt.group(2);
			String ch = mt.group(4).trim();
			list = mt.group(6);
			Pattern p = Pattern.compile("(([a-z-]+ )*([a-z-]+ )+([@,;\\.] )+\\s*)(([a-z-]+ )*(\\3)+[@,;\\.%].*)");//merely shape, @ shape
			Matcher m = p.matcher(list);
			while(m.matches()){
				l += m.group(1);
				list = m.group(5);
				m = p.matcher(list);
			}
			l += list.replaceFirst("[@,;\\.%].*$", "");//take the last seg from the list
			int end = start+(l.trim()+" b").trim().split("\\s+").length-1;
			if(! l.matches(".*?@[^,;\\.]*") && l.matches(".*?,.*")){ //the last state is not connected by or/to, then it is not a list
				start = end;
			}
				
			
			list = list.replaceFirst("^.*?(?=[@,;\\.%])", "");
			mt = pt.matcher(list);
			
			for(int i = base; i<start; i++){
				result += this.chunkedtokens.get(i)+" ";
			}
			if(end>start){ //if it is a list
				result += "{"+ch+"-list-";
				for(int i = start; i<end; i++){
					result += this.chunkedtokens.get(i).trim().replaceAll("[{}]", "").replaceAll("[,;\\.]", "punct")+"-";
				}
				result = result.replaceFirst("-$", "}")+" ";
			}
			base = end;
		}
		
		for(int i = base; i<(list.trim()+" a").trim().split("\\s+").length+base-1; i++){
			result += this.chunkedtokens.get(i)+" ";
		}
		
		/*Pattern pt = Pattern.compile("(.*?)((?:(?:@|;|,|\\.)? (?:\\w+ )*(\\w+) )+@ (?:\\w+ )*(?:\\3) [\\.,;]?)(.*)"); //*brownish color , color color @ color @ color . *
		Matcher m = pt.matcher(list);
		int base = 0;
		while(m.matches()){
			int start = m.group(1).trim().split("\\s+").length+base;
			String l = m.group(2).trim();
			int end = start+l.split("\\s+").length;
			String ch = m.group(3);
			list = m.group(4).trim();
			m = pt.matcher(list);
			
			for(int i = base; i<start; i++){
				result += this.chunkedtokens.get(i)+" ";
			}
			result += "{"+ch+"-list-";
			for(int i = start; i<end; i++){
				result += this.chunkedtokens.get(i).replaceAll("[{}]", "")+"-";
			}
			result = result.replaceFirst("-$", "}")+" ";			
			base = end;
		}
		for(int i = base; i<list.split("\\s+").length+base; i++){
			result += this.chunkedtokens.get(i)+" ";
		}*/
		
		return result.trim();
	}
	/**
	 * 		//insert our POS tags to segments (simple or complex: new segmentation)
			 //output POSed segments to a database table and to the posed file	
			  * str is markedsent

	 */
		public String POSTag(String str){
			//12-{pinnately} or -{palmately} {lobed} => {12-pinnately-or-palmately} {lobed}
			if(str.indexOf(" -{")>=0){
				str = str.replaceAll("\\s+or\\s+-\\{", "-or-").replaceAll("\\s+to\\s+-\\{", "-to-").replaceAll("\\s+-\\{", "-{");
			}
			try{
				Statement stmt = conn.createStatement();
				Statement stmt1 = conn.createStatement();
				Statement stmt2 = conn.createStatement();
				Statement stmt3 = conn.createStatement();
								
	            //Pattern pattern3 = Pattern.compile("[\\d]+[\\-\\]+[\\d]+");
	            Pattern pattern3 = Pattern.compile(this.numberpattern);
				//Pattern pattern4 = Pattern.compile("(?<!(ca[\\s]?|diam[\\s]?))([\\d]?[\\s]?\\.[\\s]?[\\d]+[\\s]?[\\\\-]+[\\s]?[\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]+[\\s]?[\\\\-]+[\\s]?[\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]/[\\d][\\s]?[\\\\-][\\s]?[\\d]/[\\d])|(?<!(ca[\\s]?|diam[\\s]?))([\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]/[\\d])");
				//Pattern pattern5 = Pattern.compile("[\\dą\\+\\\\-\\°˛:Ŋ/ŧ\"\\_´\\×ĩ%\\*\\{\\}\\[\\]=]+");
				Pattern pattern5 = Pattern.compile("[\\d\\+°˛Ŋ/ŧ\"´\\×ĩ%\\*]+");
				Pattern pattern6 = Pattern.compile("([\\s]*0[\\s]*)+");
				//Pattern pattern5 = Pattern.compile("((?<!(/|(\\.[\\s]?)))[\\d]+[\\-\\]+[\\d]+(?!([\\\\-]+/|([\\s]?\\.))))|((?<!(\\{|/))[\\d]+(?!(\\}|/)))");
	           //[\\dą\\+\\\\-\\°.˛:Ŋ/ŧ\"\\_;x´\\×\\s,ĩ%\\*\\{\\}\\[\\]=(<\\{)(\\}>)]+
				Matcher	 matcher1 = pattern3.matcher(str);
	           str = matcher1.replaceAll(" 0 ");
	           matcher1.reset();
	           
	           /*matcher1 = pattern4.matcher(str);
	           str = matcher1.replaceAll("0");
	           matcher1.reset();*/
	           
	           matcher1 = pattern5.matcher(str);
	           str = matcher1.replaceAll("0");
	           matcher1.reset();
	           
	           matcher1 = pattern6.matcher(str);
	           str = matcher1.replaceAll(" 0 ");
	           matcher1.reset();
	           //3 -{many} => {3-many}
	           str = str.replaceAll("0\\s+-", "0-").replaceAll("0", "3").replaceAll("3\\s*[-]\\{", "{3-").replaceAll("ą","{moreorless}"); //stanford parser gives different results on 0 and other numbers.
	           
	           //str = str.replaceAll("}>", "/NN").replaceAll(">}", "/NN").replaceAll(">", "/NN").replaceAll("}", "/JJ").replaceAll("[<{]", "");
	           
	           lookupCharacters(str);//populate charactertokens
	           if(str.indexOf(" or ")>=0 ||str.indexOf(" to ")>=0){
	        	   str = normalizeCharacterLists(); //a set of states of the same character connected by ,/to/or => {color-blue-to-red}
	           }
	           if(str.matches(".*? as\\s+[\\w{}<>]+\\s+as .*")){
	           	   str = normalizeAsAs(str);
	           }
	           StringBuffer sb = new StringBuffer();
	           /*Pattern pattern7 = Pattern.compile("(.*?)([<{]*)([0-9a-zA-Z-]+)[}>]*(.*)");
	           Matcher m = pattern7.matcher(str);
	           while ( m.matches()){
	        	   sb.append(m.group(1));
	        	   String pos = m.group(2);
	        	   String word = m.group(3);
	        	   str = m.group(4);*/
    		   	   //m = pattern7.matcher(str);
    		   	   //continue;
	           String[] tokens = str.split("\\s+");
	           for(int i = 0; i<tokens.length; i++){
	        	   String word = tokens[i];
	        	   String pos = "";
	        	   if(word.endsWith("}")){
	        		   pos = "{";
	        	   }else if(word.endsWith(">")){
	        		   pos = "<";
	        	   }
	        	   word = word.replaceAll("[<>{}]", "");
	        	   ResultSet rs1 = stmt1.executeQuery("select * from wordpos4parser where word='"+word+"'");
	       		   String p = "";
	       		   if(rs1.next()){
	       			   p = rs1.getString(2);
	       		   }
	        	   if(word.endsWith("ly")){
	        		   sb.append(word+"/RB ");
	        	   //}else if(word.endsWith("ing")){
	        		//   sb.append(word+" ");
	        	   }else if(word.matches("("+ChunkedSentence.units+")")){
	       			   sb.append(word+"/NNS ");
	       		   }else if(word.matches("as-\\S+")){ //as-wide-as
	       			   sb.append(word+"/IN ");
	       		   }else if(p.contains("p")){ //<inner> larger.
	       				//System.out.println(rs1.getString(2));
	       				sb.append(word+"/NNS ");
	       		   }else if(p.contains("s") || pos.indexOf('<') >=0){
	       				sb.append(word+"/NN ");
	       		   }else if(p.contains("b")|| pos.indexOf('{') >=0){
	       			   	//ResultSet rs3 = stmt1.executeQuery("select word from wordpos4parser where word='"+word+"' and certaintyl>5");
	       				ResultSet rs2 = stmt1.executeQuery("select word from Brown.wordfreq where word='"+word+"' and freq>79");//1/largest freq in wordpos = 79/largest in brown
	       				if(rs2.next()){
	       					sb.append(word+" ");
	       				//}else if(word.indexOf("3-")>=0){
	       				//	sb.append(word+"/CD");
	       				}else{
	       					sb.append(word+"/JJ ");
	       				}
	       		   }else{
	       				sb.append(word+" ");
	       		   }
	       		   //m = pattern7.matcher(str);
	       		}
	           	//sb.append(str);
	       		str = sb.toString().trim();
	       		str = str.replaceAll("(?<=[a-z])\\s+[_-]\\s+(?=[a-z])", "-").replaceAll("/[A-Z]+\\s*[-]\\s*", "-").replaceAll("\\d-\\s+(?=[a-z])", "3-"); //non -septate/JJ or linear/JJ _ovoid/JJ
	       		str = str.replaceAll("[\\[\\(]", " -LRB-/-LRB- ").replaceAll("[\\)\\]]", " -RRB-/-RRB- ").replaceAll("\\s+", " ").trim(); 
	       		str = str.replaceAll("moreorless/JJ","moreorless/RB");
	       		return str;
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return "";
	}
	/**
	 * as wide as => as-wide-as/IN
	 * as wide as or/to wider than inner
	 * as wide as inner
	 * as wide as long
	 * @return
	 */	
	private String normalizeAsAs(String str) {
		String result = "";
		Pattern p = Pattern.compile("(.*?\\b)(as\\s+[\\w{}<>]+\\s+as)(\\b.*)");
		Matcher m = p.matcher(str);
		while(m.matches()){
			result+=m.group(1);
			result+="{"+m.group(2).replaceAll("\\s+", "-").replaceAll("[{}<>]", "")+"}";
			str = m.group(3);
			m = p.matcher(str);
		}
		result+=str;
		return result.trim();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
