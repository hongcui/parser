/**
 * $Id$
 */
package fna.parsing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import fna.db.VolumeTransformerDbAccess;

/**
 * To transform the extracted data to the xml format.
 * 
 * Note: before the transformation, the data should pass the check without
 * error.
 * 
 * @author chunshui
 */
@SuppressWarnings({ "unchecked", "unused","static-access" })
public class VolumeTransformer extends Thread {
	
	private static String organnames ="2n|achene|anther|apex|awn|ax|bark|beak|blade|bract|bracteole|branch|branchlet|broad|calyx|capsule|cap_sule|caropohore|carpophore|caudex|cluster|corolla|corona|crown|cup_|cusp|cyme|cymule|embryo|endosperm|fascicle|filament|flower|fruit|head|herb|homophyllous|hypanthium|hypanth_ium|indument|inflore|inflorescence|inflores_cence|inflo_rescence|internode|involucre|invo_lucre|in_florescence|in_ternode|leaf|limb|lobe|margin|midvein|nectary|node|ocrea|ocreola|ovary|ovule|pair|papilla|pedicel|pedicle|peduncle|perennial|perianth|petal|petiole|plant|prickle|rhizome|rhi_zome|root|rootstock|rosette|scape|seed|sepal|shoot|spikelet|spur|stamen|stem|stigma|stipule|sti_pule|structure|style|subshrub|taproot|taprooted|tap_root|tendril|tepal|testa|tooth|tree|tube|tubercle|tubercule|tuft|twig|utricle|vein|vine|wing|x";
	private static String organnamep ="achenes|anthers|awns|axes|blades|bracteoles|bracts|branches|buds|bumps|calyces|capsules|clusters|crescents|crowns|cusps|cymes|cymules|ends|escences|fascicles|filaments|flowers|fruits|heads|herbs|hoods|inflores|inflorescences|internodes|involucres|leaves|lengths|limbs|lobes|margins|midribs|midveins|nectaries|nodes|ocreae|ocreolae|ovules|pairs|papillae|pedicels|pedicles|peduncles|perennials|perianths|petals|petioles|pistils|plants|prickles|pules|rescences|rhizomes|rhi_zomes|roots|rows|scapes|seeds|sepals|shoots|spikelets|stamens|staminodes|stems|stigmas|stipules|sti_pules|structures|styles|subshrubs|taproots|tap_roots|teeth|tendrils|tepals|trees|tubercles|tubercules|tubes|tufts|twigs|utricles|veins|vines|wings";
	private static String usstates ="Ala\\.|Alaska|Ariz\\.|Ark\\.|Calif\\.|Colo\\.|Conn\\.|Del\\.|D\\.C\\.|Fla\\.|Ga\\.|Idaho|Ill\\.|Ind\\.|Iowa|Kans\\.|Ky\\.|La\\.|Maine|Md\\.|Mass\\.|Mich\\.|Minn\\.|Miss\\.|Mo\\.|Mont\\.|Nebr\\.|Nev\\.|N\\.H\\.|N\\.J\\.|N\\.Mex\\.|N\\.Y\\.|N\\.C\\.|N\\.Dak\\.|Ohio|Okla\\.|Oreg\\.|Pa\\.|R\\.I\\.|S\\.C\\.|S\\.Dak\\.|Tenn\\.|Tex\\.|Utah|Vt\\.|Va\\.|Wash\\.|W\\.Va\\.|Wis\\.|Wyo\\.";	
	private static String caprovinces="Alta\\.|B\\.C\\.|Man\\.|N\\.B\\.|Nfld\\. and Labr|N\\.W\\.T\\.|N\\.S\\.|Nunavut|Ont\\.|P\\.E\\.I\\.|Que\\.|Sask\\.|Yukon";
	private Properties styleMappings;
	private TaxonIndexer ti;
	private ProcessListener listener;
	private Hashtable errors;
	//TODO: put the following in a conf file. same for those in volumeExtractor.java
	//private String start = "^Heading.*"; //starts a treatment
	private String start = VolumeExtractor.getStart(); //starts a treatment
	private String names = ".*?(Syn|Name).*"; //other interesting names worth parsing
	private String conservednamestatement ="(name conserved|nom. cons.)";
	private static final Logger LOGGER = Logger.getLogger(VolumeTransformer.class);
	private VolumeTransformerDbAccess vtDbA = null;	
	private Hashtable ranks;

	private String taxontable = null;
	private String authortable = null;
	private String publicationtable = null;
	private Connection conn = null;
	private String dataPrefix;
	
	public VolumeTransformer(ProcessListener listener, String dataPrefix) throws ParsingException {
		this.listener = listener;
		this.dataPrefix = dataPrefix;
		this.errors = new Hashtable();
		this.taxontable = dataPrefix.trim()+"_"	+ ApplicationUtilities.getProperty("taxontable");
		this.authortable = dataPrefix.trim() + "_" + ApplicationUtilities.getProperty("authortable");
		this.publicationtable = dataPrefix.trim() + "_" + ApplicationUtilities.getProperty("publicationtable");
		vtDbA = new VolumeTransformerDbAccess(dataPrefix);
		
		ti = TaxonIndexer.loadUpdated(Registry.ConfigurationDirectory);
		if(ti.emptyNumbers() || ti.emptyNames()) ti = null;
		
		// load style mapping
		styleMappings = new Properties();
		try {
			styleMappings.load(new FileInputStream(
					Registry.ConfigurationDirectory
							+ "/style-mapping.properties"));
		} catch (IOException e) {
			throw new ParsingException(
					"Failed to load the style mapping file!", e);
		}
		
		try{
			if(conn == null){
				String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists "+taxontable+" (taxonnumber varchar(10), name varchar(100), rank varchar(20), filenumber int, primary key(filenumber, name, rank))");
				stmt.execute("delete from "+taxontable);
				stmt.execute("create table if not exists "+ authortable+" (authority varchar(200) NOT NULL PRIMARY KEY)");
				stmt.execute("delete from "+authortable);
				stmt.execute("create table if not exists "+ publicationtable+" (publication varchar(200) NOT NULL PRIMARY KEY)");
				stmt.execute("delete from "+publicationtable);
			}
		}catch(Exception e){
			LOGGER.error("VolumeTransformer : Database error in constructor", e);
			e.printStackTrace();
		}	

	}

	
	/**
	 * Transform the extracted data to the xml format.
	 */
	public void run() {
		listener.setProgressBarVisible(true);
		transform();
		listener.setProgressBarVisible(false);
	}
	public void transform() throws ParsingException {
		//add start
		List idlist = new ArrayList();
		int iteratorcount = 0;
		String state = "", preid = "", id = "", nextstep = "";
		String split[] = new String[3];
		String split1[] = new String[30];
		String latin[] = new String[300];
		latin[0] = "a";
		latin[1] = "b";
		latin[2] = "c";
		latin[3] = "d";
		latin[4] = "e";
		latin[5] = "f";
		latin[6] = "g";
		latin[7] = "h";
		latin[8] = "i";
		//add end
		// get the extracted files list
		File source = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty("EXTRACTED"));
		int total = source.listFiles().length;
		listener.progress(1);
		try {
			for (int count = 1; count <= total; count++) {

				File file = new File(source, count + ".xml");
				// logger.info("Start to process: " + file.getName());

				SAXBuilder builder = new SAXBuilder();
				Document doc = builder.build(file);
				Element root = doc.getRootElement();

				Element treatment = new Element("treatment");
				Element e2 = new Element("key");
				List plist = XPath.selectNodes(root, "/treatment/paragraph");
				int textcount = 0, nextstepid = 0;
				String ptexttag ="";
				String idstorage = "1";
				
				for (Iterator iter = plist.iterator(); iter.hasNext();) {
					Element pe = (Element) iter.next();
					String style = pe.getChildText("style");
					String text = getChildText(pe, "text");
					
					
					if (style.matches(start) ) {
						// process the name tag
						String sm= styleMappings.getProperty(style);//hong 6/26/08
						parseNameTag(count - 1, sm, text, treatment);
					}else if  (style.matches(names)) {
						// process the  synonym name tag
						String sm= styleMappings.getProperty(style);//hong 6/26/08
						parseSynTag(sm, text, treatment);
					}else if  (style.indexOf("Text") >= 0) {//hong 6/26/08
						// process the description, distribution, discussion tag
						if(text.trim().compareTo("") !=0){
							textcount++;
							ptexttag = parseTextTag(textcount, text, treatment, count, ptexttag);
						}
					}
					else {
						String sm = styleMappings.getProperty(style);
						Element e = new Element(sm);
						text=text.replaceFirst("SELECTED REFERENCES?", "").trim();
						//Start text format change++++++++++++++++++++++++++++++++++++++++++++++++++
						
						//
						
						Element initial = new Element("initial_state");
						Element states = new Element("state");
						Element nextsteps = new Element("next_step");
						
						
						if(sm.equalsIgnoreCase("run_in_sidehead")){
							e2 = new Element("key");
							e2.setAttribute(new Attribute("name", text));
							treatment.addContent(e2);
							idlist.clear();
						}
						else if(sm.equals("key")){
							Element e1 = new Element("couplet");
							if (text.contains(" v. ") && text.contains(" p. ")
									&& !text.contains("Group ")) {
								split = text.split("[0-9]+[a-z]?\\. ");
								split1 = split[0].split("\\.");
								preid = split1[0];
								state = split[0].replace(preid + ".", "");
								nextstep = text.replace(split[0], "");
								idstorage = preid;
								Iterator iditerator = idlist.iterator();
								iteratorcount = 0;
								while(iditerator.hasNext()){
									String itemid = (String)iditerator.next();
									if(itemid.equalsIgnoreCase(preid)){
										iteratorcount++;
									}
								}
								id = preid + latin[iteratorcount];
								idlist.add(preid);
								nextsteps.setText(nextstep);
								// System.out.println(nextstep);
							} else if (text.contains(" v. ")
									&& text.contains(" p. ")
									&& text.contains("Group ")) {
								split = text.split("Group [0-9]");
								split1 = split[0].split("\\.");
								preid = split1[0];
								state = split[0].replace(preid + ".", "");
								nextstep = text.replace(split[0], "");
								idstorage = preid;
								Iterator iditerator = idlist.iterator();
								iteratorcount = 0;
								while(iditerator.hasNext()){
									String itemid = (String)iditerator.next();
									if(itemid.equalsIgnoreCase(preid)){
										iteratorcount++;
									}
								}
								id = preid + latin[iteratorcount];
								idlist.add(preid);
								nextsteps.setText(nextstep);
								// System.out.println(nextstep);
							} else if (!text.contains("Shifted to left margin.")&&text.contains("")) {
								split1 = text.split("\\.");
								preid = split1[0];
								state = text.replace(preid + ".", "");
								try{
								nextstepid = Integer.parseInt(idstorage) + 1;
								}catch(Exception excep){
									continue;
								}
								nextstep = nextstepid + "a";
								idstorage = preid;
								Iterator iditerator = idlist.iterator();
								iteratorcount = 0;
								while(iditerator.hasNext()){
									String itemid = (String)iditerator.next();
									if(itemid.equalsIgnoreCase(preid)){
										iteratorcount++;
									}
								}
								id = preid + latin[iteratorcount];
								idlist.add(preid);
								nextsteps.setAttribute(new Attribute("id", nextstep));
								//nextstep = nextid + "a";
								 //System.out.println(preid + "   " + state + "   " + nextstep);
							}
							
							initial.setAttribute(new Attribute("id", id));
							states.setText(state);
							
							e1.addContent(initial);
							e1.addContent(states);
							e1.addContent(nextsteps);
							e2.addContent(e1);
						}
						else{
							e.setName(sm);
							e.setText(text);
							treatment.addContent(e);
						}
							
						//end text format change++++++++++++++++++++++++++++++++++++++++++++++
						Matcher refM=Pattern.compile("([A-Z]\\w*?,? [A-Z]\\.)+(.*?)\\.(?=\\s[A-Z]\\w*?,? [A-Z]\\.(,|\\s?\\d{4}|\\s?[A-Z]\\.)|$)").matcher(text);
						while(refM.find()){
							addElement("reference",refM.group(1),e);
						}
						//e.setText(text);
						
						
	
					}
				}

				// output the treatment to transformed
				File xml = new File(Registry.TargetDirectory,
						ApplicationUtilities.getProperty("TRANSFORMED") + "/" + count + ".xml");
				ParsingUtil.outputXML(treatment, xml ,null);
				String error = (String)errors.get(count+"");
				error = error ==null? "":error;
				
				// output the description part to Registry.descriptions 08/04/09
				List<Element> textList = XPath.selectNodes(treatment, "./description");
				StringBuffer buffer = new StringBuffer("");
				for (Iterator ti = textList.iterator(); ti.hasNext();) {
					Element wt = (Element) ti.next();
					buffer.append(wt.getText()).append(" ");
				}
				String text = buffer.toString().replaceAll("\\s+", " ").trim();
				outputElementText(count, text, "DESCRIPTIONS");
				
				// output the habitat part to Registry.habitat 08/04/09
				textList = XPath.selectNodes(treatment, "./habitat");
				buffer = new StringBuffer("");
				for (Iterator ti = textList.iterator(); ti.hasNext();) {
					Element wt = (Element) ti.next();
					buffer.append(wt.getText()).append(" ");
				}
				text = buffer.toString().replaceAll("\\s+", " ").trim();
				outputElementText(count, text, "HABITATS");
				
				
				//listener.info(String.valueOf(count), xml.getPath(), error);
				listener.progress((count*50) / total);
			}
			
			HabitatParser4FNA hpf = new HabitatParser4FNA(dataPrefix);
			hpf.parse();
			VolumeFinalizer vf = new VolumeFinalizer(listener, null, this.conn,null);
			vf.replaceWithAnnotated(hpf, "/treatment/habitat", "TRANSFORMED", true);
		} catch (Exception e) {
			LOGGER.error("VolumeTransformer : transform - error in parsing", e);
			e.printStackTrace();
			throw new ParsingException(e);
		}
	}

	private String getChildText(Element pe, String string) throws Exception{
		// TODO Auto-generated method stub
		StringBuffer buffer=new StringBuffer();
		List<Element> textList = XPath.selectNodes(pe, "./"+string);
		for (Iterator ti = textList.iterator(); ti.hasNext();) {
			Element wt = (Element) ti.next();
			buffer.append(wt.getText()).append(" ");
		}
		return buffer.toString().replaceAll("\\s+", " ").trim();
	}

	private String parseTextTag(int textcount, String text, Element treatment, int filecount, String ptag){

		String tag = "";
		Pattern organpt = Pattern.compile("\\b("+this.organnamep+"|"+this.organnames+")\\b", Pattern.CASE_INSENSITIVE);
		Matcher m = organpt.matcher(text);
		int organcount = 0;
		while(m.find()){
			////System.out.println(m.group());
			organcount++;
		}
		if(textcount ==1 && organcount >=2){
			tag = "description";
			addElement("description", text, treatment);
			//outputDescriptionText(filecount, text); //hong: 08/04/09 take this function out. FOC descriptions are not part of TEXT.
		}else if((textcount ==1 && organcount < 2)){
			tag = "distribution";
			//TODO: further markup distribution to: # of infrataxa, introduced, generalized distribution, flowering time,habitat, elevation, state distribution, global distribution 
			//addElement("distribution", text, treatment);
			parseDistriTag(text, treatment);
		}//else if(ptag.compareTo("distribution")==0){
		else if(ptag.compareTo("description")==0){//hong: 3/11/10 for FNA v19
			tag = "distribution";
			//TODO: further markup distribution to: # of infrataxa, introduced, generalized distribution, flowering time,habitat, elevation, state distribution, global distribution 
			//addElement("distribution", text, treatment);
			parseDistriTag(text, treatment);
		}else if(ptag.compareTo("distribution")==0||ptag.compareTo("discussion")==0){
			tag = "discussion";
			addElement("discussion", text, treatment);
			//System.out.println("discussion:"+text);
		}
		return tag;
		
	}
	/**
	 * further markup distribution to: (species-with infrataxa and higher)
	 * # of infrataxa, introduced, generalized distribution, 
	 * or (species-without infrataxa and lower)
	 * flowering time,habitat, elevation, state distribution, global distribution 
	 * @param text
	 * @param treatment
	 */
	private void parseDistriTag(String text, Element treatment){
		//System.out.println("::::::::::::::::::::::::::::::::::\ndistribution: "+text);
		Pattern rankp = Pattern.compile("^((?:Genera|Genus|Species|Subspecies|Varieties|Subgenera).*?:)\\s*(introduced\\s*;)?(.*)");
		Matcher m = rankp.matcher(text);
		if(m.matches()){//species and higher
			if(m.group(1) != null){
				addElement("number_of_infrataxa",m.group(1), treatment);
				//System.out.println("number_of_infrataxa:"+m.group(1));
			}
			if(m.group(2)!=null){
					addElement("introduced", m.group(2), treatment);
					//System.out.println("introduced:"+m.group(2));
			}
			if(m.group(3) != null){
					//addElement("general_distribution", m.group(3), treatment);
					//further markkup distribution
					DistributionParser4FNA dp = new DistributionParser4FNA(treatment, m.group(3), "general_distribution");
					treatment = dp.parse(); 
					//System.out.println("general_distribution:"+m.group(3));
			}	
		}else{//species and lower
			Pattern h = Pattern.compile("(Flowering.*?\\.)?(.*?(?:;|\\.$))?(\\s*of conservation concern\\s*(?:;|\\.$))?(.*?\\b(?:\\d+|m)\\b.*?(?:;|\\.$))?\\s*(introduced(?:;|\\.$))?(.*)");
			Matcher mh = h.matcher(text);
			if(mh.matches()){//TODO:habitat, elevation, state distribution, global distribution
				if(mh.group(1) != null){
					//addElement("flowering_time",mh.group(1), treatment);
					//further markkup distribution
					FloweringTimeParser4FNA dp = new FloweringTimeParser4FNA(treatment, mh.group(1), "flowering_time");
					treatment = dp.parse(); 
					//System.out.println("flowering_time:"+mh.group(1));
				}
				if(mh.group(2)!= null){
					addElement("habitat",mh.group(2), treatment);
					System.out.println("habitat:"+mh.group(2));
				}
				if(mh.group(3)!= null){
					addElement("conservation",mh.group(3), treatment);
					//System.out.println("conservation:"+mh.group(3));
				}
				if(mh.group(4)!= null){
					addElement("elevation",mh.group(4), treatment);
					//System.out.println("elevation:"+mh.group(4));
				}
				if(mh.group(5)!= null){
					addElement("introduced",mh.group(5), treatment);
					//System.out.println("introduced:"+mh.group(5));
				}
				if(mh.group(6)!= null){
					String[] distrs = mh.group(6).split(";");
					for(int i= 0; i<distrs.length; i++){
						if(distrs[i].matches(".*?\\b("+this.usstates+")(\\W|$).*")){
							//addElement("us_distribution",distrs[i], treatment);
							//further markkup distribution
							DistributionParser4FNA dp = new DistributionParser4FNA(treatment, distrs[i], "us_distribution");
							treatment = dp.parse(); 
							//System.out.println("us_distribution:"+distrs[i]);
						}else if(distrs[i].matches(".*?\\b("+this.caprovinces+")(\\W|$).*")){
							//addElement("ca_distribution",distrs[i], treatment);
							//further markkup distribution
							DistributionParser4FNA dp = new DistributionParser4FNA(treatment, distrs[i], "ca_distribution");
							treatment = dp.parse(); 
							//System.out.println("ca_distribution:"+distrs[i]);
						}else{
							//addElement("global_distribution",distrs[i], treatment);
							//further markkup distribution
							DistributionParser4FNA dp = new DistributionParser4FNA(treatment, distrs[i], "global_distribution");
							treatment = dp.parse(); 
							//System.out.println("global_distribution:"+distrs[i]);
						}
					}
				}
			}else{
				System.err.println("distribution not match: "+text);
			}
			
			
		}
	}
	
	private void parseSynTag(String tag, String text, Element treatment){
		Element e = treatment.getChild("variety_name");
		if(e != null){
			tag = "synonym_of_variety_name";
		}else if((e = treatment.getChild("subspecies_name"))!=null){
			tag = "synonym_of_subspecies_name";
		}else if((e = treatment.getChild("species_name"))!=null){
			tag = "synonym_of_species_name";
		}else if((e = treatment.getChild("tribe_name"))!=null){
			tag = "synonym_of_tribe_name";
		}else if((e = treatment.getChild("genus_name"))!=null){
			tag = "synonym_of_genus_name";
		}
		
		addElement(tag, text, treatment);
		//System.out.println(tag+":"+text);
	}
	
	private String parseNameTag(int index, String namerank, String line,
			Element treatment) {
		if(line == null || line.equals("")){
			return ""; //TODO: should not happen. but did happen with v. 19 295.xml==>VolumeExtractor JDOM problem.
		}
		
		String name = ti.getName(index);
		if(name==null ||name.compareTo("") == 0){
			errors.put((index+1)+"","no name found in: "+line);
			return "";
		}
		// make a copy of the line and will work on the new copy
		String text = new String(line);
		//System.out.println("index="+index);
		//System.out.println("text="+text);
		
		String number = null;
		if(ti != null)
			number = ti.getNumber(index);
		else{
			number = line.substring(0, line.indexOf('.'));
		}
		// number
		addElement("number", number, treatment); // TODO: add the number tag
		                                         // to the sytle mapping
		//System.out.println("number="+number);
		
		//text = text.substring(number.length() + 1); //Hong 08/04/09 change to
		text = text.replaceFirst("\\s*\\d.*?\\s+", "");
		
		//name
		if(namerank.indexOf("species_subspecies_variety_name")>=0){
			if(text.indexOf("var.") >=0){
				namerank = "variety_name";
			}else if(text.indexOf("subsp.") >=0){
				namerank = "subspecies_name";
			}else {
				namerank = "species_name";
			}
		}
		
		addElement(namerank, name, treatment);
		try {
			vtDbA.add2TaxonTable(number, name, namerank, index+1);
		} catch (ParsingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOGGER.error("Couldn't perform parsing in VolumeTransformer:parseNameTag", e);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOGGER.error("Database access error in VolumeTransformer:parseNameTag", e);
		}
		//System.out.println("name="+name);
		//System.out.println(namerank+":"+name);
		text = VolumeVerifier.fixBrokenNames(text);
		text = text.replaceFirst("^\\s*.{"+name.length()+"}","").trim();
		
		//authority
		Pattern p = Pattern.compile("(.*?)((?: in|,|·|\\?).*)");
		Matcher m = p.matcher(text);
		if(m.matches()){
			if(m.group(1).trim().compareTo("")!= 0){
				addElement("authority", m.group(1).trim(), treatment);
				try {
					vtDbA.add2AuthorTable(m.group(1).trim());
				} catch (ParsingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					LOGGER.error("Couldn't perform parsing in VolumeTransformer:parseNameTag", e);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					LOGGER.error("Database access error in VolumeTransformer:parseNameTag", e);
				}
				//System.out.println("authority:"+m.group(1).trim());
			}
			text = m.group(2).trim();
		}
		//save the segment after ?or ?for later
		String ending = "";
		int pos = text.lastIndexOf('.');
		if(pos < 0){
			pos = text.lastIndexOf('?');
		}
		if (pos != -1) {
			ending = text.substring(pos + 1).trim();
			text = text.substring(0, pos+1);
		}
		
		//place of publication
		p = Pattern.compile("(.* [12]\\d\\d\\d|.*(?=·)|.*(?=.))(.*)"); //TODO: a better fix is needed Brittonia 28: 427, fig. 1.  1977   ?  Yellow spinecape [For George Jones Goodman, 1904-1999
		m = p.matcher(text);
		
		if(m.matches()){
			if(m.group(1).trim().compareTo("")!= 0){
				String pp = m.group(1).replaceFirst("^\\s*[,\\.]", "").trim();			
				String pt="";
				String pip="";
				
				String place_in_publication="(?mis)(.*?)(\\d*?:\\s\\d*?\\.\\s\\d*)";
				Matcher pubm=Pattern.compile(place_in_publication).matcher(pp);
				if(pubm.find()){
					pt=pubm.group(1).trim();
					pip=pubm.group(2).trim();
				}			
				
				Element placeOfPub=new Element("place_of_publication");
				addElement("publication_title",pt,placeOfPub);
				addElement("place_in_publication",pip,placeOfPub);
				treatment.addContent(placeOfPub);
				//addElement("place_of_publication",pp, treatment);
				
				try {
					vtDbA.add2PublicationTable(pp);
				} catch (ParsingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					LOGGER.error("Couldn't perform parsing in VolumeTransformer:parseNameTag", e);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					LOGGER.error("Database access error in VolumeTransformer:parseNameTag", e);
				}
				//System.out.println("place_of_publication:"+m.group(1).trim());
			}
			text = m.group(2).trim();
		}

		// conserved
		{
			pos = text.indexOf("name conserved");
			if(pos < 0){
				pos = text.indexOf("name proposed for conservation");
			}
			if(pos < 0){
				pos = text.indexOf("nom. cons.");
			}
			if (pos != -1) {
				String conserved = text.substring(pos).trim();
				addElement("conserved", conserved, treatment);
				//System.out.println("conserved:"+conserved);
				
				// trim the text
				int p1 = text.lastIndexOf(',', pos);
				text = text.substring(0, p1);
			}
		}
		//past_name
		p = Pattern.compile("\\((?:as )?(.*?)\\)(.*)");
		m = p.matcher(text);
		if(m.matches()){
			if(m.group(1).trim().compareTo("")!= 0){
				addElement("past_name", m.group(1).trim(), treatment);
				//System.out.println("past_name:"+m.group(1).trim());
			}
			text = m.group(2).trim();
		}
		// format mark, common name, derivation
		{
			//int pos = text.lastIndexOf('?);
			//if(pos < 0){
			//	pos = text.lastIndexOf('?);
			//}
			if (ending.compareTo("") != 0) {
				//String ending = text.substring(pos + 1).trim();
				String[] results = ending.split("\\[");

				String commonName = results[0].trim();
				addElement("common_name", commonName, treatment);
				//System.out.println("common_name:"+commonName);

				if (results.length > 1) {
					String derivation = results[1].trim();
					derivation = derivation.substring(0,
							derivation.length() - 1); // remove the last ']'
					addElement("derivation", derivation, treatment);
					//System.out.println("derivation:"+derivation);
				}
				
				//text = text.substring(0, pos).trim();
			}
		}
		

		if(text.trim().matches(".*?\\w+.*")){
			errors.put((index+1)+"","still left: "+text);
		}
		return namerank.replace("_name", "");
	}

	private static void addElement(String tag, String text, Element parent) {
		Element e = new Element(tag);
		e.setText(text);
		parent.addContent(e);
	}

	private void outputElementText(int count, String text, String elementname) throws ParsingException {
		//System.out.println("write file "+count+".txt");
		//elementname = "DESCRIPTIONS"
		try {
			File file = new File(Registry.TargetDirectory,
					ApplicationUtilities.getProperty(elementname) + "/" + count + ".txt");
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(text);
			out.close(); // don't forget to close the output stream!!!
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("Failed to output text file in VolumeTransformer:outputDescriptionText", e);
			throw new ParsingException("Failed to output text file.", e);
		}
	}
	


}
