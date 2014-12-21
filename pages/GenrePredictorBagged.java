/**
 * 
 */
package pages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import java.lang.Math;

/**
 * @author tunder
 *
 */
public class GenrePredictorBagged extends GenrePredictor implements
		Serializable {

	private static final long serialVersionUID = 123L;
	private ArrayList<WekaDriver> classifiers;
	public String genreToIdentify;
	
	public GenrePredictorBagged(String dummyString) {
		classifiers = new ArrayList<WekaDriver>();
		WekaDriver dummyClassifier = new WekaDriver(dummyString);
		classifiers.add(dummyClassifier);
		genreToIdentify = "dummy";
	}
	
	public GenrePredictorBagged(GenreList genres, ArrayList<String> features, String genreToIdentify, 
			ArrayList<DataPoint> datapoints, String ridgeParameter, boolean verbose) {
		this.genreToIdentify = genreToIdentify;
		
		int repetitions = 3;
		String[] ridgeoptions = {"0.1", "10", "50"};
		classifiers = new ArrayList<WekaDriver>(repetitions);
		
		Random coin = new Random(System.currentTimeMillis());
		int subsetSize = (int) Math.ceil(datapoints.size() * 0.66);
		
		for (int i = 0; i < repetitions; ++i) {
			// bootstrap sample with replacement
			ArrayList<DataPoint> subsetForThisDistinction = new ArrayList<DataPoint>();
			for (int j = 0; j < subsetSize; ++ j) {
				int idx = coin.nextInt(datapoints.size());
				DataPoint aPoint = datapoints.get(idx);
				subsetForThisDistinction.add(aPoint);
			}
			String ridge = ridgeoptions[i];
			WekaDriver aClassifier = new WekaDriver(genres, features, genreToIdentify, subsetForThisDistinction, 
					ridge, verbose);
			classifiers.add(aClassifier);
		}
	}
	
	public double[][] testNewInstances(ArrayList<DataPoint> pointsToTest) {
		int testSize = pointsToTest.size();
		double[][] testProbs = new double[testSize][2];
		
		for (WekaDriver thisClassifier : classifiers) {
			
			double[][] thisComparison = thisClassifier.testNewInstances(pointsToTest);
			for (int i = 0; i < testSize; ++i) {
				testProbs[i][0] += thisComparison[i][0];
				testProbs[i][1] += thisComparison[i][1];
			}
		}
		
		// now we normalize so that each row adds to 1
		// not really for any reason, but why not?
		for (int i = 0; i < testSize; ++i) {
			double rowSum = testProbs[i][0] + testProbs[i][1];
			testProbs[i][0] = testProbs[i][0] / rowSum;
			testProbs[i][1] = testProbs[i][1] / rowSum;
		}
		
		return testProbs;
	}
	
}
