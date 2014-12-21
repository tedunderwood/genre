package edu.illinois.i3.genre.pagetagger.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import edu.illinois.i3.genre.pagetagger.backend.Preferences;
import edu.illinois.i3.genre.pagetagger.backend.VolumeReader;

@SuppressWarnings("serial")
public class PageMapper extends JFrame {
    /**
     * author: Mike Black @mlblack884
     *
     * This class allows the user to create page maps of volumes that are locally
     * accessible. As currently written, it will assumes users are working with flat
     * directories of volumes with legalized HTid filenames.  Support for the full
     * pairtree structure is built into the VolumeReader class that it relies on to
     * read data from the disk, but PageMapper will not ask users whether data directory
     * is a flat or pairtree.
     *
     * Many of the functions below are ported with as little modification from
     * @tunderwood's Python script.  TreeMaps are used to store page codes and capitalized
     * line percentages because they are similar in function to Python dictionaries.  Some
     * wrappers were necessary to make them iterable.
     *
     * See user documentation for more information on how pagemapping works.
     */

    private JPanel pagePanel,actionsPanel,movePanel,centerPanel,savePanel,storedPanel;
    private JPanel generalCodesPanel, pageCodesPanel;
    private JLabel position,storedCount,currentPageTag;
    private JTable storedTable;
    private JTextArea pagetextArea;
    private JTextField scanNumField,skipNumField;
    private JButton store,scan,save,load,cancel,skip;
    private JButton navFirst,navPrev,navNext,navLast;
    private JScrollPane pageScroll,storedScroll;
    private JCheckBox reverse;
    private final DefaultTableModel storedModel;
    private final VolumeReader volume;
    private int current;
    private JComboBox codeBox;
    private final TreeMap<Integer,String> codeDictionary;
    private final TreeMap<Integer,Double> capsDictionary;
    private final double threshold = 2.25; // If you want to change stdev threshold...
    boolean complete;

    // Passed in via Preferences
    private final String[][] generalCodes, pageCodes;
    private final String pageMapDir;

    public PageMapper (VolumeReader input, Preferences p) {
        /**
         * Constructor requires both a volume, and the preferences class (this latter is
         * so that the program will not have to prompt users everytime for a list of
         * allowable genre codes).  The codes themselves are read from disk by Preferences
         * during the program's intialization sequence.
         */
        generalCodes = p.getGeneralCodes();
        pageCodes = p.getPageCodes();
        pageMapDir = p.getMapDir();
        volume = input;
        current = 0;
        codeDictionary = new TreeMap<Integer,String>();
        capsDictionary = new TreeMap<Integer,Double>();
        storedModel = new DefaultTableModel();
        complete = false;
        defineGUI();
        buildCodeBox();
        displayPage(volume.getPage(current));
        setTitle(input.getHTID());
        defineListeners();
    }

    private void defineGUI() {
        /**
         * Initializes all of the graphics objects and positions them within nested
         * panels/layout managers.  Panels used by groups are objects are initialized
         * in the same section as those objects.
         */

        // Shared look and feel variables used for configuring sets of objects
        Dimension buttonSize = new Dimension(100,30);
        Dimension textSize = new Dimension(100,30);
        Dimension storedSize = new Dimension(200,100);
        GridBagConstraints buttonProperties = new GridBagConstraints();
        buttonProperties.gridx = 0;
        GridBagConstraints textProperties = new GridBagConstraints();
        buttonProperties.gridx = 1;

        // Initializes all panels, layout managers, spacing
        setMinimumSize(new Dimension(750,750));
        setLayout(new BorderLayout());
        pagePanel = new JPanel();
        pagePanel.setLayout(new BorderLayout());
        pagePanel.setBorder(new EmptyBorder(10, 10, 5, 10));
        pagePanel.setPreferredSize(new Dimension(600,500));
        actionsPanel = new JPanel();
        actionsPanel.setLayout(new BorderLayout());
        actionsPanel.setBorder(new EmptyBorder(5, 10, 10, 10));
        actionsPanel.setPreferredSize(new Dimension(600,200));
        movePanel = new JPanel();
        movePanel.setLayout(new GridBagLayout());
        movePanel.setMaximumSize(new Dimension(225,150));
        centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel,BoxLayout.X_AXIS));
        savePanel = new JPanel();
        savePanel.setLayout(new BoxLayout(savePanel,BoxLayout.X_AXIS));
        storedPanel = new JPanel();
        storedPanel.setLayout(new BoxLayout(storedPanel,BoxLayout.Y_AXIS));

        // Intializes all of the graphics objects, passes in variables defined above
        position = new JLabel();
        position.setHorizontalAlignment(SwingConstants.LEFT);
        position.setHorizontalTextPosition(SwingConstants.LEFT);
        position.setPreferredSize(new Dimension(100, 20));
        currentPageTag = new JLabel();
        currentPageTag.setHorizontalAlignment(SwingConstants.RIGHT);
        currentPageTag.setHorizontalTextPosition(SwingConstants.RIGHT);
        currentPageTag.setPreferredSize(new Dimension(100,20));
        pagetextArea = new JTextArea();
        pagetextArea.setEditable(false);
        pageScroll = new JScrollPane(pagetextArea);
        scanNumField = new JTextField();
        scanNumField.setPreferredSize(textSize);
        skipNumField = new JTextField();
        skipNumField.setPreferredSize(textSize);
        scan = new JButton("Scan To");
        scan.setPreferredSize(buttonSize);
        skip = new JButton("Skip To");
        skip.setPreferredSize(buttonSize);
        store = new JButton("Store Code");
        store.setPreferredSize(buttonSize);
        codeBox = new JComboBox();
        reverse = new JCheckBox("Reverse");
        reverse.setSelected(false);
        reverse.setPreferredSize(textSize);
        save = new JButton("Save");
        save.setPreferredSize(buttonSize);
        load = new JButton("Load");
        load.setPreferredSize(buttonSize);
        cancel = new JButton("Cancel");
        cancel.setPreferredSize(buttonSize);
        storedTable = new JTable(storedModel);
        storedTable.setEnabled(false);
        storedScroll = new JScrollPane(storedTable);
        storedScroll.setPreferredSize(storedSize);
        storedScroll.setAlignmentX(LEFT_ALIGNMENT);
        storedCount = new JLabel("Stored: 0");
        storedCount.setAlignmentX(LEFT_ALIGNMENT);

        // Layout sequence for the navigation/assignment buttons.  Uses a GridBagLayout,
        // so the position of each but me updated manually before adding to the layout
        buttonProperties.gridy = 0;
        textProperties.gridy = 0;
        movePanel.add(store,buttonProperties);
        movePanel.add(reverse,textProperties);
        buttonProperties.gridy = 1;
        textProperties.gridy = 1;
        movePanel.add(scan,buttonProperties);
        movePanel.add(scanNumField,textProperties);
        buttonProperties.gridy = 2;
        textProperties.gridy = 2;
        movePanel.add(skip,buttonProperties);
        movePanel.add(skipNumField,textProperties);

        // Layout sequence for save/cancel buttons
        savePanel.add(Box.createHorizontalGlue());
        savePanel.add(save);
        savePanel.add(cancel);
        savePanel.add(Box.createHorizontalGlue());

        // Define page navigation buttons
        navFirst = new JButton("|<");
        navPrev = new JButton("<");
        navNext = new JButton(">");
        navLast = new JButton(">|");

        JPanel navPanel = new JPanel();
        //navPanel.setHorizontalAlignment(SwingConstants.CENTER);
        //navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.X_AXIS));
        //navPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        navPanel.add(navFirst);
        navPanel.add(navPrev);
        navPanel.add(navNext);
        navPanel.add(navLast);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.add(position);
        topPanel.add(navPanel);
        topPanel.add(currentPageTag);

        // Layout sequence for page display
        pagePanel.add(topPanel,BorderLayout.NORTH);
        pagePanel.add(pageScroll,BorderLayout.CENTER);

        // Layout sequence for the stored counter and table display
        storedPanel.add(storedCount);
        storedPanel.add(storedScroll);

        // Layout sequence for the meta-panel that contains navigation and stored display
        centerPanel.add(Box.createHorizontalGlue());
        centerPanel.add(movePanel);
        centerPanel.add(Box.createHorizontalGlue());
        centerPanel.add(storedPanel);
        centerPanel.add(Box.createHorizontalGlue());

        // Layout sequence for the meta-panel that contains all the interactive objects
        actionsPanel.add(codeBox,BorderLayout.NORTH);
        actionsPanel.add(centerPanel,BorderLayout.CENTER);
        actionsPanel.add(savePanel,BorderLayout.SOUTH);

        generalCodesPanel = new JPanel();
        generalCodesPanel.setBorder(new TitledBorder("General"));
        generalCodesPanel.setLayout(new BoxLayout(generalCodesPanel, BoxLayout.Y_AXIS));

        pageCodesPanel = new JPanel();
        pageCodesPanel.setBorder(new TitledBorder("Page"));
        pageCodesPanel.setLayout(new BoxLayout(pageCodesPanel, BoxLayout.Y_AXIS));

        JPanel labelsPanel = new JPanel();
        //labelsPanel.setLayout(new BoxLayout(labelsPanel,BoxLayout.X_AXIS));
        labelsPanel.add(pageCodesPanel);
        labelsPanel.add(generalCodesPanel);

        // Layout sequence for the entire JFrame, contains page display and interactive objects
        add(pagePanel,BorderLayout.CENTER);
        add(actionsPanel,BorderLayout.SOUTH);
        add(labelsPanel, BorderLayout.EAST);
    }

    private void displayPage(String[] lines) {
        /**
         * Accepts a page stored as a String array where each cell is a line, stripped of
         * newline characters.  This method adds newlines, merging the cells into a
         * single String and passing it to the display textarea object.  It also
         * clears all of the scan/skip fields and resets the scroll on the textarea object
         * so that box will always start with the top of the page (otherwise it will auto-
         * scroll to the last line).
         */
        StringBuilder pageText = new StringBuilder();
        for(int i=0;i<lines.length;i++){
            pageText.append(lines[i]).append("\n");
        }
        pagetextArea.setText(pageText.toString());
        pagetextArea.setCaretPosition(0);
        scanNumField.setText("");
        skipNumField.setText("");
        position.setText("Page " + (current+1) + " / " + volume.getLength());
        String tag = codeDictionary.containsKey(current) ? codeDictionary.get(current) : "N/A";
        currentPageTag.setText(String.format("Label: %s", tag));
    }

    private double getCapsPercent(String[] lines) {
        /**
         * Calculates the percentage of lines in an String array that begin with a
         * capitalized letter. String array is assumed to have one line per cell.
         */
        int index, linecount = 0, caps = 0;
        for(int i=0;i<lines.length;i++){
            if (lines[i].length() < 2) {
                continue;
            }
            linecount++;
            index = 0;
            if(!Character.isLetterOrDigit(lines[i].charAt(0))) {
                // In the event that the first character is a quotation mark, skip.
                index = 1;
            }
            if(Character.isUpperCase(lines[i].charAt(index))) {
                caps++;
            }
        }

        return (double)caps / linecount;
    }

    private double getMean() {
        /**
         * Returns the mean of all stored first character capital percentages for stored
         * pages. Because pages that are too short are assigned -1.0, the total number of
         * pages counted will not match the size of the dictionary.  Returns 0 to avoid
         * division error if dictionary is empty.
         */
        double sum = 0;
        int total = 0;
        storePagePercent(); // In case the current page is not stored, store it.
        Integer[] keys = getIterableKeys(capsDictionary.keySet());
        for(int i=0;i<keys.length;i++) {
            if(capsDictionary.get(keys[i]) >= 0.0) {
                total++;
                sum+=capsDictionary.get(keys[i]);
            }
        }
        if (total==0){
            return 0.0;
        } else {
            return sum / total;
        }
    }

    double getStDev() {
        /**
         * Returns the standard deviation of all stored first character capital percentages
         * for stored pages.  Because pages that are too short are assigned -1.0, the total
         * number of pages counted will not match the size of dictionary.  Returns 0 to
         * avoid division error if dictionary is empty.
         *
         * TED: See below note in the scan button's for a quick way to implement a more selective
         * mean/standev check that only calculates them for specific portions of a volume.
         */
        double variance = 0.0;
        int total = 0;
        storePagePercent(); // In case the current page is not stored, store it.
        Integer[] keys = getIterableKeys(capsDictionary.keySet());
        for(int i=0;i<keys.length;i++) {
            if(capsDictionary.get(keys[i]) >= 0.0) {
                total++;
                variance+=Math.pow(capsDictionary.get(keys[i]) - getMean(),2);
            }
        }
        if(total==0) {
            return 0.0;
        } else {
            return Math.sqrt(variance/total);
        }
    }

    private void storePageData(String code) {
        /**
         * When a page is stored, it's caps% and code need to be stored.  They are handled
         * by different functions because sometimes its necessary to store percent without
         * storing codes (i.e., mean/standev calculations).
         */
        storePagePercent();
        storePageCode(code);
    }

    private void storePagePercent() {
        /**
         * Updates the capsDictionary (a Map) by setting the value according to page number
         * keys. If a page has less than 3 lines, then store -1. Iterators that use this
         * data will check to see if value is >= 0 and skip all stored keys with values
         * set to -1.
         */
        if(volume.getPage(current).length >= 3) {
            capsDictionary.put(current, getCapsPercent(volume.getPage(current)));
        } else {
            capsDictionary.put(current, -1.0);
        }
    }

    private void storePageCode(String code) {
        /**
         * Updates the codeDictionary (a Map) by setting the value according to page number
         * keys. It also tells the counter and storedModel to update their displays with
         * the currently stored codes.
         */
        codeDictionary.put(current,code);
        updateCodeDisplay();
        storedCount.setText("Stored: " + codeDictionary.size() + "/" + volume.getLength());
    }

    private void updateCodeDisplay() {
        /**
         * This function builds an array from codeDictionary and sets it as data vector for
         * storedModel, which holds for display all pages with assigned codes.
         */
        String[][] codes = new String[codeDictionary.size()][2];
        String[] columns = {"Page","Code"};
        Integer[] keys = getIterableKeys(codeDictionary.keySet());
        for(int i=0;i<keys.length;i++) {
            codes[i][0] = Integer.toString(keys[i]+1);
            codes[i][1] = codeDictionary.get(keys[i]);
        }
        storedModel.setDataVector(codes,columns);
    }

    private void defineListeners() {
        /**
         * Sets all the button commands (ActionListeners).  This function creates them as
         * anonymous subclasses.  It's hacky, and for a bigger program it would probably
         * be better to define them each separately.  The overall function of each button
         * is described in comments preceding the ActionListener definitions.
         */

        store.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /**
                 * Stores the current page (if a valid code is selected) and advances
                 * to the next in the direction specified by the user.  If the end of a
                 * volume is reached in either direction, a pop-up will notify the user
                 * instead of advancing.
                 */
                if(isValidCode()) {
                    storePageData(getCode());
                    advance();
                } else {
                    JOptionPane.showMessageDialog(null,"Invalid selection. Please choose a genre code.","Invalid Selection",JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        scan.addActionListener(new ActionListener() {
            /**
             * Fast Forward Function:
             * This function first checks to see whether it should go forward or backward.
             * If the number in the scan to box is greater than the number of pages, then it
             * the function will jump to the end and move in reverse and stop at the page
             * that the user was at before beginning the scan command.  If the number in
             * the scan to box is less than the current page, it will move in reverse from
             * the current page to the target page.  Otherwise, will move forward until
             * target page is reached.  Either way, the fast forward will be halted if
             * the standard deviation is reached.
             *
             * NOTE: Target page will not be assigned code.  The scan will just stop at
             * that page.
             *
             * TED: As we discussed at the final summer meeting, the way this works right now
             * is that it checks the mean/standev for all pages from the beginning. The easiest
             * way to modify this function to do so only for specific sections of a volume would
             * be to wrap the getIterableKeys call within the getMean and getStanDev functions
             * with a function that culls the iterable keys array to only include those pages
             * you want included in the check (something like start from the current page, working
             * backwards and adding keys to a new array until a different page code is reached).
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                if(isValidCode()) {
                    try {
                        int increment;
                        int toPage = Integer.parseInt(scanNumField.getText());
                        if (toPage > current && toPage < volume.getLength()) {
                            increment = 1;
                        } else if (toPage < current) {
                            increment = -1;
                            reverse.setEnabled(true);
                        } else if (toPage >= volume.getLength()) {
                            increment = -1;
                            reverse.setEnabled(true);
                            toPage = current;
                            current = volume.getLength()-1;
                        } else {
                            JOptionPane.showMessageDialog(null, "Please enter a page number other than the current one.","Page Advance Error",JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        for(int i=current;i!=toPage;i+=increment) {
                            setCursorBusy(true);
                            current = i;
                            displayPage(volume.getPage(current));
                            // If the page has more than 3 lines and exceeds the Standard Deviation threshold, then stop and ask user if they wish to continue fast fowarding.
                            if(volume.getPage(current).length > 3 && Math.abs(getCapsPercent(volume.getPage(current)) - getMean()) > getStDev()*threshold) {
                                setCursorBusy(false);
                                int choice = JOptionPane.showConfirmDialog(null,"Are you sure you wish to assign " + getCode() + " to page " +Integer.toString(current) +"?","Threshold Reached",JOptionPane.YES_NO_OPTION);
                                // If user selects "No", then break from fast-forward loop and return to main program
                                if(choice == JOptionPane.NO_OPTION) {
                                    return;
                                }
                            }
                            // If page is less than 3 lines, does not exceed threshold, or user selected "Yes" then assign code to page and continue to fast-forward.
                            storePageData(getCode());
                        }
                        if (current != 0 && current != volume.getLength()-1) {
                            // If the end of the volume has not been reached, then show user the next page so they can decide on a code.
                            current+=increment;
                            displayPage(volume.getPage(current));
                        }
                        setCursorBusy(false);

                    } catch (NumberFormatException badnum) {
                        JOptionPane.showMessageDialog(null,"Enter a valid page number.","Invalid Selection",JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else {
                    JOptionPane.showMessageDialog(null,"Invalid selection. Please choose a genre code.","Invalid Selection",JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        skip.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /**
                 * This function skips to a page specified by the user without storing a
                 * code on the current page or on any pages between it and the target page.
                 * The only thing it checks is that the target page requested exists
                 * within the volume.
                 */
                try {
                    int toPage = Integer.parseInt(skipNumField.getText())-1;
                    if (toPage < 0 || toPage >= volume.getLength()) {
                        JOptionPane.showMessageDialog(null, "Specified page is not within range.","Invalid Selection",JOptionPane.ERROR_MESSAGE);
                    } else {
                        current = toPage;
                        displayPage(volume.getPage(current));
                    }
                } catch (NumberFormatException badnum) {
                    JOptionPane.showMessageDialog(null,"Enter a valid page number.","Invalid Selection",JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        });

        navFirst.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (current > 0) {
                    current = 0;
                    displayPage(volume.getPage(current));
                }
            }
        });

        navPrev.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (current > 0) {
                    current--;
                    displayPage(volume.getPage(current));
                }
            }
        });

        navNext.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (current < volume.getLength()-1) {
                    current++;
                    displayPage(volume.getPage(current));
                }
            }
        });

        navLast.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (current < volume.getLength()-1) {
                    current = volume.getLength()-1;
                    displayPage(volume.getPage(current));
                }
            }
        });

        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /**
                 * When the user tries to save, this method checks to see if the user has
                 * stored codes for all pages.  If no, it will navigate to the earliest
                 * non-coded page.  Otherwise, it will call the save function and close
                 * the window.
                 */
                int missing = volume.getLength() - codeDictionary.size();
                if(missing > 0) {
                    JOptionPane.showMessageDialog(null, "Page Map is missing " + Integer.toString(missing) + " codes.\nSkipping to first uncoded page.","Incomplete Map.",JOptionPane.WARNING_MESSAGE);
                    for(int i=0;i<volume.getLength();i++) {
                        if(!codeDictionary.containsKey(i)) {
                            current = i;
                            displayPage(volume.getPage(current));
                            return;
                        }
                    }
                } else {
                    doSave();
                    JOptionPane.showMessageDialog(null, "Page map for " + volume.getHTID() + " saved to disk in " + pageMapDir,"Map Complete.",JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                }
            }
        });

        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /**
                 * This button just warns the user that they are about to dismiss the
                 * page map without saving their work
                 */
                int choice = JOptionPane.showConfirmDialog(PageMapper.this, "Are you sure you want to abandon this page map?","Confirm Cancel",JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    dispose();
                }
            }
        });

        //TODO: A window close listener?
    }

    private Integer[] getIterableKeys(Set<Integer> keys) {
        /**
         * Used to take a Set of keys from a Map (which is similar to a dictionary in
         * Python) and turn them into an iterable array.  To iterate a Map using its keys,
         * setup a loop as follows:
         *
         * int keys[] = getIterableKeys(someTree.keySet());
         * for(int i=0,i<keys.length;i++) {
         * 		value = someTree.get(keys[i]);
         * 		...
         */
        return keys.toArray(new Integer[keys.size()]);
    }

    private void setCursorBusy (boolean busy) {
        /**
         * Sets the cursor icon to busy (if true is passed in) and back to the regular
         * point (if false is passed in).  This function is used to signal to the user
         * that the scan algorithm is still in progress.
         */
        if(busy) {
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
        } else {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private boolean isValidCode() {
        /**
         * Checks to make sure that the user has not selected a category label. Returns
         * true if the user has selected a genre code, false if user's selection is a
         * category label.
         */
        if(codeBox.getSelectedIndex() == 0) {
            // The first item will always be a category label
            return false;
        } else if (codeBox.getSelectedIndex() == generalCodes.length + 1 || codeBox.getSelectedIndex() == generalCodes.length + 2) {
            // Because of the first label, the list is effectively 1-indexed.  So spacer and next label will be at length + 1, 2 respectively.
            return false;
        } else {
            // If none of the others hit, then it should be a valid code
            return true;
        }
    }

    private String getCode() {
        /**
         * Calculates the index of the selected genre code label and returns the label as
         * a String.
         */
        int selected = codeBox.getSelectedIndex();
        if (selected <= generalCodes.length) {
            return generalCodes[selected-1][0];
        } else {
            return pageCodes[selected-(generalCodes.length+3)][0];
        }
    }

    private void buildCodeBox() {
        /**
         * A simple function to concatenate the two String matrices of codes into a single
         * array for display in a combobox (codeBox).
         */

        ActionListener setPageLabelListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CodeButton codeBtn = (CodeButton)e.getSource();
                storePageData(codeBtn.getCode());
                advance();
            }
        };

        codeBox.addItem("GENERAL CODES");
        for (int i=0;i<generalCodes.length;i++) {
            String genCode = generalCodes[i][0];
            String genCodeDescription = generalCodes[i][1];
            codeBox.addItem(genCode + " - " + genCodeDescription);

            JButton genCodeBtn = new CodeButton(genCode, genCodeDescription);
            genCodeBtn.addActionListener(setPageLabelListener);
            generalCodesPanel.add(genCodeBtn);
        }

        codeBox.addItem(" ");

        codeBox.addItem("PAGE ONLY CODES");
        for (int i=0;i<pageCodes.length;i++) {
            String pageCode = pageCodes[i][0];
            String pageCodeDescription = pageCodes[i][1];
            codeBox.addItem(pageCode + " - " + pageCodeDescription);

            JButton pageCodeBtn = new CodeButton(pageCode, pageCodeDescription);
            pageCodeBtn.addActionListener(setPageLabelListener);
            pageCodesPanel.add(pageCodeBtn);
        }
    }

    private void doSave() {
        /**
         * This function saves the pagemap to disk.  Rather than use the format of
         * @tunderwood's script and repeat htid on each line, this method sets the first
         * line to the map's htid and then stores page numbers and codes as a tsv on the
         * following lines.  This should save disk space, but it means that single pagemaps
         * will may need a bit of processing before they can be merged.
         */
        File outDir = new File(pageMapDir);
        if (!outDir.isDirectory()) {
            outDir.mkdir();
        }
        File mapOutFile = new File(pageMapDir + volume.getFileID() + ".tsv");
        try {
            BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mapOutFile),"UTF-8"));
            output.write(volume.getHTID());
            Integer keys[] = getIterableKeys(codeDictionary.keySet());
            for(int i=0;i<keys.length;i++){
                output.append("\n" + keys[i].toString() + "\t" + codeDictionary.get(keys[i]));
            }
            output.close();
            complete = true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Page Map could not be saved in " + pageMapDir + ". Check directory priviledges and try again.","Save Error",JOptionPane.ERROR_MESSAGE);
        }

    }

    private void advance() {
        if (!reverse.isSelected()) {
            if (current < volume.getLength()-1) {
                current++;
                displayPage(volume.getPage(current));
            } else {
                JOptionPane.showMessageDialog(null,"End of volume reached. Cannot advance further in current direction.","Traversal Error",JOptionPane.WARNING_MESSAGE);
            }
        } else {
            if (current > 0) {
                current--;
                displayPage(volume.getPage(current));
            } else {
                JOptionPane.showMessageDialog(null,"Start of volume reached. Cannot advance further in current direction.","Traversal Error",JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private final class CodeButton extends JButton {
        private final String code;
        private final String codeDescription;

        public CodeButton(String code, String codeDescription) {
            super(code);
            setToolTipText(codeDescription);

            this.code = code;
            this.codeDescription = codeDescription;
        }

        public String getCode() {
            return code;
        }

        public String getCodeDescription() {
            return codeDescription;
        }
    }
}
