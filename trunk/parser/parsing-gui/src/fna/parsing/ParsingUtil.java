/**
 * $Id$
 */
package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * @author chunshui
 */
public class ParsingUtil {
	
	public static void outputXML(Element treatment, File file) {
		try {
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			Document doc = new Document(treatment);
			// File file = new File(path, dest + "/" + count + ".xml");
			BufferedOutputStream out = new BufferedOutputStream(
					new FileOutputStream(file));

			outputter.output(doc, out);
			out.close(); // don't forget to close the output stream!!!
			
			// generate the information to the listener (gui)
			// listener.info(String.valueOf(count), "", file.getPath());
		} catch (IOException e) {
			throw new ParsingException(e);
		}
	}
}
