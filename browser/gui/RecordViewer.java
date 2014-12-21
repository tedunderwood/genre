package edu.illinois.i3.genre.pagetagger.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

@SuppressWarnings("serial")
public class RecordViewer extends JDialog {
    /**
     * This class displays the full contents of a volume's metadata record from
     * the Derby database in a pop-up window.  It accepts the record as a String
     * array with fields in the following order: htid, volume number, call number,
     * author, title, publisher, data, copy information, subject keywords.  It accepts
     * data from the SearchResults getSelection method (so any changes there must be
     * reflected here, and vice versa).
     */

    JLabel htidLabel, volnumLabel, callnumLabel, authorLabel, titleLabel, publishLabel, dateLabel, copyLabel, subjectLabel;
    JTextField htidField, volnumField, callnumField, authorField, dateField, copyField;
    JTextArea titleArea, publishArea, subjectArea;
    JScrollPane titleScroll, publishScroll, subjectScroll;
    JButton close;
    JPanel labelsPane, fieldsPane;

    public RecordViewer (String[] record) {
        /**
         * Basic constructor that initializes textfields with the passed in record
         * String array and then calls the UI drawing and action defining methods.
         */

        // Set pop-up properties
        setModalityType(ModalityType.APPLICATION_MODAL);
        setLocationRelativeTo(null);
        setResizable(false);
        setAlwaysOnTop(true);

        //Pass record data to relevant fields & add to ArrayList for management
        htidField = new JTextField(record[0]);
        volnumField = new JTextField(record[1]);
        callnumField = new JTextField(record[2]);
        authorField = new JTextField(record[3]);
        titleArea = new JTextArea(record[4]);
        publishArea = new JTextArea(record[5]);
        dateField = new JTextField(record[6]);
        copyField = new JTextField(record[7]);
        subjectArea = new JTextArea(record[8]);

        //Draw dialog window objects and configure interactivity
        drawGUI();
        defineListeners();
    }

    private void drawGUI() {
        /**
         * Initializes all of the graphics objects and positions them within nested
         * panels/layout managers.  Panels used by groups are objects are initialized
         * in the same section as those objects.
         */

        // Initialize shared size definitions and set window size property
        Dimension areaSize = new Dimension(269,75);
        Dimension fieldSize = new Dimension(275,25);
        Dimension labelSizeSmall = new Dimension(100,25);
        Dimension labelSizeBig = new Dimension(100,75);
        setSize(440,500);

        // Initialize layout manager & column+cell property objects
        setLayout(new GridBagLayout());
        GridBagConstraints labelProperties= new GridBagConstraints();
        GridBagConstraints fieldProperties = new GridBagConstraints();
        GridBagConstraints fullRowProperties = new GridBagConstraints();
        labelProperties.gridwidth = 1;
        labelProperties.gridx = 0;
        fieldProperties.gridwidth = 3;
        fieldProperties.gridx = 1;
        fieldProperties.insets = new Insets(3,3,3,3);
        fullRowProperties.gridwidth = 4;
        fullRowProperties.gridx = 0;

        //Initialize & configure static components
        htidLabel = new JLabel("HTID:");
        htidLabel.setPreferredSize(labelSizeSmall);
        volnumLabel = new JLabel("Volume ID:");
        volnumLabel.setPreferredSize(labelSizeSmall);
        callnumLabel = new JLabel("Call Number:");
        callnumLabel.setPreferredSize(labelSizeSmall);
        authorLabel = new JLabel("Author:");
        authorLabel.setPreferredSize(labelSizeSmall);
        titleLabel = new JLabel("Title:");
        titleLabel.setPreferredSize(labelSizeBig);
        publishLabel = new JLabel("Publisher:");
        publishLabel.setPreferredSize(labelSizeBig);
        dateLabel = new JLabel("Date:");
        dateLabel.setPreferredSize(labelSizeSmall);
        copyLabel = new JLabel("Copy:");
        copyLabel.setPreferredSize(labelSizeSmall);
        subjectLabel = new JLabel("Subject:");
        subjectLabel.setPreferredSize(labelSizeBig);
        close = new JButton("Close");

        //Initialize & configure textfield containers
        titleScroll = new JScrollPane(titleArea);
        titleScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        titleScroll.setPreferredSize(areaSize);
        titleArea.setLineWrap(true);
        titleArea.setWrapStyleWord(true);
        titleArea.setEditable(false);
        publishScroll = new JScrollPane(publishArea);
        publishScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        publishScroll.setPreferredSize(areaSize);
        publishArea.setLineWrap(true);
        publishArea.setWrapStyleWord(true);
        publishArea.setEditable(false);
        subjectScroll = new JScrollPane(subjectArea);
        subjectScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        subjectScroll.setPreferredSize(areaSize);
        subjectArea.setLineWrap(true);
        subjectArea.setWrapStyleWord(true);
        subjectArea.setEditable(false);

        //Configure uncontained textfields
        htidField.setPreferredSize(fieldSize);
        htidField.setEditable(false);
        volnumField.setPreferredSize(fieldSize);
        volnumField.setEditable(false);
        callnumField.setPreferredSize(fieldSize);
        callnumField.setEditable(false);
        authorField.setPreferredSize(fieldSize);
        authorField.setEditable(false);
        dateField.setPreferredSize(fieldSize);
        dateField.setEditable(false);
        copyField.setPreferredSize(fieldSize);
        copyField.setEditable(false);

        //Add pairs of objects to columns using column+cell property objects
        labelProperties.gridy = 0;
        fieldProperties.gridy = 0;
        add(htidLabel,labelProperties);
        add(htidField,fieldProperties);
        labelProperties.gridy = 1;
        fieldProperties.gridy = 1;
        add(volnumLabel,labelProperties);
        add(volnumField,fieldProperties);
        labelProperties.gridy = 2;
        fieldProperties.gridy = 2;
        add(callnumLabel,labelProperties);
        add(callnumField,fieldProperties);
        labelProperties.gridy = 3;
        fieldProperties.gridy = 3;
        add(authorLabel,labelProperties);
        add(authorField,fieldProperties);
        labelProperties.gridy = 4;
        fieldProperties.gridy = 4;
        add(titleLabel,labelProperties);
        add(titleScroll,fieldProperties);
        labelProperties.gridy = 5;
        fieldProperties.gridy = 5;
        add(publishLabel,labelProperties);
        add(publishScroll,fieldProperties);
        labelProperties.gridy = 6;
        fieldProperties.gridy = 6;
        add(dateLabel,labelProperties);
        add(dateField,fieldProperties);
        labelProperties.gridy = 7;
        fieldProperties.gridy = 7;
        add(copyLabel,labelProperties);
        add(copyField,fieldProperties);
        labelProperties.gridy = 8;
        fieldProperties.gridy = 8;
        add(subjectLabel,labelProperties);
        add(subjectScroll,fieldProperties);
        fullRowProperties.gridy = 9;
        add(close,fullRowProperties);
    }

    private void defineListeners() {
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }
}
