/**
 * 
 */
package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 * @author Hong Updates
 * some us/ca_distributions are mistaken as global distribution
 * fix them for JSTOR
 */
public class FNAGlobalDistribution {
	private static Pattern usstates =Pattern.compile(".*?(Ala\\.|Alabama|Alaska|Ariz\\.|Arizona|Ark\\.|Arkansas|Calif\\.|California|Colo\\.|Colorado|Conn\\.|Connecticut|Del\\.|Delaware|D\\.C\\.|District of Columbia|Fla\\.|Florida|Ga\\.|Georgia|Idaho|Ill\\.|Illinois|Ind\\.|Indiana|Iowa|Kans\\.|Kansas|Ky\\.|Kentucky|La\\.|Louisiana|Maine|Maryland|Md\\.|Massachusetts|Mass\\.|Michigan|Mich\\.|Minnesota|Minn\\.|Mississippi|Miss\\.|Missouri|Mo\\.|Montana|Mont\\.|Nebraska|Nebr\\.|Nevada|Nev\\.|New Hampshire|N\\.H\\.|New Jersey|N\\.J\\.|New Mexico|N\\.Mex\\.|New York|N\\.Y\\.|North Carolina|N\\.C\\.|North Dakota|N\\.Dak\\.|Ohio|Oklahoma|Okla\\.|Oregon|Oreg\\.|Pennsylvania|Pa\\.|Rhode Island|R\\.I\\.|South Carolina|S\\.C\\.|South Dakota|S\\.Dak\\.|Tennessee|Tenn\\.|Texas|Tex\\.|Utah|Vermont|Vt\\.|Virginia|Va\\.|Washington|Wash\\.|West Virginia|W\\.Va\\.|Wisconsin|Wis\\.|Wyoming|Wyo\\.)\\.?");	
	private static Pattern caprovinces=Pattern.compile(".*?(Alta\\.|Alberta|B\\.C\\.|British Columbia|Manitoba|Man\\.|New Brunswick|N\\.B\\.|Newfoundland and Labrador|Nfld\\. and Labr|Northwest Territories|N\\.W\\.T\\.|Nova Scotia|N\\.S\\.|Nunavut|Ontario|Ont\\.|Prince Edward Island|P\\.E\\.I\\.|Quebec|Que\\.|Saskatchewan|Sask\\.|Yukon)\\.?");
	private static final Logger LOGGER = Logger.getLogger(FNAGlobalDistribution.class);  


	/**
	 * 
	 */
	public FNAGlobalDistribution() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String xmldir = "C:\\Documents and Settings\\Hong Updates\\Desktop\\FNANameReviewer\\CompleteReviewed\\v23_hong_reviewed_final";		
		try{
			File dir = new File(xmldir);
			File[] files = dir.listFiles();
			int count = 0;
			for(File file: files){
				SAXBuilder builder = new SAXBuilder();
				Document doc = builder.build(file);
				Element treatment = doc.getRootElement();
				List<Element> gdis = treatment.getChildren("global_distribution");
				for(Element dis : gdis){
					String text = dis.getTextTrim();
					Matcher m = usstates.matcher(text);
					if(m.matches()){
						dis.setName("us_distribution");
						System.out.println(text+" is us_distribution ["+file.getName()+"]");
						count++;
						continue;
					}
					if(m.find()){
						System.out.println(text+" check for us_distribution ["+file.getName()+"]");
						continue;
					}
					
					m = caprovinces.matcher(text);
					if(m.matches()){
						dis.setName("ca_distribution");
						System.out.println(text+" is ca_distribution ["+file.getName()+"]");
						count++;
						continue;
					}
					if(m.find()){
						System.out.println(text+" check for ca_distribution ["+file.getName()+"]");
						continue;
					}
				}	
				BufferedOutputStream out = new BufferedOutputStream(
						new FileOutputStream(file));
				XMLOutputter outputter = new XMLOutputter();
				outputter.output(doc, out);
			}
			System.out.println(count+" files fixed");
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(sw.toString());
		}


	}

}
