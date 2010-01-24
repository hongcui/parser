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
                	StringBuffer sb2 = new StringBuffer();
                	Pattern pattern12 = Pattern.compile("<[a-zA-Z_ ]+>");
                	matcher = pattern12.matcher(str);
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
						int k=matcher.start()+1;
						int l=matcher.end()-1;
						String state=str.subSequence(k,l).toString();
						if(state.contains("_")){
							String firstr=state.subSequence(0,state.indexOf("_")).toString();
							String secstr=state.subSequence(state.indexOf("_")+1,state.length()).toString();
							matcher.appendReplacement(sb1, "{"+firstr+"} or {"+secstr+"}");
						}
					}
					matcher.appendTail(sb1);
					str=sb1.toString();
					matcher.reset();
                	String str1="";
                	Pattern pattern1 = Pattern.compile("[\\w±\\+\\–\\-\\—°²\\.:=/\\s½\"¼;x´\\×\\*µ%“”\\_,]+");
                	matcher = pattern1.matcher(str);
                	while ( matcher.find()){
                		int i=matcher.start();
                		int j=matcher.end();
                		str1=str1.concat(str.subSequence(i,j).toString());
                	}
                	matcher.reset();
                	String str6=str1;
                	String str2="";
                	String str4="";
                	String chrtag="";
                	int flag6 = 0;
                	int flag8 = 0;
                	int ct = 0;
                	Pattern pattern2 = Pattern.compile("<[a-zA-Z_ ]+>");
                	matcher = pattern2.matcher(str);
                	while ( matcher.find()){
                		ct++;
                		int i=matcher.start();
                		int j=matcher.end();
                		if(ct==1 && srcflag==0)
                			oldorg=str.subSequence(i,j).toString();
                		else if(ct==1 && srcflag==1){
                			Pattern pattern16 = Pattern.compile("((?<![\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×,µ%\\*\\{\\}\\[\\](<\\{)(\\}>) m]\\s)<[a-zA-Z_]+>(\\s<[a-zA-Z_]+>)*[\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×\\s,µ%\\*\\{\\}\\[\\](<\\{)(\\}>) m]*)");
                            
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
                					//str2=str2.concat(">"+str.subSequence(i,j-1).toString());
                					str2=str2.concat("><"+singorg);
                					//str4="</"+str.subSequence(i+1,j).toString()+str4;
                					str4="</"+singorg+">"+str4;
                				}
                				else{
                					//str2=str2.concat(str.subSequence(i,j-1).toString());
                					str2=str2.concat("<"+singorg);
                					//str4="</"+str.subSequence(i+1,j).toString();
                					str4="</"+singorg+">";
                				}
                				Pattern pattern7 = Pattern.compile("[\\d]?[\\s]?n[\\s]?=[\\s]?[\\d]+([\\s]?[\\–\\-\\—]?[:]?[\\s]?[\\d]+)*|x[\\s]?=[\\s]?[\\d]+([\\s]?[\\–\\-\\—]?[:]?[\\s]?[\\d]+)*");
                				Matcher matcher1 = pattern7.matcher(str1);
                				if ( matcher1.find()){
                					int o=matcher1.start();
                					int p=matcher1.end();
                					String chr = str1.substring(o,p);
                					chrtag = "<chromosome_count";
                					Pattern pattern15 = Pattern.compile("(?<!([/]))([±]?[\\d]+[\\–\\-][\\d]+[+]?|[±]?[\\d]+[+]?)(?!([\\s]?[n/]))");
                                	Matcher matcher2 = pattern15.matcher(chr);
                                	int flag7=0;
                                	while ( matcher2.find()){
                                		int q=matcher2.start();
                                		int r=matcher2.end();
                                		if(flag7==0)
                                			chrtag=chrtag.concat(" "+"count=\""+chr.subSequence(q,r).toString());
                                		else
                                			chrtag=chrtag.concat(","+chr.subSequence(q,r).toString());
                                		flag7=1;
                                	}
                                	if(flag7==1)
                                		chrtag=chrtag.concat("\"");
                                	chrtag=chrtag.concat(">"+chr+"</chromosome_count>");
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
                				if(ct>1){
                					str2=str2.concat("><chromosome_count");
                					str4="</chromosome_count>"+str4;
                				}
                				else{
                				str2=str2.concat("<chromosome_count");
                				str4="</chromosome_count>";
                				}
                				//str1="";
                			}
                		flag6=1;
                	}
                	if(flag6 == 0){
                		Pattern pattern8 = Pattern.compile("[\\d]?[\\s]?n[\\s]?=[\\s]?[\\d]+([\\s]?[\\–\\-\\—]?[:]?[\\s]?[\\d]+)*|x[\\s]?=[\\s]?[\\d]+([\\s]?[\\–\\-\\—]?[:]?[\\s]?[\\d]+)*");
                    	Matcher matcher1 = pattern8.matcher(str1);
                    	if ( matcher1.find()){
                    		str2=str2.concat("<chromosome_count");
                    		str4="</chromosome_count>";
                    	}
                    	else{
                		str2=str2.concat("<org");
                		str4="</org>";
                    	}
                    	oldorg="<org>";
                	}
                	matcher.reset();
                	Pattern pattern4 = Pattern.compile("[xX\\×±\\d\\–\\-\\.\\s\\+]+[\\s]?[dcmµ]?m(?![\\w])(([\\s]diam)?([\\s]wide)?)");
                	matcher = pattern4.matcher(str1);
                	int flag=0;
                	while ( matcher.find()){
                		int i;
                		if(str1.charAt(matcher.start())==' '){
                			i=matcher.start()+1;
                		}
                		else{
                			i=matcher.start();
                		}
                		int j=matcher.end();
                		if(flag==0)
                			str2=str2.concat(" "+"size=\""+str1.subSequence(i,j).toString());
                		else
                			str2=str2.concat(","+str1.subSequence(i,j).toString());
                		flag=1;
                	}
                	if(flag==1)
                		str2=str2.concat("\"");
                	str1 = matcher.replaceAll("#");
                	matcher.reset();
                	Pattern pattern5 = Pattern.compile("[±\\d\\–\\-\\./\\s]+[\\s]?[\\–\\-]?(% of [\\w]+ length|height of [\\w]+|times as [\\w]+ as [\\w]+|total length|their length|(times)?[\\s]?length of [\\w]+)");
                	matcher = pattern5.matcher(str1);
                	int flag1=0;
                	while ( matcher.find()){
                		int i;
                		if(str1.charAt(matcher.start())==' '){
                			i=matcher.start()+1;
                		}
                		else{
                			i=matcher.start();
                		}
                		int j=matcher.end();
                		if(flag1==0)
                			str2=str2.concat(" "+"size=\""+str1.subSequence(i,j).toString());
                		else
                			str2=str2.concat(","+str1.subSequence(i,j).toString());
                		flag1=1;
                	}
                	if(flag1==1)
                		str2=str2.concat("\"");
                	str1 = matcher.replaceAll("#");
                	matcher.reset();
                	Pattern pattern10 = Pattern.compile("([±]?[\\d]+[\\–\\-][\\d]+[+]?|[±]?[\\d]+[+]?)[\\–\\–\\-]+[a-zA-Z]+");
                	matcher = pattern10.matcher(str1);
                	str1 = matcher.replaceAll("#");
                	matcher.reset();     	
                	Pattern pattern6 = Pattern.compile("(?<!([/]))([±]?[\\d]+[\\–\\-][\\d]+[+]?|[±]?[\\d]+[+]?)(?!([\\s]?[n/]|[\\s]?[\\–\\-]?% of [\\w]+ length|[\\s]?[\\–\\-]?height of [\\w]+|[\\s]?[\\–\\-]?times|[\\s]?[\\–\\-]?total length|[\\s]?[\\–\\-]?their length|[\\s]?[\\–\\-]?(times)?[\\s]?length of|[\\s]?[cm]?m))");
                	matcher = pattern6.matcher(str1);
                	int flag2=0;
                	while ( matcher.find()){
                		int i=matcher.start();
                		int j=matcher.end();
                		if(flag2==0)
                			str2=str2.concat(" "+"count=\""+str1.subSequence(i,j).toString());
                		else
                			str2=str2.concat(","+str1.subSequence(i,j).toString());
                		flag2=1;
                	}
                	if(flag2==1)
                		str2=str2.concat("\"");
                	matcher.reset();
                	String str3="";
                	Pattern pattern3 = Pattern.compile("[{][\\w±\\+\\–\\-\\.:=/\\_]+[}]");
                	matcher = pattern3.matcher(str);
                	int flag3=0;
                	while ( matcher.find()){
                		int flag5=0;
                		String first = "";
                		String chstate = "";
                		int i=matcher.start()+1;
                		int j=matcher.end()-1;
                		str3=str.subSequence(i,j).toString();
                		if(str3.contains("-")|str3.contains("–")){
                			first = str3.substring(0, str3.indexOf("-"));
                			str3=str3.substring(str3.indexOf("-")+1|str3.indexOf("–")+1, str3.length());
                			flag5=1;
                		}
                		/*if(flag3==0){
                			ResultSet rs1 = stmt1.executeQuery("select * from character_markup_ontology where term='"+str3+"'");
                			if(rs1.next()){
                				if(flag5==1)
                					str2=str2.concat(" "+rs1.getString(4)+"=\""+first+"-"+str3+"\"");
                				else
                					str2=str2.concat(" "+rs1.getString(4)+"=\""+str3+"\"");
                				flag3=1;
                			}
                		}*/
                		//else{
                			ResultSet rs1 = stmt1.executeQuery("select * from character_markup_ontology where term='"+str3+"'");
                			if(rs1.next()){
                				int flag4=0;
                				chstate=rs1.getString(4);
                				if(chstate.contains("/")){
                					String [] terms = chstate.split("/");
                					chstate=terms[0];
                        			for(int m=1;m<terms.length;m++)
                        				chstate=chstate.concat("_or_"+terms[m]);  
                					System.out.println(chstate);
                				}
                				StringBuffer sb = new StringBuffer();
            					Pattern pattern9 = Pattern.compile(chstate+"=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,xX\\×]+\"");
            					Matcher matcher1 = pattern9.matcher(str2);
            					while ( matcher1.find()){
            						int k=matcher1.start();
            						int l=matcher1.end();
            						if(flag5==1)
            							matcher1.appendReplacement(sb, str2.subSequence(k,l-1)+","+first+"-"+str3+"\"");
                    				else
                    					matcher1.appendReplacement(sb, str2.subSequence(k,l-1)+","+str3+"\"");
            						flag4=1;
            					}
            					if(flag4==1){
            						matcher1.appendTail(sb);
            						str2=sb.toString();
            					}
            					else{
            						if(flag5==1)
            							str2=str2.concat(" "+chstate+"=\""+first+"-"+str3+"\"");
            						else
            							str2=str2.concat(" "+chstate+"=\""+str3+"\"");
            					}
            					matcher1.reset();
                			}                			
                		//}
                		
                	}
                	matcher.reset();
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
                        str2=str2.concat(">"+str6+str4+chrtag);
                    }
                    else
                    	str2=str2.concat(">"+str6+str4);
                	stmt2.execute("insert into marked_simpleseg values('"+rs.getString(1)+"','"+rs.getString(2)+"','"+str2+"')");
                	matcher.reset();
                }
                else{
                	StringBuffer sb2 = new StringBuffer();
                	Pattern pattern12 = Pattern.compile("<[a-zA-Z_ ]+>");
                	matcher = pattern12.matcher(str);
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
						int k=matcher.start()+1;
						int l=matcher.end()-1;
						String state=str.subSequence(k,l).toString();
						if(state.contains("_")){
							String firstr=state.subSequence(0,state.indexOf("_")).toString();
							String secstr=state.subSequence(state.indexOf("_")+1,state.length()).toString();
							matcher.appendReplacement(sb1, "{"+firstr+"} or {"+secstr+"}");
						}
					}
					matcher.appendTail(sb1);
					str=sb1.toString();
					matcher.reset();
					
					if(srcflag==0){
                		Pattern pattern2 = Pattern.compile("<[a-zA-Z_ ]+>");
                    	matcher = pattern2.matcher(str);
                    	if ( matcher.find()){
                    		int i=matcher.start();
                    		int j=matcher.end();
                   			oldorg=str.subSequence(i,j).toString();
                    	}
                	}
                	else{
                		Pattern pattern3 = Pattern.compile("((?<![\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×,µ%\\*\\{\\}\\[\\](<\\{)(\\}>) m]\\s)"+oldorg+"(\\s<[a-zA-Z_]+>)*[\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×\\s,µ%\\*\\{\\}\\[\\](<\\{)(\\}>) m]*)");
                        matcher = pattern3.matcher(str);
                        if ( matcher.find()){
                        	str=str.substring(str.indexOf(">")+1);
                        }
                	}
					
                	String str1="";
                	Pattern pattern1 = Pattern.compile("[\\w±\\+\\–\\-\\—°²\\.:=/\\s½\"¼;x´\\×\\*µ%“”\\_,]+");
                	matcher = pattern1.matcher(str);
                	while ( matcher.find()){
                		int i=matcher.start();
                		int j=matcher.end();
                		str1=str1.concat(str.subSequence(i,j).toString());
                	}
                	matcher.reset();
                	String str6=str1;
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
                	
                	stmt1.execute("insert into marked_simpleseg values('"+rs.getString(1)+"','"+rs.getString(2)+"','"+str6+"')");
                }
                oldsrcstr=rs.getString(2);
            }
        	
		}
        catch (Exception e)
        {
        		System.err.println(e);
        }
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new ParseSimpleseg("fnav19_benchmark");
	}

}
