/**
 * 
 */
package fna.parsing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.jdom.Content;
import org.jdom.Text;

import fna.db.*;

/**
 * @author hongcui
 * split taxonX documents to smaller units, each resulting xml document contains 1 treatment.
 * 
 */
@SuppressWarnings({ "unchecked", "unused" })
public class Type4Transformer extends Thread {
	//private File source =new File(Registry.SourceDirectory); //a folder of text documents to be annotated
	private File source = new File(Registry.SourceDirectory);
	//File target = new File(Registry.TargetDirectory);

	//File target = new File("Z:\\DATA\\Plazi\\2ndFetchFromPlazi\\target-taxonX-ants-trash");
	//private String tableprefix = "plazi_ants";

	//target folder
	File target = new File(Registry.TargetDirectory);
	//private String tableprefix = "plazi_ants";
	

	private XMLOutputter outputter = null;
	// this is the dataprfix from general tab
	private String dataprefix = null;
	private ProcessListener listener;
	protected static final Logger LOGGER = Logger.getLogger(Type3Transformer.class);
	/**
	 * 
	 */
	
	public Type4Transformer(ProcessListener listener, String dataprefix) {
		this.listener = listener;
		this.dataprefix = dataprefix;
		/* Remove this hardcoding later*/
		//dataprefix = "plazi_ants";
	}
	
	public Type4Transformer() {
		if(!target.exists()){
			target.mkdir();
		}
		
		File d = new File(target, "descriptions");
		if(!d.exists()){
			d.mkdir();
		}
		
		File t = new File(target, "transformed");
		if(!t.exists()){
			t.mkdir();
		}
		
		
	}
	
	public void run(){
		listener.setProgressBarVisible(true);
		transform();
		listener.setProgressBarVisible(false);
	}
	
	public void transform(){
		File[] files =  source.listFiles();
		Hashtable<String, String> filemapping = new Hashtable<String, String>();
    	//read in taxonX documents from source
		int number = 0;
		try{
			SAXBuilder builder = new SAXBuilder();
			listener.progress(1);
			for(int f = 0; f < files.length; f++) {
				listener.progress((100*(f+1))/files.length);
				//create renaming mapping table
				int fn = f+1;
				System.out.println (files[f].getName()+" to "+ (f+1)+".xml");
				filemapping.put(files[f].getName(), (f+1)+".xml");
				
				//split by treatment
				Document doc = builder.build(files[f]);
				Element root = doc.getRootElement();
				List<Element> treatments = XPath.selectNodes(root,"/tax:taxonx/tax:taxonxBody/tax:treatment");
				//detach all but one treatments from doc
				ArrayList<Element> saved = new ArrayList<Element>();
				for(int t = 1; t<treatments.size(); t++){
					Element e = treatments.get(t);
					doc.removeContent(e);
					e.detach();
					saved.add(e);
				}
				//now doc is a template to create other treatment files
				//root.detach();
				formatDescription((Element)XPath.selectSingleNode(root,"/tax:taxonx/tax:taxonxBody/tax:treatment"), fn, 0);
				root.detach();
				writeTreatment2Transformed(root, fn, 0);
				listener.info((number++)+"", fn+"_0.xml");
		        getDescriptionFrom(root,fn, 0);
				//replace treatement in doc with a new treatment in saved
				Iterator<Element> it = saved.iterator();
				int count = 1;
				while(it.hasNext()){
					Element e = it.next();
					Element body = root.getChild("taxonxBody", root.getNamespace());
					Element treatment = (Element) XPath.selectSingleNode(
						root,"/tax:taxonx/tax:taxonxBody/tax:treatment");	
					//in treatment/div[@type="description"], replace <tax:p> tag with <description pid="1.txtp436_1.txt">
					int index = body.indexOf(treatment);
					e = formatDescription(e, fn, count);
					body.setContent(index, e);
					//write each treatment as a file in the target/transfromed folder
					//write description text in the target/description folder
					root.detach();
					writeTreatment2Transformed(root, fn, count);
					listener.info((number++)+"", fn+"_"+count+".xml");
					getDescriptionFrom(root, fn, count);
					count++;
				}
				/* Show on GUI here*/
				
			//listener.info((f+1)+"", (f+1)+".xml");
				
			}
		}catch(Exception e){
			e.printStackTrace();
			LOGGER.error("Type4Transformer : error.", e);
		}
		
		Type4TransformerDbAccessor t4tdb = new Type4TransformerDbAccessor("filenamemapping", dataprefix);
		t4tdb.addRecords(filemapping);
		
	}

	private Element formatDescription(Element treatment, int fn, int count) {
		try{
			Element description = (Element)XPath.selectSingleNode(treatment, ".//tax:div[@type='description']");
			if(description==null){
				return treatment;
			}else{
				List<Element> ps = XPath.selectNodes(description, ".//tax:p");
				Iterator<Element> it = ps.iterator();
				int i = 0;
				while(it.hasNext()){
					Element p = it.next();
					p.setName("description");
					p.setAttribute("pid", fn+"_"+count+".txtp"+i);
					p.setNamespace(null);
					i++;
				}
				return treatment;
			}
		}catch(Exception e){
			e.printStackTrace();
			LOGGER.error("Type4Transformer : error.", e);
		}
		return null;
	}

	private void getDescriptionFrom(Element root, int fn,  int count) {
		// TODO Auto-generated method stub
		try{
		List<Element> divs = XPath.selectNodes(root, "/tax:taxonx/tax:taxonxBody/tax:treatment/tax:div");
		Iterator<Element> it = divs.iterator();
		int i = 0;
		while(it.hasNext()){
			Element div = it.next();
			if(div.getAttributeValue("type").compareToIgnoreCase("description")==0){
				//List<Element> ps = div.getChildren("p", div.getNamespace());
				List<Element> ps = div.getChildren("description");
				Iterator<Element> t = ps.iterator();
				while(t.hasNext()){
					Element p = t.next();
					int size = p.getContentSize();
					StringBuffer sb = new StringBuffer();
					for(int c = 0; c < size; c++){
						Content cont = p.getContent(c);
						if(cont instanceof Element){
							sb.append(((Element)cont).getTextNormalize()+" ");
						}else if(cont instanceof Text){
							sb.append(((Text)cont).getTextNormalize()+" ");
						}
					}
					
					//writeDescription2Descriptions(sb.toString(), fn+"_"+count+"_"+i); //record the position for each paragraph.
					writeDescription2Descriptions(sb.toString(), fn+"_"+count+".txtp"+i); //record the position for each paragraph.
					i++;
				}
			}
		}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private void writeDescription2Descriptions(String textNormalize, String fn) {
		try {
			File file = new File(target+"/descriptions", fn+ ".txt");
			
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(textNormalize);
			out.close(); // don't forget to close the output stream!!!
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("Failed to output text file in Type4Transformer:outputDescriptionText", e);
			throw new ParsingException("Failed to output text file.", e);
		}
		
	}

	private void writeTreatment2Transformed(Element root, int fn, int count) {
		// TODO Auto-generated method stub
		ParsingUtil.outputXML(root, new File(target+"/transformed", fn+"_"+count+".xml"));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Type4Transformer t4t = new Type4Transformer();
		t4t.transform();
	}

}
