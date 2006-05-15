package bluej.debugmgr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import bluej.BlueJTheme;
import bluej.debugmgr.objectbench.ObjectBench;
import bluej.debugmgr.objectbench.ObjectBenchEvent;
import bluej.debugmgr.objectbench.ObjectBenchListener;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;
import bluej.utility.MultiLineLabel;

/**
 * Superclass for interactive call dialogs (method calls or free
 * form calls.
 *
 * @author  Michael Kolling
 *
 * @version $Id: CallDialog.java 4277 2006-05-15 23:43:11Z polle $
 */
public abstract class CallDialog extends EscapeDialog
	implements ObjectBenchListener
{
    public static final int OK = 0;
    public static final int CANCEL = 1;

    private MultiLineLabel errorLabel;

    private ObjectBench bench;
    private CallDialogWatcher watcher;
    
    protected JButton okButton;

    public CallDialog(JFrame parentFrame, ObjectBench objectBench, String title)
    {
        super(parentFrame, title, false);
        bench = objectBench;
    }

    /**
     * The Ok button was pressed.
     */
    public abstract void doOk();

    /**
     * The Cancel button was pressed.
     */
    public abstract void doCancel();

    /**
     * Set a watcher for events of this dialog.
     */
    public void setWatcher(CallDialogWatcher w)
    {
        watcher = w;
    }

    /**
     * callWatcher - notify watcher of dialog events.
     */
    public void callWatcher(int event)
    {
        if (watcher != null)
            watcher.callDialogEvent(this, event);
    }

    /**
     * setWaitCursor - Sets the cursor to "wait" style cursor, using swing
     *  bug workaround at present
     */
    public void setWaitCursor(boolean wait)
    {
        if(wait)
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        else
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Return the label to be used for showing error messages.
     */
    protected MultiLineLabel getErrorLabel()
    {
        if(errorLabel == null) {
            errorLabel = new MultiLineLabel("\n\n", LEFT_ALIGNMENT);
            errorLabel.setForeground(new Color(136,56,56));  // dark red
        }
        return errorLabel;
    }

    /**
     * Return the frame's object bench.
     */
    protected ObjectBench getObjectBench()
    {
        return bench;
    }

    /**
     * Start listening to object bench events.
     */
    protected void startObjectBenchListening()
    {
        if (bench != null)
            bench.addObjectBenchListener(this);
    }

    /**
     * Stop listening to object bench events.
     */
    protected void stopObjectBenchListening()
    {
        if (bench != null)
            bench.removeObjectBenchListener(this);
    }

    /**
     * setMessage - Sets a status bar style message for the dialog mainly
     *  for reporting back compiler errors upon method calls.
     */
    public void setErrorMessage(String message)
    {
        // cut the "location: __SHELL3" bit from some error messages
        int index = message.indexOf("location:");
        if(index != -1)
            message = message.substring(0,index-1);

        errorLabel.setText(message);
        pack();
        invalidate();
        validate();
    }

    /**
     * Insert text into the text field that has the focus.
     */
    public abstract void insertText(String text);

    // ---- ObjectBenchListener interface ----

    /**
     * The object was selected interactively (by clicking
     * on it with the mouse pointer).
     */
    public void objectEvent(ObjectBenchEvent obe)
    {
        NamedValue value = obe.getValue();
        insertText(value.getName());
    }

    /**
     * Build the Swing dialog. The top and center components
     * are supplied by the specific subclasses. This method
     * add the Ok and Cancel buttons.
     */
    protected void makeDialog(JComponent topComponent, JComponent centerComponent)
    {
        JPanel contentPane = (JPanel)getContentPane();

        // create the ok/cancel button panel
        JPanel buttonPanel = new JPanel();
        {
            buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

            //JButton okButton = BlueJTheme.getOkButton();
            okButton = BlueJTheme.getOkButton();
            okButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) { doOk(); }
                    });
            buttonPanel.add(okButton);

            JButton cancelButton = BlueJTheme.getCancelButton();
            cancelButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) { doCancel(); }
                    });
            buttonPanel.add(cancelButton);

            getRootPane().setDefaultButton(okButton);
        }

        //contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setLayout(new BorderLayout(6,6));
        contentPane.setBorder(BlueJTheme.generalBorder);

        if(topComponent != null)
            contentPane.add(topComponent, BorderLayout.NORTH);
        if(centerComponent != null)
            contentPane.add(centerComponent, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        pack();
        DialogManager.centreDialog(this);

        // Close Action when close button is pressed
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent event) {
                    setVisible(false);
                }
            });
    }
}
