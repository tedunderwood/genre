package pages;

import java.util.Comparator;
import java.util.HashMap;

/**
 * This Comparator allows us to sort a list of strings by comparing
 * integer values associated with those strings in a HashMap.
 * 
 */

class ValueComparator implements Comparator<String> {

    HashMap<String, Integer> base;
    public ValueComparator(HashMap<String, Integer> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.    
    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}
