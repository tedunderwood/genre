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
public class GenrePredictorAllVsAll extends GenrePredictor implements
		Serializable {
	private static final long serialVersionUID = 122L;
	private ArrayList<WekaDriver> classifiers;
	public String genreToIdentify;
	
	public GenrePredictorAllVsAll(String dummyString) {
		classifiers = new ArrayList<WekaDriver>();
		WekaDriver dummyClassifier = new WekaDriver(dummyString);
		classifiers.add(dummyClassifier);
		genreToIdentify = "dummy";
	}
	
	public GenrePredictorAllVsAll(GenreList genres, ArrayList<String> features, String genreToIdentify, 
			ArrayList<DataPoint> datapoints, String ridgeParameter, boolean verbose) {
		this.genreToIdentify = genreToIdentify;
		
		int genreCount = genres.getSize() - 3;
		classifiers = new ArrayList<WekaDriver>(genreCount);
		
		for (String genreToDistinguish: genres.genreLabels) {
			if (genreToDistinguish.equals("begin") | genreToDistinguish.equals("end")) continue;
			if (genreToDistinguish.equals(genreToIdentify)) continue;
			
			ArrayList<DataPoint> subsetForThisDistinction = new ArrayList<DataPoint>();
			for (DataPoint aPoint: datapoints) {
				if (aPoint.genre.equals(genreToIdentify) | aPoint.genre.equals(genreToDistinguish)) {
					subsetForThisDistinction.add(aPoint);
				}
			}
			WekaDriver aClassifier = new WekaDriver(genres, features, genreToIdentify, subsetForThisDistinction, 
					ridgeParameter, verbose);
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
