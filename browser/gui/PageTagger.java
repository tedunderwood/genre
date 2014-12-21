package edu.illinois.i3.genre.pagetagger.gui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import edu.illinois.i3.genre.pagetagger.backend.Preferences;

@SuppressWarnings("serial")
public class PageTagger extends JFrame {

    /**
     * author: Mike Black @mblack884
     *
     * Functionally speaking, this class is the GenreBrowser.  It loads all of the core
     * components in sequence, passing in the backend objects (DerbyDB and Preferences)
     * to primary GUI components (PredictionManager, SearchBox, SearchResults).
     *
     */

    // Check that we are on Mac OS X.  This is crucial to loading and using the OSXAdapter class.
    public static boolean MAC_OS_X = (System.getProperty("os.name").toLowerCase().startsWith("mac os x"));

    // Ask AWT which menu modifier we should be using.
    final static int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    Preferences prefs;
    PredictionManager predict;
    JPanel top,bottom;

    protected JDialog aboutBox, prefsDialog;

    protected JMenu fileMenu, helpMenu;
    protected JMenuItem openMI, optionsMI, quitMI;
    protected JMenuItem supportMI, aboutMI;

    public static void main(String[] args) {
        final Preferences p = new Preferences(Preferences.DEFAULT_PREF_FILE);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new PageTagger(p).setVisible(true);
            }
        });
    }

    PageTagger (Preferences p) {
        /**
         * This is essentially the main program.  The main above starts two more core
         * backend components.  The core GUI objects are intialized here and then the
         * main window is displayed.  Layout-wise, the main window is subdivided into
         * two equal panels using the Grid Layout, and the bottom is subdivided using
         * Border with SearchBox taking the smaller West space and SearchResults the
         * larger Center space.
         */

        super("Page Tagger");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        prefs = p;

        //getContentPane().setLayout(new GridLayout(0,1));
        getContentPane().setLayout(new BorderLayout());
        setSize(900,600);
        predict = new PredictionManager(prefs);
        getContentPane().add(predict);

        // set up a simple about box
        aboutBox = new JDialog(this, "About");
        aboutBox.getContentPane().setLayout(new BorderLayout());
        aboutBox.getContentPane().add(new JLabel(getTitle(), JLabel.CENTER));
        aboutBox.getContentPane().add(new JLabel("\u00A92013 University of Illinois at Urbana-Champaign", JLabel.CENTER), BorderLayout.SOUTH);
        aboutBox.setSize(400, 120);
        aboutBox.setResizable(false);

        // Preferences dialog lets you select the background color when displaying an image
        prefsDialog = new JDialog(this, getTitle() + " Preferences");
        JPanel masterPanel = new JPanel();
        masterPanel.setBorder(new TitledBorder("Window background color:"));
        prefsDialog.getContentPane().add(masterPanel);
        prefsDialog.setSize(240, 100);
        prefsDialog.setResizable(false);

        addMenus();

        defineListeners();

        // Set up our application to respond to the Mac OS X application menu
        registerForMacOSXEvents();
    }

    // Generic registration with the Mac OS X application menu
    // Checks the platform, then attempts to register with the Apple EAWT
    // See OSXAdapter.java to see how this is done without directly referencing any Apple APIs
    public void registerForMacOSXEvents() {
        if (MAC_OS_X) {
            try {
                // Generate and register the OSXAdapter, passing it a hash of all the methods we wish to
                // use as delegates for various com.apple.eawt.ApplicationListener methods
                OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("doQuit", (Class[])null));
                OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("doAbout", (Class[])null));
                OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("doPreferences", (Class[])null));
                //OSXAdapter.setFileHandler(this, getClass().getDeclaredMethod("loadImageFile", new Class[] { String.class }));
            } catch (Exception e) {
                System.err.println("Error while loading the OSXAdapter:");
                e.printStackTrace();
            }
        }
    }

    private void defineListeners() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                /**
                 * This Window Listener should behave like those in most other data editing programs.
                 * If data has been modified without save, it will prompt users to save.  If users select
                 * yes, then data will be saved and program closed.  If they select no, then any changes
                 * since the last save will be dismissed and program will close.  If they select cancel,
                 * then the program will remain open.  As far as changes go, this Listener only checks
                 * for changes to the target prediction.
                 */
                doQuit();
            }
        });
    }

    public void addMenus() {
        JMenu fileMenu = new JMenu("File");
        JMenuBar mainMenuBar = new JMenuBar();
        mainMenuBar.add(fileMenu = new JMenu("File"));
        fileMenu.add(openMI = new JMenuItem("Open..."));
        openMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, MENU_MASK));
        openMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                predict.loadArff();
            }
        });

        // Quit/prefs menu items are provided on Mac OS X; only add your own on other platforms
        if (!MAC_OS_X) {
            fileMenu.addSeparator();
            fileMenu.add(optionsMI = new JMenuItem("Options"));
            optionsMI.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    doPreferences();
                }
            });

            fileMenu.addSeparator();
            fileMenu.add(quitMI = new JMenuItem("Quit"));
            quitMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MASK));
            quitMI.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    doQuit();
                }
            });
        }

        mainMenuBar.add(helpMenu = new JMenu("Help"));
        helpMenu.add(supportMI = new JMenuItem("Technical Support"));
        supportMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/tedunderwood/pagetagger/issues"));
                }
                catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        // About menu item is provided on Mac OS X; only add your own on other platforms
        if (!MAC_OS_X) {
            helpMenu.addSeparator();
            helpMenu.add(aboutMI = new JMenuItem("About OSXAdapter"));
            aboutMI.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    doAbout();
                }
            });
        }

        setJMenuBar(mainMenuBar);
    }

    // General preferences dialog; fed to the OSXAdapter as the method to call when
    // "Preferences..." is selected from the application menu
    public void doPreferences() {
        prefsDialog.setLocation((int)this.getLocation().getX() + 22, (int)this.getLocation().getY() + 22);
        prefsDialog.setVisible(true);
    }

    // General info dialog; fed to the OSXAdapter as the method to call when
    // "About OSXAdapter" is selected from the application menu
    public void doAbout() {
        aboutBox.setLocation((int)this.getLocation().getX() + 22, (int)this.getLocation().getY() + 22);
        aboutBox.setVisible(true);
    }

    private void doClose() {
        prefs.writePrefs();
        System.exit(0);
    }

    public boolean doQuit() {
        if (predict.getSaveState()) {
            int response = JOptionPane.showConfirmDialog(null,"Target prediction has been modified. Save on exit?","Exit without Saving",JOptionPane.YES_NO_CANCEL_OPTION);
            switch (response) {
                case JOptionPane.YES_OPTION:
                    JOptionPane.showMessageDialog(null, "TODO: Do something with dirty files");
                    break;
                case JOptionPane.NO_OPTION:
                    doClose();
                    break;
                case JOptionPane.CANCEL_OPTION:
                    break;
            }

            return response == JOptionPane.YES_OPTION;
        } else {
            doClose();
            return true;
        }
    }

}
