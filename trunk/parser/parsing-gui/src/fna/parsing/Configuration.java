package fna.parsing;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.widgets.Button;

import com.swtdesigner.SWTResourceManager;

public class Configuration {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new Configuration().viewConfigurationForm();
	}
	
	
	/**
	 * @wbp.parser.entryPoint
	 */
	public void viewConfigurationForm () {
		final Display display = Display.getDefault();
		
		final Shell shell = new Shell();
		shell.setSize(800, 600);
		shell.setText("Choose a document style");
	
		Group group = new Group(shell, SWT.NONE);
		group.setBounds(20, 10, 740, 65);
		
		Label label = new Label(group, SWT.NONE);
		label.setBounds(10, 23, 607, 32);
		label.setText("Please choose a type that closely matches the document you are using for semantic mark-up :");
		
		Button button = new Button(group, SWT.RADIO);
		button.setBounds(527, 23, 90, 16);
		button.setText("Radio Button");
		
		Group group_1 = new Group(shell, SWT.NONE);
		group_1.setBounds(20, 98, 740, 456);
		
		Button button_1 = new Button(group_1, SWT.RADIO);
		button_1.setBounds(10, 29, 90, 16);
		button_1.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent mEvent){
				new Type1Document().showType1Document();
				shell.dispose();
				//call the parser application
				MainForm.main(new String[1]);
			}
			
			public void mouseDown(MouseEvent mEvent) { }
			public void mouseDoubleClick(MouseEvent mEvent) {}
		});
		button_1.setText("Type 1");
		
		Button button_2 = new Button(group_1, SWT.RADIO);
		button_2.setBounds(10, 166, 90, 16);
		button_2.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent mEvent){
				ApplicationUtilities.showPopUpWindow("You clicked type2" , "Info", SWT.ICON_INFORMATION);
			}
			
			public void mouseDown(MouseEvent mEvent) { }
			public void mouseDoubleClick(MouseEvent mEvent) {}
		});
		button_2.setText("Type 2");
		
		Button button_3 = new Button(group_1, SWT.RADIO);
		button_3.setBounds(10, 307, 90, 16);
		button_3.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent mEvent){
				ApplicationUtilities.showPopUpWindow("You clicked type3" , "Info", SWT.ICON_INFORMATION);
			}
			
			public void mouseDown(MouseEvent mEvent) { }
			public void mouseDoubleClick(MouseEvent mEvent) {}
		});
		button_3.setText("Type 3");
		
		Label label_1 = new Label(group_1, SWT.NONE);
		label_1.setBackgroundImage(SWTResourceManager.getImage(Configuration.class, 
				ApplicationUtilities.getProperty("application.logo")));
		label_1.setBounds(106, 167, 611, 127);
		
		Label label_2 = new Label(group_1, SWT.NONE);
		label_2.setBackgroundImage(SWTResourceManager.getImage(Configuration.class, 
				ApplicationUtilities.getProperty("application.logo")));
		label_2.setBounds(106, 30, 611, 121);
		
		Label label_3 = new Label(group_1, SWT.NONE);
		label_3.setBackgroundImage(SWTResourceManager.getImage(Configuration.class, 
				ApplicationUtilities.getProperty("application.logo")));
		label_3.setBounds(106, 308, 611, 127);
	
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}
	
	
	
}
