 /* $Id$ */
package fna.parsing;
//
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowData;
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

import fna.beans.CharacterGroupBean;
import fna.beans.CoOccurrenceBean;
import fna.beans.ContextBean;
import fna.beans.TermBean;
import fna.beans.TermsDataBean;
import fna.db.CharacterStateDBAccess;
import fna.db.MainFormDbAccessor;
import fna.parsing.ParsingException;
import fna.parsing.ProcessListener;
import fna.parsing.Registry;
import fna.parsing.VolumeExtractor;
import fna.parsing.VolumeFinalizer;
import fna.parsing.VolumeMarkup;
import fna.parsing.VolumeTransformer;
import fna.parsing.VolumeVerifier;
import fna.parsing.character.CoOccurrenceGraph;
import fna.parsing.character.GraphNode;
import fna.parsing.character.LearnedTermsReport;
import fna.parsing.character.ManipulateGraphML;
import fna.parsing.state.GraphMLOutputter;
import fna.parsing.state.StateCollectorBootstrapper;
import fna.parsing.state.StateCollectorTest;

import org.eclipse.swt.custom.ScrolledComposite;
/**
 * @author chunshui, Partha Pratim Sanyal (ppsanyal@email.arizona.edu)
 */

//
public class MainForm {

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
	private Table transformationTable;
	
	private Table verificationTable;
	private Table extractionTable;
	private Table tagTable;
	private Text targetText;
	private Text sourceText;
	private Text configurationText;
	private TabItem generalTabItem;
	private StyledText contextStyledText;
	private ProgressBar markupProgressBar;
	private ProgressBar extractionProgressBar;
	private ProgressBar verificationProgressBar;
	private ProgressBar transformationProgressBar;
	private ProgressBar finalizerProgressBar;
	
	private Combo tagListCombo;
	public static Combo dataPrefixCombo;

	
	private StyledText glossaryStyledText;
	public Shell shell;
	/*In Unknown removal this variable is used to remember the last tab selected*/
	private static int hashCodeOfItem = 0;
	private boolean [] statusOfMarkUp = {false, false, false, false, false, false, false, false};
	private static boolean saveFlag = false;
	private static final Logger LOGGER = Logger.getLogger(MainForm.class);
	/* This will determine how many tabs to show */
	private String type = "";
		/**
	 * Launch the application
	 * @param args
	 */
	
	private MainFormDbAccessor mainDb = new MainFormDbAccessor();
	private CharacterStateDBAccess charDb = new CharacterStateDBAccess();
	public static Text markUpPerlLog;
	
	/*Character Tab variables-----------------------------------------------------------------------------------*/
	/* This combo is the decision combo in Character tab */
	private static Combo comboDecision;
	/* This combo is the groups list on the Character Tab*/
	public static Combo groupsCombo;
	/* This Scrolled composite holds the termsGroup */
	private static Group termsGroup;
	/* This Scrolled Composite will hold the terms group */	
	private static ScrolledComposite termsScrolledComposite;
	/* This Group will hold all the removed terms */
	private static Group removedTermsGroup;
	/* This Scrolled Composite will hold the removed terms group */	
	private static ScrolledComposite removedScrolledComposite ;
	/* This is the standard increment for every terms row*/
	private static int standardIncrement = 30;
	/* These are the initial coordinates of term 1 group - this will keep on changing and hold the latest group
	 * Once a new group is loaded, this will be reset to initial values
	 * Initial y =
	 * */
	private static Rectangle term1 = new Rectangle(40, 10, 130, 35);
	
	/* These are the initial coordinates of term 2 group - this will keep on changing and hold the latest group
	 * Once a new group is loaded, this will be reset to initial values
	 * Initial y =
	 * */
	private static Rectangle term2 = new Rectangle(210, 10, 130, 35);
	
	/* These are the initial coordinates of deleted term 2 group - this will keep on changing and hold the latest group
	 * Once a new group is loaded, this will be reset to initial values
	 * Initial y =
	 * */
	private static Rectangle contextRadio = new Rectangle(10, 20, 20, 15);
	
	/* These are the initial coordinates of frequency label - this will keep on changing and hold the latest group
	 * Once a new group is loaded, this will be reset to initial values
	 * Initial y =
	 * */
	private static Rectangle frequencyLabel = new Rectangle(370, 20, 35, 15);
	/* This HashMap will hold all the group info temporarily*/
	private static HashMap <String, CharacterGroupBean> groupInfo = new HashMap <String, CharacterGroupBean> ();
	/* This HashMap will hold all the group info of removed terms*/
	private static ArrayList <TermsDataBean> removedTermsGroups;
	/* This HashMap will hold all processed groups information */
	private static TreeMap <String, String> processedGroups = new TreeMap<String, String> ();	
	/* This table is for showing contextual sentences */
	private Table contextTable;
	/* This table holed currently processed groups */
	private Table processedGroupsTable;
	/* This will hold sorted order of each group of terms */
	private boolean [] sortedBy ;
	/* This is the sort label picture */
	private Label sortLabel;
	/* This will all the groups removed edges from the graph */
	private static HashMap<String, ArrayList<String>> removedEdges 
		= new HashMap<String, ArrayList<String>>();
	/* This variable stores the number of groups produced initially. This is needed to check whether
	 * remaining terms group can be generated.
	 */
	private static int noOfTermGroups = 0;
	/* Show information to the user after the first time pop up message while grouping the remaining terms */
	private static boolean charStatePopUp = true;
	
	//-----------------------------------Character Tab Variables ---END-------------------------------//
	
	public static void launchMarker(String type) {
		try {
			
			MainForm window = new MainForm();
			window.type = type ;
			window.open();
		} catch (Exception e) {
			LOGGER.error("Error Launching application", e);
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launchMarker("");
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
		if(shell.isDisposed()) {
			System.exit(0);
		}
	}

	/**
	 * Create contents of the window
	 */
	protected void createContents(Display display) throws Exception{
		shell = new Shell(display);
		shell.setImage(SWTResourceManager.getImage(MainForm.class, "/fna/parsing/garland_logo.gif"));
		shell.setSize(843, 614);
		shell.setLocation(200, 100);
		shell.setText(ApplicationUtilities.getProperty("application.name"));

		final TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
		tabFolder.setBounds(10, 10, 803, 469);
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
						mainDb.savePrefixData(dataPrefixCombo.getText().replaceAll("-", "_").trim());
					} catch (Exception exe) {
						LOGGER.error("Error saving dataprefix", exe);
						exe.printStackTrace();
					}
				 }
				/* Check if the Type of document selected is Type 3 or Type 4*/
				if (type.equals("")){
					//segmentation
					if (!statusOfMarkUp[1]) {
						if(!tabName.equals(ApplicationUtilities.getProperty("tab.one.name"))
								&& !tabName.equals(ApplicationUtilities.getProperty("tab.two.name"))) {
							ApplicationUtilities.showPopUpWindow( 
									ApplicationUtilities.getProperty("popup.error.tab")+ " " +
									ApplicationUtilities.getProperty("tab.two.name"), 
									ApplicationUtilities.getProperty("popup.header.error"),
									SWT.ICON_ERROR);
							tabFolder.setSelection(1);
							tabFolder.setFocus();
							return;
						}
					}
					//verification
					if (!statusOfMarkUp[2]) {
						if(!tabName.equals(ApplicationUtilities.getProperty("tab.one.name"))
								&& !tabName.equals(ApplicationUtilities.getProperty("tab.two.name"))
								&& !tabName.equals(ApplicationUtilities.getProperty("tab.three.name"))) {
							ApplicationUtilities.showPopUpWindow(								
									ApplicationUtilities.getProperty("popup.error.tab")+ " " +
									ApplicationUtilities.getProperty("tab.three.name"), 
									ApplicationUtilities.getProperty("popup.header.error"),
									SWT.ICON_ERROR);
							tabFolder.setSelection(2);
							tabFolder.setFocus();
							return;
							
						}

					}
				}

				//Transformation
				if (!statusOfMarkUp[3]) {
					if(!tabName.equals(ApplicationUtilities.getProperty("tab.one.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.two.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.three.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.four.name"))) {
						ApplicationUtilities.showPopUpWindow(								
								ApplicationUtilities.getProperty("popup.error.tab")+ " " +
								ApplicationUtilities.getProperty("tab.four.name"), 
								ApplicationUtilities.getProperty("popup.header.error"),
								SWT.ICON_ERROR);
						if (type.equals("")){
							tabFolder.setSelection(3);
						} else {
							tabFolder.setSelection(1);
						}
						
						tabFolder.setFocus();
						return;							
					}

				}
				// Mark Up
				if (!statusOfMarkUp[4]) {
					if(!tabName.equals(ApplicationUtilities.getProperty("tab.one.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.two.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.three.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.four.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.five.name"))) {
						ApplicationUtilities.showPopUpWindow(								
								ApplicationUtilities.getProperty("popup.error.tab")+ " " +
								ApplicationUtilities.getProperty("tab.five.name")
								.substring(0, ApplicationUtilities.getProperty("tab.five.name").indexOf(" ")), 
								ApplicationUtilities.getProperty("popup.header.error"),
								SWT.ICON_ERROR);
						if (type.equals("")){
							tabFolder.setSelection(4);
						} else {
							tabFolder.setSelection(2);
						}
						tabFolder.setFocus();
						return;
					}
					

				}
				//Unknown Removal
				if (!statusOfMarkUp[5]) {
					if(!tabName.equals(ApplicationUtilities.getProperty("tab.one.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.two.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.three.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.four.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.five.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.six.name"))) {
						ApplicationUtilities.showPopUpWindow(								
								ApplicationUtilities.getProperty("popup.error.tab")+ " " +
								ApplicationUtilities.getProperty("tab.six.name"), 
								ApplicationUtilities.getProperty("popup.header.error"),
								SWT.ICON_ERROR);
						if (type.equals("")){
							tabFolder.setSelection(5);
						} else {
							tabFolder.setSelection(3);
						}
						tabFolder.setFocus();
						return;
					}

				}
				//Finalizer
				if (!statusOfMarkUp[6]) {
					if(!tabName.equals(ApplicationUtilities.getProperty("tab.one.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.two.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.three.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.four.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.five.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.six.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.seven.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.character"))
					) {
						ApplicationUtilities.showPopUpWindow(								
								ApplicationUtilities.getProperty("popup.error.tab")+ " " +
								ApplicationUtilities.getProperty("tab.seven.name"), 
								ApplicationUtilities.getProperty("popup.header.error"),
								SWT.ICON_ERROR);
						if (type.equals("")){
							tabFolder.setSelection(6);
						} else {
							tabFolder.setSelection(4);
						}
						tabFolder.setFocus();
						return;
					}

				}	
				//changed from 6 to 5
				if (statusOfMarkUp[5]) {
					if(tabName.equals(ApplicationUtilities.getProperty("tab.character"))){
						// set the decisions combo
						setCharacterTabDecisions();
						// set the groups list
						setCharactertabGroups();
						// show the terms that co-occured in the first group
						loadTerms();
						//Clear context table;
						contextTable.removeAll();
						//load processed groups table;
						loadProcessedGroups();
						
						//createRemainingTermsGroup();
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
					String t = mainDb.getLastAccessedDataSet();
					String prefix = t==null? "" : t;
					
					MainForm.dataPrefixCombo.setText(prefix);
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
					mainDb.savePrefixData(dataPrefixCombo.getText().replaceAll("-", "_").trim());
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
				/* remove the deleted edges graph if a new prefix is selected*/
				removedEdges.clear();
			}
		});
		
		if (type.equals("")){
			/* Segmentation Tab */
			final TabItem segmentationTabItem = new TabItem(tabFolder, SWT.NONE);
			segmentationTabItem.setText(ApplicationUtilities.getProperty("tab.two.name"));
			
			final Composite composite_1 = new Composite(tabFolder, SWT.NONE);
			segmentationTabItem.setControl(composite_1);

			extractionProgressBar = new ProgressBar(composite_1, SWT.NONE);
			extractionProgressBar.setVisible(false);
			extractionProgressBar.setBounds(10, 386, 581, 17);
			
			final Button startExtractionButton = new Button(composite_1, SWT.NONE);
			startExtractionButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent e) {
					try {
						extractionTable.removeAll();
						startExtraction(); // start the extraction process
					} catch (Exception exe) {
						LOGGER.error("unable to extract in mainform", exe);
						exe.printStackTrace();
					}
					 
					// Saving the status of markup
					statusOfMarkUp[1] = true;
					try {
						mainDb.saveStatus(ApplicationUtilities.getProperty("tab.two.name"), combo.getText(), true);
					} catch (Exception exe) {
						LOGGER.error("Couldnt save status - segment" , exe);
						exe.printStackTrace();
					}
				}
			});
			startExtractionButton.setText(ApplicationUtilities.getProperty("start"));
			startExtractionButton.setBounds(656, 380, 53, 23);

			extractionTable = new Table(composite_1, SWT.FULL_SELECTION | SWT.BORDER );
			extractionTable.setLinesVisible(true);
			extractionTable.setHeaderVisible(true);
			extractionTable.setBounds(10, 10, 744, 358);
			/*Hyperlinking the file */
			extractionTable.addMouseListener(new MouseListener () {
				public void mouseDoubleClick(MouseEvent event) {
					String filePath = Registry.TargetDirectory + "\\"+ 
					ApplicationUtilities.getProperty("EXTRACTED")+"\\"+
					extractionTable.getSelection()[0].getText(1).trim();
					
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
				public void mouseDown(MouseEvent event) {}
				public void mouseUp(MouseEvent event) {}
			});

			final TableColumn extractionNumberColumnTableColumn = new TableColumn(extractionTable, SWT.NONE);
			extractionNumberColumnTableColumn.setWidth(100);
			extractionNumberColumnTableColumn.setText(
					ApplicationUtilities.getProperty("count"));

			final TableColumn extractionFileColumnTableColumn = new TableColumn(extractionTable, SWT.NONE);
			extractionFileColumnTableColumn.setWidth(254);
			extractionFileColumnTableColumn.setText(
					ApplicationUtilities.getProperty("file"));
			
			Button btnLoad = new Button(composite_1, SWT.NONE);
			btnLoad.setBounds(597, 380, 53, 23);
			btnLoad.setText("Load");
			btnLoad.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent e) {
					loadFileInfo(extractionTable, Registry.TargetDirectory + 
							ApplicationUtilities.getProperty("EXTRACTED"));
				}
			});
			
			Button btnClear = new Button(composite_1, SWT.NONE);
			btnClear.setBounds(715, 380, 45, 23);
			btnClear.setText("Clear");
			btnClear.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent e) {
					extractionTable.removeAll();
					statusOfMarkUp[1] = false;
					try {
						mainDb.saveStatus(ApplicationUtilities.getProperty("tab.two.name"), combo.getText(), false);
					} catch (Exception exe) {
						LOGGER.error("Couldnt save status - verify" , exe);
						exe.printStackTrace();
					}
				}
			});
						
			/* Verification Tab*/			
			
			final TabItem verificationTabItem = new TabItem(tabFolder, SWT.NONE);
			verificationTabItem.setText(ApplicationUtilities.getProperty("tab.three.name"));

			final Composite composite_2 = new Composite(tabFolder, SWT.NONE);
			verificationTabItem.setControl(composite_2);

			final Button startVerificationButton = new Button(composite_2, SWT.NONE);
			startVerificationButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent e) {
					System.out.println("Starting!!");
					verificationTable.removeAll();
					startVerification(); // start the verification process
					statusOfMarkUp[2] = true;
					try {
						mainDb.saveStatus(ApplicationUtilities.getProperty("tab.three.name"), combo.getText(), true);
					} catch (Exception exe) {
						LOGGER.error("Couldnt save status - verify" , exe);
						exe.printStackTrace();
					}
				}
			});
			startVerificationButton.setBounds(624, 385, 65, 23);
			startVerificationButton.setText(ApplicationUtilities.getProperty("start"));

			final Button clearVerificationButton = new Button(composite_2, SWT.NONE);
			clearVerificationButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent e) {
					clearVerification();
					statusOfMarkUp[2] = false;
					try {
						mainDb.saveStatus(ApplicationUtilities.getProperty("tab.three.name"), combo.getText(), false);
					} catch (Exception exe) {
						LOGGER.error("Couldnt save status - verify" , exe);
						exe.printStackTrace();
					}
				}
			});
			clearVerificationButton.setBounds(695, 385, 59, 23);
			clearVerificationButton.setText("Clear");
			verificationTable = new Table(composite_2, SWT.BORDER | SWT.FULL_SELECTION);
			verificationTable.setBounds(10, 10, 744, 369);
			verificationTable.setLinesVisible(true);
			verificationTable.setHeaderVisible(true);
			verificationTable.addMouseListener(new MouseListener () {
				public void mouseDoubleClick(MouseEvent event) {
					String filePath = Registry.TargetDirectory + 
					ApplicationUtilities.getProperty("EXTRACTED")+ "\\" +
					verificationTable.getSelection()[0].getText(1).trim();
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

			final TableColumn verificationStageColumnTableColumn = new TableColumn(verificationTable, SWT.NONE);
			verificationStageColumnTableColumn.setWidth(168);
			verificationStageColumnTableColumn.setText("Task");

			final TableColumn verificationFileColumnTableColumn = new TableColumn(verificationTable, SWT.NONE);
			verificationFileColumnTableColumn.setWidth(172);
			verificationFileColumnTableColumn.setText(ApplicationUtilities.getProperty("file"));

			final TableColumn verificationErrorColumnTableColumn = new TableColumn(verificationTable, SWT.NONE);
			verificationErrorColumnTableColumn.setWidth(376);
			verificationErrorColumnTableColumn.setText("Error");

			verificationProgressBar = new ProgressBar(composite_2, SWT.NONE);
			verificationProgressBar.setVisible(false);
			verificationProgressBar.setBounds(10, 387, 515, 17);
			
			Button btnLoad_1 = new Button(composite_2, SWT.NONE);
			btnLoad_1.setBounds(543, 385, 75, 23);
			btnLoad_1.setText("Load");
			btnLoad_1.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent e) {
					loadFileInfo(verificationTable, Registry.TargetDirectory + 
							ApplicationUtilities.getProperty("EXTRACTED"));
				}
			});
			
		}
		


		final TabItem transformationTabItem = new TabItem(tabFolder, SWT.NONE);
		transformationTabItem.setText(ApplicationUtilities.getProperty("tab.four.name"));

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

		final Composite composite_3 = new Composite(tabFolder, SWT.NONE);
		transformationTabItem.setControl(composite_3);

		transformationTable = new Table(composite_3, SWT.FULL_SELECTION | SWT.BORDER);
		transformationTable.setBounds(10, 10, 744, 369);
		transformationTable.setLinesVisible(true);
		transformationTable.setHeaderVisible(true);
		transformationTable.addMouseListener(new MouseListener () {
			public void mouseDoubleClick(MouseEvent event) {
				String filePath = Registry.TargetDirectory + 
				ApplicationUtilities.getProperty("TRANSFORMED")+ "\\" +
				transformationTable.getSelection()[0].getText(1).trim();
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


		final TableColumn transformationNumberColumnTableColumn_1 = new TableColumn(transformationTable, SWT.NONE);
		transformationNumberColumnTableColumn_1.setWidth(168);
		transformationNumberColumnTableColumn_1.setText("Count");

		final TableColumn transformationNameColumnTableColumn_1 = new TableColumn(transformationTable, SWT.NONE);
		transformationNameColumnTableColumn_1.setWidth(172);
		transformationNameColumnTableColumn_1.setText("File");

		final TableColumn transformationFileColumnTableColumn_1 = new TableColumn(transformationTable, SWT.NONE);
		transformationFileColumnTableColumn_1.setWidth(376);
		transformationFileColumnTableColumn_1.setText("Error");

		final Button startTransformationButton = new Button(composite_3, SWT.NONE);
		startTransformationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				transformationTable.removeAll();
				if(type.equals("")) {
					startTransformation(); // start the transformation process
				} else if (type.equals("type3")) {
					startPreMarkUp(); // start pre-mark up process
				} else if (type.equals("type4")) {
					startType4Transformation(); // When the doc selected is type 4
				}
				
				
				try {
					mainDb.saveStatus(ApplicationUtilities.getProperty("tab.four.name"), combo.getText(), true);
					statusOfMarkUp[3] = true;
				} catch (Exception exe) {
					LOGGER.error("Couldnt save status - transform" , exe);
					exe.printStackTrace();
				}
			}
		});
		startTransformationButton.setBounds(633, 385, 60, 23);
		startTransformationButton.setText("Start");
		
		/* Type 4 Transformation doesn't do anything other than listing source files : Doubtful*/
		if (type.equals("type4")){
			startTransformationButton.setVisible(false);
		}

		final Button clearTransformationButton = new Button(composite_3, SWT.NONE);
		clearTransformationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				clearTransformation();				
				try {
					mainDb.saveStatus(ApplicationUtilities.getProperty("tab.four.name"), combo.getText(), false);
					statusOfMarkUp[3] = false;
				} catch (Exception exe) {
					LOGGER.error("Couldnt save status - transform" , exe);
					exe.printStackTrace();
				}
			}
		});
		clearTransformationButton.setBounds(701, 385, 53, 23);
		clearTransformationButton.setText("Clear");

		transformationProgressBar = new ProgressBar(composite_3, SWT.NONE);
		transformationProgressBar.setVisible(false);
		transformationProgressBar.setBounds(10, 387, 524, 17);
		
		Button button = new Button(composite_3, SWT.NONE);
		button.setBounds(567, 385, 60, 23);
		button.setText("Load");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				loadFileInfo(transformationTable, Registry.TargetDirectory + 
						ApplicationUtilities.getProperty("TRANSFORMED"));
			}
		});

		final TabItem tagTabItem = new TabItem(tabFolder, SWT.NONE);
		tagTabItem.setText(ApplicationUtilities.getProperty("tab.six.name"));

		final Composite composite_6 = new Composite(tabFolder, SWT.NONE);
		tagTabItem.setControl(composite_6);
		/* Changing the "unknown removal checked box to RADIO*/
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
		
		///////////////// New Tab!!????????????/////////////////////////
		/* Character State tab */
		TabItem tbtmCharacterStates = new TabItem(tabFolder, SWT.NONE);
		tbtmCharacterStates.setText(ApplicationUtilities.getProperty("tab.character"));
		
		Composite composite_8 = new Composite(tabFolder, SWT.NONE);
		tbtmCharacterStates.setControl(composite_8);
		
		Group grpContextTable = new Group(composite_8, SWT.NONE);
		grpContextTable.setText("Context Table");
		grpContextTable.setBounds(0, 282, 635, 149);
		// Add the context table here
		contextTable = new Table(grpContextTable, SWT.FULL_SELECTION | SWT.BORDER);
		contextTable.setBounds(10, 20, 615, 119);
		contextTable.setHeaderVisible(true);
		contextTable.setLinesVisible(true);
		contextTable.addMouseListener(new MouseListener () {
			public void mouseDoubleClick(MouseEvent event) {
				try {
					String filePath = Registry.TargetDirectory + 
					ApplicationUtilities.getProperty("DEHYPHENED")+ "\\";
					String fileName = contextTable.getSelection()[0].getText(0).trim();
					fileName = fileName.substring(0, fileName.indexOf("-"));
					filePath += fileName;
					if (filePath.indexOf("txt") != -1) {
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
				} catch (Exception exe) {
					LOGGER.error("Error in displaying the file in context table", exe);
					exe.printStackTrace();
				}
 
			}			
			public void mouseDown(MouseEvent event) {}
			public void mouseUp(MouseEvent event) {}
		});
		
		final TableColumn contextTablecolumn_1 = new TableColumn(contextTable, SWT.NONE);
		contextTablecolumn_1.setWidth(100);
		contextTablecolumn_1.setText("Source");
		
		final TableColumn contextTablecolumn_2 = new TableColumn(contextTable, SWT.NONE);
		contextTablecolumn_2.setWidth(512);
		contextTablecolumn_2.setText("Sentence");
		
		Group group_2 = new Group(composite_8, SWT.NONE);
		group_2.setBounds(641, 240, 144, 191);
		
		processedGroupsTable = new Table(group_2, SWT.BORDER | SWT.FULL_SELECTION);
		processedGroupsTable.setBounds(10, 10, 124, 171);
		processedGroupsTable.setLinesVisible(true);
		processedGroupsTable.setHeaderVisible(true);
		
		TableColumn tableColumn = new TableColumn(processedGroupsTable, SWT.NONE);
		tableColumn.setWidth(120);
		tableColumn.setText("Processed Groups");
		
		Group group_3 = new Group(composite_8, SWT.NONE);
		group_3.setBounds(0, 240, 635, 36);
		
		Label lblGroup = new Label(group_3, SWT.NONE);
		lblGroup.setBounds(10, 13, 40, 15);
		lblGroup.setText("Group");
		
		groupsCombo = new Combo(group_3, SWT.NONE);
		groupsCombo.setBounds(56, 10, 161, 23);
		groupsCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//Clear context table as the sentences from the previous group needn't be shown for another group;
				contextTable.removeAll();
				loadTerms();
			}
		});
		
		Label lblDecision = new Label(group_3, SWT.NONE);
		lblDecision.setBounds(286, 13, 55, 15);
		lblDecision.setText("Decision");
		
		comboDecision = new Combo(group_3, SWT.NONE);
		comboDecision.setBounds(365, 10, 145, 23);

		
		Button btnSave = new Button(group_3, SWT.NONE);
		btnSave.setBounds(550, 8, 75, 25);
		btnSave.setText("Save");
		btnSave.setToolTipText("Save");
		btnSave.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				((CharacterGroupBean)groupInfo.get(groupsCombo.getText())).setSaved(true);
				((CharacterGroupBean)groupInfo.get(groupsCombo.getText())).setDecision(comboDecision.getText());
				
				ArrayList<TermsDataBean> terms = new ArrayList<TermsDataBean>();				
				ArrayList <CoOccurrenceBean> cooccurrences = groupInfo.get(groupsCombo.getText()).getCooccurrences();								
				try {
					
					/*Save the decision first */
					charDb.saveDecision(cooccurrences.get(0).getGroupNo(), comboDecision.getText());
					
					/*Save the rest of the Character tab */
					for (CoOccurrenceBean cbean : cooccurrences) {
						TermsDataBean tbean = new TermsDataBean();
						tbean.setFrequency(Integer.parseInt(cbean.getFrequency().getText()));
						tbean.setGroupId(cbean.getGroupNo());
						tbean.setKeep(cbean.getKeep());
						tbean.setSourceFiles(cbean.getSourceFiles());
						
						/* The fun starts here! try and save the terms that are there in parentTermGroup*/
						if(cbean.getTerm1() != null && cbean.getTerm1().isTogglePosition()) {
							tbean.setTerm1(cbean.getTerm1().getTermText().getText());
						} else {
							tbean.setTerm1("");
						}
						
						if(cbean.getTerm2() != null && cbean.getTerm2().isTogglePosition()){
							tbean.setTerm2(cbean.getTerm2().getTermText().getText());
						} else {
							tbean.setTerm2("");
						}
						/* Add the termdatabean to the arraylist*/
						terms.add(tbean);
					}
					
					/* Save to terms table */
					charDb.saveTerms(terms);
					
				} catch (Exception exe) {
					LOGGER.error("Couldnt save the character tab details in MainForm" , exe);
					exe.printStackTrace();
				}
				
				String savedGroupName = groupsCombo.getText();
				processedGroups.put(savedGroupName, savedGroupName);				
				Set <String> processed = processedGroups.keySet();
				processedGroupsTable.removeAll();
				for (String groupName : processed) {
					TableItem item = new TableItem(processedGroupsTable, SWT.NONE);
					item.setText(groupName);
				}				

				/** Logic for remaining terms goes here */
				if (noOfTermGroups <= getNumberOfGroupsSaved() && isTermsNotGrouped()) {
					int choice = 0;
					if(charStatePopUp){
						choice = ApplicationUtilities.showPopUpWindow(
								ApplicationUtilities.getProperty("popup.charstates.info"),
								ApplicationUtilities.getProperty("popup.header.info") + " : " +
								ApplicationUtilities.getProperty("popup.header.character"), 
								SWT.YES | SWT.NO | SWT.CANCEL);
						charStatePopUp = false;
					} else {
						choice = ApplicationUtilities.showPopUpWindow(
								ApplicationUtilities.getProperty("popup.char.cont"),
								ApplicationUtilities.getProperty("popup.header.info") + " : " +
								ApplicationUtilities.getProperty("popup.header.character"), 
								SWT.YES | SWT.NO | SWT.CANCEL);
					}

					
				    switch (choice) {
				    case SWT.OK:
				    case SWT.YES:
				    	showRemainingTerms();
				    	break;
				    case SWT.CANCEL:
				    case SWT.NO:
				    case SWT.RETRY:
				    case SWT.ABORT:
				    case SWT.IGNORE:
				    default : //Do Nothing! :-)
				    }

				}
			}
		});
		
		
		Label label_1 = new Label(composite_8, SWT.SEPARATOR | SWT.VERTICAL);
		label_1.setBounds(510, 240, -6, 191);
		
		Group grpRemovedTerms = new Group(composite_8, SWT.NONE);
		grpRemovedTerms.setText("Removed Terms");
		grpRemovedTerms.setBounds(457, 26, 328, 208);
		
		removedScrolledComposite = new ScrolledComposite(grpRemovedTerms, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		removedScrolledComposite.setBounds(10, 24, 308, 174);
		removedScrolledComposite.setExpandHorizontal(true);
		removedScrolledComposite.setExpandVertical(true);
		
		removedTermsGroup = new Group(removedScrolledComposite, SWT.NONE);
		removedTermsGroup.setLayoutData(new RowData());
		
		removedScrolledComposite.setContent(removedTermsGroup);
		removedScrolledComposite.setMinSize(removedTermsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		Group grpDeleteAnyTerm = new Group(composite_8, SWT.NONE);
		grpDeleteAnyTerm.setText("Delete any term that you think doesn't belong to this group");
		grpDeleteAnyTerm.setBounds(0, 0, 451, 234);
		
		termsScrolledComposite = new ScrolledComposite(grpDeleteAnyTerm, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		termsScrolledComposite.setBounds(10, 41, 429, 183);
		termsScrolledComposite.setExpandHorizontal(true);
		termsScrolledComposite.setExpandVertical(true);
		
		termsGroup = new Group(termsScrolledComposite, SWT.NONE);
		termsScrolledComposite.setContent(termsGroup);
		termsScrolledComposite.setMinSize(termsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		Label lblContext = new Label(grpDeleteAnyTerm, SWT.NONE);
		lblContext.setBounds(10, 20, 55, 15);
		lblContext.setText("Context");
		
		Label lblTerm = new Label(grpDeleteAnyTerm, SWT.NONE);
		lblTerm.setBounds(72, 20, 55, 15);
		lblTerm.setText("Term");
		
		Label lblTerm_1 = new Label(grpDeleteAnyTerm, SWT.NONE);
		lblTerm_1.setBounds(231, 20, 105, 15);
		lblTerm_1.setText("Co-occurred Term");
		
		Label lblFrequency = new Label(grpDeleteAnyTerm, SWT.NONE);
		lblFrequency.setBounds(353, 20, 55, 15);
		lblFrequency.setText("Frequency");
		
		sortLabel = new Label(grpDeleteAnyTerm, SWT.NONE);
		sortLabel.setImage(SWTResourceManager.getImage(MainForm.class, "/fna/parsing/down.jpg"));		
		sortLabel.setBounds(408, 10, 31, 25);
		sortLabel.setToolTipText("Sort by frequency");
		sortLabel.addMouseListener(new MouseListener() {
			public void mouseDown(MouseEvent me){
				ArrayList <CoOccurrenceBean> cooccurences = null;
				Rectangle tempCoordinates = null;
					cooccurences = 
						((CharacterGroupBean)groupInfo.get(groupsCombo.getText())).getCooccurrences();
					int selectionIndex = groupsCombo.getSelectionIndex();
					int size = cooccurences.size();
					CoOccurrenceBean [] cbeans = new CoOccurrenceBean[size];
					int i = 0, j = 0, k = 0;
					for (CoOccurrenceBean cbean: cooccurences) {
						cbeans[i++] = cbean;
					}
					
					boolean toSort = true;
					int firstFrequency = cbeans[0].getFrequency().getBounds().y;
					int lastFrequency = cbeans[size-1].getFrequency().getBounds().y;
					if(firstFrequency<lastFrequency) {
						toSort = true;
					} else if (firstFrequency>lastFrequency){
						toSort = false;
					}
					
					for (i = 0, j = size-1, k = size-1; i < size/2; i++, j--, k-=2){
						CoOccurrenceBean beanFirst = cbeans[i];
						CoOccurrenceBean beanLast = cbeans[j];
						if(beanFirst.getTerm1() != null 
								&& beanFirst.getTerm2() != null
								&& beanLast.getTerm1() != null
								&& beanLast.getTerm2() != null) {
							/* Swap coordinates of radio button */
							tempCoordinates = beanFirst.getContextButton().getBounds();
							beanFirst.getContextButton().setBounds(beanLast.getContextButton().getBounds());
							beanLast.getContextButton().setBounds(tempCoordinates);
							
							/* Swap Frequencies */						
							tempCoordinates = null;
							tempCoordinates = beanFirst.getFrequency().getBounds();
							beanFirst.getFrequency().setBounds(beanLast.getFrequency().getBounds());
							beanLast.getFrequency().setBounds(tempCoordinates);
							
							
							if (toSort) {
								/*Sort Ascending*/
									/* Swap Group 1 */
									tempCoordinates = beanFirst.getTerm1().getTermGroup().getBounds();
									tempCoordinates.y += k * standardIncrement;
									beanFirst.getTerm1().getTermGroup().setBounds(tempCoordinates);
									
									tempCoordinates = beanLast.getTerm1().getTermGroup().getBounds();
									tempCoordinates.y -= k * standardIncrement;						
									beanLast.getTerm1().getTermGroup().setBounds(tempCoordinates);
									
									/* Swap Group 2 */
									tempCoordinates = beanFirst.getTerm2().getTermGroup().getBounds();
									tempCoordinates.y += k * standardIncrement;
									beanFirst.getTerm2().getTermGroup().setBounds(tempCoordinates);
									
									tempCoordinates = beanLast.getTerm2().getTermGroup().getBounds();
									tempCoordinates.y -= k * standardIncrement;						
									beanLast.getTerm2().getTermGroup().setBounds(tempCoordinates);

								
							} else {
								/* Sort Descending*/
								/* Swap Group 1 */
									tempCoordinates = beanFirst.getTerm1().getTermGroup().getBounds();
									tempCoordinates.y -= k * standardIncrement;
									beanFirst.getTerm1().getTermGroup().setBounds(tempCoordinates);
									
									tempCoordinates = beanLast.getTerm1().getTermGroup().getBounds();
									tempCoordinates.y += k * standardIncrement;						
									beanLast.getTerm1().getTermGroup().setBounds(tempCoordinates);
									
									/* Swap Group 2 */
									tempCoordinates = beanFirst.getTerm2().getTermGroup().getBounds();
									tempCoordinates.y -= k * standardIncrement;
									beanFirst.getTerm2().getTermGroup().setBounds(tempCoordinates);
									
									tempCoordinates = beanLast.getTerm2().getTermGroup().getBounds();
									tempCoordinates.y += k * standardIncrement;						
									beanLast.getTerm2().getTermGroup().setBounds(tempCoordinates);


							}
						}
						
					}
						
					if(sortedBy[selectionIndex]) {
						/*Sort Ascending*/
						sortedBy[selectionIndex] = false;
						sortLabel.setImage(SWTResourceManager.getImage(MainForm.class, "/fna/parsing/up.jpg"));
						
					} else {
						/* Sort Descending*/
						sortedBy[selectionIndex] = true;
						sortLabel.setImage(SWTResourceManager.getImage(MainForm.class, "/fna/parsing/down.jpg"));	
						
					}
					
					termsScrolledComposite.setMinSize(termsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
					removedScrolledComposite.setMinSize(removedTermsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
					
			}
			public void mouseUp(MouseEvent me){	}
			public void mouseDoubleClick(MouseEvent me){	}
		});
		Button btnViewGraphVisualization = new Button(composite_8, SWT.NONE);
		btnViewGraphVisualization.setBounds(457, 0, 159, 25);
		btnViewGraphVisualization.setText("View Graph Visualization");
		btnViewGraphVisualization.setToolTipText("Click to view the graph visualization of the terms that have co-occurred");
		
		/*Removed terms test*/
/*		Button btnRemovedTerms = new Button(composite_8, SWT.NONE);
		btnRemovedTerms.setBounds(622, 0, 105, 25);
		btnRemovedTerms.setText("Removed Terms");
		btnRemovedTerms.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				showRemainingTerms();
			}
		});*/
		
		btnViewGraphVisualization.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				CoOccurrenceGraph.viewGraph(Registry.TargetDirectory+
				ApplicationUtilities.getProperty("CHARACTER-STATES") + "\\" + groupsCombo.getText()+".xml", groupsCombo.getText());
			}
		});
		/////////////////////////////////////////////////////////////////////////////////////////////
		/*
		 * Finalizer tab */

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
				finalizerTable.getSelection()[0].getText(1).trim();				
				
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
		transformationNameColumnTableColumn_1_2.setText("File");

		final TableColumn transformationFileColumnTableColumn_1_2 = new TableColumn(finalizerTable, SWT.NONE);
		transformationFileColumnTableColumn_1_2.setWidth(376);
		transformationFileColumnTableColumn_1_2.setText("Taxon Name");

		final Button startFinalizerButton = new Button(composite_5, SWT.NONE);
		startFinalizerButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e){
				finalizerTable.removeAll();
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
		startFinalizerButton.setBounds(618, 385, 65, 23);
		startFinalizerButton.setText("Start");

		final Button clearFinalizerButton = new Button(composite_5, SWT.NONE);
		clearFinalizerButton.setBounds(689, 385, 65, 23);
		clearFinalizerButton.setText("Clear");
		clearFinalizerButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				finalizerTable.removeAll();
			}
		});

		finalizerProgressBar = new ProgressBar(composite_5, SWT.NONE);
		finalizerProgressBar.setVisible(false);
		finalizerProgressBar.setBounds(10, 387, 522, 17);
		
		Button btnLoad_2 = new Button(composite_5, SWT.NONE);
		btnLoad_2.setBounds(552, 385, 58, 23);
		btnLoad_2.setText("Load");
		btnLoad_2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				loadFileInfo(finalizerTable, Registry.TargetDirectory + 
						ApplicationUtilities.getProperty("FINAL"));
			}
		});

/*		final TabItem glossaryTabItem = new TabItem(tabFolder, SWT.NONE);
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
		reportGlossaryButton.setText("Report");*/

		final Label logoLabel = new Label(shell, SWT.NONE);
		String ls = System.getProperty("line.separator");
		logoLabel.setText(ApplicationUtilities.getProperty("application.instructions"));
		logoLabel.setBounds(10, 485, 530, 83);

		final Label label = new Label(shell, SWT.NONE);
		label.setBackgroundImage(SWTResourceManager.getImage(MainForm.class, 
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
	
	private void startExtraction() throws Exception {
		
		ProcessListener listener = new ProcessListener(extractionTable, extractionProgressBar, shell.getDisplay());
		VolumeExtractor ve = new VolumeExtractor(Registry.SourceDirectory, Registry.TargetDirectory, listener);
		ve.start();
	}
	
	
	private void startVerification() {
		ProcessListener listener = new ProcessListener(verificationTable, verificationProgressBar, shell.getDisplay());
		VolumeVerifier vv = new VolumeVerifier(listener);
		vv.start();
	}
	
	private void clearVerification() {		
		verificationTable.removeAll();
	}
	
	private void startTransformation() {
		ProcessListener listener = new ProcessListener(transformationTable, transformationProgressBar, shell.getDisplay());
		VolumeTransformer vt = new VolumeTransformer(listener, dataPrefixCombo.getText().replaceAll("-", "_").trim());
		vt.start();
	}
	
	private void startPreMarkUp() {
		ProcessListener listener = 
			new ProcessListener(transformationTable, transformationProgressBar, 
					shell.getDisplay());
		/* Need to clarify perlLog, and seeds new arraylist from Dr Hong*/ 
		Type3Transformation preMarkUp = 
			new Type3Transformation(listener, shell.getDisplay(), 
					null, dataPrefixCombo.getText().replaceAll("-", "_").trim(), new ArrayList());
		preMarkUp.start();
	}
	
	private void startType4Transformation () {
		ProcessListener listener = 
			new ProcessListener(transformationTable, transformationProgressBar, 
					shell.getDisplay());
		Type4Transformer transformer = new Type4Transformer(listener, dataPrefixCombo.getText().replaceAll("-", "_").trim());
		transformer.start();
	}
	private void clearTransformation() {
		transformationTable.removeAll();
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

		String workdir = Registry.TargetDirectory;
		String todofoldername = ApplicationUtilities.getProperty("DESCRIPTIONS");
		String databasename = ApplicationUtilities.getProperty("database.name");
		ProcessListener listener = new ProcessListener(markupTable, markupProgressBar, shell.getDisplay());
		
		VolumeDehyphenizer vd = new VolumeDehyphenizer(listener, workdir, todofoldername,
				databasename, shell.getDisplay(), markUpPerlLog, dataPrefixCombo.getText().replaceAll("-", "_").trim());
		vd.start();
	}
	
	private void startFinalize() {
		ProcessListener listener = new ProcessListener(finalizerTable, finalizerProgressBar, shell.getDisplay());
		Connection conn = null;
		String database=ApplicationUtilities.getProperty("database.name");
		String username=ApplicationUtilities.getProperty("database.username");
		String password=ApplicationUtilities.getProperty("database.password");
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		VolumeFinalizer vf = new VolumeFinalizer(listener, dataPrefixCombo.getText().replaceAll("-", "_").trim(), conn);
		vf.start();
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
			LOGGER.error("Exception encountered in removing tags from database in MainForm:removeMarkup", exe);
			exe.printStackTrace();
		}

		ProcessListener listener = new ProcessListener(markupTable, markupProgressBar, shell.getDisplay());
		VolumeMarkup vm = new VolumeMarkup(listener, shell.getDisplay(), null, this.dataPrefixCombo.getText().replaceAll("-", "_").trim());
		vm.update();
	}
	
	private void loadTags() {
		loadTagTable();
		tagListCombo.add("PART OF LAST SENTENCE"); //part of the last sentence
		
		try {
			 mainDb.loadTagsData(tagListCombo, modifierListCombo);
			} catch (Exception exe) {
				LOGGER.error("Exception encountered in loading tags from database in MainForm:loadTags", exe);
				exe.printStackTrace();
		    }

	}

	private void loadTagTable() {
		tagTable.removeAll();

		try {
			 if(mainDb.loadTagsTableData(tagTable)==0){
				 //StateCollectorBootstrapper sct = new StateCollectorBootstrapper(ApplicationUtilities.getProperty("database.name"), MainForm.dataPrefixCombo.getText().replaceAll("-", "_").trim()); /*TODO: debug*/
				Connection conn = null;
				try{
					if(conn == null){
						Class.forName("com.mysql.jdbc.Driver");
						String URL = ApplicationUtilities.getProperty("database.url");
						conn = DriverManager.getConnection(URL);
					}
				}catch(Exception e){
					e.printStackTrace();
				}
				 
				 
				 StateCollectorTest sct = new StateCollectorTest(conn, this.dataPrefixCombo.getText().replaceAll("-", "_").trim()); /*using learned semanticroles only*/
				 sct.collect();
				 sct.saveStates();
				 sct.grouping4GraphML();
				 conn.close();
			 }
		} catch (Exception exe) {
				LOGGER.error("Exception encountered in loading tags from database in MainForm:loadTags", exe);
				exe.printStackTrace();
		}

	}
	
	private void updateContext(int sentid) throws ParsingException {
		contextStyledText.setText("");
		tagListCombo.setText("");
		
		
		try {
			mainDb.updateContextData(sentid, contextStyledText);

		} catch (Exception e) {
			LOGGER.error("Exception encountered in loading tags from database in MainForm:updateContext", e);
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
			LOGGER.error("Exception encountered in loading tags from database in MainForm:saveTag", exe);
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
		
		if (dataPrefixCombo != null && dataPrefixCombo.getText().replaceAll("-", "_").trim().equals("")) {
			
			messageText.append(ApplicationUtilities.getProperty("popup.error.dataset"));
			
		}
		
		if (messageText.length() != 0) {
			messageText.append(ApplicationUtilities.getProperty("popup.error.info"));
			ApplicationUtilities.showPopUpWindow(messageText.toString(), 
					ApplicationUtilities.getProperty("popup.header.missing"), SWT.ICON_WARNING);						
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
	
	/**
	 * This function will set the decisions for the character tab
	 */
	private void setCharacterTabDecisions() {
		ArrayList<String> decisions = new ArrayList<String> ();
		
		try {
			
			charDb.getDecisionCategory(decisions);
		} catch (Exception exe) {
			LOGGER.error("Couldnt retrieve decision names" , exe);
			exe.printStackTrace();
		}
		int count = 0;
		String [] strDecisions = new String[decisions.size()];
		for (String decision : decisions) {
			strDecisions[count++] = decision;
		}
		comboDecision.setItems(strDecisions);
		comboDecision.setText(strDecisions[0]);
	}
	
	/**
	 * This function will prepare the character tab for display of co-occured terms
	 */
	private void setCharactertabGroups() {
		File directory = new File(Registry.TargetDirectory+"\\"+
				ApplicationUtilities.getProperty("CHARACTER-STATES"));
		File [] files = directory.listFiles();
		/**Update the global variable with number of groups**/
		noOfTermGroups = files.length;
		
		String [] fileNames = new String[files.length];
		int count = 0, removedEdgesSize = removedEdges.size();
		sortedBy = new boolean [fileNames.length];
		for (File group : files) {
			sortedBy[count] = true;
			fileNames[count] = group.getName().substring(0, group.getName().indexOf(".xml"));
			if (removedEdgesSize == 0){
				/* RemovedEdges HashMap is intialized to store removed edges when
				 *  the user interacts with the terms*/
				removedEdges.put(fileNames[count], new ArrayList<String>());
			}
			
			count++;
		}
		
		groupsCombo.setItems(fileNames);		
		groupsCombo.setText(fileNames[0]);
		groupsCombo.select(0);
		
	}
	
	/***
	 * This function loads the character tab with the co-occurred terms
	 */
	private void loadTerms() {
		String groupName = groupsCombo.getText();
		CharacterGroupBean charGrpBean = groupInfo.get(groupName);
		int selectionIndex = groupsCombo.getSelectionIndex();
		if(charGrpBean == null || !charGrpBean.isSaved()){
			showTerms();
			//restore edges if they were removed but the group was not processed (saved)
			restoreUnsavedEdges();
			sortLabel.setImage(SWTResourceManager.getImage(MainForm.class, "/fna/parsing/down.jpg"));
			
		} else {
			/* Load it from memory! */
			termsGroup = null;
			termsGroup = new Group(termsScrolledComposite, SWT.NONE);
			termsGroup.setLayoutData(new RowData());
			termsScrolledComposite.setContent(termsGroup);
			termsScrolledComposite.setMinSize(termsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			
			removedTermsGroup = null;
			removedTermsGroup = new Group(removedScrolledComposite, SWT.NONE);
			removedTermsGroup.setLayoutData(new RowData());
			removedScrolledComposite.setContent(removedTermsGroup);
			removedScrolledComposite.setMinSize(removedTermsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			
			ArrayList<CoOccurrenceBean> cooccurrences = (ArrayList<CoOccurrenceBean>)charGrpBean.getCooccurrences();
			
			if(cooccurrences.size() > 5) {
				
				/* If the number of rows is more than what is displayed, resize the group*/
				RowData rowdata = (RowData)termsGroup.getLayoutData();
				rowdata.height = cooccurrences.size() * 36;
				termsGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
		        Rectangle rect = termsGroup.getBounds();
		        rect.height = cooccurrences.size() * 36;
		        termsGroup.setBounds(rect);
				
				
				rowdata = (RowData)removedTermsGroup.getLayoutData();
				rowdata.height = cooccurrences.size() * 36;
				removedTermsGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
		        rect = removedTermsGroup.getBounds();
		        rect.height = cooccurrences.size() * 36;
		        removedTermsGroup.setBounds(rect);
			}
			
			/*Set the decision if it was saved*/
			comboDecision.setText(charGrpBean.getDecision());
			
			if (cooccurrences.size() != 0) {
				for (CoOccurrenceBean cbean : cooccurrences) {
					cbean.getContextButton().setParent(termsGroup);
					cbean.getContextButton().setSelection(false);
					cbean.getFrequency().setParent(termsGroup);
					if (cbean.getTerm1() != null){
						if (cbean.getTerm1().isTogglePosition()) {
							cbean.getTerm1().getTermGroup().setParent(termsGroup);
							cbean.getTerm1().setParentGroup(termsGroup);
							cbean.getTerm1().setDeletedGroup(removedTermsGroup);
						} else {
							cbean.getTerm1().getTermGroup().setParent(removedTermsGroup);
							cbean.getTerm1().setParentGroup(termsGroup);
							cbean.getTerm1().setDeletedGroup(removedTermsGroup);
						}
					}

					if (cbean.getTerm2() != null) {
						if (cbean.getTerm2().isTogglePosition()) {
							cbean.getTerm2().getTermGroup().setParent(termsGroup);
							cbean.getTerm2().setParentGroup(termsGroup);
							cbean.getTerm2().setDeletedGroup(removedTermsGroup);
						} else {
							cbean.getTerm2().getTermGroup().setParent(removedTermsGroup);
							cbean.getTerm2().setParentGroup(termsGroup);
							cbean.getTerm2().setDeletedGroup(removedTermsGroup);
						}
					}

				}
			}
			
			/*Resize the groups*/
			termsScrolledComposite.setMinSize(termsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			removedScrolledComposite.setMinSize(removedTermsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			
				/** Show the correct sort order image */		
				if(sortedBy[selectionIndex]) {
					sortLabel.setImage(SWTResourceManager.getImage(MainForm.class, "/fna/parsing/down.jpg"));			
				} else {
					sortLabel.setImage(SWTResourceManager.getImage(MainForm.class, "/fna/parsing/up.jpg"));	
				
			}
			
		}
		
	}
	
	private void loadProcessedGroups() {
		try {
			ArrayList<String> processedGroupsList = charDb.getProcessedGroups();
			processedGroupsTable.removeAll();
			for (String groupName : processedGroupsList){
				processedGroups.put(groupName, groupName);
				TableItem item = new TableItem(processedGroupsTable, SWT.NONE);
				item.setText(groupName);
				
			}
		} catch (Exception exe) {
			LOGGER.error("Couldnt retrieve processedGroups terms" , exe);
			exe.printStackTrace();
		}
	}
	
	private void showTerms() {
		ArrayList<TermsDataBean> terms = null;
		boolean saved = false;
		term1.y = 10;
		term2.y = 10;
		contextRadio.y = 20;
		frequencyLabel.y = 20;
		ArrayList<CoOccurrenceBean> cooccurrences = new ArrayList<CoOccurrenceBean>();
		String decision = "";

		termsGroup = null;
		termsGroup = new Group(termsScrolledComposite, SWT.NONE);
		termsGroup.setLayoutData(new RowData());
		termsScrolledComposite.setContent(termsGroup);
		termsScrolledComposite.setMinSize(termsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		removedTermsGroup = null;
		removedTermsGroup = new Group(removedScrolledComposite, SWT.NONE);
		removedTermsGroup.setLayoutData(new RowData());
		removedScrolledComposite.setContent(removedTermsGroup);
		removedScrolledComposite.setMinSize(removedTermsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				
		try {
			terms = charDb.getTerms(groupsCombo.getText());
			if(terms.size() != 0) {
					int groupId = ((TermsDataBean)terms.get(0)).getGroupId();
					decision = charDb.getDecision(groupId);
			}
			
		} catch (Exception exe) {
			LOGGER.error("Couldnt retrieve co-occurring terms" , exe);
			exe.printStackTrace();
		}

		if (terms.size() > 5) {
			
			RowData rowdata = (RowData)termsGroup.getLayoutData();
			rowdata.height = terms.size() * 36;
			termsGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
	        Rectangle rect = termsGroup.getBounds();
	        rect.height = terms.size() * 36;
	        termsGroup.setBounds(rect);
			
			
			rowdata = (RowData)removedTermsGroup.getLayoutData();
			rowdata.height = terms.size() * 36;
			removedTermsGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
	        rect = removedTermsGroup.getBounds();
	        rect.height = terms.size() * 36;
	        removedTermsGroup.setBounds(rect);
			
		}
		
		if (terms.size() != 0) {
			
			for (final TermsDataBean tbean : terms) {
				CoOccurrenceBean cbean = new CoOccurrenceBean();
				if (!(tbean.getTerm1() == null) && !tbean.getTerm1().equals("")) {
					
				Group term1Group = new Group(termsGroup, SWT.NONE);
				term1Group.setToolTipText(tbean.getTerm1());
				term1Group.setBounds(term1.x, term1.y, term1.width, term1.height);
				cbean.setTerm1(new TermBean(term1Group, removedTermsGroup, true, tbean.getTerm1()));

				}
				
				if (!(tbean.getTerm2() == null) && !tbean.getTerm2().equals("")) {
					Group term2Group = new Group(termsGroup, SWT.NONE);	
					term2Group.setToolTipText(tbean.getTerm2());
					term2Group.setBounds(term2.x, term2.y, term2.width, term2.height);
					cbean.setTerm2(new TermBean(term2Group, removedTermsGroup, true, tbean.getTerm2()));
				}
				
				cbean.setGroupNo(tbean.getGroupId());
				cbean.setSourceFiles(tbean.getSourceFiles());
				cbean.setKeep(tbean.getKeep());
				
				final Button button = new Button(termsGroup, SWT.RADIO);
				button.setBounds(contextRadio.x, contextRadio.y, contextRadio.width, contextRadio.height);
				button.setToolTipText("Select to see the context sentences");
				button.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(final SelectionEvent e) {
						if (button.getSelection()) {
							contextTable.removeAll();
							try {
								ArrayList<ContextBean> contexts = charDb.getContext(tbean.getSourceFiles());
								
								for (ContextBean cbean : contexts){
									TableItem item = new TableItem(contextTable, SWT.NONE);
									item.setText(new String[]{cbean.getSourceText(), cbean.getSentence()});
								}
								
								
							} catch (Exception exe) {
								LOGGER.error("Couldnt retrieve sentences terms" , exe);
								exe.printStackTrace();
							}
							
						}

					}
				});

				cbean.setContextButton(button);
				if (decision != null && !decision.equals("")){
					comboDecision.setText(decision);
					saved = true;
				}
				
				Label label = new Label(termsGroup, SWT.NONE);
				label.setBounds(frequencyLabel.x, frequencyLabel.y, frequencyLabel.width, frequencyLabel.height);
				label.setText(tbean.getFrequency()+ "");
				label.setToolTipText("Frequency of co-occurrence");
				cbean.setFrequency(label);
				cooccurrences.add(cbean);
				
				term1.y += standardIncrement;
				term2.y += standardIncrement;
				contextRadio.y += standardIncrement;
				frequencyLabel.y += standardIncrement;
			}
		}
		
		termsScrolledComposite.setMinSize(termsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		removedScrolledComposite.setMinSize(removedTermsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		CharacterGroupBean charGrpBean = new CharacterGroupBean(cooccurrences, groupsCombo.getText(), saved);
		groupInfo.put(groupsCombo.getText(), charGrpBean);
	}

	/**
	 * @return the groupInfo
	 */
	public static HashMap<String, CharacterGroupBean> getGroupInfo() {
		return groupInfo;
	}
	
	private void restoreUnsavedEdges(){
		String group = groupsCombo.getText();
		if (!groupInfo.get(group).isSaved()) {
			ArrayList <String> edges = removedEdges.get(group);
			if (edges != null) {
				for (String edgeNodes : edges){
					String [] nodes = edgeNodes.split(",");
					if(nodes[0] != null && !nodes[0].equals("") && nodes[1] != null && !nodes[1].equals("") ) {
						ManipulateGraphML.insertEdge(new GraphNode(nodes[0]), new GraphNode(nodes[1]), 
								Registry.TargetDirectory+
									ApplicationUtilities.getProperty("CHARACTER-STATES")+ "\\"+ group + ".xml");
					}

				}
			}
	
		}
	}

	/**
	 * @return the removedEdges
	 */
	public static HashMap<String, ArrayList<String>> getRemovedEdges() {
		return removedEdges;
	}
	
	/* This function loads the files, if any, to the respective tabs*/
	private void loadFileInfo(Table table, String directoryPath){
		File directory = new File(directoryPath);
		File [] files = directory.listFiles();
		int count = 0;
		int [] fileNumbers = new int[files.length];
		/* Will need to change this logic if the filenames are no long numbers but strings */
		for (File file : files) {
			String fileName = file.getName();
			fileNumbers[count++] = Integer.parseInt(fileName.substring(0, fileName.indexOf(".xml")));
		}
		
		Arrays.sort(fileNumbers);
		
		for (int fileNumber : fileNumbers) {			
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(new String [] {fileNumber+"", fileNumber+".xml"});
		}
		
		
	}
	
	//removedTermsGroups
	//0. Copy and create a removed terms list
	//1. Check if there are terms remaining - a function that returns true/false
	//2. Create a new group with the number one more than the existing groups.
	//3. Create an xml for the group in 2
	//4. Prepare the GUI for displaying the terms : 
	//      a) Ignore repeated terms 
	//      b)Ignore if terms were reinserted in some existing groups
	//5. Save operation - call the same function as before but create a termsdatabean and pass it along.
	//6. Go to 1
	
	/**
	 * This function creates remaining terms group for the character tab.
	 */
	private void showRemainingTerms() {
		ArrayList<TermsDataBean> terms = null;
		term1.y = 10;
		term2.y = 10;
		contextRadio.y = 20;
		frequencyLabel.y = 20;
		ArrayList<CoOccurrenceBean> cooccurrences = new ArrayList<CoOccurrenceBean>();
		String decision = "";

		termsGroup = null;
		termsGroup = new Group(termsScrolledComposite, SWT.NONE);
		termsGroup.setLayoutData(new RowData());
		termsScrolledComposite.setContent(termsGroup);
		termsScrolledComposite.setMinSize(termsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		removedTermsGroup = null;
		removedTermsGroup = new Group(removedScrolledComposite, SWT.NONE);
		removedTermsGroup.setLayoutData(new RowData());
		removedScrolledComposite.setContent(removedTermsGroup);
		removedScrolledComposite.setMinSize(removedTermsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		
		int newGroupNumber = groupsCombo.getItemCount()+1;
		String newGroup = "Group_" + newGroupNumber;
		groupsCombo.add(newGroup);
		groupsCombo.setText(newGroup);
		groupsCombo.select(groupsCombo.getItemCount()-1);
		/*Generate the graph XML*/
		ArrayList <ArrayList> groups = null;
		/* Create the arraylist to create new terms list*/
		terms = getRemovedTerms(newGroupNumber);	
		/* Create the arraylist for Graph Visualization*/
		groups = createGraphML(terms);
		/* Create the GraphML */
		new GraphMLOutputter(false).output(groups, newGroupNumber);
		/* Create an entry in the removedEdges hashmap for the new group*/
		removedEdges.put(newGroup, new ArrayList<String>());
		/* Add an entry to the sort order */
		boolean [] newSortedBy = new boolean [sortedBy.length+1];
		for(int i = 0 ; i< sortedBy.length; i++) {
			newSortedBy[i] = sortedBy[i];
		}
		sortedBy = newSortedBy;

		if (terms.size() > 5) {
			
			RowData rowdata = (RowData)termsGroup.getLayoutData();
			rowdata.height = terms.size() * 36;
			termsGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
	        Rectangle rect = termsGroup.getBounds();
	        rect.height = terms.size() * 36;
	        termsGroup.setBounds(rect);
			
			
			rowdata = (RowData)removedTermsGroup.getLayoutData();
			rowdata.height = terms.size() * 36;
			removedTermsGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
	        rect = removedTermsGroup.getBounds();
	        rect.height = terms.size() * 36;
	        removedTermsGroup.setBounds(rect);
			
		}
		
		if (terms.size() != 0) {
			
			for (final TermsDataBean tbean : terms) {
				CoOccurrenceBean cbean = new CoOccurrenceBean();
				if (!(tbean.getTerm1() == null) && !tbean.getTerm1().equals("")) {
					
				Group term1Group = new Group(termsGroup, SWT.NONE);
				term1Group.setToolTipText(tbean.getTerm1());
				term1Group.setBounds(term1.x, term1.y, term1.width, term1.height);
				cbean.setTerm1(new TermBean(term1Group, removedTermsGroup, true, tbean.getTerm1()));

				}
				
				if (!(tbean.getTerm2() == null) && !tbean.getTerm2().equals("")) {
					Group term2Group = new Group(termsGroup, SWT.NONE);	
					term2Group.setToolTipText(tbean.getTerm2());
					term2Group.setBounds(term2.x, term2.y, term2.width, term2.height);
					cbean.setTerm2(new TermBean(term2Group, removedTermsGroup, true, tbean.getTerm2()));
				}
				
				cbean.setGroupNo(tbean.getGroupId());
				cbean.setSourceFiles(tbean.getSourceFiles());
				cbean.setKeep(tbean.getKeep());
				
				final Button button = new Button(termsGroup, SWT.RADIO);
				button.setBounds(contextRadio.x, contextRadio.y, contextRadio.width, contextRadio.height);
				button.setToolTipText("Select to see the context sentences");
				button.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(final SelectionEvent e) {
						if (button.getSelection()) {
							contextTable.removeAll();
							try {
								ArrayList<ContextBean> contexts = charDb.getContext(tbean.getSourceFiles());
								
								for (ContextBean cbean : contexts){
									TableItem item = new TableItem(contextTable, SWT.NONE);
									item.setText(new String[]{cbean.getSourceText(), cbean.getSentence()});
								}								
								
							} catch (Exception exe) {
								LOGGER.error("Couldnt retrieve sentences terms" , exe);
								exe.printStackTrace();
							}
							
						}

					}
				});

				cbean.setContextButton(button);
				if (decision != null && !decision.equals("")){
					comboDecision.setText(decision);
				}
				
				Label label = new Label(termsGroup, SWT.NONE);
				label.setBounds(frequencyLabel.x, frequencyLabel.y, frequencyLabel.width, frequencyLabel.height);
				label.setText(tbean.getFrequency()+ "");
				label.setToolTipText("Frequency of co-occurrence");
				cbean.setFrequency(label);
				cooccurrences.add(cbean);
				
				term1.y += standardIncrement;
				term2.y += standardIncrement;
				contextRadio.y += standardIncrement;
				frequencyLabel.y += standardIncrement;
			}
		}
		
		termsScrolledComposite.setMinSize(termsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		removedScrolledComposite.setMinSize(removedTermsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		CharacterGroupBean charGrpBean = new CharacterGroupBean(cooccurrences, groupsCombo.getText(), false);
		groupInfo.put(groupsCombo.getText(), charGrpBean);
		
		/* Save the newly formed group to db*/
		try {
			charDb.saveTerms(terms);
		} catch(Exception exe){
			exe.printStackTrace();
		}
	}
	
	/***
	 * This function creates the graph for the remaining character tab terms
	 * @param terms
	 * @return
	 */
	private ArrayList<ArrayList> createGraphML(ArrayList<TermsDataBean> terms) {
		ArrayList<ArrayList> group = new ArrayList<ArrayList>();
		ArrayList<ArrayList> groups = new ArrayList<ArrayList>();
		for (TermsDataBean tbean : terms){
			ArrayList coTerms = new ArrayList();
			if(tbean.getTerm1() != null) {
				coTerms.add(tbean.getTerm1());
			} else {
				coTerms.add("");
			}
			if(tbean.getTerm2() != null) {
				coTerms.add(tbean.getTerm2());
			} else {
				coTerms.add("");
			}
			if(coTerms.size() != 0){
				groups.add(coTerms);
			}
		}
		group.add(groups);
		return group;
	}
	
	/***
	 * This function will get all the removed terms and form the pool
	 * @param groupNo
	 * @return
	 */
	private ArrayList<TermsDataBean> getRemovedTerms(int groupNo) {
		ArrayList <TermsDataBean> terms = new ArrayList<TermsDataBean>();

			Set <String> keys = groupInfo.keySet();
			for (String key : keys){	
				CharacterGroupBean charBean = groupInfo.get(key);
				if (charBean.isSaved()){
					terms.addAll(getTermsInformation (charBean, groupNo));
				}
				
			}			

		
		return terms;
	}
	/**
	 * This is a helper internal function that gets the specific removed 
	 * terms information from the character tab's removed terms'
	 * @param charGroupBean
	 * @param groupNo
	 * @return
	 */
	private ArrayList <TermsDataBean> getTermsInformation (CharacterGroupBean charGroupBean, int groupNo){
		
		ArrayList <TermsDataBean> terms = new ArrayList<TermsDataBean>();	
		ArrayList <CoOccurrenceBean> cooccurrences = charGroupBean.getCooccurrences();
		
		for (CoOccurrenceBean  bean : cooccurrences) {
			
			TermsDataBean tbean = new TermsDataBean();
			tbean.setFrequency(Integer.parseInt(bean.getFrequency().getText()));
			tbean.setSourceFiles(bean.getSourceFiles());
			tbean.setGroupId(groupNo);
			if(bean.getTerm1() != null) {
				if (!bean.getTerm1().isTogglePosition()) {
					tbean.setTerm1(bean.getTerm1().getTermText().getText());
					/* Remove the term from the original group of removed terms*/
					bean.setTerm1(null);
					
			    } else {
			    	tbean.setTerm1("");
			    }
			} else {
		    	tbean.setTerm1("");
		    }

			if (bean.getTerm2() != null){
				if(!bean.getTerm2().isTogglePosition()){
					tbean.setTerm2(bean.getTerm2().getTermText().getText());
					/* Remove the term from the original group of removed terms*/
					bean.setTerm2(null);
					
			    } else {
			    	tbean.setTerm2("");
			    }
			} else {
		    	tbean.setTerm2("");
		    }

			if (tbean.getTerm1() != null && tbean.getTerm2() != null) {
				if (!tbean.getTerm1().equals("")|| 
						!tbean.getTerm2().equals("")) {
					terms.add(tbean);
				}
			}
			
		}
		
		return terms;
	}
	
	/**
	 * This function checks how 
	 * many groups were saved
	 * @return
	 */  	
	private int getNumberOfGroupsSaved(){
		int returnValue = 0;
		Set <String>
		keys = groupInfo.keySet();
		for (String key : keys){
			CharacterGroupBean charGrpBean = groupInfo.get(key);
			if(charGrpBean.isSaved()) {
				returnValue ++;
			}
		}
		return returnValue;
	}
	
	/**
	 * This function checks if there are terms remaining that are not yet grouped.
	 * @return boolean
	 */
	
	private boolean isTermsNotGrouped(){
		boolean returnValue = false;
		Set <String>
		keys = groupInfo.keySet();
		for (String key : keys){
			CharacterGroupBean charGrpBean = groupInfo.get(key);
			ArrayList <CoOccurrenceBean> cooccurrences = charGrpBean.getCooccurrences();
			for (CoOccurrenceBean  bean : cooccurrences) {
				if((bean.getTerm1() != null && !bean.getTerm1().isTogglePosition()) ||
						(bean.getTerm2() != null && !bean.getTerm2().isTogglePosition())){
						returnValue = true;
						break;
				    } 
				}
			if(returnValue) {
				break;
			}
		}
		return returnValue;
	}
}
