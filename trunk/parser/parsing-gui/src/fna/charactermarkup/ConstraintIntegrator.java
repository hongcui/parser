/**
 * 
 */
package fna.charactermarkup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import fna.parsing.ApplicationUtilities;

/**
 * @author Hong Cui
 * This class reads volumes of annotated taxonomic descriptions (e.g., FNA) from final folders and integrate 
 * taxon constraints, parallelism constraints, geo constraints, and provenance attributes in the taxon descriptions involved.
 * The expected results are 
 * 1. that the integrated descriptions confirm to the xml schema published at
 * http://biosemantics.googlecode.com/svn/trunk/characterStatements/characterAnnotationSchema.xsd
 * which is also the default schema assumed by the SDD project (format conversion code by Alex).
 * AND
 * 2. that the description of a higher taxa is applicable for all its lower taxa. i.e., 
 * "a lower taxon can inherit all characters from its higher rank taxon (i.e., its parent taxon) and also have characters specific 
 * to the lower taxon itself" ----really? --this may be the ideal
 * 
 * or
 * 
 * that the description of a higher taxa hold all possible characters of its lower taxa (color from white, pink to red) that 
 * may be overridden by characters of lower ranked taxa. ---this more like the reality
 * 
 * 
 * Because some taxa span cross multiple volumes, 
 * to access descriptions of a taxa (e.g. a family), all related filename2taxons table needs to be merged.
 * 
 * "provenance" attribute will have to be removed?
 */

public class ConstraintIntegrator {
	private static final Logger LOGGER = Logger.getLogger(ConstraintIntegrator.class);
	private Connection conn;
	private String outputfolder;
	private Hashtable<String, String> inputnprefix;
	private ArrayList<String> dataprefixes = new ArrayList<String> ();
	private static final String filename2taxon = "filename2taxon";
	private static int id = 0;
	private SAXBuilder builder = new SAXBuilder();
	private static Hashtable<String, String> provenance = new Hashtable<String, String>();
	private static XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
	private static ArrayList<String> ranks = new ArrayList<String>();
	private static XPath parallelismconstraint;
	private static XPath geoconstraint;
	private static XPath taxonconstraint;
	private static XPath provenanceattribute;
	private static XPath inbracketsattribute;

	static{
		try{
			parallelismconstraint = XPath.newInstance(".//*[@parallelism_constraint]");
			geoconstraint = XPath.newInstance(".//*[@geographical_constraint]");
		    taxonconstraint = XPath.newInstance(".//*[@taxon_constraint]");
		    provenanceattribute = XPath.newInstance(".//*[@provenance]");
		    inbracketsattribute = XPath.newInstance(".//*[@in_brackets]");
		    ranks.add("domain");
		    ranks.add("kingdom");
		    ranks.add("phylum");
		    ranks.add("subphylum");
		    ranks.add("superdivision");
		    ranks.add("division");
		    ranks.add("subdivision");
		    ranks.add("superclass");
		    ranks.add("class");
		    ranks.add("subclass");
		    ranks.add("superorder");
		    ranks.add("order");
		    ranks.add("suborder");
		    ranks.add("superfamily");
		    ranks.add("family");
		    ranks.add("subfamily");
		    ranks.add("tribe");
		    ranks.add("subtribe");
		    ranks.add("genus");
		    ranks.add("subgenus");
		    ranks.add("section");
		    ranks.add("subsection");
		    ranks.add("species");
		    ranks.add("subspecies");
		    ranks.add("variety");
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}
	
	/**
	 * constructor
	 * @param sourcenprefix is a hashtable mapping absolute input dirs (e.g. final dirs) to dataprefixs
	 * @param targetfolder
	 * @param database
	 */
	public ConstraintIntegrator(Hashtable<String, String> sourcenprefix, String targetfolder, String database) {
		this.inputnprefix = sourcenprefix;
		this.outputfolder = targetfolder;	
		this.dataprefixes = (ArrayList<String>) sourcenprefix.values();
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
			    String URL = "jdbc:mysql://localhost/"+database+"?user=termsuser&password=termspassword&connectTimeout=0&socketTimeout=0&autoReconnect=true";
				//String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);						
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}

	/**
	 * main action of this class
	 */
	public void integrateConstraints(){
		Enumeration<String> en = inputnprefix.keys(); //input dirs
		while(en.hasMoreElements()){
			String inputdir = en.nextElement();
			File[] inputfiles = (new File(inputdir)).listFiles();
		
			for(File inputfile: inputfiles){				
				try{
					boolean changed = false;
					Document doc = builder.build(inputfile);
					Element root = doc.getRootElement();
					
					List<Element> targets = this.parallelismconstraint.selectNodes(root);
					if(targets.size()>0) changed = true;
					integrateParallelismConstraint(targets, root, inputfile);
					
					targets = this.geoconstraint.selectNodes(root);
					if(targets.size()>0) changed = true;
					integrateGeoConstraint(targets, root, inputfile);
				
					
					targets = this.taxonconstraint.selectNodes(root);
					if(targets.size()>0) changed = true;
					integrateTaxonConstraint(targets, root); //other affected files are saved in this method
					
					
					targets = this.provenanceattribute.selectNodes(root);
					if(targets.size()>0) changed = true;
					integrateProvenanceAttribute(targets, root, inputfile);	
					
					targets = this.inbracketsattribute.selectNodes(root);
					if(targets.size()>0) changed = true;
					integrateInbracketsAttribute(targets, root, inputfile);
					
					if(changed) outputXML(root, inputfile);	
				}catch(Exception e){
					StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());				
				}
			}
		}
		
		//Having gone through all provenance instances, output provenance hashtable in the same folder as the xml files
		try{
			FileOutputStream fos = new FileOutputStream(new File(this.outputfolder, "provenance_info"));
			ObjectOutputStream oos = new ObjectOutputStream(fos);	
			oos.writeObject(this.provenance);
			oos.close();
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());							
		}
		
 	}
	
	/**
	 * simply remove in_brackets attributes for character and relation targets
	 * @param targets
	 * @param root
	 * @param inputfile
	 */
	private void integrateInbracketsAttribute(List<Element> targets,
			Element root, File inputfile) {
		if(targets.size()==0) return;
		for(Element target: targets){
			target.removeAttribute("in_brackets");
		}		
	}

	/**
	 * Remove provenance attribute and save it in the external Hashtable provenance with a UUID
	 * Add an attribute "constraint" with the UUID in the xml file.
	 * UUID is the linkage between the XML files and the external provenance information
	 * No need to differentiate character and relation targets.
	 * @param targets
	 * @param root
	 * @param inputfile
	 */
	private void integrateProvenanceAttribute(List<Element> targets,
			Element root, File inputfile) {
		if(targets.size()==0) return;
		
		for(Element target: targets){
			String prov = target.getAttributeValue("provenance");
			String uuid = UUID.randomUUID().toString();
			//save to the hash
			this.provenance.put(uuid, inputfile.getAbsolutePath()+":"+prov);
			//add UUID to the file
			Attribute constraint = target.getAttribute("constraint");
			if(constraint==null){
				target.setAttribute(new Attribute("constraint", "UUID:"+uuid));
			}else{
				constraint.setValue(constraint.getValue() + ", UUID:"+uuid);
			}
			//remove provenance attribute
			target.removeAttribute("provenance");
		}	
	}



	/**
	 * Deals with three cases: 1. "apply only to" taxa,  2. "except" taxa, 3. "for example" and "see" taxa
	 * 1. apply only to: (cue phrases: in, as, under)
	 * 	  move affected character elements to the descriptions of the mentioned taxa, if not already included there
	 * 2. except: (cue phrases: not in, except in, except)
	 *    move affected character elements to all subtaxa but the mentioned ones, if not already included there
	 * 3. for example: (cue phrases: e.g., see)
	 *    do nothing   
	 * For all cases, keep character elements for the current description, but remove taxon_constraint attribute.    [assume the "reality" scenario]
	 *
	 * For example: leaves alternate (opposite in S. nathorstii, S. oppositifolia). 
	 * current description[Saxifraga]: leaves alternate or opposite
	 *  S. nathorstii, S. oppositifolia: leaves opposite
	 * 
	 * 
	 * @param elementsWithConstraint
	 * @param root: root element of the file holding the elementsWithConstraint
	 * @param dataprefix: used to select the taxon2filename table for this collection
	 */
	private void integrateTaxonConstraint(List<Element> elementsWithConstraint, Element root) {
		if(elementsWithConstraint.size()==0) return;
		Hashtable<File, Element> affectedroots = new Hashtable<File, Element>();
		ArrayList<String> includedfiles = new ArrayList<String>();
		ArrayList<String> excludedfiles = new ArrayList<String>();
		for(Element elementWithConstraint: elementsWithConstraint){
			String taxonconstraint = elementWithConstraint.getAttributeValue("taxon_constraint").trim();
			if(taxonconstraint.startsWith("not in ") || taxonconstraint.startsWith("except in ") | taxonconstraint.startsWith("except ")){
				String[] excludes = taxonconstraint.replaceFirst("^(not in|except in|except)\\b", "").split("\\s*(,|and)\\s*");
				groupingFiles(root, excludes, includedfiles, excludedfiles, "E");
			}else if (taxonconstraint.startsWith("in ") || taxonconstraint.startsWith("as ") || taxonconstraint.startsWith("under ")){
				String[] includes = taxonconstraint.replaceFirst("^(in|as|under)\\b", "").split("\\s*(,|and)\\s*");
				groupingFiles(root, includes, includedfiles, excludedfiles, "I");
			}
			checkFiles(elementWithConstraint, includedfiles, excludedfiles, affectedroots);  
			elementWithConstraint.removeAttribute("taxon_constraint");			
		}
		//output affected files. The originating file is saved in the main method.
		Enumeration<File> en = affectedroots.keys();
		while(en.hasMoreElements()){
			File affected = en.nextElement();
			outputXML(affectedroots.get(affected), affected);
		}				
	}
	/*
	private String getPrefix(File file){
		String prefix = null;		
		do{//find the right dir to retrieve the prefix
			String dir = file.getParent(); 
			if(dir == null) break;
			prefix = this.inputnprefix.get(dir);
		}while (prefix ==null);
		
		return prefix;
	}*/

	/**
	 * Check and make sure the elementWithConstraint is included in the includedfiles and is not included in the excludedfiles
	 * The elementWithConstraint may be a character element or a relation element

	 * @param elementWithConstraint
	 * @param includedfiles
	 * @param excludedfiles
	 * @param affectedroots: to be populated by the root element of files that are changed by this method.
	 */
	private void checkFiles(Element elementWithConstraint, ArrayList<String> includedfiles, ArrayList<String> excludedfiles, Hashtable<File, Element> affectedroots) {
		String type =  elementWithConstraint.getName();
		if(type.compareTo("character")==0){
			checkCharactersInFiles(elementWithConstraint, includedfiles,
					excludedfiles, affectedroots);
		}else if(type.compareTo("relation")==0){
			//collect information about the relation
			String relationstring = "";
			List<Attribute> atts = elementWithConstraint.getAttributes();
			for(Attribute att: atts){
				String attname = att.getName();
				String attvalue= att.getValue();
				if(!attname.matches("(.*?_constraint|in_brackets|provenance|from|to")){
					relationstring += attname+"='"+attvalue+"'&";
				}
			}
			relationstring = relationstring.replaceFirst("&$", "");
			Hashtable<String, String> structureinfo = new Hashtable<String, String> ();
			String relationname = elementWithConstraint.getAttributeValue("name");
			//collect information about from and to structures the relation is associated with
			getAssociatedStructureInfo(elementWithConstraint, structureinfo);

			//check includedfile one by one: search for relation and search for relational characters
			for(String includedfile: includedfiles){
				try{
					Document doc = builder.build(includedfile);
					Element root = doc.getRootElement();
					boolean findrelation = false;
					//any relation with that name?
					List<Element> relations = XPath.selectNodes(root, "//relation["+relationstring+"]");
					for(Element relation: relations){
						//associated with the right structures?
						Hashtable<String, String> structureinfo2 = new Hashtable<String, String> ();
						getAssociatedStructureInfo(relation, structureinfo2);
						if(structureinfo.get("fromstructname").compareTo(structureinfo2.get("fromstructname")) == 0 &&
								structureinfo.get("tostructname").compareTo(structureinfo2.get("tostructname")) == 0 &&
								structureinfo.get("fromstructconstraint").compareTo(structureinfo2.get("fromstructconstraint")) == 0 &&
								structureinfo.get("tostructconstraint").compareTo(structureinfo2.get("tostructconstraint")) == 0){
							findrelation = true;
							break;							
						}
					}
					boolean findrelationalcharacter = false;
					//if not, try to find relational characters with constraints. The relational character should has a value that overlaps with the relation name (which would have an extra prep). The subject of the character and its constraining structure should match the from and to structure of the relation
					if(!findrelation){
						List<Element> structures = XPath.selectNodes(root,  "//structure[@name='"+structureinfo.get("fromstructname")+"'"+(structureinfo.get("fromstructconstraint").length() >0? "&@constraint='"+structureinfo.get("fromstructconstraint")+"']":"]"));
						for(Element structure: structures){
							List<Element> characters = structure.getChildren("character");
							for(Element character: characters){
								String value = character.getAttribute("value") != null? character.getAttributeValue("value") : null;
								if(value !=null && relationname.startsWith(value)){
									//check further for the constraint
									String constraintids = character.getAttribute("constraintid")!=null? character.getAttributeValue("constraintid") : null;
									if(constraintids!=null){
										String[] structids = constraintids.split("\\s+");
										for(String structid : structids){
											//does this match tostructure?
											Element struct = (Element) XPath.selectSingleNode(root, "//structure[@id='"+structid+"']");
											String toconstraint = struct.getAttribute("constraint") !=null? struct.getAttributeValue("constraint") : "" ;
											if(struct.getAttributeValue("name").compareTo(structureinfo.get("tostructname"))==0 && toconstraint.compareTo(structureinfo.get("tostructconstraint"))==0){
												//do the modifiers match too?
												String thismodifier = character.getAttribute("modifier") !=null? character.getAttributeValue("modifier"): null;
												String modifier = elementWithConstraint.getAttribute("modifier") !=null? elementWithConstraint.getAttributeValue("modifier"): null;
												if(thismodifier.contains(modifier) || modifier.contains(thismodifier)){
													findrelationalcharacter = true;
													break;
												}
											}
										}
									}
									
								}
							}
						}
					}
					//still not found
					if(!findrelationalcharacter){
						LOGGER.info("Relation ["+relationstring+"] from "+structureinfo.get("fromstructconstraint") +" "+structureinfo.get("fromstructname") +" to "+
								structureinfo.get("tostructconstraint") +" "+structureinfo.get("tostructname") +
								" was not found in "+includedfile +" where it should be. It is added.");

						//add the relation to this file: need to add both structures and relation
						//add structures if needed
						//from structures
						List<Element> fromstructures = XPath.selectNodes(root,  "//structure[@name='"+structureinfo.get("fromstructname")+"'"+(structureinfo.get("fromstructconstraint").length() >0? "&@constraint='"+structureinfo.get("fromstructconstraint")+"']":"]"));
						ArrayList<String> fromids = new ArrayList<String> ();
						if(fromstructures.size()>0){
							for(Element fromstructure: fromstructures){
								fromids.add(fromstructure.getAttributeValue("id"));
							}
						}else{
							//create structure
							Element struct = new Element("structure");
							struct.setAttribute("id", "i"+id); //integration id, original ids starts with "o"
							fromids.add("i"+id);
							id++;
							struct.setAttribute("name", structureinfo.get("fromstructname"));
							if(structureinfo.get("fromstructconstraint").length()>0) struct.setAttribute("constraint", structureinfo.get("fromstructconstraint"));
							root.addContent(struct);
						}
						//to structures
						List<Element> tostructures = XPath.selectNodes(root,  "//structure[@name='"+structureinfo.get("tostructname")+"'"+(structureinfo.get("tostructconstraint").length() >0? "&@constraint='"+structureinfo.get("tostructconstraint")+"']":"]"));
						ArrayList<String> toids = new ArrayList<String> ();
						if(tostructures.size()>0){
							for(Element tostructure: tostructures){
								toids.add(tostructure.getAttributeValue("id"));
							}
						}else{
							//create structure
							Element struct = new Element("structure");
							struct.setAttribute("id", "i"+id); //integration id, original ids starts with "o"
							toids.add("i"+id);
							id++;
							struct.setAttribute("name", structureinfo.get("tostructname"));
							if(structureinfo.get("tostructconstraint").length()>0) struct.setAttribute("constraint", structureinfo.get("tostructconstraint"));
							root.addContent(struct);
						}
						//add relation
						for(String fromid: fromids){
							for(String toid: toids){
								Element relation = new Element("relation");
								relation.setAttribute("from", fromid);
								relation.setAttribute("to", toid);
								String[] attributes = relationstring.replace("'", "").split("&");
								for(String attribute : attributes){
									String[] info = attribute.split("=");
									relation.setAttribute(info[0], info[1]);
								}
								root.addContent(relation);
							}
						}
					}					
				}catch(Exception e){
					StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());							
				}
			}
			
			//check excludedfile one by one search for relation and search for relational characters
			for(String excludedfile: excludedfiles){
				try{
					Document doc = builder.build(excludedfile);
					Element root = doc.getRootElement();
					boolean findrelation = false;
					//any relation with that name?
					List<Element> relations = XPath.selectNodes(root, "//relation["+relationstring+"]");
					for(Element relation: relations){
						//associated with the right structures?
						Hashtable<String, String> structureinfo2 = new Hashtable<String, String> ();
						getAssociatedStructureInfo(relation, structureinfo2);
						if(structureinfo.get("fromstructname").compareTo(structureinfo2.get("fromstructname")) == 0 &&
								structureinfo.get("tostructname").compareTo(structureinfo2.get("tostructname")) == 0 &&
								structureinfo.get("fromstructconstraint").compareTo(structureinfo2.get("fromstructconstraint")) == 0 &&
								structureinfo.get("tostructconstraint").compareTo(structureinfo2.get("tostructconstraint")) == 0){
							findrelation = true;
							LOGGER.info("Relation ["+relationstring+"] from "+structureinfo.get("fromstructconstraint") +" "+structureinfo.get("fromstructname") +" to "+
									structureinfo.get("tostructconstraint") +" "+structureinfo.get("tostructname") +
									" was found in "+excludedfile +" where it should not be. It is removed.");
							relation.detach();
						}
					}
					boolean findrelationalcharacter = false;
					//if not, try to find relational characters with constraints. The relational character should has a value that overlaps with the relation name (which would have an extra prep). The subject of the character and its constraining structure should match the from and to structure of the relation
					if(!findrelation){
						List<Element> structures = XPath.selectNodes(root,  "//structure[@name='"+structureinfo.get("fromstructname")+"'"+(structureinfo.get("fromstructconstraint").length() >0? "&@constraint='"+structureinfo.get("fromstructconstraint")+"']":"]"));
						for(Element structure: structures){
							List<Element> characters = structure.getChildren("character");
							for(Element character: characters){
								String value = character.getAttribute("value") != null? character.getAttributeValue("value") : null;
								if(value !=null && relationname.startsWith(value)){
									//check further for the constraint
									String constraintids = character.getAttribute("constraintid")!=null? character.getAttributeValue("constraintid") : null;
									if(constraintids!=null){
										String[] structids = constraintids.split("\\s+");
										for(String structid : structids){
											//does this match tostructure?
											Element struct = (Element) XPath.selectSingleNode(root, "//structure[@id='"+structid+"']");
											String toconstraint = struct.getAttribute("constraint") !=null? struct.getAttributeValue("constraint") : "" ;
											if(struct.getAttributeValue("name").compareTo(structureinfo.get("tostructname"))==0 && toconstraint.compareTo(structureinfo.get("tostructconstraint"))==0){
												//do the modifiers match too?
												String thismodifier = character.getAttribute("modifier") !=null? character.getAttributeValue("modifier"): null;
												String modifier = elementWithConstraint.getAttribute("modifier") !=null? elementWithConstraint.getAttributeValue("modifier"): null;
												if(thismodifier.contains(modifier) || modifier.contains(thismodifier)){
													findrelationalcharacter = true;
													LOGGER.info("Relation ["+relationstring+"] from "+structureinfo.get("fromstructconstraint") +" "+structureinfo.get("fromstructname") +" to "+
															structureinfo.get("tostructconstraint") +" "+structureinfo.get("tostructname") +
															" was found in "+excludedfile +"(as relational character) where it should not be. It is removed.");
													character.detach();

												}
											}
										}
									}
									
								}
							}
						}
					}
				}catch(Exception e){
					StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());							
				}
			}
		}
		
	}

	private void getAssociatedStructureInfo(Element elementWithConstraint,
			Hashtable<String, String> structureinfo) {
		String fromid = elementWithConstraint.getAttributeValue("from");
		String toid = elementWithConstraint.getAttributeValue("to");
		
		//String fromstructname = null;
		//String fromstructconstraint = null;
		//String tostructname = null;
		//String tostructconstraint = null;
		try{
			Element root = elementWithConstraint.getDocument().getRootElement();
			Element fromstructure = (Element) XPath.selectSingleNode(root, "//structure[@id='"+fromid+"']");
			if(fromstructure!=null){
				structureinfo.put("fromstructname",  fromstructure.getAttributeValue("name"));
				structureinfo.put("fromstructconstraint",  fromstructure.getAttribute("constraint")!=null? fromstructure.getAttributeValue("constraint") : "");
			}
			Element tostructure = (Element) XPath.selectSingleNode(root, "//structure[@id='"+toid+"']");
			if(tostructure!=null){
				structureinfo.put("tostructname", tostructure.getAttributeValue("name"));
				structureinfo.put("tostructconstraint",  tostructure.getAttribute("constraint")!=null? tostructure.getAttributeValue("constraint") : "");
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());							
		}
	}

	private void checkCharactersInFiles(Element elementWithConstraint,
			ArrayList<String> includedfiles, ArrayList<String> excludedfiles,
			Hashtable<File, Element> affectedroots) {
		//deal with a character element:
		//collect character information
		String characterstring = "";
		List<Attribute> atts = elementWithConstraint.getAttributes();
		for(Attribute att: atts){
			String attname = att.getName();
			String attvalue= att.getValue();
			if(!attname.matches("(.*?_constraint|in_brackets|provenance")){
				characterstring += attname+"='"+attvalue+"'&";
			}
		}
		characterstring = characterstring.replaceFirst("&$", "");
		
		//collect information of the structure the character is associated with
		Element structure = elementWithConstraint.getParentElement();
		String structurename = structure.getAttributeValue("name");
		String constraint = structure.getAttribute("constraint") == null? null : structure.getAttributeValue("constraint");
		
		//construct the XPath to select affected structures
		String xpath = "//structure[@name='"+structurename+"'"+(constraint!=null? "&@constraint='"+constraint+"']":"]")+
				"/character["+characterstring+"]";
		
		//checking includedfile one by one
		for(String includedfile : includedfiles){
			try{
				Document doc = builder.build(includedfile);
				Element root = doc.getRootElement();
				Element match = (Element) XPath.selectSingleNode(root, xpath);
				if(match == null){
					//character should be in the file, but not
					LOGGER.info("Character ["+characterstring+"] of structure ["+constraint+" "+structure+"] was not found in "+includedfile +" where it should be. It is added.");
					//add character to its structure(s), if the structure does not exist, create the structure element
					//find the structure in root
					List<Element> structures = XPath.selectNodes(root,  "//structure[@name='"+structurename+"'"+(constraint!=null? "&@constraint='"+constraint+"']":"]"));
					if(structures.size() == 0){
						//create structure element
						Element struct = new Element("structure");
						struct.setAttribute("id", "i"+id); //integration id, original ids starts with "o"
						id++;
						struct.setAttribute("name", structurename);
						if(constraint.length()>0) struct.setAttribute("constraint", constraint);
						structures.add(struct);
						root.addContent(struct);
					}
					//create character element
					Element character = new Element("character");
					String[] characters = characterstring.replace("'", "").split("&");
					for(String acharacter : characters){
						String[] info = acharacter.split("=");
						character.setAttribute(info[0], info[1]);
					}
					for(Element astructure: structures){
						astructure.addContent(character);
					}
					//put the file to the updated list
					affectedroots.put(new File(includedfile), root);
				}
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());							
			}
		}
		
		//checking excludedfile one by one
		for(String excludedfile : excludedfiles){
			try{
				Document doc = builder.build(excludedfile);
				Element root = doc.getRootElement();
				List<Element> matches = (List<Element>) XPath.selectNodes(root, xpath);
				if(matches != null){
					//should not have the character, but had
					LOGGER.info("Character ["+characterstring+"] of structure ["+constraint+" "+structure+"] was found in "+excludedfile +" where it should not be. It is removed.");
					for(Element amatch: matches){
						//remove the characters
						//amatch.detach();
						List<Element> characters = XPath.selectNodes(amatch, ".//character["+characterstring+"]");
						for(Element character: characters){
							character.detach();
						}
					}
					affectedroots.put(new File(excludedfile), root);
				}
								
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());							
			}
		}
	}

	private void groupingFiles(Element root, String[] taxonnames, ArrayList<String> includedfiles, ArrayList<String> excludedfiles, String flag) {
		
		//name parts of current root
		Hashtable<String, Element> names = getTaxonNamesFrom(root);
		
		int lowest = 0;
		//formulate the query
		String query = "select * from "+"PREFIX_"+filename2taxon+" where "; //TODO: what if the taxon spans two filename2taxon tables?
		Enumeration<String> en = names.keys();
		while(en.hasMoreElements()){
			String rank = en.nextElement();
			String name = names.get(rank).getTextNormalize();
			query += rank+"="+name+",";
			int rankindex =  ranks.indexOf(rank);
			if(lowest < rankindex) lowest = rankindex;
		}
		query = query.replaceFirst(",$", "");
		//collect ranks that are lower than the current rank
		String lowerranks = ""; //variety, subspecies, species, ...
		for(int i = lowest+1; i < ranks.size()-1; i++){
			lowerranks += ranks.get(i)+",";
		}
		lowerranks = lowerranks.replaceFirst(",$", "");
		query = query.replace("*", "filename,"+lowerranks);
		String[] lowerrankarray = lowerranks.split(",");
		
		//construct union query from the base query
		String unionquery = "";
		for(String prefix : dataprefixes){
			unionquery += query.replaceFirst(" PREFIX_"+filename2taxon+" ", " "+prefix+"_"+filename2taxon+" ")+" UNION ";
		}
		unionquery = unionquery.trim().replaceFirst(" UNION$", "");
		
		int matches = 0;
		String matched = "";
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(unionquery);
			while(rs.next()){
				String filename = rs.getString("filename");
				for(String listedname: taxonnames){
					listedname = listedname.replaceFirst(".*?\\.", "");
					boolean match = false;
					for(String lowerrank: lowerrankarray){
						String name = rs.getString(lowerrank);
						if(name.compareTo(listedname)==0){
							matches++;
							match = true;
							matched +=name+",";
							if(flag.compareTo("E")==0) excludedfiles.add(filename);
							else if(flag.compareTo("I")==0) includedfiles.add(filename);
						}
					}
					if(!match){
						if(flag.compareTo("E")==0) includedfiles.add(filename);
						else if(flag.compareTo("I")==0) excludedfiles.add(filename);
					}
				}			
			}
			if(taxonnames.length > matches){
				LOGGER.info("Some listed taxon names are not found using query:  "+unionquery);
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());							
		}
	}

	/**
	 * 
	 * @param root
	 * @return hastable rank => rank element
	 */
	private Hashtable<String, Element> getTaxonNamesFrom(Element root) {
		Hashtable<String, Element> names = new Hashtable<String, Element>();
		try{
			for(int i = this.ranks.size()-1; i>=0; i--){
				Element name = (Element) XPath.selectSingleNode(root, "//"+ranks.get(i)+"_name");
				if(name!=null){
					names.put(ranks.get(i), name);
				}
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());							
		}
		return names;
	}

	/**
	 * Change Geographical constraints to regular constraint attribute
	 * This applies to both character and relation elements.
	 * @param targets
	 * @param root
	 * @param inputfile
	 */
	private void integrateGeoConstraint(List<Element> targets, Element root,
			File inputfile) {
		if(targets.size()==0) return;
		
		for(Element target: targets){
			String geo = target.getAttributeValue("geographical_constraint");
			Attribute constraint = target.getAttribute("constraint");
			if(constraint==null){
				target.setAttribute(new Attribute("constraint", "GEO:"+geo));
			}else{
				constraint.setValue(constraint.getValue() + ", GEO:"+geo);
			}
			//remove geo_constraint attribute
			target.removeAttribute("geographical_constraint");
		}		
	}

	/**
	 * In this case, parenthetical characters/relations are not parallel among the taxa at the same rank of the current taxa: 
	 * i.e., it is included in some taxa, but not in other sibling taxa. When turned into taxon-character matrices, these characters 
	 * will have many empty cells.
	 *  
	 * No integration is needed, simply remove parallelism_constraint.  
	 * 
	 * @param targets
	 * @param root
	 * @param inputfile
	 */
	private void integrateParallelismConstraint(List<Element> targets,
			Element root, File inputfile) {
		if(targets.size()==0) return;
		for(Element target: targets){
			target.removeAttribute("parallelism_constraint");
		}
	}

	private void outputXML(Element root, File file) {
		//output the changed file
		try{
			BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(new File(this.outputfolder, file.getName())));
			outputter.output(root, out);
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());							
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Hashtable<String, String> inputnprefix = new Hashtable<String, String>();
		//inputprefix should include all volumns that belong to one family or the taxon in question
		inputnprefix.put("", "fnav19");
		inputnprefix.put("", "fnav20");
		

	}

}
