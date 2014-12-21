/**
 * 
 */
package pages;
import java.util.ArrayList;

// import java.io.File;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.DenseInstance;

/**
 * @author tunderwood
 *
 */
public class WekaDriver implements java.io.Serializable {
	
	Logistic logistic;
	transient Instances trainingSet;
	ArrayList<Attribute> featureNames;
	int numFeatures;
	int numInstances;
	String ridgeParameter;
	String classLabel;
	double[][] memberProbs;
	private static final long serialVersionUID = 115L;
	
	public WekaDriver() {
	}
	
	public WekaDriver (String genre) {
		// Returns a dummy class to fill an unused spot in the ArrayList.
		// TODO: Refactor so this is not necessary.
		this.classLabel = genre;
	}
	
	public WekaDriver (GenreList genres, ArrayList<String> features, String genreToIdentify, ArrayList<DataPoint> datapoints, String ridgeParameter, boolean verbose) {
		numFeatures = features.size();
		numInstances = datapoints.size();
		this.ridgeParameter = ridgeParameter;
		this.classLabel = genreToIdentify;
		memberProbs = new double[numInstances][2];
		
		String outpath = "/Users/tunder/output/classifiers/" + classLabel;
		// File existingVersion = new File(outpath);
		// if (existingVersion.exists()) existingVersion.delete();
		
		LineWriter writer = new LineWriter(outpath, true);
		
		featureNames = new ArrayList<Attribute>(numFeatures + 1);
		for (int i = 0; i < numFeatures; ++ i) {
			Attribute a = new Attribute(features.get(i));
			featureNames.add(a);
		}
		
		// Now we add the class attribute.
		ArrayList<String> classValues = new ArrayList<String>(2);
		classValues.add("positive");
		classValues.add("negative");
		Attribute classAttribute = new Attribute("ClassAttribute", classValues);
		featureNames.add(classAttribute);
		
		trainingSet = new Instances(genreToIdentify, featureNames, numInstances);
		trainingSet.setClassIndex(numFeatures);
		ArrayList<DenseInstance> simpleListOfInstances = new ArrayList<DenseInstance>(numInstances);
		
		int poscount = 0;
		for (DataPoint aPoint : datapoints) {
			DenseInstance instance = new DenseInstance(numFeatures + 1);
			for (int i = 0; i < numFeatures; ++i) {
				instance.setValue(featureNames.get(i), aPoint.vector[i]);
			}
			if (aPoint.genre.equals(genreToIdentify)) {
				instance.setValue(featureNames.get(numFeatures), "positive");
				poscount += 1;
			}
			else {
				instance.setValue(featureNames.get(numFeatures), "negative");
			}
			trainingSet.add(instance);
			simpleListOfInstances.add(instance);
		}
		
		if (verbose) {
			writer.print(genreToIdentify + " count: " + poscount + "\n");
		}
		System.out.println(genreToIdentify + " count: " + poscount);
		
		try {
			String[] options = {"-R", ridgeParameter, "-M", "1500"};
			logistic = new Logistic();
			logistic.setOptions(options);
			logistic.buildClassifier(trainingSet);
			if (verbose) {
				writer.print(logistic.toString());
			}
			 
			Evaluation eTest = new Evaluation(trainingSet);
			eTest.evaluateModel(logistic, trainingSet);
			 
			String strSummary = eTest.toSummaryString();
			if (verbose) {
				writer.print(strSummary);
			}
			
			for (int i = 0; i < numInstances; ++i) {
				DenseInstance anInstance = simpleListOfInstances.get(i);
				memberProbs[i] = logistic.distributionForInstance(anInstance);
			}
			// Get the confusion matrix
			double[][] cmMatrix = eTest.confusionMatrix();
			if (verbose) {
				writer.print("      Really " + genreToIdentify + "     other.");
				writer.print("===================================");
				String[] lineheads = {"ID'd " + genreToIdentify+ ":  ", "ID'd other "};
				for (int i = 0; i < 2; ++i) {
					double[] row = cmMatrix[i];
					writer.print(lineheads[i] + Integer.toString((int) row[0]) + "             " + Integer.toString((int) row[1]));
				}
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
	
	public void recreateDataset (GenreList genres, ArrayList<String> features) {
		featureNames = new ArrayList<Attribute>(numFeatures + 1);
		for (int i = 0; i < numFeatures; ++ i) {
			Attribute a = new Attribute(features.get(i));
			featureNames.add(a);
		}
		
		// Now we add the class attribute.
		ArrayList<String> classValues = new ArrayList<String>(2);
		classValues.add("positive");
		classValues.add("negative");
		Attribute classAttribute = new Attribute("ClassAttribute", classValues);
		featureNames.add(classAttribute);
		
		trainingSet = new Instances(classLabel, featureNames, numInstances);
		trainingSet.setClassIndex(numFeatures);
	}
	
	public double[][] testNewInstances(ArrayList<DataPoint> pointsToTest) {

		String genreToIdentify = classLabel;
		int testSize = pointsToTest.size();
		double[][] testProbs = new double[testSize][2];
		
		ArrayList<DenseInstance> testSet = new ArrayList<DenseInstance>(testSize);
		
		for (DataPoint aPoint : pointsToTest) {
			DenseInstance instance = new DenseInstance(numFeatures + 1);
			instance.setDataset(trainingSet);
			for (int i = 0; i < numFeatures; ++i) {
				instance.setValue(featureNames.get(i), aPoint.vector[i]);
			}
			if (aPoint.genre.equals(genreToIdentify)) {
				instance.setValue(featureNames.get(numFeatures), "positive");
			}
			else {
				instance.setValue(featureNames.get(numFeatures), "negative");
			}
			testSet.add(instance);
		}
		
		try{
			for (int i = 0; i < testSize; ++i) {
				DenseInstance anInstance = testSet.get(i);
				testProbs[i] = logistic.distributionForInstance(anInstance);
			}
		}
		catch (Exception e) {
			System.out.println(e);
		}
		
		return testProbs;
	}
	

}
