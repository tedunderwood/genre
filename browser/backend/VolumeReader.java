package edu.illinois.i3.genre.pagetagger.backend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

public class VolumeReader {
    /**
     * author: Mike Black @mblack824
     *
     * This class retrieves volumes stored in bzip2 or text files and returns them as
     * String arrays.  Initializing a new VolumeReader will retrieve and load the volume
     * into memory, allowing the reader to act as an interface.
     *
     * NOTE: When initializing a new VolumeReader, you must specify whether the volume is
     * located in a pairtree structure or in a flat directory.  Pass in true to have
     * VolumeReader generate a path using HTRC's pairtree scheme, or pass in false to
     * have it look for a legalized HTID filename in a flat directory.
     */

    private String rootPath, absPath, prefix, postfix, htid;
    private ArrayList<String[]> volumePages;
    private File volumeFile;

    static final String dataDir = "volumes/";

    public VolumeReader () {
        /**
         * This constructor should not be used under normal circumstances.  It's purpose
         * is to access the HTid parsing utilities without having to actually read a volume.
         * To get a legal filename for an HTid (to check to see if a file exists with
         * that name, for example):
         *
         * VolumeReader check = new VolumeReader();
         * check.learnNameParts(someHTID);
         * filename = check.getFileID;
         */
    }

    public VolumeReader (String id, String root, boolean pairtree) throws FileNotFoundException {
        rootPath = root;
        htid = id;
        String filename;
        if(pairtree) {
            parseVolumePath(htid);
            filename = absPath + postfix + "/" + postfix + ".txt";
        } else {
            learnNameParts(htid);
            filename = rootPath + prefix + "." + postfix + ".txt";
        }
        //Checks to see if a compressed version exists before opening the uncompressed file
        volumeFile = new File(filename + ".bz2");
        if(!volumeFile.exists()) {
            System.out.println("BZ2 file not found, trying uncompressed.");
            volumeFile = new File(filename);
            if (!volumeFile.exists()) {
                System.out.println("Uncompressed file not found.");
                throw new FileNotFoundException("Volume does not exist in specified source directory.");
            } else {
                readTXTFile();
            }
        } else {
            readBZ2File();
        }
    }

    private void parseVolumePath(String htid) {
        /**
         * This function takes a given htid and parses it out to a file location.  Rather
         * than return a file or the path, it stores those values for internal use.
         */
        learnNameParts(htid);
        absPath = rootPath + prefix + "/pairtree_root/";
        if (postfix.length() % 2 == 0) {
            for(int i=0;i<postfix.length();i+=2) {
                absPath += postfix.substring(i,i+2) + "/";
            }
        } else {
            for(int i=0;i<postfix.length()-2;i+=2) {
                absPath += postfix.substring(i,i+2) + "/";
            }
            absPath += postfix.substring(postfix.length()-1) + "/";
        }
        System.out.println(absPath);
    }

    public void learnNameParts(String htid) {
        int period = htid.indexOf(".");
        prefix = htid.substring(0,period);
        postfix = htid.substring(period+1);
        if(postfix.indexOf(":") != -1) {
            postfix = postfix.replaceAll(":","+");
            postfix = postfix.replaceAll("/","=");
        }
    }

    private void readBZ2File() {

        String line;
        volumePages = new ArrayList<String[]>();
        ArrayList<String> currentPage = new ArrayList<String>();

        try {
            BZip2CompressorInputStream bzipIn = new BZip2CompressorInputStream(new FileInputStream(volumeFile));
            BufferedReader inLines = new BufferedReader(new InputStreamReader(bzipIn,Charset.forName("UTF-8")));
            while ((line = inLines.readLine()) != null) {
                if(line.startsWith("<div") || line.startsWith("</div")) {
                    continue;
                } else if (line.startsWith("<pb>")) {
                    volumePages.add(currentPage.toArray(new String[currentPage.size()]));
                    currentPage = new ArrayList<String>();
                } else {
                    currentPage.add(line.trim());
                }
            }
            inLines.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void readTXTFile() {
        String line;
        volumePages = new ArrayList<String[]>();
        ArrayList<String> currentPage = new ArrayList<String>();
        System.out.println("Reading volume...");
        try {
            InputStream volumeIn = new FileInputStream(volumeFile);
            BufferedReader inLines = new BufferedReader(new InputStreamReader(volumeIn,Charset.forName("UTF-8")));
            while ((line = inLines.readLine()) != null) {
                if(line.startsWith("<div") || line.startsWith("</div")) {
                    continue;
                } else if (line.startsWith("<pb>")) {
                    volumePages.add(currentPage.toArray(new String[currentPage.size()]));
                    currentPage = new ArrayList<String>();
                } else {
                    currentPage.add(line.trim());
                }
            }
            inLines.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public int getLength() {
        return volumePages.size();
    }

    public String[] getPage(int p) {
        return volumePages.get(p);
    }

    public String getFileID() {
        return prefix + "." + postfix;
    }

    public String getHTID() {
        return htid;
    }

}
