/**
 * $Id$
 */
package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * @author chunshui
 */
public class ParsingUtil {
	
	static XMLOutputter outputter;
	static Document doc;
	static BufferedOutputStream out;
	private static final Logger LOGGER = Logger.getLogger(ParsingUtil.class);
	public static void outputXML(Element treatment, File file, Comment comment) {
		try {
			outputter = new XMLOutputter(Format.getPrettyFormat());
			doc = new Document(treatment);
			// File file = new File(path, dest + "/" + count + ".xml");
			out = new BufferedOutputStream(
					new FileOutputStream(file));
			
			outputter.output(doc, out);
			if(comment!=null) outputter.output(comment, out);
			out.close(); // don't forget to close the output stream!!!
			
			// generate the information to the listener (gui)
			// listener.info(String.valueOf(count), "", file.getPath());
		} catch (IOException e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			throw new ParsingException(e);
		}
	}
}
