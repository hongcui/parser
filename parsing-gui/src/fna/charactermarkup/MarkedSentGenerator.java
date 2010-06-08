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

public class MarkedSentGenerator {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "root";
	static protected String password = "";
	
	public MarkedSentGenerator(String database) {
		// TODO Auto-generated constructor stub
		collect(database);
	}
	
	protected void collect(String database){
		MarkedSentGenerator.database = database;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists markedsentence (source varchar(100) NOT NULL, markedsent varchar(2000), PRIMARY KEY(source))");
				stmt.execute("delete from markedsentence");
				markedsentgen();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	protected void markedsentgen(){
		try
		{
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			String str = "";
        	ResultSet rs = stmt.executeQuery("select * from sentence");
        	while(rs.next()){
        		str = rs.getString(3);
        		Pattern pattern = Pattern.compile("<N>");
                Matcher matcher = pattern.matcher(str);
                str = matcher.replaceAll("<");
                matcher.reset();
                Pattern pattern1 = Pattern.compile("</N>");
                matcher = pattern1.matcher(str);
                str = matcher.replaceAll(">");
                matcher.reset();
                StringBuffer sb = new StringBuffer();
                Pattern pattern2 = Pattern.compile("(<B>)([\\(\\)?\\–\\—°²:=/½\"¼x´\\×\\*µ%“”+\\d±.;,\\[\\]]+)(</B>)");
                matcher = pattern2.matcher(str);
                while (matcher.find()){
                	matcher.appendReplacement(sb, matcher.group(2));
                }
                matcher.appendTail(sb);
        		str=sb.toString();
                matcher.reset();
                Pattern pattern3 = Pattern.compile("<M><B>|<B>|<M>");
                matcher = pattern3.matcher(str);
                str = matcher.replaceAll("{");
                matcher.reset();
                Pattern pattern4 = Pattern.compile("</B></M>|</B>|</M>");
                matcher = pattern4.matcher(str);
                str = matcher.replaceAll("}");
                matcher.reset();
        		stmt1.execute("insert into markedsentence values('"+rs.getString(2)+"','"+str+"')");
        	}
		}catch (Exception e)
        {
    		System.err.println(e);
        }
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new MarkedSentGenerator("benchmark_learningcurve_fnav19_test_24");
	}

}
