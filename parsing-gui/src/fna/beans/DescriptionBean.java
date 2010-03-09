package fna.beans;

import java.util.HashMap;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class DescriptionBean {
	private Button yesButton;
	private Button noButton;
	private Combo OtherInfo;
	private HashMap <Integer, Text> descriptions;
	private HashMap <Integer, Label> sections;
	
	public DescriptionBean(Button yesButton, Button noButton, Combo otherInfo,
			HashMap<Integer, Text> descriptions,
			HashMap<Integer, Label> sections) {
		super();
		this.yesButton = yesButton;
		this.noButton = noButton;
		OtherInfo = otherInfo;
		this.descriptions = descriptions;
		this.sections = sections;
	}

	/**
	 * @return the yesButton
	 */
	public Button getYesButton() {
		return yesButton;
	}

	/**
	 * @param yesButton the yesButton to set
	 */
	public void setYesButton(Button yesButton) {
		this.yesButton = yesButton;
	}

	/**
	 * @return the noButton
	 */
	public Button getNoButton() {
		return noButton;
	}

	/**
	 * @param noButton the noButton to set
	 */
	public void setNoButton(Button noButton) {
		this.noButton = noButton;
	}

	/**
	 * @return the otherInfo
	 */
	public Combo getOtherInfo() {
		return OtherInfo;
	}

	/**
	 * @param otherInfo the otherInfo to set
	 */
	public void setOtherInfo(Combo otherInfo) {
		OtherInfo = otherInfo;
	}

	/**
	 * @return the descriptions
	 */
	public HashMap<Integer, Text> getDescriptions() {
		return descriptions;
	}

	/**
	 * @param descriptions the descriptions to set
	 */
	public void setDescriptions(HashMap<Integer, Text> descriptions) {
		this.descriptions = descriptions;
	}

	/**
	 * @return the sections
	 */
	public HashMap<Integer, Label> getSections() {
		return sections;
	}

	/**
	 * @param sections the sections to set
	 */
	public void setSections(HashMap<Integer, Label> sections) {
		this.sections = sections;
	}

	
	
	
}
