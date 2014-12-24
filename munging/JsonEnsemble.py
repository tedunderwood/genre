# Gather models into an ensemble.
# Ensemble.py

import os
import numpy as np
import pandas as pd
from scipy.stats.stats import pearsonr
import SonicScrewdriver as utils
import MetadataCascades as cascades
import Coalescer
from math import log
import statsmodels.api as sm
import json
import ConfusionMatrix
import random

def pairtreelabel(htid):
    ''' Given a clean htid, returns a dirty one that will match
    the metadata table.'''

    if '+' in htid or '=' in htid:
        htid = htid.replace('+',':')
        htid = htid.replace('=','/')

    return htid

def get_ground_truth(filename):
	global groundtruthfiles
	if ".predict" not in filename:
		return ""
	htid = filename[0:-8]

	groundtruthversion = htid + ".map"

	if groundtruthversion not in groundtruthfiles:
		return ""
	else:
		return groundtruthversion

# genretranslations = {'subsc' : 'front', 'argum': 'non', 'pref': 'non', 'aut': 'bio', 'bio': 'bio',
# 'toc': 'front', 'title': 'front', 'bookp': 'front',
# 'bibli': 'back', 'gloss': 'back', 'epi': 'fic', 'errat': 'non', 'notes': 'non', 'ora': 'non',
# 'let': 'non', 'trv': 'non', 'lyr': 'poe', 'nar': 'poe', 'vdr': 'dra', 'pdr': 'dra',
# 'clo': 'dra', 'impri': 'front', 'libra': 'back', 'index': 'back'}

genretranslations = {'subsc' : 'front', 'argum': 'non', 'pref': 'non', 'aut': 'bio', 'bio': 'bio', 'toc': 'front', 'title': 'front', 'bookp': 'front', 'bibli': 'back', 'gloss': 'back', 'epi': 'fic', 'errat': 'non', 'notes': 'non', 'ora': 'non', 'let': 'bio', 'trv': 'non', 'lyr': 'poe', 'nar': 'poe', 'vdr': 'dra', 'pdr': 'dra', 'clo': 'dra', 'impri': 'front', 'libra': 'back', 'index': 'back'}

user = input("Count words (y/n)? ")
if user == "y":
	countwords = True
else:
	countwords = False

tocoalesce = input("Coalesce? ")
if tocoalesce == "y":
	tocoalesce = True
else:
	tocoalesce = False

# infolder = input("Directory of ensemble predictions? ")

# predictroot = "/Volumes/TARDIS/"
# firstdir = predictroot + infolder + "/"

firstdir = "/Users/tunder/Dropbox/pagedata/production/testsetnewpredicts/"
#firstdir = "/Volumes/TARDIS/predicts/"
predictfiles = os.listdir(firstdir)

validfiles = list()

for filename in predictfiles:
	if filename.endswith(".predict"):
		validfiles.append(filename)

user = input("Use main ground truth? ")
if user == "y":
	groundtruthdir = "/Users/tunder/Dropbox/pagedata/thirdfeatures/genremaps/"
	wordcountpath = "/Users/tunder/Dropbox/pagedata/thirdfeatures/pagelevelwordcounts.tsv"
else:
	groundtruthdir = "/Users/tunder/Dropbox/pagedata/testmaps/"
	wordcountpath = "/Users/tunder/Dropbox/pagedata/production/pagelevelwordcounts.tsv"

if countwords:
	filewordcounts = dict()
	with open(wordcountpath, mode="r", encoding="utf-8") as f:
		filelines = f.readlines()

	for line in filelines[1:]:
		line = line.rstrip()
		fields = line.split('\t')
		htid = fields[0]
		pagenum = int(fields[1])
		count = int(fields[2])

		if htid in filewordcounts:
			filewordcounts[htid].append((pagenum, count))
		else:
			filewordcounts[htid] = [(pagenum, count)]

	for key, value in filewordcounts.items():
		value.sort()
		# This just makes sure tuples are sorted in pagenum order.
else:
	filewordcounts = dict()

groundtruthfiles = os.listdir(groundtruthdir)

matchedfilenames = dict()
for filename in validfiles:
	gt = get_ground_truth(filename)
	if gt != "":
		matchedfilenames[filename] = gt

def interpret_probabilities(listoffields):
	probdict = dict()
	for field in listoffields:
		parts = field.split("::")
		genre = parts[0]

		try:
			probability = float(parts[1])
		except:
			probability = 0
			print("Float conversion error!")

		probdict[genre] = probability
	return probdict

def normalize(probdict):
	sumtotal = 0
	listofkeys = list()
	for key, value in probdict.items():
		sumtotal += value
		listofkeys.append(key)
	for key in listofkeys:
		probdict[key] = probdict[key]/sumtotal

	return probdict

def maxkey(dictionary):
	tuplelist = utils.sortkeysbyvalue(dictionary, whethertoreverse = True)
	winner = tuplelist[0][1]
	# if winner == "bio":
	# 	winner = "non"
	return winner

def highest_other_than(dictionary, excludedkey):
	maxval = 0
	maxkey = ""
	for key, value in dictionary.items():
		value = float(value)
		if value > maxval and key != excludedkey:
			maxkey = key
			maxval = value

	return maxkey

consensus = dict()
dissentperfile = dict()
secondthoughts = dict()

for filename in validfiles:
	filepath = firstdir + filename
	with open(filepath, encoding="utf-8") as f:
		fl = f.readlines()
	for line in fl:
		jsonobject = json.loads(line.rstrip())
		if "model" in jsonobject and jsonobject["model"] == "ensemble":
			consensus[filename] = jsonobject["smoothedPredictions"]
			dissentperfile[filename] = jsonobject["dissentLevels"]
			probabilities = jsonobject["probabilities"]
			assert len(probabilities) == len(consensus[filename])
			runnersup = list()
			for i in range(len(probabilities)):
				secondplace = highest_other_than(probabilities[i], consensus[filename][i])
				runnersup.append(secondplace)
			secondthoughts[filename] = runnersup

def genresareequal(truegenre, predictedgenre):
	arethesame = ["bio", "trv", "aut", "non"]
	alsothesame = ["back", "index", "front", "ads"]
	if truegenre == predictedgenre:
		return True
	elif truegenre in arethesame and predictedgenre in arethesame:
		return True
	elif truegenre in alsothesame and predictedgenre in alsothesame:
		return True
	else:
		return False

def compare_two_lists(truelist, predicted, wordsperpage, whethertocountwords):
	global genretranslations
	assert(len(truelist) == len(predicted))

	errorsbygenre = dict()
	correctbygenre = dict()
	accurate = 0
	inaccurate = 0
	totaltruegenre = dict()

	for index, truegenre in enumerate(truelist):
		if truegenre in genretranslations:
			truegenre = genretranslations[truegenre]

		if whethertocountwords:
			increment = wordsperpage[index]
		else:
			increment = 1

		utils.addtodict(truegenre, increment, totaltruegenre)

		predictedgenre = predicted[index]

		if genresareequal(truegenre, predictedgenre):
			utils.addtodict(truegenre, increment, correctbygenre)
			accurate += increment
		else:
			utils.addtodict((truegenre, predictedgenre), increment, errorsbygenre)
			inaccurate += increment

	return totaltruegenre, correctbygenre, errorsbygenre, accurate, inaccurate

def add_dictionary(masterdict, dicttoadd):
	for key, value in dicttoadd.items():
		if key in masterdict:
			masterdict[key] += value
		else:
			masterdict[key] = value
	return masterdict

def nix_a_genre(firstthoughts, genretonix, secondthoughts):
	returnsequence = list()
	assert len(firstthoughts) == len(secondthoughts)

	for i in range(len(firstthoughts)):
		genre = firstthoughts[i]
		if genre == genretonix:
			returnsequence.append(secondthoughts[i])
		else:
			returnsequence.append(genre)

	return returnsequence


def evaluate_filelist(matchedfilenames, excludedhtidlist):
	global consensus, groundtruthdir, filewordcounts

	smoothederrors = dict()
	unsmoothederrors = dict()
	smoothedcorrect = dict()
	unsmoothedcorrect = dict()
	coalescederrors = dict()
	coalescedcorrect = dict()
	totalgt = dict()
	roughaccurate = 0
	roughnotaccurate = 0
	smoothaccurate = 0
	smoothnotaccurate = 0
	coalescedaccurate = 0
	coalescednotaccurate = 0

	# The correct dictionaries pair a genre code (in the original) to a number of times it was correctly
	# identified

	# The error dictionaries map a tuple of (correct code, error code) to a number of times it occurred.

	truesequences = dict()
	predictedsequences = dict()
	accuracies = dict()
	metadatatable = dict()

	symptoms = ["weakconfirmation", "weakdenial", "strongconfirmation", "strongdenial", "modelagrees", "modeldisagrees"]
	for symptom in symptoms:
		metadatatable[symptom] = dict()
	metadatatable["numberofchunks"] = dict()
	# metadatatable["fictonon"] = dict()
	# metadatatable["bio"] = dict()

	for pfile, gtfile in matchedfilenames.items():
		htid = gtfile[0:-4]
		if htid in excludedhtidlist:
			continue

		# The predictionfile has three columns, of which the second
		# is an unsmoothed prediction and the third is smoothed

		smoothlist = consensus[pfile]

		# roughlist = list()
		# detailedprobabilities = list()

		# pfilepath = os.path.join(predictdir, pfile)
		# with open(pfilepath,encoding = "utf-8") as f:
		# 	filelines = f.readlines()

		# for line in filelines:
		# 	line = line.rstrip()
		# 	fields = line.split('\t')
		# 	roughlist.append(fields[1])
		# 	smoothlist.append(fields[2])
		# 	if len(fields) > 5:
		# 		detailedprobabilities.append("\t".join(fields[5:]))

		# 	# The prediction file has this format:
		# 	# pagenumber roughgenre smoothgenre many ... detailed predictions
		# 	# fields 3 and 4 will be predictions for dummy genres "begin" and "end"

		correctlist = list()

		gtfilepath = os.path.join(groundtruthdir, gtfile)
		with open(gtfilepath,encoding = "utf-8") as f:
			filelines = f.readlines()

		for line in filelines:
			line = line.rstrip()
			fields = line.split('\t')
			correctlist.append(fields[1])

		assert len(correctlist) == len(smoothlist)

		if countwords:
			tuplelist = filewordcounts[htid]
			wordsperpage = [x[1] for x in tuplelist]
		else:
			wordsperpage = list()

		# Experiment.
		oldgenre = ""
		transitioncount = 0
		biocount = 0
		for agenre in smoothlist:
			if agenre == "bio":
				biocount += 1
			if oldgenre == "fic" and (agenre == "non" or agenre =="bio"):
				transitioncount += 1
			oldgenre = agenre


		# fictionfilepath = os.path.join(thefictiondir, pfile)
		# poetryfilepath = os.path.join(thepoedir, pfile)

		# mainmodel = cascades.read_probabilities(detailedprobabilities)

		mostlydrapoe, probablybiography, probablyfiction, notdrama, notfiction = cascades.choose_cascade(htid, smoothlist)
		# # This function returns three boolean values which will help us choose a specialized model
		# # to correct current predictions. This scheme is called "cascading classification," thus
		# # we are "choosing a cascade."

		# Make defensive copy
		adjustedlist = [x for x in smoothlist]

		if notdrama:
			adjustedlist = nix_a_genre(adjustedlist, "dra", secondthoughts[pfile])


		if notfiction:
			adjustedlist = nix_a_genre(adjustedlist, "fic", secondthoughts[pfile])

		# if thepoedir != "n" and thefictiondir != "n":

		# 	numberoftrues = sum([mostlydrapoe, probablybiography, probablyfiction])

		# 	if numberoftrues == 1:
		# 		if mostlydrapoe and thepoedir != "n":
		# 			adjustedlist, mainmodel = cascades.drapoe_cascade(adjustedlist, mainmodel, poetryfilepath)
		# 		elif probablybiography:
		# 			adjustedlist = cascades.biography_cascade(adjustedlist)
		# 		elif probablyfiction and thefictiondir != "n":
		# 			adjustedlist, mainmodel = cascades.fiction_cascade(adjustedlist, mainmodel, fictionfilepath)

		if tocoalesce:
			coalescedlist, numberofdistinctsequences = Coalescer.coalesce(adjustedlist)
			# This function simplifies our prediction by looking for cases where a small
			# number of pages in genre X are surrounded by larger numbers of pages in
			# genre Y. This is often an error, and in cases where it's not technically
			# an error it's a scale of variation we usually want to ignore. However,
			# we will also record detailed probabilities for users who *don't* want to
			# ignore these
		else:
			coalescedlist = adjustedlist
			dummy, numberofdistinctsequences = Coalescer.coalesce(adjustedlist)

		metadataconfirmation = cascades.metadata_check(htid, coalescedlist)
		#  Now that we have adjusted

		for key, value in metadataconfirmation.items():
			metadatatable[key][htid] = value
		metadatatable["numberofchunks"][htid] = log(numberofdistinctsequences + 1)
		# metadatatable["fictonon"][htid] = transitioncount
		# metadatatable["bio"][htid] = biocount / len(roughlist)
		# This is significant. We don't want to overpenalize long books, but there is
		# a correlation between the number of predicted genre shifts and inaccuracy.
		# So we take the log.

		totaltruegenre, correctbygenre, errorsbygenre, accurate, inaccurate = compare_two_lists(correctlist, smoothlist, wordsperpage, countwords)
		add_dictionary(smoothederrors, errorsbygenre)
		add_dictionary(smoothedcorrect, correctbygenre)
		add_dictionary(totalgt, totaltruegenre)
		# Only do this for one comparison
		smoothaccurate += accurate
		smoothnotaccurate += inaccurate

		if ("index", "non") in errorsbygenre:
			if errorsbygenre[("index", "non")] > 2:
				print("Index fail: " + htid + " " + str(errorsbygenre[("index", "non")]))

		# totaltruegenre, correctbygenre, errorsbygenre, accurate, inaccurate = compare_two_lists(correctlist, roughlist, wordsperpage, countwords)
		# add_dictionary(unsmoothederrors, errorsbygenre)
		# add_dictionary(unsmoothedcorrect, correctbygenre)
		# roughaccurate += accurate
		# roughnotaccurate += inaccurate

		totaltruegenre, correctbygenre, errorsbygenre, accurate, inaccurate = compare_two_lists(correctlist, coalescedlist, wordsperpage, countwords)
		add_dictionary(coalescederrors, errorsbygenre)
		add_dictionary(coalescedcorrect, correctbygenre)
		coalescedaccurate += accurate
		coalescednotaccurate += inaccurate

		truesequences[gtfile] = correctlist
		predictedsequences[gtfile] = coalescedlist
		thisaccuracy = accurate / (accurate + inaccurate)
		accuracies[htid] = thisaccuracy

	# Now we need to interpret the dictionaries.

	for genre, count in totalgt.items():

		print()
		print(genre.upper() + " : " + str(count))

		if count < 1:
			continue

		print()
		print("SMOOTHED PREDICTION, " + str(count) + " | " + genre)

		print("Correctly identified: " + str(smoothedcorrect.get(genre, 0) / count))
		print("Errors: ")

		for key, errorcount in smoothederrors.items():
			gt, predict = key
			if gt == genre:
				print(predict + ": " + str(errorcount) + "   " + str (errorcount/count))

		print()
		print("COALESCED PREDICTION, " + str(count) + " | " + genre)

		print("Correctly identified: " + str(coalescedcorrect.get(genre, 0) / count))
		print("Errors: ")

		for key, errorcount in coalescederrors.items():
			gt, smoothed = key
			if gt == genre:
				print(smoothed + ": " + str(errorcount) + "   " + str (errorcount/count))

	# roughaccuracy = roughaccurate / (roughaccurate + roughnotaccurate)
	smoothaccuracy = smoothaccurate / (smoothaccurate + smoothnotaccurate)
	coalaccuracy = coalescedaccurate / (coalescedaccurate + coalescednotaccurate)

	confusion = ConfusionMatrix.confusion_matrix(coalescedcorrect, coalescederrors)

	return metadatatable, accuracies, smoothaccuracy, coalaccuracy

metadatatable, accuracies, smoothaccuracy, coalaccuracy = evaluate_filelist(matchedfilenames, list())

print()
# print("ROUGH MICROACCURACY:")
# print(roughaccuracy)
print("SMOOTHED MICROACCURACY:")
print(smoothaccuracy)
print("COALESCED MICROACCURACY:")
print(coalaccuracy)

with open("/Users/tunder/Dropbox/pagedata/interrater/ActualAccuracies.tsv", mode = "w", encoding="utf-8") as f:
	f.write("htid\taccuracy\n")
	for key, value in accuracies.items():
		outline = key + "\t" + str(value) + "\n"
		f.write(outline)

metadatapath = os.path.join(firstdir, "predictionMetadata.tsv")
rowindices, columns, metadata = utils.readtsv(metadatapath)

metadatatable['maxprob']= metadata['maxprob']
metadatatable['gap'] = metadata['gap']
metadatatable['accuracy'] = accuracies
metadatatable['dissent'] = dissentperfile

data = pd.DataFrame(metadatatable, dtype = "float")

data['intercept'] = 1.0
train_cols = data.columns[1:]
logit = sm.Logit(data['accuracy'], data[train_cols])
result = logit.fit()
print(result.summary())
predictions = result.predict(data[train_cols])
print(pearsonr(data['accuracy'], predictions))

# print("Checking logitpredict.")
# import LogisticPredict

# homegrown = LogisticPredict.logitpredict(result.params, data[train_cols])
# print(pearsonr(predictions, homegrown))

# user = input("Dump model to pickle file? (y/n) ")

# if user == "y":
# 	with open("/Volumes/TARDIS/output/models/PredictAccuracy.p", mode = "w+b") as picklefile:
# 		pickle.dump(result, picklefile, protocol = 3)

user = input("Dump model parameters to file? (y/n) ")
if user == "y":
 	result.params.to_csv("/Volumes/TARDIS/output/models/ConfidenceModelParameters.csv")
 	print("Saved to /Volumes/TARDIS/output/models/ConfidenceModelParameters.csv")

idstoexclude = [x for x in data.index[predictions < .9]]

metadatatable, newaccuracies, roughaccuracy, smoothaccuracy, coalaccuracy = evaluate_filelist(matchedfilenames, idstoexclude)

print()
print("ROUGH MICROACCURACY:")
print(roughaccuracy)
print("SMOOTHED MICROACCURACY:")
print(smoothaccuracy)
print("COALESCED MICROACCURACY:")
print(coalaccuracy)

user = input("Continue? ")

for filename, accuracy in accuracies.items():

	print(accuracy, filename)

	truesequence = truesequences[filename]
	predictedsequence = predictedsequences[filename]

	for index, truegenre in enumerate(truesequence):
		print(truegenre + ' \t ' + predictedsequence[index])

	user = input("Continue? ")









