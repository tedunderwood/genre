package pages;

import java.util.ArrayList;
import java.util.HashMap;

public class Corpus {

	ArrayList<DataPoint> datapoints;
	// Individual pages, represented as vectors of features, plus labels.
	ArrayList<ArrayList<String>> volumeGenres;
	ArrayList<Volume> volumes;
	ArrayList<String> trainingVols;
	int numPoints;
	int numVolumes;
	GenreList genres;
	// Primarily a list of genre labels. It is guaranteed to begin with two dummy genres,
	// "begin" and "end," which apply to the imaginary pages right before the start of the
	// volume and right after its end.
	HashMap<String, Integer> featureMap;
//	int featureCount;
//	static ArrayList<String> features;
//	// Note that this list of features is going to be larger than the size of the vocabulary,
//	// because it will include the STRUCTURALFEATURES contained in Global.
//	ArrayList<Double> stdevOfFeatures;
//	ArrayList<Double> meansOfFeatures;
	Vocabulary vocabulary;
	FeatureNormalizer normalizer;

	public Corpus(ArrayList<String> featurePaths, ArrayList<String> genrePaths, ArrayList<String> trainingVols, Vocabulary vocab) {

		genres = new GenreList();
		vocabulary = vocab;
		featureMap = vocabulary.getMap();
		// In this version of corpus, we assume that we already have a preset vocabulary
		// for classification. Features is a map where the feature names are keys and
		// the values are integers representing sequence order.
		
		this.trainingVols = trainingVols;
		numVolumes = trainingVols.size();
		
		// First read in a genre sequence for each volume.
		volumeGenres = getVolumeGenres(genrePaths, trainingVols);
		
		// Now read in a feature sequence for each volume
		
		volumes = readVolumes(featurePaths, trainingVols, featureMap);

		// The Volume objects read the lines associated with a single
		// HathiTrust volume ID. Then they can produce DataPoints that
		// either represent volumes or individual pages. I've implemented
		// these alternatives as two different methods of the Volume class.

		// In this Corpus constructor, we create datapoints for each page
		// that are then sorted and grouped by genre.

		numPoints = 0;
		datapoints = new ArrayList<DataPoint>();
		
		// Now we have three ArrayLists, all of which should have the same length and
		// contain data structures corresponding to each other in the same sequence.
		
		// volumeLabels is a list of volumeIDs, corresponding to
		// volumeGenres, a list containing an ArrayList of genres for the pages of each volume
		// and volumes, a list containing Volume data objects that can produce page-level
		// data points. Our task now is to match genres to page-level DataPoints, and
		// add the genre-tagged points to a master array "datapoints" containing all
		// the pages as individual objects.
		
		// We match pages purely by sequence. There are no absolute page numbers in the underlying
		// data structure, so there's no reason why we should ever have a "skipped" or "missing" page.
		// The 11th page is by definition between the 10th and the 12th.
		
		assert (trainingVols.size() == volumes.size());
		assert (volumeGenres.size() == volumes.size());
		System.out.println("We have " + volumes.size() + " volumes in the training set.");
		
		// If either of those things are false, we're in big trouble.
		
		for (int i = 0; i < numVolumes; ++ i) {
			Volume thisVol = volumes.get(i);
			ArrayList<String> genresOfThisVol = volumeGenres.get(i);
			
			ArrayList<DataPoint> newPoints = thisVol.makePagePoints(featureMap);
			
			if (genresOfThisVol.size() != newPoints.size()) {
				System.out.println("Genre file for " + thisVol.volumeID + " has " + genresOfThisVol.size() +
						" pages, but the feature file has " + newPoints.size());
			}
			
			for (int j = 0; j < newPoints.size(); ++j) {
				String genre = genresOfThisVol.get(j);
				genre = normalizeGenre(genre);
				if (!genres.genreLabels.contains(genre)) {
					genres.addLabel(genre);
				}
				
				DataPoint aPoint = newPoints.get(j);
				aPoint.setGenre(genre);
				datapoints.add(aPoint);
				numPoints += 1;
			}
		}
		normalizer = new FeatureNormalizer(vocabulary, datapoints);
		
		// The normalizer centers all features on the feature mean, and normalizes them by their
		// standard deviations. Aka, transforms features to z-scores. This is
		// desirable because I'm using regularized logistic regression, which will
		// shrink coefficients toward the origin, and shrinkage pressure is
		// distributed more evenly if the features have been normalized.
		
		normalizer.normalizeFeatures(datapoints);
		
		System.out.println(genres.genreLabels);

	}
	
	private ArrayList<Volume> readVolumes(ArrayList<String> featurePaths, ArrayList<String> volumeLabels, HashMap<String, Integer> featureMap) {
		ArrayList<Volume> volumes = new ArrayList<Volume>();
		
		if (featurePaths.size() != volumeLabels.size()) {
			System.out.println("Error: number of paths != number of volumes in readVolumes method.");
			System.exit(0);
		}
		
		for (int i = 0; i < volumeLabels.size(); ++i) {
			String volID = volumeLabels.get(i);
			String featurePath = featurePaths.get(i);
			String volumePath = featurePath + volID + ".pg.tsv";
			LineReader fileSource = new LineReader(volumePath);
			String[] filelines = fileSource.readlines();
			Volume thisVol = new Volume(volID);
			
			for (String line : filelines) {
				String[] tokens = line.split("\t");
				int tokenCount = tokens.length;
				if (tokenCount != 3) {
					System.out.println("Token count not 3 at " + line);
					continue;
					// TODO: better error handling
				}
				// If the feature is either a) in the vocabulary or
				// b) a structural features, which always begins with a hashtag,
				// we pass it through unaltered.
				if (featureMap.containsKey(tokens[1]) | tokens[1].startsWith("#")) {
					thisVol.addFeature(tokens);
				}
				else {
					tokens[1] = "wordNotInVocab";
					// Words not in the vocabulary still need to be included, for instance,
					// in the total count of words per page. Also the density of rare words
					// is itself revealing. So we count these as a special feature,
					// "wordNotInVocab." Paradoxically, this is itself a word in the
					// vocabulary. :)
					thisVol.addFeature(tokens);
				}
			}
			// end iteration across lines
			volumes.add(thisVol);
		}
		// end iteration across volume labels
		 
		return volumes;
	}
	
	private ArrayList<ArrayList<String>> getVolumeGenres(ArrayList<String> genrePaths, ArrayList<String> volumeLabels) {
		volumeGenres = new ArrayList<ArrayList<String>>();
		
		if (genrePaths.size() != volumeLabels.size()) {
			System.out.println("Error: number of paths != number of volumes in getVolumeGenres method.");
			System.exit(0);
		}
		
		for (int i = 0; i < volumeLabels.size(); ++i) {
			String label = volumeLabels.get(i);
			String genrePath = genrePaths.get(i);
			String genreFilePath = genrePath + label + ".map";
			LineReader fileSource = new LineReader(genreFilePath);
			String[] filelines = fileSource.readlines();
			
			ArrayList<String> aGenreSequence = new ArrayList<String>();
			int pagecounter = 0;
			
			for (String line : filelines) {
				String[] tokens = line.split("\t");
				int tokenCount = tokens.length;
				if (tokenCount != 2) {
					System.out.println("Error: tokenCount not equal to 2 at "
							+ line + " in " + label);
					// not the world's most sophisticated error handling here
					// TODO: define Exception handling for input format issues
					continue;
				}
				int pagenum = Integer.parseInt(tokens[0]);
				if (pagenum != pagecounter) {
					System.out.println("pagination oddity");
				}
				pagecounter += 1;
				String genre = tokens[1];
				genre = normalizeGenre(genre);
				if (!genres.genreLabels.contains(genre)) {
					genres.addLabel(genre);
				}
				aGenreSequence.add(genre);
			// end iterating across lines
			}
			volumeGenres.add(aGenreSequence);
		// end iterating across volumes
		}
	return volumeGenres;
	}
	
	/**
	 * A constructor that we use to create one-volume corpora when a model
	 * has already been trained and we're cycling through the test set one
	 * volume at a time
	 * 
	 * @param featurePath Directory in which the source file is contained.
	 * @param volumeLabels An ArrayList of length one, containing the filename
	 * for the volume to be loaded in this corpus, minus ".pg.tsv", which will
	 * be added back in the readVolumes method.
	 * @param vocabulary The vocabulary used to select features.
	 * @param normalizer Stores feature means and stdevs for normalization.
	 *
	 */
	public Corpus(String featurePath, ArrayList<String> volumeLabels, 
			Vocabulary vocabulary, FeatureNormalizer normalizer) {
		
		this.vocabulary = vocabulary;
		this.normalizer = normalizer;
		featureMap = vocabulary.getMap();
		ArrayList<String> featurePaths = new ArrayList<String>(1);
		featurePaths.add(featurePath);
		// In this builder of corpora, we assume a) that we only have
		// wordcounts and don't yet know the genres and b) a vocabulary
		// for the model has already been established.

		volumes = readVolumes(featurePaths, volumeLabels, featureMap);

		// In this implementation of Corpus, we should actually
		// only have one volume in our data.
		
		if (volumes.size() > 1) {
			System.out.println("Error: multiple volumes in file.");
		}
		
		Volume thisVol = volumes.get(0);

		// We're producing page points.
		
		datapoints = thisVol.makePagePoints(featureMap);
		numPoints = datapoints.size();
		
		normalizer.normalizeFeatures(datapoints);
		// Note 1) that this method actually mutates the datapoints sent as a parameter,
		// and 2) that it depends on the existence of a previously constructed
		// FeatureNormalizer.

	}
	
	/**
     * A constructor that we use to create one-volume corpora when a model
	 * has already been trained and we're cycling through the test set. This version
	 * differs from the one above by extracting the volume from a pairtree structure.
	 * It also expects to receive a *dirty* HathiTrust pairtree id. 
	 * 
	 * @param rootPath Root of the pairtree file structure.
	 * @param dirtyHtid The HathiTrust ID (including any slashes and colons) that
	 * specifies a pairtree path. Note that the ID gets cleaned when used as a label
	 * in the Volume object, so it can be used as a filename later.
	 * @param vocabulary The vocabulary used to select features.
	 * @param normalizer Stores feature means and stdevs for normalization.
	 * @param pairtree A generic pairtree object to invoke. In truth this could be a
	 * static class.
	 */
	public Corpus(String rootPath, String dirtyHtid, 
			Vocabulary vocabulary, FeatureNormalizer normalizer) {
		
		this.vocabulary = vocabulary;
		this.normalizer = normalizer;
		featureMap = vocabulary.getMap();
		
		Volume thisVol = readAVolumeFromPairtree(rootPath, dirtyHtid, featureMap);
		volumes = new ArrayList<Volume>(1);
		volumes.add(thisVol);
		
		if (thisVol.totalWords > 1) {

			// We're producing page points.
			datapoints = thisVol.makePagePoints(featureMap);
			numPoints = datapoints.size();
			
			normalizer.normalizeFeatures(datapoints);
			// Note 1) that this method actually mutates the datapoints sent as a parameter,
			// and 2) that it depends on the existence of a previously constructed
			// FeatureNormalizer.
		}
		else {
			numPoints = 0;
			// This will be caught at a higher level so we don't attempt to classify an empty volume.
		}
	}
	
	private Volume readAVolumeFromPairtree(String rootPath, String dirtyHtid,
			HashMap<String, Integer> featureMap) {
		
		String cleanHtid = PairtreeReader.cleanID(dirtyHtid); 
		Volume thisVol = new Volume(cleanHtid);
		PairtreeReader reader = new PairtreeReader(rootPath);
		
		ArrayList<String> filelines = reader.getVolume(dirtyHtid);
		
		for (String line : filelines) {
			String[] tokens = line.split("\t");
			int tokenCount = tokens.length;
			if (tokenCount != 3) {
				System.out.println("Token count not 3 at " + line);
				continue;
				// TODO: better error handling
			}
			// If the feature is either a) in the vocabulary or
			// b) a structural features, which always begins with a hashtag,
			// we pass it through unaltered.
			if (featureMap.containsKey(tokens[1]) | tokens[1].startsWith("#")) {
				thisVol.addFeature(tokens);
			}
			else {
				tokens[1] = "wordNotInVocab";
				// Words not in the vocabulary still need to be included, for instance,
				// in the total count of words per page. Also the density of rare words
				// is itself revealing. So we count these as a special feature,
				// "wordNotInVocab." Paradoxically, this is itself a word in the
				// vocabulary. :)
				thisVol.addFeature(tokens);
			}
		}
		// end iteration across lines
		 
		return thisVol;
	}
	
	/**
     * A constructor that we use to minimize i/o in cases where the same volume is
     * going to be passed through multiple models. Instead of accepting a filepath,
     * it accepts the already-read lines of the file.
	 * 
	 * @param filelines The raw text of the underlying volume feature file, as a
	 * list of strings.
	 * @param vocabulary The vocabulary used to select features.
	 * @param normalizer Stores feature means and stdevs for normalization.
	 */
	public Corpus(ArrayList<String> filelines, String dirtyID, 
			Vocabulary vocabulary, FeatureNormalizer normalizer) {
		
		this.vocabulary = vocabulary;
		this.normalizer = normalizer;
		featureMap = vocabulary.getMap();
		
		Volume thisVol = readAVolumeFromList(filelines, dirtyID, featureMap);
		volumes = new ArrayList<Volume>(1);
		volumes.add(thisVol);
		
		if (thisVol.totalWords > 1) {

			// We're producing page points.
			datapoints = thisVol.makePagePoints(featureMap);
			numPoints = datapoints.size();
			
			normalizer.normalizeFeatures(datapoints);
			// Note 1) that this method actually mutates the datapoints sent as a parameter,
			// and 2) that it depends on the existence of a previously constructed
			// FeatureNormalizer.
		}
		else {
			numPoints = 0;
			// This will be caught at a higher level so we don't attempt to classify an empty volume.
		}
	}
	
	private Volume readAVolumeFromList(ArrayList<String> filelines, String dirtyID,
			HashMap<String, Integer> featureMap) {
		
		String cleanID = PairtreeReader.cleanID(dirtyID);
		Volume thisVol = new Volume(cleanID);
		
		for (String line : filelines) {
			String[] tokens = line.split("\t");
			int tokenCount = tokens.length;
			if (tokenCount != 3) {
				System.out.println("Token count not 3 at " + line);
				continue;
				// TODO: better error handling
			}
			// If the feature is either a) in the vocabulary or
			// b) a structural features, which always begins with a hashtag,
			// we pass it through unaltered.
			if (featureMap.containsKey(tokens[1]) | tokens[1].startsWith("#")) {
				thisVol.addFeature(tokens);
			}
			else {
				tokens[1] = "wordNotInVocab";
				// Words not in the vocabulary still need to be included, for instance,
				// in the total count of words per page. Also the density of rare words
				// is itself revealing. So we count these as a special feature,
				// "wordNotInVocab." Paradoxically, this is itself a word in the
				// vocabulary. :)
				thisVol.addFeature(tokens);
			}
		}
		// end iteration across lines
		 
		return thisVol;
	}

	private static String normalizeGenre(String genre) {
		for (String[] row : Global.CONVERSIONS) {
			if (row[0].equals(genre)) {
				genre = row[1];
			}
		}
		return genre;
	}
	

	public DataPoint getPoint(int i) {
		return datapoints.get(i);
	}
	
	public MarkovTable makeMarkovTable(ArrayList<String> volumesToUse, double alpha) {
		MarkovTable markov = new MarkovTable(alpha, genres);
		
		for (String volume : volumesToUse) {
			int idx = trainingVols.indexOf(volume);
			ArrayList<String> aGenreSequence = volumeGenres.get(idx);
			markov.trainSequence(aGenreSequence);
		}
		
		markov.interpolateProbabilities();
		markov.writeTable("/Users/tunder/output/markovtable.tsv");
		return markov;
	}
	
	public int getNumGenres() {
		return genres.getSize();
	}
	
	/**
	 * This is relevant mainly to one-volume corpora; it returns
	 * the volume ID for the first volume in the list.
	 */
	public String getFirstVolID() {
		Volume firstVolume = volumes.get(0);
		return firstVolume.volumeID;
	}

}
