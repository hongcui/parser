package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

public class V24_Genusextractor {
	Element treatment = new Element("treatment");
	static int i=0;
	//protected XMLOutputter outputter;

	public static void main(String[] args) throws Exception {
		int count = 1;
		V24_Genusextractor extractor = new V24_Genusextractor();
		SAXBuilder builder = new SAXBuilder();
		//Document doc = builder.build("d:/Library Project/V8/document.xml");
		//Document doc = builder.build("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V8\\source\\FNA08 word11.xml");
		Document doc = builder.build("C:\\Users\\mohankrishna89\\Desktop\\FNA24\\source\\FNA24.xml");
		Element root = doc.getRootElement();
		//List wpList = XPath.selectNodes(root, "/w:document/w:body/w:p");
		//List wpList = XPath.selectNodes(root, "pkg:part/pkg:xmlData/w:document/w:body/w:p");
		List wpList = XPath.selectNodes(root, "w:body/w:p");
		Iterator iter = wpList.iterator();
		while (iter.hasNext()) {
			extractor.processParagraph((Element) iter.next());
			//break;
		}
		i++;
		extractor.output(i);
		extractor.createtreatment();
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
		Attribute att = (Attribute) XPath.selectSingleNode(wp,"./w:pPr/w:pStyle/@w:val");
		String style = "";
		if (att != null) {
			style = att.getValue();
		}
		if (style != "") {
			Element se = new Element("style");
			se.setText(style);
			pe.addContent(se);
		}
		//mohan code to get the right tag of the authors name as right tag
		Attribute att1 = (Attribute) XPath.selectSingleNode(wp,"./w:pPr/w:jc/@w:val");
		String ri="";
		if (att1 != null) {
			ri = att1.getValue();
		}
		if(ri.contentEquals("right"))
		{
			Element se = new Element("right");
			//se.setText(ri);
			pe.addContent(se);
		}
		
		//end mohan code
		
		//Element rowtag = (Element)XPath.selectSingleNode(wp,"./w:r");
		//Element rowtag = (Element)XPath.selectSingleNode(wp,"./w:r");
		//if (rowtag != null) {
			//List wrlist = XPath.selectNodes(wp, "./w:r");
			//List wrlist = XPath.selectNodes(wp, "./w:r");
			List wrlist=wp.getChildren();
			Iterator riter = wrlist.iterator();
			while (riter.hasNext()) {
				int bid=0;
				Element r = (Element) riter.next();

				if(r.getName()=="pPr")
				{
					continue;
				}
				
				//function to get to the row element if the element is not a row element.
				else if(r.getName()!="r")
				{
					r= getrowelement(r);
				}
				// start to process tab
				Element tabe = (Element) XPath.selectSingleNode(r,"./w:tab");
				if (tabe != null) {
					Element tab = new Element("tab");
					pe.addContent(tab);
				}
				// process tab end

				/*// start to process rPr
				Element rPre = (Element) XPath.selectSingleNode(r,"./w:rPr");
				if (rPre != null) {
					Element bold = (Element) XPath.selectSingleNode(rPre,"./w:b");
					if (bold != null) {
						isbold = true;
						bold = new Element("bold");
						pe.addContent(bold);
					}
					Element italy = (Element) XPath.selectSingleNode(rPre,"./w:i");
					if (italy != null) {
						italy = new Element("italy");
						pe.addContent(italy);
					}
				}
				// process rPr end*/
				
				// start to process text
				Element te = (Element)XPath.selectSingleNode(r,"./w:t");
				if (te != null) {
					if(!te.getText().matches("((\\s+)|(\\.))"))
					{
						bid=1;
					}
					
					Element text = new Element("text");
					text.setText(te.getText());
					pe.addContent(text);
					buffer.append(te.getText());
				}
				// process text end
				
				// start to process rPr
				Element rPre = (Element) XPath.selectSingleNode(r,"./w:rPr");
				if (bid==1){
				if (rPre != null) {
					Element bold = (Element) XPath.selectSingleNode(rPre,"./w:b");
					if (bold != null) {
						isbold = true;
						bold = new Element("bold");
						pe.addContent(bold);
					}
					Element italy = (Element) XPath.selectSingleNode(rPre,"./w:i");
					if (italy != null) {
						italy = new Element("italy");
						pe.addContent(italy);
					}
					
					Element smallcaps = (Element) XPath.selectSingleNode(rPre,"./w:smallCaps");
					if (smallcaps != null) {
						issmallcaps = true;
						smallcaps = new Element("smallcaps");
						pe.addContent(smallcaps);
					}
				}
				}
				// process rPr end
				
				//if(re.getContentSize()!=0){
				//	pe.addContent(re);
				//}
			}
		//}
		//System.out.println(buffer.toString().replaceAll("\\s+", " "));
		String text = buffer.toString().replaceAll("\\s+", " ");
		text=text.trim();
		
		
		/*if((text.matches("^\\d*\\.*\\s*[A-Z]{2}.*"))||(text.matches("^\\d+\\w*\\.\\s*\\w+.*")&&isbold)||
				((text.matches("^\\d+\\w*\\..*(sect|subfam|var|subgen|subg|subsp|ser|tribe)\\..*\\d+\\s*\\.\\s*\\d+.*")&&isbold))||
				(text.matches("^\\d+\\w*\\..*(sect|subfam|var|subgen|subg|subsp|ser|tribe)\\..*")&&issmallcaps)){*/
		if(text.matches("^\\d+\\.*\\s*[A-Z]{2}.*")){
			
			//Note: This creates a new file whenever we have a bold in the sentence and the sentence starts with a digit.One instance in FNAV_27 is "leaf apex variously both papillose and toothed; median leaf cells with horned papillae on both surfaces varying to nearly smooth</text><text>; </text><bold /><text>cells on adaxial surface of the costa elongate (3–6:1),"
			//Such cases need to be manually corrected by removing the </w:b> tag in the source XML file. Another instance in FNAV_27 is "Vegetative leaves usually lanceolate to ovate-lanceolate, acute to short-acuminate; distal laminal cells usually 8</text><text>–</text><bold /><text>12 mm; le"
			if(!text.contains("SELECTED REFERENCE")){
				i++;
				System.out.println("Volume:"+i);
				output(i);
				createtreatment();
			}
		}
		treatment.addContent(pe);
	}

	private void output(int i) throws Exception {
		XMLOutputter outputter = new XMLOutputter();
		//String file = "d:/Library Project/V8/Extracted/" + i + ".xml";
		String file = "C:\\Users\\mohankrishna89\\Desktop\\FNA24\\target\\genus-extracted\\" + i + ".xml";
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
	
	
}

