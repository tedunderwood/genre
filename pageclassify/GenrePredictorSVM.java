/**
 * 
 */
package pages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * @author tunder
 *
 */
public class GenrePredictorSVM extends GenrePredictor implements Serializable {
	private static final long serialVersionUID = 131L;
	private WekaDriverSVM theClassifier;
	
	public GenrePredictorSVM (GenreList genres, ArrayList<String> features, String genreToIdentify, 
			ArrayList<DataPoint> datapoints, String ridgeParameter, boolean verbose) {
		super(genreToIdentify);
		
		int inClassCount = 0;
		int outOfClassCount = 0;
		ArrayList<DataPoint> inClass = new ArrayList<DataPoint>();
		ArrayList<DataPoint> outClass = new ArrayList<DataPoint>();
		
		for (DataPoint aPoint: datapoints) {
			if (aPoint.genre.equals(genreToIdentify)) {
				inClassCount += 1;
				inClass.add(aPoint);
			}
			else {
				outOfClassCount += 1;
				outClass.add(aPoint);
			}
		}
		
		double ratio = outOfClassCount / (double) inClassCount;
		int ceiling = 8000;
		int inClassTarget = inClassCount;
		int outClassTarget = outOfClassCount;
		if (inClassTarget > ceiling) {
			inClassTarget = ceiling;
			outClassTarget = (int) Math.floor(ratio * ceiling);
		}
		if (outClassTarget > 100000) outClassTarget = 100000;
		
		ArrayList<DataPoint> subset = new ArrayList<DataPoint>();
		subset.addAll(selectFrom(inClass, inClassTarget));
		subset.addAll(selectFrom(outClass, outClassTarget));
		
		theClassifier = new WekaDriverSVM(genres, features, genreToIdentify, subset, ridgeParameter, verbose);
	}
	
	public GenrePredictorSVM (String dummyString) {
		super(dummyString);
		theClassifier = new WekaDriverSVM(dummyString);
	}
	
	@Override
	public double[][] testNewInstances(ArrayList<DataPoint> pointsToTest) {
		double[][] probabilities = theClassifier.testNewInstances(pointsToTest);
		return probabilities;
	}
	
	@Override
	public String reportStatus() {
		return "Status: " + theClassifier.classLabel;
	}
	
	private ArrayList<DataPoint> selectFrom(ArrayList<DataPoint> source, int k) {
		assert (k <= source.size());
		Random coin = new Random(System.currentTimeMillis());
		Collections.shuffle(source, coin);
		// We're selecting without replacement.
		ArrayList<DataPoint> subset = new ArrayList<DataPoint>();
		for (int i = 0; i < k; ++ i) {
			subset.add(source.get(i));
		}
		return subset;
	}
}
