package fna.parsing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.Logger;

public class Utilities {
	private static final Logger LOGGER = Logger.getLogger(Utilities.class);

	public static void resetFolder(File folder, String subfolder) {
		File d = new File(folder, subfolder);
		if(!d.exists()){
			d.mkdir();
		}else{ //empty folder
			Utilities.emptyFolder(d);
		}
	}
	
	public static void emptyFolder(File f){
			File[] fs = f.listFiles();
			for(int i =0; i<fs.length; i++){
				fs[i].delete();
			}
	}
	
	public static void copyFile(String f, File fromfolder, File tofolder){
		try{
			  File f1 = new File(fromfolder, f);
			  File f2 = new File(tofolder, f);
			  InputStream in = new FileInputStream(f1);
			  OutputStream out = new FileOutputStream(f2);

			  byte[] buf = new byte[1024];
			  int len;
			  while ((len = in.read(buf)) > 0){
				  out.write(buf, 0, len);
			  }
			  in.close();
			  out.close();
			  System.out.println("File copied.");
		}
		catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}		
	}
	
}
