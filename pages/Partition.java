/**
 * 
 */
package pages;
import java.util.ArrayList;

/**
 * @author tunderwood
 *
 */
public class Partition {
	int folds;
	ArrayList<String> masterList;
	ArrayList<ArrayList<String>> partitionedVolumes;
	
	public Partition(ArrayList<String> volumesToPartition, int folds) {
		this.masterList = volumesToPartition;
		this.folds = folds;
		
		partitionedVolumes = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < folds; ++i) {
			partitionedVolumes.add(new ArrayList<String>());
		}
		for (int i = 0; i < masterList.size(); ++i) {
			int remainder = i % folds;
			partitionedVolumes.get(remainder).add(masterList.get(i));
		}	
	}
	
	public ArrayList<String> volumesInFold(int fold) {
		return partitionedVolumes.get(fold);
	}
	
	public ArrayList<String> volumesExcluding(int toExclude) {
		ArrayList<String> everythingBut = new ArrayList<String>();
		
		for (int i = 0; i < folds; ++i) {
			if (i != toExclude) {
				everythingBut.addAll(partitionedVolumes.get(i));
			}
		}
		return everythingBut;
	}
}
