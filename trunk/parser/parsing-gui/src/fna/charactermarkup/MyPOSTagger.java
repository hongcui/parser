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
	static public String numberpattern = "[ ()\\[\\]\\-\\–\\d\\.×\\+°²½/¼\\*/%]{4,}";

	/**
	 * 
	 */
	public MyPOSTagger(String database) {
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * 		//insert our POS tags to segments (simple or complex: new segmentation)
			 //output POSed segments to a database table and to the posed file	
			  * str is markedsent

	 */
		public String POSTag(String str){
		try{
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			Statement stmt3 = conn.createStatement();
			//ResultSet rs = stmt.executeQuery("select * from newsegments");
			//ResultSet rs = stmt.executeQuery("select * from markedsentence order by source");
			//while(rs.next()){
				//str=rs.getString(3);
				//str = rs.getString(2);
				/* normalization done by Ramu
				str = reversecondense(str); //when excuted on markedsentence, no condense was done so no reverse is needed
				str = plaintextextractor(str);
				Pattern pattern1 = Pattern.compile("_");
	           // Replace all occurrences of pattern in input
	           Matcher matcher1 = pattern1.matcher(str);
	           str = matcher1.replaceAll(" ");
	           matcher1.reset();
	           Pattern pattern2 = Pattern.compile(":");
	           // Replace all occurrences of pattern in input
	           matcher1 = pattern2.matcher(str);
	           str = matcher1.replaceAll(",");
	           matcher1.reset();
	           */
				
				
	           //Pattern pattern3 = Pattern.compile("[\\d]+[\\-\\–]+[\\d]+");
	           Pattern pattern3 = Pattern.compile(this.numberpattern);
				//Pattern pattern4 = Pattern.compile("(?<!(ca[\\s]?|diam[\\s]?))([\\d]?[\\s]?\\.[\\s]?[\\d]+[\\s]?[\\–\\-]+[\\s]?[\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]+[\\s]?[\\–\\-]+[\\s]?[\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]/[\\d][\\s]?[\\–\\-][\\s]?[\\d]/[\\d])|(?<!(ca[\\s]?|diam[\\s]?))([\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]/[\\d])");
				//Pattern pattern5 = Pattern.compile("[\\d±\\+\\–\\-\\—°²:½/¼\"“”\\_´\\×µ%\\*\\{\\}\\[\\]=]+");
				Pattern pattern5 = Pattern.compile("[\\d\\+°²½/¼\"“”´\\×µ%\\*]+");
				Pattern pattern6 = Pattern.compile("([\\s]*0[\\s]*)+");
				//Pattern pattern5 = Pattern.compile("((?<!(/|(\\.[\\s]?)))[\\d]+[\\-\\–]+[\\d]+(?!([\\–\\-]+/|([\\s]?\\.))))|((?<!(\\{|/))[\\d]+(?!(\\}|/)))");
	           //[\\d±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×\\s,µ%\\*\\{\\}\\[\\]=(<\\{)(\\}>)]+
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
	           
	           str = str.replaceAll("0\\s+-", "0-").replaceAll("0", "3"); //stanford parser gives different results on 0 and other numbers.
	           
	           //str = str.replaceAll("}>", "/NN").replaceAll(">}", "/NN").replaceAll(">", "/NN").replaceAll("}", "/JJ").replaceAll("[<{]", "");
	           
	           
	           StringBuffer sb = new StringBuffer();
	           Pattern pattern7 = Pattern.compile("(.*?)([<{]*)([a-zA-Z-]+)[}>]*(.*)");
	           Matcher m = pattern7.matcher(str);
	           while ( m.matches()){
	        	   sb.append(m.group(1));
	        	   String pos = m.group(2);
	        	   String word = m.group(3);
	        	   str = m.group(4);
	        	   if(word.endsWith("ly")){
	        		   sb.append(word);
	        		   m = pattern7.matcher(str);
	        		   continue;
	        	   }	  
	       		   ResultSet rs1 = stmt1.executeQuery("select * from wordpos4parser where word='"+word+"'");
	       		   String p = "";
	       		   if(rs1.next()){
	       			   p = rs1.getString(2);
	       		   }
	       		   if(p.contains("p")){ //<inner> larger.
	       					//System.out.println(rs1.getString(2));
	       					sb.append(word+"/NNS ");
	       		   }else if(p.contains("s") || pos.indexOf('<') >=0){
	       					sb.append(word+"/NN ");
	       		   }else if(p.contains("b")|| pos.indexOf('{') >=0){
	       			   		//ResultSet rs3 = stmt1.executeQuery("select word from wordpos4parser where word='"+word+"' and certaintyl>5");
	       					ResultSet rs2 = stmt1.executeQuery("select word from Brown.wordfreq where word='"+word+"' and freq>79");//1/largest freq in wordpos = 79/largest in brown
	       					if(rs2.next()){
	       						sb.append(word+" ");
	       					}else{
	       						sb.append(word+"/JJ ");
	       					}
	       		   }else{
	       					sb.append(word+" ");
	       		   }
	       		   m = pattern7.matcher(str);
	       		}
	           	sb.append(str);
	       		str = sb.toString();
	       		str = str.replaceAll("(?<=[a-z])\\s+[_–-]\\s+(?=[a-z])", "-").replaceAll("/[A-Z]+\\s*[-–]\\s*", "-").replaceAll("\\d-\\s+(?=[a-z])", "3-"); //non -septate/JJ or linear/JJ _ovoid/JJ
	       		str = str.replaceAll("[\\[\\(]", " -LRB-/-LRB- ").replaceAll("[\\)\\]]", " -RRB-/-RRB- ").replaceAll("\\s+", " ").trim(); 
	       		str = str.replaceAll("±","more-or-less/RB");
	       		return str;
			//}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return "";
	}
		
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
