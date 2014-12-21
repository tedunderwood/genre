package pages;

import java.util.HashMap;


/**
 * @author tunderwood
 *
 * 
 */

public class Vocabulary implements java.io.Serializable {
	private static final long serialVersionUID = 111L; 
	public String inputFile;
	public int vocabularySize;
	public String[] vocabularyArray;
	HashMap<String, Integer> vocabularyMap;
	double[] meanFreqOfWords;
	
	public Vocabulary(String dataSource) {
		inputFile = dataSource;
	}
	
	public Vocabulary(String dataSource, int suggestedSize, boolean addCatchAll) {
		inputFile = dataSource;
		vocabularySize = suggestedSize;
		LineReader vocabReader = new LineReader(dataSource);
		String[] filelines = vocabReader.readlines();
		
		if (filelines.length < vocabularySize) vocabularySize = filelines.length;
		
		vocabularyMap = new HashMap<String, Integer>();
		
		// Normally we add a catch-all entry for words not otherwise in the
		// vocabulary, so they can collectively be treated as a distinct feature.
		// This increases the size of the vocabulary array, without
		// changing the number of lines we read from the file.

		if (addCatchAll) {
			vocabularyArray = new String[vocabularySize + 1];
		}
		else {
			vocabularyArray = new String[vocabularySize];
		}
		
		for (int i = 0; i < vocabularySize; ++i) {
			String line = filelines[i];
			line = line.replaceAll("\n", "");
			if (line.length() < 1) System.out.println("Blank entry in vocabulary.");
			if (vocabularyMap.containsKey(line)) System.out.println("Duplicate entry in vocab.");
			vocabularyMap.put(line, i);
			vocabularyArray[i] = line;
		}
		
		if (addCatchAll) {
			vocabularyArray[vocabularySize]= "wordNotInVocab";
			vocabularyMap.put("wordNotInVocab", vocabularySize);
			vocabularySize += 1;
		}
	}
	
	
	public boolean includes(String aWord) {
		if (vocabularyMap.containsKey(aWord)) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public HashMap<String, Integer> getMap() {
		return vocabularyMap;
	}
	

}
