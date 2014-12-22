confidence
----------

Scripts that we used to predict levels of confidence for individual volumes, and more importantly, for corpora.

The Java workflow under [/pages](https://github.com/tedunderwood/pages) makes page-level genre predictions. It smooths those predictions using a hidden Markov model that understands context, but doesn't otherwise attempt to make judgments about the *volumes* themselves.

However, we do have a lot of useful volume-level information that we could use to assess the reliability of predictions within a given volume. We know how confident, overall, our page-level classifiers were. We have previous metadata about the volume (e.g., does the MARC record say it's a biography?) We can also draw some inferences from the pattern of page labels. Volumes that are evenly divided between different genres -- especially genres that flip back and forth every few pages -- tend to be unreliable.

So we could help users select reliable corpora by flagging certain volumes as unlikely to be reliable.

We generate two kinds of predictions. The estimate of "overall" accuracy
is based on, and attempts to predict, the overall number of pages correctly predicted
in any given volume. We also produce genre-specific predictions for drama, fiction,
and poetry, which attempt to predict the proportion of pages predicted to be genre X
that are correctly identified. (Actually, technically all of these predictions are based
on models that attempt to predict the number of *words* rather than the sheer number
of pages correctly identified.)

The way we do it, technically, is odd. Instead of training a model that directly
predicts accuracy, we train logistic models that estimate the probability
of a binary threshold -- i.e., what's the *probability* that the pages in this volume are
more than 95% correctly identified by genre?

For reasons that I do not pretend to fully understand, this turned out more accurate than a lot of other possible modeling strategies. (I think the basic reason is that the function we're dealing with here is nonlinear.) Anyway, this approach worked well, even when cross-validated, and some others didn't.

Of course, what users really want to know is, what threshold should I set if I want to
ensure that the corpus I'm getting has a certain level of precision? I've calculated
that in an imperfect, ad-hoc way, by measuring the recall and precision stats for corpora
created by thresholding my training corpus at different probability levels. This gives me
predicted precision and recall curves, which I also smoothed with lowess regression to
minimize the influence of arbitrary artefacts in the training set. Then I can use the
predicted probability of accuracy in an individual volume to infer, What precision or recall would I likely get *if* I cut the whole corpus at this probability threshold, discarding all volumes predicted to be less reliable?

The important scripts here are logisticconfidence.py (which creates the models), smooth.R, which smooths calibration curves, and applyconfidence.py, which applies the models and calibration curves to generate the final json files.

Older scripts (logitconfidence and modelconfidence.py) represent discarded branches that used different modeling strategies. A straightforward logistic regression on binarized data turned out to work best.
