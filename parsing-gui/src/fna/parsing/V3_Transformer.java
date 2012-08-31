package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

public class V3_Transformer {
	Element treatment = new Element("treatment");
	private boolean debugref = false;
	public static void main(String[] args)throws Exception{
		ObjectOutputStream outputStream = null;
		//outputStream = new ObjectOutputStream(new FileOutputStream("d:/Library Project/work3/part2/namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:/Users/mohankrishna89/Desktop/Library Project/V3/source/vol03h Taxon HTML/namemapping.bin"));
		outputStream = new ObjectOutputStream(new FileOutputStream("C:/Users/mohankrishna89/Desktop/Library Project/V3/source/test1/namemapping.bin"));
		String taxonname = null;
		Hashtable mapping = new Hashtable();
		//File resource = new File("d:/Library Project/work3/part2/Extracted");
		//File resource = new File("C:/Users/mohankrishna89/Desktop/Library Project/V3/source/vol03h Taxon HTML/extracted");
		File resource = new File("C:/Users/mohankrishna89/Desktop/Library Project/V3/source/test1/extracted");
		String filename = null;
		File[] files = resource.listFiles();
		Document doc = null;
		for(int i = 0; i<files.length; i++){
			filename = files[i].getName();
			SAXBuilder builder = new SAXBuilder();
			doc = builder.build(files[i]);//creates a Doc tree of the tags from the extracted file
			Element root = doc.getRootElement();
			List paralist = XPath.selectNodes(root, "/treatment/paragraph");//get each paragraph from the extracted files into the LIST.
			V3_Transformer transformer = new V3_Transformer();
			transformer.createtreatment();//creates a new element <treatment>
			taxonname = transformer.processparagraph(paralist,filename);
			transformer.output(filename);
			if(taxonname == null){
				taxonname = "taxon name";
			}
			System.out.println(taxonname);
			mapping.put(i, taxonname);
		} 
		outputStream.writeObject(mapping);
	}

	private String processparagraph(List paralist, String filename)throws Exception{
		String taxonname = null;
		Iterator paraiter = paralist.iterator();
		int familydetecter = 0;
		int syndetecter=0;
		while(paraiter.hasNext()){
			int bolddetecter = 0;
			int fnamedetecter = 0;
			Element pe = (Element)paraiter.next();
			String text = pe.getText();
			//mohan code to replace &nbsp; with a space
			//text=text.replaceAll(" ", " ");
			//text=text.replaceAll(" ", " ").replaceAll("&eacute;", "é").replaceAll("&ouml;", "ö").replaceAll("&acirc;", "â").replaceAll("&ccedil;", "ç").replaceAll("&amp;", "&");
			
			V3_Transformer transformer1 = new V3_Transformer();
			text=transformer1.cleantext(text);//replaces HTML special characters with their corresponding elements.
			
			//text=text.replaceAll(" ", " ").replaceAll("×", "X").replaceAll("&eacute;", "é").replaceAll("&ouml;", "ö").replaceAll("&amp;", "&");
			//here the first " " is not a normal white space but &nbsp;
			// the × is not normal X but &#215;
			//end mohan code
			//code to check the second paragraph
			//if((syndetecter==1)&&text.contains("......true"))
			if((syndetecter==1)&&text.contains("......true")&&(!text.matches("(Genus|Species|Genera|Varieties) (ca)?.+")))	//To prevent matching Varieties ca. 4 (1 in the flora): North America, Eurasia.
			{
				text = text.replaceAll("......true", "");
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
			}
			if(text.contains("......false")){
				bolddetecter = 1;
				text = text.replaceAll("......false", "");
			}
			if(text.contains("......name")&!text.matches("\\s*\\.*name")){
				fnamedetecter = 1;
				text = text.replaceAll("......name", "");
			}
			text = text.replaceAll("......true", "");
			syndetecter=0;
			//System.out.println(text);
			if(text.contains("Family")&text.matches("[A-Z]+.+")&&!text.matches("SELECTED REFERENCE.+")){//Family Name
				familydetecter = 1;
				Element familyname = new Element("family_name");
				familyname.setText(text);
				treatment.addContent(familyname);
				taxonname = text;
			}
			else if(familydetecter == 1 ){//Author
				Element author = new Element("author");
				author.setText(text);
				treatment.addContent(author);		
				familydetecter = 0;
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
				Element reference = new Element("references");
				//reference.setAttribute("Heading",heading);
				reference.setAttribute("heading",heading);
				reference.setText(text);
				furtherMarkupReference(reference);
				treatment.addContent(reference);
			}
			else if(bolddetecter == 1&!text.matches("[0-9]+\\..+")){//Description
				Element description = new Element("description");
				
				//code to write the descriptions to a separate file
				File descriptionout = new File("C:/Users/mohankrishna89/Desktop/Library Project/V3/source/test1/description/" + filename + ".txt");//why am i doing this?
				descriptionout.delete();
				FileWriter fw = new FileWriter(descriptionout, true);
				fw.append(text + "\r\n");
				fw.flush();
				
				//end mohan code
				
				description.setText(text);
				treatment.addContent(description);
			}
		   // else if(text.matches("(Genus|Species|Genera|Varieties) (ca)?.+")){//number of infrataxa
			 else if(text.matches("(Genus|Species|Genera|Varieties|Subspecies) (ca)?.+")&&text.contains("):")){//number of infrataxa
				String[] newchunks=new String[0];
				newchunks=text.split(":");
				
		    	/*Element infrataxa = new Element("Number_of_Infrataxa");
				infrataxa.setText(newchunks[0]);
				treatment.addContent(infrataxa);*/
				
				for(int k=0;k<newchunks.length;k++)
				{
					if(k==0)
					{
						Element infrataxa = new Element("number_of_infrataxa");
						infrataxa.setText(newchunks[k]+":");
						treatment.addContent(infrataxa);

					}
					else
					{
						Element distribution = new Element("distribution");
						distribution.setText(newchunks[k]);
						treatment.addContent(distribution);
					}
				}
			}
			/*else if(text.matches("[0-9]+\\. {0,5}[A-Z]{2}.+")){//Gene name i.e digit. (upto 5 space) "STRING" anything else.
				Element genename = new Element("Gene_Name");
				genename.setText(text);
				treatment.addContent(genename);
				taxonname = text;
			}*/
			/*else if(text.matches("SELECTED REFERENCE.+")){//Reference
				Element reference = new Element("Reference");
				reference.setText(text);
				treatment.addContent(reference);
			}*/
			//else if(text.matches("Flowering.+")&text.contains(";")){//Distribution
		    else if(text.matches("Flowering.+|Fruiting.+")&text.contains(";")){//Distribution
				Element floweringtime = new Element("flowering_time");
				Element habitat = new Element("habitat");
				Element conservation = new Element("conservation");
				Element elevation = new Element("elevation");
				Element distribution = new Element("distribution");
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
								//File habitatout = new File("d:/Library Project/work3/part2/habitat/" + filename + ".txt");
								//File habitatout = new File("C:/Users/mohankrishna89/Desktop/Library Project/V3/source/vol03h Taxon HTML/habitat/" + filename + ".txt");
								File habitatout = new File("C:/Users/mohankrishna89/Desktop/Library Project/V3/source/test1/habitat/" + filename + ".txt");//why am i doing this?
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
								//File habitatout = new File("d:/Library Project/work3/part2/habitat/" + filename + ".txt");
								//File habitatout = new File("C:/Users/mohankrishna89/Desktop/Library Project/V3/source/vol03h Taxon HTML/habitat/" + filename + ".txt");
								File habitatout = new File("C:/Users/mohankrishna89/Desktop/Library Project/V3/source/test1/habitat/" + filename + ".txt");//why am i doing this?
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
						//else if(semi[i].matches(".+ m")){
						else if(semi[i].matches(".+\\s?m")){
							eleva = semi[i];
							elevation.setText(eleva);
							
						}
						//else if(semi[i].contains(".,")){
						//to match " Ont." or " Europe."
						//else if(semi[i].contains(".,")|semi[i].matches("\\s[A-Z][a-z]+\\.")){
						else{
							//distri = semi[i];
							distri += ","+semi[i]; //to add additional distributions
							distribution.setText(distri);
							
							//File distributionout = new File("d:/Library Project/work3/part2/distribution/" + filename + ".txt");
							//File distributionout = new File("C:/Users/mohankrishna89/Desktop/Library Project/V3/source/vol03h Taxon HTML/distribution/" + filename + ".txt");
							File distributionout = new File("C:/Users/mohankrishna89/Desktop/Library Project/V3/source/test1/distribution/" + filename + ".txt");
							distributionout.delete();
							//FileWriter fw = new FileWriter(distributionout, true);
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
						treatment.addContent(distribution);
					
			}
			//else if(fnamedetecter == 1){//Species name
			else if((fnamedetecter == 1)|(text.matches("[0-9]+\\. {0,5}[A-Z]{2}.+"))){//Species name or genus name but tagged as species name
				syndetecter=1;
				//mohan code to match the number and mark it
				//Pattern p = Pattern.compile("(^\\d+\\w*\\.)(.*)");
				Pattern p = Pattern.compile("^((\\d+\\w*\\.)+)(.*)"); //to match 2b.3. Ranunculus Linnaeus 
				Matcher m = p.matcher(text);
				if(m.matches()){
					String number = m.group(1).replaceAll("\\s", "").trim(); //in case an extra space is there
					Element num = new Element("number");
					num.setText(number);
					treatment.addContent(num);
					//text= m.group(2).trim();
					text= m.group(3).trim();//to match 2b.3. Ranunculus Linnaeus 
				}
				String[] chunks = new String[2];
				//chunks=text.split("\\s-\\s");
				//chunks=text.split("\\s-");//to match UMBELLULARIA (Nees) Nuttall, N. Amer. Sylv. 1: 87. 1842 -Californian bay [Latin umbellula, partial umbel]
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
							
					}
					else
					{
						pub+=spchunks[s]+",";
						/*Element pubname = new Element("Publication");
						pubname.setText(spchunks[s]);
						treatment.addContent(pubname);*/
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
						String inpubl=pubchunks[p1].substring(inlength, pubchunks[p1].length()-1);
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
				/*String[] pubchunks = new String[0];
				pubchunks=pub.split(";");
				for(int p1=0;p1<pubchunks.length;p1++)
				{
					String[] titlechunks = new String[1];
					titlechunks=pubchunks[p1].split("[\\d].*");
					String publtitl=titlechunks[0];
					int inlength=titlechunks[0].length();
					String inpubl=pubchunks[p1].substring(inlength, pubchunks[p1].length()-1);
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
				}*/
				/*if(i>1){
					String cname=chunks[1].trim();	
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
					//have to write code for processing the common names and the etymology.
				}*/
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
				/*if(i>2)
				{
					String token=chunks[2];
					Element tokenname = new Element("Token");
					tokenname.setText(token);
					treatment.addContent(tokenname);	
				}
				if(i>3)//Default condition for first paragraph. Mostly never met
				{
					int j=0;
					for(j=3;j<i;j++)
					{
						String disc=chunks[2];
						Element discname = new Element("Discussion");
						discname.setText(disc);
						treatment.addContent(discname);
					}
				}*/
				/*Element speciesname = new Element("Species_Name");
				speciesname.setText(text);
				treatment.addContent(speciesname);*/
				taxonname = text;
			}
			else{
				if(text.matches("[0-9]+\\..+\\.")){//Key without next step
					Element key = new Element("Key");
					key.setText(text);
					treatment.addContent(key);
				}
				else if(text.matches("[0-9]+\\.[A-Z]+.+")&text.contains("   ")){//Key with next step
					Element key = new Element("Key");
					key.setText(text);
					treatment.addContent(key);
				}
				else{//Discussion
					if(!text.matches("\\s*\\.*name")){
						Element discussion = new Element("discussion");
						discussion.setText(text);
						treatment.addContent(discussion);
					}
				}
			}
		}
		return taxonname;
	}

	private void output(String filename)throws Exception{
		XMLOutputter outputter = new XMLOutputter();
		//String file = "d:/Library Project/work3/part2/Transformed/" + filename;
		//String file = "C:/Users/mohankrishna89/Desktop/Library Project/V3/source/vol03h Taxon HTML/Transformed/" + filename;
		String file = "C:/Users/mohankrishna89/Desktop/Library Project/V3/source/test1/Transformed/" + filename;
		Document doc = new Document(treatment);
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		outputter.output(doc, out);
	}
	
	private void createtreatment() throws Exception{
		treatment = new Element("treatment");
	}
	
	//function to clean the text
	public String cleantext(String text) throws Exception{
	text=text.replaceAll(" ", " ").replaceAll("&eacute;", "é").replaceAll("&ouml;", "ö").replaceAll("&acirc;", "â").replaceAll("&ccedil;", "ç").replaceAll("&amp;", "&").replaceAll("&shy;", "").replaceAll("&ntilde;", "ñ")
			.replaceAll("&egrave;", "è").replaceAll("&middot;", "·").replaceAll("&quot;", "\"").replaceAll("&uuml;", "ü").replaceAll("&oacute;", "ó").replaceAll("&Aring;", "Å").replaceAll("&auml;", "ä").replaceAll("&ecirc;", "ê")
			.replaceAll("&oslash;", "ø").replaceAll("</P>", "").replaceAll("<BR>", "");	
	return text;
	}
	
	//function which returns the species/genus/family name along with the authority
	private String[] getNameAuthority(String name) {
		String[] nameinfo = new String[2];
		if(name.matches(".*?\\b(subfam|var|subgen|subg|subsp|ser|tribe|sect|subsect)\\b.*")){
			nameinfo[0] = name;
			nameinfo[1] = "";
			return nameinfo;
		}
		//family
		Pattern p = Pattern.compile("^([a-z]*?ceae)(\\b.*)", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(name);
		if(m.matches()){
			nameinfo[0] = m.group(1).replaceAll("\\s", "").trim(); //in case an extra space is there
			nameinfo[1] = m.group(2).trim();
			return nameinfo;
		}
		//genus
		p = Pattern.compile("^([A-Z][A-Z].*?)(\\b.*)"); 
		m = p.matcher(name);
		if(m.matches()){
			nameinfo[0] = m.group(1).replaceAll("\\s", "").trim();
			nameinfo[1] = m.group(2).trim();
			return nameinfo;
		}
		//species
		p = Pattern.compile("^([A-Z].*?)\\s+([(A-Z].*)");
		m = p.matcher(name);
		if(m.matches()){
			nameinfo[0] = m.group(1).trim();
			nameinfo[1] = m.group(2).trim();
			return nameinfo;
		}
		
		
		return nameinfo;
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
		Pattern p = Pattern.compile("(.*?\\d+-\\d+\\.\\]?)(\\s+[A-Z]\\w+,.*)");
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
