# applyconfidence.py

# Applies models that were trained by logisticconfidence.py in order to estimate
# the likely accuracy of page-level predictions for volumes. Accuracy is being
# estimated at the volume level so that users can choose to filter out volumes
# of dubious reliability.

# We're generating two kinds of predictions. The estimate of "overall" accuracy
# is based on, and attempts to predict, the overall number of pages correctly predicted
# in any given volume. We also produce genre-specific predictions for drama, fiction,
# and poetry, which attempt to predict the proportion of pages predicted to be genre X
# that are correctly identified. (Actually, technically all of these predictions are based
# on models that attempt to predict the number of *words* rather than the sheer number
# of pages correctly identified.)

# The way we do it, technically, is odd. Instead of training a model that directly
# predicts accuracy, we train logistic models that estimate the probability
# of a binary threshold -- i.e., what's the *probability* that the pages in this volume are
# more than 95% correctly identified by genre?

# For reasons that I do not pretend to fully understand, this turned out more accurate than a lot of
# other possible modeling strategies. (I think the basic reason is that the function we're
# dealing with here is nonlinear.) Anyway, this approach worked well, even when cross-validated,
# and some others didn't.

# Of course, what users really want to know is, what threshold should I set if I want to
# ensure that the corpus I'm getting has a certain given level of precision? I've calculated
# that in an imperfect, ad-hoc way, by measuring the recall and precision stats for corpora
# created by thresholding my training corpus at different probability levels. This gives me
# predicted precision and recall curves, which I also smoothed with lowess regression to
# minimize the influence of arbitrary artefacts in the training set. Then I can use the
# predicted probability of accuracy in an individual volume to infer, What precision or recall
# would I likely get *if* I cut the whole corpus at this probability threshold, discarding
# all volumes predicted to be less reliable.

import json
import os, sys
import numpy as np
import pandas as pd
import SonicScrewdriver as utils
from sklearn.linear_model import LogisticRegression
from sklearn import cross_validation
from scipy.stats.stats import pearsonr
import pickle, csv

def intpart(afloat):
    ''' Given a float between 0 and 1, returns an index between 0 and 99.
    We use this to index into precision and recall curves that were calculated
    with thresholds varying through range(0, 1, 0.01).
    '''
    idx = (int(afloat*100))
    if idx < 0:
        idx = 0
    if idx > 99:
        idx = 99
    return idx

def calibrate(probability, curveset):
    ''' Simply returns the corpus precision and recall estimates appropriate
    for a given volume probability.
    '''

    idx = intpart(probability)
    precision = curveset['precision'][idx]
    recall = curveset['recall'][idx]
    return idx/100, precision, recall

def sequence_to_counts(genresequence):
    '''Converts a sequence of page-level predictions to
    a dictionary of counts reflecting the number of pages
    assigned to each genre. Also reports the largest genre.
    Note that this function cannot return "bio." If
    biography is the largest genre it returns "non"fiction.
    It counts bio, but ensures that all votes for bio are also votes
    for non.
    '''

    genrecounts = dict()

    for page in genresequence:
        utils.addtodict(page, 1, genrecounts)
        if page == 'bio':
            utils.addtodict('non', 1, genrecounts)

    # Convert the dictionary of counts into a sorted list, and take the max.
    genretuples = utils.sortkeysbyvalue(genrecounts, whethertoreverse = True)
    maxgenre = genretuples[0][1]

    if maxgenre == 'bio':
        maxgenre = 'non'

    return genrecounts, maxgenre

def count_flips(sequence):
    ''' Volumes that go back and forth a lot between genres are less reliable than
    those with a more stable sequence. So, we count flips.
    '''
    numflips = 0
    prevgenre = ""
    for genre in sequence:
        if genre != prevgenre:
            numflips += 1

        prevgenre = genre

    return numflips

def normalizearray(featurearray):
    '''Normalizes an array by centering on means and
    scaling by standard deviations.
    '''

    numinstances, numfeatures = featurearray.shape
    means = list()
    stdevs = list()
    for featureidx in range(numfeatures):
        thiscolumn = featurearray[ : , featureidx]
        thismean = np.mean(thiscolumn)
        means.append(thismean)
        thisstdev = np.std(thiscolumn)
        stdevs.append(thisstdev)
        featurearray[ : , featureidx] = (thiscolumn - thismean) / thisstdev

    return featurearray

def normalizeformodel(featurearray, modeldict):
    '''Normalizes an array by centering on means and
    scaling by standard deviations associated with the given model.
    This version of the function is designed to operate, actually, on
    a one-dimensional array for a single volume.
    '''

    numfeatures = len(featurearray)
    means = modeldict['means']
    stdevs = modeldict['stdevs']
    for featureidx in range(numfeatures):
        thiscolumn = featurearray[featureidx]
        thismean = means[featureidx]
        thisstdev = stdevs[featureidx]
        featurearray[featureidx] = (thiscolumn - thismean) / thisstdev

    return featurearray

class Prediction:
    ''' Holds information about a single volume, or technically about the
    page-level genre predictions we have made for the volume.
    '''

    def __init__(self, filepath):
        with open(filepath, encoding='utf-8') as f:
            filelines = f.readlines()
        jsonobject = json.loads(filelines[0])
        self.dirtyid = jsonobject['volID']
        self.rawPredictions = jsonobject['rawPredictions']
        self.smoothPredictions = jsonobject['smoothedPredictions']
        self.probabilities = jsonobject['smoothedProbabilities']
        self.avggap = jsonobject['avgGap']
        self.maxprob = jsonobject['avgMaxProb']
        self.pagelen = len(self.smoothPredictions)

        self.genrecounts, self.maxgenre = sequence_to_counts(self.smoothPredictions)
        pagesinmax = self.genrecounts[self.maxgenre]
        self.maxratio = pagesinmax / self.pagelen

        self.rawflipratio = (count_flips(self.rawPredictions) / self.pagelen)
        self.smoothflips = count_flips(self.smoothPredictions)

        if 'bio' in self.genrecounts and self.genrecounts['bio'] > (self.pagelen / 2):
            self.bioflag = True
            print("BIO: " + self.dirtyid)
        else:
            self.bioflag = False

    def addmetadata(self, row, table):
        self.author = table['author'][row]
        self.title = table['title'][row]
        self.date = utils.simple_date(row, table)
        genrelist = table['genres'][row].split(';')
        self.genres = set(genrelist)

        varietiesofnon = ['Bibliographies', 'Catalog', 'Dictionary', 'Encyclopedia', 'Handbooks', 'Indexes', 'Legislation', 'Directories', 'Statistics', 'Legal cases', 'Legal articles', 'Calendars', 'Autobiography', 'Biography', 'Letters', 'Essays', 'Speeches']

        self.nonmetaflag = False
        for genre in varietiesofnon:
            if genre in self.genres:
                self.nonmetaflag = True

    def missingmetadata(self):
        self.author = ''
        self.title = ''
        self.date = ''
        self.genres = set()
        self.nonmetaflag = False

    def getfeatures(self):
        ''' Returns features used for an overall accuracy prediction. There are more of
        these (13) than we use for genre-specific predictions. See logisticconfidence.py
        for more information about features.
        '''

        features = np.zeros(13)

        if self.maxgenre == 'fic':

            if 'Fiction' in self.genres or 'Novel' in self.genres or 'Short stories' in self.genres:
                features[0] = 1

            if 'Drama' in self.genres or 'Poetry' in self.genres or self.nonmetaflag:
                features[1] = 1

        if self.maxgenre == 'poe':

            if 'Poetry' in self.genres or 'poems' in self.title.lower():
                features[2] = 1

            if 'Drama' in self.genres or 'Fiction' in self.genres or self.nonmetaflag:
                features[3] = 1

        if self.maxgenre == 'dra':
            if 'Drama' in self.genres or 'plays' in self.title.lower():
                features[4] = 1

            if 'Fiction' in self.genres or 'Poetry' in self.genres or self.nonmetaflag:
                features[5] = 1

        if self.maxgenre == 'non':

            if self.nonmetaflag:
                features[6] = 1

            if 'Fiction' in self.genres or 'Poetry' in self.genres or 'Drama' in self.genres or 'Novel' in self.genres or 'Short stories' in self.genres:
                features[7] = 1

        features[8] = self.maxratio
        features[9] = self.rawflipratio
        features[10] = self.smoothflips
        features[11] = self.avggap
        features[12] = self.maxprob

        return features

    def genrefeatures(self, agenre):
        ''' Extracts features to characterize the likelihood of accuracy in a
        particular genre.
        '''

        if agenre in self.genrecounts:
            pagesingenre = self.genrecounts[agenre]
        else:
            pagesingenre = 0

        genreproportion = pagesingenre / self.pagelen

        features = np.zeros(8)

        if agenre == 'fic':

            if 'Fiction' in self.genres or 'Novel' in self.genres or 'Short stories' in self.genres:
                features[0] = 1

            if 'Drama' in self.genres or 'Poetry' in self.genres or self.nonmetaflag:
                features[1] = 1

        if agenre == 'poe':

            if 'Poetry' in self.genres or 'poems' in self.title.lower():
                features[0] = 1

            if 'Drama' in self.genres or 'Fiction' in self.genres or self.nonmetaflag:
                features[1] = 1

        if agenre == 'dra':

            if 'Drama' in self.genres or 'plays' in self.title.lower():
                features[0] = 1

            if 'Fiction' in self.genres or 'Poetry' in self.genres or self.nonmetaflag:
                features[1] = 1

        features[2] = genreproportion
        features[3] = self.rawflipratio
        features[4] = self.smoothflips
        features[5] = self.avggap
        features[6] = self.maxprob
        features[7] = self.maxratio

        return features

    def getpredictions(self):
        ''' A getter function that transforms a list of page predictions into a dictionary. It's
        debatable whether we should be representing pages as a dictionary, since our sequencing logic
        not in fact allow skipped pages! But I'm doing it this way because it will be more flexible
        in the long run, in case something happens that I can't anticipate.
        '''
        pagedict = dict()
        for idx, genre in enumerate(self.smoothPredictions):
            pagedict[idx] = genre
        return pagedict

    def getmetadata(self):
        ''' Basically just a getter function for metadata in a Prediction object. Only thing
        interesting is that it filters out certain genre tags known to be unreliable or convey
        little information.
        '''
        metadict = dict()
        metadict['htid'] = self.dirtyid
        metadict['author'] = self.author
        metadict['title'] = self.title
        metadict['inferred_date'] = self.date
        genrelist = []
        for genre in self.genres:
            if genre == "NotFiction":
                continue
            if genre == "UnknownGenre":
                continue
            if genre == "ContainsBiogMaterial":
                continue

            # In my experience, none of those tags are informative in my Hathi dataset.

            genrelist.append(genre.lower())

        metadict['genre_tags'] = ", ".join(genrelist)
        return metadict


# Begin main script.

args = sys.argv

sourcedirfile = args[1]
with open(sourcedirfile, encoding = 'utf-8') as f:
    filelines = f.readlines()

directorylist = [x.strip() for x in filelines]

modeldir = args[2]

genrestocheck = ['fic', 'poe', 'dra']
genrepath = dict()
genremodel = dict()

overallpath = os.path.join(modeldir, 'overallmodel.p')
genrepath['fic'] = os.path.join(modeldir, 'ficmodel.p')
genrepath['dra'] = os.path.join(modeldir, 'dramodel.p')
genrepath['poe'] = os.path.join(modeldir, 'poemodel.p')

with open(overallpath, mode='rb') as f:
    overallmodel = pickle.load(f)

for genre in genrestocheck:
    with open(genrepath[genre], mode='rb') as f:
        genremodel[genre] = pickle.load(f)

fullnames = {'fic': 'fiction', 'poe': 'poetry', 'dra': 'drama'}

# The logistic models we train on volumes are technically
# predicting the probability that an individual volume will
# cross a particular accuracy threshold. For the overall model
# it's .95, for the genres it's .8.

# This doesn't tell us what we really want to know, which is,
# if we construct a corpus of volumes like this, what will our
# precision and recall be? To infer that, we calculate precision
# and recall in the test set using different probability-thresholds,
# smooth the curve, and then use it empiricially to map a
# threshold-probability to a corpus level prediction for precision and recall.

genrestocalibrate = ['overall', 'fic', 'poe', 'dra']

calibration = dict()
for genre in genrestocalibrate:
    calibration[genre] = dict()
    calibration[genre]['precision'] = list()
    calibration[genre]['recall'] = list()

calipath = os.path.join(modeldir, 'calibration.csv')
with open(calipath, encoding = 'utf-8') as f:
    reader = csv.reader(f)
    next(reader, None)
    for row in reader:
        for idx, genre in enumerate(genrestocalibrate):
            calibration[genre]['precision'].append(float(row[idx * 2]))
            calibration[genre]['recall'].append(float(row[(idx * 2) + 1]))

outputdir = args[3]

metadatapath = '/projects/ichass/usesofscale/hathimeta/MergedMonographs.tsv'
# metadatapath = '/Volumes/TARDIS/work/metadata/MergedMonographs.tsv'
# if you run it locally

rows, columns, table = utils.readtsv(metadatapath)

for sourcedir in directorylist:
    predicts = os.listdir(sourcedir)
    predicts = [x for x in predicts if not x.startswith('.')]

    for filename in predicts:
        cleanid = utils.pairtreelabel(filename.replace('.predict', ''))
        fileid = filename.replace('.predict', '')
        filepath = os.path.join(sourcedir, filename)

        try:
            predicted = Prediction(filepath)
        except:
            print("Failure to load prediction from " + filepath)
            continue

        if cleanid in rows:
            predicted.addmetadata(cleanid, table)
        else:
            print('Missing metadata for ' + cleanid)
            predicted.missingmetadata()

        overallfeatures = predicted.getfeatures()
        featurearray = normalizeformodel(np.array(overallfeatures), overallmodel)
        featureframe = pd.DataFrame(featurearray)
        thismodel = overallmodel['model']
        overall95proba = thismodel.predict_proba(featureframe.T)[0][1]

        genreprobs = dict()

        for genre in genrestocheck:
            features = predicted.genrefeatures(genre)
            featurearray = normalizeformodel(np.array(features), genremodel[genre])
            featureframe = pd.DataFrame(featurearray)
            thismodel = genremodel[genre]['model']
            genreprobs[genre] = thismodel.predict_proba(featureframe.T)[0][1]

        jsontemplate = dict()

        jsontemplate['page_genres'] = predicted.getpredictions()
        jsontemplate['hathi_metadata'] = predicted.getmetadata()
        jsontemplate['added_metadata'] = dict()
        jsontemplate['added_metadata']['totalpages'] = predicted.pagelen
        jsontemplate['added_metadata']['maxgenre'] = predicted.maxgenre
        jsontemplate['added_metadata']['genre_counts'] = predicted.genrecounts

        overallprob, overallprecision, overallrecall = calibrate(overall95proba, calibration['overall'])

        overallaccuracy = dict()
        overallaccuracy['prob>95acc'] = overallprob
        overallaccuracy['precision@prob'] = overallprecision
        overallaccuracy['recall@prob'] = overallrecall

        jsontemplate['volume_accuracy'] = overallaccuracy

        for genre in genrestocheck:
            if genre in predicted.genrecounts:
                gpages = predicted.genrecounts[genre]
                gpercent = round((gpages / predicted.pagelen) * 100) / 100
                gprob, gprecision, grecall = calibrate(genreprobs[genre], calibration[genre])
                name = fullnames[genre]
                newdict = dict()
                newdict['prob_' + genre + '>80precise'] = gprob
                newdict['pages_' + genre] = gpages
                newdict['pct_' + genre] = gpercent
                newdict[genre+ '_precision@prob'] = gprecision
                newdict[genre + '_recall@prob'] = grecall
                jsontemplate[name] = newdict

        prefix = filename.split('.')[0]

        subdirectory = os.path.join(outputdir, prefix)
        if not os.path.isdir(subdirectory):
            os.mkdir(subdirectory)

        outpath = os.path.join(subdirectory, fileid + ".json")
        with open(outpath, mode = 'w', encoding = 'utf-8') as f:
            f.write(json.dumps(jsontemplate, sort_keys = True))

