/*
 * The files should be run in the following order
 * V24_Genusextractor
 * V24_Extractor
 * V24_Transformer
 * FNATaxonNameFinalizerStep1
 * 
 */
package fna.parsing;

import org.apache.log4j.Logger;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class V24_Transformer {
	/*
	 * Do the complete formatting of the files other than the names
	 * Names processing will be done in FNATaxonNameFinalizerStep1
	 */
	private static final Logger LOGGER = Logger.getLogger(V24_Transformer.class);
	Element treatment = new Element("treatment");
	private boolean debugref = false;
	String keystorage = "";
	int keydetecter = 0;
	int keylast=0;
	static int count;
	static boolean isfamily = false;
	static Hashtable hashtable = new Hashtable();
	public static void main(String[] args) throws Exception{
		int testkey=1;//Modifies keys only when it is 1.
		ObjectOutputStream outputStream = null;
		//outputStream = new ObjectOutputStream(new FileOutputStream("d:/Library Project/V8/namemapping.bin"));
		outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\FNA24\\namemapping.bin"));
		String taxonname = null;
		Hashtable mapping = new Hashtable();
		//File extracted = new File("d:/Library Project/V8/Extracted");
		File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\FNA24\\target\\Extracted");
		File[] files = extracted.listFiles();
		for(int i = 1; i<=files.length; i++){
			count = i;
			SAXBuilder builder = new SAXBuilder();
			//Document doc = builder.build("d:/Library Project/V8/Extracted/" + i + ".xml");
			Document doc = builder.build("C:\\Users\\mohankrishna89\\Desktop\\FNA24\\target\\Extracted\\" + i + ".xml");
			//Document doc = builder.build("C:\\Users\\mohankrishna89\\Desktop\\FNA24\\target\\Extracted\\" + "16.xml");
			Element root = doc.getRootElement();
			isfamily = true;
			
			
			/*Need to fix all the broken paragraphs*/
			root = fix_paragraphs(root);
			/*End of Need to fix all the broken paragraphs*/
			
			
			
			
			
			//List paralist = XPath.selectNodes(root, "/treatment/paragraph");
			List paralist = XPath.selectNodes(root, "paragraph");
			//System.out.println(paralist.get(4).toString());
			V24_Transformer transformer = new V24_Transformer();
			transformer.createtreatment();
			
				taxonname = transformer.processparagraph(paralist);
				if(taxonname == null){
					taxonname = "taxon name";
				}
				System.out.println(taxonname);
				mapping.put(i, taxonname);

			//Have to write code for modifying keys here.
			if(testkey==1)
			{
				Element newtreatment=transformer.treatment;
				List<Element> elements = XPath.selectNodes(newtreatment, "./Key");
				if(elements.size()>0){//contains key
					furtherMarkupKeys(newtreatment);
				}
				transformer.treatment=newtreatment;
			}
			//transformer.outputauthor(i);
			//End mohan code
			//transformer.output(i);
			transformer.output(i);
		}
		outputStream.writeObject(mapping);
	}
	
	
	private String processparagraph(List paralist) throws Exception{
		String taxonname = null;
		Iterator paraiter = paralist.iterator();
		boolean filebeg = true; //filebeginning
		int familydetecter = 0;
		int syndetecter=0;
		int authdetecter=0;
		
		
		while(paraiter.hasNext()){
			int bolddetecter = 0;
			int smallcaps = 0;
			int tabdetecter =0;
			Element pe = (Element)paraiter.next();
			
			List contentlist = pe.getChildren();
			Iterator contentiter = contentlist.iterator();
			String text = "";
			while(contentiter.hasNext()){
				Element te = (Element)contentiter.next();
				if(te.getName()=="text"){
					text = text + te.getText();
				}
				if(te.getName()=="bold"){
					//if(te.getText().length()!=0)
					bolddetecter = 1;
				}
				if(te.getName()=="tab")//adding a space for every tag encountered
				{
					//text = text + " ";
					text = text + " ### ";
					tabdetecter =1;
				}
				if(te.getName()=="right"){
					//if(te.getText().length()!=0)
					authdetecter = 1;
				}
				if(te.getName()=="smallcaps"){
					//if(te.getText().length()!=0)
					smallcaps = 1;
				}
				
			}
			
			//text = text.replaceAll("\n", " ").replaceAll("\\s+", " ").replaceAll("•", "******");
			text = text.replaceAll("\n", " ").replaceAll("\\s+", " ").replaceAll("•\\s+", "•");
			
			//Once a key is reached all the following elements will be keys 
			/*if(keylast==1){//Key without next step
				Element key = new Element("Key");
				key.setText(text);
				treatment.addContent(key);
				keylast=1;
				continue;
			}*/
			
			//to match the synonyms
			//if((syndetecter==1)&&(!text.matches("(Genus|Species|Genera|Varieties) (ca)?.+"))&&(bolddetecter!=1))	//To prevent matching Varieties ca. 4 (1 in the flora): North America, Eurasia.
			if((syndetecter==1)&&(!text.matches("(Genus|Species|Genera|Varieties) (ca)?.+"))&&(bolddetecter!=1)&&(authdetecter!=1)&&(smallcaps!=1)&&(!(text.matches(".*\\[p\\..*\\].*")||text.matches(".*\\[not.*\\].*"))))
			{
				text = text.replaceAll(" ### ", " ");
				if(text.matches("\\s*Grass Family\\s*")){
					Element commonname = new Element("common_name");
					commonname.setText(text);
					treatment.addContent(commonname);
					continue;
				}
				else if(text.matches("\\s*Grass Phylogeny Working Group\\s*")){
					Element commonname = new Element("common_name");
					commonname.setText(text);
					treatment.addContent(commonname);
					continue;
				}else if(text.length()>75||text.matches("\\d.*")){
					Element discussion = new Element("discussion");
					text = text.replaceAll(" ### ", " ");
					treatment.addContent(discussion);
					continue;
				}else if(text.matches("^[A-Z]{4}.+[a-z]")){
					String[] synchunks = new String[2];
					synchunks=text.split(";");
					for(int x=0;x<synchunks.length;x++)
					{
						Element synname = new Element("synonym");
						synname.setText(synchunks[x]);
						treatment.addContent(synname);
					}
					syndetecter=0;
					continue;
				}else if(text.matches("^[A-Z]{4}.+")||text.matches("^[A-Z][a-z][A-Z].*")){//to match PlNEWOODS NEEDLEGRASS
					String[] synchunks = new String[2];
					synchunks=text.split(";|,");
					for(int x=0;x<synchunks.length;x++)
					{
						Element synname = new Element("common_name");
						synname.setText(synchunks[x]);
						treatment.addContent(synname);
					}
					continue;
				}else if((text.matches("^[A-Z][a-z]+\\s+[A-Z].*")||text.matches("^[A-Z]\\.[A-Z]\\..*"))&&(!text.matches(".*\\d.*"))){//to match J.K. Wipff
					
					addauthor(text,treatment);
					/*Element authname = new Element("author");
					authname.setText(text);
					treatment.addContent(authname);*/
					continue;
				}else{
					Element otherinfo = new Element("discussion");
					text = text.replaceAll(" ### ", " ");
					otherinfo.setText(text);
					treatment.addContent(otherinfo);
					System.out.println("**********************************************"+text+"**********************************************");
					continue;
				}
				
			}
			if((syndetecter==1)&&(!text.matches("(Genus|Species|Genera|Varieties) (ca)?.+"))&&(bolddetecter!=1)&&(authdetecter!=1)&&(smallcaps==1))
			{
				text = text.replaceAll(" ### ", " ");
				String[] synchunks = new String[2];
				synchunks=text.split(";|,");
				for(int x=0;x<synchunks.length;x++)
				{
					Element synname = new Element("common_name");
					synname.setText(synchunks[x]);
					treatment.addContent(synname);
				}
				continue;
			}

			syndetecter=0;
			
			//System.out.println(text);

			if(text.matches("^•.*")){
				text = text.replaceAll(" ### ", " ");
				Element commonname = new Element("common_name");
				commonname.setText(text.replace("•", ""));
				treatment.addContent(commonname);
			}

			//if((text.contains("Family")|text.contains("family"))&text.matches("[A-Z]+.+")){//Family Name
			//if((text.contains("Family")|text.contains("family"))&text.matches("^[A-Z]{4}.+")){//To prevent matching every sentence with family and starting with caps

			//else if(familydetecter == 1)
			else if(((familydetecter == 1 && !text.matches("SELECTED REFERENCE.+") && (bolddetecter != 1))||(authdetecter==1))&&(keylast!=1)&&(!text.matches(".*\\d.*"))
					&&(text.matches("^[A-Z][a-z]+\\s+[A-Z].*")||text.matches("^[A-Z]\\.[A-Z]\\..*"))){//Author// To prevent matching SELECTED REFERENCE in 9.xml
				/*Element author = new Element("author");
				author.setText(text);
				treatment.addContent(author);*/
				text = text.replaceAll(" ### ", " ");
				
				addauthor(text,treatment);
				
				authdetecter=0;
				//familydetecter = 0;
			}
			else if(text.matches("SELECTED REFERENCE.+")&&(keylast!=1)){//References as it is the root tag
				text = text.replaceAll(" ### ", " ");
				String heading="";
				if(text.contains("SELECTED REFERENCES"))
				{
					heading="SELECTED REFERENCES";
					text=text.replaceFirst("SELECTED REFERENCES", "").trim();
				}
				else
				{
					heading="SELECTED REFERENCE";
					text=text.replaceFirst("SELECTED REFERENCE", "").trim();
				}
				Element reference = new Element("references");
				//reference.setAttribute("Heading",heading);
				reference.setAttribute("heading",heading);
				reference.setText(text);
				furtherMarkupReference(reference);
				treatment.addContent(reference);
			}


			else if(((bolddetecter == 1&&!text.matches("[0-9]+\\w*\\..+")&&keylast!=1&&(!(text.matches(".*\\[p\\..*\\].*")||text.matches(".*\\[not.*\\].*"))))||(text.matches("(Plant|Culm).*")&&keylast!=1))&&!filebeg){//Description
				text = text.replaceAll(" ### ", " ");
				Element description = new Element("description");
				//code to write the descriptions to a separate file
				//File descriptionout = new File("C:/Users/mohankrishna89/Desktop/Library Project/V3/source/test1/description/" + filename + ".txt");//why am i doing this?
				File descriptionout = new File("C:\\Users\\mohankrishna89\\Desktop\\FNA24\\target\\description\\" + count + ".txt");//why am i doing this?
				descriptionout.delete();
				FileWriter fw = new FileWriter(descriptionout, true);
				fw.append(text + "\r\n");
				fw.flush();
				
				//end mohan code

				description.setText(text);
				treatment.addContent(description);
				bolddetecter=0;
				familydetecter = 0;
			}

			//else if((bolddetecter == 1&&text.matches("\\d+\\w*\\..+"))||(text.matches("[0-9]+\\. {0,5}[A-Z]{2}.+"))){//Species name
			//else if((bolddetecter == 1&&text.matches("\\d+\\w*\\..+"))||((text.matches("[0-9]+\\. {0,5}[A-Z]{2}.+")||text.matches("[0-9]+\\w\\.[A-Z].+"))&&(!text.contains("-")))){//Species name
			else if((bolddetecter == 1&&text.matches("\\d+\\w*\\..+"))||((text.matches("[0-9]+\\. {0,5}[A-Z]{2}.+")||text.matches("[0-9]+\\w\\.[A-Z].+"))&&(!text.contains("-")))||text.matches(".*\\[p\\..*\\].*")||text.matches(".*\\[not.*\\].*")||filebeg){//Species name
				text = text.replaceAll(" ### ", " ");
				filebeg = false;
			syndetecter=1;
			String ill_common_name ="";
			String illustration = "";
			String common_name = "";
			
			Pattern icp = Pattern.compile("(\\[.*\\])(.*)"); //to match Alopecurus rendlei Eig [p. 785] Rendle's Meadow Foxtail
			Matcher mcp = icp.matcher(text);
			
			if(mcp.find()){
				ill_common_name = mcp.group();
				illustration = mcp.group(1);
				common_name = mcp.group(2);
				text = text.replace(ill_common_name, "");	
			}
			/*
			 * Must remove the illustration and common names from text before processing.
			 */
				
				//mohan code to match the number and mark it
				//Pattern p = Pattern.compile("(^\\d+\\w*\\.)(.*)");
				Pattern p = Pattern.compile("^((\\d+\\w*\\.(\\d+.?)?)+)(.*)"); //to match 2b.3. Ranunculus Linnaeus 
				Matcher m = p.matcher(text);
				if(m.matches()){
					String number = m.group(1).replaceAll("\\s", "").trim(); //in case an extra space is there
					Element num = new Element("number");
					num.setText(number);
					treatment.addContent(num);
					//text= m.group(2).trim();
					text= m.group(4).trim();//to match 2b.3. Ranunculus Linnaeus 
				}
				String[] chunks = new String[2];
				chunks=text.split("(\\s-)|(\\s·)");//to match Vancouveria planipetala Calloni, Malpighia 1: 266, plate 6. 1887 · Redwood-ivy, redwood inside-out flower · E
				int i=chunks.length;
				//end mohan code
				text=chunks[0];//contains the name and publication
				String[] spchunks = new String[1];
				spchunks=text.split(",");
				int s=0;
				String pub="";//string for publication
				for(s=0;s<spchunks.length;s++)
				{
					if(s==0)
					{
						if(spchunks[s].contains("var."))
						{
							Element varietyname = new Element("variety_name");
							varietyname.setText(spchunks[s]);
							treatment.addContent(varietyname);	
						}
						else if(spchunks[s].contains("subsp."))
						{
							Element subspeciesname = new Element("subspecies_name");
							subspeciesname.setText(spchunks[s]);
							treatment.addContent(subspeciesname);	
						}
						else if(spchunks[s].contains("subsect."))
						{
							Element subsectionname = new Element("subsection_name");
							subsectionname.setText(spchunks[s]);
							treatment.addContent(subsectionname);	
						}
						else if(spchunks[s].contains("sect."))
						{
							Element sectionname = new Element("section_name");
							sectionname.setText(spchunks[s]);
							treatment.addContent(sectionname);	
						}
						else if(spchunks[s].contains("subg."))
						{
							Element subgenusname = new Element("subgenus_name");
							subgenusname.setText(spchunks[s]);
							treatment.addContent(subgenusname);	
						}
						else if(spchunks[s].matches("(^[A-Z]+\\s.*)")&&isfamily)
						{
							isfamily = false;
							Element familyname = new Element("family_name");
							familyname.setText(spchunks[s]);
							treatment.addContent(familyname);	
						}
						else if(spchunks[s].matches("(^[A-Z]+\\s.*)"))
						{
							Element genusname = new Element("genus_name");
							genusname.setText(spchunks[s]);
							treatment.addContent(genusname);	
						}
						else
						{
							Element speciesname = new Element("species_name");
							speciesname.setText(spchunks[s]);
							treatment.addContent(speciesname);
						}
						taxonname=spchunks[s];
							
					}
					else
					{
						//pub+=spchunks[s]+",";
						pub+=spchunks[s]+",";
						
					}
				}
				if(pub.length()!=0)
				{
					String[] pubchunks = new String[0];
					pubchunks=pub.split(";");
					for(int p1=0;p1<pubchunks.length;p1++)
					{
						String[] titlechunks = new String[1];
						titlechunks=pubchunks[p1].split("[\\d].*");
						String publtitl=titlechunks[0];
						int inlength=titlechunks[0].length();
						//String inpubl=pubchunks[p1].substring(inlength, pubchunks[p1].length()-1);
						String inpubl="";
						//if(inlength<pubchunks[p1].length())//just to check that pubchunks[p1] is not empty.
						if(inlength<=pubchunks[p1].length())//just to check that pubchunks[p1] is not empty.
						//inpubl=pubchunks[p1].substring(inlength, pubchunks[p1].length()-1);
							inpubl=pubchunks[p1].substring(inlength, pubchunks[p1].length());
						else
							System.out.println("Problem in publication:"+count);
						Element pubname = new Element("place_of_publication");
						Element publ_title = new Element("publication_title");
						publ_title.setText(publtitl);
						pubname.addContent(publ_title);
						
						Element in_publication = new Element("place_in_publication");
						in_publication.setText(inpubl);
						pubname.addContent(in_publication);
						
						//pubname.setText(pubchunks[p1]);
						//if(pubname.getText().length()!=0)
						//{
							treatment.addContent(pubname);
						//}
					}

				}
				for(int j=1;j<i;j++)
				{
					if(chunks[j].matches("(([A-Z]*\\d*)\\s*)*"))
					{
						String token=chunks[j];
						Element tokenname = new Element("token");
						tokenname.setText(token);
						treatment.addContent(tokenname);
					}
					else
					{
						String cname=chunks[j].trim();	
						String[] cnamechunks = new String[2];
						cnamechunks=cname.split("\\[");
						int k=cnamechunks.length;
						for (int m1=0;m1<k;m1++)
						{
							String collname=cnamechunks[m1];
							if(collname.contains("]"))
							{
								collname='['+collname;
								Element etyname = new Element("etymology");
								etyname.setText(collname);
								treatment.addContent(etyname);
							}
							else if(collname.length()!=0)//to prevent empty <Common_name\>
							{
								String[] common = new String[2];
								common=collname.split(",");
								for(int q=0;q<common.length;q++)
								{
									Element comname = new Element("common_name");
									comname.setText(common[q]);
									treatment.addContent(comname);
								}
							}
							
						}
					
					}

			}
				if(illustration.length()!=0){
					Element illu = new Element("illustrated");
					illu.setText(illustration.replace("[", "").replace("]", ""));
					treatment.addContent(illu);
				}
				if(common_name.length()!=0){
					String[] synchunks = new String[2];
					synchunks=common_name.split(";|,");
					for(int x=0;x<synchunks.length;x++)
					{
						Element synname = new Element("common_name");
						synname.setText(synchunks[x]);
						treatment.addContent(synname);
					}
				}
			}
			else{
				if(text.matches("[0-9]+\\..+\\.")){//Key without next step
					text=hashText(text);//to add the  ### symbols
					Element key = new Element("Key");
					key.setText(text);
					treatment.addContent(key);
					keylast=1;
				}
				//else if(text.matches("[0-9]+\\.[A-Z]+.+")&&text.contains("   ")){//Key with next step
				else if(text.matches("[0-9]+\\.\\s*[A-Z]+.+")&&text.contains("   ")){//Key with next step
					text=hashText(text);//to add the  ### symbols
					Element key = new Element("Key");
					key.setText(text);
					treatment.addContent(key);
					keylast=1;
				}
				else if(text.matches("\\[\\d.+Shifted to.+\\]"))//to match [33. Shifted to left margin.—Ed.]
				{
					text=hashText(text);//to add the  ### symbols
					Element key = new Element("Key");
					key.setText(text);
					treatment.addContent(key);
					keylast=1;
				}
				else if(text.matches("[0-9]+\\..+")&&!text.contains("   ")&&!text.matches(".+\\.")){//Key need to combine
					text=hashText(text);//to add the  ### symbols
					keystorage = text;
					keydetecter = 1;
					keylast=1;
					Element key = new Element("Key");
					key.setText(text);
					treatment.addContent(key);
				}
				else if(keylast == 1)//default key case. Once a key is there nothing else follows so make it Key
				{
					text=hashText(text);//to add the  ### symbols
					Element key = new Element("Key");
					key.setText(text);
					treatment.addContent(key);	
				}
				else{//Discussion
					text = text.replaceAll(" ### ", " ");
					if(text.matches("\\s*Grass Family\\s*")){
						Element commonname = new Element("common_name");
						commonname.setText(text);
						treatment.addContent(commonname);
					}else{
					Element discussion = new Element("discussion");
					discussion.setText(text);
					if(discussion.getText().length()!=0)//if discussion is not empty
					treatment.addContent(discussion);
					}
				}
			}
		}
		taxonname=count+" "+taxonname;
		return taxonname;
	}
	

	private void output(int i) throws Exception {
		XMLOutputter outputter = new XMLOutputter();
		//String file = "d:/Library Project/V8/Transformed/" + i + ".xml";
		String file = "C:\\Users\\mohankrishna89\\Desktop\\FNA24\\target\\Transformed\\" + i + ".xml";
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\FNA24\\target\\Transformed\\" + i +taxonname+ ".xml";
		Document doc = new Document(treatment);
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(file));
		outputter.output(doc, out);
	}
	private void createtreatment() throws Exception{
		treatment = new Element("treatment");
	}
	
	
	/*
	 * To write all the author names to a file
	 * 
	 */
	private void outputauthor(int i) throws Exception {
		//XMLOutputter outputter = new XMLOutputter();
		//String file = "d:/Library Project/V8/Transformed/" + i + ".xml";
		String file = "C:\\Users\\mohankrishna89\\Desktop\\FNA24\\author.txt";
		try{
			  // Create file 
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
			  List<Element> authorlist = treatment.getChildren("author");
			  for(Element author: authorlist){
				  out.println(i+"  "+author.getText());
				}
			  //out.write("Hello Java");
			  //Close the output stream
			  out.close();
			}catch (Exception e){//Catch exception if any
			  System.err.println("Error: " + e.getMessage());
			  }

	}
	
	/*
	 * Given a string and a treatment, parses the string and adds it as <author> to the treatment
	 */
	
	private void addauthor(String text, Element treatment){
		Pattern p = Pattern.compile("(([A-Z][a-z]+\\s+)?(([A-Z]\\.)+\\s+)([A-Z][a-z]+\\s*)(,\\s+Jr\\.\\s*)?)");
		Matcher m = p.matcher(text);
		while(m.find()){
			String authortext = m.group();
			Element authname = new Element("author");
			authname.setText(authortext);
			treatment.addContent(authname);
			text = text.replace(authortext, "");
			m = p.matcher(text);	
		}
		if(text.trim().length()!=0){
			Element authname = new Element("author");
			authname.setText(text);
			treatment.addContent(authname);
		}
	}
	
	/**
	 * turn
	 * <references>SELECTED REFERENCES Behnke, H.-D., C. Chang, I. J. Eifert, and T. J. Mabry. 1974. Betalains and P-type sieve-tube plastids in Petiveria and Agdestis (Phytolaccaceae). Taxon 23: 541–542. Brown, G. K. and G. S. Varadarajan. 1985. Studies in Caryophyllales I: Re-evaluation of classification of Phytolaccaceae s.l. Syst. Bot. 10: 49–63. Heimerl, A. 1934. Phytolaccaceae. In: H. G. A. Engler et al., eds. 1924+. Die natürlichen Pflanzenfamilien…, ed. 2. 26+ vols. Leipzig and Berlin. Vol. 16c, pp. 135–164. Nowicke, J. W. 1968. Palynotaxonomic study of the Phytolaccaceae. Ann. Missouri Bot. Gard. 55: 294–364. Rogers, G. K. 1985. The genera of Phytolaccaceae in the southeastern United States. J. Arnold Arbor. 66: 1–37. Thieret, J. W. 1966b. Seeds of some United States Phytolaccaceae and Aizoaceae. Sida 2: 352–360. Walter, H. P. H. 1906. Die Diagramme der Phytolaccaceen. Leipzig. [Preprinted from Bot. Jahrb. Syst. 37(suppl.): 1–57.] Walter, H. P. H. 1909. Phytolaccaceae. In: H. G. A. Engler, ed. 1900–1953. Das Pflanzenreich…. 107 vols. Berlin. Vol. 39[IV,83], pp. 1–154. Wilson, P. 1932. Petiveriaceae. In: N. L. Britton et al., eds. 1905+. North American Flora…. 47+ vols. New York. Vol. 21, pp. 257–266.</references>
	 * to
	 * <references><reference>Behnke, H.-D., C. Chang, I. J. Eifert, and T. J. Mabry. 1974. Betalains and P-type sieve-tube plastids in Petiveria and Agdestis (Phytolaccaceae). Taxon 23: 541–542. </reference> <reference>...</reference>....</references>
	 * @param ref
	 * @return
	 */
	private void furtherMarkupReference(Element ref) {
		//Element marked = new Element("references");
		String text = ref.getText();
		ref.setText("");
		if(this.debugref) System.out.println("\nReferences text:"+text);
		
		//Pattern p = Pattern.compile("(.*?\\d+–\\d+\\.\\]?)(\\s+[A-Z]\\w+,.*)");
		Pattern p = Pattern.compile("(^[^;]*)(;.*)");
		Matcher m = p.matcher(text);
		while(m.matches()){
			String refstring = m.group(1);
			Element refitem = new Element("reference");
			refitem.setText(refstring);
			ref.addContent(refitem);
			if(this.debugref) System.out.println("a ref:"+refstring);
			text = m.group(2).replaceFirst(";", "");
			m = p.matcher(text);
		}
		Element refitem = new Element("reference");
		//refitem.setText("item:"+text);
		refitem.setText(text);
		ref.addContent(refitem);
		if(this.debugref) System.out.println("a ref:"+text);
		//ref.getParentElement().addContent(marked);
		//ref.detach();	
	}
	
	
	
	
	/**
	 * First assemble the key element(s) <key></key>
	 * Then turn individual statement :
	 *  <key>2. Carpels and stamens more than 5; plants perennial; leaves alternate; inflorescences ax-</key>
  	 *	<key>illary, terminal, or leaf-opposed racemes or spikes ### 3. Phytolac ca ### (in part), p. 6</key>
     * to:
     * <key_statement>
     * <statement_id>2</statement_id>
     * <statement>Carpels and stamens more than 5; 
     * plants perennial; leaves alternate; inflorescences ax-illary, terminal, 
     * or leaf-opposed racemes or spikes</statement>
     * <determination>3. Phytolacca (in part), p. 6</determination>
     * </key_statement>
     * 
     * <determination> is optional, and may be replaced by <next_statement_id>.
	 * @param treatment
	 */
	private static void furtherMarkupKeys(Element treatment) {
		assembleKeys(treatment);
		try{
			List<Element> keys = XPath.selectNodes(treatment, "./TaxonKey");
			for(Element key: keys){
				furtherMarkupKeyStatements(key);
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
		
	}
	
	/* Turn individual statement :
	 *  <key>2. Carpels and stamens more than 5; plants perennial; leaves alternate; inflorescences ax-</key>
  	 *	<key>illary, terminal, or leaf-opposed racemes or spikes ### 3. Phytolac ca ### (in part), p. 6</key>
     * To:
     * <key_statement>
     * <statement_id>2</statement_id>
     * <statement>Carpels and stamens more than 5; 
     * plants perennial; leaves alternate; inflorescences ax-illary, terminal, 
     * or leaf-opposed racemes or spikes</statement>
     * <determination>3. Phytolacca (in part), p. 6</determination>
     * </key_statement>
     * 
     * <determination> is optional, and may be replaced by <next_statement_id>.
	 * @param treatment
	 */
	private static void furtherMarkupKeyStatements(Element taxonkey) {
		ArrayList<Element> allstatements = new ArrayList<Element>();
		Element marked = new Element("key");
		List<Element> states = taxonkey.getChildren();
		//Pattern p1 = Pattern.compile("(.*?)(( ### [\\d ]+[a-z]?\\.| ?#* ?Group +\\d).*)");//determ
		Pattern p1 = Pattern.compile("(.*?)((### [\\d ]+[a-z]?\\.| ?#* ?Group +\\d).*)");//determ
		Pattern p2 = Pattern.compile("^([\\d ]+[a-z]?\\..*?) (.? ?[A-Z].*)");//id   2. "Ray” corollas
		String determ = null;
		String id = "";
		String broken = "";
		String preid = null;
		//process statements backwards
		for(int i = states.size()-1; i>=0; i--){
			Element state = states.get(i);
			//if(state.getName().compareTo("key") == 0 || state.getName().compareTo("couplet") == 0){
			if(state.getName().compareTo("Key") == 0){
				String text = state.getTextTrim()+broken;
				Matcher m = p1.matcher(text);
				if(m.matches()){
					text = m.group(1).trim();
					determ = m.group(2).trim();
				}
				m = p2.matcher(text);
				if(m.matches()){//good, statement starts with an id
					id = m.group(1).trim();
					text = m.group(2).trim();
					broken = "";
					//form a statement
					Element statement = new Element("key_statement");
					Element stateid = new Element("statement_id");
					stateid.setText(id.replaceAll("\\s*###\\s*", ""));
					Element stmt = new Element("statement");
					stmt.setText(text.replaceAll("\\s*###\\s*", ""));
					Element dtm = null;
					Element nextid = null;
					if(determ!=null) {
						dtm = new Element("determination");
						dtm.setText(determ.replaceAll("\\s*###\\s*", ""));
						determ = null;
					}else if(preid!=null){
						nextid = new Element("next_statement_id");
						nextid.setText(preid.replaceAll("\\s*###\\s*", ""));
						//preid = null;
					}
					preid = id;
					statement.addContent(stateid);
					statement.addContent(stmt);
					if(dtm!=null) statement.addContent(dtm);
					if(nextid!=null) statement.addContent(nextid);
					allstatements.add(statement);
				}else if(text.matches("^[a-z]+.*")){//a broken statement, save it
					broken =" "+ text;
				}
			}else{
				Element stateclone = (Element)state.clone();
				if(stateclone.getName().compareTo("run_in_sidehead")==0){
					stateclone.setName("key_head");
				}
				allstatements.add(stateclone);//"discussion" remains
			}
		}
		
		for(int i = allstatements.size()-1; i >=0; i--){
			marked.addContent(allstatements.get(i));
		}		
		taxonkey.getParentElement().addContent(marked);
		taxonkey.detach();
	}


	/**
	 * <treatment>
	 * <...>
	 * <references>...</references>
	 * <key>...</key>
	 * </treatment>
	 * deals with two cases:
	 * 1. the treatment contains one key with a set of "key/couplet" statements (no run_in_sidehead tags)
	 * 2. the treatment contains multiple keys that are started with <run_in_sidehead>Key to xxx (which may be also used to tag other content)
	 * @param treatment
	 */
	private static void assembleKeys(Element treatment) {
		Element key = null;
		//removing individual statements from treatment and putting them in key
		List<Element> children = treatment.getChildren();////changes to treatment children affect elements too.
		Element[] elements = children.toArray(new Element[0]); //take a snapshot
		ArrayList<Element> detacheds = new ArrayList<Element>();
		boolean foundkey = false;
		for(int i = 0; i < elements.length; i++){
			Element e = elements[i];
			/*if(e.getName().compareTo("run_in_sidehead")==0 && (e.getTextTrim().startsWith("Key to ") || e.getTextTrim().matches("Group \\d+.*"))){
				foundkey = true;
				if(key!=null){
					treatment.addContent((Element)key.clone());	
				}
				key = new Element("TaxonKey");
			}*/
			//if(!foundkey && (e.getName().compareTo("key")==0 || e.getName().compareTo("couplet")==0)){
			if(!foundkey && (e.getName().compareTo("Key")==0)){
				foundkey = true;	
				if(key==null){
					key = new Element("TaxonKey");
				}
			}
			if(foundkey){
				detacheds.add(e);
				key.addContent((Element)e.clone());
			}			
		}
		if(key!=null){
			treatment.addContent(key);					
		}
		for(Element e: detacheds){
			e.detach();
		}
	}
	
	/*
	 * Used to insert the ### where needed for the keys
	 */
	private String hashText(String text)
	{
		//Pattern p1=Pattern.compile("(\\d+\\..*?)(\\d+\\w*\\.\\s+.*)");
		Pattern p1=Pattern.compile("(.*?)(\\d+\\w*\\.\\s\\s\\s.*)");
		Matcher m = p1.matcher(text);
		if(m.matches())
		{
			//String textbeg=m.group(1).trim();
			String textbeg=m.group(1);
			String textend=m.group(2).trim();
			textend=" ### "+textend;
			text=textbeg+textend;
		}
		return text;
	}
	
	
	/*
	 * Fix_paragraphs is used to fix any broken paragraphs.
	 * Any paragraph starting with neither a capital letter or a number is merged with its previous paragraph
	 */
	
	private static Element fix_paragraphs(Element treatment) {
		//Element key = null;
		//removing individual statements from treatment and putting them in key
		List<Element> children = treatment.getChildren();////changes to treatment children affect elements too.
		Element[] elements = children.toArray(new Element[0]); //take a snapshot
		//ArrayList<Element> detacheds = new ArrayList<Element>();
		//boolean foundkey = false;
		
		Element newtreatment = new Element("treatment");
		for(int i = 0 ; i < elements.length; i++){
			Element e = elements[i];
			
			if(e.getContentSize()!=0){
				String text = get_paragraph_text(e);
				text = text.replaceAll("\n", " ").replaceAll("\\s+", " ");
				//Pattern p = Pattern.compile("^(\\s*[a-z])(.*)"); //to match paragraphs which start with a small letter
				//Pattern p = Pattern.compile("([^0-9A-Z])(.*)"); //to match paragraphs which start with anything other than a digit or number
				Pattern p = Pattern.compile("(([^0-9A-Z])|(\\d+(\\.\\d+)?-\\d+(\\.\\d+)?))(.*)"); //to match paragraphs which start with anything other than a digit or number
				Matcher m = p.matcher(text.trim());
				if(m.matches()){
					Element lastparagraph = (Element) newtreatment.getContent(newtreatment.getContentSize()-1); //get the last element in newtreatment and merge it with the current element
					lastparagraph = merge_elements (lastparagraph,e);
					newtreatment.removeContent(newtreatment.getContentSize()-1);
					newtreatment.addContent(lastparagraph);
					
				}else{
					newtreatment.addContent(e.detach());
				}
			}

		}
		return newtreatment;
	}
	
private static String get_paragraph_text(Element paragraph){
	List contentlist = paragraph.getChildren();
	Iterator contentiter = contentlist.iterator();
	String text = "";
	while(contentiter.hasNext()){
		Element te = (Element)contentiter.next();
		if(te.getName()=="text"){
			text = text + te.getText();
		}
		/*else if(te.getName()=="tab"){
			text = text +" ### ";
		}*/
	}
	return text;	
}

private static Element merge_elements(Element prevelement,Element currentelement){
	List<Element> children = currentelement.getChildren();////changes to treatment children affect elements too.
	Element[] elements = children.toArray(new Element[0]); //take a snapshot
	for(int i = 0 ; i < elements.length; i++){
		prevelement.addContent(elements[i].detach());
	}
	return prevelement;	
}



}
