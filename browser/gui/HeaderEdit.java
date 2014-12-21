package edu.illinois.i3.genre.pagetagger.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
public class HeaderEdit extends JDialog {
    /**
     * author: Mike Black @mblack884
     *
     * This class is a pop-up dialog box with two text fields for use when editing the
     * ARFF's header and relation fields.  To use:
     * - Initialize with constructor (no data passed in)
     * - Use setHeading(String[]) to pass in the target's prediction's header
     * - In the owner class, call the display function of the initialized HeaderEdit
     * - Use getHeading to retrieve the user's input after the dialog has been closed
     * - Optionally, use getResult if you want to see if user saved header or cancelled
     */

    private JTextArea headingField;
    private JTextField relationField;
    private JScrollPane headingScroll;
    private JLabel headingLabel, relationLabel;
    private JButton accept,cancel;
    private JPanel fieldsPanel,buttonsPanel,headingPanel,relationPanel;
    private Boolean result;

    public HeaderEdit() {
        /**
         * Basic constructor to call all the window object setting methods.  Does not
         * set the text data that appears in the input fields.
         */
        setModalityType(ModalityType.APPLICATION_MODAL);
        setAlwaysOnTop(true);
        setLocationRelativeTo(null);
        setResizable(false);
        drawGUI();
        defineListeners();
        result = false;
    }

    private void drawGUI() {
        /**
         * Initializes all of the graphics objects and positions them within nested
         * panels/layout managers.  Panels used by groups are objects are initialized
         * in the same section as those objects.
         */
        setSize(300, 300);

        // Create the text boxes and labels for heading & relation edits
        fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BorderLayout());
        headingPanel = new JPanel();
        headingLabel = new JLabel("Heading:");
        headingField = new JTextArea();
        headingScroll = new JScrollPane(headingField);
        headingPanel.setLayout(new BorderLayout());
        headingPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        headingPanel.add(headingLabel,BorderLayout.NORTH);
        headingPanel.add(headingScroll,BorderLayout.CENTER);
        fieldsPanel.add(headingPanel,BorderLayout.CENTER);
        relationPanel = new JPanel();
        relationPanel.setLayout(new BoxLayout(relationPanel,BoxLayout.X_AXIS));
        relationLabel = new JLabel("Relation:");
        relationField = new JTextField();
        relationPanel.add(Box.createHorizontalGlue());
        relationPanel.add(relationLabel);
        relationPanel.add(relationField);
        relationPanel.add(Box.createHorizontalGlue());
        headingPanel.add(relationPanel,BorderLayout.SOUTH);

        // Create the buttons
        buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel,BoxLayout.X_AXIS));
        accept = new JButton("Accept");
        cancel = new JButton("Cancel");
        buttonsPanel.add(Box.createHorizontalGlue());
        buttonsPanel.add(accept);
        buttonsPanel.add(cancel);
        buttonsPanel.add(Box.createHorizontalGlue());
        buttonsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Set groups into dialog's primary layout manager
        setLayout(new BorderLayout());
        add(headingPanel,BorderLayout.CENTER);
        add(buttonsPanel,BorderLayout.SOUTH);
    }

    private void defineListeners() {
        /**
         * Sets all the button commands (ActionListeners).  This function creates them as
         * anonymous subclasses.  It's hacky, and for a bigger program it would probably
         * be better to define them each separately.  The overall function of each button
         * is described in comments preceding the ActionListener definitions.
         */

        accept.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /**
                 * Sets "result" to true, indicating that user choice to save their entry.
                 */
                result = true;
                dispose();
            }
        });

        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /**
                 * Sets "result" to false, indicating that user choice to dismiss their entry.
                 */
                result = false;
                dispose();
            }
        });
    }

    void setHeading (String[] input) {
        /**
         * Accepts the heading as a String array, where each cell is a line of text stripped
         * of any trailing white space or newline characters.  This function will format the
         * array into a single String, inserting newlines where necessary.  It then stores
         * it the heading field.
         */
        String single = new String();
        for(int i=0;i<input.length;i++) {
            single += input[i];
            if (i<input.length-1){
                single += "\n";
            }
        }
        headingField.setText(single);
    }

    String[] getHeading () {
        /**
         * This will strip the newlines from the header's textfield and return it
         * as a String array.  Use to retrieve a user's input after the dialog box
         * has been closed.
         */
        String[] output = headingField.getText().split("\\n");
        for (int i=0;i<output.length;i++) {
            if (output[i].startsWith("% ") || output[i].startsWith("%")) {
                continue;
            }
            output[i] = "% " + output[i];
        }
        return output;
    }

    void setRelation (String input) {
        /**
         * Used to set the textfield for the relation from the target ARFF.
         */
        relationField.setText(input);
    }

    String getRelation () {
        /**
         * Used to retrieve the relation from the textfield after the dialog has been
         * closed by the user (to store it in the target ARFF).
         */
        return relationField.getText();
    }

    Boolean getResult () {
        /**
         * Check to see whether the user decided to save or dismiss their input.  This keeps
         * the variable for internal use only, not allowing it to be set from outside of the
         * class.
         */
        return result;
    }
}
