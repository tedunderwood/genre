# Uses metadata to help assess degrees

import os, sys
import SonicScrewdriver as utils

rowindices, columns, metadata = utils.readtsv("/Users/tunder/Dropbox/PythonScripts/hathimeta/ExtractedMetadata.tsv")

modelindices, modelcolumns, modeldata = utils.readtsv("/Users/tunder/Dropbox/PythonScripts/hathimeta/newgenretable.txt")

options = ["non", "bio", "poe", "dra", "fic"]

def keywithmaxval(dictionary):
    maxval = 0
    maxkey = ""

    for key, value in dictionary.items():
        if value > maxval:
            maxval = value
            maxkey = key

    return maxkey

def sequence_to_counts(genresequence):
    '''Converts a sequence of page-level predictions to
    a dictionary of counts reflecting the number of pages
    assigned to each genre. Also reports the largest genre.'''

    genrecounts = dict()
    genrecounts['fic'] = 0
    genrecounts['poe'] = 0
    genrecounts['dra'] = 0
    genrecounts['non'] = 0

    for page in genresequence:
        indexas = page

        # For this purpose, we treat biography and indexes as equivalent to nonfiction.
        if page == "bio" or page == "index" or page == "back" or page == "trv":
            indexas = "non"

        utils.addtodict(indexas, 1, genrecounts)

    # Convert the dictionary of counts into a sorted list, and take the max.
    genretuples = utils.sortkeysbyvalue(genrecounts, whethertoreverse = True)
    maxgenre = genretuples[0][1]

    return genrecounts, maxgenre

def choose_cascade(htid, pagepredictions):
    '''Reads metadata about this volume and uses it, combined with
    the thrust of page-level predictions, to decide what other models,
    if any, should be used to correct/adjust current predictions.

    Returns three boolean flags, indicating whether the volume is
    1) Mostly drama and poetry.
    2) Probably biography.
    3) Probably fiction.

    It's entirely conceivable that more than one of these flags could be true
    at the same time. In that case no cascade will be applied, because we have
    inconsistent/untrustworthy evidence.'''

    global rowindices, columns, metadata

    genresequence = [x for x in pagepredictions]
    # Make a defensive copy of current page predictions

    # Then count genres.
    genrecounts, maxgenre = sequence_to_counts(genresequence)

    if genrecounts['fic'] > 0 and genrecounts['fic'] < (len(genresequence) / 3):
        notfiction = True
    else:
        notfiction = False

    if genrecounts['dra'] > 0 and (genrecounts['non'] > len(genresequence) / 2 or genrecounts['fic'] > len(genresequence) / 2 or genrecounts['poe'] > len(genresequence) * .9):
        notdrama = True
    else:
        notdrama = False


    # Use those counts to decide whether the volume is more than 50% drama and/or poetry.
    if (genrecounts['dra'] + genrecounts['poe']) > (len(genresequence) / 2):
        mostlydrapoe = True
    else:
        mostlydrapoe = False

    # One other flag will be governed by existing metadata.

    probablyfiction = False
    probablybiography = False

    htid = utils.pairtreelabel(htid)
    # convert the clean pairtree filename into a dirty pairtree label for metadata matching

    if htid not in rowindices:
        # We have no metadata for this volume.
        print("Volume missing from ExtractedMetadata.tsv: " + htid)

    else:
        genrestring = metadata["genres"][htid]
        genreinfo = genrestring.split(";")
        # It's a semicolon-delimited list of items.

        for info in genreinfo:

            if info == "Biography" or info == "Autobiography":
                probablybiography = True

            if info == "Fiction" or info == "Novel":
                probablyfiction = True

            if (info == "Poetry" or info == "Poems"):
                mostlydrapoe = True

            if (info == "Drama" or info == "Tragedies" or info == "Comedies"):
                mostlydrapoe = True

        title = metadata["title"][htid].lower()
        titlewords = title.split()

        if "poems" in titlewords or "ballads" in titlewords or "poetical" in titlewords:
            mostlydrapoe = True

        if "comedy" in titlewords or "tragedy" in titlewords or "plays" in titlewords:
            mostlydrapoe = True

    return mostlydrapoe, probablybiography, probablyfiction, notdrama, notfiction

def biography_cascade(pagepredictions):
    '''This cascade is a simple rule-based solution, based on the
    observation that -- although volumes of fiction often include
    biographical intros -- volumes of biography rarely include fiction.
    So if the metadata says this is a biography or autobiography, pages
    classified as fiction are probably in error.'''

    genresequence = [x for x in pagepredictions]
    # Make a defensive copy of current page predictions

    numberofpages = len(genresequence)
    for i in range(numberofpages):
        if genresequence[i] == "fic":
            genresequence[i] = "bio"

    return genresequence

def otherthandrama(currentpredictions, mainmodel):
    '''we've decided the drama predictions are unlikely;
    what's next most likely?'''

    assert len(currentpredictions) == len(mainmodel)

    for i in range(len(currentpredictions)):
        if currentpredictions[i] == 'dra':
            genremax = 0
            maxgenre = ""
            for key, value in mainmodel[i].items():
                mainmodel[i]["dra"] = 0
                if value > genremax:
                    genremax = value
                    maxgenre = key
            currentpredictions[i] = maxgenre

    return currentpredictions

def otherthanfiction(currentpredictions, mainmodel):
    '''we've decided the fiction predictions are unlikely;
    what's next most likely?'''

    assert len(currentpredictions) == len(mainmodel)

    for i in range(len(currentpredictions)):
        if currentpredictions[i] == 'fic':
            genremax = 0
            maxgenre = ""
            for key, value in mainmodel[i].items():
                if key != "fic" and value > genremax:
                    genremax = value
                    maxgenre = key
            currentpredictions[i] = maxgenre

    return currentpredictions

def read_probabilities(stringlist):
    '''Interprets a sequence of lines as a sequence of
    dictionaries mapping genres to probabilities.'''

    pagesequence = list()

    for aline in stringlist:
        aline = aline.rstrip()
        fields = aline.split("\t")
        probdict = dict()

        for field in fields:
            parts = field.split("::")
            genre = parts[0]

            try:
                probability = float(parts[1])
            except:
                probability = 0
                print("Float conversion error!")

            probdict[genre] = probability

        # for current purposes we treat nonfiction and biography
        # as equivalent, so we take the maximum of the two
        # probabilities

        if "non" in probdict and "bio" in probdict:
            probdict["non"] = max(probdict["non"], probdict["bio"])
            probdict.pop("bio")

        pagesequence.append(probdict)

    return pagesequence

def fiction_cascade(currentpredictions, mainmodel, fictiondir):
    '''If metadata indicates the volume is probably fiction, we
    compare the predictions of the main model to predictions made by
    a model trained mostly on fiction -- and favor the latter.

    Returns a list of predictions (genres deemed most likely for each page)
    as well as a "mergedmodel," a list of dictionaries corresponding to pages,
    and reporting the merged probability of each genre for that page.'''

    try:
        fiction_probabilities = list()
        with open(fictiondir, mode = "r", encoding = "utf-8") as f:
            filelines = f.readlines()
        for line in filelines:
            line = line.rstrip()
            fields = line.split('\t')
            fiction_probabilities.append("\t".join(fields[5:]))

        fictionmodel = read_probabilities(fiction_probabilities)

        numpages = len(mainmodel)
        limit = len(fictionmodel)

        mergedpredictions = list()
        mergedmodel = list()

        for i in range(numpages):
            if i < limit:
                ficprobs = fictionmodel[i]
            else:
                ficprobs = mainmodel[i]

            mainprobs = mainmodel[i]
            merged = dict()
            # merged will hold the new probabilities for each genre
            # on this page

            for key, ficvalue in ficprobs.items():
                if key in mainprobs:
                    newvalue = (0.75 * ficvalue) + (0.25 * mainprobs[key])
                    # This is the heart of the process. The coefficients here
                    # are currently arbitrary.
                else:
                    newvalue = ficvalue

                merged[key] = newvalue

            thispage = keywithmaxval(merged)
            if thispage != currentpredictions[i] and thispage != "" and currentpredictions[i] == "non":
                pass
            else:
                thispage = currentpredictions[i]

            mergedpredictions.append(thispage)
            mergedmodel.append(merged)

    except:
        mergedpredictions = currentpredictions
        mergedmodel = mainmodel

    return mergedpredictions, mergedmodel

def drapoe_cascade(currentpredictions, mainmodel, drapoepath):
    '''If metadata indicates the volume is probably drama or poetry, we
    compare the predictions of the main model to predictions made by
    a model trained mostly on drama and poetry -- and favor the latter.

    Returns a list of predictions (genres deemed most likely for each page)
    as well as a "mergedmodel," a list of dictionaries corresponding to pages,
    and reporting the merged probability of each genre for that page.'''
    allowables = {"non", "bio"}
    try:
        poetry_probabilities = list()
        with open(drapoepath, mode = "r", encoding = "utf-8") as f:
            filelines = f.readlines()
        for line in filelines:
            line = line.rstrip()
            fields = line.split('\t')
            poetry_probabilities.append("\t".join(fields[5:]))

        poetrymodel = read_probabilities(poetry_probabilities)

        limit = len(poetrymodel)

        numpages = len(mainmodel)

        mergedpredictions = list()
        mergedmodel = list()

        for i in range(numpages):
            if i < limit:
                poeprobs = poetrymodel[i]
            else:
                poeprobs = mainmodel[i]

            mainprobs = mainmodel[i]
            merged = dict()
            # merged will hold the new probabilities for each genre
            # on this page

            for key, poevalue in poeprobs.items():
                if key in mainprobs:
                    newvalue = (0.5 * poevalue) + (0.5 * mainprobs[key])
                    # This is the heart of the process. The coefficients here
                    # are currently arbitrary.
                else:
                    newvalue = poevalue

                merged[key] = newvalue

            thispage = keywithmaxval(merged)
            if thispage != currentpredictions[i] and thispage != "" and currentpredictions[i] == "non":
                pass
            else:
                thispage = currentpredictions[i]

            mergedpredictions.append(thispage)
            mergedmodel.append(merged)

    except:
        mergedpredictions = currentpredictions
        mergedmodel = mainmodel
        print("Skipped drapoe.")

    return mergedpredictions, mergedmodel

def metadata_check(htid, inputsequence):
    global options, rowindices, columns, metadata, modelindices, modelcolumns, modeldata
    '''Assesses whether previous metadata tend to deny or confirm the
    thrust of page-level genre predictions. For this purpose we use both
    genre codes extracted from the MARC record and the predictions of a volume-
    level probabilistic model.

    Returns two parameters: 1) a dictionary of "confirmations" that indicate
    whether metadata aligns with page-level predictions in six specific ways.
    2) The "maxgenre" or genre most commonly predicted at the page level.'''

    genresequence = [x for x in inputsequence]
    # make a defensive copy of incoming parameter

    htid = utils.pairtreelabel(htid)
    # convert the htid into a dirty pairtree label for metadata matching

    # Create a dictionary with entries for all possible conditions, initially set negative.
    symptoms = ["weakconfirmation", "weakdenial", "strongconfirmation", "strongdenial", "modelagrees", "modeldisagrees"]
    # The first four of these symptoms reflect metadata extracted from the MARC record. Weakconfirmation and
    # weakdenial are based on flags extracted from controlfield 008 which I find are not very reliable as guides.
    # Strongconfirmation and strongdenial are based on strings extracted from other fields that are more
    # specific and reliable as indications of genre. Modelagrees and modeldisagrees reflect the alignment of
    # page-level predictions with an earlier volume-level model of the corpus.

    confirmations = dict()
    for symptom in symptoms:
        confirmations[symptom] = 0

    genrecounts, maxgenre = sequence_to_counts(genresequence)

    if htid not in rowindices and htid not in modelindices:
        return confirmations

    if htid in rowindices:

        genrestring = metadata["genres"][htid]
        genreinfo = genrestring.split(";")
        # It's a semicolon-delimited list of items.

        for info in genreinfo:

            # if info == "biog?" and maxgenre == "non":
            #     confirmations["weakconfirmation"] = 1
            # if info == "biog?" and maxgenre != "non":
            #     confirmations["weakdenial"] = 1

            if info == "Not fiction" and maxgenre == "non":
                confirmations["weakconfirmation"] = 1
            if info == "Not fiction" and maxgenre == "fic":
                confirmations["weakdenial"] = 1

            if (info == "Fiction" or info == "Novel") and maxgenre == "fic":
                confirmations["strongconfirmation"] = 1
            if (info == "Fiction" or info == "Novel") and maxgenre != "fic":
                confirmations["strongdenial"] = 1

            if info == "Biography" and maxgenre == "non":
                confirmations["strongconfirmation"] = 1
            if info == "Biography" and maxgenre != "non":
                confirmations["strongdenial"] = 1

            if info == "Autobiography" and maxgenre == "non":
                confirmations["strongconfirmation"] = 1
            if info == "Autobiography" and maxgenre != "non":
                confirmations["strongdenial"] = 1

            if (info == "Poetry" or info == "Poems") and maxgenre == "poe":
                confirmations["strongconfirmation"] = 1
            if (info == "Poetry" or info == "Poems") and maxgenre != "poe":
                confirmations["strongdenial"] = 1

            if (info == "Drama" or info == "Tragedies" or info == "Comedies") and maxgenre == "dra":
                confirmations["strongconfirmation"] = 1
            if (info == "Drama" or info == "Tragedies" or info == "Comedies") and maxgenre != "dra":
                confirmations["strongdenial"] = 1

            if (info == "Catalog" or info == "Dictionary" or info=="Bibliographies") and maxgenre == "non":
                confirmations["strongconfirmation"] = 1
                couldbefiction = False
            if (info == "Catalog" or info == "Dictionary" or info=="Bibliographies") and maxgenre != "non":
                confirmations["strongdenial"] = 1
    else:
        print("Skipped.")

    if htid in modelindices:

        modelpredictions = dict()
        for genre, genrecolumn in modeldata.items():
            if not genre in options:
                # this column is not a genre!
                continue
            modelpredictions[genre] = float(genrecolumn[htid])
        predictionlist = utils.sortkeysbyvalue(modelpredictions, whethertoreverse = True)
        modelprediction = predictionlist[0][1]
        modelconfidence = predictionlist[0][0]
        nextclosest = predictionlist[1][0]
        # Take the top prediction.

        # For purposes of this routine, treat biography as nonfiction:
        if modelprediction == "bio":
            modelprediction = "non"

        if maxgenre == modelprediction:
            confirmations["modelagrees"] = 1 ## modelconfidence - nextclosest
            confirmations["modeldisagrees"] = 0
        if maxgenre != modelprediction:
            ## divergence = modelconfidence - modelpredictions[maxgenre]
            confirmations["modeldisagrees"] = 1
            confirmations["modelagrees"] = 0
            ## print(maxgenre + " ≠ " + modelprediction)
    else:
        confirmations["modelagrees"] = 0
        confirmations["modeldisagrees"] = 0
        modelprediction = "unknown"

    return confirmations


