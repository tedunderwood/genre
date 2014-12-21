package pages;

import java.util.ArrayList;

public class ArrayWriter {
	String separator;
	int rows;
	int columns;
	ArrayList<ArrayList<String>> cells;
	ArrayList<String> header;
	
	public ArrayWriter(String separator) {
		this.separator = separator;
		cells = new ArrayList<ArrayList<String>>();
		header = new ArrayList<String>();
		rows = 0;
		columns = 0;
	}
	
	public void addStringColumn(ArrayList<String> column, String headerLabel) {
		int impliedrows = column.size();
		if (rows == 0 | rows == impliedrows) {
			cells.add(column);
			header.add(headerLabel);
			rows = impliedrows;
			columns += 1;
		}
		else {
			WarningLogger.logWarning("ArrayWriter: adding " + Integer.toString(impliedrows) + " rows to array with "
					+ Integer.toString(rows) + ".");
		}
	}
	
	public void addDoubleColumn(ArrayList<Double> column, String headerLabel) {
		int impliedrows = column.size();
		if (rows == 0 | rows == impliedrows) {
			ArrayList<String> stringColumn = new ArrayList<String>();
			for (Double value : column) {
				stringColumn.add(Double.toString(value));
			}
			cells.add(stringColumn);
			header.add(headerLabel);
			rows = impliedrows;
			columns += 1;
		}
		else {
			WarningLogger.logWarning("ArrayWriter: adding " + Integer.toString(impliedrows) + " rows to array with "
					+ Integer.toString(rows) + ".");
		}
	}
	
	public void addDoubleArray(ArrayList<ArrayList<Double>> doubleArray, ArrayList<String> headerLabels) {
		assert doubleArray.size() == headerLabels.size();
		for (int i = 0; i < doubleArray.size(); ++i) {
			addDoubleColumn(doubleArray.get(i), headerLabels.get(i));
		}
	}
	
	public void addIntegerColumn(ArrayList<Integer> column, String headerLabel) {
		int impliedrows = column.size();
		if (rows == 0 | rows == impliedrows) {
			ArrayList<String> stringColumn = new ArrayList<String>();
			for (Integer value : column) {
				stringColumn.add(Integer.toString(value));
			}
			cells.add(stringColumn);
			header.add(headerLabel);
			rows = impliedrows;
			columns += 1;
		}
		else {
			WarningLogger.logWarning("ArrayWriter: adding " + Integer.toString(impliedrows) + " rows to array with "
					+ Integer.toString(rows) + ".");
		}
	}
	
	public void writeToFile(String filePath) {
		LineWriter outFile = new LineWriter(filePath, false);
		String [] outLines = new String[rows];
		for (int i = 0; i < rows; ++ i) {
			String thisLine = "";
			boolean separatorYet = false;
			for (int j = 0; j < columns; ++ j) {
				thisLine = thisLine + cells.get(j).get(i);
				if (separatorYet) thisLine = thisLine + separator;
				else separatorYet = true;
			}
			outLines[i] = thisLine;
		}
		outFile.send(outLines);
	}
	
}
