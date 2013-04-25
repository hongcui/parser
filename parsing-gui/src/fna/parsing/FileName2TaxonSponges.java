package fna.parsing;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

public class FileName2TaxonSponges extends FileName2Taxon  {

	static  String taxon_hierarchystr ="";
	static  String nomenclature_namestr ="";
	static  String nomenclature_rankstr ="";
	static  String domainstr ="";
	static  String kingdomstr ="";
	static  String phylumstr ="";
	static  String subphylumstr ="";
	static  String superdivisionstr ="";
	static  String divisionstr ="";
	static  String subdivisionstr ="";
	static  String superclassstr ="";
	static  String classstr ="";
	static  String subclassstr ="";
	static  String superorderstr ="";
	static  String orderstr ="";
	static  String suborderstr ="";
	static  String superfamilystr ="";
	static	String familystr ="";
	static	String subfamilystr="";
	static	String tribestr="";
	static	String subtribestr="";
	static	String genusstr="";
	static	String subgenusstr="";
	static	String sectionstr="";
	static	String subsectionstr="";
	static	String speciesstr="";
	static	String subspeciesstr="";
	static	String varietystr="";
    
	static  XPath taxon_hierarchypath;
	static  XPath nomenclature_namepath;
	static  XPath nomenclature_rankpath;
	static	XPath familypath;
	static	XPath subfamilypath;
	static	XPath tribepath;
	static	XPath subtribepath;
	static	XPath genuspath;
	static	XPath subgenuspath;
	static	XPath sectionpath;
	static	XPath subsectionpath;
	static	XPath speciespath;
	static	XPath subspeciespath;
	static	XPath varietypath;
    static XPath descriptionpath;
	static{
		try{
			taxon_hierarchypath = XPath.newInstance("//taxon_hierarchy");
			nomenclature_namepath = XPath.newInstance("//nomenclature/name");
			nomenclature_rankpath = XPath.newInstance("//nomenclature/rank");
			familypath = XPath.newInstance("//TaxonIdentification[@Status='ACCEPTED']/family_name");
			subfamilypath = XPath.newInstance("//TaxonIdentification[@Status='ACCEPTED']/subfamily_name");
			tribepath = XPath.newInstance("//TaxonIdentification[@Status='ACCEPTED']/tribe_name");
			subtribepath = XPath.newInstance("//TaxonIdentification[@Status='ACCEPTED']/subtribe_name");
			genuspath = XPath.newInstance("//TaxonIdentification[@Status='ACCEPTED']/genus_name");
			subgenuspath = XPath.newInstance("//TaxonIdentification[@Status='ACCEPTED']/subgenus_name");
			sectionpath = XPath.newInstance("//TaxonIdentification[@Status='ACCEPTED']/section_name");
			subsectionpath = XPath.newInstance("//TaxonIdentification[@Status='ACCEPTED']/subsection_name");
			speciespath = XPath.newInstance("//TaxonIdentification[@Status='ACCEPTED']/species_name");
			subspeciespath = XPath.newInstance("//TaxonIdentification[@Status='ACCEPTED']/subspecies_name");
			varietypath = XPath.newInstance("//TaxonIdentification[@Status='ACCEPTED']/variety_name");
			descriptionpath = XPath.newInstance("//description");
		}catch(Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}
	
	/**
	 * 
	 */
	public FileName2TaxonSponges(String inputfilepath, String database, String prefix) {
		super(inputfilepath, database, prefix);
	}
	
	/**
	 * 
	 */
	public FileName2TaxonSponges(String inputfilepath, Connection conn, String prefix) {
		super(inputfilepath, conn, prefix);
	}
		
	protected void populateFilename2TaxonTableUsing(File xml) {
		try{
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(xml);
			Element root = doc.getRootElement();
			Element taxon_hierarchy = (Element)taxon_hierarchypath.selectSingleNode(root);
			taxon_hierarchystr = taxon_hierarchy.getTextNormalize().toLowerCase();
		
			if(descriptionpath.selectNodes(root).size() > 0){
				values.put("hasdescription", "1");
			}else{
				values.put("hasdescription", "0");
			}
			values.put("filename", xml.getName());
			
			int startIdx = 0;
			int endIdx = 0;			
			if (taxon_hierarchystr.indexOf("domain")>=0){
				startIdx = taxon_hierarchystr.indexOf("domain")+("domain").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				domainstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("domain",domainstr);
			
			if (taxon_hierarchystr.indexOf("kingdom")>=0){
				startIdx = taxon_hierarchystr.indexOf("kingdom")+("kingdom").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				kingdomstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("kingdom",kingdomstr);
			
			if (taxon_hierarchystr.indexOf("phylum")>=0){
				startIdx = taxon_hierarchystr.indexOf("phylum")+("phylum").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				phylumstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("phylum",phylumstr);
			
			if (taxon_hierarchystr.indexOf("subphylum")>=0){
				startIdx = taxon_hierarchystr.indexOf("subphylum")+("subphylum").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				subphylumstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("subphylum",subphylumstr);
			
			if (taxon_hierarchystr.indexOf("superdivision")>=0){
				startIdx = taxon_hierarchystr.indexOf("superdivision")+("superdivision").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				superdivisionstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("superdivision",superdivisionstr);
			
			if (taxon_hierarchystr.indexOf("division")>=0){
				startIdx = taxon_hierarchystr.indexOf("division")+("division").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				divisionstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("division",divisionstr);
			
			if (taxon_hierarchystr.indexOf("subdivision")>=0){
				startIdx = taxon_hierarchystr.indexOf("subdivision")+("subdivision").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				subdivisionstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("subdivision",subdivisionstr);
			
			if (taxon_hierarchystr.indexOf("superclass")>=0){
				startIdx = taxon_hierarchystr.indexOf("superclass")+("superclass").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				superclassstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("superclass",superclassstr);
			
			if (taxon_hierarchystr.indexOf("class")>=0){
				startIdx = taxon_hierarchystr.indexOf("class")+("class").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				classstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("class",classstr);
			
			if (taxon_hierarchystr.indexOf("subclass")>=0){
				startIdx = taxon_hierarchystr.indexOf("subclass")+("subclass").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				subclassstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("subclass",subclassstr);			
			
			if (taxon_hierarchystr.indexOf("superorder")>=0){
				startIdx = taxon_hierarchystr.indexOf("superorder")+("superorder").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				superorderstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("superorder",superorderstr);
			
			if (taxon_hierarchystr.indexOf("order")>=0){
				startIdx = taxon_hierarchystr.indexOf("order")+("order").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				orderstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("order",orderstr);
			
			if (taxon_hierarchystr.indexOf("suborder")>=0){
				startIdx = taxon_hierarchystr.indexOf("suborder")+("suborder").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				suborderstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("suborder",suborderstr);
			
			if (taxon_hierarchystr.indexOf("superfamily")>=0){
				startIdx = taxon_hierarchystr.indexOf("superfamily")+("superfamily").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				superfamilystr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("superfamily",superfamilystr);
			
			if (taxon_hierarchystr.indexOf("family")>=0){
				startIdx = taxon_hierarchystr.indexOf("family")+("family").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				familystr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("family",familystr);
			
			if (taxon_hierarchystr.indexOf("subfamily")>=0){
				startIdx = taxon_hierarchystr.indexOf("subfamily")+("subfamily").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				subfamilystr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("subfamily",subfamilystr);
			
			if (taxon_hierarchystr.indexOf("tribe")>=0){
				startIdx = taxon_hierarchystr.indexOf("tribe")+("tribe").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				tribestr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("tribe",tribestr);
			
			if (taxon_hierarchystr.indexOf("subtribe")>=0){
				startIdx = taxon_hierarchystr.indexOf("subtribe")+("subtribe").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				subtribestr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("subtribe",subtribestr);
			
			if (taxon_hierarchystr.indexOf("genus")>=0){
				startIdx = taxon_hierarchystr.indexOf("genus")+("genus").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				genusstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("genus",genusstr);
			
			if (taxon_hierarchystr.indexOf("subgenus")>=0){
				startIdx = taxon_hierarchystr.indexOf("subgenus")+("subgenus").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				subgenusstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("subgenus",subgenusstr);
			
			if (taxon_hierarchystr.indexOf("section")>=0){
				startIdx = taxon_hierarchystr.indexOf("section")+("section").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				sectionstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("section",sectionstr);
			
			if (taxon_hierarchystr.indexOf("subsection")>=0){
				startIdx = taxon_hierarchystr.indexOf("subsection")+("subsection").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				subsectionstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("subsection",subsectionstr);

			if (taxon_hierarchystr.indexOf("species")>=0){
				startIdx = taxon_hierarchystr.indexOf("species")+("species").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				speciesstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("species",speciesstr);

			if (taxon_hierarchystr.indexOf("subspecies")>=0){
				startIdx = taxon_hierarchystr.indexOf("subspecies")+("subspecies").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				subspeciesstr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("subspecies",subspeciesstr);
			
			if (taxon_hierarchystr.indexOf("variety")>=0){
				startIdx = taxon_hierarchystr.indexOf("variety")+("variety").length()+1;
				endIdx = taxon_hierarchystr.indexOf(" ",startIdx);
				varietystr = taxon_hierarchystr.substring(startIdx, endIdx).toLowerCase();
			}
			values.put("variety",varietystr);
			
			Element nomenclature_name = (Element)nomenclature_namepath.selectSingleNode(root);
			nomenclature_namestr = nomenclature_name.getTextNormalize().toLowerCase();
			Element nomenclature_rank = (Element)nomenclature_rankpath.selectSingleNode(root);
			nomenclature_rankstr = nomenclature_rank.getTextNormalize().toLowerCase();
			if (nomenclature_namestr.indexOf(nomenclature_rankstr)>=0){
				startIdx = nomenclature_namestr.indexOf(nomenclature_rankstr)+nomenclature_rankstr.length()+1;
				endIdx = nomenclature_namestr.indexOf(" ",startIdx);
				nomenclature_namestr = nomenclature_namestr.substring(startIdx, endIdx).toLowerCase();
			}else{
				endIdx = nomenclature_namestr.indexOf(" ");
				nomenclature_namestr = nomenclature_namestr.substring(0, endIdx).toLowerCase();
			}
			values.put(nomenclature_rankstr, nomenclature_namestr);
			
			insertIntoFilename2TaxonTable();
			resetValues();
			resetAllStr();
		}catch(Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}		
	}
	
	protected void resetAllStr() {
		taxon_hierarchystr ="";
		nomenclature_namestr ="";
		nomenclature_rankstr = "";
		domainstr ="";
		kingdomstr ="";
		phylumstr ="";
		subphylumstr ="";
		superdivisionstr ="";
		divisionstr ="";
		subdivisionstr ="";
		superclassstr ="";
		classstr ="";
		subclassstr ="";
		superorderstr ="";
		orderstr ="";
		suborderstr ="";
		superfamilystr ="";
		familystr ="";
		subfamilystr="";
		tribestr="";
		subtribestr="";
		genusstr="";
		subgenusstr="";
		sectionstr="";
		subsectionstr="";
		speciesstr="";
		subspeciesstr="";
		varietystr="";
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String filepath = "C:\\Users\\jingliu5\\UFLwork\\Sponges\\sponges-11mar13\\sponges-11mar13\\target\\final\\";
		String database = "matrices";
		String prefix = "sponges";
		FileName2TaxonSponges fnts = new FileName2TaxonSponges(filepath, database, prefix);
		fnts.createFilename2taxonTable();
		fnts.populateFilename2TaxonTable_AlphebeticNames();
	}
	

	

}
