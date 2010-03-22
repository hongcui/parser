/**
 * $Id$
 */
package fna.parsing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import fna.parsing.character.CharacterLearner;

/**
 * @author chunshui
 */
public class VolumeFinalizer extends Thread {
	//glossary established in VolumeDehyphenizer
	//private String glossary;

	private ProcessListener listener;
	private Display display;
	private String dataPrefix;
	private ProgressBar progressBar;
	private static final Logger LOGGER = Logger.getLogger(VolumeFinalizer.class);

	public VolumeFinalizer(ProcessListener listener, Display display, String dataPrefix, ProgressBar progressBar) {
		//glossary = Registry.ConfigurationDirectory + "FNAGloss.txt"; // TODO
		this.listener = listener;
		this.display = display;
		this.dataPrefix = dataPrefix;
		this.progressBar = progressBar;
	}
	
	public static void main (String [] args) {
		
	}
	
	public void run () {
		outputFinal();
	}
	
	public void incrementProgressBar(final int progress) {
		display.syncExec(new Runnable() {
			public void run() {
				if(!progressBar.getVisible()) {
					progressBar.setVisible(true);
				}				
				progressBar.setSelection(progress);
			}
		});
	}
	public void outputFinal() throws ParsingException {

		incrementProgressBar(20);
		CharacterLearner cl = new CharacterLearner(ApplicationUtilities.getProperty("database.name")
				/*+ "_corpus"*/, dataPrefix);
		incrementProgressBar(40);

		cl.markupCharState();
		// output final records
		// read in treatments, replace description with what cl output
		replaceWithAnnotated(cl, "treatment/description", "FINAL", false);
	}
	
	public void replaceWithAnnotated(Learn2Parse cl, String xpath, String targetstring, boolean flatten) {
		File source = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty("TRANSFORMED"));
		int total = source.listFiles().length;

		File target = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty(targetstring));

		try {

			SAXBuilder builder = new SAXBuilder();
			for (int count = 1; count <= total; count++) {
				System.out.println("finalizing "+count);
				listener.progress(40+(count*60/total));
				File file = new File(source, count + ".xml");
				Document doc = builder.build(file);
				Element root = doc.getRootElement();	
				//create xml from annotated text
				ArrayList<String> elementstrs = cl.getMarkedDescription(count + ".txt");	
				Iterator<String> it = elementstrs.iterator();
				ArrayList<Element> content = new ArrayList<Element>();
				while(it.hasNext()){
					String descXML =(String)it.next();
					if (descXML != null && !descXML.equals("")) {
						doc = builder.build(new ByteArrayInputStream(
								descXML.getBytes("UTF-8")));
						Element descript = doc.getRootElement(); // marked-up
						descript.detach();
						content.add(descript);
					}
				}
				//get the element index of the unannotated counterpart 	
				Element description = (Element) XPath.selectSingleNode(
						root, xpath);			
				int index = root.indexOf(description);
				
				// replace
				if (index >= 0) {
					System.out.println(count+".xml has "+xpath+" element replaced");
					root.setContent(index, content);
				}
				/*System.out.println(descXML);
				if (descXML != null && !descXML.equals("")) {
					doc = builder.build(new ByteArrayInputStream(
							descXML.getBytes("UTF-8")));
					Element descript = doc.getRootElement(); // marked-up
					descript.detach();

					Element description = (Element) XPath.selectSingleNode(
							root, xpath);
				
					int index = root.indexOf(description);
					
					// replace
					if (index >= 0) {
						System.out.println(count+".xml has "+xpath+" element replaced");
						root.setContent(index, descript);
					}
				}
				*/
				root.detach();
				
				File result = new File(target, count + ".xml");
				ParsingUtil.outputXML(root, result);

				listener.info("" + count, result.getPath(), "");//TODO: test 3/19/10 
			}

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("VolumeFinalizer : Failed to output the final result.", e);
			throw new ParsingException("Failed to output the final result.", e);
		}
	}
}
