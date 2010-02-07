package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

/**
 * Hong 08/04/09
 * revised for FoC volumes
 * 	a) add start, names, tribegenusnamestyle private properties.
 *  b) if(treatment.indexOf(new Element("text"))>=0){ =>added this condition to filter out empty files.
 * Hong 10/7/08:
 * 	a) record "smallcaps" for genus/tribe names
 * 	this is necessary when a taxonlist is not provided
 * 	with "smallcaps" info in the extracted records, VolumeVerifier can build a taxon index for VolumeTransformer.
 *  b) also keep the original delimiters in names: may be useful for VolumeVerifier. 
 * 
 * Chunshui summer 08:
 * To extract the data from the docx file.
 * 
 * The functions include: 1, (TODO)extract the document.xml from the docx file. 2,
 * parse the document.xml 3, output individual treatment in an intermediate xml
 * file.
 * 
 * Only the paragraphs enclosed in the style listed in style-mapping.properties file will be kept.
 * 
 * And save the data to an XML format listing style and text pair for each paragraph.
 * 
 * The output will be processed further by VolumeVerifier.java
 * 
 * @author chunshui
 */
public class VolumeExtractor {
	
	private String source;
	//private MainForm mainForm;
	private static final Logger LOGGER = Logger.getLogger(VolumeExtractor.class);
	
	private String target;
	
	private ProcessListener listener;

	private int count;

	private Element treatment;

	private XMLOutputter outputter;
	
	//private String start = "Name"; //TODO: include the following in the configuration file: style names indicating the start of a new treatment
	//private String syn = "Syn";
	//private String tribegennamestyle = "smallCaps";
	public static String start = ".*?(Heading|Name).*"; //starts a treatment
	//public static String start = ""; //starts a treatment
	private String names = ".*?(Syn|Name).*"; //other interesting names worth parsing
	public String tribegennamestyle = "caps";
	private static String ignorednames = "incertae sedis";
	
	public VolumeExtractor(String source, String target, ProcessListener listener) {
		this.source = source;
		this.target = target;
		this.listener = listener;
		//this.mainForm = mainForm;
		Registry.TribeGenusNameCase = tribegennamestyle;
		Registry.NomenclatureStylePtn = start;
		Registry.SynonymStylePtn = names;
	}
	/**
	 * Extract the data from the source file
	 * 
	 * TODO: unzip the document.xml from the docx file
	 */
	public void extract() throws ParsingException {
		try {
			listener.progress(1);
			// init the outputter
			outputter = new XMLOutputter(Format.getPrettyFormat());

			// build the root element from the xml file
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(source + "document.xml");
			System.out.println(source + "document.xml");
			Element root = doc.getRootElement();
			
			// find all <w:p> tags
			List wpList = XPath.selectNodes(root, "/w:document/w:body/w:p");

			// iterate over the <w:p> tags
			count = 1;
			int total = wpList.size();
			for (Iterator iter = wpList.iterator(); iter.hasNext();) {
				processParagraph((Element) iter.next());
				listener.progress((count*100) / total);
				//output();
			}

			// output the last file
			output();
		} catch (Exception e) {
			LOGGER.error("Unable to parse/ extract the file in VolumeExtractor:extract", e);
			e.printStackTrace();
			throw new ParsingException(e);
		}
	}

	/**
	 * To process a w:p tag
	 * 
	 * output style:text pairs for each paragraph
	 * @param wp
	 * @throws JDOMException
	 */
	private void processParagraph(Element wp) throws Exception {
		// read the paragraph style
		Attribute att = (Attribute) XPath.selectSingleNode(wp,
		"./w:pPr/w:pStyle/@w:val");
		if(att == null){//TODO: issue a warning
			System.out.println("============================================>null");
			return;
		}
		String style = att.getValue();
		System.out.println(style);

		Element se = new Element("style");
		se.setText(style);

		Element pe = new Element("paragraph");
		pe.addContent(se);

		// check if a name paragraph reached, assuming a treatment starts with a Name paragraph
		//if (style.indexOf("Name") >= 0) {
		if (style.matches(start)){
			// The code reaches to a name paragraph
			// output the current treatment file
			//if (treatment != null) {
			if(treatment!=null && 
					treatment.getChild("paragraph").getChild("text") != null && 
					! treatment.getChild("paragraph").getChild("text").getTextTrim().matches(".*?"+ignorednames+".*") &&
					treatment.getChildren("paragraph").size()>=2){ //must contain style and text, must contain >=2 paragraphs
				/*It is not possible for a treatment to just have a name
				 * Heading4 /Taxa incertae sedis from FoC v22, taxa whose placement is uncertain*/
				
				output(); // ready to write this treatment out
				count++;
			}
			
			// logger.info("processing: " + count);
			// create a new output file
			treatment = new Element("treatment");
		}
		
		// for non-name paragraph, just output the text content
		// build the <w:t> content
		//if(style.indexOf("Name") >=0 || style.indexOf("Syn") >=0){
		if(style.matches(start) || style.matches(names)){
			extractNameParagraph(wp, pe);
		}else{
			extractTextParagraph(wp, pe);
		}
		// add the element to the treatment (root) element
		treatment.addContent(pe);
	}

	private void extractNameParagraph(Element wp, Element pe)
			throws JDOMException {
		String acase = "";		
		List rList = XPath.selectNodes(wp, "./w:r");
		for(Iterator ti = rList.iterator(); ti.hasNext();){
			Element re = (Element)ti.next();
			//find smallCaps
			Element rpr = (Element)XPath.selectSingleNode(re, "./w:rPr"); //Genus, Tribe names are in smallCaps
			if(rpr != null && XPath.selectSingleNode(rpr, "./w:"+tribegennamestyle)!=null){
				acase = tribegennamestyle;
			}else{
				acase = "";
			}
			//collect text
			StringBuffer buffer = new StringBuffer();
			List textList = XPath.selectNodes(re, "./w:t");
			for(Iterator it = textList.iterator(); it.hasNext();){
				Element wt = (Element)it.next();
				String tmp = wt.getText();
				buffer.append(tmp).append(" ");
			}
		//}
		String text = buffer.toString().replaceAll("\\s+", " ").trim();;
		// build the elements
		Element te = null;
		if(text.matches(".*?\\S.*")){ //not an empty string or a number of spaces
			te = new Element("text");
			te.setText(text);
		}
		System.out.println("Name: "+acase+" : "+text);
		Attribute ca = null;
		if(!acase.equals("") && te != null){
			ca = new Attribute ("case", tribegennamestyle);
			te.setAttribute(ca);
		}

		if(te!=null) pe.addContent(te);
		}
	}

	private void extractTextParagraph(Element wp, Element pe) throws JDOMException {
		StringBuffer buffer=new StringBuffer();
		
		List textList = XPath.selectNodes(wp, "./w:r/w:t");
		for (Iterator ti = textList.iterator(); ti.hasNext();) {
			Element wt = (Element) ti.next();
			buffer.append(wt.getText()).append("#");
		}
		String text = buffer.toString().replaceAll("-#", "-").replaceAll("#", "").replaceAll("\\s+", " ").trim();
		
		/*			buffer.append(wt.getText()).append("-");
		}
		String text = buffer.toString().replaceAll("\\s+", " ").trim();*/
		Element te = new Element("text");
		te.setText(text);
		pe.addContent(te);
		
	}
	
	/**
	 * To output the <treatment> element
	 * 
	 * @throws IOException
	 */
	private void output() throws ParsingException {
		try {
			
			String file = target + "extracted\\" + count + ".xml";
			Document doc = new Document(treatment);
			BufferedOutputStream out = new BufferedOutputStream(
					new FileOutputStream(file));
			/* Producer */
			outputter.output(doc, out);
			
			/* Consumer */
			listener.info(count + "", file);

		} catch (IOException e) {
			LOGGER.error("Exception in VolumeExtractor : output", e);
			throw new ParsingException(e);
		}
	}
}
