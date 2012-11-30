package fna.parsing;


import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import com.jcraft.jsch.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class UploadTerms2OTO{
	
	private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(UploadTerms2OTO.class);  

	private static String dumpfolder; 
	private static String dataprefix;

	
	public static DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
    public static Calendar cal = Calendar.getInstance();
    
    private boolean standalone = true;
    /**
     * 
     * @param dataprefix: must the the dataprefix set in the configuration tab of CharaParser
     */
	public UploadTerms2OTO(String thedataprefix){
		dataprefix = thedataprefix;
		if(standalone)
			dumpfolder = "C:\\Documents and Settings\\Hong Updates\\Desktop\\BiosemanticsWorkshopTest\\TreatisePartO\\target\\"; //must have the trailing \\ !
		else 
			dumpfolder = Registry.TargetDirectory;
		
		dumpFiles(dataprefix);
		createTextFile(dataprefix);
		
		String textfile = dataprefix+"_sqlscript.txt";
		scpTo(textfile);
		String tcategory = dataprefix+"_term_category_dump.sql";
		scpTo(tcategory);
		String tsentence = dataprefix+"_sentence_dump.sql";
		scpTo(tsentence);
		
	    String backup = "mysqldump -u termsuser -ptermspassword markedupdatasets > markedupdatasets_bak_"+dateFormat.format(cal.getTime())+".sql";
		execute(backup);
		
		String excom = "mysql -u termsuser -ptermspassword < "+textfile+" 2> "+dataprefix+"_sqllog.txt";//write output to log file
		execute(excom);
	}
    /*
     * Used to create the dumps which need to be uploaded to the website
     */
    
    public static void dumpFiles(String dataprefix) {
      try {
    	  Runtime rt = Runtime.getRuntime();
    	  String term_category_command = "";
    	  String sentence_command = "";
    	  //rt.exec("/C:/Program Files/MySQL/MySQL Server 5.0/bin/mysqldump -u [username] -p[password]  [ databaseName] -r  /D:/databasebackup/backup.sql");
    	  //rt.exec("C:\\Program Files\\MySQL\\MySQL Server 5.5\\bin\\mysqldump -u termsuser -ptermspassword  markedupdatasets treatise_o_term_category -r  C:\\Users\\mohankrishna89\\Documents\\dumps\\newdump.sql");
    	  //if(standalone)
    	  //{
    		  term_category_command = "mysqldump -u termsuser -ptermspassword  markedupdatasets "+dataprefix+"_term_category -r \""+dumpfolder+dataprefix+"_term_category_dump.sql\"";
    		  sentence_command = "mysqldump -u termsuser -ptermspassword  markedupdatasets "+dataprefix+"_sentence -r \""+dumpfolder+dataprefix+"_sentence_dump.sql\"";

    	  //}
    	  //else
    	  //{
    		  //term_category_command = "C:\\Program Files\\MySQL\\MySQL Server 5.5\\bin\\mysqldump -u termsuser -ptermspassword  markedupdatasets "+dataprefix+"_term_category -r "+(String) Registry.TargetDirectory+"\\"+dataprefix+"_term_category_dump.sql";
        	  //sentence_command = "C:\\Program Files\\MySQL\\MySQL Server 5.5\\bin\\mysqldump -u termsuser -ptermspassword  markedupdatasets "+dataprefix+"_sentence -r "+(String) Registry.TargetDirectory+"\\"+dataprefix+"_sentence_dump.sql";	  
    	//	  term_category_command = "mysqldump -u termsuser -ptermspassword  markedupdatasets "+dataprefix+"_term_category -r "+(String) Registry.TargetDirectory+"\\"+dataprefix+"_term_category_dump.sql";
        //	  sentence_command = "mysqldump -u termsuser -ptermspassword  markedupdatasets "+dataprefix+"_sentence -r "+(String) Registry.TargetDirectory+"\\"+dataprefix+"_sentence_dump.sql";	  
    	  //}
    	  
    	  rt.exec(term_category_command);
    	  rt.exec(sentence_command);
    	 } 
    	 catch(Exception e) {
    		 StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
    	 }
    	}
    	
    	
    	/*
    	 * NOTE: Always backup the database before running this scriptfile
    	 * create a text file with the following commands in it. 
    	 * datasetprefix is the prefix for the set of the new tables to be created
    	 */
    	public static void createTextFile(String dataprefix)
    	{
    		try{
    			/*DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    		    Calendar cal = Calendar.getInstance();*/
    			System.out.println("Dumping files of dataprefix: "+dataprefix);
    			String datasetprefix = dataprefix+"_"+dateFormat.format(cal.getTime());
    			String filename="sqlscript";
    			//String directory="";
    			/*if(standalone){
    				directory = directory+""+dumpfolder;
    			}
    			else
    			{
    				directory=(String) Registry.TargetDirectory;
    			}*/
    			//FileWriter fstream = new FileWriter(directory+"\\"+dataprefix+"_"+filename+".txt");
    			FileWriter fstream = new FileWriter(dumpfolder+dataprefix+"_"+filename+".txt");
    			BufferedWriter out = new BufferedWriter(fstream);
    			//store all the commands into a text file
    			String[] commands=new String[28];
    			//import data: _sentence, _term_category
    			
    			/*commands[0] = "drop database if exists tempdatabase;";
    			commands[1] = "create database if not exists tempdatabase;";
    			commands[2] = "use tempdatabase;";
    			commands[3] = "source ~/"+dataprefix+"_term_category_dump.sql;";
    			commands[4] = "source ~/"+dataprefix+"_sentence_dump.sql;";
    			commands[5] = "use markedupdatasets;";*/
    			
    			commands[0] = "use markedupdatasets;";
    			commands[1] = "";
    			commands[2] = "";
    			commands[3] = "source ~/"+dataprefix+"_term_category_dump.sql;";
    			commands[4] = "source ~/"+dataprefix+"_sentence_dump.sql;";
    			commands[5] = "";
    			
    			
    			
    			
    			
    			//insert dataset prefix
    			commands[6] = "insert into datasetprefix (prefix) value ('"+datasetprefix+"');";
    			//table _comments
    			commands[7] = "create table "+datasetprefix+"_comments like fna_gloss_comments;";
    			 
    			//table _review_history
    			commands[8] = "create table "+datasetprefix+"_review_history like fna_gloss_review_history;";
    			//page Group Terms
    			//categories
    			commands[9] = "create table "+datasetprefix+"_categories like categories;";
    			//get default records from table categories
    			commands[10] = "insert "+datasetprefix+"_categories select * from categories;";
    			commands[11] = "create table "+datasetprefix+"_web_grouped_terms like fna_gloss_web_grouped_terms;";
    			//commands[12] = "create table "+datasetprefix+"_finalized_terms like fna_gloss_finalized_terms;";
    			commands[12] = "create table "+datasetprefix+"_confirmed_category like fna_gloss_confirmed_category;";
    			commands[13] = "create table "+datasetprefix+"_user_terms_decisions like fna_gloss_user_terms_decisions;";
    			commands[14] = "create table "+datasetprefix+"_user_terms_relations like fna_gloss_user_terms_relations;";
    			commands[15] = "create table "+datasetprefix+"_sentence like fna_gloss_sentence;";
    			
    			//page Hierarchy Tree
    			commands[16] = "create table "+datasetprefix+"_web_tags like fna_gloss_web_tags;";
    			//commands[17] = "create table "+datasetprefix+"_finalized_tags like fna_gloss_finalized_tags;";
    			commands[17] = "create table "+datasetprefix+"_confirmed_paths like fna_gloss_confirmed_paths;";
    			commands[18] = "create table "+datasetprefix+"_user_tags_decisions like fna_gloss_user_tags_decisions;";
    			
    			//page Orders
    			//commands[19] = "create table "+datasetprefix+"_finalized_orders like fna_gloss_finalized_orders;";
    			commands[19] = "create table "+datasetprefix+"_confirmed_orders like fna_gloss_confirmed_orders;";
    			commands[20] = "create table "+datasetprefix+"_user_orders_decisions like fna_gloss_user_orders_decisions;";
    			commands[21] = "create table "+datasetprefix+"_web_orders like fna_gloss_web_orders;";
    			commands[22] = "create table "+datasetprefix+"_web_orders_terms like fna_gloss_web_orders_terms;";
    			
    			//Insert terms into table _web_grouped_terms
    			commands[23] = "insert into "+datasetprefix+"_web_grouped_terms(term, groupid) select distinct term, 1 as groupid from "+dataprefix+"_term_category;";
    			
    			//Insert sentence
    			commands[24] = "insert into "+datasetprefix+"_sentence(sentid, source, sentence, originalsent, lead, status, tag, modifier, charsegment) " +
    					"select sentid, source, sentence, originalsent, lead, status, tag, modifier, charsegment from "+dataprefix+"_sentence;";
    			
    			//Generate original decisions: some terms already have category information in source table _term_category 
    			commands[25] = "insert into "+datasetprefix+"_user_terms_decisions(term, decision, userid, decisiondate, groupid) " +
    					"select distinct term, category, 32 as userid, sysdate(), 1 as groupid from "+dataprefix+"_term_category where category in " +
    							"(select category from treatise_categories);";
    			//update a column with empty string. This is because the default value is null. we need empty string 
    			commands[26] = "update "+datasetprefix+"_user_terms_decisions set relatedTerms = \"\";";
    			commands[27] = "";
    			
    			
    			//write the commands into the text file.
    			for(int i=0;i<commands.length;i++){
    				/*String text = commands[i];
    				out.append(text);
    				out.newLine();*/
    				out.write(commands[i]);
    				out.newLine();
    			}
    			
    			out.close();
    		}
    		catch(Exception e){
    			System.err.println("Error in creating and writing to a text file: " + e.getMessage());
    			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());

    		}
    	}
    	
    	/**
    	 * copy from a remote server to local machine
    	 * 
    	 */
    		  public static void scpFrom (String rfile, String lfile){
    		    FileOutputStream fos=null;
    		    try{
    		    	//hongcui@:
    		      String user="hongcui";
    		      String host="biosemantics.arizona.edu";
    		      

    		      String prefix=null;
    		      if(new File(lfile).isDirectory()){
    		        prefix=lfile+File.separator;
    		      }

    		      JSch jsch=new JSch();
    		      Session session=jsch.getSession(user, host, 22);
        	      java.util.Properties config = new java.util.Properties(); 
        	      config.put("StrictHostKeyChecking", "no");
        	      session.setConfig(config);
    		      session.setPassword("2t2gPTfz");
        	      session.connect();

    		      // exec 'scp -f rfile' remotely
    		      String command="scp -f "+rfile;
    		      Channel channel=session.openChannel("exec");
    		      ((ChannelExec)channel).setCommand(command);

    		      // get I/O streams for remote scp
    		      OutputStream out=channel.getOutputStream();
    		      InputStream in=channel.getInputStream();

    		      channel.connect();

    		      byte[] buf=new byte[1024];

    		      // send '\0'
    		      buf[0]=0; out.write(buf, 0, 1); out.flush();

    		      while(true){
    			int c=checkAck(in);
    		        if(c!='C'){
    			  break;
    			}

    		        // read '0644 '
    		        in.read(buf, 0, 5);

    		        long filesize=0L;
    		        while(true){
    		          if(in.read(buf, 0, 1)<0){
    		            // error
    		            break; 
    		          }
    		          if(buf[0]==' ')break;
    		          filesize=filesize*10L+(long)(buf[0]-'0');
    		        }

    		        String file=null;
    		        for(int i=0;;i++){
    		          in.read(buf, i, 1);
    		          if(buf[i]==(byte)0x0a){
    		            file=new String(buf, 0, i);
    		            break;
    		  	  }
    		        }

    			//System.out.println("filesize="+filesize+", file="+file);

    		        // send '\0'
    		        buf[0]=0; out.write(buf, 0, 1); out.flush();

    		        // read a content of lfile
    		        fos=new FileOutputStream(prefix==null ? lfile : prefix+file);
    		        //File local = new File(prefix==null ? lfile : prefix+file);
    		        //if(!local.exists()) local.createNewFile();
    		        //fos=new FileOutputStream(local);
    		        int foo;
    		        while(true){
    		          if(buf.length<filesize) foo=buf.length;
    		          else foo=(int)filesize;
    		          foo=in.read(buf, 0, foo);
    		          if(foo<0){
    		            // error 
    		            break;
    		          }
    		          fos.write(buf, 0, foo);
    		          filesize-=foo;
    		          if(filesize==0L) break;
    		        }
    		        fos.close();
    		        fos=null;

    			if(checkAck(in)!=0){
    			  System.err.println("failed in ScpFrom");
    			  LOGGER.error("Failed in UploadTerms2OTO.ScpFrom");
    			}

    		        // send '\0'
    		        buf[0]=0; out.write(buf, 0, 1); out.flush();
    		      }

    		      session.disconnect();
    		    }
    		    catch(Exception e){
    		      System.out.println(e);
    		      StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());

    		      try{if(fos!=null)fos.close();}catch(Exception ee){
        		      sw = new StringWriter(); pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString()); 
    		      }
    		    }
    		  }
    	
    	/*
    	 * Provide the function with a file name and it will upload the file based on the current directory
    	 */
    	public static void scpTo(String filename)
    	{
    		/*String directory="";
			if(standalone){
				directory = directory+""+dumpfolder;
			}
			else
			{
				directory=(String) Registry.TargetDirectory;
			}
    		String localfile= directory+"\\"+filename;
    		*/
    		String localfile= dumpfolder+filename;
    		String fileonserver="hongcui@biosemantics.arizona.edu:/home/sirls/hongcui/"+filename;
    		
    		FileInputStream fis=null;
    	    try{

    	    	String pass ="2t2gPTfz";
    	      /*String lfile=arg[0];
    	      String user=arg[1].substring(0, arg[1].indexOf('@'));
    	      arg[1]=arg[1].substring(arg[1].indexOf('@')+1);
    	      String host=arg[1].substring(0, arg[1].indexOf(':'));
    	      String rfile=arg[1].substring(arg[1].indexOf(':')+1);*/
    	    	String lfile=localfile;
    	        String user=fileonserver.substring(0, fileonserver.indexOf('@'));
    	        fileonserver=fileonserver.substring(fileonserver.indexOf('@')+1);
    	        String host=fileonserver.substring(0, fileonserver.indexOf(':'));
    	        String rfile=fileonserver.substring(fileonserver.indexOf(':')+1);

    	      JSch jsch=new JSch();
    	      Session session=jsch.getSession(user, host, 22);
    	    //Used to bypass the checking of the unknownhost key MOHAN
    	      java.util.Properties config = new java.util.Properties(); 
    	      config.put("StrictHostKeyChecking", "no");
    	      session.setConfig(config);
    	      // username and password will be given via UserInfo interface.
    	      /*UserInfo ui=new MyUserInfo();
    	      session.setUserInfo(ui);*/
    	      session.setPassword(pass);
    	      session.connect();

    	      boolean ptimestamp = true;

    	      // exec 'scp -t rfile' remotely
    	      String command="scp " + (ptimestamp ? "-p" :"") +" -t "+rfile;
    	      Channel channel=session.openChannel("exec");
    	      ((ChannelExec)channel).setCommand(command);

    	      // get I/O streams for remote scp
    	      OutputStream out=channel.getOutputStream();
    	      InputStream in=channel.getInputStream();

    	      channel.connect();

    	      if(checkAck(in)!=0){
    	    	  System.out.println("Error in SCPto");
    	    	  LOGGER.error("UploadTerms2OTO.SCPto: checkAck problem");
    	    	  System.exit(0);
    	      }

    	      File _lfile = new File(lfile);

    	      if(ptimestamp){
    	        command="T "+(_lfile.lastModified()/1000)+" 0";
    	        // The access time should be sent here,
    	        // but it is not accessible with JavaAPI ;-<
    	        command+=(" "+(_lfile.lastModified()/1000)+" 0\n"); 
    	        out.write(command.getBytes()); out.flush();
    	        if(checkAck(in)!=0){
    	        	System.out.println("Error in SCPto");
    	        	LOGGER.error("UploadTerms2OTO.SCPto: checkAck problem");
    	  	  System.exit(0);
    	        }
    	      }

    	      // send "C0644 filesize filename", where filename should not include '/'
    	      long filesize=_lfile.length();
    	      command="C0644 "+filesize+" ";
    	      if(lfile.lastIndexOf('/')>0){
    	        command+=lfile.substring(lfile.lastIndexOf('/')+1);
    	      }
    	      else{
    	        command+=lfile;
    	      }
    	      command+="\n";
    	      out.write(command.getBytes()); out.flush();
    	      if(checkAck(in)!=0){
    	    	  System.out.println("Error in SCPto");
    	    	  LOGGER.error("UploadTerms2OTO.SCPto: checkAck problem");
    		System.exit(0);
    	      }

    	      // send a content of lfile
    	      fis=new FileInputStream(lfile);
    	      byte[] buf=new byte[1024];
    	      while(true){
    	        int len=fis.read(buf, 0, buf.length);
    		if(len<=0) break;
    	        out.write(buf, 0, len); //out.flush();
    	      }
    	      fis.close();
    	      fis=null;
    	      // send '\0'
    	      buf[0]=0; out.write(buf, 0, 1); out.flush();
    	      if(checkAck(in)!=0){
    	    	  System.out.println("Error in SCPto");
    	    	  LOGGER.error("UploadTerms2OTO.SCPto: checkAck problem");
    	    	  System.exit(0);
    	      }
    	      out.close();

    	      channel.disconnect();
    	      session.disconnect();

    	      //System.exit(0);
    	    }
    	    catch(Exception e){
    	      System.out.println(e);
    	      try{if(fis!=null)fis.close();}catch(Exception ee){}
    	    }
    	  }

    	
  /*
   * Used to check the acknowledgement. Used in Scpto  	
   */
static int checkAck(InputStream in) throws IOException{
    int b=in.read();
    // b may be 0 for success,
    //          1 for error,
    //          2 for fatal error,
    //          -1
    if(b==0) return b;
    if(b==-1) return b;

    if(b==1 || b==2){
      StringBuffer sb=new StringBuffer();
      int c;
      do {
	c=in.read();
	sb.append((char)c);
      }
      while(c!='\n');
      if(b==1){ // error
    	  System.out.print(sb.toString());
      }
      if(b==2){ // fatal error
    	  System.out.print(sb.toString());
      }
    }
    return b;
  }


	/**
	 * Used to execute commands on the sql server
	 * @param command
	 * @return ArrayList of at least 1 element, which is the exit status. If there are other output, the size of the ArrayList > 1
	 */
	public static ArrayList<String> execute(String command)
	{
		ArrayList<String> result = new ArrayList<String>();
		try{
		      JSch jsch=new JSch();  
		      String host ="biosemantics.arizona.edu";
		      String user="hongcui";
		      String pass ="2t2gPTfz";

		      Session session=jsch.getSession(user, host, 22);
		      
		      //Used to bypass the checking of the unknownhost key MOHAN
		      java.util.Properties config = new java.util.Properties(); 
		      config.put("StrictHostKeyChecking", "no");
		      session.setConfig(config);
		      
		      session.setPassword(pass);
		      session.connect();
		      
		      System.out.println(session.toString());

		      Channel channel=session.openChannel("exec");
		      ((ChannelExec)channel).setCommand(command);

		      channel.setInputStream(null);

		      ((ChannelExec)channel).setErrStream(System.err);
		      InputStream err = ((ChannelExec)channel).getErrStream();
		  
		      InputStream in =channel.getInputStream();
		      channel.connect();
		      System.out.println(channel.toString());
		      
		      //wait for in become available
		      //do{
		      //}while(in.available()<=0 && err.available()<=0 && !channel.isClosed());
		      

		      BufferedReader br = null;
		      byte[] tmp=new byte[1024];
		      //StringBuffer sb = new StringBuffer();
		      while(true){
		    	  if(in.available() >0) {
		    		  //System.out.println(l);
	    			  int i=in.read(tmp, 0, 1024);
	    			  if(i<=0) break;		          
	    			  //sb.append(new String(tmp, 0, i));	
	    			  String[] lines = new String(tmp, 0, i).replaceFirst("\\n$", "").split("\\n");
		    		  result.addAll(Arrays.asList(lines));
		    		  /*br = new BufferedReader(new InputStreamReader(in));
		    		  while(br!=null && br.ready()){	
		    			  //System.out.println("reading from in...");  
		    			  String l = br.readLine();
		    			  result.add(l);          
		    		  }*/
		    	  }
		   	     if(channel.isClosed()){
		   	    	String t = channel.getExitStatus()+"";
		        	result.add(t);
		        	//System.out.println("channel closed");
		        	//System.out.println("exit-status: "+t);
		        	break;
		   	     }
		      //Mohan's code to quit if the input stream is empty. Which means the system is just waiting.
		        /*else if(in.available()<=0)
		        {
		        	String t = channel.getExitStatus()+"";
		        	result.add(t);
		        	System.out.println("exit-status: "+t);
			        break;
		        }*/
			    /*try{
			        Thread.sleep(1000);
			        System.out.println("waiting for 1000");
			    }catch(Exception ee){
			        StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);ee.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			    }*/
		      }
		      channel.disconnect();
		      session.disconnect();
		    }
		    catch(Exception e){
		    	StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		    	//System.out.println(e);
		    }
		return result;
	}

	public static void main(String[] args) {	
		String dataprefix = "treatise_o_test";
		UploadTerms2OTO uto = new UploadTerms2OTO(dataprefix);
		
		/*dumpFiles(dataprefix);
		createTextFile(dataprefix);
		
		String textfile = dataprefix+"_sqlscript.txt";
		scpTo(textfile);
		String tcategory = dataprefix+"_term_category_dump.sql";
		scpTo(tcategory);
		String tsentence = dataprefix+"_sentence_dump.sql";
		scpTo(tsentence);
		
		
		String backup = "mysqldump -u termsuser -ptermspassword markedupdatasets > markedupdatasets_bak_"+dateFormat.format(cal.getTime())+".sql";
		execute(backup);
		
		String excom = "mysql -u termsuser -ptermspassword < "+textfile+" 2> "+dataprefix+"_sqllog.txt";//write output to log file
		execute(excom);*/
	}
}