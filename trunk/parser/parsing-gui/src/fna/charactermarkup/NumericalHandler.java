/**
 * 
 */
package fna.charactermarkup;

//import java.sql.ResultSet;
//import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

import org.jdom.*;
//import org.jdom.input.*;
//import org.jdom.xpath.*;
//import org.jdom.output.*;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * @author hongcui
 * chara rewrite of CharStateHandler in JDOM terms
 * 
 */
public class NumericalHandler  {

	//static public String numberpattern = "[ ()\\[\\]\\-\\�\\d\\.�\\+���/�\\*/%]*?[�/�\\d][ ()\\[\\]\\-\\�\\d\\.�\\+���/�\\*/%]{2,}(?!~[a-z])";
	static public String numberpattern = "[()\\[\\]\\-\\�\\d\\.�\\+���/�\\*/%]*?[�/�\\d][()\\[\\]\\-\\�\\d\\.,?�\\+���/�\\*/%]{2,}(?![a-z{}])"; //added , and ? for chromosome counts
	static private boolean debug = true;
	
	public NumericalHandler() {
	}

	/**
	 * 
	 * @param tobechunkedmarkedsent: e.g. <Florets> 4�25 [ �60 ] , {bisexual} , {fertile} ;
	 * @return <Florets> 4�25[�60] , {bisexual} , {fertile} ;
	 */
	public static String normalizeNumberExp(String sentence) {
		sentence = sentence.replaceAll("-\\s*LRB-/-LRB\\s*-", "[").replaceAll("-\\s*RRB-/-RRB\\s*-", "]");
		String norm = "";
		/*Pattern p = Pattern.compile("(.*?)("+NumericalHandler.numberpattern+")(.*)");
		Matcher m = p.matcher(sentence);
		while(m.matches()){
			sentence  = m.group(3);
			norm += m.group(1);
			norm += " "+m.group(2).replaceAll("\\s+", "")+" ";
			m = p.matcher(sentence);
		}
		norm += sentence;*/
		norm = sentence;
		norm = norm.trim().replaceFirst("(?<=[0-9])\\.$", " .").replaceAll("\\[","-LRB-/-LRB-").replaceAll("\\]","-RRB-/-RRB-");
		return norm;
	}
	
	public static String originalNumForm(String token){
		if(token.matches(".*[a-z].*?")){
			return token.replaceAll("-\\s*LRB-/-LRB\\s*-", "(").replaceAll("-\\s*RRB-/-RRB\\s*-", ")");
		}else{
			return token.replaceAll("-\\s*LRB-/-LRB\\s*-", "[").replaceAll("-\\s*RRB-/-RRB\\s*-", "]");
		}
	}
	public static boolean isNumerical(String token){
		if(token.matches(".*?\\d+\\+?%?$")){
			return true;
		}
		if(token.matches(".*?\\d+-RRB-/-RRB-$")){
			return true;
		}
		return false;
	}
	/**
	 * 
	 * @param plaincharset : styles 2[10] mm diam.
	 * @param cname: 
	 * @return: characters marked up in XML format <character name="" value="">
	 */
	//public static ArrayList<Element> characterstate(String plaincharset, String state){
	public static ArrayList<Element> parseNumericals(String plaincharset, String cname){	
		//new CharStateHandler();
		if(debug) {
			System.out.println();
			System.out.println(">>>>>>>>>>>>>"+plaincharset);
		}
		ArrayList<Element> innertagstate = new ArrayList<Element>();
		try{
			int i,j;
			plaincharset = plaincharset.replaceAll("\\([\\s]?|\\[[\\s]?", "[");
			plaincharset = plaincharset.replaceAll("[\\s]?\\)|[\\s]?\\]", "]");
			Pattern p1 = Pattern.compile("(.*?\\b(\\d+))\\s*\\[\\+\\](.*)");
			Matcher m = p1.matcher(plaincharset);
			if(m.matches()){
				//4-5[+] => 4-5[-5+]
				plaincharset = m.group(1)+"[-"+m.group(2)+"+]"+m.group(3);
				m = p1.matcher(plaincharset);
			}
			
			
			
			///////////////////////////////////////////////////////////////////
			//      area                                               ////////
			Pattern pattern19 = Pattern.compile("[�]?[\\[]?[\\d\\s\\.]+[\\]]?[\\s]?[\\[]?[\\�\\-]+[\\]]?[\\s]?[\\[]?[\\d\\s\\.]+[+]?[\\]]?[\\s]?[dcm�]?[m]?[\\s]?[xX\\�]+[\\s]?[\\[]?[\\d\\s\\.]+[\\]]?[\\s]?[\\[]?[\\�\\-]+[\\]]?[\\s]?[\\[]?[\\d\\s\\.]+[+]?[\\]]?[\\s]?[dcm�]?m");
        	Matcher matcher2 = pattern19.matcher(plaincharset);
        	while ( matcher2.find()){
        		if(plaincharset.charAt(matcher2.start())==' '){
        			i=matcher2.start()+1;
        		}
        		else{
        			i=matcher2.start();
        		}
        		j=matcher2.end();
        		String match = plaincharset.substring(i, j);
        		Pattern pattern18 = Pattern.compile("[\\s]?[dcm�]?m");
            	Matcher matcher3 = pattern18.matcher(match);
            	String[] unit = new String[2];
        		int num = 0;
            	while ( matcher3.find()){
            		unit[num] = match.substring(matcher3.start(), matcher3.end());
            		num++;
            	}
            	match = matcher3.replaceAll("#");
            	matcher3.reset();
        		int en = match.indexOf('-');
        		int lasten = match.lastIndexOf('-');
        		if (match.substring(en+1, match.indexOf('�',en+1)).contains("+")){
        			Element character = new Element("character");
        			character.setAttribute("char_type", "range_value");
        			character.setAttribute("name", "length");
        			character.setAttribute("from", match.substring(0,en).trim());
        			character.setAttribute("from_unit",unit[0].trim());
        			character.setAttribute("to", match.substring(en+1, match.indexOf('+',en+1)).trim());
        			character.setAttribute("to_unit", unit[0].trim());
        			character.setAttribute("upper_restricted", "false");
        			innertagstate.add(character);
        			//innertagstate=innertagstate.concat("<character char_type=\"range_value\" name=\"length\" from=\""+match.substring(0,en).trim()+"\" to=\""+match.substring(en+1, match.indexOf('+',en+1)).trim()+"\" upper_restricted=\"false\" unit=\""+unit[0].trim()+"\"/>");
        		}else{
        			Element character = new Element("character");
        			character.setAttribute("char_type", "range_value");
        			character.setAttribute("name", "length");
        			character.setAttribute("from", match.substring(0,en).trim());
        			character.setAttribute("from_unit",unit[0].trim());
        			//character.setAttribute("to", match.substring(en+1, match.indexOf('#',en+1)).trim());
        			character.setAttribute("to", match.substring(en+1, match.indexOf('�',en+1)).trim());
        			character.setAttribute("to_unit", unit[0].trim());
        			innertagstate.add(character);
        			//innertagstate=innertagstate.concat("<character char_type=\"range_value\" name=\"length\" from=\""+match.substring(0,en).trim()+"\" to=\""+match.substring(en+1, match.indexOf('�',en+1)).trim()+"\" unit=\""+unit[0].trim()+"\"/>");
        		}
        		
        		
        		if (num>1){
        			if (match.substring(lasten+1, match.indexOf('#',lasten+1)).contains("+")){
        				Element character = new Element("character");
            			character.setAttribute("char_type", "range_value");
            			character.setAttribute("name", "width");
            			//character.setAttribute("from", match.substring(match.indexOf('�')+2,lasten).trim());
            			character.setAttribute("from", match.substring(match.indexOf('�')+1,lasten).trim());
            			character.setAttribute("from_unit",unit[1].trim());
            			character.setAttribute("to", match.substring(lasten+1, match.indexOf('+',lasten+1)).trim());
            			character.setAttribute("to_unit", unit[1].trim());
            			character.setAttribute("upper_restricted", "false");
            			innertagstate.add(character);
        				//innertagstate=innertagstate.concat("<character char_type=\"range_value\" name=\"width\" from=\""+match.substring(match.indexOf('�')+2,lasten).trim()+"\" to=\""+match.substring(lasten+1, match.indexOf('+',lasten+1)).trim()+"\" upper_restricted=\"false\" unit=\""+unit[1].trim()+"\"/>");
        			}else{
        				Element character = new Element("character");
            			character.setAttribute("char_type", "range_value");
            			character.setAttribute("name", "width");
            			//character.setAttribute("from", match.substring(match.indexOf('�')+2,lasten).trim());
            			character.setAttribute("from", match.substring(match.indexOf('�')+1,lasten).trim());
            			character.setAttribute("from_unit",unit[1].trim());
            			character.setAttribute("to", match.substring(lasten+1, match.indexOf('#',lasten+1)).trim());
            			character.setAttribute("to_unit", unit[1].trim());
            			innertagstate.add(character);
        				//innertagstate=innertagstate.concat("<character char_type=\"range_value\" name=\"width\" from=\""+match.substring(match.indexOf('�')+2,lasten).trim()+"\" to=\""+match.substring(lasten+1, match.indexOf('#',lasten+1)).trim()+"\" unit=\""+unit[1].trim()+"\"/>");
        			}
        		}else{
        			if (match.substring(lasten+1, match.indexOf('#',lasten+1)).contains("+")){
        				Element character = new Element("character");
            			character.setAttribute("char_type", "range_value");
            			character.setAttribute("name", "width");
            			//character.setAttribute("from", match.substring(match.indexOf('�')+2,lasten).trim());
            			character.setAttribute("from", match.substring(match.indexOf('�')+1,lasten).trim());
            			character.setAttribute("from_unit",unit[0].trim());
            			character.setAttribute("to", match.substring(lasten+1, match.indexOf('+',lasten+1)).trim());
            			character.setAttribute("to_unit", unit[0].trim());
            			character.setAttribute("upper_restricted", "false");
            			innertagstate.add(character);
        				//innertagstate=innertagstate.concat("<character char_type=\"range_value\" name=\"width\" from=\""+match.substring(match.indexOf('�')+2,lasten).trim()+"\" to=\""+match.substring(lasten+1, match.indexOf('+',lasten+1)).trim()+"\" upper_restricted=\"false\" unit=\""+unit[0].trim()+"\"/>");
        			}else{
        				Element character = new Element("character");
            			character.setAttribute("char_type", "range_value");
            			character.setAttribute("name", "width");
            			//character.setAttribute("from", match.substring(match.indexOf('�')+2,lasten).trim());//3-5x1.5-2
            			character.setAttribute("from", match.substring(match.indexOf('�')+1,lasten).trim());
            			character.setAttribute("from_unit",unit[0].trim());
            			character.setAttribute("to", match.substring(lasten+1, match.indexOf('#',lasten+1)).trim());
            			character.setAttribute("to_unit", unit[0].trim());
            			innertagstate.add(character);
        				//innertagstate=innertagstate.concat("<character char_type=\"range_value\" name=\"width\" from=\""+match.substring(match.indexOf('�')+2,lasten).trim()+"\" to=\""+match.substring(lasten+1, match.indexOf('#',lasten+1)).trim()+"\" unit=\""+unit[0].trim()+"\"/>");
        			}
        		}
        	}
        	plaincharset = matcher2.replaceAll("#");
        	matcher2.reset();
        	
        	////////////////////////////////////////////////////////////////////////////////////
        	//   ratio                                                              ////////////
        	Pattern pattern24 = Pattern.compile("l/w[\\s]?=[\\d\\.\\s\\+\\�\\-]+");
        	matcher2 = pattern24.matcher(plaincharset);
        	while ( matcher2.find()){
        		if(plaincharset.charAt(matcher2.start())==' '){
        			i=matcher2.start()+1;
        		}
        		else{
        			i=matcher2.start();
        		}
        		j=matcher2.end();
        		String match = plaincharset.substring(i, j);
        		int en = match.indexOf('-');
        		if (match.contains("+")){
        			Element character = new Element("character");
        			character.setAttribute("char_type", "range_value");
        			character.setAttribute("name", "l_w_ratio");
        			//character.setAttribute("from", match.substring(match.indexOf('=')+2,en).trim());
        			character.setAttribute("from", match.substring(match.indexOf('=')+1,en).trim());
        			character.setAttribute("to", match.substring(en+1, match.indexOf('+',en+1)).trim());
        			character.setAttribute("upper_restricted", "false");
        			innertagstate.add(character);
        			//innertagstate=innertagstate.concat("<character char_type=\"range_value\" name=\"l_w_ratio\" from=\""+match.substring(match.indexOf('=')+2,en).trim()+"\" to=\""+match.substring(en+1, match.indexOf('+',en+1)).trim()+"\" upper_restricted=\"false\"/>");
        		}else{
        			Element character = new Element("character");
        			character.setAttribute("char_type", "range_value");
        			character.setAttribute("name", "l_w_ratio");
        			//character.setAttribute("from", match.substring(match.indexOf('=')+2,en).trim());
        			character.setAttribute("from", match.substring(match.indexOf('=')+1,en).trim());
        			character.setAttribute("to", match.substring(en+1, match.indexOf(' ',en+1)).trim());
        			innertagstate.add(character);
        			//innertagstate=innertagstate.concat("<character char_type=\"range_value\" name=\"l_w_ratio\" from=\""+match.substring(match.indexOf('=')+2,en).trim()+"\" to=\""+match.substring(en+1, match.indexOf(' ',en+1)).trim()+"\"/>");
           		}
        	}
        	plaincharset = matcher2.replaceAll("#");
        	matcher2.reset();
        	
        	/////////////////////////////////////////////////////////////////////////////////////////////////////////
        	// size: deal with  "[5-]10-15[-20] cm", not deal with "5 cm - 10 cm"                        ////////////
        	//int sizect = 0;
			Pattern pattern13 = Pattern.compile("[xX\\ױ\\d\\[\\]\\�\\-\\.\\s\\+]+[\\s]?([dcm�]?m)(?![\\w])(([\\s]diam)?([\\s]wide)?)");
        	matcher2 = pattern13.matcher(plaincharset);
        	String toval="";
        	String fromval="";
        	while ( matcher2.find()){
        		String unit = matcher2.group(1);
        		if(plaincharset.charAt(matcher2.start())==' '){
        			i=matcher2.start()+1;
        		}
        		else{
        			i=matcher2.start();
        		}
        		j=matcher2.end();
        		String extreme = plaincharset.substring(i,j);
    			i = 0;
    			j = extreme.length();
        		Pattern pattern20 = Pattern.compile("\\[[�\\d\\.\\s\\+]+[\\�\\-]{1}[�\\d\\.\\s\\+\\�\\-]*\\]");
            	Matcher matcher1 = pattern20.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		if(extreme.charAt(q-2)=='�' | extreme.charAt(q-2)=='-'){
            			Element character = new Element("character");
	        			character.setAttribute("char_type", "range_value");
	        			character.setAttribute("name", "atypical_size");
	        			character.setAttribute("from", extreme.substring(p+1,q-2).trim());
	        			character.setAttribute("to", "");
	        			character.setAttribute("from_unit", unit);
	        			character.setAttribute("to_unit", unit);
	        			//character.setAttribute("upper_restricted", "false");
	        			innertagstate.add(character);
            			//innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,q-2).trim()+"\" to=\"\"/>");
            		}else{
            			Element character = new Element("character");
	        			character.setAttribute("char_type", "range_value");
	        			character.setAttribute("name", "atypical_size");
	        			character.setAttribute("from", extreme.substring(p+1,extreme.indexOf("-",p+1)).trim());
	        			character.setAttribute("to", extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim());
	        			character.setAttribute("from_unit", unit);
	        			character.setAttribute("to_unit", unit);
	        			//character.setAttribute("upper_restricted", "??");
	        			innertagstate.add(character);
            			//innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim()+"\"/>");
            		}
            	}
            	extreme = matcher1.replaceAll("#");
        		matcher1.reset();
        		if(extreme.contains("#"))
        			i = extreme.indexOf("#")+1;
        		Pattern pattern21 = Pattern.compile("\\[[�\\d\\.\\s\\+\\�\\-]*[\\�\\-]{1}[�\\d\\.\\s\\+]+\\]");
            	matcher1 = pattern21.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		if (extreme.charAt(p+1)=='�' | extreme.charAt(p+1)=='-'){
            			if (extreme.charAt(q-2)=='+'){
            				Element character = new Element("character");
    	        			character.setAttribute("char_type", "range_value");
    	        			character.setAttribute("name", "atypical_size");
    	        			character.setAttribute("from", "");
    	        			character.setAttribute("to", extreme.substring(p+2,q-2).trim());
    	        			character.setAttribute("from_unit", unit);
    	        			character.setAttribute("to_unit", unit);
    	        			character.setAttribute("upper_restricted", "false");
    	        			innertagstate.add(character);
            				//innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_size\" from=\"\" to=\""+extreme.substring(p+2,q-2).trim()+"\" upper_restricted=\"false\"/>");
            			}else{
            				Element character = new Element("character");
    	        			character.setAttribute("char_type", "range_value");
    	        			character.setAttribute("name", "atypical_size");
    	        			character.setAttribute("from", "");
    	        			character.setAttribute("to", extreme.substring(p+2,q-1).trim());
    	        			character.setAttribute("from_unit", unit);
    	        			character.setAttribute("to_unit", unit);
    	        			//character.setAttribute("upper_restricted", "true");
    	        			innertagstate.add(character);
            				//innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_size\" from=\"\" to=\""+extreme.substring(p+2,q-1).trim()+"\"/>");
            			}
            		}
            		else{
            			if (extreme.charAt(q-2)=='+'){
            				Element character = new Element("character");
    	        			character.setAttribute("char_type", "range_value");
    	        			character.setAttribute("name", "atypical_size");
    	        			character.setAttribute("from", extreme.substring(p+1,extreme.indexOf("-",p+1)).trim());
    	        			character.setAttribute("to", extreme.substring(extreme.indexOf("-",p+1)+1,q-2).trim());
    	        			character.setAttribute("from_unit", unit);
    	        			character.setAttribute("to_unit", unit);
    	        			character.setAttribute("upper_restricted", "false");
    	        			innertagstate.add(character);
            				//innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-2).trim()+"\" upper_restricted=\"false\"/>");
            			}else{
            				Element character = new Element("character");
    	        			character.setAttribute("char_type", "range_value");
    	        			character.setAttribute("name", "atypical_size");
    	        			character.setAttribute("from", extreme.substring(p+1,extreme.indexOf("-",p+1)).trim());
    	        			character.setAttribute("to", extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim());
    	        			character.setAttribute("from_unit", unit);
    	        			character.setAttribute("to_unit", unit);
    	        			//character.setAttribute("upper_restricted", "true");
    	        			innertagstate.add(character);
            				//innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim()+"\"/>");
            			}
            		}
            	}
            	extreme = matcher1.replaceAll("#");
        		matcher1.reset();
        		j = extreme.length();
        		Pattern pattern23 = Pattern.compile("\\[[�\\d\\.\\s\\+]+\\]");
            	matcher1 = pattern23.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		if (extreme.charAt(q-2)=='+'){
            			Element character = new Element("character");
	        			character.setAttribute("name", "atypical_size");
	        			character.setAttribute("from", extreme.substring(p+1,q-2).trim());
	        			character.setAttribute("to", "");
	        			character.setAttribute("from_unit", unit);
	        			character.setAttribute("to_unit", unit);
	        			character.setAttribute("upper_restricted", "false");
	        			innertagstate.add(character);
            			//innertagstate = innertagstate.concat("<character name=\"atypical_size\" from=\""+extreme.substring(p+1,q-2).trim()+"\" upper_restricted=\"false\"/>");
            		}else{
            			Element character = new Element("character");
	        			character.setAttribute("name", "atypical_size");
	        			character.setAttribute("value", extreme.substring(p+1,q-1).trim());
	        			character.setAttribute("unit", unit);
	        			//character.setAttribute("unit", extreme.substring(q-1).trim());
	        			innertagstate.add(character);
        				//innertagstate = innertagstate.concat("<character name=\"atypical_size\" value=\""+extreme.substring(p+1,q-1).trim()+"\"/>");
            		}
            	}
            	extreme = matcher1.replaceAll("#");
            	matcher1.reset();
            	j = extreme.length();
        		if(extreme.substring(i,j).contains("�")|extreme.substring(i,j).contains("-") && !extreme.substring(i,j).contains("�") && !extreme.substring(i,j).contains("x") && !extreme.substring(i,j).contains("X")){
        			String extract = extreme.substring(i,j);
        			Pattern pattern18 = Pattern.compile("[\\s]?[dcm�]?m(([\\s]diam)?([\\s]wide)?)");
                	Matcher matcher3 = pattern18.matcher(extract);
                	unit="";
                	if ( matcher3.find()){
                		unit = extract.substring(matcher3.start(), matcher3.end());
                	}
                	extract = matcher3.replaceAll("#");
                	matcher3.reset();
                	String from = extract.substring(0, extract.indexOf('-')).trim();
                	String to = extract.substring(extract.indexOf('-')+1,extract.indexOf('#')).trim();
                	boolean upperrestricted = ! to.endsWith("+");
                	to = to.replaceFirst("\\+$", "").trim();
                	
                	Element character = new Element("character");
        			character.setAttribute("char_type", "range_value");
        			character.setAttribute("name", "size");
        			character.setAttribute("from", from);
        			character.setAttribute("from_unit", unit.trim());
        			character.setAttribute("to", to);
        			character.setAttribute("to_unit", unit.trim());
        			if(!upperrestricted)
        				character.setAttribute("upper_restricted", upperrestricted+"");
        			innertagstate.add(character);
                	//innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"size\" from=\""+from+"\" from_unit=\""+unit.trim()+"\" to=\""+to+"\" to_unit=\""+unit.trim()+"\" upper_restricted=\""+upperrestricted+"\"/>");
        			toval = extract.substring(0, extract.indexOf('-'));
        			fromval = extract.substring(extract.indexOf('-')+1,extract.indexOf('#'));
                	//sizect+=1;
        		}
        		else{
        			String extract = extreme.substring(i,j);
        			Pattern pattern18 = Pattern.compile("[\\s]?[dcm�]?m(([\\s]diam)?([\\s]wide)?)");
                	Matcher matcher3 = pattern18.matcher(extract);
                	unit="";
                	if ( matcher3.find()){
                		unit = extract.substring(matcher3.start(), matcher3.end());
                	}
                	extract = matcher3.replaceAll("#");
                	matcher3.reset();
                	
                	Element character = new Element("character");
        			character.setAttribute("name", "size");
        			character.setAttribute("value", extract.substring(0,extract.indexOf('#')).trim());
        			character.setAttribute("unit", unit.trim());
        			innertagstate.add(character);
                	//innertagstate = innertagstate.concat("<character name=\"size\" value=\""+extract.substring(0,extract.indexOf('#')).trim()+"\" unit=\""+unit.trim()+"\"/>");
        			toval = extract.substring(0,extract.indexOf('#'));
        			fromval = extract.substring(0,extract.indexOf('#'));
        		}
        		
        		
        		Iterator<Element> it = innertagstate.iterator();
        		while(it.hasNext()){
        			Element e = it.next();
        			if(e.getAttribute("to") != null && e.getAttributeValue("to").compareTo("")==0){
        				if(toval.endsWith("+")){
        					toval = toval.replaceFirst("\\+$", "");
        					e.setAttribute("upper_restricted", "false");
        				}
        				e.setAttribute("to", toval.trim());
        				e.setAttribute("to_inclusive", "false");
        			}
        			if(e.getAttribute("from") != null && e.getAttributeValue("from").compareTo("")==0){
        				e.setAttribute("from", fromval.trim());
        				e.setAttribute("from_inclusive", "false");
        			}
        		}
        		
        		/*
        		StringBuffer sb = new StringBuffer();
				Pattern pattern25 = Pattern.compile("to=\"\"");
				matcher1 = pattern25.matcher(innertagstate);
				while ( matcher1.find()){
					matcher1.appendReplacement(sb, "to=\""+toval.trim()+"\"");
				}
				matcher1.appendTail(sb);
				innertagstate=sb.toString();
				matcher1.reset();
				StringBuffer sb1 = new StringBuffer();
				Pattern pattern26 = Pattern.compile("from=\"\"");
				matcher1 = pattern26.matcher(innertagstate);
				while ( matcher1.find()){
					matcher1.appendReplacement(sb1, "from=\""+fromval.trim()+"\"");
				}
				matcher1.appendTail(sb1);
				innertagstate=sb1.toString();
				matcher1.reset();
				*/
        	}
        	plaincharset = matcher2.replaceAll("#");
        	matcher2.reset();
        	//System.out.println("plaincharset2:"+plaincharset);
        	
        	
        	
        	
        	
        	////////////////////////////////////////////////////////////////////////////////////////////
        	//   size                                                                             /////
        	Pattern pattern14 = Pattern.compile("[�\\d\\[\\]\\�\\-\\./\\s]+[\\s]?[\\�\\-]?(% of [\\w]+ length|height of [\\w]+|times as [\\w]+ as [\\w]+|total length|their length|(times)?[\\s]?length of [\\w]+)");
        	matcher2 = pattern14.matcher(plaincharset);
        	toval="";
        	fromval="";
        	while ( matcher2.find()){
        		if(plaincharset.charAt(matcher2.start())==' '){
        			i=matcher2.start()+1;
        		}
        		else{
        			i=matcher2.start();
        		}
        		j=matcher2.end();
        		String extreme = plaincharset.substring(i,j);
    			i = 0;
    			j = extreme.length();
        		Pattern pattern20 = Pattern.compile("\\[[�\\d\\.\\s\\+]+[\\�\\-]{1}[�\\d\\.\\s\\+\\�\\-]*\\]");
            	Matcher matcher1 = pattern20.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		if(extreme.charAt(q-2)=='�' | extreme.charAt(q-2)=='-'){
            			Element character = new Element("character");
            			character.setAttribute("char_type", "relative_range_value");
            			character.setAttribute("name", "atypical_size");
            			character.setAttribute("from", extreme.substring(p+1,q-2).trim());
            			character.setAttribute("to", "");
            			innertagstate.add(character);
            			//innertagstate = innertagstate.concat("<character char_type=\"relative_range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,q-2).trim()+"\" to=\"\"/>");
            		}else{
            			Element character = new Element("character");
            			character.setAttribute("char_type", "relative_range_value");
            			character.setAttribute("name", "atypical_size");
            			character.setAttribute("from", extreme.substring(p+1,extreme.indexOf("-",p+1)).trim());
            			character.setAttribute("to", extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim());
            			innertagstate.add(character);
            		    //innertagstate = innertagstate.concat("<character char_type=\"relative_range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim()+"\"/>");
            		}
            	}
            	extreme = matcher1.replaceAll("#");
        		matcher1.reset();
        		if(extreme.contains("#"))
        			i = extreme.indexOf("#")+1;
        		Pattern pattern21 = Pattern.compile("\\[[�\\d\\.\\s\\+\\�\\-]*[\\�\\-]{1}[�\\d\\.\\s\\+]+\\]");
            	matcher1 = pattern21.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		if (extreme.charAt(p+1)=='�' | extreme.charAt(p+1)=='-'){
            			if (extreme.charAt(q-2)=='+'){
            				Element character = new Element("character");
                			character.setAttribute("char_type", "relative_range_value");
                			character.setAttribute("name", "atypical_size");
                			character.setAttribute("from", "");
                			character.setAttribute("to", extreme.substring(p+2,q-2).trim());
                			character.setAttribute("upper_restricted", "false");
                			innertagstate.add(character);
            				//innertagstate = innertagstate.concat("<character char_type=\"relative_range_value\" name=\"atypical_size\" from=\"\" to=\""+extreme.substring(p+2,q-2).trim()+"\" upper_restricted=\"false\"/>");
            			}else{
            				Element character = new Element("character");
                			character.setAttribute("char_type", "relative_range_value");
                			character.setAttribute("name", "atypical_size");
                			character.setAttribute("from","");
                			character.setAttribute("to", extreme.substring(p+2,q-1).trim());
                			innertagstate.add(character);
            				//innertagstate = innertagstate.concat("<character char_type=\"relative_range_value\" name=\"atypical_size\" from=\"\" to=\""+extreme.substring(p+2,q-1).trim()+"\"/>");
            			}
            		}
            		else{
            			if (extreme.charAt(q-2)=='+'){
            				Element character = new Element("character");
                			character.setAttribute("char_type", "relative_range_value");
                			character.setAttribute("name", "atypical_size");
                			character.setAttribute("from", extreme.substring(p+1,extreme.indexOf("-",p+1)).trim());
                			character.setAttribute("to", extreme.substring(extreme.indexOf("-",p+1)+1,q-2).trim());
                			character.setAttribute("upper_restricted", "false");
                			innertagstate.add(character);
            				//innertagstate = innertagstate.concat("<character char_type=\"relative_range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-2).trim()+"\" upper_restricted=\"false\"/>");
            			}else{
            				Element character = new Element("character");
                			character.setAttribute("char_type", "relative_range_value");
                			character.setAttribute("name", "atypical_size");
                			character.setAttribute("from", extreme.substring(p+1,extreme.indexOf("-",p+1)).trim());
                			character.setAttribute("to", extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim() );
                			//character.setAttribute("upper_restricted", "true");
                			innertagstate.add(character);
            				//innertagstate = innertagstate.concat("<character char_type=\"relative_range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim()+"\"/>");
            		
            			}
            		}
            	}
            	extreme = matcher1.replaceAll("#");
        		matcher1.reset();
        		j = extreme.length();
        		Pattern pattern23 = Pattern.compile("\\[[�\\d\\.\\s\\+]+\\]");
            	matcher1 = pattern23.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		if (extreme.charAt(q-2)=='+'){
            			Element character = new Element("character");
            			character.setAttribute("char_type", "relative_value");
            			character.setAttribute("name", "atypical_size");
            			character.setAttribute("from", extreme.substring(p+1,q-2).trim());
            			character.setAttribute("upper_restricted", "false");
            			innertagstate.add(character);
            			//innertagstate = innertagstate.concat("<character char_type=\"relative_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,q-2).trim()+"\" upper_restricted=\"false\"/>");
            		}else{
            			Element character = new Element("character");
            			character.setAttribute("char_type", "relative_value");
            			character.setAttribute("name", "atypical_size");
            			character.setAttribute("value", extreme.substring(p+1,q-1).trim());
            			innertagstate.add(character);
        				//innertagstate = innertagstate.concat("<character char_type=\"relative_value\" name=\"atypical_size\" value=\""+extreme.substring(p+1,q-1).trim()+"\"/>");
            		}
            	}
            	extreme = matcher1.replaceAll("#");
            	matcher1.reset();
            	j = extreme.length();      	
            	if(extreme.substring(i,j).contains("�")|extreme.substring(i,j).contains("-") && !extreme.substring(i,j).contains("�") && !extreme.substring(i,j).contains("x") && !extreme.substring(i,j).contains("X")){
        			String extract = extreme.substring(i,j);
        			Pattern pattern18 = Pattern.compile("[\\s]?[\\�\\-]?(% of [\\w]+ length|height of [\\w]+|times as [\\w]+ as [\\w]+|total length|their length|(times)?[\\s]?length of [\\w]+)");
                	Matcher matcher3 = pattern18.matcher(extract);
                	String relative="";
                	if ( matcher3.find()){
                		relative = extract.substring(matcher3.start(), matcher3.end());
                	}
                	extract = matcher3.replaceAll("#");
                	matcher3.reset();
                	
                	Element character = new Element("character");
        			character.setAttribute("char_type", "relative_range_value");
        			character.setAttribute("name", "size");
        			character.setAttribute("from", extract.substring(0, extract.indexOf('-')).trim());
        			character.setAttribute("to", extract.substring(extract.indexOf('-')+1,extract.indexOf('#')).trim());
        			character.setAttribute("relative_constraint",relative.trim());
        			innertagstate.add(character);
                	//innertagstate = innertagstate.concat("<character char_type=\"relative_range_value\" name=\"size\" from=\""+extract.substring(0, extract.indexOf('-')).trim()+"\" to=\""+extract.substring(extract.indexOf('-')+1,extract.indexOf('#')).trim()+"\" relative_constraint=\""+relative.trim()+"\"/>");
        			toval = extract.substring(0, extract.indexOf('-'));
        			fromval = extract.substring(extract.indexOf('-')+1,extract.indexOf('#'));
                	//sizect+=1;
        		}
        		else{
        			String extract = extreme.substring(i,j);
        			Pattern pattern18 = Pattern.compile("[\\s]?[\\�\\-]?(% of [\\w]+ length|height of [\\w]+|times as [\\w]+ as [\\w]+|total length|their length|(times)?[\\s]?length of [\\w]+)");
                	Matcher matcher3 = pattern18.matcher(extract);
                	String relative="";
                	if ( matcher3.find()){
                		relative = extract.substring(matcher3.start(), matcher3.end());
                	}
                	extract = matcher3.replaceAll("#");
                	matcher3.reset();
                	Element character = new Element("character");
        			character.setAttribute("char_type", "relative_value");
        			character.setAttribute("name", "size");
        			character.setAttribute("value", extract.substring(0,extract.indexOf('#')).trim());
        			character.setAttribute("relative_constraint", relative.trim());
        			innertagstate.add(character);
                	//innertagstate = innertagstate.concat("<character char_type=\"relative_value\" name=\"size\" value=\""+extract.substring(0,extract.indexOf('#')).trim()+"\" relative_constraint=\""+relative.trim()+"\"/>");
        			toval = extract.substring(0,extract.indexOf('#'));
        			fromval = extract.substring(0,extract.indexOf('#'));
        		}
        		
            	Iterator<Element> it = innertagstate.iterator();
        		while(it.hasNext()){
        			Element e = it.next();
        			if(e.getAttribute("to") != null && e.getAttributeValue("to").compareTo("")==0){
        				if(toval.endsWith("+")){
        					toval = toval.replaceFirst("\\+$", "");
        					e.setAttribute("upper_restricted", "false");
        				}
        				e.setAttribute("to", toval.trim());
        				e.setAttribute("to_inclusive", "false");
        			}
        			if(e.getAttribute("from") != null && e.getAttributeValue("from").compareTo("")==0){
        				e.setAttribute("from", fromval.trim());
        				e.setAttribute("from_inclusive", "false");
        			}
        		}
            	
        		/*StringBuffer sb = new StringBuffer();
				Pattern pattern25 = Pattern.compile("to=\"\"");
				matcher1 = pattern25.matcher(innertagstate);
				while ( matcher1.find()){
					matcher1.appendReplacement(sb, "to=\""+toval.trim()+"\"");
				}
				matcher1.appendTail(sb);
				innertagstate=sb.toString();
				matcher1.reset();
				StringBuffer sb1 = new StringBuffer();
				Pattern pattern26 = Pattern.compile("from=\"\"");
				matcher1 = pattern26.matcher(innertagstate);
				while ( matcher1.find()){
					matcher1.appendReplacement(sb1, "from=\""+fromval.trim()+"\"");
				}
				matcher1.appendTail(sb1);
				innertagstate=sb1.toString();
				matcher1.reset();*/
        	}
        	plaincharset = matcher2.replaceAll("#");
        	matcher2.reset();
        
        	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        	//   count                                                                                             ///////////////
        	
        	if(plaincharset.matches("\\[\\d+\\]")){
        		Element character = new Element("character");
    			character.setAttribute("name", "atypical_"+(cname==null?"count": cname));
    			character.setAttribute("value", plaincharset.replaceAll("[^\\d]", ""));
    			innertagstate.add(character);
    			plaincharset = "";
        	}
        	
        	if(plaincharset.matches("\\[\\d+\\+\\]")){
        		Element character = new Element("character");
        		character.setAttribute("char_type", "range_value");
    			character.setAttribute("name", "atypical_"+(cname==null?"count": cname));
    			character.setAttribute("from", plaincharset.replaceAll("[^\\d]", ""));
    			character.setAttribute("upper_restricted", "false");
    			innertagstate.add(character);
    			plaincharset = "";
        	}
        	
        	
        	//int countct = 0;
        	Pattern pattern15 = Pattern.compile("([\\[]?[�]?[\\d]+[\\]]?[\\s]?[\\[]?[\\�\\-][\\]]?[\\s]?[\\[]?[\\d]+[+]?[\\]]?|[\\[]?[�]?[\\d]+[+]?[\\]]?[\\s]?)[\\�\\�\\-]+[a-zA-Z]+");
        	matcher2 = pattern15.matcher(plaincharset);
        	plaincharset = matcher2.replaceAll("#");
        	matcher2.reset();     	
        	//Pattern pattern16 = Pattern.compile("(?<!([/][\\s]?))([\\[]?[�]?[\\d]+[\\]]?[\\s]?[\\[]?[\\�\\-][\\]]?[\\s]?[\\[]?[\\d]+[+]?[\\]]?[\\s]?([\\[]?[\\�\\-]?[\\]]?[\\s]?[\\[]?[\\d]+[+]?[\\]]?)*|[�]?[\\d]+[+]?)(?!([\\s]?[n/]|[\\s]?[\\�\\-]?% of [\\w]+ length|[\\s]?[\\�\\-]?height of [\\w]+|[\\s]?[\\�\\-]?times|[\\s]?[\\�\\-]?total length|[\\s]?[\\�\\-]?their length|[\\s]?[\\�\\-]?(times)?[\\s]?length of|[\\s]?[dcm�]?m))");
        	Pattern pattern16 = Pattern.compile("(?<!([/][\\s]?))([\\[]?[�]?[\\d\\./%]+[\\]]?[\\s]?[\\[]?[\\�\\-][\\]]?[\\s]?[\\[]?[\\d\\./%]+[+]?[\\]]?[\\s]?([\\[]?[\\�\\-]?[\\]]?[\\s]?[\\[]?[\\d\\./%]+[+]?[\\]]?)*|[�]?[\\d\\./%]+[+]?)(?!([\\s]?[n/]|[\\s]?[\\�\\-]?% of [\\w]+ length|[\\s]?[\\�\\-]?height of [\\w]+|[\\s]?[\\�\\-]?times|[\\s]?[\\�\\-]?total length|[\\s]?[\\�\\-]?their length|[\\s]?[\\�\\-]?(times)?[\\s]?length of|[\\s]?[dcm�]?m))");
        	matcher2 = pattern16.matcher(plaincharset);
        	while ( matcher2.find()){
        		i=matcher2.start();
        		j=matcher2.end();
        		String extreme = plaincharset.substring(i,j);
    			i = 0;
    			j = extreme.length();
        		Pattern pattern20 = Pattern.compile("\\[[�\\d\\.\\s\\+]+[\\�\\-]{1}[�\\d\\.\\s\\+\\�\\-]*\\]");
            	Matcher matcher1 = pattern20.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		if(extreme.charAt(q-2)=='�' | extreme.charAt(q-2)=='-'){
            			Element character = new Element("character");
            			character.setAttribute("char_type", "range_value");
            			character.setAttribute("name", "atypical_"+(cname==null?"count": cname));
            			character.setAttribute("from", extreme.substring(p+1,q-2).trim());
            			character.setAttribute("to", "");
            			innertagstate.add(character);
            			
            			//innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_count\" from=\""+extreme.substring(p+1,q-2).trim()+"\" to=\"\"/>");
            		}else{
            			Element character = new Element("character");
            			character.setAttribute("char_type", "range_value");
            			character.setAttribute("name", "atypical_"+(cname==null?"count": cname));
            			character.setAttribute("from", extreme.substring(p+1,extreme.indexOf("-",p+1)).trim());
            			character.setAttribute("to", extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim());
            			innertagstate.add(character);
            			//innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_count\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim()+"\"/>");
            		}
            	}
            	extreme = matcher1.replaceAll("#");
        		matcher1.reset();
        		if(extreme.contains("#"))
        			i = extreme.indexOf("#")+1;
        		j = extreme.length();
        		Pattern pattern21 = Pattern.compile("\\[[�\\d\\.\\s\\+\\�\\-]*[\\�\\-]{1}[�\\d\\.\\s\\+]+\\]");
            	matcher1 = pattern21.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		j = p;
            		if (extreme.charAt(p+1)=='�' | extreme.charAt(p+1)=='-'){
            			if (extreme.charAt(q-2)=='+'){
            				Element character = new Element("character");
                			character.setAttribute("char_type", "range_value");
                			character.setAttribute("name", "atypical_"+(cname==null?"count": cname));
                			character.setAttribute("from", "");
                			character.setAttribute("to", extreme.substring(p+2,q-2).trim());
                			character.setAttribute("upper_restricted", "false");
                			innertagstate.add(character);
            				//innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_count\" from=\"\" to=\""+extreme.substring(p+2,q-2).trim()+"\" upper_restricted=\"false\"/>");
            			}else{
            				Element character = new Element("character");
                			character.setAttribute("char_type", "range_value");
                			character.setAttribute("name", "atypical_"+(cname==null?"count": cname));
                			character.setAttribute("from", "");
                			character.setAttribute("to", extreme.substring(p+2,q-1).trim());
                			innertagstate.add(character);
            				//innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_count\" from=\"\" to=\""+extreme.substring(p+2,q-1).trim()+"\"/>");
            			}
            		}
            		else{
            			if (extreme.charAt(q-2)=='+'){
            				Element character = new Element("character");
                			character.setAttribute("char_type", "range_value");
                			character.setAttribute("name", "atypical_"+(cname==null?"count": cname));
                			character.setAttribute("from", extreme.substring(p+1,extreme.indexOf("-",p+1)).trim());
                			character.setAttribute("to", extreme.substring(p+1,extreme.indexOf("-",p+1)).trim());
                			character.setAttribute("upper_restricted", "false");
                			innertagstate.add(character);
            				//innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_count\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-2).trim()+"\" upper_restricted=\"false\"/>");
            			}else{
            				Element character = new Element("character");
                			character.setAttribute("char_type", "range_value");
                			character.setAttribute("name", "atypical_"+(cname==null?"count": cname));
                			character.setAttribute("from", extreme.substring(p+1,extreme.indexOf("-",p+1)).trim());
                			character.setAttribute("to", extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim());
                			innertagstate.add(character);
            				//innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_count\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim()+"\"/>");
            			}
            		}
            			
            	}
        		matcher1.reset();
        		Pattern pattern23 = Pattern.compile("\\[[�\\d\\.\\s\\+]+\\]");
            	matcher1 = pattern23.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		j = p;
            		if (extreme.charAt(q-2)=='+'){
            			Element character = new Element("character");
            			character.setAttribute("char_type", "range_value");
            			character.setAttribute("name", "atypical_"+(cname==null?"count": cname));
            			character.setAttribute("from", extreme.substring(p+1,q-2).trim());
            			character.setAttribute("upper_restricted", "false");
            			innertagstate.add(character);
            			//innertagstate = innertagstate.concat("<character name=\"atypical_count\" from=\""+extreme.substring(p+1,q-2).trim()+"\" upper_restricted=\"false\"/>");
            		}else{
            			Element character = new Element("character");
            			character.setAttribute("name", "atypical_"+(cname==null?"count": cname));
            			character.setAttribute("value", extreme.substring(p+1,q-1).trim());
               			innertagstate.add(character);
        				//innertagstate = innertagstate.concat("<character name=\"atypical_count\" value=\""+extreme.substring(p+1,q-1).trim()+"\"/>");
            		}
            	}
            	matcher1.reset();
        		if(extreme.substring(i,j).contains("�")|extreme.substring(i,j).contains("-") && !extreme.substring(i,j).contains("�") && !extreme.substring(i,j).contains("x") && !extreme.substring(i,j).contains("X")){
        			String extract = extreme.substring(i,j);
        			Pattern pattern22 = Pattern.compile("[\\[\\]]+");
        			matcher1 = pattern22.matcher(extract);
        			extract = matcher1.replaceAll("");
        			matcher1.reset();
        			
        			String to = extract.substring(extract.indexOf('-')+1,extract.length()).trim();
        			boolean upperrestricted = true;
        			if(to.endsWith("+")){
        				upperrestricted = false;
        				to = to.replaceFirst("\\+$", "");
        			}
        			Element character = new Element("character");
        			character.setAttribute("char_type", "range_value");
        			character.setAttribute("name", cname==null?"count": cname);
        			character.setAttribute("from", extract.substring(0, extract.indexOf('-')).trim());
        			character.setAttribute("to", to);
        			if(!upperrestricted)
        				character.setAttribute("upper_restricted", upperrestricted+"");
        			innertagstate.add(character);
                	//innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"count\" from=\""+extract.substring(0, extract.indexOf('-')).trim()+"\" to=\""+extract.substring(extract.indexOf('-')+1,extract.length()).trim()+"\"/>");
        			toval = extract.substring(0, extract.indexOf('-'));
        			fromval = extract.substring(extract.indexOf('-')+1,extract.length());
        			//countct+=1;
        		}
        		else{
        			String extract = extreme.substring(i,j);
        			Element character = new Element("character");
        			character.setAttribute("name", cname==null?"count": cname);
        			character.setAttribute("value", extract.trim());
        			innertagstate.add(character);
        			//innertagstate = innertagstate.concat("<character name=\"count\" value=\""+extract.trim()+"\"/>");
        			toval = extract;
        			fromval = extract;
        		}
        		
        		Iterator<Element> it = innertagstate.iterator();
        		while(it.hasNext()){
        			Element e = it.next();
        			if(e.getAttribute("to") != null && e.getAttributeValue("to").compareTo("")==0){
        				if(toval.endsWith("+")){
        					toval = toval.replaceFirst("\\+$", "");
        					e.setAttribute("upper_restricted", "false");
        				}
        				e.setAttribute("to", toval.trim());
        				e.setAttribute("to_inclusive", "false");
        			}
        			if(e.getAttribute("from") != null && e.getAttributeValue("from").compareTo("")==0){
        				e.setAttribute("from", fromval.trim());
        				e.setAttribute("from_inclusive", "false");
        			}
        		}
        		/*
        		StringBuffer sb = new StringBuffer();
				Pattern pattern25 = Pattern.compile("to=\"\"");
				matcher1 = pattern25.matcher(innertagstate);
				while ( matcher1.find()){
					matcher1.appendReplacement(sb, "to=\""+toval.trim()+"\"");
				}
				matcher1.appendTail(sb);
				innertagstate=sb.toString();
				matcher1.reset();
				StringBuffer sb1 = new StringBuffer();
				Pattern pattern26 = Pattern.compile("from=\"\"");
				matcher1 = pattern26.matcher(innertagstate);
				while ( matcher1.find()){
					matcher1.appendReplacement(sb1, "from=\""+fromval.trim()+"\"");
				}
				matcher1.appendTail(sb1);
				innertagstate=sb1.toString();
				matcher1.reset();*/
        	}
        	matcher2.reset();   
 		}
		catch (Exception e)
        {
			e.printStackTrace();
    		System.err.println(e);
        }
		
		if(debug){
			try{
				XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
				Iterator<Element> it = innertagstate.iterator();
				while(it.hasNext()){
					Element e = it.next();
					System.out.println(outputter.outputString(e));
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
 		return innertagstate;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//String str1 = "stems 2�3";
		//String str2 = "stems 2�3";	
		//String str1 = "10-20 mm � 40-50 mm";
		//String str1 = "4-5[+]"; //should output atypical count, but output count
		//String str1 = "4-5[-5+]";
		//String str1 = "[5-]10-15[-20]";
		//String str1 = "[30-]80-250[-450+];";
		//String str1 ="3-5 �(0.6-)1.5-2 cm"; //not solved
		String str1 ="3-5 �1.5-2 cm"; //not solved
		//String str1 = "[5+]";
		String str2 = null;	
		
		System.out.println(NumericalHandler.parseNumericals(str1, str2));
	}

}
