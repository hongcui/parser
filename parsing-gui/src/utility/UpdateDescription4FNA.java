/**
 * 
 */
package utility;

import java.io.File;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import fna.parsing.ApplicationUtilities;
import fna.parsing.ParsingUtil;
import fna.parsing.Registry;

/**
 * @author Hong Updates
 * The class is used to fix one particular problem:
 * FNA volumes were once incorrectly processed in transformation step. These volumes have good <description> elements, but may miss other elements.
 * These volumes then reprocessed to the transformation step. New files have all good elements except its <description> elements are not parsed.
 * The class uses the good <description> to replace its un-parsed counterpart in the correctly processed files. 
 *
 */
public class UpdateDescription4FNA {
	private String transformed;
	private File[] finalfiles;
	private File outdir;

	/**
	 * 
	 */
	public UpdateDescription4FNA(String finall, String transformed, String output) {
		File finaldir = new File(finall);
		finalfiles = finaldir.listFiles();
		outdir = new File(output);
		this.transformed = transformed;
		if(!outdir.exists()){
			outdir.mkdir();
			System.out.println("mkdir "+output);
		}
		update();
	}
	private void update() {
		for(File finalf : finalfiles){
			String fname = finalf.getName();
			File transformedf = new File(transformed, fname);
			replace(finalf, transformedf, outdir);			
		}
	}
	/**
	 * result, corrected file saved in finalf
	 * @param finalf
	 * @param transformed
	 * @param outdir
	 */
	private void replace(File finalf, File transformedf, File outdir) {
		try{
			SAXBuilder builder = new SAXBuilder();
			Document finaldoc = builder.build(finalf);
			Element root = finaldoc.getRootElement();
			Element description = (Element)XPath.selectSingleNode(root, "//description");
			Document transformeddoc = builder.build(transformedf);
			Element roott = transformeddoc.getRootElement();
			if(description != null){//with description
				description.detach();
				Element descriptiont = (Element)XPath.selectSingleNode(roott, "//description");
				roott.setContent(roott.indexOf(descriptiont), description);
				descriptiont.detach();
			}
			//write roott out
			File result = new File(outdir, finalf.getName());
			roott.detach();
			ParsingUtil.outputXML(roott, result ,null);
			System.out.println(finalf.getName()+ "written!");

		}catch (Exception e){
			e.printStackTrace();
		}
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String finall = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\V19\\target\\final";
		String transformedf = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\V19\\target\\transformed";
		String outdir = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\V19\\target\\finalfix";
		UpdateDescription4FNA update = new UpdateDescription4FNA(finall, transformedf, outdir);
		update.update();
	}

}
