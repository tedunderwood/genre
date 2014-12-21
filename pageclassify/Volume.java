package pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;

/**
 * @author tunderwood
 * 
 * This class acts as a reducer, collecting the features associated with a single docID
 * (aka volumeID).
 * 
 * addFeature is a method that gets called for every feature the Volume receives.
 * A feature is defined as an array of four strings: pageNum, formField, word, and count.
 * 
 * Then, after all features are received, there are two different methods that could be used 
 * to transform Volumes into DataPoints in vector space.
 * 
 * makeVolumePoint turns the whole volume into a single DataPoint.
 * 
 * makePagePoint turns the volume into a collection of DataPoints representing pages; these
 * DataPoints get three extra features that reflect structural information about a page's
 * length, shape, and position in the volume.
 * 
 * @param pagesPerVol	HashMap storing the number of pages (value) for a volume ID (key).
 * @param meanLinesPerPage	HashMap storing the mean number of lines per page 
 * 							in a volume ID (key).
 */

public class Volume {
	String volumeID;
	ArrayList<Integer> listOfLineCounts;
	ArrayList<Integer> listOfPages;
	int numberOfPages;
	int maxPageNum;
	int totalWords;
	ArrayList<String[]> sparseTable;
	HashMap<String, Double> metadataFeatures;
	
	public Volume(String volumeID) {
		this.volumeID = volumeID;
		listOfLineCounts = new ArrayList<Integer>();
		listOfPages = new ArrayList<Integer>();
		numberOfPages = 0;
		maxPageNum = 0;
		totalWords = 0;
		sparseTable = new ArrayList<String[]>();
		metadataFeatures = new HashMap<String, Double>();
	}
	/** 
	 * This method accepts a line from the database, already parsed into a
	 * sequence of fields, with docid removed (it is volumeID of this volume).
	 *  
	 * @param feature:	0 - pageNum, 1 - feature, 2 - count
	 * 
	 */
	public void addFeature(String[] feature) {
		
		sparseTable.add(feature);
		
		// The number of pages in the volume is defined as the number of distinct
		// page numbers it receives. Note that this is not necessarily == to the
		// maximum pageNum value. It's possible for some pages to be blank, in
		// which case we might conceivable have no information about them here even though they've
		// increased the max pageNum value.
		
		// In reality, given my current page-level tokenizing script (NormalizeOCR 1.0),
		// it's pretty much *not* possible to have a page without features, because e.g.
		// #textlines gets reported even if zero.
		
		int pageNum = Integer.parseInt(feature[0]);
		String featurename = feature[1];
		int count = Integer.parseInt(feature[2]);
		
		if (pageNum < 0) {
			// This is a special volume-level feature that will be attached to all pages
			metadataFeatures.put(featurename, (double) count);
			return;
		}
		
		if (!listOfPages.contains(pageNum)) {
			listOfPages.add(pageNum);
			numberOfPages = listOfPages.size();
		}
		
		if (!featurename.startsWith("#")) {
			totalWords += count; 
		}
		
		if (pageNum > maxPageNum) maxPageNum = pageNum;
		
		// If the "word" is actually a structural feature recording the number
		// of lines in a page, this needs to be added to the listOfLineCounts
		// for pages in the volume.
		
		if (featurename.equals("#textlines")) {
			listOfLineCounts.add(count);
		}
		
		// We don't assume that this list will have the same length as the
		// listOfPages, or that there is any mapping between the two. We use
		// it purely to produce a meanLinesPerPage value later.
			
	}
	
	public DataPoint makeVolumePoint(HashMap<String, Integer> vocabularyMap) {
		
		// Create a vector of the requisite dimensionality; initialize to zero.
		int dimensionality = vocabularyMap.size();
		double[] vector = new double[dimensionality];
		Arrays.fill(vector, 0);
		
		double sumAllWords = 0d;
		// Then sum all occurrences of words to the appropriate vector index.
		for (String[] feature : sparseTable) {
			String word = feature[2];
			if (vocabularyMap.containsKey(word)) {
				int idx = vocabularyMap.get(word);
				double count = Double.parseDouble(feature[3]);
				vector[idx] += count;
				sumAllWords += count;
				// perhaps make sure structural features not included in sum
			}
		}
		
		DataPoint point = new DataPoint(volumeID, vector, sumAllWords);
		return point;
	}
	
	public ArrayList<DataPoint> makePagePoints(HashMap<String, Integer> vocabularyMap) {
		// Page points are much more complex.
		// To start with, divide the sparseTable into page groups.
		
		ArrayList<ArrayList<String[]>> featuresByPage = new ArrayList<ArrayList<String[]>>();
		for (int i = 0; i < numberOfPages; ++ i) {
			ArrayList<String[]> blankPage = new ArrayList<String[]>();
			featuresByPage.add(blankPage);
		}
		
		for (String[] feature : sparseTable) {
			int pageNum = Integer.parseInt(feature[0]);
			if (listOfPages.contains(pageNum)) {
				int idx = listOfPages.indexOf(pageNum);
				ArrayList<String[]> thisPage = featuresByPage.get(idx);
				thisPage.add(feature);
			}
		}
		
		// We create the following eleven "structural" features that are designed to
		// characterize types of paratext by capturing typographical characteristics
		// of pages.
		//
		// "posInVol" = pagenum / totalpages
		// "lineLengthRatio" = textlines / mean lines per page
		// "capRatio" = caplines / textlines
		// "wordRatio" = words on page / mean words per page
		// "distanceFromMid" = abs( 0.5 - posInVol)
		// "allCapRatio" = words in all caps / words on this page
		// "maxInitalRatio" = largest number of repeated initials / textlines
		// "maxPairRatio" = largest number of repeats for alphabetically adjacent initials / textlines
		// "wordsPerLine" = total words on page / total lines on page
		// "totalWords" = total words on page
		// "typeToken" = number of types / total words on page.
		//
		// NOTE that this must match the list of structural features in Global.
		
		int totalTextLines = 0;
		for (int lineCount: listOfLineCounts) {
			totalTextLines += lineCount;
		}
		
		double meanLinesPerPage;
		if (numberOfPages > 0 & totalTextLines > 0) {
			meanLinesPerPage = totalTextLines / (double) numberOfPages;		
		}
		else {
			// avoid division by zero, here and later
			meanLinesPerPage = 1;
			System.out.println("Suspicious mean lines per page in volume " + volumeID);
		}
		
		double meanWordsPerPage;
		if (numberOfPages > 0 & totalWords > 0) {
			meanWordsPerPage = totalWords / (double) numberOfPages;
		}
		else {
			meanWordsPerPage = 1;
			System.out.println("Suspicious mean words per page in volume " + volumeID);
		}
		
		// We're going to create a DataPoint for each page.
		ArrayList<DataPoint> points = new ArrayList<DataPoint>(numberOfPages);
		
		for (int i = 0; i < numberOfPages; ++i) {
			
			ArrayList<String[]> thisPage = featuresByPage.get(i);
			if (thisPage.size() < 1) {
				// there are no features in this page
				System.out.println("Error: featureless page " + Integer.toString(i));
				continue;
			}
			int thisPageNum = listOfPages.get(i);
			
			// Create a vector of the requisite dimensionality; initialize to zero.
			// Note that the dimensionality for page points is 
			// vocabularySize + FEATURESADDED  !! Because structural features.
			
			int vocabularySize = vocabularyMap.size();
			int dimensionality = vocabularySize + Global.FEATURESADDED;
			double[] vector = new double[dimensionality];
			double sumAllWords = 0.0001d;
			// This is a super-cheesy way to avoid div by zero.
			double types = 0;
			Arrays.fill(vector, 0);
			
			// Then sum all occurrences of words to the appropriate vector index.
			double textlines = 0.0001d;
			double lines = 0.0001d;
			double caplines = 0.0001d;
			double maxinitial = 0;
			double maxpair = 0;
			double allcapwords = 0;
			double commas = 0;
			double exclamationpoints = 0;
			double questionmarks = 0;
			double endwpunct = 0;
			double endwnumeral = 0;
			double startwrubric = 0;
			double startwname = 0;
			double sequentialcaps = 0;
			
			for (String[] feature : thisPage) {
				String word = feature[1];
				if (vocabularyMap.containsKey(word)) {
					int idx = vocabularyMap.get(word);
					double count = Double.parseDouble(feature[2]);
					vector[idx] += count;
					sumAllWords += count;

					if (word.equals("wordNotInVocab")) {
						types += count;
					}
					else {
						types += 1;
					}
					// Note that since "wordNotInVocab" is, paradoxically, in the vocab,
					// this will count separate occurrences of "wordNotInVocab" as new types,
					// and sum their counts.
					
					// Certain types handled collectively should actually increase
					// the number of types by more than one. I could include personal
					// names here, but don't, because my secret agenda is to make
					// feature #23 high in pages of drama.
					if (word.equals("propernoun")) {
						types += (count - 1);
					}
					if (word.equals("placename")) {
						types += (count - 1);
					}
				}
				
				if (word.equals("#textlines")) {
					textlines = Double.parseDouble(feature[2]);
					continue;
					// Really an integer but cast as double to avoid 
					// integer division. Same below.
				}
				
				if (word.equals("#lines")) {
					lines = Double.parseDouble(feature[2]);
					continue;
				}
				
				if (word.equals("#caplines")) {
					caplines = Double.parseDouble(feature[2]);
					continue;
				}
				if (word.equals("#maxinitial")) {
					maxinitial = Double.parseDouble(feature[2]);
					continue;
				}
				if (word.equals("#maxpair")) {
					maxpair = Double.parseDouble(feature[2]);
					continue;
				}
				if (word.equals("#allcapswords")) {
					allcapwords = Double.parseDouble(feature[2]);
					continue;
				}
				if (word.equals("#commas")) {
					commas = Double.parseDouble(feature[2]);
					continue;
				}
				if (word.equals("#exclamationpoints")) {
					exclamationpoints = Double.parseDouble(feature[2]);
					continue;
				}
				if (word.equals("#questionmarks")) {
					questionmarks = Double.parseDouble(feature[2]);
					continue;
				}
				if (word.equals("#endwpunct")) {
					endwpunct = Double.parseDouble(feature[2]);
					continue;
				}
				if (word.equals("#endwnumeral")) {
					endwnumeral = Double.parseDouble(feature[2]);
					continue;
				}
				if (word.equals("#startwrubric")) {
					startwrubric = Double.parseDouble(feature[2]);
					continue;
				}
				if (word.equals("#startwname")) {
					startwname = Double.parseDouble(feature[2]);
					continue;
				}
				if (word.equals("#sequentialcaps")) {
					sequentialcaps = Double.parseDouble(feature[2]);
					continue;
				}
			}
			
			if (textlines < 0.1) textlines = 0.001d;
			// hack to avoid div by zero in cases where #textlines == 0.
			
			// Normalize the feature counts for total words on page:
			
			for (int j = 0; j < vocabularySize; ++ j) {
				vector[j] = vector[j] / sumAllWords;
			}
			
			// Now we have a feature vector with all the words filled in, but
			// the eleven extra spaces at the end are still zero.
			// We need to create structural "page features."
			
			double positionInVol = (double) thisPageNum / maxPageNum;
			// TODO: Error handling to avoid division by zero here.
					
			double lengthRatio = textlines / meanLinesPerPage;
			double capRatio = caplines / textlines;
			
			vector[vocabularySize] = positionInVol;
			// normalized by maxPageNum
			vector[vocabularySize + 1] = lengthRatio;
			// length in lines relative to mean for volume
			vector[vocabularySize + 2] = capRatio;
			// proportion of lines that are initial-capitalized
			vector[vocabularySize + 3] = sumAllWords / meanWordsPerPage;
			// wordRatio: length in words relative to mean for volume
			vector[vocabularySize + 4] = Math.abs(thisPageNum - (maxPageNum/2)) / (double) maxPageNum;
			// distanceFrom Mid: absolute distance from midpoint of volume, normalized for length of volume
			vector[vocabularySize + 5] = allcapwords / sumAllWords;
			// "allCapRatio" = words in all caps / words on this page
			vector[vocabularySize + 6] = (maxinitial + 0.2d) / (textlines + 0.5d);
			// "maxInitalRatio" = largest number of repeated initials / textlines
			vector[vocabularySize + 7] = (maxpair + 0.1d) / (textlines + 0.5d);		
			// "maxPairRatio" = largest number of repeats for alphabetically adjacent initials / textlines
			vector[vocabularySize + 8] = sumAllWords / textlines;
			// "wordsPerLine" = total words on page / total lines on page
			vector[vocabularySize + 9] = sumAllWords;
			// "totalWords" = total words on page
			vector[vocabularySize + 10] = (types + 1.0d) / (sumAllWords + 1.5d);
			// type-token ratio
			vector[vocabularySize + 11] = commas / sumAllWords;
			// commas normalized for wordcount
			vector[vocabularySize + 12] = textlines / lines;
			// The number of lines with text divided by the total number of lines.
			// periods normalized for wordcount
			vector[vocabularySize + 13] = vector[vocabularySize + 10] * vector[vocabularySize + 10];
			// squared typetoken.
			vector[vocabularySize + 14] = exclamationpoints / sumAllWords;
			// exclamation points normalized for wordcount
			vector[vocabularySize + 15] = questionmarks / sumAllWords;
			// question marks normalized for wordcount
			vector[vocabularySize + 16] = (endwpunct + 0.1d) / (textlines + 0.3d);
			// Proportion of lines ending with punctuation.
			vector[vocabularySize + 17] = (endwnumeral + 0.01d) / (textlines + 0.2d);
			// Proportion of lines ending with a digit as either of last two chars.
			vector[vocabularySize + 18] = startwname / textlines;
			// Proportion of lines starting with a word that might be a name.
			vector[vocabularySize + 19] = startwrubric / textlines;
			// Proportion of lines starting with a capitalized word that ends w/ a period.
			vector[vocabularySize + 20] = sequentialcaps;
			// Largest number of capitalized initials in alphabetical sequence.
			vector[vocabularySize + 21] = (sequentialcaps + 0.2d) / (caplines + 2.0d);
			// Sequential caps normalized for the number of capitalized lines.
			vector[vocabularySize + 22] = vector[vocabularySize + 10] * Math.log(sumAllWords + 50.0d);
			// the type-token ratio times a logarithmically corrected word length
			// the intuition here is that type-token tends to decrease with page length,
			// so it will be more informative to normalize by multiplying
			vector[vocabularySize + 23] = Math.abs(sumAllWords - meanWordsPerPage) / meanWordsPerPage;
			// absolute deviation, plus or minus, from mean num words, normalized by mean
			
			if (metadataFeatures.containsKey("#metaBiography")) {
				vector[vocabularySize + 24] = 1;
			} else {
				vector[vocabularySize + 24] = 0;
			}

			if (metadataFeatures.containsKey("#metaFiction")) {
				vector[vocabularySize + 25] = 1;
			} else {
				vector[vocabularySize + 25] = 0;
			}
			
			String label = volumeID + "," + Integer.toString(thisPageNum);
			DataPoint thisPoint = new DataPoint(label, vector, sumAllWords);
			points.add(thisPoint);
		}
	return points;	
	}

}
