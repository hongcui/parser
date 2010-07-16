/**
 * 
 */
package fna.charactermarkup;
import org.jdom.*;
import org.jdom.input.*;
import org.jdom.xpath.*;

import fna.parsing.state.SentenceOrganStateMarker;

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
	private String currentsubject = null;
	private Element currentmainstructure = null;
	private int structid = 1;
	private int relationid = 1;
	private SentenceOrganStateMarker sosm = null;
	/**
	 * 
	 */
	public CharacterAnnotatorChunked(int sentindex, String sentsrc, ChunkedSentence cs, Document doc, SentenceOrganStateMarker sosm) {
		this.sosm = sosm;
		this.root = doc.getRootElement();
		this.cs = cs;
		this.sentsrc = sentsrc;
		this.statement = new Element("statement");
		this.statement.setAttribute("id", "s"+sentindex);
	}
	
	/**
	 * output annotated sentence in XML format
	 * Chunk types:
	 * PrepChunk, IVerbChunk (Intransitive verb chunk, followed by a preposition), VerbChunk, ADJChunk, SBARChunk,
	 * etc.
	 * @return
	 */
	public Element annotate() throws Exception{
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
		
	}
	/**
	 * 
	 * @param np: in state, modifier, organ pattern
	 * @return the ids of the <structure>s created here
	 */
	private ArrayList annotateNP(String np) {
		ArrayList ids = new ArrayList();
		String marked = sosm.markASentence(this.sentsrc, np);
		if(marked.endsWith(">")){ //ends with an organ
			String[] tokens = marked.split("\\s+");
			for(int i = tokens.length-1; i>=0; i--){
			
			}
		}else{//e.g. in {corymbiform} or {paniculiform} arrays
			//insert a character in the last mentioned organ, which may or may not be the currentmainstructure
		}
		return ids;
	}

	private Element createRelationElement(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	private Element createStructureElement(String currentsubject2,
			String modifier, int i) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
