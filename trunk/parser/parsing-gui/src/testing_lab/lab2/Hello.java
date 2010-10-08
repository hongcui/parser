package testing_lab.lab2;

import javax.swing.JApplet;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;

import fna.parsing.MainForm;

public class Hello extends JApplet {
    //Called when this applet is loaded into the browser.
    public void init() {
        //Execute a job on the event-dispatching thread; creating this applet's GUI.
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    //JLabel lbl = new JLabel("Hello World");
                    //add(lbl);
                	MainForm.launchMarker("");
                }
            });
        } catch (Exception e) {
            System.err.println("createGUI didn't complete successfully");
        }
    }
}