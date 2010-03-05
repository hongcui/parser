package fna.beans;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NomenclatureBean {

	/**
	 * @param args
	 */
	
	private Group parent;
	private Button yesRadioButton;
	private Button noRadioButton;
	private Text description;
	private Label label;
	private double random = Math.random() * Math.random() * Math.random();
	
	public NomenclatureBean(Group parent, Button yesRadioButton, Button noRadioButton, Text description, Label label) {
		this.parent = parent;
		this.description = description;
		this.noRadioButton = noRadioButton;
		this.yesRadioButton = yesRadioButton;
		this.label = label;
	}
	

	public Label getLabel() {
		return label;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public Group getParent() {
		return parent;
	}


	public Button getYesRadioButton() {
		return yesRadioButton;
	}


	public Button getNoRadioButton() {
		return noRadioButton;
	}

	public Text getDescription() {
		return description;
	}
	
	public boolean equals(Object obj) {
		if (this == obj) {
			return true ;
		}
		
		if(!(obj instanceof NomenclatureBean)) {
			return false;
		}
		
		NomenclatureBean nbean = (NomenclatureBean) obj;
		
		return this.parent.equals(nbean.parent) &&
				this.noRadioButton.equals(nbean.noRadioButton) &&
				this.yesRadioButton.equals(nbean.yesRadioButton) &&
				this.label.equals(nbean.label);
		
	}
	
	public int hashCode() {			
		return  this.description.hashCode() * 
			this.noRadioButton.hashCode() * 
			this.yesRadioButton.hashCode() *
			this.label.hashCode() *
			(int) (random * 10000000);
	}

	

}
