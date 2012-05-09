package fna.parsing;

import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class V27_Transformer {
	Element treatment = new Element("treatment");
	private boolean debugref = false;
	String keystorage = "";
	int keydetecter = 0;
	int keylast=0;
	static int partdetecter, count;
	static Hashtable hashtable = new Hashtable();
	public static void main(String[] args) throws Exception{
		ObjectOutputStream outputStream = null;
		//outputStream = new ObjectOutputStream(new FileOutputStream("d:/Library Project/V8/namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V8\\namemapping.bin"));
		outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V27\\namemapping.bin"));
		String taxonname = null;
		Hashtable mapping = new Hashtable();
		//File extracted = new File("d:/Library Project/V8/Extracted");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V8\\target\\Extracted");
		File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V27\\target\\Extracted");
		File[] files = extracted.listFiles();
		for(int i = 1; i<=files.length; i++){
			count = i;
			SAXBuilder builder = new SAXBuilder();
			//Document doc = builder.build("d:/Library Project/V8/Extracted/" + i + ".xml");
			//Document doc = builder.build("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V8\\target\\Extracted\\" + i + ".xml");
			//Document doc = builder.build("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V8\\target\\Extracted\\" + "16.xml");
			Document doc = builder.build("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V27\\target\\Extracted\\" + i + ".xml");
			Element root = doc.getRootElement();
			List paralist = XPath.selectNodes(root, "/treatment/paragraph");
			//System.out.println(paralist.get(4).toString());
			V27_Transformer transformer = new V27_Transformer();
			transformer.createtreatment();
			if(partdetecter == 0){
				taxonname = transformer.processparagraph(paralist);
				if(taxonname == null){
					taxonname = "taxon name";
				}
				System.out.println(taxonname);
				mapping.put(i, taxonname);
			}else{
				transformer.processparagraph2(paralist);
			}
			//transformer.output(i);
			transformer.output(i);
		}
		outputStream.writeObject(mapping);
	}
	
	
	private String processparagraph(List paralist) throws Exception{
		String taxonname = null;
		Iterator paraiter = paralist.iterator();
		int familydetecter = 0;
		int syndetecter=0;
		int authdetecter=0;
		while(paraiter.hasNext()){
			int bolddetecter = 0;
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
					text = text + " ";
				}
				if(te.getName()=="right"){
					//if(te.getText().length()!=0)
					authdetecter = 1;
				}
				
			}
			
			//Once a key is reached all the following elements will be keys 
			/*if(keylast==1){//Key without next step
				Element key = new Element("Key");
				key.setText(text);
				treatment.addContent(key);
				keylast=1;
				continue;
			}*/
			
			//to match the synonyms
			if(text.matches("Excluded Species:"))
			{
				Element disc = new Element ("Discussion");
				disc.setText(text);
				treatment.addContent(disc);
				continue;
			}
			//if((syndetecter==1)&&(!text.matches("(Genus|Species|Genera|Varieties) (ca)?.+"))&&(bolddetecter!=1))	//To prevent matching Varieties ca. 4 (1 in the flora): North America, Eurasia.
			if((syndetecter==1)&&(!text.matches("(Genus|Species|Genera|Varieties) (ca)?.+"))&&(bolddetecter!=1)&&(authdetecter!=1))
			{
				String[] synchunks = new String[2];
				synchunks=text.split(";");
				for(int x=0;x<synchunks.length;x++)
				{
					Element synname = new Element("Synonym");
					synname.setText(synchunks[x]);
					treatment.addContent(synname);
				}
				syndetecter=0;
				continue;
			}

			syndetecter=0;
			
			//System.out.println(text);
			if(text.contains("that are not pertinent in this volume")){
				partdetecter = 1;
			}
			//if((text.contains("Family")|text.contains("family"))&text.matches("[A-Z]+.+")){//Family Name
			//if((text.contains("Family")|text.contains("family"))&text.matches("^[A-Z]{4}.+")){//To prevent matching every sentence with family and starting with caps
			if((text.contains("Family")|text.contains("family"))&&(text.matches("^[A-Z]{4}.+"))&& (!text.matches("SELECTED REFERENCE.+"))){//To prevent matching every sentence with family and starting with caps
				familydetecter = 1;
				String[] famchunks=new String[0];
				famchunks=text.split("-|·");
				for(int f=0;f<famchunks.length;f++)
				{
					if(f==0)
					{
						Element familyname = new Element("Family_Name");
						familyname.setText(famchunks[f]);
						treatment.addContent(familyname);
						taxonname = famchunks[f];
					}
					else
					{
						Element commonname = new Element("Common_name");
						commonname.setText(famchunks[f]);
						treatment.addContent(commonname);
					}
				}
				
			}
			//else if(familydetecter == 1)
			else if((familydetecter == 1 && !text.matches("SELECTED REFERENCE.+") && (bolddetecter != 1))||(authdetecter==1)){//Author// To prevent matching SELECTED REFERENCE in 9.xml
				Element author = new Element("Author");
				author.setText(text);
				treatment.addContent(author);
				authdetecter=0;
				//familydetecter = 0;
			}
			else if(text.matches("SELECTED REFERENCE.+")){//References as it is the root tag
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
				Element reference = new Element("References");
				reference.setAttribute("Heading",heading);
				reference.setText(text);
				furtherMarkupReference(reference);
				treatment.addContent(reference);
			}
			else if(text.matches("(Genus|Species|Varieties|Subspecies|Genera) (ca)?.+")&&(text.contains("):"))){//number of infrataxa// to prevent matching Species of ribes in 10.xml
				//to match Genera 4 or 5, species ca. 34 (1 in the flora): worldwide in tropical and subtopical regions
				/*Element infrataxa = new Element("Number_of_Infrataxa");
				infrataxa.setText(text);
				treatment.addContent(infrataxa);*/
				String[] newchunks=new String[0];
				newchunks=text.split(":");
				for(int k=0;k<newchunks.length;k++)
				{
					if(k==0)
					{
						Element infrataxa = new Element("Number_of_Infrataxa");
						infrataxa.setText(newchunks[k]+":");
						treatment.addContent(infrataxa);

					}
					else
					{
						Element distribution = new Element("Distribution");
						distribution.setText(newchunks[k]);
						treatment.addContent(distribution);
					}
				}
			}

									/*else if(text.matches("[0-9]+\\. {0,5}[A-Z]{2}.+")){//Gene name
				Element genename = new Element("Gene_Name");
				genename.setText(text);
				treatment.addContent(genename);
				taxonname = text;
			}*/

			else if(text.matches("Flowering.+|Fruiting.+")&text.contains(";")){//Distribution

				Element floweringtime = new Element("Flowering_Time");
				Element habitat = new Element("Habitat");
				Element conservation = new Element("Conservation");
				Element elevation = new Element("Elevation");
				Element distribution = new Element("Distribution");
				String flowtime = null, habi = null, eleva=null, distri="", conserv=null, fh = null;
				String[] semi = new String[4];
				String[] dot = new String[3];
				semi = text.split(";");
				
				for(int i = 0; i<semi.length;i++)
				{
						if(semi[i].contains("Flowering")){
							if(semi[i].contains(".")){
								dot = semi[i].split("\\.");
								flowtime = dot[0];
								habi = dot[1];
								//File habitatout = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V8\\target\\habitat\\" + count + ".txt");
								File habitatout = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V27\\target\\habitat\\" + count + ".txt");
								habitatout.delete();
								FileWriter fw = new FileWriter(habitatout, true);
								fw.append(habi + "\r\n");
								fw.flush();
								floweringtime.setText(flowtime);
								habitat.setText(habi);
							}
							else
							{
								floweringtime.setText(semi[i]);
							}
							
						}
						else if(semi[i].matches("\\s*fruiting.+"))//if fruiting exists it has to be added to flowering i.e. to match "Flowering early spring-summer; fruiting spring-summer. Coastal plains and westward; 10-1500 m; Tex.; Mexico (Coahuila, Durango, and Nuevo León)."
						{
							if(semi[i].contains(".")){
								dot = semi[i].split("\\.");
								flowtime = dot[0];
								habi = dot[1];
								//File habitatout = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V8\\target\\habitat\\" + count + ".txt");
								File habitatout = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V27\\target\\habitat\\" + count + ".txt");
								habitatout.delete();
								FileWriter fw = new FileWriter(habitatout, true);
								fw.append(habi + "\r\n");
								fw.flush();
								String fruiting=floweringtime.getText();
								fruiting+=";"+flowtime;
								floweringtime.setText(fruiting);
								habitat.setText(habi);
							}
							else
							{
								String fruiting=floweringtime.getText();
								fruiting+=";"+semi[i];
								floweringtime.setText(fruiting);
							}
						}
						else if(semi[i].contains("conservation concern")){
							conserv = semi[i];
							conservation.setText(conserv);
						}
						else if(semi[i].matches(".+\\s?m")){
							eleva = semi[i];
							elevation.setText(eleva);
							
						}
						else{
							//distri = semi[i];
							distri += semi[i]+";"; //to add additional distributions // to add the missing ; but have one extra ; at the end
							//distri = distri.replaceFirst(";$", "");
							distribution.setText(distri);
							//File distributionout = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V8\\target\\distribution\\" + count + ".txt");
							File distributionout = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V27\\target\\distribution\\" + count + ".txt");
							distributionout.delete();
							FileWriter fw = new FileWriter(distributionout, false);
							fw.append(distri + "\r\n");
							fw.flush();
						}
						
					}				
					if(floweringtime.getText().length()!=0)
						treatment.addContent(floweringtime);
					if(habitat.getText().length()!=0)
						treatment.addContent(habitat);
					if(conservation.getText().length()!=0)
					{
						treatment.addContent(conservation);
					}
					if(elevation.getText().length()!=0)
						treatment.addContent(elevation);
					if(distribution.getText().length()!=0)
					{
						distri = distri.replaceFirst(";$", ""); // to remove the extra ; added at the end before
						distribution.setText(distri);
						treatment.addContent(distribution);
					}

			}
			else if(bolddetecter == 1&!text.matches("[0-9]+\\w*\\..+")){//Description
				Element description = new Element("Description");
				//code to write the descriptions to a separate file
				//File descriptionout = new File("C:/Users/mohankrishna89/Desktop/Library Project/V3/source/test1/description/" + filename + ".txt");//why am i doing this?
				//File descriptionout = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V8\\target\\description\\" + count + ".txt");//why am i doing this?
				File descriptionout = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V27\\target\\description\\" + count + ".txt");//why am i doing this?
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
			else if((bolddetecter == 1&&text.matches("\\d+\\w*\\..+"))||(text.matches("[0-9]+\\. {0,5}[A-Z]{2}.+")||text.matches("[0-9]+\\w\\.[A-Z].+"))){//Species name
			syndetecter=1;
				
				//mohan code to match the number and mark it
				//Pattern p = Pattern.compile("(^\\d+\\w*\\.)(.*)");
				Pattern p = Pattern.compile("^((\\d+\\w*\\.)+)(.*)"); //to match 2b.3. Ranunculus Linnaeus 
				Matcher m = p.matcher(text);
				if(m.matches()){
					String number = m.group(1).replaceAll("\\s", "").trim(); //in case an extra space is there
					Element num = new Element("Number");
					num.setText(number);
					treatment.addContent(num);
					//text= m.group(2).trim();
					text= m.group(3).trim();//to match 2b.3. Ranunculus Linnaeus 
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
							Element varietyname = new Element("Variety_Name");
							varietyname.setText(spchunks[s]);
							treatment.addContent(varietyname);	
						}
						else if(spchunks[s].contains("subsp."))
						{
							Element subspeciesname = new Element("Sub_Species_Name");
							subspeciesname.setText(spchunks[s]);
							treatment.addContent(subspeciesname);	
						}
						else if(spchunks[s].contains("subsect."))
						{
							Element subsectionname = new Element("Sub_Section_Name");
							subsectionname.setText(spchunks[s]);
							treatment.addContent(subsectionname);	
						}
						else if(spchunks[s].contains("sect."))
						{
							Element sectionname = new Element("Section_Name");
							sectionname.setText(spchunks[s]);
							treatment.addContent(sectionname);	
						}
						else if(spchunks[s].contains("subg."))
						{
							Element subgenusname = new Element("Sub_Genus_Name");
							subgenusname.setText(spchunks[s]);
							treatment.addContent(subgenusname);	
						}
						else if(spchunks[s].matches("(^[A-Z]+\\s.*)"))
						{
							Element genusname = new Element("Genus_Name");
							genusname.setText(spchunks[s]);
							treatment.addContent(genusname);	
						}
						else
						{
							Element speciesname = new Element("Species_Name");
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
						Element pubname = new Element("Publication");
						Element publ_title = new Element("Publication_Title");
						publ_title.setText(publtitl);
						pubname.addContent(publ_title);
						
						Element in_publication = new Element("Place_In_Publication");
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
						Element tokenname = new Element("Token");
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
								Element etyname = new Element("Etymology");
								etyname.setText(collname);
								treatment.addContent(etyname);
							}
							else if(collname.length()!=0)//to prevent empty <Common_name\>
							{
								String[] common = new String[2];
								common=collname.split(",");
								for(int q=0;q<common.length;q++)
								{
									Element comname = new Element("Common_name");
									comname.setText(common[q]);
									treatment.addContent(comname);
								}
							}
							
						}
					
					}

			}
			}
			else{
				if(text.matches("[0-9]+\\..+\\.")){//Key without next step
					Element key = new Element("Key");
					key.setText(text);
					treatment.addContent(key);
					keylast=1;
				}
				//else if(text.matches("[0-9]+\\.[A-Z]+.+")&&text.contains("   ")){//Key with next step
				else if(text.matches("[0-9]+\\.\\s*[A-Z]+.+")&&text.contains("   ")){//Key with next step
					Element key = new Element("Key");
					key.setText(text);
					treatment.addContent(key);
					keylast=1;
				}
				else if(text.matches("\\[\\d.+Shifted to.+\\]"))//to match [33. Shifted to left margin.—Ed.]
				{
					Element key = new Element("Key");
					key.setText(text);
					treatment.addContent(key);
					keylast=1;
				}
				else if(text.matches("[0-9]+\\..+")&&!text.contains("   ")&&!text.matches(".+\\.")){//Key need to combine
					keystorage = text;
					keydetecter = 1;
					keylast=1;
				}
				else if(keydetecter == 1){//Combine key
					text = keystorage + text;

					Element key = new Element("Key");
					key.setText(text);
					treatment.addContent(key);
					
					keydetecter = 0;
					keylast=1;
				}
				else if(keylast == 1)//default key case. Once a key is there nothing else follows so make it Key
				{
					Element key = new Element("Key");
					key.setText(text);
					treatment.addContent(key);	
				}
				else{//Discussion
					Element discussion = new Element("Discussion");
					discussion.setText(text);
					if(discussion.getText().length()!=0)//if discussion is not empty
					treatment.addContent(discussion);
				}
			}
		}
		taxonname=count+" "+taxonname;
		return taxonname;
	}
	
	private void processparagraph2(List paralist) throws Exception{
		Iterator paraiter = paralist.iterator();
		while(paraiter.hasNext()){
			Element pe = (Element)paraiter.next();
			List contentlist = pe.getChildren();
			Iterator contentiter = contentlist.iterator();
			String text = "";
			while(contentiter.hasNext()){
				Element te = (Element)contentiter.next();
				if(te.getName()=="text"){
					text = text + te.getText();
				}
			}
			//System.out.println(text);
			if(text.contains("=")){
				Element discussion = new Element("Discussion");
				discussion.setText(text);
				treatment.addContent(discussion);
				String[] university = new String[3];
				String[] name = new String[3];
				String shortname, fullname;
				university = text.split("=");
				shortname = university[0];
				if(university[1].contains(";")){
					name = university[1].split(";");
					fullname = name[0];
				}else{
					fullname = university[1].replace(".", "");
				}
				hashtable.put(shortname, fullname);
			}else{
				Element reference = new Element("Reference");
				for(Iterator itr = hashtable.keySet().iterator(); itr.hasNext();){
					String key = (String) itr.next();
					String value = (String) hashtable.get(key);
					if(text.contains(key)){
						text =  text.replace(key, value);
					}
				}
				reference.setText(text);
				treatment.addContent(reference);
			}
		}
	}
	private void output(int i) throws Exception {
		XMLOutputter outputter = new XMLOutputter();
		//String file = "d:/Library Project/V8/Transformed/" + i + ".xml";
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V8\\target\\Transformed\\" + i + ".xml";
		String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V27\\target\\Transformed\\" + i + ".xml";
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V8\\target\\Transformed\\" + i +taxonname+ ".xml";
		Document doc = new Document(treatment);
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(file));
		outputter.output(doc, out);
	}
	private void createtreatment() throws Exception{
		treatment = new Element("treatment");
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
		Pattern p = Pattern.compile("(.*?\\d+–\\d+\\.\\]?)(\\s+[A-Z]\\w+,.*)");
		Matcher m = p.matcher(text);
		while(m.matches()){
			String refstring = m.group(1);
			Element refitem = new Element("reference");
			refitem.setText(refstring);
			ref.addContent(refitem);
			if(this.debugref) System.out.println("a ref:"+refstring);
			text = m.group(2);
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

}
