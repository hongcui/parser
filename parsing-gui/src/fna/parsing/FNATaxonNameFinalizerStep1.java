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
 * The complete FNATaxonNameFinalizer consists 2 steps:
 * FNATaxonNameFinalizerStep1:
 * run this class on Transformed folder (for FNA, v3, 8, 27 used different parsing algorithms than the rest of the volumes)
 * used to 
 * 1. review <xxx_name> and <synonym> elements and process them in FNA volumes for JSTOR
 * it also 
 * 2. fix those problematic habitat and discussion elements (phenology being marked as habitat, habitat, elevation, and distribution being marked as discussion).
 * 3. expand abbreviated genus name (e.g. C.) in synonyms to full name
 * 
 * After manual review** of the annotation results, run step 2:
 * FNATaxonNameFinalizerStep2:
 * 4. fix the rank for some synonyms (before the fix, all synonym starts at the rank of genus)
 * 
 * 
 * Manual Review procedure:
 * Use FNANameReviewer to produce the report for review for points 1 - 4.
 * 1. check tribe_name elements in ACCEPTED TaxonIdentification (they sometimes are mistaken as section_name)
 * 2. check section_name elements in ACCEPTED TaxonIdentification (they sometimes are mistaken as tribe_name)
 * 3. check family_name, subfamily_name, tribe_name, subtribe_name, section_name, subsection_name, series_name, species_name, subspecies_name, variety_name
 *     expect: all names is 1 word long; species and lower rank names are all in small case;
 *     check for special characters: "x" 
 *     check for very short names, 2-3 character long: "de"
 *     "series_name" may be confused with "ser." in publication title.
 * 4. check .*_authority:
 *      citation and year (e.g., 1897) should not be part of authority: year should be marked as place_in_publication
 *      "not ..." and "based on ..." should not be part of authority [note: "authority [not xyz]" is fine, but "authority YEAR [not xyz]" is not: YEAR => <place_in_publication>, [not xyz] => <other_info>
 * 5. check publication_title:
 *      check for the inclusion of authority in <publication_title>: output all pub_title to excel, sort it, then review. (Smith & John would be suspicious in pub_title)
 *      check taxon ranks such as var. subg. sect. ser. subsp. subf. in pub_title
 * Use oXygen to check
 * 6. references: remove "item:" and "selected references:" in references
 * 7. review <habitat> elements
 * 8. make sure there is 0 <synonym> tag       
 * 9. check <global_distribution> for "introduced;" (should be marked as <introduced>).    
 * 10. check <discussion> for number_of_infrataxa and distribution.  
 *          
 */
public class FNATaxonNameFinalizerStep1{
	static String filename = null;
	Element treatment = new Element("treatment");
	static int partdetecter, count;
	static Hashtable hashtable = new Hashtable();
	static int fnamedetecter=0;
	Pattern time = Pattern.compile("(year[ -]round|spring|summer|fall|winter|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\\b.*", Pattern.CASE_INSENSITIVE);
	Pattern abb = Pattern.compile("([A-Z][a-z]+\\.[;,]? ?){2,}");
	//Pattern meter = Pattern.compile("\\d+(?:[–-]\\d+)? m\\b");
	Pattern meter = Pattern.compile("[\\d–()-]+ m\\b");
	Pattern name = Pattern.compile("\\b[A-Z]\\. "); //should not contain species names C. sdfl
	Pattern year = Pattern.compile(".* [12]\\d{3,3}$");
	private static String usstates ="Ala\\.|Alaska|Ariz\\.|Ark\\.|Calif\\.|Colo\\.|Conn\\.|Del\\.|D\\.C\\.|Fla\\.|Ga\\.|Idaho|Ill\\.|Ind\\.|Iowa|Kans\\.|Ky\\.|La\\.|Maine|Md\\.|Mass\\.|Mich\\.|Minn\\.|Miss\\.|Mo\\.|Mont\\.|Nebr\\.|Nev\\.|N\\.H\\.|N\\.J\\.|N\\.Mex\\.|N\\.Y\\.|N\\.C\\.|N\\.Dak\\.|Ohio|Okla\\.|Oreg\\.|Pa\\.|R\\.I\\.|S\\.C\\.|S\\.Dak\\.|Tenn\\.|Tex\\.|Utah|Vt\\.|Va\\.|Wash\\.|W\\.Va\\.|Wis\\.|Wyo\\.";	
	private static String caprovinces="Alta\\.|B\\.C\\.|Man\\.|N\\.B\\.|Nfld\\. and Labr|N\\.W\\.T\\.|N\\.S\\.|Nunavut|Ont\\.|P\\.E\\.I\\.|Que\\.|Sask\\.|Yukon";
	Pattern us = Pattern.compile("("+usstates+")");
	Pattern ca = Pattern.compile("("+caprovinces+")");
	Pattern abbrgenus = Pattern.compile("[A-Z]\\.");
	static XPath discussion;
	static XPath placeInPub;
	static XPath synPath;
	static XPath acceptPath;
	static XPath namePath;
	static XPath authorPath;
	static{
		try{
			discussion = XPath.newInstance(".//discussion");
			placeInPub = XPath.newInstance(".//place_in_publication");
			synPath = XPath.newInstance(".//TaxonIdentification[@Status=\"SYNONYM\"]"); //TaxonIdentification Status="SYNONYM"
			acceptPath =  XPath.newInstance(".//TaxonIdentification[@Status=\"ACCEPTED\"]");
			namePath = XPath.newInstance(".//*[contains(name(),'_name')]");
			authorPath = XPath.newInstance(".//*[contains(name(),'_authority')]");
		}catch(Exception e){
			e.printStackTrace();
		}		
	}

	public static void main(String[] args) throws Exception{
	
		ObjectOutputStream outputStream = null;
		

		outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V26-good\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V23-good\\namemapping.bin"));

		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V19-good\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V20-good\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V21-good\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Documents and Settings\\Hong Updates\\Desktop\\trash\\test\\namemapping.bin"));

		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V19-good\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V20-good\\namemapping.bin"));
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
		Hashtable mapping = new Hashtable();//the mapping here is trivial,mapping file index to file name only.
		

		File extracted = new File("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V26-good\\target\\transformed");
		//File extracted = new File("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V23-good\\target\\transformed");

		//File extracted = new File("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V19-good\\target\\transformed");
		//File extracted = new File("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V20-good\\target\\transformed");
		//File extracted = new File("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V21-good\\target\\transformed");
		//File extracted = new File("C:\\Documents and Settings\\Hong Updates\\Desktop\\trash\\test");

		
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V19-good\\target\\transformed");
		//File extracted = new File("C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V20-good\\target\\transformed");
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
			FNATaxonNameFinalizerStep1 transformer = new FNATaxonNameFinalizerStep1();
			//1. fine-grained markup of name elements
			transformer.createtreatment();
			if(partdetecter == 0){
				taxonname = transformer.processparagraph(paralist);
				if(taxonname == null){
					taxonname = "taxon name";
				}
				System.out.println(taxonname);//print filename
				mapping.put(i, taxonname);
			}else{
				System.out.println("Problem in main");
			}
			//Then 2. fix habitat and discussion elements that has problems
			transformer.fixHabitatDiscussion();
			//Then 3. expand abbreviated genus synonym name to full name
			transformer.expandAbbrNames();
			//Then 3.5 split authority from year (part of place_in_publication)
			transformer.splitYear();
			//Then 4. fix ranks for some synonyms, do this only after manual review of previous results, so the errors there don't get propagated to 4.
			//transformer.fixSynonymRank();
			transformer.output(filename);
		}
		outputStream.writeObject(mapping);
	}
	
	private void splitYear() {
		try{
			List<Element> auths = authorPath.selectNodes(treatment);
			for(Element auth : auths){
				String text = auth.getTextTrim();
				Matcher m = year.matcher(text);
				if(m.matches()){
					String year = text.substring(text.length()-4);
					text = text.replace(year, "").trim();
					auth.setText(text);
					Element parent = auth.getParentElement();
					int authindex = parent.indexOf(auth);
					Element next = (Element) parent.getContent(authindex+1);
					if(next.getName().equals("place_of_publication")){
						//insert year in place_in_publication
						Element e = new Element("place_in_publication");
						e.setText(year);
						next.addContent(0, e);
					}else{ //create place_of_publication
						Element e = new Element("place_of_publication");
						Element e1 = new Element("place_in_publication");
						e1.setText(year);
						e.addContent(e1);
						parent.addContent(authindex+1, e);
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	/**
	 * deal with two categories:
	 * 1. synonyms with at least genus and species(and any lower rank) need not fix
	 * 		1.1 if has citation, Status = "basionym"
	 *      1.2 else, Status = "synonym"
	 * 2. synonyms consisting one name need be checked:
	 * 		2.1 if has citation, Status = "basionym"
	 *      2.2 else, Status = "synonym" and rank = the lowest rank in "Accepted" name
	 */
	@SuppressWarnings("unchecked")
	private void fixSynonymRank() {
		try{
			List <Element> synonyms = synPath.selectNodes(treatment);
			for(Element synonym: synonyms){
				//test for basionym
				List<Element> citations = synonym.getChildren("place_of_publication");
				if(citations.size()>0){
					synonym.setAttribute("Status", "BASIONYM");
				}
				
				//test for single name
				List<Element> names = namePath.selectNodes(synonym);
				if(names.size()==1){
					//find the lowest rank in accepted name
					Element accepted = (Element) acceptPath.selectSingleNode(treatment);
					List<Element> anames = namePath.selectNodes(accepted);
					String rank = anames.get(anames.size()-1).getName().replaceFirst("_name", "");
					//replace the rank for name
					String oldname = names.get(0).getName();					
					names.get(0).setName(oldname.replaceFirst("^[^_]*", rank));
					//replace the rank for authority
					Element authority = (Element) authorPath.selectSingleNode(synonym);
					String oldauthname = authority.getName();					
					authority.setName(oldauthname.replaceFirst("^[^_]*", rank));
					System.out.println("rank replaced from "+oldname +" to "+rank);
				}				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	/*
	 *  "C. lanceolatus"  - will anyone know what genus "C." refers to?  
	 *  In this case, knowing the FNA format (within the synonymy, after the genus is spelled out once, 
	 *  the next, adjacent, occurrence of that genus can be abbreviated by the first letter)  
	 *  means that this "C." refers to Carduus, and not Cirsium as one might be expecting since
	 *   we are dealing with the treatment of Cirsium.  
	 */
	private void expandAbbrNames() {
		try{
			List <Element> synonyms = synPath.selectNodes(treatment);
			String lastfullname = "";
			for(Element synonym: synonyms){
				Element genus = synonym.getChild("genus_name");
				if(genus ==null) continue;
				String genusname = genus.getTextTrim();
				if(genusname.length()>2) lastfullname = genusname;
				else{
					Matcher m = abbrgenus.matcher(genusname);
					if(m.matches() && lastfullname.startsWith(genusname.replaceFirst("\\.$", ""))) genus.setText(lastfullname);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	/**
	 * fix the wrong tags for examples like:
	 *   <TaxonIdentification Status="SYNONYM">
        <genus_name>M.</genus_name>
        <species_name>platycarpha</species_name>
        <species_authority>(A. Gray) Schultz-Bipontinus</species_authority>
        <variety_name>parishii</variety_name>
        <variety_authority>(Greene) H. M. Hall</variety_authority>
    </TaxonIdentification>
    <habitat>cypselae columnar, 4–10 mm (tapered to basal callosities</habitat>
    <habitat>filled by embryos or distal 1/4–1/2 empty), ribs slightly flared apically</habitat>
    <habitat>slightly constricted below flaring</habitat>
    <elevation>pappi of orbiculate to lanceolate, arcuate, involute (glabrous or villous) scales
        1–6.5 mm, usually 1–6 mm shorter than cypselae. 2n = 18.</elevation>
    <discussion>Flowering Mar-–Jun. Clay soils, flats and hillsides, often by vernal pools or near
        serpentine outcrops, grasslands and open oak and Pinus sabiniana woodlands; 10–800 m;
        Calif., Oreg.</discussion>
	 * 
	 * 
	 * 
	 * @param root
	 * @return
	 */
	private void fixHabitatDiscussion() {
		try{
			List<Element> dises = discussion.selectNodes(treatment);
			for(Element dis: dises){
				if(needfix(dis)){
					System.out.println("problem discussion: "+dis.getTextTrim());
					fix(dis);
				}
			}
		}catch (Exception e){
			e.printStackTrace();
		}		
	}

	/**
	 * merge all habitat before discussion into <description>
	 * markup discussion by calling various parsers
	 * @param dis
	 */
	private void fix(Element dis) {
		//compose <description> element
		ArrayList<Element> afterdis = new ArrayList<Element>();
		Element description = treatment.getChild("description");
		if(description == null){
			description = new Element("description");
		}else{
			description.detach();
		}
		
		List<Element> children = treatment.getChildren();
		boolean finddis = false;
		String descrpt = "";
		int index = -1;
		int total = children.size();
		for(int i = total-1; i >=0; i--){
			Element e = children.get(i);
			if(!finddis && e.equals(dis)){
				finddis = true;
			}else if(finddis && e.getName().equals("habitat") || 
					finddis && e.getName().equals("elevation")||
					finddis && e.getName().contains("distribution") ||
					finddis && e.getName().equals("discussion") ||
					finddis && e.getName().equals("description")){
				if(e.getName().equals("discussion"))descrpt = e.getTextTrim()+descrpt;
				else descrpt = e.getTextTrim() + ", "+descrpt;
				e.detach();
				index = i;
			}
			if(!finddis && !e.equals(dis)){
				afterdis.add(e); //reserved order
				e.detach();
			}

		}
		descrpt = descrpt.trim().replaceAll("(^,\\s+|\\s+,$)", "").trim();
		//Hong
		if(descrpt.length()>0){
			if(!descrpt.endsWith("."))descrpt +=".";
			description.setText(description.getTextTrim()+descrpt);
			treatment.addContent(index, description);
		}
		
		//markup <discussion>
		String phenology="";
		String conservation="";
		String habitat="";
		String elevation = "";
		String distribution = "";

		dis.detach(); //new elements should be added at index + 1
		String text = dis.getTextTrim();
		
	    //<discussion>Flowering Mar-–Jun. Clay soils, flats and hillsides, often by vernal pools or near
        //serpentine outcrops, grasslands and open oak and Pinus sabiniana woodlands; 10–800 m;
        //Calif., Oreg.</discussion>
		Matcher m = time.matcher(text);
		String flag = "";
		if(text.startsWith("Flowering") || text.startsWith("Fruiting") || m.matches()){
			if(text.startsWith("Fruiting")) flag = "fruiting";
			phenology = text.substring(0, text.indexOf("."))+".";
			text = text.replace(phenology, "").trim();
			//markup flowering time
		}
		
		if(text.contains("of conservation concern")){
			conservation = "of conservation concern";
			text = text.replace(conservation, "").trim();
		}
		
		habitat = text.substring(0, text.indexOf(";"))+";";
		text = text.replace(habitat, "");
		m = meter.matcher(habitat);
		Matcher n = abb.matcher(habitat);
		if(m.find()){
			elevation = habitat;
			habitat = "";
		}else if(n.find()){
			distribution = habitat;
			habitat = "";
		}
		
		if(habitat.trim().length()>0){ //keep looking for elevation and distribution
			m = meter.matcher(text);
			if(m.find()){
				elevation = text.substring(m.start(), m.end());
				distribution = text.replace(elevation, "").replaceFirst("\\s*\\W\\s+", "");
			}else{
				distribution = text;
			}						
		}
		
		if(phenology.trim().length()>0){
			String tag = flag.length()==0? "phenology" : "phenology_"+flag; 
			FloweringTimeParser4FNA ftpf = new FloweringTimeParser4FNA(treatment, phenology, tag);
			ftpf.parse();			
		}

		if(habitat.trim().length() > 0){
			Element e = new Element("habitat");
			e.setText(habitat);
			treatment.addContent(e);
		}
		
		if(conservation.trim().length() > 0){
			Element e = new Element("conservation");
			e.setText(conservation);
			treatment.addContent(e);
		}	
		
		if(elevation.trim().length() > 0){
			Element e = new Element("elevation");
			e.setText(elevation);
			treatment.addContent(e);
		}
		
		if(distribution.trim().length() > 0){
			String [] distributes = distribution.split(";");
			for(String distr : distributes){
				String type = type(distr);
				DistributionParser4FNA p = new DistributionParser4FNA(treatment, distr, type+"_distribution");
				p.parse();
			}			
		}	
		
		//restore from afterdis
		for(int i = afterdis.size()-1; i>=0; i--){
			treatment.addContent(afterdis.get(i));
		}
	}
	
	

	private String type(String distr) {
		Matcher m = us.matcher(distr);
		if(m.find()) return "us";
		m = ca.matcher(distr);
		if(m.find()) return "ca";				
		return "global";
	}

	/**
	 * identify the problem cases by finding discussion elements with "Flowering...", "10-800 m;" and list of abbreivated words
	 * @param dis
	 * @return
	 */
	private boolean needfix(Element dis) {
		String text = dis.getTextTrim();
		Matcher m = name.matcher(text);
		if(m.find()) return false;
		int score = 0;
		if(text.startsWith("Flowering") || text.startsWith("Fruiting")) score += 1;
		m = time.matcher(text);
		if(m.matches()) score += 1;
		m = abb.matcher(text);
		if(m.find()) score +=1;
		m=meter.matcher(text);
		if(m.find()) score +=1;
		if(score >=2){
			return true;
		}
		return false;
	}

	/**
	 * this is the main method of the class
	 * @param paralist
	 * @return
	 * @throws Exception
	 */
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
					if(!((Element) contentlist.get(i+1)).getName().equals("synonym")){
						i++;
						nextele = (Element) contentlist.get(i);
					}else{
						nextele = (Element) contentlist.get(i+1) ; //do not forward the index for synonym
					}
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
					syntext = te.getText(); //e.g.may have N pubs: Crepis rhagadioloides Linnaeus, Syst. Nat. ed. 12, 2: 527. 1767; Mant. Pl., 108. 1767
					String[] indsyn = syntext.split(";"); //; separate N syns or N pubs of a syn
					
					/**/
					Element prvtaxid = null; //remember the last taxid element to be used with additional pubs
					for(int s=0; s<indsyn.length; s++)// for every synonym
					{
						Element pubname = new Element("place_of_publication");
						Element taxid= new Element("TaxonIdentification");
						taxid.setAttribute("Status","SYNONYM");
						indsyntext=indsyn[s];
						if(s!=0 && !indsyntext.matches("\\D*?,\\D*?,\\s*\\d.*") && indsyntext.matches(".*?\\d.*?") && !indsyntext.matches("\\D*?[a-z]\\s+\\d{4,4}\\W.*")){//this is the 2nd pub, e.g. Mant. Pl., 108. 1767
							if(indsyntext.trim().matches("^\\d.*")) { //this is the second place_in_publication, attach it to the last place_in_pub element
								taxid = prvtaxid;
								pubname = add2pubname(indsyntext);
							}else{
								taxid = prvtaxid;
								publicationMarkuper(pubpart, indsyntext, pubname);
							}
						}else{
							namepart="";
							pubpart="";
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
								String publication=indsyntext.substring(commaindex+1, indsyntext.length());
								publicationMarkuper(pubpart, publication, pubname); //populate pubname element
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
							prvtaxid = taxid;
						/*Iterator synit=synlist.iterator();
							while(synit.hasNext())
							{
								Element synpe = (Element)synit.next();
								taxid.addContent(synpe.detach());
							}*/
							//taxid.addContent(synlist);						
						}
						if(pubname.getContentSize()!=0 && pubname.getParentElement()==null) //to check if the publication has children.
						{
							taxid.addContent(pubname);
							//System.out.println();
						}
						if(taxid.getParentElement()==null) treatment.addContent(taxid); //don't add taxid when the taxid is only updated by adding a pub
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

	private Element add2pubname(String placeinpubtext) {
		try{
			List<Element> placeinpub = placeInPub.selectNodes(treatment);
			int size = placeinpub.size();
			for(int i = size-1; i>=0; i--){
				if(placeinpub.get(i).getTextTrim().length() > 0){
					String newtext = placeinpub.get(i).getTextTrim()+"; "+placeinpubtext;
					placeinpub.get(i).setText(newtext);
					return placeinpub.get(i).getParentElement();
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}

	private void publicationMarkuper(String pubpart, String publication,
			Element pubname) {
		String other="";
		//Code to process other_info
		//String publication=indsyntext.substring(commaindex+1, indsyntext.length()).trim();
		
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
			pubpart=(pubpart+publication).trim();
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
		
		
		
		
		if(publi !=null){
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
		// it was assumed that publication was what follows taxon_name, but when it turns out to be synonym... 
		/*else if(newpubli.getName()=="synonym")
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
							
							
							//process the publication part
							String publication=indsyntext.substring(commaindex+1, indsyntext.length());
							publicationMarkuper(pubpart, publication, pubname); //populate pubname element
						
							
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
		}*/
		else
		{
			treatment.addContent(taxid);
			if(Comtext.length()!=0)
			{
				Element comt= new Element("common_name");
				comt.setText(Comtext);
				treatment.addContent(comt);
			}
			if(!newpubli.getName().equals("synonym")) treatment.addContent(newpubli); //synonym will be transformed to additional markup
		}
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
			//hong
			text = text.replaceFirst("(?<=^[A-Z])\\.", ". "); //to insert a space after C.leptosepala 
			//
			String newtext= text;
			String spauth="";
			String varauth="";
			String[] var= text.split("\\s+");
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
		

		String file = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V26-good\\target\\last\\" + filename;
		//String file = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V23-good\\target\\last\\" + filename;

		//String file = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V19-good\\target\\last\\" + filename;
		//String file = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V20-good\\target\\last\\" + filename;
		//String file = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V21-good\\target\\last\\" + filename;
		//String file = "C:\\Documents and Settings\\Hong Updates\\Desktop\\trash\\test\\" + filename;
		
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V19-good\\target\\Last\\" + filename;
		//String file = "C:\\Users\\mohankrishna89\\Desktop\\Library Project\\FNA2\\V20-good\\target\\last\\" + filename;
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

