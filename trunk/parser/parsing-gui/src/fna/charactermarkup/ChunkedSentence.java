 /* $Id$ */
/**
 * 
 */
package fna.charactermarkup;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;

import fna.parsing.ApplicationUtilities;
import fna.parsing.state.SentenceOrganStateMarker;




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
	private static final Logger LOGGER = Logger.getLogger(ChunkedSentence.class);
	private String glosstable = null;
	private String markedsent = null;
	private String chunkedsent = null;
	private ArrayList<String> chunkedtokens = null;
	@SuppressWarnings("unused")
	private ArrayList<String> charactertokensReversed = new ArrayList<String>();
	private int pointer = 0; //pointing at the next chunk to be annotated
	private String subjecttext = null;
	private String originaltext = null;
	private String text = null;
	private String sentsrc = null;
	private String tableprefix = null;
	private Hashtable<String, String> thantype = new Hashtable<String, String>();
	public static final String locationpp="near|from";
	public static final String units= "cm|mm|dm|m|meters|meter|microns|micron|unes|µm|um";
	public static final String percentage="%|percent";
	public static final String degree="°|degrees|degree";
	public static final String times = "times|folds|lengths|widths";
	public static final String per = "per";
	public static final String more="greater|more|less|fewer";
	public static final String counts="few|several|many|none|numerous|single|couple";
	public static final String basecounts="each|every|per";
	public static final String clusters="clusters|cluster|involucres|involucre|rosettes|rosette|pairs|pair|series|ornamentation|ornament|arrays|array"; //pl before singular.
	public static final String allsimplepreps = "aboard|about|above|across|after|against|along|alongside|amid|amidst|among|amongst|anti|around|as|astride|at|atop|bar|barring|before|behind|below|beneath|beside|besides|between|beyond|but|by|circa|concerning|considering|counting|cum|despite|down|during|except|excepting|excluding|following|for|from|given|gone|in|including|inside|into|less|like|minus|near|notwithstanding|of|off|on|onto|opposite|outside|over|past|pending|per|plus|pro|re|regarding|respecting|round|save|saving|since|than|through|throughout|till|to|touching|toward|towards|under|underneath|unlike|until|up|upon|versus|via|with|within|without|worth";
	public static final String prepositions = "above|across|after|along|among|amongst|around|as|at|before|behind|below|beneath|between|beyond|by|during|for|from|in|into|near|of|off|on|onto|out|outside|over|per|than|through|throughout|to|toward|towards|up|upward|with|without|"+POSTagger4StanfordParser.comprepstring;
	public static final String stop = "a|about|above|across|after|along|also|although|amp|an|and|are|as|at|be|because|become|becomes|becoming|been|before|being|beneath|between|beyond|but|by|ca|can|could|did|do|does|doing|done|for|from|had|has|have|hence|here|how|if|in|into|inside|inward|is|it|its|may|might|more|most|near|no|not|of|off|on|onto|or|out|outside|outward|over|should|so|than|that|the|then|there|these|this|those|throughout|to|toward|towards|up|upward|was|were|what|when|where|which|why|with|within|without|would";
	public static final String skip = "and|becoming|if|or|that|these|this|those|to|what|when|where|which|why|not|throughout";
	private Pattern taxonnamepattern1 = null;
	public static Pattern taxonnamepattern2 = null;
	public static Pattern sandwich = Pattern.compile(".*?(N.*?C.*N|C.*?N.*?C).*"); 
	public static Hashtable<String, String> eqcharacters = new Hashtable<String, String>();
	private boolean inSegment = false;
	private boolean rightAfterSubject = false;
	@SuppressWarnings("unused")
	private int sentid = -1;
	private ArrayList<String> pastpointers = new ArrayList<String>();

	public String unassignedmodifier = null;
	//caches
	public static Hashtable<String, String[]> characterhash = new Hashtable<String, String[]>();
	public static ArrayList<String> adverbs = new ArrayList<String>();
	public static ArrayList<String> verbs = new ArrayList<String>();
	public static ArrayList<String> nouns = new ArrayList<String>();
	public static ArrayList<String> notadverbs = new ArrayList<String>();
	public static ArrayList<String> notverbs = new ArrayList<String>();
	public static ArrayList<String> notnouns = new ArrayList<String>();
	
	protected Connection conn = null;
	/*static protected String username = "root";
	static protected String password = "root";
	static protected String database = "fnav19_benchmark";*/
	
	private boolean printNorm = false;
	private boolean printNormThan = false;
	private boolean printNormTo = false;
	private boolean printExp = false;
	private boolean printRecover = false;
	private boolean printParentheses = false;
	private String clauseModifierConstraint;
	private String clauseModifierContraintId;
	private ArrayList<Attribute> scopeattributes = new ArrayList<Attribute>();
	

	
	public ChunkedSentence(ArrayList<String> chunkedtokens, String chunkedsent, Connection conn, String glosstable, String tableprefix){
		this.chunkedtokens = chunkedtokens;
		this.chunkedsent = chunkedsent;
		this.conn = conn;
		this.glosstable = glosstable;
		this.tableprefix = tableprefix;
		this.recoverOrgans();
		
	}
	/**
	 * @param tobechunkedmarkedsent 
	 * @param tree 
	 * @param taxonnamepattern 
	 * @param taxonnamepattern2 
	 * 
	 */
	public ChunkedSentence(int id, Document collapsedtree, Document tree, String tobechunkedmarkedsent,  String sentsrc, String tableprefix,Connection conn, String glosstable/*, Pattern taxonnamepattern, Pattern taxonnamepattern2*/) {
		eqcharacters.put("wide", "width");
		eqcharacters.put("long", "length");
		eqcharacters.put("broad", "width");
		eqcharacters.put("diam", "diameter");
		//eqcharacters.put("diameter", "diameter");
				
		this.tableprefix = tableprefix;
		this.glosstable = glosstable;
		this.conn = conn;
		//this.taxonnamepattern1 = taxonnamepattern;
		//ChunkedSentence.taxonnamepattern2 = taxonnamepattern2;
		Statement stmt = null;
		ResultSet rs = null;
		try{
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select term from "+glosstable+" where category='character'");
			while(rs.next()){
				nouns.add(rs.getString("term")); //initialize nouns with "size", "color", etc.
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
			LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
					System.getProperty("line.separator")+"source:"+this.sentsrc+System.getProperty("line.separator")
					+sw.toString());
		}finally{
			try{
				if(rs!=null) rs.close();
				if(stmt!=null) stmt.close();
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
						System.getProperty("line.separator")+"source:"+this.sentsrc+System.getProperty("line.separator")
						+sw.toString());
			}
		}
		
		this.sentsrc = sentsrc;
		this.sentid = id;
		this.markedsent = tobechunkedmarkedsent;
		//tobechunkedmarkedsent = tobechunkedmarkedsent.replaceAll("[\\[\\(]", " -LRB-/-LRB- ").replaceAll("[\\]\\)]", " -RRB-/-RRB- ").replaceAll("\\s+", " ").trim(); 
		//tobechunkedmarkedsent = tobechunkedmarkedsent.replaceAll("[\\[\\(]", "-LRB-/-LRB-").replaceAll("[\\]\\)]", "-RRB-/-RRB-").trim(); 
		tobechunkedmarkedsent = tobechunkedmarkedsent.replaceAll("(^| )\\( ", " -LRB-/-LRB- ").replaceAll(" \\) ", " -RRB-/-RRB- ").replaceAll("(^| )\\[ ", " -LSB-/-LSB- ").replaceAll(" \\] ", " -RSB-/-RSB- ").trim(); //brackets with surrounding spaces are textual brackets (i.e., not part of numerical expressions)
		//if(tobechunkedmarkedsent.matches(".*?\\d.*")){
			//tobechunkedmarkedsent = NumericalHandler.normalizeNumberExp(tobechunkedmarkedsent);
		//}
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
				//treetoken[i] = treetoken[i].replace("~list~", "[{").replaceAll("\\{(?=\\w{2,}\\[)", "").replaceAll("(?<=~[a-z0-9-]{2,40})(\\}| |$)","}]");
				treetoken[i] = treetoken[i].replace("~list~", "[{").replaceAll("\\{(?=\\w{2,}\\[)", "").replaceAll("(?<=~[a-z0-9-]{1,40})(\\}| |$)","}]");
			}
		}		
		for(i= 0; i<treetoken.length; i++){
			if(treetoken[i].indexOf('[') >=0){
				int bcount  = treetoken[i].replaceAll("[^\\[]", "").trim().length();
				for(int j = 0; j < bcount; j++){
					brackets.add("[");
				}
			}

			if(brackets.size()>0){//in
				//restore original number expressions
				String w = treetoken[i].replaceAll("(\\w+\\[|\\])", "");
				//String w = treetoken[i].replaceAll("(\\[|\\])", "");
				//mohan code to fix {colorttt-list-black-and-orange}
				/*if(treetoken[i].matches("colorttt.*")){
					//realchunk += treetoken[i];
					realchunk += chunkedtokens.get(i);
				}
				else*/
				//End mohan code
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
			chunkedtokens.set(i-1, realchunk.trim());
		}		
		this.chunkedsent = ""+"";
		
		
		int discoveredchunks = 0;
		if (this.chunkedtokens.contains("or")) discoveredchunks +=normalizeConjunctINs("or");
		if (this.chunkedtokens.contains("and")) discoveredchunks +=normalizeConjunctINs("and");
		discoveredchunks +=normalizeOtherINs(); //find objects for those VB/IN that without
		discoveredchunks +=normalizeThan();
		discoveredchunks +=normalizeTo();
		normalizeUnits();
		int allchunks = chunks();
		StanfordParser.countChunks(allchunks, discoveredchunks);
		
		recoverVPChunks();//recover unidentified verb phrases
		recoverConjunctedOrgans(); //
		//findSubject(); //set the pointer to a place right after the subject, assuming the subject part is stable in chunkedtokens at this time
		recoverOrgans();
		processParentheses();
		segmentSent();//insert segment marks in chunkedtokens while producing this.chunkedsent
		
		//TODO move this to an earlier place
		//if the last words in l[] are marked with {}, take them out of the chunk
		if(this.chunkedsent.matches(".*?l\\[[^\\[].*?}\\].*")){
			removeStateFromList();
		}
	}
	
	/**
	 * turn: r[p[at]], or, soon, , r[p[after] o[anthesis]]
	 * to: r[p[at or soon after] o[anthesis]]
	 * @return
	 */
	private int normalizeConjunctINs(String conjunct) {
	
		int normalized = 0;
		int index = this.chunkedtokens.indexOf(conjunct);
		while(index > 0){
			//scan the tokens before and after index, if both tokens are r[p[]], merge the three
			Object[] tokenb = tokensbefore(index);
			String tokenbefore = (String)tokenb[0];
			int indexbefore = ((Integer)tokenb[1]).intValue();
			Object[] tokena = tokensafter(index);
			String tokenafter = (String)tokena[0];
			int indexafter = ((Integer)tokena[1]).intValue();
			int indexof2ndprep = index ;
			String sndprep = "";
			while(indexof2ndprep<this.chunkedtokens.size()-1 && !sndprep.startsWith("r[p[")){
				sndprep = this.chunkedtokens.get(++indexof2ndprep);
				if(sndprep.indexOf("[") > 0) break; //can't have other chunks in btw 1st and 2nd prep
			}								

			if(tokenbefore.matches("r\\[p\\[\\w+\\]\\]")){
				//test other cases
				if(tokenafter.startsWith("r[p[")){
					//merge
					String merged = tokenbefore.replaceFirst("\\]+$", "")+" "+this.chunkedtokens.get(index)+" "+tokenafter.replaceFirst("^r\\[p\\[", "");
					int i = 0;
					for(i = indexbefore; i< indexafter; i++){
						this.chunkedtokens.set(i, "");
					}
					this.chunkedtokens.set(i, merged);
					normalized++;
				}else if(sndprep.startsWith("r[p[")){
					//merge
					String merged = tokenbefore.replaceFirst("\\]+$", "")+" "+this.chunkedtokens.get(index)+" ";
					int i = 0;
					for(i = index+1; i< indexof2ndprep; i++){
						merged += this.chunkedtokens.get(i)+" ";
					}
					for(i = indexbefore; i< indexof2ndprep; i++){
						this.chunkedtokens.set(i, "");
					}
					
					merged +=sndprep.replaceFirst("^r\\[p\\[", "").replaceAll("\\s+", " ").replaceAll("#", "");
					this.chunkedtokens.set(i, merged);
					normalized++;
				}
			}
			
			index = this.chunkedtokens.indexOf("or");
			if(index>=0) this.chunkedtokens.set(index, "#or#"); //mark the "or" as checked to prevent infinite loop
		}
		for(int i = 0; i < this.chunkedtokens.size(); i++){
			this.chunkedtokens.set(i, this.chunkedtokens.get(i).replace("#or#", "or"));
		}
		return normalized;
	}
	
	/**
	 * find the non-empty token before index
	 * @param index
	 * @return
	 */
	private Object[] tokensbefore(int index) {
		String token = "";
		do{
			token = this.chunkedtokens.get(--index);
		} while(index>0 && token.length()==0);
		
		Object[] result = new Object[2];
		result[0] = token;
		result[1] = new Integer(index);
		return result;
	}
	
	/**
	 * find the non-empty token before index
	 * @param index
	 * @return
	 */
	private Object[] tokensafter(int index) {
		String token = "";
		do{
			token = this.chunkedtokens.get(++index);
		} while(index<this.chunkedtokens.size()-2 && token.length()==0);
		
		Object[] result = new Object[2];
		result[0] = token;
		result[1] = new Integer(index);
		return result;
	}
	/**
	 * different types of parentheses: see 
	 * https://sites.google.com/site/biosemanticsproject/character-annotation-discussions/dealing-with-exceptions
	 * 1 []: g[] for geography-constraints G:geo
	 * 2. (... in taxon): x[{} (in taxon)] for taxon-contraints X:taxon
	 * 3. , (): in brackets: parallelism among ranks. P:
	 * 4. (alternative terms, definitions, explanations): R: all the rest
	 * 	 * 
	 * 
	 * this generates three new chunk types:
	 * g[], x[], p[] 
	 * the remaining parenthetical cases will be annotated with in_bracket = "true" attribute.
	 * 
	 */
	private void processParentheses() {
		// TODO Auto-generated method stub
		for(int i = 0; i < this.chunkedtokens.size(); i++){
			String chunk = this.chunkedtokens.get(i);
			if(chunk.equals("-LSB-/-LSB-")){//case 1: geographical constraint: found the opening [
				int j = i;
				String gchunk = "g[";
				while(i+1 < this.chunkedtokens.size() && !this.chunkedtokens.get(++i).equals("-RSB-/-RSB-")){
					gchunk +=this.chunkedtokens.get(i)+" ";
					this.chunkedtokens.set(i, "");
				}
				gchunk = gchunk.trim()+"]";
				if(this.printParentheses) System.out.println("G: ["+this.sentsrc+"/"+this.sentid+"] "+gchunk+ " in: "+this.markedsent);
				this.chunkedtokens.set(j, "");
				this.chunkedtokens.set(i, gchunk);								
			}else if(chunk.equals("-LRB-/-LRB-")){
				int x = i;
				//whether a bracket appears after a comma or a semicolon/period
				boolean aftercomma = false;
				int j = x-1;
				while(j>=0 && this.chunkedtokens.get(j).length()==0){j=j-1;}
				if(j<0) aftercomma = true; //the bracket is the first token in the sentence [i.e., after a semicolon or a period]
				if(j>=0 && this.chunkedtokens.get(j).equals(",")){
					aftercomma = true;
				}
				//collect everything in the pair of round brackets
				
				ArrayList<String> chunkedtokenscopy = (ArrayList<String>)this.chunkedtokens.clone();
				String text = "";
				int left = 0;//if nested brackets, get the outer-most 
				while(i+1 < this.chunkedtokens.size() /*&& !this.chunkedtokens.get(++i).equals("-RRB-/-RRB-")*/){
					if(this.chunkedtokens.get(++i).equals("-RRB-/-RRB-") && left==0) break;
					String t = this.chunkedtokens.get(i); 
					text += t+" ";
					this.chunkedtokens.set(i, "");
					if(t.equals("-LRB-/-LRB-")) left++;
					if(t.equals("-RRB-/-RRB-")) left--;
				}
				text = text.trim(); //containing text from j+1 to i-1, now i is -RRB-/-RRB- or EOL
				//test for case 2: taxon constraint
				//Matcher m = this.taxonnamepattern2.matcher(text);
				//System.out.println(this.taxonnamepattern2.toString());
				//if(m.matches()){//case 2: taxon constraint 205.txt-15	
				if(text.contains("taxonname-")){
					this.chunkedtokens.set(x, ""); // open (
					this.chunkedtokens.set(i, ""); // close )
					int index = x;
					ArrayList<String[]> charataxas = separateCharaTaxa(text); //TOFIX: 2396, 5937

					for(int c = 0; c < charataxas.size(); c++){
						String[] chartaxa = charataxas.get(c);
						//is there a taxon for each result? if not, look ahead and look behind to find one
						String taxon = chartaxa[1];
						if(chartaxa[1].length()==0 && chartaxa[0].length() != 0){//has character without taxon, fill in taxon by looking ahead/behind
							if(c-1 >=0 && charataxas.get(c-1)[1].length()>0){
								taxon = charataxas.get(c-1)[1];
							}else if(c+1 < charataxas.size() && charataxas.get(c+1)[1].length()>0){
								taxon = charataxas.get(c+1)[1];
							}
						}
						if(taxon.length()>0){
							int bracketlength = chartaxa[0].replaceAll("[^\\[]", "").trim().length() - chartaxa[0].replaceAll("[^\\]]","").trim().length();
							String xchunk = "x["+chartaxa[0]+(chartaxa[0].length()>0? " ": "")+"("+taxon+")]";
							for(int b=0;b<bracketlength;b++){
								xchunk+="]";
							}
							index += ("x["+chartaxa[0]+(chartaxa[0].length()>0? " ": "")+"("+chartaxa[1]+")]").split("\\s+").length; //use original chartaxa[1] to calculate the index, as the "taxon" may be added
							xchunk = xchunk.replaceAll("-taxonname-", ". ");
							xchunk = xchunk.replaceAll("taxonname-", "");
							this.chunkedtokens.set(index, xchunk); //fill in xchunks at the correct index
						}
						//if(this.printParentheses) System.out.println("X: ["+this.sentsrc+"/"+this.sentid+"] "+xchunk+ " in: "+this.markedsent);
						if(this.printParentheses) System.out.println("X: ["+this.sentsrc+"/"+this.sentid+"]: ("+chartaxa[1]+")");
						if(chartaxa[2].length()>0){ //optional punctuation mark
							this.chunkedtokens.set(index+1, chartaxa[2]);
						}
					}
				}else if(aftercomma){//case 3: parallelism 
					String pchunk = "q["+text+"]"; //should have used "p", but it is taken already, use "q" instead.
					this.chunkedtokens.set(x, pchunk);
					this.chunkedtokens.set(i, "");
					
					if(this.printParentheses) System.out.println("P: ["+this.sentsrc+"/"+this.sentid+"]"+text.replaceAll("(\\w+\\[|\\]|\\(|\\)|\\{|\\})", "").replaceAll("\\s+", " ").trim());
					String beforeachunk = "";
					int count = 0 ;
					for(int c = x ; count < 6 && c>=0; c--){
						String atoken = "";
					    atoken = this.chunkedtokens.get(c);
						if(atoken.length()>0){
							beforeachunk = atoken + " "+beforeachunk;
							count++;
						}
					}
					if(this.printParentheses) System.out.println("P: "+beforeachunk.replaceAll("(\\w+\\[|\\]|\\(|\\)|\\{|\\})", "").replaceAll("\\s+", " ").trim());															
				}else if(!aftercomma){//case 4: rest 
					this.chunkedtokens = chunkedtokenscopy; //do nothing, reverse to the original chunkedtokens
					if(this.printParentheses) System.out.println("R: ["+this.sentsrc+"/"+this.sentid+"]"+text+ " in:"+this.markedsent);
					if(this.printParentheses) System.out.println("R: "+this.markedsent);
				}				
			}
		}
	}
	
	/**
	 * 	Ribes speciosum {semievergreen} , R. viburnifolium {evergreen}
		{urceolate} in S. purpurea, S. rosea
		may also: Ribes speciosum {semievergreen} , {evergreen} in R. viburnifolium 
	 * @param text
	 * @return arraylist of arrays of size 3: 0: character 1: taxa 2:punct
	 * 0:semievergreen
	 * 1:Ribes speciosum
	 * 2:,
	 * 
	 * 0:evergreen
	 * 1:R. viburnifolium 
	 * 2:
	 * 
	 * 0:urceolate 
	 * 1:in (S. purpurea), (S. rosea)
	 * 2: 
	 */
	public static ArrayList<String[]> separateCharaTaxa(String text) {
		//normalization
		//text = text.replaceAll("(\\w+\\[|\\])", ""); //remove any chunking symbols
		//r[p[in] o[n]] .   l[cuspidata and n] . troximoides 
		text = text.replaceAll("\\s+", " ");
		text = text.replaceAll("(?<=\\b[a-z]\\]{0,2}\\s\\.\\s)l\\[", "");
		text = text.replaceAll("\\{(?=[a-z]\\})", "AAA").replaceAll("(?<=AAA[a-z])\\}", "").replaceAll("AAA", "");
		text = text.replaceAll("(?<=\\b[a-z])\\s+(?=\\.)", ""); //as c . subniveum => as c. subniveum
		//text = text.replaceAll("(?<=\\[[a-z])\\]]\\s*(?=\\.)", "").replaceAll("(?<=\\[[a-z]\\.\\s\\w{2,20})( |$)", "]] ").trim(); //u[o[(pappi)]] 0  r[p[in] o[k]] . cespitosa => u[o[(pappi)]] 0  r[p[in] o[k. cespitosa]]
		text = text.replaceAll("(?<=\\b[a-z])\\]+\\s*(?=\\.)", "").replaceAll("(?<=\\[[a-z]\\.\\s\\w{2,20})( |$)", "]] ").trim(); //u[o[(pappi)]] 0  r[p[in] o[k]] . cespitosa => u[o[(pappi)]] 0  r[p[in] o[k. cespitosa]]
		text = text.replaceAll("(?<=[a-z]\\.)(\\s|\\w\\[)+(?=\\(?\\w{2,20})", "~"); //u[o[(pappi)]] 0  r[p[in] o[k. cespitosa]] => u[o[(pappi)]] 0  r[p[in] o[k.~cespitosa]]
		
		ArrayList<String[]> results = new ArrayList<String[]>();
				
		String[] tokens = text.split("\\s+");
		//Matcher m = null;
		String ptn = "";
		for(String token : tokens){
			//m = taxonnamepattern2.matcher(token);
			//if(m.matches()){
			if(token.contains("taxonname-")){
				ptn +="N"; //taxon name
			}else if(token.length()==2 && token.endsWith(".")){
				ptn +="N"; //taxon name
			}else if(token.startsWith("{") || token.endsWith("}") || token.contains("[{") || token.matches(".*\\d.*")){ //-{discoid}
				ptn +="C"; //character
			}else if(token.startsWith("(") || token.endsWith(")") ||token.contains("o[") ||token.contains("u[")||token.contains("z[")){
				ptn +="C"; //organ
			}else if(token.matches("[,;]")){
				ptn +=",";				
			}else{
				ptn +="X"; //other tokens
			}
		}
		//evaluate the pattern
		Matcher m = sandwich.matcher(ptn);
		if(m.matches()){//multiple constraints: e.g., NNC,NNC ; NNC,CNN, what about CN,|C,CN or CN,N,|CN
			//must have puncts(or X) to separate the constraints
			//each constraint must NOT match sandwich pattern
			int p = 0; //pointing to the position of the beginning of ptn in tokens.
			int i = ptn.indexOf(',');//CXN,N,CCCXN,N
			String chunkptn = "";
			while(i>=0){
				String seg = ptn.substring(0, i+1); 
				if(sandwich.matcher(chunkptn+seg).matches()){
					//chunk is the longest sequence
					//find the match part of tokens for chunk: p-chunk.length() to p
					String[] tokenseg = new String[chunkptn.length()];
					for(int t = p-chunkptn.length(), j = 0; t < p; t++, j++){
						tokenseg[j] = tokens[t];
					}
					results.add(getSingleChunk(tokenseg, chunkptn));
					chunkptn = seg;
				}else{
					chunkptn +=seg;
				}
				ptn = ptn.replaceFirst(seg, "");
				p += i+1;
				i = ptn.indexOf(',');
			}
			//last seg
			String seg = ptn; 
			if(sandwich.matcher(chunkptn+seg).matches()){
				//chunk is the longest sequence
				//find the match part of tokens for chunk: p-chunk.length() to p
				String[] tokenseg = new String[chunkptn.length()];
				int t = 0; int j = 0;
				for(j = 0, t = p-chunkptn.length() ; t < p; t++, j++){
					tokenseg[j] = tokens[t];
				}
				results.add(getSingleChunk(tokenseg, chunkptn));
				chunkptn = seg;
				tokenseg = new String[chunkptn.length()];
				for(j = 0; t < tokens.length; t++, j++){
					tokenseg[j] = tokens[t];
				}
				results.add(getSingleChunk(tokenseg, chunkptn));
			}else{
				String[] tokenseg = new String[chunkptn.length()+seg.length()];
				int t = 0; int j = 0;
				for(j = 0, t = p-chunkptn.length() ; t < tokens.length; t++, j++){
					tokenseg[j] = tokens[t];
				}
				chunkptn += seg;
				results.add(getSingleChunk(tokenseg, chunkptn));				
			}
		}else{//single constraint: either N+C+ or C+N+
			results.add(getSingleChunk(tokens, ptn));
		}		

		return results;
	}
	/*
	 * above (leaves) in Taxon A => XCXN
	 * Taxon A above (leaves)  => XNXC
	 */
	private static String[] getSingleChunk(String[] tokens, String ptn) {
		int cindex = ptn.lastIndexOf("C");
		int nindex = ptn.lastIndexOf("N");
		String[] chunk = new String[3];
		chunk[2] = "";
		if(ptn.endsWith(",") || ptn.endsWith(";")){
			chunk[2] = ptn.charAt(ptn.length()-1)+"";
			ptn = ptn.replaceFirst(chunk[2]+"$", "");
		}
		if(cindex < nindex){ 
			//collect from beginning to the last C, including puncts and stop/prep words 
			String chara = "";
			for(int i = 0; i<=cindex; i++){
				chara += tokens[i]+" ";
			}
			//collect from after last C to the end of the text, 
			String names = "";
			//collect everything in between, including puncts
			for(int i = cindex+1; i<ptn.length(); i++){
				names += tokens[i]+" ";
			}

			chunk[0] = chara.trim();
			chunk[1] = names.trim().replaceAll("\\.~", ". ").replaceAll("(\\w\\[|\\]|\\(|\\))", "");
		}else{
			//collect from beginning to the last N
			String names = "";
			for(int i = 0; i<=nindex; i++){
				names += tokens[i]+" ";
			}
			//collect from after last N to the end 
			String chara = "";
			//collect everything in between, including puncts
			for(int i = nindex+1; i<ptn.length(); i++){
				chara += tokens[i]+" ";
			}
			chunk[0] = chara.trim();
			chunk[1] = names.trim().replaceAll("\\.~", ". ").replaceAll("(\\w\\[|\\]|\\(|\\))", ""); //m. u[o[(borealis)]] 
		}
		return chunk;
	}
	/**
	 * count the chunks in chunkedtokens
	 * @return
	 */
	private int chunks() {
		int count = 0;
		Iterator<String> it = this.chunkedtokens.iterator();
		while(it.hasNext()){
			if(it.next().matches("[^l]\\[.*")){
				count++;
			}
		}
		return count;
	}
	/**
	 * scan through a chunkedtokens to find Verbs not parsed as such by the parser
	 * find verbs by
	 * 1. look into this.verbs
	 * 2. find pattern o ting/ted by o, then t must be a verb and save this verb in verbs
	 */
	private void recoverVPChunks() {
		for(int i = 0; i < this.chunkedtokens.size(); i++){
			String t = this.chunkedtokens.get(i);
			if(t.contains("-")) continue; //check 751
			if(!t.contains("[") && this.verbs.contains(t)){
				recoverVPChunk(i);
			}else if(!t.contains("[") && (t.endsWith("ing")|| t.endsWith("ing}"))){
				 if(connects2organs(i)){
					 ChunkedSentence.verbs.add(t.replaceAll("\\W", ""));
					 recoverVPChunk(i);
				 }
			}/*else if(!t.contains("[")&& t.endsWith("ed") && this.chunkedtokens.size()>i+1 && this.chunkedtokens.get(i+1).matches(".*?\\bby\\b.*")){				
			}*/
		}
	}

	/**
	 * 
	 * @param i :index of the verb
	 * @return
	 */
	private boolean connects2organs(int i) {
		boolean organ1 = false;
		boolean organ2 = false;
		if(i>=1 && this.chunkedtokens.size()>i+1){
			String t = this.chunkedtokens.get(i-1);
			if(t.endsWith(">") || t.matches(".*\\bo\\[[^\\]\\[]*\\]+") || t.endsWith(")") ){
				organ1 = true;
			}
			
			do{
				i++;
				t = this.chunkedtokens.get(i).trim();
			}while(t.length()==0);
			if(t.endsWith(">") || t.matches("[uz]?\\[?\\bo\\[[^\\]\\[]*\\]+") || t.endsWith(")") ){
				organ2 = true;
			}
			
			/*for(int j = i+1; j < this.chunkedtokens.size(); j++){
				t = this.chunkedtokens.get(j);
				if(t.endsWith(">") || t.matches("[uz]?\\[?\\bo\\[[^\\]\\[]*\\]+") || t.endsWith(")") ){
					organ2 = true;
					break;
				}
				if((j == i+1 && t.equals(","))|| t.matches("\\w+")){
					organ2 = false;
					break;
				}
			}*/
		}
		return organ1 && organ2;
	}
	/**
	 * 
	 * @param i: the index of a possible verb
	 */
	private void recoverVPChunk(int i) {
	 
		String chunk = "";
		boolean foundo = false;
		int j = i+1;
		for(; j < chunkedtokens.size(); j++){
			//scan for the end of the chunk TODO: may refactor with normalizeOtherINs on this search
			String t = this.chunkedtokens.get(j);
			if(!foundo && t.equals("-LRB-/-LRB-")){ //collect everything untill -RRB-
				//test case: bearing 4-15 <bristles> -LRB-/-LRB- 0 r[p[in] o[{young} growth]] -RRB-/-RRB-
				boolean findRRB = false;
				do{
					t = this.chunkedtokens.get(j);
					if(t.equals("-RRB-/-RRB-")) findRRB = true;
					if(t.length()>0){
						chunk += t+" ";
					}
					this.chunkedtokens.set(j, "");
					j++;
				}while(j < this.chunkedtokens.size() && ! findRRB);
			}
			if(j==i+1 && t.matches(",")){ //verb not have object
				return;
			}
			if(t.matches("(;|\\.|-RRB-/-RRB-)")) break;
			if(foundo && (t.contains("-LRB-/-LRB-") || t.contains("{") || t.contains("~list~")||t.matches("(\\w+|,|;|\\.)")||t.contains("["))){
				break;
			}
			if(t.contains("<")){
				chunk += t+" ";
				foundo = true;
			}else if(t.matches(".*?\\bo\\[[^\\]*]+") || t.matches(".*?l\\[[^\\]]*\\]+")){//found noun)
				chunk += t+" ";
				foundo = true;
				j++;
				break;
			}else{
				chunk += t+" ";
			}						
		}
		if(!foundo) return;
		//format the chunk
		chunk = chunk.trim();
		if(chunk.endsWith(">")){
			chunk = "b[v["+this.chunkedtokens.get(i)+"]"+" o["+chunk.replaceAll("<", "(").replaceAll(">", ")")+"]]";
		}else if(chunk.matches(".*?\\bo\\[.*\\]+")){
			if(chunk.contains(" v[")){
				chunk = chunk.replaceFirst(" v[", " v["+this.chunkedtokens.get(i)+" ");
			}else if(chunk.matches("^r\\[.*")){//t[c[{extending}] r[p[to] o[(midvalve)]]]
				//chunk = chunk.replaceFirst("^r[p[", "b[v["+this.chunkedtokens.get(i)+ " "); //need to make the v is taken as a relation in processChunkVP
				chunk = "t[c["+this.chunkedtokens.get(i)+"] "+chunk+"]";
			}else if(chunk.startsWith("l[")){
				chunk = "b[v["+this.chunkedtokens.get(i)+"] "+chunk.replaceFirst("^l\\[", "o[")+"]"; 
			}else if(chunk.startsWith("u[")){
				chunk = chunk.replaceFirst("^u[", "b[v["+this.chunkedtokens.get(i)+ "] "); 
			}
		}
		//this.chunkedtokens.set(i, chunk);
		if(this.printRecover){
			System.out.println("verb chunk formed: "+chunk +" for \n"+this.sentid+"["+this.sentsrc+"]"+this.markedsent);
		}
		for(int k = i; k<j; k++){
			this.chunkedtokens.set(k, "");
		}
		this.chunkedtokens.set(j-1, chunk);
			/*
			t = t.replaceFirst("^u\\[", "").replaceFirst("\\]$", "");
			String o = t.substring(t.indexOf("o[")).trim();
			t = t.substring(0, t.indexOf("o[")).trim();
			if(t.length()>0){
				String[] states = t.split("\\s+");
				for(int k = 0; k < states.length; k++){
					String ch = Utilities.lookupCharacter(states[k], conn, characterhash, glosstable);
					if(ch!=null){
						scs = (scs.trim().length()>0? scs.trim()+"] ": "")+ch+"["+states[k].replaceAll("[{}]", "")+" ";
					}else{
						scs = (scs.trim().length()>0? scs.trim()+"] ": "")+"m["+states[k].replaceAll("[{}]", "")+" ";
					}
				}		
			}
			scs = (scs.trim().length()>0? scs.trim()+"] ": "")+o;
		}*/
		
	}
	/**
	 * attempts to mark modified non-subject organs as a chunk to avoid characters of these organs be attached to previous organs
	 * run this after recoverConjunctedOrgans to exclude organs that are objects of VP/PP-phrases)
	 * does not attempt to recognize conjunctions as the decisions may be context-sensitive
	 */
	private void recoverOrgans() {
		for(int i = this.chunkedtokens.size()-1; i >=this.pointer; i--){
			String t = this.chunkedtokens.get(i);
			if((t.endsWith(">") || t.endsWith(")") && ! t.matches("^[\\[(]?\\d.*"))){// 9(18?) is not an organ TODO: not dealing with nplist at this time, may be later
				recoverOrgan(i);//chunk and update chunkedtokens
			}
		}		
	}
	
	/**
	 * 
	 * @param last: the index of the last part of an organ name
	 */
	private void recoverOrgan(int last) {
		String chunk = this.chunkedtokens.get(last);
		boolean foundm = false; //modifiers
		boolean subjecto = false;
		int i = last-1;
		for(;i >=this.pointer; i--){
			String t = this.chunkedtokens.get(i);
			/*preventing "the" from blocking the organ following ",the" to being matched as a subject organ- mohan 10/19/2011*/
			if(t.matches("the|a|an")){
				if(i!=0){
					i=i-1;
					t = this.chunkedtokens.get(i);
				}
			}			
			/*end mohan*/
			if((t.matches("\\{\\w+\\}") && !t.matches("\\{("+StanfordParser.characters+")\\}")) || t.contains("~list~") || t.matches(".*?\\d+?")){
				chunk = t+" "+chunk;
				foundm = true;
			}else if(!foundm && (t.endsWith(">") ||t.endsWith(")") )){ //if m o m o, collect two chunks
				chunk = t+" "+chunk;
			}else{
				if(t.equals(",") || t.equals("-LRB-/-LRB-") || t.equals("-LSB-/-LSB-") || t.equals(";")) subjecto = true;
				break;
			}
		}
		//if(i==0) subjecto = true;
		//reformat this.chunkedtokens
		if(subjecto || i==-1){ 
			chunk = "z["+chunk.trim().replaceAll("<", "(").replaceAll(">", ")")+"]";
		}else{
			chunk = "u["+chunk.trim().replaceFirst("[<(]", "o[(").replaceFirst("[)>]$", ")]").replaceAll("<", "(").replaceAll(">", ")").replaceAll("[{}]", "")+"]";//<leaf><blade> => u[o[(leaf)(blade)]]
		}
		
		//reset from i+2 to last
		for(int j = i+1; j <last; j++){
			this.chunkedtokens.set(j, "");
		}
		this.chunkedtokens.set(last, chunk);
		if(this.printRecover){
			System.out.println("nsorgan chunk formed: "+chunk +" for \n"+this.sentid+"["+this.sentsrc+"]"+this.markedsent);
		}
	}
	/**
	 * attempts to include broken-away conjuncted organs to pp and vb phrase
	 */
	private void recoverConjunctedOrgans() {
		for(int i = 0; i < this.chunkedtokens.size(); i++){
			String t = this.chunkedtokens.get(i);
			if(this.chunkedtokens.size()>i+2){
				if((t.startsWith("r[p") || t.startsWith("b[v")) && 
						(this.chunkedtokens.get(i+1).matches("(and|or|plus)")||
								(this.chunkedtokens.get(i+1).matches(",") && this.chunkedtokens.get(i+2).matches("(and|or|plus)")))) {//check 211
					recoverConjunctedOrgans4PP(i);
				}else if((t.startsWith("r[p") || t.startsWith("b[v")) && this.chunkedtokens.get(i+1).startsWith("<")){//found a broken away noun
					int j = i;
					String newo = "";
					String o = this.chunkedtokens.get(++j);					
					do{
						newo += o;
						this.chunkedtokens.set(j, "");
						o = this.chunkedtokens.get(++j);					
					}while (o.startsWith("<"));
					String p1 = t.replaceFirst("\\]+$", "");
					String p2 = t.replace(p1, "");
					newo = newo.replaceAll("<", "(").replaceAll(">", ")").trim();
					t = p1+" "+newo+p2;
					this.chunkedtokens.set(i, "");
					this.chunkedtokens.set(--j, t);
				}
				
				
				/*else if (t.startsWith("b[v") && this.chunkedtokens.get(i+1).matches("(and|or|plus)")){
					recoverConjunctedOrgans4VB(i);
				}*/
			}
		}
	}

	/**
	 * recover if what follows the PP is "and|or|plus" and a (modified) organ followed by a , or a series of chunks
	 * @param i: the index where a PP-chunk followed by and|or|plus is found
	 */
	private void recoverConjunctedOrgans4PP(int i) {
		String recovered = this.chunkedtokens.get(i+1)+" ";//and|or|plus
		boolean foundo = false;
		boolean recover = true;
		int endindex = 0;
		for(int j = i+2; j < this.chunkedtokens.size(); j++){
			String t = this.chunkedtokens.get(j);
			if(!foundo && (t.matches("\\{\\w+\\}") || t.equals(",") || t.contains("~list~"))){//states before an organ 
				recovered += t+" ";
			}else if(t.matches("<\\w+>") || t.contains("l[")){//organ
				recovered += t+" ";
				endindex = j;
				foundo = true;
			}else if(foundo && t.matches("(,|;|\\.)")){//states before an organ 
				break; //organ followed by ",",  should recover
			}else if(foundo && t.contains("[") && !t.contains("~list~")){//found or not found organ
				//do nothing
			}else{
				recover = false;
				break;
			}
		}
		
		if(recover){
			//reformat: insert recovered before the last set of ] 
			String chunk = this.chunkedtokens.get(i);
			String p1 = chunk.replaceFirst("\\]+$", "");
			String p2 = chunk.replace(p1, "");
			recovered = recovered.replaceAll("<", "(").replaceAll(">", ")").trim();
			chunk = p1+" "+recovered+p2;
			this.chunkedtokens.set(i, "");
			//reset from i+1 to endindex
			for(int j = i+1; j <endindex; j++){
				this.chunkedtokens.set(j, "");
			}
			this.chunkedtokens.set(endindex, chunk);
			if(this.printRecover){
				System.out.println("pp/vp object chunk formed: "+chunk +" for \n"+this.sentid+"["+this.sentsrc+"]"+this.markedsent);
			}
			
		}
	}
	/**
	 * insert segment marks in chunkedtokens while producing this.chunkedsent
	 * after first round of segmentation, proceed to the 2nd round to disambiguate ", those of" 
	 */
	private void segmentSent() {
		int i;
		for(i = this.chunkedtokens.size()-1; i>=0; i--){
			String t = this.chunkedtokens.get(i);
			if(t.compareTo("") !=0){
				this.chunkedsent = t+" "+this.chunkedsent;;
			}
			if(t.indexOf('<')>=0 || t.indexOf("z[")>=0){//z[ is chunkOrgan
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
		disambiguateThose();
	}
	
	/**
	 * <corollas> {purple} , those of {sterile} <florets> ± {expanded} , {exceeding} <corollas> of {fertile} <florets> , those of {fertile} <florets> 15-18 {mm} .
	 * <phyllaries> {many} in 6-8 <series>... , <apices> {shape~list~acute~to~acuminate} , those of {innermost} {bristly-ciliate-or-plumose} .
	 * find "those" instances in chunkedsent, fix chunkedsent, then fix chunkedtokens
	 * fix = replacing those with the subject of the last segment
	 */
	private void disambiguateThose() {
		Pattern p = null;
		if(this.chunkedsent.indexOf(" those r[p[of")>0){
			//p = Pattern.compile("((?:.*?SG\\WSG.*|^)<(.*?)>.*?)those(\\s+r?\\[?p?\\[?of.*)");
			p = Pattern.compile("((?:.*?SG\\WSG.*|^)(?:z\\[\\(|<)(.*?)(?:>|\\)\\]).*?)those(\\s+r?\\[?p?\\[?of.*)");
			Matcher m = p.matcher(this.chunkedsent);
			while(m.matches()){
				String noun = m.group(2);
				int indexOfthose = m.group(1).split("\\s+").length;
				//in case there are to~12~cm, need to adjust indexOfthose
				String textbeforethose = m.group(1);
				Pattern pt = Pattern.compile("(.*?)\\b(to~\\d+~(?:"+this.units+").*?)\\b(.*)");
				Matcher mt = pt.matcher(textbeforethose);
				while(mt.matches()){
					textbeforethose = mt.group(3);
					indexOfthose += mt.group(2).replaceAll("[^~]", "").length();
					mt = pt.matcher(textbeforethose);
				}
				//update chunkedsent and chunkedtokens
				//"those" may be included in a chunk
				String token = this.chunkedtokens.get(indexOfthose); //indexOfthose may be off due to empty elements in chunkedtokens
				while(!token.contains("those")) {
					indexOfthose++;
					token = this.chunkedtokens.get(indexOfthose);
				}
				if(token.compareTo("those")==0){
					String temp = m.group(1).trim();
					temp = temp.replaceFirst(",$", "SG,SG");
					this.chunkedsent = temp+" <"+noun+">"+m.group(3);
					this.chunkedtokens.set(indexOfthose, "<"+noun+">");
					if(this.chunkedtokens.get(indexOfthose-1).compareTo(",")==0){
						this.chunkedtokens.set(indexOfthose-1, "SG,SG");
					}
				}else{//in a chunk: break the chunk into two
					int indexOfchunk = findChunk(indexOfthose, "those");
					String chunk = this.chunkedtokens.get(indexOfchunk);
					String[] two = chunk.split("\\s*those\\s*");
					two[0] += " ("+noun+")";
					//find how many closing brackets are needed in two[0] and form the two new chunks
					int lb = two[0].replaceAll("[^\\[]", "").length();
					int rb = two[0].replaceAll("[^\\]]", "").length();
					for(int i = 0; i<lb-rb; i++){
						two[0]+="]";
						two[1] = two[1].replaceFirst("\\]$", "");						
					}
					String newchunk = two[0]+" "+two[1];
					this.chunkedsent = this.chunkedsent.replace(chunk, newchunk);
					//replace the old chunk with two chunks in this.chunkedtokens 
					if(this.chunkedtokens.get(indexOfchunk+1).length()==0){
						this.chunkedtokens.set(indexOfchunk, two[0]);
						this.chunkedtokens.set(indexOfchunk+1, two[1]);
					}else if(this.chunkedtokens.get(indexOfchunk-1).length()==0){
						this.chunkedtokens.set(indexOfchunk-1, two[0]);
						this.chunkedtokens.set(indexOfchunk, two[1]);
					}
				}
				m = p.matcher(this.chunkedsent);
			}
		}
	}
	
	/**
	 * find the index in this.chunkedtokens that is near indexofkeyword and hold a chunk containing "keyword"
	 * @param indexOfkeyword
	 * @param keyword
	 * @return
	 */
	private int findChunk(int indexOfkeyword, String keyword) {
		//is this it?
		String chunk = "";
		int i = indexOfkeyword;
		chunk = this.chunkedtokens.get(i);
		if(chunk.indexOf(keyword)>=0){
			return i;
		}
		//search downwards
		do{
			i++;
			chunk = this.chunkedtokens.get(i++);
		}while(chunk.length()==0);
		if(chunk.indexOf(keyword)>=0){
			return i;
		}
		//search upwards
		chunk = "";
		i = indexOfkeyword;
		do{
			i--;
			chunk = this.chunkedtokens.get(i);
		}while(chunk.length()==0);
		if(chunk.indexOf(keyword)>=0){
			return i;
		}		
		System.out.println("Wrong chunks in ChunkedSentence, System exiting.");
		System.exit(1); //should never reach here
		return 0;
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
	private int normalizeThan(){
		int count = 0;
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
				if(/*more.length()==0 &&*/ //result in the capture of "more" that is not immediately before the "than" token (i.e., hide the "more" that is immediately before "than")
						((token.matches(".*?\\b(\\w+er|more|less)\\b.*") && token.indexOf("er>")<0)
								|| (token.length()>0 && this.markedsent.indexOf(token+" than")>=0))){ //<inner> is not, but <longer> than is 
					firstmorei = i;
					if(token.matches(".*?\\bmore\\b.*")){
						more = "more";
					}else if(token.matches(".*?\\b\\w+er\\b.*")){
						if(token.matches("^rather\\s*")){ //To prevent matching rather than.
							more = "rather";
						}else{
							more = "er";
						}
					}
				}else if(more.compareTo("er") == 0 && !token.matches(".*?\\b(\\w+er|more|less|and|or|than)\\b.*") ){
					more = "";
					firstmorei = this.chunkedtokens.size();;
				}
				//if(token.matches(".*?\\bthan\\b.*")){
				if(token.matches(".*?\\bthan\\b.*")&&!more.contains("rather")){
					//needs normalization
					thani = i;
					if(firstmorei == thani){
						//token can be a complex chunk and firstmorei == thani
						//e.g.: r[p[of] o[10-14 , orientation[{orientation~list~spreading~to~appressed}] -LRB-/-LRB- {thinner} than (phyllaries) -RRB-/-RRB- , {dark-c-green} , shape[{shape~list~broadly~ovate~or~ovate~to~oblong}] (bractlets)]]
						Pattern than = Pattern.compile("(.*? )(\\{(?:\\w+er|more|less)\\}?.*? than .*?\\(.*?\\))(.*)");
						Matcher m = than.matcher(token);
						String newtoken = "";
						while(m.matches()){
							newtoken +=m.group(1);
							newtoken += "n["+m.group(2)+"]";
							token = m.group(3);
							m = than.matcher(token);
						}
						newtoken += token;
						this.chunkedtokens.set(i, newtoken);					
					}else if(firstmorei < thani){
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
							if(w.matches("-L[SR]B-/-L[SR]B-")){ 
								String left = w;
								//case 1: the previous word is an organ, the than-chuck should end there. 
								if(i-1>=0 && (np.endsWith(">") || np.endsWith(" ones "))){
									np = np.replace(" ones ", " (ones) ");
									np = np.replaceAll("<", "(").replaceAll(">", ")").trim();
									//this.chunkedtokens.set(thani, "n["+np+"]");
									this.chunkedtokens.set(i-1, "n["+np+"]");
									this.chunkedtokens.set(thani, "");
									count++;
									break;									
								}else{
								//case 2 else: continue collect everything untill -RRB-
									boolean findRRB = false;
									do{
										w = this.chunkedtokens.get(i);
										String right = left.replaceAll("-L", "-R");
										if(w.compareTo(right)==0) findRRB = true;
										//if(w.equals("-RRB-/-RRB-")) findRRB = true;
										if(w.length()>0){
											//np += w+" ";
											np += this.chunkedtokens.get(i)+" ";
										}
										this.chunkedtokens.set(i, "");
										i++;
									}while(i < this.chunkedtokens.size() && ! findRRB);
								}
							}
							//if(w.matches("\\b("+preps+"|and|or|that|which|but)\\b") || w.matches("\\W")){
							if(w.matches("\\b("+preps+"|and|that|which|but)\\b.*") || w.matches("\\p{Punct}") || w.matches("-R[SR]B-/-R[SR]B-")){ //should allow ±, n[{shorter} than] ± {campanulate} <throats>
								np = np.replaceAll("<", "(").replaceAll(">", ")").trim();
								//this.chunkedtokens.set(thani, "n["+np+"]");
								this.chunkedtokens.set(i-1, "n["+np+"]");
								this.chunkedtokens.set(thani, "");
								count++;
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
		return count;
	}
	
	/**
	 * expanded to <throats>
	 * to 6 m.
	 */
	private int normalizeTo(){
		int count = 0;
		String np = "";
		boolean startn = false;
		//ArrayList<String> copy = (ArrayList<String>)this.chunkedtokens.clone();
		for(int i = 0; i<this.chunkedtokens.size(); i++){
			ArrayList<String> copy = (ArrayList<String>)this.chunkedtokens.clone();
			String token = this.chunkedtokens.get(i);
			//{proximal}, , , , , , , , , , , , , , , r[p[to] o[-LRB-/-LRB- i . e . , outside of or {abaxial} to -RRB-/-RRB- the (florets)]],
			if(token.startsWith("r[p[to]")){ //include {proximal} in the chunk. This if-control was added because the change in SentenceChunker (i.e., treating 'to' as an preposition)
				int j = i;
				String chara ="";
				do{
					if(j > 0) chara = this.chunkedtokens.get(--j).trim();
				}while(chara.length()==0);
				
				if(chara.startsWith("{") && chara.endsWith("}")){//found the character
					this.chunkedtokens.set(j, "");
					//w[{proximal} to the (florets)]
					String chunk = "w["+(chara + " "+token.replaceAll("(\\w\\[|\\])", "")).trim()+"]";
					this.chunkedtokens.set(i, chunk);
					count++;
				}
			}
			
			if(token.compareTo("to") == 0 || token.matches(".*?\\bto]+$")){
				
				//scan for the next organ
				for(int j = i+1; j<this.chunkedtokens.size(); j++){
					String t = this.chunkedtokens.get(j).trim();
					if(t.equals("-LRB-/-LRB-")){ //collect everything untill -RRB-
						boolean findRRB = false;
						do{
							t = this.chunkedtokens.get(j);
							if(t.equals("-RRB-/-RRB-")) findRRB = true;
							if(t.length()>0){
								np += t+" ";
							}
							this.chunkedtokens.set(j, "");
							j++;
						}while(j < this.chunkedtokens.size() && ! findRRB);
					}
					if(j==i+1 && t.matches("\\d[^a-z]*")){//match "to 6[-9]" ; not match "to 5-lobed"
						copy = formRangeMeasure(i, copy); //use the copy to undone the insertion of w[] 
						break;
					}
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
						if(t.matches(".*?\\b("+ChunkedSentence.prepositions+"|and|or|that|which|but)\\b.*") || t.matches(".*?[>;,:].*") ||(t.matches("^\\w+\\[.*") && j!=i-1) ){ //the last condition is to avoid nested chunks. cannot immediately before w[].e.g: b[v[{placed}] o[{close}]] w[to {posterior} (shell) (margin)] ; 
							np = np.replaceAll("<", "(").replaceAll(">", ")").replaceAll("\\s+", " ").trim();
							//np = np.replaceAll("\\s+", " ").trim();
							this.chunkedtokens.set(i, "w["+np+"]"); //replace "to" with np
							count++;
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
		return count;
	}
	/**
	 * form a chunk if a pattern "to # unit" is found starting from i
	 * @param i: index of "to", which is followed by a number
	 * @return this.chunkedtokens
	 */
	private ArrayList<String> formRangeMeasure(int i, ArrayList<String> chunkedtokens) {
		String chunk = "to~"+chunkedtokens.get(i+1)+"~"; //"to"
		if(chunkedtokens.size()>i+2){
			String unit = chunkedtokens.get(i+2).replaceAll("\\W", " ").trim();
			if(unit.matches("("+this.units+"|"+this.degree+")")){
				chunk += unit;
				chunkedtokens.set(i+2, chunk);
				chunkedtokens.set(i+1, "");
				if(chunkedtokens.get(i).equals("to")){
					chunkedtokens.set(i, "");
				}else{
					chunkedtokens.set(i, chunkedtokens.get(i).replaceFirst("\\s+to(?=\\W+$)", ""));
				}
			}
		}		
		return chunkedtokens;
	}
	/**
	 * most [of] lengths
	 * [in] zyz arrays
	 */
	private int normalizeOtherINs(){
		
		//boolean startn = false;
		int count = 0;
		String preps = ChunkedSentence.prepositions.replaceFirst("\\bthan\\|", "").replaceFirst("\\bto\\|", "");
		//preps += "|as-\\w+-as";
		for(int i = 0; i<this.chunkedtokens.size(); i++){
			String token = this.chunkedtokens.get(i);
			
			//if(token.matches(".*?p\\[[a-z]+\\]+") || token.matches(".*?\\b("+preps+")\\b\\]*$")){//[of] ...onto]]
			if(token.matches(".*?\\b("+preps+")\\b\\]*$")){//[of] ...onto]]
				if(this.printNorm){
					System.out.println(token+" needs normalization!");
				}
				// a prep is identified, needs normalization
				ArrayList<String> copy = (ArrayList<String>)this.chunkedtokens.clone();
				//String nscopy = null;
				String npcopy = null;
				ArrayList<String> ctcopy = null;
				boolean startn = false;
				String np = "";
				//String ns = "";
				boolean foundorgan = false;
				//boolean ofnumber = false;
				//lookforward in chunkedtokens to find the object noun
				int j = 0;
				for(j = i+1; j<this.chunkedtokens.size(); j++){
					String t = this.chunkedtokens.get(j).trim();
					if(!foundorgan && !startn && t.equals("-LRB-/-LRB-")){ //collect everything untill -RRB-
						boolean findRRB = false;
						do{
							t =this.chunkedtokens.get(j); 
							if(t.equals("-RRB-/-RRB-")) findRRB = true;
							if(t.length()>0){
								np += t+" ";
							}
							this.chunkedtokens.set(j, "");
							j++;
						}while(j < this.chunkedtokens.size() && ! findRRB);
						t = this.chunkedtokens.get(j).trim();
					}
								
					if(t.length() == 0) continue;
					//if(t.equals("-LRB-/-LRB-"))
					if(j==i+1 && t.matches("^[,;\\.]")){//"smooth throughout, ", but what about "smooth throughout OR hairy basally"?
						if(this.printNorm){
							System.out.println("encounter ',' immediately, no object is expected");
						}
						break; 						
					}
					/*if(t.startsWith("r[p[") && !np.matches(".*?\\b(or|and)\\b\\s+$")){
						npcopy = np;//TODO: 4/14/2011 check out 501.txt-4, 502.txt-5 "after flowering, 10 cm in fruit" 512.txt-11 "differing from inner, highly variable in <color>"
						break;
					}*/
					if(!foundorgan && startn && t.indexOf('<')<0 && t.indexOf('(')<0 && !Utilities.isNoun(t, nouns, notnouns)){ //test whole t, not the last word once a noun has been found
						//save ns for now, but keep looking for organs
						//nscopy = nscopy == null ? ns : nscopy; //keep only the first copy
						npcopy = npcopy == null? np : npcopy;
						ctcopy = ctcopy == null? (ArrayList<String>)this.chunkedtokens.clone():ctcopy;
					}
					if(ishardstop(j)) break;
					if(startn && !foundorgan && ishardstop(j)){
						//hard stop encountered, break
						//ns = nscopy;
						np = npcopy;
						this.chunkedtokens = ctcopy;
						break;
					}
					
					if(foundorgan && t.indexOf('<')<0 && t.indexOf('(')<0){ //test whole t, not the last word once a noun has been found
						break; //break, the end of the search is reached, found organ as object
					}
					
					np +=t+" "; //any word in betweens
					this.chunkedtokens.set(j, "");
					
					if(t.indexOf('<')>=0 ||t.indexOf('(')>=0){ //t may have []<>{}
						startn = true; //not break yet, may be the next token is also a noun
						foundorgan = true;
					}
					
					if(!foundorgan && Utilities.isNoun(t, nouns, notnouns)){ //t may have []<>{}
						startn = true; //won't affect the value of foundorgan, after foundorgan is true, "plus" problem
						if(Utilities.isPlural(t)){
							foundorgan = true;
							np = np.trim();
							if(np.lastIndexOf(" ")>0){
								np = np.substring(0, np.lastIndexOf(" "))+" "+ "("+t.replaceAll("\\W", "")+") ";
							}else{
								np = "("+np.replaceAll("\\W", "")+") ";
							}
						}
					}
				}
				
				/*
				 for(int j = i+1; j<this.chunkedtokens.size(); j++){
					String t = this.chunkedtokens.get(j).trim();
					if(startn && t.indexOf('<')<0 && !Utilities.isNoun(t, nouns)){ //test whole t, not the last word once a noun has been found
						break; //break, the end of the search is reached
					}
					np +=t+" ";
					this.chunkedtokens.set(j, "");
					
					if(t.indexOf('<')>=0 ||t.indexOf('(')>=0 || Utilities.isNoun(t, nouns)){ //t may have []<>{}
						startn = true; //not break yet, may be the next token is a noun
						ns += t+" ";
					}
				} 
				 */
				//form the normalized chunk
				if(foundorgan || npcopy!= null /*|| ofnumber*/){
					//ns = ns.trim();
					//if(!ns.endsWith("]")){ //not already a chunk
						//np = np.replace(ns, "").trim();
						//ns  = "("+ns.replaceAll("[{(<>)}]", "").replaceAll("\\s+", ") (")+")"; //mark the object as organ word by word
						//np = (np.replaceAll("<", "(").replaceAll(">", ")")+" "+ns).trim();
						np = np.replaceAll("<", "(").replaceAll(">", ")").replaceAll("\\s+", " ").trim();
					//}
					String symbol = "o";	
					/*if(ofnumber){
						symbol = "c";
					}*/
					if(token.indexOf('[')>=0){
						String rest = token.replaceFirst("\\]+$", "").trim();
						String brackets = token.replace(rest, "").replaceFirst("\\]$", "").trim();
						token = rest + "] "+symbol+"["+np.trim()+"]"+brackets;
						this.chunkedtokens.set(i, token);
						if(this.printNorm){
							System.out.println("!normalized!: "+token);
						}
					}else{//without [], one word per token
						token = "r[p["+token+"] "+symbol+"["+np.trim()+"]]";
						this.chunkedtokens.set(i, token);
						if(this.printNorm){
							System.out.println("!normalized!: "+token);
						}
					}
					count++;
				}else{ 
					if(j-i==1){
						//cancel the normalization attempt on this prep, return to the original chunkedtokens
						this.chunkedtokens = copy;
					}else{//reached the end of the sentence or hit a hardstop. This is the case for "plumose on distal 80 % ."? or "throughout or only r[p[in] o[ultimate branches]]"
						this.chunkedtokens = copy;
						//if np =~ ^or and the next token is a prep chunk, then merge np and the chunk: r[i[throughout or only in] o[ultimate branches]]
						String nextoken = this.chunkedtokens.get(j);
						if(np.startsWith("or ") && nextoken.startsWith("r[")){
							token ="r[p["+token+" "+np.trim()+" "+nextoken.replaceFirst("^r\\[\\w\\[", "");
							this.chunkedtokens.set(i,  token);
							for(int k = i+1; k<=j; k++){
								this.chunkedtokens.set(k, "");
							}
						}else{
							//np = np.replaceAll("\\s+", " ").trim();
							String head = token.replaceFirst("\\]+$", "").trim();//assuming token is like r[p[in]]
							String brackets = token.replace(head, "").replaceFirst("\\]$", "").trim();
							String rest = np.replaceFirst(".*?(?=(\\.|;|,|\\band\\b|\\bor\\b|\\w\\[))", "").trim();
							if(!rest.equals(np.trim())) np = np.replace(rest, ""); //perserve spaces for later
							String object = np.replaceAll("\\s+", " ").trim();
							if(object.length()>0){
								token = head + "] o["+np.replaceAll("\\s+", " ").trim()+"]"+brackets; //token = r[p[on] o[{proximal} 2/3-3/4]]: <leaves> on {proximal} 2/3-3/4
								if(!token.startsWith("r[")) token = "r[p["+token+"]";
								//if next token is r[p[ too, join the pp
								int npsize = np.split("\\s").length; //split on single space to perserve correct count of tokens
								int k = i+1;
								for(; k<=i+npsize; k++){
									this.chunkedtokens.set(k, "");
								}
								while(this.chunkedtokens.get(k).length()==0)k++;
								if(this.chunkedtokens.get(k).startsWith("r[p[")){//join
									token = token.replaceAll("(\\w\\[|\\])", "");
									token = chunkedtokens.get(k).replaceFirst("r\\[p\\[", "r[p["+token+" ");
									this.chunkedtokens.set(k, token);
									this.chunkedtokens.set(i, "");
								}else{
									this.chunkedtokens.set(i, token);
								}
								if(this.printNorm){
									System.out.println("!default normalized to (.|;|,|and|or|r[)!: "+token);
								}
								count++;
							}
						}
					}
				}
			}
			//i=i+1;

		}
		/*if(!startn){
			this.chunkedtokens = copy;
		}*/
		return count;
	}
	

	

	private boolean ishardstop(int j) {
		String t1 = this.chunkedtokens.get(j).trim();
		
		if(t1.equals("-RRB-/-RRB-") ||t1.equals("-LRB-/-LRB-") ||t1.equals("-RSB-/-RSB-") ||t1.equals("-LSB-/-RSB-") ){
			return true;
		}
		
		if(t1.matches("^\\w\\[.*")){
			return true;
		}
		if(t1.startsWith(".") || t1.startsWith(";")){
			return true;
		}
		
		if(this.chunkedtokens.size()==j+1){
			return true;
		}

		String t2 = this.chunkedtokens.get(j+1).trim();
		if(t1.startsWith(",") && t2.matches("^\\W*[<(].*")){
			return true;
		}
		return false;
	}
	public String toString(){
		return this.chunkedsent;
	}
	public int getPointer(){
		return this.pointer;
	}
	//mohan code to reset the pointer
	public void resetPointer(){
		this.pointer=0;
	}
	//end mohan code
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
		}else if(tobeskipped[tobeskipped.length-1].compareTo("whole_organism")==0){ //Treatises: "general" = "whole_organism"
			//do not skip
		}else{
			int sl = tobeskipped.length;
			for(int i = 0; i<this.chunkedtokens.size(); i++){
				//chunkedtokens may be a list: shape[{shape~list~planoconvex~to~ventribiconvex~subquadrate~to~subcircular}]
				/*wcount += (this.chunkedtokens.get(i)+" a").replaceAll(",", "or").replaceAll("\\b(or )+", "or ")
				.replaceFirst("^.*?~list~", "").replaceAll("~", " ")
				.trim().split("\\s+").length-1;*/
				wcount ++;
				//if(this.chunkedtokens.get(i).matches(".*?\\b"+tobeskipped[sl-1]+".*") && wcount>=sl){
				if(this.chunkedtokens.get(i).replace("SG", "").replaceAll("(\\w+\\[|\\]|\\)|\\(|\\{|\\})", "").replaceAll("-", "_").toLowerCase().matches(".*?\\b"+(tobeskipped[sl-1].length()-2>0 ? tobeskipped[sl-1].substring(0, tobeskipped[sl-1].length()-2) : tobeskipped[sl-1])+".*") && wcount>=sl){//try to match <phyllaries> to phyllary, "segement I", i is 1-character long
					if(wcount==sl){
						this.pointer = i;
					}else{
						//if wcount > sl, then there must be some extra words that have been skipped
						//put those words in chunkedtokens for process
						//example:{thin} {dorsal} {median} <septum> {centrally} only ; 
						int save = i;
						if(!this.chunkedtokens.get(i).matches(".*?\\bof\\b.*")){//, , l[(taproots) and clusters], , , , r[p[of] o[{coarse} {fibrous} (roots)]],, tobeskiped is "taproot and root"
							i++;
							for(int j = 0; j < wcount-sl; j++){
								this.chunkedtokens.add(i++, this.chunkedtokens.get(j));
							}
							this.chunkedtokens.add(i++, ",");
						}
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
		//return ck==null? new ChunkEOS(".") : ck;
		ck = ck==null? new ChunkEOS(".") : ck;
		//parenthetical expression
		//String content = ck.toString();
		if(ck instanceof ChunkChrom) return ck;
		
		/*
		if(content.indexOf("-LRB-")>=0 || content.indexOf("-RRB-")>=0 || content.indexOf("-LSB-")>=0 || content.indexOf("-RSB-")>=0){
			//remove () and its content
			String trimmed = content.replaceAll("-L[RS]B-/-L[RS]B-.*?-R[RS]B-/-R[RS]B-", "");
			String removed = content.replaceAll(".*?(?=-L[RS]B-/-L[RS]B-)", "").replaceAll("(?<=-R[RS]B-/-R[RS]B-).*", "");
			System.err.println("Removed "+removed+ " from "+content);
			ck.setText(trimmed.replaceAll("\\s+", " "));
		}
		*/
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
		if(pointer == this.chunkedtokens.size()) return chunk;
		String token = this.chunkedtokens.get(pointer);////a token may be a word or a chunk of text
		while(token.trim().length()==0){
			pointer++;
			if(pointer < this.chunkedtokens.size()) token = this.chunkedtokens.get(pointer);
			else return chunk;
		}
		token = token.compareTo("±")==0? "moreorless" : token;
		token = token.matches(".*?\\d.*") && !token.startsWith("r[p[")? NumericalHandler.originalNumForm(token) : token;
		
		if(token.compareTo("and")==0){
			pointer++;
			return new ChunkComma(",");
		}

		if((NumericalHandler.isNumerical(token) ||token.matches("^to~\\d.*")|| token.matches("[wl]\\s*\\W\\s*[wl]")) && !token.matches(".*?[nx]=[\\[(\\d].*")){//l-w or l/w
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
				token = token.replaceAll("×[^0-9(\\[]*", " × ").replaceAll("(?<=[^a-z])(?=[a-z])", " ").replaceAll("(?<=[a-z])(?=[^a-z])", " ").replaceAll("\\s+", " ").trim();
				chunk = new ChunkArea(token);
				pointer++;
				return chunk;
			}
		}

		if(token.indexOf("=")>0){//chromosome count 2n=, FNA specific, also seen in Diatom descriptions
			if(token.matches(".?\\b\\d?[xn]=\\s*[\\[(]?\\d.*")){
				String l = "";
				String t= this.chunkedtokens.get(pointer++);
				while(t.indexOf("SG")<0 && this.chunkedtokens.size()>pointer-1){
					l +=t+" ";
					t= this.chunkedtokens.get(pointer++);				
				}
				l = l.replaceFirst("\\b\\d?[xn]=", "").trim();
				chunk = new ChunkChrom(l);
				return chunk;
			}
		}
		
		//create a new ChunkedSentence object
		if(token.startsWith("s[")){
			ArrayList<String> tokens = new ArrayList<String>();
			String text = token.replaceFirst("s\\[", "").replaceFirst("\\]$", "");
			//break text into correct tokens: s[that is {often} {concealed} r[p[by] o[(trichomes)]]] ;
			tokens = Utilities.breakText(text);
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
			//if(pointer == this.chunkedtokens.size()) return new ChunkEOL(""); //treat . in () as chunkcomma, because EOL will reset unassignedmodifier and unassignedcharacters. Width (tr.) of ventral areas needs character width.
			return new ChunkComma("");
		}
		
		if(token.matches("\\b(and|either)\\b")){
			pointer++;
			this.unassignedmodifier = null;
			return null;
		}
		//end of a segment
		if(token.matches("SG[;:\\.]SG")){
			this.inSegment = false;
			pointer++;
			//this.unassignedmodifier = null;
			return new ChunkEOL(""); //end of line/statement
		}
		
		if(token.matches("SG,SG")){
			this.inSegment = false;
			pointer++;
			this.unassignedmodifier = null;
			return new ChunkEOS("");//end of segment/substence
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
		
		//create a new ChunkedSentence object for bracketed text 
		//if(token.startsWith("-LRB-/-LRB-")){
		if(token.startsWith("-LRB-/-LRB-") || token.startsWith("-LSB-/-LSB-")){
			ArrayList<String> tokens = new ArrayList<String>();
			//collect text in brackets
			String text="";
			int lr = 0;
			int ls = 0;
			if(this.chunkedtokens.get(pointer).equals("-LRB-/-LRB-"))	lr++;
			if(this.chunkedtokens.get(pointer).equals("-LSB-/-LSB-"))	ls++;
			while(pointer+1 < this.chunkedtokens.size() && (lr != 0 || ls !=0)){
				pointer++;
				if(this.chunkedtokens.get(pointer).equals("-LRB-/-LRB-"))	lr++;
				if(this.chunkedtokens.get(pointer).equals("-LSB-/-LSB-"))	ls++;
				if(this.chunkedtokens.get(pointer).equals("-RRB-/-RRB-"))	lr--;
				if(this.chunkedtokens.get(pointer).equals("-RSB-/-RSB-"))	ls--;
				text += this.chunkedtokens.get(pointer)+" ";
				tokens.add(this.chunkedtokens.get(pointer));
			}
			//form chunk
			text = text.trim();
			boolean geo = false; //other types are already assembled in ChunkedSentence, only deals with geo here
			if(text.length()>0){
				this.pointer++;
				if(text.endsWith("-RSB-/-RSB-")){
					geo = true;
				}
				text = text.replaceFirst("-R[RS]B-/-R[RS]B-$", "").trim();
				if(tokens.get(tokens.size()-1).matches("-R[RS]B-/-R[RS]B-")) tokens.set(tokens.size()-1, null);
				if(!text.matches(".*?[,;\\.:]$")){
					text +=" .";
					tokens.add(".");
				}
				Chunk c = geo? new ChunkScopeGeo(text) : new ChunkBracketed(text);
				c.setChunkedTokens(tokens);
				return c;
			}
			/*String text = "";
			if(token.indexOf("-RRB-/-RRB-")<0){
				String t = this.chunkedtokens.get(++this.pointer);
				while(!t.endsWith("-RRB-/-RRB-")){
					tokens.add(t);
					text += t+ " ";
					t = this.chunkedtokens.get(++this.pointer);
				}
			}*/
			
			/*
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
			*/
		}
		
		
		//all chunks
		if(token.matches("^\\w+\\[.*")){
			String type = chunkType(pointer);
			token = this.chunkedtokens.get(pointer); //as checkType may have reformatted token.
			token = token.matches(".*?\\d.*")? NumericalHandler.originalNumForm(token) : token;
			try{
				if(type != null){
					Class c = Class.forName("fna.charactermarkup."+type);
					Constructor cons = c.getConstructor(String.class);
					pointer++;
					//deal with any unassignedmodifier when EOS is approached.
					//if(this.unassignedmodifier != null && this.chunkedtokens.get(pointer).matches("(SG)?\\W(SG)?")){
					if(this.unassignedmodifier != null){ //did not see why the 2nd condition is needed. Here, assuming any unassigned modifier should be applied to the next valid chunk
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
					//skip tokens until a [,;:.] 
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
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
						System.getProperty("line.separator")+"source:"+this.sentsrc+System.getProperty("line.separator")+
						sw.toString());
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
		int i = 0;
		for(i = this.pointer; i<this.chunkedtokens.size(); i++){
			token = this.chunkedtokens.get(i);
			/* if one of the tokens match those in the stop list but not in skip list, skip it and get the next token- mohan 10/19/2011*/
			if(token.matches("("+stop+")") && !token.matches("("+skip+")")){
				i=i+1;
				if(i < this.chunkedtokens.size()) token = this.chunkedtokens.get(i);//sometimes barely so
				else{
					this.unassignedmodifier = scs+"]";
					pointer = i;
					return new ChunkEOL(".");
				}
			}
			/*end mohan 10/19/2011*/
			token = token.matches(".*?\\d.*")? NumericalHandler.originalNumForm(token):token;
			if(token.length()==0){
				continue;
			}
			//token = NumericalHandler.originalNumForm(token); //turn -LRB-/-LRB-2
			if(token.matches("^\\w+\\[.*")){ //modifier +  a chunk: m[usually] n[size[{shorter}] constraint[than or {equaling} (phyllaries)]]
				//if(scs.matches("\\w{2,}\\[.*") && token.matches("\\w{2,}\\[.*")){ // scs: position[{adaxial}] token: pubescence[{pubescence~list~glabrous~or~villous}]
				if(scs.matches(".*?\\bo\\[\\w+\\s.*")){
					pointer = i;
					scs = scs.replaceAll("o\\[", "o[(").trim()+")]";
					return new ChunkNonSubjectOrgan("u["+scs+"]");
				}else if(scs.matches(".*?\\w{2,}\\[.*")){
					pointer = i;
					return new ChunkSimpleCharacterState("a["+scs.trim()+"]]"); 
				}else {
					String type = chunkType(i); //changed from pointer to i
					token = this.chunkedtokens.get(i);
					token = token.matches(".*?\\d.*")? NumericalHandler.originalNumForm(token):token;
					scs = scs.trim().length()>0? scs.trim()+"] " : ""; //modifier 
					String start = token.substring(0, token.indexOf("[")+1); //becomes n[m[usually] size[{shorter}] constraint[than or {equaling} (phyllaries)]]
					String end = token.replace(start, "");
					//String abc="";
					//String end = token.replaceFirst(start, abc);
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
						StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
						LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
								System.getProperty("line.separator")+"source:"+this.sentsrc+System.getProperty("line.separator")
								+sw.toString());
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
			
			if(token.matches(".*?"+NumericalHandler.numberpattern+"$") || token.matches("\\d+\\+?") || token.matches("^to~\\d.*")){ //0. sentence ends with a number, the . is not separated by a space
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
			if(role.compareTo("<") !=0 && true){
				if(Utilities.isAdv(token, adverbs, notadverbs)){
					scs = scs.trim().length()>0? scs.trim()+ "] m["+token+" " : "m["+token;
				}else if(token.matches(".*[,;:\\.\\[].*") || token.matches("\\b("+ChunkedSentence.prepositions+"|or|and)\\b") || token.compareTo("-LRB-/-LRB-")==0){
					this.pointer = i;
					if(scs.matches(".*?\\w{2,}\\[.*")){//must have character[
						scs = scs.replaceFirst("^\\]\\s+", "").trim()+"]";
						return new ChunkSimpleCharacterState("a["+scs.trim()+"]");
					}else{
						if(scs.indexOf("m[")>=0){
							this.unassignedmodifier = "{"+scs.trim().replaceAll("(m\\[|\\])", "").replaceAll("\\s+", "} {")+"}";
						}
						if(this.pastpointers.contains(i+"")){
							this.pointer = i+1;
						}else{
							this.pastpointers.add(i+"");
						}
						//if(token.matches("SG.SG")) return new ChunkEOS("");
						return null;
					}
				}else{
					String[] charainfo = Utilities.lookupCharacter(token, conn, characterhash, glosstable, tableprefix);
					if(!founds && charainfo!=null){
						scs = (scs.trim().length()>0? scs.trim()+"] ": "")+charainfo[0]+"["+token+ (charainfo[1].length()>0? "_"+charainfo[1]+"_" : "")+" ";
						founds = true;
						if(i+1==this.chunkedtokens.size()){ //reach the end of chunkedtokens
							scs = scs.replaceFirst("^\\]\\s+", "").trim()+"]";
							this.pointer = i+1;
							return new ChunkSimpleCharacterState("a["+scs.trim()+"]");
						}
					}else if(founds && charainfo!=null && scs.matches(".*?"+charainfo[0]+"\\[.*")){ //coloration coloration: dark blue
						scs += token+" ";
					}else if(founds){
						this.pointer = i;
						scs = scs.replaceFirst("^\\]\\s+", "").trim()+"]";
						return new ChunkSimpleCharacterState("a["+scs.trim()+"]");
					}else if(charainfo==null){
						if(Utilities.isVerb(token, verbs, notverbs) && !founds){//construct ChunkVP or ChunkCHPP
							scs = (scs.trim().length()>0? scs.trim()+"] ": "")+"v["+token+" ";
							//continue searching for either a <> or a r[]
							boolean findc = false; //find a chunk
							boolean findo = false; //find an organ
							boolean findm = false; //find a modifier
							boolean findt = false; //find a text token
							for(int j = i+1; j < this.chunkedtokens.size(); j++){
								String t = this.chunkedtokens.get(j).trim();
								if(t.length() == 0){continue;}
								if(t.startsWith("u[")){//form a vb chunk
									t = t.replaceFirst("^u\\[", "").replaceFirst("\\]$", "");
									String o = t.substring(t.indexOf("o[")).trim();
									t = t.substring(0, t.indexOf("o[")).trim();
									if(t.length()>0){
										String[] states = t.split("\\s+");
										for(int k = 0; k < states.length; k++){
											String[] chinfo = Utilities.lookupCharacter(states[k], conn, characterhash, glosstable, tableprefix);
											if(chinfo!=null){
												scs = (scs.trim().length()>0? scs.trim()+"] ": "")+chinfo[0]+"["+states[k].replaceAll("[{}]", "")+ (chinfo[1].length()>0? "_"+chinfo[1]+"_" : "")+" ";
											}else{
												scs = (scs.trim().length()>0? scs.trim()+"] ": "")+"m["+states[k].replaceAll("[{}]", "")+" ";
											}
										}		
									}
									scs = (scs.trim().length()>0? scs.trim()+"] ": "")+o;
									//if there is position: v[engages] position[basal] o[(half)]
									if(scs.contains("position[")){
										String temp1 = scs.substring(0, scs.indexOf(" position[")).trim(); 
										String temp2 = scs.substring(scs.indexOf(" position[")).replaceAll("(\\w+\\[|\\])", "").trim();
										scs = temp1+" o["+temp2+"]";
									}
									this.pointer = j+1;
									return new ChunkVP("b["+scs+"]"); 
								}
								
								String[] chinfo = t.matches("\\w\\[.*")? null : Utilities.lookupCharacter(t, conn, characterhash, glosstable, tableprefix);
								if((!findc &&!findo) && t.matches("^[rwl]\\[.*")){
									scs = scs.replaceFirst("^\\]\\s+", "").trim()+"] ";
									scs += t;
									findc = true;
								}else if(!findo && t.indexOf("<")>=0){
									scs = (scs.trim().length()>0? scs.trim()+"] ": "")+"o["+t.replace("<", "(").replace(">", ")").replaceAll("[{}]", "")+" ";
									findo = true;
								}else if(!findo && !findc && chinfo!=null){
									scs = (scs.trim().length()>0? scs.trim()+"] ": "")+chinfo[0]+"["+t.replaceAll("[{}]", "")+ (chinfo[1].length()>0? "_"+chinfo[1]+"_" : "")+" ";
								}else if(!findo && !findc && !findm && Utilities.isAdv(t, adverbs, notadverbs)){
									scs = (scs.trim().length()>0? scs.trim()+"] ": "")+"m["+t.replaceAll("[{}]", "")+" ";
									findm = true;
								}else if(!findo && !findc && findm && Utilities.isAdv(t, adverbs, notadverbs)){
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
		if(i==this.chunkedtokens.size()){
			this.pointer = this.chunkedtokens.size();
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
					StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
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
		String v = rest.substring(0, rest.indexOf(']')+1);
		String o = rest.replace(v, "").trim(); //m[a] architecture[surrounding] o[(involucre)]
		String newo = "o[";
		do{
			String t = o.indexOf(' ')>=0? o.substring(0, o.indexOf(' ')) : o;
			o = o.replaceFirst(t.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)").replaceAll("\\{", "\\\\{").replaceAll("\\}", "\\\\}"),"").trim();
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
	 * 
	 *problem : 4-15 <bristles> -LRB-/-LRB- 0 r[p[in] o[{young} growth]] -RRB-/-RRB-
	 */
	private Chunk getNextNumerics() {
		String numerics = "";
		String t = this.chunkedtokens.get(this.pointer);
		t = NumericalHandler.originalNumForm(t).replaceAll("\\?", "");		
		if(t.matches("^to~\\d.*")){ //deal with " to~5 cm", need synchronization with the code in SentenceOrganStateMarkup 10/26/2012
			this.pointer++;
			return new ChunkValue(t.replaceAll("~", " ").trim());
		}
		if(t.matches(".*?("+ChunkedSentence.percentage+")")){
			numerics += t+ " ";
			pointer++;
			return new ChunkValuePercentage(numerics.trim());
		}
		if(t.matches(".*?("+ChunkedSentence.degree+")")){
			numerics += t+ " ";
			pointer++;
			return new ChunkValueDegree(numerics.trim());
		}
		if(t.matches(".*?[()\\[\\]\\-\\–\\d\\.×\\+°²½/¼\\*/%]*?[½/¼\\d][()\\[\\]\\-\\–\\d\\.×\\+°²½/¼\\*/%]*(-\\s*("+ChunkedSentence.counts+")\\b|$)")){ //ends with a number
			numerics += t+ " ";
			pointer++;
			if(pointer==this.chunkedtokens.size()){
				return new ChunkCount(numerics.replaceAll("[{}]", "").trim());
			}
			t = this.chunkedtokens.get(this.pointer);//read next token
			/*if(t.matches("^[{<(]*("+ChunkedSentence.percentage+").*")){
				numerics += t+ " ";
				pointer++;
				return new ChunkValuePercentage(numerics.replaceAll("[{<>}]", "").trim());
			}
			if(t.matches("^[{<(]*("+ChunkedSentence.degree+")\\b.*")){
				numerics += t+ " ";
				pointer++;
				return new ChunkValueDegree(numerics.replaceAll("[{<>}]", "").trim());
			}*/
			if(t.matches("^[{<(]*("+ChunkedSentence.units+")\\b.*?")){
				numerics += t+ " ";
				pointer++;
				adjustPointer4Dot(pointer);//in bhl, 10 cm . long, should skip the ". long" after the unit
				numerics = numerics.replaceAll("[{<>}]", "").trim();
				if(numerics.contains("×")){
					return new ChunkArea(numerics);
				}
				return new ChunkValue(numerics);
			}
			if(t.matches("^[{<(]*("+ChunkedSentence.clusters+")\\b.*?")){
				numerics += t+ " ";
				pointer++;
				numerics = numerics.replaceAll("[{<>}]", "").trim();
				return new ChunkCount(numerics);
			}
			if(t.matches("^[{<(]*("+ChunkedSentence.times+")\\b.*?")){
				numerics += t+ " ";
				pointer++;
				numerics = numerics.replaceAll("[{<>}]", "");
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
			return new ChunkCount(numerics.replaceAll("[{}]", "").trim());
		}
		
		if(t.matches("[wl]\\s*\\W\\s*[wl]")){
			String name = t;
			while(!t.matches(".*?\\d.*")){
				t = this.chunkedtokens.get(++this.pointer);
			}
			this.pointer++;
			ChunkRatio r = new ChunkRatio(NumericalHandler.originalNumForm(t).trim());
			r.setName(name);
			return r;
		}
		return null;
	}
	
	
	/**
	 * needed for cases like "10 cm . long/broad/wide/thick", skip ". "
	 * @param pointer2
	 */
	private void adjustPointer4Dot(int pointer) {
		//boolean iscase = false;
		while(this.chunkedtokens.size()>pointer && this.chunkedtokens.get(pointer).trim().length()==0){
			pointer++;
		}
		if(this.chunkedtokens.size()>pointer && this.chunkedtokens.get(pointer).trim().matches("\\.")){//optional
			this.pointer++;
		}
		
		/*while(this.chunkedtokens.size()>pointer && this.chunkedtokens.get(pointer).trim().length()==0){
			pointer++;
		}
		while(this.chunkedtokens.size()>pointer && this.chunkedtokens.get(pointer).trim().matches("[{(<]?(long|broad|wide|thick)[})>]?")){//required
			pointer++;
			iscase = true;
		}
		if(iscase){
			this.pointer = pointer;
		}*/
	}
	/**
	 * at least 1 u[posterior o[(segment)] ...
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
			if(token.matches("-L[RS]B-/-L[RS]B-")){
				//collect until the matching right brackets are found
				int left = 1;
				String text = token+" "; 				
				do{
					token = this.chunkedtokens.get(++i);
					if(token.matches("-L[RS]B-/-L[RS]B-")) left++;
					if(token.matches("-R[RS]B-/-R[RS]B-")) left--;
					text += token+" ";
				}while(left!=0);
				organ += text;
				continue;
			}
			if(token.startsWith("z[")){
				pointer++;
				return new ChunkOrgan(token);
			}
			//at-{least} should not be matched by preppostions
			if(token.matches(".*?\\b("+ChunkedSentence.prepositions+")[ \\]].*") || token.matches(".*?[,;:\\.].*")){
				break;
			}
			if(found && token.matches("\\b(and|or)\\b")){
				found = false;
			}
			if(found && !token.matches(".*?[>)]\\]*$")){
				pointer = i;
				organ = organ.trim();
				if(organ.matches("^\\w+\\[")){
					organ = organ.replaceAll("(\\w+\\[|\\])", "");
				}
				if(organ.indexOf("u[")>=0){
					organ = organ.replaceFirst("u\\[o\\[", "").replaceFirst("\\]{2,2}$", "");
				}
				organ = organ.replaceAll("[<(]", "(").replaceAll("[>)]", ")").replaceAll("(\\w\\[|\\])", "").trim();
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
			organ = organ.replaceAll("[<(]", "(").replaceAll("[>)]", ")").replaceAll("(\\w\\[|\\])", "").trim();
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
geography scope: g[other chunks]
taxon scope: x[other chunks]
parallelism scope: q[other chunks]

	 */
	private String chunkType(int id) {
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
			//r[p[around] o[10 mm]] should be ChunkValue
			if(token.matches(".* o\\[\\(?[0-9+×x°²½/¼*/%-]+\\)?.*("+ChunkedSentence.units+")\\]+") && !token.contains("p[in]")){
				token = token.replaceFirst("\\[p\\[", "[m[").replaceAll("[or]\\[", "").replaceFirst("\\]+$", "");
				this.chunkedtokens.set(id, token);
				return "ChunkValue";
			}else if(token.matches(".* o\\[\\(?[0-9+×x°²½/¼*/%-]+\\)?\\]+") && !token.matches(".*[×x]\\].*")){//r[p[at] o[30×]] is not a value
				token = token.replaceFirst("\\[p\\[", "[m[").replaceAll("[or]\\[", "").replaceFirst("\\]+$", "");
				this.chunkedtokens.set(id, token);
				return "ChunkValue";
			}else if(token.indexOf("o[")>=0 /*|| token.indexOf("c[")>=0*/){
				//r[p[without] o[or r[p[with] o[{poorly} {developed} {glutinous} ({ridge})]]]] ; 
				token = token.replaceAll("r\\[p\\[of\\]\\]", "of");
				this.chunkedtokens.set(id, token);
				if(token.matches(".*?\\[p\\[\\w+\\] o\\[\\w+ r\\[p\\[.*")){
					Pattern p = Pattern.compile("(.*?\\[p\\[\\w+)(\\] o\\[)(\\w+ )(r\\[p\\[)(.*)");
					Matcher m = p.matcher(token);
					if(m.matches()){
						token = m.group(1)+" "+m.group(3)+m.group(5).replaceFirst("\\]\\]\\s*$", "");
						this.chunkedtokens.set(id, token);
					}					
				}
				return "ChunkPrep";
			}else if(token.equals("r[p[to]]")){
				if(id+1 < this.chunkedtokens.size()){
					token = this.chunkedtokens.get(id+1);
					if(id+1 < this.chunkedtokens.size() && token.startsWith("{")){
						//r[p[to]] {reniform}
						String[] charainfo = Utilities.lookupCharacter(token, conn, characterhash, glosstable, tableprefix);
						if(charainfo!=null){
							this.chunkedtokens.set(id, "a["+charainfo[0]+"[to_"+token.replaceAll("[{}]", "")+ (charainfo[1].length()>0? "_"+charainfo[1]+"_" : "")+"]]");
							this.chunkedtokens.set(id+1, "");
							return "ChunkSimpleCharacterState";
						}
					}
				}
			}else{
				return null;
			}
		}
		if(token.startsWith("t[")){
			//this was for FNAv19, but it seemed all t[ chunks were only generated by composeChunk, bypassing this step. t[ chunks generated by chunking does not seem to need this reformatting.
			//reformat c[] in t[]: c: {loosely} {arachnoid} : should be m[loosely] architecture[arachnoid]
			/*Pattern p = Pattern.compile("(.*?\\b)c\\[([^]].*?)\\](.*)");
			Matcher m = p.matcher(token)
			String reformed = "";
			if(m.matches()){
				reformed += m.group(1);
				String c = reformCharacterState(m.group(2));
				reformed += c+ m.group(3);
			}
			this.chunkedtokens.set(id, reformed);*/
			return "ChunkCHPP"; //character/state-pp
		}
		if(token.startsWith("n[")){//returns three different types of ChunkTHAN
			if(thantype.get(token)!=null) return thantype.get(token);
			Pattern p = Pattern.compile("\\bthan\\b");
			Matcher m = p.matcher(token);
			m.find();
			//String beforethan = token.substring(0, token.indexOf(" than "));
			String beforethan = token.substring(0, m.start()).trim();
			String charword = beforethan.lastIndexOf(' ')>0 ? beforethan.substring(beforethan.lastIndexOf(' ')+1) : beforethan.replaceFirst("n\\[", "");
			String beforechar = beforethan.replace(charword, "").trim().replaceFirst("n\\[", "");
			
			String[] charainfo = null;
			String chara = "";
			if(!charword.matches("\\{?("+ChunkedSentence.more+")\\}?")){
				charainfo = Utilities.lookupCharacter(charword, this.conn, ChunkedSentence.characterhash, glosstable, tableprefix);
				if(charainfo==null && charword.endsWith("er"))
					charainfo = Utilities.lookupCharacter(charword.replaceFirst("er$", ""), this.conn, ChunkedSentence.characterhash, glosstable, tableprefix);
			}
			String afterthan = token.substring(token.indexOf(" than ")+6);
			//Case B
			if(afterthan.matches(".*?\\d.*?\\b("+ChunkedSentence.units+"|long|length|wide|width)\\b.*") || afterthan.matches(".*?\\d\\.\\d.*")){// "n[{longer} than 3 (cm)]" => n[size[{longer} than 3 (cm)]]				
				if(charainfo==null){chara="size";}
				else{chara = charainfo[0];}
				token = "n["+token.replaceFirst("n\\[", chara+"[")+"]";
				this.chunkedtokens.set(id, token);
				thantype.put(token, "ChunkTHAN");
				return "ChunkTHAN"; //character
			}else if(afterthan.matches(".*?\\d.*")){// "n[{longer} than 3 (cm)]" => n[size[{longer} than 3 (cm)]]
				if(charainfo==null){chara="count";}
				else{chara = charainfo[0];}
				token = "n["+token.replaceFirst("n\\[", chara+"[")+"]";
				this.chunkedtokens.set(id, token);
				thantype.put(token, "ChunkTHAN");
				return "ChunkTHAN";
			} else if(afterthan.matches("\\{?half\\}?.*")){// "n[{more} than half]" => n[size[{more} than half]]
				if(charainfo==null){
					chara=CharacterAnnotatorChunked.unassignedcharacter==null? "size": CharacterAnnotatorChunked.unassignedcharacter;
					CharacterAnnotatorChunked.unassignedcharacter = null;
					}
				else{chara = charainfo[0];}
				token = "n["+token.replaceFirst("n\\[", chara+"[")+"]";
				this.chunkedtokens.set(id, token);
				thantype.put(token, "ChunkTHAN");
				return "ChunkTHAN";
			}//Case C
			else if(afterthan.indexOf("(")>=0){ //contains organ
				if(charainfo==null){//is a constraint, lobed n[more than...]
					token = "n["+token.replaceFirst("n\\[", "constraint[")+"]";
					this.chunkedtokens.set(id, token);
					thantype.put(token, "ChunkTHAN");
					return "ChunkTHAN";
				}else{//n[more deeply lobed than...
					token = "n["+(beforechar.length()>0? "m["+beforechar+"] ": "")+charainfo[0]+"["+charword+ (charainfo[1].length()>0? "_"+charainfo[1]+"_" : "")+"] constraint[than "+afterthan+"]";
					this.chunkedtokens.set(id, token);
					thantype.put(token, "ChunkTHAN");
					return "ChunkTHAN";
				}
			}//Case A n[wider than long]
			else{
				//mohan special case if charainfo is null//
			/*	if(charainfo==null)
				{
					charainfo="".split("\\s");
				}*/
				//End mohan case

				if(charainfo!=null){
					token = "n["+token.replaceFirst("n\\[", charainfo[0]+"[")+"]";
					this.chunkedtokens.set(id, token);
					thantype.put(token, "ChunkTHANC");
					return "ChunkTHANC"; //character
				}else{
					//parsing failure
					//charainfo = null cases:
					//{distance} r[p[between] o[1st and 2d (pinnae)]] not or slightly n[more than] r[p[between] o[l[2d and 3d]]] pairs (many occurrence in fnav2)
					//z[(hairs)] more {numerous} n[abaxially than adaxially] : fna v2
					//n[less than their {width} {apart}] (many occurrence fnav2)
					//1/3 n[less than those] r[p[on] o[{upright} (stems)]] :fnav2
					return null;
				}
			}
		}
		if(token.startsWith("w[")){//w[{proximal} to the (florets)] ; or w[to (midvine)]
			//w[{proximal} to -LRB-/-LRB- i . e . , outside of or {abaxial} to -RRB-/-RRB- the (florets)]
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
		if(token.startsWith("u[")){
			return "ChunkNonSubjectOrgan";
		}
		if(token.startsWith("x[")){
			return "ChunkScopeTaxa";
		}
		if(token.startsWith("g[")){
			return "ChunkScopeGeo";
		}
		if(token.startsWith("q[")){
			return "ChunkScopeParallelism";
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
		String first = "";
		String last = "";
		if(charstring.lastIndexOf(' ')>=0){
			last = charstring.substring(charstring.lastIndexOf(' ')).trim();
			first = charstring.replace(last, "").trim();
			result = "m["+first+"] ";
		}else{
			last = charstring.trim();
		}
			
		String[] charainfo = Utilities.lookupCharacter(last, conn, characterhash, glosstable, tableprefix);
		if(charainfo!=null){
			result += charainfo[0]+"["+last+ (charainfo[1].length()>0? "_"+charainfo[1]+"_" : "")+"]";
		}else if(Utilities.isVerb(last, verbs, notverbs)){
			result += "v["+last+"]";
		}
	
		return result.trim();
	}
	
	/**
	 * when parsing fails at certain point, forward the pointer to the next comma
	 */
	public void setPointer2NextComma() {
		for(; this.pointer<this.chunkedtokens.size(); pointer++){
			if(this.chunkedtokens.get(pointer).matches("(,|\\.|;|:)")){
				break;
			}
		}
		
	}

	public String getText() throws Exception{
		if(this.text==null){
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select originalsent from "+this.tableprefix+"_sentence where source ='"+sentsrc+"'");
			rs.next();
			this.text = rs.getString(1);
			if(rs!=null) rs.close();
			if(stmt!=null) stmt.close();
		}
		return this.text;
	}
	
	
	public String getSubjectText(){
		return this.subjecttext;
	}

	private void findSubject(){
		String senttag = null;
		String sentmod = null;
		String text = null;
		//boolean islifestyle = false;//make this a post-process
		Statement stmt = null;
		ResultSet rs = null;
		try{
			stmt = conn.createStatement();
			//ResultSet rs = stmt.executeQuery("select modifier, tag, originalsent from "+this.tableprefix+"_sentence where source ='"+sentsrc+"'");
		    rs = stmt.executeQuery("select modifier, tag, originalsent from "+this.tableprefix+"_sentence where source ='"+sentsrc+"'");
			if(rs.next()){
				senttag = rs.getString(2).trim();
				senttag = senttag.compareTo("general")==0? "whole_organism" : senttag;
				sentmod = rs.getString(1).trim();
				this.originaltext = rs.getString(3); //has to use originalsent, because it is "ditto"-fixed (in SentenceOrganStateMarker.java) and perserve capitalization for measurements markup
				this.text = Utilities.handleBrackets(this.originaltext);
			
			}
			rs = stmt.executeQuery("select rmarkedsent from "+this.tableprefix+"_markedsentence where source ='"+sentsrc+"'");
			if(rs.next()){
				text = rs.getString(1).replaceAll("[{}<>]", "").trim();
			}
			/*rs = stmt.executeQuery("select * from "+this.glosstable+" where category ='life_style' and term like'%"+senttag+"'");
			if(rs.next()){
				islifestyle = true;
			}*/

		}
		catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
			LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
					System.getProperty("line.separator")+"source:"+this.sentsrc+System.getProperty("line.separator")
					+sw.toString());
		}finally{
			try{
				if(rs!=null) rs.close();
				if(stmt!=null) stmt.close();
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+
						System.getProperty("line.separator")+"source:"+this.sentsrc+System.getProperty("line.separator")
						+sw.toString());
			}
		}
		
		if(senttag.compareTo("ignore")!=0){
			//sentence subject
			if(senttag.compareTo("whole_organism")==0 /*|| islifestyle*/){
				this.subjecttext = "(whole_organism)";
			}else if(senttag.compareTo("chromosome")==0){
				this.subjecttext = "(chromosome)";
				skipLead("chromosome".split("\\s"));
			}else if(senttag.compareTo("ditto")!=0 && senttag.length()>0){
				//find the subject segment
				String subject = "";
				String [] tokens = text.split("\\s+");
				if(senttag.indexOf("[")<0){ 
					if(senttag.matches(".*\\b(or|and|plus)\\b.*")){// a , c, and/or b
						int or = senttag.lastIndexOf(" or ");
						int and = senttag.lastIndexOf(" and ");
						int ind = or < and ? and : or;
						int plus = senttag.lastIndexOf(" plus ");
						ind = plus < ind ? ind : plus;
						String seg = senttag.substring(ind).replaceAll("oo", "(oo|ee)").trim();// and/or b
						if(seg.indexOf("(oo|ee)")>=0){
							seg =seg.replaceFirst(".$", "\\\\w+\\\\b");
						}else if(seg.length() < 5){
							seg =seg.replaceFirst("..$", "\\\\w+\\\\b");
						}else{
							seg = seg.replaceFirst("...$", "\\\\w+\\\\b");
						}
						/*if(seg.length() - seg.lastIndexOf(")")-1 >=3){
							seg =seg.replaceFirst("...$", "\\\\w+\\\\b");
						}else{
							seg = seg.replaceFirst("(?<=\\)).*", "\\\\w+\\\\b");
						}*/
						//seg = seg.replaceFirst("(and|or) ", "(and|or|plus|,) .*?");
						seg = seg.replaceFirst("(and|or) ", "(\\\\band\\\\b|\\\\bor\\\\b|\\\\bplus\\\\b|,).*?\\\\b");
						//tag derived from complex text expression: "biennial or short_lived perennial" from "iennials or short-lived , usually monocarpic perennials ,"
						seg = seg.replaceAll("(?<=\\W)\\s+(?=\\W)", ".*?")
						.replaceAll("(?<=\\W)\\s+(?=\\w)", ".*?\\\\b")
						.replaceAll("(?<=\\w)\\s+(?=\\W)", "\\\\b.*?")
						.replaceAll("(?<=\\w)\\s+(?=\\w)", "\\\\b.*?\\\\b");
						Pattern p = Pattern.compile("(^.*?"+seg+")");
						Matcher m = p.matcher(text.replaceAll("\\s*-\\s*", "_"));
						if(m.find()){
							subject = m.group(1);
							subject = subject.replaceAll("\\s+-\\s+", "-");
							skipLead(subject.split("\\s+"));
							String organs = senttag.replaceAll("\\w+\\s+(?!(and |or |plus |$))", "|").replaceAll("\\s*\\|\\s*", "|").replaceAll("(^\\||\\|$)", "").replaceAll("\\|+", "|");//o1|o2
							//turn organ names in subject to singular
							String[] stokens = subject.split("\\s+");
							subject = "";
							for(int i = 0; i < stokens.length; i++){
								String singular = Utilities.toSingular(stokens[i]);
								if(singular.matches("("+organs+")")){
									stokens[i] = singular;
								}
								subject += stokens[i]+" ";
							}
							subject = subject.trim().replaceAll("(?<=\\b("+organs+")\\b) ", ") ").replaceAll(" (?=\\b("+organs+")\\b)", " (").replaceFirst("(?<=\\b("+organs+")\\b)$", ")").replaceFirst("^(?=\\b("+organs+")\\b)", "(").trim();
							subject = subject.replaceAll("(?<=\\w) ", "} ").replaceAll(" (?=\\w)", " {").replaceAll("(?<=\\w)$", "}").replaceAll("^(?=\\w)", "{").replaceAll("[{(]and[)}]", "and").replaceAll("[{(]or[)}]", "or").replaceAll("\\{\\}", "").replaceAll("\\s+", " ").trim();;
							//subject = ("{"+sentmod.replaceAll("[\\[\\]]", "").replaceAll(" ", "} {")+"} ("+senttag.replaceAll("[\\[\\]]", "").replaceAll(" ", ") (")+")").replaceAll("[{(]and[)}]", "and").replaceAll("[{(]or[)}]", "or").replaceAll("\\{\\}", "").replaceAll("\\s+", " ").trim();
							this.subjecttext = subject;
						}	
						
					}else{
						for(int i = 0; i<tokens.length; i++){
							if(Utilities.toSingular(tokens[i]).compareTo(senttag.replaceAll("_", ""))==0){
								
								//subject += tokens[i]+" ";
								subject = subject.replaceAll("\\s+-\\s+", "-");
								subject = "{"+subject.trim().replaceAll("[\\[\\]{}()]", "").replaceAll(" ", "} {")+"}";
								subject = (subject + " ("+tokens[i].replaceAll("[\\[\\]]", "").replaceAll(" ", ") (")+")").replaceAll("[{(]and[)}]", "and").replaceAll("[{(]or[)}]", "or").replaceAll("\\{\\}", "").replaceAll("\\s+", " ").trim();
								//this.subjecttext=subject;
								this.subjecttext = addSentmod(subject, sentmod);
								if(subject.length()>0){
									skipLead(subject.replaceAll("[\\[\\]{}()]", "").split("\\s+"));
									break;
								}
							}else{
								subject += tokens[i]+" ";
							}
						}
					}
				}else if(senttag.indexOf("[")>=0){// must not be of-case
					subject = ("{"+sentmod.replaceAll("[\\[\\]]", "").replaceAll(" ", "} {")+"} ("+senttag.replaceAll("[\\[\\]]", "").replaceAll(" ", ") (")+")").replaceAll("[{(]and[)}]", "and").replaceAll("[{(]or[)}]", "or").replaceAll("\\{\\}", "").replaceAll("\\s+", " ").trim();
					this.subjecttext=subject;
					String mt = (sentmod+" "+senttag).replaceAll("\\[+.+?\\]+", "").replaceAll("\\s+", " ").trim();
					if(mt.length()>0)
						skipLead(mt.split("\\s+"));
				}
				
				/*
				String subject = ("{"+sentmod.replaceAll("[\\[\\]]", "").replaceAll(" ", "} {")+"} ("+senttag.replaceAll("[\\[\\]]", "").replaceAll(" ", ") (")+")").replaceAll("[{(]and[)}]", "and").replaceAll("[{(]or[)}]", "or").replaceAll("\\{\\}", "").replaceAll("\\s+", " ").trim();
				establishSubject(subject, true);
				String mt = "";
				if(of){
					mt = senttag.replaceAll("\\[+.+?\\]+", "").replaceAll("\\s+", " ").trim();
				}else{
					mt = (sentmod+" "+senttag).replaceAll("\\[+.+?\\]+", "").replaceAll("\\s+", " ").trim();
				}
				if(mt.length()>0)
					cs.skipLead(mt.split("\\s+"));*/
			}else if(senttag.compareTo("ditto")==0){
				if(sentsrc.endsWith("0")){
					this.subjecttext ="(whole_organism)";//it is a starting sentence in a treatment, without an explicit subject.
				}else{
					this.subjecttext ="ditto";
					//mohan code :10/28/2011. If the subject is ditto and the first chunk is a preposition chunk make the subject empty so that it can search within the same sentence for the subject.
					int j=0;
					String text1 = "";
					for(j=0;j<this.chunkedtokens.size();j++)
					{
					text1 = "";
					text1 += this.chunkedtokens.get(j);//gets the first token to check if its a preposition
					if(text1.compareTo("")!=0)
					{
						break;
					}
					}if(text1.matches("r\\[p\\[.*\\]")){
						int i=0;
						for(i=0;i<this.chunkedtokens.size();i++)
						{
							String text2="";
							text2+=this.chunkedtokens.get(i);
							if(text2.matches("(\\<.*\\>)"))
							{
								this.subjecttext =null;
								break;
							}
							/*else
							{
								this.subjecttext="ditto";
							}*/
						}
						
					}
					//End of mohan//
				}
			}
		}else{

			if(this.originaltext.matches(".*?([A-Z]{2,})\\s*\\d+.*")){ //this.text must be originalsent where captalization is perserved.

				this.subjecttext = "measurements";
			}else{
				this.subjecttext = "ignore";
			}
		}
	}
	
	

	/**
	 * sent
	 * @param subject: {basal} (blade)
	 * @param sentmod basal [leaf]
	 * @return
	 */
	private String addSentmod(String subject, String sentmod) {
		if(sentmod.indexOf("[")>=0){
			String[] tokens = subject.split("\\s+");
			String substring = "";
			for(int i = 0; i<tokens.length; i++){
				if(!sentmod.matches(".*?\\b"+tokens[i].replaceAll("[{()}]", "")+"\\b.*")){
					substring +=tokens[i]+" ";
				}
			}
			substring = substring.trim();
			substring ="{"+sentmod.replaceAll("[\\[\\]]", "").replaceAll(" ", "} {").replaceAll("[{(]and[)}]", "and").replaceAll("[{(]or[)}]", "or").replaceAll("\\{\\}", "").replaceAll("\\s+", " ")+"} "+substring;
			return substring;
		}
		return subject;
	}
	/**
	 * 
	 * @param begainindex (inclusive)
	 * @param endindex (not include)
	 * @return element in the range
	 */
	public String getText(int begainindex, int endindex) {
		String text = "";
		for(int i = begainindex; i < endindex; i++){
			text += this.chunkedtokens.get(i)+" ";
		}
		return text.replaceAll("\\s+", " ").trim();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	public String getTokenAt(int i) {
		if(i < this.chunkedtokens.size() && i >=0)
			return this.chunkedtokens.get(i);
		return null;
	}
	
	public void resetScopeAttributes(){
		this.scopeattributes = new ArrayList<Attribute>();
	}
	public void addScopeAttributes(Attribute scope){
		this.scopeattributes.add(scope);
	}
	
	public void addScopeAttributesAll(ArrayList<Attribute> scopes){
		for(Attribute scope : scopes)
			this.scopeattributes.add(scope);
	}
	public ArrayList<Attribute> getScopeAttributes(){
		return this.scopeattributes;
	}
	public void setClauseModifierConstraint(String modifier, String constraintId) {
		this.clauseModifierConstraint = modifier;		
		this.clauseModifierContraintId = constraintId;
	}
	public ArrayList<String> getClauseModifierConstraint() {//apply to all characters in this chunkedsentence
		if(this.clauseModifierConstraint!=null){
			ArrayList<String> mc = new ArrayList<String>();
			mc.add(this.clauseModifierConstraint);	
			if(this.clauseModifierContraintId!=null) mc.add(this.clauseModifierContraintId);
			return mc;
		}else{
			return null;
		}
	}
	

	
	
}
