/**
 * 
 */
package fna.parsing;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.Hashtable;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * @author Hong Updates
 *
 */
public class FileName2TaxonTreatise extends FileName2Taxon {
	
	static	XPath namepath;
	static	XPath rankpath;
	static	XPath hierarchypath;
    static XPath descriptionpath;
	static{
		try{
			namepath = XPath.newInstance("//name");
			rankpath = XPath.newInstance("//rank");
			hierarchypath = XPath.newInstance("//taxon_hierarchy");
			descriptionpath = XPath.newInstance("//description");
		}catch(Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}

	/**
	 * @param inputfilepath
	 * @param database
	 * @param prefix
	 */
	public FileName2TaxonTreatise(String inputfilepath, String database,
			String prefix) {
		super(inputfilepath, database, prefix);
		// TODO Auto-generated constructor stub
	}
	
	public FileName2TaxonTreatise(String inputfilepath, Connection conn,
			String prefix) {
		super(inputfilepath, conn, prefix);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see fna.parsing.FileName2Taxon#populateFilename2TaxonTableUsing(java.io.File)
	 */
	@Override
	/**
	 * <nomenclature>
	 * 	<name>Lunolenus SDZUY</name>
	 * 	<rank>Genus</rank>
	 * 	<taxon_hierarchy>Class TRILOBITA Walch; Order REDLICHIIDA Richter; Suborder REDLICHIINA Richter; Superfamily REDLICHIOIDEA Poulsen; Family ABADIELLIDAE Hupé</taxon_hierarchy>
	 * 	<name_info>Lunolenus SDZUY, 1961, p. 549 [*L. lunae; OD; holo-type (SDZUY, 1961, pl. 7, fig. 9), L3110, UMU, Münster].</name_info>
	 * </nomenclature>
	 * use <name>, <rank>, and <taxon_hierarchy> to populate values hashtable
	 */
	protected void populateFilename2TaxonTableUsing(File xml) {
		try{
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(xml);
			Element root = doc.getRootElement();
			if(descriptionpath.selectNodes(root).size() > 0){
				values.put("hasdescription", "1");
			}else{
				values.put("hasdescription", "0");
			}
			values.put("filename", xml.getName());
			String aname = ((Element)namepath.selectSingleNode(root)).getTextNormalize().toLowerCase();
			if(aname.contains(" ")){
				String temp = aname.substring(0, aname.indexOf(" "));
				if(temp.matches("[a-z]\\.")){//get the next word : O. (Oncagnostus)
					aname = aname.substring(3).replaceAll("[()]", "").trim();
					if(aname.contains(" ")) aname = aname.substring(0, aname.indexOf(" "));
				}else if(values.get(temp) !=null){//is rank, get the next word
					aname = aname.substring(aname.indexOf(" ")).trim(); //skip rank
					if(aname.contains(" ")) aname = aname.substring(0, aname.indexOf(" "));
				}else if(aname.contains(" ")){
					aname = aname.substring(0, aname.indexOf(" "));
				}
			}
			
			String rank = ((Element)rankpath.selectSingleNode(root)).getTextNormalize().toLowerCase();
			values.put(rank, aname);
			
			String hierarchy = ((Element)hierarchypath.selectSingleNode(root)).getTextNormalize().toLowerCase();
			String[] names = hierarchy.split("\\s*;\\s*");
			for(String name: names){
				String rnk = name.substring(0, name.indexOf(" "));
				if(values.get(rnk)==null){
					rnk = "genus"; //rank Genus was not used in the original text, add it 
					name = name.replaceFirst(rnk, "").trim();
					if(name.contains(" ")) name = name.substring(0, name.indexOf(" "));
					values.put(rnk, name);
					break; //Genus is the lowest rank.
				}
				name = name.replaceFirst(rnk, "").trim();
				if(name.contains(" ")) name = name.substring(0, name.indexOf(" "));
				values.put(rnk, name);
			}			
			insertIntoFilename2TaxonTable();
			resetValues();
		}catch(Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}		
	}
		

	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String filepath = "C:\\Documents and Settings\\Hong Updates\\Desktop\\CharaParserWorkshop2012\\TreatisePartO\\source";
		String database = "markedupdatasets";
		String prefix = "treatise_o";
		FileName2TaxonTreatise fntf = new FileName2TaxonTreatise(filepath, database, prefix);
		fntf.createFilename2taxonTable();
		fntf.populateFilename2TaxonTable();
	}

}
