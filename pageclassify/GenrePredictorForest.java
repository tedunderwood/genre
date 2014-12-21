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
public class GenrePredictorForest extends GenrePredictor implements
		Serializable {
	private static final long serialVersionUID = 161L;
	private WekaDriverForest theClassifier;
	
	public GenrePredictorForest (GenreList genres, ArrayList<String> features, String genreToIdentify, 
			ArrayList<DataPoint> datapoints, String ridgeParameter, boolean verbose) {
		super(genreToIdentify);
		theClassifier = new WekaDriverForest(genres, features, genreToIdentify, datapoints, verbose);
	}
	
	public GenrePredictorForest (String dummyString) {
		super(dummyString);
		theClassifier = new WekaDriverForest(dummyString);
	}
	
	@Override
	public double[][] testNewInstances(ArrayList<DataPoint> pointsToTest) {
		double[][] probabilities = theClassifier.testNewInstances(pointsToTest);
		return probabilities;
	}
}
