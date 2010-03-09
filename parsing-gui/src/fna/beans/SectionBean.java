package fna.beans;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class SectionBean {

	private Text order;
	private Label section;
	private Text startTokens;
	private Text endTokens;
	private Text embeddedTokens;
	public SectionBean(Text order, Label section, Text startTokens,
			Text endTokens, Text embeddedTokens) {
		super();
		this.order = order;
		this.section = section;
		this.startTokens = startTokens;
		this.endTokens = endTokens;
		this.embeddedTokens = embeddedTokens;
	}
	/**
	 * @return the order
	 */
	public Text getOrder() {
		return order;
	}
	/**
	 * @param order the order to set
	 */
	public void setOrder(Text order) {
		this.order = order;
	}
	/**
	 * @return the section
	 */
	public Label getSection() {
		return section;
	}
	/**
	 * @param section the section to set
	 */
	public void setSection(Label section) {
		this.section = section;
	}
	/**
	 * @return the startTokens
	 */
	public Text getStartTokens() {
		return startTokens;
	}
	/**
	 * @param startTokens the startTokens to set
	 */
	public void setStartTokens(Text startTokens) {
		this.startTokens = startTokens;
	}
	/**
	 * @return the endTokens
	 */
	public Text getEndTokens() {
		return endTokens;
	}
	/**
	 * @param endTokens the endTokens to set
	 */
	public void setEndTokens(Text endTokens) {
		this.endTokens = endTokens;
	}
	/**
	 * @return the embeddedTokens
	 */
	public Text getEmbeddedTokens() {
		return embeddedTokens;
	}
	/**
	 * @param embeddedTokens the embeddedTokens to set
	 */
	public void setEmbeddedTokens(Text embeddedTokens) {
		this.embeddedTokens = embeddedTokens;
	}
	
	
}
