package edu.illinois.i3.genre.pagetagger.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
public class RangeEdit extends JDialog {

    /**
     * This class is a customized pop-up with four fields that allows users to
     * edit the Range values for a given prediction.  This class is necessary because
     * the table that holds table is not editable (to prevent users from accidentally
     * creating errors in the HTids).
     */

    private JLabel pageStartLabel,pageEndLabel,partStartLabel,partEndLabel;
    private JTextField pageStartField,pageEndField,partStartField,partEndField;
    private JButton accept, cancel;
    private JPanel fieldsPanel,buttonsPanel;
    private Boolean result;

    public RangeEdit() {
        setModalityType(ModalityType.APPLICATION_MODAL);
        setAlwaysOnTop(true);
        setLocationRelativeTo(null);
        setResizable(false);
        drawGUI();
        defineListeners();
        result = false;
    }

    public void drawGUI () {
        /**
         * Initializes all of the graphics objects and positions them within nested
         * panels/layout managers.  Panels are initialized separately here, but objects
         * (including nested panels) are added in groups to their respective containers.
         */

        // Set shared UI size objects
        Dimension objectSize = new Dimension(100,30);

        // Set layout managers & dialog window size
        setSize(200,200);
        setLayout(new BorderLayout());
        fieldsPanel = new JPanel(new GridLayout(4,2,5,5));
        fieldsPanel.setBorder(new EmptyBorder(5, 8, 5, 5));
        buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel,BoxLayout.X_AXIS));
        buttonsPanel.setBorder(new EmptyBorder(5, 8, 5, 5));

        // Initialize & configure objects
        pageStartLabel = new JLabel("Page Start:");
        pageStartLabel.setPreferredSize(objectSize);
        pageEndLabel = new JLabel("Page End:");
        pageEndLabel.setPreferredSize(objectSize);
        partStartLabel = new JLabel("Part Start:");
        partStartLabel.setPreferredSize(objectSize);
        partEndLabel = new JLabel("Part End:");
        partEndLabel.setPreferredSize(objectSize);
        pageStartField = new JTextField();
        pageStartField.setPreferredSize(objectSize);
        pageEndField = new JTextField();
        pageEndField.setPreferredSize(objectSize);
        partStartField = new JTextField();
        partStartField.setPreferredSize(objectSize);
        partEndField = new JTextField();
        partEndField.setPreferredSize(objectSize);
        accept = new JButton("Accept");
        cancel = new JButton("Cancel");

        //Add objects in pairs to the field/label grid panel
        fieldsPanel.add(pageStartLabel);
        fieldsPanel.add(pageStartField);
        fieldsPanel.add(pageEndLabel);
        fieldsPanel.add(pageEndField);
        fieldsPanel.add(partStartLabel);
        fieldsPanel.add(partStartField);
        fieldsPanel.add(partEndLabel);
        fieldsPanel.add(partEndField);

        //Add buttons to button box panel
        buttonsPanel.add(Box.createHorizontalGlue());
        buttonsPanel.add(accept);
        buttonsPanel.add(cancel);
        buttonsPanel.add(Box.createHorizontalGlue());

        //Add panels to dialog layout
        add(fieldsPanel,BorderLayout.CENTER);
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
                 * Remembers that users wanted to save their changes so that when the
                 * object is queried after it closes, those changes can be transferred
                 * to the prediction table.
                 */
                result = true;
                dispose();
            }
        });

        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /**
                 * Remembers that users wanted to dismiss their changes so that when the
                 * object is queried after it closes, those changes will be ignored.
                 */
                result = false;
                dispose();
            }
        });
    }

    public void setRange (String[] range) {
        /**
         * This accepts an array of values passed in from the PredictionTableModel and
         * displays them in the text fields for editing. For use with PredictionTableModel,
         * which includes a method for retrieving a record's range as an array.
         */
        pageStartField.setText(range[0]);
        pageEndField.setText(range[1]);
        partStartField.setText(range[2]);
        partEndField.setText(range[3]);
    }

    public String[] getRange () {
        /**
         * Takes the values stored in the text fields at the moment the dialog box was
         * closed and passed them back out as an array.  For use with PredictionTableModel,
         * which includes a method for setting a record's range using an array.
         */
        String[] range = new String[4];
        range[0] = pageStartField.getText();
        range[1] = pageEndField.getText();
        range[2] = partStartField.getText();
        range[3] = partEndField.getText();
        return range;
    }

    public Boolean getResult () {
        /**
         * Used to query whether or not users wanted to save changes made to range values.
         */
        return result;
    }

}
