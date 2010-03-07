package fna.beans;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;

public class SpecialBean {
	private Button glossButton;
	private Button refButton;
	private Text glossText;
	private Text refText;
	
	public SpecialBean(Button glossButton, 
			Button refButton,Text glossText, Text refText) {
		this.glossButton = glossButton;
		this.glossText = glossText;
		this.refButton = refButton;
		this.refText = refText;
	}

	
}
