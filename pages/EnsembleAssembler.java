/**
 * 
 */
package pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Implements ensembles as an assembly line of threads, each serving a different
 * model, and connected by BlockingQueues.
 * 
 * @param 	model	The model making predictions.
 * @param	name	Model name.
 * @param	modelType	Tells us whether this is a multiclass random forest, or an array
 * 						of one-vs-all logistic models.
 * @param 	inQueue		Transports Unknowns from the previous stage of the assembly line.
 * @param 	outQueue	Transports Unknowns to the next stage of the assembly line.
 *
 */

public class EnsembleAssembler implements Runnable {
	private Model model;
	private String name;
	private String modelType;
	private ArrayList<String> genres;
	private int numGenres;
	private int numVolumes;
	private final BlockingQueue<Unknown> inQueue;
	private final BlockingQueue<Unknown> outQueue;
	
	public EnsembleAssembler(Model model, String modelName, String modelInstruction, int numVolumes, 
			BlockingQueue<Unknown> inQueue, BlockingQueue<Unknown> outQueue) {
		
		this.model = model;
		this.name = modelName;
		this.modelType = modelInstruction;
		this.numVolumes = numVolumes;
		this.inQueue = inQueue;
		this.outQueue = outQueue;
		
		// In addition to simply storing parameters, the constructor re-creates part of the model
		// that is defined transient and may not have been serialized: the dataset definition.
		GenreList genrelist = model.genreList;
		numGenres = genrelist.getSize();
		ArrayList<String> features = model.normalizer.features;
		
		if (modelType.equals("-multiclassforest")) {
			GenrePredictorMulticlass forest = (GenrePredictorMulticlass) model.classifiers.get(0);
			forest.recreateDataset(genrelist, features);
		}
		else {
			// We assume model type is -onevsalllogistic.
			for (int i = 2; i < numGenres; ++i) {
				GenrePredictor classify = model.classifiers.get(i);
				classify.recreateDataset(genrelist, features);
			}
		}
	}
	
	@Override
	public void run() {
		
		try {
			for (int i = 0; i < numVolumes; ++i) {
				Unknown volume = classifyVolume(inQueue.poll(10, TimeUnit.MINUTES));
				outQueue.offer(volume, 10, TimeUnit.MINUTES);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		System.out.println("Terminated loop, model " + name);
	}
	
	private Unknown classifyVolume(Unknown beingClassified) {
			
		Vocabulary vocabulary = model.vocabulary;
		MarkovTable markov = model.markov;
		this.genres = model.genreList.genreLabels;
		FeatureNormalizer normalizer = model.normalizer;
		ArrayList<GenrePredictor> classifiers = model.classifiers;
		numGenres = genres.size();
		
		Corpus thisVolume = new Corpus(beingClassified.getLines(), beingClassified.getLabel(), vocabulary, normalizer);
		beingClassified.putNumPoints(thisVolume.numPoints);
		int numPoints = thisVolume.numPoints;
		String volLabel = thisVolume.getFirstVolID();
		
		if (numPoints < 1) {
			WarningLogger.logWarning(volLabel + " was found to have zero pages!");
			return beingClassified;
		}
		
		ArrayList<DataPoint> thesePages = thisVolume.datapoints;
		ArrayList<double[]> rawProbs;
		if (modelType.equals("-multiclassforest")) {
			GenrePredictorMulticlass forest = (GenrePredictorMulticlass) classifiers.get(0);
			rawProbs = forest.getRawProbabilities(thisVolume, numPoints);
		}
		else {
			// We assume model type is -onevsalllogistic.
			
			rawProbs = new ArrayList<double[]>(numPoints);
			for (int i = 0; i < numPoints; ++i) {
				double[] probs = new double[numGenres];
				Arrays.fill(probs, 0);
				rawProbs.add(probs);
			}
			
			// The logistic approach I use builds separate classifiers for each
			// genre in a one-vs-all fashion. We speed up this embarrassingly
			// parallel problem with an executor service.
			final ExecutorService executor = Executors.newFixedThreadPool(4);
			
			ArrayList<Future<double[][]>> summaries = new ArrayList<Future<double[][]>>(numGenres-2);
			for (int i = 2; i < numGenres; ++i) {
				GenrePredictor classify = classifiers.get(i);
				AtomicClassifyingThread oneGenre = new AtomicClassifyingThread(classify, thesePages);
				Future<double[][]> summary = executor.submit(oneGenre);
				summaries.add(summary);
			}
			
			executor.shutdown();
			try {
				executor.awaitTermination(600, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				System.out.println("Helpful error message: Execution was interrupted.");
			}
			// block until all threads are completed
			
			for (int i = 2; i < numGenres; ++i) {
				Future<double[][]> aFuture = summaries.get(i-2);
				try{
					double[][] probs = aFuture.get();
					for (int j = 0; j < numPoints; ++j) {
						rawProbs.get(j)[i] = probs[j][0];
					}
				} catch (Exception e) {
					System.out.println("Sophisticated error handling.");
				}
			}
		}
		
		double[] wordLengths = new double[numPoints];
		for (int i = 0; i < numPoints; ++i) {
			wordLengths[i] = thesePages.get(i).wordcount;
		}
		// Whatever type of model this is, the remaining steps are the same.
		// Smooth predictions using a hidden Markov model.
				
		ArrayList<double[]> smoothedProbs = ForwardBackward.smooth(rawProbs, markov, wordLengths);
	
		ClassificationResult rawResult = new ClassificationResult(rawProbs, numGenres, genres);
		ClassificationResult smoothResult = new ClassificationResult(smoothedProbs, numGenres, genres);
			
		beingClassified.putRaw(rawResult);
		beingClassified.putSmooth(smoothResult);
		
		return beingClassified;	
	}
	
}
