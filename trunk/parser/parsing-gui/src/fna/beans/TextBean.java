package fna.beans;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;

public class TextBean {
	
	private Text firstPara;
	private Text leadingIndentation;
	private Text spacing;
	private Text estimatedLength;
	private Text pageNumberFormsText;
	private Button sectionHeadingsCapButton;
	private Button sectionHeadingsAllCapButton;
	private Text sectionHeadingsText;
	private SpecialBean footerHeaderBean;
	
	public TextBean(Text firstPara, Text leadingIndentation, 
			Text spacing, Text estimatedLength, Text pageNumberFormsText, 
			Button sectionHeadingsCapButton, Button sectionHeadingsAllCapButton,
			Text sectionHeadingsText, SpecialBean footerHeaderBean) {
		this.estimatedLength = estimatedLength;
		this.firstPara = firstPara;
		this.footerHeaderBean = footerHeaderBean;
		this.leadingIndentation = leadingIndentation;
		this.pageNumberFormsText = pageNumberFormsText;
		this.sectionHeadingsAllCapButton = sectionHeadingsAllCapButton;
		this.sectionHeadingsCapButton = sectionHeadingsCapButton;
		this.sectionHeadingsText = sectionHeadingsText;
		this.spacing = spacing;
	}
	
	public Text getFirstPara() {
		return firstPara;
	}
	public Text getLeadingIndentation() {
		return leadingIndentation;
	}
	public Text getSpacing() {
		return spacing;
	}
	public Text getEstimatedLength() {
		return estimatedLength;
	}
	public Text getPageNumberFormsText() {
		return pageNumberFormsText;
	}
	public Button getSectionHeadingsCapButton() {
		return sectionHeadingsCapButton;
	}
	public Button getSectionHeadingsAllCapButton() {
		return sectionHeadingsAllCapButton;
	}
	public Text getSectionHeadingsText() {
		return sectionHeadingsText;
	}
	public SpecialBean getFooterHeaderBean() {
		return footerHeaderBean;
	}
}
