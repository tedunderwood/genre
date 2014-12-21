package pages;

import java.util.ArrayList;
import java.util.HashMap;

public class GenreList implements java.io.Serializable {
	public ArrayList<String> genreLabels;
	private static final long serialVersionUID = 114L;
	public HashMap<String, Integer> genreIndex;
	
	public GenreList() {
		genreLabels = new ArrayList<String>();
		genreLabels.add("begin");
		genreLabels.add("end");
		// Whenever you create a new GenreList, it must have entries for the "begin" and "end" genres,
		// because every Markov sequence has these as endposts.
	}
	
	public void addLabel(String newLabel) {
		if (!genreLabels.contains(newLabel)) {
			genreLabels.add(newLabel);
		}
	}
	
	public int getIndex(String genre) {
		return genreLabels.indexOf(genre);
	}
	
	public int getSize() {
		return genreLabels.size();
	}
	
	/**
	 * Compares this GenreList to another to decide whether they are equal.
	 * For this purpose we treat the lists as if they were sets. Two lists 
	 * are equal if they are the same length, and if every member of list A
	 * is present in list B.
	 * 
	 * Since the addLabel method does not allow duplicate labels, it is not
	 * necessary to test the converse (every member of B also present in A).
	 * 
	 * @param otherList The GenreList to be compared to this one.
	 * @return
	 */	
	public boolean equals(GenreList otherList) {
		ArrayList<String> otherLabels = otherList.genreLabels;
		
		if (otherLabels.size() == genreLabels.size()) {
			
			boolean theyareequal = true;
			
			for (String label : genreLabels) {
				boolean matchfound = false;
				for (String otherLabel : otherLabels) {
					if (label.equals(otherLabel)) matchfound = true;
				}
				if (!matchfound){
					theyareequal = false;
				}
			}
			return theyareequal;
		}
		else {
			return false;
		}
	}
	
	public void makeIndex() {
		int numGenres = genreLabels.size();
		genreIndex = new HashMap<String, Integer>(numGenres);
		for (int i = 0; i < numGenres; ++ i) {
			genreIndex.put(genreLabels.get(i), i);
		}
	}

}
