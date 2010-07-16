/**
 * 
 */
package fna.charactermarkup;

import java.io.FileOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import edu.stanford.nlp.parser.lexparser.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
/**
 * @author hongcui
 *
 */
public class Test {
	Connection conn = null;
	/**
	 * 
	 */
	public Test() {
		String sent = "<a> {b} {c} {d} of {e} {f} <g>";
		String[] temp = sent.split("\\s+");
		ArrayList<String> chunkedtokens = new ArrayList<String>(Arrays.asList(temp));
		int totaltokens = temp.length;
		String treetext = "a [b [c] d] of e [f] g"; 
		String[] treetoken = treetext.split("\\s+");
		String realchunk = "";
		ArrayList<String> brackets = new ArrayList<String>();
		for(int i = 0; i<treetoken.length; i++){
			if(treetoken[i].indexOf('[') >=0){
				brackets.add("[");
			}
			if(brackets.size()>0){//in
				realchunk += treetoken[i]+" ";
				chunkedtokens.set(i, "");
			}
			if(brackets.size()==0 && realchunk.length()>0){
				chunkedtokens.set(i-1, realchunk.trim());
				realchunk="";
			}
			if(treetoken[i].indexOf(']')>=0){
				brackets.remove(0);
			}
		}
		String r = "";
		Iterator<String> it = chunkedtokens.iterator();
		while(it.hasNext()){
			String t = it.next();
			if(t.compareTo("") !=0){
				 r+= t+" ";
			}
		}
		r.trim();
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Test t = new Test();
	}

}
