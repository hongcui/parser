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
 *      citation and year (e.g., 1897) should not be part of authority
 *      "not ..." and "based on ..." should not be part of authority [note: "authority [not xyz]" is fine, but "authority YEAR [not xyz]" is not: YEAR => <place_in_publication>, [not xyz] => <other_info>
 * 5. check publication_title:
 *      check for the inclusion of authority in <publication_title>: output all pub_title to excel, sort it, then review. (Smith & John would be suspicious in pub_title)
 *      check taxon ranks such as var. subg. sect. ser. subsp. subf. in pub_title
 * Use oXygen to check
 * 6. references: remove "item:" and "selected references:" in references
 * 7. review <habitat> elements
 * 8. make sure there is 0 <synonym> tag 
 * 9. check <global_distribution> for introduced (should be marked as <introduced>)     
 */
public class FNATaxonNameFinalizerStep2{
	static String filename = null;
	Element treatment = new Element("treatment");
	static int partdetecter, count;
	static int fnamedetecter=0;
	Pattern time = Pattern.compile("(year[ -]round|spring|summer|fall|winter|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\\b.*", Pattern.CASE_INSENSITIVE);
	Pattern abb = Pattern.compile("([A-Z][a-z]+\\.[;,]? ?){2,}");
	//Pattern meter = Pattern.compile("\\d+(?:[–-]\\d+)? m\\b");
	Pattern meter = Pattern.compile("[\\d–()-]+ m\\b");
	Pattern name = Pattern.compile("\\b[A-Z]\\. "); //should not contain species names C. sdfl
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
		
		
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V19-good\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V20-good\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V21-good\\namemapping.bin"));
		//outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V23-good\\namemapping.bin"));
		outputStream = new ObjectOutputStream(new FileOutputStream("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V26-good\\namemapping.bin"));

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
		
		//File extracted = new File("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V19-good\\target\\last");
		//File extracted = new File("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V20-good\\target\\last");
		//File extracted = new File("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V21-good\\target\\last");
		//File extracted = new File("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V23-good\\target\\last");
		File extracted = new File("C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V26-good\\target\\last");
			

		
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
			FNATaxonNameFinalizerStep2 transformer = new FNATaxonNameFinalizerStep2();	
			transformer.createtreatment(files[i]);
			//Then 4. fix ranks for some synonyms
			System.out.println(filename);//filename
			transformer.fixSynonymRank();
			transformer.output(filename);
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






		
	
	
	
	private void output(String filename) throws Exception {
		XMLOutputter outputter = new XMLOutputter();
		
		//String file = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V19-good\\target\\last\\" + filename;
		//String file = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V20-good\\target\\last\\" + filename;
		//String file = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V21-good\\target\\last\\" + filename;
		//String file = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V23-good\\target\\last\\" + filename;
		String file = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameCode\\V26-good\\target\\last\\" + filename;


		
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
		treatment.detach();
		Document doc = new Document(treatment);
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(file));
		outputter.output(doc, out);
	}
	private void createtreatment(File f) throws Exception{
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(f);
		treatment = doc.getRootElement();
		//treatment = root.getChild("treatment");
	}
		

}

