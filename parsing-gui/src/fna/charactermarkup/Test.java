 /* $Id$ */
/**
 * 
 */
package fna.charactermarkup;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @author hongcui
 *
 */
@SuppressWarnings({ "unused" })
public class Test {
	Connection conn = null;
	private static Pattern areapattern = Pattern.compile("(.*?)([\\d\\.()+-]+ ?\\{?[cmd]?m?\\}?×\\S*\\s*[\\d\\.()+-]+ \\{?[cmd]?m\\}?×?(\\S*\\s*[\\d\\.()+-]+ \\{?[cmd]?m\\}?)?)(.*)");

	/**
	 * 
	 */
	public Test() {
	}
	
	public void constraint(){
		String[] organ = new String[]{"long", "cauline", "leaf", "abaxial", "surface", "trichomode"};
		Hashtable<String, String> mapping = new Hashtable<String, String>();
		mapping.put("cauline", "type");
		mapping.put("leaf", "parent_organ");
		mapping.put("long", "null");
		mapping.put("surface", "parent_organ");
		mapping.put("abaxial", "type");
		mapping.put("trichomode", "type");
		int j = 5;
		boolean terminate =false;
		for(;j >=0; j--){
			if(terminate) break;
			String w = organ[j].replaceAll("(\\w+\\[|\\]|\\{|\\})", "");
			String type = "null";
			if(w.startsWith("(")) type="parent_organ";
			else type = mapping.get(w);
			if(!type.equals("null")){
				organ[j] = "";
				if(type.equals("type")){
					System.out.println("constraint_"+type+": "+w.replaceAll("(\\(|\\))", "").trim()); //may not have.						
				}else{//"parent_organ": collect all until a null constraint is found
					String constraint = w;
					j--;
					for(; j>=0; j--){
						w = organ[j].replaceAll("(\\w+\\[|\\]|\\{|\\})", "");
						if(w.startsWith("(")) type="parent_organ";
						else type = mapping.get(w);;
						if(!type.equals("null")){
							constraint = w+" "+constraint;
							organ[j] = "";
						}
						else{
							System.out.println("constraint_parent_organ: "+constraint.replaceAll("(\\(|\\))", "").trim()); //may not have.
							terminate = true;
							break;
						}
					}
				}
			}else{
				break;
			}
		}
		j++;
		System.out.println(j);
	}
	public void test1(){

		String tsent = "<a b> a b <a b c> {a b} <a> <b>";
		Pattern p = Pattern.compile("(.*?<[^>]*) ([^<]*>.*)");//<floral cup> => <floral-cup>
		Matcher m = p.matcher(tsent);
		while(m.matches()){
			tsent = m.group(1)+"-"+m.group(2);
			m = p.matcher(tsent);
		}
		System.out.println(tsent);
	}

	private ArrayList<String> breakText(String text) {
		ArrayList<String> tokens = new ArrayList<String>();
		String[] words = text.split("\\s+");
		String t = "";
		int left = 0;
		for(int i = 0; i<words.length; i++){
			String w = words[i];
			if(w.indexOf("[")<0 && w.indexOf("]")<0 && left==0){
				if(!w.matches("\\b(this|have|that|may|be)\\b")){tokens.add(w);};
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

	public String addSentmod(String subject, String sentmod) {
		String[] tokens = subject.split("\\s+");
		String substring = "";
		for(int i = 0; i<tokens.length; i++){
			if(!sentmod.matches(".*?\\b"+tokens[i].replaceAll("[{()}]", "")+"\\b.*")){
				substring +=tokens[i]+" ";
			}
		}
		substring = substring.trim();
		substring ="{"+sentmod.replaceAll("[\\[\\]]", "").replaceAll(" ", "} {").replaceAll("[{(]and[)}]", "and").replaceAll("[{(]or[)}]", "or").replaceAll("\\{\\}", "").replaceAll("\\s+", " ")+"} "+substring;
		return substring;
	}
	
	private static String combineModifiers(String element){
		Pattern ptn = Pattern.compile("(.*? )(modifier=\\S+)(['\"].*)");
		Matcher m = ptn.matcher(element);
		String result = "";
		String modifiers = "";
		while(m.matches()){
			result +=m.group(1).replaceFirst("^['\"]", "");
			modifiers += m.group(2).replaceAll("modifier=", "")+";";
			element = m.group(3);
			m = ptn.matcher(element);
		}
		result += element.replaceFirst("^['\"]", "");
		modifiers = "modifier=\""+modifiers.replaceAll("['\"]", "").replaceAll("\\W+$", "").trim()+"\"";
		result = result.replaceFirst("value", modifiers+" value").replaceAll("\\s+", " ");
		return result;
	}
	
	private static String normalizemodifier(String str) {
		String[] tokens = str.trim().split("\\s+");
		List<String> chunkedtokens = Arrays.asList(tokens);
		String result = "";
		Pattern modifierlist = Pattern.compile("(.*?\\b)(\\w+ly\\s+(?:to|or)\\s+\\w+ly)(\\b.*)");
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
				chunkedtokens.set(i, "");
			}
			chunkedtokens.set(start, newtoken);
		}
		result +=str;
		return result;
		
		
	}
	
	private String[] normalizeArea(String text){
		String[] result = new String[2];
		String text2= text;
		Matcher m = areapattern.matcher(text);
		while(m.matches()){
			if(m.group(2).matches("\\d.*")){
				text = m.group(1)+m.group(2).replaceAll("[ \\{\\}]", "")+ m.group(4);
				//m = areapattern.matcher(text2);
				//m.matches();
				text2 = m.group(1)+m.group(2).replaceAll("[cmd]?m", "").replaceAll("[ \\{\\}]", "")+ m.group(4);
				m = areapattern.matcher(text);
			}else {//{pistillate} 9-47 ( -55 in <fruit> ) ×5.5-19 mm , {flowering} <branchlet> 0-4 mm ; m.group(2)= ) ×5.5-19 mm , {flowering} <branchlet> 0-4 mm ;
				String left = "";
				if(m.group(2).startsWith(")")) left = "(";
				if(m.group(2).startsWith("]")) left = "[";
				if(left.length()>0){
					String temp = m.group(1); //m.group(1) = {pistillate} 9-47 ( -55 in <fruit> 
					//find the starting brackets in temp and remove the braketed content
					temp = temp.substring(0, temp.lastIndexOf(left)).trim();
					String group2 = m.group(2).replaceFirst("^[)\\]]", "").replaceAll("[ \\{\\}]", "");
					if(temp.matches(".*?\\d$")){
						text = temp+ group2 + m.group(4);
						//m = areapattern.matcher(text2);
						//m.matches();
						text2 = temp+group2.replaceAll("[cmd]?m", "").replaceAll("[ \\{\\}]", "")+ m.group(4);
						m = areapattern.matcher(text);
					}
				}
			}
		}
		result[0] = text;//{oblanceolate} , 15-70×30-150+cm , {flat}  
		result[1] = text2;//{oblanceolate} , 15-70×30-150+ , {flat} 
		return result;
}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Test t = new Test();
		
		System.out.println(
		//t.addSentmod("{distal} (face)", "distal [basal leaf]")
		//t.combineModifiers("<character name=\"n\" modifier=\"a\" value=\"c\"/>")
		//t.normalizemodifier("leaves shallowly to deeply pinnatifid, weekly to strongly angled")
		t.normalizeArea("{pistillate} 9-47 ( -55 in <fruit> ) ×5.5-19 mm , {flowering} <branchlet> 0-4 mm ;")[1]		
				);
		//{pistillate} 9-47 ( -55 in <fruit> )×5.5-19mm , {flowering} <branchlet> 0-4 mm ;
		//String text = "that often do not overtop the heads";
		//t.breakText(text);
	}

}
