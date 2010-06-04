/**
 * 
 */
package fna.parsing;
import java.io.BufferedWriter;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import org.jdom.*;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import fna.db.*;

/**
 * @author hongcui
 * 	
 * Markup OCRed text (type3)
 * Identify and mark up morphological descriptions only
 * call paragraphExtraction/bootstrapDescriptionExtraction.pl
 *
 * input: seeds description, with or without
 * output: prefix_paragraphs and prefix_sentence, prefix_wordpos tables etc by unsupervisedClauseMarkupBenchmarked.pl
 */
public class Type3PreMarkup{
	private ArrayList<String> seeds = new ArrayList<String>();
	//private File source =new File(Registry.SourceDirectory); //a folder of text documents to be annotated
	private File source = new File("Z:\\WorkFeb2008\\WordNov2009\\Description_Extraction\\extractionSource\\Plain_text");
	//File target = new File(Registry.TargetDirectory);
	File target = new File("Z:\\WorkFeb2008\\WordNov2009\\Description_Extraction\\extractionSource\\Plain_text_transformed");
	private XMLOutputter outputter = null;
	private String markupMode = "plain";
	private String dataprefix = null;
	protected static final Logger LOGGER = Logger.getLogger(Type3PreMarkup.class);
	private String seedfilename = "seeds";
	
	Type3PreMarkup(ProcessListener listener, Display display, Text perllog, String dataprefix, ArrayList seeds){
		//super(listener, display, perllog, dataprefix);
		this.seeds = seeds;
		saveSeeds();
		//this.markupMode = "plain";
		this.dataprefix = dataprefix;
		outputter = new XMLOutputter(Format.getPrettyFormat());
	}	
	
	private void saveSeeds(){
		if(seeds !=null){
			Iterator it = seeds.iterator();
			StringBuffer sb = new StringBuffer();
			while(it.hasNext()){
				sb.append(it.next());
				sb.append(System.getProperty("line.separator"));
			}

			write2file(target, seedfilename, sb.toString());
		}
	}
	
	/**
	 * use prefix_paragraphbootstrap.isDescription column to decide if a paragraph contains descriptive statements
	 * use prefix_paragraphs.paragraph and remark columns to get original text and tagged text
	 * @param source
	 */
	private void bootstrapMorphDes() {
		String workdir =this.source.getAbsolutePath();
		String com = "perl " + ApplicationUtilities.getProperty("PARAGRAPHBOOTSTRAP") + " "+
		workdir + " " +
		ApplicationUtilities.getProperty("database.name") + " " +
		this.markupMode + " " + 
		target.getAbsolutePath()+"\\"+seedfilename+" "+
		dataprefix.trim();
		
		//this command will not output marked-up descriptions to the file system. it only holds the results in mySQL database
		System.out.println("Run command: " + com);

		try {
			 runCommand(com);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Type3Markup : markup Failed to run bootstrapDescriptionExtraction.pl", e);
			throw new ParsingException("Failed to run bootstrapDescriptionExtraction.pl.", e);
		}
	}

	protected void runCommand(String com) throws IOException,
	InterruptedException {
		long time = System.currentTimeMillis();

		Process p = Runtime.getRuntime().exec(com);
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p
				.getInputStream()));

		BufferedReader errInput = new BufferedReader(new InputStreamReader(p
				.getErrorStream()));

		//		read the output from the command
		String s = "";
		int i = 0;
		while ((s = stdInput.readLine()) != null) {
			System.out.println(s + " at " + (System.currentTimeMillis() - time)
					/ 1000 + " seconds");
		}

		//		read the errors from the command
		String e = "";
		while ((e = errInput.readLine()) != null) {
			System.out.println(e + " at " + (System.currentTimeMillis() - time)
					/ 1000 + " seconds");
		}
	}
	/**
	 * create folders:
	 * descriptions for to-be-annotated morph description segments
	 * transformed for entire documents (tags: <document><nonMorph></nonMorph><treatment></treatment></document>)
	 */
	private void output2Target() {
		File des = createFolderIn(target, "descriptions");
		File tra = createFolderIn(target, "transformed");
		File[] files = this.source.listFiles();
		for(int i = 0; i<files.length; i++){
			String fname = files[i].getName();
			outputTo(des,tra,fname);
		}
	}
	/**
	 * use the tables created by bootstrapMorphDes
	 * use prefix_paragraphbootstrap.isDescription column to decide if a paragraph contains descriptive statements
	 * use prefix_paragraphs.paragraph and remark columns to get original text and tagged text

	 * @param folder
	 * @param fname
	 */
	private void outputTo(File desfolder, File trafolder, String fname) {
		Type3PreMarkupDbAccessor dba = new Type3PreMarkupDbAccessor();
		ArrayList<String> desIDs = dba.selectRecords("paragraphID", dataprefix.trim()+"_paragraphbootstrap", "paragraphID like '"+fname+"%'");
		
		ArrayList<String> allPs = dba.selectRecords("paragraphID, remark", dataprefix.trim()+"_paragraphs", "paragraphID like '"+fname+"%'");
		//put in to a hashtable
		Hashtable<String, String> paras = new Hashtable<String, String>();
		
		for(int i = 0; i<allPs.size(); i++){
			String[] t = allPs.get(i).split("###");
			paras.put(t[0], t[1]);
		}
		
		//output this file
		//write to description as text file, create the XML file for transformed
		Element doc = new Element("document");
		for(int i = 0; i<allPs.size(); i++){
			String pID = fname+"p"+i;
			if(desIDs.contains(pID)){
				Element morph = new Element("description");
				String text=paras.get(pID);
				String[] parts = text.split("(<@<|>@>)"); //1-3 parts
				if(parts.length==1){
					morph.setAttribute("id", pID);
					morph.setText(parts[0]);
					doc.addContent(morph);	
				}else{
					Element nonmorph = new Element("nonMorph");
					nonmorph.setText(parts[0]);
					doc.addContent(nonmorph);
					morph.setAttribute("id", pID);
					morph.setText(parts[1]);
					doc.addContent(morph);				
					if(parts.length==3){
						nonmorph = new Element("nonMorph");
						nonmorph.setText(parts[2]);
						doc.addContent(nonmorph);
					}
				}
				write2file(desfolder, pID+".txt", morph.getText());
			}
			Element nonmorph = new Element("nonMorph");
			nonmorph.setText(paras.get(pID));
			doc.addContent(nonmorph);
		}
		//write xml to transformed
		try {
			Document xml = new Document(doc);
			BufferedOutputStream out = new BufferedOutputStream(
					new FileOutputStream(new File(trafolder, fname+".xml")));
			/* Producer */
			outputter.output(doc, out);
		} catch (IOException e) {
			LOGGER.error("Exception in Type3PreMarkup : output", e);
			throw new ParsingException(e);
		}

	}

	private void write2file(File desfolder, String fname, String text) {
		try{
			BufferedWriter out = new BufferedWriter(
					new FileWriter(new File(desfolder, fname)));
			out.write(text);
			out.flush();
			out.close();
		}catch(IOException e){
			LOGGER.error("Exception in Type3PreMarkup.write2file", e);
		}
	}

	private File createFolderIn(File target, String foldername) {
		File nfile = new File(target, foldername);
		if(nfile.mkdir()){
			return nfile;
		}else{
			nfile.renameTo(new File(nfile.getName()+""+System.currentTimeMillis()));
			if(nfile.mkdir()){
				return nfile;
			}
		}
		return nfile;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//save to-be-annotated files to source folder 
		Type3PreMarkup tpm = new Type3PreMarkup(null, null, null, "ant", null);
		tpm.bootstrapMorphDes();
		tpm.output2Target();
	}

}
