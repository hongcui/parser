/**
 * 
 */
package fna.parsing;

import org.eclipse.swt.widgets.Display;
import org.jdom.Element;

/**
 * @author updates
 *
 */
public class VolumeTransformerFoC extends VolumeTransformer {

	/**
	 * @param listener
	 * @param dataPrefix
	 * @param glosstable
	 * @param display
	 * @throws ParsingException
	 */
	public VolumeTransformerFoC(ProcessListener listener, String dataPrefix,
			String glosstable, Display display) throws ParsingException {
		super(listener, dataPrefix, glosstable, display);
	}

	/**
	 * species and lower ranks get authority parsed here.
	 */
	protected void parseName(String name, String namerank, Element taxid){
		String text = name;
		if(namerank.equals("subgenus_name")&&name.matches(".*?\\b[Ss]ect\\..*")){ //section wrongly marked as subgenus
			namerank = "section_name";
		}
		
		if(namerank.matches("(family|subfamily|tribe|section|genus|subgenus|series)_name")) 
		{
			String rank = namerank.replaceFirst("_name", "");
			text = text.replaceFirst("(Family|Subfam|Tribe|Sect|Genus|Subg|Ser)\\.?", "").trim();
			if(rank.matches("(family|genus)") && text.indexOf(" ")<0){ //single word
				Element newele= new Element(rank+"_name");
				newele.setText(text);
				taxid.addContent(newele);//no need to parse authority again here. Family, genus, species authority already parsed in getNameAuthority
			}else{
				String famauth="";
				String[] Chunks = text.split("-");
				text = Chunks[0];
				String[] family= text.split("\\s");
				Element newele= new Element(rank+"_name");
				newele.setText(family[0]);
				taxid.addContent(newele); 
				for(int k=1;k<family.length;k++)
				{
					famauth+=family[k]+" ";
				}
				famauth=famauth.trim();
				if(famauth.length()!=0)
				{
					Element famat= new Element(rank+"_authority");
					famat.setText(famauth);
					taxid.addContent(famat);
				}
			}
			
		}else if(namerank.equals("species_name")){
			int k;
			String spauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			taxid.addContent(newele);
			Element spele= new Element("species_name");
			spele.setText(var[1]);
			taxid.addContent(spele);
			for(k=2;k<var.length;k++)
			{
					spauth+=var[k]+" ";
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("species_authority");
			spat.setText(spauth);
			taxid.addContent(spat);
			}
		}else if(namerank.equals("subspecies_name")){
			int k;
			String spauth="";
			String varauth="";
			String[] var= text.split("\\s");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			taxid.addContent(newele);
			Element spele= new Element("species_name");
			spele.setText(var[1]);
			taxid.addContent(spele);
			for(k=2;k<var.length;k++)
			{
				if(var[k].matches(".*?\\b[Ss]ubsp\\..*"))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("species_authority");
			spat.setText(spauth);
			taxid.addContent(spat);
			}
			k++;
			Element subfm= new Element("subspecies_name");
			subfm.setText(var[k]);
			taxid.addContent(subfm);
			k++;
			while(k<var.length)
			{
				varauth+=var[k]+" ";
				k++;
			}
			varauth=varauth.trim();
			if(varauth.length()!=0)
			{
			Element subfamat= new Element("subspecies_authority");
			subfamat.setText(varauth);
			taxid.addContent(subfamat);	
			}
		}
		

		else if(namerank.equals("variety_name")){
			int k;
			//hong
			text = text.replaceFirst("(?<=^[A-Z])\\.", ". "); //to insert a space after C.leptosepala 
			//
			String spauth="";
			String varauth="";
			String[] var= text.split("\\s+");
			Element newele= new Element("genus_name");
			newele.setText(var[0]);
			taxid.addContent(newele);
			//if(!var[1].contains("var."))
			//{
			Element spele= new Element("species_name");
			spele.setText(var[1]);
			taxid.addContent(spele);
			//}
			for(k=2;k<var.length;k++)
			{
				if(var[k].matches(".*?\\b[Vv]ar\\..*"))
				{
					break;
				}
				else
				{
					spauth+=var[k]+" ";
				}
			}
			spauth=spauth.trim();
			if(spauth.length()!=0)
			{
			Element spat= new Element("species_authority");
			spat.setText(spauth);
			taxid.addContent(spat);
			}
			k++;
			Element subfm= new Element("variety_name");
			subfm.setText(var[k]);
			taxid.addContent(subfm);
			k++;
			while(k<var.length)
			{
				varauth+=var[k]+" ";
				k++;
			}
			varauth=varauth.trim();
			if(varauth.length()!=0)
			{
			Element subfamat= new Element("variety_authority");
			subfamat.setText(varauth);
			taxid.addContent(subfamat);	
			}
		}
		
	}
	
	/**
	 * the fixBrokenNames function in Verifier works better than this.
	 * @param name string in the original letter case 
	 * @param namerank
	 * @return fixed name
	 */
	/*protected String fixBrokenName(String name, String namerank) {
		String text = name;
		String rank = "";
		String[] tokens = name.split("\\s+");
		if(namerank.matches("(family|tribe|subfamily|genus|subtribe)_name")){//1 all-capital word
			rank = name.replaceFirst("[A-Z]{2,}.*", "");
			tokens = name.replace(rank, "").split("\\s+");
			for(int i = 1; i < tokens.length; i++){
				if(tokens[i].matches("[A-Z]{2,}")){
					tokens[0] = tokens[0]+tokens[i]; //add to the first token
					tokens[i]="";
				}else if(tokens[i].length()==0){}
				else{
					break;
				}
			}
		}else if(namerank.matches("(section|subgenus)_name")){//Astragalus sect. Cytisodes Bunge, Oxytropis subg. Ptiloxytropi s
		    //the part before sect.|subg. (1-word name)
			boolean merge = true;
			int i = 1;
			for(; i < tokens.length; i++){
				if(tokens[i].matches("(sect|subg)\\.")){
					break;
				}else if(merge && tokens[i].matches("[a-z]{2,}")){
					tokens[0] = tokens[0]+tokens[i]; //add to the first token
					tokens[i]="";
				}else if(tokens[i].length()==0){}
				else{
					merge = false;
				}
			}
			 //the part after sect.|subg. (1-word name)
			int j = i+1;
			for(i=j; i < tokens.length; i++){
				if(tokens[i].matches("[a-z]{2,}")){
					tokens[j] = tokens[j]+tokens[i]; //add to the first token
					tokens[i]="";
				}else if(tokens[i].length()==0){}
				else{
					break;
				}
			}			
		}else if(namerank.matches("(species|subspecies|variety)_name")){//multiple words
			//the part before subsp.|var. (2-word name)
			boolean merge = true;
			int i = 1;
			for(; i < tokens.length; i++){
				if(tokens[i].matches("(subsp|var)\\.")){
					break;
				}else if(merge && tokens[i].matches("[a-z]{2,}")){
					if(!isName(tokens[i-1])){
						if(isName(tokens[i-1]+tokens[i])){
							tokens[i-1] += tokens[i];
							tokens[i] ="";
						}else if(i+1 < tokens.length && i >= 2 && (tokens[i+1].matches(".*?[A-Z].*") ||tokens[i].matches("(subsp|var)\\."))){//reach the end of this part and have two words already
							tokens[i-1] += tokens[i];
							tokens[i] ="";
						}
					}
				}else if(tokens[i].length()==0){}
				else{
					merge = false;
				}
			}
			 //the part after (1-word name)
			int j = i+2;
			for(i=j; i < tokens.length; i++){
				if(tokens[i].matches("[a-z]{2,}")){
					tokens[j-1] = tokens[j-1]+tokens[i]; //add to the first token
					tokens[i]="";
				}else if(tokens[i].length()==0){}
				else{
					break;
				}
			}			
		}
		
		name = "";
		for(String t: tokens){
			name += t+" ";
		}
		name = (rank+name).replaceAll("\\s+", " ").trim();
		text = text.replaceAll("\\s+", " ").trim();
		if(text.length() != name.length()){
			System.out.println("fixed broken names (foc) "+text+" to "+name);
		}
		return name;
	}

	private boolean isName(String string) {
		String count = super.allNameTokens.get(string.toLowerCase());
		if(count!=null && Integer.parseInt(count)>2) return true;
		return false;
	}*/

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
