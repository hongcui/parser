/**
 * 
 */
package fna.charactermarkup;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;


/**
 * 
 * @author hongcui
 *	This class generates a chunked sentence from the parsing tree and provides a set of access methods to facilitate final annotation.
 *	A chunked sentence is a marked sentence (with organs enclosed by <> and states by {}) with "chunks" of text enclosed by [], for example 
 *	<Heads> 3 , {erect} , [in corymbiform or paniculiform arrays]. (sent. 302)
 *
 *	the annotation of a chunk may require access to the original parsing tree, but that is not handled by this class.
 */

@SuppressWarnings("unchecked")
public class ChunkedSentence {
	private String glosstable = null;
	private String markedsent = null;
	private String chunkedsent = null;
	private ArrayList<String> chunkedtokens = null;
	@SuppressWarnings("unused")
	private ArrayList<String> charactertokensReversed = new ArrayList<String>();
	private int pointer = 0; //pointing at the next chunk to be annotated
	public static final String locationpp="near|from";
	public static final String units= "cm|mm|dm|m|meter|meters|microns|micron|unes";
	public static final String times = "times|folds|lengths|widths";
	public static final String per = "per";
	public static final String more="greater|more|less|fewer";
	public static final String counts="few|several|many|none|numerous|single|couple";
	public static final String basecounts="each|every|per";
	public static final String clusters="cluster|clusters|involucre|involucres|rosette|rosettes";
	public static final String prepositions = "above|across|after|along|among|amongst|around|as|at|before|beneath|between|beyond|by|for|from|in|into|near|of|off|on|onto|out|outside|over|than|throughout|to|toward|towards|up|upward|with|without";
	public static final String stop = "a|about|above|across|after|along|also|although|amp|an|and|are|as|at|be|because|become|becomes|becoming|been|before|being|beneath|between|beyond|but|by|ca|can|could|did|do|does|doing|done|for|from|had|has|have|hence|here|how|if|in|into|inside|inward|is|it|its|may|might|more|most|near|no|not|of|off|on|onto|or|out|outside|outward|over|should|so|than|that|the|then|there|these|this|those|throughout|to|toward|towards|up|upward|was|were|what|when|where|which|why|with|within|without|would";
	private boolean inSegment = false;
	private boolean rightAfterSubject = false;
	@SuppressWarnings("unused")
	private int sentid = -1;
	private ArrayList<String> pastpointers = new ArrayList<String>();

	private String unassignedmodifier = null;
	//caches
	public static Hashtable<String, String> characterhash = new Hashtable<String, String>();
	public static ArrayList<String> adverbs = new ArrayList<String>();
	public static ArrayList<String> verbs = new ArrayList<String>();
	public static ArrayList<String> nouns = new ArrayList<String>();
	
	protected Connection conn = null;
	/*static protected String username = "root";
	static protected String password = "root";
	static protected String database = "fnav19_benchmark";*/
	
	private boolean printNorm = false;
	private boolean printNormThan = false;
	private boolean printNormTo = false;
	private boolean printExp = false;
	

	
	public ChunkedSentence(ArrayList<String> chunkedtokens, String chunkedsent, Connection conn, String glosstable){
		this.chunkedtokens = chunkedtokens;
		this.chunkedsent = chunkedsent;
		this.conn = conn;
		this.glosstable = glosstable;
	}
	/**
	 * @param tobechunkedmarkedsent 
	 * @param tree 
	 * 
	 */
	public ChunkedSentence(int id, Document collapsedtree, Document tree, String tobechunkedmarkedsent, Connection conn, String glosstable) {
		this.glosstable = glosstable;
		this.conn = conn;
		this.sentid = id;
		this.markedsent = tobechunkedmarkedsent;
		//tobechunkedmarkedsent = tobechunkedmarkedsent.replaceAll("[\\[\\(]", " -LRB-/-LRB- ").replaceAll("[\\]\\)]", " -RRB-/-RRB- ").replaceAll("\\s+", " ").trim(); 
		tobechunkedmarkedsent = tobechunkedmarkedsent.replaceAll("[\\[\\(]", "-LRB-/-LRB-").replaceAll("[\\]\\)]", "-RRB-/-RRB-").trim(); 
		if(tobechunkedmarkedsent.matches(".*?\\d.*")){
			tobechunkedmarkedsent = NumericalHandler.normalizeNumberExp(tobechunkedmarkedsent);
		}
		String[] temp = tobechunkedmarkedsent.split("\\s+");
		chunkedtokens = new ArrayList<String>(Arrays.asList(temp)); //based on markedsent, which provides <>{} tags.
				
		Element root = collapsedtree.getRootElement();
		String treetext = SentenceChunker4StanfordParser.allText(root).trim();
		String[] treetoken = treetext.split("\\s+"); //based on the parsing tree, which holds some chunks.
		String realchunk = "";
		ArrayList<String> brackets = new ArrayList<String>();
		int i = 0;
		//go through treetoken to chunk state lists, and brackets
		for(; i<treetoken.length; i++){
			if(treetoken[i].matches("^\\S+~list~\\S+")){//r[p[of] o[{architecture~list~smooth~or~barbellulate~to~plumose} (bristles)]]
				//String[] parts = treetoken[i].split("~list~");
				//treetoken[i] = parts[0]+"["+parts[1]+"]"; 
				treetoken[i] = treetoken[i].replace("~list~", "[{").replaceAll("\\{(?=\\w{2,}\\[)", "").replaceAll("(?<=~[a-z0-9-]{2,40})(\\}| |$)","}]");
			}
		}		
		for(i = 0; i<treetoken.length; i++){
			if(treetoken[i].indexOf('[') >=0){
				int bcount  = treetoken[i].replaceAll("[^\\[]", "").trim().length();
				for(int j = 0; j < bcount; j++){
					brackets.add("[");
				}
			}

			if(brackets.size()>0){//in
				//restore original number expressions
				String w = treetoken[i].replaceAll("(\\w+\\[|\\])", "");
				realchunk += treetoken[i].replace(w, chunkedtokens.get(i))+" ";
				chunkedtokens.set(i, "");
			}
			
			if(treetoken[i].indexOf(']')>=0){
				int bcount  = treetoken[i].replaceAll("[^\\]]", "").trim().length();
				for(int j = 0; j < bcount; j++){
					brackets.remove(0);
				}
			}
			
			if(brackets.size()==0 && realchunk.length()>0){
				chunkedtokens.set(i, realchunk.replaceAll("<", "(").replaceAll(">", ")").trim()); //inside a chunk, an organ is marked by #. e.g. #leaves# 
				realchunk="";
			}
			
		}
		if(realchunk.length()>0){
			chunkedtokens.set(i-1+0, realchunk.trim());
		}		
		this.chunkedsent = "";
		
		normalizeOtherINs(); //find objects for those VB/IN that without
		normalizeThan();
		normalizeTo();
		normalizeUnits();
		
		
		//insert segment marks in chunkedtokens while producing this.chunkedsent
		//boolean suspend = false;
		for(i = this.chunkedtokens.size()-1; i>=0; i--){
			String t = this.chunkedtokens.get(i);
			//if(t.compareTo("-RRB-/-RRB-")==0){
			//	suspend = true;
			//}
			//if(t.compareTo("-LRB-/-LRB-")==0){
			//	suspend = false;
			//}
			if(t.compareTo("") !=0){
				this.chunkedsent = t+" "+this.chunkedsent;;
			}
			if(t.indexOf('<')>=0){
				for(i = i-1; i>=0; i--){
					String m = this.chunkedtokens.get(i);
					if(m.matches(".*?\\b("+ChunkedSentence.prepositions+")\\b.*")){
						this.chunkedsent = m+" "+this.chunkedsent;
						break; //has prepositions before <
					}
					//if(m.matches("(,|;|:)") && !suspend){
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
		if(this.chunkedtokens.get(this.chunkedtokens.size()-1).matches("\\W")){
			this.chunkedtokens.set(this.chunkedtokens.size()-1, "SG"+this.chunkedtokens.get(this.chunkedtokens.size()-1)+"SG");
		}
		this.chunkedsent.trim();
		
		//if the last words in l[] are marked with {}, take them out of the chunk
		if(this.chunkedsent.matches(".*?l\\[[^\\[].*?}\\].*")){
			removeStateFromList();
		}
		normalizeSubject();
	}
	
	/**
	 * l[(mid) and (distal) (cauline) {smaller}]
	 * ==>
	 * l[(mid) and (distal) (cauline)] {smaller}
	 */
	private void removeStateFromList() {
		for(int i = 0; i<this.chunkedtokens.size(); i++){
			String t = this.chunkedtokens.get(i);
			if(t.matches("l\\[[^\\[]*?}\\]")){
				String list = t.substring(0, t.lastIndexOf(")")+1).trim();
				String state = t.replace(list, "").replaceFirst("\\]$", "").trim();
				list= list+"]";
				if(this.chunkedtokens.get(i+1).length()==0){
					this.chunkedtokens.set(i, list);
					this.chunkedtokens.set(i+1, state);
				}else if(this.chunkedtokens.get(i-1).length()==0){
					this.chunkedtokens.set(i-1, list);
					this.chunkedtokens.set(i, state);
				}else{
					System.err.println("removeStateFromList messed up");
				}
				this.chunkedsent = this.chunkedsent.replace(t, list+" "+state);
			}
			
		}
		
	}


	/**
	 * for the first <> after SG,SG, put the organ and its modifier in one chunk
	 */
	private void normalizeSubject(){
		

	}
	
	/**
	 * 3] {mm}
	 * 
	 */
	private void normalizeUnits(){
		for(int i = 0; i<this.chunkedtokens.size(); i++){
			String word = this.chunkedtokens.get(i);
			if(word.matches("[<{]("+ChunkedSentence.units+")[}>]")){
				if(i-1>=0){
					String latest = this.chunkedtokens.get(i-1);
					if(latest.matches(".*?\\d\\]+$")){
						String rest = latest.replaceAll("\\]+$", "").trim();
						String brackets = latest.replace(rest, "").trim();
						String norm = rest+ " "+word.replaceAll("[{}<>]", "")+brackets; //mm, not {mm}
						this.chunkedtokens.set(i-1, norm);
						this.chunkedtokens.set(i, "");
					}					
				}
			}
		}
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
		String preps = ChunkedSentence.prepositions.replaceFirst("\\bthan\\|", "").replaceFirst("\\bto\\|", "");
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
							String w = this.chunkedtokens.get(i).replaceAll("(\\<|\\>|\\{|\\}|\\w+\\[|\\])", "");
							//if(w.matches("\\b("+preps+"|and|or|that|which|but)\\b") || w.matches("\\W")){
							if(w.matches("\\b("+preps+"|and|that|which|but)\\b") || w.matches("\\p{Punct}")){ //should allow ±, n[{shorter} than] ± {campanulate} <throats>
								np = np.replaceAll("<", "(").replaceAll(">", ")").trim();
								this.chunkedtokens.set(thani, "n["+np+"]");
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
			if(token.compareTo("to") == 0 || token.matches(".*?\\bto]+$")){
				
				//scan for the next organ
				for(int j = i+1; j<this.chunkedtokens.size(); j++){
					String t = this.chunkedtokens.get(j).trim();
					if(startn && t.indexOf('<')<0){
						break;
					}
					//to b[v[expose] o[(stigma)]]
					if(t.matches("[,:;\\d]") || t.matches(".*?\\b[pv]\\[.*") ||t.matches(".*?\\b("+ChunkedSentence.prepositions+"|and|or|that|which|but)\\b.*")){
						break;
					}
					np +=t+" ";
					this.chunkedtokens.set(j, "");
					if(t.lastIndexOf(' ') >=0){
						t = t.substring(t.lastIndexOf(' ')); //last word there
					}
					if(t.indexOf('<')>=0 || t.indexOf('(')>=0){ //t may have []<>{}
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
						if(t.matches(".*?\\b("+ChunkedSentence.prepositions+"|and|or|that|which|but)\\b.*") || t.matches(".*?[>;,:].*")){
							np = np.replaceAll("<", "(").replaceAll(">", ")").replaceAll("\\s+", " ").trim();
							//np = np.replaceAll("\\s+", " ").trim();
							this.chunkedtokens.set(i, "w["+np+"]"); //replace "to" with np
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
		
		//boolean startn = false;
		String preps = ChunkedSentence.prepositions.replaceFirst("\\bthan\\|", "").replaceFirst("\\bto\\|", "");
		for(int i = 0; i<this.chunkedtokens.size(); i++){
			String token = this.chunkedtokens.get(i);
			
			if(token.matches(".*?p\\[[a-z]+\\]+") || token.matches(".*?\\b("+preps+")\\b\\]*$")){//[of] ...onto]]
				if(this.printNorm){
					System.out.println(token+" needs normalization!");
				}
				ArrayList<String> copy = (ArrayList<String>)this.chunkedtokens.clone();
				boolean startn = false;
				String np = "";
				String ns = "";
				for(int j = i+1; j<this.chunkedtokens.size(); j++){
					String t = this.chunkedtokens.get(j).trim();
					if(startn && t.indexOf('<')<0 && !Utilities.isNoun(t, nouns)){ //test whole t, not the last word once a noun has been found
						break;
					}
					np +=t+" ";
					this.chunkedtokens.set(j, "");
					
					if(t.indexOf('<')>=0 ||t.indexOf('(')>=0 || Utilities.isNoun(t, nouns)){ //t may have []<>{}
						startn = true; //not break yet, may be the next token is a noun
						ns += t+" ";
						if(this.printNorm){
							System.out.println("!normalized!");
						}
					}
				}
				if(startn){
					ns = ns.trim();
					if(!ns.endsWith("]")){
						np = np.replace(ns, "").trim();
						ns  = "("+ns.replaceAll("[{(<>)}]", "").replaceAll("\\s+", ") (")+")";
						np = (np.replaceAll("<", "(").replaceAll(">", ")")+" "+ns).trim();
					}
					if(token.indexOf('[')>=0){
						String rest = token.replaceFirst("\\]+$", "").trim();
						String brackets = token.replace(rest, "").replaceFirst("\\]$", "").trim();
						token = rest + "] o["+np.trim()+"]"+brackets;
						this.chunkedtokens.set(i, token);
					}else{//without [], one word per token
						this.chunkedtokens.set(i, "r[p["+token+"] o["+np.trim()+"]]");
					}
				}else{
					this.chunkedtokens = copy;
				}
			}
			//i=i+1;

		}
		/*if(!startn){
			this.chunkedtokens = copy;
		}*/
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
	
	public void setInSegment(boolean yes){
		this.inSegment = yes;
	}
	
	public void setRightAfterSubject(boolean yes){
		this.rightAfterSubject = yes;
	}
	/**
	 * move pointer after lead in chunkedtokens
	 * @param lead
	 */
	public void skipLead(String[] tobeskipped){
		int wcount = 0;
		if(tobeskipped[tobeskipped.length-1].compareTo("chromosome")==0){
			for(int i = 0; i<this.chunkedtokens.size(); i++){
				if(this.chunkedtokens.get(i).endsWith("=")){
					this.pointer = i;
					break;
				}
			}
			this.pointer++;
		}else if(tobeskipped[tobeskipped.length-1].compareTo("general")==0){ //Treatises: "general" = "wholeorganism"
			//do not skip
		}else{
			int sl = tobeskipped.length;
			for(int i = 0; i<this.chunkedtokens.size(); i++){
				wcount += (this.chunkedtokens.get(i)+" a").replaceAll(",", "or").replaceAll("\\b(or )+", "or ").trim().split("\\s+").length-1;
				//if(this.chunkedtokens.get(i).matches(".*?\\b"+tobeskipped[sl-1]+".*") && wcount>=sl){
				String skipthis = tobeskipped[sl-1];
				if(tobeskipped[sl-1].length()>2){
					skipthis = tobeskipped[sl-1].substring(0, tobeskipped[sl-1].length()-2);//for structure names such as "f3"
				}
				if(this.chunkedtokens.get(i).toLowerCase().matches(".*?\\b"+skipthis+".*") && wcount>=sl){//try to match <phyllaries> to phyllary
					if(wcount==sl){
						this.pointer = i;
					}else{
						//if wcount > sl, then there must be some extra words that have been skipped
						//put those words in chunkedtokens for process
						//example:{thin} {dorsal} {median} <septum> {centrally} only ; 
						int save = i;
						i++;
						for(int j = 0; j < wcount-sl; j++){
							this.chunkedtokens.add(i++, this.chunkedtokens.get(j));
						}
						this.chunkedtokens.add(i++, ",");
						this.pointer = save;
					}
					break;
				}
			}
			this.pointer++;
		}
	}

	public boolean hasNext(){
		if(pointer <this.chunkedtokens.size()){
			return true;
		}
		return false;
	}
	
	public Chunk nextChunk(){

		Chunk ck = getNextChunk();
		while(ck==null && this.hasNext()){
			ck=this.getNextChunk();
		}
		if(ck instanceof ChunkOrgan){
			this.rightAfterSubject = true;
		}else{
			this.rightAfterSubject = false;
		}
		return ck;
	}
	/**
	 * returns the next Chunk: may be a
	 * Organ, Value, Comparative Value, SimpleCharacterState, Subclause,
	 * PrepChunk, IVerbChunk (Intransitive verb chunk, followed by a preposition), VerbChunk, ADJChunk
	 * @return
	 */
	public Chunk getNextChunk(){
		Chunk chunk = null;
		String token = this.chunkedtokens.get(pointer);////a token may be a word or a chunk of text
		while(token.trim().length()==0){
			pointer++;
			token = this.chunkedtokens.get(pointer);
		}
		token = token.compareTo("±")==0? "moreorless" : token;
		token = token.matches(".*?\\d.*")? NumericalHandler.originalNumForm(token) : token;
		
		//all tokens: 
		//number:
		//if(token.matches(".*?\\d+$")){ //ends with a number
		if(NumericalHandler.isNumerical(token) || token.matches("l\\s*\\W\\s*w")){//l-w or l/w
				chunk = getNextNumerics();//pointer++;
				if(this.unassignedmodifier != null){
					chunk.setText(this.unassignedmodifier+ " "+chunk.toString());
				}
				return chunk;
		}
		
		if(token.indexOf("×")>0 && token.length()>0 && token.indexOf(" ")<0){
			//token: 4-9cm×usually15-25mm			
			String[] dim = token.split("×");
			boolean isArea = true;
			int c = 0;
			for(int i = 0; i<dim.length; i++){
				isArea = dim[i].matches(".*?\\d.*") && isArea;
				c++;
			}
			if(isArea && c>=2){
				token = token.replaceAll("×[^0-9]*", " × ").replaceAll("(?<=[^a-z])(?=[a-z])", " ").replaceAll("(?<=[a-z])(?=[^a-z])", " ").replaceAll("\\s+", " ").trim();
				chunk = new ChunkArea(token);
				pointer++;
				return chunk;
			}
		}

		if(token.indexOf("=")>0){//chromosome count 2n=, FNA specific
			String l = "";
			String t= this.chunkedtokens.get(pointer++);
			while(t.indexOf("SG")<0){
				l +=t+" ";
				t= this.chunkedtokens.get(pointer++);				
			}
			l = l.replaceFirst("\\d[xn]=", "").trim();
			chunk = new ChunkChrom(l);
			return chunk;
		}
		//create a new ChunkedSentence object for bracketed text 
		if(token.startsWith("-LRB-/-LRB-")){
			ArrayList<String> tokens = new ArrayList<String>();
			String text = "";
			if(token.indexOf("-RRB-/-RRB-")<0+0){
				String t = this.chunkedtokens.get(++this.pointer);
				while(!t.endsWith("-RRB-/-RRB-")){
					tokens.add(t);
					text += t+ " ";
					t = this.chunkedtokens.get(++this.pointer);
				}
			}
			text=text.trim();
			if(text.length()>0){ //when -LRB- and -RRB- are on the same line, text="" for example, as in -LRB-/-LRB-3--RRB-/-RRB-5-{merous} (3-)5-{merous}
				this.pointer++;
				if(!text.matches(".*?[,;\\.:]$")){
					text +=" .";
					tokens.add(".");
				}
				Chunk c = new ChunkBracketed(text);
				c.setChunkedTokens(tokens);
				return c;
			} //else, continue on
		}
		//create a new ChunkedSentence object
		if(token.startsWith("s[")){
			ArrayList<String> tokens = new ArrayList<String>();
			String text = token.replaceFirst("s\\[", "").replaceFirst("\\]$", "");
			//break text into correct tokens: s[that is {often} {concealed} r[p[by] o[(trichomes)]]] ;
			tokens = breakText(text);
			this.pointer++;
			text=text.trim();
			if(!text.matches(".*?[,;\\.:]$")){
				text +=" .";
				tokens.add(".");
			}
			Chunk c = new ChunkSBAR(text);
			c.setChunkedTokens(tokens);
			return c;
		}
		if(token.matches("\\W") ){//treat L/RRBs as either , or null
			pointer++;
			this.unassignedmodifier = null;
			return new ChunkComma("");
		}
		
		if(token.matches("\\b(and|either)\\b")){
			pointer++;
			this.unassignedmodifier = null;
			return null;
		}
		//end of a segment
		if(token.matches("SG[;:,\\.]SG")){
			this.inSegment = false;
			pointer++;
			this.unassignedmodifier = null;
			return new ChunkEOS("");
		}
		
		//start of a segment
		if(!this.inSegment){
			this.inSegment = true;
			chunk = getNextOrgan();//pointer++
			if(chunk != null){
				this.unassignedmodifier = null;
				return chunk;
			}
		}
		
		//all chunks
		if(token.matches("^\\w+\\[.*")){
			String type = checkType(pointer);
			token = this.chunkedtokens.get(pointer); //as checkType may have reformatted token.
			try{
				if(type != null){
					Class c = Class.forName("fna.charactermarkup."+type);
					Constructor cons = c.getConstructor(String.class);
					pointer++;
					if(this.unassignedmodifier != null && this.chunkedtokens.get(pointer).matches("(SG)?\\W(SG)?")){
						token = token.replaceFirst("\\[", "["+this.unassignedmodifier+" ");
						this.unassignedmodifier = null;
					}
					return (Chunk)cons.newInstance(token.trim());
				}else{//if the chunk is not correctly formatted. Forward pointer to the next comma.
					//forward pointer to after the next [;:,.]
					if(this.printExp){
						System.out.println("PP without a Noun: "+token);
					}
					String t = "";
					do{
						if(this.pointer < this.chunkedtokens.size()){
							t = this.chunkedtokens.get(this.pointer++);
						}else{
							break;
						}
					}while (!t.matches("[,;:\\.]"));
					return null;
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		//OR:
		if(token.compareTo("or") == 0){
			this.pointer++;
			return new ChunkOR("or");
		}
		
		//text:
		chunk = composeChunk();
		
		return chunk;
	}
	/**
	 * break text into correct tokens: 
	 * @param text: that is {often} {concealed} r[p[by] o[(trichomes)]];
	 * @return
	 */
	private ArrayList<String> breakText(String text) {
		ArrayList<String> tokens = new ArrayList<String>();
		String[] words = text.split("\\s+");
		String t = "";
		int left = 0;
		for(int i = 0; i<words.length; i++){
			String w = words[i];
			if(w.indexOf("[")<0 && w.indexOf("]")<0 && left==0){
				if(!w.matches("\\b(this|have|that|may|be)\\b")){tokens.add(w);};
			}else{
				left += w.replaceAll("[^\\[]", "").length();
				left -= w.replaceAll("[^\\]]", "").length();
				t += w+" ";
				if(left==0){
					tokens.add(t.trim());
					t = "";
				}
			}
		}
		return tokens;
	}
	
	private Chunk composeChunk() {
		Chunk chunk;
		String token;
		String scs = "";
		String role = "";
		boolean foundo = false;//found organ
		boolean founds = false;//found state
		if(this.unassignedmodifier != null){
			scs =(scs.trim().length()>0? scs.trim()+"] ": "")+"m["+this.unassignedmodifier.replaceAll("[{}]", "")+" ";
			this.unassignedmodifier = null;
		}
		for(int i = this.pointer; i<this.chunkedtokens.size(); i++){
			token = this.chunkedtokens.get(i);
			token = token.matches(".*?\\d.*")? NumericalHandler.originalNumForm(token):token;
			if(token.length()==0){
				continue;
			}
			//token = NumericalHandler.originalNumForm(token); //turn -LRB-/-LRB-2
			if(token.matches("^\\w+\\[.*")){ //modifier +  a chunk: m[usually] n[size[{shorter}] constraint[than or {equaling} (phyllaries)]]
				//if(scs.matches("\\w{2,}\\[.*") && token.matches("\\w{2,}\\[.*")){ // scs: position[{adaxial}] token: pubescence[{pubescence~list~glabrous~or~villous}]
				if(scs.matches(".*?\\w{2,}\\[.*")){
					pointer = i;
					return new ChunkSimpleCharacterState("a["+scs.trim()+"]]"); 
				}else if(scs.matches("o\\[\\w+\\s*")){
					pointer = i;
					scs = scs.replaceAll("o\\[", "o[(").trim()+")]";
					return new ChunkNonSubjectOrgan("u["+scs+"]");
				}else{
					String type = checkType(i); //changed from pointer to i
					token = this.chunkedtokens.get(i);
					token = token.matches(".*?\\d.*")? NumericalHandler.originalNumForm(token):token;
					scs = scs.trim().length()>0? scs.trim()+"] " : ""; //modifier 
					String start = token.substring(0, token.indexOf("[")+1); //becomes n[m[usually] size[{shorter}] constraint[than or {equaling} (phyllaries)]]
					String end = token.replace(start, "");
					token = start+scs+end;
					try{
						if(type !=null){//r[p[as]] without o[]
							Class c = Class.forName("fna.charactermarkup."+type);
							Constructor cons = c.getConstructor(String.class);
							pointer = i+1;
							return (Chunk)cons.newInstance(token.trim());
						}else{ //parsing failure, continue with the next chunk
							pointer = i+1;
							return null;
						}
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
			
			role = token.charAt(0)+"";
			token = token.replaceAll("[<>{}]", "");
			//<roots> {usually} <taproots> , {sometimes} {fibrous}.
			String symbol= this.rightAfterSubject? "type" : "o";
			if(!foundo && role.compareTo("<")==0){
				scs = (scs.trim().length()>0? scs.trim()+"] ": "")+symbol+"["+token+" ";
				foundo = true;
			}else if(foundo && role.compareTo("<")==0){
				scs += token+" ";
			}else if(foundo && role.compareTo("<") !=0){
				this.pointer = i;
				scs = scs.replaceFirst("^\\]\\s+", "").replaceFirst(symbol+"\\[", "###[").replaceAll("\\w+\\[", "m[").replaceAll("###\\[", symbol+"[").trim()+"]"; //change all non-type character to modifier: <Inflorescences> {indeterminate} <heads>
				if(!this.rightAfterSubject){
					//reformat m[] o[] o[] to m[] o[()] o[()]
					String m = scs.substring(0, scs.indexOf("o["));
					String o = scs.substring(scs.indexOf("o[")).replaceAll("\\[", "[(").replaceAll("\\]", ")]");
					scs = m+o;
				}
				return this.rightAfterSubject? new ChunkSimpleCharacterState("a["+scs+"]") : new ChunkNonSubjectOrgan("u["+scs+"]"); //must have type[ or o[
			}
			
			if(token.matches(".*?"+NumericalHandler.numberpattern+"$") || token.matches("\\d+\\+?")){ //0. sentence ends with a number, the . is not separated by a space
				if(scs.matches(".*?\\w{2,}\\[.*")){//must have character[
					pointer=i;
					scs = scs.replaceFirst("^\\]\\s+", "").trim()+"]";
					return new ChunkSimpleCharacterState("a["+scs.trim()+"]");
				}else{
					pointer=i;
					chunk = getNextNumerics();
					if(chunk!=null){
						if(scs.length()>0){
							scs = scs.replaceFirst("^\\]", "").trim()+"] "+chunk.toString();
						}else{
							scs = chunk.toString();
						}
						chunk.setText(scs);
						return chunk;
					}else{
						pointer++;
						return chunk; //return null, skip this token: parsing failure
					}
				}
			}

			
			//add to a state chunk until a) a preposition b) a punct mark or c)another state is encountered
			if(role.compareTo("<") !=0+0 && true){
				if(Utilities.isAdv(token, adverbs)){
					scs = scs.trim().length()>0? scs.trim()+ "] m["+token+" " : "m["+token;
				}else if(token.matches(".*[,;:\\.\\[].*") || token.matches("\\b("+ChunkedSentence.prepositions+"|or|and)\\b") || token.compareTo("-LRB-/-LRB-")==0){
					this.pointer = i;
					if(scs.matches(".*?\\w{2,}\\[.*")){//must have character[
						scs = scs.replaceFirst("^\\]\\s+", "").trim()+"]";
						return new ChunkSimpleCharacterState("a["+scs.trim()+"]");
					}else{
						if(scs.indexOf("m[")>=0+0){
							this.unassignedmodifier = "{"+scs.trim().replaceAll("(m\\[|\\])", "").replaceAll("\\s+", "} {")+"}";
						}
						if(this.pastpointers.contains(i+"")){
							this.pointer = i+1;
						}else{
							this.pastpointers.add(i+"");
						}
						return null;
					}
				}else{
					String chara = Utilities.lookupCharacter(token, conn, characterhash, glosstable);
					if(!founds && chara!=null){
						scs = (scs.trim().length()>0? scs.trim()+"] ": "")+chara+"["+token+" ";
						founds = true;
					}else if(founds && chara!=null && scs.matches(".*?"+chara+"\\[.*")){ //coloration coloration: dark blue
						scs += token+" ";
					}else if(founds){
						this.pointer = i;
						scs = scs.replaceFirst("^\\]\\s+", "").trim()+"]";
						return new ChunkSimpleCharacterState("a["+scs.trim()+"]");
					}else if(chara==null){
						if(Utilities.isVerb(token, verbs) && !founds){//construct ChunkVP or ChunkCHPP
							scs = (scs.trim().length()>0? scs.trim()+"] ": "")+"v["+token+" ";
							//continue searching for either a <> or a r[]
							boolean findc = false; //find a chunk
							boolean findo = false; //find an organ
							boolean findm = false; //find a modifier
							boolean findt = false; //find a text token
							for(int j = i+1; j < this.chunkedtokens.size(); j++){
								String t = this.chunkedtokens.get(j).trim();
								String ch = Utilities.lookupCharacter(t, conn, characterhash, glosstable);
								if(t.length() == 0){continue;}
								if((!findc &&!findo) && t.matches("^[rwl]\\[.*")){
									scs = scs.replaceFirst("^\\]\\s+", "").trim()+"] ";
									scs += t;
									findc = true;
								}else if(!findo && t.indexOf("<")>=0){
									scs = (scs.trim().length()>0? scs.trim()+"] ": "")+"o["+t.replace("<", "(").replace(">", ")").replaceAll("[{}]", "")+" ";
									findo = true;
								}else if(!findo && !findc && ch!=null){
									scs = (scs.trim().length()>0? scs.trim()+"] ": "")+ch+"["+t.replaceAll("[{}]", "")+" ";
								}else if(!findo && !findc && !findm && Utilities.isAdv(t, adverbs)){
									scs = (scs.trim().length()>0? scs.trim()+"] ": "")+"m["+t.replaceAll("[{}]", "")+" ";
									findm = true;
								}else if(!findo && !findc && findm && Utilities.isAdv(t, adverbs)){
									scs += t.replaceAll("[{}]", "")+" ";
								}else if(findo && t.indexOf("<")>=0){
									scs += t.replace("<", "(").replace(">", ")").replaceAll("[{}]", "")+" ";
								}else if((findo || findc) && t.indexOf("<")<0){ //must have foundo or foundc
									this.pointer = j;
									if(findo){scs = scs.replaceFirst("^\\]\\s+", "").trim()+"]";}
									if(scs.indexOf("p[")>=0){
										return new ChunkCHPP("t["+scs.replace("v[", "c[")+"]");
									}else{
										scs = scs.replace("l[", "o[");
										if(scs.matches(".*?\\bv\\[[^\\[]* m\\[.*")){//v[comprising] m[a] architecture[surrounding] o[(involucre)]
											scs = format(scs);
											//scs = scs.replaceFirst("\\] o\\[", " ").replaceFirst("\\] m\\[", "] o[");
										}else if(scs.matches(".*?\\bv\\[[^\\[]* \\w{2,}\\[.*")){//v[comprising]  architecture[surrounding]
											scs = format(scs);
											//scs = scs.replaceFirst("\\] o\\[", " ").replaceFirst("\\] \\w{2,}\\[", "] o[");
										}
										return new ChunkVP("b["+scs+"]"); 
									}
								}else if(t.matches(".*?\\W.*") || t.matches("\\b("+ChunkedSentence.prepositions+"|or|and)\\b") || t.compareTo("-LRB-/-LRB-")==0){
									if(scs.matches(".*?\\w{2,}\\[.*")){ //borne {singly
										this.pointer = j;
										scs = (scs.replaceFirst("^\\]", "").trim()+"]").replaceFirst("\\bv\\[[^\\[]*?\\]\\s*", "");
										return new ChunkSimpleCharacterState("a["+scs.trim()+"]");
									}else{
										//search failed
										if(this.pastpointers.contains(i+"")){
											this.pointer = i+1;
										}else{
											this.pointer = i;
											this.pastpointers.add(i+"");
										}
										return null;
									}
								}else if(!findt){ //usually v[comprising] m[a {surrounding}] o[involucre]
									scs = (scs.trim().length()>0? scs.trim()+"] ": "")+"m["+t+" "; //taking modifiers
									findt = true;
								}else if(findt){
									scs += t+" ";
								}
							}
						}else{
							scs = "";
						}
					}
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
	/*private Chunk getNextBroken() {
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
	}*/
	/**
	 * m[usually] v[comprising] m[a] architecture[surrounding] o[(involucre)]
	 * 
	 * m[usually] v[comprising] o[1 architecture[surrounding] (involucre)]
	 */
	private String format(String scs) {
		String first = scs.substring(0, scs.indexOf("v["));
		String rest = scs.replace(first, "");
		String v = rest.substring(0, rest.indexOf(']')+1+0);
		String o = rest.replace(v, "").trim(); //m[a] architecture[surrounding] o[(involucre)]
		String newo = "o[";
		do{
			String t = o.indexOf(' ')>=0? o.substring(0, o.indexOf(' ')) : o;
			o = o.replaceFirst(t.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)"),"").trim();
			if(t.startsWith("m[")){
				t = t.replaceAll("(m\\[|\\])", "").trim();
				if(t.compareTo("a") == 0 && !o.matches("(couple|few)")){
					t = "1";
				}
			}
			if(t.startsWith("o[")){
				t=t.replaceAll("(o\\[|\\])", "").trim();
			}
			newo+=t+" ";
			
		}while(o.length()>0);
		return first+v+" "+newo.trim()+"]";
	}
	/**
	 * TODO: deal with LRB-/-LRB
	 * @return e.g. 3 cm, what about "3 cm to 10 dm"?
	 * also 3 times (... longer than, as wide as ...)
	 */
	private Chunk getNextNumerics() {
		String numerics = "";
		String t = this.chunkedtokens.get(this.pointer);
		t = NumericalHandler.originalNumForm(t).replaceAll("\\?", "");		
		if(t.matches(".*?[()\\[\\]\\-\\–\\d\\.×\\+°²½/¼\\*/%]*?[½/¼\\d][()\\[\\]\\-\\–\\d\\.×\\+°²½/¼\\*/%]*(-\\s*("+ChunkedSentence.counts+")\\b|$)")){ //ends with a number
			numerics += t+ " ";
			pointer++;
			if(pointer==this.chunkedtokens.size()){
				return new ChunkCount(numerics.replaceAll("[{()}]", "").trim());
			}
			t = this.chunkedtokens.get(this.pointer);
			if(t.matches("^[{<(]*("+ChunkedSentence.units+")\\b.*?")){
				numerics += t+ " ";
				pointer++;
				adjustPointer4DotLong(pointer);//in bhl, 10 cm . long, should skip the ". long" after the unit
				return new ChunkValue(numerics.replaceAll("[{(<>)}]", "").trim());
			}
			if(t.matches("^[{<(]*("+ChunkedSentence.times+")\\b.*?")){
				numerics += t+ " ";
				pointer++;
				numerics = numerics.replaceAll("[{(<>)}]", "");
				Chunk c = nextChunk();
				numerics +=c.toString();
				if(c instanceof ChunkTHANC){
					return new ChunkValue(numerics);//1.5-2 times n[size[{longer} than {wide}]]
				}else{
					return new ChunkComparativeValue(numerics);//1-2 times a[shape[divided]]???; 1-2 times shape[{shape~list~pinnately~lobed~or~dissected}];many 2-4[-6+] times a[size[widths]];[0.5-]1.5-4.5 times u[o[(leaves)]];0.4-0.5 times u[o[(diams)]]
				}
			}
			/*if(found && this.chunkedtokens.get(i).matches("^("+this.per+")\\b.*?")){
				numerics += this.chunkedtokens.get(i)+ " ";
				pointer = i+1;
				return new ChunkBasedCount(numerics.replaceAll("[<>]", "").trim());
			}*/
			return new ChunkCount(numerics.replaceAll("[{()}]", "").trim());
		}
		
		if(t.matches("l\\s*\\W\\s*w")){
			while(!t.matches(".*?\\d.*")){
				t = this.chunkedtokens.get(++this.pointer);
			}
			this.pointer++;
			return new ChunkRatio(NumericalHandler.originalNumForm(t).trim());
		}
		return null;
	}
	
	
	/**
	 * needed for cases like "10 cm . long/broad/wide/thick", skip ". long"
	 * @param pointer2
	 */
	private void adjustPointer4DotLong(int pointer) {
		boolean iscase = false;
		while(this.chunkedtokens.get(pointer).trim().length()==0){
			pointer++;
		}
		if(this.chunkedtokens.get(pointer).trim().matches("\\.")){//optional
			pointer++;
		}
		
		while(this.chunkedtokens.get(pointer).trim().length()==0){
			pointer++;
		}
		while(this.chunkedtokens.get(pointer).trim().matches("[{(<]?(long|broad|wide|thick)[})>]?")){//required
			pointer++;
			iscase = true;
		}
		if(iscase){
			this.pointer = pointer;
		}
	}
	/**
	 * 
	 * @return e.g. z[m[leaf] e[blade]], apex, 
	 * margins and apexes
	 * {} <> <>
	 * {} ()
	 */
	public Chunk getNextOrgan() {
		String organ = "";
		boolean found = false;
		int i = 0;
		for(i = pointer; i<this.chunkedtokens.size(); i++){
			String token = this.chunkedtokens.get(i);
			if(token.matches(".*?\\b("+ChunkedSentence.prepositions+")\\b.*") || token.matches(".*?[,;:\\.].*")){
				break;
			}
			if(found && token.matches("\\b(and|or)\\b")){
				found = false;
			}
			if(found && !token.matches(".*?[>)]\\]*$")){
				pointer = i;
				if(organ.matches("^\\w+\\[")){
					organ = organ.replaceAll("(\\w+\\[|\\])", "");
				}
				organ = organ.replaceAll("[<(]", "(").replaceAll("[>)]", ")").trim();
				return new ChunkOrgan("z["+organ+"]");
			}
			organ += token+" ";
			if(token.matches(".*?[>)]\\]*$")){
				found = true;
			}
			
		}
		if(found){
			pointer = i;
			if(organ.matches("^\\w+\\[")){
				organ = organ.replaceAll("(\\w+\\[|\\])", "");
			}
			organ = organ.replaceAll("[<(]", "(").replaceAll("[>)]", ")").trim();
			return new ChunkOrgan("z["+organ+"]");
		}
		
		return null;
	}
	
	/**
	 * use the un-collapsedTree (this.tree) to check the type of a chunk with the id, 
	 * @param i
	 * @return: 

SBAR: s
VP: b[v/o]
PP: r[p/o]
VP-PP: t[c/r[p/o]]
ADJ-PP:t[c/r[p/o]]
Than: n
To: w
NPList: l
PPList: i
main subject: z[m/e]
non-subject organ/structure u[m[] relief[] o[]]
character modifier: a[m[largely] relief[smooth] m[abaxially]]

	 */
	private String checkType(int id) {
		String token = this.chunkedtokens.get(id);
		if(token.matches("^\\w{2,}\\[.*")){
			return "ChunkSL"; //state list
		}
		/*if(token.startsWith("q[")){
			return "ChunkQP";
		}*/
		/*if(token.startsWith("s[")){
			return "ChunkSBAR";
		}*/
		if(token.startsWith("b[")){
			return "ChunkVP";
		}
		//if(token.startsWith("r[") && token.indexOf("[of]") >= 0){
		//	return "ChunkOf";
		//}
		if(token.startsWith("r[")){
			if(token.indexOf("o[")>=0){
				return "ChunkPrep";
			}else{
				return null;
			}
		}
		if(token.startsWith("t[")){
			//reformat c[] in t[]: c: {loosely} {arachnoid} : should be m[loosely] architecture[arachnoid]
			Pattern p = Pattern.compile("(.*?\\b)c\\[([^]].*)\\](.*)");
			Matcher m = p.matcher(token);
			String reformed = "";
			if(m.matches()){
				reformed += m.group(1);
				String c = reformCharacterState(m.group(2));
				reformed += c+ m.group(3);
			}
			this.chunkedtokens.set(id, reformed);
			return "ChunkCHPP"; //character/state-pp
		}
		if(token.startsWith("n[")){//returns three different types of ChunkTHAN
			Pattern p = Pattern.compile("\\bthan\\b");
			Matcher m = p.matcher(token);
			m.find();
			//String beforethan = token.substring(0, token.indexOf(" than "));
			String beforethan = token.substring(0, m.start());
			String charword = beforethan.lastIndexOf(' ')>0 ? beforethan.substring(beforethan.lastIndexOf(' ')+1) : beforethan.replaceFirst("n\\[", "");
			String beforechar = beforethan.replace(charword, "").trim().replaceFirst("n\\[", "");
			
			String chara = null;
			if(!charword.matches("("+ChunkedSentence.more+")")){
				chara = Utilities.lookupCharacter(charword, this.conn, ChunkedSentence.characterhash, glosstable);
			}
			String afterthan = token.substring(token.indexOf(" than ")+6);
			//Case B
			if(afterthan.matches(".*?\\d.*?\\b("+ChunkedSentence.units+"|long|length|wide|width)\\b.*") || afterthan.matches(".*?\\d\\.\\d.*")){// "n[{longer} than 3 (cm)]" => n[size[{longer} than 3 (cm)]]
				if(chara==null){chara="size";}
				token = "n["+token.replaceFirst("n\\[", chara+"[")+"]";
				this.chunkedtokens.set(id, token);
				return "ChunkTHAN"; //character
			}else if(afterthan.matches(".*?\\d.*")){// "n[{longer} than 3 (cm)]" => n[size[{longer} than 3 (cm)]]
				if(chara==null){chara="count";}
				token = "n["+token.replaceFirst("n\\[", chara+"[")+"]";
				this.chunkedtokens.set(id, token);
				return "ChunkTHAN";
			}//Case C
			else if(afterthan.indexOf("(")>=0){ //contains organ
				if(chara==null){//is a constraint, lobed n[more than...]
					token = "n["+token.replaceFirst("n\\[", "constraint[")+"]";
					this.chunkedtokens.set(id, token);
					return "ChunkTHAN";
				}else{//n[more deeply lobed than...
					token = "n["+(beforechar.length()>0? "m["+beforechar+"] ": "")+chara+"["+charword+"] constraint[than "+afterthan+"]";
					this.chunkedtokens.set(id, token);
					return "ChunkTHAN";
				}
			}//Case A n[wider than long]
			else{
				token = "n["+token.replaceFirst("n\\[", chara+"[")+"]";
				this.chunkedtokens.set(id, token);
				return "ChunkTHANC"; //character
			}
		}
		if(token.startsWith("w[")){//w[{proximal} to the (florets)] ; or w[to (midvine)]
			//reformat it to CHPP
			if(token.indexOf("w[to ")>=0){
				token = token.replaceFirst("w\\[to ", "r[p[to] o[")+"]";
				this.chunkedtokens.set(id, token);
				return "ChunkPrep";
			}else{
				token = token.replaceFirst("w\\[","t[c[").replaceFirst("(\\s+|\\b)to\\s+", "] r[p[to] o[")+"]]";
				this.chunkedtokens.set(id, token);
				return "ChunkCHPP";
			}
			
		}
		if(token.startsWith("l[")){
			return "ChunkNPList";
		}
		if(token.startsWith("i[")){
			return "ChunkPPList";
		}
		if(token.startsWith("z[")){
			return "ChunkOrgan";
		}
		
		return null;
	}

	/**
	 * 
	 * @param group: {loosely} {arachnoid}
	 * @return:m[loosely] architecture[arachnoid]
	 */
	private String reformCharacterState(String charstring) {
		String result = "";
		String last = charstring.substring(charstring.lastIndexOf(' ')).trim();
		String first = charstring.replace(last, "").trim();
		result = "m["+first+"] ";
		
		String c = Utilities.lookupCharacter(last, conn, characterhash, glosstable);
		if(c!=null){
			result += c+"["+last+"]";
		}else if(Utilities.isVerb(last, verbs)){
			result += "v["+last+"]";
		}

		return result;
	}
	
	/**
	 * when parsing fails at certain point, forward the pointer to the next comma
	 */
	public void setPointer2NextComma() {
		// TODO Auto-generated method stub
		for(; this.pointer<this.chunkedtokens.size(); pointer++){
			if(this.chunkedtokens.get(pointer).matches("(,|\\.|;|:)")){
				break;
			}
		}
		
	}

	
	
	
}
