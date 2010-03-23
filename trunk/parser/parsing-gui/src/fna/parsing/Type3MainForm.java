/**
 * $Id$
 */
package fna.parsing;
//
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.swtdesigner.SWTResourceManager;

import fna.db.MainFormDbAccessor;
import fna.parsing.ParsingException;
import fna.parsing.ProcessListener;
import fna.parsing.Registry;
import fna.parsing.VolumeExtractor;
import fna.parsing.VolumeFinalizer;
import fna.parsing.VolumeMarkup;
import fna.parsing.VolumeTransformer;
import fna.parsing.VolumeVerifier;
import fna.parsing.character.LearnedTermsReport;
/**
 * @author chunshui
 */


public class Type3MainForm {

	static {
		//Set the Log File path
		try {
			ApplicationUtilities.setLogFilePath();
		} catch (Exception exe) {
			exe.printStackTrace();
		}

	}
	private Combo combo;
	
	private Combo modifierListCombo;
	private Table finalizerTable;
	private Table markupTable;
	private Table tagTable;
	private Text targetText;
	private Text sourceText;
	private Text configurationText;
	private TabItem generalTabItem;
	private StyledText contextStyledText;
	private ProgressBar markupProgressBar;
	private ProgressBar finalizerProgressBar;
	private Combo tagListCombo;
	public static Combo dataPrefixCombo;
	private StyledText glossaryStyledText;
	public Shell shell;
	/*In Unknown removal this variable is used to remember the last tab selected*/
	private static int hashCodeOfItem = 0;
	private boolean [] statusOfMarkUp = {false, false, false, false, false, false, false, false};
	private static boolean saveFlag = false;
	private static final Logger LOGGER = Logger.getLogger(Type3MainForm.class);
		/**
	 * Launch the application
	 * @param args
	 */
	
	private MainFormDbAccessor mainDb = new MainFormDbAccessor();
	public static Text markUpPerlLog;
	
	public static void main(String[] args) {
		try {
/*			if (args != null && args[0] != null) {
				typeOfmarkup = args[0];
			}*/
			Type3MainForm window = new Type3MainForm();
			window.open();
		} catch (Exception e) {
			LOGGER.error("Error Launching application", e);
			e.printStackTrace();
		}
	}
	
	/**
	 * Open the window
	 */
	public void open() throws Exception {
		final Display display = Display.getDefault();
		createContents(display);
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		if (shell.isDisposed()) {
			System.exit(0);
		}
	}

	/**
	 * Create contents of the window
	 */
	protected void createContents(Display display) throws Exception{
		shell = new Shell(display);
		shell.setSize(852, 600);
		shell.setLocation(200, 100);
		shell.setText(ApplicationUtilities.getProperty("application.name"));

		final TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
		tabFolder.setBounds(10, 10, 803, 444);
		tabFolder.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			public void widgetSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
				// chk if values were loaded
				StringBuffer messageText = new StringBuffer();
				String tabName = arg0.item.toString();
				tabName = tabName.substring(tabName.indexOf("{")+1, tabName.indexOf("}"));
	
				/* Logic for tab access goes here*/
				
				/* 
				 * if status is true  - u can go to the next tab, else dont even think! */
				// For general tab
				if (configurationText == null ) return;
				if(tabName.indexOf(
						ApplicationUtilities.getProperty("tab.one.name")) == -1 && !statusOfMarkUp[0] 
						      && !saveFlag)  {
					// inform the user that he needs to load the information for starting mark up
					// focus back to the general tab
					checkFields(messageText, tabFolder);
					return;
				}
				if (combo.getText().equals("")) {
					checkFields(messageText, tabFolder);
					return;
				}
				//show pop up to inform the user
				if(statusOfMarkUp[0]) {
					if(!saveFlag) {
						ApplicationUtilities.showPopUpWindow(
								ApplicationUtilities.getProperty("popup.info.prefix.save"), 
								ApplicationUtilities.getProperty("popup.header.info"), SWT.ICON_INFORMATION);
						saveFlag = true;
					}

					try {
						mainDb.savePrefixData(dataPrefixCombo.getText().trim());
					} catch (Exception exe) {
						LOGGER.error("Error saving dataprefix", exe);
						exe.printStackTrace();
					}
				 }
				
				// Mark Up
				if (!statusOfMarkUp[4]) {
					if(!tabName.equals(ApplicationUtilities.getProperty("tab.one.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.five.name"))) {
						ApplicationUtilities.showPopUpWindow(								
								ApplicationUtilities.getProperty("popup.error.tab")+ " " +
								ApplicationUtilities.getProperty("tab.five.name")
								.substring(0, ApplicationUtilities.getProperty("tab.five.name").indexOf(" ")), 
								ApplicationUtilities.getProperty("popup.header.error"),
								SWT.ICON_ERROR);
						tabFolder.setSelection(1);
						tabFolder.setFocus();
						return;
					}			

				}

				//Unknown Removal
				if (!statusOfMarkUp[5]) {
					if(!tabName.equals(ApplicationUtilities.getProperty("tab.one.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.five.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.six.name"))) {
						ApplicationUtilities.showPopUpWindow(								
								ApplicationUtilities.getProperty("popup.error.tab")+ " " +
								ApplicationUtilities.getProperty("tab.six.name"), 
								ApplicationUtilities.getProperty("popup.header.error"),
								SWT.ICON_ERROR);
						tabFolder.setSelection(2);
						//System.out.println(tabFolder.);
						tabFolder.setFocus();
						return;
					}

				}
				//Finalizer
				if (!statusOfMarkUp[6]) {
					if(!tabName.equals(ApplicationUtilities.getProperty("tab.one.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.five.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.six.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.seven.name"))) {
						ApplicationUtilities.showPopUpWindow(								
								ApplicationUtilities.getProperty("popup.error.tab")+ " " +
								ApplicationUtilities.getProperty("tab.seven.name"), 
								ApplicationUtilities.getProperty("popup.header.error"),
								SWT.ICON_ERROR);
						tabFolder.setSelection(3);
						tabFolder.setFocus();
						return;
					}

				}							

			}
			
		});

		generalTabItem = new TabItem(tabFolder, SWT.NONE);
		generalTabItem.setText(ApplicationUtilities.getProperty("tab.one.name"));

		final Composite composite = new Composite(tabFolder, SWT.NONE);
		generalTabItem.setControl(composite);

		final Group configurationDirectoryGroup = new Group(composite, SWT.NONE);
		configurationDirectoryGroup.setText(ApplicationUtilities.getProperty("config"));
		configurationDirectoryGroup.setBounds(10, 10, 763, 70);

		configurationText = new Text(configurationDirectoryGroup, SWT.BORDER);
		configurationText.setEditable(false);
		configurationText.setBounds(10, 25, 618, 23);

		final Button browseConfigurationButton = new Button(configurationDirectoryGroup, SWT.NONE);
		browseConfigurationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				browseConfigurationDirectory(); // browse the configuration directory
			}
		});
		browseConfigurationButton.setText(ApplicationUtilities.getProperty("browse"));
		browseConfigurationButton.setBounds(653, 24, 100, 23);

		final Group configurationDirectoryGroup_1 = new Group(composite, SWT.NONE);
		configurationDirectoryGroup_1.setBounds(10, 86, 763, 70);
		configurationDirectoryGroup_1.setText(ApplicationUtilities.getProperty("source"));

		sourceText = new Text(configurationDirectoryGroup_1, SWT.BORDER);
		sourceText.setEditable(false);
		sourceText.setBounds(10, 25, 618, 23);

		final Button browseSourceButton = new Button(configurationDirectoryGroup_1, SWT.NONE);
		browseSourceButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				browseSourceDirectory(); // browse the source directory
			}
		});
		browseSourceButton.setBounds(653, 24, 100, 23);
		browseSourceButton.setText(ApplicationUtilities.getProperty("browse"));

		final Group configurationDirectoryGroup_1_1 = new Group(composite, SWT.NONE);
		configurationDirectoryGroup_1_1.setBounds(10, 162, 763, 70);
		configurationDirectoryGroup_1_1.setText(
				ApplicationUtilities.getProperty("target"));

		targetText = new Text(configurationDirectoryGroup_1_1, SWT.BORDER);
		targetText.setEditable(false);
		targetText.setBounds(10, 25, 618, 23);

		final Button browseTargetButton = new Button(configurationDirectoryGroup_1_1, SWT.NONE);
		browseTargetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				browseTargetDirectory(); // browse the target directory
			}
		});
		browseTargetButton.setBounds(653, 24, 100, 23);
		browseTargetButton.setText(ApplicationUtilities.getProperty("browse"));

		final Button loadProjectButton = new Button(composite, SWT.NONE);
		loadProjectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e){
				loadProject();
				// code for setting the text of the combo to the last accessed goes here - Partha
				try {
					Type3MainForm.dataPrefixCombo.setText(mainDb.getLastAccessedDataSet());
					mainDb.loadStatusOfMarkUp(statusOfMarkUp, combo.getText());
					//mainDb.saveStatus("general", combo.getText(), true);
					statusOfMarkUp[0] = true;
					
				} catch (Exception ex) {
					LOGGER.error("Error Setting focus", ex);
					ex.printStackTrace();
				} 
				
			}
		});
		loadProjectButton.setBounds(548, 385, 100, 23);
		loadProjectButton.setText(ApplicationUtilities.getProperty("load"));

		final Button saveProjectButton = new Button(composite, SWT.NONE);
		saveProjectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (checkFields(new StringBuffer(), tabFolder)) {
					return;
				}
				saveProject(); 
				saveFlag = false;
				try {
					mainDb.savePrefixData(dataPrefixCombo.getText().trim());
					mainDb.loadStatusOfMarkUp(statusOfMarkUp, combo.getText());
				} catch (Exception exe) {
					exe.printStackTrace();
					LOGGER.error("Error saving dataprefix", exe);
				}
				String messageHeader = ApplicationUtilities.getProperty("popup.header.info");
				String message = ApplicationUtilities.getProperty("popup.info.saved");				
				ApplicationUtilities.showPopUpWindow(message, messageHeader, SWT.ICON_INFORMATION);
				
			}
		});
		saveProjectButton.setBounds(673, 385, 100, 23);
		saveProjectButton.setText(ApplicationUtilities.getProperty("save"));

		final Group configurationDirectoryGroup_1_1_1 = new Group(composite, SWT.NONE);
		configurationDirectoryGroup_1_1_1.setBounds(10, 255, 763, 70);
		configurationDirectoryGroup_1_1_1.setText(
				ApplicationUtilities.getProperty("dataset"));

		combo = new Combo(configurationDirectoryGroup_1_1_1, SWT.NONE);
		combo.setToolTipText(ApplicationUtilities.getProperty("application.dataset.instruction"));
		dataPrefixCombo = combo;
		// get value from the project conf and set it here
		List <String> datasetPrefixes = new ArrayList <String> (); 
		mainDb.datasetPrefixRetriever(datasetPrefixes);
		String [] prefixes = new String [datasetPrefixes.size()];
		int loopCount = 0;
		for (String s : datasetPrefixes) {
			prefixes [loopCount] = s;
			loopCount++;
		}
		combo.setItems(prefixes);
		combo.setBounds(10, 26, 138, 23);
		combo.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent me) {
				 if (combo.getText().trim().equals("")) {
					saveFlag = false;
				}
			}
		});
		combo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				try {
					mainDb.loadStatusOfMarkUp(statusOfMarkUp, combo.getText());
				} catch (Exception exe) {
					LOGGER.error("Error in loading Status of mark Up", exe);
					exe.printStackTrace();
				}
			}
		});

		final TabItem markupTabItem = new TabItem(tabFolder, SWT.NONE);
		markupTabItem.setText(ApplicationUtilities.getProperty("tab.five.name"));

		final Composite composite_4 = new Composite(tabFolder, SWT.NONE);
		markupTabItem.setControl(composite_4);

		markupTable = new Table(composite_4, SWT.CHECK | SWT.BORDER);
		markupTable.setBounds(10, 10, 744, 228);
		markupTable.setLinesVisible(true);
		markupTable.setHeaderVisible(true);
		

		final TableColumn transformationNumberColumnTableColumn_1_1 = new TableColumn(markupTable, SWT.NONE);
		transformationNumberColumnTableColumn_1_1.setWidth(81);
		transformationNumberColumnTableColumn_1_1.setText("Count");

		final TableColumn transformationNameColumnTableColumn_1_1 = new TableColumn(markupTable, SWT.NONE);
		transformationNameColumnTableColumn_1_1.setWidth(418);
		transformationNameColumnTableColumn_1_1.setText("Structure Name");

		final TableColumn transformationFileColumnTableColumn_1_1 = new TableColumn(markupTable, SWT.NONE);
		transformationFileColumnTableColumn_1_1.setWidth(215);

		final Button startMarkupButton = new Button(composite_4, SWT.NONE);
		startMarkupButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				startMarkup();
				
				try {
					mainDb.saveStatus(ApplicationUtilities.getProperty("tab.five.name"), combo.getText(), true);
					statusOfMarkUp[4] = true;
				} catch (Exception exe) {
					LOGGER.error("Couldnt save status - markup" , exe);
					exe.printStackTrace();
				}
				
			}
		});
		startMarkupButton.setBounds(548, 385, 100, 23);
		startMarkupButton.setText("Start");

		final Button removeMarkupButton = new Button(composite_4, SWT.NONE);
		removeMarkupButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				removeMarkup();
				/*try { You don't need to run markup again ater removal!
					mainDb.saveStatus(ApplicationUtilities.getProperty("tab.five.name"), combo.getText(), false);
					statusOfMarkUp[4] = false;
				} catch (Exception exe) {
					LOGGER.error("Couldnt save status - markup" , exe);
				} */
				
			}
		});
		removeMarkupButton.setBounds(654, 385, 100, 23);
		removeMarkupButton.setText("Remove");

		markupProgressBar = new ProgressBar(composite_4, SWT.NONE);
		markupProgressBar.setVisible(false);
		markupProgressBar.setBounds(10, 388, 532, 17);
		
		markUpPerlLog = new Text(composite_4, SWT.BORDER | SWT.MULTI| SWT.WRAP | SWT.V_SCROLL);
		markUpPerlLog.setBounds(10, 266, 744, 113);
		markUpPerlLog.setEnabled(false);
		
		Label lblPerlMessages = new Label(composite_4, SWT.NONE);
		lblPerlMessages.setBounds(10, 245, 100, 15);
		lblPerlMessages.setText("Status Messages :");

		final TabItem tagTabItem = new TabItem(tabFolder, SWT.NONE);
		tagTabItem.setText(ApplicationUtilities.getProperty("tab.six.name"));

		final Composite composite_6 = new Composite(tabFolder, SWT.NONE);
		tagTabItem.setControl(composite_6);
		/* Changing the "unknown removal checked box to RADIO*/
	    //tagTable = new Table(composite_6, SWT.CHECK | SWT.BORDER);
		//final Group group = new Group(, SWT.RADIO);
		tagTable = new Table(composite_6, SWT.CHECK | SWT.BORDER);
	
	    tagTable.addListener(SWT.Selection, new Listener() {
	        public void handleEvent(Event event) {
	        	TableItem item = (TableItem) event.item;
	        	//tagTable.getItem(hashCodeOfItem).setChecked(false);
	        	for (TableItem tempItem : tagTable.getItems()) {
	        		if (tempItem.hashCode() == hashCodeOfItem) {
	        			tempItem.setChecked(false);
	        		}
	        	} 
	        	int sentid = Integer.parseInt(item.getText(1));
	        	updateContext(sentid);
	        	if (hashCodeOfItem != item
	        			.hashCode()) {
	        		hashCodeOfItem = item.hashCode();
	        	} else {
	        		hashCodeOfItem = 0;
	        	}
	        	
	        }
	      });
		tagTable.setLinesVisible(true);
		tagTable.setHeaderVisible(true);
		tagTable.setBounds(10, 10, 744, 203);

	    final TableColumn newColumnTableColumn = new TableColumn(tagTable, SWT.NONE);
	    newColumnTableColumn.setWidth(81);
	    newColumnTableColumn.setText("Check");

		final TableColumn numberColumnTableColumn = new TableColumn(tagTable, SWT.NONE);
		numberColumnTableColumn.setWidth(78);
		numberColumnTableColumn.setText("Sentence Id");

	    final TableColumn modifierColumnTableColumn = new TableColumn(tagTable, SWT.NONE);
	    modifierColumnTableColumn.setWidth(65);
	    modifierColumnTableColumn.setText("Modifier");

		final TableColumn tagColumnTableColumn = new TableColumn(tagTable, SWT.NONE);
		tagColumnTableColumn.setWidth(78);
		tagColumnTableColumn.setText("Tag");

		final TableColumn sentenceColumnTableColumn = new TableColumn(tagTable, SWT.NONE);
		sentenceColumnTableColumn.setWidth(515);
		sentenceColumnTableColumn.setText("Sentence");

		tagListCombo = new Combo(composite_6, SWT.NONE);
		tagListCombo.setBounds(260, 387, 210, 21);

		final Button saveTagButton = new Button(composite_6, SWT.NONE);
		saveTagButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				saveTag();
			}
		});
		saveTagButton.setBounds(654, 219, 100, 23);
		saveTagButton.setText("Save");

		final Button loadTagButton = new Button(composite_6, SWT.NONE);
		loadTagButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				loadTags();
				try {
					mainDb.saveStatus(ApplicationUtilities.getProperty("tab.six.name"), combo.getText(), true);
					statusOfMarkUp[5] = true;
				} catch (Exception exe) {
					LOGGER.error("Couldnt save status - unknown" , exe);
					exe.printStackTrace();
				}
				
			}
		});
		loadTagButton.setBounds(548, 219, 100, 23);
		loadTagButton.setText("Load");

		final Label contextLabel = new Label(composite_6, SWT.NONE);
		contextLabel.setText("Context");
		contextLabel.setBounds(10, 229, 88, 13);

		contextStyledText = new StyledText(composite_6, SWT.V_SCROLL | SWT.READ_ONLY | SWT.H_SCROLL | SWT.BORDER);
		contextStyledText.setBounds(10, 248, 744, 114);

		modifierListCombo = new Combo(composite_6, SWT.NONE);
		modifierListCombo.setBounds(14, 387, 210, 21);

		final Label modifierLabel = new Label(composite_6, SWT.NONE);
		modifierLabel.setText("Modifier");
		modifierLabel.setBounds(15, 368, 64, 13);

		final Label tagLabel = new Label(composite_6, SWT.NONE);
		tagLabel.setText("Tag");
		tagLabel.setBounds(260, 368, 25, 13);

		final Button applyToAllButton = new Button(composite_6, SWT.NONE);
		applyToAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				applyTagToAll();
			}
		});
		applyToAllButton.setText("Apply to Checked");
		applyToAllButton.setBounds(513, 385, 110, 23);

		final TabItem finalizerTabItem = new TabItem(tabFolder, SWT.NONE);
		finalizerTabItem.setText(ApplicationUtilities.getProperty("tab.seven.name"));

		final Composite composite_5 = new Composite(tabFolder, SWT.NONE);
		finalizerTabItem.setControl(composite_5);

		finalizerTable = new Table(composite_5, SWT.FULL_SELECTION | SWT.BORDER);
		finalizerTable.setBounds(10, 10, 744, 369);
		finalizerTable.setLinesVisible(true);
		finalizerTable.setHeaderVisible(true);
		finalizerTable.addMouseListener(new MouseListener () {
			public void mouseDoubleClick(MouseEvent event) {
				String filePath = Registry.TargetDirectory + 
				ApplicationUtilities.getProperty("FINAL")+ "\\" +
				finalizerTable.getSelection()[0].getText(2).trim();				
				
				if (filePath.indexOf("xml") != -1) {
					try {
						Runtime.getRuntime().exec(ApplicationUtilities.getProperty("notepad") 
								+ " \"" + filePath + "\"");
					} catch (Exception e){
						ApplicationUtilities.showPopUpWindow(ApplicationUtilities.getProperty("popup.error.msg") +
								ApplicationUtilities.getProperty("popup.editor.msg"),
								ApplicationUtilities.getProperty("popup.header.error"), 
								SWT.ERROR);
					}
				} 
			}			
			public void mouseDown(MouseEvent event) {}
			public void mouseUp(MouseEvent event) {}
		});

		final TableColumn transformationNumberColumnTableColumn_1_2 = new TableColumn(finalizerTable, SWT.NONE);
		transformationNumberColumnTableColumn_1_2.setWidth(168);
		transformationNumberColumnTableColumn_1_2.setText("Number");

		final TableColumn transformationNameColumnTableColumn_1_2 = new TableColumn(finalizerTable, SWT.NONE);
		transformationNameColumnTableColumn_1_2.setWidth(172);
		transformationNameColumnTableColumn_1_2.setText("Name");

		final TableColumn transformationFileColumnTableColumn_1_2 = new TableColumn(finalizerTable, SWT.NONE);
		transformationFileColumnTableColumn_1_2.setWidth(376);
		transformationFileColumnTableColumn_1_2.setText("File");

		final Button startFinalizerButton = new Button(composite_5, SWT.NONE);
		startFinalizerButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				startFinalize();
				try {
					mainDb.saveStatus(ApplicationUtilities.getProperty("tab.seven.name"), combo.getText(), true);
					statusOfMarkUp[6] = true;
				} catch (Exception exe) {
					LOGGER.error("Couldnt save status - markup" , exe);
					exe.printStackTrace();
				}
				
			}
		});
		startFinalizerButton.setBounds(548, 385, 100, 23);
		startFinalizerButton.setText("Start");

		final Button clearFinalizerButton = new Button(composite_5, SWT.NONE);
		clearFinalizerButton.setBounds(654, 385, 100, 23);
		clearFinalizerButton.setText("Clear");

		finalizerProgressBar = new ProgressBar(composite_5, SWT.NONE);
		finalizerProgressBar.setVisible(false);
		finalizerProgressBar.setBounds(10, 387, 522, 17);

		final TabItem glossaryTabItem = new TabItem(tabFolder, SWT.NONE);
		glossaryTabItem.setText(ApplicationUtilities.getProperty("tab.eight.name"));

		final Composite composite_7 = new Composite(tabFolder, SWT.NONE);
		glossaryTabItem.setControl(composite_7);

		glossaryStyledText = new StyledText(composite_7, SWT.V_SCROLL | SWT.READ_ONLY | SWT.H_SCROLL | SWT.BORDER);
		glossaryStyledText.setBounds(10, 10, 744, 369);

		final Button reportGlossaryButton = new Button(composite_7, SWT.NONE);
		reportGlossaryButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				reportGlossary();
				try {
					mainDb.saveStatus(ApplicationUtilities.getProperty("tab.eight.name"), combo.getText(), true);
					statusOfMarkUp[7] = true;
				} catch (Exception exe) {
					LOGGER.error("Couldnt save status - glossary" , exe);
					exe.printStackTrace();
				}
				
			}
		});
		reportGlossaryButton.setBounds(654, 385, 100, 23);
		reportGlossaryButton.setText("Report");

		final Label logoLabel = new Label(shell, SWT.NONE);
		logoLabel.setText(ApplicationUtilities.getProperty("application.instructions"));
		logoLabel.setBounds(10, 460, 530, 96);

		final Label label = new Label(shell, SWT.NONE);
		label.setBackgroundImage(SWTResourceManager.getImage(Type3MainForm.class, 
				ApplicationUtilities.getProperty("application.logo")));
		label.setBounds(569, 485, 253, 71);

	}
	
	private void browseConfigurationDirectory() {
        DirectoryDialog directoryDialog = new DirectoryDialog(shell);
        directoryDialog.setMessage("Please select a directory and click OK");
        
        String directory = directoryDialog.open();
        if(directory != null && !directory.equals("")) {
        	String dirsep = System.getProperty("file.separator");
        	if(!directory.endsWith(dirsep)){
        		directory =directory+dirsep;
        	}
          configurationText.setText(directory);
          Registry.ConfigurationDirectory = directory;
        }
	}
	
	private void browseSourceDirectory() {
        DirectoryDialog directoryDialog = new DirectoryDialog(shell);
        directoryDialog.setMessage("Please select a directory and click OK");
        
        String directory = directoryDialog.open();
        if(directory != null && !directory.equals("")) {
        	String dirsep = System.getProperty("file.separator");
        	if(!directory.endsWith(dirsep)){
        		directory =directory+dirsep;
        	}
          sourceText.setText(directory);
          Registry.SourceDirectory = directory;
        }
	}
	
	private void browseTargetDirectory() {
        DirectoryDialog directoryDialog = new DirectoryDialog(shell);
        directoryDialog.setMessage("Please select a directory and click OK");
        
        String directory = directoryDialog.open();
        if(directory != null && !directory.equals("")) {
        	String dirsep = System.getProperty("file.separator");
        	if(!directory.endsWith(dirsep)){
        		directory =directory+dirsep;
        	}
        	targetText.setText(directory);
        	Registry.TargetDirectory = directory;
        }
	}
	

	
	private void loadProject() {
		//TODO load configure from a local file
		try{
		File project = new File("project.conf");
		BufferedReader in = new BufferedReader(new FileReader(project));
		String conf = in.readLine();
		conf = conf == null ? "" : conf;
		configurationText.setText(conf);
        Registry.ConfigurationDirectory = conf;

        String source = in.readLine();
        source = source == null ? "" : source;
        sourceText.setText(source);
        Registry.SourceDirectory = source;
        
        String target = in.readLine();
        target = target == null ? "" : target;
        targetText.setText(target);
        Registry.TargetDirectory = target;
		
        /*configurationText.setText("c:\\fna-v19\\conf\\");
        Registry.ConfigurationDirectory = "c:\\fna-v19\\conf\\";

        sourceText.setText("c:\\fna-v19\\source\\");
        Registry.SourceDirectory = "c:\\fna-v19\\source\\";
        
        targetText.setText("c:\\fna-v19\\target\\");
        Registry.TargetDirectory = "c:\\fna-v19\\target\\";*/
		}catch(Exception e){
			LOGGER.error("couldn't load the configuration file", e);
			e.printStackTrace();
		}
	}
	
	private void saveProject() {

		StringBuffer sb = new StringBuffer();
		sb.append(configurationText.getText()).append("\n");
		sb.append(sourceText.getText()).append("\n");
		sb.append(targetText.getText());
		try{
			File project = new File("project.conf");
			if(!project.exists()){
				project.createNewFile();
			}
			BufferedWriter out = new BufferedWriter(new FileWriter(project));
			out.write(sb.toString());
			out.close();
		}catch(Exception e){
			LOGGER.error("couldn't save project", e);
			e.printStackTrace();
		}
	
	}
	
	private void startMarkup() {
		markupProgressBar.setVisible(true);
		String workdir = Registry.TargetDirectory;
		String todofoldername = ApplicationUtilities.getProperty("DESCRIPTIONS");
		String databasename = ApplicationUtilities.getProperty("database.name");
		ProcessListener listener = new ProcessListener(markupTable, markupProgressBar, shell.getDisplay());
		
		VolumeDehyphenizer vd = new VolumeDehyphenizer(listener, workdir, todofoldername,
				databasename, shell.getDisplay(), markUpPerlLog, dataPrefixCombo.getText());
		vd.dehyphen();
		
		markupProgressBar.setVisible(false);
	}
	
	private void startFinalize() {
		ProcessListener listener = new ProcessListener(finalizerTable, finalizerProgressBar, shell.getDisplay());
		VolumeFinalizer vf = new VolumeFinalizer(listener, dataPrefixCombo.getText());
		vf.start();
		finalizerProgressBar.setVisible(false);
	}
	
	private void removeMarkup() {
		// gather removed tag
		List<String> removedTags = new ArrayList<String>();
		for (TableItem item : markupTable.getItems()) {
			if (item.getChecked()) {
				removedTags.add(item.getText(1));
			}
		}
		
		// remove the tag from the database
		
		try {
			mainDb.removeMarkUpData(removedTags);
		} catch (Exception exe) {
			LOGGER.error("Exception encountered in removing tags from database in Type3MainForm:removeMarkup", exe);
			exe.printStackTrace();
		}

		ProcessListener listener = new ProcessListener(markupTable, markupProgressBar, shell.getDisplay());
		VolumeMarkup vm = new VolumeMarkup(listener, null, null, null);
		vm.update();
	}
	
	private void loadTags() {
		loadTagTable();
		tagListCombo.add("PART OF LAST SENTENCE"); //part of the last sentence
		
		try {
			 mainDb.loadTagsData(tagListCombo, modifierListCombo);
			} catch (Exception exe) {
				LOGGER.error("Exception encountered in loading tags from database in Type3MainForm:loadTags", exe);
				exe.printStackTrace();
		    }

	}

	private void loadTagTable() {
		tagTable.removeAll();
		
		try {
			 mainDb.loadTagsTableData(tagTable);
		} catch (Exception exe) {
				LOGGER.error("Exception encountered in loading tags from database in Type3MainForm:loadTags", exe);
				exe.printStackTrace();
		}

	}
	
	private void updateContext(int sentid) throws ParsingException {
		contextStyledText.setText("");
		tagListCombo.setText("");
		
		
		try {
			mainDb.updateContextData(sentid, contextStyledText);

		} catch (Exception e) {
			LOGGER.error("Exception encountered in loading tags from database in Type3MainForm:updateContext", e);
			e.printStackTrace();
			throw new ParsingException("Failed to execute the statement.", e);
			
		}
	}
	
	private void applyTagToAll(){
		String tag = tagListCombo.getText();
		String modifier = this.modifierListCombo.getText();
		
		if (tag == null || tag.equals(""))
			return;
		
		for (TableItem item : tagTable.getItems()) {
			//if (item.hashCode() == hashCodeOfItem) {
			if (item.getChecked()) {
				item.setText(2, modifier);
				item.setText(3, tag);
			}
		}
	}
	
	
	private void saveTag() {

		try {
			mainDb.saveTagData(tagTable);
			
		} catch (Exception exe) {
			LOGGER.error("Exception encountered in loading tags from database in Type3MainForm:saveTag", exe);
			exe.printStackTrace();
		}
		loadTagTable();
		//reset context box
		contextStyledText.setText("");
	}
	
	private void reportGlossary() {
		
		LearnedTermsReport ltr = new LearnedTermsReport(ApplicationUtilities.getProperty("database.name") + "_corpus");
		glossaryStyledText.append(ltr.report());
	}
	
	private boolean checkFields(StringBuffer messageText, TabFolder tabFolder) {
		
		boolean errorFlag = false;
		
		if ( configurationText != null && configurationText.getText().equals("")) {
			messageText.append(ApplicationUtilities.getProperty("popup.error.config"));
		}  			
		if ( targetText != null && targetText.getText().equals("")) {
			messageText.append(ApplicationUtilities.getProperty("popup.error.target"));
		} 					
		if ( sourceText != null && sourceText.getText().equals("")) {
			messageText.append(ApplicationUtilities.getProperty("popup.error.source"));
		} 
		
		if (dataPrefixCombo != null && dataPrefixCombo.getText().trim().equals("")) {
			
			messageText.append(ApplicationUtilities.getProperty("popup.error.dataset"));
			
		}
		
		if (messageText.length() != 0) {
			messageText.append(ApplicationUtilities.getProperty("popup.error.info"));
			ApplicationUtilities.showPopUpWindow(messageText.toString(), 
					ApplicationUtilities.getProperty("popup.header.missing"), SWT.ICON_WARNING);						
			//tabFolder.
			tabFolder.setSelection(0);
			tabFolder.setFocus();
			errorFlag = true;
		} else {
			if(configurationText != null && !saveFlag) {
				errorFlag = false;
			}

		}
		
		return errorFlag;
	}
}
