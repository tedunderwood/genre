pages
=====

Java code that does classification at the page level, while preserving the organization of pages in volumes, and using information about volume-level structure to guide page-level inference.

I've used this code to find the fiction, nonfiction, poetry, and drama in 854,000 HathiTrust volumes, while separating those body text genres from genres of paratext (front matter, back matter, publisher's advertisements). However, the workflow for that task is complex; this is not a general-purpose "tool" that could simply be pointed at another collection in order to accomplish the same thing. For instance, the models of genre produced by this code depend on training data; we had to manually tag the pages of 414 volumes with genre information. Training data is inevitably collection-specific.

Instead of viewing this as a tool, it's probably more realistic to view it as a set of useful models for people who are interested in accomplishing something similar. Weka 3.7.11 is a dependency.

Fuller documentation is contained in the project report (link TBA).

This code was originally developed [in a different repo, which you should consult if you're curious about its commit history.](https://github.com/tedunderwood/pages)

The package works by training regularized logistic classifiers whose predictions are then smoothed with a hidden Markov model of volume structure. [See the article.](http://arxiv.org/abs/1309.3323) Weka is a dependency. Originally based on the repo pagelevelHMM, this repo is changed in several major ways:

1. Performance improved by engineering a wider variety of “structural” features (e.g. information about the initial character in lines) -- see Global.java for a full list.

2. Code refactored to support serialization (a model can now be saved to disk and used later for classification).

3. Code refactored to allow classification of datasets that don’t fit in memory.

4. Parsing command-line arguments to specify source and output directories, request crossvalidation, set parameters, etc.

5. Can be applied to volumes stored in the pairtree structure used, for instance, by HathiTrust.

doc
---
This subfolder contains javadocs for the pages package. Not browsable from within the repo, but once cloned, clicking index.html on your local machine should work.

class descriptions
------------------
Coming soon!
