package edu.illinois.i3.genre.pagetagger.backend;

import java.util.ArrayList;

public class ARFF {
    /**
     * author: Mike Black @mlblack884
     *
     * This class represents ARFF files in memory.  ARFFs must be formatted according to
     * the specifications outlined by @tunderwood in planning documents. Data is stored
     * in flexible lists rather than fixed length arrays so predictions can be passed in
     * one at a time.  The ARFF can accessed as Strings via public methods.  Reading/writing
     * to disk should be handled outside of this class using these methods.
     *
     */

    private String [] header;
    private String relation;
    private ArrayList<String> attributes;
    private ArrayList<String []> data;
    public final static String[] DEFAULT_HEADER = {"% 1. Description","% (Description)","%","% 2. Sources:", "% (List)"};
    public final static String DEFAULT_RELATION = "blank";
    public final static String[] DEFAULT_ATTRIBUTES = {"htid string","startpg numeric","endpg numeric","startpgpart numeric","endpgpart numeric","probability numeric"};
    private final static int HTID_COL = 0;
    private final static int STARTPG_COL = 1;
    private final static int ENDPG_COL = 2;
    private final static int STARTPGPART_COL = 3;
    private final static int ENDPGPART_COL = 4;
    private final static int PROBABILITY_COL = 5;

    public ARFF (String[] file) {
        /**
         * This constructor accepts an ARFF file processed as an array where each cell is
         * one line of text from the file.
         */
        read(file);
    }

    public ARFF() {
        /**
         * This constructor creates an empty ARFF and populates its metadata fields using
         * default filler.
         */
        header = DEFAULT_HEADER;
        relation = DEFAULT_RELATION;
        attributes = new ArrayList<String>();
        for (int i=0;i<DEFAULT_ATTRIBUTES.length;i++){
            attributes.add(DEFAULT_ATTRIBUTES[i]);
        }
    }

    public void read(String[] file) {
        /**
         * This method accepts an ARFF as an array of strings (each is a line from a file).
         * File operations should he handled by data loader in GUI.
         *
         * When defining relation and attribute labels, this splits the line at the first
         * space and puts all remaining characters as the label.
         */

        String[] parts;
        ArrayList<String> temphead = new ArrayList<String>();
        attributes = new ArrayList<String>();
        data = new ArrayList<String[]>();

        for (int i=0;i<file.length;i++){
            if (file[i].startsWith("%")) {
                temphead.add(file[i].trim());
            }
            else if (file[i].toLowerCase().startsWith("@relation")) {
                relation = file[i].trim().split("\\s")[1];
            }
            else if (file[i].toLowerCase().startsWith("@attribute")) {
                attributes.add(file[i].trim().split("\\s")[1]);
            }
            else if (file[i].trim().length() > 0) {
                // If there are characters left after removing whitespace and no matching declarations, then it must be a line of data!
                parts = file[i].trim().split(",");
                data.add(parts.clone());
            }
        }

        header = temphead.toArray(new String[temphead.size()]);

    }

    public void setHeader (String[] input) {
        /**
         * Sets the current metadata header as a String array where each cell represents a
         * line of text in the file.
         */
        header = input.clone();
    }

    public String[] getHeader() {
        /**
         * Returns the current metadata header as a String array where each cell represents
         * a line of text in the file.
         */
        return header.clone();
    }

    public void setRelation (String input) {
        /**
         * Sets the current metadata relation as a String.
         */
        relation = input;
    }

    public String getRelation() {
        /**
         * Returns the current metadata relation as a String.
         */
        return relation;
    }

    public void add(String htid,String startpg,String endpg,String startpart,String endpart,String probability) {
        /**
         * Use this method to add a record to the ARFF. It uses constants to place input in the
         * correct order.  If changes are made to the ARFF structure, try update the constants
         * instead of changing this (unless fields are added/removed).
         */
        String[] row = new String[6];
        row[HTID_COL] = htid;
        row[STARTPG_COL] = startpg;
        row[ENDPG_COL] = endpg;
        row[STARTPGPART_COL] = startpart;
        row[ENDPGPART_COL] = endpart;
        row[PROBABILITY_COL] = probability;
        data.add(row);
    }

    public void clearItems() {
        /**
         * Removes all items from the internal data model. This prepares the prediction model
         * to receive updated data from exterior sources by removing all records from the ARFF.
         * It's faster to clear and add them all than it is to check to see which have been
         * added/removed.
         */
        data = new ArrayList<String[]>();
    }

    public String[] getString() {
        /**
         * Returns an array of Strings where each cell is a line of text in the processed ARFF
         * file.  Does not include new line characters and should be added by the GUI's file
         * handlers.
         */

        String[] output = new String[header.length + 1 + attributes.size() + data.size()];
        int bigi = 0; // The "big index" that keeps track of current line across the loops for header, attributes, and records.
        String dataline;

        // Header loop
        for (int i=0;i<header.length;i++){
            output[i] = header[i];
            bigi = i;
        }

        bigi++;
        output[bigi] = "@RELATION " + relation;

        // Attributes loop
        for(int i=0;i<attributes.size();i++) {
            bigi++;
            output[bigi] = "@ATTRIBUTE " + attributes.get(i);
        }

        // Records loop
        for(int i=0;i<data.size();i++) {
            bigi++;
            dataline = new String();
            for(int subs=0;subs<data.get(i).length;subs++){
                if (subs > 0){
                    dataline += ',';
                }
                dataline += data.get(i)[subs];
            }
            output[bigi] = dataline;
        }

        return output;
    }

    public String[] getItems () {
        /**
         * Returns a list of record identifiers (HTid). Right now it's only use is with DeryDB
         * for creating subtables.
         */

        String[] items = new String[data.size()];
        for (int i=0;i<data.size();i++){
            items[i] = data.get(i)[HTID_COL];
        }
        return items;
    }

}
