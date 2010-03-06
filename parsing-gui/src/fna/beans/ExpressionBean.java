package fna.beans;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ExpressionBean {

	private Label label;
	private Text text;
	private double random = Math.random()*Math.random()*Math.E;
	
	public ExpressionBean(Label label, Text text) {
		this.label = label;
		this.text = text;
	}
	
	public Label getLabel() {
		return label;
	}
	public Text getText() {
		return text;
	}
	
	public boolean equals(Object obj) {
		
		if (this == obj) {
			return true ;
		}
		
		if(!(obj instanceof ExpressionBean)) {
			return false;
		}
		
		ExpressionBean expBean = (ExpressionBean)obj;
		return label.equals(expBean.label) &&
			text.equals(expBean.text);
	}
	
	public int hashCode(){
		return label.hashCode() * 
			text.hashCode() * (int)random * 1009707 ;
	}
	
}
