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
 * fnaglossaryfixed: move verbs such as comprising from the glossary
 *
 */
public class CharacterAnnotatorChunked {
	private Element statement = null;
	private ChunkedSentence cs = null;
	private static ArrayList<Element> subjects = new ArrayList<Element>();//static so a ditto sent can see the last subject
	private ArrayList<Element> latestelements = new ArrayList<Element>();//save the last set of elements added. independent from adding elements to <Statement>
	private String delims = "comma|or";
	private static int structid = 1;
	private static int relationid = 1;
	private String unassignedmodifiers = ""; //holds modifiers that may be applied to the next chunk
	private String senttag = "";
	private String sentmod = "";
	protected Connection conn = null;
	private String tableprefix = null;
	/*static protected String username = "root";
	static protected String password = "root";
	static protected String database = "fnav19_benchmark";*/
	private boolean inbrackets = false;
	private String text  = null;

	
	/**
	 * 
	 */
	public CharacterAnnotatorChunked(Connection conn, String tableprefix) {
		this.conn = conn;
		this.tableprefix = tableprefix;
	}
	
	public Element annotate(String sentindex, String sentsrc, ChunkedSentence cs) throws Exception{
		this.statement = new Element("statement");
		this.statement.setAttribute("id", "s"+sentindex);
		this.cs = cs;
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select modifier, tag, originalsent from "+this.tableprefix+"_sentence where source ='"+sentsrc+"'");
			if(rs.next()){
				this.senttag = rs.getString(2).trim();
				this.sentmod = rs.getString(1).trim();
				this.text = rs.getString(3).trim();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		
		//sentence subject
		if(this.senttag.compareTo("ditto")!=0 && this.senttag.length()>0){
			String subject = ("{"+this.sentmod.replaceAll("[\\[\\]]", "").replaceAll(" ", "} {")+"} ("+this.senttag.replaceAll("[\\[\\]]", "").replaceAll(" ", ") (")+")").replaceAll("[{(]and[)}]", "and").replaceAll("[{(]or[)}]", "or").replaceAll("\\{\\}", "").replaceAll("\\s+", " ").trim();
			establishSubject(subject);
			cs.skipLead((this.sentmod+" "+this.senttag).replaceAll("\\[\\w+\\]", "").replaceAll("\\s+", " ").trim().split("\\s+"));
			cs.setInSegment(true);
		}else if(this.senttag.compareTo("ditto")==0){
			this.reestablishSubject();
			cs.setInSegment(true);
		}
				
		annotateByChunk(cs, false);
		Element text = new Element("text");
		text.addContent(this.text);
		this.statement.addContent(text);
		XMLOutputter xo = new XMLOutputter(Format.getPrettyFormat());
		System.out.println(xo.outputString(this.statement));
		return this.statement;
	}

	private void annotateByChunk(ChunkedSentence cs, boolean inbrackets) {
		this.inbrackets = inbrackets;
		while(cs.hasNext()){
			Chunk ck = cs.nextChunk();
			
			if(ck instanceof ChunkOR){
				Element last = this.latestelements.get(this.latestelements.size()-1);
				ck = cs.nextChunk();
				if(last.getName().compareTo("character")==0){
					String cname = last.getAttributeValue("name");
					if(!(ck instanceof ChunkSimpleCharacterState)){
						Element e = new Element("character");
						if(this.inbrackets){e.setAttribute("note", "in_bracket");}
						e.setAttribute("name", cname);
						String v = ck.toString(); //may be a character list
						if(v.indexOf("~list~")>=0){
							v = v.replaceFirst("\\w{2,}\\[.*?~list~","").replaceAll("punct", ",").replaceAll("~", " ");
						}
						v = v.replaceAll("(\\w\\[|\\]|\\{|\\}|\\(|\\)|<|>)", "");
						e.setAttribute("value", v);
						last.getParentElement().addContent(e);
					}
				}
				ArrayList<Element> e = new ArrayList<Element>();
				e.add(new Element("or"));
				updateLatestElements(e);
			}
				
			if(ck instanceof ChunkOrgan){//this is the subject of a segment. May contain multiple organs
				String content = ck.toString().replaceFirst("^z\\[", "").replaceFirst("\\]$", "");
				establishSubject(content);
			}else if(ck instanceof ChunkPrep){
				processPrep((ChunkPrep)ck);
			}else if(ck instanceof ChunkCHPP){//t[c/r[p/o]]
				String content = ck.toString().replaceFirst("^t\\[", "").replaceFirst("\\]$", "");
				processCHPP(content);
			}else if(ck instanceof ChunkNPList){
				establishSubject(ck.toString().replaceFirst("^l\\[", "").replaceFirst("\\]$", ""));				
			}else if(ck instanceof ChunkSimpleCharacterState){
				String content = ck.toString().replaceFirst("^a\\[", "").replaceFirst("\\]$", "");
				ArrayList<Element> parents = null;
				if(this.latestelements.get(this.latestelements.size()-1).getName().compareTo("structure") ==0){
					parents = this.latestelements;
				}else{
					parents = this.subjects;
				}
				ArrayList<Element> chars = processSimpleCharacterState(content, parents);
				updateLatestElements(chars);
			}else if(ck instanceof ChunkSL){//coloration[coloration-list-red-to-black]
				ArrayList<Element> chars = processCharacterList(ck.toString(), this.subjects);
				updateLatestElements(chars);
			}else if(ck instanceof ChunkBasedCount){
				ArrayList<Element> chars = annotateCount(this.subjects, ck.toString(), this.unassignedmodifiers);
				this.unassignedmodifiers = "";
				updateLatestElements(chars);
			}else if(ck instanceof ChunkComma){
				this.latestelements.add(new Element("comma"));
			}else if(ck instanceof ChunkVP){
				ArrayList<Element> es = processTVerb(ck.toString().replaceFirst("^b\\[", "").replaceFirst("\\]$", ""), this.subjects);
				updateLatestElements(es);
			}else if(ck instanceof  ChunkCount){
				ArrayList<Element> chars = annotateCount(this.subjects, ck.toString(), this.unassignedmodifiers);
				this.unassignedmodifiers = "";
				updateLatestElements(chars);
			}else if(ck instanceof ChunkValue){
				ArrayList<Element> chars = this.processSimpleCharacterState("size["+ck.toString().replaceAll("[{()}]", "")+"]", this.subjects);
				updateLatestElements(chars);
			}else if(ck instanceof ChunkComparativeValue){
				ArrayList<Element> chars = processComparativeValue(ck.toString().replaceFirst("^n\\[", "").replaceFirst("\\]", ""), this.subjects);
				updateLatestElements(chars);
			}else if(ck instanceof ChunkTHAN){
				ArrayList<Element> chars = processTHAN(ck.toString().replaceFirst("^n\\[", "").replaceFirst("\\]", ""), this.subjects);
				updateLatestElements(chars);
			}else if(ck instanceof ChunkBracketed){
				annotateByChunk(new ChunkedSentence(ck.getChunkedTokens(), ck.toString(), conn), true); //no need to updateLatestElements
				this.inbrackets =false;
			}else if(ck instanceof ChunkSBAR){
				ArrayList<Element> subjectscopy = this.subjects;
				if(this.latestelements.get(this.latestelements.size()-1).getName().compareTo("structure")==0){
					this.unassignedmodifiers = null;
					this.subjects = latest("structure", this.latestelements);
					annotateByChunk(new ChunkedSentence(ck.getChunkedTokens(), ck.toString(), conn), false); //no need to updateLatestElements
					this.subjects = subjectscopy;
					this.unassignedmodifiers = null;
				}else{
					System.err.println("parsing error in SBAR");
				}
			}
		}

	}
	/**
	 * 3 times n[...than...]
	   lengths 0.5�0.6+ times <bodies>
	   ca .3.5 times length of <throat>
       1�3 times {pinnately} {lobed}
       1�2 times shape[{shape~list~pinnately~lobed~or~divided}]
       4 times longer than wide
	 * @param replaceFirst
	 * @param subjects2
	 * @return
	 */
	private ArrayList<Element> processComparativeValue(String content,
			ArrayList<Element> parents) {
		String v = content.replaceAll("(?<="+ChunkedSentence.times+").*$", "");
		String n = content.replace(v, "").trim();
		if(n.indexOf("n[")>=0){//1.5�2.5 times n[{longer} than (throat)]
			content = "n["+content.replace("n[", "");
			this.processTHAN(content, parents);
		}else if(n.indexOf("o[")>=0){
			this.processSimpleCharacterState("a[size["+v.replace(" times", "")+"]]", parents);
			ArrayList<Element> structures = this.processObject(n);
			this.createRelationElements("times", parents, structures, this.unassignedmodifiers);
			this.unassignedmodifiers = null;
		}else if(n.indexOf("a[")>=0){ //characters:1�3 times {pinnately} {lobed}
			n = n.replaceFirst("a\\[", "").replaceFirst("\\]$", "");
			n = "a[m["+v+"] "+n+"]";
			this.processSimpleCharacterState(n, parents);
		}
		

		
		return null;
	}

	/**
	 * size[{longer}] constraint[than (object}]";
	 * @param replaceFirst
	 * @param subjects2
	 * @return
	 */
	private ArrayList<Element> processTHAN(String content,
			ArrayList<Element> parents) {
		ArrayList<Element> charas = new ArrayList<Element>();
		String[] parts = content.split("constraint\\[");
		if(content.startsWith("constraint")){
			charas = latest("character", this.latestelements);
		}else{
			charas = this.processSimpleCharacterState(parts[0].trim(), parents);
		}
		String object = null;
		ArrayList<Element> structures = null;
		if(parts.length>1 && parts[1].length()>0){
			if(parts[1].indexOf("(")>=0){
				while(parts[1].indexOf(" to ")>=0){
					object = parts[1].replaceFirst(".* to ", "");
				}
			}
			if(object != null){
				structures = this.processObject(object);
			}
			Iterator<Element> it = charas.iterator();
			while(it.hasNext()){
				Element e = it.next();
				this.addAttribute(e, "constraint", parts[1].replaceFirst("\\]$", ""));
				if(object!=null){
					this.addAttribute(e, "constraintids", this.listStructureIds(structures));//TODO: check: some constraints are without constraintids
				}
			}
			
		}
		if(structures != null){
			return structures;
		}else{
			return charas;
		}
	}

	private ArrayList<Element> latest(String name,
			ArrayList<Element> list) {
		ArrayList<Element> selected = new ArrayList<Element>();
		int size = list.size();
		for(int i = size-1; i>=0; i--){
			if(list.get(i).getName().compareTo(name) == 0){
				selected.add(list.get(i));
			}else{
				break;
			}
		}
		return selected;
	}

	/**
	 * 
	 * @param replaceFirst
	 */
	private void processChunkBracketed(String content) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 
	 * m[usually] v[comprising] o[a {surrounding} (involucre)]
	 * @param content
	 * @param parents
	 * @return
	 */
	private ArrayList<Element> processTVerb(String content,
			ArrayList<Element> parents) {
		ArrayList<Element> results = new ArrayList<Element>();
		String object = content.substring(content.indexOf("o["));
		String rest = content.replace(object, "").trim();
		String relation = rest.substring(rest.indexOf("v["));
		String modifier = rest.replace(relation, "").trim().replaceAll("(m\\[|\\])", "");
		
		ArrayList<Element> tostructures = this.processObject(object); //TODO: fix content is wrong. i8: o[a] architecture[surrounding (involucre)]
		results.addAll(tostructures);
		
		this.createRelationElements(relation.replaceAll("(v\\[|\\])", ""), this.subjects, tostructures, modifier);
		return results;
	}

	/**
	 * @param content: m[usually] coloration[dark brown]: there is only one character states and several modifiers
	 * @param parents: of the character states
	 */
	private ArrayList<Element> processSimpleCharacterState(String content,
			ArrayList<Element> parents) {
		ArrayList<Element> results = new ArrayList<Element>();
		String modifier = "";
		String character = "";
		String state = "";
		String[] tokens = content.split("\\]\\s*");
		for(int i = 0; i<tokens.length; i++){
			if(tokens[i].matches("^m\\[.*")){
				modifier += tokens[i]+" ";
			}else if(tokens[i].matches("^\\w+\\[.*")){
				String[] parts = tokens[i].split("\\[");
				character = parts[0];
				state = parts[1];
				modifier += "; ";
			}
		}
		modifier = modifier.replaceAll("m\\[", "").replaceAll("\\W+$", "").trim();
		if(state.length()>0){
			this.createCharacterElement(parents, results, modifier, state, character, "");
		}
		
		return results;
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
	
	private void reestablishSubject() {
		Iterator<Element> it = this.subjects.iterator();
		this.latestelements = new ArrayList<Element>();
		while(it.hasNext()){
			Element e = it.next();
			e.detach();
			this.statement.addContent(e);
			this.latestelements.add(e);
		}
	}
	/**
	 * TODO: {shape-list-usually-flat-to-convex-punct-sometimes-conic-or-columnar}
	 *       {pubescence-list-sometimes-bristly-or-hairy}
	 * @param content: pubescence[{pubescence-list-sometimes-bristly-or-hairy}]
	 * @param parents
	 * @return
	 */
	private ArrayList<Element> processCharacterList(String content,
			ArrayList<Element> parents) {
		ArrayList<Element> results= new ArrayList<Element>();
		String[] parts = content.split("\\["); 
		String cname = parts[0];
		String cvalue = parts[1].replaceFirst("\\{"+cname+"~list~", "").replaceFirst("\\W+$", "").replaceAll("~", " ").trim();
		String modifier="";
		if(cvalue.indexOf(" to ")>=0){
			createCharacterElement(parents, results, modifier, cvalue.replaceAll("punct", ","), cname, "range_value"); //add a general statement: coloration="red to brown"
		}
		String mall = "";
		boolean findm = false;
		//gather modifiers from the end of cvalues[i]. this modifier applies to all states
		do{
			findm = false;
			String last = cvalue.substring(cvalue.lastIndexOf(' ')+1);
			if(Utilities.lookupCharacter(last, conn, ChunkedSentence.characterhash)==null && Utilities.isAdv(last, ChunkedSentence.adverbs)){
				mall +=last+ " ";
				cvalue = cvalue.replaceFirst(last+"$", "").trim();
				findm = true;
			}
		}while(findm);
		
		String[] cvalues = cvalue.split("\\b(to|or|punct)\\b");//add individual values
		for(int i = 0; i<cvalues.length; i++){
			String state = cvalues[i].trim();//usually papillate to hirsute distally
			//gather modifiers from the beginning of cvalues[i]. a modifier takes effect for all state until a new modifier is found
			String m = "";
			do{
				findm = false;
				if(state.length()==0){
					break;
				}
				int end = state.indexOf(' ')== -1? state.length():state.indexOf(' ');
				String w = state.substring(0, end);
				if(Utilities.lookupCharacter(w, conn, ChunkedSentence.characterhash)==null && Utilities.isAdv(w, ChunkedSentence.adverbs)){
					m +=w+ " ";
					state = state.replaceFirst(w, "").trim();
					findm = true;
				}
			}while (findm);
			if(m.length()==0){
				state = (modifier+" "+mall+" "+state).trim(); //prefix the previous modifier 
			}else{
				modifier = m; //update modifier
				//cvalues[i] = (mall+" "+cvalues[i]).trim();
				state = (modifier+" "+mall+" "+state).trim(); //prefix the previous modifier 
			}
			results.addAll(this.processCharacterText(state.split("\\s+"), parents, cname));
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
	 * m[sometimes] v[subtended] r[p[by] o[(calyculi)]] 
	 * m[loosely loosely] architecture[arachnoid] r[p[at] o[m[distal] {end}]]
	 * 
	 * t[c[{sometimes} with (bases) {decurrent}] r[p[onto] o[(stems)]]]
	 * 
	 * nested:{often} {dispersed} r[p[with] o[aid  r[p[from] o[(pappi)]]]] 
	 * @param ck
	 */
	
	private void processCHPP(String content) {
		
		String c = content.substring(0, content.indexOf("r["));
		String r = content.replace(c, "");
		String p = r.substring(0, r.lastIndexOf("o["));//{often} {dispersed} r[p[with] o[aid  r[p[from] o[(pappi)]]]] 
		String o = r.replace(p, "");
		String[] mc = c.split("\\]\\s*");
		String m = "";
		c = "";
		for(int i =0; i<mc.length; i++){
			if(mc[i].startsWith("m[")){
				m += mc[i]+" ";
			}else if(mc[i].startsWith("c[")){
				c += mc[i]+" ";
			}
		}
		m = m.replaceAll("(m\\[|\\]|\\{|\\})", "").trim();
		c = c.replaceAll("(c\\[|\\]|\\{|\\})", "").trim(); //TODO: will this work for nested chuncks?
		p = p.replaceAll("(\\w\\[|\\])", "").trim();
		//c: {loosely} {arachnoid}
		String[] words = c.split("\\s+");
		if(Utilities.isVerb(words[words.length-1], ChunkedSentence.verbs) || p.compareTo("to")==0){//t[c[{connected}] r[p[by] o[{conspicuous} {arachnoid} <trichomes>]]] TODO: what if c was not included in this chunk?
			String relation = (c+" "+p).replaceAll("\\s+", " ");
			o = o.replaceAll("(o\\[|\\])", "");
			if(!o.endsWith(")") &&!o.endsWith("}")){ //1-5 series => 1-5 (series)
				String t = o.substring(o.lastIndexOf(' ')+1);
				o = o.replaceFirst(t+"$", "("+t)+")";
			}
			ArrayList<Element> structures = processObject("o["+o+"]");
			ArrayList<Element> entity1 = null;
			Element e = this.latestelements.get(this.latestelements.size()-1);
			if(e.getName().matches("("+this.delims+")") || e.getName().compareTo("character")==0 ){
				entity1 = this.subjects;
			}else{
				entity1 = (ArrayList<Element>)this.latestelements.clone();
				//entity1.remove(entity1.size()-1);
			}
			createRelationElements(relation, entity1, structures, m);
			updateLatestElements(structures);
		}else{//c: {loosely} {arachnoid} : should be m[loosly] architecture[arachnoid]
			//String[] tokens = c.replaceAll("[{}]", "").split("\\s+");
			//ArrayList<Element> charas = this.processCharacterText(tokens, this.subjects);
			ArrayList<Element> charas = this.processSimpleCharacterState(c, this.subjects);
			updateLatestElements(charas);
			processPrep(new ChunkPrep(r)); //not as a relation
		}
		
	}



	/**
	 * CK takes form of relation character/states [structures]?
	 * update this.latestElements with structures only.
	 * 
	 * nested: r[p[with] o[aid  r[p[from] o[(pappi)]]]]
	 * @param ck
	 * @param asrelation: if this PP should be treated as a relation
	 */
	private void processPrep(ChunkPrep ck) {
		String ckstring = ck.toString(); //r[{} {} p[of] o[.....]]
		String modifier = ckstring.substring(0, ckstring.indexOf("p[")).replaceFirst("^r\\[", "").replaceAll("[{}]", "").trim();
		String pp = ckstring.substring(ckstring.indexOf("p["), ckstring.lastIndexOf("] o[")).replaceAll("(\\w\\[|])", "");
		String object  = ckstring.substring(ckstring.lastIndexOf("o[")).replaceFirst("\\]+$", "")+"]";
		boolean lastIsStruct = false;
		boolean lastIsChara = false;
		boolean lastIsComma = false;
		Element lastelement = this.latestelements.get(this.latestelements.size()-1);
		if(lastelement.getName().compareTo("structure") == 0){//lastest element is a structure
			lastIsStruct = true;
		}else if(lastelement.getName().compareTo("character") == 0){
			lastIsChara = true;
		}else if(lastelement.getName().matches("("+this.delims+")")){
			lastIsComma = true;
		}
		ArrayList<Element> structures = new ArrayList<Element>();
		if(! object.matches(".*?\\}\\]+$")){ //contains organ: > or untagged: arrays
			//add () around the last word if it is bare
			if(object.matches(".*?[a-z]\\]+$")){
				int l = object.lastIndexOf(' ');
				if(l>0){
					String last = object.substring(l+1);
					object = object.replaceFirst(last+"$", "("+last.replaceFirst("\\]", ")]"));
				}else{//object= o[tendrils]
					object = object.replaceFirst("\\[", "[(").replaceFirst("\\]", ")]");
				}
			}
			
			structures = processObject(object);
			String base = "";
			if(object.matches("o?\\[*\\{*("+ChunkedSentence.basecounts+")\\b.*")){
				base = "each";
			}
			if(lastIsChara){
				addAttribute(lastelement, "constraint", (pp+" "+base+" "+listStructureNames(structures)).replaceAll("\\s+", " ")); //a, b, c
				addAttribute(lastelement, "constraintids", listStructureIds(structures));//1, 2, 3
				if(modifier.length()>0){
					addAttribute(lastelement, "modifier", modifier);
				}
			}else{
				ArrayList<Element> entity1 = null;
				if(lastIsStruct){
					entity1 = this.latestelements;
				}else{
					entity1 = this.subjects;
				}
				String relation = relationLabel(pp, entity1, structures);//determine the relation
				if(relation != null){
					createRelationElements(relation, entity1, structures, modifier);//relation elements not visible to outside 
				}
			}
			
		}else{ //contains no organ, e.g. "at flowering"
			//Element last = this.latestelements.get(this.latestelements.size()-1);
			if(lastIsStruct){
				addAttribute(lastelement, "constraint", ckstring.replaceAll("(\\w\\[|\\]|\\{|\\})", ""));
			}else if(lastIsChara){ //character element
				addAttribute(lastelement, "modifier", ckstring.replaceAll("(\\w\\[|\\]|\\{|\\})", ""));
			}
			//addPPAsAttributes(ckstring);
		}
		
		//bookkeeping: update this.latestElements: only structures are visible
		updateLatestElements(structures);
	}
	/**
	 * o[.........{m} {m} (o1) and {m} (o2)]
	 * o[each {bisexual} , architecture[{architecture-list-functionally-staminate-punct-or-pistillate}] (floret)]] ; 
	 * @param object
	 * @return
	 */
	private ArrayList<Element> processObject(String object) {
		ArrayList<Element> structures;
		String[] twoparts = separate(object);//find the organs in object o[.........{m} {m} (o1) and {m} (o2)]
		structures = createStructureElements(twoparts[1]);//to be added structures found in 2nd part, not rewrite this.latestelements yet
		if(twoparts[0].length()>0){
			String[] tokens = twoparts[0].split("\\s+");//add character elements
			processCharacterText(tokens, structures, null); //process part 1, which applies to all lateststructures, invisible
		}
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

	private void createRelationElements(String relation, ArrayList<Element> fromstructs, ArrayList<Element> tostructs, String modifier) {
		//add relation elements
		for(int i = 0; i<fromstructs.size(); i++){
			String o1id = fromstructs.get(i).getAttributeValue("id");
			for(int j = 0; j<tostructs.size(); j++){
				String o2id = tostructs.get(j).getAttributeValue("id");
				Element rela = new Element("relation");
				if(this.inbrackets){rela.setAttribute("in_bracket", "true");}
				rela.setAttribute("id", "r"+this.relationid);
				this.relationid++;
				rela.setAttribute("name", relation);
				rela.setAttribute("from", o1id);
				rela.setAttribute("to", o2id);
				if(modifier.length()>0){
					rela.setAttribute("modifier", modifier);
				}
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
		String v = e.getAttributeValue(attribute);
		if(v==null || !v.matches(".*?(^|; )"+value+"(;|$).*")){
			if(v !=null && v.trim().length() > 0){
				v += "; "+value;
			}else{
				v = value;
			}
			e.setAttribute(attribute, v);
		}
	}

	/**
	 * 
	 * @param organs
	 * @param organs2
	 * @return part-of or consists-of
	 * 
	 * involucre of => consists of
	 */
	private String differentiateOf(ArrayList<Element> organsbeforeOf,
			ArrayList<Element> organsafterOf) {
		String result = "part of";
		try{
			Statement stmt = conn.createStatement();
			
			for (int i = 0; i<organsbeforeOf.size(); i++){
				String b = organsbeforeOf.get(i).getAttributeValue("name");
				if(b.matches("("+ChunkedSentence.clusters+")")){
					result = "consists of";
					break;
				}
				for(int j = 0; j<organsafterOf.size(); j++){
					String a = organsafterOf.get(i).getAttributeValue("name");
					String pattern = a+"[ ]+of[ ]+[0-9]+.*"+b+"[,\\.]"; //consists-of
					ResultSet rs  = stmt.executeQuery("select * from "+this.tableprefix+"_sentence where originalsent rlike '"+pattern+"'" );
					if(rs.next()){
						result = "consists of";
						break;
					}
					rs.close();
				}
				stmt.close();
			}			
		}catch(Exception e){
			e.printStackTrace();
		}
		return result;
	}

	//separate o[......... {m} {m} (o1) and {m} (o2)] to two parts: the last part include all organ names
	private String[] separate(String object) {
		String[] twoparts  = new String[2];
		object = object.replaceFirst("^o\\[", "").replaceFirst("\\]$", "").replaceAll("<", "(").replaceAll(">", ")");
		String part2 = object.substring(object.indexOf("(")).trim();
		String part1 = object.replace(part2, "").trim();
		if(part1.length()>0){
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
			part1 = part1.replaceAll("\\s+", " ").trim();
			part2 = part2.replaceAll("\\s+", " ").trim();
		}
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
					o = organ[j]+" "+o;
					organ[j] = "";
				}else{
					break;
				}
			}
			o = o.replaceAll("(\\w\\[|\\]|\\(|\\))", "");
			//create element, 
			Element e = new Element("structure");
			if(this.inbrackets){e.setAttribute("note", "in_bracket");}
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
			processCharacterText(organ, list, null); //characters created here are final and all the structures will have, therefore they shall stay local and not visible from outside
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
	private ArrayList<Element> processCharacterText(String[] tokens, ArrayList<Element> parents, String character) {
		ArrayList<Element> results = new ArrayList<Element>();
		//determine characters and modifiers
		String modifiers = "";
		for(int j = 0; j <tokens.length; j++){
			if(tokens[j].trim().length()>0){
				if(tokens[j].indexOf("~list~")>=0){
					this.processCharacterList(tokens[j], parents);
				}else{
					String w = null;
					String chara= null;
					if(tokens[j].matches("\\w+\\[.*")){
						chara=tokens[j].substring(0, tokens[j].indexOf('['));
						w = tokens[j].replaceAll("(\\w+\\[|\\]|\\{|\\})", "");
					}else{
						w = tokens[j].replaceAll("(\\w+\\[|\\]|\\{|\\})", "");
						chara = Utilities.lookupCharacter(w, conn, ChunkedSentence.characterhash);
					}
					if(chara==null && Utilities.isAdv(w, ChunkedSentence.adverbs)){//TODO: can be made more efficient, since sometimes character is already given
						modifiers +=w+" ";
					}else if(w.matches(".*?\\d.*") && !w.matches(".*?[a-z].*")){//TODO: 2 times =>2-times?
						annotateCount(parents, w, modifiers);
						modifiers = "";
					}else{
						//String chara = MyPOSTagger.characterhash.get(w);
						if(chara != null){
							if(character!=null){
								chara = character;
							}
							createCharacterElement(parents, results,
									modifiers, w, chara, ""); //default type "" = individual vaues
							modifiers = "";
						}
					}
				}
				
			}
		}
		return results;
	}

	private String createCharacterElement(ArrayList<Element> parents,
			ArrayList<Element> results, String modifiers, String cvalue, String cname, String char_type) {
		Element character = new Element("character");
		if(this.inbrackets){character.setAttribute("note", "in_bracket");}
		if(char_type.length() >0){
			character.setAttribute("char_type", char_type);
		}
		character.setAttribute("name", cname);
		character.setAttribute("value", cvalue);
		boolean usedm = false;
		Iterator<Element> it = parents.iterator();
		while(it.hasNext()){
			Element e = it.next();
			character = (Element)character.clone();
			if(modifiers.trim().length() >0){
				addAttribute(character, "modifier", modifiers.trim()); //may not have
				usedm = true;
			}
			results.add(character); //add to results
			e.addContent(character);//add to e
		}
		if(usedm){
			modifiers = "";
		}
		return modifiers;
	}

	
	
	/**
	 * 
	 * @param parents
	 * @param w: m[usually] 0
	 * @param modifiers
	 * @return
	 */
	private ArrayList<Element> annotateCount(ArrayList<Element> parents, String w, String modifiers) {
		// TODO Auto-generated method stub
		String modifier = w.replaceFirst("\\d.*", "").trim();
		String number= w.replace(modifier, "").trim();
		ArrayList<Element> e = new ArrayList<Element>();
		Element count = new Element("character");
		if(this.inbrackets){count.setAttribute("in_bracket", "true");}
		count.setAttribute("name", "count");
		count.setAttribute("value", number);
		if(modifiers.length()>0){
			this.addAttribute(count, "modifier", modifiers);
		}
		if(modifier.length()>0){
			this.addAttribute(count, "modifier", modifier.replaceAll("(m\\[|\\])", ""));
		}
		Iterator<Element> it= parents.iterator();
		while(it.hasNext()){
			count = (Element)count.clone();
			e.add(count);
			it.next().addContent(count);
		}
		return e;
	}

	



	//if w has been seen used as a modifier to organ o
	private boolean isConstraint(String w, String o) {
		boolean result = false;
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select * from "+this.tableprefix+"_sentence where modifier ='"+w+"'");
			if(rs.next()){
				result = true;
			}
			rs.close();
			stmt.close();
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

	public void setInBrackets(boolean b){
		this.inbrackets = b;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
