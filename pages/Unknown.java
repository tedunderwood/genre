/**
 * 
 */
package pages;
import java.util.*;

/**
 * @author tunder
 *
 */
public class Unknown {
	private ArrayList<String> filelines;
	private String volumeLabel;
	public ArrayList<ClassificationResult> rawResults;
	public ArrayList<ClassificationResult> smoothResults;
	private int numPoints;
	
	public Unknown(String volumeLabel, ArrayList<String> filelines, int numModels) {
		this.filelines = filelines;
		this.volumeLabel = volumeLabel;
		this.rawResults = new ArrayList<ClassificationResult>(numModels);
		this.smoothResults = new ArrayList<ClassificationResult>(numModels);
	}
	
	public void putNumPoints(int num) {
		numPoints = num;
	}
	
	public int getNumPoints() {
		return numPoints;
	}
	
	public int getNumResults() {
		return rawResults.size();
	}
	
	public ArrayList<String> getLines() {
		return filelines;
	}
	
	public String getLabel() {
		return volumeLabel;
	}
	
	public void putRaw(ClassificationResult toAdd) {
		rawResults.add(toAdd);
	}
	
	public void putSmooth(ClassificationResult toAdd) {
		smoothResults.add(toAdd);
	}
	
	public ClassificationResult getSmooth(int j) {
		return smoothResults.get(j);
	}
	
	public ClassificationResult getRaw(int j) {
		return rawResults.get(j);
	}
	
}
