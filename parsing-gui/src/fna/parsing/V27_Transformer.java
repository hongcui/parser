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

public class V27_Transformer {
	private static final Logger LOGGER = Logger.getLogger(V27_Transformer.class);
	Element treatment = new Element("treatment");
	private boolean debugref = false;
	String keystorage = "";
	int keydetecter = 0;
	int keylast=0;
	static int partdetecter, count;
	static Hashtable hashtable = new Hashtable();
	public static void main(String[] args) throws Exception{
		int testkey=1;//Modifies keys only when it is 1.
		ObjectOutputStream outputStream = null;
		outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V27\\namemapping.bin"));
		String taxonname = null;
		Hashtable mapping = new Hashtable();
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V8\\target\\Extracted");
		File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V27\\target\\Extracted");
		File[] files = extracted.listFiles();
		for(int i = 1; i<=files.length; i++){
			count = i;
			SAXBuilder builder = new SAXBuilder();
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
			
			//End mohan code
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
			int smallcapsdetecter = 0;
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
				if(te.getName()=="smallcaps"){
					//if(te.getText().length()!=0)
					smallcapsdetecter = 1;
				}
				
			}
			text=text.trim();
			
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
				Element disc = new Element ("discussion");
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
					Element synname = new Element("synonym");
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
						Element familyname = new Element("family_name");
						familyname.setText(famchunks[f]);
						treatment.addContent(familyname);
						taxonname = famchunks[f];
					}
					else
					{
						Element commonname = new Element("common_name");
						commonname.setText(famchunks[f]);
						treatment.addContent(commonname);
					}
				}
				
			}
			//else if(familydetecter == 1)
			else if((familydetecter == 1 && !text.matches("SELECTED REFERENCE.+") && (bolddetecter != 1))||((authdetecter==1)&&(keylast!=1))){//Author// To prevent matching SELECTED REFERENCE in 9.xml
				Element author = new Element("author");
				author.setText(text);
				treatment.addContent(author);
				authdetecter=0;
				//familydetecter = 0;
				syndetecter=1;// to match the synonymns that occur after the author
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
			//else if(text.matches("(Genus|Species|Varieties|Subspecies|Genera) (ca)?.+")&&(text.contains("):"))){//number of infrataxa// to prevent matching Species of ribes in 10.xml
				//to match Genera 4 or 5, species ca. 34 (1 in the flora): worldwide in tropical and subtopical regions
			//else if(text.matches("(Genus|Species|Varieties|Subspecies|Genera) (ca)?.+")&&((text.contains("):"))||(text.matches(".*\\d:.*")))){//number of infrataxa// to prevent matching Species of ribes in 10.xml
			else if((text.matches("(Genus|Species|Varieties|Subspecies|Genera) (ca)?.+")&&((text.contains("):"))||(text.matches(".*\\d:.*"))))&&keylast!=1){//number of infrataxa// to prevent matching Species of ribes in 10.xml
				String[] newchunks=new String[0];
				newchunks=text.split(":");
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

			//else if((bolddetecter == 1&&text.matches("\\d+\\w*\\..+"))||(text.matches("[0-9]+\\. {0,5}[A-Z]{2}.+"))){//Species name
			//else if((bolddetecter == 1&&text.matches("\\d+\\w*\\..+"))||(text.matches("[0-9]+\\. {0,5}[A-Z]{2}.+")||text.matches("[0-9]+\\w\\.[A-Z].+"))){//Species name
			else if((((bolddetecter == 1)||(smallcapsdetecter==1))&&text.matches("\\d+\\w*\\..+"))||(text.matches("[0-9]+\\. {0,5}[A-Z]{2}.+")||text.matches("[0-9]+\\w\\.[A-Z].+"))){//Species name & sect.
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
				//chunks=text.split("(\\s-)|(\\s·)");//to match Vancouveria planipetala Calloni, Malpighia 1: 266, plate 6. 1887 · Redwood-ivy, redwood inside-out flower · E
				chunks=text.split("(\\s·)");//to match Vancouveria planipetala Calloni, Malpighia 1: 266, plate 6. 1887 · Redwood-ivy, redwood inside-out flower · E
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
						if(spchunks[s].matches(".*\\sin\\s.*"))//to add "in.." to publication title
						{
							String[] inchunks = new String[2];
							inchunks=spchunks[s].split("\\sin\\s");
							spchunks[0]=inchunks[0];
							spchunks[1]="in "+inchunks[1]+","+spchunks[1];
						}
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
						taxonname=spchunks[s];
							
					}
					else
					{
						pub+=spchunks[s]+",";	
					}
				}
				if(pub.length()!=0)
				{
					String[] pubchunks = new String[0];
					pubchunks=pub.split(";");
					for(int p1=0;p1<pubchunks.length;p1++)
					{	
						String namec="";
						String[] titlechunks = new String[1];
						titlechunks=pubchunks[p1].split("[\\d].*");
						String publtitl=titlechunks[0];
						int inlength=titlechunks[0].length();
						//String inpubl=pubchunks[p1].substring(inlength, pubchunks[p1].length()-1);
						String inpubl="";
						//if(inlength<pubchunks[p1].length())//just to check that pubchunks[p1] is not empty.
						if(inlength<=pubchunks[p1].length())//just to check that pubchunks[p1] is not empty.
						//inpubl=pubchunks[p1].substring(inlength, pubchunks[p1].length()-1);
						{
							inpubl=pubchunks[p1].substring(inlength, pubchunks[p1].length());
							if(inpubl.matches("(.*(name\\s+conserved).*)"))
							{
								String namecons[] = new String[1];
								namecons=inpubl.split(",");
								inpubl=namecons[0]+",";
								namec=namecons[1];
							}
						}
							
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
							if(namec.length()!=0)//to add name conserved with a special tag
							{
								Element nameconserved=new Element("conserved_name");
								nameconserved.setText(namec);
								treatment.addContent(nameconserved);
							}
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
			}

			//else if(text.contains("elevations")&&text.contains(";")&&!text.contains("elevations,")){//Distribution
			//else if(text.matches("(.*elevation(s)?\\s*(\\(.*\\))?;.*)")){//Distribution
			//else if(text.matches("(.*elevation(s)?\\s*(\\(.*\\))?;.*)|(.*conservation\\s+concern\\s*;.*)")){
			else if((text.matches("(.*elevation(s)?\\s*(\\(.*\\))?;.*)|(.*conservation\\s+concern\\s*;.*)"))||(text.matches(".*[A-Z]((\\w*)\\.?)*(\\.,|\\.;).*"))){
				//System.out.println("elevation"+count);
				Element phenology = new Element("phenology");
				Element habitat = new Element("habitat");
				Element conservation = new Element("conservation");
				Element elevation = new Element("elevation");
				Element distribution = new Element("distribution");
				String pheno="", habi="", eleva="", distri="", conserv="";
				String[] semi = new String[4];
				String[] dot = new String[3];
				dot = text.split("\\.");
				int eletag = 0;
				String newtext="";
				if((dot[0].contains("mature"))||(dot[0].contains("maturing")))
				{
					pheno=dot[0]+'.';
					phenology.setText(pheno);
					for(int p=1;p<dot.length;p++)
					{
					newtext=newtext+dot[p]+'.';	
					}
				}
				else
				{
					for(int p=0;p<dot.length;p++)
					{
					newtext=newtext+dot[p]+'.';	
					}
				}
				//newtext=newtext.substring(0, newtext.length()-1);
				newtext= newtext.replaceFirst("\\.\\s*\\.$", ".");
				//distri = distri.replaceFirst(";$", "");
				//if(newtext.matches("(.*elevation(s)?\\s*(\\(.*\\))?;.*)|(.*conservation\\s+concern\\s*;.*)"))
				if(newtext.matches("(.*elevation(s)?\\s*(\\(.*\\))?;.*)|(.*conservation\\s+concern\\s*;.*)")||(newtext.matches(".*[A-Z]((\\w*)\\.?)*(\\.,|\\.;).*")))
				{
					semi = newtext.split(";");
					for(int semit=0;semit<semi.length;semit++)
					{
						if(!semi[semit].contains("conservation"))
						{
							if(!semi[semit].contains("elevation"))
							//Note: Manually correct the elevation of 89.XML i.e Sphagnum capillifolium   (Ehrhart) Hedwig and 166.XML i.e. Polytrichum commune   Hedwig var. commune
							//if((!semi[semit].contains("elevation"))&&(semi[semit].matches(".*\\s+elevations,\\s+(m|subalpine)")))
							{
								if(!semi[semit].matches(".*\\d+\\s+m"))
								{
									//if(!((semi[semit].matches(".*[A-Z]((\\w*)\\.?)*(\\.).*"))||(semi[semit].matches(".*\\."))||(semi[semit].matches("\\w\\s+.*"))||(semi[semit].matches("(([A-Z]|[a-z])+)"))))
									//if((!(semi[semit].matches("\\s*((\\w+)|(.*[A-Z]((\\w*)\\.?)*(\\.).*)|(.*\\.)|(\\w\\s+.*))")))&&!(semi[semit].contains("America"))&&!(semi[semit].contains("Asia"))&&!(semi[semit].contains("Africa"))&&!(semi[semit].contains("Europe"))
										//	&&!(semi[semit].contains("Atlantic"))&&!(semi[semit].contains("Pacific")))
									
									if((!(semi[semit].matches("\\s*((\\w+)|(.*[A-Z]((\\w*)\\.?)*(\\.).*)|(.*\\.)|(\\w\\s+.*))|(.*[A-Z]\\w+\\s+[A-Z]\\w+.*)")))&&!(semi[semit].contains("America"))&&!(semi[semit].contains("Asia"))&&!(semi[semit].contains("Africa"))&&!(semi[semit].contains("Europe"))
												&&!(semi[semit].contains("Atlantic"))&&!(semi[semit].contains("Pacific"))&&(eletag!=1))
											
									{
										habi =habi + semi[semit]+';';
										File habitatout = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V27\\target\\habitat\\" + count + ".txt");
										habitatout.delete();
										FileWriter fw = new FileWriter(habitatout, true);
										fw.append(habi + "\r\n");
										fw.flush();
									}
									else
									{
										distri=distri+semi[semit]+';';
										File distributionout = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V27\\target\\distribution\\" + count + ".txt");
										distributionout.delete();
										FileWriter fw = new FileWriter(distributionout, false);
										fw.append(distri + "\r\n");
										fw.flush();
										eletag=1;
									}
								}
								else
								{
									eleva = eleva + semi[semit]+';';
									eletag=1;
								}
							}
							//else if((semi[semit].contains("elevation"))||(semi[semit].matches("(\\d+(–)?\\s+m)")))
							else
							{
								eleva = eleva + semi[semit]+';';
								eletag=1;
							}
						}
						
						//else if(semi[semit].contains("conservation"))
						else
						{
							conserv = conserv+ semi[semit]+';';
							eletag=1;
						}
						
					}
					
					if(distri.length()!=0)
					{
						//distri=distri.substring(0, distri.length()-1);
						distri = distri.replaceFirst(";$", "");
						distribution.setText(distri);
					}
					if(habi.length()!=0)
					{
						habitat.setText(habi);
					}
					
					if(eleva.length()!=0)
					{
						elevation.setText(eleva);
					}
					
					if(conserv.length()!=0)
					{
						conservation.setText(conserv);
					}
					
					
				}
				else
				{
					System.out.println("Problem in elevation");
				}
								
				
					if(phenology.getText().length()!=0)
						treatment.addContent(phenology);
					//if(floweringtime.getText().length()!=0)
						//treatment.addContent(floweringtime);
					if(habitat.getText().length()!=0)
						treatment.addContent(habitat);
					
					if(elevation.getText().length()!=0)
						treatment.addContent(elevation);
					if(conservation.getText().length()!=0)
					{
						treatment.addContent(conservation);
					}
					if(distribution.getText().length()!=0)
					{
						//distri = distri.replaceFirst(";$", ""); // to remove the extra ; added at the end before
						distribution.setText(distri);
						treatment.addContent(distribution);
					}

			}
			//else if(bolddetecter == 1&!text.matches("[0-9]+\\w*\\..+")){//Description
			else if(bolddetecter == 1&!text.matches("[0-9]+\\w*\\..+")&&keylast!=1){//Description
				Element description = new Element("description");
				//code to write the descriptions to a separate file
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

			
			else{
				if(text.matches("[0-9]+\\..+\\.")&&keydetecter!=1){//Key without next step
				//if(text.matches("[0-9]+\\..+\\.")){//Key without next step
					text=hashText(text);//to add the  ### symbols
					Element key = new Element("Key");
					key.setText(text);
					treatment.addContent(key);
					keylast=1;
				}
				//else if(text.matches("[0-9]+\\.[A-Z]+.+")&&text.contains("   ")){//Key with next step
				else if(text.matches("[0-9]+\\.\\s*[A-Z]+.+")&&text.contains("   ")&&keydetecter!=1){//Key with next step
				//else if(text.matches("[0-9]+\\.\\s*[A-Z]+.+")&&text.contains("   ")){//Key with next step
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
				/*else if(text.matches("[0-9]+\\..+")&&!text.contains("   ")&&!text.matches(".+\\.")&&keydetecter!=1){//Key need to combine
				//else if(text.matches("[0-9]+\\..+")&&!text.contains("   ")&&!text.matches(".+\\.")){//Key need to combine
					keystorage = text;
					keydetecter = 1;
					keylast=1;
				}
				else if(keydetecter == 1){//Combine key
					text = keystorage + text;
					text=hashText(text);//to add the  ### symbols
					Element key = new Element("Key");
					key.setText(text);
					treatment.addContent(key);
					
					keydetecter = 0;
					keylast=1;
				}*/
				else if(text.matches("[0-9]+\\..+")&&!text.contains("   ")&&!text.matches(".+\\.")&&keydetecter!=1){//Key need to combine
					text=hashText(text);//to add the  ### symbols
					Element key = new Element("Key");
					key.setText(text);
					treatment.addContent(key);
					keylast=1;
				}
				else if(keylast == 1)//default key case. Once a key is there nothing else follows so make it Key
				{
					text=hashText(text);//to add the  ### symbols
					Element key = new Element("Key");
					key.setText(text);
					treatment.addContent(key);	
				}
				else{//Discussion
					Element discussion = new Element("discussion");
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
				Element discussion = new Element("discussion");
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
				Element reference = new Element("reference");
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
	
	

}





