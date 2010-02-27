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
	int flag6;
	
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
                					str2=str2.concat("><"+singorg);
                					str4="</"+singorg+">"+str4;
                				}
                				else{
                					str2=str2.concat("<"+singorg);
                					str4="</"+singorg+">";
                				}
                				Pattern pattern7 = Pattern.compile("[\\d]?[\\s]?n[\\s]?=[\\s]?[\\[]?[\\d]+[\\]]?([\\s]?[\\–\\-\\—]?[:]?[\\s]?[\\[]?[\\d]+[\\]]?)*|x[\\s]?=[\\s]?[\\[]?[\\d]+[\\]]?([\\s]?[\\–\\-\\—]?[:]?[\\s]?[\\[]?[\\d]+[\\]]?)*");
                				Matcher matcher1 = pattern7.matcher(str1);
                				if ( matcher1.find()){
                					int o=matcher1.start();
                					int p=matcher1.end();
                					String chr = str1.substring(o,p);
                					chrtag = "<chromosome_count";
                					Pattern pattern15 = Pattern.compile("(?<!([/][\\s]?))([±]?[\\d]+[\\–\\-][\\d]+[+]?|[±]?[\\d]+[+]?)(?!([\\s]?[n/]))");
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
                		Pattern pattern8 = Pattern.compile("[\\d]?[\\s]?n[\\s]?=[\\s]?[\\[]?[\\d]+[\\]]?([\\s]?[\\–\\-\\—]?[:]?[\\s]?[\\[]?[\\d]+[\\]]?)*|x[\\s]?=[\\s]?[\\[]?[\\d]+[\\]]?([\\s]?[\\–\\-\\—]?[:]?[\\s]?[\\[]?[\\d]+[\\]]?)*");
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
                	
                	
                	Pattern pattern19 = Pattern.compile("[±]?[\\[]?[\\d\\s\\.]+[\\]]?[\\[]?[\\–\\-]+[\\]]?[\\[]?[\\d\\s\\.]+[\\]]?[dcmµ]?[m]?[\\s]?[xX\\×]+[\\[]?[\\d\\s\\.]+[\\]]?[\\[]?[\\–\\-]+[\\]]?[\\[]?[\\d\\s\\.]+[\\]]?[dcmµ]?m");
                	matcher = pattern19.matcher(str1);
                	int flag3=0;
                	while ( matcher.find()){
                		int i;
                		if(str1.charAt(matcher.start())==' '){
                			i=matcher.start()+1;
                		}
                		else{
                			i=matcher.start();
                		}
                		int j=matcher.end();
                		if(flag3==0)
            				str2=str2.concat(" "+"size=\""+str1.subSequence(i,j).toString());
            			else
            				str2=str2.concat(","+str1.subSequence(i,j).toString());
            			flag3=1;
                	}
                	if(flag3==1)
                		str2=str2.concat("\"");
                	str1 = matcher.replaceAll("#");
                	matcher.reset();
                	
                	int sizect = 0;
                	Pattern pattern4 = Pattern.compile("[xX\\×±\\d\\[\\]\\–\\-\\.\\s\\+]+[\\s]?[dcmµ]?m(?![\\w])(([\\s]diam)?([\\s]wide)?)");
                	matcher = pattern4.matcher(str1);
                	int flag=0;
                	String numrange="";
                	while ( matcher.find()){
                		int i;
                		if(str1.charAt(matcher.start())==' '){
                			i=matcher.start()+1;
                		}
                		else{
                			i=matcher.start();
                		}
                		int j=matcher.end();
                		String extreme = str1.substring(i,j);
            			i = 0;
            			j = extreme.length();
                		Pattern pattern20 = Pattern.compile("\\[[±\\d\\.\\s\\+]+[\\–\\-]{1}[±\\d\\.\\s\\+\\–\\-]*\\]");
                    	Matcher matcher1 = pattern20.matcher(extreme);
                    	if ( matcher1.find()){
                    		int k = matcher1.start();
                    		int l = matcher1.end();
                    		if(extreme.charAt(l-2)=='–' | extreme.charAt(l-2)=='-')
                    			numrange = numrange.concat(" min_extreme_size_"+sizect+"=\""+extreme.substring(k+1,l-2)+"\"");
                    		else
                    			numrange = numrange.concat(" min_extreme_size_"+sizect+"=\""+extreme.substring(k+1,l-1)+"\"");
                    	}
                    	extreme = matcher1.replaceAll("#");
                		matcher1.reset();
                		if(extreme.contains("#"))
                			i = extreme.indexOf("#")+1;
                		Pattern pattern21 = Pattern.compile("\\[[±\\d\\.\\s\\+\\–\\-]*[\\–\\-]{1}[±\\d\\.\\s\\+]+\\]");
                    	matcher1 = pattern21.matcher(extreme);
                    	if ( matcher1.find()){
                    		int k = matcher1.start();
                    		int l = matcher1.end();
                    		if(extreme.charAt(k+1)=='–' | extreme.charAt(k+1)=='-')
                    			numrange = numrange.concat(" max_extreme_size_"+sizect+"=\""+extreme.substring(k+2,l-1)+"\"");
                    		else
                    			numrange = numrange.concat(" max_extreme_size_"+sizect+"=\""+extreme.substring(k+1,l-1)+"\"");
                    	}
                    	extreme = matcher1.replaceAll("#");
                		matcher1.reset();
                		j = extreme.length();
                		Pattern pattern23 = Pattern.compile("\\[[±\\d\\.\\s\\+]+\\]");
                    	matcher1 = pattern23.matcher(extreme);
                    	if ( matcher1.find()){
                    		int k = matcher1.start();
                    		int l = matcher1.end();
                    		numrange = numrange.concat(" max_extreme_size_"+sizect+"=\""+extreme.substring(k+1,l-1)+"\"");
                    	}
                    	extreme = matcher1.replaceAll("#");
                    	matcher1.reset();
                    	j = extreme.length();
                		//System.out.println("extreme:"+extreme);
                		if(extreme.substring(i,j).contains("–")|extreme.substring(i,j).contains("-") && !extreme.substring(i,j).contains("×") && !extreme.substring(i,j).contains("x") && !extreme.substring(i,j).contains("X")){
                			String extract = extreme.substring(i,j);
                			Pattern pattern18 = Pattern.compile("[\\s]?[dcmµ]?m(([\\s]diam)?([\\s]wide)?)");
                        	Matcher matcher2 = pattern18.matcher(extract);
                        	String unit="";
                        	if ( matcher2.find()){
                        		unit = extract.substring(matcher2.start(), matcher2.end());
                        	}
                        	extract = matcher2.replaceAll("#");
                        	matcher2.reset();
                			numrange = numrange.concat(" min_size_"+sizect+"=\""+extract.substring(0, extract.indexOf('-'))+"\" min_size_unit_"+sizect+"=\""+unit+"\" max_size_"+sizect+"=\""+extract.substring(extract.indexOf('-')+1,extract.indexOf('#'))+"\" max_size_unit_"+sizect+"=\""+unit+"\"");
                			sizect+=1;
                		}
                		else{
                			if(flag3==1){
                				StringBuffer sb = new StringBuffer();
        						Pattern pattern9 = Pattern.compile("size=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,xX\\×]+\"");
        						matcher1 = pattern9.matcher(str2);
        						while ( matcher1.find()){
        							int k=matcher1.start();
        							int l=matcher1.end();
        							matcher1.appendReplacement(sb, str2.subSequence(k,l-1)+","+extreme.subSequence(i,j).toString()+"\"");
        						}
        						matcher1.appendTail(sb);
        						str2=sb.toString();
        						matcher1.reset();
                			}
                			else{
                				if(flag==0)
                					str2=str2.concat(" "+"size=\""+extreme.subSequence(i,j).toString());
                				else
                					str2=str2.concat(","+extreme.subSequence(i,j).toString());
                				flag=1;
                			}
                		}
                	}
                	if(flag==1)
                		str2=str2.concat("\"");
                	str1 = matcher.replaceAll("#");
                	str2 = str2.concat(numrange);
                	matcher.reset();
                	Pattern pattern5 = Pattern.compile("[±\\d\\[\\]\\–\\-\\./\\s]+[\\s]?[\\–\\-]?(% of [\\w]+ length|height of [\\w]+|times as [\\w]+ as [\\w]+|total length|their length|(times)?[\\s]?length of [\\w]+)");
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
                		if(flag==1){
                			StringBuffer sb = new StringBuffer();
        					Pattern pattern9 = Pattern.compile("size=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,xX\\×]+\"");
        					Matcher matcher1 = pattern9.matcher(str2);
        					while ( matcher1.find()){
        						int k=matcher1.start();
        						int l=matcher1.end();
        						matcher1.appendReplacement(sb, str2.subSequence(k,l-1)+","+str1.subSequence(i,j).toString()+"\"");
        					}
        					matcher1.appendTail(sb);
        					str2=sb.toString();
        					matcher1.reset();
                		}
                		else{
                			if(flag1==0)
                				str2=str2.concat(" "+"size=\""+str1.subSequence(i,j).toString());
                			else
                				str2=str2.concat(","+str1.subSequence(i,j).toString());
                			flag1=1;
                		}
                	}
                	if(flag1==1)
                		str2=str2.concat("\"");
                	str1 = matcher.replaceAll("#");
                	matcher.reset();
                	int countct = 0;
                	Pattern pattern10 = Pattern.compile("([\\[]?[±]?[\\d]+[\\]]?[\\[]?[\\–\\-][\\]]?[\\[]?[\\d]+[+]?[\\]]?|[\\[]?[±]?[\\d]+[+]?[\\]]?)[\\–\\–\\-]+[a-zA-Z]+");
                	matcher = pattern10.matcher(str1);
                	str1 = matcher.replaceAll("#");
                	matcher.reset();     	
                	Pattern pattern6 = Pattern.compile("(?<!([/][\\s]?))([\\[]?[±]?[\\d]+[\\]]?[\\[]?[\\–\\-][\\]]?[\\[]?[\\d]+[+]?[\\]]?([\\[]?[\\–\\-]?[\\]]?[\\[]?[\\d]+[+]?[\\]]?)*|[±]?[\\d]+[+]?)(?!([\\s]?[n/]|[\\s]?[\\–\\-]?% of [\\w]+ length|[\\s]?[\\–\\-]?height of [\\w]+|[\\s]?[\\–\\-]?times|[\\s]?[\\–\\-]?total length|[\\s]?[\\–\\-]?their length|[\\s]?[\\–\\-]?(times)?[\\s]?length of|[\\s]?[dcmµ]?m))");
                	matcher = pattern6.matcher(str1);
                	int flag2=0;
                	String countrange = "";
                	while ( matcher.find()){
                		int i=matcher.start();
                		int j=matcher.end();
                		String extreme = str1.substring(i,j);
            			i = 0;
            			j = extreme.length();
            			//System.out.println("extreme1:"+extreme);
                		Pattern pattern20 = Pattern.compile("\\[[±\\d\\.\\s\\+]+[\\–\\-]{1}[±\\d\\.\\s\\+\\–\\-]*\\]");
                    	Matcher matcher1 = pattern20.matcher(extreme);
                    	if ( matcher1.find()){
                    		int k = matcher1.start();
                    		int l = matcher1.end();
                    		if(extreme.charAt(l-2)=='–' | extreme.charAt(l-2)=='-')
                    			countrange = countrange.concat(" min_extreme_count_"+countct+"=\""+extreme.substring(k+1,l-2)+"\"");
                    		else
                    			countrange = countrange.concat(" min_extreme_count_"+countct+"=\""+extreme.substring(k+1,l-1)+"\"");
                    	}
                    	extreme = matcher1.replaceAll("#");
                		matcher1.reset();
                		if(extreme.contains("#"))
                			i = extreme.indexOf("#")+1;
                		j = extreme.length();
                		Pattern pattern21 = Pattern.compile("\\[[±\\d\\.\\s\\+\\–\\-]*[\\–\\-]{1}[±\\d\\.\\s\\+]+\\]");
                    	matcher1 = pattern21.matcher(extreme);
                    	if ( matcher1.find()){
                    		int k = matcher1.start();
                    		int l = matcher1.end();
                    		j = k;
                    		if(extreme.charAt(k+1)=='–' | extreme.charAt(k+1)=='-')
                    			countrange = countrange.concat(" max_extreme_count_"+countct+"=\""+extreme.substring(k+2,l-1)+"\"");
                    		else
                    			countrange = countrange.concat(" max_extreme_count_"+countct+"=\""+extreme.substring(k+1,l-1)+"\"");
                    	}
                		matcher1.reset();
                		//System.out.println("extreme2:"+extreme.substring(i, j));
                		if(extreme.substring(i,j).contains("–")|extreme.substring(i,j).contains("-") && !extreme.substring(i,j).contains("×") && !extreme.substring(i,j).contains("x") && !extreme.substring(i,j).contains("X")){
                			String extract = extreme.substring(i,j);
                			//System.out.println("extract1:"+extract);
                			Pattern pattern22 = Pattern.compile("[\\[\\]]+");
                			matcher1 = pattern22.matcher(extract);
                			extract = matcher1.replaceAll("");
                			matcher1.reset();
                        	//System.out.println("extract2:"+extract);
                			countrange = countrange.concat(" min_count_"+countct+"=\""+extract.substring(0, extract.indexOf('-'))+"\" max_count_"+countct+"=\""+extract.substring(extract.indexOf('-')+1,extract.length())+"\"");
                			countct+=1;
                		}
                		else{
                			if(flag2==0)
                				str2=str2.concat(" "+"count=\""+extreme.subSequence(i,j).toString());
                			else
                				str2=str2.concat(","+extreme.subSequence(i,j).toString());
                			flag2=1;
                		}
                	}
                	if(flag2==1)
                		str2=str2.concat("\"");
                	str2 = str2.concat(countrange);
                	matcher.reset();
                	String str3="";
                	Pattern pattern3 = Pattern.compile("[{][\\w±\\+\\–\\-\\.:=/\\_]+[}]");
                	matcher = pattern3.matcher(str);
                	//int flag3=0;
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
                			ResultSet rs1 = stmt1.executeQuery("select * from character_markup_ontology where term='"+str3+"'");
                			if(rs1.next()){
                				int flag4=0;
                				chstate=rs1.getString(4);
                				if(chstate.contains("/")){
                					String [] terms = chstate.split("/");
                					chstate=terms[0];
                        			for(int m=1;m<terms.length;m++)
                        				chstate=chstate.concat("_or_"+terms[m]);  
                					//System.out.println(chstate);
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
                    
                    StringBuffer sb = new StringBuffer();
                    Pattern pattern17 = Pattern.compile("<[a-zA-Z_]+><[a-zA-Z_]+");
                	matcher2 = pattern17.matcher(str2);
					while ( matcher2.find()){
						int k=matcher2.start();
						int l=matcher2.end();
						matcher2.appendReplacement(sb, str2.subSequence(str2.indexOf('<', str2.indexOf('>')),l)+" modifier=\""+str2.subSequence(k+1,str2.indexOf('>'))+"\"");
					}
					matcher2.appendTail(sb);
					str2=sb.toString();
					matcher2.reset();
                    
                	stmt2.execute("insert into marked_simpleseg values('"+rs.getString(1)+"','"+rs.getString(2)+"','"+str2+"')");
                	matcher.reset();
                }
                else{
                	str = reversecondense(str);
	            	String str1="";
	            	str1 = plaintextextractornum(str);
	            	String str6=plaintextextractor(str);
	            	orcount = 0;
	            	                    
	                // Replace all occurrences of pattern in input
	                matcher = pattern.matcher(str);
	                while ( matcher.find()){
	                	//System.out.println("Organ:"+str.substring(matcher.start(),matcher.end()));
	                	orcount +=1;
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
	            	String innertags1 = "";
	            	String markedsent = "";
	            	String markedsent1 = "";
	            	String markedsent2 = "";
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
	            			}
	            			if(org1.compareTo("n")==0)
	            				org1 = "chromosome_count";
	            			l=str2.indexOf("<",k);
	            			if(str2.charAt(l+1)!='{'){
	            				org2=str2.substring(l+1,str2.indexOf(">",l));
	            				organ2 = org2;
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
	            				int flag7 = 0;
	            				String plaincharset = "";
	            				plaincharset = plaintextextractornum(charset);
	            				Pattern pattern19 = Pattern.compile("[±]?[\\[]?[\\d\\s\\.]+[\\]]?[\\[]?[\\–\\-]+[\\]]?[\\[]?[\\d\\s\\.]+[\\]]?[dcmµ]?[m]?[\\s]?[xX\\×]+[\\[]?[\\d\\s\\.]+[\\]]?[\\[]?[\\–\\-]+[\\]]?[\\[]?[\\d\\s\\.]+[\\]]?[dcmµ]?m");
	                        	Matcher matcher2 = pattern19.matcher(plaincharset);
	                        	int flag3=0;
	                        	while ( matcher2.find()){
	                        		if(plaincharset.charAt(matcher2.start())==' '){
	                        			i=matcher2.start()+1;
	                        		}
	                        		else{
	                        			i=matcher2.start();
	                        		}
	                        		j=matcher2.end();
	                        		if(flag3==0)
	                    				innertagstate=innertagstate.concat(" "+"size=\""+plaincharset.subSequence(i,j).toString());
	                    			else
	                    				innertagstate=innertagstate.concat(","+plaincharset.subSequence(i,j).toString());
	                    			flag3=1;
	                        	}
	                        	if(flag3==1)
	                        		innertagstate=innertagstate.concat("\"");
	                        	plaincharset = matcher2.replaceAll("#");
	                        	matcher2.reset();
	                        	//System.out.println("plaincharset1:"+plaincharset);
	                        	int sizect = 0;
	            				Pattern pattern13 = Pattern.compile("[xX\\×±\\d\\[\\]\\–\\-\\.\\s\\+]+[\\s]?[dcmµ]?m(?![\\w])(([\\s]diam)?([\\s]wide)?)");
	                        	matcher2 = pattern13.matcher(plaincharset);
	                        	int flag=0;
	                        	String numrange="";
	                        	while ( matcher2.find()){
	                        		flag7 = 1;
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
	                        		Pattern pattern20 = Pattern.compile("\\[[±\\d\\.\\s\\+]+[\\–\\-]{1}[±\\d\\.\\s\\+\\–\\-]*\\]");
	                            	Matcher matcher1 = pattern20.matcher(extreme);
	                            	if ( matcher1.find()){
	                            		int p = matcher1.start();
	                            		int q = matcher1.end();
	                            		if(extreme.charAt(q-2)=='–' | extreme.charAt(q-2)=='-')
	                            			numrange = numrange.concat(" min_extreme_size_"+sizect+"=\""+extreme.substring(p+1,q-2)+"\"");
	                            		else
	                            			numrange = numrange.concat(" min_extreme_size_"+sizect+"=\""+extreme.substring(p+1,q-1)+"\"");
	                            	}
	                            	extreme = matcher1.replaceAll("#");
	                        		matcher1.reset();
	                        		if(extreme.contains("#"))
	                        			i = extreme.indexOf("#")+1;
	                        		Pattern pattern21 = Pattern.compile("\\[[±\\d\\.\\s\\+\\–\\-]*[\\–\\-]{1}[±\\d\\.\\s\\+]+\\]");
	                            	matcher1 = pattern21.matcher(extreme);
	                            	if ( matcher1.find()){
	                            		int p = matcher1.start();
	                            		int q = matcher1.end();
	                            		if(extreme.charAt(p+1)=='–' | extreme.charAt(p+1)=='-')
	                            			numrange = numrange.concat(" max_extreme_size_"+sizect+"=\""+extreme.substring(p+2,q-1)+"\"");
	                            		else
	                            			numrange = numrange.concat(" max_extreme_size_"+sizect+"=\""+extreme.substring(p+1,q-1)+"\"");
	                            	}
	                            	extreme = matcher1.replaceAll("#");
	                        		matcher1.reset();
	                        		j = extreme.length();
	                        		Pattern pattern23 = Pattern.compile("\\[[±\\d\\.\\s\\+]+\\]");
	                            	matcher1 = pattern23.matcher(extreme);
	                            	if ( matcher1.find()){
	                            		int p = matcher1.start();
	                            		int q = matcher1.end();
	                            		numrange = numrange.concat(" max_extreme_size_"+sizect+"=\""+extreme.substring(p+1,q-1)+"\"");
	                            	}
	                            	extreme = matcher1.replaceAll("#");
	                            	matcher1.reset();
	                            	j = extreme.length();
	                        		if(extreme.substring(i,j).contains("–")|extreme.substring(i,j).contains("-") && !extreme.substring(i,j).contains("×") && !extreme.substring(i,j).contains("x") && !extreme.substring(i,j).contains("X")){
	                        			String extract = extreme.substring(i,j);
	                        			Pattern pattern18 = Pattern.compile("[\\s]?[dcmµ]?m(([\\s]diam)?([\\s]wide)?)");
	                                	Matcher matcher3 = pattern18.matcher(extract);
	                                	String unit="";
	                                	if ( matcher3.find()){
	                                		unit = extract.substring(matcher3.start(), matcher3.end());
	                                	}
	                                	extract = matcher3.replaceAll("#");
	                                	matcher3.reset();
	                        			numrange = numrange.concat(" min_size_"+sizect+"=\""+extract.substring(0, extract.indexOf('-'))+"\" min_size_unit_"+sizect+"=\""+unit+"\" max_size_"+sizect+"=\""+extract.substring(extract.indexOf('-')+1,extract.indexOf('#'))+"\" max_size_unit_"+sizect+"=\""+unit+"\"");
	                        			sizect+=1;
	                        		}
	                        		else{
	                        			if(flag3==1){
	                        				StringBuffer sb = new StringBuffer();
	                						Pattern pattern9 = Pattern.compile("size=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,xX\\×]+\"");
	                						matcher1 = pattern9.matcher(innertagstate);
	                						while ( matcher1.find()){
	                							int p=matcher1.start();
	                							int q=matcher1.end();
	                							matcher1.appendReplacement(sb, innertagstate.subSequence(p,q-1)+","+extreme.subSequence(i,j).toString()+"\"");
	                						}
	                						matcher1.appendTail(sb);
	                						innertagstate=sb.toString();
	                						matcher1.reset();
	                        			}
	                        			else{
	                        				if(flag==0)
	                        					innertagstate=innertagstate.concat(" "+"size=\""+extreme.subSequence(i,j).toString());
	                        				else
	                        					innertagstate=innertagstate.concat(","+extreme.subSequence(i,j).toString());
	                        				flag=1;
	                        			}
	                        		}
	                        	}
	                        	if(flag==1)
	                        		innertagstate=innertagstate.concat("\"");
	                        	plaincharset = matcher2.replaceAll("#");
	                        	innertagstate = innertagstate.concat(numrange);
	                        	matcher2.reset();
	                        	//System.out.println("plaincharset2:"+plaincharset);
	                        	Pattern pattern14 = Pattern.compile("[±\\d\\[\\]\\–\\-\\./\\s]+[\\s]?[\\–\\-]?(% of [\\w]+ length|height of [\\w]+|times as [\\w]+ as [\\w]+|total length|their length|(times)?[\\s]?length of [\\w]+)");
	                        	matcher2 = pattern14.matcher(plaincharset);
	                        	int flag1=0;
	                        	while ( matcher2.find()){
	                        		flag7 = 1;
	                        		if(plaincharset.charAt(matcher2.start())==' '){
	                        			i=matcher2.start()+1;
	                        		}
	                        		else{
	                        			i=matcher2.start();
	                        		}
	                        		j=matcher2.end();
	                        		if(flag==1){
	                        			StringBuffer sb = new StringBuffer();
	                					Pattern pattern9 = Pattern.compile("size=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,xX\\×]+\"");
	                					Matcher matcher1 = pattern9.matcher(innertagstate);
	                					while ( matcher1.find()){
	                						int p=matcher1.start();
	                						int q=matcher1.end();
	                						matcher1.appendReplacement(sb, innertagstate.subSequence(p,q-1)+","+plaincharset.subSequence(i,j).toString()+"\"");
	                					}
	                					matcher1.appendTail(sb);
	                					innertagstate=sb.toString();
	                					matcher1.reset();
	                        		}
	                        		else{
	                        			if(flag1==0)
	                        				innertagstate=innertagstate.concat(" "+"size=\""+plaincharset.subSequence(i,j).toString());
	                        			else
	                        				innertagstate=innertagstate.concat(","+plaincharset.subSequence(i,j).toString());
	                        			flag1=1;
	                        		}
	                        	}
	                        	if(flag1==1)
	                        		innertagstate=innertagstate.concat("\"");
	                        	plaincharset = matcher2.replaceAll("#");
	                        	matcher2.reset();
	                        	int countct = 0;
	                        	Pattern pattern15 = Pattern.compile("([\\[]?[±]?[\\d]+[\\]]?[\\[]?[\\–\\-][\\]]?[\\[]?[\\d]+[+]?[\\]]?|[\\[]?[±]?[\\d]+[+]?[\\]]?)[\\–\\–\\-]+[a-zA-Z]+");
	                        	matcher2 = pattern15.matcher(plaincharset);
	                        	plaincharset = matcher2.replaceAll("#");
	                        	matcher2.reset();     	
	                        	Pattern pattern16 = Pattern.compile("(?<!([/][\\s]?))([\\[]?[±]?[\\d]+[\\]]?[\\[]?[\\–\\-][\\]]?[\\[]?[\\d]+[+]?[\\]]?([\\[]?[\\–\\-]?[\\]]?[\\[]?[\\d]+[+]?[\\]]?)*|[±]?[\\d]+[+]?)(?!([\\s]?[n/]|[\\s]?[\\–\\-]?% of [\\w]+ length|[\\s]?[\\–\\-]?height of [\\w]+|[\\s]?[\\–\\-]?times|[\\s]?[\\–\\-]?total length|[\\s]?[\\–\\-]?their length|[\\s]?[\\–\\-]?(times)?[\\s]?length of|[\\s]?[dcmµ]?m))");
	                        	matcher2 = pattern16.matcher(plaincharset);
	                        	int flag2=0;
	                        	String countrange = "";
	                        	while ( matcher2.find()){
	                        		flag7 = 1;
	                        		i=matcher2.start();
	                        		j=matcher2.end();
	                        		String extreme = plaincharset.substring(i,j);
	                    			i = 0;
	                    			j = extreme.length();
	                        		Pattern pattern20 = Pattern.compile("\\[[±\\d\\.\\s\\+]+[\\–\\-]{1}[±\\d\\.\\s\\+\\–\\-]*\\]");
	                            	Matcher matcher1 = pattern20.matcher(extreme);
	                            	if ( matcher1.find()){
	                            		int p = matcher1.start();
	                            		int q = matcher1.end();
	                            		if(extreme.charAt(q-2)=='–' | extreme.charAt(q-2)=='-')
	                            			countrange = countrange.concat(" min_extreme_count_"+countct+"=\""+extreme.substring(p+1,q-2)+"\"");
	                            		else
	                            			countrange = countrange.concat(" min_extreme_count_"+countct+"=\""+extreme.substring(p+1,q-1)+"\"");
	                            	}
	                            	extreme = matcher1.replaceAll("#");
	                        		matcher1.reset();
	                        		if(extreme.contains("#"))
	                        			i = extreme.indexOf("#")+1;
	                        		j = extreme.length();
	                        		Pattern pattern21 = Pattern.compile("\\[[±\\d\\.\\s\\+\\–\\-]*[\\–\\-]{1}[±\\d\\.\\s\\+]+\\]");
	                            	matcher1 = pattern21.matcher(extreme);
	                            	if ( matcher1.find()){
	                            		int p = matcher1.start();
	                            		int q = matcher1.end();
	                            		j = p;
	                            		if(extreme.charAt(p+1)=='–' | extreme.charAt(p+1)=='-')
	                            			countrange = countrange.concat(" max_extreme_count_"+countct+"=\""+extreme.substring(p+2,q-1)+"\"");
	                            		else
	                            			countrange = countrange.concat(" max_extreme_count_"+countct+"=\""+extreme.substring(p+1,q-1)+"\"");
	                            	}
	                        		matcher1.reset();
	                        		if(extreme.substring(i,j).contains("–")|extreme.substring(i,j).contains("-") && !extreme.substring(i,j).contains("×") && !extreme.substring(i,j).contains("x") && !extreme.substring(i,j).contains("X")){
	                        			String extract = extreme.substring(i,j);
	                        			Pattern pattern22 = Pattern.compile("[\\[\\]]+");
	                        			matcher1 = pattern22.matcher(extract);
	                        			extract = matcher1.replaceAll("");
	                        			matcher1.reset();
	                        			countrange = countrange.concat(" min_count_"+countct+"=\""+extract.substring(0, extract.indexOf('-'))+"\" max_count_"+countct+"=\""+extract.substring(extract.indexOf('-')+1,extract.length())+"\"");
	                        			countct+=1;
	                        		}
	                        		else{
	                        			if(flag2==0)
	                        				innertagstate=innertagstate.concat(" "+"count=\""+extreme.subSequence(i,j).toString());
	                        			else
	                        				innertagstate=innertagstate.concat(","+extreme.subSequence(i,j).toString());
	                        			flag2=1;
	                        		}
	                        	}
	                        	if(flag2==1)
	                        		innertagstate=innertagstate.concat("\"");
	                        	innertagstate = innertagstate.concat(countrange);
	                        	matcher2.reset();                				
	            				Pattern pattern7 = Pattern.compile("[{][\\w±\\+\\–\\-\\.:=/\\_]+[}]");
	                        	matcher2 = pattern7.matcher(charset);
	                        	String str3 = "";
	                        	//int flag3=0;
	                        	while (matcher2.find()){
	                        		flag7=1;
	                        		int flag5=0;
	                        		String first = "";
	                        		String chstate = "";
	                        		i=matcher2.start()+1;
	                        		j=matcher2.end()-1;
	                        		str3=charset.subSequence(i,j).toString();
	                        		if(str3.contains("-")|str3.contains("–")){
	                        			first = str3.substring(0, str3.indexOf("-"));
	                        			str3=str3.substring(str3.indexOf("-")+1|str3.indexOf("–")+1, str3.length());
	                        			flag5=1;
	                        		}
	                        			ResultSet rs1 = stmt1.executeQuery("select * from character_markup_ontology where term='"+str3+"'");
	                        			if(rs1.next()){
	                        				int flag4=0;
	                        				chstate=rs1.getString(4);
	                        				if(chstate.contains("/")){
	                        					String [] terms = chstate.split("/");
	                        					chstate=terms[0];
	                                			for(int t=1;t<terms.length;t++)
	                                				chstate=chstate.concat("_or_"+terms[t]);  
	                        				}
	                        				StringBuffer sb = new StringBuffer();
	                    					Pattern pattern8 = Pattern.compile(chstate+"=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,xX\\×]+\"");
	                    					Matcher matcher3 = pattern8.matcher(innertagstate);
	                    					while ( matcher3.find()){
	                    						int q=matcher3.start();
	                    						int r=matcher3.end();
	                    						if(flag5==1)
	                    							matcher3.appendReplacement(sb, innertagstate.subSequence(q,r-1)+","+first+"-"+str3+"\"");
	                            				else
	                            					matcher3.appendReplacement(sb, innertagstate.subSequence(q,r-1)+","+str3+"\"");
	                    						flag4=1;
	                    					}
	                    					if(flag4==1){
	                    						matcher3.appendTail(sb);
	                    						innertagstate=sb.toString();
	                    					}
	                    					else{
	                    						if(flag5==1)
	                    							innertagstate=innertagstate.concat(" "+chstate+"=\""+first+"-"+str3+"\"");
	                    						else
	                    							innertagstate=innertagstate.concat(" "+chstate+"=\""+str3+"\"");
	                    					}
	                    					matcher3.reset();
	                        			}                			
	                        	}
	                    		matcher2.reset();                				
	            				Pattern pattern5 = Pattern.compile("[a-zA-Z]+");
	                    		Matcher matcher1 = pattern5.matcher(relation);
	                    		flag=0;
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
	                            matcher2 = pattern10.matcher(plainrelation);
	                            plainrelation = matcher2.replaceAll(" ");
	                            matcher2.reset();
	                            Pattern pattern11 = Pattern.compile(":");
	                            // Replace all occurrences of pattern in input
	                            matcher2 = pattern11.matcher(plainrelation);
	                            plainrelation = matcher2.replaceAll(",");
	                            matcher2.reset();
	                        	if(ct==1)
	                        		outertag=outertag.concat("<"+org1+"_R"+ct+"_"+org2);
	                        	else
	                        		outertag=outertag.concat("_R"+ct+"_"+org2);
	                        	if(flag7 == 1)
	                        		innertags=innertags.concat("<"+org1+innertagstate+"/><"+org2+"/><relation code=\"R"+ct+"\" agent=\""+org1+"\" target=\""+org2+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
	                        	else	
	                        		innertags=innertags.concat("<"+org1+"/><"+org2+"/><relation code=\"R"+ct+"\" agent=\""+org1+"\" target=\""+org2+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
	                        	markedrelations = markedrelations.concat("<relation code=\"R"+ct+"\" agent=\""+org1+"\" target=\""+org2+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
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
	                        	if(ct==1)
	                        		outertag=outertag.concat("<"+org1+"_R"+ct+"_"+org2);
	                        	else
	                        		outertag=outertag.concat("_R"+ct+"_"+org2);
	                        	innertags=innertags.concat("<"+org1+"/><"+org2+"/><relation code=\"R"+ct+"\" agent=\""+org1+"\" target=\""+org2+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
	                        	markedrelations = markedrelations.concat("<relation code=\"R"+ct+"\" agent=\""+org1+"\" target=\""+org2+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
	            			}
	            			ct++;
	            		}
	            		outertag=outertag.concat(">");               		
	            		
	            //handle states for first and last organ in segment
	            		innertagstate = "";
	            		Pattern pattern6 = Pattern.compile("[\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×\\s,µ%\\*\\{\\}\\[\\](<\\{)(\\}>)m]+<"+organ1+">");
	                	Matcher matcher1 = pattern6.matcher(str);
	                	String state = "";
	                	flag6 = 0;
	                	while ( matcher1.find()){
	                		i=matcher1.start();
	                		j=matcher1.end();
	                		state=state.concat(str.subSequence(i,j).toString());
	                		String plaincharset = "";
	                		plaincharset = plaintextextractornum(state);
	        				innertagstate = charstatehandler(plaincharset, state);
	                	}
	                	matcher1.reset();
	                	if(flag6 == 1){
	                		StringBuffer sb = new StringBuffer();
	                		if(organ1.compareTo("n")==0)
	                			organ1 = "chromosome_count";
	                		Pattern pattern9 = Pattern.compile("<"+organ1+"[\\w±\\+\\–\\-\\—°²\\.:=/\\s½\"¼;x´\\×\\*µ%“”\\_,]*/>");
	                		Matcher matcher2 = pattern9.matcher(innertags);
	                		if ( matcher2.find()){
	                			int q=matcher2.start();
	                			matcher2.appendReplacement(sb, innertags.subSequence(q,innertags.indexOf("/",q))+" "+innertagstate+"/>");
	                		}
	                		matcher2.appendTail(sb);
	                		innertags=sb.toString();
	                	}
	                	innertagstate = "";
	                	Pattern pattern10 = Pattern.compile("<"+organ2+">[\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×\\s,=µ%\\*\\{\\}\\[\\](<\\{)(\\}>)m]+");
	                	matcher1 = pattern10.matcher(str);
	                	state = "";
	                	flag6 = 0;
	                	while ( matcher1.find()){
	                		i=matcher1.start();
	                		j=matcher1.end();
	                		state=state.concat(str.subSequence(i,j).toString());
	                		String plaincharset = "";
	                		plaincharset = plaintextextractornum(state);
	                		innertagstate = charstatehandler(plaincharset, state);
	                	}
	                	matcher1.reset();
	                	if(flag6 == 1){
	                		//System.out.println("innertags:"+innertags);
	                		//System.out.println("<"+org2+"/><relation code=\"R"+(ct-1)+"\" agent=\""+org1+"\" target=\""+org2+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
	                		ct-=1;
	                		StringBuffer sb = new StringBuffer();
	                		Pattern pattern9 = Pattern.compile("<"+org2+"/><relation code=\"R"+ct+"\" agent=\""+org1+"\" target=\""+org2+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
	                		Matcher matcher2 = pattern9.matcher(innertags);
	                		while ( matcher2.find()){
	                			int q=matcher2.start();
	                			matcher2.appendReplacement(sb, innertags.subSequence(q,innertags.indexOf("/",q))+innertagstate+"/><relation code=\"R"+ct+"\" agent=\""+org1+"\" target=\""+org2+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
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
	        		markedsent = markedsent.concat(outertag+innertags+"</"+outertag.substring(1));
	        		//stmt1.execute("insert into marked_complexseg values('"+rs.getString(1)+"','"+rs.getString(2)+"','"+markedsent+"','"+markedrelations+"')");
	        		stmt1.execute("insert into marked_simpleseg values('"+rs.getString(1)+"','"+rs.getString(2)+"','"+markedsent+"')");
    	
                        	

	        		
                	
                	
                	
                	
                	
                	
                	
                	
                	
                	
                	
                	
                	/*
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
                */
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
	
	protected String charstatehandler(String plaincharset, String state){
		String innertagstate = "";
		try{
			Statement stmt2 = conn.createStatement();
			int i,j;
			//System.out.println("plain:"+plaincharset);
			//System.out.println("state:"+state);
			Pattern pattern19 = Pattern.compile("[±]?[\\[]?[\\d\\s\\.]+[\\]]?[\\[]?[\\–\\-]+[\\]]?[\\[]?[\\d\\s\\.]+[\\]]?[dcmµ]?[m]?[\\s]?[xX\\×]+[\\[]?[\\d\\s\\.]+[\\]]?[\\[]?[\\–\\-]+[\\]]?[\\[]?[\\d\\s\\.]+[\\]]?[dcmµ]?m");
        	Matcher matcher2 = pattern19.matcher(plaincharset);
        	int flag3=0;
        	while ( matcher2.find()){
        		if(plaincharset.charAt(matcher2.start())==' '){
        			i=matcher2.start()+1;
        		}
        		else{
        			i=matcher2.start();
        		}
        		j=matcher2.end();
        		if(flag3==0)
    				innertagstate=innertagstate.concat(" "+"size=\""+plaincharset.subSequence(i,j).toString());
    			else
    				innertagstate=innertagstate.concat(","+plaincharset.subSequence(i,j).toString());
    			flag3=1;
        	}
        	if(flag3==1)
        		innertagstate=innertagstate.concat("\"");
        	plaincharset = matcher2.replaceAll("#");
        	matcher2.reset();
        	
           	int sizect = 0;
           	Pattern pattern13 = Pattern.compile("[xX\\×±\\d\\[\\]\\–\\-\\.\\s\\+]+[\\s]?[dcmµ]?m(?![\\w])(([\\s]diam)?([\\s]wide)?)");
	    	matcher2 = pattern13.matcher(plaincharset);
	    	int flag=0;
	    	String numrange="";
	    	while ( matcher2.find()){
	    		flag6 = 1;
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
        		Pattern pattern20 = Pattern.compile("\\[[±\\d\\.\\s\\+]+[\\–\\-]{1}[±\\d\\.\\s\\+\\–\\-]*\\]");
            	Matcher matcher1 = pattern20.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		if(extreme.charAt(q-2)=='–' | extreme.charAt(q-2)=='-')
            			numrange = numrange.concat(" min_extreme_size_"+sizect+"=\""+extreme.substring(p+1,q-2)+"\"");
            		else
            			numrange = numrange.concat(" min_extreme_size_"+sizect+"=\""+extreme.substring(p+1,q-1)+"\"");
            	}
            	extreme = matcher1.replaceAll("#");
        		matcher1.reset();
        		if(extreme.contains("#"))
        			i = extreme.indexOf("#")+1;
        		Pattern pattern21 = Pattern.compile("\\[[±\\d\\.\\s\\+\\–\\-]*[\\–\\-]{1}[±\\d\\.\\s\\+]+\\]");
            	matcher1 = pattern21.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		if(extreme.charAt(p+1)=='–' | extreme.charAt(p+1)=='-')
            			numrange = numrange.concat(" max_extreme_size_"+sizect+"=\""+extreme.substring(p+2,q-1)+"\"");
            		else
            			numrange = numrange.concat(" max_extreme_size_"+sizect+"=\""+extreme.substring(p+1,q-1)+"\"");
            	}
            	extreme = matcher1.replaceAll("#");
        		matcher1.reset();
        		j = extreme.length();
        		Pattern pattern23 = Pattern.compile("\\[[±\\d\\.\\s\\+]+\\]");
            	matcher1 = pattern23.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		numrange = numrange.concat(" max_extreme_size_"+sizect+"=\""+extreme.substring(p+1,q-1)+"\"");
            	}
            	extreme = matcher1.replaceAll("#");
            	matcher1.reset();
            	j = extreme.length();
        		if(extreme.substring(i,j).contains("–")|extreme.substring(i,j).contains("-") && !extreme.substring(i,j).contains("×") && !extreme.substring(i,j).contains("x") && !extreme.substring(i,j).contains("X")){
        			String extract = extreme.substring(i,j);
        			Pattern pattern18 = Pattern.compile("[\\s]?[dcmµ]?m(([\\s]diam)?([\\s]wide)?)");
                	Matcher matcher3 = pattern18.matcher(extract);
                	String unit="";
                	if ( matcher3.find()){
                		unit = extract.substring(matcher3.start(), matcher3.end());
                	}
                	extract = matcher3.replaceAll("#");
                	matcher3.reset();
        			numrange = numrange.concat(" min_size_"+sizect+"=\""+extract.substring(0, extract.indexOf('-'))+"\" min_size_unit_"+sizect+"=\""+unit+"\" max_size_"+sizect+"=\""+extract.substring(extract.indexOf('-')+1,extract.indexOf('#'))+"\" max_size_unit_"+sizect+"=\""+unit+"\"");
        			sizect+=1;
        		}
        		else{
        			if(flag3==1){
        				StringBuffer sb = new StringBuffer();
						Pattern pattern9 = Pattern.compile("size=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,xX\\×]+\"");
						matcher1 = pattern9.matcher(innertagstate);
						while ( matcher1.find()){
							int p=matcher1.start();
							int q=matcher1.end();
							matcher1.appendReplacement(sb, innertagstate.subSequence(p,q-1)+","+extreme.subSequence(i,j).toString()+"\"");
						}
						matcher1.appendTail(sb);
						innertagstate=sb.toString();
						matcher1.reset();
        			}
        			else{
        				if(flag==0)
        					innertagstate=innertagstate.concat(" "+"size=\""+extreme.subSequence(i,j).toString());
        				else
        					innertagstate=innertagstate.concat(","+extreme.subSequence(i,j).toString());
        				flag=1;
        			}
        		}
	    	}
	    	if(flag==1)
	    		innertagstate=innertagstate.concat("\"");
	    	plaincharset = matcher2.replaceAll("#");
	    	innertagstate = innertagstate.concat(numrange);
	    	matcher2.reset();
	    	Pattern pattern14 = Pattern.compile("[±\\d\\[\\]\\–\\-\\./\\s]+[\\s]?[\\–\\-]?(% of [\\w]+ length|height of [\\w]+|times as [\\w]+ as [\\w]+|total length|their length|(times)?[\\s]?length of [\\w]+)");
	    	matcher2 = pattern14.matcher(plaincharset);
	    	int flag1=0;
	    	while ( matcher2.find()){
	    		flag6 = 1;
	    		if(plaincharset.charAt(matcher2.start())==' '){
	    			i=matcher2.start()+1;
	    		}
	    		else{
	    			i=matcher2.start();
	    		}
	    		j=matcher2.end();
	    		if(flag==1){
	    			StringBuffer sb = new StringBuffer();
					Pattern pattern9 = Pattern.compile("size=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,xX\\×]+\"");
					Matcher matcher3 = pattern9.matcher(innertagstate);
					while ( matcher3.find()){
						int p=matcher3.start();
						int q=matcher3.end();
						matcher3.appendReplacement(sb, innertagstate.subSequence(p,q-1)+","+plaincharset.subSequence(i,j).toString()+"\"");
					}
					matcher3.appendTail(sb);
					innertagstate=sb.toString();
					matcher3.reset();
	    		}
	    		else{
	    			if(flag1==0)
	    				innertagstate=innertagstate.concat(" "+"size=\""+plaincharset.subSequence(i,j).toString());
	    			else
	    				innertagstate=innertagstate.concat(","+plaincharset.subSequence(i,j).toString());
	    			flag1=1;
	    		}
	    	}
	    	if(flag1==1)
	    		innertagstate=innertagstate.concat("\"");
	    	plaincharset = matcher2.replaceAll("#");
	    	matcher2.reset();
	    	int countct = 0;
	    	Pattern pattern15 = Pattern.compile("([\\[]?[±]?[\\d]+[\\]]?[\\[]?[\\–\\-][\\]]?[\\[]?[\\d]+[+]?[\\]]?|[\\[]?[±]?[\\d]+[+]?[\\]]?)[\\–\\–\\-]+[a-zA-Z]+");
	    	matcher2 = pattern15.matcher(plaincharset);
	    	plaincharset = matcher2.replaceAll("#");
	    	matcher2.reset();     	
	    	Pattern pattern16 = Pattern.compile("(?<!([/][\\s]?))([\\[]?[±]?[\\d]+[\\]]?[\\[]?[\\–\\-][\\]]?[\\[]?[\\d]+[+]?[\\]]?([\\[]?[\\–\\-]?[\\]]?[\\[]?[\\d]+[+]?[\\]]?)*|[±]?[\\d]+[+]?)(?!([\\s]?[n/]|[\\s]?[\\–\\-]?% of [\\w]+ length|[\\s]?[\\–\\-]?height of [\\w]+|[\\s]?[\\–\\-]?times|[\\s]?[\\–\\-]?total length|[\\s]?[\\–\\-]?their length|[\\s]?[\\–\\-]?(times)?[\\s]?length of|[\\s]?[dcmµ]?m))");
	    	matcher2 = pattern16.matcher(plaincharset);
	    	int flag2=0;
	    	String countrange = "";
	    	while ( matcher2.find()){
	    		flag6 = 1;
	    		i=matcher2.start();
	    		j=matcher2.end();
	    		String extreme = plaincharset.substring(i,j);
    			i = 0;
    			j = extreme.length();
        		Pattern pattern20 = Pattern.compile("\\[[±\\d\\.\\s\\+]+[\\–\\-]{1}[±\\d\\.\\s\\+\\–\\-]*\\]");
            	Matcher matcher1 = pattern20.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		if(extreme.charAt(q-2)=='–' | extreme.charAt(q-2)=='-')
            			countrange = countrange.concat(" min_extreme_count_"+countct+"=\""+extreme.substring(p+1,q-2)+"\"");
            		else
            			countrange = countrange.concat(" min_extreme_count_"+countct+"=\""+extreme.substring(p+1,q-1)+"\"");
            	}
            	extreme = matcher1.replaceAll("#");
        		matcher1.reset();
        		if(extreme.contains("#"))
        			i = extreme.indexOf("#")+1;
        		j = extreme.length();
        		Pattern pattern21 = Pattern.compile("\\[[±\\d\\.\\s\\+\\–\\-]*[\\–\\-]{1}[±\\d\\.\\s\\+]+\\]");
            	matcher1 = pattern21.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		j = p;
            		if(extreme.charAt(p+1)=='–' | extreme.charAt(p+1)=='-')
            			countrange = countrange.concat(" max_extreme_count_"+countct+"=\""+extreme.substring(p+2,q-1)+"\"");
            		else
            			countrange = countrange.concat(" max_extreme_count_"+countct+"=\""+extreme.substring(p+1,q-1)+"\"");
            	}
        		matcher1.reset();
        		if(extreme.substring(i,j).contains("–")|extreme.substring(i,j).contains("-") && !extreme.substring(i,j).contains("×") && !extreme.substring(i,j).contains("x") && !extreme.substring(i,j).contains("X")){
        			String extract = extreme.substring(i,j);
        			Pattern pattern22 = Pattern.compile("[\\[\\]]+");
        			matcher1 = pattern22.matcher(extract);
        			extract = matcher1.replaceAll("");
        			matcher1.reset();
        			countrange = countrange.concat(" min_count_"+countct+"=\""+extract.substring(0, extract.indexOf('-'))+"\" max_count_"+countct+"=\""+extract.substring(extract.indexOf('-')+1,extract.length())+"\"");
        			countct+=1;
        		}
        		else{
        			if(flag2==0)
        				innertagstate=innertagstate.concat(" "+"count=\""+extreme.subSequence(i,j).toString());
        			else
        				innertagstate=innertagstate.concat(","+extreme.subSequence(i,j).toString());
        			flag2=1;
        		}
	    	}
	    	if(flag2==1)
	    		innertagstate=innertagstate.concat("\"");
	    	innertagstate = innertagstate.concat(countrange);
	    	matcher2.reset();                	
			Pattern pattern7 = Pattern.compile("[{][\\w±\\+\\–\\-\\.:=/\\_]+[}]");
	    	matcher2 = pattern7.matcher(state);
	    	String str3 = "";
	    	//int flag3=0;
	    	while ( matcher2.find()){
	    		flag6=1;
	    		int flag5=0;
	    		String first = "";
	    		String chstate = "";
	    		i=matcher2.start()+1;
	    		j=matcher2.end()-1;
	    		str3=state.subSequence(i,j).toString();
	    		if(str3.contains("-")|str3.contains("–")){
	    			first = str3.substring(0, str3.indexOf("-"));
	    			str3=str3.substring(str3.indexOf("-")+1|str3.indexOf("–")+1, str3.length());
	    			flag5=1;
	    		}
	    			ResultSet rs1 = stmt2.executeQuery("select * from character_markup_ontology where term='"+str3+"'");
	    			if(rs1.next()){
	    				int flag4=0;
	    				chstate=rs1.getString(4);
	    				if(chstate.contains("/")){
	    					String [] terms = chstate.split("/");
	    					chstate=terms[0];
	            			for(int t=1;t<terms.length;t++)
	            				chstate=chstate.concat("_or_"+terms[t]);  
	    				}
	    				StringBuffer sb = new StringBuffer();
						Pattern pattern8 = Pattern.compile(chstate+"=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,xX\\×]+\"");
						Matcher matcher3 = pattern8.matcher(innertagstate);
						while ( matcher3.find()){
							int q=matcher3.start();
							int r=matcher3.end();
							if(flag5==1)
								matcher3.appendReplacement(sb, innertagstate.subSequence(q,r-1)+","+first+"-"+str3+"\"");
	        				else
	        					matcher3.appendReplacement(sb, innertagstate.subSequence(q,r-1)+","+str3+"\"");
							flag4=1;
						}
						if(flag4==1){
							matcher3.appendTail(sb);
							innertagstate=sb.toString();
						}
						else{
							if(flag5==1)
								innertagstate=innertagstate.concat(" "+chstate+"=\""+first+"-"+str3+"\"");
							else
								innertagstate=innertagstate.concat(" "+chstate+"=\""+str3+"\"");
						}
						matcher3.reset();
	    			}                			
	    	}
			matcher2.reset();
		}
		catch (Exception e)
        {
    		System.err.println(e);
        }
		return(innertagstate);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new ParseSimpleseg("fnav19_benchmark");
	}

}
