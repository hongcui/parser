package fna.parsing;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import java.lang.*;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;

import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;
public class ApplicationUtilities {

	/**
	 * @author Partha Pratim Sanyal, Creation date : 09/25/2009
	 */
	private static Shell shell;
	private static final Logger LOGGER = Logger.getLogger(ApplicationUtilities.class);
	private static Properties properties = null;
	private static Properties notepadproperties = null;
	//private static FileInputStream fstream = null;
	private static InputStream fstream = null; 
	private static InputStream notepadstream = null;
	
	static {
		try {
						
			/*fstream = new FileInputStream(System.getProperty("user.dir")
					+"\\application.properties");*/
					
		fstream = ApplicationUtilities.class.getClassLoader().getResourceAsStream("application.properties");
		notepadstream = ApplicationUtilities.class.getClassLoader().getResourceAsStream("notepad.properties");
			
		} //catch (FileNotFoundException e) {
		catch (Exception e) {
			LOGGER.error("couldn't open file in ApplicationUtilities:getProperties", e);
		}
	}
	
	private ApplicationUtilities(){}
	
	public static void setLogFilePath() throws Exception {
		
		//FileInputStream fstream = null;
		InputStream fstream= null;
		FileWriter fwriter = null;
		BufferedWriter out = null;
		
		try {
			//System.out.println("hello"+getProperty("LOG.FILE.LOCATION"));
			//String logProperties = System.getProperty("user.dir")+ getProperty("LOG.FILE.LOCATION");
			String logProperties = getProperty("LOG.FILE.LOCATION").replace("\\", "");
			//fstream = new FileInputStream(logProperties);
			fstream = ApplicationUtilities.class.getClassLoader().getResourceAsStream(logProperties);
			Properties properties = new Properties();
			properties.load(fstream);
			String logFilePath = properties.getProperty("log4j.appender.ROOT.File");
			/* Check if log path is already set*/
			if (logFilePath == null) {
				fwriter = new FileWriter(logProperties ,true);
		        out = new BufferedWriter(fwriter);
		        out.newLine();
		        logFilePath = getProperty("LOG.APPENDER") + System.getProperty("user.dir") 
		        	+ getProperty("LOG");
		        logFilePath = logFilePath.trim();
		        logFilePath = logFilePath.replaceAll("\\\\", "\\\\\\\\");
		        out.write(logFilePath);
				/* Show log path setting message */
				showPopUpWindow(getProperty("popup.info.logpath") + 
						System.getProperty("user.dir") + getProperty("LOG"), 
						getProperty("popup.header.info"), SWT.ICON_INFORMATION);
				out.flush();
				System.exit(0);
			}			
		} catch(Exception e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		} finally {
			if (out != null) {
				out.close();
			}
			
			if (fwriter != null){
				fwriter.close();
			}
			
			if (fstream != null){
				fstream.close();
			}			
		}		
	}
	
	public static String getProperty(String key) {
	
		if(properties == null) {
			properties = new Properties();
			notepadproperties = new Properties();
			try {
				
				notepadproperties.load(notepadstream);
				properties.load(fstream);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				LOGGER.error("couldn't open file in ApplicationUtilities:getProperty", e);
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			} 
		}
		if(key.compareTo("notepad")==0) return notepadproperties.getProperty("notepad");
		return properties.getProperty(key);
	}
	

	public static int showPopUpWindow(String message , String messageHeader, int type) {
		
		int returnVal = 0;
		try {
			final Display display = Display.getDefault();
			shell = new Shell(display);
			MessageBox messageBox = new MessageBox(shell, type);
			messageBox.setMessage(message);
			messageBox.setText(messageHeader);
			returnVal = messageBox.open();	 
		} catch (Exception e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
		return returnVal;
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

	public static void showProgressPopup(ProgressBar popupBar) {
		// TODO Auto-generated method stub
		
		try {/*
			final Display display = Display.getDefault();
			shell = new Shell(display);
			final Composite composite_1 = new Composite(shell, SWT.NONE);
			shell.dispose();			
			popupBar = new ProgressBar(composite_1, SWT.NONE);
			popupBar.setVisible(true);
			popupBar.setBounds(10, 40, 100, 17);
			ProcessListener process= new ProcessListener( popupBar, display);
			
			for(int i=0;i<100;i++)
			{
				process.progress(i);
			}
			
		*/}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
	}

	public static void showPopUpWindowTab5(String message, String messageHeader, int type, TabFolder tabFolder) {
		int returnVal = 0;
		try {
			final Display display = Display.getDefault();
			shell = new Shell(display);
			MessageBox messageBox = new MessageBox(shell, type);
			messageBox.setMessage(message);
			messageBox.setText(messageHeader);
			returnVal = messageBox.open();	 
			//System.out.println("-----------"+returnVal);
			if(returnVal==32)
			{
				System.out.println("currently on folder:"+tabFolder.getSelectionIndex());
				tabFolder.setSelection(tabFolder.getSelectionIndex()+1);
				tabFolder.setFocus();
			}
		} catch (Exception e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
		
		
	}
	

}
