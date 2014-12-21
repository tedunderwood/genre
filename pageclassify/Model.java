/**
 * 
 */
package pages;

import java.util.ArrayList;
import java.io.Serializable;

/**
 * This class is basically just a convenience wrapper for all the elements of a model.
 * All of its fields must be serializable.
 * 
 * @author tunder
 *
 */
public class Model implements Serializable {
	Vocabulary vocabulary;
	FeatureNormalizer normalizer;
	GenreList genreList; 
	ArrayList<GenrePredictor> classifiers;
	MarkovTable markov;
	private static final long serialVersionUID = 113L;
	
	public Model(Vocabulary vocabulary, FeatureNormalizer normalizer, GenreList genreList, 
			ArrayList<GenrePredictor> classifiers, MarkovTable markov) {
		this.vocabulary = vocabulary;
		this.normalizer = normalizer;
		this.genreList = genreList;
		this.classifiers = classifiers;
		this.markov = markov;
		genreList.makeIndex();
		// That may be used if we use this model as part of an ensemble.
	}

}
