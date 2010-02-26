package fna.parsing;

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
	private Text text_8;
	private Text text_29;
	private Text text_30;
	private Text text_31;
	private Text text_32;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
    /**
     * @wbp.parser.entryPoint
     */
    public void showType2Document() {
		final Display display = Display.getDefault();
		
		final Shell shlTypeDocument = new Shell();
		shlTypeDocument.setText("Type 2 Document");
		shlTypeDocument.setSize(771, 634);
		
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
		
		ScrolledComposite scrolledComposite = new ScrolledComposite(grpNomenclature, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setBounds(10, 53, 714, 278);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		
		Group group = new Group(scrolledComposite, SWT.NONE);
		
		Label lblName = new Label(group, SWT.NONE);
		lblName.setBounds(10, 21, 55, 15);
		lblName.setText("Name");
		
		Label lblAuthors = new Label(group, SWT.NONE);
		lblAuthors.setBounds(10, 66, 55, 15);
		lblAuthors.setText("Authors");
		
		Label lblDate = new Label(group, SWT.NONE);
		lblDate.setBounds(10, 113, 55, 15);
		lblDate.setText("Date");
		
		Label lblPublication = new Label(group, SWT.NONE);
		lblPublication.setBounds(10, 157, 67, 15);
		lblPublication.setText("Publication");
		
		Label lblTaxonRank = new Label(group, SWT.NONE);
		lblTaxonRank.setBounds(10, 204, 67, 15);
		lblTaxonRank.setText("Taxon Rank");
		
		Group group_2 = new Group(group, SWT.NONE);
		group_2.setBounds(97, 10, 182, 40);
		
		Button button = new Button(group_2, SWT.RADIO);
		button.setText("Yes");
		button.setBounds(10, 13, 39, 16);
		
		Button button_1 = new Button(group_2, SWT.RADIO);
		button_1.setText("No");
		button_1.setBounds(55, 13, 39, 16);
		
		text_14 = new Text(group_2, SWT.BORDER);
		text_14.setBounds(100, 11, 76, 21);
		
		Group group_1 = new Group(group, SWT.NONE);
		group_1.setBounds(299, 10, 182, 40);
		
		Button button_2 = new Button(group_1, SWT.RADIO);
		button_2.setText("Yes");
		button_2.setBounds(10, 13, 39, 16);
		
		Button button_3 = new Button(group_1, SWT.RADIO);
		button_3.setText("No");
		button_3.setBounds(55, 13, 39, 16);
		
		text_15 = new Text(group_1, SWT.BORDER);
		text_15.setBounds(100, 11, 76, 21);
		
		Group group_4 = new Group(group, SWT.NONE);
		group_4.setBounds(501, 10, 182, 40);
		
		Button button_4 = new Button(group_4, SWT.RADIO);
		button_4.setText("Yes");
		button_4.setBounds(10, 13, 39, 16);
		
		Button button_5 = new Button(group_4, SWT.RADIO);
		button_5.setText("No");
		button_5.setBounds(55, 13, 39, 16);
		
		text_16 = new Text(group_4, SWT.BORDER);
		text_16.setBounds(100, 11, 76, 21);
		
		Group group_5 = new Group(group, SWT.NONE);
		group_5.setBounds(97, 56, 182, 40);
		
		Button button_6 = new Button(group_5, SWT.RADIO);
		button_6.setText("Yes");
		button_6.setBounds(10, 13, 39, 16);
		
		Button button_7 = new Button(group_5, SWT.RADIO);
		button_7.setText("No");
		button_7.setBounds(55, 13, 39, 16);
		
		text_17 = new Text(group_5, SWT.BORDER);
		text_17.setBounds(100, 11, 76, 21);
		
		Group group_6 = new Group(group, SWT.NONE);
		group_6.setBounds(299, 56, 182, 40);
		
		Button button_8 = new Button(group_6, SWT.RADIO);
		button_8.setText("Yes");
		button_8.setBounds(10, 13, 39, 16);
		
		Button button_9 = new Button(group_6, SWT.RADIO);
		button_9.setText("No");
		button_9.setBounds(55, 13, 39, 16);
		
		text_18 = new Text(group_6, SWT.BORDER);
		text_18.setBounds(100, 11, 76, 21);
		
		Group group_7 = new Group(group, SWT.NONE);
		group_7.setBounds(501, 56, 182, 40);
		
		Button button_10 = new Button(group_7, SWT.RADIO);
		button_10.setText("Yes");
		button_10.setBounds(10, 13, 39, 16);
		
		Button button_11 = new Button(group_7, SWT.RADIO);
		button_11.setText("No");
		button_11.setBounds(55, 13, 39, 16);
		
		text_19 = new Text(group_7, SWT.BORDER);
		text_19.setBounds(100, 11, 76, 21);
		
		Group group_8 = new Group(group, SWT.NONE);
		group_8.setBounds(97, 101, 182, 40);
		
		Button button_12 = new Button(group_8, SWT.RADIO);
		button_12.setText("Yes");
		button_12.setBounds(10, 13, 39, 16);
		
		Button button_13 = new Button(group_8, SWT.RADIO);
		button_13.setText("No");
		button_13.setBounds(55, 13, 39, 16);
		
		text_20 = new Text(group_8, SWT.BORDER);
		text_20.setBounds(100, 11, 76, 21);
		
		Group group_9 = new Group(group, SWT.NONE);
		group_9.setBounds(299, 101, 182, 40);
		
		Button button_14 = new Button(group_9, SWT.RADIO);
		button_14.setText("Yes");
		button_14.setBounds(10, 13, 39, 16);
		
		Button button_15 = new Button(group_9, SWT.RADIO);
		button_15.setText("No");
		button_15.setBounds(55, 13, 39, 16);
		
		text_21 = new Text(group_9, SWT.BORDER);
		text_21.setBounds(100, 11, 76, 21);
		
		Group group_10 = new Group(group, SWT.NONE);
		group_10.setBounds(501, 102, 182, 40);
		
		Button button_16 = new Button(group_10, SWT.RADIO);
		button_16.setText("Yes");
		button_16.setBounds(10, 13, 39, 16);
		
		Button button_17 = new Button(group_10, SWT.RADIO);
		button_17.setText("No");
		button_17.setBounds(55, 13, 39, 16);
		
		text_22 = new Text(group_10, SWT.BORDER);
		text_22.setBounds(100, 11, 76, 21);
		
		Group group_11 = new Group(group, SWT.NONE);
		group_11.setBounds(97, 147, 182, 40);
		
		Button button_18 = new Button(group_11, SWT.RADIO);
		button_18.setText("Yes");
		button_18.setBounds(10, 13, 39, 16);
		
		Button button_19 = new Button(group_11, SWT.RADIO);
		button_19.setText("No");
		button_19.setBounds(55, 13, 39, 16);
		
		text_23 = new Text(group_11, SWT.BORDER);
		text_23.setBounds(100, 11, 76, 21);
		
		Group group_12 = new Group(group, SWT.NONE);
		group_12.setBounds(299, 147, 182, 40);
		
		Button button_20 = new Button(group_12, SWT.RADIO);
		button_20.setText("Yes");
		button_20.setBounds(10, 13, 39, 16);
		
		Button button_21 = new Button(group_12, SWT.RADIO);
		button_21.setText("No");
		button_21.setBounds(55, 13, 39, 16);
		
		text_24 = new Text(group_12, SWT.BORDER);
		text_24.setBounds(100, 11, 76, 21);
		
		Group group_13 = new Group(group, SWT.NONE);
		group_13.setBounds(501, 148, 182, 40);
		
		Button button_22 = new Button(group_13, SWT.RADIO);
		button_22.setText("Yes");
		button_22.setBounds(10, 13, 39, 16);
		
		Button button_23 = new Button(group_13, SWT.RADIO);
		button_23.setText("No");
		button_23.setBounds(55, 13, 39, 16);
		
		text_25 = new Text(group_13, SWT.BORDER);
		text_25.setBounds(100, 11, 76, 21);
		
		Group group_14 = new Group(group, SWT.NONE);
		group_14.setBounds(97, 193, 182, 40);
		
		Button button_24 = new Button(group_14, SWT.RADIO);
		button_24.setText("Yes");
		button_24.setBounds(10, 13, 39, 16);
		
		Button button_25 = new Button(group_14, SWT.RADIO);
		button_25.setText("No");
		button_25.setBounds(55, 13, 39, 16);
		
		text_26 = new Text(group_14, SWT.BORDER);
		text_26.setBounds(100, 11, 76, 21);
		
		Group group_15 = new Group(group, SWT.NONE);
		group_15.setBounds(299, 193, 182, 40);
		
		Button button_26 = new Button(group_15, SWT.RADIO);
		button_26.setText("Yes");
		button_26.setBounds(10, 13, 39, 16);
		
		Button button_27 = new Button(group_15, SWT.RADIO);
		button_27.setText("No");
		button_27.setBounds(55, 13, 39, 16);
		
		text_27 = new Text(group_15, SWT.BORDER);
		text_27.setBounds(100, 11, 76, 21);
		
		Group group_16 = new Group(group, SWT.NONE);
		group_16.setBounds(501, 194, 182, 40);
		
		Button button_28 = new Button(group_16, SWT.RADIO);
		button_28.setText("Yes");
		button_28.setBounds(10, 13, 39, 16);
		
		Button button_29 = new Button(group_16, SWT.RADIO);
		button_29.setText("No");
		button_29.setBounds(55, 13, 39, 16);
		
		text_28 = new Text(group_16, SWT.BORDER);
		text_28.setBounds(100, 11, 76, 21);
		
		Label label_1 = new Label(group, SWT.SEPARATOR | SWT.VERTICAL);
		label_1.setBounds(285, 10, 2, 242);
		
		Label label_4 = new Label(group, SWT.SEPARATOR | SWT.VERTICAL);
		label_4.setBounds(487, 10, 2, 242);
		scrolledComposite.setContent(group);
		scrolledComposite.setMinSize(group.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		Button btnAddARow = new Button(grpNomenclature, SWT.NONE);
		btnAddARow.setBounds(10, 337, 75, 25);
		btnAddARow.setText("Add a Row");
		
		TabItem tbtmExpressions = new TabItem(tabFolder, SWT.NONE);
		tbtmExpressions.setText("Expressions");
		
		Group grpExpressionsUsedIn = new Group(tabFolder, SWT.NONE);
		grpExpressionsUsedIn.setText("Expressions used in Nomenclature");
		tbtmExpressions.setControl(grpExpressionsUsedIn);
		
		Label lblUseCapLetters = new Label(grpExpressionsUsedIn, SWT.NONE);
		lblUseCapLetters.setBounds(10, 22, 426, 15);
		lblUseCapLetters.setText("Use cap letters for fix tokens; use small letters for variables");
		
		Label lblSpecialTokensUsed = new Label(grpExpressionsUsedIn, SWT.NONE);
		lblSpecialTokensUsed.setBounds(10, 59, 119, 15);
		lblSpecialTokensUsed.setText("Special tokens used:");
		
		Label lblMinorAmendment = new Label(grpExpressionsUsedIn, SWT.NONE);
		lblMinorAmendment.setBounds(10, 151, 119, 15);
		lblMinorAmendment.setText("Minor Amendment:");
		
		Label lblPastName = new Label(grpExpressionsUsedIn, SWT.NONE);
		lblPastName.setBounds(10, 238, 55, 15);
		lblPastName.setText("Past name:");
		
		Label lblNameOrigin = new Label(grpExpressionsUsedIn, SWT.NONE);
		lblNameOrigin.setBounds(10, 329, 119, 15);
		lblNameOrigin.setText("Name origin:");
		
		Label lblHononyms = new Label(grpExpressionsUsedIn, SWT.NONE);
		lblHononyms.setBounds(10, 420, 99, 15);
		lblHononyms.setText("Hononyms:");
		
		text_8 = new Text(grpExpressionsUsedIn, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		text_8.setBounds(135, 63, 589, 70);
		
		text_29 = new Text(grpExpressionsUsedIn, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		text_29.setBounds(135, 151, 589, 70);
		
		text_30 = new Text(grpExpressionsUsedIn, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		text_30.setBounds(135, 238, 589, 70);
		
		text_31 = new Text(grpExpressionsUsedIn, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		text_31.setBounds(135, 329, 589, 70);
		
		text_32 = new Text(grpExpressionsUsedIn, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		text_32.setBounds(135, 420, 589, 70);
		
		TabItem tbtmDescription = new TabItem(tabFolder, SWT.NONE);
		tbtmDescription.setText("Description");
		
		Group group_3 = new Group(tabFolder, SWT.NONE);
		tbtmDescription.setControl(group_3);
		
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
}
