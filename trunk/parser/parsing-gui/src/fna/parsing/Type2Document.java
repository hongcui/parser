 package fna.parsing;

import java.util.HashMap;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Combo;
import com.swtdesigner.SWTResourceManager;

import fna.beans.NomenclatureBean;

public class Type2Document {
	private Text text;
	private Text text_1;
	private Text text_2;
	private Text text_3;
	private Text text_4;
	private Text text_5;
	private Text text_6;
	private Text text_7;
	private Text text_9;
	private Text text_10;
	private Text text_11;
	private Text text_12;
	private Text text_13;
	private Text text_14;
	private Text text_15;
	private Text text_16;
	private Text text_17;
	private Text text_18;
	private Text text_19;
	private Text text_20;
	private Text text_21;
	private Text text_22;
	private Text text_23;
	private Text text_24;
	private Text text_25;
	private Text text_26;
	private Text text_27;
	private Text text_28;
	private Text text_33;
	private Text text_34;
	private Text text_35;
	private Text text_36;
	private Text text_37;
	private Text text_38;
	private Text text_39;
	private Text text_40;
	private Text text_41;
	private Text text_42;
	private Text text_43;
	private Text text_44;
	private Text text_45;
	private Text text_46;
	private Text text_47;
	private Text text_48;
	private Text text_49;
	private Text text_50;
	private Text text_51;
	private Text text_52;
	private Text text_53;
	private Text text_54;
	private Text text_55;
	private Text text_56;
	private Text text_57;
	private Text text_58;
	private Text text_59;
	private Text text_60;
	private Text text_8;
	private Text text_29;
	private Text text_30;
	private Text text_31;
	private Text text_32;
	private Text text_61;

	/* String for userinput while adding a row*/
	
	private static String identifier = null;
	private Group nomenclatureGroup = null;
	private ScrolledComposite nomenScrolledComposite = null;
	private HashMap<Integer, NomenclatureBean> nomenclatures = new HashMap<Integer, NomenclatureBean>();
	// This variable will count the number of instances of nomenclature beans on the UI Nomenclature tab
	private int count = 0;
	private Shell shlTypeDocument = null;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new Type2Document().showType2Document();

	}
	
    /**
     * @wbp.parser.entryPoint
     */
    public void showType2Document() {
		final Display display = Display.getDefault();
		
		shlTypeDocument = new Shell(display);
		shlTypeDocument.setText("Type 2 Document");
		shlTypeDocument.setSize(780, 634);
		shlTypeDocument.setLocation(display.getBounds().x+200, display.getBounds().y+100);
		
		Composite composite = new Composite(shlTypeDocument, SWT.NONE);
		composite.setBounds(0, 0, 759, 557);
		
		TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
		tabFolder.setBounds(10, 10, 742, 545);
		
		TabItem tbtmText = new TabItem(tabFolder, SWT.NONE);
		tbtmText.setText("Text");
		
		Group grpText = new Group(tabFolder, SWT.NONE);
		grpText.setText("Text");
		tbtmText.setControl(grpText);
		
		Label lblLeadingIndentionOf = new Label(grpText, SWT.NONE);
		lblLeadingIndentionOf.setBounds(10, 82, 374, 15);
		lblLeadingIndentionOf.setText("Leading indention of other paragraph:");
		
		text = new Text(grpText, SWT.BORDER);
		text.setBounds(422, 79, 76, 21);
		
		Label lblCharacters = new Label(grpText, SWT.NONE);
		lblCharacters.setBounds(504, 82, 85, 15);
		lblCharacters.setText("characters");
		
		Label lblSpacingBetweenCharacters = new Label(grpText, SWT.NONE);
		lblSpacingBetweenCharacters.setBounds(10, 116, 374, 15);
		lblSpacingBetweenCharacters.setText("Spacing between paragraphs:");
		
		text_1 = new Text(grpText, SWT.BORDER);
		text_1.setBounds(422, 113, 76, 21);
		
		Label lblLines = new Label(grpText, SWT.NONE);
		lblLines.setBounds(504, 116, 85, 15);
		lblLines.setText("lines");
		
		Label lblstParagraph = new Label(grpText, SWT.NONE);
		lblstParagraph.setBounds(10, 47, 374, 15);
		lblstParagraph.setText("1st Paragraph:");
		
		text_2 = new Text(grpText, SWT.BORDER);
		text_2.setBounds(422, 47, 76, 21);
		
		Label label_2 = new Label(grpText, SWT.NONE);
		label_2.setBounds(504, 47, 85, 15);
		label_2.setText("characters");
		
		Label lblEstimatedAverageLengths = new Label(grpText, SWT.NONE);
		lblEstimatedAverageLengths.setBounds(10, 154, 374, 15);
		lblEstimatedAverageLengths.setText("Estimated average length(s) of a line:");
		
		text_3 = new Text(grpText, SWT.BORDER);
		text_3.setBounds(422, 148, 76, 21);
		
		Label label_3 = new Label(grpText, SWT.NONE);
		label_3.setBounds(504, 154, 85, 15);
		label_3.setText("characters");
		
		Label lblPageNumberForms = new Label(grpText, SWT.NONE);
		lblPageNumberForms.setBounds(10, 194, 141, 15);
		lblPageNumberForms.setText("Page number forms:");
		
		text_4 = new Text(grpText, SWT.BORDER);
		text_4.setBounds(161, 191, 477, 21);
		
		Label label = new Label(grpText, SWT.NONE);
		label.setBounds(10, 237, 141, 15);
		label.setText("Page number forms:");
		
		Button btnCapitalized = new Button(grpText, SWT.RADIO);
		btnCapitalized.setBounds(169, 236, 90, 16);
		btnCapitalized.setText("Capitalized");
		
		Button btnAllCapital = new Button(grpText, SWT.RADIO);
		btnAllCapital.setBounds(263, 237, 90, 16);
		btnAllCapital.setText("ALL CAPITAL");
		
		text_5 = new Text(grpText, SWT.BORDER);
		text_5.setBounds(366, 231, 272, 21);
		
		Label lblFooterTokens = new Label(grpText, SWT.NONE);
		lblFooterTokens.setBounds(10, 283, 85, 15);
		lblFooterTokens.setText("Footer tokens:");
		
		Button btnHasFooters = new Button(grpText, SWT.CHECK);
		btnHasFooters.setBounds(123, 282, 93, 16);
		btnHasFooters.setText("Has footers");
		
		text_6 = new Text(grpText, SWT.BORDER);
		text_6.setBounds(222, 280, 98, 21);
		
		Label lblHeaderTokens = new Label(grpText, SWT.NONE);
		lblHeaderTokens.setBounds(337, 283, 85, 15);
		lblHeaderTokens.setText("Header tokens:");
		
		Button btnHasHeaders = new Button(grpText, SWT.CHECK);
		btnHasHeaders.setBounds(428, 282, 93, 16);
		btnHasHeaders.setText("Has headers");
		
		text_7 = new Text(grpText, SWT.BORDER);
		text_7.setBounds(523, 277, 98, 21);
		
		TabItem tbtmNomenclature = new TabItem(tabFolder, SWT.NONE);
		tbtmNomenclature.setText("Nomenclature");
		
		Group grpNomenclature = new Group(tabFolder, SWT.NONE);
		grpNomenclature.setText("Nomenclature");
		tbtmNomenclature.setControl(grpNomenclature);
		
		Label lblWhatIsIn = new Label(grpNomenclature, SWT.NONE);
		lblWhatIsIn.setBounds(10, 28, 111, 15);
		lblWhatIsIn.setText("What is in a name?");
		
		Label lblFamily = new Label(grpNomenclature, SWT.NONE);
		lblFamily.setBounds(233, 28, 55, 15);
		lblFamily.setText("Family");
		
		Label lblGenus = new Label(grpNomenclature, SWT.NONE);
		lblGenus.setBounds(399, 28, 55, 15);
		lblGenus.setText("Genus");
		
		Label lblSpecies = new Label(grpNomenclature, SWT.NONE);
		lblSpecies.setBounds(569, 28, 55, 15);
		lblSpecies.setText("Species");
		
		nomenScrolledComposite = new ScrolledComposite(grpNomenclature, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		nomenScrolledComposite.setBounds(10, 53, 714, 278);
		nomenScrolledComposite.setExpandHorizontal(true);
		nomenScrolledComposite.setExpandVertical(true);
		
		nomenclatureGroup = new Group(nomenScrolledComposite, SWT.NONE);
		nomenclatureGroup.setLayoutData(new RowData());
		
		Label lblName = new Label(nomenclatureGroup, SWT.NONE);
		lblName.setBounds(10, 20, 67, 15);
		lblName.setText("Name");
		
		Label lblAuthors = new Label(nomenclatureGroup, SWT.NONE);
		lblAuthors.setBounds(10, 65, 67, 15);
		lblAuthors.setText("Authors");
		
		Label lblDate = new Label(nomenclatureGroup, SWT.NONE);
		lblDate.setBounds(10, 110, 67, 15);
		lblDate.setText("Date");
		
		Label lblPublication = new Label(nomenclatureGroup, SWT.NONE);
		lblPublication.setBounds(10, 155, 67, 15);
		lblPublication.setText("Publication");
		
		Label lblTaxonRank = new Label(nomenclatureGroup, SWT.NONE);
		lblTaxonRank.setBounds(10, 200, 67, 15);
		lblTaxonRank.setText("Taxon Rank");
		

		Group group_2 = new Group(nomenclatureGroup, SWT.NONE);
		group_2.setBounds(100, 10, 182, 40);
		
		Button button = new Button(group_2, SWT.RADIO);
		button.setText("Yes");
		button.setBounds(10, 13, 39, 16);
		
		Button button_1 = new Button(group_2, SWT.RADIO);
		button_1.setText("No");
		button_1.setBounds(55, 13, 39, 16);
		
		text_14 = new Text(group_2, SWT.BORDER);
		text_14.setBounds(100, 11, 76, 21);
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_2, button, button_1, text_14, lblName));
		count++;
		
		///////////
		Group group_1 = new Group(nomenclatureGroup, SWT.NONE);
		group_1.setBounds(300, 10, 182, 40);
		
		Button button_2 = new Button(group_1, SWT.RADIO);
		button_2.setText("Yes");
		button_2.setBounds(10, 13, 39, 16);
		
		Button button_3 = new Button(group_1, SWT.RADIO);
		button_3.setText("No");
		button_3.setBounds(55, 13, 39, 16);
		
		text_15 = new Text(group_1, SWT.BORDER);
		text_15.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_1, button_2, button_3, text_15, lblName));
		count++;
		
		///////////////
		Group group_4 = new Group(nomenclatureGroup, SWT.NONE);
		group_4.setBounds(500, 10, 182, 40);
		
		Button button_4 = new Button(group_4, SWT.RADIO);
		button_4.setText("Yes");
		button_4.setBounds(10, 13, 39, 16);
		
		Button button_5 = new Button(group_4, SWT.RADIO);
		button_5.setText("No");
		button_5.setBounds(55, 13, 39, 16);
		
		text_16 = new Text(group_4, SWT.BORDER);
		text_16.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_4, button_4, button_5, text_16, lblName));
		count++;
		/////////
		
		Group group_5 = new Group(nomenclatureGroup, SWT.NONE);
		group_5.setBounds(100, 55, 182, 40);
		
		Button button_6 = new Button(group_5, SWT.RADIO);
		button_6.setText("Yes");
		button_6.setBounds(10, 13, 39, 16);
		
		Button button_7 = new Button(group_5, SWT.RADIO);
		button_7.setText("No");
		button_7.setBounds(55, 13, 39, 16);
		
		text_17 = new Text(group_5, SWT.BORDER);
		text_17.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_5, button_6, button_7, text_17, lblAuthors));
		count++;
		/////////////////
		
		Group group_6 = new Group(nomenclatureGroup, SWT.NONE);
		group_6.setBounds(300, 55, 182, 40);
		
		Button button_8 = new Button(group_6, SWT.RADIO);
		button_8.setText("Yes");
		button_8.setBounds(10, 13, 39, 16);
		
		Button button_9 = new Button(group_6, SWT.RADIO);
		button_9.setText("No");
		button_9.setBounds(55, 13, 39, 16);
		
		text_18 = new Text(group_6, SWT.BORDER);
		text_18.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_6, button_8, button_9, text_18, lblAuthors));
		count++;
		
		///////////////////////////////////////////
		
		Group group_7 = new Group(nomenclatureGroup, SWT.NONE);
		group_7.setBounds(500, 55, 182, 40);
		
		Button button_10 = new Button(group_7, SWT.RADIO);
		button_10.setText("Yes");
		button_10.setBounds(10, 13, 39, 16);
		
		Button button_11 = new Button(group_7, SWT.RADIO);
		button_11.setText("No");
		button_11.setBounds(55, 13, 39, 16);
		
		text_19 = new Text(group_7, SWT.BORDER);
		text_19.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_7, button_10, button_11, text_19, lblAuthors));
		count++;
		//////////////////////////////
		
		Group group_8 = new Group(nomenclatureGroup, SWT.NONE);
		group_8.setBounds(100, 100, 182, 40);
		
		Button button_12 = new Button(group_8, SWT.RADIO);
		button_12.setText("Yes");
		button_12.setBounds(10, 13, 39, 16);
		
		Button button_13 = new Button(group_8, SWT.RADIO);
		button_13.setText("No");
		button_13.setBounds(55, 13, 39, 16);
		
		text_20 = new Text(group_8, SWT.BORDER);
		text_20.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_8, button_12, button_13, text_20, lblDate));
		count++;
		////////////////////////
		
		Group group_9 = new Group(nomenclatureGroup, SWT.NONE);
		group_9.setBounds(300, 100, 182, 40);
		
		Button button_14 = new Button(group_9, SWT.RADIO);
		button_14.setText("Yes");
		button_14.setBounds(10, 13, 39, 16);
		
		Button button_15 = new Button(group_9, SWT.RADIO);
		button_15.setText("No");
		button_15.setBounds(55, 13, 39, 16);
		
		text_21 = new Text(group_9, SWT.BORDER);
		text_21.setBounds(100, 11, 76, 21);
				
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_9, button_14, button_15, text_21, lblDate));
		count++;
		//////////////////////////////////
		
		Group group_10 = new Group(nomenclatureGroup, SWT.NONE);
		group_10.setBounds(500, 100, 182, 40);
		
		Button button_16 = new Button(group_10, SWT.RADIO);
		button_16.setText("Yes");
		button_16.setBounds(10, 13, 39, 16);
		
		Button button_17 = new Button(group_10, SWT.RADIO);
		button_17.setText("No");
		button_17.setBounds(55, 13, 39, 16);
		
		text_22 = new Text(group_10, SWT.BORDER);
		text_22.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_10, button_16, button_17, text_22, lblDate));
		count++;
		//////////////////////////////
		
		Group group_11 = new Group(nomenclatureGroup, SWT.NONE);
		group_11.setBounds(100, 145, 182, 40);
		
		Button button_18 = new Button(group_11, SWT.RADIO);
		button_18.setText("Yes");
		button_18.setBounds(10, 13, 39, 16);
		
		Button button_19 = new Button(group_11, SWT.RADIO);
		button_19.setText("No");
		button_19.setBounds(55, 13, 39, 16);
		
		text_23 = new Text(group_11, SWT.BORDER);
		text_23.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_11, button_18, button_19, text_23, lblPublication));
		count++;
		///////////////////////////////////////
		
		Group group_12 = new Group(nomenclatureGroup, SWT.NONE);
		group_12.setBounds(300, 145, 182, 40);
		
		Button button_20 = new Button(group_12, SWT.RADIO);
		button_20.setText("Yes");
		button_20.setBounds(10, 13, 39, 16);
		
		Button button_21 = new Button(group_12, SWT.RADIO);
		button_21.setText("No");
		button_21.setBounds(55, 13, 39, 16);
		
		text_24 = new Text(group_12, SWT.BORDER);
		text_24.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_12, button_20, button_21, text_24, lblPublication));
		count++;
		//////////////////
		
		Group group_13 = new Group(nomenclatureGroup, SWT.NONE);
		group_13.setBounds(500, 145, 182, 40);
		
		Button button_22 = new Button(group_13, SWT.RADIO);
		button_22.setText("Yes");
		button_22.setBounds(10, 13, 39, 16);
		
		Button button_23 = new Button(group_13, SWT.RADIO);
		button_23.setText("No");
		button_23.setBounds(55, 13, 39, 16);
		
		text_25 = new Text(group_13, SWT.BORDER);
		text_25.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_13, button_22, button_23, text_25, lblPublication));
		count++;
		/////////////////////////
		
		Group group_14 = new Group(nomenclatureGroup, SWT.NONE);
		group_14.setBounds(100, 190, 182, 40);
		
		Button button_24 = new Button(group_14, SWT.RADIO);
		button_24.setText("Yes");
		button_24.setBounds(10, 13, 39, 16);
		
		Button button_25 = new Button(group_14, SWT.RADIO);
		button_25.setText("No");
		button_25.setBounds(55, 13, 39, 16);
		
		text_26 = new Text(group_14, SWT.BORDER);
		text_26.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_14, button_24, button_25, text_26, lblTaxonRank));
		count++;
		//////////////////////////////
		
		Group group_15 = new Group(nomenclatureGroup, SWT.NONE);
		group_15.setBounds(300, 190, 182, 40);
		
		Button button_26 = new Button(group_15, SWT.RADIO);
		button_26.setText("Yes");
		button_26.setBounds(10, 13, 39, 16);
		
		Button button_27 = new Button(group_15, SWT.RADIO);
		button_27.setText("No");
		button_27.setBounds(55, 13, 39, 16);
		
		text_27 = new Text(group_15, SWT.BORDER);
		text_27.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_15, button_26, button_27, text_27, lblTaxonRank));
		count++;
		////////////////////////////////
		
		Group group_16 = new Group(nomenclatureGroup, SWT.NONE);
		group_16.setBounds(500, 190, 182, 40);
		
		Button button_28 = new Button(group_16, SWT.RADIO);
		button_28.setText("Yes");
		button_28.setBounds(10, 13, 39, 16);
		
		Button button_29 = new Button(group_16, SWT.RADIO);
		button_29.setText("No");
		button_29.setBounds(55, 13, 39, 16);
		
		text_28 = new Text(group_16, SWT.BORDER);
		text_28.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_16, button_28, button_29, text_28, lblTaxonRank));
		count++;
		///////////////////////////////
		nomenScrolledComposite.setContent(nomenclatureGroup);
		//When you add a row, reset the size of scrolledComposite
		nomenScrolledComposite.setMinSize(nomenclatureGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		Button btnAddARow = new Button(grpNomenclature, SWT.NONE);
		btnAddARow.setBounds(10, 337, 75, 25);
		btnAddARow.setText("Add a Row");
		btnAddARow.addSelectionListener (new SelectionAdapter () {
			public void widgetSelected (SelectionEvent e) {
				showInputBox();
				if(identifier != null && !identifier.equals("")) {
					addNomenclatureRow();
				}
			}
		});
		
		TabItem tbtmExpressions = new TabItem(tabFolder, SWT.NONE);
		tbtmExpressions.setText("Expressions");
		
		Group grpExpressionsUsedIn = new Group(tabFolder, SWT.NONE);
		grpExpressionsUsedIn.setText("Expressions used in Nomenclature");
		tbtmExpressions.setControl(grpExpressionsUsedIn);
		
		Label lblUseCapLetters = new Label(grpExpressionsUsedIn, SWT.NONE);
		lblUseCapLetters.setBounds(10, 22, 426, 15);
		lblUseCapLetters.setText("Use CAP words for fixed tokens; use small letters for variables");
		
		Label lblHononyms = new Label(grpExpressionsUsedIn, SWT.NONE);
		lblHononyms.setBounds(348, 22, 99, 15);
		lblHononyms.setText("Hononyms:");
		
		ScrolledComposite scrolledComposite_2 = new ScrolledComposite(grpExpressionsUsedIn, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite_2.setBounds(10, 43, 714, 440);
		scrolledComposite_2.setExpandHorizontal(true);
		scrolledComposite_2.setExpandVertical(true);
		
		Group group_17 = new Group(scrolledComposite_2, SWT.NONE);
		
		Label lblSpecialTokensUsed = new Label(group_17, SWT.NONE);
		lblSpecialTokensUsed.setBounds(10, 21, 119, 15);
		lblSpecialTokensUsed.setText("Special tokens used:");
		
		text_29 = new Text(group_17, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		text_29.setBounds(135, 18, 550, 69);
		
		Label lblMinorAmendment = new Label(group_17, SWT.NONE);
		lblMinorAmendment.setBounds(10, 110, 119, 15);
		lblMinorAmendment.setText("Minor Amendment:");
		
		text_30 = new Text(group_17, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		text_30.setBounds(137, 107, 548, 69);
		
		Label lblPastName = new Label(group_17, SWT.NONE);
		lblPastName.setBounds(10, 203, 67, 15);
		lblPastName.setText("Past name:");
		
		text_31 = new Text(group_17, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		text_31.setBounds(137, 200, 548, 69);
		
		Label lblNameOrigin = new Label(group_17, SWT.NONE);
		lblNameOrigin.setBounds(10, 283, 119, 15);
		lblNameOrigin.setText("Name origin:");
		
		text_32 = new Text(group_17, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		text_32.setBounds(137, 290, 550, 69);
		
		text_61 = new Text(group_17, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		text_61.setBounds(137, 381, 548, 69);
		
		Label lblHononyms_1 = new Label(group_17, SWT.NONE);
		lblHononyms_1.setBounds(10, 381, 85, 15);
		lblHononyms_1.setText("Homonyms:");
		scrolledComposite_2.setContent(group_17);
		scrolledComposite_2.setMinSize(group_17.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		Button btnAddARow_2 = new Button(grpExpressionsUsedIn, SWT.NONE);
		btnAddARow_2.setBounds(10, 489, 75, 25);
		btnAddARow_2.setText("Add a row");
		
		TabItem tbtmDescription = new TabItem(tabFolder, SWT.NONE);
		tbtmDescription.setText("Description");
		
		Group grpMorphologicalDescriptions = new Group(tabFolder, SWT.NONE);
		tbtmDescription.setControl(grpMorphologicalDescriptions);
		
		Label lblAllInOne = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblAllInOne.setBounds(10, 53, 160, 15);
		lblAllInOne.setText("All in one paragraph");
		
		Button btnYes = new Button(grpMorphologicalDescriptions, SWT.RADIO);
		btnYes.setBounds(241, 52, 90, 16);
		btnYes.setText("Yes");
		
		Button btnNo = new Button(grpMorphologicalDescriptions, SWT.RADIO);
		btnNo.setBounds(378, 52, 90, 16);
		btnNo.setText("No");
		
		Label lblOtherInformationMay = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblOtherInformationMay.setBounds(10, 85, 438, 15);
		lblOtherInformationMay.setText("Other information may also be included in a description paragraph:");
		
		Combo combo = new Combo(grpMorphologicalDescriptions, SWT.NONE);
		combo.setItems(new String[] {"Nomenclature", "Habitat", "Distribution", "Discussion", "Other"});
		combo.setBounds(496, 82, 177, 23);
		
		Label lblMorphological = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblMorphological.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
		lblMorphological.setBounds(10, 25, 242, 15);
		lblMorphological.setText("Morphological Descriptions: ");
		
		Label lblOrder = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblOrder.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
		lblOrder.setBounds(22, 142, 55, 15);
		lblOrder.setText("Order");
		
		Label lblSection = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblSection.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
		lblSection.setBounds(140, 142, 55, 15);
		lblSection.setText("Section");
		
		Label lblStartTokens = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblStartTokens.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
		lblStartTokens.setBounds(285, 142, 90, 15);
		lblStartTokens.setText("Start tokens");
		
		Label lblEndTokens = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblEndTokens.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
		lblEndTokens.setBounds(443, 142, 68, 15);
		lblEndTokens.setText("End tokens");
		
		Label lblEmbeddedTokens = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblEmbeddedTokens.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
		lblEmbeddedTokens.setBounds(592, 142, 132, 15);
		lblEmbeddedTokens.setText("Embedded tokens");
		
		ScrolledComposite scrolledComposite_1 = new ScrolledComposite(grpMorphologicalDescriptions, 
				SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite_1.setBounds(10, 169, 714, 310);
		scrolledComposite_1.setExpandHorizontal(true);
		scrolledComposite_1.setExpandVertical(true);
		
		Group group_3 = new Group(scrolledComposite_1, SWT.NONE);
		
		text_33 = new Text(group_3, SWT.BORDER);
		text_33.setBounds(10, 21, 76, 21);
		
		text_34 = new Text(group_3, SWT.BORDER);
		text_34.setBounds(10, 62, 76, 21);
		
		text_35 = new Text(group_3, SWT.BORDER);
		text_35.setBounds(10, 105, 76, 21);
		
		text_36 = new Text(group_3, SWT.BORDER);
		text_36.setBounds(10, 151, 76, 21);
		
		text_37 = new Text(group_3, SWT.BORDER);
		text_37.setBounds(10, 191, 76, 21);
		
		text_38 = new Text(group_3, SWT.BORDER);
		text_38.setBounds(10, 232, 76, 21);
		
		text_39 = new Text(group_3, SWT.BORDER);
		text_39.setBounds(10, 275, 76, 21);
		
		Label lblNomenclature = new Label(group_3, SWT.NONE);
		lblNomenclature.setBounds(120, 21, 101, 15);
		lblNomenclature.setText("Nomenclature");
		
		text_40 = new Text(group_3, SWT.BORDER);
		text_40.setBounds(270, 21, 113, 21);
		
		text_41 = new Text(group_3, SWT.BORDER);
		text_41.setBounds(432, 21, 101, 21);
		
		text_42 = new Text(group_3, SWT.BORDER);
		text_42.setBounds(576, 21, 107, 21);
		
		Label lblMorphDescription = new Label(group_3, SWT.NONE);
		lblMorphDescription.setBounds(120, 62, 113, 15);
		lblMorphDescription.setText("Morph. description");
		
		text_43 = new Text(group_3, SWT.BORDER);
		text_43.setBounds(270, 62, 113, 21);
		
		text_44 = new Text(group_3, SWT.BORDER);
		text_44.setBounds(432, 62, 101, 21);
		
		text_45 = new Text(group_3, SWT.BORDER);
		text_45.setBounds(576, 62, 107, 21);
		
		Label lblHabitat = new Label(group_3, SWT.NONE);
		lblHabitat.setBounds(120, 105, 55, 15);
		lblHabitat.setText("Habitat");
		
		text_46 = new Text(group_3, SWT.BORDER);
		text_46.setBounds(270, 105, 113, 21);
		
		text_47 = new Text(group_3, SWT.BORDER);
		text_47.setBounds(432, 105, 101, 21);
		
		text_48 = new Text(group_3, SWT.BORDER);
		text_48.setBounds(576, 105, 107, 21);
		
		Label lblDistribution = new Label(group_3, SWT.NONE);
		lblDistribution.setBounds(120, 151, 101, 15);
		lblDistribution.setText("Distribution");
		
		text_49 = new Text(group_3, SWT.BORDER);
		text_49.setBounds(270, 151, 113, 21);
		
		text_50 = new Text(group_3, SWT.BORDER);
		text_50.setBounds(432, 151, 101, 21);
		
		text_51 = new Text(group_3, SWT.BORDER);
		text_51.setBounds(576, 151, 107, 21);
		
		text_52 = new Text(group_3, SWT.BORDER);
		text_52.setBounds(270, 191, 113, 21);
		
		text_53 = new Text(group_3, SWT.BORDER);
		text_53.setBounds(432, 191, 101, 21);
		
		text_54 = new Text(group_3, SWT.BORDER);
		text_54.setBounds(576, 191, 107, 21);
		
		text_55 = new Text(group_3, SWT.BORDER);
		text_55.setBounds(270, 232, 113, 21);
		
		Label lblDiscussion = new Label(group_3, SWT.NONE);
		lblDiscussion.setBounds(120, 191, 101, 15);
		lblDiscussion.setText("Discussion");
		
		Label lblKeys = new Label(group_3, SWT.NONE);
		lblKeys.setBounds(120, 238, 55, 15);
		lblKeys.setText("Keys");
		
		Label lblReferences_1 = new Label(group_3, SWT.NONE);
		lblReferences_1.setBounds(120, 275, 87, 15);
		lblReferences_1.setText("References");
		
		text_56 = new Text(group_3, SWT.BORDER);
		text_56.setBounds(270, 275, 113, 21);
		
		text_57 = new Text(group_3, SWT.BORDER);
		text_57.setBounds(432, 232, 101, 21);
		
		text_58 = new Text(group_3, SWT.BORDER);
		text_58.setBounds(576, 232, 107, 21);
		
		text_59 = new Text(group_3, SWT.BORDER);
		text_59.setBounds(432, 275, 101, 21);
		
		text_60 = new Text(group_3, SWT.BORDER);
		text_60.setBounds(576, 275, 107, 21);
		scrolledComposite_1.setContent(group_3);
		scrolledComposite_1.setMinSize(group_3.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		Button btnAddARow_1 = new Button(grpMorphologicalDescriptions, SWT.NONE);
		btnAddARow_1.setBounds(10, 482, 75, 25);
		btnAddARow_1.setText("Add a row");
		
		Label lblSectionIndicationsAnd = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblSectionIndicationsAnd.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
		lblSectionIndicationsAnd.setBounds(10, 116, 222, 15);
		lblSectionIndicationsAnd.setText("Section indications and order:");
		
		TabItem tbtmSpecial = new TabItem(tabFolder, SWT.NONE);
		tbtmSpecial.setText("Special");
		
		Group grpSpecialSections = new Group(tabFolder, SWT.NONE);
		grpSpecialSections.setText("Special Sections");
		tbtmSpecial.setControl(grpSpecialSections);
		
		Label lblGlossaries = new Label(grpSpecialSections, SWT.NONE);
		lblGlossaries.setBounds(10, 51, 55, 15);
		lblGlossaries.setText("Glossaries:");
		
		Button btnHasGlossaries = new Button(grpSpecialSections, SWT.CHECK);
		btnHasGlossaries.setBounds(96, 51, 93, 16);
		btnHasGlossaries.setText("has glossaries");
		
		Label lblGlossaryHeading = new Label(grpSpecialSections, SWT.NONE);
		lblGlossaryHeading.setBounds(257, 51, 93, 15);
		lblGlossaryHeading.setText("Glossary heading");
		
		text_9 = new Text(grpSpecialSections, SWT.BORDER);
		text_9.setBounds(377, 48, 76, 21);
		
		Label lblReferences = new Label(grpSpecialSections, SWT.NONE);
		lblReferences.setBounds(10, 102, 69, 15);
		lblReferences.setText("References :");
		
		Button btnHasReferences = new Button(grpSpecialSections, SWT.CHECK);
		btnHasReferences.setBounds(96, 102, 93, 16);
		btnHasReferences.setText("has references");
		
		Label lblReferencesHeading = new Label(grpSpecialSections, SWT.NONE);
		lblReferencesHeading.setBounds(257, 102, 114, 15);
		lblReferencesHeading.setText("References heading:");
		
		text_10 = new Text(grpSpecialSections, SWT.BORDER);
		text_10.setBounds(377, 99, 76, 21);
		
		TabItem tbtmAbbreviations = new TabItem(tabFolder, SWT.NONE);
		tbtmAbbreviations.setText("Abbreviations");
		
		Group grpAbbreviationsUsedIn = new Group(tabFolder, SWT.NONE);
		tbtmAbbreviations.setControl(grpAbbreviationsUsedIn);
		
		text_11 = new Text(grpAbbreviationsUsedIn, SWT.BORDER | SWT.MULTI| SWT.WRAP | SWT.V_SCROLL );
		text_11.setBounds(10, 52, 691, 69);
		
		Label lblAbbreviationsUsedIn = new Label(grpAbbreviationsUsedIn, SWT.NONE);
		lblAbbreviationsUsedIn.setBounds(10, 31, 272, 15);
		lblAbbreviationsUsedIn.setText("Abbreviations used in text:");
		
		Label lblAbbreviationsUsedIn_1 = new Label(grpAbbreviationsUsedIn, SWT.NONE);
		lblAbbreviationsUsedIn_1.setBounds(10, 150, 272, 15);
		lblAbbreviationsUsedIn_1.setText("Abbreviations used in bibliographical citations:");
		
		text_12 = new Text(grpAbbreviationsUsedIn, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		text_12.setBounds(10, 175, 691, 69);
		
		Label lblAbbreviationsUsedIn_2 = new Label(grpAbbreviationsUsedIn, SWT.NONE);
		lblAbbreviationsUsedIn_2.setBounds(10, 275, 272, 15);
		lblAbbreviationsUsedIn_2.setText("Abbreviations used in authorities:");
		
		text_13 = new Text(grpAbbreviationsUsedIn, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		text_13.setBounds(10, 296, 691, 69);
		
		Label lblAbbreviationsUsedIn_3 = new Label(grpAbbreviationsUsedIn, SWT.NONE);
		lblAbbreviationsUsedIn_3.setBounds(10, 395, 204, 15);
		lblAbbreviationsUsedIn_3.setText("Abbreviations used in others:");
		
		text_8 = new Text(grpAbbreviationsUsedIn, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		text_8.setBounds(10, 416, 691, 69);
		
		Button btnSave = new Button(shlTypeDocument, SWT.NONE);
		btnSave.setBounds(670, 563, 75, 25);
		btnSave.setText("Save");
		
		shlTypeDocument.open();
		shlTypeDocument.layout();
		while (!shlTypeDocument.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
    }

    private void addNomenclatureRow() {
    	
		RowData rowdata = (RowData)nomenclatureGroup.getLayoutData();
		rowdata.height += 30;
		nomenclatureGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
        Rectangle rect = nomenclatureGroup.getBounds();
        rect.height += 30;
        nomenclatureGroup.setBounds(rect);
        
        /*Create a row*/
        
        NomenclatureBean nbean = nomenclatures.get(new Integer(count-1));
        Label previousLabel = nbean.getLabel();
        rect = previousLabel.getBounds();
        rect.y += 45;
       
        Label lblNew = new Label(nomenclatureGroup, SWT.NONE);
		lblNew.setBounds(rect);
		lblNew.setText(identifier);
        
		/* Create the first group*/
		Group prevGroup = nbean.getParent();
		rect = prevGroup.getBounds();
		Group group_1 = new Group(nomenclatureGroup, SWT.NONE);
		group_1.setBounds(100, rect.y+45, 182, 40);
		
		Button buttonYes_1 = new Button(group_1, SWT.RADIO);
		buttonYes_1.setText("Yes");
		buttonYes_1.setBounds(10, 13, 39, 16);
		
		Button buttonNo_1 = new Button(group_1, SWT.RADIO);
		buttonNo_1.setText("No");
		buttonNo_1.setBounds(55, 13, 39, 16);
		
		Text text1 = new Text(group_1, SWT.BORDER);
		text1.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_1, buttonYes_1, buttonNo_1, text1, lblNew));
		count++;
		///////////////////////////////////
		
		 /*Create the second group */
		Group group_2 = new Group(nomenclatureGroup, SWT.NONE);
		group_2.setBounds(300, rect.y+45, 182, 40);
		
		Button buttonYes_2 = new Button(group_2, SWT.RADIO);
		buttonYes_2.setText("Yes");
		buttonYes_2.setBounds(10, 13, 39, 16);
		
		Button buttonNo_2 = new Button(group_2, SWT.RADIO);
		buttonNo_2.setText("No");
		buttonNo_2.setBounds(55, 13, 39, 16);
		
		Text text2 = new Text(group_2, SWT.BORDER);
		text2.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_2, buttonYes_2, buttonNo_2, text2, lblNew));
		count++; 
        ////////////////////////////////////////
		
		/* Create the third group */
		Group group_3 = new Group(nomenclatureGroup, SWT.NONE);
		group_3.setBounds(500,rect.y+45, 182, 40);
		
		Button buttonYes_3 = new Button(group_3, SWT.RADIO);
		buttonYes_3.setText("Yes");
		buttonYes_3.setBounds(10, 13, 39, 16);
		
		Button buttonNo_3 = new Button(group_3, SWT.RADIO);
		buttonNo_3.setText("No");
		buttonNo_3.setBounds(55, 13, 39, 16);
		
		Text text3 = new Text(group_3, SWT.BORDER);
		text3.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(count), new NomenclatureBean(group_3, buttonYes_3, buttonNo_3, text3, lblNew));
		count++;
		////////////////////////////
        nomenScrolledComposite.setMinSize(nomenclatureGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }
    private void showInputBox() {
    	Display display = Display.getDefault();
    	final Shell dialog = new Shell (display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setText("Add a row");
		dialog.setLocation(shlTypeDocument.getBounds().x/2 + shlTypeDocument.getBounds().width/2, 
				shlTypeDocument.getBounds().y/2+ shlTypeDocument.getBounds().height/2);
		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth = 10;
		formLayout.marginHeight = 10;
		formLayout.spacing = 10;
		dialog.setLayout (formLayout);

		Label label = new Label (dialog, SWT.NONE);
		label.setText ("Type an identifier:");
		FormData data = new FormData ();
		label.setLayoutData (data);

		Button cancel = new Button (dialog, SWT.PUSH);
		cancel.setText ("Cancel");
		data = new FormData ();
		data.width = 60;
		data.right = new FormAttachment (100, 0);
		data.bottom = new FormAttachment (100, 0);
		cancel.setLayoutData (data);
		cancel.addSelectionListener (new SelectionAdapter () {
			public void widgetSelected (SelectionEvent e) {
				dialog.close ();
			}
		});

		final Text text = new Text (dialog, SWT.BORDER);
		data = new FormData ();
		data.width = 200;
		data.left = new FormAttachment (label, 0, SWT.DEFAULT);
		data.right = new FormAttachment (100, 0);
		data.top = new FormAttachment (label, 0, SWT.CENTER);
		data.bottom = new FormAttachment (cancel, 0, SWT.DEFAULT);
		text.setLayoutData (data);

		Button ok = new Button (dialog, SWT.PUSH);
		ok.setText ("OK");
		data = new FormData ();
		data.width = 60;
		data.right = new FormAttachment (cancel, 0, SWT.DEFAULT);
		data.bottom = new FormAttachment (100, 0);
		ok.setLayoutData (data);
		ok.addSelectionListener (new SelectionAdapter () {
			public void widgetSelected (SelectionEvent e) {
				identifier = text.getText();
				dialog.close ();
			}
		});

		dialog.setDefaultButton (ok);
		dialog.pack ();
		dialog.open ();
		while (!dialog.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

	}
}
