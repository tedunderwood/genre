package edu.illinois.i3.genre.pagetagger.backend;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.apache.commons.compress.utils.Charsets;

public class Preferences {
    /**
     * The purposes of this class is to read, write, and hold application preferences.
     * Essentially, anything that needs to be loaded from disk at runtime and passed
     * into any of the core components for configuration purposes is done by this class.
     * Currently, it's primary tasks include:
     * - providing the location of the derby database
     * - keeping track of which seed file was used to create the derby database
     * - remembering which genre codes file users selected (optional)
     *
     * NOTE: Although not explicitly supported within the program, you can bypass the
     * default pagemaps/ directory by editing the configuration file that Preferences
     * loads and adding a pagemapdir= field.  Otherwise, Preferences will stick with
     * the internal default (defined here, NOT IN PAGEMAPPER).
     */

    public final static String DEFAULT_PREF_FILE = "pagetagger.conf";

    private final Properties prefs = new Properties();
    private final File prefsFile;
    private String[][] generalCodes, pageCodes;

    public Preferences(String filename) {
        /**
         * This constructor allows for alternate preference profiles. To use just the
         * default file (as PrimaryWindow does), call it as:
         * Preferences p = new Preferences(Preferences.DEFAULT_PREF_FILE).
         *
         */

        prefsFile = new File(filename);

        // set some defaults
        prefs.put("pagemapdir", "pagemaps/");

        Reader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(prefsFile), Charsets.UTF_8));
            prefs.load(reader);
        }
        catch (FileNotFoundException e) {
            // do nothing
        }
        catch (IOException e) {
            JOptionPane.showConfirmDialog(null, e.getMessage(), "Preferences Loading Error", JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
        }
        finally {
            if (reader != null)
                try { reader.close(); } catch (Exception e) {}
        }

        if (hasGenreCodes()) {
            // If users selected a genre codes file during a past session, retrieve it!
            System.out.println("Trying to load genre codes file.");
            readCodes();
        }
    }

    public void writePrefs() {
        /**
         * Writes configuration settings to disk.  It will write to the same file that it
         * read from initially, or in the case of a first-run to the filename passed into
         * the constructor at creation.
         */
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prefsFile), "UTF-8"));
            prefs.store(writer, "PageTagger Preferences");
            System.out.println("Preferences written to disk.");
        }
        catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Problem writing file. Please ensure you have the necessary privledges.","Write Error",JOptionPane.ERROR_MESSAGE);
        }
        finally {
            if (writer != null)
                try { writer.close(); } catch (Exception e) {}
        }
    }

    // The following block of codes are just private variables that require public
    // request methods so that they are effectively read only outside of this class.
    // This is standard OOP practice.

    public String getSource() {
        return prefs.getProperty("sourcefile");
    }

    public String[][] getGeneralCodes() {
        return generalCodes;
    }

    public String[][] getPageCodes() {
        return pageCodes;
    }

    public String getMapDir() {
        return prefs.getProperty("pagemapdir");
    }

    public void setGenreCodes(String filename) {
        /**
         * Public interface method for genre code files.  The code reader is private and
         * internal.  This sets the internal filename and then loads the internal code reader.
         */
        prefs.put("genrecodes", filename);
        readCodes();
    }

    private void readCodes () {
        /**
         * Reads the genre codes file from disk and parses codes into two categories:
         * general codes that reflect volume and page level data and those that could
         * reflect only page level data.  There's no real reason to keep them separate right
         * now other than to display them as distinct categories to users.
         */
        String line;
        // ArrayLists are used for their Python-like functionally.  They are converted to
        // fixed length arrays at the end of this method.
        ArrayList<String[]> all, page;
        all = new ArrayList<String[]>();
        page = new ArrayList<String[]>();
        boolean both = true;
        String genreCodes = prefs.getProperty("genrecodes");
        try {
            InputStream codesIn = new FileInputStream(new File(genreCodes));
            BufferedReader inLines = new BufferedReader(new InputStreamReader(codesIn,Charset.forName("UTF-8")));
            while ((line = inLines.readLine()) != null) {
                if (line.trim().length() > 0) {
                    if(line.contains("#GENERAL")) {
                        both = true;
                    } else if (line.contains("#PAGES")) {
                        both = false;
                    } else {
                        if (both) {
                            all.add(line.trim().split("\\t",2));
                        } else {
                            page.add(line.trim().split("\\t",2));
                        }
                    }
                }
            }
            inLines.close();
            generalCodes = all.toArray(new String[all.size()][2]);
            pageCodes = page.toArray(new String[page.size()][2]);
        }
        catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not load Genre Codes from " + genreCodes + ".\nPlease ensure file exists.","Load Error.",JOptionPane.ERROR_MESSAGE);
            prefs.remove("genrecodes");
        }
    }

    public boolean hasGenreCodes() {
        /**
         * Simple check to see if a genre code file has been selected (either at load-time
         * from the configuration file or during run-time by the user when starting the
         * PageMapper).
         */
        return prefs.containsKey("genrecodes");
    }

}
