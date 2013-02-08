/**
 * $Id$
 */
package fna.parsing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import fna.db.VolumeMarkupDbAccessor;

/**
 * To run unsupervised.pl
 * 
 * @author chunshui
 */
@SuppressWarnings({  "unused" })
public class VolumeMarkup {
	
	protected ProcessListener listener;
	
	protected Display display = null;
	protected Text perlLog = null;
	protected String dataPrefix = null;
	
	protected String markupMode = "plain"; //TODO: make this configurable

	private String glossarytable;
	public static Process p = null;
	protected static final Logger LOGGER = Logger.getLogger(VolumeMarkup.class);
	
	public VolumeMarkup(ProcessListener listener, Display display, Text perlLog, String dataPrefix, String glossarytable) {
		this.listener = listener;
        this.display = display;
        this.perlLog = perlLog;
        this.dataPrefix = dataPrefix;
        this.glossarytable = glossarytable;
	}
	
	public void showPerlMessage(final String message) {
		display.syncExec(new Runnable() {
			public void run() {
				perlLog.append(message);
			}
		});
	}
	
	public void incrementProgressBar(int progress) {
		listener.progress(progress);
	}
	public void markup() throws ParsingException {
		// call unsupervised.pl [descriptions are save in
		// docs/output/descriptions]
		String workdir = Registry.TargetDirectory;
		String todofoldername = ApplicationUtilities.getProperty("DEHYPHENED");
		String savefoldername = ApplicationUtilities.getProperty("MARKEDUP");
		String databasenameprefix = ApplicationUtilities.getProperty("database.name");

		
		
		String comstring = "perl " + ApplicationUtilities.getProperty("UNSUPERVISED")+" " +workdir
		+ todofoldername + "/ "+ databasenameprefix+" "+this.markupMode +" "+dataPrefix.trim() + " "+glossarytable;
		
		String[] com =  new String[]{"perl",  ApplicationUtilities.getProperty("UNSUPERVISED"), workdir
		+ todofoldername + "/", databasenameprefix, this.markupMode, dataPrefix.trim() , glossarytable};
		//this command will not output marked-up descriptions to the file system. it only holds the results in mySQL database
		System.out.println("Run command: " + comstring);
		showPerlMessage("Run command: " + comstring + "\n");
		try {
			 runCommand(com);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			showPerlMessage("VolumeMarkup : markup Failed to run the unsupervised.pl" + e.getMessage() + "\n");
			throw new ParsingException("Failed to run the unsupervised.pl.", e);
		}
		
		//update();
		

	}
	
	/*public void update() throws ParsingException{
		listener.clear();
		
		List<String> tagList = new ArrayList<String>();
		VolumeMarkupDbAccessor vmDba = new VolumeMarkupDbAccessor(this.dataPrefix, this.glossarytable);
		try {
			vmDba.structureTags4Curation(tagList);
		} catch (Exception e) {
			LOGGER.error("Couldn't perform database operation in VolumeMarkup:update", e);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			throw new ParsingException("Failed to execute the statement.", e);
		}
		
		// fill in the table
		for (int i = 1; i < tagList.size(); i++) {
			listener.info("" + i, tagList.get(i));
		}
	}*/
	//Perl would hang on any MySQL warnings or errors
	protected void runCommand(String[] com) throws IOException,
			InterruptedException {
		long time = System.currentTimeMillis();
		
		p = Runtime.getRuntime().exec(com);
				
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p
				.getInputStream()));
		
		BufferedReader errInput = new BufferedReader(new InputStreamReader(p
				.getErrorStream()));
		
		// read the output from the command
		String s = "";
		int i = 0;
		while ((s = stdInput.readLine()) != null) {
			// listener.info(String.valueOf(i), s);
			incrementProgressBar(i++ % 100);
			System.out.println(s + " at " + (System.currentTimeMillis() - time)
					/ 1000 + " seconds");
			showPerlMessage(s + " at " + (System.currentTimeMillis() - time)
					/ 1000 + " seconds\n");
		}
		
		// read the errors from the command
		String e = "";
		while ((e = errInput.readLine()) != null) {
			// listener.info(String.valueOf(i), s);
			incrementProgressBar(i++ % 100);
			System.out.println(e + " at " + (System.currentTimeMillis() - time)
					/ 1000 + " seconds");
			showPerlMessage(e + " at " + (System.currentTimeMillis() - time)
					/ 1000 + " seconds\n");
		}
	}
}
