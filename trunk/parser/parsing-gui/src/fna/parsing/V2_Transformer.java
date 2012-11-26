package fna.parsing;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;

import org.apache.log4j.Logger;
import org.jdom.xpath.XPath;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;


public class V2_Transformer {
	static protected Connection con = null;
	static protected String username = "termsuser";
	static protected String password = "termspassword";
	static Element treatment = new Element("treatment");
	static protected String connectionUrl = null;
	static ResultSet rs = null;
	public static final String usstate="Ala|Alaska|Ariz|Ark|Calif|Colo|Conn|Del|D.C|Fla|Ga|Idaho|Ill|Ind|Iowa|Kans|Ky|La|Maine|Md|Mass|Mich|Minn|Miss|Mo|Mont|Nebr|Nev|" +
			"N.H|N.J|N.Mex|N.Y|N.C|N.Dak|Ohio|Okla|Oreg|Pa|R.I|S.C|S.Dak|Tenn|Tex|Utah|Vt|Va|Wash|W.Va|Wis|Wyo";
	public static final String castate="Alta|B.C|Man|N.B|Nfld. and Labr|N.W.T|N.S|Nunavut|Ont|P.E.I|Que|Sask|Yukon|Nfld|Labr|Nfld. and Labr. (Nfld.)|Nfld. and Labr. (Labr.)";
	public static final String pheno="summer|winter|autumn|January|February|March|April|May|June|July|August|September|October|November|December|Sporulation|Sporulates|Sporulating|Sporophylls|Sporocarps|mature|Leaves|Spor|maturing";
	public static int keyexists=0; //Used to check if a key already exists.
	public static String taxname="";
	public static XPath synPath;
	Pattern abbrgenus = Pattern.compile("[A-Z]\\.");
	private static final Logger LOGGER = Logger.getLogger(V2_Transformer.class);
	
	public V2_Transformer()
	{
		connectionUrl = "jdbc:sqlserver://localhost:1433;Instance=MSSQLSERVER;user="+username+";password="+password;
		try {
			synPath = XPath.newInstance(".//TaxonIdentification[@Status=\"SYNONYM\"]"); //TaxonIdentification Status="SYNONYM"
	         // Establish the connection to the database
	         Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
	         con = DriverManager.getConnection(connectionUrl);
		}
		 catch (Exception e) {
	         StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
	      }
	}

	public static void main(String[] args) throws Exception{
		String TaxonName = null; //Contains the name of the current file
		Statement stmt = null;
		
		int z=1; //just a counter variable
		/*ResultSet tx = null;
		Statement txstmt=null;*/
		V2_Transformer transformer = new V2_Transformer();
		//Select all the files from Volume 2 of FNA (FloraId of FNA = 1, VolumeId of Volume 2 =1002)
         String SQL = "select * from FloraTaxon where FloraId = 1 and VolumeId = 1002;";
         stmt = con.createStatement();
         rs = stmt.executeQuery(SQL);

         // Iterate through the data in the result set and display it.
         while (rs.next()) {
        	 keyexists=0;
        	 taxname="";
        	 String txSQL="select * from Taxon where TaxonId="+rs.getInt(2);//TaxonId is 2nd column which is an integer in the table FloraTaxon
     		 Statement txstmt= con.createStatement();
     		 ResultSet tx = null;
     		tx = txstmt.executeQuery(txSQL);
     		if(tx.next())
     		{
     		 TaxonName = tx.getString(2);
        	 transformer.createtreatment();
        	 String seqno = rs.getString(24)+rs.getString(25);
        	 // If the sequence number is not empty then add it to the document
        	 if((seqno.trim().length()!=0)&&(!seqno.trim().matches("0")))
        	 {
        		 Element num = new Element("number");
        		 num.setText(seqno);
				 treatment.addContent(num);
        	 }
        	 else
        	 {
        	 	 System.out.println("Empty Sequence Number"); 
        	 }
        	 transformer.processName(TaxonName,rs.getInt(2),tx.getInt(4));// Takes care of taxon name, authority, publication. //The integer is not used anywhere.//passing rankid aswell
        	 //Process the common name and etymology
        	 String commonname="";
        	 commonname+=rs.getString(14); //14th column is the common name in FloraTaxa
        	 commonname=commonname.replaceAll("null", "");
        	 if(commonname.length()!=0)
        	 {
        		 transformer.processcommonname(commonname);
        	 }
        	 //Process the Flora text
        	 String FtSQL="select * from FloraText where FloraId = 1 and TaxonId="+rs.getInt(2);//TaxonId is 2nd column which is an integer in the table FloraTaxon
     		 Statement Ftstmt= con.createStatement();
     		 ResultSet fsql = null;
     		 fsql = Ftstmt.executeQuery(FtSQL);
     		 while(fsql.next())//Execute each row differently based on CategoryId
     		 {
     			 if(fsql.getInt(3)==20091)//Author names have CategoryId 20091 which is the third column in FloraText <author>
     			 {
     				 transformer.processAuthor(fsql.getString(4));//4th column is the text
     			 }
     			 else  if(fsql.getInt(3)==20093)//Selected references have CategoryId 20093 which is the third column in FloraText
     			 {
     				 transformer.processReference(fsql.getString(4));
     			 }
     			 else  if(fsql.getInt(3)==20021)//Description have CategoryId 20021 which is the third column in FloraText
    			 {
     				String text=fsql.getString(4);
     				text=transformer.cleaninputtext(text);
     				Element desc= new Element("description");
    				desc.setText(text);
    				treatment.addContent(desc);
    			 }
     			 else  if(fsql.getInt(3)==20027)//Number of infrataxa have CategoryId 20021 which is the third column in FloraText
     			 {
     				String text=fsql.getString(4);
     				text=transformer.cleaninputtext(text);
     				String[] newchunks=new String[0];
    				newchunks=text.split(":");
    				for(int k=0;k<newchunks.length;k++)
    				{
    					if(k==0)
    					{
    						Element infrataxa = new Element("number_of_infrataxa");
    						infrataxa.setText(newchunks[k]+":");
    						treatment.addContent(infrataxa);
    					}
    					else
    					{

    						String disttext="";
    						int closure=0;
    						text = newchunks[k];
    						String[] distchunks=text.split(";|,");
    						
    						for(int i=0;i<distchunks.length;i++)
    						{
    							if(distchunks[i].length()==0)
    							{
    								continue;
    							}
    							
    							if(closure==1&&!distchunks[i].contains(")"))
    							{
    								disttext=disttext+","+distchunks[i];
    							}
    							else if(closure==1&&distchunks[i].contains(")"))
    							{
    								disttext=disttext+","+distchunks[i];
    								closure=0;
    								Element dist = new Element("global_distribution");
    								dist.setText(disttext);
    								treatment.addContent(dist);
    							}
    							else if(distchunks[i].contains("(")&&!distchunks[i].contains(")"))
    							{
    								disttext=disttext+distchunks[i];
    								closure=1;
    							}
    							else if(distchunks[i].trim().contains("introduced"))//introduced
    							{
    								Element dist = new Element("introduced");
    								dist.setText(distchunks[i]);
    								treatment.addContent(dist);
    							}
    							else if(distchunks[i].trim().matches("\\s*("+castate+")\\.?\\s*.*"))  //try this
    							//else if(distchunks[i].trim().matches(".*\\s*("+castate+")\\s*.*"))
    							{
    								Element dist = new Element("ca_distribution");
    								dist.setText(distchunks[i]);
    								treatment.addContent(dist);
    							}
    							else if(distchunks[i].trim().matches("\\s*("+usstate+")\\.?\\s*.*")&&!distchunks[i].trim().matches(".*Pacific.*"))   //try this
    							//else if(distchunks[i].trim().matches(".*\\s*("+usstate+")\\s*.*"))
    							{
    								Element dist = new Element("us_distribution");
    								dist.setText(distchunks[i]);
    								treatment.addContent(dist);
    							}
    							else
    							{
    								Element dist = new Element("global_distribution");
    								dist.setText(distchunks[i]);
    								treatment.addContent(dist);
    							}
    							
    						}

    					}
    				}

     			 }
     			else  if(fsql.getInt(3)==20023)//Discussion/Comment have CategoryId 20023 which is the third column in FloraText
     			{
    				String text=fsql.getString(4);
    				String[] discchunks=text.split("<(p|P)>");
    				for(int x=0;x<discchunks.length;x++)
    				{
    					String localtext=discchunks[x];
    					localtext=transformer.cleaninputtext(localtext);
    					Element disc= new Element("discussion");
        				disc.setText(localtext);
        				treatment.addContent(disc);
    				}
    				
     			}
     			else  if(fsql.getInt(3)==20041)//Habitat have CategoryId 20041 which is the third column in FloraText
     			{
     				 System.out.println("Handle habitat");	
     			}
     			else  if(fsql.getInt(3)==20099)//Version date have CategoryId 20099 which is the third column in FloraText
     			{
     				 System.out.println("Handle Version Date");	
     			}
     			else  if(fsql.getInt(3)==20019)//Common Name Note have CategoryId 20019 which is the third column in FloraText
     			{
     				 System.out.println("Handle Common Name Note");	
     			}
     			else  if(fsql.getInt(3)==20011)//Synonym have CategoryId 20011 which is the third column in FloraText
     			{
     				String text=fsql.getString(4);
     				text=transformer.cleaninputtext(text);
     				transformer.processSynonym(text,rs.getInt(2),taxname); //pass the text and the taxonid
     			}
     			else  if(fsql.getInt(3)==20040)//Phenology,Habitat,Elevation,distribution,conservationconcern have CategoryId 20040 which is the third column in FloraText
     			{
     				String text=fsql.getString(4);
     				text=transformer.cleaninputtext(text);
     				transformer.processHabiEle(text,rs.getInt(2)); //pass the text and the taxonid
     			}
     			 
     			else
     			{
     				 if(fsql.getString(4).matches("\\s*\\[.*\\]\\s*"))
     				 {
     					Element ety= new Element("etymology");
     					ety.setText("fsql.getString(4)");
     					treatment.addContent(ety);
     				 }
     				 else
     				 {
     					 Element extra= new Element("EXTRA");
        				 extra.setText(fsql.getString(4));
        				 treatment.addContent(extra);
     				 }
     				 
     			}
     				 
     		 }
     		 
     		 //Process the FloraKey
     		 Element keynode;
     		 if(keyexists==1) //remove the content of the <key> from the treatment and make a new one
     		 {
     			keynode = treatment.getChild("key"); 
     			String keyheadtext = keynode.getChildText("key_heading");
     			treatment.removeChild("key");
     			keyexists=0;
     		 }
     		 else
     		 {
     			 keynode = new Element("key");
     		 }
     		 
     		 String keySQL="select * from FloraKey where FloraId=1 and TaxonId="+rs.getInt(2)+"ORDER BY FloraKeyId;";//TaxonId is 2nd column which is an integer in the table FloraTaxon
    		 Statement keystmt= con.createStatement();
    		 ResultSet ksql = null;
    		 ksql = keystmt.executeQuery(keySQL);
    		 while(ksql.next())//Process the keys.
    		 {
    			 if(ksql.getInt(4)==0) //Item No is 0. which means it is either a key_heading or key_discussion
    			 {
    				 String keysourcetext=""+ksql.getString(11);//11th column is description
    				 String[] keychunks = keysourcetext.split("<(p|P)>");
    				 for(int c=0;c<keychunks.length;c++)
    				 {
    					 String keytext = keychunks[c];
    					 keytext=transformer.cleaninputtext(keytext).trim();
        				 if(keytext.matches("^Key.*")) //It is a key heading
        				 {
        					 keytext=transformer.cleaninputtext(keytext);
        					 Element keyhead = new Element("key_head");
        					 keyhead.setText(keytext);
        					 keynode.addContent(keyhead);
        				 }
        				 else// It is a key discussion
        				 {
        					 keytext=transformer.cleaninputtext(keytext);
        					 Element keydisc = new Element("key_discussion");
        					 keydisc.setText(keytext);
        					 keynode.addContent(keydisc);
        				 }
    				 }
    				 
    				 
    			 }
    			 else
    			 {
    				 Element statement = new Element("key_statement");
    				 String stmtno=""+ksql.getString(4);//ItemNo becomes <statement_id>
    				 stmtno = transformer.cleaninputtext(stmtno);
    				 Element sno= new Element("statement_id");
    				 sno.setText(stmtno);
    				 statement.addContent(sno);
    				 
    				 String stmttext=""+ksql.getString(11);//description becomes <statement>
    				 stmttext = transformer.cleaninputtext(stmttext);
    				 Element st= new Element("statement");
    				 st.setText(stmttext);
    				 statement.addContent(st);
    				 
    				 int Coupletlink = ksql.getInt(7);
    				 if(Coupletlink!=0)
    				 {
    					 Element stid= new Element("next_statement_id");
        				 stid.setText(Integer.toString(Coupletlink));//Convert the integer to string
        				 statement.addContent(stid);
    				 }
    				 else
    				 {
    					 String det = ""+ksql.getString(9)+" "+ksql.getString(10);
    					 det = transformer.cleaninputtext(det);
    					 Element deter= new Element("determination");
        				 deter.setText(det);//Convert the integer to string
        				 statement.addContent(deter); 
    				 }
    				 keynode.addContent(statement);	 
    			 }
    			 
    		 }
    		 if(keynode.getContentSize()!=0)
    		 {
    			 treatment.addContent(keynode);
    		 }
    		 
        	 System.out.println(z+++"   "+TaxonName+"  "+rs.getInt(2));
        	 transformer.expandAbbrNames();
        	 transformer.output(TaxonName);
     		}
     		else
     		{
     			continue;
     		}
     		
         }
        /* while (tx.next()) {
             System.out.println(tx.getString(2) + " " + tx.getString(5));
          }*/

   }
	
private void processHabiEle(String text, int TaxonId) {
		// TODO Auto-generated method stub
	int eletag=0;
	String localtext="";
	String[] textchunks = text.split(";");
	for(int a=0;a<textchunks.length;a++)
	{
		localtext=textchunks[a];
		if(eletag==1)
		{
			if(localtext.contains("introduced"))
			{
				Element intro=new Element("introduced");
				intro.setText(localtext);
				treatment.addContent(intro);
			}
			else if(localtext.contains("conservation"))
			{
				Element cons=new Element("conservation");
				cons.setText(localtext);
				treatment.addContent(cons);
			}
			else// It is a distribution
			{
				String disttext="";
				int closure=0;
				String[] distchunks=localtext.split(",");
				
				for(int i=0;i<distchunks.length;i++)
				{
					if(distchunks[i].length()==0)
					{
						continue;
					}
					
					if(closure==1&&!distchunks[i].contains(")"))
					{
						disttext=disttext+","+distchunks[i];
					}
					else if(closure==1&&distchunks[i].contains(")"))
					{
						disttext=disttext+","+distchunks[i];
						closure=0;
						Element dist = new Element("global_distribution");
						dist.setText(disttext);
						treatment.addContent(dist);
					}
					else if(distchunks[i].contains("(")&&!distchunks[i].contains(")"))
					{
						disttext=disttext+distchunks[i];
						closure=1;
					}
					else if(distchunks[i].trim().contains("introduced"))//introduced
					{
						Element dist = new Element("introduced");
						dist.setText(distchunks[i]);
						treatment.addContent(dist);
					}
					else if(distchunks[i].trim().matches("\\s*("+castate+")\\.?\\s*.*"))  //try this
					//else if(distchunks[i].trim().matches(".*\\s*("+castate+")\\s*.*"))
					{
						Element dist = new Element("ca_distribution");
						dist.setText(distchunks[i]);
						treatment.addContent(dist);
					}
					else if(distchunks[i].trim().matches("\\s*("+usstate+")\\.?\\s*.*")&&!distchunks[i].trim().matches(".*Pacific.*"))   //try this
					//else if(distchunks[i].trim().matches(".*\\s*("+usstate+")\\s*.*"))
					{
						Element dist = new Element("us_distribution");
						dist.setText(distchunks[i]);
						treatment.addContent(dist);
					}
					else
					{
						Element dist = new Element("global_distribution");
						dist.setText(distchunks[i]);
						treatment.addContent(dist);
					}
					
				}
			}
			
		}
		else
		{
			if(localtext.contains("introduced"))
			{
				Element intro=new Element("introduced");
				intro.setText(localtext);
				treatment.addContent(intro);
			}
			else if(localtext.contains("conservation"))
			{
				Element cons=new Element("conservation");
				cons.setText(localtext);
				treatment.addContent(cons);
			}
			else if(localtext.matches(".*\\d+\\s*m\\s*")|localtext.matches(".*\\d+\\s*m\\s+.*"))//localtext.contains("elevation")
			{
				eletag=1;
				Element ele=new Element("elevation");
				ele.setText(localtext);
				treatment.addContent(ele);
			}
			else if(localtext.trim().matches(".*("+pheno+").*"))
			{
				String[] phenohabi = localtext.trim().split("\\.");
				for(int b=0;b<phenohabi.length;b++)
				{
					if(phenohabi[b].matches(".*("+pheno+").*"))
					{
						Element phe=new Element("phenology");
						phe.setText(phenohabi[b]);
						treatment.addContent(phe);
					}
					else
					{
						Element habi=new Element("habitat");
						habi.setText(phenohabi[b]);
						treatment.addContent(habi);
					}
				}
			}
			else if((localtext.trim().matches(".*("+usstate+"|"+castate+")(\\.|\\s).*"))|(localtext.matches("[A-Z].[A-Z]."))|(localtext.contains("Greenland"))|(localtext.matches(".*[A-Z][a-z]\\..*")))
			{
				String disttext="";
				int closure=0;
				String[] distchunks=localtext.split(",");
				
				for(int i=0;i<distchunks.length;i++)
				{
					if(distchunks[i].length()==0)
					{
						continue;
					}
					
					if(closure==1&&!distchunks[i].contains(")"))
					{
						disttext=disttext+","+distchunks[i];
					}
					else if(closure==1&&distchunks[i].contains(")"))
					{
						disttext=disttext+","+distchunks[i];
						closure=0;
						Element dist = new Element("global_distribution");
						dist.setText(disttext);
						treatment.addContent(dist);
					}
					else if(distchunks[i].contains("(")&&!distchunks[i].contains(")"))
					{
						disttext=disttext+distchunks[i];
						closure=1;
					}
					else if(distchunks[i].trim().contains("introduced"))//introduced
					{
						Element dist = new Element("introduced");
						dist.setText(distchunks[i]);
						treatment.addContent(dist);
					}
					else if(distchunks[i].trim().matches("\\s*("+castate+")\\.?\\s*.*"))  //try this
					//else if(distchunks[i].trim().matches(".*\\s*("+castate+")\\s*.*"))
					{
						Element dist = new Element("ca_distribution");
						dist.setText(distchunks[i]);
						treatment.addContent(dist);
					}
					else if(distchunks[i].trim().matches("\\s*("+usstate+")\\.?\\s*.*")&&!distchunks[i].trim().matches(".*Pacific.*"))   //try this
					//else if(distchunks[i].trim().matches(".*\\s*("+usstate+")\\s*.*"))
					{
						Element dist = new Element("us_distribution");
						dist.setText(distchunks[i]);
						treatment.addContent(dist);
					}
					else
					{
						Element dist = new Element("global_distribution");
						dist.setText(distchunks[i]);
						treatment.addContent(dist);
					}
					
				}

			}
			else
			{
				Element habi=new Element("habitat");
				habi.setText(localtext);
				treatment.addContent(habi);
			}
		}
		
	}
		
	}

/*
 * Process the Synonyms
 */
	private void processSynonym(String text, int TaxonId, String taxname) throws Exception {
		// TODO Auto-generated method stub
		 /*  String localtext=text;
		   if(taxname.contains("family_name"))
		   {
			   System.out.println("It is a family synonym");
		   }
		   Element taxonid = new Element("TaxonIdentification");
		   taxonid.setAttribute("Status","SYNONYM");
		   //Add TaxonName
		   Element taxonname = new Element("TaxonName");
		   taxonname.setText(text);
		   taxonid.addContent(taxonname);
		   
		   treatment.addContent(taxonid);*/

		
		
		// SYNONYM SHOULD BE PROCESSED BEFORE ADDING IT TO THE treatment.
		 		String namepart="";
				String pubpart="";
				String indsyntext="";
				//String syntext = newpubli.getText();
				String syntext = text;
				String[] indsyn = syntext.split(";");
				
				
				for(int s=0; s<indsyn.length; s++)// for every synonym
				{
					namepart="";
					pubpart="";
					Element pubname = new Element("place_of_publication");
					Element newtaxid= new Element("TaxonIdentification");
					newtaxid.setAttribute("Status","SYNONYM");
					indsyntext=indsyn[s];
					int commaindex=-1;
					int genindex=100000;
					
					String patternStr = "(\\s\\d).*";
					Pattern pattern = Pattern.compile(patternStr);
					Matcher matcher = pattern.matcher(indsyntext);
					boolean matchFound = matcher.find();
					int groupStart=-1;
					if(matchFound){
						groupStart = matcher.start();
					}
					
					  
					  
					int ci_in=indsyntext.indexOf(" in ");
					int ci_based=indsyntext.indexOf(" based ");
					int ci_comma=indsyntext.indexOf(",");
					
					if(ci_in>=0&&ci_in<genindex)
					{
						genindex=ci_in;
					}
					if(ci_based>=0&&ci_based<genindex)
					{
						genindex=ci_based;
					}
					if(ci_comma>=0&&ci_comma<genindex)
					{
						genindex=ci_comma;
					}
					if(groupStart>=0&&groupStart<genindex)
					{
						genindex=groupStart;
					}
					
					
					if(genindex!=100000)
					{
					commaindex=genindex;
					}
					
					if(commaindex!=-1)
					{
						namepart=(namepart+indsyntext.substring(0, commaindex)).trim();
						String other="";
						//Code to process other_info
						//String publication=indsyntext.substring(commaindex+1, indsyntext.length()).trim();
						String publication=indsyntext.substring(commaindex+1, indsyntext.length());
						int n_commaindex=-1;
						int n_genindex=100000;
						
						int n_ci_in=publication.indexOf(" not ");
						int n_ci_based=publication.indexOf(" based ");
						if(n_ci_in>=0&&n_ci_in<n_genindex)
						{
							n_genindex=n_ci_in;
						}
						if(n_ci_based>=0&&n_ci_based<n_genindex)
						{
							n_genindex=n_ci_based;
						}
													
						if(n_genindex!=100000)
						{
						n_commaindex=n_genindex;
						}
						if(n_commaindex!=-1)
						{
							pubpart=(pubpart+publication.substring(0, n_commaindex)).trim();
							other=(other+publication.substring(n_commaindex+1, publication.length())).trim();
						}
						else
						{
							pubpart=(pubpart+indsyntext.substring(commaindex+1, indsyntext.length())).trim();
						}
						//End of Code to process other_info
						
						//process the publication part
					
						//pubpart=(pubpart+indsyntext.substring(commaindex+1, indsyntext.length())).trim();
						String[] titlechunks = new String[1];
						titlechunks=pubpart.split("[\\d].*");
						String publtitl="";
						if(titlechunks.length!=0)
						{
							publtitl=titlechunks[0];
						}
						if(publtitl.length()!=0)
						{
						Element publ_title = new Element("publication_title");
						publ_title.setText(publtitl);
						pubname.addContent(publ_title);
						}
						int inlength=0;
						if(titlechunks.length!=0)
						{
							inlength=titlechunks[0].length();
						}
						if(inlength<pubpart.length())
						{
							//String inpubl=pubpart.substring(inlength, pubpart.length()-1);
							String inpubl=pubpart.substring(inlength, pubpart.length());
							Element in_publication = new Element("place_in_publication");
							in_publication.setText(inpubl);
							pubname.addContent(in_publication);
						}
						if(other.length()!=0)
						{
							Element other_info = new Element("other_info");
							other_info.setText(other);
							pubname.addContent(other_info);
						}
					
						
						//finished processing the publication.
					}
					else
					{
						namepart=indsyntext.trim();
					}
					
					Element syn=synprocess(namepart,taxname);//Pass the name part and taxname
					List synlist=syn.getChildren();
					for(int m=0;m<synlist.size();m++)
					{
						Element synte = (Element) synlist.get(m);
						Element newsynte=(Element) synte.clone();
						newtaxid.addContent(newsynte);
					}
					
					//newtaxid.addContent(synlist);
					
					
					if(pubname.getContentSize()!=0) //to check if the publication has children.
					{
						newtaxid.addContent(pubname);
						//System.out.println();
					}
					treatment.addContent(newtaxid);
				}

	
		//treatment.addContent(newpubli);

		
	}

/*
 * Processes the references	
 */
	private void processReference(String reftext) throws Exception{
		String[] refchunks=reftext.split("<P>");
		if(refchunks.length!=2)
		{
			System.out.println(" Has keys ");
		}
		String heading=refchunks[0];
		String text=refchunks[1].replaceAll("(<(..?)>)", " ");
		Element reference = new Element("references");
		reference.setAttribute("heading",heading);
		reference.setText(text);
		furtherMarkupReference(reference);
		treatment.addContent(reference);
		
		//To add other stuff to key heading
		if(refchunks.length>2)
		{
			Element key = new Element("key");
			for(int x=2;x<refchunks.length;x++)
			{
				text=refchunks[x].replaceAll("(<(..?)>)", "").trim();
				if(text.length()!=0)
				{
					Element comname = new Element("key_heading");
					comname.setText(text);
					key.addContent(comname);
				}	
			}
			if(key.getContentSize()!=0)
			{
				keyexists=1;
				treatment.addContent(key);
			}
		}
		
		
	}
	
	/**
	 * turn
	 * <references>SELECTED REFERENCES Behnke, H.-D., C. Chang, I. J. Eifert, and T. J. Mabry. 1974. Betalains and P-type sieve-tube plastids in Petiveria and Agdestis (Phytolaccaceae). Taxon 23: 541–542. Brown, G. K. and G. S. Varadarajan. 1985. Studies in Caryophyllales I: Re-evaluation of classification of Phytolaccaceae s.l. Syst. Bot. 10: 49–63. Heimerl, A. 1934. Phytolaccaceae. In: H. G. A. Engler et al., eds. 1924+. Die natürlichen Pflanzenfamilien…, ed. 2. 26+ vols. Leipzig and Berlin. Vol. 16c, pp. 135–164. Nowicke, J. W. 1968. Palynotaxonomic study of the Phytolaccaceae. Ann. Missouri Bot. Gard. 55: 294–364. Rogers, G. K. 1985. The genera of Phytolaccaceae in the southeastern United States. J. Arnold Arbor. 66: 1–37. Thieret, J. W. 1966b. Seeds of some United States Phytolaccaceae and Aizoaceae. Sida 2: 352–360. Walter, H. P. H. 1906. Die Diagramme der Phytolaccaceen. Leipzig. [Preprinted from Bot. Jahrb. Syst. 37(suppl.): 1–57.] Walter, H. P. H. 1909. Phytolaccaceae. In: H. G. A. Engler, ed. 1900–1953. Das Pflanzenreich…. 107 vols. Berlin. Vol. 39[IV,83], pp. 1–154. Wilson, P. 1932. Petiveriaceae. In: N. L. Britton et al., eds. 1905+. North American Flora…. 47+ vols. New York. Vol. 21, pp. 257–266.</references>
	 * to
	 * <references><reference>Behnke, H.-D., C. Chang, I. J. Eifert, and T. J. Mabry. 1974. Betalains and P-type sieve-tube plastids in Petiveria and Agdestis (Phytolaccaceae). Taxon 23: 541–542. </reference> <reference>...</reference>....</references>
	 * @param ref
	 * @return
	 */
	private void furtherMarkupReference(Element ref) {
		//Element marked = new Element("references");
		String text = ref.getText();
		ref.setText("");
		//if(this.debugref) System.out.println("\nReferences text:"+text);
		Pattern p = Pattern.compile("(.*?\\d+--\\d+\\.\\]?)(\\s+[A-Z]\\w+,.*)");
		//Pattern p = Pattern.compile("(.*?\\d+–\\d+\\.\\]?)(\\s+[A-Z]\\w+,.*)");
		Matcher m = p.matcher(text);
		while(m.matches()){
			String refstring = m.group(1);
			Element refitem = new Element("reference");
			refitem.setText(refstring);
			ref.addContent(refitem);
			//if(this.debugref) System.out.println("a ref:"+refstring);
			text = m.group(2);
			m = p.matcher(text);
		}
		Element refitem = new Element("reference");
		//refitem.setText("item:"+text);
		refitem.setText(text);
		ref.addContent(refitem);
		//if(this.debugref) System.out.println("a ref:"+text);
		//ref.getParentElement().addContent(marked);
		//ref.detach();	
	}
	
/*
 * Function to cleanup the text
 */
	private String cleaninputtext(String text) throws Exception{
		String newtext=text.replaceAll("<..?>","").trim();
		return newtext;
	}

/*Function to create a new "treatment" element 
 */
   private void createtreatment() throws Exception{
		treatment = new Element("treatment");
	}
/*
 * Function to output the treatments into individual files  
 */
   private void output(String TaxonName) throws Exception {
		XMLOutputter outputter = new XMLOutputter();
		String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V2\\Transformed\\" + TaxonName + ".xml";
		Document doc = new Document(treatment);
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(file));
		outputter.output(doc, out);
	}
   /*
    * Takes care of common name and etymology 
    */
   private void processcommonname(String commonname) throws Exception {
	   Element ety= new Element("etymology");
	   Pattern p = Pattern.compile(".*(\\[.*\\]).*");
	   Matcher m = p.matcher(commonname); 
	   if(m.matches())
	   {
		   String Etymology=m.group(1);
		   ety.setText(Etymology);
		   commonname=commonname.replaceFirst("\\[.*\\]", "").trim();
	   }
	   String[] cname=commonname.split(",");
	   for(int x=0;x<cname.length;x++)
		{
			Element comname = new Element("common_name");
			comname.setText(cname[x]);
			treatment.addContent(comname);
		}
	   
	   if(ety.getText().length()!=0)
	   {
		   treatment.addContent(ety);
	   }
   }
   
   /*
    * Process the author names
    */
   private void processAuthor(String authname) throws Exception{
	   String[] auth=authname.split("(<BR>|&)");
	   for(int x=0;x<auth.length;x++)
		{
		   if(auth[x].contains("family"))
		   {
			   Element comname = new Element("common_name");
			   comname.setText(auth[x]);
			   treatment.addContent(comname);
		   }
		   else
		   {
			   Element auname = new Element("author");
			   auname.setText(auth[x]);
			   treatment.addContent(auname);  
		   }
			
		}
	   
   }
   
   
   
   /*
    * Takes care of the taxonname, authority and publication
    */
   private void processName(String TaxonName,int TaxonId,int RankId) throws Exception{
	   Element taxonid = new Element("TaxonIdentification");
	   taxonid.setAttribute("Status","ACCEPTED");
	   String authorityname="";
	   //Add TaxonName
	  /* Element taxonname = new Element("TaxonName");
	   taxonname.setText(TaxonName);
	   taxonid.addContent(taxonname);*/
	   //Get authority if it exists
	   String authSQL="select * from Authority where AuthorityId="+rs.getInt(4);//FloraAuthorityId is 4th column which is an integer in the table FloraTaxon
	   Statement authstmt= con.createStatement();
	   ResultSet auth = null;
	   auth = authstmt.executeQuery(authSQL);
	   if(auth.next())
	   {
			authorityname+=auth.getString(2);//Authority name is the second column of the Authority table
			authorityname=authorityname.replace("null", "");
	   }
	   if((RankId==10030)|(RankId==10015)) //It is a family
	   {
		   taxname="family_name";
		   Element taxonname = new Element("family_name");
		   taxonname.setText(TaxonName);
		   taxonid.addContent(taxonname);
		   if(authorityname.length()!=0)
		   {
			   Element authname = new Element("family_authority");
			   authname.setText(authorityname);
			   taxonid.addContent(authname);
		   }
	   }
	   else if((RankId==10040)) //It is a Genus
	   {
		   taxname="genus_name";
		   Element taxonname = new Element("genus_name");
		   taxonname.setText(TaxonName);
		   taxonid.addContent(taxonname);
		   if(authorityname.length()!=0)
		   {
			   Element authname = new Element("genus_authority");
			   authname.setText(authorityname);
			   taxonid.addContent(authname);
		   }
	   }
	   else if((RankId==10042)) //It is a SubGenus
	   {
		   taxname="subgenus_name";
		   String[] subgchunks = TaxonName.split("Subg.");
		   Element taxonname = new Element("genus_name");
		   taxonname.setText(subgchunks[0].trim());
		   taxonid.addContent(taxonname);
		   Element subgtaxonname = new Element("subgenus_name");
		   subgtaxonname.setText(subgchunks[1].trim());
		   taxonid.addContent(subgtaxonname);
		   if(authorityname.length()!=0)
		   {
			   Element authname = new Element("subgenus_authority");
			   authname.setText(authorityname);
			   taxonid.addContent(authname);
		   }
	   }
	   else if((RankId==10050)) //It is a Section
	   {
		   taxname="section_name";
		   String[] subgchunks = TaxonName.split("Sect.");
		   Element taxonname = new Element("genus_name");
		   taxonname.setText(subgchunks[0].trim());
		   taxonid.addContent(taxonname);
		   Element secttaxonname = new Element("section_name");
		   secttaxonname.setText(subgchunks[1].trim());
		   taxonid.addContent(secttaxonname);
		   if(authorityname.length()!=0)
		   {
			   Element authname = new Element("section_authority");
			   authname.setText(authorityname);
			   taxonid.addContent(authname);
		   }
	   }
	   else if((RankId==10060)) //It is a Species
	   {
		   taxname="species_name";
		   String[] subgchunks = TaxonName.trim().split("\\s+");
		   Element taxonname = new Element("genus_name");
		   taxonname.setText(subgchunks[0].trim());
		   taxonid.addContent(taxonname);
		   Element sptaxonname = new Element("species_name");
		   sptaxonname.setText(subgchunks[1].trim());
		   taxonid.addContent(sptaxonname);
		   if(authorityname.length()!=0)
		   {
			   Element authname = new Element("species_authority");
			   authname.setText(authorityname);
			   taxonid.addContent(authname);
		   }
	   }
	   else if((RankId==10062)) //It is a SubSpecies
	   {
		   taxname="subspecies_name";
		   String[] subgchunks = TaxonName.trim().split("\\s+");
		   Element taxonname = new Element("genus_name");
		   taxonname.setText(subgchunks[0].trim());
		   taxonid.addContent(taxonname);
		   Element sptaxonname = new Element("species_name");
		   sptaxonname.setText(subgchunks[1].trim());
		   taxonid.addContent(sptaxonname);
		   Element subsptaxonname = new Element("subspecies_name");
		   subsptaxonname.setText(subgchunks[3].trim());
		   taxonid.addContent(subsptaxonname);
		   if(authorityname.length()!=0)
		   {
			   Element authname = new Element("subspecies_authority");
			   authname.setText(authorityname);
			   taxonid.addContent(authname);
		   }
	   }
	   else if((RankId==10065)) //It is a Variety
	   {
		   taxname="variety_name";
		   String[] subgchunks = TaxonName.trim().split("\\s+");
		   Element taxonname = new Element("genus_name");
		   taxonname.setText(subgchunks[0].trim());
		   taxonid.addContent(taxonname);
		   Element sptaxonname = new Element("species_name");
		   sptaxonname.setText(subgchunks[1].trim());
		   taxonid.addContent(sptaxonname);
		   Element vartaxonname = new Element("variety_name");
		   vartaxonname.setText(subgchunks[3].trim());
		   taxonid.addContent(vartaxonname);
		   if(authorityname.length()!=0)
		   {
			   Element authname = new Element("variety_authority");
			   authname.setText(authorityname);
			   taxonid.addContent(authname);
		   }
	   }
	   else if((RankId==10085)) //It is a Species -- Asplenium ×biscayneanum
	   {
		   taxname="species_name";
		   String[] subgchunks = TaxonName.trim().split("\\s+");
		   Element taxonname = new Element("genus_name");
		   taxonname.setText(subgchunks[0].trim());
		   taxonid.addContent(taxonname);
		   Element sptaxonname = new Element("species_name");
		   sptaxonname.setText(subgchunks[1].trim());
		   taxonid.addContent(sptaxonname);
		   if(authorityname.length()!=0)
		   {
			   Element authname = new Element("species_authority");
			   authname.setText(authorityname);
			   taxonid.addContent(authname);
		   }
	   }
	   else
	   {
		   System.out.println("Unhandled Taxon Name");
	   }
	   //Place_of_Publication
	   Element pop = new Element("place_of_publication");
	   //publication_title
	   String pubtitle="";
	   if(rs.getString(17).length()!=0)
	   {
		   pubtitle+=rs.getString(17)+",";
	   }
	   String pubtSQL="select * from PubTitle where PubTitleId="+rs.getInt(18);//pubtitleid is the 18th column of Florataxon
	   Statement pubtstmt= con.createStatement();
	   ResultSet pubt = null;
	   pubt = pubtstmt.executeQuery(pubtSQL);
	   if(pubt.next())
	   {
		   pubtitle+=pubt.getString(2);
	   }
	   if(!pubtitle.isEmpty())
	   {
		   Element pubti = new Element("publication_title");
		   pubti.setText(pubtitle);
		   pop.addContent(pubti);
	   }
	   else
	   {
		   //System.out.println("No Publication Title");
	   }
	   //place_in_publication
	   String pip="";
	   pip+=rs.getString(19); //19th column is pubpage
	   if(rs.getString(20).trim().length()!=0)
	   {
		   if(pip.endsWith("."))
		   {
			   pip+=" "+rs.getString(20);
		   }
		   else
		   {
			   pip+=". "+rs.getString(20);
		   }
	   }
	   if(!pip.isEmpty())
	   {
		   Element plip = new Element("place_in_publication");
		   plip.setText(pip);
		   pop.addContent(plip);
	   }
	   else
	   {
		   //System.out.println("No place in publication");
	   }
	   
	   if(pop.getContentSize()!=0)
	   {
		   taxonid.addContent(pop);
	   }	 
	   treatment.addContent(taxonid);
   }
   /*
    * Clean text used in synprocess
    */
   private String cleantext(String namepart) throws Exception{
		String result="";
		String[] chunks=namepart.split("\\s");
		for(int j=0;j<chunks.length;j++)
		{
			if(chunks[j].matches("\\."))
			{
				result=result+chunks[j];
			}
			else
			{
				result=result+" "+chunks[j];
			}	
		}
		result=result.trim();
		String inter="";
		String[] newchunks=result.split("\\s");
		for(int m=0;m<newchunks.length;m++)
		{
			if(m==0&&newchunks[m].matches("\\w"))
			{
				inter=inter+newchunks[m];
				m++;
				inter=inter+newchunks[m];
			}
			else if(newchunks[m].matches("\\w|\\)"))
			{
				inter=inter+newchunks[m];
			}
			else if(newchunks[m].matches("×"))
			{
				inter=inter+" "+newchunks[m];
				m++;
				inter=inter+newchunks[m];
			}
			else if(newchunks[m].matches("§"))
			{

			}
			else
			{
				inter=inter+" "+newchunks[m];
			}
		}
		result=inter.trim();
		return result;
	}
   
   
   
   /*
    * Process name of synonyms
    */
   private Element synprocess(String namepart, String taxname) throws Exception{
		Element syn=new Element("synonym");
		String text=cleantext(namepart);
		if(text.contains("subtribe"))//Synonym is a subtribe
		{
			int k;
			String spauth="";
			String sectauth="";
			String subgauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("family_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].matches("\\s*\\(tribe\\s*")||var[k].contains("subtribe"))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("family_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			if(var[k].matches("\\s*\\(tribe\\s*"))
			{
			k++;
			Element subfm= new Element("tribe_name");
			subfm.setText(var[k].replaceFirst("(\\W)$", ""));
			syn.addContent(subfm);
			k++;
			while((k<var.length)&&!(var[k].contains("subtribe")))
			{
				subgauth=subgauth+var[k]+" ";
				k++;
			}
			subgauth=subgauth.trim();
			if(subgauth.length()!=0)
			{
				Element subauth= new Element("tribe_authority");
				subauth.setText(subgauth);
				syn.addContent(subauth);
			}
			if(var[k].contains("subtribe"))
			{
				k++;
				Element sect= new Element("subtribe_name");
				sect.setText(var[k]);
				syn.addContent(sect);
				k++;
				while(k<var.length)
				{
					sectauth=sectauth+var[k]+" ";
					k++;
				}
				sectauth=sectauth.trim();
				if(sectauth.length()!=0)
				{
				Element subat= new Element("subtribe_authority");
				subat.setText(sectauth);
				syn.addContent(subat);
				}
			}
			else
			{
				System.out.println("Problem in subtribe");
			}
			}
			else
			{
			k++;
			Element sect= new Element("section_name");
			sect.setText(var[k]);
			syn.addContent(sect);
			k++;
			while(k<var.length)
			{
				sectauth=sectauth+var[k]+" ";
				k++;
			}
			sectauth=sectauth.trim();
			if(sectauth.length()!=0)
			{
				Element subat= new Element("section_authority");
				subat.setText(sectauth);
				syn.addContent(subat);
			}
			}
		}
		else if(text.contains("tribe"))//Synonym is a tribe
		{
			int k;
			String spauth="";
			String sectauth="";
			String subgauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("family_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].contains("subfam.")||var[k].contains("tribe"))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("family_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			if(var[k].contains("subfam."))
			{
			k++;
			Element subfm= new Element("subfamily_name");
			subfm.setText(var[k].replaceFirst("(\\W)$", ""));
			syn.addContent(subfm);
			k++;
			while((k<var.length)&&!(var[k].contains("tribe")))
			{
				subgauth=subgauth+var[k]+" ";
				k++;
			}
			subgauth=subgauth.trim();
			if(subgauth.length()!=0)
			{
				Element subauth= new Element("subfamily_authority");
				subauth.setText(subgauth);
				syn.addContent(subauth);
			}
			if(var[k].contains("tribe"))
			{
				k++;
				Element sect= new Element("tribe_name");
				sect.setText(var[k]);
				syn.addContent(sect);
				k++;
				while(k<var.length)
				{
					sectauth=sectauth+var[k]+" ";
					k++;
				}
				sectauth=sectauth.trim();
				if(sectauth.length()!=0)
				{
				Element subat= new Element("tribe_authority");
				subat.setText(sectauth);
				syn.addContent(subat);
				}
			}
			else
			{
				System.out.println("Problem in subtribe");
			}
			}
			else
			{
			k++;
			Element sect= new Element("section_name");
			sect.setText(var[k]);
			syn.addContent(sect);
			k++;
			while(k<var.length)
			{
				sectauth=sectauth+var[k]+" ";
				k++;
			}
			sectauth=sectauth.trim();
			if(sectauth.length()!=0)
			{
				Element subat= new Element("section_authority");
				subat.setText(sectauth);
				syn.addContent(subat);
			}
			}
		}
		else if(text.contains("subfam."))// SUBFAMILY
		{	
			int k;
			String famauth="";
			String subfamauth="";
			String[] family= text.split("\\s");
			Element newele= new Element("family_name");
			newele.setText(family[0]);
			syn.addContent(newele);
			for(k=1;k<family.length;k++)
			{
				if(family[k].contains("subfam."))
				{
					break;
				}
				else
				{
					famauth+=family[k]+" ";
				}
			}
			famauth=famauth.trim();
			if(famauth.length()!=0)
			{
			Element famat= new Element("family_authority");
			famat.setText(famauth);
			syn.addContent(famat);
			}
			k++;
			Element subfm= new Element("subfamily_name");
			subfm.setText(family[k]);
			syn.addContent(subfm);
			k++;
			while(k<family.length)
			{
				subfamauth+=family[k]+" ";
				k++;
			}
			subfamauth=subfamauth.trim();
			if(subfamauth.length()!=0)
			{
			Element subfamat= new Element("subfamily_authority");
			subfamat.setText(subfamauth);
			syn.addContent(subfamat);	
			}
		}
		
		else if(text.contains("var."))//Synonym is a variety
		{
			int k;
			String spauth="";
			String varauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			//if(!var[1].contains("var."))
			//{
			Element spele= new Element("species_name");
			spele.setText(var[1]);
			syn.addContent(spele);
			//}
			for(k=2;k<var.length;k++)
			{
				if(var[k].contains("var."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("species_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			k++;
			Element subfm= new Element("variety_name");
			subfm.setText(var[k]);
			syn.addContent(subfm);
			k++;
			while(k<var.length)
			{
				varauth+=var[k]+" ";
				k++;
			}
			varauth=varauth.trim();
			if(varauth.length()!=0)
			{
			Element subfamat= new Element("variety_authority");
			subfamat.setText(varauth);
			syn.addContent(subfamat);	
			}
		}
		else if(text.contains("subsp."))//Synonym is a subspecies
		{
			int k;
			String spauth="";
			String varauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			Element spele= new Element("species_name");
			spele.setText(var[1]);
			syn.addContent(spele);
			for(k=2;k<var.length;k++)
			{
				if(var[k].contains("subsp."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("species_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			k++;
			Element subfm= new Element("subspecies_name");
			subfm.setText(var[k]);
			syn.addContent(subfm);
			k++;
			while(k<var.length)
			{
				varauth+=var[k]+" ";
				k++;
			}
			varauth=varauth.trim();
			if(varauth.length()!=0)
			{
			Element subfamat= new Element("subspecies_authority");
			subfamat.setText(varauth);
			syn.addContent(subfamat);	
			}
		}
		
		else if(text.contains("ser."))//SERIES NAME
		{
			int k;
			String spauth="";
			String sectauth="";
			String subgauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].contains("subg.")||var[k].contains("sect.")||var[k].contains("ser."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("genus_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			if(var[k].contains("subg."))
			{
			k++;
			Element subfm= new Element("subgenus_name");
			subfm.setText(var[k].replaceFirst("(\\W)$", ""));
			syn.addContent(subfm);
			k++;
			while((k<var.length)&&!(var[k].contains("sect.")))
			{
				subgauth=subgauth+var[k]+" ";
				k++;
			}
			subgauth=subgauth.trim();
			subgauth=subgauth.replaceFirst("(\\W)$", "");
			if(subgauth.length()!=0)
			{
				Element subauth= new Element("subgenus_authority");
				subauth.setText(subgauth);
				syn.addContent(subauth);
			}
			if(var[k].contains("sect."))
			{
				k++;
				Element sect= new Element("section_name");
				sect.setText(var[k]);
				syn.addContent(sect);
				k++;
				while(k<var.length&&!(var[k].contains("ser.")))
				{
					sectauth=sectauth+var[k]+" ";
					k++;
				}
				sectauth=sectauth.trim();
				sectauth=sectauth.replaceFirst("(\\W)$", "");
				if(sectauth.length()!=0)
				{
				Element subat= new Element("section_authority");
				subat.setText(sectauth);
				syn.addContent(subat);
				}
				if(var[k].contains("ser."))
				{
					k++;
					Element ser= new Element("series_name");
					ser.setText(var[k]);
					syn.addContent(ser);
					k++;
					while(k<var.length)
					{
						sectauth=sectauth+var[k]+" ";
						k++;
					}
					sectauth=sectauth.trim();
					sectauth=sectauth.replaceFirst("(\\W)$", "");
					if(sectauth.length()!=0)
					{
					Element subat= new Element("series_authority");
					subat.setText(sectauth);
					syn.addContent(subat);
					}
				}
			}
			else
			{
				System.out.println("Problem in section");
			}
			}
			else
			{
			k++;
			Element sect= new Element("series_name");
			sect.setText(var[k]);
			syn.addContent(sect);
			k++;
			while((k<var.length))
			{
				sectauth=sectauth+var[k]+" ";
				k++;
			}
			sectauth=sectauth.trim();
			if(sectauth.length()!=0)
			{
				Element subat= new Element("series_authority");
				subat.setText(sectauth);
				syn.addContent(subat);
			}
			}
			
		}
		else if(text.contains("subsect."))//Subsection
		{
			int k;
			String spauth="";
			String varauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].contains("subsect."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("genus_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			k++;
			Element subfm= new Element("subsection_name");
			subfm.setText(var[k]);
			syn.addContent(subfm);
			k++;
			while(k<var.length)
			{
				varauth+=var[k]+" ";
				k++;
			}
			varauth=varauth.trim();
			if(varauth.length()!=0)
			{
			Element subfamat= new Element("subsection_authority");
			subfamat.setText(varauth);
			syn.addContent(subfamat);	
			}
		}
		else if(text.contains("sect."))//Section
		{
			int k;
			String spauth="";
			String sectauth="";
			String subgauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].contains("subg.")||var[k].contains("sect."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("genus_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			if(var[k].contains("subg."))
			{
			k++;
			Element subfm= new Element("subgenus_name");
			subfm.setText(var[k].replaceFirst("(\\W)$", ""));
			syn.addContent(subfm);
			k++;
			while((k<var.length)&&!(var[k].contains("sect.")))
			{
				subgauth=subgauth+var[k]+" ";
				k++;
			}
			subgauth=subgauth.trim();
			if(subgauth.length()!=0)
			{
				Element subauth= new Element("subgenus_authority");
				subauth.setText(subgauth);
				syn.addContent(subauth);
			}
			if(var[k].contains("sect."))
			{
				k++;
				Element sect= new Element("section_name");
				sect.setText(var[k]);
				syn.addContent(sect);
				k++;
				while(k<var.length)
				{
					sectauth=sectauth+var[k]+" ";
					k++;
				}
				sectauth=sectauth.trim();
				if(sectauth.length()!=0)
				{
				Element subat= new Element("section_authority");
				subat.setText(sectauth);
				syn.addContent(subat);
				}
			}
			else
			{
				System.out.println("Problem in section");
			}
			}
			else
			{
			k++;
			Element sect= new Element("section_name");
			sect.setText(var[k]);
			syn.addContent(sect);
			k++;
			while(k<var.length)
			{
				sectauth=sectauth+var[k]+" ";
				k++;
			}
			sectauth=sectauth.trim();
			if(sectauth.length()!=0)
			{
				Element subat= new Element("section_authority");
				subat.setText(sectauth);
				syn.addContent(subat);
			}
			}
		}
		else if(text.contains("subg."))//Subgenus
		{
			int k;
			String spauth="";
			String sectauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].contains("subg."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("genus_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			k++;
			Element sect= new Element("subgenus_name");
			sect.setText(var[k].replaceFirst("(\\W)$", ""));
			syn.addContent(sect);
			k++;
			while(k<var.length)
			{
				sectauth=sectauth+var[k]+" ";
				k++;
			}
			sectauth=sectauth.trim();
			if(sectauth.length()!=0)
			{
			Element subat= new Element("subgenus_authority");
			subat.setText(sectauth);
			syn.addContent(subat);		
			}
		}
		else if(taxname.matches("species_name"))//Synonym is a species name
		{
			int k;
			String spauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			Element spele= new Element("species_name");
			spele.setText(var[1]);
			syn.addContent(spele);
			for(k=2;k<var.length;k++)
			{
					spauth+=var[k]+" ";
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("species_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			
		}
		else if(taxname.matches("genus_name"))//genus name
		{
			int k;
			int unrank=0;
			String spauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(!var[k].matches("\\[unranked\\]"))
					spauth+=var[k]+" ";
				else
				{
					unrank=1;
					System.out.println("Unranked element in synonym");
					break;
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("genus_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			if(unrank==1)
			{
				k++;
				Element urname= new Element("unranked_epithet_name");
				urname.setText(var[k]);
				syn.addContent(urname);
				String unauth="";
				k++;
				while(k<var.length)
				{
					unauth=unauth+var[k]+" ";
					k++;
				}
				unauth=unauth.trim();
				if(unauth.length()!=0)
				{
				Element unat= new Element("unranked_epithet_authority");
				unat.setText(unauth);
				syn.addContent(unat);
				}
				
			}
		}

		else if(taxname.matches("family_name")) // if it is a family name
		{
			String famauth="";
			String[] Chunks = text.split("-");
			text = Chunks[0];
			String[] family= text.split("\\s");
			Element newele= new Element("family_name");
			newele.setText(family[0]);
			syn.addContent(newele);
			for(int k=1;k<family.length;k++)
			{
				famauth+=family[k]+" ";
			}
			famauth=famauth.trim();
			if(famauth.length()!=0)
			{
				Element famat= new Element("family_authority");
				famat.setText(famauth);
				syn.addContent(famat);
			}
			
			/*for(int j=1; j<Chunks.length;j++)
			{
				Comtext+=Chunks[j]+"-";
			}
			Comtext=Comtext.replaceFirst("-$", "");	*/
			
		}
		
		else
		{
			System.out.println("Check your synonym");
			//if(text.matches("(^[A-Z](\\w|\\.|ï)+\\s+){1}(([a-z]|×)(\\w|-)+\\s*){1}.*"))//species name
			if(text.matches("(^[A-Z](\\S)+\\s+){1}(([a-z]|×)(\\S)+\\s*){1}.*"))//species name
			{
				int k;
				String spauth="";
				String[] var= text.split("\\s");
				Element newele= new Element("genus_name");
				newele.setText(var[0]);
				syn.addContent(newele);
				Element spele= new Element("species_name");
				spele.setText(var[1]);
				syn.addContent(spele);
				for(k=2;k<var.length;k++)
				{
						spauth+=var[k]+" ";
				}
				spauth=spauth.trim();
				if(spauth.length()!=0)
				{
				Element spat= new Element("species_authority");
				spat.setText(spauth);
				syn.addContent(spat);
				}
			}
			/*else if(fnamedetecter==1) // if it is a genus name
			{
				
				String newtext= text;
				String famauth="";
				String[] Chunks = text.split("-");
				text = Chunks[0];
				String[] family= text.split("\\s");
				Element newele= new Element("family_name");
				newele.setText(family[0]);
				syn.addContent(newele);
				for(int k=1;k<family.length;k++)
				{
					famauth+=family[k]+" ";
				}
				famauth=famauth.trim();
				if(famauth.length()!=0)
				{
					Element famat= new Element("family_authority");
					famat.setText(famauth);
					syn.addContent(famat);
				}

				
			}*/
			//else if(text.matches("(^[A-Z](\\w|\\.|ï)+\\s+){1}((\\[.*\\])|(\\(.*\\)))?\\s*([A-Z](\\w|\\.)+).*"))//genus name
			//else if(text.matches("(^[A-Z](\\S)+\\s+){1}((\\[.*\\])|(\\(.*\\)))?\\s*([A-Z](\\S)+).*"))//genus name
			else if(text.matches("(^[A-Z](\\S)+\\s+){1}((\\[.*\\])|(\\(.*\\)))?\\s*(([A-Z]|Ö|Á)(\\S)+).*"))//genus name
			{
				int k;
				int unrank=0;
				String spauth="";
				String[] var= text.split("\\s");
				Element newele= new Element("genus_name");
				newele.setText(var[0]);
				syn.addContent(newele);
				for(k=1;k<var.length;k++)
				{
					if(!var[k].matches("\\[unranked\\]"))
						spauth+=var[k]+" ";
					else
					{
						unrank=1;
						System.out.println("Unranked element in synonym");
						break;
					}
				}
				spauth=spauth.trim();
				if(spauth.length()!=0)
				{
				Element spat= new Element("genus_authority");
				spat.setText(spauth);
				syn.addContent(spat);
				}
				if(unrank==1)
				{
					k++;
					Element urname= new Element("unranked_epithet_name");
					urname.setText(var[k]);
					syn.addContent(urname);
					String unauth="";
					k++;
					while(k<var.length)
					{
						unauth=unauth+var[k]+" ";
						k++;
					}
					unauth=unauth.trim();
					if(unauth.length()!=0)
					{
					Element unat= new Element("unranked_epithet_authority");
					unat.setText(unauth);
					syn.addContent(unat);
					}
					
				}
			}

			else
			{
				System.out.println("Missed Chunk from Synonym");
				System.out.println(text);
				
			}
			
			
		}
	
		return syn;
	}
   
   
   
   /*
    *  "C. lanceolatus"  - will anyone know what genus "C." refers to?  
    *  In this case, knowing the FNA format (within the synonymy, after the genus is spelled out once, 
    *  the next, adjacent, occurrence of that genus can be abbreviated by the first letter)  
    *  means that this "C." refers to Carduus, and not Cirsium as one might be expecting since
    *   we are dealing with the treatment of Cirsium.  
    */
   private void expandAbbrNames() {
       try{
           List <Element> synonyms = synPath.selectNodes(treatment);
           String lastfullname = "";
           for(Element synonym: synonyms){
               Element genus = synonym.getChild("genus_name");
               if(genus ==null) continue;
               String genusname = genus.getTextTrim();
               if(genusname.length()>2) lastfullname = genusname;
               else{
                   Matcher m = abbrgenus.matcher(genusname);
                   if(m.matches() && lastfullname.startsWith(genusname.replaceFirst("\\.$", ""))) genus.setText(lastfullname);
               }
           }
       }catch(Exception e){
           StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
       }
       
   }

}