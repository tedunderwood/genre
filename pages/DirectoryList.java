package pages;
import java.io.File;
import java.util.ArrayList;

public class DirectoryList {
	
	/**
	 * Return paths for all files in directory <code>path</code> that end with suffix.
	 * @param path
	 * @param suffix
	 * @return
	 */
	public static ArrayList<String> getMatchingPaths(String path, String suffix) {
		 
	  String files;
	  File folder = new File(path);
	  File[] listOfFiles = folder.listFiles(); 
	  ArrayList<String> matchingFiles = new ArrayList<String>();
	 
	  for (int i = 0; i < listOfFiles.length; i++) {
	 
	   if (listOfFiles[i].isFile()) 
	   {
	   files = listOfFiles[i].getName();
	       if (files.endsWith(suffix))
	       {
	          matchingFiles.add(path + files);
	          // Note that this methods returns a whole path and not just a filename.
	        }
	     }
	  }
	  return matchingFiles;	 
	}

	 
	 public static ArrayList<String> getCSVs(String path) {
	 
	  String files;
	  File folder = new File(path);
	  File[] listOfFiles = folder.listFiles(); 
	  ArrayList<String> textFiles = new ArrayList<String>();
	 
	  for (int i = 0; i < listOfFiles.length; i++) 
	  {
	 
	   if (listOfFiles[i].isFile()) 
	   {
	   files = listOfFiles[i].getName();
	       if (files.endsWith(".csv") || files.endsWith(".CSV"))
	       {
	          textFiles.add(files);
	        }
	     }
	  }
	  return textFiles;
	  
	}
	 
	/**
	 * This method searches a directory for files that end with ".pg.tsv,"
	 * and returns the portion of the filename preceding that extension,
	 * which will be equal to the 'clean' HathiTrust volume ID.
	 * 
	 * @param path The directory to be searched.
	 * @return An ArrayList of 'clean' HathiTrust volume IDs.
	 */
	public static ArrayList<String> getStrippedPGTSVs(String path) {
		  System.out.println(path);
		  String filename;
		  File folder = new File(path);
		  File[] listOfFiles = folder.listFiles(); 
		  System.out.println(listOfFiles.length);
		  ArrayList<String> idParts = new ArrayList<String>();
		 
		  for (int i = 0; i < listOfFiles.length; i++) {
		 
		   if (listOfFiles[i].isFile()) {
			   filename = listOfFiles[i].getName();
		       if (filename.endsWith(".pg.tsv"))	{
		    	   int namelength = filename.length();
		    	   int sevenback = namelength - 7;
		    	   if (sevenback < 1) continue;
		    	   // We assume that each file in this folder should end with ".pg.tsv"
		    	   String idPart = filename.substring(0, sevenback);
		           idParts.add(idPart);
		        }
		     }
		  }
		  return idParts;
		  
		}
}

