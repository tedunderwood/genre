# ConfusionMatrix.py

import numpy
import pandas

def confusion_matrix(correctbygenre, errorsbygenre):
	''' Function turns the dictionaries produced by
	Evaluate.py into a confusion matrix.'''

	genrelist = ["front", "ads", "back"]
	for genre in genrelist:
		if genre not in correctbygenre:
			correctbygenre[genre] = 0

	for genre, count in correctbygenre.items():
		if genre not in genrelist:
			genrelist.append(genre)

	assignto = dict()
	for genre in genrelist:
		assignto[genre] = genre

	assignto["bio"] = "non"
	assignto["front"] = "paratext"
	assignto["back"] = "paratext"
	assignto["ads"] = "paratext"

	predicted = dict()
	for genre in assignto.values():
		newcolumn = dict()
		predicted[genre] = newcolumn
		for genreb in assignto.values():
			predicted[genre][genreb] = 0

	for predictedgenre in genrelist:
		assignpredicted = assignto[predictedgenre]
		for truegenre in genrelist:
			assigntrue = assignto[truegenre]
			if truegenre==predictedgenre:
				predicted[assignpredicted][assigntrue] += correctbygenre[truegenre]
			else:
				tuplekey = (truegenre, predictedgenre)
				if tuplekey in errorsbygenre:
					predicted[assignpredicted][assigntrue] += errorsbygenre[tuplekey]
				else:
					pass

	confusion = pandas.DataFrame(predicted)

	newrow = dict()
	newcolumn = list()
	columngenres = [x for x in confusion.columns]

	accurate = 0
	total = 0
	for genre in columngenres:
		accurate += confusion.loc[genre, genre]
		total += confusion[genre].sum()

	print()
	print("Microaveraged: " + str(accurate/total))


	for genre in columngenres:
		precision = (confusion.loc[genre, genre] / confusion[genre].sum()) * 100
		newrow[genre] = {'precision': precision}
		recall = (confusion.loc[genre, genre] / confusion.loc[genre].sum()) * 100
		newcolumn.append(recall)
	newcolumn.append(0)
	# Because we're about to add a row.
	confusion = confusion.append(pandas.DataFrame(newrow))
	confusion["recall"] = newcolumn
	pandas.options.display.float_format = '{:10.1f}'.format
	print()
	print(confusion)

	for genre in columngenres:
		precision = confusion.loc["precision", genre]
		recall = confusion.loc[genre, "recall"]
		F1 = 2 * ((precision * recall) / (precision + recall))
		print(genre + " \tF1 = " + str(F1))


def print_pandaframe(pandaframe):
	return pandaframe

