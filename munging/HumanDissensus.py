# HumanDissensus.py
# Takes training data produced by five different people and
# measures their agreement with the consensus model that was
# produced by collating their data.

from zipfile import ZipFile
import sys, os
import SonicScrewdriver as utils

rootpath = "/Users/tunder/Dropbox/pagedata/deprecated/"
folderlist = ["Jonathan", "Lea", "Nicole", "Shawn", "Ted"]

def addgenre(agenre, thedictionary):
	if agenre in thedictionary:
		thedictionary[agenre] += 1
	else:
		thedictionary[agenre] = 1

	return thedictionary

# translator = {'subsc' : 'front', 'argum': 'non', 'pref': 'non', 'aut': 'non', 'bio': 'non', 'toc': 'front', 'title': 'front', 'bookp': 'front', 'bibli': 'back', 'gloss': 'back', 'epi': 'fic', 'errat': 'non', 'notes': 'non', 'ora': 'non', 'let': 'non', 'trv': 'non', 'lyr': 'poe', 'nar': 'poe', 'vdr': 'dra', 'pdr': 'dra', 'clo': 'dra', 'impri': 'front', 'libra': 'back', 'index': 'back'}

translator = {'subsc' : 'front', 'argum': 'non', 'pref': 'non', 'aut': 'non', 'bio': 'non', 'toc': 'front', 'title': 'front', 'bookp': 'front', 'bibli': 'back', 'gloss': 'back', 'epi': 'fic', 'errat': 'non', 'notes': 'non', 'ora': 'non', 'let': 'non', 'trv': 'non', 'lyr': 'poe', 'nar': 'poe', 'vdr': 'dra', 'pdr': 'dra', 'clo': 'dra', 'impri': 'front', 'libra': 'back', 'index': 'back'}

secondtranslate = {'front': 'paratext', 'back': 'paratext', 'ads': 'paratext'}

def translate(agenre):
	global translator

	if agenre in translator:
		agenre = translator[agenre]

	return agenre

def effectively_equal(genreA, genreB):
	global secondtranslate

	if genreA in secondtranslate:
		genreA = secondtranslate[genreA]

	if genreB in secondtranslate:
		genreB = secondtranslate[genreB]

	if genreA == genreB:
		return True
	else:
		return False



genrecounts = dict()

volumesread = dict()

for folder in folderlist:
	thispath = os.path.join(rootpath, folder)
	filelist = os.listdir(thispath)
	for afile in filelist:
		if afile.endswith("maps.zip"):
			filepath = os.path.join(thispath, afile)
			with ZipFile(filepath, mode='r') as zf:
				for member in zf.infolist():

					if not member.filename.endswith('/') and not member.filename.endswith("_Store") and not member.filename.startswith("_"):
						datafile = ZipFile.open(zf, name=member, mode='r')
						filelines = datafile.readlines()
						filelines[0] = filelines[0].rstrip()
						htid = filelines[0].decode(encoding="UTF-8")
						thismap = list()
						counter = 0
						for line in filelines[1:]:
							line = line.decode(encoding="UTF-8")
							line = line.rstrip()
							fields = line.split("\t")
							if int(fields[0]) != counter:
								print("error\a")
							counter += 1
							thisgenre = fields[1]
							thismap.append(thisgenre)
							generalized = translate(thisgenre)
							genrecounts = addgenre(generalized, genrecounts)


						if htid in volumesread:
							volumesread[htid].append((folder,thismap))
							# Note that we append a twotuple, of which the first element is the folder string
							# and the second, the map itself. We will use the folder ID to give preference to
							# ratings by me (Ted).
						else:
							volumesread[htid] = [(folder, thismap)]

def comparelists(firstmap, secondmap, genremistakes, correctbygenre, wordcounts):
	if len(firstmap) > len(secondmap):
		length = len(secondmap)
	elif len(firstmap) == len(secondmap):
		length = len(firstmap)
	else:
		print("Error, Will Robinson. There are occasions where the consensus version is shorter but no valid reason for it to be longer.")

	divergence = 0.0

	for i in range(length):

		generalizedfirst = translate(firstmap[i])
		generalizedsecond = translate(secondmap[i])

		if effectively_equal(generalizedfirst, generalizedsecond):
			utils.addtodict(generalizedsecond, wordcounts[i], correctbygenre)
		else:
			divergence += wordcounts[i]
			utils.addtodict((generalizedsecond, generalizedfirst), wordcounts[i], genremistakes)

	return divergence

genremistakes = dict()
correctbygenre = dict()
volumepercents = dict()
overallcomparisons = 0
overallagreement = 0

countwords = True

if countwords:
	filewordcounts = dict()
	with open("/Users/tunder/Dropbox/pagedata/pagelevelwordcounts.tsv", mode="r", encoding="utf-8") as f:
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

badvols = ["njp.32101072911116", "nyp.33433069339749", "hvd.hwjsgk"]
consensuspath = "/Users/tunder/Dropbox/pagedata/mixedtraining/genremaps/"
consensusversions = dict()

ficcounter = 0

for htid, listoftuples in volumesread.items():
	if htid in badvols:
		continue
	filepath = consensuspath + htid + ".map"

	try:
		with open(filepath, mode="r", encoding="utf-8") as f:
			filelines = f.readlines()
	except:
		continue

	thismap = list()
	for line in filelines:
		line = line.rstrip()
		fields = line.split("\t")
		genre = translate(fields[1])
		thismap.append(genre)
		if genre == "fic":
			ficcounter += 1

	consensusversions[htid] = thismap

for key, listoftuples in volumesread.items():

	htid = key
	if htid in badvols:
		continue

	truegenres = consensusversions[htid]

	nummaps = len(listoftuples)

	lengthofvolume = len(listoftuples[0][1])

	if nummaps == 1:
		continue

	# We don't check agreement when there was only one rater, because
	# it's a foregone conclusion that one person will agree with herself.

	if countwords:
		wordcounts = [x[1] for x in filewordcounts[htid]]
	else:
		wordcounts = [1] * lengthofvolume

	potentialcomparisons = nummaps * sum(wordcounts)
	totaldivergence = 0

	for reading in listoftuples:
		readera = reading[0]
		predictedgenres = reading[1]

		divergence = comparelists(predictedgenres, truegenres, genremistakes, correctbygenre, wordcounts)
		totaldivergence += divergence

	agreement = (potentialcomparisons - totaldivergence)
	agreementpercent = agreement / potentialcomparisons
	volumepercents[htid] = agreementpercent
	overallcomparisons += potentialcomparisons
	overallagreement += agreement

print("Average human agreement: " + str(overallagreement / overallcomparisons))

with open("/Users/tunder/Dropbox/pagedata/interrater/HumanDissensus.tsv", mode="w", encoding = "utf-8") as f:
	f.write("htid\tagreement\n")
	for key, value in volumepercents.items():
		outline = utils.pairtreelabel(key) + "\t" + str(value) + "\n"
		f.write(outline)

import ConfusionMatrix
ConfusionMatrix.confusion_matrix(correctbygenre, genremistakes)







