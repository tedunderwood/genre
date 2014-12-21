/**
 * 
 */
package pages;

import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 * @author tunder
 *
 */
public class AtomicClassifyingThread implements Callable<double[][]> {
	
	private GenrePredictor predictor;
	private ArrayList<DataPoint> pages;
	
	public AtomicClassifyingThread(GenrePredictor predictor, ArrayList<DataPoint> pages) {
		this.predictor = predictor;
		this.pages = pages;
	}
	
	public double[][] call() {
		double[][] probs = predictor.testNewInstances(pages);
		return probs;
	}

}
