package fna.parsing;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.nebula.widgets.formattedtext.FormattedText;
import java.awt.TextArea;

public class Type3Document {
	private Text text;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	/**
	 * @wbp.parser.entryPoint
	 */
	public void showType3Document() {
		final Display display = Display.getDefault();
		
		final Shell shell = new Shell();
		shell.setSize(606, 315);
		shell.setText("Type 3");
		shell.setLayout(new RowLayout(SWT.HORIZONTAL));
		
		final Group group = new Group(shell, SWT.NONE);
		group.setLayoutData(new RowData(577, 256));
		group.setBounds(10, 10, 500, 115);
		
		Button button = new Button(group, SWT.NONE);
		button.setBounds(399, 239, 75, 25);
		button.setText("Save");
		
		Button button_1 = new Button(group, SWT.NONE);
		button_1.setBounds(498, 239, 75, 25);
		button_1.setText("Skip");
		
		Label label = new Label(group, SWT.NONE);
		label.setBounds(10, 10, 479, 15);
		label.setText("If you have a sample paragraph for morphological description, please paste that below : ");
		
		Label label_1 = new Label(group, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_1.setBounds(10, 31, 563, 2);
		
		text = new Text(group, SWT.BORDER | SWT.MULTI| SWT.WRAP | SWT.V_SCROLL);
		text.setBounds(10, 39, 563, 191);
	}
}
