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
/**
 * @author Mohan Updates
 * 
 * used to review <xxx_name> and <synonym> elements and process them in FNA volumes for JSTOR
 *
 */
public class Name_Finalizer {
	static String filename = null;
	Element treatment = new Element("treatment");
	static int partdetecter, count;
	static Hashtable hashtable = new Hashtable();
	static int fnamedetecter=0;
	public static void main(String[] args) throws Exception{
	
		ObjectOutputStream outputStream = null;
		
		
		
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V19-good\\namemapping.bin"));
		outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V20-good\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V21-good\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\v23-good\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V26-good\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V3-good\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V4-good\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V5-good\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V7-good\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V8-good\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V27-good\\namemapping.bin"));
		String taxonname = null;
		Hashtable mapping = new Hashtable();
		
		
		
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V19-good\\target\\transformed");
		File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V20-good\\target\\transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V21-good\\target\\transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\v23-good\\target\\transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V26-good\\target\\transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V3-good\\target\\Transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V4-good\\target\\transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V5-good\\target\\transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V7-good\\target\\transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V8-good\\target\\Transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V27-good\\target\\Transformed");
		File[] files = extracted.listFiles();
		
		for(int i = 0; i<files.length; i++){
			fnamedetecter=0;
			filename = files[i].getName();
			count = i;
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(files[i]);
			Element root = doc.getRootElement();
			List paralist = XPath.selectNodes(root,"/treatment");
			Name_Finalizer transformer = new Name_Finalizer();
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
			int nextelevalid = 0;
			int smallcapsdetecter = 0;
			Element pe = (Element)paraiter.next();
			Element nextele;						//used to store the next element of a <xxx_name>
			List contentlist = pe.getChildren();
			int lilen= contentlist.size();
			//Iterator contentiter = contentlist.iterator();
			String text = "";
			String syntext= "";
			//while(contentiter.hasNext())
			for(int i=0;i<lilen;i++)
			{
				//Element te = (Element)contentiter.next();
				Element te = (Element) contentlist.get(i);
				if((te.getName()=="genus_name")||(te.getName()=="section_name")||(te.getName()=="series_name")||(te.getName()=="species_name")||(te.getName()=="subfamily_name")||(te.getName()=="subgenus_name")||(te.getName()=="subsection_name")||(te.getName()=="subspecies_name")||(te.getName()=="tribe_name")||(te.getName()=="variety_name")||(te.getName()=="family_name")){
					text = te.getText();
					//nextele = (Element)contentiter.next();
					i++;
					nextele = (Element) contentlist.get(i);
					if(nextele.getName()=="authority") //need not worry about next element as the text is already included
					{
						text = text + " " + nextele.getText();//add authority
						
						//nextele = (Element)contentiter.next();
						i++;
						nextele = (Element) contentlist.get(i);//get next element which might be a publication
						//have to write a function by passing the text string along with the (te.getname()- not necessary) to a function or can do the steps here
						nameprocessing(text,te.getName(),nextele);
					}
					else	//have to do normal processing along with next element processing
					{
						//have to write a function by passing the text string along with the (te.getname()- not necessary) to a function or can do the steps here
						nameprocessing(text,te.getName(),nextele);
						//System.out.println("*****************************");
					/*Element disc=(Element) nextele.clone();
						treatment.addContent(disc);*/
					}
					
				}	
				// HAVE TO WRITE CODE FOR SYNONYMNS	
				else if(te.getName()=="synonym")
				{
					String namepart="";
					String pubpart="";
					String indsyntext="";
					syntext = te.getText();
					String[] indsyn = syntext.split(";");
					
					/**/
					for(int s=0; s<indsyn.length; s++)// for every synonym
					{
						namepart="";
						pubpart="";
						Element pubname = new Element("place_of_publication");
						Element taxid= new Element("TaxonIdentification");
						taxid.setAttribute("Status","SYNONYM");
						indsyntext=indsyn[s];
						int commaindex=-1;
						int genindex=100000;
						
						int ci_in=indsyntext.indexOf(" in ");
						int ci_based=indsyntext.indexOf(" based ");
						int ci_comma=indsyntext.indexOf(",");
						if(ci_in>=0&&ci_in<genindex)
						{
							genindex=ci_in;
						}
						if(ci_based>=0&&ci_based<genindex)
						{
							genindex=ci_based;
						}
						if(ci_comma>=0&&ci_comma<genindex)
						{
							genindex=ci_comma;
						}
						
						if(genindex!=100000)
						{
						commaindex=genindex;
						}
						
						/*int commaindex=indsyntext.indexOf(" in ");
						int inorcomma=0;
						if(commaindex==-1)
						{
							commaindex=indsyntext.indexOf(",");
							inorcomma=1;
						}*/
						
						
						if(commaindex!=-1)
						{
							namepart=(namepart+indsyntext.substring(0, commaindex)).trim();
							String other="";
							//Code to process other_info
							//String publication=indsyntext.substring(commaindex+1, indsyntext.length()).trim();
							String publication=indsyntext.substring(commaindex+1, indsyntext.length());
							int n_commaindex=-1;
							int n_genindex=100000;
							
							int n_ci_in=publication.indexOf(" not ");
							int n_ci_based=publication.indexOf(" based ");
							if(n_ci_in>=0&&n_ci_in<n_genindex)
							{
								n_genindex=n_ci_in;
							}
							if(n_ci_based>=0&&n_ci_based<n_genindex)
							{
								n_genindex=n_ci_based;
							}
														
							if(n_genindex!=100000)
							{
							n_commaindex=n_genindex;
							}
							if(n_commaindex!=-1)
							{
								pubpart=(pubpart+publication.substring(0, n_commaindex)).trim();
								other=(other+publication.substring(n_commaindex+1, publication.length())).trim();
							}
							else
							{
								pubpart=(pubpart+indsyntext.substring(commaindex+1, indsyntext.length())).trim();
							}
							//End of Code to process other_info
							
							//process the publication part
						
							//pubpart=(pubpart+indsyntext.substring(commaindex+1, indsyntext.length())).trim();
							String[] titlechunks = new String[1];
							titlechunks=pubpart.split("[\\d].*");
							String publtitl=titlechunks[0];
							/*int inlength=titlechunks[0].length();
							
							if(inlength<pubpart.length())
							{
								Element publ_title = new Element("publication_title");
								publ_title.setText(publtitl);
								pubname.addContent(publ_title);
								String inpubl=pubpart.substring(inlength, pubpart.length()-1);
								Element in_publication = new Element("place_in_publication");
								in_publication.setText(inpubl);
								pubname.addContent(in_publication);
							}
							else
							{
								namepart=indsyntext.trim();
							}*/
							if(publtitl.length()!=0)
							{
							Element publ_title = new Element("publication_title");
							publ_title.setText(publtitl);
							pubname.addContent(publ_title);
							}
							int inlength=titlechunks[0].length();
							if(inlength<pubpart.length())
							{
								//String inpubl=pubpart.substring(inlength, pubpart.length()-1);
								String inpubl=pubpart.substring(inlength, pubpart.length());
								Element in_publication = new Element("place_in_publication");
								in_publication.setText(inpubl);
								pubname.addContent(in_publication);
							}
							if(other.length()!=0)
							{
								Element other_info = new Element("other_info");
								other_info.setText(other);
								pubname.addContent(other_info);
							}
						/*	else
							{
								System.out.println("only one element in publication");
							}*/
							
							//finished processing the publication.
						}
						else
						{
							namepart=indsyntext.trim();
						}
						
						Element syn=synprocess(namepart);
						List synlist=syn.getChildren();
						for(int m=0;m<synlist.size();m++)
						{
							Element synte = (Element) synlist.get(m);
							Element newsynte=(Element) synte.clone();
							taxid.addContent(newsynte);
						}
						/*Iterator synit=synlist.iterator();
						while(synit.hasNext())
						{
							Element synpe = (Element)synit.next();
							taxid.addContent(synpe.detach());
						}*/
						//taxid.addContent(synlist);
						
						
						if(pubname.getContentSize()!=0) //to check if the publication has children.
						{
							taxid.addContent(pubname);
							//System.out.println();
						}
						treatment.addContent(taxid);
					}
					/*Element disc=(Element) te.clone();
					treatment.addContent(disc);*/
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
	

		
	private void nameprocessing(String text, String tagname, Element publi) throws Exception{
		Element taxid= new Element("TaxonIdentification");
		taxid.setAttribute("Status","ACCEPTED");
		String Comtext="";
		if(text.contains("subtribe"))
		{
			int k;
			String newtext= text;
			String spauth="";
			String sectauth="";
			String subgauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("family_name");
			newele.setText(var[0]);
			taxid.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].matches("\\s*\\(tribe\\s*")||var[k].contains("subtribe"))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("family_authority");
			spat.setText(spauth);
			taxid.addContent(spat);
			}
			if(var[k].matches("\\s*\\(tribe\\s*"))
			{
			k++;
			Element subfm= new Element("tribe_name");
			subfm.setText(var[k].replaceFirst("(\\W)$", ""));
			taxid.addContent(subfm);
			k++;
			while((k<var.length)&&!(var[k].contains("subtribe")))
			{
				subgauth=subgauth+var[k]+" ";
				k++;
			}
			subgauth=subgauth.trim();
			if(subgauth.length()!=0)
			{
				Element subauth= new Element("tribe_authority");
				subauth.setText(subgauth);
				taxid.addContent(subauth);
			}
			if(var[k].contains("subtribe"))
			{
				k++;
				Element sect= new Element("subtribe_name");
				sect.setText(var[k]);
				taxid.addContent(sect);
				k++;
				while(k<var.length)
				{
					sectauth=sectauth+var[k]+" ";
					k++;
				}
				sectauth=sectauth.trim();
				if(sectauth.length()!=0)
				{
				Element subat= new Element("subtribe_authority");
				subat.setText(sectauth);
				taxid.addContent(subat);
				}
			}
			else
			{
				System.out.println("Problem in subtribe");
			}
			}
			else
			{
			k++;
			Element sect= new Element("section_name");
			sect.setText(var[k]);
			taxid.addContent(sect);
			k++;
			while(k<var.length)
			{
				sectauth=sectauth+var[k]+" ";
				k++;
			}
			sectauth=sectauth.trim();
			if(sectauth.length()!=0)
			{
				Element subat= new Element("section_authority");
				subat.setText(sectauth);
				taxid.addContent(subat);
			}
			}
		}
		else if(text.contains("tribe"))
		{
			int k;
			String newtext= text;
			String spauth="";
			String sectauth="";
			String subgauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("family_name");
			newele.setText(var[0]);
			taxid.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].contains("subfam.")||var[k].contains("tribe"))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("family_authority");
			spat.setText(spauth);
			taxid.addContent(spat);
			}
			if(var[k].contains("subfam."))
			{
			k++;
			Element subfm= new Element("subfamily_name");
			subfm.setText(var[k].replaceFirst("(\\W)$", ""));
			taxid.addContent(subfm);
			k++;
			while((k<var.length)&&!(var[k].contains("tribe")))
			{
				subgauth=subgauth+var[k]+" ";
				k++;
			}
			subgauth=subgauth.trim();
			if(subgauth.length()!=0)
			{
				Element subauth= new Element("subfamily_authority");
				subauth.setText(subgauth);
				taxid.addContent(subauth);
			}
			if(var[k].contains("tribe"))
			{
				k++;
				Element sect= new Element("tribe_name");
				sect.setText(var[k]);
				taxid.addContent(sect);
				k++;
				while(k<var.length)
				{
					sectauth=sectauth+var[k]+" ";
					k++;
				}
				sectauth=sectauth.trim();
				if(sectauth.length()!=0)
				{
				Element subat= new Element("tribe_authority");
				subat.setText(sectauth);
				taxid.addContent(subat);
				}
			}
			else
			{
				System.out.println("Problem in subtribe");
			}
			}
			else
			{
			k++;
			Element sect= new Element("section_name");
			sect.setText(var[k]);
			taxid.addContent(sect);
			k++;
			while(k<var.length)
			{
				sectauth=sectauth+var[k]+" ";
				k++;
			}
			sectauth=sectauth.trim();
			if(sectauth.length()!=0)
			{
				Element subat= new Element("section_authority");
				subat.setText(sectauth);
				taxid.addContent(subat);
			}
			}
		}
		else if(text.contains("subfam."))// SUBFAMILY
		{	
			int k;
			String newtext= text;
			String famauth="";
			String subfamauth="";
			String[] family= text.split("\\s");
			Element newele= new Element("family_name");
			newele.setText(family[0]);
			taxid.addContent(newele);
			for(k=1;k<family.length;k++)
			{
				if(family[k].contains("subfam."))
				{
					break;
				}
				else
				{
					famauth+=family[k]+" ";
				}
			}
			famauth=famauth.trim();
			if(famauth.length()!=0)
			{
			Element famat= new Element("family_authority");
			famat.setText(famauth);
			taxid.addContent(famat);
			}
			k++;
			Element subfm= new Element("subfamily_name");
			subfm.setText(family[k]);
			taxid.addContent(subfm);
			k++;
			while(k<family.length)
			{
				subfamauth+=family[k]+" ";
				k++;
			}
			subfamauth=subfamauth.trim();
			if(subfamauth.length()!=0)
			{
			Element subfamat= new Element("subfamily_authority");
			subfamat.setText(subfamauth);
			taxid.addContent(subfamat);	
			}
		}
		else if(tagname.matches("family_name")) // if it is a genus name
		{
			fnamedetecter=1;
			String newtext= text;
			String famauth="";
			String[] Chunks = text.split("-");
			text = Chunks[0];
			String[] family= text.split("\\s");
			Element newele= new Element("family_name");
			newele.setText(family[0]);
			taxid.addContent(newele);
			for(int k=1;k<family.length;k++)
			{
				famauth+=family[k]+" ";
			}
			famauth=famauth.trim();
			if(famauth.length()!=0)
			{
				Element famat= new Element("family_authority");
				famat.setText(famauth);
				taxid.addContent(famat);
			}
			
			for(int j=1; j<Chunks.length;j++)
			{
				Comtext+=Chunks[j]+"-";
			}
			Comtext=Comtext.replaceFirst("-$", "");	
			
		}
		else if(text.contains("var."))
		{
			int k;
			String newtext= text;
			String spauth="";
			String varauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			taxid.addContent(newele);
			Element spele= new Element("species_name");
			spele.setText(var[1]);
			taxid.addContent(spele);
			for(k=2;k<var.length;k++)
			{
				if(var[k].contains("var."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("species_authority");
			spat.setText(spauth);
			taxid.addContent(spat);
			}
			k++;
			Element subfm= new Element("variety_name");
			subfm.setText(var[k]);
			taxid.addContent(subfm);
			k++;
			while(k<var.length)
			{
				varauth+=var[k]+" ";
				k++;
			}
			varauth=varauth.trim();
			if(varauth.length()!=0)
			{
			Element subfamat= new Element("variety_authority");
			subfamat.setText(varauth);
			taxid.addContent(subfamat);	
			}
		}
		else if(text.contains("subsp."))
		{
			int k;
			String newtext= text;
			String spauth="";
			String varauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			taxid.addContent(newele);
			Element spele= new Element("species_name");
			spele.setText(var[1]);
			taxid.addContent(spele);
			for(k=2;k<var.length;k++)
			{
				if(var[k].contains("subsp."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("species_authority");
			spat.setText(spauth);
			taxid.addContent(spat);
			}
			k++;
			Element subfm= new Element("subspecies_name");
			subfm.setText(var[k]);
			taxid.addContent(subfm);
			k++;
			while(k<var.length)
			{
				varauth+=var[k]+" ";
				k++;
			}
			varauth=varauth.trim();
			if(varauth.length()!=0)
			{
			Element subfamat= new Element("subspecies_authority");
			subfamat.setText(varauth);
			taxid.addContent(subfamat);	
			}
		}
		else if(tagname.matches("species_name"))
		{
			int k;
			String newtext= text;
			String spauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			taxid.addContent(newele);
			Element spele= new Element("species_name");
			spele.setText(var[1]);
			taxid.addContent(spele);
			for(k=2;k<var.length;k++)
			{
					spauth+=var[k]+" ";
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("species_authority");
			spat.setText(spauth);
			taxid.addContent(spat);
			}
			
		}
		else if(text.contains("ser."))//SERIES NAME
		{
			int k;
			String newtext= text;
			String spauth="";
			String sectauth="";
			String subgauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			taxid.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].contains("subg.")||var[k].contains("sect.")||var[k].contains("ser."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("genus_authority");
			spat.setText(spauth);
			taxid.addContent(spat);
			}
			if(var[k].contains("subg."))
			{
			k++;
			Element subfm= new Element("subgenus_name");
			subfm.setText(var[k].replaceFirst("(\\W)$", ""));
			taxid.addContent(subfm);
			k++;
			while((k<var.length)&&!(var[k].contains("sect.")))
			{
				subgauth=subgauth+var[k]+" ";
				k++;
			}
			subgauth=subgauth.trim();
			subgauth=subgauth.replaceFirst("(\\W)$", "");
			if(subgauth.length()!=0)
			{
				Element subauth= new Element("subgenus_authority");
				subauth.setText(subgauth);
				taxid.addContent(subauth);
			}
			if(var[k].contains("sect."))
			{
				k++;
				Element sect= new Element("section_name");
				sect.setText(var[k]);
				taxid.addContent(sect);
				k++;
				while(k<var.length&&!(var[k].contains("ser.")))
				{
					sectauth=sectauth+var[k]+" ";
					k++;
				}
				sectauth=sectauth.trim();
				sectauth=sectauth.replaceFirst("(\\W)$", "");
				if(sectauth.length()!=0)
				{
				Element subat= new Element("section_authority");
				subat.setText(sectauth);
				taxid.addContent(subat);
				}
				if(var[k].contains("ser."))
				{
					k++;
					Element ser= new Element("series_name");
					ser.setText(var[k]);
					taxid.addContent(ser);
					k++;
					while(k<var.length)
					{
						sectauth=sectauth+var[k]+" ";
						k++;
					}
					sectauth=sectauth.trim();
					sectauth=sectauth.replaceFirst("(\\W)$", "");
					if(sectauth.length()!=0)
					{
					Element subat= new Element("series_authority");
					subat.setText(sectauth);
					taxid.addContent(subat);
					}
				}
			}
			else
			{
				System.out.println("Problem in section");
			}
			}
			else
			{
			k++;
			Element sect= new Element("series_name");
			sect.setText(var[k]);
			taxid.addContent(sect);
			k++;
			while((k<var.length))
			{
				sectauth=sectauth+var[k]+" ";
				k++;
			}
			sectauth=sectauth.trim();
			if(sectauth.length()!=0)
			{
				Element subat= new Element("series_authority");
				subat.setText(sectauth);
				taxid.addContent(subat);
			}
			}
			
		}
		else if(text.contains("subsect."))
		{
			int k;
			String newtext= text;
			String spauth="";
			String varauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			taxid.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].contains("subsect."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("genus_authority");
			spat.setText(spauth);
			taxid.addContent(spat);
			}
			k++;
			Element subfm= new Element("subsection_name");
			subfm.setText(var[k]);
			taxid.addContent(subfm);
			k++;
			while(k<var.length)
			{
				varauth+=var[k]+" ";
				k++;
			}
			varauth=varauth.trim();
			if(varauth.length()!=0)
			{
			Element subfamat= new Element("subsection_authority");
			subfamat.setText(varauth);
			taxid.addContent(subfamat);	
			}
		}
		else if(text.contains("sect."))
		{
			int k;
			String newtext= text;
			String spauth="";
			String sectauth="";
			String subgauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			taxid.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].contains("subg.")||var[k].contains("sect."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("genus_authority");
			spat.setText(spauth);
			taxid.addContent(spat);
			}
			if(var[k].contains("subg."))
			{
			k++;
			Element subfm= new Element("subgenus_name");
			subfm.setText(var[k].replaceFirst("(\\W)$", ""));
			taxid.addContent(subfm);
			k++;
			while((k<var.length)&&!(var[k].contains("sect.")))
			{
				subgauth=subgauth+var[k]+" ";
				k++;
			}
			subgauth=subgauth.trim();
			if(subgauth.length()!=0)
			{
				Element subauth= new Element("subgenus_authority");
				subauth.setText(subgauth);
				taxid.addContent(subauth);
			}
			if(var[k].contains("sect."))
			{
				k++;
				Element sect= new Element("section_name");
				sect.setText(var[k]);
				taxid.addContent(sect);
				k++;
				while(k<var.length)
				{
					sectauth=sectauth+var[k]+" ";
					k++;
				}
				sectauth=sectauth.trim();
				if(sectauth.length()!=0)
				{
				Element subat= new Element("section_authority");
				subat.setText(sectauth);
				taxid.addContent(subat);
				}
			}
			else
			{
				System.out.println("Problem in section");
			}
			}
			else
			{
			k++;
			Element sect= new Element("section_name");
			sect.setText(var[k]);
			taxid.addContent(sect);
			k++;
			while(k<var.length)
			{
				sectauth=sectauth+var[k]+" ";
				k++;
			}
			sectauth=sectauth.trim();
			if(sectauth.length()!=0)
			{
				Element subat= new Element("section_authority");
				subat.setText(sectauth);
				taxid.addContent(subat);
			}
			}
		}
		else if(text.contains("subg."))
		{
			int k;
			String newtext= text;
			String spauth="";
			String sectauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			taxid.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].contains("subg."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("genus_authority");
			spat.setText(spauth);
			taxid.addContent(spat);
			}
			k++;
			Element sect= new Element("subgenus_name");
			sect.setText(var[k].replaceFirst("(\\W)$", ""));
			taxid.addContent(sect);
			k++;
			while(k<var.length)
			{
				sectauth=sectauth+var[k]+" ";
				k++;
			}
			sectauth=sectauth.trim();
			if(sectauth.length()!=0)
			{
			Element subat= new Element("subgenus_authority");
			subat.setText(sectauth);
			taxid.addContent(subat);		
			}
		}
		else if(tagname.matches("genus_name"))
		{
			int k;
			String newtext= text;
			String spauth="";
			String sectauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			taxid.addContent(newele);
			for(k=1;k<var.length;k++)
			{
					spauth+=var[k]+" ";
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("genus_authority");
			spat.setText(spauth);
			taxid.addContent(spat);
			}
		}
		else
		{
			if(tagname.matches("series_name"))
			{
				int k;
				String newtext= text;
				String spauth="";
				String[] var= text.split("\\s");
				Element newele= new Element("genus_name");
				newele.setText(var[0]);
				taxid.addContent(newele);
				Element spele= new Element("species_name");
				spele.setText(var[1]);
				taxid.addContent(spele);
				for(k=2;k<var.length;k++)
				{
						spauth+=var[k]+" ";
				}
				spauth=spauth.trim();
				if(spauth.length()!=0)
				{
				Element spat= new Element("species_authority");
				spat.setText(spauth);
				taxid.addContent(spat);
				}
				System.out.println("*****series->species");
			}
			else if((tagname.matches("subgenus_name")))
			{
				int k;
				String newtext= text;
				String spauth="";
				String sectauth="";
				String[] var= text.split("\\s");
				Element newele= new Element("genus_name");
				newele.setText(var[0]);
				taxid.addContent(newele);
				for(k=1;k<var.length;k++)
				{
						spauth+=var[k]+" ";
				}
				spauth=spauth.trim();
				if(spauth.length()!=0)
				{
				Element spat= new Element("genus_authority");
				spat.setText(spauth);
				taxid.addContent(spat);
				}
				System.out.println("*****subgenus->genus");
			}
			else if(text.matches("(^[A-Z](\\S)+\\s+){1}((\\[.*\\])|(\\(.*\\)))?\\s*([A-Z](\\S)+).*"))//genus name
			{
				int k;
				int unrank=0;
				String newtext= text;
				String spauth="";
				String sectauth="";
				String[] var= text.split("\\s");
				Element newele= new Element("genus_name");
				newele.setText(var[0]);
				taxid.addContent(newele);
				for(k=1;k<var.length;k++)
				{
					if(!var[k].matches("\\[unranked\\]"))
						spauth+=var[k]+" ";
					else
					{
						unrank=1;
						System.out.println("Unranked element");
						break;
					}
				}
				spauth=spauth.trim();
				if(spauth.length()!=0)
				{
				Element spat= new Element("genus_authority");
				spat.setText(spauth);
				taxid.addContent(spat);
				}
				if(unrank==1)
				{
					k++;
					Element urname= new Element("unranked_epithet_name");
					urname.setText(var[k]);
					taxid.addContent(urname);
					String unauth="";
					k++;
					while(k<var.length)
					{
						unauth=unauth+var[k]+" ";
						k++;
					}
					unauth=unauth.trim();
					if(unauth.length()!=0)
					{
					Element unat= new Element("unranked_epithet_authority");
					unat.setText(unauth);
					taxid.addContent(unat);
					}
					
				}
			}

			else
			{
				System.out.println("Missed Chunk");
				System.out.println(text);
				System.out.println(tagname);
			}
			
			
		}
		
		/*Element cons = new Element("key_author");
		cons.setText(text);
		taxid.addContent(cons);*/
		
		
		
		
		
		//Code to add the element.
		Element newpubli=(Element) publi.clone();
		if(publi.getName()=="place_of_publication")
		{
			taxid.addContent(newpubli);// Add publication to <Taxonidentification>
			if(Comtext.length()!=0)
			{
				Element comt= new Element("common_name");
				comt.setText(Comtext);
				treatment.addContent(comt);
			}
			treatment.addContent(taxid);
		}
		// MUST ADD THE SYNONYM PROCESSING CODE HERE
		else if(newpubli.getName()=="synonym")
		{
			treatment.addContent(taxid);//add the Taxonidentification to the treatment
			if(Comtext.length()!=0)
			{
				Element comt= new Element("common_name");
				comt.setText(Comtext);
				treatment.addContent(comt);
			}
			// SYNONYM SHOULD BE PROCESSED BEFORE ADDING IT TO THE treatment.
			 		String namepart="";
					String pubpart="";
					String indsyntext="";
					String syntext = newpubli.getText();
					String[] indsyn = syntext.split(";");
					
					
					for(int s=0; s<indsyn.length; s++)// for every synonym
					{
						namepart="";
						pubpart="";
						Element pubname = new Element("place_of_publication");
						Element newtaxid= new Element("TaxonIdentification");
						newtaxid.setAttribute("Status","SYNONYM");
						indsyntext=indsyn[s];
						int commaindex=-1;
						int genindex=100000;
						
						int ci_in=indsyntext.indexOf(" in ");
						int ci_based=indsyntext.indexOf(" based ");
						int ci_comma=indsyntext.indexOf(",");
						if(ci_in>=0&&ci_in<genindex)
						{
							genindex=ci_in;
						}
						if(ci_based>=0&&ci_based<genindex)
						{
							genindex=ci_based;
						}
						if(ci_comma>=0&&ci_comma<genindex)
						{
							genindex=ci_comma;
						}
						
						if(genindex!=100000)
						{
						commaindex=genindex;
						}
						
						if(commaindex!=-1)
						{
							namepart=(namepart+indsyntext.substring(0, commaindex)).trim();
							String other="";
							//Code to process other_info
							//String publication=indsyntext.substring(commaindex+1, indsyntext.length()).trim();
							String publication=indsyntext.substring(commaindex+1, indsyntext.length());
							int n_commaindex=-1;
							int n_genindex=100000;
							
							int n_ci_in=publication.indexOf(" not ");
							int n_ci_based=publication.indexOf(" based ");
							if(n_ci_in>=0&&n_ci_in<n_genindex)
							{
								n_genindex=n_ci_in;
							}
							if(n_ci_based>=0&&n_ci_based<n_genindex)
							{
								n_genindex=n_ci_based;
							}
														
							if(n_genindex!=100000)
							{
							n_commaindex=n_genindex;
							}
							if(n_commaindex!=-1)
							{
								pubpart=(pubpart+publication.substring(0, n_commaindex)).trim();
								other=(other+publication.substring(n_commaindex+1, publication.length())).trim();
							}
							else
							{
								pubpart=(pubpart+indsyntext.substring(commaindex+1, indsyntext.length())).trim();
							}
							//End of Code to process other_info
							
							//process the publication part
						
							//pubpart=(pubpart+indsyntext.substring(commaindex+1, indsyntext.length())).trim();
							String[] titlechunks = new String[1];
							titlechunks=pubpart.split("[\\d].*");
							String publtitl=titlechunks[0];

							if(publtitl.length()!=0)
							{
							Element publ_title = new Element("publication_title");
							publ_title.setText(publtitl);
							pubname.addContent(publ_title);
							}
							int inlength=titlechunks[0].length();
							if(inlength<pubpart.length())
							{
								//String inpubl=pubpart.substring(inlength, pubpart.length()-1);
								String inpubl=pubpart.substring(inlength, pubpart.length());
								Element in_publication = new Element("place_in_publication");
								in_publication.setText(inpubl);
								pubname.addContent(in_publication);
							}
							if(other.length()!=0)
							{
								Element other_info = new Element("other_info");
								other_info.setText(other);
								pubname.addContent(other_info);
							}
						
							
							//finished processing the publication.
						}
						else
						{
							namepart=indsyntext.trim();
						}
						
						Element syn=synprocess(namepart);
						List synlist=syn.getChildren();
						for(int m=0;m<synlist.size();m++)
						{
							Element synte = (Element) synlist.get(m);
							Element newsynte=(Element) synte.clone();
							newtaxid.addContent(newsynte);
						}
						
						//newtaxid.addContent(synlist);
						
						
						if(pubname.getContentSize()!=0) //to check if the publication has children.
						{
							newtaxid.addContent(pubname);
							//System.out.println();
						}
						treatment.addContent(newtaxid);
					}

		
			treatment.addContent(newpubli);
		}
		else
		{
			treatment.addContent(taxid);
			if(Comtext.length()!=0)
			{
				Element comt= new Element("common_name");
				comt.setText(Comtext);
				treatment.addContent(comt);
			}
			treatment.addContent(newpubli);
		}
	}
	
	
	private Element synprocess(String namepart) throws Exception{
		Element syn=new Element("synonym");
		String text=cleantext(namepart);
		if(text.contains("subtribe"))
		{
			int k;
			String newtext= text;
			String spauth="";
			String sectauth="";
			String subgauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("family_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].matches("\\s*\\(tribe\\s*")||var[k].contains("subtribe"))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("family_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			if(var[k].matches("\\s*\\(tribe\\s*"))
			{
			k++;
			Element subfm= new Element("tribe_name");
			subfm.setText(var[k].replaceFirst("(\\W)$", ""));
			syn.addContent(subfm);
			k++;
			while((k<var.length)&&!(var[k].contains("subtribe")))
			{
				subgauth=subgauth+var[k]+" ";
				k++;
			}
			subgauth=subgauth.trim();
			if(subgauth.length()!=0)
			{
				Element subauth= new Element("tribe_authority");
				subauth.setText(subgauth);
				syn.addContent(subauth);
			}
			if(var[k].contains("subtribe"))
			{
				k++;
				Element sect= new Element("subtribe_name");
				sect.setText(var[k]);
				syn.addContent(sect);
				k++;
				while(k<var.length)
				{
					sectauth=sectauth+var[k]+" ";
					k++;
				}
				sectauth=sectauth.trim();
				if(sectauth.length()!=0)
				{
				Element subat= new Element("subtribe_authority");
				subat.setText(sectauth);
				syn.addContent(subat);
				}
			}
			else
			{
				System.out.println("Problem in subtribe");
			}
			}
			else
			{
			k++;
			Element sect= new Element("section_name");
			sect.setText(var[k]);
			syn.addContent(sect);
			k++;
			while(k<var.length)
			{
				sectauth=sectauth+var[k]+" ";
				k++;
			}
			sectauth=sectauth.trim();
			if(sectauth.length()!=0)
			{
				Element subat= new Element("section_authority");
				subat.setText(sectauth);
				syn.addContent(subat);
			}
			}
		}
		else if(text.contains("tribe"))
		{
			int k;
			String newtext= text;
			String spauth="";
			String sectauth="";
			String subgauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("family_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].contains("subfam.")||var[k].contains("tribe"))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("family_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			if(var[k].contains("subfam."))
			{
			k++;
			Element subfm= new Element("subfamily_name");
			subfm.setText(var[k].replaceFirst("(\\W)$", ""));
			syn.addContent(subfm);
			k++;
			while((k<var.length)&&!(var[k].contains("tribe")))
			{
				subgauth=subgauth+var[k]+" ";
				k++;
			}
			subgauth=subgauth.trim();
			if(subgauth.length()!=0)
			{
				Element subauth= new Element("subfamily_authority");
				subauth.setText(subgauth);
				syn.addContent(subauth);
			}
			if(var[k].contains("tribe"))
			{
				k++;
				Element sect= new Element("tribe_name");
				sect.setText(var[k]);
				syn.addContent(sect);
				k++;
				while(k<var.length)
				{
					sectauth=sectauth+var[k]+" ";
					k++;
				}
				sectauth=sectauth.trim();
				if(sectauth.length()!=0)
				{
				Element subat= new Element("tribe_authority");
				subat.setText(sectauth);
				syn.addContent(subat);
				}
			}
			else
			{
				System.out.println("Problem in subtribe");
			}
			}
			else
			{
			k++;
			Element sect= new Element("section_name");
			sect.setText(var[k]);
			syn.addContent(sect);
			k++;
			while(k<var.length)
			{
				sectauth=sectauth+var[k]+" ";
				k++;
			}
			sectauth=sectauth.trim();
			if(sectauth.length()!=0)
			{
				Element subat= new Element("section_authority");
				subat.setText(sectauth);
				syn.addContent(subat);
			}
			}
		}
		else if(text.contains("subfam."))// SUBFAMILY
		{	
			int k;
			String newtext= text;
			String famauth="";
			String subfamauth="";
			String[] family= text.split("\\s");
			Element newele= new Element("family_name");
			newele.setText(family[0]);
			syn.addContent(newele);
			for(k=1;k<family.length;k++)
			{
				if(family[k].contains("subfam."))
				{
					break;
				}
				else
				{
					famauth+=family[k]+" ";
				}
			}
			famauth=famauth.trim();
			if(famauth.length()!=0)
			{
			Element famat= new Element("family_authority");
			famat.setText(famauth);
			syn.addContent(famat);
			}
			k++;
			Element subfm= new Element("subfamily_name");
			subfm.setText(family[k]);
			syn.addContent(subfm);
			k++;
			while(k<family.length)
			{
				subfamauth+=family[k]+" ";
				k++;
			}
			subfamauth=subfamauth.trim();
			if(subfamauth.length()!=0)
			{
			Element subfamat= new Element("subfamily_authority");
			subfamat.setText(subfamauth);
			syn.addContent(subfamat);	
			}
		}
		/*else if(tagname.matches("family_name")) // if it is a family name
		{
			String newtext= text;
			String famauth="";
			String[] Chunks = text.split("-");
			text = Chunks[0];
			String[] family= text.split("\\s");
			Element newele= new Element("family_name");
			newele.setText(family[0]);
			syn.addContent(newele);
			for(int k=1;k<family.length;k++)
			{
				famauth+=family[k]+" ";
			}
			famauth=famauth.trim();
			if(famauth.length()!=0)
			{
				Element famat= new Element("family_authority");
				famat.setText(famauth);
				syn.addContent(famat);
			}
			
			for(int j=1; j<Chunks.length;j++)
			{
				Comtext+=Chunks[j]+"-";
			}
			Comtext=Comtext.replaceFirst("-$", "");	
			
		}*/
		else if(text.contains("var."))
		{
			int k;
			String newtext= text;
			String spauth="";
			String varauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			//if(!var[1].contains("var."))
			//{
			Element spele= new Element("species_name");
			spele.setText(var[1]);
			syn.addContent(spele);
			//}
			for(k=2;k<var.length;k++)
			{
				if(var[k].contains("var."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("species_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			k++;
			Element subfm= new Element("variety_name");
			subfm.setText(var[k]);
			syn.addContent(subfm);
			k++;
			while(k<var.length)
			{
				varauth+=var[k]+" ";
				k++;
			}
			varauth=varauth.trim();
			if(varauth.length()!=0)
			{
			Element subfamat= new Element("variety_authority");
			subfamat.setText(varauth);
			syn.addContent(subfamat);	
			}
		}
		else if(text.contains("subsp."))
		{
			int k;
			String newtext= text;
			String spauth="";
			String varauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			Element spele= new Element("species_name");
			spele.setText(var[1]);
			syn.addContent(spele);
			for(k=2;k<var.length;k++)
			{
				if(var[k].contains("subsp."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("species_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			k++;
			Element subfm= new Element("subspecies_name");
			subfm.setText(var[k]);
			syn.addContent(subfm);
			k++;
			while(k<var.length)
			{
				varauth+=var[k]+" ";
				k++;
			}
			varauth=varauth.trim();
			if(varauth.length()!=0)
			{
			Element subfamat= new Element("subspecies_authority");
			subfamat.setText(varauth);
			syn.addContent(subfamat);	
			}
		}
		/*else if(tagname.matches("species_name"))
		{
			int k;
			String newtext= text;
			String spauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			Element spele= new Element("species_name");
			spele.setText(var[1]);
			syn.addContent(spele);
			for(k=2;k<var.length;k++)
			{
					spauth+=var[k]+" ";
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("species_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			
		}*/
		else if(text.contains("ser."))//SERIES NAME
		{
			int k;
			String newtext= text;
			String spauth="";
			String sectauth="";
			String subgauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].contains("subg.")||var[k].contains("sect.")||var[k].contains("ser."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("genus_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			if(var[k].contains("subg."))
			{
			k++;
			Element subfm= new Element("subgenus_name");
			subfm.setText(var[k].replaceFirst("(\\W)$", ""));
			syn.addContent(subfm);
			k++;
			while((k<var.length)&&!(var[k].contains("sect.")))
			{
				subgauth=subgauth+var[k]+" ";
				k++;
			}
			subgauth=subgauth.trim();
			subgauth=subgauth.replaceFirst("(\\W)$", "");
			if(subgauth.length()!=0)
			{
				Element subauth= new Element("subgenus_authority");
				subauth.setText(subgauth);
				syn.addContent(subauth);
			}
			if(var[k].contains("sect."))
			{
				k++;
				Element sect= new Element("section_name");
				sect.setText(var[k]);
				syn.addContent(sect);
				k++;
				while(k<var.length&&!(var[k].contains("ser.")))
				{
					sectauth=sectauth+var[k]+" ";
					k++;
				}
				sectauth=sectauth.trim();
				sectauth=sectauth.replaceFirst("(\\W)$", "");
				if(sectauth.length()!=0)
				{
				Element subat= new Element("section_authority");
				subat.setText(sectauth);
				syn.addContent(subat);
				}
				if(var[k].contains("ser."))
				{
					k++;
					Element ser= new Element("series_name");
					ser.setText(var[k]);
					syn.addContent(ser);
					k++;
					while(k<var.length)
					{
						sectauth=sectauth+var[k]+" ";
						k++;
					}
					sectauth=sectauth.trim();
					sectauth=sectauth.replaceFirst("(\\W)$", "");
					if(sectauth.length()!=0)
					{
					Element subat= new Element("series_authority");
					subat.setText(sectauth);
					syn.addContent(subat);
					}
				}
			}
			else
			{
				System.out.println("Problem in section");
			}
			}
			else
			{
			k++;
			Element sect= new Element("series_name");
			sect.setText(var[k]);
			syn.addContent(sect);
			k++;
			while((k<var.length))
			{
				sectauth=sectauth+var[k]+" ";
				k++;
			}
			sectauth=sectauth.trim();
			if(sectauth.length()!=0)
			{
				Element subat= new Element("series_authority");
				subat.setText(sectauth);
				syn.addContent(subat);
			}
			}
			
		}
		else if(text.contains("subsect."))
		{
			int k;
			String newtext= text;
			String spauth="";
			String varauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].contains("subsect."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("genus_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			k++;
			Element subfm= new Element("subsection_name");
			subfm.setText(var[k]);
			syn.addContent(subfm);
			k++;
			while(k<var.length)
			{
				varauth+=var[k]+" ";
				k++;
			}
			varauth=varauth.trim();
			if(varauth.length()!=0)
			{
			Element subfamat= new Element("subsection_authority");
			subfamat.setText(varauth);
			syn.addContent(subfamat);	
			}
		}
		else if(text.contains("sect."))
		{
			int k;
			String newtext= text;
			String spauth="";
			String sectauth="";
			String subgauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].contains("subg.")||var[k].contains("sect."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("genus_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			if(var[k].contains("subg."))
			{
			k++;
			Element subfm= new Element("subgenus_name");
			subfm.setText(var[k].replaceFirst("(\\W)$", ""));
			syn.addContent(subfm);
			k++;
			while((k<var.length)&&!(var[k].contains("sect.")))
			{
				subgauth=subgauth+var[k]+" ";
				k++;
			}
			subgauth=subgauth.trim();
			if(subgauth.length()!=0)
			{
				Element subauth= new Element("subgenus_authority");
				subauth.setText(subgauth);
				syn.addContent(subauth);
			}
			if(var[k].contains("sect."))
			{
				k++;
				Element sect= new Element("section_name");
				sect.setText(var[k]);
				syn.addContent(sect);
				k++;
				while(k<var.length)
				{
					sectauth=sectauth+var[k]+" ";
					k++;
				}
				sectauth=sectauth.trim();
				if(sectauth.length()!=0)
				{
				Element subat= new Element("section_authority");
				subat.setText(sectauth);
				syn.addContent(subat);
				}
			}
			else
			{
				System.out.println("Problem in section");
			}
			}
			else
			{
			k++;
			Element sect= new Element("section_name");
			sect.setText(var[k]);
			syn.addContent(sect);
			k++;
			while(k<var.length)
			{
				sectauth=sectauth+var[k]+" ";
				k++;
			}
			sectauth=sectauth.trim();
			if(sectauth.length()!=0)
			{
				Element subat= new Element("section_authority");
				subat.setText(sectauth);
				syn.addContent(subat);
			}
			}
		}
		else if(text.contains("subg."))
		{
			int k;
			String newtext= text;
			String spauth="";
			String sectauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			syn.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(var[k].contains("subg."))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("genus_authority");
			spat.setText(spauth);
			syn.addContent(spat);
			}
			k++;
			Element sect= new Element("subgenus_name");
			sect.setText(var[k].replaceFirst("(\\W)$", ""));
			syn.addContent(sect);
			k++;
			while(k<var.length)
			{
				sectauth=sectauth+var[k]+" ";
				k++;
			}
			sectauth=sectauth.trim();
			if(sectauth.length()!=0)
			{
			Element subat= new Element("subgenus_authority");
			subat.setText(sectauth);
			syn.addContent(subat);		
			}
		}
		
		else
		{
			//if(text.matches("(^[A-Z](\\w|\\.|ï)+\\s+){1}(([a-z]|×)(\\w|-)+\\s*){1}.*"))//species name
			if(text.matches("(^[A-Z](\\S)+\\s+){1}(([a-z]|×)(\\S)+\\s*){1}.*"))//species name
			{
				int k;
				String newtext= text;
				String spauth="";
				String[] var= text.split("\\s");
				Element newele= new Element("genus_name");
				newele.setText(var[0]);
				syn.addContent(newele);
				Element spele= new Element("species_name");
				spele.setText(var[1]);
				syn.addContent(spele);
				for(k=2;k<var.length;k++)
				{
						spauth+=var[k]+" ";
				}
				spauth=spauth.trim();
				if(spauth.length()!=0)
				{
				Element spat= new Element("species_authority");
				spat.setText(spauth);
				syn.addContent(spat);
				}
			}
			else if(fnamedetecter==1) // if it is a genus name
			{
				
				String newtext= text;
				String famauth="";
				String[] Chunks = text.split("-");
				text = Chunks[0];
				String[] family= text.split("\\s");
				Element newele= new Element("family_name");
				newele.setText(family[0]);
				syn.addContent(newele);
				for(int k=1;k<family.length;k++)
				{
					famauth+=family[k]+" ";
				}
				famauth=famauth.trim();
				if(famauth.length()!=0)
				{
					Element famat= new Element("family_authority");
					famat.setText(famauth);
					syn.addContent(famat);
				}

				
			}
			//else if(text.matches("(^[A-Z](\\w|\\.|ï)+\\s+){1}((\\[.*\\])|(\\(.*\\)))?\\s*([A-Z](\\w|\\.)+).*"))//genus name
			//else if(text.matches("(^[A-Z](\\S)+\\s+){1}((\\[.*\\])|(\\(.*\\)))?\\s*([A-Z](\\S)+).*"))//genus name
			else if(text.matches("(^[A-Z](\\S)+\\s+){1}((\\[.*\\])|(\\(.*\\)))?\\s*(([A-Z]|Ö|Á)(\\S)+).*"))//genus name
			{
				int k;
				int unrank=0;
				String newtext= text;
				String spauth="";
				String sectauth="";
				String[] var= text.split("\\s");
				Element newele= new Element("genus_name");
				newele.setText(var[0]);
				syn.addContent(newele);
				for(k=1;k<var.length;k++)
				{
					if(!var[k].matches("\\[unranked\\]"))
						spauth+=var[k]+" ";
					else
					{
						unrank=1;
						System.out.println("Unranked element in synonym");
						break;
					}
				}
				spauth=spauth.trim();
				if(spauth.length()!=0)
				{
				Element spat= new Element("genus_authority");
				spat.setText(spauth);
				syn.addContent(spat);
				}
				if(unrank==1)
				{
					k++;
					Element urname= new Element("unranked_epithet_name");
					urname.setText(var[k]);
					syn.addContent(urname);
					String unauth="";
					k++;
					while(k<var.length)
					{
						unauth=unauth+var[k]+" ";
						k++;
					}
					unauth=unauth.trim();
					if(unauth.length()!=0)
					{
					Element unat= new Element("unranked_epithet_authority");
					unat.setText(unauth);
					syn.addContent(unat);
					}
					
				}
			}

			else
			{
				System.out.println("Missed Chunk from Synonym");
				System.out.println(text);
				
			}
			
			
		}
	
		return syn;
	}
	
	private String cleantext(String namepart) throws Exception{
		String result="";
		String[] chunks=namepart.split("\\s");
		for(int j=0;j<chunks.length;j++)
		{
			if(chunks[j].matches("\\."))
			{
				result=result+chunks[j];
			}
			else
			{
				result=result+" "+chunks[j];
			}	
		}
		result=result.trim();
		String inter="";
		String[] newchunks=result.split("\\s");
		for(int m=0;m<newchunks.length;m++)
		{
			if(m==0&&newchunks[m].matches("\\w"))
			{
				inter=inter+newchunks[m];
				m++;
				inter=inter+newchunks[m];
			}
			else if(newchunks[m].matches("\\w|\\)"))
			{
				inter=inter+newchunks[m];
			}
			else if(newchunks[m].matches("×"))
			{
				inter=inter+" "+newchunks[m];
				m++;
				inter=inter+newchunks[m];
			}
			else if(newchunks[m].matches("§"))
			{

			}
			else
			{
				inter=inter+" "+newchunks[m];
			}
		}
		result=inter.trim();
		return result;
	}
	
	private void output(String filename) throws Exception {
		XMLOutputter outputter = new XMLOutputter();
		
		
		
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V19-good\\target\\Last\\" + filename;
		String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V20-good\\target\\last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V21-good\\target\\last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\v23-good\\target\\last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V26-good\\target\\last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V3-good\\target\\last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V4-good\\target\\Last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V5-good\\target\\Last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V7-good\\target\\Last\\" + filename;		
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V8-good\\target\\last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V27-good\\target\\last\\" + filename;
		Document doc = new Document(treatment);
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(file));
		outputter.output(doc, out);
	}
	private void createtreatment() throws Exception{
		treatment = new Element("treatment");
	}
		

}

