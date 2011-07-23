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

public class V3_Extractor {
	static Element treatment = new Element("treatment");

	public static void main(String[] args) throws Exception {
		//File resource = new File("d:/Library Project/work3/part2/vol03h Taxon HTML/vol03h Taxon HTML");
		File resource = new File("d:/Library Project/work3/part2/vol03m Taxon HTML/vol03m Taxon HTML");
		// new
		// File("D:/Library Project/work3/part2/vol03h Taxon HTML/vol03h Taxon HTML");
		File[] files = resource.listFiles();
		org.jsoup.select.Elements paras, bolds;
		org.jsoup.nodes.Element pe;
		org.jsoup.nodes.Document doc;
		String content = null, spicename = null,stage = "",content1 = "";
		V3_Extractor ex = new V3_Extractor();
		for (int i = 0; i < files.length; i++) {
			stage = "";
			content1="";
			if (files[i].getName().contains(".html")) {
				int selectdetecter = 0;
				doc = org.jsoup.Jsoup.parse(files[i], "UTF-8");
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
				content1 = content1.replaceAll("</*(A|B|I)\\s*(NAME=\"\\d*\")*>", "").replaceAll("\\s+", " ");
				spicename = content1 + "......name";
				para = new Element("paragraph");
				para.setText(spicename);
				treatment.addContent(para);
				while (paraiter.hasNext()) {
					boolean bool = true;
					pe = (org.jsoup.nodes.Element) paraiter.next();
					bool = pe.getElementsByTag("b").isEmpty();
					if (!pe.text().isEmpty()) {
						content = pe.text() + "......" + bool;
						if(content.contains("SELECTED REFERENCES")){
							selectdetecter = 1;
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
				ex.outputter(i+1);
				ex.createtreatment();
			}
		}
	}

	private void outputter(int filename) throws Exception {
		XMLOutputter outputter = new XMLOutputter();
		String file = "d:/Library Project/work3/part2/Extracted/" + filename
				+ ".xml";
		Document doc = new Document(treatment);
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(file));
		outputter.output(doc, out);
	}

	private void createtreatment() throws Exception {
		treatment = new Element("treatment");
	}
}

