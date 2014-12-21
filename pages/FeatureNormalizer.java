/**
 * 
 */
package pages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author tunder
 *
 */
public class FeatureNormalizer implements Serializable {
	private static final long serialVersionUID = 112L;
	public HashMap<String, Integer> featureMap;
	public int featureCount;
	public ArrayList<String> features;
	// Note that this list of features is going to be larger than the size of the vocabulary,
	// because it will include the STRUCTURALFEATURES contained in Global.
	public ArrayList<Double> stdevOfFeatures;
	public ArrayList<Double> meansOfFeatures;
	
	public FeatureNormalizer(Vocabulary vocabulary, ArrayList<DataPoint> datapoints) {
		int numPoints = datapoints.size();
		String[] vocabularyArray = vocabulary.vocabularyArray;
		int vocabSize = vocabularyArray.length;
		int FEATURESADDED = Global.FEATURESADDED;
		features = new ArrayList<String>(vocabSize + FEATURESADDED);
		meansOfFeatures = new ArrayList<Double>(vocabSize + FEATURESADDED);
		stdevOfFeatures = new ArrayList<Double>(vocabSize + FEATURESADDED);
		// Create the list of features.
			
		for (int i = 0; i < vocabSize; ++i) {
			features.add(vocabularyArray[i]);
		}
		for (String aFeature : Global.STRUCTURALFEATURES) {
			features.add(aFeature);
		}
		
		// Now get means.
		for (int i = 0; i < vocabSize + FEATURESADDED; ++i) {
			double sum = 0d;
			for (DataPoint aPoint : datapoints) {
				sum += aPoint.getVector()[i];
			}
			meansOfFeatures.add(sum / numPoints);
		}

		featureCount = vocabSize + FEATURESADDED;
		for (int i = 0; i < featureCount; ++i) {
			stdevOfFeatures.add(0d);
			for (DataPoint aPoint : datapoints) {
				double current = stdevOfFeatures.get(i);
				current = current
						+ Math.pow((aPoint.vector[i] - meansOfFeatures.get(i)),
								2);
				stdevOfFeatures.set(i, current);
			}
			// We've summed the variance; now divide by the number of points and
			// take sqrt to get stdev.
			stdevOfFeatures.set(i,
					Math.sqrt(stdevOfFeatures.get(i) / numPoints));
		}

	}
	
	/**
	 * Centers all features on the feature mean, and normalizes them by their
	 * standard deviations. Aka, transforms features to z-scores. This is
	 * desirable because I'm using regularized logistic regression, which will
	 * shrink coefficients toward the origin, and shrinkage pressure is
	 * distributed more evenly if the features have been normalized.
	 * 
	 */	
	public ArrayList<DataPoint> normalizeFeatures(ArrayList<DataPoint> datapoints) {
		
		double[] vector = new double[featureCount];
		// now normalize ALL the points!
		for (DataPoint aPoint : datapoints) {
			vector = aPoint.vector;
			for (int i = 0; i < featureCount; ++i) {
				vector[i] = (vector[i] - meansOfFeatures.get(i))
						/ stdevOfFeatures.get(i);
			}
			aPoint.setVector(vector);
		}
		return datapoints;
	}
}
