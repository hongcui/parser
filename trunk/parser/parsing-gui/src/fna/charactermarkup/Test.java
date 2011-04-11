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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Test t = new Test();
		t.constraint();
		//String text = "that often do not overtop the heads";
		//t.breakText(text);
	}

}
