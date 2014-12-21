package pages;

import java.io.*;
import java.util.Vector;
import java.util.ArrayList;

public class LineReader {
	File fileName;
	
public LineReader(String dirPath) {
	this.fileName = new File(dirPath);
}

public String[] readlines() {
	try{
		BufferedReader filein = new BufferedReader(
				new InputStreamReader(
		                  new FileInputStream(fileName), "UTF8"));
		Vector<String> holding = new Vector<String>(1000,1000);
		int count = 0;
		try{
			while (filein.ready()){
				String line = filein.readLine();
				holding.add(line);
				++ count;
			}
		}
		finally {
			filein.close();
		}
		String[] lineArray = new String[count];
		for (int i = 0; i < count; ++i) {
			String line = holding.get(i);
			lineArray[i] = line;
		}
		return lineArray;
	}
	catch (IOException e){
		System.out.println("Exception: " + e);
		String[] lineArray = new String[1];
		lineArray[0] = null;
		return lineArray;
	}
}

public ArrayList<String> readList() throws InputFileException {
	
	ArrayList<String> holding = new ArrayList<String>(1000);
	try{
		BufferedReader filein = new BufferedReader(
				new InputStreamReader(
		                  new FileInputStream(fileName), "UTF8"));
		try{
			while (filein.ready()){
				String line = filein.readLine();
				holding.add(line);
			}
		}
		finally {
			filein.close();
		}
		return holding;
	}
	catch (IOException e) {
		InputFileException problem = new InputFileException("IOException in LineReader: " + e);
		throw problem;
	}
}

}