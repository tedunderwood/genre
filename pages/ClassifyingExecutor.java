package pages;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
/**
 * @author tunder
 *
 */

public class ClassifyingExecutor implements Runnable {
	
	private String inputDir;
	private String outputDir;
	private int numGenres;
	private ArrayList<String> genres;
	private ArrayList<GenrePredictor> classifiers;
	private MarkovTable markov;
	private Vocabulary vocabulary;
	private FeatureNormalizer normalizer;
	private int threadNumber;
	private boolean isPairtree;
	private boolean outputJson;
	public String predictionMetadata;
	private String modelLabel;
	private final BlockingQueue<String> jobQueue;
	
	public ClassifyingExecutor(String inputDir, String outputDir, String modelPath, int threadNumber,
			boolean isPairtree, boolean outputJson, String modelLabel, BlockingQueue<String> jobQueue) {
		this.inputDir = inputDir;
		this.outputDir = outputDir;
		
		// deserialize the Model, and unpack it
		Model model = deserializeModel(modelPath);
		this.vocabulary = model.vocabulary;
		this.markov = model.markov;
		this.genres = model.genreList.genreLabels;
		this.normalizer = model.normalizer;
		this.classifiers = model.classifiers;
		this.numGenres = genres.size();
		
		this.threadNumber = threadNumber;
		this.isPairtree = isPairtree;
		this.outputJson = outputJson;
		this.modelLabel = modelLabel;
		this.jobQueue = jobQueue;
	}

	@Override
	public void run() {
		// loop getting tasks until we are interrupted
     
		String thisFile = "initial value";
        while (!Thread.currentThread().isInterrupted()) {
        	
        	try {
        		thisFile = jobQueue.poll(1, TimeUnit.MINUTES);
        	} catch (InterruptedException e) {
        		Thread.currentThread().interrupt();
        	}
        	
        	if (thisFile == null) break;
        	if (thisFile.equals("STOP")) break;
		
			// We have a choice of two different corpus constructors, depending on whether we
			// are running this classification on a local directory, or on the cluster using
			// files located in a pairtree hierarchy. The reason for the difference is that
			// we need different i/o routines inside Corpus. This really has nothing to do 
			// with the "wrapper" business, which is purposeless code inherited from an
			// older version.
			
			Corpus thisVolume;
			if (isPairtree) {
				thisVolume = new Corpus(inputDir, thisFile, vocabulary, normalizer);
			}
			else {
				ArrayList<String> wrapper = new ArrayList<String>();
				wrapper.add(thisFile);
				thisVolume = new Corpus(inputDir, wrapper, vocabulary, normalizer);
			}
			
			int numPoints = thisVolume.numPoints;
			
			if (numPoints > 0) {
					
				ArrayList<DataPoint> thesePages = thisVolume.datapoints;
				ArrayList<double[]> rawProbs = new ArrayList<double[]>(numPoints);
				for (int i = 0; i < numPoints; ++i) {
					double[] probs = new double[numGenres];
					Arrays.fill(probs, 0);
					rawProbs.add(probs);
				}
				
				for (int i = 2; i < numGenres; ++i) {
					GenrePredictor classify = classifiers.get(i);
					// System.out.println(classify.reportStatus());
					double[][] probs = classify.testNewInstances(thesePages);
					for (int j = 0; j < numPoints; ++j) {
						rawProbs.get(j)[i] = probs[j][0];
					}
				}
				double[] wordLengths = new double[numPoints];
				for (int i = 0; i < numPoints; ++i) {
					wordLengths[i] = thesePages.get(i).wordcount;
				}
				
				ArrayList<double[]> smoothedProbs = ForwardBackward.smooth(rawProbs, markov, wordLengths);
				smoothedProbs = ForwardBackward.smooth(smoothedProbs, markov, wordLengths);
				// This is really silly, but in practice it works: run the Markov smoothing twice!
		
				ClassificationResult rawResult = new ClassificationResult(rawProbs, numGenres, genres);
				ClassificationResult smoothedResult = new ClassificationResult(smoothedProbs, numGenres, genres);
				
				String outFile = thisFile + ".predict";
				String outPath = outputDir + "/" + outFile;
				
				if (outputJson) {
					JSONResultWriter writer = new JSONResultWriter(outPath, modelLabel, genres);
					writer.writeJSON(thisVolume.numPoints, thisVolume.getFirstVolID(), rawResult, smoothedResult);
				}
				else {
					ArrayList<String> rawPredictions = rawResult.predictions;
					ArrayList<String> predictions = smoothedResult.predictions;
				
					LineWriter writer = new LineWriter(outPath, false);
			
					String[] outlines = new String[numPoints];
					for (int i = 0; i < numPoints; ++i) {
						outlines[i] = thesePages.get(i).label + "\t" + rawPredictions.get(i) + "\t" + predictions.get(i);
						for (int j = 0; j < genres.size(); ++j) {
							double[] thisPageProbs = smoothedProbs.get(i);
							outlines[i] = outlines[i] + "\t" + genres.get(j) + "::" + Double.toString(thisPageProbs[j]);
						}
					}
					writer.send(outlines);
				}
				
			}
        }
        System.out.println("Thread #" + threadNumber + " ordered to stand down.");
	}
	
	private static Model deserializeModel (String modelPath) {
		Model m = null;
	    try {
	    	FileInputStream fileIn = new FileInputStream(modelPath);
	        ObjectInputStream in = new ObjectInputStream(fileIn);
	        m = (Model) in.readObject();
	        in.close();
	        fileIn.close();
	      }
	    catch(IOException except) {
	         except.printStackTrace();
	         return m;
	      }
	    catch(ClassNotFoundException c) {
	         System.out.println("Employee class not found");
	         c.printStackTrace();
	         return m;
	      }
	   return m;
	}
	
}
