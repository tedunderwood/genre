/**
 * 
 */
package pages;

import java.util.ArrayList;

/**
 * @author tunderwood
 *
 */
public class MarkovTable implements java.io.Serializable {
	GenreList genres;
	ArrayList<Integer> unigramFrequencies;
	ArrayList<ArrayList<Integer>> bigramFrequencies;
	// Rows (first dimension) are keyed to the genre of *previous* page.
	// Columns (second dimension) are the genre of the next.
	double[][] probabilityMatrix;
	double lambda;
	private static final long serialVersionUID = 117L;
	
	public MarkovTable (double lambda, GenreList genres) {
		this.lambda = lambda;
		this.genres = genres;
		unigramFrequencies = new ArrayList<Integer>();
		bigramFrequencies = new ArrayList<ArrayList<Integer>>();
		// Now we need to add rows for genres that are already present in
		// the GenreList. There will be at least two rows, "begin" and "end,"
		// added to every GenreList when it is constructed.
		
		// Initialize unigram and bigram tables.
		int numGenres = genres.getSize();
		for (int i = 0; i < numGenres; ++i) {
			unigramFrequencies.add(0);
			bigramFrequencies.add(new ArrayList<Integer>());
		}
		for (int i = 0; i < numGenres; ++ i) {
			for (int j = 0; j < numGenres; ++j) {
				bigramFrequencies.get(i).add(0);
			}
		}
	}
	
	public MarkovTable (double[][] matrix) {
		this.probabilityMatrix = matrix;
	}
	
	public void writeTable (String filepath) {
		LineWriter writer = new LineWriter(filepath, false); // don't append
		String[] outlines = new String[probabilityMatrix.length];
		
		int i = 0;
		for (double[] row : probabilityMatrix) {
			String line = "";
			for (double value : row) {
				line = line + String.valueOf(value) + "\t";
			}
		outlines[i] = line + "\n";
		++ i;
		}
		
		writer.send(outlines);
	}
	
	public void trainSequence (ArrayList<String> sequenceToClone) {
		ArrayList<String> sequence = new ArrayList<String>(sequenceToClone.size());
		for (String a : sequenceToClone) {
			sequence.add(a);
		}
		sequence.add("end");
		int previdx = genres.getIndex("begin");
		// Every Markov sequence has "begin" as its starting element and
		// concludes with "end." Here we implement this by actually adding
		// "end" to the sequence. We don't add "begin," but we set a variable
		// indicating that we've *just seen* a "begin."
		
		for (String genre : sequence) {
			int idx = genres.getIndex(genre);
			if (idx >= 0) {
				unigramFrequencies.set(idx, unigramFrequencies.get(idx) + 1);
				int current = bigramFrequencies.get(previdx).get(idx);
				// The number of times previdx was followed by idx.
				bigramFrequencies.get(previdx).set(idx, current + 1);
				// We could have done that without first assigning to "current" as a holder,
				// but the code would be opaque.
				previdx = idx;
			}
			if (idx < 0) {
				// This is a genre we have not yet seen, so it needs to be added.
				// Given current code design, this should not be happening.
				System.out.println("Warning: Markov table is discovering a new genre in training process.");
				// TODO: better error handling.
				int numGenres = genres.getSize();
				genres.addLabel(genre);
				unigramFrequencies.add(1);
				// Add a new column to every existing row. Blank except for row = previdx.
				for (int i = 0; i < numGenres; ++ i) {
					if (i == previdx) {
						bigramFrequencies.get(i).add(1);
					}
					else {
						bigramFrequencies.get(i).add(0);
					}
				}
				// Now add a new row.
				numGenres += 1;
				ArrayList<Integer> blankRow = new ArrayList<Integer>();
				for (int i = 0; i < numGenres; ++i) {
					blankRow.add(0);
				}
				bigramFrequencies.add(blankRow);
				previdx = genres.getIndex(genre);
			}
		}
	}
	
	public void interpolateProbabilities() {
		int numGenres = genres.getSize();
		probabilityMatrix = new double[numGenres][numGenres];
		
		// We go through the raw bigram frequencies by row (indicating previous genre).
		// Then for each column (indicating genre that might follow), we increment
		// observed frequencies by raw-frequency-of-genre times lambda.
		System.out.println(bigramFrequencies.size());
		System.out.println(unigramFrequencies.size());
		for (int i = 0; i < bigramFrequencies.size(); ++i) {
			System.out.println(i + ": " + bigramFrequencies.get(i).size());
		}
		for (int i = 0; i < numGenres; ++i) {
			double[] proportions = new double[numGenres];
			double sum = 0d;
			for (int j = 0; j < numGenres; ++j) {
				proportions[j] = bigramFrequencies.get(i).get(j) + (unigramFrequencies.get(j) * lambda) + 2;
				if (j == 0) {
					proportions[j] = 0;
					// There is no possibility of returning to genre "begin."
				}
				sum += proportions[j];
			}
			// Normalize proportions.
			for (int j = 0; j < numGenres; ++j) {
				proportions[j] = proportions[j] / sum;
			}
			probabilityMatrix[i] = proportions;
		}
	}
	
	public double[] transitionProbs(int genre) {
		int seconddimension = probabilityMatrix[genre].length;
		double[] aClone = new double[seconddimension];
		for (int i = 0; i < seconddimension; ++i) {
			aClone[i] = probabilityMatrix[genre][i];
		}
		return aClone;
	}

}
