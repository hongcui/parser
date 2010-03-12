package fna.charactermarkup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CharStateHandler {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "root";
	static protected String password = "";
	
	public CharStateHandler() {
		CharStateHandler.database = "fnav19_benchmark";
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
	
	public static String characterstate(String plaincharset, String state){
		new CharStateHandler();
		String innertagstate = "";
		try{
			Statement stmt2 = conn.createStatement();
			int i,j;
			//System.out.println("plain:"+plaincharset);
			//System.out.println("state:"+state);
			Pattern pattern19 = Pattern.compile("[±]?[\\[]?[\\d\\s\\.]+[\\]]?[\\[]?[\\–\\-]+[\\]]?[\\[]?[\\d\\s\\.]+[+]?[\\]]?[dcmµ]?[m]?[\\s]?[xX\\×]+[\\[]?[\\d\\s\\.]+[\\]]?[\\[]?[\\–\\-]+[\\]]?[\\[]?[\\d\\s\\.]+[+]?[\\]]?[dcmµ]?m");
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
        		String match = plaincharset.substring(i, j);
        		Pattern pattern18 = Pattern.compile("[\\s]?[dcmµ]?m");
            	Matcher matcher3 = pattern18.matcher(match);
            	String[] unit = new String[2];
        		int num = 0;
            	while ( matcher3.find()){
            		unit[num] = match.substring(matcher3.start(), matcher3.end());
            		num++;
            	}
            	matcher3.reset();
        		int en = match.indexOf('-');
        		int lasten = match.lastIndexOf('-');
        		if (match.substring(en+1, match.indexOf(' ',en+1)).contains("+"))
        			innertagstate=innertagstate.concat("<character type=\"range_value\" name=\"length\" from=\""+match.substring(0,en)+"\" to=\""+match.substring(en+1, match.indexOf('+',en+1))+"\" upper_restricted=\"no\" unit=\""+unit[0]+"\"/>");
        		else
        			innertagstate=innertagstate.concat("<character type=\"range_value\" name=\"length\" from=\""+match.substring(0,en)+"\" to=\""+match.substring(en+1, match.indexOf(' ',en+1))+"\" unit=\""+unit[0]+"\"/>");
        		if (num>1){
        			if (match.substring(lasten+1, match.indexOf(' ',lasten+1)).contains("+"))
        				innertagstate=innertagstate.concat("<character type=\"range_value\" name=\"width\" from=\""+match.substring(match.indexOf('×')+2,lasten)+"\" to=\""+match.substring(lasten+1, match.indexOf('+',lasten+1))+"\" upper_restricted=\"no\" unit=\""+unit[1]+"\"/>");
        			else
        				innertagstate=innertagstate.concat("<character type=\"range_value\" name=\"width\" from=\""+match.substring(match.indexOf('×')+2,lasten)+"\" to=\""+match.substring(lasten+1, match.indexOf(' ',lasten+1))+"\" unit=\""+unit[1]+"\"/>");
        		}
        		else{
        			if (match.substring(lasten+1, match.indexOf(' ',lasten+1)).contains("+"))
        				innertagstate=innertagstate.concat("<character type=\"range_value\" name=\"width\" from=\""+match.substring(match.indexOf('×')+2,lasten)+"\" to=\""+match.substring(lasten+1, match.indexOf('+',lasten+1))+"\" upper_restricted=\"no\" unit=\""+unit[0]+"\"/>");
        			else
        				innertagstate=innertagstate.concat("<character type=\"range_value\" name=\"width\" from=\""+match.substring(match.indexOf('×')+2,lasten)+"\" to=\""+match.substring(lasten+1, match.indexOf(' ',lasten+1))+"\" unit=\""+unit[0]+"\"/>");
        		}
        		//if(flag3==0)
    				//innertagstate=innertagstate.concat(" "+"size=\""+plaincharset.subSequence(i,j).toString());
    			//else
        			//innertagstate=innertagstate.concat(","+plaincharset.subSequence(i,j).toString());
    			//flag3=1;
        	}
        	//if(flag3==1)
        		//innertagstate=innertagstate.concat("\"/>");
        	plaincharset = matcher2.replaceAll("#");
        	matcher2.reset();
        	//System.out.println("plaincharset1:"+plaincharset);
        	Pattern pattern24 = Pattern.compile("l/w[\\s]?=[\\d\\.\\s\\+\\–\\-]+");
        	matcher2 = pattern24.matcher(plaincharset);
        	while ( matcher2.find()){
        		if(plaincharset.charAt(matcher2.start())==' '){
        			i=matcher2.start()+1;
        		}
        		else{
        			i=matcher2.start();
        		}
        		j=matcher2.end();
        		String match = plaincharset.substring(i, j);
        		int en = match.indexOf('-');
        		if (match.contains("+"))
        			innertagstate=innertagstate.concat("<character type=\"range_value\" name=\"l_w_ratio\" from=\""+match.substring(match.indexOf('=')+2,en)+"\" to=\""+match.substring(en+1, match.indexOf('+',en+1))+"\" upper_restricted=\"no\"/>");
        		else
        			innertagstate=innertagstate.concat("<character type=\"range_value\" name=\"l_w_ratio\" from=\""+match.substring(match.indexOf('=')+2,en)+"\" to=\""+match.substring(en+1, match.indexOf(' ',en+1))+"\"/>");
        	}
        	plaincharset = matcher2.replaceAll("#");
        	matcher2.reset();
        	int sizect = 0;
			Pattern pattern13 = Pattern.compile("[xX\\×±\\d\\[\\]\\–\\-\\.\\s\\+]+[\\s]?[dcmµ]?m(?![\\w])(([\\s]diam)?([\\s]wide)?)");
        	matcher2 = pattern13.matcher(plaincharset);
        	int flag=0;
        	String toval="";
        	String fromval="";
        	while ( matcher2.find()){
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
            			innertagstate = innertagstate.concat("<character type=\"range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,q-2)+"\" to=\"\"/>");
            		else
            			innertagstate = innertagstate.concat("<character type=\"range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1))+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-1)+"\"/>");
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
            		if (extreme.charAt(p+1)=='–' | extreme.charAt(p+1)=='-'){
            			if (extreme.charAt(q-2)=='+')
            				innertagstate = innertagstate.concat("<character type=\"range_value\" name=\"atypical_size\" from=\"\" to=\""+extreme.substring(p+2,q-2)+"\" upper_restricted=\"no\"/>");
            			else
            				innertagstate = innertagstate.concat("<character type=\"range_value\" name=\"atypical_size\" from=\"\" to=\""+extreme.substring(p+2,q-1)+"\"/>");
            		}
            		else{
            			if (extreme.charAt(q-2)=='+')
            				innertagstate = innertagstate.concat("<character type=\"range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1))+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-2)+"\" upper_restricted=\"no\"/>");
            			else
            				innertagstate = innertagstate.concat("<character type=\"range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1))+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-1)+"\"/>");
            		}
            	}
            	extreme = matcher1.replaceAll("#");
        		matcher1.reset();
        		j = extreme.length();
        		Pattern pattern23 = Pattern.compile("\\[[±\\d\\.\\s\\+]+\\]");
            	matcher1 = pattern23.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		if (extreme.charAt(q-2)=='+')
            			innertagstate = innertagstate.concat("<character name=\"atypical_size\" from=\""+extreme.substring(p+1,q-2)+"\" upper_restricted=\"no\"/>");
        			else
        				innertagstate = innertagstate.concat("<character name=\"atypical_size\" value=\""+extreme.substring(p+1,q-1)+"\"/>");
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
                	innertagstate = innertagstate.concat("<character type=\"range_value\" name=\"size\" from=\""+extract.substring(0, extract.indexOf('-'))+"\" from_unit=\""+unit+"\" to=\""+extract.substring(extract.indexOf('-')+1,extract.indexOf('#'))+"\" to_unit=\""+unit+"\"/>");
        			toval = extract.substring(0, extract.indexOf('-'));
        			fromval = extract.substring(extract.indexOf('-')+1,extract.indexOf('#'));
                	sizect+=1;
        		}
        		else{
        			/*if(flag3==1){
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
        			}*/
        			String extract = extreme.substring(i,j);
        			Pattern pattern18 = Pattern.compile("[\\s]?[dcmµ]?m(([\\s]diam)?([\\s]wide)?)");
                	Matcher matcher3 = pattern18.matcher(extract);
                	String unit="";
                	if ( matcher3.find()){
                		unit = extract.substring(matcher3.start(), matcher3.end());
                	}
                	extract = matcher3.replaceAll("#");
                	matcher3.reset();
        			innertagstate = innertagstate.concat("<character name=\"size\" value=\""+extract.substring(0,extract.indexOf('#'))+"\" unit=\""+unit+"\"/>");
        			toval = extract.substring(0,extract.indexOf('#'));
        			fromval = extract.substring(0,extract.indexOf('#'));
        		}
        		
        		StringBuffer sb = new StringBuffer();
				Pattern pattern25 = Pattern.compile("to=\"\"");
				matcher1 = pattern25.matcher(innertagstate);
				while ( matcher1.find()){
					matcher1.appendReplacement(sb, "to=\""+toval+"\"");
				}
				matcher1.appendTail(sb);
				innertagstate=sb.toString();
				matcher1.reset();
				StringBuffer sb1 = new StringBuffer();
				Pattern pattern26 = Pattern.compile("from=\"\"");
				matcher1 = pattern26.matcher(innertagstate);
				while ( matcher1.find()){
					matcher1.appendReplacement(sb1, "from=\""+fromval+"\"");
				}
				matcher1.appendTail(sb1);
				innertagstate=sb1.toString();
				matcher1.reset();
        	}
        	//if(flag==1)
        		//innertagstate=innertagstate.concat("\"");
        	plaincharset = matcher2.replaceAll("#");
        	matcher2.reset();
        	//System.out.println("plaincharset2:"+plaincharset);
        	Pattern pattern14 = Pattern.compile("[±\\d\\[\\]\\–\\-\\./\\s]+[\\s]?[\\–\\-]?(% of [\\w]+ length|height of [\\w]+|times as [\\w]+ as [\\w]+|total length|their length|(times)?[\\s]?length of [\\w]+)");
        	matcher2 = pattern14.matcher(plaincharset);
        	int flag1=0;
        	while ( matcher2.find()){
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
        	while ( matcher2.find()){
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
            			innertagstate = innertagstate.concat("<character type=\"range_value\" name=\"atypical_count\" from=\""+extreme.substring(p+1,q-2)+"\" to=\"\"/>");
            		else
            			innertagstate = innertagstate.concat("<character type=\"range_value\" name=\"atypical_count\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1))+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-1)+"\"/>");
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
            		if (extreme.charAt(p+1)=='–' | extreme.charAt(p+1)=='-'){
            			if (extreme.charAt(q-2)=='+')
            				innertagstate = innertagstate.concat("<character type=\"range_value\" name=\"atypical_count\" from=\"\" to=\""+extreme.substring(p+2,q-2)+"\" upper_restricted=\"no\"/>");
            			else
            				innertagstate = innertagstate.concat("<character type=\"range_value\" name=\"atypical_count\" from=\"\" to=\""+extreme.substring(p+2,q-1)+"\"/>");
            		}
            		else{
            			if (extreme.charAt(q-2)=='+')
            				innertagstate = innertagstate.concat("<character type=\"range_value\" name=\"atypical_count\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1))+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-2)+"\" upper_restricted=\"no\"/>");
            			else
            				innertagstate = innertagstate.concat("<character type=\"range_value\" name=\"atypical_count\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1))+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-1)+"\"/>");
            		}
            	}
        		matcher1.reset();
        		Pattern pattern23 = Pattern.compile("\\[[±\\d\\.\\s\\+]+\\]");
            	matcher1 = pattern23.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		j = p;
            		if (extreme.charAt(q-2)=='+')
            			innertagstate = innertagstate.concat("<character name=\"atypical_count\" from=\""+extreme.substring(p+1,q-2)+"\" upper_restricted=\"no\"/>");
        			else
        				innertagstate = innertagstate.concat("<character name=\"atypical_count\" value=\""+extreme.substring(p+1,q-1)+"\"/>");
            	}
            	matcher1.reset();
        		if(extreme.substring(i,j).contains("–")|extreme.substring(i,j).contains("-") && !extreme.substring(i,j).contains("×") && !extreme.substring(i,j).contains("x") && !extreme.substring(i,j).contains("X")){
        			String extract = extreme.substring(i,j);
        			Pattern pattern22 = Pattern.compile("[\\[\\]]+");
        			matcher1 = pattern22.matcher(extract);
        			extract = matcher1.replaceAll("");
        			matcher1.reset();
                	innertagstate = innertagstate.concat("<character type=\"range_value\" name=\"count\" from=\""+extract.substring(0, extract.indexOf('-'))+"\" to=\""+extract.substring(extract.indexOf('-')+1,extract.length())+"\"/>");
        			toval = extract.substring(0, extract.indexOf('-'));
        			fromval = extract.substring(extract.indexOf('-')+1,extract.length());
        			countct+=1;
        		}
        		else{
        			/*if(flag2==0)
        				innertagstate=innertagstate.concat(" "+"count=\""+extreme.subSequence(i,j).toString());
        			else
        				innertagstate=innertagstate.concat(","+extreme.subSequence(i,j).toString());
        			flag2=1;*/
        			String extract = extreme.substring(i,j);
        			innertagstate = innertagstate.concat("<character name=\"count\" value=\""+extract+"\"/>");
        			toval = extract;
        			fromval = extract;
        		}
        		
        		StringBuffer sb = new StringBuffer();
				Pattern pattern25 = Pattern.compile("to=\"\"");
				matcher1 = pattern25.matcher(innertagstate);
				while ( matcher1.find()){
					matcher1.appendReplacement(sb, "to=\""+toval+"\"");
				}
				matcher1.appendTail(sb);
				innertagstate=sb.toString();
				matcher1.reset();
				StringBuffer sb1 = new StringBuffer();
				Pattern pattern26 = Pattern.compile("from=\"\"");
				matcher1 = pattern26.matcher(innertagstate);
				while ( matcher1.find()){
					matcher1.appendReplacement(sb1, "from=\""+fromval+"\"");
				}
				matcher1.appendTail(sb1);
				innertagstate=sb1.toString();
				matcher1.reset();
        	}
        	//if(flag2==1)
        		//innertagstate=innertagstate.concat("\"");
        	matcher2.reset();                				
			Pattern pattern7 = Pattern.compile("[{][\\w±\\+\\–\\-\\.:=/\\_]+[}]");
        	matcher2 = pattern7.matcher(state);
        	String str3 = "";
        	//int flag3=0;
        	while (matcher2.find()){
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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}

}
