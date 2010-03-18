/**
 * $Id$
 */
package fna.parsing;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;



/**
 * Listen to the parse process. And append the contents to the table.
 * 
 * TODO: to seperate the background code from SWT, an interface should be
 * added.
 * 
 * @author chunshui
 */
public class ProcessListener {
	
	private Table table;
	
	private ProgressBar progressBar;
	
	public ProcessListener(Table table) {
		this.table = table;
	}
	
	public ProcessListener(Table table, ProgressBar progressBar) {
		this.table = table;
		this.progressBar = progressBar;
	}
	
	public void info(String... contents) {
		TableItem item = new TableItem(table, SWT.NONE);
		if (contents.length > 1) {
			contents[1] = contents[1].substring(contents[1].lastIndexOf("\\")+1);
		}
	    item.setText(contents);	
	}


	
	public void progress(int selection) {
		progressBar.setSelection(selection);
	}
	
	public void clear() {
		table.removeAll();
	}
	
	public Table getTable() {
		return this.table;
	}
}
