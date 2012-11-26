 /* $Id$ */
/**
 * 
 */
package fna.charactermarkup;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.*;


/**
 * @author hongcui
 * fnaglossaryfixed: move verbs such as comprising from the glossary
 *
 */

@SuppressWarnings({ "unchecked", "unused","static-access" })

public class CharacterAnnotatorChunked {
	private static final Logger LOGGER = Logger.getLogger(CharacterAnnotatorChunked.class);
	private Element statement = null;
	//private ChunkedSentence cs = null;
	private static ArrayList<Element> subjects = new ArrayList<Element>();//static so a ditto sent can see the last subject
	private ArrayList<Element> latestelements = new ArrayList<Element>();//save the last set of elements added. independent from adding elements to <Statement>
	private String delims = "comma|or";
	private static int structid = 1;
	private static int relationid = 1;
	private String unassignedcharacter = null;
	//private String unassignedmodifiers = null; //holds modifiers that may be applied to the next chunk
	protected Connection conn = null;
	private String tableprefix = null;
	private String glosstable = null;
	//private boolean inbrackets = false; //repeated by the scope approach (dealing with parenthetical expressions)  
	private String text  = null;
	private String notInModifier = "a|an|the";
	private String lifestyle = "";
	private String characters = "";
	private boolean partofinference = false;
	private ArrayList<Element> pstructures = new ArrayList<Element>();//parent structures
	private ArrayList<Element> cstructures = new ArrayList<Element>();//children structures
	private boolean attachToLast = false; //this switch controls where a character will be attached to. "true": attach to last organ seen. "false":attach to the subject of a clause
	private boolean printAnnotation = true;
	private boolean debugNum = false;
	private boolean printComma = false;
	private boolean printAttach = false;
	private boolean printParenthetical = true;
	private boolean evaluation = true;
	private String sentsrc;
	private boolean nosubject;

	
	/**
	 * 
	 */
	public CharacterAnnotatorChunked(Connection conn, String tableprefix, String glosstable, boolean evaluation) {
		this.conn = conn;
		this.tableprefix = tableprefix;
		this.glosstable = glosstable;
		this.evaluation = evaluation;
		this.nosubject = false;
		if(this.evaluation) this.partofinference = false; //partofinterference causes huge number of "relations"
		try{
			//collect life_style terms
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select distinct term from "+this.glosstable+ " where category='life_style'");
			while(rs.next()){
				this.lifestyle += rs.getString(1)+"|";
			}
			this.lifestyle = lifestyle.replaceFirst("\\|$", "");
			
			rs = stmt.executeQuery("select distinct term from "+this.glosstable+ " where category='character'");
			while(rs.next()){
				this.characters += rs.getString(1)+"|";
			}
			this.characters = characters.replaceFirst("\\|$", "");
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
		}
	}
	/**
	 * reset annotator to process next description paragraph.
	 */
	public void reset(){
		this.subjects = new ArrayList<Element>();//static so a ditto sent can see the last subject
		this.latestelements = new ArrayList<Element>();//save the last set of elements added. independent from adding elements to <Statement>
		this.unassignedcharacter = null;
		//this.inbrackets = false;
		this.pstructures = new ArrayList<Element>();
		this.cstructures = new ArrayList<Element>();
		this.nosubject = false;

	}
	
	/**
	 * Labels the chunks into XML parts with tags.
	 * @param sentindex
	 * @param sentsrc
	 * @param cs
	 * @return
	 * @throws Exception
	 */
	public Element annotate(String sentindex, String sentsrc, ChunkedSentence cs) throws Exception{
		this.statement = new Element("statement");
		this.statement.setAttribute("id", sentindex);
		// 7-12-02 this.cs = cs;
		//this.text = cs.getOriginalText();
		this.text = cs.getText();
		this.sentsrc = sentsrc;
		Element text = new Element("text");//make <text> the first element in statement
		text.addContent(this.text);
		if(!this.evaluation) this.statement.addContent(text);
		//because sentence tags are not as reliable as chunkedsentence
		//no longer get subject text from cs
		//instead, annotate chunk by chunk
		Chunk ck = cs.nextChunk();
		if(ck instanceof ChunkOrgan){//start with a subject
			String content = ck.toString().replaceFirst("^z\\[", "").replaceFirst("\\]$", "");
			establishSubject(content/*, false*/, cs);// 7-12-02 add cs
			if(this.partofinference){
				this.cstructures.addAll(this.subjects);
			}
			cs.setInSegment(true);
			cs.setRightAfterSubject(true);
		}else{//not start with a subject
			if(ck instanceof ChunkPrep){	//check if the first chunk is a preposition chunk. If so, make the subjects and the latest elements from the previous sentence empty, and skip+ignore this chunk.
				//write code to make the latestelements nil
				this.latestelements = new ArrayList<Element>();
				String content = ck.toString();
				if(content.startsWith("r[p[with]")){ //r[p[with] o[1-2(-5)-(flowers)]] r[p[in] o[(axils)]] r[p[of] o[(bracts)]] . 
					//turn with-phrase to an organ chunk
					content = content.replaceFirst("^r\\[p\\[with\\] o\\[", "").replaceFirst("\\]+$", ""); //1-2(-5)-(flowers)
					establishSubject(content/*, false*/, cs);// 7-12-02 add cs
					if(this.partofinference){
						this.cstructures.addAll(this.subjects);
					}
					cs.setInSegment(true);
					cs.setRightAfterSubject(true);
				}else{// if (content.startsWith("r[p[without]")){ //r[p[without] o[{nodal} (spines)]] 
					//mostly r[p[at] o[(tips)]] r[p[of] o[(inflorescences)]] ; other prepchunks should be processed as well if they are not followed by an organ chunk.
					Chunk nextck = cs.nextChunk();
					if(!(nextck instanceof ChunkOrgan) && !(nextck instanceof ChunkNPList) && !(nextck instanceof ChunkNonSubjectOrgan)){ 
						reestablishSubject();
						cs.resetPointer(); //make sure the prep chunk is annotated later
						if(this.partofinference){
							this.cstructures.addAll(this.subjects);
						}
						cs.setInSegment(true);
						cs.setRightAfterSubject(true);
					}
				}
			}else{ //ck is a character
				reestablishSubject();	//creates a subject
				cs.setInSegment(true);
				cs.setRightAfterSubject(true);
				cs.resetPointer(); //make sure ck is annotated
			}
		}
		//annotateByChunk(cs, false);
		annotateByChunk(cs);
			
		/*
		String subject= cs.getSubjectText();
		//mohan code 11/4/2011
		if(subject==null && cs.getPointer()==0){
			Chunk ck = cs.nextChunk();
			cs.resetPointer();
			if(ck instanceof ChunkPrep){	//check if the first chunk is a preposition chunk. If so make the subjects and the latest elements from the previous sentence empty.
				//write code to make the latestelements nil
				this.latestelements = new ArrayList<Element>();
			}

			annotateByChunk(cs, false);
		}//end mohan code
		else if(subject.equals("measurements")){
			this.annotatedMeasurements(Utilities.handleBrackets(this.text));
		}else if(!subject.equals("ignore")){
			if(subject.equals("ditto")){
				reestablishSubject();	//creates a subject
			}else{
				establishSubject(subject);
				if(this.partofinference){
					this.pstructures.addAll(CharacterAnnotatorChunked.subjects);
				}
			}
			cs.setInSegment(true);
			cs.setRightAfterSubject(true);
			annotateByChunk(cs, false);
		}*/
		
		lifeStyle();
		if(!this.evaluation) mayBeSameRelation(cs);// 7-12-02 add cs
		if(this.partofinference){
			puncBasedPartOfRelation(cs); // 7-12-02 add cs
		}
		
		XMLOutputter xo = new XMLOutputter(Format.getPrettyFormat());
		if(printAnnotation){
			System.out.println();
			System.out.println(xo.outputString(this.statement));
		}
		return this.statement;
	}

	/**
	 * assuming subject organs of subsentences in a sentence are parts of the subject organ of the sentence
	 * this assumption seemed hold for FNA data.
	 */
	private void puncBasedPartOfRelation(ChunkedSentence cs) {// 7-12-02 add cs
		for(int p = 0; p < this.pstructures.size(); p++){
			for(int c = 0; c < this.cstructures.size(); c++){
				String pid = this.pstructures.get(p).getAttributeValue("id");
				String cid = this.cstructures.get(c).getAttributeValue("id");
				this.addRelation("part_of", "", false, cid, pid, false, "based_on_punctuation", cs); // 7-12-02 add cs
			}
		}		
	}

	/**
	 * re-annotate "trees" from structure to character lifestyle
	 */
	private void lifeStyle() {
		try{			
			//find life_style structures
			List<Element> structures = XPath.selectNodes(this.statement, ".//structure");
			Iterator<Element> it = structures.iterator();
			//Element structure = null;
			while(it.hasNext()){
				Element structure = it.next();
				String name = structure.getAttributeValue("name").trim();
				if(name.length()<=0) continue;
				if(lifestyle.matches(".*\\b"+name+"\\b.*")){
					if(structure.getAttribute("constraint_type") !=null)
						name = structure.getAttributeValue("constraint_type")+" "+name;
					if(structure.getAttribute("constraint_parent_organ") !=null)
						name = structure.getAttributeValue("constraint_parent_organ")+" "+name;
					Element wo = (Element)XPath.selectSingleNode(this.statement, ".//structure[@name='whole_organism']");
					if(wo!=null){
						List<Element> content = structure.getContent();
						structure.removeContent();
						/*for(int i = 0; i<content.size(); i++){
							Element e = content.get(i);
							e.detach();
							content.set(i, e);
						}*/
						wo.addContent(content);
						structure.detach();
						structure = wo;
					}
					structure.setAttribute("name", "whole_organism");
					Element ch = new Element("character");
					ch.setAttribute("name", "life_style");
					ch.setAttribute("value", name);
					structure.addContent(ch);
				}
				//keep each life_style structure
				/*if(lifestyle.matches(".*\\b"+name+"\\b.*")){
					if(structure.getAttribute("constraint") !=null)
						name = structure.getAttributeValue("constraint")+" "+name;
					structure.setAttribute("name", "whole_organism");
					Element ch = new Element("character");
					ch.setAttribute("name", "life_style");
					ch.setAttribute("value", name);
					structure.addContent(ch);
				}*/
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
		}
		
	}

	/**
	 * if there are structure with the same name and constraint but different ids
	 * add a relation 'may_be_the_same' among them, set symmetric="true"
	 */

	private void mayBeSameRelation(ChunkedSentence cs) {// 7-12-02 add cs
		try{
			List<Element> structures = XPath.selectNodes(this.statement, ".//structure");
			Hashtable<String, ArrayList<String>> names = new Hashtable<String, ArrayList<String>>();
			Iterator<Element> it = structures.iterator();
			//structure => ids hash
			while(it.hasNext()){
				Element structure = it.next();
				String name = structure.getAttributeValue("name");
				//one the two contraint types
				if(structure.getAttribute("constraint_type") !=null)
					name = structure.getAttributeValue("constraint_type")+" "+name;
				if(structure.getAttribute("constraint_parent_organ") !=null)
					name = structure.getAttributeValue("constraint_parent_organ")+" "+name;
				if(structure.getAttribute("constraint") !=null)
					name = structure.getAttributeValue("constraint")+" "+name;
				String id = structure.getAttributeValue("id");
				if(names.containsKey(name)){	
					names.get(name).add(id);//update the value for name 
					//names.put(name, names.get(name)); 
				}else{
					ArrayList<String> ids = new ArrayList<String>();
					ids.add(id);
					names.put(name, ids);
				}
			}
			//use the hash to create relations
			Enumeration<String> en = names.keys();
			while(en.hasMoreElements()){
				String name = en.nextElement();
				ArrayList<String> ids = names.get(name);
				if(ids.size()>1){
					for(int i = 0; i<ids.size(); i++){
						for(int j = i+1; j<ids.size(); j++){
							this.addRelation("may_be_the_same", "", true, ids.get(i), ids.get(j), false, "based_on_text", cs);// 7-12-02 add cs
						}
					}
				}				
			}
			
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
		}
		
	}

	//private void annotateByChunk(ChunkedSentence cs, boolean inbrackets) {
	private void annotateByChunk(ChunkedSentence cs) throws Exception{
		if(cs == null) return;
		//this.inbrackets = inbrackets;
		//process chunks one by one
		while(cs.hasNext()){
			Chunk ck = cs.nextChunk();//
						
			if(ck instanceof ChunkOR){
				int afterorindex = cs.getPointer();
				Element last = this.latestelements.get(this.latestelements.size()-1);
				ck = cs.nextChunk();
				if(ck instanceof ChunkEOS) break;
				if(ck!=null &&  last.getName().compareTo("character")==0){
					String cname = last.getAttributeValue("name");
					if(!(ck instanceof ChunkSimpleCharacterState) && !(ck instanceof ChunkNumericals)){//these cases can be handled by the normal annoation procedure
						Element e = new Element("character");
						//if(this.inbrackets){e.setAttribute("in_bracket", "true");}
						e.setAttribute("name", cname);
						String v = ck.toString(); //may be a character list
						if(v.length()>=1){//chunk contains text						
							if(v.indexOf("~list~")>=0){
								v = v.replaceFirst("\\w{2,}\\[.*?~list~","").replaceAll("punct", ",").replaceAll("~", " ");
							}
							v = v.replaceAll("(\\w\\[|\\]|\\{|\\}|\\(|\\)|<|>)", "");
							e.setAttribute("value", v);
							addClauseModifierConstraint(cs, e);
							this.addScopeConstraints(cs, e);
							last.getParentElement().addContent(e);
						}else{//chunk not contain text: or nearly so, or not, or throughout
							e = traceBack4(e, last, afterorindex, cs.getPointer(), cs); // 7-12-02 
							last.getParentElement().addContent(e);
						}
					}
				}else if(last.getName().compareTo("structure")==0 && (ck instanceof ChunkOrgan || ck instanceof ChunkNonSubjectOrgan || ck instanceof ChunkNPList)){
					annotateType(ck.toString(), last);
				}
				ArrayList<Element> e = new ArrayList<Element>();
				e.add(new Element("or"));
				updateLatestElements(e);
			}
				
			if(ck instanceof ChunkOrgan){//this is the subject of a segment. May contain multiple organs
				String content = ck.toString().replaceFirst("^z\\[", "").replaceFirst("\\]$", "");
				establishSubject(content/*, false*/, cs);// 7-12-02 add cs
				if(this.partofinference){
					this.cstructures.addAll(this.subjects);
				}
				cs.setInSegment(true);
				cs.setRightAfterSubject(true);
			}else if(ck instanceof ChunkNonSubjectOrgan){
				String content = ck.toString().replaceFirst("^u\\[", "").replaceFirst("\\]$", "");
				String structure = "";
				if(content.indexOf("o[")>=0){
					//String m = content.substring(0, content.indexOf("o[")).replaceAll("m\\[", "{").replaceAll("\\]", "}");
					String beforeo = content.substring(0, content.indexOf("o[")).replaceAll("m\\[", "{").replaceAll("\\] ", "} ");//mohan
					String o = content.substring(content.indexOf("o[")).replaceAll("o\\[", "").replaceAll("\\]", "");
					structure = beforeo+o;
				}else{
					structure = content;
				}
				ArrayList<Element> structures = createStructureElements(structure/*, false*/, cs);// 7-12-02 add cs
				//deal with "type" case
				Element laste = this.latestelements.get(this.latestelements.size()-1);
				if(laste.getName().equals("structure")){//u[m[sometimes] woody o[(caudices)]] after z[{underground} (stems)], create a type for the subject
					annotateType(content, laste);
				}else{
					updateLatestElements(structures);
				}
				
			}else if(ck instanceof ChunkPrep){
				//r[p[without] o[-LRB-/-LRB- or r[p[with]] very {obscure} -RRB-/-RRB- ({ridge})]]
				String ckstring = ck.toString(); 
				if(ckstring.matches(".* o\\[-[LR][RS]B-.*")){
					//if there are > 1 preps, 
					//	if they share the same object
					//      create alternative expression for relation name
					//  else //
					//	    create multiple chunks
					
					//fetch content in brackets
					String bracketedprep = ckstring.replaceFirst(".*?(?=-[LR][RS]B-)", "");
					int o = bracketedprep.lastIndexOf("-RRB-/-RRB-") >= 0 ? bracketedprep.lastIndexOf("-RRB-/-RRB-") : bracketedprep.lastIndexOf("-RSB-/-RSB-");
					String object = bracketedprep.substring(o+11).trim();
					bracketedprep = bracketedprep.substring(0, o+11).trim();
					ckstring = ckstring.replace(bracketedprep, "").replaceFirst("(?<= o\\[)\\s+", "");
					boolean sameobject = false;
					if(bracketedprep.matches(".*?\\b("+ChunkedSentence.prepositions+")\\s*-[LR][RS]B-/-[LR][RS]B-\\W*")){
						sameobject = true;
					}
					ArrayList<Element> copy = this.latestelements;
					processPrep(ckstring, cs); // 7-12-02 add cs; one prep chunk
					if(sameobject){
						Element relation = this.latestelements.get(this.latestelements.size()-1);
						bracketedprep = bracketedprep.replaceAll("(\\w\\[|\\]|\\{|\\})", "").trim();
						this.addAttribute(relation, "alter_name", bracketedprep);
					}else{
						this.latestelements = copy;
						//-LRB-/-LRB- or r[p[with]] very {obscure} -RRB-/-RRB-
						//-LRB-/-LRB- or with very {obscure} -RRB-/-RRB-, reformat this as the above
						if(! bracketedprep.matches(".*\\br\\[p\\[("+ChunkedSentence.prepositions+")\\]\\].*")){
							bracketedprep = (" "+bracketedprep+" ").replaceFirst(" (?=("+ChunkedSentence.prepositions+")\\b)", " r[p[").
									replaceFirst("(?<=("+ChunkedSentence.prepositions+")) ", "]] ").trim();
						}
						bracketedprep = bracketedprep.replaceFirst(".*\\b(?=r\\[)", "").replaceAll("-[LR][RS]B-/-[LR][RS]B-", "").trim();
						bracketedprep = bracketedprep.replaceFirst("] ", " o[")+" "+object;
						processChunkBracketed(new ChunkBracketed(bracketedprep), cs); //prep chunk in brackets
					}
				}else{
					processPrep(ckstring, cs); // 7-12-02 add cs
				}				
			}else if(ck instanceof ChunkCHPP){//t[c/r[p/o]] this chunk is converted internally and not shown in the parsing output
				String content = ck.toString().replaceFirst("^t\\[", "").replaceFirst("\\]$", "");
				//c[{proximal}] r[p[to] o[-LRB-/-LRB- i . e . , outside-of or {abaxial} to -RRB-/-RRB- the (florets)]]
				if(content.matches(".* o\\[-[LR][RS]B-.*")){
					//if there are > 1 preps, 
					//	if they share the same object
					//      create alternative expression for relation name
					//  else 
					//	    create multiple chunks
					
					//fetch content in brackets
					String bracketedprep = content.replaceFirst(".*?(?=-[LR][RS]B-)", "");
					int o = bracketedprep.lastIndexOf("-RRB-/-RRB-") >= 0 ? bracketedprep.lastIndexOf("-RRB-/-RRB-") : bracketedprep.lastIndexOf("-RSB-/-RSB-");
					String object = bracketedprep.substring(o+11).trim();
					bracketedprep = bracketedprep.substring(0, o+11).trim();
					content = content.replace(bracketedprep, "").replaceFirst("(?<= o\\[)\\s+", "");
					boolean sameobject = false;
					if(bracketedprep.matches(".*?\\b("+ChunkedSentence.prepositions+")\\s*-[LR][RS]B-/-[LR][RS]B-\\W*")){
						sameobject = true;
					}
					ArrayList<Element> copy = this.latestelements;
					ArrayList<Element> relations = processCHPP(content, cs); // 7-12-02 add cs; one prep chunk
					if(sameobject){
						Element relation = relations.get(relations.size()-1);
						bracketedprep = bracketedprep.replaceAll("(\\w\\[|\\]|\\{|\\})", "").replaceAll("-[LR][RS]B-/-[LR][RS]B-", "").trim();
						this.addAttribute(relation, "alter_name", bracketedprep);
					}else{
						//c[{proximal}] r[p[to] o[-LRB-/-LRB- i . e . , outside-of or {abaxial} to -RRB-/-RRB- the (florets)]]
						this.latestelements = copy;
						if(! bracketedprep.matches(".*\\br\\[p\\[("+ChunkedSentence.prepositions+")\\]\\].*")){
							bracketedprep = (" "+bracketedprep+" ").replaceFirst(" (?=("+ChunkedSentence.prepositions+")\\b)", " r[p[").
									replaceFirst("(?<=("+ChunkedSentence.prepositions+")) ", "]] ").trim();
						}
						//the word before r[p[ is tagged c[]
						if(!bracketedprep.startsWith("r[p[")){
							bracketedprep = ("c["+bracketedprep).replaceFirst("\\s+r\\[", "] r[").trim();
						}
						bracketedprep = bracketedprep.replaceFirst(".*\\b(?=r\\[)", "").replaceAll("-[LR][RS]B-/-[LR][RS]B-", "").trim();
						bracketedprep = bracketedprep.replaceFirst("] ", " o[")+" "+object;
						if(bracketedprep.startsWith("c[") || bracketedprep.indexOf(" c[")> 0) bracketedprep = "t["+bracketedprep+"]";
						processChunkBracketed(new ChunkBracketed(bracketedprep), cs); //prep chunk in brackets
					}
				}else{
					processCHPP(content, cs);// 7-12-02 add cs
				}
			}else if(ck instanceof ChunkNPList){
				establishSubject(ck.toString().replaceFirst("^l\\[", "").replaceFirst("\\]$", "")/*, false*/, cs);// 7-12-02 add cs				
			}else if(ck instanceof ChunkSimpleCharacterState){
				String content = ck.toString().replaceFirst("^a\\[", "").replaceFirst("\\]$", "");
				//ArrayList<Element> chars = processSimpleCharacterState(content, lastStructures());//with teeth closely spaced
				//ArrayList<Element> parents = this.attachToLast? lastStructures() : subjects;
				if(this.latestelements.size() == 0){//other Chunk types may also need this place holder
					this.establishSubject("(unknown_subject)", cs); //put in a place holder
				}
				ArrayList<Element> parents = lastStructures();
				//if latestelements is empty, then subjects is empty too, indicating a problem
				ArrayList<Element> chars = processSimpleCharacterState(content, parents, cs);// 7-12-02 add cs//apices of basal leaves spread 
				//if(printAttach && subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name")) != 0){
				//	System.out.println(content + " attached to "+parents.get(0).getAttributeValue("name"));
				//}
				updateLatestElements(chars);
			}else if(ck instanceof ChunkSL){//coloration[coloration-list-red-to-black]
				//ArrayList<Element> parents = this.attachToLast? lastStructures() : subjects;
				//if(printAttach && subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name")) != 0){
				//	System.out.println(ck.toString() + " attached to "+parents.get(0).getAttributeValue("name"));
				//}
				ArrayList<Element> chars = processCharacterList(ck.toString(), this.subjects, cs); // 7-12-02 add cs
				updateLatestElements(chars);
			}else if(ck instanceof ChunkComma){
				this.latestelements.add(new Element("comma"));
			}else if(ck instanceof ChunkVP){
				ArrayList<Element> parents = this.attachToLast? lastStructures() : subjects;
				/*if(printAttach && subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name")) != 0){
					System.out.println(ck.toString() + " attached to "+parents.get(0).getAttributeValue("name"));
				}*/
				ArrayList<Element> es = processTVerb(ck.toString().replaceFirst("^b\\[", "").replaceFirst("\\]$", ""), parents, cs); // 7-12-02 add cs
				//ArrayList<Element> es = processTVerb(ck.toString().replaceFirst("^b\\[", "").replaceFirst("\\]$", ""), CharacterAnnotatorChunked.subjects);
				updateLatestElements(es);
			}else if(ck instanceof ChunkComparativeValue){
				//ArrayList<Element> chars = processComparativeValue(ck.toString().replaceAll("–", "-"), lastStructures());
				String content = ck.toString();
				ArrayList<Element> parents = this.attachToLast? lastStructures() : subjects;
				if(printAttach && subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name")) != 0){
					System.out.println(content + " attached to "+parents.get(0).getAttributeValue("name"));
				}
				ArrayList<Element> chars = processComparativeValue(content.replaceAll("–", "-"), lastStructures(), cs);// 7-12-02 add cs
				updateLatestElements(chars);
			}else if(ck instanceof ChunkRatio){
				//ArrayList<Element> chars = annotateNumericals(ck.toString(), "lwratio", "", lastStructures());
				String content = ck.toString();
				ArrayList<Element> parents = this.attachToLast? lastStructures() : subjects;
				if(printAttach && subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name")) != 0){
					System.out.println(content + " attached to "+parents.get(0).getAttributeValue("name"));
				}
				ArrayList<Element> chars = annotateNumericals(content, "lwratio", "", lastStructures(), false, cs);//added cs
				updateLatestElements(chars);
			}else if(ck instanceof ChunkArea){
				//ArrayList<Element> chars = annotateNumericals(ck.toString(), "area", "", lastStructures());
				String content = ck.toString();
				ArrayList<Element> parents = this.attachToLast? lastStructures() : subjects;
				if(printAttach && subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name")) != 0){
					System.out.println(content + " attached to "+ parents.get(0).getAttributeValue("name"));
				}
				ArrayList<Element> chars = annotateNumericals(content, "area", "", lastStructures(), false, cs); //added cs
				updateLatestElements(chars);
			}else if(ck instanceof  ChunkNumericals){
				//** find parents, modifiers
				//TODO: check the use of [ and ( in extreme values
				//ArrayList<Element> parents = lastStructures();
				String text = ck.toString().replaceAll("–", "-");
				boolean resetfrom = false;
				if(text.matches(".*\\bto \\d.*")){ //m[mostly] to 6 m ==> m[mostly] 0-6 m
					text = text.replaceFirst("to\\s+", "0-");
					resetfrom = true;
				}
				ArrayList<Element> parents = this.attachToLast? lastStructures() : subjects;
				if(printAttach && subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name")) != 0){
					System.out.println(text + " attached to "+parents.get(0).getAttributeValue("name"));
				}				
				if(debugNum){
					System.out.println();
					System.out.println(">>>>>>>>>>>>>"+text);
				}
				String modifier1 = "";//m[mostly] [4-]8–12[-19] mm m[distally]; m[usually] 1.5-2 times n[size[{longer} than {wide}]]:consider a constraint
				String modifier2 = "";
				modifier1 = text.replaceFirst("\\[?\\d.*$", "");
				String rest = text.replace(modifier1, "");
				modifier1 =modifier1.replaceAll("(\\w\\[|\\]|\\{|\\})", "").trim();
				modifier2 = rest.replaceFirst(".*?(\\d|\\[|\\+|\\-|\\]|%|\\s|"+ChunkedSentence.units+")+\\s?(?=[a-z]|$)", "");//4-5[+]
				String content = rest.replace(modifier2, "").replaceAll("(\\{|\\})", "").trim();
				modifier2 = modifier2.replaceAll("(\\w+\\[|\\]|\\{|\\})", "").trim();
				ArrayList<Element> chars = annotateNumericals(content, text.indexOf("size")>=0 || content.indexOf('/')>0 || content.indexOf('%')>0 || content.indexOf('.')>0? "size" : null, (modifier1+";"+modifier2).replaceAll("(^\\W|\\W$)", ""), lastStructures(), resetfrom, cs); //added cs
				updateLatestElements(chars);
			}else if(ck instanceof ChunkTHAN){
				ArrayList<Element> chars = processTHAN(ck.toString().replaceFirst("^n\\[", "").replaceFirst("\\]$", ""), this.subjects, cs); // 7-12-02 add cs
				updateLatestElements(chars);
			}else if(ck instanceof ChunkTHANC){//n[(longer) than {wide}] .
				ArrayList<Element> chars = processTHAN(ck.toString().replaceFirst("^n\\[", "").replaceFirst("\\]$", ""), this.subjects, cs); // 7-12-02 add cs
				updateLatestElements(chars);
			}else if(ck instanceof ChunkChrom){
				Element structure = new Element("structure");
				this.addAttribute(structure, "name", "chromosome");
				this.addAttribute(structure, "id", "o"+this.structid);
				this.structid++;
				ArrayList<Element> parents = new ArrayList<Element>();
				parents.add(structure); //create chromosome element
				
				//String content = ck.toString().replaceAll("[^\\d()\\[\\],+ -]", "").trim();
				String content = ck.toString();
				boolean inbracket = false;
				String note = null;
				if(content.matches("^[xgq]\\[.*")){ //the entire statement is in brackets
					content = content.replaceAll("(^\\w\\[|\\]$)", "");//
					inbracket = true;
					String[] twoparts = content.split("[,?](?!\\d)");
					content = twoparts[0];
					if(twoparts.length > 1) note = twoparts[1].replaceAll("(\\w\\[|\\]|\\{|\\}|\\(|\\))", "").replaceAll("\\s+", " ").trim();
				}
				String[] segs = content.split("\\s*[,?]\\s*"); //23 -LRB-/-LRB- japan -RRB-/-RRB-, 30 (china)
				ArrayList<Element> precharas = null;
				for(String seg : segs){
					String bracketed = "";
					if(seg.matches(".*(-LRB-/-LRB-|[xgq]\\[).*")) bracketed = seg.replaceAll(".*?(?=(-LRB-/-LRB-|[xgq]\\[))", "");
					String count = seg.replace(bracketed, "").trim();
					ArrayList<Element> charas = null;
					if(count.matches(".*?\\d+.*")){ //bracketed="", count=seg as for content="26 x[(russia , as s. glomerata)]"
						charas = this.annotateNumericals(count, "count", "", parents, false, cs); //added cs, establish a new count
						precharas = charas;
					}else if(precharas !=null){
						charas = precharas;
						bracketed = seg;
					}
					String attname = "geographical_constraint";
					if(bracketed.startsWith("x[") || bracketed.indexOf(" x[")>=0 || bracketed.matches(".*\\b[a-z]\\]*\\s*\\.\\s*[a-z]+.*")) attname = "taxon_constraint";
					bracketed = bracketed.replaceAll("-[LR][RS]B-/-[LR][RS]B-", "").replaceAll("(\\w+\\[|\\]|\\}|\\{)", "").replaceAll("\\s+", " ").trim();
					if(bracketed.length() > 0 || note!=null){
						Attribute scp = null;
						if(this.text.contains(bracketed) && ! attname.equals("taxon_constraint")){ //bracketed text is not proper noun
							for(Element chara : charas){
								this.addAttribute(chara, "other_constraint", bracketed);
								if(note!=null) this.addAttribute(chara, "note", note);
								if(inbracket) this.addAttribute(chara, "in_bracket", "true");
							}
						}else{
							for(Element chara : charas){
								this.addAttribute(chara, attname, bracketed);
								if(note!=null) this.addAttribute(chara, "note", note);
								if(inbracket) this.addAttribute(chara, "in_bracket", "true");
							}
						}						
					}
				}
				//this.annotateNumericals(content, "count", "", parents, false, cs); //added cs
				addClauseModifierConstraint(cs, structure);
				this.addScopeConstraints(cs, structure);
				this.statement.addContent(structure);
			}else if(ck instanceof ChunkValuePercentage || ck instanceof ChunkValueDegree){
				String content = ck.toString();
				Element lastelement = this.latestelements.get(this.latestelements.size()-1);
				if(lastelement!=null && lastelement.getName().compareTo("character") == 0){
					this.addAttribute(lastelement, "modifier", content);
				}else{
					cs.unassignedmodifier = content;
				}
			}else if(ck instanceof ChunkSBAR){
				ArrayList<Element> subjectscopy = this.subjects;
				if(this.latestelements.get(this.latestelements.size()-1).getName().compareTo("structure")==0){
					this.subjects = latest("structure", this.latestelements);
				}else{
					int p = cs.getPointer()-2;
					String last = ""; //the chunk before ck??
					if(p>0){
						do{
							last = cs.getTokenAt(p--);
						}while(!last.matches(".*?\\S.*"));
					}
					String constraintId = null;
					if(last.matches(".*?\\)\\]+")){
						constraintId = "o"+(this.structid-1);
						try{
							Element laststruct = (Element)XPath.selectSingleNode(this.statement, ".//structure[@id='"+constraintId+"']");
							ArrayList<Element> temp = new ArrayList<Element>();
							temp.add(laststruct);
							this.subjects = temp;
						}catch(Exception e){
							StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
						}
					}else{
						//do nothing
						System.err.println("no structure element found for the SBARChunk, use subjects instead ");
						//this only works for situations where states before subjects got reintroduced after subjects in skiplead
						//this will not work for misidentified nouns before "that/which" statements, in "of/among which", and other cases
					}
				}
				String connector = ck.toString().substring(0,ck.toString().indexOf(" "));
				String content = ck.toString().substring(ck.toString().indexOf(" ")+1);
				ChunkedSentence newcs = new ChunkedSentence(ck.getChunkedTokens(), content, conn, glosstable, this.tableprefix);
				if(connector.compareTo("when")==0){//rewrite content and its chunkedTokens
					Pattern p = Pattern.compile("[\\.,:;]");
					Matcher m = p.matcher(ck.toString());
					int end = 0;
					if(m.find()){
						end = m.start();
					}
					//int end = ck.toString().indexOf(",") > 0? ck.toString().indexOf(",") : ck.toString().indexOf(".");
					String modifier = ck.toString().substring(0, end).trim();//when mature, 
					content = ck.toString().substring(end).replaceAll("^\\W+", "").trim();
					if(content.length()>0){
						ck.setChunkedTokens(Utilities.breakText(content));					
						newcs = new ChunkedSentence(ck.getChunkedTokens(), content, conn, glosstable, this.tableprefix);
					}else{
						//{aromatic} ( when bruised ).					
						newcs = null;
					}
					//attach modifier to the last characters
					if(this.latestelements.get(this.latestelements.size()-1).getName().compareTo("character")==0){
						Iterator<Element> it = this.latestelements.iterator();
						while(it.hasNext()){
							this.addAttribute(it.next(), "modifier", modifier);
						}
					}else{ 
						if(newcs!=null) newcs.unassignedmodifier = "m["+modifier+"]";//this when clause is a modifier for the subclause
						else{
							if(this.latestelements.get(this.latestelements.size()-1).getName().compareTo("comma")==0){
								this.latestelements.remove(this.latestelements.size()-1); //remove comma, so what follows when-clause may refer to the structure mentioned before as in <apex> r[p[of] o[(scape)]] , s[when laid {straight} {back} r[p[from] o[its (insertion)]] ,] just touches the {midpoint} r[p[of] o[the {posterior} (margin)]] r[p[in] o[(fullface)]] {view} ; 
							}
							cs.unassignedmodifier = "m["+modifier.replaceAll("(\\w+\\[|\\]|\\(|\\)|\\{|\\})", "")+"]";
						}
					}
				}

				if(connector.compareTo("where") == 0){
					//retrieve the last non-comma, non-empty chunk					
					int p = cs.getPointer()-2;
					if(p>0){
						String last = "";
						do{
							last = cs.getTokenAt(p--);
						}while(last!=null && !last.matches(".*?\\w.*"));
						String constraintId = null;
						if(last!=null && last.matches(".*?\\)\\]+")) constraintId = "o"+(this.structid-1);				
						if(last!=null) cs.setClauseModifierConstraint(last.replaceAll("(\\w+\\[|\\]|\\{|\\}|\\)|\\()", ""), constraintId);
					}
				}
				if(newcs!=null){
					newcs.setInSegment(true);
					//annotateByChunk(newcs, false); //no need to updateLatestElements	
					annotateByChunk(newcs); //no need to updateLatestElements	
				}
				this.subjects = subjectscopy;//return to original status
				cs.setClauseModifierConstraint(null, null); //return to original status
				//this.unassignedmodifiers = null;
			}else if(ck instanceof ChunkBracketed){
				processChunkBracketed(ck, cs);
			}else if(ck instanceof ChunkScopeTaxa){//the taxon part is in the last ()
				processChunkScopeTaxa(ck, cs);
			}else if(ck instanceof ChunkScopeGeo){
				Attribute geo = new Attribute("geographical_constraint", "outside of North America");//create a constraint attribute
				annotateScopeChunk(ck, geo, cs);
			}else if(ck instanceof ChunkScopeParallelism){
				Attribute parallelism = new Attribute("parallelism_constraint", "possible");//create a constraint attribute
				annotateScopeChunk(ck, parallelism, cs);
			}else if(ck instanceof ChunkChrom){
				String content = ck.toString().replaceAll("[^\\d()\\[\\],+ -]", "").trim();
				//Element structure = new Element("chromosome");
				Element structure = new Element("structure");
				this.addAttribute(structure, "name", "chromosome");
				this.addAttribute(structure, "id", "o"+this.structid);
				this.structid++;
				ArrayList<Element> list = new ArrayList<Element>();
				list.add(structure);
				this.annotateNumericals(content, "count", "", list, false, cs);
				/*for(int i = 0; i<counts.length; i++){
					Element character = new Element("character");
					this.addAttribute(character, "count", counts[i]);
					structure.addContent(character);
				}*/
				addClauseModifierConstraint(cs, structure);
				this.statement.addContent(structure);
			}else if(ck instanceof ChunkValuePercentage || ck instanceof ChunkValueDegree){
				String content = ck.toString();
				Element lastelement = this.latestelements.get(this.latestelements.size()-1);
				if(lastelement!=null && lastelement.getName().compareTo("character") == 0){
					this.addAttribute(lastelement, "modifier", content);
				}else{
					cs.unassignedmodifier = content;
				}				
			}else if(ck instanceof ChunkEOS || ck instanceof ChunkEOL){
				if(cs.unassignedmodifier!=null && cs.unassignedmodifier.length()>0){
					Element lastelement = this.latestelements.get(this.latestelements.size()-1);
					if(lastelement.getName().compareTo("structure") == 0){
						Iterator<Element> it = this.latestelements.iterator();
						while(it.hasNext()){
							String sid = it.next().getAttributeValue("id");
							try{
								List<Element> relations = XPath.selectNodes(this.statement, ".//relation[@to='"+sid+"']");
								Iterator<Element> rit = relations.iterator();
								int greatestid = 0;
								Element relation = null;
								while(rit.hasNext()){
									Element r = rit.next();
									int rid = Integer.parseInt(r.getAttributeValue("id").replaceFirst("r", ""));
									if(rid>greatestid){
										greatestid = rid;
										relation = r;
									}
								}
								if(relation !=null)	 this.addAttribute(relation, "modifier", cs.unassignedmodifier);
								//TODO: otherwise, categorize modifier and create a character for the structure e.g.{thin} {dorsal} {median} <septum> {centrally} only ;
							}catch(Exception e){
							
								StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
							}
						}
						
					}else if(lastelement.getName().compareTo("character") == 0){
						Iterator<Element> it = this.latestelements.iterator();
						while(it.hasNext()){
							this.addAttribute(it.next(), "modifier", cs.unassignedmodifier);
						}
					}
				}
				this.attachToLast = false;
				cs.unassignedmodifier = null;
				this.unassignedcharacter = null;
			}
		}

	}
	private void processChunkBracketed(Chunk ck, ChunkedSentence cs) throws Exception{
		//ChunkedSentence newcs = new ChunkedSentence(ck.getChunkedTokens() , ck.toString(), conn, glosstable, this.tableprefix);
		//newcs.setInSegment(true);
		//annotateByChunk(newcs, true); //no need to updateLatestElements
		//this.inbrackets =false;
		String content = ck.toString().replaceFirst("\\s+\\W+$", "").trim(); //removes " ." from "abc ."
		//deal with single phrase brackets here: position and organ
		boolean singlephrase = false;
		boolean organconstraint = false;
		String token = content.lastIndexOf(" ") < 0? content : content.substring(content.lastIndexOf(" ")).trim();
		int size = content.split("\\s+").length;
		if(size<=3 && (token.endsWith(">")||token.endsWith(")")) && token.indexOf("]")<0){
			singlephrase = true; //organ
		}
		if(size<=5 && content.matches("(r\\[p\\[)?except.*") && content.matches(".*\\)\\]*")){
			organconstraint = true; //r[p[except] o[r[p[in] o[{sterile} (fruits)]]]]
		}
		
		if(! singlephrase && size<=3){
			String[] charainfo = Utilities.lookupCharacter(token, conn, ChunkedSentence.characterhash, glosstable, tableprefix);
			if(charainfo != null && charainfo[0].contains("position")){
				singlephrase = true; //position
			}
		}
		Element lastelement = this.latestelements.get(this.latestelements.size()-1);
		if(organconstraint){//must be associated with a character or a relation
			//z[{basal} (tubercles)] not markedly n[{longer} than {distal} (ones)] -LRB-/-LRB- r[p[except] o[r[p[in] o[{sterile} (fruits)]]]] -RRB-/-RRB- ; 
			ArrayList<Element> targets  = new ArrayList<Element>();
			if(lastelement!=null && lastelement.getName().compareTo("character") == 0){ //character or relation
				targets.add(lastelement);
			}else{ //find the character associated with the lastelement
				targets.addAll(charrel(lastelement));
			}
			for(Element target: targets) this.addAttribute(target, "organ_constraint", content.replaceAll("(\\w\\[|\\]|\\{|\\}|\\(|\\))", "").replaceAll("\\s+", " ").trim());
		}else if(singlephrase){
			if(lastelement!=null && lastelement.getName().compareTo("character") == 0){ //character or relation
				this.addAttribute(lastelement, "organ_constraint", content.replaceAll("(\\w\\[|\\]|\\{|\\}|\\(|\\))", "").replaceAll("\\s+", " ").trim());
			}else{
				this.addAttribute(lastelement, "alter_name", content.replaceAll("(\\w\\[|\\]|\\{|\\}|\\(|\\))", "").replaceAll("\\s+", " ").trim());
			}
		}else{
			Attribute bracket = new Attribute("in_brackets", "true");//create a constraint attribute
			annotateScopeChunk(ck, bracket, cs);
		}
	}
	
	//find the character/relation that is associated with structure 
	private List<Element> charrel(Element structure) throws Exception {
		// TODO Auto-generated method stub
		String id = structure.getAttributeValue("id");
		List<Element> targets = XPath.selectNodes(this.statement, ".//*[@constraintid='"+id+"']");
		targets.addAll(XPath.selectNodes(this.statement, ".//*[@from='"+id+"']")); //part of relation
		targets.addAll(XPath.selectNodes(this.statement, ".//*[@to='"+id+"']")); //part of relation
		return targets;
	}
	private void processChunkScopeTaxa(Chunk ck, ChunkedSentence cs) throws Exception {
		String content = ck.toString().replaceAll("(^x\\[|\\]$)", "");
		//r[p[except] o[r[p[in] (m. parvifolia and m. bostockii) => (except in m. parvifolia and m. bostockii)
		String taxon = content.substring(content.lastIndexOf("(")).trim();
		content = content.substring(0, content.lastIndexOf("(")).trim();
		//content: r[p[except] o[r[p[in] 
		if(!content.contains("{") && !content.contains("(")){
			taxon = "("+content.replaceAll("(\\w\\[|\\])", "").trim()+" "+taxon.replaceFirst("\\(", "");
			content = "";
		}	
		if(content.length()>0){
			ck.setText(content);
			Attribute taxonscp = new Attribute("taxon_constraint", taxon);//create a constraint attribute
			annotateScopeChunk(ck, taxonscp, cs);
		}else{
			Element lastelement = this.latestelements.get(this.latestelements.size()-1);
			if(lastelement!=null && lastelement.getName().compareTo("structure") != 0){ //character or relation
				this.addAttribute(lastelement, "taxon_constraint", taxon);
			}else{ //lastelement is a structure, find the relation/character associated with the structure
				List<Element> targets = this.charrel(lastelement);
				for(Element target: targets){
					this.addAttribute(target, "taxon_constraint", taxon);
				}				
			}
		}
	}
	private void annotateType(String content, Element laste) {
		String m = "";
		String o = "";
		if(content.indexOf("m[")>=0){
			m = content.substring(content.indexOf("m[")+2, content.indexOf("]")).replaceAll("[{}]", "");
			o = content.substring(content.indexOf("]")+1).trim().replaceAll("(\\w\\[|\\]|\\(|\\)|\\{|\\})", "");
		}else{
			o = content.replaceAll("(\\w\\[|\\]|\\(|\\)|\\{|\\})", "").trim();
		}
		Element type = new Element("character");
		laste.addContent(type);
		//if(this.inbrackets) this.addAttribute(type, "in_bracket", "true");
		this.addAttribute(type, "type", o);
		if(m.length()>0) this.addAttribute(type, "modifier", m);

	}

	/**
	 * 1. create a new chunkedsentence and perform annotateByChunk on it
	 * 2. make the geo constraint a "global" attribute for all elements created from the new chunkedsentence
	 * 3. restore the annotation procedure after this ChunkScopeGeo
	 * @param scopeck
	 * @param scope
	 */
	private void annotateScopeChunk(Chunk scopeck, Attribute scope, ChunkedSentence cs) throws Exception{
		ArrayList<Element> subjectcopy = this.subjects;//snap shot of current subject
		String content = scopeck.toString();
		if(content.matches("^[xgq]\\[.*"))	content = content.replaceFirst("^[xgq]\\[", "").replaceFirst("\\]$", "").trim();
		ArrayList<String> chunks = Utilities.breakText(content+"");
		if(this.latestelements.size()>0 && this.latestelements.get(this.latestelements.size()-1).getName().compareTo("structure")==0){
			this.subjects = latest("structure", this.latestelements);
		}
		ChunkedSentence newcs = new ChunkedSentence(chunks, content, conn, glosstable, this.tableprefix); //form a new chunkedsent
		newcs.setInSegment(true);
		newcs.addScopeAttributes(scope);
		newcs.addScopeAttributesAll(cs.getScopeAttributes());
		//annotateByChunk(newcs, false);//annotate it
		annotateByChunk(newcs);//annotate it		
		this.subjects = subjectcopy; //restore
		// 7-12-02 cs.resetScopeAttributes(); //restore
	}
	
	private void addClauseModifierConstraint(ChunkedSentence cs, Element e) {
		ArrayList<String> cm = cs.getClauseModifierConstraint();
		if(cm!=null){
			if(cm.size()>1){//is a constraint
				this.addAttribute(e, "constraint", cm.get(0));
				this.addAttribute(e, "constraintid", cm.get(1));
			}else{
				this.addAttribute(e, "modifier", cm.get(0));
			}
		}
	}

	private void addScopeConstraints(ChunkedSentence cs, Element e){
		ArrayList<Attribute> scopes = cs.getScopeAttributes();
		for(Attribute scope: scopes){
			this.addAttribute(e,scope.getName(), scope.getValue());
		}
	}
	/**
	 * track back in this.chunkedTokens to populate the afteror element
	 * afteror shares the same character name and value with beforeor, but have different modifier--which is found from the missing text
	 * 	branched distally or throughout
	   	constricted distally or not
		subequal or weakly to strongly
		well distributed or not
		dioecious or nearly so
		spinulose or not
		openly branched distally or throughout
		branched proximally or distally
		usually 1 cm or less
	 * @param afteror
	 * @param beforor
	 */
	private Element traceBack4(Element afteror, Element beforeor, int afterorindex, int endindex, ChunkedSentence cs) {
		// 7-12-02 
		String text =cs.getText(afterorindex, endindex); //from afterorindex (include) to endindex (not include)
		text = text.replaceAll("SG", "").replaceAll("\\W+", " ").replaceAll("\\s+", " ").trim();
		text = text.replaceFirst("\\s+so$", "");
		afteror = (Element)beforeor.clone();
		this.addAttribute(afteror, "modifier", text);
		return afteror;
	}

	private ArrayList<Element> annotateNumericals(String chunktext, String character, String modifier, ArrayList<Element> parents, boolean resetfrom, ChunkedSentence cs) { //added cs
		ArrayList<Element> chars = NumericalHandler.parseNumericals(chunktext, character);
		if(chars.size()==0){//failed, simplify chunktext
			chunktext = chunktext.replaceAll("[()\\]\\[]", "");
			chars = NumericalHandler.parseNumericals(chunktext, character);
		}
		Iterator<Element> it = chars.iterator();
		ArrayList<Element> results = new ArrayList<Element>();
		while(it.hasNext()){
			Element e = it.next();
			if(resetfrom && e.getAttribute("from")!=null && e.getAttributeValue("from").equals("0") &&(e.getAttribute("from_inclusive")==null || e.getAttributeValue("from_inclusive").equals("true"))){// to 6[-9] m.
				e.removeAttribute("from");
				if(e.getAttribute("from_unit")!=null){
					e.removeAttribute("from_unit");
				}
			}
			if(modifier !=null && modifier.compareTo("")!=0){this.addAttribute(e, "modifier", modifier);}
			//if(this.inbrackets){e.setAttribute("in_bracket", "true");}
			/*
			if(this.unassignedmodifiers != null && this.unassignedmodifiers.compareTo("") !=0){
				this.addAttribute(e, "modifier", this.unassignedmodifiers);
				this.unassignedmodifiers = "";
			}*/
			addClauseModifierConstraint(cs, e);
			this.addScopeConstraints(cs, e);
			Iterator<Element> pit= parents.iterator();
			while(pit.hasNext()){
				Element ec = (Element)e.clone();
				ec.detach();
				Element p = pit.next();
				p.addContent(ec);
				results.add(ec);
			}
		}
		return results;
	}

	private ArrayList<Element> lastStructures() {
		ArrayList<Element> parents;
		if(this.latestelements.size()> 0 && this.latestelements.get(this.latestelements.size()-1).getName().compareTo("structure") ==0){
			parents = this.latestelements;
		}else{
			parents = this.subjects;
		}
		return parents;
	}
	/**
	 * 3 times n[...than...]
	   lengths 0.5–0.6+ times <bodies>
	   ca .3.5 times length of <throat>
       1–3 times {pinnately} {lobed}
       1–2 times shape[{shape~list~pinnately~lobed~or~divided}]
       4 times longer than wide
       
       
       
	 * @param content: 0.5–0.6+ times a[type[bodies]]
	 * @param subjects2
	 * @return
	 */
	private ArrayList<Element> processComparativeValue(String content,
			ArrayList<Element> parents, ChunkedSentence cs)  throws Exception {// 7-12-02 add cs
		if(content.startsWith("n[")){
			content = content.replaceFirst("^n\\[", "").replaceFirst("\\]", "").trim();
		}
		String v = content.replaceAll("\\b("+ChunkedSentence.times+")\\b.*$", "").trim(); // v holds numbers
		String n = content.replace(v, "").trim();
		if(n.indexOf("constraint")>=0){
			n = n.replaceFirst("constraint\\[", "").replaceFirst("\\]$", ""); //n holds times....
		}
		if(n.indexOf("n[")>=0 ){//1.5–2.5 times n[{longer} than (throat)]
			//content = "n["+content.replace("n[", "");
			content = v.replaceFirst("(^| )(?=\\d)", " size[")+"] constraint["+n.replaceFirst("n\\[", "").trim(); //m[usually] 1.5-2
			return this.processTHAN(content, parents, cs);// 7-12-02 add cs
		}else if(n.indexOf("type[")==0 || n.indexOf(" type[")>0){//size[{longer}] constraint[than (object}]
			//this.processSimpleCharacterState("a[size["+v.replace(" times", "")+"]]", parents);
			//ArrayList<Element> structures = this.processObject(n);
			//this.createRelationElements("times", parents, structures, this.unassignedmodifiers);
			//this.unassignedmodifiers = null;
			//return structures;
			n = "constraint["+n.replaceFirst("type\\[", "(").replaceFirst("\\]", ")").replaceAll("a\\[", ""); //1-1.6 times u[o[bodies]] => constraint[times (bodies)]
			content = "size["+v+"] "+n;
			return this.processTHAN(content, parents, cs);// 7-12-02 add cs			
		}else if(n.indexOf("o[")>=0 ||n.indexOf("z[")>=0  || n.indexOf("l[")>=0 ){//ca .3.5 times length r[p[of] o[(throat)]]
			n = "constraint["+n.replaceAll("[o|l|z]\\[", ""); //times o[(bodies)] => constraint[times (bodies)]
			content = "size["+v+"] "+n;
			return this.processTHAN(content, parents, cs);// 7-12-02 add cs	
		}else if(n.indexOf("a[")==0 || n.indexOf(" a[")>0 || n.indexOf("~list~")>0){ //characters:1–3 times {pinnately} {lobed}
			String times = n.substring(0, n.indexOf(' '));
			n = n.substring(n.indexOf(' ')+1);
			if(n.indexOf("~list~")>0){
				v = v.replaceAll("(\\w\\[|\\])", "");
				n = "m["+v+" "+times+"] "+n;
				return this.processCharacterList(n, parents, cs);// 7-12-02 add cs
			}else{
				n = n.replaceFirst("a\\[", "").replaceFirst("\\]$", "");
				n = "m["+v+" "+times+"] "+n;
				return this.processSimpleCharacterState(n, parents, cs);// 7-12-02 add cs
			}
		}else if(content.indexOf("[")<0){ //{forked} {moreorless} unevenly ca . 3-4 times , 
			//content = 3-4 times; v = 3-4; n=times
			//marked as a constraint to the last character "forked". "ca." should be removed from sentences in SentenceOrganStateMarker.java
			Element lastelement = this.latestelements.get(this.latestelements.size()-1);
			if(lastelement.getName().compareTo("character")==0){
				Iterator<Element> it = this.latestelements.iterator();
				while(it.hasNext()){
					lastelement = it.next();
					if(cs.unassignedmodifier != null && cs.unassignedmodifier.trim().length()!=0){
						lastelement.setAttribute("modifier", cs.unassignedmodifier);
						cs.unassignedmodifier = null;
					}
					lastelement.setAttribute("constraint", content);
				}
			}else if(lastelement.getName().compareTo("structure")==0){
				return null; //parsing failure
			}
			return this.latestelements;
			
		}
		return null;
	}

	/**
	 * size[{longer}] constraint[than (object)]";
	 * shape[{lobed} constraint[than (proximal)]]
	 * @param replaceFirst
	 * @param subjects2
	 * @return
	 */
	private ArrayList<Element> processTHAN(String content,
			ArrayList<Element> parents, ChunkedSentence cs)  throws Exception{// 7-12-02 add cs
		
		ArrayList<Element> charas = new ArrayList<Element>();
		String[] parts = content.split("constraint\\[");
		if(content.startsWith("constraint")){
			charas = latest("character", this.latestelements);
		}else{
			if(parts[0].matches(".*?\\d.*") && parts[0].matches(".*size\\[.*")){//size[m[mostly] [0.5-]1.5-4.5] ;// often wider than 2 cm.
				parts[0] = parts[0].trim().replace("size[", "").replaceFirst("\\]$", "");
				Pattern p = Pattern.compile(NumericalHandler.numberpattern+" ?[{<(]?[cdm]?m?[)>}]?");
				Matcher m = p.matcher(parts[0]);
				String numeric = "";
				if(m.find()){ //a series of number
					numeric = parts[0].substring(m.start(), m.end()).trim().replaceAll("[{<(]$", "");
				}else{
					p = Pattern.compile("\\d+ ?[{<(]?[cdm]?m?[)>}]?"); //1 number
					m = p.matcher(parts[0]);
					m.find();
					numeric = parts[0].substring(m.start(), m.end()).trim().replaceAll("[{<(]$", "");
				}
				String modifier = parts[0].substring(0, parts[0].indexOf(numeric)).replaceAll("(\\w+\\[|\\[|\\])", "").trim();
				if(parts.length<2){//parse out a constraint for further process
					String constraint = parts[0].substring(parts[0].indexOf(numeric)+numeric.length()).trim();
					String t = parts[0];
					parts = new String[2];//parsed out a constraint for further process
					parts[0] = t;
					parts[1] = constraint;
				}
				/*String modifier = parts[0].replaceFirst("size\\[.*?\\]", ";").trim().replaceAll("(^;|;$|\\w\\[|\\])", "");
				String numeric = parts[0].substring(parts[0].indexOf("size["));
				numeric = numeric.substring(0, numeric.indexOf("]")+1).replaceAll("(\\w+\\[|\\])", "");*/
				charas = this.annotateNumericals(numeric.replaceAll("[{<()>}]", ""), "size", modifier.replaceAll("[{<()>}]", ""), parents, false, cs); //added cs
			}else{//size[{shorter} than {plumose} {inner}]
				charas = this.processSimpleCharacterState(parts[0].replaceAll("(\\{|\\})", "").trim(), parents, cs); // 7-12-02 add cs//numeric part
			}
		}
		String object = null;
		ArrayList<Element> structures = new ArrayList<Element>();
		if(parts.length>1 && parts[1].length()>0){//parts[1]: than (other) {pistillate} (paleae)]
			if(parts[1].indexOf("(")>=0){
				String ostr = parts[1];
				object = ostr.replaceFirst("^.*?(?=[({])", "").replaceFirst("\\]+$", ""); //(other) {pistillate} (paleae)
				object = "o["+object+"]";
				if(object != null){
					structures.addAll(this.processObject(object, cs));// 7-12-02 add cs
				}
				/*while(ostr.indexOf('(')>=0){
					object = ostr.substring(ostr.indexOf('('), ostr.indexOf(')')+1);
					object = "o["+object+"]";
					ostr = ostr.substring(ostr.indexOf(')')+1);
					if(object != null){
						structures.addAll(this.processObject(object));
					}
				}*/
			}
			//have constraints even without an organ 12/15/10
				Iterator<Element> it = charas.iterator();
				while(it.hasNext()){
					Element e = it.next();
					//if(parts[1].indexOf("(")>=0){
					//	this.addAttribute(e, "constraint", this.listStructureNames(parts[1]));
					//}else{
						this.addAttribute(e, "constraint", parts[1].replaceAll("(\\(|\\)|\\{|\\}|\\w*\\[|\\])", ""));
				   //}
					if(object!=null){
						this.addAttribute(e, "constraintid", this.listStructureIds(structures));//TODO: check: some constraints are without constraintid
					}
				}
				
			
		}
		if(structures.size() > 0){
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
			ArrayList<Element> parents, ChunkedSentence cs)  throws Exception{// 7-12-02 add cs
		ArrayList<Element> results = new ArrayList<Element>();
		//String object = content.substring(content.indexOf("o["));
		int objectindex = content.lastIndexOf("o[");
		String object = null;
		if(objectindex < 0){
			object = content.substring(content.lastIndexOf("w[")); //v[leaving] w[{deep} to almost {flat} (scar)]: failed to recognize the to_range, normalizeTo identified a w[] chunk
		}else{
			object = content.substring(objectindex);
		}
		String rest = content.replace(object, "").trim();
		String relation = rest.substring(rest.indexOf("v["));
		String modifier = rest.replace(relation, "").trim().replaceAll("(m\\[|\\])", "");
		
		object = parenthesis(object);
		ArrayList<Element> tostructures = this.processObject(object, cs); // 7-12-02 add cs//TODO: fix content is wrong. i8: o[a] architecture[surrounding (involucre)]
		results.addAll(tostructures);
		
		this.createRelationElements(relation.replaceAll("(\\w\\[|\\])", ""), this.subjects, tostructures, modifier, false, cs);// 7-12-02 add cs
		return results;
	}

	/**
	 * @param content: m[usually] coloration[dark brown]: there is only one character states and several modifiers
	 * @param parents: of the character states
	 */
	private ArrayList<Element> processSimpleCharacterState(String content,
			ArrayList<Element> parents, ChunkedSentence cs)  throws Exception{// 7-12-02 add cs
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
				if(this.unassignedcharacter!=null){
					character = this.unassignedcharacter;
					this.unassignedcharacter = null;
				}
				state = parts[1];
				modifier += "; ";
			}
		}
		String statecp = state;
		String charactercp = character;
		modifier = modifier.replaceAll("m\\[", "").trim().replaceAll("(^\\W|\\W$)", "").trim();
		String eqcharacter = ChunkedSentence.eqcharacters.get(state);
		if(eqcharacter != null){
			state = eqcharacter;
			String[] charainfo = Utilities.lookupCharacter(eqcharacter, conn, ChunkedSentence.characterhash, this.glosstable, this.tableprefix);
			if(charainfo == null){
				state = statecp;
				character = charactercp;
			}else{
				character = charainfo[0];
			}
		}
		if(character.compareToIgnoreCase("character")==0 && modifier.length() ==0){//high relief: character=relief, reset the character of "high" to "relief"
			Element lastelement = this.latestelements.get(this.latestelements.size()-1);
			if(lastelement.getName().compareTo("character")==0){
				Iterator<Element> it = this.latestelements.iterator();
				while(it.hasNext()){
					lastelement = it.next();
					lastelement.setAttribute("name", state);
				}
			}else if(lastelement.getName().compareTo("structure")==0){
				this.unassignedcharacter = state;
			}
			results = this.latestelements;
		}else if(state.length()>0){
			/*if(this.unassignedmodifiers!=null && this.unassignedmodifiers.length()>0){
				modifier = modifier+";"+this.unassignedmodifiers;
				this.unassignedmodifiers = "";
			}*/
			//mohan code 11/7/2011 original replace after verification
			//this.createCharacterElement(parents, results, modifier, state, character, "");
			//end mohan code
			this.createCharacterElement(parents, results, modifier, state, character, "", cs);// 7-12-02 add cs
		}
		
		return results;
	}

	private void establishSubject(String content/*, boolean makeconstraint*/, ChunkedSentence cs)  throws Exception{// 7-12-02 add cs
		ArrayList<Element> structures = createStructureElements(content/*, makeconstraint*/, cs);// 7-12-02 add cs
		this.subjects = new ArrayList<Element>();
		this.latestelements = new ArrayList<Element>();
		Iterator<Element> it = structures.iterator();
		while(it.hasNext()){
			Element e = it.next();
			if(e.getName().compareTo("structure")==0){ //ignore character elements
				this.subjects.add(e);
				this.latestelements.add(e);
			}
		/*Iterator<Element> it = structures.iterator();
		while(it.hasNext()){
			Element e = it.next();
			if(e.getName().compareTo("structure")==0){ //ignore character elements
				this.subjects.add(e);
				this.latestelements.add(e);
			}*/
		}
	}
	
	//fix: should prohibit grabbing subject across treatments
	private void reestablishSubject() {
		//if(this.subjects.size()>0){
			Iterator<Element> it = this.subjects.iterator();
			this.latestelements = new ArrayList<Element>();
			while(it.hasNext()){
				Element e = it.next();
				e.detach();
				this.statement.addContent(e);
				this.latestelements.add(e);
			}
		//}else{//reach out to the last statement
			
		//}
	}
	/**
	 * TODO: {shape-list-usually-flat-to-convex-punct-sometimes-conic-or-columnar}
	 *       {pubescence-list-sometimes-bristly-or-hairy}
	 * @param content: pubescence[m[not] {pubescence-list-sometimes-bristly-or-hairy}]
	 * @param parents
	 * @return
	 */
	private ArrayList<Element> processCharacterList(String content,
			ArrayList<Element> parents, ChunkedSentence cs)  throws Exception {// 7-12-02 add cs
		ArrayList<Element> results= new ArrayList<Element>();
		String modifier = "";
		while(content.indexOf("m[")>=0){
			String m = content.substring(content.indexOf("m["), content.indexOf("]", content.indexOf("m["))+1);
			modifier += m+" ";
			content = content.replace(m, "").trim();
			
		}
		modifier = modifier.trim().replaceAll("(m\\[|\\])", "");
		//content = content.replace(modifier, "");
		String[] parts = content.split("\\s*\\[\\s*");
		//String[] parts = content.split("\\[?\\{");
		if(parts.length<2){
			return results; //@TODO: parsing failure
		}
		String cname = parts[0];
		if(this.unassignedcharacter!=null){
			cname = this.unassignedcharacter;
			this.unassignedcharacter = null;
		}
		String cvalue = parts[1].replaceFirst("\\{"+cname+"~list~", "").replaceFirst("\\W+$", "").replaceAll("~", " ").trim();
		//String cvalue = parts[1].replaceFirst("\\{"+cname+"-list", "").replaceFirst("\\W+$", "").replaceAll("-", " ").trim();
		//String cvalue = parts[1].replaceFirst("^"+cname+"~list~", "").replaceFirst("\\W+$", "").replaceAll("~", " ").trim();
		if(cname.endsWith("ttt")){
			this.createCharacterElement(parents, results, modifier, cvalue, cname.replaceFirst("ttt$", ""), "", cs);// 7-12-02 add cs
			return results;
		}
		if(cvalue.indexOf(" to ")>=0){
			createRangeCharacterElement(parents, results, modifier, cvalue.replaceAll("punct", ",").replaceAll("(\\{|\\})", ""), cname, cs); //add a general statement: coloration="red to brown" // 7-12-02 add cs
		}
		String mall = "";
		boolean findm = false;
		//gather modifiers from the end of cvalues[i]. this modifier applies to all states
		do{
			findm = false;
			String last = cvalue.substring(cvalue.lastIndexOf(' ')+1);
			if(Utilities.lookupCharacter(last, conn, ChunkedSentence.characterhash, glosstable, tableprefix)==null && Utilities.isAdv(last, ChunkedSentence.adverbs, ChunkedSentence.notadverbs)){
				mall +=last+ " ";
				cvalue = cvalue.replaceFirst(last+"$", "").trim();
				findm = true;
			}
		}while(findm);
		
		String[] cvalues = cvalue.split("\\b(to|or|punct)\\b");//add individual values
		for(int i = 0; i<cvalues.length; i++){
			String state = cvalues[i].trim();//usually papillate to hirsute distally
			if(state.length() == 0) continue;
			//gather modifiers from the beginning of cvalues[i]. a modifier takes effect for all state until a new modifier is found
			String m = "";
			do{
				findm = false;
				if(state.length()==0){
					break;
				}
				int end = state.indexOf(' ')== -1? state.length():state.indexOf(' ');
				String w = state.substring(0, end);
				if(Utilities.lookupCharacter(w, conn, ChunkedSentence.characterhash, glosstable, tableprefix)==null && Utilities.isAdv(w, ChunkedSentence.adverbs, ChunkedSentence.notadverbs)){
					m +=w+ " ";
					w = w.replaceAll("\\{", "\\\\{").replaceAll("\\}", "\\\\}").replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)").replaceAll("\\+", "\\\\+");
					state = state.replaceFirst(w, "").trim();
					findm = true;
				}
			}while (findm);
			
			if(m.length()==0){
				modifier = (modifier+" "+mall).trim();
				state = state.replaceAll("\\s+", " ").trim(); 
			}else{
				modifier = modifier.matches(".*?\\bnot\\b.*")? modifier +" "+m : m; //update modifier
				modifier = (modifier+" "+mall).trim();
				state = state.replaceAll("\\s+", " ").trim(); 
			}
			results.addAll(this.processCharacterText("m["+modifier+"] "+state, parents, cname, cs));// 7-12-02 add cs
			//doesn't make sense to remove modifier marks and process a character string (with modifier) as plain tokens
			/*if(m.length()==0){
				state = (modifier+" "+mall+" "+state.replaceAll("\\s+", "#")).trim(); //prefix the previous modifier 
			}else{
				modifier = modifier.matches(".*?\\bnot\\b.*")? modifier +" "+m : m; //update modifier
				//cvalues[i] = (mall+" "+cvalues[i]).trim();
				state = (modifier+" "+mall+" "+state.replaceAll("\\s+", "#")).trim(); //prefix the previous modifier 
			}
			String[] tokens = state.split("\\s+");
			tokens[tokens.length-1] = tokens[tokens.length-1].replaceAll("#", " ");
			results.addAll(this.processCharacterText(tokens, parents, cname));
			//results.addAll(this.processCharacterText(new String[]{state}, parents, cname));*/
		}

		return results;
	}
	/**
	 * crowded to open
	 * for categorical range-value
	 * @param parents
	 * @param results
	 * @param modifier
	 * @param cvalue
	 * @param cname
	 */
	private String createRangeCharacterElement(ArrayList<Element> parents,
			ArrayList<Element> results, String modifiers, String cvalue,
			String cname, ChunkedSentence cs) {// 7-12-02 add cs
		Element character = new Element("character");
		//if(this.inbrackets){character.setAttribute("in_bracket", "true");}
		character.setAttribute("char_type", "range_value");
		character.setAttribute("name", cname);
		
		String[] range = cvalue.split("\\s+to\\s+");//a or b, c, to d, c, e
		String[] tokens = range[0].replaceFirst("\\W$", "").replaceFirst("^.*?\\s+or\\s+", "").split("\\s*,\\s*"); //a or b, c, =>
		String from = getFirstCharacter(tokens[tokens.length-1]);
		tokens = range[1].split("\\s*,\\s*");
		String to = getFirstCharacter(tokens[0]);
		character.setAttribute("from", from.replaceAll("-c-", " ")); //a or b to c => b to c
		character.setAttribute("to", to.replaceAll("-c-", " "));
		
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
		addClauseModifierConstraint(cs, character);
		this.addScopeConstraints(cs, character);
		return modifiers;
		
	}

	/**
	 * 
	 * @param tokens: usually large
	 * @return: large
	 */
	private String getFirstCharacter(String character) {
		String[] tokens = character.trim().split("\\s+");
		String result = "";
		for(int i = 0; i<tokens.length; i++){
			if(Utilities.lookupCharacter(tokens[i], conn, ChunkedSentence.characterhash, glosstable, tableprefix)!=null){
				 result += tokens[i]+" ";
			}
		}
		return result.trim();
	}

	/**
	 * 
	 * @param elements
	 */
	private void updateLatestElements(ArrayList<Element> elements) {
		this.latestelements = new ArrayList<Element>();
		if(elements != null){
			latestelements.addAll(elements);
		}
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
	
	private ArrayList<Element> processCHPP(String content, ChunkedSentence cs)  throws Exception {// 7-12-02 add cs
		ArrayList<Element> relations = new ArrayList<Element>();
		//having oval outline
		if(this.characterPrep(content)){
			return relations;
		}		
		String c = content.substring(0, content.indexOf("r["));
		String r = content.replace(c, "");
		if(r.lastIndexOf("o[")<0){ //#{usually} {arising} r[p[in]]# {distal} 1/2
			//failed parse
			cs.setPointer2NextComma();
			return relations;
		}
		String p = r.substring(0, r.lastIndexOf("o["));//{often} {dispersed} r[p[with] o[aid  r[p[from] o[(pappi)]]]] 
		String o = r.replace(p, "");
		String[] mc = c.split("(?<=\\])\\s*");
		String m = "";
		c = "";
		for(int i =0; i<mc.length; i++){
			if(mc[i].startsWith("m[")){
				m += mc[i]+" ";
			}else if(mc[i].startsWith("c[")/*mc[i].matches("^\\w+\\[.*")*/){
				c += mc[i]+" ";
			}
		}
		m = m.replaceAll("(m\\[|\\]|\\{|\\})", "").trim();
		c = c.replaceAll("(c\\[|\\]|\\{|\\})", "").trim(); //TODO: will this work for nested chuncks?
		p = p.replaceAll("(\\w\\[|\\])", "").trim();
		//c: {loosely} {arachnoid}
		String[] words = c.split("\\s+");
		if(Utilities.isVerb(words[words.length-1], ChunkedSentence.verbs, ChunkedSentence.notverbs) || p.compareTo("to")==0){//t[c[{connected}] r[p[by] o[{conspicuous} {arachnoid} <trichomes>]]] TODO: what if c was not included in this chunk?
			String relation = (c+" "+p).replaceAll("\\s+", " ");
			o = o.replaceAll("(o\\[|\\])", "");
			/*if(!o.endsWith(")") &&!o.endsWith("}")){ //1-5 series => 1-5 (series)
				String t = o.substring(o.lastIndexOf(' ')+1);
				o = o.replaceFirst(t+"$", "("+t)+")";
			}*/
			if(!o.endsWith(")")){ //force () on the last word. Hong 3/4/11
				String t = o.substring(o.lastIndexOf(' ')+1);
				t = t.replace("{", "").replace("}", "");
				o = o.substring(0, o.lastIndexOf(' ')+1)+"("+t+")";
				
				//System.out.println("forced organ in: "+o);
			}
			ArrayList<Element> structures = processObject("o["+o+"]", cs);// 7-12-02 add cs
			ArrayList<Element> entity1 = null;
			
			Element e = this.latestelements.get(this.latestelements.size()-1);
			if(e.getName().matches("("+this.delims+")") || e.getName().compareTo("character")==0 ){
				entity1 = this.subjects;
			}else{
				entity1 = (ArrayList<Element>)this.latestelements.clone();
				//entity1.remove(entity1.size()-1);
			}
			
			relations = createRelationElements(relation, entity1, structures, m, false, cs);// 7-12-02 add cs
			updateLatestElements(structures);
		}else{//c: {loosely} {arachnoid} : should be m[loosly] architecture[arachnoid]
			//String[] tokens = c.replaceAll("[{}]", "").split("\\s+");
			//ArrayList<Element> charas = this.processCharacterText(tokens, this.subjects);
			ArrayList<Element> charas = this.processSimpleCharacterState(c, this.subjects, cs);// 7-12-02 add cs
			updateLatestElements(charas);
			//processPrep(new ChunkPrep(r), cs); //not as a relation // 7-12-02 add cs
			processPrep(r, cs); //not as a relation // 7-12-02 add cs
		}
		return relations;
	}



	/**
	 * CK takes form of relation character/states [structures]?
	 * update this.latestElements with structures only.
	 * 
	 * nested1: r[p[of] o[5-40 , fusion[{fusion~list~distinct~or~basally~connate}] r[p[in] o[groups]] , coloration[{coloration~list~white~to~tan}] , {wholly} or {distally} {plumose} (bristles)]] []]
	 * nested2: r[p[with] o[{central} {cluster} r[p[of] o[(spines)]]]]
	 * @param ck
	 * @param asrelation: if this PP should be treated as a relation
	 */
	private void processPrep(String ckstring, ChunkedSentence cs)  throws Exception{// 7-12-02 add cs
		//r[{} {} p[of] o[.....]]
		String modifier = ckstring.substring(0, ckstring.indexOf("p[")).replaceFirst("^r\\[", "").replaceAll("[{}]", "").trim();
		//sometime o[] is not here as in ckstring=r[p[at or above]] {middle}
		//String pp = ckstring.substring(ckstring.indexOf("p["), ckstring.lastIndexOf("] o[")).replaceAll("(\\w\\[|])", "");
		//String object  = ckstring.substring(ckstring.lastIndexOf("o[")).replaceFirst("\\]+$", "")+"]";	
		int objectindex = ckstring.indexOf("] ", ckstring.indexOf("p[")+1);
		String pp = ckstring.substring(ckstring.indexOf("p["), objectindex).replaceAll("(\\w\\[|])", "");
		String object = ckstring.substring(objectindex+1).replaceFirst("\\]$", "").trim(); //to keep chunking clues in bracketed expressions 8/6/2012, need more test.
		if(!object.startsWith("o[")) object = "o["+object+"]";		
		//String object = "o["+ckstring.substring(objectindex).trim().replaceAll("(\\b\\w\\[)|]", "")+"]";
		//String object = "o["+ckstring.substring(objectindex).trim().replaceAll("(\\b\\w\\[|])", "")+"]";
		//String object = "o["+ckstring.substring(objectindex).trim().replaceAll("(\\[|])", "")+"]";
		//TODO: r[p[in] o[outline]] or r[p[with] o[irregular ventral profile]]
		if(characterPrep(ckstring)){
			return;		
		}
		/*String pp = null;
		String object = null;
		if(ckstring.matches(".*?\\]{4,}$")){//nested2 
			pp = ckstring.substring(ckstring.indexOf("p["), ckstring.lastIndexOf("] o[")).replaceAll("(\\w\\[|])", "");
			object  = ckstring.substring(ckstring.lastIndexOf("o[")).replaceFirst("\\]+$", "")+"]";	
		}else{//nested1 or not nested
			pp = ckstring.substring(ckstring.indexOf("p["), ckstring.indexOf("] o[")).replaceAll("(\\w\\[|])", "");
			object  = ckstring.substring(ckstring.indexOf("o[")).replaceFirst("\\]+$", "")+"]";//nested or not
		}*/
		
		
		
		object = NumericalHandler.originalNumForm(object);
		boolean lastIsStruct = false;
		boolean lastIsChara = false;
		boolean lastIsComma = false;
		//mohan code to get the original subject if the subject is empty Store the chunk into the modifier
		
		if(this.latestelements.size()==0)
		{
			//String content = ck.toString().replaceAll(" ","-");
			String content = ckstring.replaceAll(" ","-");
			//String structure = "m[" +content+"]";
			String structure = content.replaceAll("]-o\\[", "-").replaceAll("[{()}]", "");
			if(cs.unassignedmodifier == null){
				cs.unassignedmodifier = structure;
			}else{
				cs.unassignedmodifier += structure;
			}
				
			return;
		}
		
		//end mohan code
		
		Element lastelement = this.latestelements.get(this.latestelements.size()-1);
		
		
		
		if(lastelement.getName().compareTo("structure") == 0){//latest element is a structure
			lastIsStruct = true;
		}else if(lastelement.getName().compareTo("character") == 0){
			lastIsChara = true;
		}else if(lastelement.getName().matches("("+this.delims+")")){
			lastIsComma = true;
			if(this.printComma){
				System.out.println("prep ahead of character: "+ckstring);
			}
		}
		//of o[3-7]
		if(lastIsStruct && object.matches("o\\[\\(?\\[?\\d.*?\\d\\+?\\]")){
			this.annotateNumericals(object.replaceAll("(o\\[|\\])", ""), "count", null, this.latestelements, false, cs);//added cs
			return;
		}
		
		ArrayList<Element> structures = new ArrayList<Element>();
		//3/30/2011: try to separate "in {} {} arrays" cases from "at {flowering}", "in fruit", and "in size" cases
		//allow () be added around the last bare word if there is a {} before the bare word, or if the word is not a character (size, profile, lengths)
		object = parenthesis(object);
		/*if(! object.matches(".*?\\}\\]+$")){ //contains organ: > or untagged: arrays
			//add () around the last word if it is bare
			if(object.matches(".*?[a-z]\\]+$")){
				System.out.println("!!!!!!Object: "+object);
				int l = object.lastIndexOf(' ');
				if(l>0){
					String last = object.substring(l+1);
					object = object.replaceFirst(last+"$", "("+last.replaceFirst("\\]", ")]"));
				}else{//object= o[tendrils]
					object = object.replaceFirst("\\[", "[(").replaceFirst("\\]", ")]");
				}
			}*/
		if(object.matches(".*?\\)\\]+$")){
			structures = linkObjects(modifier, pp, object, lastIsStruct,
					lastIsChara, lastelement, cs); // 7-12-02 add cs
			updateLatestElements(structures);
		}else if(object.matches(".*?\\([-a-z]+\\).*") && !object.matches(".*?[-a-z]+\\]+$")){//contains organ in the middle of object:r[p[from] o[{thick} {notothyrial} (platform) {excavated} {laterally}]]
			String obj = object.substring(0, object.lastIndexOf(")")+1).trim();
			String modi = object.substring(object.lastIndexOf(")")+1).trim(); //TODO: left out right end modi for now.
			
			object = obj;
			structures = linkObjects(modifier, pp, object, lastIsStruct,
					lastIsChara, lastelement, cs); // 7-12-02 add cs
			updateLatestElements(structures);
		}else{// "at {flowering}]" or "in size]" 
			//contains no organ, e.g. "at flowering"
			//Element last = this.latestelements.get(this.latestelements.size()-1);
			if(lastIsStruct){
				addAttribute(lastelement, "constraint", ckstring.replaceAll("(\\w\\[|\\]|\\{|\\})", ""));//TODO 5/16/2011 <corollas> r[p[of] o[{sterile} {much} {expanded} and {exceeding} (corollas)]] This should not be happening.z[{equaling} (phyllaries)] r[p[at] o[{flowering}]]
			}else if(lastIsChara){ //character element
				addAttribute(lastelement, "modifier", ckstring.replaceAll("(\\w\\[|\\]|\\{|\\})", ""));
			}
			//addPPAsAttributes(ckstring);
		}
		
		//bookkeeping: update this.latestElements: only structures are visible
		//updateLatestElements(structures);
	}

	/**
	 * 
	 * @param ckstring:r[p[in] o[outline]]
	 * @return
	 */
	private boolean characterPrep(String ckstring) {
		boolean done =false;
		if(ckstring.indexOf(" ") < 0) return done;
		String lastword = ckstring.substring(ckstring.lastIndexOf(" ")).replaceAll("\\W", "");
		if(lastword.matches("("+this.characters+")")){
			Element lastelement = this.latestelements.get(this.latestelements.size()-1);
			if(lastelement.getName().compareTo("character")==0){//shell oval in outline
				Iterator<Element> it = this.latestelements.iterator();
				while(it.hasNext()){
					lastelement = it.next();
					lastelement.setAttribute("name", lastword);
				}
				done = true;
			}else if(lastelement.getName().compareTo("structure")==0){//shell in oval outline
				String cvalue = ckstring.replaceFirst(".*?\\]", "").replaceAll("\\w+\\[","").replaceAll(lastword, "").replaceAll("[{}\\]\\[]", "");
				Iterator<Element> it = this.latestelements.iterator();
				while(it.hasNext()){
					lastelement = it.next();
					lastelement.setAttribute("name", lastword);
					lastelement.setAttribute("value", cvalue);
				}
				done = true;
			}
		}
		return done;
	}

	private String parenthesis(String object) {
		if(!object.matches(".*?\\}\\]+$")){ //contains organ: > or untagged: arrays
			if(object.matches(".*?[a-z]\\]+$")){//there is a bare word
				int l = object.lastIndexOf(' ');
				l = l < 0 ? object.lastIndexOf('[') : l; 
				String last = object.substring(l+1).replaceAll("\\W+$", "");
				if(object.indexOf('{')>=0 || !isCharacter(last)){// if there are other modifiers/characters, then must make "last" a structure
					object = object.replaceFirst(last+"(?=\\]+$)", "("+last+")");
				}								
			}
		}
		return object;
	}

	private boolean isCharacter(String last) {
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select * from "+this.glosstable +" where term='"+last+"' and category='character'");
			if(rs.next()){
				return true;
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
		}
		return false;
	}

	private ArrayList<Element> linkObjects(String modifier, String pp,
			String object, boolean lastIsStruct, boolean lastIsChara,
			Element lastelement, ChunkedSentence cs)  throws Exception {// 7-12-02 add cs
		ArrayList<Element> structures;
		structures = processObject(object, cs);// 7-12-02 add cs
		String base = "";
		if(object.matches("o?\\[*\\{*("+ChunkedSentence.basecounts+")\\b.*")){
			base = "each";
		}
		if(lastIsChara){
			//if last character is size, change to location: <margins> r[p[with] o[3–6 (spines)]] 1–3 {mm} r[p[{near}] o[(bases)]]. 
			//1-3 mm is not a size, but a location of spines
			if(lastelement.getAttributeValue("name").compareTo("size") == 0 && 
					((lastelement.getAttributeValue("value")!=null && lastelement.getAttributeValue("value").matches(".*?\\d.*")) || (lastelement.getAttributeValue("from")!=null && lastelement.getAttributeValue("from").matches(".*?\\d.*")))
					&& pp.matches("("+ChunkedSentence.locationpp+")")){
				lastelement.setAttribute("name", "location");
			}
			//addAttribute(lastelement, "constraint", (pp+" "+base+" "+listStructureNames(structures)).replaceAll("\\s+", " ").replaceAll("(\\{|\\})", "")); //a, b, c
			addAttribute(lastelement, "constraint", (pp+" "+listStructureNames(object)).replaceAll("\\s+", " ").replaceAll("(\\w+\\[|\\(|\\)|\\{|\\}|\\])", ""));
			addAttribute(lastelement, "constraintid", listStructureIds(structures));//1, 2, 3
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
				createRelationElements(relation, entity1, structures, modifier, false, cs);//relation elements not visible to outside //// 7-12-02 add cs
			}
			if(relation!= null && relation.compareTo("part_of")==0) structures = entity1; //part_of holds: make the organbeforeof/entity1 the return value, all subsequent characters should be refering to organbeforeOf/entity1
			
		}
		return structures;
	}
	/**
	 * o[.........{m} {m} (o1) and {m} (o2)]
	 * o[each {bisexual} , architecture[{architecture-list-functionally-staminate-punct-or-pistillate}] (floret)]] ; 
	 * @param object
	 * @return
	 */
	private ArrayList<Element> processObject(String object, ChunkedSentence cs)  throws Exception {// 7-12-02 add cs
		ArrayList<Element> structures;		
		if(object.indexOf("l[")>=0){
			//a list of object
			object = object.replace("l[", "").replaceFirst("\\]", "");
		}
		String[] twoparts = separate(object);//find the organs in object o[.........{m} {m} (o1) and {m} (o2)]
		structures = createStructureElements(twoparts[1]/*, false*/, cs);// 7-12-02 add cs//to be added structures found in 2nd part, not rewrite this.latestelements yet
		if(twoparts[0].length()>0){
			/*if(twoparts[0].matches(".*?\\b\\w\\[.*")){//nested chunks: e.g. 5-40 , fusion[{fusion~list~distinct~or~basally~connate}] r[p[in] o[groups]] , coloration[{coloration~list~white~to~tan}] , {wholly} or {distally} {plumose}
				//get tokens for the new chunkedsentence
				ArrayList<String>tokens = Utilities.breakText(twoparts[0]);
				twoparts[0]=twoparts[0].trim();
				if(!twoparts[0].matches(".*?[,;\\.:]$")){
					twoparts[0] +=" .";
					tokens.add(".");
				}
				ChunkedSentence newcs = new ChunkedSentence(tokens, twoparts[0], conn, glosstable);
				//annotate this new chunk
				ArrayList<Element> subjectscopy = this.subjects;
				this.subjects = structures;
				newcs.setInSegment(true);
				annotateByChunk(newcs, false); //no need to updateLatestElements
				this.subjects = subjectscopy;
			}else{*/
				ArrayList<Element> structurescp = (ArrayList<Element>) structures.clone();
				String[] tokens = twoparts[0].replaceFirst("[_-]$", "").split("\\s+");//add character elements
				if(twoparts[1].indexOf(") plus")>0){//(teeth) plus 1-2 (bristles), the structure comes after "plus" should be excluded
					String firstorgans = twoparts[1].substring(0, twoparts[1].indexOf(") plus")); //(teeth
					String lastorganincluded = firstorgans.substring(firstorgans.lastIndexOf("(")+1);
					for(int i = structures.size()-1; i>=0;  i--){
						if(!structures.get(i).getAttributeValue("name").equals(Utilities.toSingular(lastorganincluded))){
							structures.remove(i);
						}
					}
				}
				processCharacterText(tokens, structures, null, cs);// 7-12-02 add cs //process part 1, which applies to all lateststructures, invisible
				structures = structurescp;
			//}
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

	//find all () in object
	private String listStructureNames(String object){
		String os = "";
		object = object.replaceAll("\\)\\s*\\(", " "); //(leaf) (blade) =>(leaf blade)
		Pattern p = Pattern.compile(".*?\\(([^)]*?)\\)(.*)");
		Matcher m = p.matcher(object);
		while(m.matches()){
			os += m.group(1)+", ";
			object = m.group(2);
			m = p.matcher(object);
		}
		return os.trim().replaceFirst(",$", "");
	}
	/*private String listStructureNames(ArrayList<Element> structures) {
		StringBuffer list = new StringBuffer();
		Iterator<Element> it = structures.iterator();
		while(it.hasNext()){
			Element e = it.next();
			list.append(e.getAttributeValue("name")+", ");
		}
		return list.toString().trim().replaceFirst(",$", "");
	}*/

	private ArrayList<Element> createRelationElements(String relation, ArrayList<Element> fromstructs, ArrayList<Element> tostructs, String modifier, boolean symmetric, ChunkedSentence cs) {// 7-12-02 add cs
		//add relation elements
		ArrayList<Element> relas = new ArrayList<Element>();
		relation = relation.replaceAll("(\\w+\\[|\\]|\\{|\\}|\\(|\\))", "");
		for(int i = 0; i<fromstructs.size(); i++){
			String o1id = fromstructs.get(i).getAttributeValue("id");
			for(int j = 0; j<tostructs.size(); j++){
				String o2id = tostructs.get(j).getAttributeValue("id");
				boolean negation=false;
				if(modifier.matches(".*?\\bnot\\b.*")){
					negation = true;
					modifier = modifier.replace("not", "").trim();
				}
				if(relation.matches(".*?\\bnot\\b.*")){
					negation = true;
					relation = relation.replace("not", "").trim();
				}
				relas.add(addRelation(relation, modifier, symmetric, o1id, o2id, negation, "based_on_text", cs));
			}
		}
		return relas;
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

	private Element addRelation(String relation, String modifier,
			boolean symmetric, String o1id, String o2id, boolean negation, String inferencemethod, ChunkedSentence cs) { // 7-12-02 add cs
		Element rela = new Element("relation");
		//if(this.inbrackets){rela.setAttribute("in_bracket", "true");}
		rela.setAttribute("id", "r"+this.relationid);
		this.relationid++;
		rela.setAttribute("name", relation);
		rela.setAttribute("from", o1id);
		rela.setAttribute("to", o2id);
		rela.setAttribute("negation", negation+"");
		//rela.setAttribute("symmetric", symmetric+"");
		//rela.setAttribute("inference_method", inferencemethod);
		//if(modifier.length()>0 && modifier.indexOf("m[")>=0){
		if(modifier.length()>0){
			addAttribute(rela, "modifier", modifier.replaceAll("m\\[|\\]", ""));
		}
		addClauseModifierConstraint(cs, rela);
		this.addScopeConstraints(cs, rela);
		this.statement.addContent(rela); //add to statement
		return rela;
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
		value = value.replaceAll("(\\w+\\[|\\]|\\{|\\}|\\(|\\))", "").replaceAll("\\s+;\\s+", ";").replaceAll("\\[", "").trim();
		if(value.indexOf("LRB-")>0) value = NumericalHandler.originalNumForm(value);
		value = value.replaceAll("\\b("+this.notInModifier+")\\b", "").trim();
		if(this.evaluation && attribute.startsWith("constraint_")) attribute="constraint"; 
		if(value.length()>0){
			if(value.indexOf("moreorless")>=0){
				value = value.replaceAll("moreorless", "more or less");
			}
			value = value.replaceAll(" , ", ", ").trim();
			String v = e.getAttributeValue(attribute);
			if(v==null || !v.matches(".*?(^|; )"+value+"(;|$).*")){
				if(v !=null && v.trim().length() > 0){
					v = v.trim()+ ";"+value;
				}else{
					v = value;
				}
				if(attribute.equals("constraintid")) v = v.replaceAll("\\W", " "); //IDREFS are space-separated
				e.setAttribute(attribute, v);
			}
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
		String result = "part_of";
		try{
			Statement stmt = conn.createStatement();
			
			for (int i = 0; i<organsbeforeOf.size(); i++){
				String b = organsbeforeOf.get(i).getAttributeValue("name");
				if(b.matches("("+ChunkedSentence.clusters+")")){
					result = "consist_of";
					break;
				}
				for(int j = 0; j<organsafterOf.size(); j++){
					String a = organsafterOf.get(j).getAttributeValue("name");
					//String pattern = a+"[ ]+of[ ]+[0-9]+.*"+b+"[,\\.]"; //consists-of
					if(a.length()>0 && b.length()>0){
						String pb = Utilities.plural(b);
						String pa = Utilities.plural(a);
						String pattern = "("+b+"|"+pb+")"+"[ ]+of[ ]+[0-9]+.*"+"("+a+"|"+pa+")"+"[ ]?(,|;|\\.|and|or|plus)"; //consists-of
						String query = "select * from "+this.tableprefix+"_sentence where sentence rlike '"+pattern+"'";
						ResultSet rs  = stmt.executeQuery(query);
						if(rs.next()){
							result = "consist_of";
							break;
						}
						rs.close();
					}
				}				
			}	
			stmt.close();
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
		}

		return result;
	}

	//separate o[......... {m} {m} (o1) and {m} (o2)] to two parts: the last part include all organ names
	//e.g., o[(cypselae) -LSB-/-LSB- {minute} (crowns) -RSB-/-RSB-]
	//o[{small} {carinal} -LRB-/-LRB- r[p[under] o[the (ridges)]] -RRB-/-RRB- and {larger} {vallecular} -LRB-/-LRB- r[p[under] o[the (valleys)]] -RRB-/-RRB- (canals)]
	private String[] separate(String object) {
		String[] twoparts  = new String[2];
		object = object.replaceFirst("^o\\[", "").replaceFirst("\\]$", "").replaceAll("<", "(").replaceAll(">", ")");
		String part2 = "";
		int objectstart = 0;
		if(object.startsWith("(") || object.indexOf("(")<0){
			part2 = object;
		}else if(object.indexOf(" (")>=0+0){
			objectstart = object.indexOf(" (");
			//do not separate a pair of brackets into two parts
			part2 = object.substring(objectstart); //object='1–5(–15+) (series)'
			part2 = part2.replaceFirst(" \\(", "~("); //to avoid match this bad object again in while loop
			int left = part2.replaceAll("-L[RS]B-/-L[RS]B-", "#").replaceAll("[^#]", "").length();
			int right = part2.replaceAll("-R[RS]B-/-R[RS]B-", "#").replaceAll("[^#]", "").length();;
			while(left!=right){
				objectstart += part2.indexOf(" (");
				part2 = part2.substring(part2.indexOf(" ("));
				left = part2.replaceAll("-L[RS]B-/-L[RS]B-", "#").replaceAll("[^#]", "").length();
				right = part2.replaceAll("-R[RS]B-/-R[RS]B-", "#").replaceAll("[^#]", "").length();
				part2 = part2.replaceFirst(" \\(", "~("); //to avoid match this bad object again in while loop
			}
		//}else if(object.lastIndexOf(" ")>=0){
		//	part2 = object.substring(object.lastIndexOf(" ")).trim();
		}//else{
		//	part2 = object;
		//}
		part2 = part2.replaceAll("~\\(", " (").trim();
		String part1 = object.substring(0, objectstart);
		//String part1 = object.replace(part2, "").trim();
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
				String escaped = ws1[i].replaceAll("\\{", "\\\\{").replaceAll("\\}", "\\\\}");
				if(constraintType(ws1[i].replaceAll("\\W", ""), o)!=null){
					part1 = part1.replaceFirst("\\s*"+escaped+"$", "");
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
	 * TODO: flower and leaf blades???
	 * @param ck: {} (), {} (), () and/or ()
	 * @return
	 */
	private ArrayList<Element> createStructureElements(String listofstructures/*, boolean makeconstraint*/, ChunkedSentence cs)  throws Exception{// 7-12-02 add cs
		ArrayList<Element> results = new ArrayList<Element>();	
		//String[] organs = listofstructures.replaceAll(" (and|or|plus) ", " , ").split("\\)\\s*,\\s*"); //TODO: flower and leaf blades???
		String[] organs = listofstructures.replaceAll(",", " , ").split("\\)\\s+(and|or|plus|,)\\s+"); //TODO: flower and leaf blades???		
		//mohan 28/10/2011. If the first organ is a preposition then join the preposition with the following organ
		for(int i = 0; i<organs.length; i++){
			if(organs[i].matches("\\{r\\[p\\[.*\\]\\]\\}\\s+\\{.*\\}\\s+.*"))
			{
				organs[i] = organs[i].replaceAll("\\]\\]\\}\\s\\{", "]]}-{");
			}
		}
		String[] sharedcharacters = null;
		for(int i = 0; i<organs.length; i++){
			String bracketed = null;
			String[] organ = organs[i].trim().split("\\s+");
			//organs[i]: "(cypselae) -LSB-/-LSB- {minute} (crowns) -RSB-/-RSB-"
			//for each organ mentioned, find organ name
			String o = "";
			int j = 0;
			for(j = organ.length-1; j >=0; j--){
				//if(organ[j].startsWith("(")){ //(spine tip)
				/*if(organ[j].endsWith(")") || organ[j].startsWith("(")){ //(spine tip)	
					o = organ[j]+" "+o;
					organ[j] = "";
				}else{
					break;
				}*/
				//collect text in brackets if any
				if(organ[j].matches("-R[RS]B-/-R[SR]B-")){
					//collect everything in brackets
					int lr = 0;
					int ls = 0;
					if(organ[j].equals("-RRB-/-RRB-"))	lr--;
					if(organ[j].equals("-RSB-/-RSB-"))	ls--;
					bracketed = organ[j]+" ";
					while(j-1 >=0 && (lr != 0 || ls !=0)){
						j--;
						if(organ[j].equals("-LRB-/-LRB-"))	lr++;
						if(organ[j].equals("-LSB-/-LSB-"))	ls++;
						if(organ[j].equals("-RRB-/-RRB-"))	lr--;
						if(organ[j].equals("-RSB-/-RSB-"))	ls--;
						bracketed = organ[j] + " "+bracketed;
					}
					j--;//continue checking the next j
				}
				if(organ[j].endsWith(")") || organ[j].startsWith("(")){ //(spine tip)	
					o = organ[j]+" "+o;
					organ[j] = "";
					break; //take the last organ name
				}
			}
			o = o.replaceAll("(\\w\\[|\\]|\\(|\\))", "").trim();
			//create element, 
			Element e = new Element("structure");
			String strid = "o"+this.structid;
			this.structid++;
			e.setAttribute("id", strid);
			//e.setAttribute("name", o.trim()); //must have.
			e.setAttribute("name", o.contains("taxonname-")? o.replaceAll("-taxonname-", ". ").replaceAll("taxonname-", "") : Utilities.toSingular(o.trim())); //must have. //corolla lobes
			//if(this.inbrackets){e.setAttribute("in_bracket", "true");}
			addScopeConstraints(cs, e);
			this.statement.addContent(e);
			results.add(e); //results only adds e
			
			if(bracketed !=null){
				bracketed = bracketed.trim();
				String text = bracketed.replaceAll("-[RL][RS]B-/-[RL][RS]B-", "").trim();
				this.addAttribute(e, "alter_name", text);
				if(bracketed.startsWith("-LSB-")){
					this.addAttribute(e, "geographical_constraint", "outside of North American");
				}else{
					this.addAttribute(e, "in_bracket", "true");
				}
			}
			
			//determine constraints
			while(j>=0 && organ[j].trim().length()==0){
				j--;
			}
			//cauline leaf abaxial surface trichmode hair long
			boolean terminate =false;
			boolean distribute = false;
			String constraint = "";//plain
			for(;j >=0; j--){
				if(terminate) break;
				
				String w = organ[j].replaceAll("(\\w+\\[|\\]|\\{\\(|\\)\\}|\\(\\{|\\}\\))", "");
				//mohan code to make w keep all the tags for a preposition chunk
				if(organ[j].matches("\\{?r\\[p\\[.*"))
				{
					w = organ[j];
				}
				//end mohan code//
				if(w.equals(",")){
					distribute = true;
					continue;
				}
				String type = null;
				if(w.startsWith("(") && !w.matches(".*?\\d.*")) type="parent_organ";
				else type = constraintType(w, o);
				if(type!=null){
					organ[j] = "";
					constraint = w+" " +constraint; //plain
					//fancy:
					/*if(type.equals("type")){
						if(distribute){//outer , mid phyllaries => distribute "phyllaries" to "outer"
							//create element, 
							Element e1 = new Element("structure");
							if(this.inbrackets){e1.setAttribute("in_bracket", "true");}
							e1.setAttribute("id", "o"+this.structid);
							this.structid++;
							e1.setAttribute("name", Utilities.toSingular(o.trim())); //must have. //corolla lobes
							addAttribute(e1, "constraint_"+type, w.replaceAll("(\\(|\\))", "").trim()); //may not have.	
							this.statement.addContent(e1);
							results.add(e1); //results only adds e
							distribute = false;
						}else{
							addAttribute(e, "constraint_"+type, w.replaceAll("(\\(|\\))", "").trim()); //may not have.	
						}
					}else{//"parent_organ": collect all until a null constraint is found
						String constraint = w;
						j--;
						for(; j>=0; j--){
							w = organ[j].replaceAll("(\\w+\\[|\\]|\\{\\(|\\)\\})", "");
							if(w.startsWith("(")) type="parent_organ";
							else type = constraintType(w, o);
							if(type!=null){
								constraint = w+" "+constraint;
								organ[j] = "";
							}
							else{
								addAttribute(e, "constraint_parent_organ", constraint.replaceAll("(\\(|\\))", "").trim()); //may not have.
								terminate = true;
								if(this.partofinference){
									driveRelationFromStructrueContraint(strid, "part_of", constraint);
								}
								constraint = "";
								break;
							}
						}
						if(constraint.length()>0){
							addAttribute(e, "constraint_parent_organ", constraint.replaceAll("(\\(|\\)|\\}|\\{)", "").trim()); //may not have.
							terminate = true;
							if(this.partofinference){
								driveRelationFromStructrueContraint(strid, "part_of", constraint);
							}
							constraint = "";
							break;							
						}
					}*/
				}else{
					break;
				}				
			}
			j++;
			//plain
			if(constraint.trim().length() >0){
				addAttribute(e, "constraint", constraint.replaceAll("(\\(|\\))", "").trim()); //may not have.
			}
			//plain
			
			//determine character/modifier
			ArrayList<Element> list = new ArrayList<Element>();
			list.add(e);
			//process text reminding in organ
			if(organ[0].trim().length()>0){//has c/m remains, may be shared by later organs
				sharedcharacters = organ;
			}else if(sharedcharacters !=null){//share c/m from a previous organ
				organ = sharedcharacters;
			}
			processCharacterText(organ, list, null, cs);// 7-12-02 add cs //characters created here are final and all the structures will have, therefore they shall stay local and not visible from outside
		}
		return results;
	}


	/**
	 * cauline leaf abaxial surface thin trichomode hair
	 * constraint_type: trichomode
	   constraint_parent_organ: cauline leaf abaxial surface
	 * @param fromid: from_id
	 * @param relation: "part_of"
	 * @param toorganname: use this to find the to_id
	 */
	private void driveRelationFromStructrueContraint(String fromid,
			String relation, String toorganname, ChunkedSentence cs) { // 7-12-02 add cs
		try{
			//try to link toorganname to an previously mentioned organ
			List<Element> structures = XPath.selectNodes(this.statement, ".//structure");
			Iterator<Element> it = structures.iterator();
			boolean exist = false;
			while(it.hasNext()){
				Element structure = it.next();
				String name = structure.getAttributeValue("name");
				if(structure.getAttribute("constraint_type")!=null){
					String tokens = structure.getAttributeValue("constraint_type"); //need to reverse order
					tokens = reversed(tokens);
					name =tokens +" "+name;
				}
				if(structure.getAttribute("constraint_parent_organ")!=null){
					name = structure.getAttributeValue("constraint_parent_organ")+" "+name;
				}
				
				if(name.equals(toorganname)){
					exist = true;
					String toid = structure.getAttributeValue("id");
					addRelation(relation, "", false, fromid, toid, false, "based_on_parent_organ_constraint", cs); // 7-12-02 add cs
					break;
				}
			}
			if(!exist){ //create a new structure
				addRelation(relation, "", false, fromid, "o"+this.structid, false, "based_on_parent_organ_constraint", cs);// 7-12-02 add cs
				toorganname = toorganname.replaceFirst(" (?=\\w+$)", " (")+")"; //format organname
				if(toorganname.indexOf('(')<0) toorganname="("+toorganname;
				this.createStructureElements(toorganname, cs);// 7-12-02 add cs				
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
		}
		
	}
	/**
	 * turn "b;a" to "a b"
	 * @param tokens
	 * @return
	 */
	private String reversed(String tokens) {
		String[] ts = tokens.split("\\s*;\\s*");
		String result = "";
		for(int i = ts.length-1; i>=0; i--){
			result += i+" ";
		}
		return result.trim();
	}

	/**
	 * 
	 * @param chunked: "m[sometimes 1-2+] pinnated "
	 * @param parents
	 * @param character
	 * @return
	 */
	private ArrayList<Element> processCharacterText(String chunked, ArrayList<Element> parents, String character, ChunkedSentence cs)  throws Exception {// 7-12-02 add cs
		ArrayList<Element> results = new ArrayList<Element>();
		String modifiers;
		String word;
		if(chunked.indexOf("m[")==0){
			String[] parts = chunked.split("\\]");
			modifiers = parts[0].replaceFirst("^m\\[", "").trim();
			word = parts[1].trim();
		}else{
			modifiers = "";
			word = chunked;
		}
		createCharacterElement(parents, results,
				modifiers, word, character, "", cs);// 7-12-02 add cs
		return results;
	}

	
	
	/**
	 * bases and tips mostly rounded 
	 * @param tokens
	 * @param parents
	 */
	private ArrayList<Element> processCharacterText(String[] tokens, ArrayList<Element> parents, String character, ChunkedSentence cs)  throws Exception{// 7-12-02 add cs
		ArrayList<Element> results = new ArrayList<Element>();
		ArrayList<Element> localresults = new ArrayList<Element>();
		ArrayList<Element> newelements = new ArrayList<Element>();
		//determine characters and modifiers
		String modifiers = "";
		for(int j = 0; j <tokens.length; j++){
//<<<<<<< .mine
			/*if(tokens[j].trim().length()>0){
				tokens[j] = NumericalHandler.originalNumForm(tokens[j]);
				//Mohan code to fix colorttt
				//if(tokens[j].contains("colorttt")){
				//	tokens[j]=tokens[j].replaceAll("-", "~");
				//}
					//End mohan code
				if(tokens[j].indexOf("~list~")>=0){
					results = this.processCharacterList(tokens[j], parents);*/
//=======
			if(tokens[j].trim().length()>0){ //nested: "{inner} -LRB-/-LRB- functionally {staminate} -LSB-/-LSB- {bisexual} -RSB-/-RSB- -RRB-/-RRB-" 
				if(tokens[j].matches("-L[RS]B-/-L[SR]B-")){
					//collect everything in brackets
					int lr = 0;
					int ls = 0;
					if(tokens[j].equals("-LRB-/-LRB-"))	lr++;
					if(tokens[j].equals("-LSB-/-LSB-"))	ls++;
					String bracketed = tokens[j]+" ";
					while(j+1 < tokens.length && (lr != 0 || ls !=0)){
						j++;
						if(tokens[j].equals("-LRB-/-LRB-"))	lr++;
						if(tokens[j].equals("-LSB-/-LSB-"))	ls++;
						if(tokens[j].equals("-RRB-/-RRB-"))	lr--;
						if(tokens[j].equals("-RSB-/-RSB-"))	ls--;
						bracketed += tokens[j]+" ";
					}
					//bracketed += tokens[j];
					bracketed = bracketed.trim();
					if(bracketed.startsWith("-LRB-/-LRB- r[")){
						//[{small}, {carinal}, -LRB-/-LRB-, r[p[under], o[the, (ridges)]], -RRB-/-RRB-, and, {larger}, {vallecular}, -LRB-/-LRB-, r[p[under], o[the, (valleys)]], -RRB-/-RRB]
						//bracketed: -LRB-/-LRB- r[p[under] o[the (ridges)]] -RRB-/-RRB-
						newelements = (ArrayList<Element>)results.clone();
						newelements.removeAll(localresults);
						localresults.addAll(results);
						for(Element newelement : newelements)
							this.addAttribute(newelement, "constraint", bracketed.replaceAll("-[RL][RS]B-/-[RL][RS]B-?", "").replaceAll("(\\w+\\[|\\]|\\(|\\))", "").trim());
					}else{
						createCharacterElement(parents, results,
								"", bracketed.trim(), "", "",cs);
						//localresults.addAll(results);
					}
//>>>>>>> .r1182
				}else{
/*<<<<<<< .mine
					String w = tokens[j];
					String chara= null;
					if(tokens[j].matches("\\w{2,}\\[.*")){
						chara=tokens[j].substring(0, tokens[j].indexOf('['));
						w = tokens[j].replaceAll("(\\w+\\[|\\]|\\{|\\})", "");
					}else if(tokens[j].matches("\\w\\[.*")){
						w = tokens[j].replaceAll("(\\w+\\[|\\]|\\{|\\})", "");
					}
					w = w.replaceAll("(\\{|\\})", "");
					//w = w.replaceAll("(\\{|\\})", "").replaceAll("colorttt-list-", ""); //to remove colorttt-list-
					chara = Utilities.lookupCharacter(w, conn, ChunkedSentence.characterhash, glosstable, tableprefix);
					if(chara==null && w.matches("no")){
						chara = "presence";
					}
					if(chara==null && Utilities.isAdv(w, ChunkedSentence.adverbs, ChunkedSentence.notadverbs)){//TODO: can be made more efficient, since sometimes character is already given
						modifiers +=w+" ";
					}else if(w.matches(".*?\\d.*") && !w.matches(".*?[a-z].*")){//TODO: 2 times =>2-times?

						results = this.annotateNumericals(w, "count", modifiers, parents, false);
						//annotateCount(parents, w, modifiers);
						modifiers = "";
=======*/
					tokens[j] = NumericalHandler.originalNumForm(tokens[j]);
					if(tokens[j].indexOf("~list~")>=0){
						results = this.processCharacterList(tokens[j], parents, cs);// 7-12-02 add cs
//>>>>>>> .r1182
					}else{
						String w = tokens[j];
						String chara= null;
						if(tokens[j].matches("\\w{2,}\\[.*")){
							chara=tokens[j].substring(0, tokens[j].indexOf('['));
							w = tokens[j].replaceAll("(\\w+\\[|\\]|\\{|\\})", "");
						}else if(tokens[j].matches("\\w\\[.*")){
							w = tokens[j].replaceAll("(\\w+\\[|\\]|\\{|\\})", "");
						}
						w = w.replaceAll("(\\{|\\})", "");
						String[] charainfo = (Utilities.lookupCharacter(w, conn, ChunkedSentence.characterhash, glosstable, tableprefix));
						if(charainfo==null && w.matches("no")){
							chara = "presence";
						}
						if(charainfo==null && Utilities.isAdv(w, ChunkedSentence.adverbs, ChunkedSentence.notadverbs)){//TODO: can be made more efficient, since sometimes character is already given
							modifiers +=w+" ";
						}else if(w.matches(".*?\\d.*") && !w.matches(".*?[a-z].*")){//TODO: 2 times =>2-times?
							results = this.annotateNumericals(w, "count", modifiers, parents, false, cs); //added cs
							//annotateCount(parents, w, modifiers);
							modifiers = "";
						}else{
							//String chara = MyPOSTagger.characterhash.get(w);
							if(charainfo != null){
								chara = charainfo[0];
								if(character!=null){
									chara = character;
								}
								if(chara.compareToIgnoreCase("character")==0 && modifiers.length() ==0){//high relief: character=relief, reset the character of "high" to "relief"
									Element lastelement = null;
									if(results.size()>=1){
										lastelement = results.get(results.size()-1);
									}else if(this.latestelements.size()>=1){
										lastelement = this.latestelements.get(this.latestelements.size()-1);
									}
									if(lastelement != null && lastelement.getName().compareTo("character")==0){
										lastelement.setAttribute("name", w);
										/*Iterator<Element> it = this.latestelements.iterator();
										while(it.hasNext()){
											lastelement = it.next();
											lastelement.setAttribute("name", w);
										}*/
									}
								}else{
									w = w + (charainfo[1].length()>0? "_"+charainfo[1]+"_" : "");//attach syn info to the word w
									createCharacterElement(parents, results,
											modifiers, w, chara, "",cs);// 7-12-02 add cs //default type "" = individual vaues
									modifiers = "";
									//localresults.addAll(results);
								}
							}
						}
					}
					
				}
			}
		}
		return results;
	}

	private String createCharacterElement(ArrayList<Element> parents,
			ArrayList<Element> results, String modifiers, String cvalue, String cname, String char_type, ChunkedSentence cs) throws Exception{// 7-12-02 add cs
		String parenthetical = null;
		Element character = null;
		if(cvalue.indexOf("( ")>=0){ //contains parenthetical, textual expressions:  lanceolate ( outer ) as part of a character list; brackets in numerical expressions do not have a trailing space
			parenthetical = cvalue.substring(cvalue.indexOf("( ")).trim();
			cvalue = cvalue.substring(0, cvalue.indexOf("( ")).trim();
		}
		if(cvalue.matches("^-[RL][SR]B-/-[RL][SR]B-.*")){ //other textual, parenthetical expressions has -[RL][SR]B-/-[RL][SR]B- as a separate token
			parenthetical = cvalue;
			cvalue = "";
		}
		
		if(cvalue.length() > 0){
			character = new Element("character");
			//if(this.inbrackets){character.setAttribute("in_bracket", "true");}
			if(cname.compareTo("count")==0 && cvalue.indexOf("-")>=0 && cvalue.indexOf("-")==cvalue.lastIndexOf("-")){
				String[] values = cvalue.split("-");
				character.setAttribute("char_type", "range_value");
				character.setAttribute("name", cname);
				character.setAttribute("from", values[0]);
				character.setAttribute("to", values[1]);
			}else{
				if(cname.compareTo("size")==0){
					String value = cvalue.replaceFirst("\\b("+ChunkedSentence.units+")\\b", "").trim(); //5-10 mm
					String unit = cvalue.replace(value, "").trim();
					if(unit.length()>0){character.setAttribute("unit", unit);}
					cvalue = value;
				}else if(cvalue.indexOf("-c-")>=0 && (cname.compareTo("color") == 0 || cname.compareTo("coloration") ==0)){//-c- set in SentenceOrganStateMarkup
					String color = cvalue.substring(cvalue.lastIndexOf("-c-")+3); //pale-blue
					String m = cvalue.substring(0, cvalue.lastIndexOf("-c-")); //color = blue m=pale
					modifiers = modifiers.length()>0 ? modifiers + ";"+ m : m;
					cvalue = color;
				}
				if(char_type.length() >0){
					character.setAttribute("char_type", char_type);
				}
				character.setAttribute("name", cname);
				character.setAttribute("value", cvalue);
			}
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
/*<<<<<<< .mine
			if(cvalue.startsWith("colorttt-list-")){
				cvalue=cvalue.replaceAll("colorttt-list-", "");
			}
			character.setAttribute("name", cname);
			character.setAttribute("value", cvalue);
=======*/
			if(usedm){
				modifiers = "";
			}
			addClauseModifierConstraint(cs, character);
			this.addScopeConstraints(cs, character);
//>>>>>>> .r1182
		}
		//parenthetical expression
		if(parenthetical !=null){
			if(this.printParenthetical) System.out.println("Process parenthetical expression...");
			//update latest element
			ArrayList<Element> copy = this.latestelements;
			String text = parenthetical.replaceAll("(^-[RL][SR]B-/-[RL][SR]B-|-[RL][SR]B-/-[RL][SR]B-$|^\\(|\\)$)", "").trim(); //remove the first and the last
			if(!(text.startsWith("z[") || text.startsWith("u[") || text.startsWith("l["))){ //a character chunk, need parent structure
				if(parents.size()>0) this.updateLatestElements(parents); //n[{thinner} than phyllaries]
			}else{
				if(results.size()>0) this.updateLatestElements(results); //results / parents, default to results, assuming expression in brackets are constraints about a character --> the latest results
			}
			
			//categorize parenthetical expressions: x, g, q
			//q:
			boolean followcomma = false;
			int p = cs.getPointer();
			if(cs.getTokenAt(p-1).contains(text)){
				if(cs.getTokenAt(p-1).matches(".*?[.,]\\W*"+text.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)").replaceAll("\\{", "\\\\{").replaceAll("\\}", "\\\\}").replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]"))){
					followcomma = true;
				}
			}else if(cs.getTokenAt(p-2)==null || cs.getTokenAt(p-2).matches(".*[,;]\\W*$")){
				followcomma = true;
			}


			if(parenthetical.startsWith("-LSB-/-LSB-")){//g
				ChunkScopeGeo chunk = new ChunkScopeGeo("g["+text+"]");
				Attribute geo = new Attribute("geographical_constraint", "outside of North America");//create a constraint attribute
				annotateScopeChunk(chunk, geo, cs);

			}else if(text.contains("taxonname-")){//t

				ArrayList<String[]> charataxas = ChunkedSentence.separateCharaTaxa(text); //TOFIX: 2396, 5937
				for(int c = 0; c < charataxas.size(); c++){
					String[] chartaxa = charataxas.get(c);
					//is there a taxon for each result? if not, look ahead and look behind to find one
					String taxon = chartaxa[1];
					if(chartaxa[1].length()==0){
						if(c-1 >=0 && charataxas.get(c-1)[1].length()>0){
							taxon = charataxas.get(c-1)[1];
						}else if(c+1 < charataxas.size() && charataxas.get(c+1)[1].length()>0){
							taxon = charataxas.get(c+1)[1];
						}
					}
					ChunkScopeTaxa chunk = new ChunkScopeTaxa("x["+chartaxa[0]+(chartaxa[0].length()>0? " ": "")+"("+taxon+")]");
					processChunkScopeTaxa(chunk, cs);
				}
			}else if(followcomma){//q
				//if there is a comma before left bracket
				Attribute parallelism = new Attribute("parallelism_constraint", "possible");//create a constraint attribute
				annotateScopeChunk(new ChunkScopeParallelism(text), parallelism, cs);
			}else{
				processChunkBracketed(new ChunkBracketed(text), cs);
			}
			this.latestelements = copy;
			//turn expressions to chunks to annotate
			//character may be null
			/*simplied test code
			parenthetical = parenthetical.replaceAll("[()]", "");
			if(character==null){
				character = results.get(results.size()-1);
			}
			this.addAttribute(character, "other_constraint", parenthetical);
			*/
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
	private ArrayList<Element> annotateCount(ArrayList<Element> parents, String w, String modifiers, ChunkedSentence cs) {// 7-12-02 add cs
		// TODO Auto-generated method stub
		String modifier = w.replaceFirst("\\d.*", "").trim();
		String number= w.replace(modifier, "").trim();
		ArrayList<Element> e = new ArrayList<Element>();
		Element count = new Element("character");
		//if(this.inbrackets){count.setAttribute("in_bracket", "true");}
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
		addClauseModifierConstraint(cs, count);
		this.addScopeConstraints(cs, count);
		return e;
	}

	

	//if w has been seen used as a modifier to organ o
	private String constraintType(String w, String o) {
		String result = null;
		
		//mohan code to make w keep all the tags for a preposition chunk
		if(w.matches("\\{?r\\[p\\[.*"))//for cases such as "with the head in full face view, the midpoint blah blah....", "r[p[with head] {in-fullface-view}]" is treated as a "condition" constraint
		{
			return "condition";
		}
		//mohan code ends.
		w = w.replaceAll("\\W", "");
		String[] chinfo = Utilities.lookupCharacter(w, conn, ChunkedSentence.characterhash, this.glosstable, tableprefix);
		if(chinfo!=null && chinfo[0].matches(".*?_?(position|insertion|structure_type)_?.*") && w.compareTo("low")!=0) return "type";
		String singlew = Utilities.toSingular(w);
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select * from "+this.tableprefix+"_sentence where tag = '"+w+"' or tag='"+singlew+"'");
			if(rs.next()){
				return "parent_organ";
			}
			//rs = stmt.executeQuery("select * from "+this.tableprefix+"_sentence where modifier = '"+w+"' or modifier like '"+w+" %' or modifier like '% "+w+" %' or modifier like '% "+w+"'");
			rs = stmt.executeQuery("select * from "+this.tableprefix+"_sentence where modifier = '"+w+"'");
			if(rs.next()){
				return "type";
			}
			rs.close();
			stmt.close();
		}catch(Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			LOGGER.error(sw.toString());
		}
		return result;
	}


	//public void setInBrackets(boolean b){
	//	this.inbrackets = b;
	//}
	
	/**
	 * 
	 * @param measurements: CI 72 - 75 (74 ), SI 99 - 108 (102 ), PeNI 73 - 83 (73 ), LPeI 46 - 53 (46 ), DPeI 135 - 155 (145 ). 
	 */	
	private void annotatedMeasurements(String measurements, ChunkedSentence cs) {//added cs
		measurements = measurements.replaceAll("([Ff]igure|[Ff]igs?\\.)[^A-Z]*", "");
		measurements = measurements.replaceAll("–", "-");
		Element whole  = new Element("whole_organism");
		this.statement.addContent(whole);
		ArrayList<Element> parent = new ArrayList<Element>();
		parent.add(whole);
		//select delimitor
		int comma = measurements.replaceAll("[^,]", "").length();
		int semi = measurements.replaceAll("[^;]", "").length();
		String del = comma > semi ? "," : ";";
		String[] values = measurements.split(del);
		for(int i = 0; i < values.length; i++){
			String value = values[i].replaceFirst("[,;\\.]\\s*$", "");
			//separate char from values
			String chara = value.replaceFirst("\\s+\\d.*", "");
			chara = chara.replaceAll("\\(", "\\(");
			chara = chara.replaceAll("\\)", "\\)");
			String vstring = value.replaceFirst("^"+chara, "").trim();
			//seperate modifiers from vlu in case there is any
			String vlu = vstring.replaceFirst("\\s+[a-zA-Z].*", "").trim();
			String modifier = vstring.substring(vlu.length()).trim();
			modifier = modifier.length()>0? "m["+modifier+"]" : null;
			vlu = vlu.replaceAll("(?<=\\d)\\s*\\.\\s*(?=\\d)", ".");
			this.annotateNumericals(vlu.trim(), chara.trim(), modifier, parent, false, cs); //added cs
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
