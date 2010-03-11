/**
 * $Id$
 */
package fna.parsing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import fna.db.VolumeMarkupDbAccessor;

/**
 * To run unsupervised.pl
 * 
 * @author chunshui
 */
public class VolumeMarkup {
	
	private ProcessListener listener;
	private String markupMode = "adj"; //TODO: make this configurable
	private static final Logger LOGGER = Logger.getLogger(VolumeMarkup.class);
	
	public VolumeMarkup(ProcessListener listener) {
		this.listener = listener;
	}
	
	public void markup() throws ParsingException {
		// call unsupervised.pl [descriptions are save in
		// docs/output/descriptions]
		String workdir = Registry.TargetDirectory;
		String todofoldername = ApplicationUtilities.getProperty("DEHYPHENED");
		String savefoldername = ApplicationUtilities.getProperty("MARKEDUP");
		String databasenameprefix = ApplicationUtilities.getProperty("database.name");

//		String com = "perl " + ApplicationUtilities.getProperty("UNSUPERVISED") +workdir
//		+ " " + todofoldername + " " + savefoldername
//		+ " seednouns.txt learntnouns.txt graphml.xml "
//		+ databasenameprefix;
		
		String com = "perl " + ApplicationUtilities.getProperty("UNSUPERVISED") +workdir
		+ todofoldername + " "+ databasenameprefix+" "+this.markupMode +" "+MainForm.dataPrefixCombo.getText().trim();
		
		//this command will not output marked-up descriptions to the file system. it only holds the results in mySQL database
		System.out.println("Run command: " + com);
        MainForm.markUpPerlLog.append("Run command: " + com + "\n");
		try {
			 runCommand(com);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("VolumeMarkup : markup Failed to run the unsupervised.pl", e);
			MainForm.markUpPerlLog.append("VolumeMarkup : markup Failed to run the unsupervised.pl" + e.getMessage() + "\n");
			throw new ParsingException("Failed to run the unsupervised.pl.", e);
		}
		
		update();
	}
	
	public void update() throws ParsingException{
		listener.clear();
		
		List<String> tagList = new ArrayList<String>();
		VolumeMarkupDbAccessor vmDba = new VolumeMarkupDbAccessor();
		try {
			vmDba.updateData(tagList);
		} catch (Exception e) {
			LOGGER.error("Couldn't perform database operation in VolumeMarkup:update", e);
			e.printStackTrace();
			throw new ParsingException("Failed to execute the statement.", e);
		}
		
		// fill in the table
		for (int i = 0; i < tagList.size(); i++) {
			listener.info("" + i, tagList.get(i));
		}
	}
	//Perl would hang on any MySQL warnings or errors
	private void runCommand(String com) throws IOException,
			InterruptedException {
		long time = System.currentTimeMillis();

		Process p = Runtime.getRuntime().exec(com);
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p
				.getInputStream()));
		
		BufferedReader errInput = new BufferedReader(new InputStreamReader(p
				.getErrorStream()));
		
		// read the output from the command
		String s = "";
		int i = 0;
		while ((s = stdInput.readLine()) != null) {
			// listener.info(String.valueOf(i), s);
			listener.progress(i++ % 100);
			System.out.println(s + " at " + (System.currentTimeMillis() - time)
					/ 1000 + " seconds");
			MainForm.markUpPerlLog.append(s + " at " + (System.currentTimeMillis() - time)
					/ 1000 + " seconds\n");
		}
		
		// read the errors from the command
		String e = "";
		while ((e = errInput.readLine()) != null) {
			// listener.info(String.valueOf(i), s);
			listener.progress(i++ % 100);
			System.out.println(e + " at " + (System.currentTimeMillis() - time)
					/ 1000 + " seconds");
			MainForm.markUpPerlLog.append(e + " at " + (System.currentTimeMillis() - time)
					/ 1000 + " seconds\n");
		}
	}
}
