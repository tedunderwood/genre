package pages;

import java.util.ArrayList;
import java.util.Arrays;

public class ClassifyingThread implements Runnable {
	
	private String thisFile;
	private String inputDir;
	private String outputDir;
	private int numGenres;
	private ArrayList<String> genres;
	private ArrayList<GenrePredictor> classifiers;
	private MarkovTable markov;
	private Vocabulary vocabulary;
	private FeatureNormalizer normalizer;
	private boolean isPairtree;
	public String predictionMetadata;
	private String modelLabel;
	
	public ClassifyingThread(String thisFile, String inputDir, String outputDir, int numGenres, 
			ArrayList<GenrePredictor> classifiers, MarkovTable markov, ArrayList<String> genres, 
			Vocabulary vocabulary, FeatureNormalizer normalizer, boolean isPairtree, String modelLabel) {
		this.thisFile = thisFile;
		this.inputDir = inputDir;
		this.outputDir = outputDir;
		this.numGenres = numGenres;
		this.classifiers = classifiers;
		this.markov = markov;
		this.genres = genres;
		this.vocabulary = vocabulary;
		this.normalizer = normalizer;
		this.isPairtree = isPairtree;
		this.modelLabel = modelLabel;
	}

	@Override
	public void run() {
		// We have a choice of two different corpus constructors, depending on whether we
		// are running this classification on a local directory, or on the cluster using
		// files located in a pairtree hierarchy. The reason for the difference is that
		// we need different i/o routines inside Corpus. This really has nothing to do 
		// with the "wrapper" business, which is purposeless code inherited from an
		// older version.
		
		Corpus thisVolume;
		if (isPairtree) {
			thisVolume = new Corpus(inputDir, thisFile, vocabulary, normalizer);
		}
		else {
			ArrayList<String> wrapper = new ArrayList<String>();
			wrapper.add(thisFile);
			thisVolume = new Corpus(inputDir, wrapper, vocabulary, normalizer);
		}
		
		int numPoints = thisVolume.numPoints;
		
		if (numPoints > 0) {
				
			ArrayList<DataPoint> thesePages = thisVolume.datapoints;
			ArrayList<double[]> rawProbs = new ArrayList<double[]>(numPoints);
			for (int i = 0; i < numPoints; ++i) {
				double[] probs = new double[numGenres];
				Arrays.fill(probs, 0);
				rawProbs.add(probs);
			}
			
			for (int i = 2; i < numGenres; ++i) {
				GenrePredictor classify = classifiers.get(i);
				// System.out.println(classify.reportStatus());
				double[][] probs = classify.testNewInstances(thesePages);
				for (int j = 0; j < numPoints; ++j) {
					rawProbs.get(j)[i] = probs[j][0];
				}
			}
			double[] wordLengths = new double[numPoints];
			for (int i = 0; i < numPoints; ++i) {
				wordLengths[i] = thesePages.get(i).wordcount;
			}
			
			ArrayList<double[]> smoothedProbs = ForwardBackward.smooth(rawProbs, markov, wordLengths);
			// smoothedProbs = ForwardBackward.smooth(smoothedProbs, markov, wordLengths);
			// This is really silly, but in practice it works: run the Markov smoothing twice!
	
			ClassificationResult rawResult = new ClassificationResult(rawProbs, numGenres, genres);
			ClassificationResult smoothedResult = new ClassificationResult(smoothedProbs, numGenres, genres);
			
			String outFile = thisFile + ".predict";
			String outPath = outputDir + "/" + outFile;
			
			if (Global.outputJSON) {
				JSONResultWriter writer = new JSONResultWriter(outPath, modelLabel, genres);
				writer.writeJSON(thisVolume.numPoints, thisVolume.getFirstVolID(), rawResult, smoothedResult);
			}
			else {
				ArrayList<String> rawPredictions = rawResult.predictions;
				ArrayList<String> predictions = smoothedResult.predictions;
			
				LineWriter writer = new LineWriter(outPath, false);
		
				String[] outlines = new String[numPoints];
				for (int i = 0; i < numPoints; ++i) {
					outlines[i] = thesePages.get(i).label + "\t" + rawPredictions.get(i) + "\t" + predictions.get(i);
					for (int j = 0; j < genres.size(); ++j) {
						double[] thisPageProbs = smoothedProbs.get(i);
						outlines[i] = outlines[i] + "\t" + genres.get(j) + "::" + Double.toString(thisPageProbs[j]);
					}
				}
				writer.send(outlines);
			}
			
			this.predictionMetadata = thisFile + "\t" + Double.toString(smoothedResult.averageMaxProb) + "\t" +
					Double.toString(smoothedResult.averageGap);
		}
		else {
			this.predictionMetadata = thisFile + "\tNA\tNA";
			// file not found
		}
	}
	
}

