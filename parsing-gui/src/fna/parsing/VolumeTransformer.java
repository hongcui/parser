/**
 * $Id$
 */
package fna.parsing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Display;
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
	private static String usstates ="Ala\\.|Alabama|Alaska|Ariz\\.|Arizona|Ark\\.|Arkansas|Calif\\.|California|Colo\\.|Colorado|Conn\\.|Connecticut|Del\\.|Delaware|D\\.C\\.|District of Columbia|Fla\\.|Florida|Ga\\.|Georgia|Idaho|Ill\\.|Illinois|Ind\\.|Indiana|Iowa|Kans\\.|Kansas|Ky\\.|Kentucky|La\\.|Louisiana|Maine|Maryland|Md\\.|Massachusetts|Mass\\.|Michigan|Mich\\.|Minnesota|Minn\\.|Mississippi|Miss\\.|Missouri|Mo\\.|Montana|Mont\\.|Nebraska|Nebr\\.|Nevada|Nev\\.|New Hampshire|N\\.H\\.|New Jersey|N\\.J\\.|New Mexico|N\\.Mex\\.|New York|N\\.Y\\.|North Carolina|N\\.C\\.|North Dakota|N\\.Dak\\.|Ohio|Oklahoma|Okla\\.|Oregon|Oreg\\.|Pennsylvania|Pa\\.|Rhode Island|R\\.I\\.|South Carolina|S\\.C\\.|South Dakota|S\\.Dak\\.|Tennessee|Tenn\\.|Texas|Tex\\.|Utah|Vermont|Vt\\.|Virginia|Va\\.|Washington|Wash\\.|West Virginia|W\\.Va\\.|Wisconsin|Wis\\.|Wyoming|Wyo\\.";	
	private static String caprovinces="Alta\\.|Alberta|B\\.C\\.|British Columbia|Manitoba|Man\\.|New Brunswick|N\\.B\\.|Newfoundland and Labrador|Nfld\\. and Labr|Northwest Territories|N\\.W\\.T\\.|Nova Scotia|N\\.S\\.|Nunavut|Ontario|Ont\\.|Prince Edward Island|P\\.E\\.I\\.|Quebec|Que\\.|Saskatchewan|Sask\\.|Yukon";
	//private static Pattern p = Pattern.compile("(.*?\\d+–\\d+[\\.;]\\]?)(\\s+[A-Z]\\w+,.*)"); //splitting between a page range and first author name. Assuming references are separated by [;.]
	private static Pattern p = Pattern.compile("(.*?\\d+–\\d+[\\.;]\\]?)(\\s+[A-Z]\\w+.*)"); //splitting between a page range and first author name. Assuming references are separated by [;.]. Removed "," after author first name (chinese author name in Foc: Wu Te-lin,

	private Properties styleMappings;
	private TaxonIndexer ti;
	private ProcessListener listener;
	//private Hashtable errors;
	//TODO: put the following in a conf file. same for those in volumeExtractor.java
	//private String start = "^Heading.*"; //starts a treatment
	private String start = VolumeExtractor.getStart(); //starts a treatment
	private String names = ".*?(Syn|Name|syn|name).*"; //other interesting names worth parsing
	private String conservednamestatement ="(name conserved|nom. cons.)";
	private static final Logger LOGGER = Logger.getLogger(VolumeTransformer.class);
	private VolumeTransformerDbAccess vtDbA = null;	
	private Hashtable ranks;

	private String taxontable = null;
	private String authortable = null;
	private String publicationtable = null;
	private Connection conn = null;
	private String dataPrefix;
	private Display display;
	private String glosstable;
	private Hashtable<String, String> unparsed = new Hashtable<String, String>(); //support display of unparsed segments
	private ArrayList<String> nonames = new ArrayList<String>(); //support rearrange of parsed files when unparsed is added
	private ArrayList<String> parsed = new ArrayList<String>(); //support rearrange of parsed files when unparsed is added
	
	private boolean debug = false;
	private boolean debugref = false;
	private boolean debugkey = true;
	protected Hashtable<String, String> allNameTokens;
	private String lastgenusname = null;
	
	
	static XPath discussion;
	static XPath placeInPub;
	static XPath synPath;
	static XPath acceptPath;
	static XPath namePath;
	static XPath authorPath;
	private static String knownPublications = "Nouv\\. Arch\\. Mus\\. Hist\\. Nat\\.|Syst\\. Nat\\.";
	static{
		try{
			discussion = XPath.newInstance(".//discussion");
			placeInPub = XPath.newInstance(".//place_in_publication");
			synPath = XPath.newInstance(".//TaxonIdentification[@Status=\"SYNONYM\"]"); //TaxonIdentification Status="SYNONYM"
			acceptPath =  XPath.newInstance(".//TaxonIdentification[@Status=\"ACCEPTED\"]");
			namePath = XPath.newInstance(".//*[contains(name(),'_name')]");
			authorPath = XPath.newInstance(".//*[contains(name(),'_authority')]");
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}		
	}

	
	
	
	public VolumeTransformer(ProcessListener listener, String dataPrefix, String glosstable, Display display) throws ParsingException {
		this.listener = listener;
		this.dataPrefix = dataPrefix;
		this.display = display;
		this.glosstable = glosstable;
		//this.errors = new Hashtable();
		this.taxontable = dataPrefix.trim()+"_"	+ ApplicationUtilities.getProperty("taxontable");
		this.authortable = dataPrefix.trim() + "_" + ApplicationUtilities.getProperty("authortable");
		this.publicationtable = dataPrefix.trim() + "_" + ApplicationUtilities.getProperty("publicationtable");
		vtDbA = new VolumeTransformerDbAccess(dataPrefix);
		
		ti = TaxonIndexer.loadUpdated(Registry.ConfigurationDirectory);
		this.allNameTokens = ti.getAllNameTokens();
		if(ti.emptyNumbers() || ti.emptyNames()) ti = null;
		
		// load style mapping
		styleMappings = new Properties();
		try {
			styleMappings.load(new FileInputStream(
					Registry.ConfigurationDirectory
							+ "/style-mapping.properties"));
		} catch (IOException e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			throw new ParsingException(
					"Failed to load the style mapping file!", e);
		}
		Statement stmt = null;
		try{
			if(conn == null){
				String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
				stmt = conn.createStatement();
				stmt.execute("drop table if exists "+taxontable);
				stmt.execute("create table if not exists "+taxontable+" (taxonnumber varchar(10), name varchar(500), rank varchar(20), filenumber int)");
				stmt.execute("drop table if exists "+authortable);
				stmt.execute("create table if not exists "+ authortable+" (authority varchar(500) NOT NULL)");
				stmt.execute("drop table if exists "+publicationtable);
				stmt.execute("create table if not exists "+ publicationtable+" (publication varchar(500) NOT NULL)");				
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}finally{
			try{
				if(stmt!=null) stmt.close();
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
						System.getProperty("line.separator")
						+sw.toString());
			}
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
			//for (int count = 1010; count <= total; count++) {

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
						String sm= styleMappings.getProperty(style+"");//hong 6/26/08
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
					}else {
						String sm = styleMappings.getProperty(style);
						if(sm.contains("author")){ //change "author_of_XXXXX" or "authors" to author according to JSTOR standards
							sm = "author";
						}
						if(sm.contains("discussion")){//may be "references" as in FoC
							int tokencount = text.replaceAll("[a-z ]", "").length(); //count of capital letters, numbers, and punct marks
							int wordcount = text.split("\\s+").length;
							if(tokencount * 2 > wordcount && text.matches("^[A-Z]\\S*\\s+[A-Z].*")){ //first two words are capitalized. To avoid treating as references: "In general, the tribes recognized here and their delimitations follow Lewis, G. P. et al. (eds.). 2005.Legumes of the World. Richmond, U.K.: Royal Botanic Gardens, Kew."
								sm = "references";
							}
						}
						Element e = new Element(sm);
						if(text.trim().length()!=0)
						e.setText(text.trim());
						if(e.getContentSize()!=0)//to prevent empty tags
						treatment.addContent(e);
												
	
					}
				}
			
				//further mark up reference
				List<Element> elements = XPath.selectNodes(treatment, "./references");
				Iterator<Element> it = elements.iterator();
				while(it.hasNext()){
					Element ref = it.next();
					furtherMarkupReference(ref);
				}
				
				//further mark up keys <run_in_sidehead>
				//elements = XPath.selectNodes(treatment, "./key|./couplet");
				elements = XPath.selectNodes(treatment, "./key|./couplet");
				if(elements.size()>0){//contains key
					furtherMarkupKeys(treatment);
				}
				
				//fixSynonymRank(treatment);
				System.out.println(count + ".xml"); //Just to check what file is outputted
				// output the treatment to transformed
				File xml = new File(Registry.TargetDirectory,
						ApplicationUtilities.getProperty("TRANSFORMED") + "/" + count + ".xml");
				ParsingUtil.outputXML(treatment, xml ,null);
				//String error = (String)errors.get(count+"");
				//error = error ==null? "":error;
				
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
				
				//when output list is displayed in CharaParser, habitat sections have not 
				//been marked up yet.
				//listener.info(String.valueOf(count), xml.getPath(), ""); //for FNA, this is executed later in vf.replaceAnnotated.
				listener.progress((count*50) / total);
			}
			
			HabitatParser4FNA hpf = new HabitatParser4FNA(dataPrefix);
			hpf.parse();
			VolumeFinalizer vf = new VolumeFinalizer(listener,null, dataPrefix, this.conn, glosstable, display);//display output files to listener here.
			parsed = vf.replaceWithAnnotated(hpf, "/treatment/habitat", "TRANSFORMED", true);
			
			if(MainForm.conn == null){
				Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
				MainForm.conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
			}

			String transformeddir = Registry.TargetDirectory+"\\transformed\\";
			TaxonNameCollector tnc = new TaxonNameCollector(conn, transformeddir, dataPrefix+"_"+ApplicationUtilities.getProperty("TAXONNAMES"), dataPrefix);
			tnc.collect();
			
		} catch (Exception e) {
			//LOGGER.error("VolumeTransformer : transform - error in parsing", e);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			throw new ParsingException(e);
		}
		//update listener.info
		listener.clear();
		//unparsed
		Enumeration<String> keys = unparsed.keys();
		while(keys.hasMoreElements()){
			String key = keys.nextElement();
			String value = unparsed.get(key);
			key = key.substring(key.indexOf(" ")).trim();			
			listener.info(key, value);
		}
		//no names
		for(String value: nonames){
			listener.info("no taxon name found in:", value);
		}
		//parsed
		int count = 1;
		for(String value: parsed){
			listener.info(""+count, value);
			count++;
		}

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
	private void furtherMarkupKeys(Element treatment) {
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
	private void furtherMarkupKeyStatements(Element taxonkey) {
		ArrayList<Element> allstatements = new ArrayList<Element>();
		Element marked = new Element("key");
		List<Element> states = taxonkey.getChildren();
		Pattern p1 = Pattern.compile("(.*?)(( ### [\\d ]+[a-z]?\\.| ?#* ?Group +\\d).*)");//determ
		Pattern p2 = Pattern.compile("^([\\d ]+[a-z]?\\..*?) (.? ?[A-Z].*)");//id   2. "Ray” corollas
		String determ = null;
		String id = "";
		String broken = "";
		String preid = null;
		//process statements backwards
		for(int i = states.size()-1; i>=0; i--){
			Element state = states.get(i);
			if(state.getName().compareTo("key") == 0 || state.getName().compareTo("couplet") == 0){
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
					broken = text;
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
	private void assembleKeys(Element treatment) {
		Element key = null;
		//removing individual statements from treatment and putting them in key
		List<Element> children = treatment.getChildren();////changes to treatment children affect elements too.
		Element[] elements = children.toArray(new Element[0]); //take a snapshot
		ArrayList<Element> detacheds = new ArrayList<Element>();
		boolean foundkey = false;
		for(int i = 0; i < elements.length; i++){
			Element e = elements[i];
			if(e.getName().compareTo("run_in_sidehead")==0 && ((e.getTextTrim().startsWith("Key to ")||(e.getTextTrim().startsWith("Key Based "))) || e.getTextTrim().matches("Group \\d+.*"))){
				foundkey = true;
				if(key!=null){
					treatment.addContent((Element)key.clone());	
				}
				key = new Element("TaxonKey");
				
			}
			if(!foundkey && (e.getName().compareTo("key")==0 || e.getName().compareTo("couplet")==0)){
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
		//String text = ref.getText();
		String text = ref.getText().replaceAll("^(\\s*SELECTED\\s*REFERENCE(S)?\\s*)", ""); //To remove selected references from the sentence.
		ref.setText("");
		if(this.debugref) System.out.println("\nReferences text:"+text);
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
		refitem.setText(text);//item not required
		ref.addContent(refitem);
		if(this.debugref) System.out.println("a ref:"+text);
		//ref.getParentElement().addContent(marked);
		//ref.detach();	
	}


	private String getChildText(Element pe, String string) throws Exception{
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
					//DistributionParser4FNA dp = new DistributionParser4FNA(treatment, m.group(3), "general_distribution");
					DistributionParser4FNA dp = new DistributionParser4FNA(treatment, m.group(3), "global_distribution");  //general_distribution -> global_distribution
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
					//FloweringTimeParser4FNA dp = new FloweringTimeParser4FNA(treatment, mh.group(1), "flowering_time");
					FloweringTimeParser4FNA dp = new FloweringTimeParser4FNA(treatment, mh.group(1), "phenology"); //flowering_time ---> phenology according to JSTOR
					treatment = dp.parse(); 
					//System.out.println("flowering_time:"+mh.group(1));
				}
				if(mh.group(2)!= null){
					String[] habitatchunk = mh.group(2).trim().replaceAll(";$", "").split(",");
					for(int l=0;l<habitatchunk.length;l++){
						addElement("habitat",habitatchunk[l].trim(), treatment);
					}
					
					//System.out.println("habitat:"+mh.group(2));
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
				LOGGER.error("VolumeTransformer.parseDistriTag: "+"distribution not match: "+text);
			}						
		}
	}
	
	
	
	private String parseNameTag(int index, String namerank, String line,
			Element treatment) {
		if(line == null || line.equals("")){
			return ""; //TODO: should not happen. but did happen with v. 19 295.xml==>VolumeExtractor JDOM problem.
		}
		
		String name = ti.getName(index);
 

		if(name==null ||name.compareTo("") == 0){
			File xml = new File(Registry.TargetDirectory,
					ApplicationUtilities.getProperty("TRANSFORMED") + "/" + (index+1) + ".xml");
			//listener.info("no name found in: ", xml.getPath());
			nonames.add(xml.getPath());
			//errors.put((index+1)+"","no name found in: "+line);
			return "";
		}
		// make a copy of the line and will work on the new copy
		String text = new String(line);
		text = text.replaceAll(" ", " ").replaceAll("\\s+", " ").trim(); //there are some whitespaces that are not really a space, don't know what they are. 
		if(debug) System.out.println("\n"+(index+1)+": text="+text);
		
		String number = null;
		if(ti != null)
			number = ti.getNumber(index);
		else{
			number = line.substring(0, line.indexOf('.'));
		}
		// number
		addElement("number", number, treatment); // TODO: add the number tag
		                                         // to the sytle mapping

		//text = text.substring(number.length() + 1); //Hong 08/04/09 change to
		text = VolumeVerifier.fixBrokenNames(text);
		text = text.replaceFirst("^.*?(?=[A-Z])", "").trim();;
		
		
		//Code to add name to taxon ID mohan 1/18/2013
		Element taxonid = new Element("TaxonIdentification");
		taxonid.setAttribute("Status","ACCEPTED");
		
		//End code
		//specificNameRank may need be adjusted depending on the style used in the orginal doc
		//namerank = specificNameRank(namerank, text);	
		//name = fixBrokenName(text, namerank);
		namerank = specificNameRank(namerank+"", name);	
		name = fixBrokenName(name, namerank);
		if(debug) System.out.println("namerank:"+namerank);
		System.out.println("namerank:"+namerank);
		String[] nameinfo = getNameAuthority(name);//genus and above: authority parsed by getNameAuthority: 0: name 1:authority
		if(nameinfo[0]!=null && nameinfo[1]!=null){
			//addElement(namerank, nameinfo[0], treatment);
			//addElement(namerank, nameinfo[0], taxonid); //add to taxonid
			try {
				vtDbA.add2TaxonTable(number, name, namerank, index+1);
			} catch (ParsingException e) {
				// TODO Auto-generated catch block
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
				LOGGER.error("Couldn't perform parsing in VolumeTransformer:parseNameTag", e);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
				LOGGER.error("Database access error in VolumeTransformer:parseNameTag", e);
			}
			if(debug) System.out.println("name:"+nameinfo[0]);
			if(nameinfo[1].length()>0){
				//addElement("authority", nameinfo[1], treatment);
				//addElement("authority", nameinfo[1], taxonid);
				try {
					vtDbA.add2AuthorTable(nameinfo[1]);
				} catch (ParsingException e) {
					// TODO Auto-generated catch block
					StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
					LOGGER.error("Couldn't perform parsing in VolumeTransformer:parseNameTag", e);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
					LOGGER.error("Database access error in VolumeTransformer:parseNameTag", e);
				}
				if(debug) System.out.println("authority:"+nameinfo[1]);
			}
			parseName(name, namerank, taxonid);
			text = text.replaceFirst("^\\s*.{"+name.length()+"}","").trim();
		}
		//authority
		/*Pattern p = Pattern.compile("(.*?)((?: in|,|Â·|\\?).*)");
		Matcher m = p.matcher(text);
		if(m.matches()){
			if(m.group(1).trim().compareTo("")!= 0){
				addElement("authority", m.group(1).trim(), treatment);
				try {
					vtDbA.add2AuthorTable(m.group(1).trim());
				} catch (ParsingException e) {
					// TODO Auto-generated catch block
					StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
					LOGGER.error("Couldn't perform parsing in VolumeTransformer:parseNameTag", e);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
					LOGGER.error("Database access error in VolumeTransformer:parseNameTag", e);
				}
				//System.out.println("authority:"+m.group(1).trim());
			}
			text = m.group(2).trim();
		}*/
		//save the segment after ?or ?for later
		/*String ending = "";
		int pos = text.lastIndexOf('.');
		if(pos < 0){
			pos = text.lastIndexOf('?');
		}
		if (pos != -1) {
			ending = text.substring(pos + 1).trim();
			text = text.substring(0, pos+1);
		}*/
		
		
		
		
		//derivation: deal with this first to remove [] and avoid pub-year match in [] but added to treatment after name
		//Pattern p = Pattern.compile("(.*?)(\\[.*?\\]$)");//was matching the outer most paranthesis but we want to match only the last pair
		Pattern p = Pattern.compile("(.*?)(\\[([^\\]]+)\\]\\.?$)");
		Matcher m = p.matcher(text);
		String etymology="";
		if(m.matches()){
			/*if(m.group(2).trim().compareTo("")!= 0){
				addElement("etymology", m.group(2).trim(), treatment);
				if(debug) System.out.println("etymology:"+m.group(2).trim());
			}*/
			etymology = m.group(2).trim();
			text = m.group(1).trim();
		}
		
		//place of publication 
		//Pattern p = Pattern.compile("(.* [12]\\d\\d\\d|.*(?=Â·)|.*(?=.))(.*)"); //TODO: a better fix is needed Brittonia 28: 427, fig. 1.  1977   ?  Yellow spinecape [For George Jones Goodman, 1904-1999
		p = Pattern.compile("(.*[ –][12]\\d\\d\\d)($|,|\\.| +)(.*)"); //TODO: a better fix is needed Brittonia 28: 427, fig. 1.  1977   ?  Yellow spinecape [For George Jones Goodman, 1904-1999
		m = p.matcher(text);
		if(m.matches()){
			String pp = m.group(1).replaceFirst("^\\s*[,\\.]", "").trim();			
			//extractPublicationPlace(treatment, pp); //pp may be "Sp. Pl. 1: 480.  1753; Gen. Pl. ed. 5, 215.  1754"
			extractPublicationPlace(taxonid, pp); //pp may be "Sp. Pl. 1: 480.  1753; Gen. Pl. ed. 5, 215.  1754"
			text = m.group(3).trim();
		}

		treatment.addContent(taxonid);
		//Adding the etymology
		if(etymology.compareTo("")!= 0){
			addElement("etymology", etymology, treatment);
			if(debug) System.out.println("etymology:"+etymology);
		}
		
		// conserved
		String conserved="name conserved";
		int	pos = text.indexOf(conserved);
		if(pos < 0){
			conserved="name proposed for conservation";
			pos = text.indexOf(conserved);
		}
		if(pos < 0){
			conserved="nom. cons.";
			pos = text.indexOf(conserved);
		}
		if (pos != -1) {
			//String conserved = text.substring(pos).trim();
			text = text.replace(conserved, "").trim();
			//conserved = conserved.replaceFirst("^\\s*[,;\\.]", "");
			//addElement("conserved", conserved, treatment);
			addElement("conserved_name", conserved, treatment);//conserved --> conserved_name according to JSTOR schema
			if(debug) System.out.println("conserved:"+conserved);
			
			// trim the text
			//int p1 = text.lastIndexOf(',', pos);
			//text = text.substring(0, p1);
		}

		//past_name
		p = Pattern.compile("\\((?:as )?(.*?)\\)(.*)");
		m = p.matcher(text);
		if(m.matches()){
			if(m.group(1).trim().compareTo("")!= 0){
				addElement("past_name", m.group(1).trim(), treatment);
				if(debug) System.out.println("past_name:"+m.group(1).trim());
			}
			text = m.group(2).trim();
		}

		//common name
		p = Pattern.compile("(.*?)[•·](.*?)(\\[.*|$)");
		m = p.matcher(text);
		if(m.matches()){
			if(m.group(2).trim().compareTo("")!= 0){
				String[] commonnames = m.group(2).trim().split("\\s*,\\s*");
				for(String cname: commonnames){
					addElement("common_name", cname, treatment);
					if(debug) System.out.println("common_name:"+cname);
				}
			}
			text = (m.group(1)+" "+m.group(3)).trim();
		}

		// format mark, common name, derivation
		/*{
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
		}*/
		

		if(text.trim().matches(".*?\\w+.*")){
			if(debug) System.out.println((index+1)+"unparsed: "+text);
			//addElement("unparsed", text, treatment);
			if(text.startsWith("[") && (text.endsWith("]") || text.endsWith("].") || text.endsWith("],"))){
				addElement("etymology", text, treatment);
			}else if(text.matches(".*?("+VolumeTransformer.knownPublications+").*") && taxonid !=null){
				extractPublicationPlace(taxonid, text);
			}else{
				addElement("other_info", text, treatment);//unparsed -->other_info according to schema for JSTOR
			
				File xml = new File(Registry.TargetDirectory,
					ApplicationUtilities.getProperty("TRANSFORMED") + "/" + (index+1) + ".xml");
				//listener.info("unparsed: "+text, xml.getPath());
				unparsed.put((index+1)+" tagged as 'other_info': "+text, xml.getPath());
			}
		}
		return namerank.replace("_name", "");
	}

	


	protected String fixBrokenName(String text, String namerank) {
		// TODO Auto-generated method stub
		return text;
	}


	/**
	 * family, genus, and species names seem to have seperate style, 
	 * others often have shared style, 
	 * use rank abbr. in the text to get the specific rank for a name
	 * 
	 * @param namerank
	 * @param text
	 * @return
	 */
	private String specificNameRank(String namerank, String text) {
		//namerank and name
		//(subfam|var|subgen|subg|subsp|ser|tribe|subsect)
		if(namerank.contains("species_subspecies_variety_name") || namerank.contains("infraspecific_name")){ //fna || foc
			if(text.indexOf("var.") >=0){
				namerank = "variety_name";
			}else if(text.indexOf("subsp.") >=0){
				namerank = "subspecies_name";
			}else if(text.indexOf("ser.") >=0 && text.indexOf(",") > text.indexOf("ser.")){ //after "," is publication where ser. may appear.
				namerank = "series_name";
			}else if(text.indexOf("sect.") >=0){
				namerank = "section_name";
			}else if(text.indexOf("subsect.") >=0){
				namerank = "subsection_name";
			}else {
				namerank = "species_name";
			}
		}
		if(namerank.indexOf("subfamily_tribe_name")>=0){ //foc
			if(text.toLowerCase().indexOf("tribe") >=0){
				namerank = "tribe_name";
			}else if(text.toLowerCase().indexOf("subfam") >=0){
				namerank = "subfamily_name";
			}
		}	
		if(namerank.indexOf("section_subgenus_name")>=0){//foc
			if(text.toLowerCase().indexOf("sect.") >=0){
				namerank = "section_name";
			}else if(text.toLowerCase().indexOf("subg.") >=0){
				namerank = "subgenus_name";
			}
		}
		return namerank;
	}


	/**
	 * family, genus, species has authority
	 * lower ranked taxon have authorities in names themselves
	 * 
	 * Cactaceae Jussieu subfam. O puntioideae Burnett
	 * @param name
	 * @return array of two elements: 0: name 1:authority
	 */
	private String[] getNameAuthority(String name) {
		String[] nameinfo = new String[2];
		if(name.toLowerCase().matches(".*?\\b(subfam|var|subgen|subg|subsp|ser|tribe|sect|subsect)\\b.*")){
			nameinfo[0] = name;
			nameinfo[1] = "";
			return nameinfo;
		}
		//family
		Pattern p = Pattern.compile("^([a-z]*?ceae)(\\b.*)", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(name);
		if(m.matches()){
			String nm = m.group(1).replaceAll("\\s", "").trim(); //in case an extra space is there
			String auth = m.group(2).trim();
			if(auth.startsWith("(") && auth.endsWith(")")){
				nm +=" "+ auth;
				auth = "";
			}
			//nameinfo[0] = m.group(1).replaceAll("\\s", "").trim(); //in case an extra space is there
			//nameinfo[1] = m.group(2).trim();
			nameinfo[0] = nm;
			nameinfo[1] = auth;
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


	private void extractPublicationPlace(Element treatment, String pp) {
		pp = pp.replaceFirst("^\\s*,", "").trim();
		String pub="";
		String pip="";
		String[] pps = pp.split(";");
		for(String apub: pps){
			String place_in_publication="(.*?)(\\d.*?)";
			Matcher pubm=Pattern.compile(place_in_publication).matcher(apub);
			if(pubm.matches()){
				pub=pubm.group(1).trim();
				pip=pubm.group(2).trim();
			}
						
			Element placeOfPub=new Element("place_of_publication");
			addElement("publication_title",pub,placeOfPub);
			addElement("place_in_publication",pip,placeOfPub);
			treatment.addContent(placeOfPub);
			if(debug) System.out.println("publication_title:"+pub);
			if(debug) System.out.println("place_in_publication:"+pip);
			
			try {
				vtDbA.add2PublicationTable(pub);
			} catch (ParsingException e) {
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
				LOGGER.error("Couldn't perform parsing in VolumeTransformer:publicationPlace", e);
			} catch (SQLException e) {
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
				LOGGER.error("Database access error in VolumeTransformer:publicationPlace", e);
			}
		}
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
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			//LOGGER.error("Failed to output text file in VolumeTransformer:outputDescriptionText", e);
			throw new ParsingException("Failed to output text file.", e);
		}
	}
	
	protected void parseName(String name, String namerank, Element taxid){
		String text = name;
		if(namerank.equals("subgenus_name")&& name.matches(".*?\\b[Ss]ect\\..*")){ //section wrongly marked as subgenus in word style(foc)
			namerank = "section_name";
		}
		if(namerank.equals("subgenus_name")&& !name.matches(".*?\\b[Ss]ubg\\..*")){ //genus wrongly marked as subgenus in word style (fna)
			namerank = "genus_name";
		}
		
		if(namerank.equals("family_name")) 
		{
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
			
		}
		
		else if(namerank.equals("subfamily_name"))// SUBFAMILY
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
				if(family[k].matches(".*?\\b[Ss]ubfam\\..*"))
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
		}else if(namerank.equals("tribe_name"))// tribe for FoC, differs from FNA, tribe names do not have a family name prefixing them
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
				if(family[k].matches(".*?\\b[Tt]ribe\\b.*"))
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
			Element subfm= new Element("tribe_name");
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
			Element subfamat= new Element("tribe_authority");
			subfamat.setText(subfamauth);
			taxid.addContent(subfamat);	
			}
		}else if(namerank.equals("genus_name")){
			int k;
			int unrank=0;
			String newtext= text;
			String spauth="";
			String sectauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			this.lastgenusname = var[0]; //save this for the later processing of lower ranks e.g. sect where genus name is not explicit mentioned.
			newele.setText(var[0]);
			taxid.addContent(newele);
			for(k=1;k<var.length;k++)
			{
				if(!var[k].matches("\\[unranked\\]"))
					spauth+=var[k]+" ";
				else
				{
					unrank=1;
					System.out.println("Unranked element in name");
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

		}else if(namerank.equals("subgenus_name")){
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
				if(var[k].matches(".*?\\b[Ss]ubg\\..*"))
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
		}else if(namerank.equals("section_name")){
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
				if(var[k].matches(".*?\\b[Ss]ubg\\..*")||var[k].matches(".*?\\b[Ss]ect\\..*"))
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
			if(var[k].matches(".*?\\b[Ss]ubg\\..*"))
			{
			k++;
			Element subfm= new Element("subgenus_name");
			subfm.setText(var[k].replaceFirst("(\\W)$", ""));
			taxid.addContent(subfm);
			k++;
			while((k<var.length)&&!var[k].matches(".*?\\b[Ss]ect\\..*"))
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
			if(var[k].matches(".*?\\b[Ss]ect\\..*"))
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
		}else if(namerank.equals("subsection_name")){
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
				if(var[k].matches(".*?\\b[Ss]ubsect\\..*"))
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
		}else if(namerank.equals("series_name")){ //after "," is publication where ser. may appear.
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
				if(var[k].matches(".*?\\b[Ss]ubg\\..*")||var[k].matches(".*?\\b[Ss]ect\\..*")||var[k].matches(".*?\\b[Ss]er\\..*"))
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
			if(var[k].matches(".*?\\b[Ss]ubg\\..*"))
			{
			k++;
			Element subfm= new Element("subgenus_name");
			subfm.setText(var[k].replaceFirst("(\\W)$", ""));
			taxid.addContent(subfm);
			k++;
			while((k<var.length)&&!var[k].matches(".*?\\b[Ss]ect\\..*"))
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
			if(var[k].matches(".*?\\b[Ss]ect\\..*"))
			{
				k++;
				Element sect= new Element("section_name");
				sect.setText(var[k]);
				taxid.addContent(sect);
				k++;
				while(k<var.length&&!var[k].matches(".*?\\b[Ss]er\\..*"))
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
				if(var[k].matches(".*?\\b[Ss]er\\..*"))
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
		}else if(namerank.equals("species_name")){
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
		}else if(namerank.equals("subspecies_name")){
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
				if(var[k].matches(".*?\\b[Ss]ubsp\\..*"))
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
		

		else if(namerank.equals("variety_name")){
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
			taxid.addContent(newele);
			//if(!var[1].contains("var."))
			//{
			Element spele= new Element("species_name");
			spele.setText(var[1]);
			taxid.addContent(spele);
			//}
			for(k=2;k<var.length;k++)
			{
				if(var[k].matches(".*?\\b[Vv]ar\\..*"))
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
		
	}
	
	
	
	private String cleantext(String namepart){
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
				System.out.println("Could be a Problem in cleantext");
				inter=inter+" "+newchunks[m];
			}
			else
			{
				inter=inter+" "+newchunks[m];
			}
		}
		result=inter.trim();
		return result;
	}
	
	private Element synprocess(String namepart){
		
		Element syn=new Element("synonym");
		String text=cleantext(namepart);
		if(text.matches("[\\x{4e00}-\\x{9fa5}].*?")){
			Element newele= new Element("family_name");
			newele.setText(text);
			syn.addContent(newele);
		}else if(text.matches(".*?\\b[sS]ubtribe\\b.*"))
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
				if(var[k].matches("\\s*\\(tribe\\s*")||var[k].matches(".*?\\b[sS]ubtribe\\b.*"))
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
			while((k<var.length)&&!var[k].matches(".*?\\b[sS]ubtribe\\b.*"))
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
			if(var[k].matches(".*?\\b[sS]ubtribe\\b.*"))
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
		else if(text.matches(".*?\\b[tT]ribe\\b.*"))
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
				if(var[k].matches(".*?\\b[Tt]ribe\\b.*") || var[k].matches(".*?\\b[sS]ubfam\\..*"))
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
			if(var[k].matches(".*?\\b[sS]ubfam\\..*"))
			{
			k++;
			Element subfm= new Element("subfamily_name");
			subfm.setText(var[k].replaceFirst("(\\W)$", ""));
			syn.addContent(subfm);
			k++;
			while((k<var.length)&&!var[k].matches(".*?\\b[tT]ribe\\b.*"))
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
			if(var[k].matches(".*?\\b[tT]ribe\\b.*"))
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
		else if(text.matches(".*?\\b[sS]ubfam\\..*"))// SUBFAMILY
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
				if(family[k].matches(".*?\\b[sS]ubfam\\..*"))
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

		else if(text.matches(".*?\\b[Vv]ar\\..*"))
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
				if(var[k].matches(".*?\\b[Vv]ar\\..*"))
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
		else if(text.matches(".*?\\b[Ss]ubsp\\..*"))
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
				if(var[k].matches(".*?\\b[Ss]ubsp\\..*"))
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
		
		else if(text.matches(".*?\\b[Ss]er\\..*"))//SERIES NAME
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
				if(var[k].matches(".*?\\b[Ss]ubg\\..*")||var[k].matches(".*?\\b[Ss]ect\\..*")||var[k].matches(".*?\\b[Ss]er\\..*"))
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
			if(var[k].matches(".*?\\b[Ss]ubg\\..*"))
			{
			k++;
			Element subfm= new Element("subgenus_name");
			subfm.setText(var[k].replaceFirst("(\\W)$", ""));
			syn.addContent(subfm);
			k++;
			while((k<var.length)&&!var[k].matches(".*?\\b[Ss]ect\\..*"))
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
			if(var[k].matches(".*?\\b[Ss]ect\\..*"))
			{
				k++;
				Element sect= new Element("section_name");
				sect.setText(var[k]);
				syn.addContent(sect);
				k++;
				while(k<var.length&&!var[k].matches(".*?\\b[Ss]er\\..*"))
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
				if(var[k].matches(".*?\\b[Ss]er\\..*"))
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
		else if(text.matches(".*?\\b[Ss]ubsect\\..*"))
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
				if(var[k].matches(".*?\\b[Ss]ubsect\\..*"))
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
		else if(text.matches(".*?\\b[Ss]ect\\..*"))
		{
			int k;
			String newtext= text;
			String spauth="";
			String sectauth="";
			String subgauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			if(var[0].matches(".*?\\b[Ss]ect\\..*")){ //in FOC, "6. Sect. Salix", no genus name before "sect."
			    newele.setText(this.lastgenusname!=null? this.lastgenusname:ApplicationUtilities.getProperty("GenusName.PlaceHolder"));	
			    k = 0;
			}else{
				newele.setText(var[0]);
				k = 1;
			}
			syn.addContent(newele);
			//for(k=1;k<var.length;k++)
			for(;k<var.length;k++)
			{
				if(var[k].matches(".*?\\b[Ss]ubg\\..*")||var[k].matches(".*?\\b[Ss]ect\\..*"))
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
			if(var[k].matches(".*?\\b[Ss]ubg\\..*"))
			{
			k++;
			Element subfm= new Element("subgenus_name");
			subfm.setText(var[k].replaceFirst("(\\W)$", ""));
			syn.addContent(subfm);
			k++;
			while((k<var.length)&&!var[k].matches(".*?\\b[Ss]ect\\..*"))
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
			if(var[k].matches(".*?\\b[Ss]ect\\..*"))
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
		else if(text.matches(".*?\\b[Ss]ubg\\..*"))
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
				if(var[k].matches(".*?\\b[Ss]ubg\\..*"))
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
				this.lastgenusname = var[0];
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

			else  //lump it in fam, and correct it later in synrank
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

			
			
		}
	
		return syn;
	}


	//private void parseSynTag(String tag, String text, Element treatment){
		/*Element e = treatment.getChild("variety_name");
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
		
		addElement(tag, text, treatment);*/
		//System.out.println(tag+":"+text);
		
		
		
		//Mohan's Code
		private void parseSynTag(String tag, String text, Element treatment){
			String namepart="";
			String pubpart="";
			String indsyntext="";
			//String syntext = text; //e.g.may have N pubs: Crepis rhagadioloides Linnaeus, Syst. Nat. ed. 12, 2: 527. 1767; Mant. Pl., 108. 1767
			String syntext = VolumeVerifier.fixBrokenNames(text); //e.g.may have N pubs: Crepis rhagadioloides Linnaeus, Syst. Nat. ed. 12, 2: 527. 1767; Mant. Pl., 108. 1767
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
						pubname = add2pubname(indsyntext,treatment);
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
			
			/*Element disc=(Element) te.clone();
			treatment.addContent(disc);*/
		}
			
			fixSynonymRank(treatment);	
	}

	
	private Element add2pubname(String placeinpubtext, Element treatment) {
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
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
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
		
		
		String publtitl=titlechunks.length>=1? titlechunks[0]: "";
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
		int inlength= titlechunks.length>=1? titlechunks[0].length() : 0;
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
	private void fixSynonymRank(Element treatment) {
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
					if(!oldname.contains(rank)){
						names.get(0).setName(oldname.replaceFirst("^[^_]*", rank));
						//replace the rank for authority
						Element authority = (Element) authorPath.selectSingleNode(synonym);
						String oldauthname = authority.getName();					
						authority.setName(oldauthname.replaceFirst("^[^_]*", rank));
						System.out.println("rank replaced from "+oldname +" to "+rank);
					}
				}				
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
		
	}

}
