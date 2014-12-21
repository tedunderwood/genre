/**
 * 
 */
package pages;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * The base class GenrePredictor is an uninteresting wrapper for WekaDriver.
 * It passes everything it's given to WekaDriver and returns everything
 * WekaDriver returns. The reason for its existence is that it can be subclassed
 * to create more interesting patterns, notably GenrePredictorAllVsAll, which
 * implements the same behavior in a very different way.
 *
 */
public class GenrePredictor implements Serializable {
	private static final long serialVersionUID = 121L;
	public String genre;
	public String predictionMetadata;
	
	public GenrePredictor() {
		// purely for subclassing
	}
	
	public GenrePredictor(String aGenre) {
		this.genre = aGenre;
	}
	
	public double[][] testNewInstances(ArrayList<DataPoint> pointsToTest) {
		double[][] probabilities = new double[1][1];
		return probabilities;
	}
	
	public String reportStatus() {
		return "Placeholder.";
	}
	
	public void classify(String thisFile, String inputDir, String outputDir, MarkovTable markov, ArrayList<String> genres, 
			Vocabulary vocabulary, FeatureNormalizer normalizer, boolean isPairtree) {
		
	}
	
	public void recreateDataset(GenreList genres, ArrayList<String> features) {
	}

}
