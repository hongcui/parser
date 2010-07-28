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
		String text="a"+" a";
		System.out.println(text.split("\\s+").length);
		text= change(text);
		System.out.println(text);
	}

	public String change(String text){
		
		return text.replaceFirst("/$", "");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Test t = new Test();
	}

}
