package fna.parsing;

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

public class Fna_Finalizer {
	static String filename = null;
	Element treatment = new Element("treatment");
	private boolean debugref = false;
	//public static final String usstate="Ala.|Alaska|Ariz.|Ark.|Calif.|Colo.|Conn.|Del.|D.C.|Fla.|Ga.|Idaho|Ill.|Ind.|Iowa|Kans.|Ky.|La.|Maine|Md.|Mass.|Mich.|Minn.|Miss.|Mo.|Mont.|Nebr.|Nev.|" +
		//	"N.H.|N.J.|N.Mex.|N.Y.|N.C.|N.Dak.|Ohio|Okla.|Oreg.|Pa.|R.I.|S.C.|S.Dak.|Tenn.|Tex.|Utah|Vt.|Va.|Wash.|W.Va.|Wis.|Wyo.";
	public static final String usstate="Ala|Alaska|Ariz|Ark|Calif|Colo|Conn|Del|D.C|Fla|Ga|Idaho|Ill|Ind|Iowa|Kans|Ky|La|Maine|Md|Mass|Mich|Minn|Miss|Mo|Mont|Nebr|Nev|" +
			"N.H|N.J|N.Mex|N.Y|N.C|N.Dak|Ohio|Okla|Oreg|Pa|R.I|S.C|S.Dak|Tenn|Tex|Utah|Vt|Va|Wash|W.Va|Wis|Wyo";
	
	//public static final String castate="Alta.|B.C.|Man.|N.B.|Nfld. and Labr.|N.W.T.|N.S.|Nunavut|Ont.|P.E.I.|Que.|Sask.|Yukon|Nfld.|Labr.|Nfld. and Labr. (Nfld.)|Nfld. and Labr. (Labr.)";
	public static final String castate="Alta|B.C|Man|N.B|Nfld. and Labr|N.W.T|N.S|Nunavut|Ont|P.E.I|Que|Sask|Yukon|Nfld|Labr|Nfld. and Labr. (Nfld.)|Nfld. and Labr. (Labr.)";
	static int partdetecter, count;
	static Hashtable hashtable = new Hashtable();
	public static void main(String[] args) throws Exception{
	
		int testkey=1;//Modifies keys only when it is 1.
		ObjectOutputStream outputStream = null;
		
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V19-fixed\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V4-fixed\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V5-fixed\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V7-fixed\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V20-fixed\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V21-fixed\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\v23-fixed\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V26-fixed\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V3-fixed\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V8-fixed\\namemapping.bin"));
		outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V27-fixed\\namemapping.bin"));
		String taxonname = null;
		Hashtable mapping = new Hashtable();
		
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V19-fixed\\target\\transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V4-fixed\\target\\transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V5-fixed\\target\\transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V7-fixed\\target\\transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V20-fixed\\target\\transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V21-fixed\\target\\transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\v23-fixed\\target\\transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V26-fixed\\target\\transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V3-fixed\\Transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V8-fixed\\Transformed");
		File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V27-fixed\\Transformed");
		File[] files = extracted.listFiles();
		
		for(int i = 0; i<files.length; i++){
			filename = files[i].getName();
			count = i;
			SAXBuilder builder = new SAXBuilder();
			//Document doc = builder.build("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V8\\target\\Extracted\\" + "16.xml");
			//Document doc = builder.build("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\V27\\target\\Extracted\\" + i + ".xml");
			Document doc = builder.build(files[i]);
			Element root = doc.getRootElement();
			//List paralist = XPath.selectNodes(root, "/treatment/paragraph");
			List paralist = XPath.selectNodes(root,"/treatment");
			//System.out.println(paralist.get(4).toString());
			Fna_Finalizer transformer = new Fna_Finalizer();
			transformer.createtreatment();
			if(partdetecter == 0){
				taxonname = transformer.processparagraph(paralist);
				if(taxonname == null){
					taxonname = "taxon name";
				}
				System.out.println(taxonname);
				mapping.put(i, taxonname);
			}else{
				System.out.println("Problem in main");
			}
			
			transformer.output(filename);
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
				
				
				
				//
				//have to start modifying code from here
				
				//code to convert all the synonym names to <synonym>
				if(te.getName()=="synonym_of_tribe_genus"){
					text = te.getText();
					Element synname = new Element("synonym");
					synname.setText(text);
					treatment.addContent(synname);	
				}
				else if(te.getName()=="synonmy_of_species_subspecies_variety"){
					text = te.getText();
					Element synname = new Element("synonym");
					synname.setText(text);
					treatment.addContent(synname);	
				}
				else if(te.getName()=="synonym_of_family"){
					text = te.getText();
					Element synname = new Element("synonym");
					synname.setText(text);
					treatment.addContent(synname);	
				}
				else if(te.getName()=="synonym_of_genus_name"){
					text = te.getText();
					Element synname = new Element("synonym");
					synname.setText(text);
					treatment.addContent(synname);	
				}
				else if(te.getName()=="synonym_of_species_name"){
					text = te.getText();
					Element synname = new Element("synonym");
					synname.setText(text);
					treatment.addContent(synname);	
				}
				else if(te.getName()=="synonym_of_subfamily"){
					text = te.getText();
					Element synname = new Element("synonym");
					synname.setText(text);
					treatment.addContent(synname);	
				}
				else if(te.getName()=="synonym_of_subspecies_name"){
					text = te.getText();
					Element synname = new Element("synonym");
					synname.setText(text);
					treatment.addContent(synname);	
				}
				else if(te.getName()=="synonym_of_tribe_name"){
					text = te.getText();
					Element synname = new Element("synonym");
					synname.setText(text);
					treatment.addContent(synname);	
				}
				else if(te.getName()=="synonym_of_variety_name"){
					text = te.getText();
					Element synname = new Element("synonym");
					synname.setText(text);
					treatment.addContent(synname);	
				}
				//code to convert <clevation> to <elevation>
				else if(te.getName()=="clevation"){
					text = te.getText();
					Element eleva = new Element("elevation");
					eleva.setText(text);
					treatment.addContent(eleva);	
				}
				//code to convert <author_of_family>,<author_of_tribe_genus>,<author_of_subfamily>,<author_of_tribe_genus> into author
				else if(te.getName()=="author_of_family"){
					text = te.getText();
					Element auth = new Element("author");
					auth.setText(text);
					treatment.addContent(auth);	
				}
				else if(te.getName()=="author_of_tribe_genus"){
					text = te.getText();
					Element auth = new Element("author");
					auth.setText(text);
					treatment.addContent(auth);	
				}
				else if(te.getName()=="author_of_subfamily"){
					text = te.getText();
					Element auth = new Element("author");
					auth.setText(text);
					treatment.addContent(auth);	
				}
				else if(te.getName()=="author_of_tribe_genus"){
					text = te.getText();
					Element auth = new Element("author");
					auth.setText(text);
					treatment.addContent(auth);	
				}
				//make <flowering_time> to phenology
				else if(te.getName()=="flowering_time"){
					text = te.getText();
					Element phen = new Element("phenology");
					phen.setText(text);
					treatment.addContent(phen);	
				}
				//make <conserved> to <conserved_name>
				else if(te.getName()=="conserved"){
					text = te.getText();
					Element cons = new Element("conserved_name");
					cons.setText(text);
					treatment.addContent(cons);	
				}
				//remove <header> <footer> <style>
				else if(te.getName()=="header"){
					//do nothing
				}
				else if(te.getName()=="footer"){
					//do nothing
				}
				else if(te.getName()=="style"){
					//do nothing
				}
				//change <genera_key_heading> to <key_heading>
				else if(te.getName()=="genera_key_heading"){
					text = te.getText();
					Element cons = new Element("key_heading");
					cons.setText(text);
					treatment.addContent(cons);	
				}
				//change <genera_key_author> to <key_author>
				else if(te.getName()=="genera_key_author"){
					text = te.getText();
					Element cons = new Element("key_author");
					cons.setText(text);
					treatment.addContent(cons);	
				}
				//code to mark <discussion> within keys as <key_discussion>
				else if(te.getName()=="key"){
					Element newkey = new Element("key");
					List child=te.getChildren();
					Iterator keyit = child.iterator();
					while(keyit.hasNext()){
						Element keyte = (Element)keyit.next();
						if(keyte.getName()=="discussion")
						{
							text = keyte.getText();
							Element keydisc = new Element("key_discussion");
							keydisc.setText(text);
							newkey.addContent(keydisc);	
						}
						else
						{
							Element keydisc=(Element) keyte.clone();
							newkey.addContent(keydisc);
						}	
					}
					treatment.addContent(newkey);
					
				}
				//us_distribution, ca_distribution, global_distribution are left unmodified. general_distribution is converted into global_distribution and <distribution> is processed further.
				else if(te.getName()=="general_distribution"){
					text = te.getText();
					Element dist = new Element("global_distribution");
					dist.setText(text);
					treatment.addContent(dist);	
				}
				//have to modify
				else if(te.getName()=="distribution"){
					String disttext="";
					int closure=0;
					text = te.getText();
					String[] distchunks=text.split(";|,");
					
					for(int i=0;i<distchunks.length;i++)
					{
						if(distchunks[i].length()==0)
						{
							continue;
						}
						
						if(closure==1&&!distchunks[i].contains(")"))
						{
							disttext=disttext+","+distchunks[i];
						}
						else if(closure==1&&distchunks[i].contains(")"))
						{
							disttext=disttext+","+distchunks[i];
							closure=0;
							Element dist = new Element("global_distribution");
							dist.setText(disttext);
							treatment.addContent(dist);
						}
						else if(distchunks[i].contains("(")&&!distchunks[i].contains(")"))
						{
							disttext=disttext+distchunks[i];
							closure=1;
						}
						else if(distchunks[i].trim().contains("introduced"))//introduced
						{
							Element dist = new Element("introduced");
							dist.setText(distchunks[i]);
							treatment.addContent(dist);
						}
						else if(distchunks[i].trim().matches("\\s*("+castate+")\\.?\\s*.*"))  //try this
						//else if(distchunks[i].trim().matches(".*\\s*("+castate+")\\s*.*"))
						{
							Element dist = new Element("ca_distribution");
							dist.setText(distchunks[i]);
							treatment.addContent(dist);
						}
						else if(distchunks[i].trim().matches("\\s*("+usstate+")\\.?\\s*.*"))   //try this
						//else if(distchunks[i].trim().matches(".*\\s*("+usstate+")\\s*.*"))
						{
							Element dist = new Element("us_distribution");
							dist.setText(distchunks[i]);
							treatment.addContent(dist);
						}
						else
						{
							Element dist = new Element("global_distribution");
							dist.setText(distchunks[i]);
							treatment.addContent(dist);
						}
						
					}
					/*Element dist = new Element("global_distribution");
					dist.setText(text);
					treatment.addContent(dist);	*/
				}
				//have to modify
				//convert token into their respective elements
				else if(te.getName()=="token"){
					text = te.getText().trim();	
					String[] tokchunks=text.split("\\s");
					for(int j=0; j<tokchunks.length;j++)
					{
						if(tokchunks[j].matches("\\s*C\\s*"))
						{
							Element tok = new Element("conservation");
							tok.setText("of conservation concern");
							treatment.addContent(tok);
							
						}
						else if(tokchunks[j].matches("\\s*E\\s*"))
						{
							Element tok = new Element("endemic");
							tok.setText("endemic");
							treatment.addContent(tok);
						}
						else if(tokchunks[j].matches("\\s*F\\s*"))
						{
							Element tok = new Element("illustrated");
							tok.setText("illustrated");
							treatment.addContent(tok);
						}
						else if(tokchunks[j].matches("\\s*I\\s*"))
						{
							Element tok = new Element("introduced");
							tok.setText("introduced");
							treatment.addContent(tok);
							
						}
						else if(tokchunks[j].matches("\\s*W\\s*"))
						{
							Element tok = new Element("weedy");
							tok.setText("weedy");
							treatment.addContent(tok);
						}
						else if(tokchunks[j].matches("\\s*W1\\s*"))
						{
							Element tok = new Element("weedy");
							tok.setText("weedy");
							treatment.addContent(tok);
						}
						else if(tokchunks[j].matches("\\s*W2\\s*"))
						{
							Element tok = new Element("weedy");
							tok.setText("weedy");
							treatment.addContent(tok);
						}
						else if(tokchunks[j].matches("[A-Z]+"))
						{	
							System.out.println("++++++++++++++++++++++++"+tokchunks[j]+"\n");
							if(tokchunks[j].contains("C"))
							{
								Element tok = new Element("conservation");
								tok.setText("of conservation concern");
								treatment.addContent(tok);
								
							}
							else if(tokchunks[j].contains("E"))
							{
								Element tok = new Element("endemic");
								tok.setText("endemic");
								treatment.addContent(tok);
							}
							else if(tokchunks[j].contains("F"))
							{
								Element tok = new Element("illustrated");
								tok.setText("illustrated");
								treatment.addContent(tok);
							}
							else if(tokchunks[j].contains("I"))
							{
								Element tok = new Element("introduced");
								tok.setText("introduced");
								treatment.addContent(tok);
								
							}
							else if(tokchunks[j].contains("W"))
							{
								Element tok = new Element("weedy");
								tok.setText("weedy");
								treatment.addContent(tok);
							}
							/*else		//to remove unknown tags
							{
								Element tok = new Element("unknown");
								tok.setText(tokchunks[j]);
								treatment.addContent(tok);
								System.out.println("+++++++++++++++++++++++++++++"+tokchunks[j]+"\n");
							}*/
						}
						/*else		//to remove unknown tags
						{
							Element tok = new Element("unknown");
							tok.setText(tokchunks[j]);
							treatment.addContent(tok);
							System.out.println("****************************"+tokchunks[j]+"\n");
						}*/
					}
				}
				else if(te.getName()=="species_name"){
					text = te.getText().trim();
					String[] spchunks=text.split("\\s");
					String species = spchunks[0]+" "+spchunks[1];
					String authority = "";
					for(int j=2; j<spchunks.length;j++)
					{
						authority=authority+spchunks[j]+" ";
					}
					authority=authority.trim();
					Element sp = new Element("species_name");
					sp.setText(species);
					treatment.addContent(sp);
					if(authority.length()!=0)
					{
					Element au = new Element("authority");
					au.setText(authority);
					treatment.addContent(au);	
					}
				}
				else if(te.getName()=="genus_name"){
					text = te.getText().trim();
					String[] spchunks=text.split("\\s");
					String genus = spchunks[0];
					String authority = "";
					for(int j=1; j<spchunks.length;j++)
					{
						authority=authority+spchunks[j]+" ";
					}
					authority=authority.trim();
					Element ge = new Element("genus_name");
					ge.setText(genus);
					treatment.addContent(ge);
					if(authority.length()!=0)
					{
					Element au = new Element("authority");
					au.setText(authority);
					treatment.addContent(au);
					}
				}
				else if(te.getName()=="family_name"){
					text = te.getText().trim();
					String[] spchunks=text.split("\\s");
					String genus = spchunks[0];
					String authority = "";
					for(int j=1; j<spchunks.length;j++)
					{
						authority=authority+spchunks[j]+" ";
					}
					authority=authority.trim();
					Element ge = new Element("family_name");
					ge.setText(genus);
					treatment.addContent(ge);
					if(authority.length()!=0)
					{
					Element au = new Element("authority");
					au.setText(authority);
					treatment.addContent(au);
					}
				}
				
				else
				{
					Element disc=(Element) te.clone();
					treatment.addContent(disc);
				}
				//End of modified code
				
			}
		}
		//taxonname=count+" "+taxonname;
		taxonname=filename;
		return taxonname;
	}
	

	private void output(String filename) throws Exception {
		XMLOutputter outputter = new XMLOutputter();
		
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V19-fixed\\target\\Last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V4-fixed\\target\\Last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V5-fixed\\target\\Last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V7-fixed\\target\\Last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V20-fixed\\target\\last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V21-fixed\\target\\last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\v23-fixed\\target\\last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V26-fixed\\target\\last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V3-fixed\\last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V8-fixed\\last\\" + filename;
		String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA\\V27-fixed\\last\\" + filename;
		Document doc = new Document(treatment);
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(file));
		outputter.output(doc, out);
	}
	private void createtreatment() throws Exception{
		treatment = new Element("treatment");
	}
		

}

