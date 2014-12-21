/**
 * 
 */
package pages;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * In the assembly-line implementation of ensemble classification, this class is basically
 * a factory for the Unknowns that will represent the volumes being classified as they
 * slide through the assembly line from model to model.
 * 
 * @param	filesToProcess	A list of volume IDs to process.
 * @param 	outQueue		The BlockingQueue that is going to take Unknowns to the
 * 							next stage of assembly.
 *
 */
public class EnsembleProducer implements Runnable {
	ArrayList<String> filesToProcess;
	BlockingQueue<Unknown> outQueue;
	String inputDir;
	boolean isPairtree;
	int numModels;
	
	public EnsembleProducer (ArrayList<String> filesToProcess, BlockingQueue<Unknown> outQueue, String inputDir, 
			boolean isPairtree, int numModels) {
		this.filesToProcess = filesToProcess;
		this.inputDir = inputDir;
		this.outQueue = outQueue;
		this.isPairtree = isPairtree;
		this.numModels = numModels;
	}
	
	@Override
	public void run() {
		for (String thisFile : filesToProcess) {
			ArrayList<String> filelines;
			String cleanID = PairtreeReader.cleanID(thisFile);
			
			if (isPairtree) {
				PairtreeReader reader = new PairtreeReader(inputDir);
				filelines = reader.getVolume(thisFile);
				Unknown mystery = new Unknown(cleanID, filelines, numModels);
				try {
					outQueue.offer(mystery, 10, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			else {
				String volumePath = inputDir + thisFile + ".pg.tsv";
				LineReader fileSource = new LineReader(volumePath);
				try {
					filelines = fileSource.readList();
					Unknown mystery = new Unknown(cleanID, filelines, numModels);
					outQueue.offer(mystery, 10, TimeUnit.MINUTES);
				}
				catch (InputFileException e) {
					WarningLogger.addFileNotFound(thisFile);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
	
			}
		}
	}
	

}
