/**
 * 
 */
package pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

/**
 * Not currently implemented as a "thread," so the class name is misleading. But it could be,
 * and probably will be, so implemented.
 * @author tunder
 *
 */
public class EnsembleThread {
	
	private ArrayList<String> genres;
	private int numPoints;
	private int numModels;
	private int numGenres;
	private HashMap<String, Integer> genreIndex;
	
	public EnsembleThread(String thisFile, String inputDir, String outputDir, ArrayList<Model> theEnsemble, 
			ArrayList<String> modelNames, ArrayList<String> modelInstructions, boolean isPairtree) {
		
		numModels = theEnsemble.size();
		assert (numModels == modelNames.size());
		assert (numModels == modelInstructions.size());
		
		ArrayList<String> filelines;
		
		if (isPairtree) {
			PairtreeReader reader = new PairtreeReader(inputDir);
			filelines = reader.getVolume(thisFile);
		}
		else {
			String volumePath = inputDir + thisFile + ".pg.tsv";
			LineReader fileSource = new LineReader(volumePath);
			try {
				filelines = fileSource.readList();
			}
			catch (InputFileException e) {
				WarningLogger.addFileNotFound(thisFile);
				return;
			}
		}
		
		ArrayList<ClassificationResult> allRawResults = new ArrayList<ClassificationResult>(numModels);
		ArrayList<ClassificationResult> allSmoothedResults = new ArrayList<ClassificationResult>(numModels);
		String volLabel = "error";
		
		String outFile = thisFile + ".predict";
		String outPath = outputDir + "/" + outFile;
		
		for (int m = 0; m < numModels; ++m) {
			
			Model model = theEnsemble.get(m);
			String name = modelNames.get(m);
			String modelType = modelInstructions.get(m);
			
			Vocabulary vocabulary = model.vocabulary;
			MarkovTable markov = model.markov;
			this.genres = model.genreList.genreLabels;
			FeatureNormalizer normalizer = model.normalizer;
			ArrayList<GenrePredictor> classifiers = model.classifiers;
			numGenres = genres.size();
			this.genreIndex = model.genreList.genreIndex;
			
			Corpus thisVolume = new Corpus(filelines, thisFile, vocabulary, normalizer);
			numPoints = thisVolume.numPoints;
			volLabel = thisVolume.getFirstVolID();
			
			if (numPoints < 1) {
				WarningLogger.logWarning(thisFile + " was found to have zero pages!");
				return;
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
				
				for (int i = 2; i < numGenres; ++i) {
					GenrePredictor classify = classifiers.get(i);
					// System.out.println(classify.reportStatus());
					double[][] probs = classify.testNewInstances(thesePages);
					for (int j = 0; j < numPoints; ++j) {
						rawProbs.get(j)[i] = probs[j][0];
					}
				}
			}
			
			double[] wordLengths = new double[numPoints];
			for (int i = 0; i < numPoints; ++i) {
				wordLengths[i] = thesePages.get(i).wordcount;
			}
			ArrayList<double[]> smoothedProbs = ForwardBackward.smooth(rawProbs, markov, wordLengths);
	
			ClassificationResult rawResult = new ClassificationResult(rawProbs, numGenres, genres);
			ClassificationResult smoothedResult = new ClassificationResult(smoothedProbs, numGenres, genres);
			
			JSONResultWriter writer = new JSONResultWriter(outPath, name, genres);
			writer.writeJSON(numPoints, volLabel, rawResult, smoothedResult);
			
			allRawResults.add(rawResult);
			allSmoothedResults.add(smoothedResult);
		}
		
		ClassificationResult consensus = reconcilePredictions(allRawResults, allSmoothedResults);
		JSONResultWriter writer = new JSONResultWriter(outPath, "ensemble", genres);
		writer.writeConsensus(volLabel, consensus, numPoints);
		
	}
	
	private ClassificationResult reconcilePredictions(ArrayList<ClassificationResult> rawResults, 
			ArrayList<ClassificationResult> smoothedResults) {
		
		// One strategy for combining ensembles is to average their predicted probabilities
		// for each genre. In practice, simple voting is often more reliable, and we
		// use that as our main strategy. But averaging probabilities is useful
		// for other purposes. It gives us probabilistic output, and we use it to generate
		// tiebreakers in the voting process. We only use smoothed probabilities here.
		
		ArrayList<double[]> averagePredictions = new ArrayList<double[]>(numPoints);
		ArrayList<String> meanGenres = new ArrayList<String>(numPoints);
		
		for (int i = 0; i < numPoints; ++i) {
			double[] thisPage = new double[numGenres];
			Arrays.fill(thisPage, 0d);
			for (int j = 0; j < numModels; ++j) {
				double[] thisPrediction = smoothedResults.get(j).probabilities.get(i);
				// That's a list of genre probabilities for page i produced by model j.
				thisPrediction = normalize(thisPrediction);
				// Sum all predictions for this page.
				for (int k = 0; k < numGenres; ++ k) {
					thisPage[k] += thisPrediction[k];
				}
			}
			thisPage = normalize(thisPage);
			String topGenreByAveraging = maxgenre(thisPage);
			averagePredictions.add(thisPage);
			meanGenres.add(topGenreByAveraging);
		}
		
		// Now for the voting. We allow both rough and smooth models to vote. The smoothed
		// are ultimately preferred, since they've generated the tiebreakers.
		
		ArrayList<String> consensus = new ArrayList<String>(numPoints);
		ArrayList<Double> dissents = new ArrayList<Double>(numPoints);
		
		for (int i = 0; i < numPoints; ++i) {
			int[] theseVotes = new int[numGenres];
			Arrays.fill(theseVotes, 0);
			for (int j = 0; j < numModels; ++j) {
				String roughPrediction = rawResults.get(j).predictions.get(i);
				String smoothPrediction = smoothedResults.get(j).predictions.get(i);
				addVote(theseVotes, roughPrediction);
				addVote(theseVotes, smoothPrediction);
			}
			Pair electionResult = runElection(theseVotes, meanGenres.get(i));
			// the second argument of runElection is a tiebreaker vote generated by numeric
			// averaging of smoothed predictions.	
			consensus.add((String) electionResult.getFirst());
			// The first part of the pair is the consensus result.
			dissents.add((Double) electionResult.getSecond());
			// And the second part reports the level of dissent in that vote.
		}
		
		ClassificationResult consensusResult = new ClassificationResult(averagePredictions, consensus, numGenres, dissents);
		return consensusResult;
	}
	
	private double[] normalize(double[] input) {
		double total = 0d;
		for (double element : input) {
			total += element;
		}
		for (int i = 0; i < input.length; ++ i) {
			input[i] = input[i] / total;
		}
		return input;
	}
	
	private String maxgenre(double[] predictions) {
		String theGenre = "error";
		double max = 0d;
		for (int i = 0; i < numGenres; ++i) {
			if (predictions[i] > max) {
				max = predictions[i];
				theGenre = genres.get(i);
			}
		}
		return theGenre;
	}
	
	private String maxvote(int[] votes) {
		// This assumes there are no ties!
		String theGenre = "error";
		int max = 0;
		for (int i = 0; i < numGenres; ++i) {
			if (votes[i] > max) {
				max = votes[i];
				theGenre = genres.get(i);
			}
		}
		return theGenre;
	}
	
	private void addVote(int[] votes, String aGenre) {
		int idx = genreIndex.get(aGenre);
		votes[idx] += 1;
	}
	
	private Pair runElection(int[] votes, String tiebreaker) {
		int electorate = 0;
		for (int votesInCategory : votes) {
			electorate += votesInCategory;
		}
		int[] sortedvotes = votes.clone();
		Arrays.sort(sortedvotes);
		// This sorted list won't tell us which genre is highest, but it
		// does tell us whether we have a tie or not. Also it tells us
		
		double dissent = (electorate - sortedvotes[numGenres - 1]) / (double) electorate;
		
		if (sortedvotes[numGenres - 1] > sortedvotes[numGenres - 2]) {
			// We have a clear winner. No tiebreaking necessary.
			return new Pair(maxvote(votes), dissent);
		}
		
		else {
			addVote(votes, tiebreaker);
			sortedvotes = votes.clone();
			Arrays.sort(sortedvotes);
			
			if (sortedvotes[numGenres - 1] > sortedvotes[numGenres - 2]) {
				// Tiebreaking has produced a winner.
				return new Pair(maxvote(votes), dissent);
			}
			else {
				return new Pair(resolveTie(votes), dissent);
			}
		}
	}
	
	private String resolveTie(int[] votes) {
		String[] contenders = new String[2];
		Arrays.fill(contenders, "error");
		int max = 0;
		int secondmax = 0;
		for (int i = 0; i < numGenres; ++i) {
			if (votes[i] >= max) {
				secondmax = max;
				max = votes[i];
				contenders[1] = contenders[0];
				contenders[0] = genres.get(i);
			}
			else if (votes[i] >= secondmax) {
				secondmax = votes[i];
				contenders[1] = genres.get(i);
			}
		}
		
		return contenders[new Random().nextInt(2)];
	}
	
}
