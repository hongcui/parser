package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

public class V25_Extractor {
	Element treatment = new Element("treatment");
	static int i=0;
	//static int j=0;
	//protected XMLOutputter outputter;

	public static void main(String[] args) throws Exception {
		//int count = 1;
		V25_Extractor extractor = new V25_Extractor();
		
		
		//Document doc = builder.build("d:/Library Project/V8/document.xml");
		//Document doc = builder.build("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V8\\source\\FNA08 word11.xml");
		File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\FNA25\\target\\genus-extracted");
		File[] files = extracted.listFiles();
		for(int f = 1; f<=files.length; f++){
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build("C:\\Users\\mohankrishna89\\Desktop\\FNA25\\target\\genus-extracted\\" + f + ".xml");
			Element root = doc.getRootElement();
			List paralist = XPath.selectNodes(root, "paragraph");
			Iterator iter = paralist.iterator();
			while (iter.hasNext()) {
				extractor.processParagraph((Element) iter.next());
				//break;
			}
			if(extractor.treatment.getContentSize()!=0){
				i++;
				System.out.println(i +"  "+ extractor.firstparagraphtext(extractor.treatment));
				extractor.output(i);
				extractor.createtreatment();
			}

		}
		
		//List wpList = XPath.selectNodes(root, "/w:document/w:body/w:p");
		//List wpList = XPath.selectNodes(root, "pkg:part/pkg:xmlData/w:document/w:body/w:p");
		

		//Document doc1 = new Document(treatment);
		//XMLOutputter outputter = new XMLOutputter();
		//String file = "d:/result.xml";
		//Document doc1 = new Document(extractor.treatment);
		//BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		//outputter.output(doc1, out);
	}

	private void processParagraph(Element wp) throws Exception {
		Element pe = new Element("paragraph");
		Boolean isbold = false;
		Boolean issmallcaps = false;
		StringBuffer buffer = new StringBuffer();
			List wrlist=wp.getChildren();
			Iterator riter = wrlist.iterator();
			while (riter.hasNext()) {			
				
				Element r = (Element) riter.next();
				if(r.getName()=="bold"){
					isbold = true;
				}
				if(r.getName()=="smallcaps"){
					issmallcaps = true;
				}


				if(r.getName()=="text"){
					buffer.append(r.getText());
				}

			}

		String text = buffer.toString().replaceAll("\\s+", " ");
		text=text.trim();
		
		
			

		
		/*if((text.matches("^\\d+\\w*\\.\\s*\\w+\\s+.*")&&(isbold||issmallcaps))||
				((text.matches("^\\d+\\w*\\..*(sect|subfam|var|subgen|subg|subsp|ser|tribe)\\..*\\d+\\s*\\.\\s*\\d+.*")&&isbold))||
				(text.matches("^\\d+\\w*\\..*(sect|subfam|var|subgen|subg|subsp|ser|tribe)\\..*")&&issmallcaps)||(text.matches("^\\d+\\w*\\.\\s*\\w+\\s*[A-Z]{2}[A-Z]*\\s.*"))||
				(text.matches("(^\\d+\\w*\\.\\s*)\\w+.*(\\[.*\\])"))||(text.matches("([A-Z][a-z]+\\s+).*((sect|subfam|var|subgen|subg|subsp|ser|tribe)\\.).*(\\[.*\\])"))||
				(text.matches("([A-Z][a-z]+\\s+)(\\w+).*(\\[.*\\])"))||(text.matches("([A-Z][a-z]+\\s+)(\\w+)\\s+[A-Z]\\.\\s(\\[.*\\]).*")&&isbold&&issmallcaps)||
				(text.matches("[A-Z]\\w+\\s*(sect|subfam|var|subgen|subg|subsp|ser|tribe)\\..*\\(.*\\).*\\."))||(text.matches("[A-Z]\\w+.*(sect|subfam|var|subgen|subg|subsp|ser|tribe)\\..*(?<!\\.)\\b"))){*/
		
			/*if((text.matches("^\\d+\\w*\\.\\s*\\w+\\s+.*")&&(isbold||issmallcaps))||
					((text.matches("^\\d+\\w*\\..*(sect|subfam|var|subgen|subg|subsp|ser|tribe)\\..*\\d+\\s*\\.\\s*\\d+.*")&&isbold))||
					(text.matches("^\\d+\\w*\\..*(sect|subfam|var|subgen|subg|subsp|ser|tribe)\\..*")&&issmallcaps)||(text.matches("^\\d+\\w*\\.\\s*\\w+\\s*[A-Z]{2}[A-Z]*\\s.*"))||
					(text.matches("(^\\d+\\w*\\.\\s*)\\w+.*(\\[.*\\])"))||(text.matches("([A-Z][a-z]+\\s+).*((sect|subfam|var|subgen|subg|subsp|ser|tribe)\\.).*(\\[.*\\])"))||
					(text.matches("([A-Z][a-z]+\\s+)(\\w+).*(\\[.*\\])"))||(text.matches("([A-Z][a-z]+\\s+)(\\w+)\\s+[A-Z]\\.\\s(\\[.*\\]).*")&&isbold&&issmallcaps)||
					(text.matches("[A-Z]\\w+\\s*(sect|subfam|var|subgen|subg|subsp|ser|tribe)\\..*\\(.*\\).*\\."))||(text.matches(".*\\[p\\.\\s\\d+\\].*"))){*/
		if(((text.matches("^\\d+\\w*\\.\\s*\\w+\\s+.*")&&(isbold||issmallcaps))||
				((text.matches("^\\d+\\w*\\..*(sect|subfam|var|subgen|subg|subsp|ser|tribe)\\..*\\d+\\s*\\.\\s*\\d+.*")&&isbold))||
				(text.matches("^\\d+\\w*\\..*(sect|subfam|var|subgen|subg|subsp|ser|tribe)\\..*")&&issmallcaps)||(text.matches("^\\d+\\w*\\.\\s*\\w+\\s*[A-Z]{2}[A-Z]*\\s.*"))||
				(text.matches("(^\\d+\\w*\\.\\s*)\\w+.*(\\[.*\\])"))||(text.matches("([A-Z][a-z]+\\s+).*((sect|subfam|var|subgen|subg|subsp|ser|tribe)\\.).*(\\[.*\\])"))||
				(text.matches("([A-Z][a-z]+\\s+)(\\w+).*(\\[.*\\])"))||(text.matches("([A-Z][a-z]+\\s+)(\\w+)\\s+[A-Z]\\.\\s(\\[.*\\]).*")&&isbold&&issmallcaps)||
				(text.matches("[A-Z]\\w+\\s*(sect|subfam|var|subgen|subg|subsp|ser|tribe)\\..*\\(.*\\).*\\."))||(text.matches(".+\\[p\\.\\s\\d+\\].*")))&&(text.length()<170)){
				
			//Note: This creates a new file whenever we have a bold in the sentence and the sentence starts with a digit.One instance in FNAV_27 is "leaf apex variously both papillose and toothed; median leaf cells with horned papillae on both surfaces varying to nearly smooth</text><text>; </text><bold /><text>cells on adaxial surface of the costa elongate (3–6:1),"
			//Such cases need to be manually corrected by removing the </w:b> tag in the source XML file. Another instance in FNAV_27 is "Vegetative leaves usually lanceolate to ovate-lanceolate, acute to short-acuminate; distal laminal cells usually 8</text><text>–</text><bold /><text>12 mm; le"
			if(!text.contains("SELECTED REFERENCE")){
				if(treatment.getContentSize()!=0){
				i++;
				
				//System.out.println();
				//System.out.println("*******"+text);
				
				//System.out.println("Volume:"+i);
				System.out.println(i +"  "+ firstparagraphtext(treatment));
				output(i);
				createtreatment();
				}
			}
		}
		treatment.addContent(wp.detach());
	}

	private void output(int i) throws Exception {
		XMLOutputter outputter = new XMLOutputter();
		//String file = "d:/Library Project/V8/Extracted/" + i + ".xml";
		String file = "C:\\Users\\mohankrishna89\\Desktop\\FNA25\\target\\Extracted\\" + i + ".xml";
		Document doc = new Document(treatment);
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(file));
		outputter.output(doc, out);
	}
	
	private void createtreatment() throws Exception{
		treatment = new Element("treatment");
	}
	
	
	private Element getrowelement(Element r) throws Exception{
		if(r.getName()=="r")
		{
			return r;
		}
		else{
			List l1=r.getChildren();
			Iterator loriter = l1.iterator();
			Element r1=(Element) loriter.next();
			Element r2=getrowelement(r1);
			return r2;
		}
	}
	/*
	 * Used to return the string of the first paragraph
	 */
	private String firstparagraphtext(Element treatment){
		Element firstParagraph = treatment.getChild("paragraph");
		List contentlist = firstParagraph.getChildren();
		Iterator contentiter = contentlist.iterator();
		String text = "";
		while(contentiter.hasNext()){
			Element te = (Element)contentiter.next();
			if(te.getName()=="text"){
				text = text + te.getText();
			}
		}
		return text;
		
	}
	
	
}

