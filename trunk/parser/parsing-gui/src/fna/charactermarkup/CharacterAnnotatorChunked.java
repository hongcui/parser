/**
 * 
 */
package fna.charactermarkup;
import org.jdom.*;
import org.jdom.input.*;
import org.jdom.xpath.*;
import org.jdom.output.*;

import fna.parsing.state.SentenceOrganStateMarker;
import fna.parsing.state.StateCollector;
import fna.parsing.state.WordNetWrapper;

import java.sql.*;
import java.util.*;
/**
 * @author hongcui
 *
 */
public class CharacterAnnotatorChunked {
	private Element root = null;
	private Element statement = null;
	private ChunkedSentence cs = null;
	private String sentsrc = null;
	private ArrayList<Element> subjects = new ArrayList<Element>();
	private ArrayList<Element> latestelements = new ArrayList<Element>();//save the last set of elements added. independent from adding elements to <Statement>
	private Hashtable<String, String> characterhash = new Hashtable<String, String>();
	//private ArrayList<String> latestelementids = new ArrayList<String>();
	private int structid = 1;
	//private ArrayList<Element> elements = null; 
	private int relationid = 1;
	private SentenceOrganStateMarker sosm = null;
	private String unassignedmodifiers = ""; //holds modifiers that may be applied to the next chunk
	private String senttag = "";
	private String sentmod = "";
	static protected Connection conn = null;
	static protected String username = "root";
	static protected String password = "root";
	static protected String database = "fnav19_benchmark";
	
	
	
	/**
	 * 
	 */
	public CharacterAnnotatorChunked(int sentindex, String sentsrc, ChunkedSentence cs, Document doc, SentenceOrganStateMarker sosm) {
		this.sosm = sosm;
		this.root = doc.getRootElement();
		this.cs = cs; //may contain 1 or more segments
		this.sentsrc = sentsrc;
		this.statement = new Element("statement");
		this.statement.setAttribute("id", "s"+sentindex);
		
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
			}
			/*
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select modifier, tag from sentence where source ='"+sentsrc+"'");
			if(rs.next()){
				this.senttag = rs.getString("tag").trim();
				this.sentmod = rs.getString("modifier").trim();
			}*/
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public Element annotate() throws Exception{
		//sentence subject
		if(this.senttag.compareTo("ditto")!=0 && this.senttag.length()>0){
			String subject = ("{"+this.sentmod.replaceAll("[\\[\\]]", "").replaceAll(" ", "} {")+"} ("+this.senttag.replaceAll("[\\[\\]]", "").replaceAll(" ", ") (")+")").replaceAll("[{(]and[)}]", "and").replaceAll("[{(]or[)}]", "or").replaceAll("\\{\\}", "").replaceAll("\\s+", " ").trim();
			establishSubject(subject);
			cs.skipLead((this.sentmod+" "+this.senttag).replaceAll("\\[\\w+\\]", "").replaceAll("\\s+", " ").trim().split("\\s+"));
		}
				
		while(cs.hasNext()){
			Chunk ck = cs.nextChunk();
			if(ck instanceof ChunkOrgan){//this is the subject of a segment. May contain multiple organs
				String content = ck.toString().replaceFirst("^z\\[", "").replaceFirst("\\]$", "");
				establishSubject(content);
			}else if(ck instanceof ChunkPrep){
				processPrep((ChunkPrep)ck);
			}else if(ck instanceof ChunkCHPP){//t[c/r[p/o]]
				processCHPP((ChunkCHPP)ck);
			}else if(ck instanceof ChunkNPList){
				establishSubject(ck.toString().replaceFirst("^l\\[", "").replaceFirst("\\]$", ""));				
			}else if(ck instanceof ChunkSimpleCharacterState){
				String[] tokens = ck.toString().replaceAll("[{}]", "").split("\\s+");
				ArrayList<Element> chars = processCharacterText(tokens, this.subjects);
				updateLatestElements(chars);
			}else if(ck instanceof ChunkSL){//coloration[red to black]
				String[] parts = ck.toString().split("[");
				ArrayList<Element> chars = processCharacterList(parts, this.subjects);
				updateLatestElements(chars);
			}else if(ck instanceof ChunkBasedCount){
				ArrayList<Element> chars = annotateCount(this.subjects, ck.toString(), this.unassignedmodifiers);
				this.unassignedmodifiers = "";
				updateLatestElements(chars);
			}
		}
		XMLOutputter xo = new XMLOutputter(Format.getPrettyFormat());
		System.out.println(xo.outputString(this.statement));
		return this.statement;
	}

	private void establishSubject(String content) {
		ArrayList<Element> structures = createStructureElements(content);
		this.subjects = new ArrayList<Element>();
		this.latestelements = new ArrayList<Element>();
		Iterator<Element> it = structures.iterator();
		while(it.hasNext()){
			Element e = it.next();
			if(e.getName().compareTo("structure")==0){ //ignore character elements
				this.subjects.add(e);
				this.latestelements.add(e);
			}
		}
	}
	
	/**
	 * 
	 * @param parts: parts[0] is character name, part[1] is values
	 * @param subjects2
	 * @return
	 */
	private ArrayList<Element> processCharacterList(String[] parts,
			ArrayList<Element> parents) {
		ArrayList<Element> results= new ArrayList<Element>();
		String cname = parts[0];
		String cvalue = parts[1].replaceFirst("\\W+$", "").trim();
		createCharacterElement(parents, results, "", cvalue, cname, "range_value"); //add a general statement: coloration="red to brown"
		
		String[] cvalues = cvalue.split("\\b(to|or|punct)\\b");//add individual values
		for(int i = 0; i<=cvalues.length; i++){
			results.addAll(this.processCharacterText(cvalues[i].split("\\s+"), parents));
		}
		return results;
	}

	/**
	 * 
	 * @param elements
	 */
	private void updateLatestElements(ArrayList<Element> elements) {
		this.latestelements = new ArrayList<Element>();
		latestelements.addAll(elements);
	}

	/**
	 * //t[c/r[p/o]]
	 * @param ck
	 */
	
	private void processCHPP(ChunkCHPP ck) {
		String ckstring = ck.toString();
		ckstring = ckstring.replaceFirst("^t\\[", "").replaceFirst("\\]$", "");
		String c = ckstring.substring(0, ckstring.indexOf("r["));
		String r = ckstring.replace(c, "");
		String p = r.substring(0, r.indexOf("o["));
		String o = r.replace(p, "");
		c = c.replaceAll("(c\\[|\\])", "").trim(); //TODO: will this work for nested chuncks?
		p = p.replaceAll("(p\\[|\\])", "").trim();
		//c: {loosely} {arachnoid}
		String[] words = c.split("\\s+");
		if(isVerb(words[words.length-1])){//t[c[{connected}] r[p[by] o[{conspicuous} {arachnoid} <trichomes>]]] TODO: what if c was not included in this chunk?
			String relation = c+" "+p;
			o = o.substring(ckstring.indexOf("o[")).replaceFirst("\\]$", "");
			ArrayList<Element> structures = processObject(o);
			createRelationElements(relation, this.latestelements, structures);
			updateLatestElements(structures);
		}else{//c: {loosely} {arachnoid}
			String[] tokens = c.replaceAll("[{}]", "").split("\\s+");
			ArrayList<Element> charas = this.processCharacterText(tokens, this.subjects);
			updateLatestElements(charas);
			processPrep(new ChunkPrep(r)); //not as a relation
		}
		
	}

	private boolean isVerb(String word) {
		word = word.replaceAll("[<>{}\\]\\[]", "");
		if(!word.matches(".*?[a-z]+.*")){
			return false;
		}
		if(word.matches("\\b("+StateCollector.stop+")\\b")){
			return false;
		}
		WordNetWrapper wnw = new WordNetWrapper(word);
		String pos = wnw.mostlikelyPOS();
		if(pos != null){
			return pos.compareTo("verb") == 0;
		}else{
			return false;
		}
	}

	/**
	 * CK takes form of relation character/states [structures]?
	 * update this.latestElements with structures only.
	 * @param ck
	 * @param asrelation: if this PP should be treated as a relation
	 */
	private void processPrep(ChunkPrep ck) {
		String ckstring = ck.toString(); //r[p[of] o[.....]]
		String pp = ckstring.substring(ckstring.indexOf("p["), ckstring.indexOf("o[")).replace("p[", "");
		String object  = ckstring.substring(ckstring.indexOf("o[")).replaceFirst("\\]$", "");
		boolean lastIsStruct = false;
		boolean lastIsChara = false;
		Element lastelement = this.latestelements.get(this.latestelements.size()-1);
		if(lastelement.getName().compareTo("structure") == 0){//lastest element is a structure
			lastIsStruct = true;
		}else if(lastelement.getName().compareTo("character") == 0){
			lastIsChara = true;
		}
		ArrayList<Element> structures = new ArrayList<Element>();
		if(object.matches(".*?\\)\\]+$")){ //contains organ
			structures = processObject(object);
			
			if(lastIsChara){
				addAttribute(lastelement, "constraint", pp+" "+listStructureNames(structures)); //a, b, c
				addAttribute(lastelement, "constraintids", pp+" "+listStructureIds(structures));//1, 2, 3
			}else if(lastIsStruct){
				String relation = relationLabel(pp, this.latestelements, structures);//determine the relation
				if(relation != null){
					createRelationElements(relation, this.latestelements, structures);//relation elements not visible to outside 
				}
			}
			
		}else{ //contains no organ, e.g. "at flowering"
			//Element last = this.latestelements.get(this.latestelements.size()-1);
			if(lastelement.getName().compareTo("structure") == 0){
				addAttribute(lastelement, "constraint", ckstring.replaceAll("(\\w\\[|\\]|{|})", ""));
			}else{ //character element
				addAttribute(lastelement, "modifier", ckstring.replaceAll("(\\w\\[|\\]|{|})", ""));
			}
			//addPPAsAttributes(ckstring);
		}
		
		//bookkeeping: update this.latestElements: only structures are visible
		updateLatestElements(structures);
	}

	private ArrayList<Element> processObject(String object) {
		ArrayList<Element> structures;
		String[] twoparts = separate(object);//find the organs in object o[.........{m} {m} (o1) and {m} (o2)]
		structures = createStructureElements(twoparts[1]);//to be added structures found in 2nd part, not rewrite this.latestelements yet
		String[] tokens = twoparts[0].split("\\s+");//add character elements
		processCharacterText(tokens, structures); //process part 1, which applies to all lateststructures, invisible
		return structures;
	}

	/**
	 * 
	 * @param structures
	 * @return
	 */
	private String listStructureIds(ArrayList<Element> structures) {
		StringBuffer list = new StringBuffer();
		Iterator<Element> it = structures.iterator();
		while(it.hasNext()){
			Element e = it.next();
			list.append(e.getAttributeValue("id")+", ");
		}
		return list.toString().trim().replaceFirst(",$", "");
	}

	private String listStructureNames(ArrayList<Element> structures) {
		StringBuffer list = new StringBuffer();
		Iterator<Element> it = structures.iterator();
		while(it.hasNext()){
			Element e = it.next();
			list.append(e.getAttributeValue("name")+", ");
		}
		return list.toString().trim().replaceFirst(",$", "");
	}

	private void createRelationElements(String relation, ArrayList<Element> fromstructs, ArrayList<Element> tostructs) {
		//add relation elements
		for(int i = 0; i<fromstructs.size(); i++){
			String o1id = fromstructs.get(i).getAttributeValue("id");
			for(int j = 0; j<tostructs.size(); j++){
				String o2id = tostructs.get(j).getAttributeValue("id");
				Element rela = new Element("relation");
				rela.setAttribute("name", relation);
				rela.setAttribute("from", o1id);
				rela.setAttribute("to", o2id);
				this.statement.addContent(rela); //add to statement
			}
		}
		//add other relations as a constraint to the structure: apex of leaves {rounded}.
		//expect some character elements in the structure element.
		//if not, in post-processing, remove the constraint
		/*if(relation.compareTo("consists of")!=0){
			String constraint = relation+" ";
			for(int j = 0; j<this.lateststructures.size(); j++){
				constraint += this.lateststructures.get(j).getAttributeValue("name")+", "; //organ name list
			}
			constraint.trim().replaceFirst("\\s*,$", "");
			for(int i = 0; i<latests.size(); i++){
				addAttribute(latests.get(i), "constraint", constraint); //base, of leaves, petals; apex, of leaves, petals
			}
		}*/
	}
	
	
	
	/**
	 * 
	 * @param pp
	 * @param latests
	 * @param lateststructures2
	 * @return
	 */
	private String relationLabel(String pp, ArrayList<Element> organsbeforepp,
			ArrayList<Element> organsafterpp) {
		if(pp.compareTo("of") ==0){
			return differentiateOf(organsbeforepp, organsafterpp);
		}
		return pp;
	}

	private void addAttribute(Element e, String attribute, String value) {
		String v = e.getAttributeValue(attribute).trim();
		if(v !=null && v.length() > 0){
			v += "; "+value;
		}else{
			v = value;
		}
		e.setAttribute(attribute, v);
	}

	/**
	 * 
	 * @param organs
	 * @param organs2
	 * @return part-of or consists-of
	 */
	private String differentiateOf(ArrayList<Element> organsbeforeOf,
			ArrayList<Element> organsafterOf) {
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = null;
			for (int i = 0; i<organsbeforeOf.size(); i++){
				String b = organsbeforeOf.get(i).getAttributeValue("name");
				for(int j = 0; j<organsafterOf.size(); j++){
					String a = organsafterOf.get(i).getAttributeValue("name");
					String pattern = a+"[ ]+of[ ]+[0-9]+.*"+b+"[,\\.]"; //consists-of
					rs = stmt.executeQuery("select * from sentence where originalsent rlike '"+pattern+"'" );
					if(rs.next()){
						return "consists of";
					}
				}
			
			}			
		}catch(Exception e){
			e.printStackTrace();
		}
		return "part of";
	}

	//separate o[......... {m} {m} (o1) and {m} (o2)] to two parts: the last part include all organ names
	private String[] separate(String object) {
		String[] twoparts  = new String[2];
		object = object.replaceFirst("$o\\[", "").replaceFirst("\\]$", "");
		String part2 = object.substring(object.indexOf("(")).trim();
		String part1 = object.replace(part2, "").trim();
		//part 1 may still have modifiers of the first organ in part 2
		String[] ws1 = part1.split("\\s+");
		String[] ws2 = part2.split("\\s+");
		String o = "";
		for(int i =0; i<ws2.length; i++){
			if(ws2[i].indexOf("(")>=0){
				o +=ws2[i]+" ";
			}else{
				break;
			}
		}
		o = o.trim();
		for(int i = ws1.length-1; i>=0; i--){
			if(isConstraint(ws1[i], o)){
				part1 = part1.replaceFirst("\\s*"+ws1[i]+"$", "");
				part2 = ws1[i]+" "+part2;
			}else{
				break;
			}
		}
		
		part1 = part1.replaceAll("\\s+", "\\s").trim();
		part2 = part2.replaceAll("\\s+", "\\s").trim();
		twoparts[0] = part1;
		twoparts[1] = part2;
		return twoparts;
	}

	/**
	 * 
	 * @param ck: {} (), {} (), () and/or ()
	 * @return
	 */
	private ArrayList<Element> createStructureElements(String listofstructures){
		ArrayList<Element> results = new ArrayList<Element>();	
		String[] organs = listofstructures.replaceAll("\\b(and|or)\\b", " , ").split("\\)\\s*,\\s*"); 
		for(int i = 0; i<organs.length; i++){
			String[] organ = organs[i].trim().split("\\s+");
			//for each organ mentioned, find organ name
			String o = "";
			int j = 0;
			for(j = organ.length-1; j >=0; j--){
				if(organ[j].startsWith("(")){
					o += organ[j]+" ";
					organ[j] = "";
				}else{
					break;
				}
			}
			o = o.replaceAll("(\\w\\[|\\]|\\(|\\))", "");
			//create element, 
			Element e = new Element("structure");
			String strid = "o"+this.structid;
			this.structid++;
			e.setAttribute("id", strid);
			e.setAttribute("name", o.trim()); //must have.

			//determine constraints
			String constraint = "";
			for(;j >=0; j--){
				if(organ[j].trim().length()>0){
					String w = organ[j].replaceAll("(\\w\\[|\\]|\\{|\\})", "");
					if(isConstraint(w, o)){
						constraint += w+" ";
						organ[j] = "";
					}else{
						break;
					}
				}
			}
			if(constraint.trim().length() >0){
				addAttribute(e, "constraint", constraint.trim()); //may not have.
			}
			
			//determine character/modifier
			ArrayList<Element> list = new ArrayList<Element>();
			list.add(e);
			processCharacterText(organ, list); //characters created here are final and all the structures will have, therefore they shall stay local and not visible from outside
			results.add(e); //results only adds e
			this.statement.addContent(e);
		}
		return results;
	}

	/**
	 * bases and tips mostly rounded 
	 * @param tokens
	 * @param parents
	 */
	private ArrayList<Element> processCharacterText(String[] tokens, ArrayList<Element> parents) {
		ArrayList<Element> results = new ArrayList<Element>();
		//determine characters and modifiers
		String modifiers = "";
		for(int j = 0; j <tokens.length; j++){
			if(tokens[j].trim().length()>0){
				String w = tokens[j].replaceAll("(\\w\\[|\\]|\\{|\\})", "");
				if(Utilities.isAdv(w)){
					modifiers +=w+" ";
				}else if(w.matches(".*?\\d.*") && !w.matches(".*?[a-z].*")){
					annotateCount(parents, w, modifiers);
					modifiers = "";
				}else{
					//String chara = Utilities.lookupCharacter(w, conn, characterhash);
					String chara = MyPOSTagger.characterhash.get(w);
					if(chara != null){
						createCharacterElement(parents, results,
								modifiers, w, chara, ""); //default type "" = individual vaues
						modifiers = "";
					}
				}
				
			}
		}
		return results;
	}

	private String createCharacterElement(ArrayList<Element> parents,
			ArrayList<Element> results, String modifiers, String cvalue, String cname, String char_type) {
		Element character = new Element("character");
		results.add(character); //add to results
		if(char_type.length() >0){
			character.setAttribute("char_type", char_type);
		}
		character.setAttribute("name", cname);
		character.setAttribute("value", cvalue);
		if(modifiers.trim().length() >0){
			addAttribute(character, "modifier", modifiers.trim()); //may not have
			modifiers = "";
		}
		Iterator<Element> it = parents.iterator();
		while(it.hasNext()){
			Element e = it.next();
			e.addContent(character);//add to e
		}
		return modifiers;
	}

	
	
	
	private ArrayList<Element> annotateCount(ArrayList<Element> elements, String w, String modifiers) {
		// TODO Auto-generated method stub
		return null;
	}

	



	//if w has been seen used as a modifier to organ o
	private boolean isConstraint(String w, String o) {
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select * from sentence where modifier ='"+w+"'");
			if(rs.next()){
				return true;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * output annotated sentence in XML format
	 * Chunk types:
	 * PrepChunk, IVerbChunk (Intransitive verb chunk, followed by a preposition), VerbChunk, ADJChunk, SBARChunk,
	 * etc.
	 * @return
	 */
	/*public Element annotate() throws Exception{
		//query the sentence database for the tag/modifier for this sentence, using this.sentsrc
		//also use the substructure table to resolve of-clauses
		ArrayList<Chunk> chunks = new ArrayList();
		ArrayList<String> structureIDs = new ArrayList();

		this.currentsubject = "fetch from sentence table";
		String modifier = "fetch from sentence table";
		this.currentmainstructure = createStructureElement(this.currentsubject, modifier, this.structid++);
		while(cs.hasNext()){
			Chunk chunk = cs.nextChunk();
			chunks.add(chunk);
			if(chunk instanceof Organ){
				String organ = chunk.getText();				
				if(chunks.size() == 1){
					continue; //this is current subject read from the sentence table
				}else{
					this.statement.addContent(this.currentmainstructure); //add this completed structure
					//create a new structure element
				}
			}else if(chunk instanceof PrepChunk){
				String pphrase = chunk.getText();
				int chunkid = cs.getPointer() - 1;
				Element thiselement = (Element)XPath.selectSingleNode(root, ".\\*[id='"+chunkid+"']"); //IN
				String relationname = thiselement.getAttributeValue("text");
				//create structure(s) from the NPs. e.g "3 florets", character/modifier before organnames 
				//NP may be a list of NPs
				String np = pphrase.replaceFirst("^"+relationname, "").trim();
				ArrayList oids = annotateNP(np); //in which <structure> may be created and inserted into the <statement>
				if(chunks.get(chunks.size()-2) instanceof Organ){ //apex of leaves
					//create a relation
					Element relation = createRelationElement(this.relationid++);
				}else{
					//create a constraint for the last character
				}
			}else if(chunk instanceof SBARChunk){ //SBARChunk could follow any xyzChunk
					
				
			}else if(chunk instanceof SimpleCharacterState){
				//check for its character
				//associate it with current subject
				if(this.currentsubject ==null){
					//save this as a constraint for the to-be-discovered subject 
				}
			}
		}
		
		return statement;
		
	}*/



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
