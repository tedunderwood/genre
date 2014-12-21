pages
=====

Java code I use to find the fiction, nonfiction, poetry, and drama in a collection, while separating those body text genres from genres of paratext (bookplate, table of contents, index, publisher's advertisements). It’s designed to divide texts at the page level; divisions below the page level can be made later (in cases where they really matter for macroscopic research -- e.g. prose footnotes in volumes of poetry). 

The goal here is only a provisional map to support macroscopic research; 95% accuracy is a reasonable target. Right now we’re at roughly 92% accuracy. (In a single-label multiclass situation, accuracy is the same as microaveraged F1.)

The package works by training regularized logistic classifiers whose predictions are then smoothed with a hidden Markov model of volume structure. [See the article.](http://arxiv.org/abs/1309.3323) Weka is a dependency. Originally based on the repo pagelevelHMM, this repo is changed in several major ways:

1. Performance improved by engineering a wider variety of “structural” features (e.g. information about the initial character in lines) -- see Global.java for a full list.

2. Code refactored to support serialization (a model can now be saved to disk and used later for classification).

3. Code refactored to allow classification of datasets that don’t fit in memory.

4. Parsing command-line arguments to specify source and output directories, request crossvalidation, set parameters, etc.

5. Can be applied to volumes stored in the pairtree structure used, for instance, by HathiTrust.

class descriptions
------------------
