package fna.parsing;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.SWT;
public class ApplicationUtilities {

	/**
	 * @author Partha Pratim Sanyal, Creation date : 09/25/2009
	 */
	private static Shell shell;
	private static final Logger LOGGER = Logger.getLogger(ApplicationUtilities.class);
	private static Properties properties = null;
	private static FileInputStream fstream = null;
	
	static {
		try {
			fstream = new FileInputStream(System.getProperty("user.dir")+"\\application.properties");
		} catch (FileNotFoundException e) {
			LOGGER.error("couldn't open file in ApplicationUtilities:getProperties", e);
		}
	}
	
	private ApplicationUtilities(){}
	
	public static void setLogFilePath() {
		
	}
	
	public static String getProperty(String key) {
	
		if(properties == null) {
			properties = new Properties();
			try {
				properties.load(fstream);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				LOGGER.error("couldn't open file in ApplicationUtilities:getProperty", e);
				e.printStackTrace();
			} 
		}
		return properties.getProperty(key);
	}
	

	public static void showPopUpWindow(String message , String messageHeader, int type) {
		
		try {
			final Display display = Display.getDefault();
			shell = new Shell(display);
			MessageBox messageBox = new MessageBox(shell, type);
			messageBox.setMessage(message);
			messageBox.setText(messageHeader);
			messageBox.open();	 
		} catch (Exception exe) {
			LOGGER.error("couldn't open file in ApplicationUtilities:showPopUpWindow", exe);
			exe.printStackTrace();
		}
		
	}
	
	public static String longestCommonSubstring(String first, String second) {
		 
		 String tmp = "";
		 String max = "";
						
		 for (int i=0; i < first.length(); i++){
			 for (int j=0; j < second.length(); j++){
				 for (int k=1; (k+i) <= first.length() && (k+j) <= second.length(); k++){
										 
					 if (first.substring(i, k+i).equals(second.substring(j, k+j))){
						 tmp = first.substring(i,k+i);
					 }
					 else{
						 if (tmp.length() > max.length())
							 max = tmp;
						 tmp = "";
					 }
				 }
					 if (tmp.length() > max.length())
							 max = tmp;
					 tmp = "";
			 }
		 }
				
		 return max;        
			    
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String type = getProperty("popup.header.error");
		String message = getProperty("popup.error.msg");
		showPopUpWindow(message, type, SWT.ICON_ERROR);
	
	}
	

}
