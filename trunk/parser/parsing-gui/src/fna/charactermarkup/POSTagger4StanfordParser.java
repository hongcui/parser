 /* $Id$ */
/**
 * 
 */
package fna.charactermarkup;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import fna.parsing.ApplicationUtilities;
import fna.parsing.MainForm;




/**
 * @author hongcui
 *
 */
@SuppressWarnings({  "unused","static-access" })
public class POSTagger4StanfordParser {
	private static final Logger LOGGER = Logger.getLogger(POSTagger4StanfordParser.class);
	static protected Connection conn = null;
	static protected String username = "root";
	static protected String password = "root";
	private ArrayList<String> chunkedtokens = null;
	private ArrayList<String> charactertokensReversed = null;
	//public static Hashtable<String, String> characterhash = new Hashtable<String, String>();
	private boolean printCharacterList = true;
	private boolean printColorList = false;
	private String tableprefix = null;
	private String glosstable = null;
	private String src;
	public static String comprepstring = "according-to|ahead-of|along-with|apart-from|as-for|aside-from|as-per|as-to-as-well-as|away-from|because-of|but-for|by-means-of|close-to|contrary-to|depending-on|due-to|except-for|forward-of|further-to|in-addition-to|in-between|in-case-of|in-face-of|in-favour-of|in-front-of|in-lieu-of|in-spite-of|instead-of|in-view-of|near-to|next-to|on-account-of|on-behalf-of|on-board|on-to|on-top-of|opposite-to|other-than|out-of|outside-of|owing-to|preparatory-to|prior-to|regardless-of|save-for|thanks-to|together-with|up-against|up-to|up-until|vis-a-vis|with-reference-to|with-regard-to";
	private static Pattern compreppattern = Pattern.compile("\\{?(according-to|ahead-of|along-with|apart-from|as-for|aside-from|as-per|as-to-as-well-as|away-from|because-of|but-for|by-means-of|close-to|contrary-to|depending-on|due-to|except-for|forward-of|further-to|in-addition-to|in-between|in-case-of|in-face-of|in-favour-of|in-front-of|in-lieu-of|in-spite-of|instead-of|in-view-of|near-to|next-to|on-account-of|on-behalf-of|on-board|on-to|on-top-of|opposite-to|other-than|out-of|outside-of|owing-to|preparatory-to|prior-to|regardless-of|save-for|thanks-to|together-with|up-against|up-to|up-until|vis-a-vis|with-reference-to|with-regard-to)\\}?");
	private static Pattern colorpattern = Pattern.compile("(.*?)((coloration|color)\\s+%\\s+(?:(?:coloration|color|@|%) )*(?:coloration|color))\\s((?![^,;()\\[\\]]*[#]).*)");
	//private Pattern viewptn = Pattern.compile( "(.*?\\b)(in\\s+[a-z_<>{} -]+\\s+[<{]*view[}>]*)(\\s.*)"); to match in dorsal view
	private static Pattern viewptn = Pattern.compile( "(.*?\\b)((?:in|at)\\s+[a-z_<>{} -]*\\s*[<{]*(?:view|profile|closure)[}>]*)(\\s.*)"); //to match in dorsal view and in profile
	private static String countp = "more|fewer|less|\\d+";
	private static Pattern countptn = Pattern.compile("((?:^| |\\{)(?:"+countp+")\\}? (?:or|to) \\{?(?:"+countp+")(?:\\}| |$))");

	//positionptn does not apply to FNA, it may apply to animal descriptions e.g. rib_I, rib_II,
	//private static Pattern positionptn = Pattern.compile("(<(\\S+?)> \\d+(?: and \\d+)?)"); // <{wing}> 1 – 3 cm ;
	private static Pattern positionptn = Pattern.compile("(<(\\S+?)> \\d+(?:(?: and |_)\\d+)?(?!\\s*(?:\\.|/|times)))");//changed to match "4_5", "4 and 5" but not "<structure> 2 / 5" or "<structure> 2 times"

	private static Pattern hyphenedtoorpattern = Pattern.compile("(.*?)((\\d-,\\s*)+ (to|or) \\d-\\{)(.*)");
	private static Pattern bulletpattern  = Pattern.compile("^(and )?([(\\[]\\s*\\d+\\s*[)\\]]|\\d+.)\\s+(.*)"); //( 1 ), [ 2 ], 12.
	private static Pattern distributePrepPattern = Pattern.compile("(^.*~list~)(.*?~with~)(.*?~or~)(.*)");
	private static Pattern areapattern = Pattern.compile("(.*?)([\\d\\.()+-]+ \\{?[cmd]?m\\}?×\\S*\\s*[\\d\\.()+-]+ \\{?[cmd]?m\\}?×?(\\S*\\s*[\\d\\.()+-]+ \\{?[cmd]?m\\}?)?)(.*)");
	//private static Pattern charalistpattern = Pattern.compile("(.*?(?:^| ))(([0-9a-z–\\[\\]\\+-]+ly )*([_a-z-]+ )+([@,;\\.] )+\\s*)(([_a-z-]+ )*(\\4)+([0-9a-z–\\[\\]\\+-]+ly )*[@,;\\.%\\[\\]\\(\\)#].*)");//
	private static Pattern charalistpattern = Pattern.compile("(.*?(?:^| ))(([0-9a-z–\\[\\]\\+-]+ly )*([_a-z-]+ )+[& ]*([`@,;\\.] )+\\s*)(([_a-z-]+ |[0-9a-z–\\[\\]\\+-]+ly )*(\\4)+([0-9a-z–\\[\\]\\+-]+ly )*[`@,;\\.%\\[\\]\\(\\)&#a-z].*)");//
	//private static Pattern charalistpattern2 = Pattern.compile("(([a-z-]+ )*([a-z-]+ )+([0-9a-z–\\[\\]\\+-]+ly )*([@,;\\.] )+\\s*)(([a-z-]+ )*(\\3)+([0-9a-z–\\[\\]\\+-]+ly )*[@,;\\.%\\[\\]\\(\\)#].*)");//merely shape, @ shape
	private static Pattern charalistpattern2 = Pattern.compile("(([a-z-]+ )*([a-z-]+ )+([0-9a-z–\\[\\]\\+-]+ly )*[& ]*([`@,;\\.] )+\\s*)(([a-z-]+ |[0-9a-z–\\[\\]\\+-]+ly )*(\\3)+([0-9a-z–\\[\\]\\+-]+ly )*[`@,;\\.%\\[\\]\\(\\)&#a-z].*)");//merely shape, @ shape
	//mohan declaration of roman numbers
	public static final String roman="i|ii|iii|iv|v|vi|vii|viii|ix|x|xi|xii|xiii|xiv|xv|xvi|xvii|xviii|xix|xx|I|II|III|IV|V|VI|VII|VIII|IX|X|XI|XII|XIII|XIV|XV|XVI|XVII|XVIII|XIX|XX";
	//private Pattern romanptn = Pattern.compile("\\b"+roman+"\\b|\\{?"+roman+"\\}");
	//private Pattern romanptn = Pattern.compile("\\b("+roman+")\\b");
	//private Pattern romanptn = Pattern.compile("(<(\\S+?)> (\\b"+roman+"\\b|\\{?"+roman+"\\}?))");
	private static Pattern romantag = Pattern.compile("\\{?\\b"+roman+"\\b\\}?");
	//private Pattern romanrange = Pattern.compile("(\\d+)-\\b("+roman+")\\b");
	private static Pattern romanrange = Pattern.compile("(\\d+)-<?\\b("+roman+")\\b>?");
	//private Pattern romanptn = Pattern.compile("(<(\\S+?)> \\{?\\b("+roman+")\\b\\}?)");
	private static Pattern romanptn = Pattern.compile("(<(\\S+?)> <?\\{?\\b("+roman+")\\b\\}?>?)"); //to include <ii>
	public static String[] romanno= { "i","ii","iii","iv","v","vi","vii","viii","ix","x","xi","xii","xiii","xiv","xv","xvi","xvii","xviii","xix","xx" };
	//public String[] capromanno= {"I","II","III","IV","V","VI","VII","VIII","IX","X","XI","XII","XIII","XIV","XV","XVI","XVII","XVIII","XIX","XX"};
	//end mohan declaration
	private static Pattern asaspattern = Pattern.compile("(.*?\\b)(as\\s+[\\w{}<>]+\\s+as)(\\b.*)");
	private static Pattern modifierlist = Pattern.compile("(.*?\\b)(\\w+ly\\s+(?:to|or)\\s+\\w+ly)(\\b.*)");
	private static String negations = "not|never|seldom";//not -ly words. -ly words are already treated in character list patterns

	/**
	 * 
	 */
	public POSTagger4StanfordParser(Connection conn, String tableprefix, String glosstable) {
		this.conn = conn;
		this.tableprefix = tableprefix;
		this.glosstable = glosstable;		
	}
	
	/**
	 * 		//insert our POS tags to segments (simple or complex: new segmentation)
			 //output POSed segments to a database table and to the posed file	
			  * str is markedsent

	 */
		protected String POSTag(String str, String src) throws Exception{
			boolean containsArea = false;
			String strcp = str;
			str = StanfordParser.normalizeSpacesRoundNumbers(str);
			this.src = src;
			
			/*str = str.replaceAll("\\b(?<=\\d+) \\. (?=\\d+)\\b", "."); //2 . 5 =>2.5
			str = str.replaceAll("(?<=\\d)\\s+/\\s+(?=\\d)", "/"); // 1 / 2 => 1/2
			str = str.replaceAll("(?<=\\d)\\s+[–-—]\\s+(?=\\d)", "-"); // 1 - 2 => 1-2*/
			/*if(str.indexOf(" -{")>=0){//1–2-{pinnately} or -{palmately} {lobed} => {1–2-pinnately-or-palmately} {lobed}
				str = str.replaceAll("\\s+or\\s+-\\{", "-or-").replaceAll("\\s+to\\s+-\\{", "-to-").replaceAll("\\s+-\\{", "-{");
			}*/

			if(str.matches(".*?-(or|to)\\b.*") || str.matches(".*?\\b(or|to)-.*") ){//1–2-{pinnately} or-{palmately} {lobed} => {1–2-pinnately-or-palmately} {lobed}
				str = str.replaceAll("\\}?-or\\s+\\{?", "-or-").replaceAll("\\}?\\s+or-\\{?", "-or-").replaceAll("\\}?-to\\s+\\{?", "-to-").replaceAll("\\}?\\s+to-\\{?", "-to-").replaceAll("-or\\} \\{", "-or-").replaceAll("-to\\} \\{", "-to-");
			}
			//{often} 2-, 3-, or 5-{ribbed} ; =>{often} {2-,3-,or5-ribbed} ;  635.txt-16
			Matcher m = hyphenedtoorpattern.matcher(str);
			while(m.matches()){
				str = m.group(1)+"{"+m.group(2).replaceAll("[, ]","").replaceAll("\\{$", "")+m.group(5);
				m = hyphenedtoorpattern.matcher(str);
			}
			String scp = str;
			str = str.replaceAll("(?<![\\d(\\[–—-]\\s?)[–—-]+\\s*(?="+NumericalHandler.numberpattern+"\\s+\\W?("+ChunkedSentence.units+")\\W?)", " to "); //fna: tips>-2.5 {mm}
			//if(!scp.equals(str)){
			//	System.out.println();
			//}
			this.chunkedtokens = new ArrayList<String>(Arrays.asList(str.split("\\s+")));
        	str = normalizemodifier(str);//shallowly to deeply pinnatifid: this should be done before other normalization that involved composing new tokens using ~
			//position list does not apply to FNA.			
			if(MainForm.containindexedparts) str = normalizePositionList(str);
			str = normalizeCountList(str+"");

			//lookupCharacters(str);//populate charactertokens
        	lookupCharacters(str, false);//treating -ly as %
	        if(this.charactertokensReversed.contains("color") || this.charactertokensReversed.contains("coloration")){
	        	str = normalizeColorPatterns();
	        	//lookupCharacters(str);
	        }
	        //lookupCharacters(str, true); //treating -ly as -ly
	        if(str.indexOf(" to ")>=0 ||str.indexOf(" or ")>=0){
	        	if(this.printCharacterList){
					System.out.println(str);
				}
	        	//str = normalizeCharacterLists(str); //a set of states of the same character connected by ,/to/or => {color-blue-to-red}
	        	str = normalize(str); 
	        }

	        if(str.matches(".*? as\\s+[\\w{}<>]+\\s+as .*")){
	           str = normalizeAsAs(str);
	        }
	        
	        if(str.matches(".*?(?<=[a-z])/(?=[a-z]).*")){
	        	str = str.replaceAll("(?<=[a-z])/(?=[a-z])", "-");
	        }
	        
	        
	        //10-20(-38) {cm}×6-10 {mm} 
	        
	        
			try{
				Statement stmt = conn.createStatement();
				Statement stmt1 = conn.createStatement();
				String strcp2 = str;
				
				String strnum = null;
				/*
				//if(str.indexOf("}×")>0){//{cm}×
				if(str.indexOf("×")>0){
					containsArea = true;
					String[] area = normalizeArea(str);
					str = area[0]; //with complete info
					strnum = area[1]; //contain only numbers
				}
				*/
		           
		        //deal with (3) as bullet
				m = bulletpattern.matcher(str.trim());
				if(m.matches()){
					str = m.group(3);
				}
				if(str.indexOf("±")>=0){
					str = str.replaceAll("±(?!~[a-z])","{moreorless}").replaceAll("±(?!\\s+\\d)","moreorless");
				}
				/*to match {more} or {less}*/
				if(str.matches(".*?\\b[{<]*more[}>]*\\s+or\\s+[{<]*less[}>]*\\b?.*")){
					str = str.replaceAll("[{<]*more[}>]*\\s+or\\s+[{<]*less[}>]*", "{moreorless}");
				}
				//if(str.matches(".*?\\bin\\s+[a-z_<>{} -]+\\s+[<{]?view[}>]?\\b.*")){//ants: "in full-face view"
				//if(str.matches(".*?\\bin\\s+[a-z_<>{} -]*\\s*[<{]?(view|profile)[}>]?\\b.*")){
				if(str.matches(".*\\b(in|at)\\b.*?\\b(view|profile|closure)\\b.*")){
					Matcher vm = viewptn.matcher(str);
					while(vm.matches()){
						str = vm.group(1)+" {"+vm.group(2).replaceAll("[<>{}]", "").replaceAll("\\s+", "-")+"} "+vm.group(3); 
						vm = viewptn.matcher(str);
					}
				}
				
				if(str.indexOf("×")>0){
					containsArea = true;
					String[] area = normalizeArea(str);
					str = area[0]; //with complete info
					strnum = area[1]; //like str but with numerical expression normalized
				}

				//str = handleBrackets(str);

				//str = Utilities.handleBrackets(str);

				stmt.execute("update "+this.tableprefix+"_markedsentence set rmarkedsent ='"+str+"' where source='"+src+"'");	
				
				if(containsArea){
					str = strnum;

					//str = handleBrackets(str);

					//str = Utilities.handleBrackets(str);

				}
				str = Utilities.threeingSentence(str);
				if(Utilities.hasUnmatchedBrackets(str)){
					System.out.println("unmatched: "+str);
				}
   	            //if(strcp.compareTo(str)!=0){
	        	//   System.out.println("orig sent==>"+ strcp);
	        	//   System.out.println("rmarked==>"+ strcp2);
	        	//   System.out.println("threed-sent==>"+ str);
				//}
	           //str = str.replaceAll("}>", "/NN").replaceAll(">}", "/NN").replaceAll(">", "/NN").replaceAll("}", "/JJ").replaceAll("[<{]", "");
	           
	           
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
	        	   word = word.replaceAll("[<>{}]", "").trim();
	        	   String p = "";
	        	   if(word.length()>0 && !word.matches("\\W") && !word.matches("("+ChunkedSentence.prepositions+")") &&!word.matches("("+ChunkedSentence.stop+")")){
		        	   ResultSet rs1 = stmt1.executeQuery("select semanticrole from "+this.tableprefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+" where word='"+word+"'");
		       		   if(rs1.next()){
		       			   p = rs1.getString("semanticrole");
		       		   }
	        	   }
	       		   
	        	   Matcher mc = compreppattern.matcher(word);
	        	   if(mc.matches()){
	        		   sb.append(word+"/IN ");
	        	   }else if(word.contains("taxonname-")){
	        		   sb.append(word+"/NNS "); 
	        	   }else if(word.matches("(in|at)-.*?(-?view|profile|closure)")){
	        		   sb.append(word+"/RB ");
	        	   }else if(word.endsWith("ly") && word.indexOf("~") <0){ //character list is not RB
	        		   sb.append(word+"/RB ");
	        	   }else if(word.compareTo("becoming")==0 || word.compareTo("about")==0){
	        		   sb.append(word+"/RB ");
	        	   }else if(word.compareTo("throughout")==0 && tokens[i+1].matches("(,|or)")){
	        		   sb.append(word+"/RB ");
	        	   }else if(word.compareTo("at-least")==0){
	        		   sb.append(word+"/RB ");
	        	   }else if(word.compareTo("plus")==0 || word.compareTo("and-or")==0){
	        		   sb.append(word+"/CC ");
	        	   }else if(word.matches("\\d+[cmd]?m\\d+[cmd]?m")){ //area turned into 32cm35mm
	        		   //sb.append(word+"/CC ");
	        		   sb.append(word+"/CD ");
	        	   }else if(word.matches("("+ChunkedSentence.units+")")){
	       			   sb.append(word+"/NNS ");
	       		   }else if(word.matches("as-\\S+")){ //as-wide-as
	       		   	   sb.append(word+"/RB ");
	       		   }else if(p.contains("op")){ //<inner> larger.
	       				//System.out.println(rs1.getString(2));
	       			   sb.append(word+"/NNS ");
	       		 }else if(p.contains("os") ||(p.length()==0 && pos.indexOf('<') >=0)){
	       			   sb.append(word+"/NN ");
	       		   }else if(word.matches("(\\{?\\b"+roman+"\\b\\}?)")){//mohan code to mark roman numbers {ii} or ii as ii/NNS
	        		   word=word.replaceAll("\\{|\\}", "");
	        		   sb.append(word+"/NNS ");
	        	   }
	        	   //end mohan code
	       		   else if(p.contains("c")|| pos.indexOf('{') >=0){
	       			   	//ResultSet rs3 = stmt1.executeQuery("select word from wordpos4parser where word='"+word+"' and certaintyl>5");
	       				ResultSet rs2 = stmt1.executeQuery("select word from Brown.wordfreq where word='"+word+"' and freq>79");//1/largest freq in wordpos = 79/largest in brown
	       				if(rs2.next()){
	       					sb.append(word+" ");
	       				//}else if(word.indexOf("3-")>=0){
	       				//	sb.append(word+"/CD");
	       				}else{
	       					sb.append(word+"/JJ ");
	       				}
	       		   }
	        	   else{
	       				sb.append(word+" ");
	       		   }
	       		   //m = pattern7.matcher(str);
	       		}
	           	//sb.append(str);
	       		str = sb.toString().trim();
	       		str = str.replaceAll("(?<=[a-z])\\s+[_–-]\\s+(?=[a-z])", "-").replaceAll("/[A-Z]+\\s*[-–]\\s*", "-").replaceAll("\\d-\\s+(?=[a-z])", "3-"); //non -septate/JJ or linear/JJ _ovoid/JJ
	       		str = str.replaceAll("(?<!~)[\\(\\[](?!~)", " -LRB-/-LRB- ").replaceAll("(?<!~)[\\)\\]](?!~)", " -RRB-/-RRB- ")
	       				/*.replaceAll("[\\[]", " -LSB-/-LSB- ").replaceAll("[\\]]", " -RSB-/-RSB- ")*/.replaceAll("\\s+", " ").trim(); 
	       		str = str.replaceAll("moreorless/JJ","moreorless/RB");
	       		return str;
			
			
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			throw e;
		}
		//return "";
	}
		

		
	/**
	 * shallowly to deeply pinnatifid
	 * => //shallowly~to~deeply pinnatifid	
	 * 
	 * 
	 * 
	 * @param str
	 * @return
	 */
	private String normalizemodifier(String str) {
		String result = "";
		int base = 0;
		Matcher m = modifierlist.matcher(str.trim());
		while(m.matches()){
			result += m.group(1);
			int start = (m.group(1).trim()+" a").trim().split("\\s+").length+base-1; 
			String l = m.group(2);
			int end = start+(l.trim()+" b").trim().split("\\s+").length-1;
			str = m.group(3);
			m = modifierlist.matcher(str);
			String newtoken = l.replaceAll("\\s+", "~");
			result += newtoken;
			base = end;
			//adjust chunkedtokens
			for(int i= start; i < end; i++){
				this.chunkedtokens.set(i, "");
			}
			this.chunkedtokens.set(start, newtoken);
		}
		result +=str;
		return result;
	}

	/** 	
	 * @param str: {upper} {pharyngeal} <tooth> <plates> 4 and 5
	 * <tergum> III-VIII
	 * <tergum> III and VIII
	 * @return: {upper} {pharyngeal} <tooth> <plates_4_and_5>
<<<<<<< .mine
	 * 
	 * warning: <{wing}> 1 – 3 cm 
=======
	 * <tergum_3-8>
	 * <tergum_3_and_8>
>>>>>>> .r1146
	 */
	private String normalizePositionList(String str) {
		//mohan code to change roman numbers with <organ> before them to normal numbers
		Matcher n=romanptn.matcher(str);
		while(n.find())
		{
			String position = n.group(2);
			//String romanid = n.group(3).replaceAll("\\{|\\}", "");
			String romanid = n.group(3).replaceAll("\\{|\\}|<|>", "");
			int k=0;
			for (k=0;k<20;k++)
			{
				if(romanid.compareToIgnoreCase(romanno[k])==0)
				{
					String l=Integer.toString(k+1);
					String replace='<'+position+'>'+' '+l;
					//str=str.replaceAll("(<"+position+"> (\\b"+romanid+"\\b|\\{?"+romanid+"\\}?))", replace);
					str=str.replaceAll("(<"+position+"> (\\b"+romanid+"\\b|<?\\{?"+romanid+"\\}?>?))", replace);
					break;
				}
			}
		}
		//end code
		Matcher j = romanrange.matcher(str);
		while(j.find())
		{
			String digit = j.group(1);
			String romanid = j.group(2);
			int k=0;
			for (k=0;k<20;k++)
			{
				if(romanid.compareToIgnoreCase(romanno[k])==0)
				{
					String l=Integer.toString(k+1);
					String replace=digit+'_'+l;
					str=str.replaceAll("("+digit+")-\\b("+romanid+")\\b", replace);
					break;
				}
			}
		}
		
		
		Matcher m = positionptn.matcher(str);
		while(m.find()){
			int start = m.start(1);
			int end = m.end(1);
			String position = m.group(1);
			String organ = m.group(2);
			//if(!isPosition(organ, position)) continue;
			if(!isPosition(organ, position,str)) continue;
			String rposition = position.replaceFirst(">", "").replaceAll("\\s+", "_")+">";
			//synchronise this.chunkedtokens
			//split by single space to get an accurate count to elements that would be in chunkedtokens
			int index = (str.substring(0, start).trim()+" a").trim().split("\\s").length-1; //number of tokens before the count pattern
			this.chunkedtokens.set(index, rposition);
			int num = position.split("\\s+").length;
			for(int i = index+1; i < index+num; i++){
				this.chunkedtokens.set(i, "");
			}
			//resemble the str from chunkedtokens, counting all empty elements, so the str and chunkedtokens are in synch.
			str = "";
			for(String t: this.chunkedtokens){
				str +=t+" ";
			}
			m = positionptn.matcher(str);
		}
		return str.replaceAll("\\s+", " ").trim();
	}


	/**
	 * tooth 5 means the fifth tooth, 5 is position (true)
	 * teeth 5 means 5 teeth, 5 is count(false)
	 * teeth 2 and 3 means the second and third teeth, 2 and 3 are position(true)
	 * tooth 1 ??? treated as position (true) for the time being
	 * @param organ: teeth
	 * @param position: <teeth> 4 and 5
	 * @return
	 */
	private boolean isPosition(String organ, String position, String sent) {
		return false;
//		/*mohan code to forward check if the next token following <organ> position , is another organ */
//		boolean isnextorganchunk=false;
//		Pattern localptn = Pattern.compile(""+position+"\\s*,");
//		Matcher k = localptn.matcher(sent);
//		if(k.find())
//		{
//			isnextorganchunk=true;
//		}
//		/*int m;
//		ArrayList<String> localchunkedtokens = new ArrayList<String>(Arrays.asList(sent.split("\\s+")));
//		for(m=0;m<localchunkedtokens.size()-3;m++)
//		{
//			if(localchunkedtokens.get(m).replaceAll("<|>","").trim().contentEquals(organ)&&localchunkedtokens.get(m+1).contentEquals(position))
//				break;
//			else
//			m++;
//		}
//		m=m+2;
//		while(m<localchunkedtokens.size())
//		{
//			if(localchunkedtokens.get(m).trim().matches("\\W"))
//			{
//				m++;
//			}	
//			else if(localchunkedtokens.get(m).trim().matches("<(\\S+?)>"))
//			{
//				isnextorganchunk=true;
//				break;
//			}
//		}*/
//		/*end mohan*/
//		boolean multiplepositions = false;
//		boolean pluralorgan = false;
//		position = position.replace("<"+organ+">", "").trim();
//		if(position.contains(" ") || position.contains("_")){
//			multiplepositions = true;
//		}		
//		if(Utilities.isPlural(organ)){
//			pluralorgan = true;
//		}
//		//if(pluralorgan && !multiplepositions) return false;
//		if((pluralorgan && !multiplepositions)|isnextorganchunk) return false;
//		return true;
	}

	/**
	 * replace "one or two" with {count~list~one~or~two} in the string
	 * update this.chunkedTokens	
	 * @param str
	 */
		private String normalizeCountList(String str) {
			Matcher m = this.countptn.matcher(str);
			while(m.find()){
				int start = m.start(1);
				int end = m.end(1);
				String count = m.group(1).trim();
				String rcount = "{count~list~"+count.replaceAll(" ","~").replaceAll("[{}]", "")+"}";
				//synchronise this.chunkedtokens
				//split by single space to get an accurate count to elements that would be in chunkedtokens
				int index = (str.substring(0, start).trim()+" a").trim().split("\\s").length-1; //number of tokens before the count pattern
				this.chunkedtokens.set(index, rcount);
				int num = count.split("\\s+").length;
				for(int i = index+1; i < index+num; i++){
					this.chunkedtokens.set(i, "");
				}
				//resemble the str from chunkedtokens, counting all empty elements, so the str and chunkedtokens are in synch.
				str = "";
				for(String t: this.chunkedtokens){
					str +=t+" ";
				}
				m = this.countptn.matcher(str);
			}
			return str.replaceAll("\\s+", " ").trim();
		}



	
	
		/**
		 * make  "suffused with dark blue and purple or green" one token
		 * ch-ptn"color % color color % color @ color"
		 * @return
		 */
	private String normalizeColorPatterns() {
		String list = "";
		String result = "";
		String header = "ttt";
		for(int i = this.charactertokensReversed.size() -1; i>=0; i--){
			list+=this.charactertokensReversed.get(i)+" ";
		}
		list = list.trim()+" "; //need to have a trailing space
		String listcp = list;
		//Pattern p = Pattern.compile("(.*?)((color|coloration)\\s+%\\s+(?:(?:color|coloration|@|%) )+)(.*)");
		Matcher m = colorpattern.matcher(list);
		int base = 0;
		boolean islist = false;
		while(m.matches()){			
			int start = (m.group(1).trim()+" a").trim().split("\\s+").length+base-1;
			int end = start+(m.group(2).trim()+" b").trim().split("\\s+").length-1;
			String ch = m.group(3)+header;
			list = m.group(4);
			m = colorpattern.matcher(list);
			//form result string, adjust chunkedtokens
			for(int i = base; i<start; i++){
				result += this.chunkedtokens.get(i)+" ";
			}
			if(end>start){ //if it is a list
				islist = true;
				String t= "{"+ch+"~list~";
				for(int i = start; i<end; i++){
					t += this.chunkedtokens.get(i).trim().replaceAll("[{}]", "").replaceAll("[,;\\.]", "punct")+"~";
					this.chunkedtokens.set(i, "");
				}
				t = t.replaceFirst("~$", "}");
				t = distributePrep(t)+" ";
				this.chunkedtokens.set(end-1, t.trim());//"suffused with ..." will not form a list with other previously mentioned colors, but may with following colors, so put this list close to the next token.
				result +=t;
			}
			//prepare for the next step
			base = end;						
		}
		//dealing with the last segment of the list or the entire list if no match
		for(int i = base; i<(list.trim()+" b").trim().split("\\s+").length+base-1; i++){
		//for(int i = base+1; i<(list.trim()+" b").trim().split("\\s+").length+base; i++){
			result += this.chunkedtokens.get(i)+" ";
		}
		if(this.printColorList){
			System.out.println(islist+":"+src+":"+listcp);
			System.out.println(islist+":"+src+":"+result);
			System.out.println();
		}
		return result;
	}

	/**
	 * 
	 * @param t: {color~list~suffused~with~red~or~purple}
	 * @return {color~list~suffused~with~red~or~purple}
	 */
	private String distributePrep(String t) {
			Matcher m = distributePrepPattern.matcher(t);
			if(m.matches()){
				t = m.group(1)+m.group(2)+m.group(3)+m.group(2)+m.group(4);
			}
			return t;
		}

	/**
	 * 
	 * @param text
	 * @return two strings: one contains all text from text with rearranged spaces, the other contains numbers as the place holder of the area expressions
	 */	
	private String[] normalizeArea(String text){
			String[] result = new String[2];
			String text2= text;
			Matcher m = areapattern.matcher(text);
			while(m.matches()){
				text = m.group(1)+m.group(2).replaceAll("[ \\{\\}]", "")+ m.group(4);
				m = areapattern.matcher(text2);
				m.matches();
				text2 = m.group(1)+m.group(2).replaceAll("[cmd]?m", "").replaceAll("[ \\{\\}]", "")+ m.group(4);
				m = areapattern.matcher(text);
			}
			result[0] = text;
			result[1] = text2;
			return result;
	}
	
	private void lookupCharacters(String str, boolean markadv) {
		if(str.trim().length() ==0){
			return;
		}
		this.charactertokensReversed = new ArrayList<String>();
		boolean save = false;
		boolean ambiguous = false;
		ArrayList<String> saved = new ArrayList<String>();
		
		ArrayList<String> amb = new ArrayList<String>();
		for(int i = this.chunkedtokens.size()-1; i>=0+0; i--){
			String word = this.chunkedtokens.get(i);
			if(word.indexOf("~list~")>0){
				String ch = word.substring(0, word.indexOf("~list~")).replaceAll("\\W", "").replaceFirst("ttt$", "");
				this.charactertokensReversed.add(ch);
			}else if(word.indexOf('{')>=0 && word.indexOf('<')<0){
				String[] charainfo = Utilities.lookupCharacter(word, conn, ChunkedSentence.characterhash, glosstable, tableprefix); //remember the char for this word (this word is a word before (to|or|\\W)
				if(charainfo==null){
					this.charactertokensReversed.add(word.replaceAll("[{}]", "")); //
				}else{
					this.charactertokensReversed.add(charainfo[0]); //color
					if(save){
						save(saved, this.chunkedtokens.size()-1-i, charainfo[0]); 
						if(charainfo[0].indexOf(Utilities.or)>0){
							ambiguous = true;
							amb.add(this.chunkedtokens.size()-1-i+"");
						}
					}
					save = false;
				}
			}else if (word.indexOf('<')>=0){
				this.charactertokensReversed.add("#");
				save = true;
			}else if(word.matches("(or|and-or|and/or|and_or)") || word.matches("\\S+ly~(or|and-or|and/or|and_or)~\\S+ly")){//loosely~to~densely 
				this.charactertokensReversed.add("@"); //or
				save = true;
			}else if(word.equals("to") || word.matches("\\S+ly~to~\\S+ly")){//loosely~to~densely 
				this.charactertokensReversed.add("`"); //to
				save = true;
			}else if(word.compareTo("±")==0){//±
				this.charactertokensReversed.add("moreorlessly"); //,;. add -ly so it will be treated as an adv.
				save = true;
			}else if(word.matches("\\W")){
				if(word.matches("[()\\[\\]]")) save(saved, this.chunkedtokens.size()-1-i, word); 
				this.charactertokensReversed.add(word); //,;.
				save = true;
			}else if(markadv && word.endsWith("ly")){
				this.charactertokensReversed.add(word);
				save = true;
			}else if(word.matches("("+this.negations+")")){
				this.charactertokensReversed.add(word);
				save = true;
			}else{
				this.charactertokensReversed.add("%");
				save = true;
			}
		}
		
		//deal with a/b characters
		if(ambiguous){
			Iterator<String> it = amb.iterator();
			while(it.hasNext()){
				int i = Integer.parseInt(it.next());
				Pattern p = Pattern.compile("("+this.charactertokensReversed.get(i)+"|"+this.charactertokensReversed.get(i).replaceAll(Utilities.or, "|")+")");
				String tl = lastSaved(saved, i);
				Matcher m = p.matcher(tl);
				//if(m.matches()){
				if(m.find()){
					this.charactertokensReversed.set(i, m.group(1));
				}else{
					String tn = nextSaved(saved, i);
					m = p.matcher(tn);
					//if(m.matches()){
					if(m.find()){
						this.charactertokensReversed.set(i, m.group(1));
					}
				}
			}
		}
	}
	/**
	 * lookback
	 * @param saved
	 * @param index
	 * @return
	 */
	private String lastSaved(ArrayList<String> saved, int index){
		int inbrackets = 0;
		for(int i = index-1; i >=0 && i<saved.size(); i--){
			String c = saved.get(i).trim();
			if(c.equals("(") || c.equals("[")) inbrackets++; //ignore characters in brackets
			else if(c.equals(")") || c.equals("]")) inbrackets--;
			else if(inbrackets ==0 && c.length()>0) return c;
		}
		return "";
	}
	
	/**
	 * lookahead
	 * @param saved
	 * @param index
	 * @return
	 */
	private String nextSaved(ArrayList<String> saved, int index){
		int inbrackets = 0;
		for(int i = index+1; i <saved.size(); i++){
			String c = saved.get(i).trim();
			if(c.equals("(") || c.equals("[")) inbrackets++; //ignore characters in brackets
			else if(c.equals(")") || c.equals("]")) inbrackets--;
			else if(inbrackets ==0 && c.length()>0) return c;			
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
	 * connect a list of character states of the same character together, 
	 * for example: keeled , elliptic to broadly ovate => shape~list~keeled~punct~elliptic~to~broadly~ovate 
	 * example 2: not keeled , elliptic to broadly ovate => not keeled , shape~list~elliptic~to~broadly~ovate //"not"-state with a comma can not be in a range. "rarely" may.
	 * example 3: not keeled or ovate => shape~list~not~keeled~or~ovate //because of "or", the modifer needs to be applied to all states in the or-list
	 * deal also with sentences with parentheses
	 * @return
	 */
	private String normalize(String src){
		lookupCharacters(src, true); //treating -ly as -ly
		
		//use & as place holders
		//create list by replace (...) with &s
		//create lists by replace things not in () with &s
		//normalizeCharacterLists(list)
		//merge result
		
		//e.g leaves lanceolate ( outer ) to linear ( inner ) 
		String inlist = ""; //represent tokens not in brackets: # size & & & @ size & & &
		String outlist = ""; //represent tokens in brackets   : & & & position & & & & position & 
		int inbrackets = 0;
		
		boolean hasbrackets = false;
		for(int i = this.charactertokensReversed.size() -1; i>=0; i--){
			String t = this.charactertokensReversed.get(i);
			if(t.equals("(") || t.equals("[")){
				inbrackets++;
				outlist += "& ";
				inlist += "& ";
				hasbrackets = true;
			}else if(t.equals(")") || t.equals("]")){
				inbrackets--;
				outlist += "& ";
				inlist += "& ";
				hasbrackets = true;
			}else if(inbrackets==0){
				outlist+=t+" ";
				inlist += "& ";
			}else{
				outlist+="& ";
				inlist+=t+" ";
			}
		}
		outlist = outlist.trim()+" "; //need to have a trailing space
		//outlist = outlist.replaceAll("(?<=(^|)not)\\s+", "");
		normalizeCharacterLists(outlist); //chunkedtokens updated

		if(hasbrackets){
			inlist = inlist.trim()+" "; //need to have a trailing space
			//inlist = inlist.replaceAll("(?<=(^|)not)\\s+", "");
			normalizeCharacterLists(inlist); //chunkedtokens updated
			//deal with cases where a range is separated by parentheses: eg. (, {yellow-gray}, to, ), {coloration~list~brown~to~black}
			int orphanedto = getIndexOfOrphanedTo(inlist, 0); //inlist as a list
			while(orphanedto >=0){
				String chara = getCharaOfTo(inlist, orphanedto);
				if(orphanedto+2 < this.chunkedtokens.size() && this.chunkedtokens.get(orphanedto+1).equals(")")){
					String nextchara = this.chunkedtokens.get(orphanedto+2);
					if(nextchara.contains(chara)){//form a range cross parenthetical boundary, eg. (, {yellow-gray}, to, ), {coloration~list~brown~to~black}
						if(nextchara.contains("~list~")){
							nextchara = nextchara.substring(nextchara.indexOf("~list~")+6);
							//form new range
							String range ="{"+chara+"~list~"+this.chunkedtokens.get(orphanedto-1).replaceAll("[{}]", "")+"~to~"+nextchara;
							this.chunkedtokens.set(orphanedto-1, range);
							this.chunkedtokens.set(orphanedto, "");
						}
					}
				}
				orphanedto = getIndexOfOrphanedTo(inlist, ++orphanedto); 
			}
		}
		
		String result = "";
		for(int i = 0; i<this.chunkedtokens.size(); i++){
			result += this.chunkedtokens.get(i)+" ";
		}
		return result.replaceAll("\\s+", " ").trim(); //{shape~list~lanceolate~(~outer~)~to~linear}, note the constraint( inner ) after liner is not in the shape list, it will be associated to "linear" later in the process (in annotator) when more information become available for more reliable associations.
	}

	/**
	 * when "to"[@] is the last token in bracketed phrase:
	 * e.g. (, {yellow-gray}, to, ), {coloration~list~brown~to~black}
	 * @param inlist: & & & & & & & & & & & & & & & coloration @ & & & & & & & & 
	 * @param index of "@"
	 * @return the character before "@"
	 */
	private String getCharaOfTo(String inlist, int orphanedto) {
		List<String> symbols = Arrays.asList(inlist.trim().split("\\s+"));
        return  symbols.get(orphanedto-1);
	}

	/**
	 * when "to"[@] is the last token in bracketed phrase:
	 * e.g. (, {yellow-gray}, to, ), {coloration~list~brown~to~black}
	 * @param inlist: & & & & & & & & & & & & & & & coloration @ & & & & & & & & 
	 * @return first indexof such "@" as a word after startindex
	 */
	private int getIndexOfOrphanedTo(String inlist, int startindex) {
		List<String> symbols =  Arrays.asList(inlist.trim().split("\\s+"));
		boolean found = false;
		for(int i = startindex; i < this.chunkedtokens.size()-1; i++){		
			if(this.chunkedtokens.get(i).equals("to") && this.chunkedtokens.get(i+1).equals(")")){
				return i;
			}
		}
		return -1;
	}

	/**
	 * connect a list of character states of the same character together, 
	 * for example: keeled , elliptic to broadly ovate => shape~list~keeled~punct~elliptic~to~broadly~ovate 
	 * example 2: not keeled , elliptic to broadly ovate => shape~list~not~keeled~punct~elliptic~to~broadly~ovate //"rarely" will be treated the same way as "not"
	 * example 3: not keeled or ovate => shape~list~not~keeled~or~ovate //because of "or", the modifer needs to be applied to all states in the or-list
	 *
	 * @return updated string in format of {shape~list~elliptic~to~broadly~ovate} 
	 */
	//private String normalizeCharacterLists(String list){
	private void normalizeCharacterLists(String list){
		//charactertokens.toString
		//String list = ""; //6/29/12
		//String result = ""; //6/29/12
		
		//lookupCharacters(src, true); //treating -ly as -ly 6/29/12
		
		//6/29/12
		//for(int i = this.charactertokensReversed.size() -1; i>=0; i--){
		//	list+=this.charactertokensReversed.get(i)+" ";
		//}
		//list = list.trim()+" "; //need to have a trailing space
		
		//pattern match: collect state one by one
		String listcopy = list;
		int base = 0;
		//Pattern pt = Pattern.compile("(.*?(?:^| ))(([0-9a-z–\\[\\]\\+-]+ly )*([a-z-]+ )+([@,;\\.] )+\\s*)(([a-z-]+ )*(\\4)+[@,;\\.%\\[\\]\\(\\)#].*)");//
		Matcher mt = charalistpattern.matcher(list);
		String connector = "";
		while(mt.matches()){
			int start = (mt.group(1).trim()+" a").trim().split("\\s+").length+base-1; //"".split(" ") == 1
			String l = mt.group(2); //the first state
			String ch = mt.group(4).trim();
			list = mt.group(6);
			Matcher m = charalistpattern2.matcher(list);
			while(m.matches()){ //all following states
				String another = m.group(1);
				l += m.group(1);
				//connector = m.group(5).matches(".*?[@`].*")? m.group(5) : connector; //the last non-punct connector
				list = m.group(6);
				m = charalistpattern2.matcher(list);
			}
			//l += list.replaceFirst("[@,;\\.%\\[\\]\\(\\)#].*$", "");//take the last seg from the list
			//l += list.replaceFirst("[@,;\\.%\\[\\]\\(\\)&#].*$", "");//take the last seg from the list 6/29/2012
			l += list.replaceFirst("(?<="+ch+"(\\s[0-9a-z–\\[\\]\\+-]{1,10}ly)?).*$", "");//arrangement_or_shape @ arrangement_or_shape coating_or_texture # ;
			int end = start+(l.trim()+" b").trim().split("\\s+").length-1;
			//if(! l.matches(".*?@[^,;\\.]*") && l.matches(".*?,.*")){ //the last state is not connected by or/to, then it is not a list
			//	start = end;
			//}
			while(! l.matches(".*?[`@][^,;\\.]*") && l.matches(".*?,.*")){ //the last state is not connected by or/to, then it is not a list
				l = l.replaceFirst("[,;\\.][^@`]*$", "").trim();
			}
			if(l.indexOf('@')>0 || l.indexOf('`')>0){
				end = start+(l.trim()+" b").trim().split("\\s+").length-1;
			}else{
				start = end;
			}
				
			
			//list = list.replaceFirst("^.*?(?=[@,;\\.%\\[\\]\\(\\)#])", ""); //6/29/2012
			//list = list.replaceFirst("^.*?(?=[@,;\\.%\\[\\]\\(\\)&#])", "");
			list = segByWord(listcopy, end);
			mt = charalistpattern.matcher(list);
			

			//6/29/12
			//for(int i = base; i<start; i++){
			//	result += this.chunkedtokens.get(i)+" ";
			//}
			if(end>start){ //if it is a list
				connector = l.replaceAll("[^`@]", "").charAt(0)+"";
				//triage: "not a, b, or c" is fine; "not a, b to c" is not
				if(connector.trim().equals("`")){//if connector is "to", then "not"-modified state should be removed.
					//check if l starts with "not"
					while(l.matches("(not|never)\\b.*")){//remove negated states from the begaining of l one by one
						if(l.indexOf(",")<0) break;
						String notstate = l.substring(0, l.indexOf(",")+1);
						l = l.substring(l.indexOf(",")+1).trim();
						start = start + (notstate.trim()+" b").trim().split("\\s+").length - 1;
					}
				}
				//if connector is "or", then "not"-modified state should be included, no additional action is needed.
				
				//adjust this.chunkedtokens.
				String t= "{"+ch+"~list~";
				for(int i = start; i<end; i++){
					if(this.chunkedtokens.get(i).length()>0){
						t += this.chunkedtokens.get(i).trim().replaceAll("[{}]", "").replaceAll("[,;\\.]", "punct")+"~";
					}else if(i == end-1){
						while(this.chunkedtokens.get(i).length()==0){
							i++;
						}
						t+=this.chunkedtokens.get(i).trim().replaceAll("[{}]", "").replaceAll("[,;\\.]", "punct")+"~";
					}
					this.chunkedtokens.set(i, "");
				}
				t = t.replaceFirst("~$", "}")+" ";
				if(t.indexOf("ttt~list")>=0) t = t.replaceAll("~color.*?ttt~list", "");
				this.chunkedtokens.set(start, t);
				if(this.printCharacterList){
					System.out.println(this.src+":"+">>>"+t);
				}
			}
			base = end;
		}
		
		//6/29/12
		//for(int i = base; i<(list.trim()+" b").trim().split("\\s+").length+base-1; i++){
		//	result += this.chunkedtokens.get(i)+" ";
		//}
		//return result.trim();
	}
	


	private String segByWord(String listcopy, int startindex) {
		String seg = "";
		if(startindex < 0) return seg;
		String[] tokens = listcopy.trim().split("\\s+");
		for(int i = startindex; i < tokens.length; i++){
			seg += tokens[i]+" ";
		}
		return seg.trim();
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
		Matcher m = asaspattern.matcher(str);
		while(m.matches()){
			result+=m.group(1);
			result+="{"+m.group(2).replaceAll("\\s+", "-").replaceAll("[{}<>]", "")+"}";
			str = m.group(3);
			m = asaspattern.matcher(str);
		}
		result+=str;
		return result.trim();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//File posedfile = new File(posedfile); 
		//File parsedfile = new File("");
		String database = "fnav19_benchmark";
		String tableprefix = "fnav19";
		String POSTaggedSentence="POSedSentence";
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				//Statement stmt = conn.createStatement();
				//stmt.execute("create table if not exists "+tableprefix+"_"+POSTaggedSentence+"(source varchar(100) NOT NULL, posedsent TEXT, PRIMARY KEY(source))");
				//stmt.execute("delete from "+tableprefix+"_"+POSTaggedSentence);
				//stmt.close();
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
		POSTagger4StanfordParser tagger = new POSTagger4StanfordParser(conn, tableprefix, "antglossaryfixed");
		
		//String str="<Cypselae> {tan} , {subcylindric} , {subterete} to 5-{angled} , 8–10 {mm} , {indistinctly} 8–10-{ribbed}";
		//String src="364.txt-15";
		//String str="{often} 2- , 3- , or 5-{ribbed}";
		//String src="625.txt-16";
		//String str = "<heads> in {paniculiform} arrays .";
		//String src = "10.txt-4";
		//String str = "<{middle}> <phyllaries> {acuminate} at <apex> with <point> 22 – 38 {mm} and <{spine}> <tip> 6 – 9 {mm} , or in some {cultivated} {forms} {broadly} {obtuse} to {truncate} and {mucronate} with or without <{spine}> <tip> 1 – 2 {mm} , {distal} <margins> with or without {indistinct} {yellowish} <margins> .";
		//String src = "41.txt-1";
		//String str = " <outer> 5 – 6 {lance-ovate} to {lanceolate} , 4 – 7 {mm} , {basally} {cartilaginous} , {distally} {herbaceous} , <inner> 8 + {lance-linear} to {linear} , 6 – 12 {mm} , {herbaceous} , all {usually} with some <{gland}>-{tipped} <hairs> 0 . 5 – 0 . 8 {mm} on <margins> near <bases> or on {abaxial} <faces> toward <tips> .";
		//String src = "273.txt-6";
		//String str = "<stems> {usually} 1 , {branched} {distally} or {openly} so throughout , {leafy} , {glabrous} or {thinly} {arachnoid-tomentose} .";
		String src = "157.txt-1";
		String str = "laminae 6 17 cm . long , 2 - 7 cm . broad , lanceolate to narrowly oblong or elliptic_oblong , abruptly and narrowly acuminate , obtuse to acute at the base , margin entire , the lamina drying stiffly chartaceous to subcoriaceous , smooth on both surfaces , essentially glabrous and the midvein prominent above , glabrous to sparsely puberulent beneath , the 8 to 18 pairs of major secondary veins prominent beneath and usually loop_connected near the margin , microscopic globose_capitate or oblongoid_capitate hairs usually present on the lower surface , clear or orange distally .";
		try{
		System.out.println(tagger.POSTag(str, src));
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}

}
