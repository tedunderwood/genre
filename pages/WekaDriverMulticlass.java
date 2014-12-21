/**
 * 
 */
package pages;

import java.util.ArrayList;

import weka.classifiers.trees.RandomForest;
import weka.classifiers.Evaluation;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

/**
 * @author tunder
 *
 */
public class WekaDriverMulticlass implements java.io.Serializable {
	RandomForest forest;
	transient Instances trainingSet;
	ArrayList<Attribute> featureNames;
	int numFeatures;
	int numInstances;
	int numGenres;
	double[][] memberProbs;
	
	private static final long serialVersionUID = 163L;
	
	public WekaDriverMulticlass (GenreList genres, ArrayList<String> features, ArrayList<DataPoint> datapoints, boolean verbose) {
		numFeatures = features.size();
		numInstances = datapoints.size();
		numGenres = genres.getSize();
		memberProbs = new double[numInstances][numGenres];
		
		String outpath = "/Users/tunder/output/classifiers/multiclass.txt";
		
		LineWriter writer = new LineWriter(outpath, true);
		
		featureNames = new ArrayList<Attribute>(numFeatures + 1);
		for (int i = 0; i < numFeatures; ++ i) {
			Attribute a = new Attribute(features.get(i));
			featureNames.add(a);
		}
		
		// Now we add the class attribute.
		ArrayList<String> classValues = new ArrayList<String>(numGenres);
		for (String genre : genres.genreLabels) {
			classValues.add(genre);
		}
		Attribute classAttribute = new Attribute("ClassAttribute", classValues);
		featureNames.add(classAttribute);
		
		trainingSet = new Instances("multiclassForest", featureNames, numInstances);
		trainingSet.setClassIndex(numFeatures);
		
		for (DataPoint aPoint : datapoints) {
			DenseInstance instance = new DenseInstance(numFeatures + 1);
			for (int i = 0; i < numFeatures; ++i) {
				instance.setValue(featureNames.get(i), aPoint.vector[i]);
			}
	
			instance.setValue(featureNames.get(numFeatures), aPoint.genre);
			trainingSet.add(instance);
		}
		
		System.out.println("Forest: multiclass.");
		
		try {
			String[] options = {"-I", "500", "-K", "40", "-num-slots", "12"};
			forest = new RandomForest();
			forest.setOptions(options);
			forest.buildClassifier(trainingSet);
			if (verbose) {
				writer.print(forest.toString());
			}
			 
			Evaluation eTest = new Evaluation(trainingSet);
			eTest.evaluateModel(forest, trainingSet);
			 
			String strSummary = eTest.toSummaryString();
			if (verbose) {
				writer.print(strSummary);
			}
			
			// Get the confusion matrix
			String strMatrix = eTest.toMatrixString();
			if (verbose) {
				writer.print(strMatrix);
			}
		}
		catch (Exception e){
			e.printStackTrace();
			System.out.println(e);
		}
		if (verbose) {
			writer.print("\n\n");
		}
		
	}
	
	public double[][] getPredictions() {
		return memberProbs;
	}
	
	/**
	 * The Instances object that defines the dataset is defined transient, since
	 * it also contains bulky training data that becomes problematic if serialized.
	 * This method recreates the dataset definition without adding training data.
	 * It's designed for use in reconstituting an ensemble of models.
	 * 
	 * @param	genres	Genre list associated with model.
	 * @param 	features	List of features from model.normalizer.
	 */
	public void recreateDataset (GenreList genres, ArrayList<String> features) {
		featureNames = new ArrayList<Attribute>(numFeatures + 1);
		for (int i = 0; i < numFeatures; ++ i) {
			Attribute a = new Attribute(features.get(i));
			featureNames.add(a);
		}
		
		// Now we add the class attribute.
		ArrayList<String> classValues = new ArrayList<String>(numGenres);
		for (String genre : genres.genreLabels) {
			classValues.add(genre);
		}
		Attribute classAttribute = new Attribute("ClassAttribute", classValues);
		featureNames.add(classAttribute);
		
		trainingSet = new Instances("multiclassForest", featureNames, numInstances);
		trainingSet.setClassIndex(numFeatures);
	}
	
	public double[][] testNewInstances(ArrayList<DataPoint> pointsToTest) {

		int testSize = pointsToTest.size();
		double[][] testProbs = new double[testSize][numGenres];
		
		ArrayList<DenseInstance> testSet = new ArrayList<DenseInstance>(testSize);
		
		for (DataPoint aPoint : pointsToTest) {
			DenseInstance instance = new DenseInstance(numFeatures + 1);
			instance.setDataset(trainingSet);
			for (int i = 0; i < numFeatures; ++i) {
				instance.setValue(featureNames.get(i), aPoint.vector[i]);
			}
			instance.setValue(featureNames.get(numFeatures), "poe");
			// It's not true that all new instances are poetry! But it doesn't really matter
			// what nominal class we give these instances; we're classifying them.
			testSet.add(instance);
		}
		
		try{
			for (int i = 0; i < testSize; ++i) {
				DenseInstance anInstance = testSet.get(i);
				testProbs[i] = forest.distributionForInstance(anInstance);
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
			System.out.println(t);
		}
		
		return testProbs;
	}
}
