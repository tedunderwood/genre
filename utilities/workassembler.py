#!/usr/bin/env python3

# workassembler.py

# Written in Python 3.3,
# January 2015, by Ted Underwood.

# Given a list of HathiTrust volume IDs, this script
# finds the volumes, pairs them with page-level
# genre metadata, and extracts only the pages
# matching a specified list of genres.
#
# It can also concatenate multiple volumes
# to produce a single "work," for instance
# if you've got a multi-volume novel.
# Extracting all the fiction pages and
# concatenating multiple volumes should
# (ideally!) give you a continuous text without
# interruption by paratext.
#
# Finally, it can optionally remove
# running headers.
#
# USAGE:
# py3 workassembler.py -rh -o /projects/ichass/usesofscale/code/assemble -genre fic -pairroot /projects/ichass/usesofscale/nonserials/ -workjson 100novels.json
#
# Here py3 is an alias to my installation of python3.4
# You can use python3, but note that the campus
# cluster installation of python3 may lack the bz2 module!

def gethelp():
    print('Allowable command-line options include:')
    print('=======================================')
    print()
    print('-help      Prints this list.')
    print()
    print('-vollist   Followed by the path to a file containing a simple')
    print('           list of HathiTrust volume ids.')
    print()
    print('-workjson  Followed by the path to a file containing a json')
    print('           that groups volumes as "works." If this is missing,')
    print('           a vollist must be provided.')
    print()
    print('-o         Followed by path to output folder. Required.')
    print()
    print('-rh        Remove running headers.')
    print()
    print('-genres    Comma-separated list of genres to get.')
    print('           If missing, returns all pages.')
    print()
    print('-pagemeta  Folder containing page-level genre predictions for the')
    print('           volumes you are assembling.')
    print()
    print('-pairroot  The root of the pairtree that stores files.')
    sys.exit(0)

import os, sys, csv, json
import argumentparser
import header
from pagealigner import Alignment

punctuple = ('-', '.', ',', '?', '!', ';', '"', '“', '”', ':', '--', '—', ')', '(', "'", "`", "[", "]", "{", "}")

with open('MainDictionary.txt', encoding = 'utf-8') as file:
    filelines = file.readlines()
lexicon = dict()
for line in filelines:
    line = line.strip()
    fields = line.split('\t')
    lexicon[fields[0]] = int(fields[2])

def parsevols(filepath):
    with open(filepath, encoding = 'utf-8') as f:
        volumelist = [x.strip() for x in f.readlines()]

    return volumelist

def parsejson(filepath):
    with open(filepath, encoding = 'utf-8') as f:
        jsonstring = f.read()

    jobj = json.loads(jsonstring)

    works = list()
    volumes = list()

    # We assume that this json is an object where keys are
    # work ids pointing to lists of volume ids. Our goal is
    # to produce instead a zipped list where unique volume ids
    # are paired with non-unique work ids.

    for workid, vollist in jobj.items():
        for vol in vollist:
            works.append(workid)
            volumes.append(vol)

    return volumes, [x for x in zip(works, volumes)]

def genrefilter(volume, targetgenres):
    ''' Just filters a list of pages to make sure
    they belong to the target genres.
    '''

    filtered = list()

    for page in volume:
        if page[1] in targetgenres:
            filtered.append(page[0])

    return filtered

def slice_list(input, size):
    # This function totally stolen from Paolo Scardine
    # on http://stackoverflow.com/questions/4119070/how-to-divide-a-list-into-n-equal-parts-python
    input_size = len(input)
    slice_size = input_size // size
    remain = input_size % size
    result = []
    iterator = iter(input)
    for i in range(size):
        result.append([])
        for j in range(slice_size):
            result[i].append(next(iterator))
        if remain:
            result[i].append(next(iterator))
            remain -= 1
    return result

def wordfreq(astring):
    global lexicon

    if astring in lexicon:
        return lexicon[astring]
    else:
        return 0

def strip_punctuation(astring):
    global punctuple
    keepclipping = True

    while keepclipping == True and len(astring) > 1:
        keepclipping = False
        if astring.endswith(punctuple):
            astring = astring[:-1]
            keepclipping = True

    keepclipping = True
    while keepclipping == True and len(astring) > 1:
        keepclipping = False
        if astring.startswith(punctuple):
            astring = astring[1:]
            keepclipping = True

    return astring

def rejoin_hyphens(linelist):
    newlist = list()
    prevline = ""

    for i in range(len(linelist)):
        line = linelist[i].strip()
        if len(line) < 1:
            continue

        line = line.lower()
        line = line.replace('\t', ' ')

        if prevline.endswith('-') or prevline.endswith('—'):
            prevtokens = prevline.split()
            thesetokens = line.split()

            if len(prevtokens) > 0 and len(thesetokens) > 0:

                lastword = strip_punctuation(prevtokens[-1])
                nextword = strip_punctuation(thesetokens[0])

                if ((wordfreq(lastword) + wordfreq(nextword)) / 3) < wordfreq(lastword + nextword):
                    prevtokens.pop(-1)
                    thesetokens.pop(0)
                    prevtokens.append(lastword + nextword)
                    prevline = ' '.join(prevtokens)
                    line = ' '.join(thesetokens)

        newlist.append(prevline)
        prevline = line

    newlist.append(prevline)

    return newlist

def output_work(thiswork, workid, removeheaders, outputmode, outputfolder):

    if removeheaders:
        thiswork, removed = header.remove_headers(thiswork)

    if len(thiswork) > 20:
        # very small works are hard to divide evenly.

        linelist = list()
        for page in thiswork:
            linelist.extend(page)

        linelist = rejoin_hyphens(linelist)

        tenchunks = slice_list(linelist, 10)

        outpath = os.path.join(outputfolder, 'malletsource.txt')

        with open(outpath, mode = 'a', encoding = 'utf-8') as f:

            for idx, chunk in enumerate(tenchunks):
                chunkid = workid + '|' + str(idx)
                outline = chunkid + '\t' + 'null' + '\t' + ' '.join(chunk) + '\n'
                f.write(outline)

def main(argdict):

    if '-help' in argdict:
        gethelp()

    if '-vollist' in argdict:
        idstoget = parsevols(argdict['-vollist'])
        workvolseq = [x for x in zip(idstoget, idstoget)]
    elif '-workjson' in argdict:
        idstoget, workvolseq = parsejson(argdict['-workjson'])
    else:
        print('No list of volumes provided. Quitting.')
        sys.exit(0)

    # idstoget is just a list of volume ids
    #
    # the workvolseq is a list of two-tuples where the first
    # element is a workid and the second is a volumeid
    # there is one of these for each volume, and the sequence
    # must be identical to idstoget
    #
    # In the common case where you just want volumes,
    # these will just be pairs of identical volume IDs.

    if "-genre" in argdict:
        targetgenres = set(argdict["-genre"].split(","))
    elif "-g" in argdict:
        targetgenres = set(argdict["-g"].split(","))
    else:
        targetgenres = {'ads', 'back', 'bio', 'dra', 'fic', 'front', 'non', 'poe'}

    if '-pairroot' in argdict:
        pairtree_root = argdict['-pairroot']
    else:
        print('No data source provided. Quitting')

    if '-o' in argdict:
        outputfolder = argdict['-o']
    else:
        print('No output folder provided. Quitting.')

    # default
    outputmode = 'mallet'

    if '-rh' in argdict:
        removeheaders = True
    else:
        removeheaders = False

    # Proceed to align and filter volumes

    alignedvols = Alignment(idstoget, datapath = pairtree_root, datatype = 'pairtree')

    thiswork = list()
    lastworkid = workvolseq[0][0]
    failedvolumes = list()

    i = 0

    for volid, successflag, volume in alignedvols:
        workid = workvolseq[i][0]
        targetvolid = workvolseq[i][1]
        i += 1

        if successflag != 'success':
            failedvolumes.append((volid, successflag))
            continue

        if volid != targetvolid:
            print('Volume alignment error; fatal.')
            sys.exit(0)

        filteredvol = genrefilter(volume, targetgenres)

        if workid == lastworkid:
            thiswork.extend(filteredvol)

        else:
            output_work(thiswork, lastworkid, removeheaders, outputmode, outputfolder)
            thiswork = list()
            lastworkid = workid
            thiswork.extend(filteredvol)

    with open('failedvolumes.tsv', mode = 'w', encoding = 'utf-8') as f:
        for volume, flag in failedvolumes:
            f.write(volume + '\t' + flag + '\n')

if __name__ == '__main__':

    args = sys.argv
    argdict = argumentparser.simple_parse(args)
    main(argdict)



