package fna.beans;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.swtdesigner.SWTResourceManager;

public class TermBean {
	private Text termText;
	private Label delete;
	private boolean togglePosition;
	private Group termGroup;
	private Group parentGroup;	
	private Group deletedGroup;
	
	/* Coordinates for the Text inside any Terms group */
	private static Rectangle textCood = new Rectangle(10, 10, 100, 20);
	/* Coordinates of the Cross Label inside the Term group*/
	private static Rectangle delCood = new Rectangle(110, 10, 15, 15);



	public TermBean(Group termGroup, Group deletedGroup, boolean toggleGroup, String text) {
		termText = new Text(termGroup, SWT.BORDER);
		termText.setBounds(textCood);
		termText.setEditable(false);
		termText.setText(text);
		
		delete = new Label(termGroup, SWT.NONE);
		delete.setImage(SWTResourceManager.getImage(TermBean.class, "/fna/parsing/remove.jpg"));
		delete.setBounds(delCood);
		delete.addMouseListener(new MouseListener() {
			public void mouseDown(MouseEvent me){
				changeParentGroup();
			}
			public void mouseUp(MouseEvent me){	}
			public void mouseDoubleClick(MouseEvent me){	}
		});
		
		this.togglePosition = toggleGroup;
		this.termGroup = termGroup;
		this.parentGroup = (Group)termGroup.getParent();			
		this.deletedGroup = deletedGroup;
		
	}

	private void changeParentGroup() {

		if(togglePosition) {
			togglePosition = false;
			Rectangle rect = termGroup.getBounds();
			if (rect.x == 40) {
				rect.x = 10;
			} else {
				rect.x = 160;
			}
			
			termGroup.setParent(deletedGroup);
			termGroup.setBounds(rect);
			((ScrolledComposite)deletedGroup.getParent()).setMinSize(deletedGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		} else {
			togglePosition = true;
			Rectangle rect = termGroup.getBounds();
			
			if (rect.x == 10) {
				rect.x = 40;
			} else {
				rect.x = 210;
			}
			termGroup.setParent(parentGroup);
			termGroup.setBounds(rect);
		}
	}

	/**
	 * @return the termGroup
	 */
	public Group getTermGroup() {
		return termGroup;
	}

	/**
	 * @param termGroup the termGroup to set
	 */
	public void setTermGroup(Group termGroup) {
		this.termGroup = termGroup;
	}

	/**
	 * @return the togglePosition
	 */
	public boolean isTogglePosition() {
		return togglePosition;
	}

	/**
	 * @param togglePosition the togglePosition to set
	 */
	public void setTogglePosition(boolean togglePosition) {
		this.togglePosition = togglePosition;
	}

	
}
