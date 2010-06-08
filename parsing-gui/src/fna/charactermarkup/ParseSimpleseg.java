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

public class ParseSimpleseg {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "root";
	static protected String password = "";
	
	public ParseSimpleseg() {
		// TODO Auto-generated constructor stub
	}
	
	public ParseSimpleseg(String database) {
		// TODO Auto-generated constructor stub
		collect(database);
	}
	
	protected void collect(String database){
		ParseSimpleseg.database = database;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists marked_simpleseg (sentid MEDIUMINT NOT NULL, source varchar(100) NOT NULL, markedsent TEXT, PRIMARY KEY(sentid))");
				stmt.execute("delete from marked_simpleseg");
				parse_simpleseg();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	protected void parse_simpleseg(){
		try
		{
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			Statement stmt3 = conn.createStatement();
			String str;
			String newsrcstr="";
			String oldsrcstr="";
			String oldorg="";
			FileOutputStream ostream = new FileOutputStream("relation.txt"); 
			PrintStream out = new PrintStream( ostream ); 
        	ResultSet rs = stmt.executeQuery("select * from segments");
        	while(rs.next()){
        		int orcount=0;
        		int srcflag=0;
        		newsrcstr=rs.getString(2);
        		if(oldsrcstr.compareTo(newsrcstr)==0)
        			srcflag=1;
        		str=rs.getString(3);
        		Pattern pattern = Pattern.compile("(<[a-zA-Z_ ]+>)");
                
                // Replace all occurrences of pattern in input
                Matcher matcher = pattern.matcher(str);
                while ( matcher.find()){
                	orcount +=1;
                }
                matcher.reset();
                if(orcount==1|orcount==0){
                	str = reversecondense(str);
                	String str1="";
                	str1 = plaintextextractornum(str);
                	String str6= plaintextextractor(str);
                	String str2="";
                	String str4="";
                	String chrtag="";
                	String modifier="";
                	int flag6 = 0;
                	int flag8 = 0;
                	int ct = 0;
                	str2=str2.concat("<statement id=\"s"+rs.getString(1)+"\">");
                	Pattern pattern2 = Pattern.compile("<[a-zA-Z_ ]+>");
                	matcher = pattern2.matcher(str);
                	while ( matcher.find()){
                		ct++;
                		int i=matcher.start();
                		int j=matcher.end();
                		if(ct==1 && srcflag==0)
                			oldorg=str.subSequence(i,j).toString();
                		else if(ct==1 && srcflag==1){
                			Pattern pattern16 = Pattern.compile("((?<![\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×,µ%\\*\\{\\}\\[\\](<\\{)(\\}>) m]\\s)<[a-zA-Z_ ]+>(\\s<[a-zA-Z_ ]+>)*[\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×\\s,µ%\\*\\{\\}\\[\\](<\\{)(\\}>) m]*)");
                            
                            // Replace all occurrences of pattern in input
                            Matcher matcher1 = pattern16.matcher(str);
                            if ( matcher1.find()){
                            	oldorg=str.subSequence(i,j).toString();
                            }
                            matcher1.reset();
                		}               			
                			if(!str.subSequence(i,j).toString().contains("<n>")){
                				String singorg = str.subSequence(i+1,j-1).toString();
                				ResultSet rs2 = stmt3.executeQuery("select * from singularplural where plural='"+singorg+"'");
                				while(rs2.next()){
                					singorg = rs2.getString(1);
                				}
                				if(ct>1){
                					str2="<structure name=\""+singorg+"\" modifier=\""+modifier+"\" id=\"o"+ct+"\">";
                					str4="</structure>";
                				}
                				else{
                					str2=str2.concat("<structure name=\""+singorg+"\" id=\"o"+ct+"\">");
                					str4="</structure>";
                					modifier=singorg;
                				}
                				Pattern pattern7 = Pattern.compile("([\\d]?[\\s]?n[\\s]?=[\\s]?)+[\\[]?[\\d]+[\\]]?([\\s]?[\\–\\-\\—]?[:]?[\\s]?[\\[]?[\\d]+[\\]]?)*|x[\\s]?=[\\s]?[\\[]?[\\d]+[\\]]?([\\s]?[\\–\\-\\—]?[:]?[\\s]?[\\[]?[\\d]+[\\]]?)*");
                				Matcher matcher1 = pattern7.matcher(str1);
                				if ( matcher1.find()){
                					int o=matcher1.start();
                					int p=matcher1.end();
                					String chr = str1.substring(o,p);
                					chrtag = "<structure name=\"chromosome_count\" id=\"o"+(ct+1)+"\">";
                					Pattern pattern15 = Pattern.compile("(?<!([/][\\s]?))([±]?[\\d]+[\\–\\-][\\d]+[+]?|[±]?[\\d]+[+]?)(?!([\\s]?[n/]))");
                                	Matcher matcher2 = pattern15.matcher(chr);
                                	int flag7=0;
                                	while ( matcher2.find()){
                                		int q=matcher2.start();
                                		int r=matcher2.end();
                                		chrtag=chrtag.concat("<character name=\"count\" value=\""+chr.subSequence(q,r).toString()+"\"/>");
                                	}
                                	chrtag=chrtag.concat("</structure><Text>"+chr+"</Text>");
                                	matcher2.reset();
                            		str1 = str1.substring(0, o);
                            		str6 = str6.substring(0, o);
                            		flag8 = 1;
                					/*i=matcher1.start();
                					j=matcher1.end();
                					str2=str2.concat(" "+"chromosome_count=\""+str1.subSequence(i,j).toString()+"\"");*/
                				}
                				matcher1.reset();
                			}
                			else{
                				if(flag8==0){
	                				if(ct>1){
	                					str2=str2.concat("<structure name=\"chromosome_count\" id=\"o"+ct+"\">");
	                					str4=str4.concat("</structure>");
	                				}
	                				else{
	                				str2=str2.concat("<structure name=\"chromosome_count\" id=\"o"+ct+"\">");
	                				str4="</structure>";
	                				}
	                				//str1="";
                				}
                			}
                		flag6=1;
                	}
                	if(flag6 == 0){
                		Pattern pattern8 = Pattern.compile("[\\d]?[\\s]?n[\\s]?=[\\s]?[\\[]?[\\d]+[\\]]?([\\s]?[\\–\\-\\—]?[:]?[\\s]?[\\[]?[\\d]+[\\]]?)*|x[\\s]?=[\\s]?[\\[]?[\\d]+[\\]]?([\\s]?[\\–\\-\\—]?[:]?[\\s]?[\\[]?[\\d]+[\\]]?)*");
                    	Matcher matcher1 = pattern8.matcher(str1);
                    	if ( matcher1.find()){
                    		str2=str2.concat("<structure name=\"chromosome_count\" id=\"o"+(ct+1)+"\">");
                    		str4="</structure>";
                    	}
                    	else{
                		str2=str2.concat("<structure name=\"org\" id=\"o"+(ct+1)+"\">");
                		str4="</structure>";
                    	}
                    	oldorg="<org>";
                	}
                	matcher.reset();
                	str2 = str2.concat(CharStateHandler.characterstate(str1, str));
                	
                	Pattern pattern13 = Pattern.compile("_");
                    // Replace all occurrences of pattern in input
                    Matcher matcher2 = pattern13.matcher(str6);
                    str6 = matcher2.replaceAll(" ");
                    matcher2.reset();
                    Pattern pattern14 = Pattern.compile(":");
                    // Replace all occurrences of pattern in input
                    matcher2 = pattern14.matcher(str6);
                    str6 = matcher2.replaceAll(",");
                    matcher2.reset();
                    if(flag8 == 1){
                    	matcher2 = pattern14.matcher(chrtag);
                        chrtag = matcher2.replaceAll(",");
                        matcher2.reset();
                        str2=str2.concat(str4+"<Text>"+str6+"</Text>"+chrtag+"</statement>");
                    }
                    else
                    	str2=str2.concat(str4+"<Text>"+str6+"</Text></statement>");
                    
                   /* StringBuffer sb = new StringBuffer();
                    Pattern pattern17 = Pattern.compile("<[a-zA-Z_]+><[a-zA-Z_]+");
                	matcher2 = pattern17.matcher(str2);
					while ( matcher2.find()){
						int k=matcher2.start();
						int l=matcher2.end();
						matcher2.appendReplacement(sb, str2.subSequence(str2.indexOf('<', str2.indexOf('>')),l)+" modifier=\""+str2.subSequence(k+1,str2.indexOf('>'))+"\"");
					}
					matcher2.appendTail(sb);
					str2=sb.toString();
					matcher2.reset();*/
                    
                	stmt2.execute("insert into marked_simpleseg values('"+rs.getString(1)+"','"+rs.getString(2)+"','"+str2+"')");
                	matcher.reset();
                }
                
                
                else{
                	str = reversecondense(str);
	            	String str6=plaintextextractor(str);
	            	orcount = 0;
	                // Replace all occurrences of pattern in input
	                matcher = pattern.matcher(str);
	                while ( matcher.find()){
	                	//System.out.println("Organ:"+str.substring(matcher.start(),matcher.end()));
	                	orcount+=1;
	                }
	                matcher.reset();
	            	String str2 = "";
	            	String org1 = "";
	            	String organ1 = "";
	            	String org2 = "";
	            	String organ2 = "";
	            	String innertagstate = "";
	            	String relation = "";
	            	String plainrelation = "";
	            	String markedrelations = "";
	            	String negation = "";
	            	String outertag = "";
	            	String innertags = "";
	            	String markedsent = "";
	            	String charset = "";
	            	//System.out.println("str:"+str);
	            	Pattern pattern4 = Pattern.compile("(<[a-zA-Z_ ]+>)[\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×\\s,µ%\\*\\{\\}\\[\\]=(<\\{)(\\}>) m]+(<[a-zA-Z_ ]+>)");
	        		matcher = pattern4.matcher(str);
	        		while ( matcher.find()){
	        			int i=matcher.start();
	            		int j=matcher.end();
	            		str2=str.subSequence(i,j).toString();
	            		//System.out.println("str2:"+str2);
	            		int m=str2.indexOf("<");
	            		int k=str2.indexOf(">");
	            		int l;
	            		int ct=1;
	            		while(ct<orcount){
	            			if(str2.charAt(m+1)!='{'){
	            				org1=str2.substring(m+1,k);
	            				if(ct==1)
	            					organ1 = org1;
	            				ResultSet rs2 = stmt3.executeQuery("select * from singularplural where plural='"+org1+"'");
                				while(rs2.next()){
                					org1 = rs2.getString(1);
                				}
	            			}
	            			if(org1.compareTo("n")==0)
	            				org1 = "chromosome_count";
	            			l=str2.indexOf("<",k);
	            			if(str2.charAt(l+1)!='{'){
	            				org2=str2.substring(l+1,str2.indexOf(">",l));
	            				organ2 = org2;
	            				ResultSet rs2 = stmt3.executeQuery("select * from singularplural where plural='"+org2+"'");
                				while(rs2.next()){
                					org2 = rs2.getString(1);
                				}
	            			}
	            			if(org2.compareTo("n")==0)
	            				org2 = "chromosome_count";
	            			relation=str2.substring(k+2,l);
	            			m=l;
	            			k=str2.indexOf(">",l);
	            			innertagstate = "";
	            			//System.out.println("relation:"+relation);
	            			if(relation.contains(":")){
	            				charset = relation.substring(0, relation.lastIndexOf("{:}"));
	            				relation = relation.substring(relation.lastIndexOf("{:}")+3);
	            				out.println(rs.getString(2)+"  "+relation);
	            				String plaincharset = "";
	            				plaincharset = plaintextextractornum(charset);
	            				innertagstate = CharStateHandler.characterstate(plaincharset, charset);
	            				Pattern pattern5 = Pattern.compile("[a-zA-Z]+");
	                    		Matcher matcher1 = pattern5.matcher(relation);
	                    		int flag=0;
	                    		while ( matcher1.find()){
	                        		i=matcher1.start();
	                        		j=matcher1.end();
	                        		ResultSet rs1=stmt1.executeQuery("Select * from negation where term='"+relation.subSequence(i,j).toString()+"'");
	                				while(rs1.next()){
	                					flag=1;
	                				}
	                    		}
	                    		matcher1.reset();
	                    		if(flag==1)
	                    			negation = "yes";
	                    		else
	                    			negation = "no";
	                    		//plainrelation = ps.plaintextextractor(relation);
	                    		Pattern pattern3 = Pattern.compile("[\\w±\\+\\–\\-\\—°²\\.:=/\\s½\"¼;x´\\×µ%“”\\_,]+");
	                    		matcher1 = pattern3.matcher(relation);
	                    		while ( matcher1.find()){
	                        		i=matcher1.start();
	                        		j=matcher1.end();
	                        		plainrelation=plainrelation.concat(relation.subSequence(i,j).toString());
	                        	}
	                        	matcher1.reset();
	                        	Pattern pattern10 = Pattern.compile("_");
	                            // Replace all occurrences of pattern in input
	                            Matcher matcher2 = pattern10.matcher(plainrelation);
	                            plainrelation = matcher2.replaceAll(" ");
	                            matcher2.reset();
	                            Pattern pattern11 = Pattern.compile(":");
	                            // Replace all occurrences of pattern in input
	                            matcher2 = pattern11.matcher(plainrelation);
	                            plainrelation = matcher2.replaceAll(",");
	                            matcher2.reset();
	                        	if(ct==1){
	                        		outertag=outertag.concat("<statement id=\"s"+rs.getString(1)+"\">");
	                        	/*else
	                        		outertag=outertag.concat("_R"+ct+"_"+org2);*/
	                        		if(innertagstate!="")
	                        			innertags=innertags.concat("<structure name=\""+org1+"\" id=\"o"+ct+"\">"+innertagstate+"</structure><structure name=\""+org2+"\" id=\"o"+(ct+1)+"\"></structure><relation id=\"R"+ct+"\" from=\"o"+ct+"\" to=\"o"+(ct+1)+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
	                        		else	
	                        			innertags=innertags.concat("<structure name=\""+org1+"\" id=\"o"+ct+"\"></structure><structure name=\""+org2+"\" id=\"o"+(ct+1)+"\"></structure><relation id=\"R"+ct+"\" from=\"o"+ct+"\" to=\"o"+(ct+1)+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
	                        	}
	                        	else{
	                        		if(innertagstate!=""){
	                        			StringBuffer sb = new StringBuffer();
	        	                		Pattern pattern23 = Pattern.compile("<structure name=\""+org1+"\" id=\"o"+ct+"\">");
	        	                		matcher2 = pattern23.matcher(innertags);
	        	                		if ( matcher2.find()){
	        	                			int p=matcher2.start();
	        	                			int q=matcher2.end();
	        	                			matcher2.appendReplacement(sb, innertags.subSequence(p,q)+innertagstate);
	        	                		}
	        	                		matcher2.appendTail(sb);
	        	                		innertags=sb.toString();
	        	                		matcher2.reset();
	                        		}
		                        	innertags=innertags.concat("<structure name=\""+org2+"\" id=\"o"+(ct+1)+"\"></structure><relation id=\"R"+ct+"\" from=\"o"+ct+"\" to=\"o"+(ct+1)+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
	                        	}
	                        	markedrelations = markedrelations.concat("<relation id=\"R"+ct+"\" from=\"o"+ct+"\" to=\"o"+(ct+1)+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
	            			}
	            			else{
	            				out.println(rs.getString(2)+"  "+relation);
	            				Pattern pattern5 = Pattern.compile("[a-zA-Z]+");
	                    		Matcher matcher1 = pattern5.matcher(relation);
	                    		int flag=0;
	                    		while ( matcher1.find()){
	                        		i=matcher1.start();
	                        		j=matcher1.end();
	                        		ResultSet rs1=stmt1.executeQuery("Select * from negation where term='"+relation.subSequence(i,j).toString()+"'");
	                				while(rs1.next()){
	                					flag=1;
	                				}
	                    		}
	                    		matcher1.reset();
	                    		if(flag==1)
	                    			negation = "yes";
	                    		else
	                    			negation = "no";
	                    		//plainrelation = ps.plaintextextractor(relation);
	                    		Pattern pattern3 = Pattern.compile("[\\w±\\+\\–\\-\\—°²\\.:=/\\s½\"¼;x´\\×µ%“”\\_,]+");
	                    		matcher1 = pattern3.matcher(relation);
	                        	while ( matcher1.find()){
	                        		i=matcher1.start();
	                        		j=matcher1.end();
	                        		plainrelation=plainrelation.concat(relation.subSequence(i,j).toString());
	                        	}
	                        	matcher1.reset();
	                        	Pattern pattern10 = Pattern.compile("_");
	                            // Replace all occurrences of pattern in input
	                            Matcher matcher2 = pattern10.matcher(plainrelation);
	                            plainrelation = matcher2.replaceAll(" ");
	                            matcher2.reset();
	                            Pattern pattern11 = Pattern.compile(":");
	                            // Replace all occurrences of pattern in input
	                            matcher2 = pattern11.matcher(plainrelation);
	                            plainrelation = matcher2.replaceAll(",");
	                            matcher2.reset();
	                        	if(ct==1){
	                        		outertag=outertag.concat("<statement id=\"s"+rs.getString(1)+"\">");
	                        		innertags=innertags.concat("<structure name=\""+org1+"\" id=\"o"+ct+"\"></structure><structure name=\""+org2+"\" id=\"o"+(ct+1)+"\"></structure><relation id=\"R"+ct+"\" from=\"o"+ct+"\" to=\"o"+(ct+1)+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
	                        	}
	                        	else
	                        		innertags=innertags.concat("<structure name=\""+org2+"\" id=\"o"+(ct+1)+"\"></structure><relation id=\"R"+ct+"\" from=\"o"+ct+"\" to=\"o"+(ct+1)+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
	                        	/*else
	                        		outertag=outertag.concat("_R"+ct+"_"+org2);*/
	                        	markedrelations = markedrelations.concat("<relation id=\"R"+ct+"\" from=\"o"+ct+"\" to=\"o"+(ct+1)+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
	            			}
	            			ct++;
	            		}
	            			            		
	            //handle states for first organ in segment
	            		innertagstate = "";
	            		Pattern pattern6 = Pattern.compile("[\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×\\s,µ%\\*\\{\\}\\[\\](<\\{)(\\}>)m]+<"+organ1+">");
	                	Matcher matcher1 = pattern6.matcher(str);
	                	String state = "";
	                	while ( matcher1.find()){
	                		i=matcher1.start();
	                		j=matcher1.end();
	                		state=state.concat(str.subSequence(i,j).toString());
	                		String plaincharset = "";
	                		plaincharset = plaintextextractornum(state);
	        				innertagstate = CharStateHandler.characterstate(plaincharset, state);
	                	}
	                	matcher1.reset();
	                	if(innertagstate!=""){
	                		StringBuffer sb = new StringBuffer();
	                		if(organ1.compareTo("n")==0)
	                			organ1 = "chromosome_count";
	                		Pattern pattern9 = Pattern.compile("<structure name=\""+organ1+"\" id=\"o1\">");
	                		Matcher matcher2 = pattern9.matcher(innertags);
	                		if ( matcher2.find()){
	                			int p=matcher2.start();
	                			int q=matcher2.end();
	                			matcher2.appendReplacement(sb, innertags.subSequence(p,q)+innertagstate);
	                		}
	                		matcher2.appendTail(sb);
	                		innertags=sb.toString();
	                		matcher2.reset();
	                	}
	                	
	             //handle states for last organ in segment
	                	innertagstate = "";
	                	Pattern pattern10 = Pattern.compile("<"+organ2+">[\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×\\s,=µ%\\*\\{\\}\\[\\](<\\{)(\\}>)m]+");
	                	matcher1 = pattern10.matcher(str);
	                	state = "";
	                	while ( matcher1.find()){
	                		i=matcher1.start();
	                		j=matcher1.end();
	                		state=state.concat(str.subSequence(i,j).toString());
	                		String plaincharset = "";
	                		plaincharset = plaintextextractornum(state);
	                		innertagstate = CharStateHandler.characterstate(plaincharset, state);
	                	}
	                	matcher1.reset();
	                	if(innertagstate!=""){
	                		StringBuffer sb = new StringBuffer();
	                		Pattern pattern9 = Pattern.compile("<structure name=\""+org2+"\" id=\"o"+ct+"\">");
	                		Matcher matcher2 = pattern9.matcher(innertags);
	                		while ( matcher2.find()){
	                			int p=matcher2.start();
	                			int q=matcher2.end();
	                			matcher2.appendReplacement(sb, innertags.subSequence(p,q)+innertagstate);
	                		}
	                		matcher2.appendTail(sb);
	                		innertags=sb.toString();
	                		matcher2.reset();
	                	}
	                }
	        		//System.out.println("str6:"+str6);
	        		Pattern pattern10 = Pattern.compile("_");
	                // Replace all occurrences of pattern in input
	                Matcher matcher2 = pattern10.matcher(str6);
	                str6 = matcher2.replaceAll(" ");
	                matcher2.reset();
	                Pattern pattern11 = Pattern.compile(":");
	                // Replace all occurrences of pattern in input
	                matcher2 = pattern11.matcher(str6);
	                str6 = matcher2.replaceAll(",");
	                matcher2.reset();
	        		innertags = innertags.concat("<Text>"+str6+"</Text>");
	        		markedsent = markedsent.concat(outertag+innertags+"</statement>");
	        		//stmt1.execute("insert into marked_complexseg values('"+rs.getString(1)+"','"+rs.getString(2)+"','"+markedsent+"','"+markedrelations+"')");
	        		stmt1.execute("insert into marked_simpleseg values('"+rs.getString(1)+"','"+rs.getString(2)+"','"+markedsent+"')");
                }
                oldsrcstr=rs.getString(2);
            }
        	
		}
        catch (Exception e)
        {
        		System.err.println(e);
        }
	}
	
	protected String reversecondense(String str) { 
		StringBuffer sb2 = new StringBuffer();
    	Pattern pattern12 = Pattern.compile("<[a-zA-Z_ ]+>");
    	Matcher matcher = pattern12.matcher(str);
    	while ( matcher.find()){
    		int k=matcher.start()+1;
			int l=matcher.end()-1;
			String org=str.subSequence(k,l).toString();
			if(org.contains("_has_")){
				String org1=org.subSequence(0,org.indexOf("_")).toString();
				String org2=org.subSequence(org.lastIndexOf("_")+1,org.length()).toString();
				matcher.appendReplacement(sb2, "<"+org1+"> <"+org2+">");
			}
    	}
    	matcher.appendTail(sb2);
		str=sb2.toString();
		matcher.reset();
    	StringBuffer sb1 = new StringBuffer();
		Pattern pattern11 = Pattern.compile("[{][\\w±\\+\\–\\-\\.:=/\\_]+[}]");
		matcher = pattern11.matcher(str);
		while ( matcher.find()){
			int k=matcher.start();
			int l=matcher.end();
			String state=str.subSequence(k,l).toString();
			Pattern pattern13 = Pattern.compile("_");
			Matcher matcher1 = pattern13.matcher(state);
			state = matcher1.replaceAll("} or {");
			matcher1.reset();
			matcher.appendReplacement(sb1, state);
		}
		matcher.appendTail(sb1);
		str=sb1.toString();
		matcher.reset();
		return(str);
	}
	
	protected String plaintextextractornum(String str) {
		String str1 = "";
		Pattern pattern1 = Pattern.compile("[\\w±\\+\\[\\]\\–\\-\\—°²\\.:=/\\s½\"¼;x´\\×\\*µ%“”\\_,]+");
    	Matcher matcher = pattern1.matcher(str);
    	while ( matcher.find()){
    		int i=matcher.start();
    		int j=matcher.end();
    		str1=str1.concat(str.subSequence(i,j).toString());
    	}
    	matcher.reset();
    	return(str1);
	}
	
	protected String plaintextextractor(String str) {
		String str1 = "";
		Pattern pattern1 = Pattern.compile("[\\w±\\+\\–\\-\\—°²\\.:=/\\s½\"¼;x´\\×\\*µ%“”\\_,]+");
    	Matcher matcher = pattern1.matcher(str);
    	while ( matcher.find()){
    		int i=matcher.start();
    		int j=matcher.end();
    		str1=str1.concat(str.subSequence(i,j).toString());
    	}
    	matcher.reset();
    	return(str1);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new ParseSimpleseg("benchmark_learningcurve_fnav19_test_24");
	}

}
