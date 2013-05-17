package fna.parsing;

/**
 * This class is specificly designed to upload ang glosssary in csv files into OTO
 * Develop environment: Windows 8
 * Author: Fengqiong Huang
 * 
 * Usage: some parameters you should set before you run the class
 * 1. dataprefix: the datasetprefix that will be used in OTO after you upload
 * 2. csv_folderPath: your local folder which holds all the .cvs files you want to use. 
 * 	  There are at most two files, one is the glossary file, which is a must, and the 
 * 	  other is synonym file, which can be optional. 
 * 3. hasSynonymFile: true or false. Specify if you have the synonyms .cvs file or not. 
 * 4. datasetOwnerID: your userID in OTO. You can get it from table 'users' on OTO server. 
 * 5. useTimeStamp: true or false. Specify if you want to attach timestamp after 
 * 	  dataprefix or not. 
 * 6. glossFileName
 * 7. synFileName
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class UploadTerms2OTO_simple_csv {

/*make sure the following variables are correctly set before you run the class*/	
	private static String csv_folderPath = "D:\\Work\\Data\\ant_gloss\\"; //your local folder 
																		//holding all the .cvs files
	private static String glossFileName = "fnagloss from Hong 16May.csv"; //the file name of the glossary file
	private static String synFileName = ""; //the file name of the synonym file
																  //can be anything if hasSynonymFile = false
	private static String dataprefix = "fna_gloss"; // the prefix that will be
													// used on OTO server
	private static String datasetOwnerID = "43"; // your userID in OTO
	private static boolean useTimeStamp = true; // attach timestamp to dataprefix or not
	private static boolean hasSynonymFile = false;// has synonym .cvs files or not
/*end of variables that need to be set*/
	

	private static String folderOnServer = "/home/sirls/hongcui/tmp/";
	private static String serverPath = "hongcui@biosemantics.arizona.edu:";	
	
	private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger
			.getLogger(UploadTerms2OTO.class);
	public static DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
	public static Calendar cal = Calendar.getInstance();

	public UploadTerms2OTO_simple_csv() {

	}

	/**
	 * This function do the major work of file transfer and script execution
	 * 
	 * @return
	 */
	public boolean upload() {
		// create the script based on the file name and prefix
		boolean success = createTextFile(dataprefix);
		if (!success)
			return false;

		// copy _sqlscript.txt file to server
		String textfile = dataprefix + "_sqlscript.txt";
		success = scpTo(csv_folderPath, serverPath + folderOnServer, textfile);
		if (!success)
			return false;

		// copy those two csv files to server
		// glossary csv file
		success = scpTo(csv_folderPath, serverPath + folderOnServer,
				glossFileName);
		if (!success)
			return false;
		
		if (hasSynonymFile) {
			// synonym csv file
			success = scpTo(csv_folderPath, serverPath + folderOnServer,
					synFileName);
			if (!success)
				return false;
		}

		// back up the OTO database
		/*
		 * NOTE: Always backup the database before running this scriptfile
		 * create a text file with the following commands in it. datasetprefix
		 * is the prefix for the set of the new tables to be created
		 */
		String backup = "mysqldump --lock-tables=false -u "
				+ ApplicationUtilities.getProperty("database.username") + " -p"
				+ ApplicationUtilities.getProperty("database.password") + " "
				+ ApplicationUtilities.getProperty("database.name") + " > "
				+ ApplicationUtilities.getProperty("database.name") + "_bak_"
				+ dateFormat.format(cal.getTime()) + ".sql";
		ArrayList<String> result = execute(backup);
		if (result.size() > 1 || result.get(result.size() - 1).equals("-1"))
			return false;

		// String excom =
		// "mysql -u ApplicationUtilities.getProperty("database.username") -ptermspassword < "+textfile+" 2> "+dataprefix+"_sqllog.txt";//write
		// output to log file
		String excom = "mysql -u "
				+ ApplicationUtilities.getProperty("database.username") + " -p"
				+ ApplicationUtilities.getProperty("database.password") + " < "
				+ folderOnServer + textfile;// write output to log file
		result = execute(excom);
		if (result.size() > 1 || result.get(result.size() - 1).equals("-1"))
			return false;
		// need check _sqllog.txt for error messages
		return true;
	}

	/**
	 * craete the sql script for create a new dataset in OTO and prepare the
	 * data with those csv files
	 * 
	 * @param dataprefix
	 * @return
	 */
	public static boolean createTextFile(String dataprefix) {
		try {
			String target_dataprefix = dataprefix;
			if (useTimeStamp) {
				target_dataprefix = dataprefix + "_"
						+ dateFormat.format(cal.getTime());
			}
			System.out.println("The datasetprefix is: " + target_dataprefix);

			String filename = "sqlscript";// the file holding all the sql
											// scripts

			FileWriter fstream = new FileWriter(csv_folderPath + dataprefix
					+ "_" + filename + ".txt");
			BufferedWriter out = new BufferedWriter(fstream);

			// create all the commands
			String command = "";
			ArrayList<String> sql_commands = new ArrayList<>();

			// use markedupdatasets
			command = "use "
					+ ApplicationUtilities.getProperty("database.name") + ";";
			sql_commands.add(command);
			command = "";// add an empty line
			sql_commands.add(command);

			// get glossary table from csv file
			command = "create table " + target_dataprefix + "_term_category ("
					+ "term varchar(100), " + "category varchar(100), "
					+ "hasSyn Boolean default false);";
			sql_commands.add(command);

			// table: _confirmed_category (term, category, confirmDate, hasSyn)
			command = "LOAD data local infile '" + folderOnServer
					+ glossFileName + "' INTO Table " + target_dataprefix
					+ "_term_category FIELDS TERMINATED BY ',' IGNORE 1 LINES;";
			sql_commands.add(command);

			command = "create table " + target_dataprefix + "_syns ("
					+ "term varchar(100), synonym varchar(100));";
			sql_commands.add(command);

			if (hasSynonymFile) {
				// table: _syns (term, synonym)
				command = "LOAD data local infile '" + folderOnServer + synFileName
						+ "' INTO Table " + target_dataprefix
						+ "_syns FIELDS TERMINATED BY ',' IGNORE 1 LINES;";
				sql_commands.add(command);	
			}
			
			// insert dataset prefix
			command = "insert into datasetprefix (prefix) value ('"
					+ target_dataprefix + "');";
			sql_commands.add(command);

			// table _comments
			command = "create table " + target_dataprefix
					+ "_comments like fna_gloss_comments;";
			sql_commands.add(command);

			// table _review_history
			command = "create table " + target_dataprefix
					+ "_review_history like fna_gloss_review_history;";
			sql_commands.add(command);

			// page Group Terms
			// categories
			command = "create table " + target_dataprefix
					+ "_categories like categories;";
			sql_commands.add(command);
			// get default records from table categories
			command = "insert into " + target_dataprefix
					+ "_categories select * from categories;";
			sql_commands.add(command);

			// get other categories from uploaded data
			command = "insert into " + target_dataprefix + "_categories "
					+ " select distinct category, '' as definition from "
					+ target_dataprefix
					+ "_term_category where category not in ("
					+ "select category from " + target_dataprefix
					+ "_categories" + ");";
			sql_commands.add(command);

			command = "create table " + target_dataprefix
					+ "_web_grouped_terms like fna_gloss_web_grouped_terms;";
			sql_commands.add(command);

			command = "create table " + target_dataprefix
					+ "_confirmed_category like fna_gloss_confirmed_category;";
			sql_commands.add(command);

			command = "create table "
					+ target_dataprefix
					+ "_user_terms_decisions like fna_gloss_user_terms_decisions;";
			sql_commands.add(command);

			command = "create table "
					+ target_dataprefix
					+ "_user_terms_relations like fna_gloss_user_terms_relations;";
			sql_commands.add(command);

			command = "create table " + target_dataprefix
					+ "_sentence like fna_gloss_sentence;";
			sql_commands.add(command);

			// page Hierarchy Tree
			command = "create table " + target_dataprefix
					+ "_web_tags like fna_gloss_web_tags;";
			sql_commands.add(command);

			command = "create table " + target_dataprefix
					+ "_confirmed_paths like fna_gloss_confirmed_paths;";
			sql_commands.add(command);

			command = "create table "
					+ target_dataprefix
					+ "_user_tags_decisions like fna_gloss_user_tags_decisions;";
			sql_commands.add(command);

			// page Orders
			command = "create table " + target_dataprefix
					+ "_confirmed_orders like fna_gloss_confirmed_orders;";
			sql_commands.add(command);

			command = "create table "
					+ target_dataprefix
					+ "_user_orders_decisions like fna_gloss_user_orders_decisions;";
			sql_commands.add(command);

			command = "create table " + target_dataprefix
					+ "_web_orders like fna_gloss_web_orders;";
			sql_commands.add(command);

			command = "create table " + target_dataprefix
					+ "_web_orders_terms like fna_gloss_web_orders_terms;";
			sql_commands.add(command);

			// Insert terms into table _web_grouped_terms
			command = "insert into "
					+ target_dataprefix
					+ "_web_grouped_terms(term, groupid) select distinct term, 1 as groupid from "
					+ target_dataprefix + "_term_category;";
			sql_commands.add(command);

			if (hasSynonymFile) {
				// get decisions related to main terms (main terms means those with
				// synonyms)
				command = "insert into "
						+ target_dataprefix
						+ "_user_terms_decisions(term, decision, userid, decisiondate, groupid, hasSyn, relatedTerms) "
						+ "select tc.term, tc.category, "
						+ datasetOwnerID
						+ " as userid, sysdate(), 1 as groupid, hasSyn, syns.relatedTerms "
						+ "from "
						+ target_dataprefix
						+ "_term_category tc left join "
						+ "(select term, group_concat(\"'\", synonym, \"'\") as relatedTerms "
						+ "from " + target_dataprefix + "_syns "
						+ "group by term) syns " + "on syns.term = tc.term "
						+ "where tc.hasSyn = true; ";
				sql_commands.add(command);

				// get decisions related to those synonyms
				command = "insert into "
						+ target_dataprefix
						+ "_user_terms_decisions(term, decision, userid, decisiondate, groupid, relatedTerms) "
						+ "select tc.term, tc.category, "
						+ datasetOwnerID
						+ " as userid, sysdate(), 1 as groupid, syns.relatedTerms "
						+ "from "
						+ target_dataprefix
						+ "_term_category tc left join "
						+ "(select synonym, group_concat(\"synonym of '\", term, \"'\") as relatedTerms, 1 as isAdditional "
						+ "from " + target_dataprefix + "_syns "
						+ "group by synonym) syns " + "on syns.synonym = tc.term "
						+ "where tc.hasSyn = false; ";
				sql_commands.add(command);

				// update isAdditional
				command = "update " + target_dataprefix
						+ "_user_terms_decisions set isAdditional = true "
						+ "where term in (select synonym from " + target_dataprefix
						+ "_syns); ";
				sql_commands.add(command);
			} else {
				// get data for table _user_terms_decisions
				command = "insert into "
						+ target_dataprefix
						+ "_user_terms_decisions(term, decision, userid, decisiondate, groupid, hasSyn, relatedTerms) "
						+ "select term, category, "
						+ datasetOwnerID
						+ " as userid, sysdate(), 1 as groupid, 0 as hasSyn, '' as relatedTerms " 
						+ "from "
						+ target_dataprefix
						+ "_term_category; ";
				sql_commands.add(command);
			}
			
			// add data into _confirmed_category
			command = "insert into "
					+ target_dataprefix
					+ "_confirmed_category (term, category, accepted, confirmDate) "
					+ "select term, category, 1 as accepted, now() as confirmDate "
					+ "from " + target_dataprefix + "_term_category; ";
			sql_commands.add(command);

			command = "";
			sql_commands.add(command);

			// write the commands into the text file.
			for (int i = 0; i < sql_commands.size(); i++) {
				/*
				 * String text = commands[i]; out.append(text); out.newLine();
				 */
				out.write(sql_commands.get(i));
				out.newLine();
			}

			out.close();
			sql_commands = null;
			return true;
		} catch (Exception e) {
			System.err.println("Error in creating and writing to a text file: "
					+ e.getMessage());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			LOGGER.error(ApplicationUtilities
					.getProperty("CharaParser.version")
					+ System.getProperty("line.separator") + sw.toString());

		}
		return false;
	}

	/**
	 * copy from a remote server to local machine
	 * 
	 */
	public static boolean scpFrom(String rfile, String lfile) {
		FileOutputStream fos = null;
		try {
			// hongcui@:
			String user = ApplicationUtilities.getProperty("server.username");
			String host = ApplicationUtilities.getProperty("server.name");

			String prefix = null;
			if (new File(lfile).isDirectory()) {
				prefix = lfile + File.separator;
			}

			JSch jsch = new JSch();
			Session session = jsch.getSession(user, host, 22);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.setPassword(ApplicationUtilities
					.getProperty("server.password"));
			session.connect();

			// exec 'scp -f rfile' remotely
			String command = "scp -f " + rfile;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			byte[] buf = new byte[1024];

			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();

			while (true) {
				int c = checkAck(in);
				if (c != 'C') {
					break;
				}

				// read '0644 '
				in.read(buf, 0, 5);

				long filesize = 0L;
				while (true) {
					if (in.read(buf, 0, 1) < 0) {
						// error
						break;
					}
					if (buf[0] == ' ')
						break;
					filesize = filesize * 10L + (long) (buf[0] - '0');
				}

				String file = null;
				for (int i = 0;; i++) {
					in.read(buf, i, 1);
					if (buf[i] == (byte) 0x0a) {
						file = new String(buf, 0, i);
						break;
					}
				}

				// System.out.println("filesize="+filesize+", file="+file);

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();

				// read a content of lfile
				fos = new FileOutputStream(prefix == null ? lfile : prefix
						+ file);
				// File local = new File(prefix==null ? lfile : prefix+file);
				// if(!local.exists()) local.createNewFile();
				// fos=new FileOutputStream(local);
				int foo;
				while (true) {
					if (buf.length < filesize)
						foo = buf.length;
					else
						foo = (int) filesize;
					foo = in.read(buf, 0, foo);
					if (foo < 0) {
						// error
						break;
					}
					fos.write(buf, 0, foo);
					filesize -= foo;
					if (filesize == 0L)
						break;
				}
				fos.close();
				fos = null;

				if (checkAck(in) != 0) {
					System.err.println("failed in ScpFrom");
					LOGGER.error("Failed in UploadTerms2OTO.ScpFrom");
				}

				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
			}

			session.disconnect();
			return true;
		} catch (Exception e) {
			System.out.println(e);
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			LOGGER.error(ApplicationUtilities
					.getProperty("CharaParser.version")
					+ System.getProperty("line.separator") + sw.toString());

			try {
				if (fos != null)
					fos.close();
			} catch (Exception ee) {
				sw = new StringWriter();
				pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities
						.getProperty("CharaParser.version")
						+ System.getProperty("line.separator") + sw.toString());
			}
		}
		return false;
	}

	/*
	 * Provide the function with a file name and it will upload the file based
	 * on the current directory
	 */
	public static boolean scpTo(String from, String to, String filename) {
		/*
		 * String directory=""; if(standalone){ directory =
		 * directory+""+dumpfolder; } else { directory=(String)
		 * Registry.TargetDirectory; } String localfile=
		 * directory+"\\"+filename;
		 */
		String localfile = from + filename;
		String fileonserver = to + filename;

		FileInputStream fis = null;
		try {

			String pass = ApplicationUtilities.getProperty("server.password");
			/*
			 * String lfile=arg[0]; String user=arg[1].substring(0,
			 * arg[1].indexOf('@'));
			 * arg[1]=arg[1].substring(arg[1].indexOf('@')+1); String
			 * host=arg[1].substring(0, arg[1].indexOf(':')); String
			 * rfile=arg[1].substring(arg[1].indexOf(':')+1);
			 */
			String lfile = localfile;
			String user = fileonserver.substring(0, fileonserver.indexOf('@'));
			fileonserver = fileonserver
					.substring(fileonserver.indexOf('@') + 1);
			String host = fileonserver.substring(0, fileonserver.indexOf(':'));
			String rfile = fileonserver
					.substring(fileonserver.indexOf(':') + 1);

			JSch jsch = new JSch();
			Session session = jsch.getSession(user, host, 22);
			// Used to bypass the checking of the unknownhost key MOHAN
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			// username and password will be given via UserInfo interface.
			/*
			 * UserInfo ui=new MyUserInfo(); session.setUserInfo(ui);
			 */
			session.setPassword(pass);
			session.connect();

			boolean ptimestamp = true;

			// exec 'scp -t rfile' remotely
			String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + rfile;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			if (checkAck(in) != 0) {
				System.out.println("Error in SCPto");
				LOGGER.error("UploadTerms2OTO.SCPto: checkAck problem");
				// System.exit(0);
			}

			File _lfile = new File(lfile);

			if (ptimestamp) {
				command = "T " + (_lfile.lastModified() / 1000) + " 0";
				// The access time should be sent here,
				// but it is not accessible with JavaAPI ;-<
				command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
				out.write(command.getBytes());
				out.flush();
				if (checkAck(in) != 0) {
					System.out.println("Error in SCPto");
					LOGGER.error("UploadTerms2OTO.SCPto: checkAck problem");
					// System.exit(0);
				}
			}

			// send "C0644 filesize filename", where filename should not include
			// '/'
			long filesize = _lfile.length();
			command = "C0644 " + filesize + " ";
			if (lfile.lastIndexOf('/') > 0) {
				command += lfile.substring(lfile.lastIndexOf('/') + 1);
			} else {
				command += lfile;
			}
			command += "\n";
			out.write(command.getBytes());
			out.flush();
			if (checkAck(in) != 0) {
				System.out.println("Error in SCPto");
				LOGGER.error("UploadTerms2OTO.SCPto: checkAck problem");
				// System.exit(0);
			}

			// send a content of lfile
			fis = new FileInputStream(lfile);
			byte[] buf = new byte[1024];
			System.out.println("start sending");

			try {
				Thread.sleep(1000);
				System.out.println("waiting for 1000");// for fis to get ready
			} catch (Exception ee) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				ee.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities
						.getProperty("CharaParser.version")
						+ System.getProperty("line.separator") + sw.toString());
			}

			while (true) {
				int len = fis.read(buf, 0, buf.length);
				System.out.println("sent " + len);
				if (len <= 0)
					break;
				out.write(buf, 0, len); // out.flush();
			}
			System.out.println("All sent");
			fis.close();
			fis = null;
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			if (checkAck(in) != 0) {
				System.out.println("Error in SCPto");
				LOGGER.error("UploadTerms2OTO.SCPto: checkAck problem");
				// System.exit(0);
			}
			out.close();

			channel.disconnect();
			session.disconnect();

			// System.exit(0);
			return true;
		} catch (Exception e) {
			System.out.println(e);
			try {
				if (fis != null)
					fis.close();
			} catch (Exception ee) {
			}
		}
		return false;
	}

	/*
	 * Used to check the acknowledgement. Used in Scpto
	 */
	static int checkAck(InputStream in) throws IOException {
		int b = in.read();

		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				System.out.print(sb.toString());
			}
			if (b == 2) { // fatal error
				System.out.print(sb.toString());
			}
		}
		return b;
	}

	/**
	 * Used to execute commands on the OTO server
	 * 
	 * @param command
	 * @return ArrayList of at least 1 element, which is the exit status. If
	 *         there are other output, the size of the ArrayList > 1
	 */
	public static ArrayList<String> execute(String command) {
		ArrayList<String> result = new ArrayList<String>();
		try {
			JSch jsch = new JSch();
			String host = "biosemantics.arizona.edu";
			String user = ApplicationUtilities.getProperty("server.username");
			String pass = ApplicationUtilities.getProperty("server.password");

			Session session = jsch.getSession(user, host, 22);

			// Used to bypass the checking of the unknownhost key MOHAN
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);

			session.setPassword(pass);
			session.connect();

			System.out.println(session.toString());

			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			channel.setInputStream(null);

			((ChannelExec) channel).setErrStream(System.err);
			InputStream err = ((ChannelExec) channel).getErrStream();

			InputStream in = channel.getInputStream();
			channel.connect();
			System.out.println(channel.toString());

			// wait for in become available
			// do{
			// }while(in.available()<=0 && err.available()<=0 &&
			// !channel.isClosed());

			BufferedReader br = null;
			byte[] tmp = new byte[1024];
			// StringBuffer sb = new StringBuffer();
			while (true) {
				if (in.available() > 0) {
					// System.out.println(l);
					int i = in.read(tmp, 0, 1024);
					if (i <= 0)
						break;
					// sb.append(new String(tmp, 0, i));
					String[] lines = new String(tmp, 0, i).replaceFirst("\\n$",
							"").split("\\n");
					result.addAll(Arrays.asList(lines));
					/*
					 * br = new BufferedReader(new InputStreamReader(in));
					 * while(br!=null && br.ready()){
					 * //System.out.println("reading from in..."); String l =
					 * br.readLine(); result.add(l); }
					 */
				}
				// collect error messages
				if (err.available() > 0) {
					// System.out.println(l);
					int i = err.read(tmp, 0, 1024);
					if (i <= 0)
						break;
					// sb.append(new String(tmp, 0, i));
					String[] lines = new String(tmp, 0, i).replaceFirst("\\n$",
							"").split("\\n");
					result.addAll(Arrays.asList(lines));
					/*
					 * br = new BufferedReader(new InputStreamReader(in));
					 * while(br!=null && br.ready()){
					 * //System.out.println("reading from in..."); String l =
					 * br.readLine(); result.add(l); }
					 */
				}
				if (channel.isClosed()) {
					String t = channel.getExitStatus() + "";
					result.add(t);
					// System.out.println("channel closed");
					// System.out.println("exit-status: "+t);
					break;
				}
				// Mohan's code to quit if the input stream is empty. Which
				// means the system is just waiting.
				/*
				 * else if(in.available()<=0) { String t =
				 * channel.getExitStatus()+""; result.add(t);
				 * System.out.println("exit-status: "+t); break; }
				 */
				/*
				 * try{ Thread.sleep(1000);
				 * System.out.println("waiting for 1000"); }catch(Exception ee){
				 * StringWriter sw = new StringWriter();PrintWriter pw = new
				 * PrintWriter
				 * (sw);ee.printStackTrace(pw);LOGGER.error(ApplicationUtilities
				 * .getProperty("CharaParser.version")+System.getProperty(
				 * "line.separator")+sw.toString()); }
				 */
			}
			channel.disconnect();
			session.disconnect();
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			LOGGER.error(ApplicationUtilities
					.getProperty("CharaParser.version")
					+ System.getProperty("line.separator") + sw.toString());
			result.add("-1"); // last element is the exit status bit
			// System.out.println(e);
		}
		return result;
	}

	/**
	 * main func
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		UploadTerms2OTO_simple_csv uto = new UploadTerms2OTO_simple_csv();
		uto.upload(); // do upload
	}
}
