/**
 * 
 */
package pages;

import java.util.ArrayList;

/**
 * Each instance of this class packages and interprets predictions made by multiple
 * classifiers about a single volume. "Interpretation" means selecting the most probable
 * genre for each page in the volume, but also involves generating summary
 * statistics that help us assess confidence about this prediction.
 * 
 * @author tunder
 * 
 * @param predictions An array holding the most probable genre for each page.
 * @param averageMaxProb The average, over all pages, of the probability predicted
 * for the genre that was most probable on each page.
 * @param averageGap The average gap between the most probable genre for each page
 * and the next most probable genre for that page.
 *
 */
public class ClassificationResult {
	
	public ArrayList<String> predictions;
	public double averageMaxProb;
	public double averageGap;
	public ArrayList<double[]> probabilities;
	public ArrayList<Double> dissents;
	
	ClassificationResult(ArrayList<double[]> probabilitiesPerPageAndGenre, 
			int numGenres, ArrayList<String> genres) {
		probabilities = probabilitiesPerPageAndGenre;
		int numPages = probabilitiesPerPageAndGenre.size();
		
		predictions = new ArrayList<String>(numPages);
		for (int i = 0; i < numPages; ++i) {
			predictions.add("none");
		}
		
		double sumOfMaxProbs = 0d;
		double sumOfGaps = 0d;
	
		for (int i = 0; i < numPages; ++i) {
			double maxprob = 0d;
			double gapBetweenTopAndNext = 0d;
			for (int j = 0; j < numGenres; ++j) {
				double probabilityPageIsGenreJ = probabilitiesPerPageAndGenre.get(i)[j];
				if (probabilityPageIsGenreJ > maxprob) {
					gapBetweenTopAndNext = probabilityPageIsGenreJ - maxprob;
					maxprob = probabilityPageIsGenreJ;
					predictions.set(i, genres.get(j));
				}
			}
			
			sumOfMaxProbs += maxprob;
			sumOfGaps += gapBetweenTopAndNext;
		}
		
		this.averageMaxProb = sumOfMaxProbs / numPages;
		this.averageGap = sumOfGaps / numPages;
	}
	
	/**
	 * This constructor is used to create consensus results, in which case we already know the
	 * predictions.
	 * 
	 * @param probabilities
	 * @param predictions
	 * @param numGenres
	 */
	ClassificationResult(ArrayList<double[]> probabilities, ArrayList<String> predictions, int numGenres, ArrayList<Double> dissents) {
		this.probabilities = probabilities;
		this.predictions = predictions;
		this.dissents = dissents;
		calculateAverages(numGenres);	
	}
	
	private void calculateAverages (int numGenres) {
		int numPages = probabilities.size();
		double sumOfMaxProbs = 0d;
		double sumOfGaps = 0d;
		
		for (int i = 0; i < numPages; ++i) {
			double maxprob = 0d;
			double gapBetweenTopAndNext = 0d;
			for (int j = 0; j < numGenres; ++j) {
				double probabilityPageIsGenreJ = probabilities.get(i)[j];
				if (probabilityPageIsGenreJ > maxprob) {
					gapBetweenTopAndNext = probabilityPageIsGenreJ - maxprob;
					maxprob = probabilityPageIsGenreJ;
				}
			}
			
			sumOfMaxProbs += maxprob;
			sumOfGaps += gapBetweenTopAndNext;
		}
		
		this.averageMaxProb = sumOfMaxProbs / numPages;
		this.averageGap = sumOfGaps / numPages;
	}

}
