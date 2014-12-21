package pages;

import java.util.ArrayList;
import java.util.Collections;
import java.lang.Math;
import java.util.Random;

public class TrainingThread implements Runnable {
	
	private GenreList genres;
	private ArrayList<String> features;
	private String genreToIdentify;
	private ArrayList<DataPoint> datapoints;
	private String ridgeParameter;
	private boolean verbose;
	public GenrePredictor classifier;
	
	public TrainingThread(GenreList genres, ArrayList<String> features, String genreToIdentify, 
			ArrayList<DataPoint> datapoints, String ridgeParameter, boolean verbose, boolean undersample) {
		this.genres = genres;
		this.features = features;
		this.genreToIdentify = genreToIdentify;
		this.ridgeParameter = ridgeParameter;
		this.verbose = verbose;
		
		if (undersample) {
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
			ratio = Math.log(1.73 + ratio);
			// This transformation means that a ratio of 1:1 remains 1:1, but for instance
			// 10:1 will become 2.5:1 and 100:1 becomes 4.7:1.
			
			int samplingCeiling = (int) Math.ceil(ratio * inClassCount);
			if (samplingCeiling > outOfClassCount) samplingCeiling = outOfClassCount;
			
			System.out.println(genreToIdentify + " against " + Integer.toString(samplingCeiling));
			
			Random coinToFlip = new Random(10);
			Collections.shuffle(outClass, coinToFlip);
			
			ArrayList<DataPoint> undersampled = new ArrayList<DataPoint>();
			for (DataPoint inClassPoint: inClass) {
				undersampled.add(inClassPoint);
			}
			
			for (int i = 0; i < samplingCeiling; ++i) {
				undersampled.add(outClass.get(i));
			}
			
			this.datapoints = undersampled;
		}
		else {
			this.datapoints = datapoints;
		}
	}

	@Override
	public void run() {
		if (genreToIdentify.equals("dummy")) {
			this.classifier = new GenrePredictor("dummy");
		}
		else if (Global.allVsAll) {
			this.classifier = new GenrePredictorAllVsAll(genres, features, genreToIdentify, datapoints, ridgeParameter, verbose);
		}
		else if (Global.multipleForests) {
			this.classifier = new GenrePredictorForest(genres, features, genreToIdentify, datapoints, ridgeParameter, verbose);
			System.out.println("Construction worked " + classifier.genre);
		}
		else {
			this.classifier = new GenrePredictorLogistic(genres, features, genreToIdentify, datapoints, ridgeParameter, verbose);
			System.out.println("Construction worked " + classifier.genre);
		}
	}

}
