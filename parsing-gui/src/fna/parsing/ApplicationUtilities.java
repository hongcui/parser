package fna.parsing;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
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
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String type = getProperty("popup.header.error");
		String message = getProperty("popup.error.msg");
		showPopUpWindow(message, type, SWT.ICON_ERROR);
	
	}
	

}
