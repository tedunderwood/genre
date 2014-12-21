package edu.illinois.i3.genre.pagetagger.backend;

import javax.swing.table.DefaultTableModel;

@SuppressWarnings("serial")
public class PredictionTableModel extends DefaultTableModel {
    /**
     * author: Mike Black @mlblack884
     *
     * This class handles predictions in memory.  It's primary purpose is to manage their
     * display within PredictionManager, but it also handles their retrieval for use when
     * formatting predictions into the ARFFs for writing to disk.
     *
     * All column names/positions are managed internally by this model.  Use the statics
     * when parsing prediction arrays that are retrieved by get methods to access particular
     * prediction fields.
     *
     */

    public static String[] COL_NAMES = {"HTID","Author","Title","Date","Page Start","Page End","Part Start","Part End","Probability","Mapped"};
    public static int HTID_COL = 0;
//    public static int AUTHOR_COL = 1;
//    public static int TITLE_COL = 2;
//    public static int DATE_COL = 3;
    public static int PAGESTART_COL = 4;
    public static int PAGEEND_COL = 5;
    public static int PARTSTART_COL = 6;
    public static int PARTEND_COL = 7;
    public static int PROBABILITY_COL = 8;
    public static int MAPPED_COL = 9;
    public static String DEFAULT_RANGE = "0";
    public static String DEFAULT_PART = "0";
    private boolean mapping;

    public PredictionTableModel() {
        /**
         * Constructor just establishes the column names.
         */
        setColumnIdentifiers(COL_NAMES);
        mapping = false;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        /**
         * This override forces the table to deny all requests to edit cells.  If you want
         * to make one or more columns editable, add a conditional that returns true for
         * specific columns. I.E.:
         * if (column == PROBABILITY_COL) {
         * 		return true;
         * }
         */
        return false;
    }

    public void addPrediction(String htid, /*String author, String title, String date,*/ String prediction) {
        /**
         * This method accepts data and stores it in the appropriate columns using the static
         * column indices.  This version is for use when adding predictions from metadata searches,
         * which won't have page ranges or parts assigned to them.
         */
        String[] record = new String[9];
        record[HTID_COL] = htid;
//        record[AUTHOR_COL] = author;
//        record[TITLE_COL] = title;
//        record[DATE_COL] = date;
        record[PAGESTART_COL] = DEFAULT_RANGE;
        record[PAGEEND_COL] = DEFAULT_RANGE;
        record[PARTSTART_COL] = DEFAULT_PART;
        record[PARTEND_COL] = DEFAULT_PART;
        record[PROBABILITY_COL] = prediction;
        addRow(record);
    }

    public void addPrediction(String htid, /*String author, String title, String date,*/ String pgstart, String pgend, String ptstart, String ptend, String prediction) {
        /**
         * This method accepts data and stores it in the appropriate columns using the static
         * column indices.  This version is for use when adding predictions from ARFF files
         * which will have page ranges and parts assigned to them.
         */
        String[] record = new String[9];
        record[HTID_COL] = htid;
//        record[AUTHOR_COL] = author;
//        record[TITLE_COL] = title;
//        record[DATE_COL] = date;
        record[PAGESTART_COL] = pgstart;
        record[PAGEEND_COL] = pgend;
        record[PARTSTART_COL] = ptstart;
        record[PARTEND_COL] = ptend;
        record[PROBABILITY_COL] = prediction;
        addRow(record);
    }

    public String[] getRange(int row) {
        /**
         * Returns the page range and parts.  For use with RangeEdit class.
         */
        String[] range = new String[4];
        range[0] = getValueAt(row,PAGESTART_COL).toString();
        range[1] = getValueAt(row,PAGEEND_COL).toString();
        range[2] = getValueAt(row,PARTSTART_COL).toString();
        range[3] = getValueAt(row,PARTEND_COL).toString();
        return range;
    }

    public void setRange(int row, String[] range) {
        /**
         * Stores the page range and parts.  For use with RangeEdit class.
         */
        setValueAt(range[0],row,PAGESTART_COL);
        setValueAt(range[1],row,PAGEEND_COL);
        setValueAt(range[2],row,PARTSTART_COL);
        setValueAt(range[3],row,PARTEND_COL);
    }

    public String[] getRow(int row) {
        /**
         * Returns a complete prediction record as a String array. For use primarily
         * for passing records to ARFFs. Refer to statics to retrieve specific data
         * from arrays.
         */
        String[] record = new String[9];
        record[HTID_COL] = getValueAt(row,HTID_COL).toString();
//        record[AUTHOR_COL] = getValueAt(row,AUTHOR_COL).toString();
//        record[TITLE_COL] = getValueAt(row,TITLE_COL).toString();
//        record[DATE_COL] = getValueAt(row,DATE_COL).toString();
        record[PAGESTART_COL] = getValueAt(row,PAGESTART_COL).toString();
        record[PAGEEND_COL] = getValueAt(row,PAGEEND_COL).toString();
        record[PARTSTART_COL] = getValueAt(row,PARTSTART_COL).toString();
        record[PARTEND_COL] = getValueAt(row,PARTEND_COL).toString();
        record[PROBABILITY_COL] = getValueAt(row,PROBABILITY_COL).toString();
        return record;
    }

    public void setMapping(boolean state) {
        mapping = state;
    }

    public boolean isMapping() {
        return mapping;
    }

    public void volumeMapped(int row) {
        setValueAt("X", row, MAPPED_COL);
    }

}
