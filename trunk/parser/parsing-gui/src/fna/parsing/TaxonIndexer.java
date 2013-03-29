/**
 * $Id$
 */
package fna.parsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.log4j.Logger;

/**
 * To parse the taxon file and build the taxon index
 * 
 * The relationship of the taxon is: F -> (SF) -> G -> (SG) - S
 * 
 * @author chunshui
 */
public class TaxonIndexer implements Serializable {

	private static final long serialVersionUID = -626445898401165211L;

	private static final String TXT_FILE = "TaxaList.txt"; //TODO:configurable

	private static final String BIN_FILE = "TaxonIndexer.bin";
	private static final Logger LOGGER = Logger.getLogger(TaxonIndexer.class);
	
	private String path;
	//Hong: 10/6/08: use arraylist for numbers and names
	//private String[] numbers;
	//private String[] names;
	private ArrayList<String> numberList = new ArrayList<String>();
	private ArrayList<String> nameList = new ArrayList<String>();
	private Hashtable<String, String> allnametokens = new Hashtable<String, String> ();
	/*if TXT_FILE is available, build TaxonIndex. Otherwise create an empty TaxonIndex to be populated by VolumeVerifier*/

	public static void saveUpdated(String path, TaxonIndexer ti) throws ParsingException {
		try {
			File file = new File(path, BIN_FILE);
			ObjectOutput out = new ObjectOutputStream(
					new FileOutputStream(file));
			out.writeObject(ti);
			out.close();
		} catch (IOException e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			throw new ParsingException(
					"Save the updated TaxonIndexer failed.", e);
		}
	}
	
	public static TaxonIndexer loadUpdated(String path) throws ParsingException{
		try {
			File file = new File(path, BIN_FILE);
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(
					file));
			// Deserialize the object
			TaxonIndexer ti = (TaxonIndexer) in.readObject();
			in.close();
			
			return ti;
		} catch (Exception e) {
			//LOGGER.error("Load the updated TaxonIndexer failed.", e);
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			throw new ParsingException(
					"Load the updated TaxonIndexer failed.", e);
		}
	}


	public TaxonIndexer(String path) {
		this.path = path;
	}

	public void build() throws ParsingException {
	
		try {
			File file = new File(path, TXT_FILE);
			if(file.exists()){//otherwise do nothing. 
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = null;
				while ((line = reader.readLine()) != null) {
					String[] fields = line.split("#");
					if (fields.length == 1)
						continue; // empty or illegal line
					String number = buildNumber(fields[1]);
					numberList.add(number);
	
					String name = buildName(fields[2]);
					nameList.add(name);
				}
				reader.close();
			}
		} catch (Exception e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			throw new ParsingException(e);
		}

		/*numbers = new String[numberList.size()];
		numberList.toArray(numbers);

		names = new String[nameList.size()];
		nameList.toArray(names);*/
	}

	public boolean emptyNumbers(){
		return numberList.size() == 0;
	}
	
	public boolean emptyNames(){
		return nameList.size() == 0;
	}
	
	public void addNumber(String number) {		
		numberList.add(number);
	}

	public void addName(String name) {
		name = name.trim();
		nameList.add(name);
		//add to allnametoken hash
		String[] tokens = getTokensFromName(name);
		for(String t: tokens){
			String count = this.allnametokens.get(t);
			if(count==null) this.allnametokens.put(t, "1");
			else this.allnametokens.put(t, ""+(Integer.parseInt(count)+1));
		}
		
	}
	
	private String[] getTokensFromName(String name) {
		String ncp = name;
		if(!name.matches(".*?(var|subsp|sect|subg)\\..*")){
			name = name.replaceFirst("(?<= [a-z]{3,20} )\\(?[A-Z].*", "").trim(); //remove authority, the first Cap after a lower case word
			if(name.length() == ncp.length()){ //authority not removed, e.g. family/tribe/genus name: ABCDE Smith
				name =name.replaceFirst("(?<=[A-Z]{3,20} )[A-Z].*", "");
			}
		}else{
			String[] parts = name.split("(var|subsp|sect|subg)\\."); //authority may appear in part1 and/or part2
			name = "";
			for(String part: parts){
				String cpart = part;
				part = part.replaceFirst("(?<= [a-z]{3,20} )\\(?[A-Z].*", "").trim(); //remove authority, the first Cap after a lower case word
				if(part.length() == cpart.length()){ //authority not removed, e.g. family/tribe/genus name: ABCDE Smith
					part = part.replaceFirst("(?<=[A-Z]{3,20} )[A-Z].*", "");
				}
				name +=part + " ";
			}
		}
		String[] tokens = name.toLowerCase().split("\\s+");
		if(name.length()!=ncp.length()) System.out.println("authority removed:"+ncp.replace(name, ""));
		return tokens;
	}

	public Hashtable<String, String> getAllNameTokens(){
		return this.allnametokens;
	}

	public String getNumber(int index) {
		return numberList.get(index);
	}

	public String getName(int index) {
		return nameList.get(index);
	}

	private String buildNumber(String field) {
		int pos = field.trim().indexOf('=');
		return (pos == -1) ? field : field.substring(pos + 1);
	}

	private String buildName(String field) {
		return field.trim();
	}
}
