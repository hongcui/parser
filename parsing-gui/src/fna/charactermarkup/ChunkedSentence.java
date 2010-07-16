/**
 * 
 */
package fna.charactermarkup;

import org.jdom.*;
import org.jdom.xpath.*;

import fna.parsing.state.WordNetWrapper;
import fna.parsing.state.StateCollector;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 
 * @author hongcui
 *	This class generates a chunked sentence from the parsing tree and provides a set of access methods to facilitate final annotation.
 *	A chunked sentence is a marked sentence (with organs enclosed by <> and states by {}) with "chunks" of text enclosed by [], for example 
 *	<Heads> 3 , {erect} , [in corymbiform or paniculiform arrays]. (sent. 302)
 *
 *	the annotation of a chunk may require access to the original parsing tree, but that is not handled by this class.
 */
public class ChunkedSentence {
	private Document tree = null;
	private String markedsent = null;
	private String chunkedsent = null;
	private ArrayList<String> chunkedtokens = null;
	private int pointer = 0; //pointing at the next chunk to be annotated
	private static final String units= "cm|mm|dm|m";
	private static final String times = "times|folds";
	private static final String per = "per";
	private static final String prepositions = "above|across|after|along|around|as|at|before|beneath|between|beyond|by|for|from|in|into|near|of|off|on|onto|out|outside|over|throughout|toward|towards|up|upward|with|without";
	private int sentid = -1;
	private boolean printNorm = true;
	private boolean printNormThan = true;
	private boolean printNormTo = true;
	
	

	/**
	 * @param tobechunkedmarkedsent 
	 * @param tree 
	 * 
	 */
	public ChunkedSentence(int id, Document collapsedtree, Document tree, String tobechunkedmarkedsent) {
		this.tree = tree;
		this.sentid = id;
		this.markedsent = tobechunkedmarkedsent;
		tobechunkedmarkedsent = tobechunkedmarkedsent.replaceAll("[\\[\\(]", " -LRB-/-LRB- ").replaceAll("[\\]\\)]", " -RRB-/-RRB- ").replaceAll("\\s+", " ").trim(); 

		String[] temp = normalizeNumberExp(tobechunkedmarkedsent).split("\\s+");
		chunkedtokens = new ArrayList<String>(Arrays.asList(temp));
		int totaltokens = temp.length;
		Element root = collapsedtree.getRootElement();
		String treetext = SentenceChunker.allText(root).trim();
		String[] treetoken = treetext.split("\\s+");
		String realchunk = "";
		ArrayList<String> brackets = new ArrayList<String>();
		int i = 0;
		for(; i<treetoken.length; i++){
			if(treetoken[i].indexOf('[') >=0){
				int bcount  = treetoken[i].replaceAll("[^\\[]", "").length();
				for(int j = 0; j < bcount; j++){
					brackets.add("[");
				}
			}
			if(brackets.size()>0){//in
				realchunk += treetoken[i]+" ";
				chunkedtokens.set(i, "");
			}
			if(brackets.size()==0 && realchunk.length()>0){
				chunkedtokens.set(i-1, realchunk.trim());
				realchunk="";
			}
			if(treetoken[i].indexOf(']')>=0){
				int bcount  = treetoken[i].replaceAll("[^\\]]", "").length();
				for(int j = 0; j < bcount; j++){
					brackets.remove(0);
				}
			}
		}
		if(realchunk.length()>0){
			chunkedtokens.set(i-1, realchunk.trim());
		}		
		this.chunkedsent = "";
		normalizeOtherINs(); //find objects for those VB/IN that without
		normalizeThan();
		normalizeTo();
		
		//insert segment marks in chunkedtokens while producing this.chunkedsent
		for(i = this.chunkedtokens.size()-1; i>=0; i--){
			String t = this.chunkedtokens.get(i);
			if(t.compareTo("") !=0){
				this.chunkedsent = t+" "+this.chunkedsent;;
			}
			if(t.indexOf('<')>=0){
				for(i = i-1; i>=0; i--){
					String m = this.chunkedtokens.get(i);		
					if(m.matches("(,|;|:)")){
						this.chunkedtokens.set(i, "SG"+m+"SG"); //insert a segment mark
						this.chunkedsent = "SG"+m+"SG"+" "+this.chunkedsent;
						break;
					}else{
						if(m.compareTo("") !=0){
							this.chunkedsent = m+" "+this.chunkedsent;
						}
					}
					
				}
			}
		}
		/*Iterator<String> it = chunkedtokens.iterator();
		while(it.hasNext()){
			String t = it.next();
			if(t.compareTo("") !=0){
				this.chunkedsent += t+" ";
			}
		}*/
		this.chunkedsent.trim();		
	}
	
	/**
	 * shorter and wider than ...
	 * more/less smooth than ...
	 * pretty good now
	 */
	private void normalizeThan(){
		String np = "";
		int thani = 0;
		int firstmorei = this.chunkedtokens.size();
		String more = "";
		if(this.markedsent.indexOf("than") >=0 ){
			if(this.printNormThan){
				System.out.println("Need to normalize Than! "+np);
			}
			for(int i = 0; i<this.chunkedtokens.size(); i++){
				//scan for JJRs
				String token = this.chunkedtokens.get(i);
				if(more.length()==0 && (token.matches(".*?\\b(\\w+er|more|less)\\b.*") && (token.indexOf("<")<0)|| this.markedsent.indexOf(token+" than")>=0)){ //<inner> is not, but <longer> than is 
					firstmorei = i;
					if(token.matches(".*?\\bmore\\b.*")){
						more = "more";
					}else if(token.matches(".*?\\b\\w+er\\b.*")){
						more = "er";
					}
				}else if(more.compareTo("er") == 0 && !token.matches(".*?\\b(\\w+er|more|less|and|or|than)\\b.*") ){
					more = "";
					firstmorei = this.chunkedtokens.size();;
				}
				if(token.matches(".*?\\bthan\\b.*")){
					//needs normalization
					thani = i;
					if(firstmorei < thani){
						//join all tokens between firstmorei and thani--this is the subject of "than"
						for(int j = firstmorei; j<=thani; j++){
							if(this.chunkedtokens.get(j).length()>0){
								np += this.chunkedtokens.get(j)+" ";
							}
							this.chunkedtokens.set(j, "");
						}
						
						//scan for the object of "than"
					
						for(i=i+1; i<this.chunkedtokens.size(); i++){
							String w = this.chunkedtokens.get(i).replaceAll("[<>{}\\]\\[]", "");
							if(w.matches("\\b("+this.prepositions+"|and|or|that|which|but)\\b") || w.matches("\\W")){
								np = np.replaceAll("[<>{}]", "").trim();
								this.chunkedtokens.set(thani, "["+np+"]");
								break;
							}else{
								if(this.chunkedtokens.get(i).length()>0){
									np += this.chunkedtokens.get(i)+" ";
								}
								this.chunkedtokens.set(i, "");
							}
						}
						if(this.printNormThan){
							System.out.println("Normalize Than! "+np);
						}
						thani = 0;
						firstmorei = this.chunkedtokens.size();
						np = "";
					}
				}
			
			}
		}
	}
	
	/**
	 * expanded to <throats>
	 */
	private void normalizeTo(){
		String np = "";
		boolean startn = false;
		//ArrayList<String> copy = (ArrayList<String>)this.chunkedtokens.clone();
		for(int i = 0; i<this.chunkedtokens.size(); i++){
			ArrayList<String> copy = (ArrayList<String>)this.chunkedtokens.clone();
			String token = this.chunkedtokens.get(i);
			if(token.compareTo("to") == 0 || token.matches(".*?\\bto]$")){
				
				//scan for the next organ
				for(int j = i+1; j<this.chunkedtokens.size(); j++){
					String t = this.chunkedtokens.get(j).trim();
					if(startn && t.indexOf('<')<0){
						break;
					}
					if(t.matches("[,:;\\d]") || t.matches(".*?\\b("+this.prepositions+"|and|or|that|which|but)\\b.*")){
						break;
					}
					np +=t+" ";
					this.chunkedtokens.set(j, "");
					if(t.lastIndexOf(' ') >=0){
						t = t.substring(t.lastIndexOf(' ')); //last word there
					}
					if(t.indexOf('<')>=0){ //t may have []<>{}
						startn = true; //not break yet, may be the next token is a noun
					}
				}
				if(!startn){
					this.chunkedtokens = copy; //not finding the organ, reset
				}else{
					if(this.printNormTo){
						System.out.println("To needs normalization!");
					}
					np = "to "+np;
					//scan forward for the start of the chunk
					boolean startc = false; //find the start of the chunk
					for(int j = i-1; j>=0; j--){
						String t = this.chunkedtokens.get(j);
						if(t.matches(".*?\\b("+this.prepositions+"|and|or|that|which|but)\\b.*") || t.matches(".*?[>;,:].*")){
							np = np.replaceAll("[<>{}]", "").replaceAll("\\s+", " ").trim();
							this.chunkedtokens.set(i, "["+np+"]"); //replace "to" with np
							startn = false;
							startc = true;
							if(this.printNormTo){
								System.out.println("!normalizedTo! "+np);
							}
							break;
						}else{	
							np = t+" "+np;
							this.chunkedtokens.set(j, "");
						}
					}
					if(!startc){
						this.chunkedtokens = copy; //not finding the start of the chunk, reset
					}
			}
		}
	}
}
	/**
	 * most [of] lengths
	 * [in] zyz arrays
	 */
	private void normalizeOtherINs(){
		ArrayList copy = (ArrayList)this.chunkedtokens.clone();
		boolean startn = false;
		for(int i = 0; i<this.chunkedtokens.size(); ){
			String token = this.chunkedtokens.get(i);
			if((token.indexOf('[')>=0 && token.indexOf(' ')<0 && token.matches(".*?\\w+.*")) || token.matches("\\b("+this.prepositions+")\\b")){//[of]
				if(this.printNorm){
					System.out.println(token+" needs normalization!");
				}
				String np = "";
				for(int j = i+1; j<this.chunkedtokens.size(); j++){
					String t = this.chunkedtokens.get(j).trim();
					if(startn && t.indexOf('<')<0 && !isNoun(t)){
						break;
					}
					np +=t+" ";
					this.chunkedtokens.set(j, "");
					if(t.lastIndexOf(' ') >=0){
						t = t.substring(t.lastIndexOf(' ')); //last word there
					}
					if(t.indexOf('<')>=0 || isNoun(t)){ //t may have []<>{}
						startn = true; //not break yet, may be the next token is a noun
						if(this.printNorm){
							System.out.println("!normalized!");
						}
					}
				}
				np = np.replaceAll("[<>{}]", "");
				if(token.indexOf('[')>=0){
					this.chunkedtokens.set(i, token.replaceFirst("\\]$", " "+np.trim()+"]"));
				}else{
					this.chunkedtokens.set(i, "["+token+" "+np.trim()+"]");
				}
			}else{
				i++;
			}
		}
		if(!startn){
			this.chunkedtokens = copy;
		}
	}
	
	private boolean isNoun(String word){
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
			return pos.compareTo("noun") == 0;
		}else{
			return false;
		}
		//return wnw.isN(); //may be more strict by using wnw.formchange
	}
	/**
	 * 
	 * @param tobechunkedmarkedsent: e.g. <Florets> 4–25 [ –60 ] , {bisexual} , {fertile} ;
	 * @return <Florets> 4–25[–60] , {bisexual} , {fertile} ;
	 */
	private String normalizeNumberExp(String sentence) {
		String norm = "";
		Pattern p = Pattern.compile("(.*?)("+MyPOSTagger.numberpattern+")(.*)");
		Matcher m = p.matcher(sentence);
		while(m.matches()){
			sentence  = m.group(3);
			norm += m.group(1);
			norm += " "+m.group(2).replaceAll("\\s+", "")+" ";
			m = p.matcher(sentence);
		}
		norm += sentence;
		return norm.trim();
	}

	public String toString(){
		return this.chunkedsent;
	}
	public int getPointer(){
		return this.pointer;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public boolean hasNext(){
		if(pointer <this.chunkedtokens.size()){
			return true;
		}
		return false;
	}
	
	/**
	 * returns the next Chunk: may be a
	 * Organ, Value, Comparative Value, SimpleCharacterState, Subclause,
	 * PrepChunk, IVerbChunk (Intransitive verb chunk, followed by a preposition), VerbChunk, ADJChunk
	 * @return
	 */
	public Chunk nextChunk(){
		Chunk chunk = null;
		String token = this.chunkedtokens.get(pointer); //a token may be a word, a phrase, or a chunk of text
		
		//cases need additional processing
		if(token.indexOf("<") >=0){ //organ name
			chunk = getNextOrgan();
			return chunk;
		}
		if(token.matches(".*?\\d+$")){ //ends with a number
			chunk = getNextNumerics();
			return chunk;
		}
		if(token.matches(".*?-$")){ //ends with a hyphen
			chunk = getNextBroken();
			return chunk;
		}
		
		//all other cases
		String type = checkType(pointer);
		try{
			if(type != null){
				Class c = Class.forName(type);
				Constructor cons = c.getConstructor(String.class);
				return (Chunk)cons.newInstance(token.trim());
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return new SimpleCharacterState(token); //default
		//that, when, which, where, whenever, etc.
		//more than, as ... as
	}
	

	/**
	 * 
	 * @return e.g. 3 cm, what about "3 cm to 10 dm"?
	 * also 3 times (... longer than, as wide as ...)
	 */
	private Chunk getNextBroken() {
		String result = "";
		String type = "";
		boolean found = false;
		for(int i = pointer; i<this.chunkedtokens.size(); i++){
			if(this.chunkedtokens.get(i).matches(".*?-")){ //ends with a hyphen
				result += this.chunkedtokens.get(i)+ " ";
				found = true;
				type = checkType(i);
			}
			if(found){
				result += this.chunkedtokens.get(i)+ " ";
				pointer = i+1;
				try{
					if(type != null){
						Class c = Class.forName(type);
						Constructor cons = c.getConstructor(String.class);
						return (Chunk)cons.newInstance(result.replaceAll("[<>]", "").trim());
					}else{
						return new SimpleCharacterState(result.replaceAll("[<>]", "").trim());
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @return e.g. 3 cm, what about "3 cm to 10 dm"?
	 * also 3 times (... longer than, as wide as ...)
	 */
	private Chunk getNextNumerics() {
		String numerics = "";
		boolean found = false;
		for(int i = pointer; i<this.chunkedtokens.size(); i++){
			if(this.chunkedtokens.get(i).matches(".*?\\d+$")){ //ends with a number
				numerics += this.chunkedtokens.get(i)+ " ";
				found = true;
			}
			if(found && this.chunkedtokens.get(i).matches("^("+this.units+")\\b.*?")){
				numerics += this.chunkedtokens.get(i)+ " ";
				pointer = i+1;
				return new Value(numerics.replaceAll("[<>]", "").trim());
			}
			if(found && this.chunkedtokens.get(i).matches("^("+this.times+")\\b.*?")){
				numerics += this.chunkedtokens.get(i)+ " ";
				pointer = i+1;
				return new ComparativeValue(numerics.replaceAll("[<>]", "").trim());
			}
			if(found && this.chunkedtokens.get(i).matches("^("+this.per+")\\b.*?")){
				numerics += this.chunkedtokens.get(i)+ " ";
				pointer = i+1;
				return new BasedCount(numerics.replaceAll("[<>]", "").trim());
			}
			if(found){
				pointer = i+1;
				return new Count(numerics.replaceAll("[<>]", "").trim());
			}
			
		}
		return null;
	}
	/**
	 * 
	 * @return e.g. leaf blade, apex of inner, organ of 5 parts, 
	 */
	public Chunk getNextOrgan() {
		String organ = "";
		boolean found = false;
		for(int i = pointer; i<this.chunkedtokens.size(); i++){
			if(this.chunkedtokens.get(i).indexOf("<") >=0){
				organ += this.chunkedtokens.get(i)+ " ";
				found = true;
			}
			if(this.chunkedtokens.get(i).indexOf("<") <0 && found){
				pointer = i+1;
				return new Organ(organ.replaceAll("[<>]", "").trim());
			}
		}
		return null;
	}
	
	/**
	 * use the un-collapsedTree (this.tree) to check the type of a chunk with the id, 
	 * @param i
	 * @return: PrepChunk, IVerbChunk (Intransitive verb chunk, followed by a preposition), VerbChunk, ADJChunk, SBARChunk
	 */
	private String checkType(int id) {
		try{
			Element root = tree.getRootElement();
			Element el = (Element)XPath.selectSingleNode(root, ".//*/@id['"+id+"']");
			String name = el.getName();
			if(name.compareTo("PP") == 0){
				return "PrepChunk";
			}
			if((name.compareTo("ADJP") == 0 || name.compareTo("ADVP") == 0 || name.compareTo("RRC") == 0 ) && el.getChild("PP") != null){
				return "ADJChunk";
			}
			if(name.compareTo("VP") == 0 && el.getChild("PP") != null){
				return "IVerbChunk";
			}
			if(name.compareTo("VP") == 0 && el.getChild("PP") == null){
				return "VerbChunk";
			}
			if(name.compareTo("SBAR") == 0){
				return "SBARChunk";
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}

}
