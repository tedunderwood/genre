/**
 * 
 */
package pages;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author tunder
 *
 */
public class GenrePredictorMulticlass extends GenrePredictor implements
		Serializable {
	private static final long serialVersionUID = 162L;
	private WekaDriverMulticlass theClassifier;
	private int numGenres;
	public String predictionMetadata;
	
	public GenrePredictorMulticlass (GenreList genres, ArrayList<String> features, 
			ArrayList<DataPoint> datapoints, boolean verbose) {
		super("multiclass");
		theClassifier = new WekaDriverMulticlass(genres, features, datapoints, verbose);
		numGenres = genres.getSize();
	}
	
	@Override
	public double[][] testNewInstances(ArrayList<DataPoint> pointsToTest) {
		double[][] probabilities = theClassifier.testNewInstances(pointsToTest);
		return probabilities;
	}

	public void classify(String thisFile, String inputDir, String outputDir, MarkovTable markov, ArrayList<String> genres, 
			Vocabulary vocabulary, FeatureNormalizer normalizer, boolean isPairtree) {
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
			double[][] probs = testNewInstances(thesePages);
			for (int i = 0; i < numPoints; ++i) {
				rawProbs.add(probs[i]);
			}
			double[] wordLengths = new double[numPoints];
			for (int i = 0; i < numPoints; ++i) {
				wordLengths[i] = thesePages.get(i).wordcount;
			}
			ArrayList<double[]> smoothedProbs = ForwardBackward.smooth(rawProbs, markov, wordLengths);
			// smoothedProbs = ForwardBackward.smooth(smoothedProbs, markov);
			// This is really silly, but in practice it can work: run the Markov smoothing twice!
	
			ClassificationResult rawResult = new ClassificationResult(rawProbs, numGenres, genres);
			ClassificationResult smoothedResult = new ClassificationResult(smoothedProbs, numGenres, genres);
			
			String outFile = thisFile + ".predict";
			String outPath = outputDir + "/" + outFile;
			
			if (Global.outputJSON) {
				JSONResultWriter writer = new JSONResultWriter(outPath, "multiclassforest", genres);
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
	
	/**
	 * Method called by EnsembleThread, which can handle the smoothing, writing, etc. itself, and
	 * only needs raw probabilities to work with.
	 * 
	 * @param thisVolume
	 * @param numPoints
	 * @return
	 */
	public ArrayList<double[]> getRawProbabilities(Corpus thisVolume, int numPoints) {
		// We have a choice of two different corpus constructors, depending on whether we
		// are running this classification on a local directory, or on the cluster using
		// files located in a pairtree hierarchy. The reason for the difference is that
		// we need different i/o routines inside Corpus. This really has nothing to do 
		// with the "wrapper" business, which is purposeless code inherited from an
		// older version.
		
		ArrayList<DataPoint> thesePages = thisVolume.datapoints;
		ArrayList<double[]> rawProbs = new ArrayList<double[]>(numPoints);
		double[][] probs = testNewInstances(thesePages);
		for (int i = 0; i < numPoints; ++i) {
			double[] defensiveCopy = probs[i].clone();
			rawProbs.add(defensiveCopy);
		}
		
		return rawProbs;
		
	}
	
	public void recreateDataset(GenreList genres, ArrayList<String> features) {
		theClassifier.recreateDataset(genres, features);
	}
	
}
