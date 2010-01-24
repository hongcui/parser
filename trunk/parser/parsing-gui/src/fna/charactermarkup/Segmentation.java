package fna.charactermarkup;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.*;
import java.io.File;
import java.util.regex.*;

public class Segmentation {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "root";
	static protected String password = "";

	public Segmentation(String database) {
		// TODO Auto-generated constructor stub
		collect(database);
	}

	protected void collect(String database){
		Segmentation.database = database;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists segments (sentid MEDIUMINT NOT NULL, source varchar(100) NOT NULL, markedsent varchar(2000), patternid MEDIUMINT NOT NULL, PRIMARY KEY(sentid))");
				stmt.execute("delete from segments");
				constraintfilter2();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//break into smaller segments
	protected void constraintfilter2(){
		try
		{
			String source;
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			int count=0;
			ResultSet rs = stmt.executeQuery("select * from tmp_result3");
			while(rs.next())
        	{
					source=rs.getString(1);
        			String [] terms = rs.getString(2).split(",");
        			String str = ""; 
        			for(int i=0;i<terms.length;i++){
        				CharSequence inputStr = terms[i];              
        				String str1 = "";
        				// Compile regular expression
        				
                        Pattern pattern = Pattern.compile("((?<![\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×,µ\\*\\{\\}\\[\\](<\\{)(\\}>) m]\\s)<[a-zA-Z_]+>(\\s<[a-zA-Z_]+>)*[\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×\\s,µ\\*\\{\\}\\[\\](<\\{)(\\}>) m]*)");
                        
                        // Replace all occurrences of pattern in input
                        Matcher matcher = pattern.matcher(inputStr);
                        if ( matcher.find()){
                        	str1=terms[i];
                        	Pattern pattern1 = Pattern.compile("(?<!\\})>\\s<(?!\\{)");
                        	Matcher matcher1 = pattern1.matcher(str1);
                        	str1 = matcher1.replaceAll("_has_");
                            matcher1.reset();

                            Pattern pattern3 = Pattern.compile("[\\s]+");
                            matcher1 = pattern3.matcher(str1);
                            str1 = matcher1.replaceAll(" ");
                            matcher1.reset();
                            count+=1;
                            if(i<terms.length-1)
                            	str1=str1.concat(",");
                            stmt1.execute("insert into segments values('"+count+"','"+source+"','"+str1+"',0)");
                        	int sind=terms[i].indexOf("<");
                           	int eind=terms[i].indexOf(">");
                       		str=terms[i].substring(sind,eind+1);
                        }
                        else{
                        	String hide = terms[i];
                        	Pattern pattern2 = Pattern.compile("(<[a-zA-Z_ ]+>)|((?<![<])\\{[a-zA-Z_\\./\\-\\d\\–{}:]+\\}(?![>]))");
                        	Matcher matcher1 = pattern2.matcher(hide);
                        	
                        	hide=matcher1.replaceAll("#");
                        	matcher1.reset();
                        	//System.out.println(source+":"+terms[i]);
                        	//System.out.println(source+":"+hide);
                        	Pattern pattern4 = Pattern.compile("[\\w±]+|(<\\{[a-zA-Z_]+\\}>)");
                        	matcher1 = pattern4.matcher(hide);
                        	if ( matcher1.find())
                        		str1=str.concat(terms[i]);
                        	else
                        		str1=terms[i];
                        	matcher1.reset();
                        	
                        	Pattern pattern1 = Pattern.compile("(?<!\\})>\\s<(?!\\{)");
                        	matcher1 = pattern1.matcher(str1);
                        	str1 = matcher1.replaceAll("_has_");
                            matcher1.reset();
                            
                            Pattern pattern3 = Pattern.compile("[\\s]+");
                            matcher1 = pattern3.matcher(str1);
                            str1 = matcher1.replaceAll(" ");
                            matcher1.reset();
                            count+=1;
                            if(i<terms.length-1)
                            	str1=str1.concat(",");
                            stmt1.execute("insert into segments values('"+count+"','"+source+"','"+str1+"',0)");
                        	if(i==0){
                        		int sind=terms[i].indexOf("<");
                            	int eind=terms[i].indexOf(">");
                            	if(sind<0)
                            		str=str.concat("<org>");
                            	else
                            		str=terms[i].substring(sind,eind+1);
                        	}
                        }
                        matcher.reset();
        			}
        	}
		}
        catch (Exception e)
        {
        		System.err.println(e);
        }
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new Segmentation("fnav19_benchmark");
	}

}
