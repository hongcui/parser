package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import javax.swing.JFileChooser;

import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jsoup.select.Elements;

public class V3_Extractor {
	static Element treatment = new Element("treatment");

	public static void main(String[] args) throws Exception {
		//File resource = new File("d:/Library Project/work3/part2/vol03h Taxon HTML/vol03h Taxon HTML");
		//File resource = new File("C:/Users/mohankrishna89/Desktop/Library Project/V3/source/vol03h Taxon HTML/source");
		File resource = new File("C:/Users/mohankrishna89/Desktop/Library Project/V3/source/test1/source");
		// new
		// File("D:/Library Project/work3/part2/vol03h Taxon HTML/vol03h Taxon HTML");
		File[] files = resource.listFiles();
		org.jsoup.select.Elements paras, bolds;
		org.jsoup.nodes.Element pe;
		org.jsoup.nodes.Document doc;
		String content = null, spicename = null,stage = "",content1 = "",scontent=null;
		V3_Extractor ex = new V3_Extractor();
		for (int i = 0; i < files.length; i++) {
			stage = "";
			content1="";
			if (files[i].getName().contains(".html")) {
				int selectdetecter = 0;
				doc = org.jsoup.Jsoup.parse(files[i], "UTF-8");
				//doc = org.jsoup.Jsoup.parse(files[i], null);
				paras = doc.select("p");
				bolds = doc.select("b");
				Iterator<org.jsoup.nodes.Element> paraiter = paras.iterator();
				Iterator<org.jsoup.nodes.Element> bolditer = paras.iterator();
				Element para = null;
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(files[i])));
				while((stage = br.readLine())!=null){
					if(stage.contains("<P>")){
						break;
					}
					content1 = content1 + stage + "\r\n";
				}
				//content1 = content1.replaceAll("</*(A|B|I)\\s*(NAME=\"\\d*\")*>", "").replaceAll("\\s+", " ").replaceAll("&nbsp;", " ");
				content1 = content1.replaceAll("</*(A|B|I)\\s*(NAME=\"\\d*\")*>", "").replaceAll("\\s+", " ").replaceAll("&nbsp;", " ").replaceAll("&amp;", "&");
				
				spicename = content1 + "......name";//Adding name to the end of content
				para = new Element("paragraph");
				para.setText(spicename);
				treatment.addContent(para);
				while (paraiter.hasNext()) {
					boolean bool = true;
					pe = (org.jsoup.nodes.Element) paraiter.next();
					//String new1=pe.toString();
					//mohan code to remove empty <b> tags so that the bold detector is not set in V3_Transformer.java
					Elements pe1;
					pe1 = pe.getElementsByTag("b");
					if(pe1.size()!=0)
					{
						String par=pe1.text();
						if(par.length()==0)
						pe.select("b").remove();
					}
					
					//end mohan code
					//List[] i1=pe.getElementsByTag("b");
					bool = pe.getElementsByTag("b").isEmpty();
					if (!pe.text().isEmpty()) {
						content = pe.text() + "......" + bool;
						if(content.contains("SELECTED REFERENCES")||content.contains("SELECTED REFERENCE")){// To match SELECTED REFERENCE too
							selectdetecter = 1;
							//mohan code
							content = content.replace("SELECTED REFERENCES", "").replace("......false", "");
							content = content.replace("SELECTED REFERENCE", "").replace("......false", "");// To match SELECTED REFERENCE too
							if(content.isEmpty())
							//end mohan code
							
							continue;
						}
						if(selectdetecter==1){
							content = "SELECTED REFERENCES " + content;
							selectdetecter = 0;
						}
						para = new Element("paragraph");
						para.setText(content);
						treatment.addContent(para);
					}
				}
				String fname=files[i].getCanonicalPath().replaceFirst("(.*)(\\\\)","").replaceFirst(".html", "");
				//ex.outputter(i+1);
				ex.outputter(fname);//writes the treatment generated into an XML file
				ex.createtreatment();//Creates a new treatment to be used by the next XML file
			}
		}
	}

	//private void outputter(int filename) throws Exception {
	private void outputter(String filename) throws Exception {
		XMLOutputter outputter = new XMLOutputter();
		//String file = "d:/Library Project/work3/part2/Extracted/" + filename + ".xml";
		//String file = "C:/Users/mohankrishna89/Desktop/Library Project/V3/target1/vol03h Taxon HTML" + filename + ".xml";
		//String file = "C:/Users/mohankrishna89/Desktop/Library Project/V3/source/vol03h Taxon HTML/extracted/" + filename + ".xml";
		String file = "C:/Users/mohankrishna89/Desktop/Library Project/V3/source/test1/extracted/" + filename + ".xml";
		Document doc = new Document(treatment);
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(file));
		outputter.output(doc, out);
	}

	private void createtreatment() throws Exception {
		treatment = new Element("treatment");
	}
}

