package edu.illinois.i3.genre.pagetagger.gui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.illinois.i3.genre.pagetagger.backend.ARFF;
import edu.illinois.i3.genre.pagetagger.backend.PredictionTableModel;
import edu.illinois.i3.genre.pagetagger.backend.Preferences;
import edu.illinois.i3.genre.pagetagger.backend.VolumeReader;

@SuppressWarnings("serial")
public class PredictionManager extends JPanel {
    /**
     * author: Mike Black @mblack884
     *
     * This class manages all user interactions with predictions.  While the ARFF class
     * manages the prediction in memory, this class displays interaction options to users
     * and passes information to ARFF when necessary.  This class also handles the saving
     * and loading of ARFFs from disk, but does little more than pass or accept String
     * data between ARFF and files stored on disk.
     *
     * See user documentation for more information about what users will do with predictions.
     *
     */

    PredictionTableModel targetModel;
    private ARFF source;
    private JPanel filesPanel;
    private JScrollPane targetScroll;
    private JTable targetTable;
    private JTextField sourceName;
    private JLabel sourceLabel;
    private Boolean modified,loaded;
    private FileNameExtensionFilter arffOnly;
    private String volumeDataDir;

    // References to external data structures
    private final Preferences prefs;

    public PredictionManager(Preferences p) {
        prefs = p;
        targetModel = new PredictionTableModel();
        drawGUI();
        defineListeners();
        modified = false;
        volumeDataDir = null;
        loaded = false;
    }

    private void drawGUI() {
        /**
         * Initializes all of the graphics objects and positions them within nested
         * panels/layout managers.  Panels used by groups are objects are initialized
         * in the same section as those objects.
         */

        // File filter for use with saving/loading arff's
        arffOnly = new FileNameExtensionFilter("Predictions","arff");

        // Intialize and configure the table that displays the target prediction's records
        targetTable = new JTable(targetModel);
        targetTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        targetTable.setAutoCreateColumnsFromModel(false);
        targetTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        targetTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        targetTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        targetTable.getColumnModel().getColumn(2).setPreferredWidth(300);
        targetTable.getColumnModel().getColumn(3).setPreferredWidth(40);
        targetTable.getColumnModel().getColumn(3).setMinWidth(40);
        targetTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        targetTable.getColumnModel().getColumn(4).setMinWidth(60);
        targetTable.getColumnModel().getColumn(5).setPreferredWidth(60);
        targetTable.getColumnModel().getColumn(5).setMinWidth(60);
        targetTable.getColumnModel().getColumn(6).setPreferredWidth(60);
        targetTable.getColumnModel().getColumn(6).setMinWidth(60);
        targetTable.getColumnModel().getColumn(7).setPreferredWidth(60);
        targetTable.getColumnModel().getColumn(7).setMinWidth(60);
        targetTable.getColumnModel().getColumn(8).setPreferredWidth(60);
        targetTable.getColumnModel().getColumn(8).setMinWidth(60);
        targetScroll = new JScrollPane(targetTable);
        targetScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Initialize and configure the remaining interactive objects
        filesPanel = new JPanel();
        sourceLabel = new JLabel("Source:");
        sourceName = new JTextField("",20);
        sourceName.setEditable(false);

        // Layout sequence...
        setBorder(new EmptyBorder(10, 5, 5, 10));
        setLayout(new BorderLayout());
        filesPanel.setLayout(new BoxLayout(filesPanel,BoxLayout.X_AXIS));
        filesPanel.add(Box.createHorizontalGlue());
        filesPanel.add(sourceLabel);
        filesPanel.add(sourceName);
        filesPanel.add(Box.createHorizontalGlue());
        add(filesPanel,BorderLayout.NORTH);
        add(targetScroll,BorderLayout.CENTER);
    }

    private void defineListeners() {
        /**
         * Sets all the button commands (ActionListeners).  This function creates them as
         * anonymous subclasses.  It's hacky, and for a bigger program it would probably
         * be better to define them each separately.  The overall function of each button
         * is described in comments preceding the ActionListener definitions.
         */

        // *****ACTIONS ON THE TABLE*******
        targetTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Double-clicking a row starts the page mapper
                if (e.getClickCount() == 2)
                    startMapper();
            }
        });

        // ***** DATA MODEL LISTENER *****
        targetModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                /**
                 * This listener should keep the user from closing without saving.  Anytime
                 * the target prediction's data is modified by the user (in the table model),
                 * it will set the modified flag to true and update the interface accordingly.
                 * If new items are added, it will also check to see if a pagemap exists. This
                 * should let users resume mapping sessions if they want to pause between
                 * volumes.
                 */
                if (targetModel.getRowCount() != 0 && !modified) {
                    modified = true;
                    // Check to see if the last row has a pagemap (so new additions will get marked).
                    String latestHTid = targetModel.getValueAt(targetModel.getRowCount()-1,PredictionTableModel.HTID_COL).toString();
                    if(checkForMap(latestHTid)) {
                        targetModel.volumeMapped(targetModel.getRowCount()-1);
                    }
                } else if (targetModel.getRowCount() == 0) {
                    modified = false;
                }
            }
        });

    }

    private String[] loadfile() {
        /**
         * This method reads a file from disk, storing it as a String array where each
         * cell is a line, stripped of newline characters.  Proper use should be to
         * pass the result into an ARFF object, which will handle it in memory.
         *
         * This method is only for the source prediction!  To load a target for modificationm
         * do loadFromSource() after this one!
         */

        ArrayList<String> rawtext = new ArrayList<String>();
        String line;
        JFileChooser fdialog = new JFileChooser(System.getProperty("user.dir"));
        fdialog.setFileFilter(arffOnly);
        int result = fdialog.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File arffin = fdialog.getSelectedFile();
        sourceName.setText(arffin.getAbsolutePath());
        try {
            BufferedReader lines = new BufferedReader(new FileReader(arffin));
            while((line = lines.readLine()) != null){
                // Arraylist used here for Python-like functionality, converted to String array for return
                rawtext.add(line.trim());
            }
            lines.close();
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(null, "File not found. Please try again.","Loading Error",JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Problem reading file. Please try again.","Loading Error",JOptionPane.ERROR_MESSAGE);
        } catch (NullPointerException e) {
            // TODO: Should anything happen if no file is selected?
        }
        return rawtext.toArray(new String[rawtext.size()]);
    }

    private void loadSourceRecords() {
        String[] arff = source.getString();

        for(int i=0;i<arff.length;i++) {
            if(!arff[i].startsWith("%") && !arff[i].startsWith("@")) {
                String[] csv = arff[i].split(",");
                targetModel.addPrediction(csv[0],csv[1],csv[2],csv[3],csv[4],csv[5]);
            }
        }
    }

    public boolean getSaveState() {
        /**
         * This method allows other classes to check whether the target prediction data
         * has been saved since it was modified.  For use with doSave and window listeners
         * in containing classes (its best practice to keep this kind of variable private
         * so that it can't be modified outside the class, so it needs a method for other
         * objects to see its state)
         */
        return modified;
    }

    public boolean checkForMap(String htid) {
        /**
         * This method checks to see if a pagemap exists for a given HTid in the default
         * pagemaps/ directory.  It does so by creating an empty VolumeReader object in
         * order to use its HTid parser utility.
         */
        VolumeReader checker = new VolumeReader();
        checker.learnNameParts(htid);
        if(new File(prefs.getMapDir() + checker.getFileID() + ".tsv").exists()) {
            return true;
        } else {
            return false;
        }
    }

    public void loadArff() {
        /**
         * When a user tries to load a source prediction, this method first checks
         * to see if a source prediction has already been successfully loaded.  If so,
         * the user is asked if they want to drop the subtables created by the prior
         * load sequence.  If not, this load sequence is abandoned.  If so, the tables
         * are dropped, the ARFF file is read from disk (stored in an ARFF object), and
         * a subtable of those records is created within Derby.
         */
        if(loaded) {
            int response = JOptionPane.showConfirmDialog(null,"A source prediction has already been loaded.  Do you want to\nclear it from memory and load a different one?","Source Already Loaded",JOptionPane.YES_NO_OPTION);
            if(response == JOptionPane.YES_OPTION) {
                sourceName.setText(null);
                loaded = false;
            } else {
                return;
            }
        }

        // If the user does not select a file, then abandon load sequence
        String input[] = loadfile();
        if (input == null) {
            return;
        }
        source = new ARFF(input);
        // TODO: Some sort of check to make sure the ARFF processed correctly?

        // Normal load sequence: produce subtable using the prediction's records, enable only those buttons pertaining to loading.
        loadSourceRecords();
        loaded = true;
    }

    private void startMapper() {
        /**
         * This button loads the PageMapper module. When first clicked during a session,
         * it will prompt user for the location of the directory containing the volumes
         * to be mapped.  It is assumed that this may change with each session.  If
         * users are running the PageMapper for the first time in an install of the browser,
         * then they will be prompted for a list of genre codes.  It is assumed that this
         * list will not change frequently (and can be manually updated by editting or
         * replacing the file).
         */
        int selected[] = targetTable.getSelectedRows();
        if (selected.length != 1)
            return;
        else if(!targetModel.isMapping()) {
            JOptionPane.showMessageDialog(null, "Please identify where volume data is stored.  Your choice will be saved for this session only.","Session Initalization",JOptionPane.OK_OPTION);
            JFileChooser vdchoose = new JFileChooser(System.getProperty("user.dir"));
            vdchoose.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            vdchoose.showOpenDialog(null);
            volumeDataDir = vdchoose.getSelectedFile().getAbsolutePath() + "/";
            vdchoose = null;
            if(!prefs.hasGenreCodes()) {
                // After users select a genre codes file, the file must be loaded into memory
                // so that the pagemapper can parse them.
                JOptionPane.showMessageDialog(null, "No genre codes table selected.  Please locate the file containing your genre codes.\nYour choice will be stored for future sessions.","Session Initalization",JOptionPane.OK_OPTION);
                JFileChooser codechoose = new JFileChooser(System.getProperty("user.dir"));
                codechoose.setFileFilter(new FileNameExtensionFilter("Configuration files","ini"));
                codechoose.showOpenDialog(null);
                prefs.setGenreCodes(codechoose.getSelectedFile().getAbsolutePath());
            }
        }
        try {
            // After the codes and data directory are set, pass the volume selected in the
            // target prediction table into the Volume Reader.  Then pass the Volume Reader
            // into the Page Mapper.  If users save their map, mark the listing as mapped
            // in the target prediction table.
            VolumeReader volume = new VolumeReader(targetModel.getValueAt(selected[0],PredictionTableModel.HTID_COL).toString(),volumeDataDir,false);
            PageMapper pagemap = new PageMapper(volume,prefs);
            pagemap.setVisible(true);
            if(pagemap.complete) {
                targetModel.volumeMapped(selected[0]);
            }
        } catch (FileNotFoundException e1) {
            int choice = JOptionPane.showConfirmDialog(null, "Selected Volume does not exist in data directory. Reset data directory?","Volume Not Found",JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                targetModel.setMapping(false);
            }
            return;
        }
    }
}
