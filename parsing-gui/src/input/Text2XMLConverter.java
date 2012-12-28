/**
 * 
 */
package input;

/**
 * @author sonaliranade
 *
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class Text2XMLConverter {
	
	String inputfilepath; 
	String outputdir; 
	String dataprovider;
	String encoding;
	
	public Text2XMLConverter(String inputfilepath, String outputdir, String dataprovider, String encoding){
		this.inputfilepath = inputfilepath;
		this.outputdir = outputdir;
		this.dataprovider = dataprovider;
		this.encoding = encoding;
	}
	
	public void convert() throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(inputfilepath));
		
		
		Document document = new Document();
		Element root = new Element("treatment");
		root.setAttribute(new Attribute("dataprovider", this.dataprovider));
		String line = in.readLine().trim();
		int count = 0;
		String name = null;
		boolean added = false;
		boolean endoffile = false;
		while(line !=null) {
			do{
				/* System.out.println(line); */	
				if(line.endsWith(":")) line = line+ " ";
				String[] parts = null;
				String[] p1 = null;
				String[] nameA = null;
				if(line.startsWith("Taxon:") ||line.startsWith("Strain:") ||line.startsWith("Reference:") 
						||line.startsWith("Text:") ||line.startsWith("File:") || line.startsWith("Notes:") ){
					parts = line.split(":", 2);
				}else{
					parts = new String[1];
					parts[0] = line;
				}
				if(parts.length==1){//add text to the last "description", assuming 
					//Element desc = root.getChild("description");
					//String newdesc = desc.getTextNormalize() + System.getProperty("line.separator") + parts[0];
					//desc.setText(newdesc);
					if(parts[0].trim().length()>0) root.addContent(new Element("description").addContent(parts[0]));
				}else{
					String part1 = parts[0].trim();
					String part2 = parts[1].trim();
					part1 = part1.toLowerCase();
					
					if (part1.compareTo("text")==0) {
						part1 = "description";
					}
					
					if (part1.compareTo("taxon")==0){
						part1 = "taxon_name";
						if(part2.contains("#")){
							p1 = part2.split("#",2);
							String pp1 = p1[0].trim();
							String pp2 = p1[1].trim();
							String part11 = "synonym";
							pp1 = pp1.replaceAll("\\(", "");
							pp1 = pp1.replaceAll("\\)", "");
							nameA = pp1.split ("\\s+");
							if (nameA[1]==null){
								name = nameA[0];
							}else{
								name = nameA[0]+"_"+nameA[1];
							}
														
							root.addContent(new Element(part1).addContent(pp1));
							root.addContent(new Element(part11).addContent(pp2));
						}else{
							part2 = part2.replaceAll("\\(", "");
							part2 = part2.replaceAll("\\)", "");
							nameA = part2.split ("\\s+");
							if (nameA[1]==null){
								name = nameA[0];
							}else{
								name = nameA[0]+"_"+nameA[1];
							}
														
							System.out.println (part1);
							System.out.println (part2); 
							root.addContent(new Element(part1).addContent(part2));
						}
					}
					else{
						System.out.println (part1);
						System.out.println (part2);  
						root.addContent(new Element(part1).addContent(part2));
					    }
					
				}
				added = true;
				line = in.readLine();
				if(line==null){endoffile=true; break;}
				else line=line.trim();
			}while(!line.startsWith("Taxon:"));
			//the blank line
			if (added) {
				document.addContent(root);
				count++;
				output2file(document, new File(outputdir, name+count+".xml"));
				document = new Document();
			}
			if (!endoffile) {
				document = new Document();
				root = new Element("treatment");
				root.setAttribute(new Attribute("dataprovider", this.dataprovider));
				//line = in.readLine().trim();
			}
		}
		
		/* root.addContent(new Element("taxon").addContent("this is taxon"));
		root.addContent(new Element("strain").addContent("this is strain"));
		root.addContent(new Element("reference")
				.addContent("this is reference"));
		root.addContent(new Element("reference")
				.addContent("this is reference"));
		root.addContent(new Element("sourcefile")
				.addContent("this is source file"));
		root.addContent(new Element("description")
				.addContent("this is description"));
		}*/

		
	}

	private void output2file(Document document, File output) {
		try {
			FileWriter writer = new FileWriter(output);
			XMLOutputter outputter = new XMLOutputter();

			// Set the XLMOutputter to pretty formatter. This formatter
			// use the TextMode.TRIM, which mean it will remove the
			// trailing white-spaces of both side (left and right)

			outputter.setFormat(Format.getPrettyFormat().setEncoding(encoding));

			// Write the document to a file and also display it on the
			// screen through System.out.

			outputter.output(document, writer);
			outputter.output(document, System.out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] argv){
		//String input = "C:/Users/sonaliranade/Desktop/Diatoms/Input/Diatom_AvotalDataFormat4Providers.txt";
		//String output="C:/Users/sonaliranade/Desktop/Diatoms/Output";
		//String dataprovider = "Anna Yu";
		
		String input = "E:/Perelleschus/Input/PerelleschusConcepts-ETC2012.txt";
		String output = "E:/Perelleschus/Output";
		String dataprovider = "Nico Franz";
		
		String encoding ="CP1252";
		//String encoding ="UTF-8";
		Text2XMLConverter txc = new Text2XMLConverter(input, output, dataprovider, encoding);
		try{	
			txc.convert();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
