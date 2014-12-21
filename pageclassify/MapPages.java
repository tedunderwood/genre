/**
 * 
 */
package pages;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author tunderwood
 * 
 * This was originally based on Genre/pages/ApplyModel.java, which was ultimately based on
 * HMM/hmm/ApplyModel.java (written 2013).
 * 
 * In late May, 2014, I changed it by parallelizing both the training of the model 
 * and the application of the model (classification of unknown volumes). 
 * Then (June 2, 2014) refactored to allow for crossvalidation.
 * 
 * @param NFOLDS       Number of folds to create if crossvalidating. E.g., tenfold.
 * @param NTHREADS     Number of threads to parallelize across; the same number is used
 *                     for parallelizing training and classification.
 * @param minutesToWait	How long to wait for the ExecutorService governing classification
 * 						to terminate.
 * @param RIDGE        The ridge parameter for regularizing logistic regression.
 * @param featureCount The number of features in the model. This will be greater than
 *                     the number of words in the vocabulary, because it also includes
 *                     structural features.
 *          
 */
public class MapPages {

	static int NTHREADS = 10;
	static int NFOLDS = 5;
	static int minutesToWait = 30;
	static String RIDGE = "0.002";
	static int featureCount;
	static int numGenres;
	static int numInstances;
	static ArrayList<String> genres;
	static final String[][] EQUIVALENT = { { "bio", "non", "adver", "aut"}, {"bookp", "front"}, {"libra", "back", "index"}};
	static final double MARKOVSMOOTHING = .0001d;
	static Vocabulary vocabulary;
	static ArgumentParser parser;
	static String logfile;

	/**
	 * Main method: mostly argument-parsing.
	 * 
	 * @param args 	Options set at the command line.
	 * -train			Include if a model is to be trained; otherwise we expect
	 * -model (path)	Path to a previously-trained model, or
	 * -ensemble (dir)	Folder that contains ensembles as well as an instruction file,
	 * 					"instructions.tsv." Model filenames should end with ".ser"
	 * -multiclassforest	Trains a Random Forest using the ordinary, direct
	 * 						multiclass capability of that algorithm.
	 * -multipleforests		Trains a Random Forest model for each genre using a one-vs-
	 * 						all method. May underperform the previous option.
	 * -allvsall		Trains a logistic model using an all-vs-all methodology, instead
	 * 					of the default one-vs-all strategy of rendering this algorithm
	 * 					multiclass. (Default works better.)
	 * -outputjson		Writes prediction files as jsons.
	 * -bio				Separates biography, autobiography, and letters from other nonfiction.
	 * -index			Separates indexes, glossaries, and bibliographies from other back matter.
	 * -output (dir)	Directory for all output.
	 * -troot (dir)		Directory for training data; needs to include subdirectories
	 * 					/pagefeatures and /genremaps.
	 * -tbranch (subdir)	If specified, defines a subdirectory of -troot for training data.
	 * -toprocess (dir)	Directory of files to be classified. Not needed if you specify
	 * -self			Which implies that training/pagefeatures will be classified.
	 * -cross (int)		Number of crossvalidation folds; e.g., five-fold. The int parameter
	 * 					is optional. Default 5.
	 * -addtraining (dir)	Specifies an additional directory to be used as training data in every
	 * 						fold of crossvalidation, but not treated as ground truth for testing.
	 * 						This is useful e.g. for co-training.		
	 * -save			Model will be saved to output directory. If you're using -cross, this will only
	 * 					save the *last* model.
	 * -local			Indicates that the model will be applied to a local directory. Otherwise we expect
	 * -pairtreeroot (dir)	The root of a pairtree hierarchy, and
	 * -slice (path)		Path to a file containing dirty HathiTrust ids that imply pairtree paths to vols.
	 * -nthreads (int)	Number of threads to run in parallel. Default 10.
	 * -ridge (double)	Ridge parameter for regularizing logistic regression.
	 * -log (path)		Sets a location for warning log other than default: "/Users/tunder/output/warninglog.txt"
	 * -bio				Separates biography (and autobiography and letters) from the rest of nonfiction.
	 * -index			Separates index (and glossary and bibliography) from the rest of back matter.	
	 */
	public static void main(String[] args) {
		
		// We send command-line arguments to a parser and then query the parser
		// to find whether certain options are present, and what values are assigned
		// to them.
		
		logfile = "/Users/tunder/output/warninglog.txt";
		parseGlobalOptions(args);
		
		boolean trainingRun = parser.isPresent("-train");
		// The most important option defines whether this is a training run.
		
		String dirToProcess;
		String dirForOutput;
		String featureDir;
		String genreDir;
		String additionalTrainingDir = null;
		String vocabPath = "/Users/tunder/Dropbox/pagedata/biggestvocabulary.txt";
		
		if (parser.isPresent("-output")) {
			dirForOutput = parser.getString("-output");
		}
		else {
			dirForOutput = "/Volumes/TARDIS/output/" + parser.getString("-tbranch") + "/";
		}
		
		dirForOutput = validateDirectory(dirForOutput, "output");
		
		if (parser.isPresent("-nthreads")) {
			NTHREADS = parser.getInteger("-nthreads");
		}
		
		if (parser.isPresent("-ridge")) {
			RIDGE = parser.getString("-ridge");
		}
		
		boolean local = parser.isPresent("-local");
		
		if (trainingRun) {
			String trainingRootDir = parser.getString("-troot");
			trainingRootDir = validateDirectory(trainingRootDir, "training root");
			
			if (parser.isPresent("-tbranch")) {
				String trainingBranch = parser.getString("-tbranch");
				if (trainingBranch.startsWith("/") | trainingBranch.startsWith("/")) {
					System.out.println("The -tbranch parameter should not include slashes.");
					trainingBranch = trainingBranch.replace("/",  "");
				}
				
				featureDir = trainingRootDir + trainingBranch + "/pagefeatures/";
				genreDir = trainingRootDir + trainingBranch + "/genremaps/";
			}
			else {
				featureDir = trainingRootDir + "pagefeatures/";
				genreDir = trainingRootDir + "genremaps/";
			}
			
			if (parser.isPresent("-self")) dirToProcess = featureDir;
			else dirToProcess = parser.getString("-toprocess");
			dirToProcess = validateDirectory(dirToProcess, "input");
			
			boolean crossvalidate = parser.isPresent("-cross");
			if (crossvalidate) {
				if (parser.getInteger("-cross") > 0) {
					NFOLDS = parser.getInteger("-cross");
				}
			}
			boolean serialize = parser.isPresent("-save");
			
			if (parser.isPresent("-addtraining")) {
				additionalTrainingDir = parser.getString("-addtraining");
			}
			
			trainingRun (vocabPath, featureDir, genreDir, dirToProcess, dirForOutput, 
					additionalTrainingDir, crossvalidate, serialize);
		}
		else if (parser.isPresent("-ensemble")) {
			// This is an ensemble run.
			String ensembleFolder = parser.getString("-ensemble");
			ArrayList<String> volsToProcess;
			boolean isPairtree;
			if (local) {
				isPairtree = false;
				dirToProcess = parser.getString("-toprocess");
				volsToProcess = DirectoryList.getStrippedPGTSVs(dirToProcess);
				System.out.println("We have " + Integer.toString(volsToProcess.size()) + " volumes to process.");
			}
			else {
				isPairtree = true;
				String slicePath = parser.getString("-slice");
				// The path to a list of dirty HTIDs specifying volume locations.
				volsToProcess = getSlice(slicePath);
				dirToProcess = parser.getString("-pairtreeroot");
				minutesToWait = 500;
				// If this is being run on a pairtree, it's probably quite a large workset.
			}
			parallelizeEnsemble(ensembleFolder, dirToProcess, volsToProcess, dirForOutput, isPairtree);
		}
		else {
			if (local) {
				dirToProcess = parser.getString("-toprocess");
				ArrayList<String> volsToProcess = DirectoryList.getStrippedPGTSVs(dirToProcess);
				System.out.println("We have " + Integer.toString(volsToProcess.size()) + " volumes to process.");
				String modelPath = parser.getString("-model");
				String modelName = parser.getString("-modelname");
				// Model model = deserializeModel(modelPath);
				applyModel(modelPath, dirToProcess, volsToProcess, dirForOutput, false, modelName);
				// The final argument == false because this is not a pairtree process.
			}
			else {
				// We infer that this model is going to be applied to volumes in a pairtree structure.
				
				String slicePath = parser.getString("-slice");
				// The path to a list of dirty HTIDs specifying volume locations.
				ArrayList<String> dirtyHtids = getSlice(slicePath);
				dirToProcess = parser.getString("-pairtreeroot");
				
				minutesToWait = 600;
				// If this is being run on a pairtree, it's probably quite a large workset.
				
				String modelPath = parser.getString("-model");
				String modelName = parser.getString("-modelname");
				// Model model = deserializeModel(modelPath);
				
				applyModel(modelPath, dirToProcess, dirtyHtids, dirForOutput, true, modelName);
				// The final argument == true because this is a pairtree process.
			}
		}
		
	}
	
	private static void parseGlobalOptions (String[] args) {
		parser = new ArgumentParser(args);
		if (parser.isPresent("-log")) {
			logfile = parser.getString("-log");
		}
		
		WarningLogger.initializeLogger(true, logfile);
		
		if (parser.isPresent("-index")) {
			Global.separateIndex();
		}
		if (parser.isPresent("-allvsall")) {
			Global.allVsAll = true;
		}
		if (parser.isPresent("-bio")) {
			Global.separateBiography();
		}
		if (parser.isPresent("-undersample")) {
			Global.undersample = true;
		}
		if (parser.isPresent("-multiclassforest")) {
			Global.multiclassForest = true;
		}
		if (parser.isPresent("-multipleforests")) {
			Global.multipleForests = true;
		}
		if (parser.isPresent("-outputjson")) {
			Global.outputJSON = true;
		}
	}
	
	private static void trainingRun (String vocabPath, String featureDir, String genreDir, 
			String dirToProcess, String dirForOutput, String additionalTrainingDir,
			boolean crossvalidate, boolean serialize) {
		
		vocabulary = new Vocabulary(vocabPath, 5000, true);
		// reads in the first 5000 features and adds a catch-all category
		// if there are fewer than 5000 features in vocab, it reads them all
		
		ArrayList<String> volumeLabels = folderIntersection(featureDir, genreDir);
		int numVolumes = volumeLabels.size();
		System.out.println("Intersection of " + numVolumes);
		
		// The additionalTrainingDir is a directory containing additional volumes
		// that should be added to every iteration of crossvalidation -- for instance,
		// as a consequence of cotraining or active learning.
		// We implement this by creating an additional array of volume labels. Since the
		// root path for finding features & genres will be different for these volumes, we
		// need to create arrays of featurePaths and genrePaths that are keyed to the
		// volume labels.
		ArrayList<String> moreVolumeLabels = new ArrayList<String>();
		ArrayList<String> moreFeaturePaths = new ArrayList<String>();
		ArrayList<String> moreGenrePaths = new ArrayList<String>();
		if (additionalTrainingDir != null) {
			String additionalFeatureDir = additionalTrainingDir + "pagefeatures/";
			String additionalGenreDir = additionalTrainingDir + "genremaps/";
			moreVolumeLabels = folderIntersection(additionalFeatureDir, additionalGenreDir);
			for (int i = 0; i < moreVolumeLabels.size(); ++ i) {
				moreFeaturePaths.add(additionalFeatureDir);
				moreGenrePaths.add(additionalGenreDir);
			}
		}

		ArrayList<String> filesToProcess = DirectoryList.getStrippedPGTSVs(dirToProcess);
		
		String outPath = dirForOutput + "/predictionMetadata.tsv";
		LineWriter metadataWriter = new LineWriter(outPath, false);
		metadataWriter.print("htid\tmaxprob\tgap");
		// Create header for predictionMetadata file, overwriting any
		// previous file.
		
		if (crossvalidate) {
			// Our approach to crossvalidation is simply to divide the volumes into N sublists (folds) and run
			// N train-and-classify sequences. In each case we train a model on all volumes *not* in
			// fold N, and then test that model only on volumes in fold N.
			
			// This approach is conceptually simple but it can break in the unlikely event that you
			// get a training set where a particular genre category isn't represented. Since "genres" are
			// currently implemented as a field of the TrainingCorpus they get generated anew each time you
			// create a new TrainingCorpus.
			
			boolean firstPass = true;
			GenreList oldList = new GenreList();
			// if the code executes properly this list will actually never be used; it will be
			// replaced on the first pass.
			
			Partition partition = new Partition(filesToProcess, NFOLDS);
			for (int i = 0; i < NFOLDS; ++i) {
				System.out.println("Iteration: " + Integer.toString(i));
				
				// We take everything not in the current fold as our trainingSet.
				ArrayList<String> trainingSet = partition.volumesExcluding(i);
				ArrayList<String> testSet = partition.volumesInFold(i);
				
				// We need to create an array of featurePaths and genrePaths
				// keyed to these volumes, because if an additionalTrainingDir is
				// present, the directory will not be the same for every volume in
				// the final trainingSet.
				int initialTrainingSize = trainingSet.size();
				ArrayList<String> featurePaths = new ArrayList<String>();
				ArrayList<String> genrePaths = new ArrayList<String>();
				for (int j = 0 ; j < initialTrainingSize; ++ j){
					featurePaths.add(featureDir);
					genrePaths.add(genreDir);
				}
				
				if (additionalTrainingDir != null) {
					trainingSet.addAll(moreVolumeLabels);
					featurePaths.addAll(moreFeaturePaths);
					genrePaths.addAll(moreGenrePaths);
				}
				
				GenreList newGenreList;
				if (Global.multiclassForest){
					newGenreList = multiclassTrainAndClassify(trainingSet, featurePaths, genrePaths, 
							dirToProcess, testSet, dirForOutput, serialize);
				}
				else {
					newGenreList = trainAndClassify(trainingSet, featurePaths, genrePaths, 
						dirToProcess, testSet, dirForOutput, serialize);
				}
				// The important outputs of trainAndClassify are obviously, the genre 
				// predictions that get written to file inside the methof. But the method also 
				// returns a GenreList, which allows us to check that all genres are 
				// represented in each pass of crossvalidation.
				
				if (firstPass) {
					oldList = newGenreList;
					firstPass = false;
				}
				else {
					if (!oldList.equals(newGenreList)) {
						// note that we override the definition of equals for GenreLists.
						System.out.println("Genre lists vary between folds of the corpus.");
					}
				}
			}
		}
		else {
			ArrayList<String> featurePaths = new ArrayList<String>();
			ArrayList<String> genrePaths = new ArrayList<String>();
			for (int j = 0 ; j < numVolumes; ++ j){
				featurePaths.add(featureDir);
				genrePaths.add(genreDir);
			}
			if (Global.multiclassForest){
				multiclassTrainAndClassify(volumeLabels, featurePaths, genrePaths, 
						dirToProcess, filesToProcess, dirForOutput, serialize);
			}
			else {
				trainAndClassify(volumeLabels, featurePaths, genrePaths, dirToProcess, 
						filesToProcess, dirForOutput, serialize);
			}
		}
	
		System.out.println("DONE.");
	}
	
	/**
	 * This is a workhorse method for this package. It trains a model on a given set
	 * of volumes and applies it to another set of volumes. Those sets can be identical, or
	 * can be disjunct (e.g. in crossvalidation).
	 * 
	 * @param trainingVols     A set of volumes IDs to be used in training. These are basically the part
	 *                         of the filename that precedes the extension.
	 * @param featureDir       Directory for page-level feature counts: we assume extension is ".pg.tsv"
	 * @param genreDir         Directory for genre maps: we assume extension is ".map"
	 * @param inputDir         Source directory for files to be classified.
	 * @param volsToProcess    A list of files in that directory to be classified. Here and above, note
	 *                         that we do not assume all files in the directory will be classified.
	 * @param dirForOutput     Self-explanatory. This is where the .map files that result from classification
	 *                         will be written out.
	 */
	private static GenreList trainAndClassify (ArrayList<String> trainingVols, ArrayList<String> featurePaths, ArrayList<String> genrePaths, 
			String inputDir, ArrayList<String> volsToProcess, String dirForOutput, boolean serialize) {
		
		Model model = trainModel(trainingVols, featurePaths, genrePaths);
		
		MarkovTable markov = model.markov;
		ArrayList<String> genres = model.genreList.genreLabels;
		FeatureNormalizer normalizer = model.normalizer;
		ArrayList<GenrePredictor> classifiers = model.classifiers;
		int numGenres = genres.size();
		
//		ExecutorService classifierPool = Executors.newFixedThreadPool(1);
		// I'm taking out the executor service for the classification stage, because reusing the same classifiers
		// in multiple threads is causing a concurrency bug.
		ArrayList<ClassifyingThread> filesToClassify = new ArrayList<ClassifyingThread>(volsToProcess.size());
		
		for (String thisFile : volsToProcess) {
			ClassifyingThread fileClassifier = new ClassifyingThread(thisFile, inputDir, dirForOutput, numGenres, 
					classifiers, markov, genres, vocabulary, normalizer, false, "model");
			// The final parameter == false because this will never be run in a pairtree context.
			fileClassifier.run();
			filesToClassify.add(fileClassifier);
		}
		
//		for (ClassifyingThread fileClassifier: filesToClassify) {
//			classifierPool.execute(fileClassifier);
//		}
//		
//		classifierPool.shutdown();
//		try {
//			classifierPool.awaitTermination(6000, TimeUnit.SECONDS);
//		}
//		catch (InterruptedException e) {
//			System.out.println("Helpful error message: Execution was interrupted.");
//		}
		// block until all threads are completed
		
		
		// write prediction metadata (confidence levels)
		
		String outPath = dirForOutput + "/predictionMetadata.tsv";
		
		LineWriter metadataWriter = new LineWriter(outPath, true);
		String[] metadata = new String[filesToClassify.size()];
		int i = 0;
		for (ClassifyingThread completedClassification : filesToClassify) {
			metadata[i] = completedClassification.predictionMetadata;
			i += 1;
		}
		metadataWriter.send(metadata);
		
		if (serialize) {
			 try {
		         FileOutputStream fileOut =
		         new FileOutputStream(dirForOutput + "/Model.ser");
		         ObjectOutputStream out = new ObjectOutputStream(fileOut);
		         out.writeObject(model);
		         out.close();
		         fileOut.close();
		         System.out.printf("Serialized data is saved in " + dirForOutput + "Model.ser\n");
		      }
			 catch(IOException except) {
		          except.printStackTrace();
		      }
		}
		return model.genreList;
	}
	
	private static GenreList multiclassTrainAndClassify (ArrayList<String> trainingVols, ArrayList<String> featurePaths, ArrayList<String> genrePaths, 
			String inputDir, ArrayList<String> volsToProcess, String dirForOutput, boolean serialize) {
		
		Model model = trainForest(trainingVols, featurePaths, genrePaths);
		
		MarkovTable markov = model.markov;
		ArrayList<String> genres = model.genreList.genreLabels;
		FeatureNormalizer normalizer = model.normalizer;
		ArrayList<GenrePredictor> classifiers = model.classifiers;
		GenrePredictor forest = classifiers.get(0);
		
		String outPath = dirForOutput + "/predictionMetadata.tsv";
		LineWriter metadataWriter = new LineWriter(outPath, true);
		String[] metadata = new String[volsToProcess.size()];
		
		int i = 0;
		for (String thisFile : volsToProcess) {
			// The final parameter == false because this will never be run in a pairtree context.
			forest.classify(thisFile, inputDir, dirForOutput, markov, genres, vocabulary, normalizer, false);
			metadata[i] = forest.predictionMetadata;
			++ i;
		}
		
		metadataWriter.send(metadata);
		
		if (serialize) {
			 try {
		         FileOutputStream fileOut =
		         new FileOutputStream(dirForOutput + "/ForestModel.ser");
		         ObjectOutputStream out = new ObjectOutputStream(fileOut);
		         out.writeObject(model);
		         out.close();
		         fileOut.close();
		         System.out.printf("Serialized data is saved in " + dirForOutput + "/ForestModel.ser\n");
		      }
			 catch(IOException except) {
		          except.printStackTrace();
		      }
		}
		return model.genreList;
	}
	
	private static Model trainModel (ArrayList<String> trainingVols, ArrayList<String> featurePaths, ArrayList<String> genrePaths) {
		
		featureCount = vocabulary.vocabularySize;
		System.out.println(featureCount + " features.");
		Corpus corpus = new Corpus(featurePaths, genrePaths, trainingVols, vocabulary);
		numGenres = corpus.genres.getSize();
		System.out.println(numGenres);
		numInstances = corpus.numPoints;
		genres = corpus.genres.genreLabels;
		FeatureNormalizer normalizer = corpus.normalizer;
		ArrayList<String> features = normalizer.features;
		
		ExecutorService executive = Executors.newFixedThreadPool(NTHREADS);
		ArrayList<TrainingThread> trainingThreads = new ArrayList<TrainingThread>(numGenres);
		
		for (int i = 0; i < numGenres; ++i) {
			String aGenre;
			if (i < 2) aGenre = "dummy";
			else aGenre = genres.get(i);
			// The first two genres are dummy genres for the front and back of the volume. So we don't actually train classifiers
			// for them. The trainingThread class knows to return a dummy classifier when aGenre.equals("dummy").
			
			TrainingThread trainClassifier = new TrainingThread(corpus.genres, features, aGenre, corpus.datapoints, RIDGE, true, Global.undersample);
			trainingThreads.add(trainClassifier);
		}
		
		for (int i = 0; i < numGenres; ++i) {
			executive.execute(trainingThreads.get(i));
		}
		
		executive.shutdown();
		// stops the addition of new threads; pool will terminate when these threads have completed
		try {
			executive.awaitTermination(15000, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			System.out.println("Helpful error message: Execution was interrupted.");
		}
		// block until all threads are completed
		
		ArrayList<GenrePredictor> classifiers = new ArrayList<GenrePredictor>(numGenres);
		
		for (int i = 0; i < numGenres; ++ i) {
			classifiers.add(trainingThreads.get(i).classifier);
		}
			
		MarkovTable markov = corpus.makeMarkovTable(trainingVols, MARKOVSMOOTHING);
		
		Model model = new Model(vocabulary, normalizer, corpus.genres, classifiers, markov);
		return model;
	}
	
	private static Model trainForest (ArrayList<String> trainingVols, ArrayList<String> featurePaths, ArrayList<String> genrePaths) {
		
		featureCount = vocabulary.vocabularySize;
		System.out.println(featureCount + " features.");
		Corpus corpus = new Corpus(featurePaths, genrePaths, trainingVols, vocabulary);
		numGenres = corpus.genres.getSize();
		System.out.println(numGenres);
		numInstances = corpus.numPoints;
		genres = corpus.genres.genreLabels;
		FeatureNormalizer normalizer = corpus.normalizer;
		ArrayList<String> features = normalizer.features;
		
		GenrePredictor multiclassifier = new GenrePredictorMulticlass(corpus.genres, features, corpus.datapoints, true);
		
		ArrayList<GenrePredictor> classifiers = new ArrayList<GenrePredictor>(1);
		classifiers.add(multiclassifier);
			
		MarkovTable markov = corpus.makeMarkovTable(trainingVols, MARKOVSMOOTHING);
		
		Model model = new Model(vocabulary, normalizer, corpus.genres, classifiers, markov);
		return model;
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

	/**
	 * Takes a previously-trained model and applies it to a new set of volumes. We parallelize
	 * by creating multiple threads, each with its own copy of the model. Then we feed files
	 * to those threads through a single BlockingQueue.
	 * 
	 * @param inputDir This can either be a directory that contains files, or the
	 * root directory of a pairtree structure.
	 * @param volsToProcess This is a list of file IDs. If this is being run on a local
	 * directory, these will be 'clean' volume IDs that can be used as filenames. If this is
	 * run on a pairtree, these will be 'dirty' volume IDs specifying a path to each file.
	 * @param dirForOutput Where to write results.
	 * @param isPairtree Boolean flag to tell us whether this is a pairtree run. It gets passed to
	 * the ClassifyingThread, which can invoke two different Corpus constructors depending on the
	 * underlying data source being used.
	 */
	private static void applyModel (String modelPath, String inputDir, ArrayList<String> volsToProcess, 
			String dirForOutput, boolean isPairtree, String modelName) {
		
		int CLASSIFYTHREADS = 6;
		// The number of threads to create.
		
		// Set up the pool. There's actually no reason this couldn't be run as separate threads, because
		// the size of the pool equals the total number of tasks. But this is how I've set it up.
		ExecutorService classifierPool = Executors.newFixedThreadPool(CLASSIFYTHREADS);
		ArrayList<ClassifyingExecutor> workers = new ArrayList<ClassifyingExecutor>(CLASSIFYTHREADS);
		
		// Create the queue so I can pass it to the worker threads.
		BlockingQueue<String> jobQueue = new LinkedBlockingQueue<String>(12000);
		for (int i = 0; i < CLASSIFYTHREADS; ++i) {
			ClassifyingExecutor worker = new ClassifyingExecutor(inputDir, dirForOutput, modelPath, i, 
					isPairtree, Global.outputJSON, modelName, jobQueue);
			workers.add(worker);
		}
		
		// Now we actually load filenames into the queue.
		for (String thisFile : volsToProcess) {
			thisFile = PairtreeReader.cleanID(thisFile);
			try {
				jobQueue.offer(thisFile, 1, TimeUnit.MINUTES);
			} catch (Exception e) {
				System.out.println("Queue overflow, failed to accept " + thisFile);
			}
		}
		
		// To ensure that thre threads stop when the end of the queue is reached, we pack 
		// the end of the queue with STOP signals that they know how to interpret.
		for (int i = 0; i < CLASSIFYTHREADS; ++i) {
			try {
				jobQueue.offer("STOP", 1, TimeUnit.MINUTES);
			} catch (Exception e) {
				System.out.println("Queue overflow, failed to accept STOP signal.");
			}
		}
		
		// Start all the worker jobs.
		for (ClassifyingExecutor worker : workers) {
			classifierPool.execute(worker);
		}
		
		// No more jobs to add. Await termination of the running jobs. This will only
		// happen when they exhaust the queue.
		classifierPool.shutdown();
		try {
			classifierPool.awaitTermination(minutesToWait, TimeUnit.MINUTES);
		}
		catch (InterruptedException e) {
			System.out.println("Helpful error message: Execution was interrupted.");
		}
		
		System.out.println("Classification complete.");
		
	}
	
	public static boolean genresAreEqual (String predictedGenre,
			String targetGenre) {
		if (predictedGenre.equals(targetGenre)) {
			return true;
		} else {
			for (String[] row : EQUIVALENT) {
				if (Arrays.asList(row).contains(predictedGenre) & Arrays.asList(row).contains(targetGenre))
					return true;
			}
		}
		return false;
	}
	
	/**
	 * Given lists of files in two different folders, with different extensions,
	 * creates a list of the intersection between them -- i.e., a list of parts before
	 * the extension that are found in both folders.
	 * 
	 * In particular, it assumes that the featureFiles will have a ".pg.tsv" extension
	 * and the genre files will have a ".map" extension.
	 *  
	 * @param genreFiles
	 * @param featureFiles
	 * @return
	 */
	private static ArrayList<String> folderIntersection(String featureDir, String genreDir) {
		
		ArrayList<String> hathiIDs = new ArrayList<String>();
		
		File featureFolder = new File(featureDir);
		File[] featureFiles = featureFolder.listFiles();
		
		File genreFolder = new File(genreDir);
		File[] genreFiles = genreFolder.listFiles();
		
		System.out.println(genreFiles.length);
		
		for (File aFile: featureFiles) {
			if (!aFile.isFile()) continue;
			// because we don't want directories, etc.
			String filename = aFile.getName();
			int namelength = filename.length();
			if (namelength < 8) continue;
			else {
				int sevenback = namelength - 7;
				// We assume that each file in this folder should end with ".pg.tsv"
				String suffix = filename.substring(sevenback, namelength);
				if (!suffix.equals(".pg.tsv")) continue;
				String idPart = filename.substring(0, sevenback);
				
				boolean isMatched = false;
				
				for (File genreFile: genreFiles) {
					if (!genreFile.isFile()) continue;
					// because we don't want directories, etc.
					String matchname = genreFile.getName();
					int matchlength = matchname.length();
					if (matchlength < 5) continue;
					else {
						int fourback = matchlength - 4;
						// We assume that each file in this folder should end with ".map"
						String anothersuffix = matchname.substring(fourback, matchlength);
						if (!anothersuffix.equals(".map")) continue;
						String anotherIdPart = matchname.substring(0, fourback);
						if (idPart.equals(anotherIdPart)) {
							isMatched = true;
							break;
						}
					}
				}
				
				if (isMatched) hathiIDs.add(idPart);
			}
		}
		return hathiIDs;
	}
	
//	private static void applyEnsemble(String ensembleFolder, String inputDir, ArrayList<String> volsToProcess, 
//			String dirForOutput, boolean isPairtree) {
//		ArrayList<String> ensembleInstructions  = new ArrayList<String>();
//		String ensembleInstructionPath = ensembleFolder + "instructions.tsv";
//		LineReader reader = new LineReader(ensembleInstructionPath);
//
//		try {
//			ensembleInstructions = reader.readList();
//		}
//		catch (InputFileException e) {
//			System.out.println("Missing ensemble file: " + ensembleInstructionPath);
//			System.exit(0);
//		}
//		
//		ArrayList<String> modelPaths = DirectoryList.getMatchingPaths(ensembleFolder, ".ser");
//		ArrayList<String> modelNames = new ArrayList<String>();
//		ArrayList<String> modelInstructions = new ArrayList<String>();
//		for (int i = 0; i < modelPaths.size(); ++i) {
//			String thisPath = modelPaths.get(i);
//			boolean matched = false;
//			for (int j = 0; j < ensembleInstructions.size(); ++ j) {
//				String[] fields = ensembleInstructions.get(j).split("\t");
//				if (fields[0].equals(thisPath)) {
//					modelNames.add(fields[1]);
//					modelInstructions.add(fields[2]);
//					matched = true;
//				}
//			}
//			if (!matched) System.out.println("No match found for a model in instruction file.");
//		}
//		
//		int ensembleSize = modelNames.size();
//		ArrayList<Model> ensemble = new ArrayList<Model>(ensembleSize);
//		for (String aPath : modelPaths) {
//			ensemble.add(deserializeModel(aPath));
//		}
//		for (String thisFile : volsToProcess) {
//			
//			EnsembleThread runEnsemble = new EnsembleThread(thisFile, inputDir, dirForOutput, ensemble, 
//				modelNames, modelInstructions, isPairtree);
//		}
//		System.out.println("DONE.");
//	}
	
	/**
	 * Implements ensemble classification using an assembly-line structure where models
	 * constitute separate threads running concurrently, and each volume to
	 * be classified is passed from model to model. The volume-reader and result-writer
	 * are also separate threads.
	 * @param ensembleFolder	Holding the model and a textfile of instructions.
	 * @param inputDir	Of files to be processed.
	 * @param volsToProcess	A list of file IDs.
	 * @param dirForOutput	Directory for results.
	 * @param isPairtree	Whether the files to be classified are stored in a pairtree.
	 */
	private static void parallelizeEnsemble(String ensembleFolder, String inputDir, ArrayList<String> volsToProcess, 
			String dirForOutput, boolean isPairtree) {
		ArrayList<String> ensembleInstructions  = new ArrayList<String>();
		String ensembleInstructionPath = ensembleFolder + "instructions.tsv";
		LineReader reader = new LineReader(ensembleInstructionPath);

		try {
			ensembleInstructions = reader.readList();
		}
		catch (InputFileException e) {
			System.out.println("Missing ensemble file: " + ensembleInstructionPath);
			System.exit(0);
		}
		
		ArrayList<String> modelPaths = DirectoryList.getMatchingPaths(ensembleFolder, ".ser");
		ArrayList<String> modelNames = new ArrayList<String>();
		ArrayList<String> modelInstructions = new ArrayList<String>();
		for (int i = 0; i < modelPaths.size(); ++i) {
			String thisPath = modelPaths.get(i);
			boolean matched = false;
			for (int j = 0; j < ensembleInstructions.size(); ++ j) {
				String[] fields = ensembleInstructions.get(j).split("\t");
				if (fields[0].equals(thisPath)) {
					modelNames.add(fields[1]);
					modelInstructions.add(fields[2]);
					matched = true;
				}
			}
			if (!matched) System.out.println("No match found for a model in instruction file.");
		}
		
		int ensembleSize = modelNames.size();
		int numVolumes = volsToProcess.size();
		
		// Deserialize each of the models.
		ArrayList<Model> ensemble = new ArrayList<Model>(ensembleSize);
		for (String aPath : modelPaths) {
			ensemble.add(deserializeModel(aPath));
		}
		
		// Now we construct the assembly line, connecting each part to the next
		// with blocking queues.
		
		BlockingQueue<Unknown> firstQueue = new LinkedBlockingQueue<Unknown>(20);
		EnsembleProducer theProducer = new EnsembleProducer (volsToProcess, firstQueue, inputDir, isPairtree, ensembleSize);
		
		BlockingQueue<Unknown> secondQueue = new LinkedBlockingQueue<Unknown>(20);
		EnsembleAssembler firstModeler = new EnsembleAssembler(ensemble.get(0), modelNames.get(0), modelInstructions.get(0), numVolumes, 
				firstQueue, secondQueue);
		
		BlockingQueue<Unknown> thirdQueue = new LinkedBlockingQueue<Unknown>(20);
		EnsembleAssembler secondModeler = new EnsembleAssembler(ensemble.get(1), modelNames.get(1), modelInstructions.get(1), numVolumes, 
				secondQueue, thirdQueue);
		
		BlockingQueue<Unknown> fourthQueue = new LinkedBlockingQueue<Unknown>(20);
		EnsembleAssembler thirdModeler = new EnsembleAssembler(ensemble.get(2), modelNames.get(2), modelInstructions.get(2), numVolumes, 
				thirdQueue, fourthQueue);
		
		BlockingQueue<Unknown> fifthQueue = new LinkedBlockingQueue<Unknown>(20);
		EnsembleAssembler fourthModeler = new EnsembleAssembler(ensemble.get(3), modelNames.get(3), modelInstructions.get(3), numVolumes, 
				fourthQueue, fifthQueue);
		
		EnsembleOutput finalResults = new EnsembleOutput(dirForOutput, fifthQueue, numVolumes, ensembleSize, 
				modelNames, ensemble.get(0).genreList.genreLabels , ensemble.get(0).genreList.genreIndex);
		
		new Thread(theProducer).start();
		new Thread(firstModeler).start();
		new Thread(secondModeler).start();
		new Thread(thirdModeler).start();
		new Thread(fourthModeler).start();
		new Thread(finalResults).start();
		
		// Right now this is not very robust. It terminates because each of these threads is
		// counting files to process and we expect them all to go through all the files. There's
		// no provision for catching failures.
		
		System.out.println("DONE.");
	}

	private static ArrayList<String> getSlice(String slicePath) {
		ArrayList<String> dirtyHtids;
		LineReader getHtids = new LineReader(slicePath);
		try {
			dirtyHtids = getHtids.readList();
		}
		catch (InputFileException e) {
			System.out.println("Missing slice file: " + slicePath);
			dirtyHtids = null;
		}
		return dirtyHtids;
	}
	
	/**
	 * We expect directories to exist, and we expect them to end with a slash "/."
	 * This method ensures both things are true.
	 * 
	 * @param dir Directory to validate.
	 * @param description To use in error message.
	 * @return A directory that ends with a slash.
	 */
	private static String validateDirectory(String dir, String description) {
		if (!dir.endsWith("/")) dir = dir + "/";
		File outputCheck = new File(dir);
		if (!outputCheck.isDirectory()) {
			outputCheck.mkdir();
		}	
		return dir;
	}
}
