package pages;

import java.util.ArrayList;
import java.util.List;

public class PairtreeReader {
	String dataPath;
	static final int NUMCOLUMNS = 3;
	WarningLogger logger;
	
	public PairtreeReader(String dataPath) {
		this.dataPath = dataPath;
	}
	
	private String getPairtreePath(String dirtyID) {
		String clean = cleanID(dirtyID);
		int periodIndex = clean.indexOf(".");
		String prefix = clean.substring(0, periodIndex);
		// the part before the period
		String pathPart = clean.substring(periodIndex+1);
		// ... and the part after, which unfortunately may contain
		// other periods. These need to become commas.
		pathPart = pathPart.replace(".", ",");
		// Then we turn this into a pairtree path.
		String ppath = mapToPPath(pathPart);
		String encapsulatingDirectory = cleanID(pathPart);
		String wholePath = dataPath + prefix + "/pairtree_root/" + ppath + "/"+ encapsulatingDirectory + 
				"/" + encapsulatingDirectory + ".pg.tsv";
		return wholePath;
	}
	
	public ArrayList<String> getVolume(String dirtyID) {
		String path = getPairtreePath(dirtyID);
		LineReader reader = new LineReader(path);
		ArrayList<String> filelines = new ArrayList<String>();
		
		try {
			filelines = reader.readList();
		}
		catch (InputFileException e) {
			WarningLogger.logWarning("Could not open file: " + path);
		}
		return filelines;
	}
	
	public static String cleanID(String dirtyID) {
		dirtyID = dirtyID.replace(":", "+");
		dirtyID = dirtyID.replace("/", "=");
		return dirtyID;
	}
	
	public String mapToPPath(String id) {
		assert id != null;
		List<String> shorties = new ArrayList<String>();
		int start = 0;
		while(start < id.length()) {
			int end = start + 2;
			if (end > id.length()) end = id.length();
			shorties.add(id.substring(start, end));
			start = end;			
		}
		return concat(shorties.toArray(new String[] {}));
	}
	
	private String concat(String... paths) { 
		if (paths == null || paths.length == 0) return null;
		StringBuffer pathBuf = new StringBuffer();
		Character lastChar = null;
		for(int i=0; i < paths.length; i++) {
			if (paths[i] != null) {
				if (lastChar != null && (! "/".equals(lastChar))) pathBuf.append("/");
				pathBuf.append(paths[i]);
				lastChar = paths[i].charAt(paths[i].length()-1);
			}
		}
		return pathBuf.toString();
	}

	
}