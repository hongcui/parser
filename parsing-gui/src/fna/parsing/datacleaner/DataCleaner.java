/**
 * 
 */
package fna.parsing.datacleaner;

import java.util.*;
import java.io.*;

import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import fna.parsing.*;
/**
 * @author hongcui
 *
 */
public abstract class DataCleaner{
	protected Hashtable<String, String> legalvalues = new Hashtable<String, String>();
	protected ArrayList<String> sourceelements = new ArrayList<String>();
	protected File outputdir = null;
	protected File sourcedir = null;
	protected String outputelement = null;
	protected ArrayList<String> sourcecontent = new ArrayList<String>();
	/**
	 * 
	 */
	public DataCleaner(String sourcedir, ArrayList<String> sourceElements, String outputElement, String outputdir) {
		// TODO Auto-generated constructor stub
		this.sourcedir = new File(sourcedir);
		this.sourceelements = sourceElements;
		this.outputdir = new File(outputdir);
		this.outputelement = outputElement;
		collectSourceContent();
		collectLegalValues();
		cleanFiles();
	}

	/**
	 * ***********************************************************************************
	 * collect content from sourceElements in the files from sourcedir
	 * save the content text in sourcecontent
	 */
	protected void collectSourceContent(){
		File[] flist = sourcedir.listFiles();
		for(int i = 0; i<flist.length; i++){
			saveContents(flist[i]);
		}
	}
	
	private void saveContents(File source){
		try{
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(source);
			Element root = doc.getRootElement();
			
			Iterator<String> it = sourceelements.iterator();
			while(it.hasNext()){
				String ename = it.next();
				List<Element> elements = XPath.selectNodes(root, "//"+ename);
				Iterator<Element> eit = elements.iterator();
				while(eit.hasNext()){
					Element e = eit.next();
					sourcecontent.add(e.getText());					
				}
			}
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/*
	 * **************************************************************************************
	 * replace the content of each source element with its legal value
	 * replace the source element name with output element name
	 */
	protected void cleanFiles(){
		File[] flist = sourcedir.listFiles();
		for(int i = 0; i<flist.length; i++){
			cleanElements(flist[i]);
		}
	}

	

	private void cleanElements(File file) {
		try{
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(file);
			Element root = doc.getRootElement();
			root = clean(root);
			ParsingUtil.outputXML(root, new File(sourcedir, file.getName()));
		}catch (Exception e){
			e.printStackTrace();
		}
		
	}

	protected abstract Element clean(Element root);

	/**
	 * 1 text may contain multiple legal values
	 * @param text
	 * @return a set of legal values
	 */
	protected ArrayList<String> cleanText(String text) {
		ArrayList<String> values = new ArrayList<String>();
		text = text.toLowerCase();
		String[] ws = text.split("\\s+");
		for(int i = 0; i<ws.length; i++){
			String v = this.legalvalues.get(ws[i]);
			if(v!=null){
				values.add(v);
			}
		}
		return values;
	}

	/*
	 * ***********************************************************************************
	 */
	protected abstract void collectLegalValues(); 


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
