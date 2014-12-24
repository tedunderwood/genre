# SelectFeatures.py
#
# Selects a set of features for paratext classification, using the logic that
# we want to include most common features from each "genre."
#
# The workflow is thus to collate genremaps with tokencounts, and assign the
# tokens on each page to the appropriate master genre dictionary.
#
# Then we sort the words in each genre dictionary and take the top N words.

import os, sys

genretranslations = {'subsc' : 'front', 'argum': 'non', 'pref': 'non', 'aut': 'bio', 'bio': 'bio',
'toc': 'front', 'title': 'front', 'bookp': 'front',
'bibli': 'back', 'gloss': 'back', 'epi': 'fic', 'errat': 'non', 'notes': 'non', 'ora': 'non',
'let': 'non', 'trv': 'non', 'lyr': 'poe', 'nar': 'poe', 'vdr': 'dra', 'pdr': 'dra',
'clo': 'dra', 'impri': 'front', 'libra': 'back', 'index': 'back'}

def addtodict(word, count, lexicon):
	'''Adds an integer (count) to dictionary (lexicon) under
	the key (word), or increments lexicon[word] if key present. '''

	if word in lexicon:
		lexicon[word] += count
	else:
		lexicon[word] = count

def sortkeysbyvalue(lexicon, whethertoreverse = False):
	'''Accepts a dictionary where keys point to a (presumably numeric) value, and
	returns a list of keys sorted by value.'''

	tuplelist = list()
	for key, value in lexicon.items():
		tuplelist.append((value, key))

	tuplelist = sorted(tuplelist, reverse = whethertoreverse)
	return tuplelist


genremapdir = "/Users/tunder/Dropbox/pagedata/thirdfeatures/genremaps/"
featuredir = "/Users/tunder/Dropbox/pagedata/thirdfeatures/pagefeatures/"

genrefiles = os.listdir(genremapdir)
featurefiles = os.listdir(featuredir)

# Collate the directories and produce a list of the underlying htids.
# Files in the genre directory end with ".map"; those in the feature
# directory end with ".pg.tsv."

htids = list()

for filename in genrefiles:
	if ".map" not in filename:
		continue

	htid = filename[0:-4]

	featureversion = htid + ".pg.tsv"

	if featureversion not in featurefiles:
		print("Missing " + htid)
	else:
		htids.append(htid)

genrelexicons = dict()

for htid in htids:
	genrepages = dict()
	featurepages = dict()
	genrepath = genremapdir + htid + ".map"
	featurepath = featuredir + htid + ".pg.tsv"

	genremaxpage = 0
	featuremaxpage = 0

	with open(genrepath, encoding = "utf-8") as f:
		filelines = f.readlines()

	for line in filelines:
		line = line.rstrip()
		fields = line.split('\t')
		genre = fields[-1]
		page = int(fields[0])
		genrepages[page] = genre

		if page > genremaxpage:
			genremaxpage = page

	with open(featurepath, encoding = "utf-8") as f:
		filelines = f.readlines()

	for line in filelines:
		line = line.rstrip()
		fields = line.split('\t')
		page = int(fields[0])

		twotuple = (fields[1], fields[2])
		if page in featurepages:
			featurepages[page].append(twotuple)
		else:
			featurepages[page] = [twotuple]

		if page > featuremaxpage:
			featuremaxpage = page

	if genremaxpage != featuremaxpage:
		print("Pagination discrepancy in " + htid)
		sys.exit()

	for page in range(genremaxpage):

		genre = genrepages[page]
		if genre in genretranslations:
			genre = genretranslations[genre]
		# The function of this is to fold minor genres together into
		# master categories. The categories created in genretranslations
		# match categories that will be used for classification.

		if genre in genrelexicons:
			lexicon = genrelexicons[genre]
		else:
			lexicon = dict()
			genrelexicons[genre] = lexicon

		featureset = featurepages[page]

		for twotuple in featureset:
			word, count = twotuple
			count = int(count)

			if word.startswith("#"):
				continue
				# These are special structural features not to be included in the
				# feature vocabulary.
			else:
				increment = count
				if increment > 3:
					increment = 3
				addtodict(word, increment, lexicon)
				# Changing this so words only get counted once

mastervocab = dict()

for genre, lexicon in genrelexicons.items():
	sortedwords = sortkeysbyvalue(lexicon, whethertoreverse = True)
	print()
	print(genre)
	maxfeatures = 20
	if len(sortedwords) < maxfeatures:
		maxfeatures = len(sortedwords)

	for i in range(maxfeatures):
		print(sortedwords[i][1])

	maxfeatures = 500
	if len(sortedwords) < maxfeatures:
		maxfeatures = len(sortedwords)

	for i in range(maxfeatures):
		word = sortedwords[i][1]
		count = sortedwords[i][0]

		addtodict(word, count, mastervocab)

tuplelist = sortkeysbyvalue(mastervocab, whethertoreverse = True)
vocabulary = [x[1] for x in tuplelist]
vocabulary = vocabulary[:2000]

wordstoadd = ["index", "glossary", "argument", "biographical", "memoir", "memoirs", "autobiography", "dramatis", "personae", "contents", "table", "arabicprice", "appendix", "preface", "ode", "scene"]

for word in wordstoadd:
	if word not in vocabulary:
		vocabulary.append(word)

with open("/Users/tunder/Dropbox/pagedata/reallybiggestvocabulary.txt", mode="w", encoding="utf-8") as f:
	for word in vocabulary:
		f.write(word + '\n')

print(len(vocabulary))









