/**
 * 
 */
package fna.charactermarkup;

import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * @author hong
 * This class extract possible relationships from a parsing tree, which is represented in XML format
 * It also takes the marked sentence on which the parsing tree was generated, e.g. <leaves> {big}.
 * Since either source could contain errors, the class tries to make the best guess possible.
 * 
 * 
 * Penn Tags that we care:
 * VB, verb; VBD, verb, past tensel VBG, verb present participle or gerund; VBN, verb, past participle; VBP, verb, present, not 3rd person sinugular; and VBZ, verb, present tense, 3rd person singular
 * PP (IN), preposition;
 * JJR/JJS, adjective, comparative/superlative
 * RBR/RBS, adverb, comparative/superlative; RB, adverb
 * WHADVP, wh-adverb phrase
 * (QP (IN at) (JJS least) (CD 3/4)))
 * 
 */
public class ExtractRelationFromXML {
	private Document tree = null;
	//private String markedsent = null;
	private String [] tokensinsent = null;
	private String [] posoftokens = null;
	/**
	 * 
	 */
	public ExtractRelationFromXML(Document parsingTree, String markedsent) {
		this.tree = parsingTree;
		//in markedsent, each non-<>{}-punctuation mark is surrounded with spaces
		this.tokensinsent = markedsent.split("\\s+");
		this.posoftokens = this.tokensinsent;
		for(int i =0; i<posoftokens.length; i++){
			if(this.tokensinsent[i].indexOf("<")>=0 && this.tokensinsent[i].indexOf("{")>=0){
				this.posoftokens[i]="NN";
			}else if(this.tokensinsent[i].indexOf("<")>=0){
				this.posoftokens[i]="NN";
			}else if(this.tokensinsent[i].indexOf("{")>=0){
				this.posoftokens[i]="ADJ";
			}else{
				this.posoftokens[i]="";
			}
		}		
	}

	public ArrayList<Relation> extract(){
		ArrayList<Relation> results = new ArrayList<Relation>();
		//check to see if the tree used the POS tag provided by markedsent, if not, return empty list.
		if(POSMatch()){
			//hide complex number patterns should already be hidden before the tree was produced			
			try{
				Element root = tree.getRootElement();
				//collapse QPs
				List<Element> QPs = XPath.selectNodes(root, "//QP");
				Iterator<Element> it = QPs.iterator();
				while(it.hasNext()){
					Element QP = it.next();
					collapseElement(QP);									
				}
				
				//get All PP/IN
				List<Element> PPINs = null;
				do{
					PPINs = XPath.selectNodes(root, "//PP/IN");
					it = PPINs.iterator();
					ArrayList<Element> lPPINs = new ArrayList<Element>();
					while(it.hasNext()){
						Element PPIN = it.next();
						if(XPath.selectNodes(PPIN, "//PP/IN").size() == 0){
							lPPINs.add(PPIN);
						}
					}
					extractFromlPPINs(lPPINs, results);
				}while (PPINs !=null || PPINs.size() > 0);	
				
				//get remaining VBs
				List<Element> VBs = null;
				do{
					VBs = XPath.selectNodes(root, "//VP/VBD|//VP/VBG|//VP/VBN|//VP/VBP|//VP/VBZ|//VP/VB");
					it = VBs.iterator();
					ArrayList<Element> lVBs = new ArrayList<Element>();
					while(it.hasNext()){
						Element VB = it.next();
						if(XPath.selectNodes(VB, "//VP/VBD|//VP/VBG|//VP/VBN|//VP/VBP|//VP/VBZ|//VP/VB").size() == 0){
							lVBs.add(VB);
						}
					}
					extractFromlVBs(lVBs, results);
					
				}while(VBs != null || VBs.size() > 0);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		return results;
	}
	
	
	
	private void extractFromlVBs(ArrayList<Element> lVBs, ArrayList<Relation> results) {
		Iterator<Element> it = lVBs.iterator();
		while(it.hasNext()){
			Element lVB = it.next();
			extractFromlVB(lVB, results);
		}		
	}

	private void extractFromlVB(Element lVB, ArrayList<Relation> results) {
		// TODO Auto-generated method stub
		
	}

	private void extractFromlPPINs(ArrayList<Element> lPPINs, ArrayList<Relation> results) {
		Iterator<Element> it = lPPINs.iterator();
		while(it.hasNext()){
			Element lPPIN = it.next();
			extractFromlPPIN(lPPIN, results);
		}
	}
	
	
	/*
	 * (NP
                  (NP (NN extension))
                  (PP (IN of)
                    (NP (NN air))))
	 */
	private void extractFromlPPIN(Element lPPIN, ArrayList<Relation> results) {
		if(lPPIN.getAttribute("text").getValue().length()<2){ //text of IN is not a word, e.g. "x"
			return;
		}
		Element PP = lPPIN.getParentElement();
		Element child = PP.getChild("NP");
		boolean chaso = containsOrgan(child);
		//both child and parent must contain an organ name to extract a relation
		//if child has no organ name, extract a constraint, location, "leaves on ...??
		//if parent has no organ name, collapse the NP. "distance of ..."
		Element parent = PP.getParentElement();//could be PP, NP, VP or UCP, others?
		boolean phaso = containsOrgan(parent); 
		if(chaso && phaso){
			//extract relation
		}else if(chaso && !phaso){
			
		}else if(!chaso && phaso){
			
		}
		collapseElement(parent);
	}

	private void collapseElement(Element e) {
		String text = allText(e);
		e.removeContent();
		e.getAttribute("text").setValue(text);	
		
	}

	/**
	 * 
	 * @param e
	 * @return true if allText of this element contains an organ name, i.e., marked with <> in markedsent
	 */
	private boolean containsOrgan(Element e) {
		try{
			List<Element> nouns = XPath.selectNodes(e, "//NN");
			Iterator<Element> it = nouns.iterator();
			while(it.hasNext()){
				Element noun = it.next();
				String word = noun.getAttribute("text").getValue();
				String index = noun.getAttribute("id").getValue();
				int i = Integer.parseInt(index);
				if(this.posoftokens[i].compareToIgnoreCase("NN")==0){
					return true;
				}				
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return false;
	}

	/**
	 *
	 * @param e
	 * @return a concatnation of value of all text attributes of this element and its descendants
	 */
	private String allText(Element e) {
		StringBuffer sb = new StringBuffer();
		Attribute t = e.getAttribute("text");
		if(t!=null){
			sb.append(t.getValue()+" ");
		}		
		Iterator<Content> it = e.getDescendants();
		while(it.hasNext()){
			Content cont = it.next();
			if(cont instanceof Element){
				t = ((Element)cont).getAttribute("text");
				if(t!=null){
					sb.append(t.getValue()+" ");
				}
			}
		}		
		return sb.toString().trim();
	}

	/**
	 * establish a mapping between the words of markedsent and the tree 
	 * @param markedsent
	 * @param tree
	 * @return
	 */
	boolean POSMatch() {		
		Iterator<Content> it = this.tree.getDescendants();
		int c = 0;
		while(it.hasNext()){
			Content cont = it.next();
			if(cont instanceof Element){
				Attribute t = ((Element)cont).getAttribute("text");
				if(t!=null){ //only leaf node has a text attribute
					String word=t.getValue();
					String pos = ((Element)cont).getName();
					if(pos.compareToIgnoreCase("PUNCT") != 0){
						if(this.tokensinsent[c].compareToIgnoreCase(word)!=0){
							System.err.println(c+"th token in sentence does not match that in the tree");
							System.exit(1);
						}
						if(this.posoftokens[c].compareTo("") !=0 && this.posoftokens[c].compareToIgnoreCase(pos)!=0){
							return false;
						}
					}
					c++;
				}
			}
		}
		return true;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Document doc = null;
		String xml="<root><e1 text='e1'><e11 text='e11'></e11></e1><e2 text='e2'></e2></root>";
		try {
		     SAXBuilder builder = new SAXBuilder();
		     ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
		     doc = builder.build(bais);
		    } catch (Exception e) {
		      System.out.print("Problem parsing the xml: \n" + e.toString());
		}
		Iterator<Content> it = doc.getDescendants();
			while(it.hasNext()){
				Content cont = it.next();
				if(cont instanceof Element){
					System.out.println(cont.toString());
					Attribute t = ((Element)cont).getAttribute("text");
					if(t!=null){
						String word=t.getValue();
						System.out.println(word);
					}
					
				}
			}   
		    

	}

}

/*examples
check markedsent and find the parts that need to be checked, 
for example: 
to-phrases like "reduced to", "longer than", "same as", "when ...", "in ... view"
one segment has 2 organs

****1 VP is ignored because no NN in it
(ROOT
  (S
    (NP (JJ Apical) (NN flagellomere))
    (VP (VBZ is)
      (NP (DT the) (JJS longest)))
    (. .)))

(ROOT
  (S
    (NP (NNS Leaves))
    (VP (VBD emersed) (, ,)
      (S
        (VP
          (ADVP (RB rarely))
          (VBG floating) (, ,)
          (ADVP (RB petiolate)))))
    (. .)))

(ROOT
  (NP
    (NP
      (NP (NNS Leaves))
      (VP (VBN emersed) (, ,)
        (ADVP (RB rarely))))
    (NP
      (NP (VBG floating))
      (, ,)
      (NP (JJ petiolate)))
    (. .)))

(ROOT
  (NP
    (NP (NNS Leaves))
    (ADJP (JJ emersed) (, ,) (RB rarely) (JJ floating) (, ,) (JJ petiolate))
    (. .)))
    
*****1 IN x: ignored
(ROOT
  (S
    (NP
      (NP (NN body) (NN ovoid))
      (, ,)
      (NP
        (NP (CD 2-4))
        (PP (IN x)
          (NP
            (NP (CD 1-1.5) (NN mm))
            (, ,)
            (ADJP (RB not) (JJ winged)))))
      (, ,))
    (VP (VBZ woolly))
    (. .)))

(ROOT
  (NP
    (NP
      (NP (NN body) (JJ ovoid))
      (, ,)
      (NP
        (NP (CD 2-4))
        (PP (IN x)
          (NP (CD 1-1.5) (NN mm))))
      (, ,))
    (ADJP (RB not) (JJ winged) (, ,) (JJ woolly))
    (. .)))

(ROOT
  (NP
    (NP
      (NP
        (NP (NNS teeth))
        (NP (CD 5))
        (, ,)
        (ADVP (CD erect) (TO to) (CD spreading)))
      (, ,)
      (NP (CD 1)))
    (: -)
    (NP (CD 3) (NN mm))
    (. .)))

(ROOT
  (FRAG
    (NP
      (NP (NNS teeth) (CD 5))
      (, ,)
      (ADJP (JJ erect) (TO to) (JJ spreading)))
    (, ,)
    (NP (CD 1/-3) (NN mm))
    (. .)))

******"when young" is not a relation but a constraint
(ROOT
  (S
    (NP (JJ basal))
    (VP (VBP rosette)
      (UCP
        (ADJP (JJ absent)
          (CC or)
          (RB poorly) (JJ developed))
        (CC and)
        (VP (VBG withering)
          (SBAR
            (WHADVP (WRB when))
            (S
              (ADJP (JJ young)))))))
    (. .)))

(ROOT
  (NP
    (NP
      (NP (JJ basal) (NN rosette))
      (UCP
        (ADJP (JJ absent)
          (CC or)
          (RB poorly) (JJ developed))
        (CC and)
        (VP (VBG withering)
          (SBAR
            (WHADVP (WRB when))
            (S
              (ADJP (JJ young)))))))
    (. .)))

(ROOT
  (NP
    (NP
      (NP (NNP Ray))
      (ADJP (JJ laminae)
        (NP (CD 6))))
    (: -)
    (NP
      (NP
        (NP (CD 7) (NNS x))
        (NP (CD 2/CD-32) (NN mm)))
      (, ,)
      (PP (IN with)
        (NP (CD 2))))
    (: -)
    (NP
      (NP (CD 5) (NNS hairs))
      (PP (IN inside)
        (NP
          (NP (NN opening))
          (PP (IN of)
            (NP (NN tube))))))
    (. .)))

****nested PPs (not in VB)
****1. start with the PP(IN with no other PP(IN in it
****2. extract the relation then replace the of-phrase with the full string NP e.g. (NP opening of tube)
****3. go to step 1
(ROOT
  (NP
    (NP (NN Ray) (NNS laminae))
    (NP
      (NP
        (QP (CD 6-7) (IN x) (CD 2-32))
        (NN mm))
      (, ,)
      (PP (IN with)
        (NP
          (NP (CD 2-5) (NNS hairs))
          (PP (IN inside)
            (NP
              (NP (NN opening))
              (PP (IN of)
                (NP (NN tube))))))))
    (. .)))



(ROOT
  (S
    (VP (VBZ veins)
      (NP
        (NP (CD 1))
        (, ,)
        (UCP
          (ADJP (RB mostly) (JJ prominent))
          (, ,)
          (ADJP
            (ADVP (RB longer)
              (PP (IN than)
                (NP
                  (NP (NN extension))
                  (PP (IN of)
                    (NP (NN air))))))
            (JJ spaces))
          (CC or)
          (VP (VBG running)
            (PP (IN through)
              (NP
                (NP
                  (QP (IN at) (JJS least) (CD 3/4)))
                (PP (IN of)
                  (NP
                    (NP (NN distance))
                    (PP (IN between)
                      (NP (NN node)
                        (CC and)
                        (NN apex)))))))))))
    (. .)))
    
    
****1 Collapse QP phrases
****2 collapse "extension of air spaces":extension is not an organ name
****3 collapse "distance between node and apex": distance is not an organ name
****4 do "longer than extension...": relation "longer than", collapse the ADJP node
****5 collapse "at least 3/4 of distance"
****6 do "running through": relation, collapse the VP node
****in 4 and 6, use the depth of the relation node to find entity1
(ROOT
  (NP
    (NP
      (NP (NNS veins) (CD 1))
      (, ,)
      (UCP
        (ADJP (RB mostly) (JJ prominent))
        (, ,)
        (ADJP (JJR longer)
          (PP (IN than)
            (NP
              (NP (NN extension))
              (PP (IN of)
                (NP (NN air) (NNS spaces))))))
        (CC or)
        (VP (VBG running)
          (PP (IN through)
            (NP
              (NP
                (QP (IN at) (JJS least) (CD 3/4)))
              (PP (IN of)
                (NP
                  (NP (NN distance))
                  (PP (IN between)
                    (NP (NN node)
                      (CC and)
                      (NN apex))))))))))
    (. .)))

****1 if decurrent is marked {decurrent} in markedsent BUT the tree has it as a VB, then report failure.
****2 otherwise, the PP (on distal phyllary margines) should be a constraint to the JJ decurrent.
(ROOT
  (S
    (NP (JJ erect) (NNS appendages))
    (VP (VB decurrent)
      (PP (IN on)
        (NP (JJ distal) (NN phyllary) (NNS margins)))
      (, ,)
      (NP (JJ dark) (JJ brown)
        (CC or)
        (JJ black) (, ,) (NNS scarious)))
    (. .)))

(ROOT
  (NP
    (NP (JJ erect) (NNS appendages))
    (ADJP
      (ADJP (JJ decurrent)
        (PP (IN on)
          (NP (JJ distal) (NN phyllary) (NNS margins))))
      (, ,)
      (ADJP (JJ dark) (JJ brown))
      (CC or)
      (ADJP (JJ black))
      (, ,)
      (ADJP (JJ scarious)))
    (. .)))

(ROOT
  (S
    (S
      (VP (VBZ stems)
        (S
          (VP (VBG arising)
            (PP
              (PP (IN at)
                (NP
                  (NP (NNS nodes))
                  (PP (IN of)
                    (NP (JJ caudex) (NNS branches)))))
              (CC and)
              (PP (IN at)
                (NP
                  (NP (JJ distal) (NNS nodes))
                  (PP (IN of)
                    (NP (JJ short))))))))))
    (, ,)
    (NP
      (NP (JJ nonflowering) (JJ aerial) (NNS branches))
      (, ,)
      (NP (CD 1-4) (NN dm))
      (, ,))
    (ADVP (RB essentially))
    (VP (VBZ glabrous))
    (. .)))

****1 do "nodes of caudex branches" => relation
****2 do "distal nodes of short, nonflowering aerial branches" => relation
****3 do "arising at nodes" => relation
****4 do "arising at distal nodes" =>relation How to get to arising in this case? the (CC and) is a clue?
*

(ROOT
  (NP
    (NP (NNS stems))
    (VP (VBG arising)
      (PP
        (PP (IN at)
          (NP
            (NP (NNS nodes))
            (PP (IN of)
              (NP (NN caudex) (NNS branches)))))
        (CC and)
        (PP (IN at)
          (NP
            (NP
              (NP (JJ distal) (NNS nodes))
              (PP (IN of)
                (NP (JJ short) (, ,) (JJ nonflowering) (JJ aerial) (NNS branches))))
            (, ,)
            (NP (CD 1-4) (NN dm))
            (, ,)
            (ADJP (RB essentially) (JJ glabrous))))))
    (. .)))

(ROOT
  (S
    (NP
      (NP (JJ Rhizomes) (NN horizontal))
      (, ,)
      (VP (VBG creeping)
        (PP (IN at)
          (CC or)
          (IN near)
          (NP (NN surface))))
      (, ,))
    (VP (VBN branched))
    (. .)))

*****1 do "at or near surface", treat it as a constraint to state "creeping"
(ROOT
  (S
    (NP
      (NP (NNS Rhizomes) (JJ horizontal))
      (, ,)
      (VP (VBG creeping)
        (PP (IN at)
          (CC or)
          (IN near)
          (NP (NN surface))))
      (, ,))
    (VP (JJ branched))
    (. .)))

(ROOT
  (S
    (NP (NNS achenes))
    (ADVP (RB obliquely))
    (VP
      (VP (VBD ovoid))
      (, ,)
      (NP
        (NP (CD 1) (NN mm))
        (, ,)
        (ADJP (RB abaxially) (JJ rounded))
        (, ,)
        (PP (IN with)
          (NP (CD 1) (JJ abaxial) (NN groove)))))
    (. .)))

*****1 use the only organ name in the sentence as entity1 of the relation with. Not the nearest NP, which is mm.
***** use the closest organ name? consider not only the distance but also the depths of the nodes.  
(ROOT
  (NP
    (NP
      (NP (NNS achenes))
      (ADVP (RB obliquely)))
    (ADJP (JJ ovoid))
    (, ,)
    (NP
      (NP (CD 1) (NN mm))
      (, ,)
      (ADJP (RB abaxially) (JJ rounded)))
    (, ,)
    (PP (IN with)
      (NP (CD 1) (JJ abaxial) (NN groove)))
    (. .)))

(ROOT
  (S
    (NP
      (NP (JJ Drupe) (NN red))
      (, ,)
      (NP (NN globose))
      (, ,))
    (VP (VBN seated)
      (PP (IN in)
        (NP (JJ small) (, ,) (JJ single-rimmed) (NN cupule))))
    (. .)))

(ROOT
  (S
    (NP
      (NP (NN Drupe) (JJ red))
      (, ,)
      (ADJP (JJ globose))
      (, ,))
    (VP (VBN seated)
      (PP (IN in)
        (NP (JJ small) (, ,) (JJ single-rimmed) (NN cupule))))
    (. .)))

(ROOT
  (S
    (VP (VBZ stamens)
      (NP
        (NP (RB mostly) (CD 6-10))
        (, ,)
        (NP
          (NP
            (QP (RB as) (JJ few) (IN as) (CD 3)))
          (PP (IN in)
            (NP (JJR more) (JJ distal) (NNS flowers))))))
    (. .)))
****1 unlike the previous example, here "in more distal flowers" should be a constraint for "as few as 3" 
****2. output of the extract function should be arrayList of things (relation and state/constraint)?
(ROOT
  (NP
    (NP (NNS stamens))
    (NP
      (NP (RB mostly) (CD 6-10))
      (, ,)
      (NP
        (NP
          (QP (RB as) (JJ few) (IN as) (CD 3)))
        (PP (IN in)
          (NP (JJR more) (JJ distal) (NNS flowers)))))
    (. .)))

(ROOT
  (S
    (NP (NNS Capsules))
    (VP (VBD exserted) (, ,)
      (S
        (NP (NNS valves))
        (VP (VBG separating)
          (PP (IN at)
            (NP (NN dehiscence))))))
    (. .)))
*****separating at a time??
*****1 dehiscence is not an organ.so this is not a relation.
(ROOT
  (NP
    (NP (NNS Capsules) (JJ exserted))
    (, ,)
    (NP
      (NP (NNS valves))
      (VP (VBG separating)
        (PP (IN at)
          (NP (NN dehiscence)))))
    (. .)))

(ROOT
  (S
    (NP
      (NP (NNP Capsules) (NNP brown))
      (, ,)
      (VP (VBN ellipsoid))
      (, ,))
    (VP (VBZ shorter)
      (PP (IN than)
        (NP (NNS tepals))))
    (. .)))

(ROOT
  (NP
    (NP (NNS Capsules))
    (ADJP
      (ADJP (JJ brown))
      (, ,)
      (ADJP (JJ ellipsoid))
      (, ,)
      (ADJP (JJR shorter)
        (PP (IN than)
          (NP (NNS tepals)))))
    (. .)))

(ROOT
  (S
    (NP
      (NP (NNS Heads))
      (ADJP (RB mostly) (VBN scattered)))
    (ADVP (RB along))
    (VP (VBZ stems))
    (. .)))

*****1 search for PP(IN
*****2 then VBs "scattered along stems"
(ROOT
  (S
    (NP (NNS Heads))
    (ADVP (RB mostly))
    (VP (VBN scattered)
      (PRT (RB along))
      (NP (NNS stems)))
    (. .)))


Bracket Labels
Clause Level

S - simple declarative clause, i.e. one that is not introduced by a (possible empty) subordinating conjunction or a wh-word and that does not exhibit subject-verb inversion.
SBAR - Clause introduced by a (possibly empty) subordinating conjunction.
SBARQ - Direct question introduced by a wh-word or a wh-phrase. Indirect questions and relative clauses should be bracketed as SBAR, not SBARQ.
SINV - Inverted declarative sentence, i.e. one in which the subject follows the tensed verb or modal.
SQ - Inverted yes/no question, or main clause of a wh-question, following the wh-phrase in SBARQ.
Phrase Level
ADJP - Adjective Phrase.
ADVP - Adverb Phrase.
CONJP - Conjunction Phrase.
FRAG - Fragment.
INTJ - Interjection. Corresponds approximately to the part-of-speech tag UH.
LST - List marker. Includes surrounding punctuation.
NAC - Not a Constituent; used to show the scope of certain prenominal modifiers within an NP.
NP - Noun Phrase.
NX - Used within certain complex NPs to mark the head of the NP. Corresponds very roughly to N-bar level but used quite differently.
PP - Prepositional Phrase.
PRN - Parenthetical.
PRT - Particle. Category for words that should be tagged RP.
QP - Quantifier Phrase (i.e. complex measure/amount phrase); used within NP.
RRC - Reduced Relative Clause.
UCP - Unlike Coordinated Phrase.
VP - Vereb Phrase.
WHADJP - Wh-adjective Phrase. Adjectival phrase containing a wh-adverb, as in how hot.
WHAVP - Wh-adverb Phrase. Introduces a clause with an NP gap. May be null (containing the 0 complementizer) or lexical, containing a wh-adverb such as how or why.
WHNP - Wh-noun Phrase. Introduces a clause with an NP gap. May be null (containing the 0 complementizer) or lexical, containing some wh-word, e.g. who, which book, whose daughter, none of which, or how many leopards.
WHPP - Wh-prepositional Phrase. Prepositional phrase containing a wh-noun phrase (such as of which or by whose authority) that either introduces a PP gap or is contained by a WHNP.
X - Unknown, uncertain, or unbracketable. X is often used for bracketing typos and in bracketing the...the-constructions.
Word level
CC - Coordinating conjunction
CD - Cardinal number
DT - Determiner
EX - Existential there
FW - Foreign word
IN - Preposition or subordinating conjunction
JJ - Adjective
JJR - Adjective, comparative
JJS - Adjective, superlative
LS - List item marker
MD - Modal
NN - Noun, singular or mass
NNS - Noun, plural
NNP - Proper noun, singular
NNPS - Proper noun, plural
PDT - Predeterminer
POS - Possessive ending
PRP - Personal pronoun
PRP$ - Possessive pronoun (prolog version PRP-S)
RB - Adverb
RBR - Adverb, comparative
RBS - Adverb, superlative
RP - Particle
SYM - Symbol
TO - to
UH - Interjection
VB - Verb, base form
VBD - Verb, past tense
VBG - Verb, gerund or present participle
VBN - Verb, past participle
VBP - Verb, non-3rd person singular present
VBZ - Verb, 3rd person singular present
WDT - Wh-determiner
WP - Wh-pronoun
WP$ - Possessive wh-pronoun (prolog version WP-S)
WRB - Wh-adverb
Function tags
Form/function discrepancies
-ADV (adverbial) - marks a constituent other than ADVP or PP when it is used adverbially (e.g. NPs or free ("headless" relatives). However, constituents that themselves are modifying an ADVP generally do not get -ADV. If a more specific tag is available (for example, -TMP) then it is used alone and -ADV is implied. See the Adverbials section.
-NOM (nominal) - marks free ("headless") relatives and gerunds when they act nominally.
Grammatical role
-DTV (dative) - marks the dative object in the unshifted form of the double object construction. If the preposition introducing the "dative" object is for, it is considered benefactive (-BNF). -DTV (and -BNF) is only used after verbs that can undergo dative shift.
-LGS (logical subject) - is used to mark the logical subject in passives. It attaches to the NP object of by and not to the PP node itself.
-PRD (predicate) - marks any predicate that is not VP. In the do so construction, the so is annotated as a predicate.
-PUT - marks the locative complement of put.
-SBJ (surface subject) - marks the structural surface subject of both matrix and embedded clauses, including those with null subjects.
-TPC ("topicalized") - marks elements that appear before the subject in a declarative sentence, but in two cases only:

   1. if the front element is associated with a *T* in the position of the gap.
   2. if the fronted element is left-dislocated (i.e. it is associated with a resumptive pronoun in the position of the gap). 

-VOC (vocative) - marks nouns of address, regardless of their position in the sentence. It is not coindexed to the subject and not get -TPC when it is sentence-initial.
Adverbials

Adverbials are generally VP adjuncts.

-BNF (benefactive) - marks the beneficiary of an action (attaches to NP or PP).
This tag is used only when (1) the verb can undergo dative shift and (2) the prepositional variant (with the same meaning) uses for. The prepositional objects of dative-shifting verbs with other prepositions than for (such as to or of) are annotated -DTV.
-DIR (direction) - marks adverbials that answer the questions "from where?" and "to where?" It implies motion, which can be metaphorical as in "...rose 5 pts. to 57-1/2" or "increased 70% to 5.8 billion yen" -DIR is most often used with verbs of motion/transit and financial verbs.
-EXT (extent) - marks adverbial phrases that describe the spatial extent of an activity. -EXT was incorporated primarily for cases of movement in financial space, but is also used in analogous situations elsewhere. Obligatory complements do not receive -EXT. Words such as fully and completely are absolutes and do not receive -EXT.
-LOC (locative) - marks adverbials that indicate place/setting of the event. -LOC may also indicate metaphorical location. There is likely to be some varation in the use of -LOC due to differing annotator interpretations. In cases where the annotator is faced with a choice between -LOC or -TMP, the default is -LOC. In cases involving SBAR, SBAR should not receive -LOC. -LOC has some uses that are not adverbial, such as with place names that are adjoined to other NPs and NAC-LOC premodifiers of NPs. The special tag -PUT is used for the locative argument of put.
-MNR (manner) - marks adverbials that indicate manner, including instrument phrases.
-PRP (purpose or reason) - marks purpose or reason clauses and PPs.
-TMP (temporal) - marks temporal or aspectual adverbials that answer the questions when, how often, or how long. It has some uses that are not strictly adverbial, auch as with dates that modify other NPs at S- or VP-level. In cases of apposition involving SBAR, the SBAR should not be labeled -TMP. Only in "financialspeak," and only when the dominating PP is a PP-DIR, may temporal modifiers be put at PP object level. Note that -TMP is not used in possessive phrases.
Miscellaneous
-CLR (closely related) - marks constituents that occupy some middle ground between arguments and adjunct of the verb phrase. These roughly correspond to "predication adjuncts", prepositional ditransitives, and some "phrasel verbs". Although constituents marked with -CLR are not strictly speaking complements, they are treated as complements whenever it makes a bracketing difference. The precise meaning of -CLR depends somewhat on the category of the phrase.

    * on S or SBAR - These categories are usually arguments, so the -CLR tag indicates that the clause is more adverbial than normal clausal arguments. The most common case is the infinitival semi-complement of use, but there are a variety of other cases.
    * on PP, ADVP, SBAR-PRP, etc - On categories that are ordinarily interpreted as (adjunct) adverbials, -CLR indicates a somewhat closer relationship to the verb. For example:
          o Prepositional Ditransitives
            In order to ensure consistency, the Treebank recognizes only a limited class of verbs that take more than one complement (-DTV and -PUT and Small Clauses) Verbs that fall outside these classes (including most of the prepositional ditransitive verbs in class [D2]) are often associated with -CLR.
          o Phrasal verbs
            Phrasal verbs are also annotated with -CLR or a combination of -PRT and PP-CLR. Words that are considered borderline between particle and adverb are often bracketed with ADVP-CLR.
          o Predication Adjuncts
            Many of Quirk's predication adjuncts are annotated with -CLR. 
    * on NP - To the extent that -CLR is used on NPs, it indicates that the NP is part of some kind of "fixed phrase" or expression, such as take care of. Variation is more likely for NPs than for other uses of -CLR. 

-CLF (cleft) - marks it-clefts ("true clefts") and may be added to the labels S, SINV, or SQ.
-HLN (headline) - marks headlines and datelines. Note that headlines and datelines always constitute a unit of text that is structurally independent from the following sentence.
-TTL (title) - is attached to the top node of a title when this title appears inside running text. -TTL implies -NOM. The internal structure of the title is bracketed as usual.

*/