package pages;

import java.util.HashSet;

/**
 * @author tunder
 * @version 1.0
 * @since 2013-12-18
 *
 */
public final class WarningLogger {
	static LineWriter theWriter;
	static boolean writeToFile = false;
	static HashSet<String> notFound = new HashSet<String>();
	
	public static void initializeLogger(boolean toFile, String filename) {
		writeToFile = toFile;
		if (writeToFile) {
			LineWriter clearFile = new LineWriter(filename, false);
			clearFile.print("Warning Log:");
			theWriter = new LineWriter(filename, true);
		}
	}
	
	public static void logWarning(String theWarning) {
		if (writeToFile) {
			theWriter.print(theWarning);
		}
		else {
			System.out.println(theWarning);
		}
	}
	
	public static void addFileNotFound(String file) {
		notFound.add(file);
	}

}
