package fna.parsing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;

import fna.db.ConfigurationDbAccessor;
import org.eclipse.swt.widgets.Text;

public class Type1Document {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new Type1Document().showType1Document();

	}
	private ConfigurationDbAccessor configDb = new ConfigurationDbAccessor();
	private static final Logger LOGGER = Logger.getLogger(Type1Document.class);
	private static final HashMap <Integer, Text> textFields = new HashMap<Integer, Text> ();
	private static final HashMap <Integer, Combo> comboFields = new HashMap<Integer, Combo> ();
	private static int count = 1;
	private static Group group = null;
	private static Shell shell = null;
	private static Button saveButton = null;
	private static Button addRowButton = null;
	private static String [] tagnames = null;
	
	public void showType1Document() {
		final Display display = Display.getDefault();
		
		shell = new Shell();
		shell.setSize(520, 178);
		shell.setText("Type 1");
		shell.setLayout(new RowLayout(SWT.HORIZONTAL));
		
		group = new Group(shell, SWT.NONE);
		group.setLayoutData(new RowData(490, 116));
		group.setBounds(10, 10, 500, 115);
		
		Combo combo_1 = new Combo(group, SWT.NONE);
		combo_1.setBounds(292, 52, 172, 23);
				
		//retrieve the data from db here.		
		List <String> tags = new ArrayList <String>();
		try {
			configDb.retrieveTagDetails(tags);
		} catch(Exception exe) {
			LOGGER.error("Error retrieving tags", exe);
		}
		tagnames = new String[tags.size()];
		int loopCount = 0;
		for (String s : tags) {
			tagnames [loopCount] = s;
			loopCount++;
		}
		combo_1.setItems(tagnames);
		comboFields.put(new Integer(1), combo_1);
		
		
		saveButton = new Button(group, SWT.NONE);
		saveButton.setBounds(389, 99, 75, 25);
		saveButton.setText("Save");
		saveButton.addMouseListener(new MouseListener(){
			public void mouseUp(MouseEvent mEvent){
				String message = ApplicationUtilities.getProperty("popup.info.savetype1.tag") ;
				
				try {
					message += createStyleMappingFile();
					configDb.saveSemanticTagDetails(comboFields);
				} catch (IOException exe) {
					LOGGER.error("Error saving to file in Type1doc", exe);
				} catch (SQLException sqlexp) {
					LOGGER.error("Error saving tags to database in Type1doc", sqlexp);
				}
				ApplicationUtilities.showPopUpWindow(message , "Information", SWT.ICON_INFORMATION);
				shell.dispose();
			}
			public void mouseDown(MouseEvent mEvent) { }
			public void mouseDoubleClick(MouseEvent mEvent) {}

		});
		
		Label label = new Label(group, SWT.NONE);
		label.setBounds(10, 24, 155, 15);
		label.setText("Word Document Style:");
		
		Label label_1 = new Label(group, SWT.NONE);
		label_1.setBounds(292, 24, 172, 15);
		label_1.setText("Semantic Tags:");
		
		Text text = new Text(group, SWT.BORDER);
		text.setBounds(10, 52, 155, 23);
		textFields.put(new Integer(1), text);
		count += 1;
		
		addRowButton = new Button(group, SWT.NONE);
		addRowButton.setBounds(296, 99, 75, 25);
		addRowButton.setText("Add a row");
		addRowButton.addMouseListener(new MouseListener(){
			public void mouseUp(MouseEvent mEvent){
				//add a row
				addRow();
			}
			
			public void mouseDown(MouseEvent mEvent) { }
			public void mouseDoubleClick(MouseEvent mEvent) {}
		});
		
		
		/*Load from style-mapping properties here */
		try {
			loadStyleMappinFile();
		} catch (Exception exe) {
			exe.printStackTrace();
			LOGGER.error("Error loading Tag styles", exe);
		}
		
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}
	
	private void addRow() {
		Point p = shell.getSize();
		p.y += 30;
		shell.setSize(p);
		
		// group height				
		RowData rowdata = (RowData)group.getLayoutData();
		rowdata.height += 30;
		group.setLayoutData(new RowData(rowdata.width, rowdata.height));
        Rectangle rect = group.getBounds();
        rect.height += 30;
        group.setBounds(rect);

        final Integer key = new Integer(count);
        final Integer previousKey = new Integer(key.intValue()-1);
        
		Combo tempCombo = new Combo(group, SWT.NONE);	
		tempCombo.setItems(tagnames);				
		rect = comboFields.get(previousKey).getBounds();
		rect.y += 30;
		tempCombo.setBounds(rect);
		comboFields.put(key, tempCombo); 
		
	    
	    Text tempText = new Text(group, SWT.BORDER);
	    rect = textFields.get(previousKey).getBounds();
	    rect.y += 30;
	    tempText.setBounds(rect);
	    textFields.put(key, tempText);	
		
		rect = saveButton.getBounds();
		rect.y += 30;				
		saveButton.setBounds(rect);
		
		rect = addRowButton.getBounds();
		rect.y += 30;
		addRowButton.setBounds(rect);				
		
		count += 1;
	}
	
	private String createStyleMappingFile() throws IOException{
		
		count = textFields.size();
		
		String pathName = getStyleMappingFile(); 
		BufferedWriter out = new BufferedWriter(new FileWriter(pathName));
		for(int i=1; i<= count; i++) {
			Integer key = new Integer(i);
			Text text = textFields.get(key);
			String text1 = text.getText();
			
			Combo combo = comboFields.get(key);
			String comboText = combo.getText();
			
			if(!text1.equals("") && !comboText.equals("")) {
				String line = text1 + "=" + comboText;
				out.write(line);
				out.newLine();
				out.flush();
			}

		}
		out.close();
		return pathName;
	}
	
	private String getStyleMappingFile() throws IOException {
		File project = new File("project.conf");
		BufferedReader in = new BufferedReader(new FileReader(project));
		String conf = in.readLine();
		conf = conf == null ? "C:\\" : conf;		
		String pathName = conf + ApplicationUtilities.getProperty("style.mapping"); 
		return pathName;
	}
	
	private void loadStyleMappinFile() throws IOException {		
	
		String pathName = getStyleMappingFile(); 
		
		File styleMapping = new File(pathName);
		if(styleMapping.isFile() && styleMapping.exists()) {
			BufferedReader in = new BufferedReader(new FileReader(styleMapping));
			String line = "";
			while ((line = in.readLine())!= null){
				String style = line.substring(0, line.indexOf("="));
				String tag = line.substring(line.indexOf("=")+1);
				Integer key = new Integer(count-1);
				Text text = textFields.get(key);
				Combo combo = comboFields.get(key);
				
				combo.setText(tag);
				text.setText(style);
				
				/* Add another row here */
				addRow();
				
			}
		}
	}

}
