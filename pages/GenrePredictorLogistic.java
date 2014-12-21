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
public class GenrePredictorLogistic extends GenrePredictor implements
		Serializable {
	private static final long serialVersionUID = 141L;
	private WekaDriver theClassifier;
	
	public GenrePredictorLogistic (GenreList genres, ArrayList<String> features, String genreToIdentify, 
			ArrayList<DataPoint> datapoints, String ridgeParameter, boolean verbose) {
		super(genreToIdentify);
		theClassifier = new WekaDriver(genres, features, genreToIdentify, datapoints, ridgeParameter, verbose);
	}
	
	public GenrePredictorLogistic (String dummyString) {
		super(dummyString);
		theClassifier = new WekaDriver(dummyString);
	}
	
	@Override
	public double[][] testNewInstances(ArrayList<DataPoint> pointsToTest) {
		double[][] probabilities = theClassifier.testNewInstances(pointsToTest);
		return probabilities;
	}
	
	public void recreateDataset(GenreList genres, ArrayList<String> features) {
		theClassifier.recreateDataset(genres, features);
	}

}
