package pages;
import java.io.*;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import java.util.ArrayList;

public class BZ2LineReader {
	
	static String currentvolume = "";
	static ArrayList<String> lineBuffer = new ArrayList<String>(); 

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		readFile("/Users/tunderwood/Eclipse/zipped.bz2");

	}
	
	public static void readFile(String filename) {
		int bufferlength = 10000;
		
		try {
			FileInputStream fin = new FileInputStream(filename);
			BufferedInputStream in = new BufferedInputStream(fin);
			BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(in);
			final byte[] buffer = new byte[bufferlength];
			try {
				int n = 0;
				int useupto = 0;
				String remainder = "";
				
				while (-1 != (n = bzIn.read(buffer))) {
					System.out.println(n);
				    String millionbytes = remainder + new String(buffer);
				    String[] lines = millionbytes.split("\n");
				    if (millionbytes.endsWith("\n")) {
				    	remainder = "";
				    	useupto = lines.length;
				    }
				    else {
				    	remainder = lines[lines.length - 1];
				    	useupto = lines.length - 1;
				    }
				    splitIntoVolumes(lines, useupto);
				}
			}
			finally {
				bzIn.close();
			}
		}
		catch (Exception e) {
			System.out.println(e);
		}

	}
	
	private static void splitIntoVolumes(String[] lines, int useupto) {
		LineWriter writer = new LineWriter("/Users/tunderwood/Eclipse/unzipped.txt", true);
		for (int i = 0; i < useupto; ++i) {
			writer.print(lines[i]);
		}
	}

}
