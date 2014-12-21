package pages;

import java.util.HashMap;

public class ArgumentParser {
	HashMap<String, String> namedArguments;
	
	/**
	 * An ArgumentParser very simply interprets command line arguments as
	 * a sequence of names (which begin with hyphens) and values associated
	 * with those names (which don't).
	 * 
	 * The constructor is designed to be portable from project to project.
	 * It simply turns the array of strings into a HashMap. The getter and
	 * setter methods that follow are project-specific.
	 * 
	 * @param args Arguments sent from the main class.
	 */
	public ArgumentParser(String[] args) {
		namedArguments = new HashMap<String, String>();
		String name = "";
		// There is initially no name.
		
		for (int i = 0; i < args.length; ++i) {
			String thisword = args[i];
			
			// Behavior follows two paths. Either this word starts with "-"
			// (is a name) or it doesn't. In either case there are two subbranches:
			// either the previous word is stored in "name," or name has length
			// zero, meaning that the previous word was *not* a name.
			
			if (thisword.startsWith("-")) {
				if (name.length() > 0) {
					// If the previous word was a name, and this one is too, then
					// we assume that the previous name should be interpreted as
					// a mere boolean flag, represented here as a string.
					namedArguments.put(name, "true");
					name = thisword;
				}
				else {
					// This initiates a new name.
					name = thisword;
				}
			}
			else {
				if (name.length() > 0) {
					// The previous argument was a name. This one isn't. So store
					// a key-value pair.
					namedArguments.put(name, thisword);
					name = "";
				}
				else {
					// This is not a name. Neither was the previous word. We don't
					// know what to do here.
					System.out.println("Unnamed argument will be ignored: " + thisword);
				}
			}
		}
		
		// Catch any final name.
		if (name.length() > 0) namedArguments.put(name, "true");
		
		for (HashMap.Entry<String, String> entry : namedArguments.entrySet()) {
		    String key = entry.getKey();
		    String value = entry.getValue();
		    System.out.println(key + " -- " + value);
		}
	}
	
	public String getString(String key) {
		String value = namedArguments.getOrDefault(key, "null");
		// This is probably crazy, but I have an aversion to passing actual nulls.
		return value;
	}
	
	/**
	 * This method checks simply whether a string was included as a name
	 * in the arguments. This allows it both to check for the presence of
	 * a named parameter and for a naked name used as a flag. (I.e., set
	 * equal to "true.")
	 * 
	 * @param key String that must have been present as a name in the args.
	 * @return Boolean value indicating whether the name was present.
	 */
	public boolean isPresent (String key) {
		String value = namedArguments.getOrDefault(key, "null");
		boolean returnval = true;
		// This is probably crazy, but I have an aversion to passing actual nulls.
		if (value.equals("null")) returnval = false;
		return returnval;
	}
	
	public int getInteger (String key) {
		int returnval;
		String value = namedArguments.getOrDefault(key, "null");
		if (value.equals("null") | value.equals("true")) {
			return 0;
			// we return 0 if no parameter was attached to the option
		}
		else {
			try {
				returnval = Integer.parseInt(value);
			}
			catch (Exception e) {
				System.out.println("The value assigned to option " + key + "does not parse as an integer.");
				returnval = 0;
			}
			return returnval;
		}
	}

}
