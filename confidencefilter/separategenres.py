import tarfile
import json
import os, csv
import SonicScrewdriver as utils

def make_outrow(htid, dirtyhtid, probability, included, columns, table):
    ''' This function creates metadata for a particular vol-genre combo,
    in the form of a list of fields we can give to a csv.writer. Most of the metadata is
    based on an existing metadata file, but there are a few quirks.

    First of all, I'm substituting a clean pairtree id for the 'dirty' one
    that was previously used to index the metadata file. This means that the
    metadata index will be == to the filename, which I see as a big plus.

    I'm also excluding a couple of metadata columns that were not useful or reliable,
    and adding two columns: the probability this volume is more than 80% accurate
    in the genre, and whether it's included in the subset of volumes we've
    selected to create a corpus with greater than 95% precision in this genre.

    Technically a single volume could be included in more than one genre
    corpus. In practice, this is unlikely, because the predominance of a single
    genre across pages is one of the chief clues indicating reliability. So
    while we certainly do mix genre predictions at the page level, we're unlikely
    to include highly mixed volumes in the selected 95-percent-precision corpora.
    If you want them, you'll need to swing a wider net.
    '''

    columns_to_exclude = ['materialtype', 'genres']
    # I'm not repeating these columns, because the first is not useful and the second
    # is not reliable.

    outrow = [htid]
    for column in columns[1:]:
        if column not in columns_to_exclude:
            outrow.append(table[column][dirtyhtid])

    outrow.append(probability)

    if included:
        inclusionstring = 'y'
    else:
        inclusionstring = 'n'
    outrow.append(inclusionstring)

    return outrow


def extractgenres(pathtotarfile, rows, columns, table):
    ''' Given a tarfile containing a bunch of jsons, this goes through all the jsons
    and identifies the ones that belong in filtered subsets for
    fiction, drama, and poetry. The cutoff is 95 percent precision, except for poetry,
    where it's 93.9, because the 95-percent threshold is hard to reach.

    We also write metadata for all jsons where maxgenre is drama, fiction, or poetry,
    including those that didn't reach threshold.
    '''

    fiction = list()
    drama = list()
    poetry = list()

    ficmeta = list()
    drameta = list()
    poemeta = list()

    tar = tarfile.open(pathtotarfile, 'r:gz')

    counter = 0
    for tarinfo in tar:
        counter += 1

        if tarinfo.isreg():
            # This is the name of a regular file rather than a directory.

            tardata = tar.extractfile(tarinfo.name)
            somebytes = tardata.read()
            astring = somebytes.decode('utf-8', 'strict')
            jobj = json.loads(astring)

            meta = jobj['hathi_metadata']
            stringdate = meta['inferred_date']
            htid = meta['htid']
            dirtyhtid = utils.pairtreelabel(htid)
            filename = htid + '.json'

            pathparts = tarinfo.name.split('/')
            if filename != pathparts[1]:
                print(filename)
                print('Is anomalous, because not equal to ' + pathparts[1])

            try:
                intdate = int(stringdate)
            except:
                intdate = 0
                print('Anomalous non-numeric date.')

            if 'drama' in jobj:
                dramadata = jobj['drama']
                precision = dramadata['dra_precision@prob']
                probability = dramadata['prob_dra>80precise']
                if precision >= 0.95:
                    drama.append((intdate, filename, astring))
                    included = True
                else:
                    included = False

                if dirtyhtid in rows:
                    drameta.append(make_outrow(htid, dirtyhtid, probability, included, columns, table))
                else:
                    print('Missing htid: ' + htid)

            if 'fiction' in jobj:
                ficdata = jobj['fiction']
                precision = ficdata['fic_precision@prob']
                probability = ficdata['prob_fic>80precise']
                if precision >= 0.95:
                    fiction.append((intdate, filename, astring))
                    included = True
                else:
                    included = False

                if dirtyhtid in rows:
                    ficmeta.append(make_outrow(htid, dirtyhtid, probability, included, columns, table))
                else:
                    print('Missing htid: ' + htid)

            if 'poetry' in jobj:
                poedata = jobj['poetry']
                precision = poedata['poe_precision@prob']
                probability = poedata['prob_poe>80precise']
                if precision >= 0.939:
                    poetry.append((intdate, filename, astring))
                    included = True
                else:
                    included = False

                if dirtyhtid in rows:
                    poemeta.append(make_outrow(htid, dirtyhtid, probability, included, columns, table))

    tar.close()

    with open('/Volumes/TARDIS/maps/drama/drama_metadata.csv', mode='a', encoding = 'utf-8') as f:
        writer = csv.writer(f)
        for row in drameta:
            writer.writerow(row)

    with open('/Volumes/TARDIS/maps/fiction/fiction_metadata.csv', mode='a', encoding = 'utf-8') as f:
        writer = csv.writer(f)
        for row in ficmeta:
            writer.writerow(row)

    with open('/Volumes/TARDIS/maps/poetry/poetry_metadata.csv', mode='a', encoding = 'utf-8') as f:
        writer = csv.writer(f)
        for row in poemeta:
            writer.writerow(row)

    return drama, fiction, poetry

def sort_jsons(rootpath, jsontriples, suffix):
    paths = list()
    paths.append(os.path.join(rootpath, '18c' + suffix))
    paths.append(os.path.join(rootpath, '19c' + suffix))
    paths.append(os.path.join(rootpath, '20cPre1923' + suffix))

    for apath in paths:
        if os.path.exists(apath) and os.path.isdir(apath):
            pass
        else:
            os.mkdir(apath)

    for date, filename, contents in jsontriples:
        writethis = False

        if date > 1699 and date < 1800:
            outpath = os.path.join(paths[0], filename)
            writethis = True

        elif date >= 1800 and date < 1900:
            outpath = os.path.join(paths[1], filename)
            writethis = True

        elif date >= 1900 and date < 1923:
            outpath = os.path.join(paths[2], filename)
            writethis = True

        if writethis:
            with open(outpath, mode = 'w', encoding = 'utf-8') as f:
                f.write(contents)

rows, columns, table = utils.readtsv('/Volumes/TARDIS/work/metadata/MergedMonographs.tsv')

header = ['htid', 'recordid', 'oclc', 'locnum', 'author', 'imprint', 'datetype', 'startdate', 'enddate', 'imprintdate', 'place', 'enumcron', 'subjects', 'title', 'prob80acc', 'prec>95pct']

with open('/Volumes/TARDIS/maps/drama/drama_metadata.csv', mode='w', encoding = 'utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(header)

with open('/Volumes/TARDIS/maps/fiction/fiction_metadata.csv', mode='w', encoding = 'utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(header)

header[-1] = 'prec>94pct'
# because we didn't quite make it with poetry
with open('/Volumes/TARDIS/maps/poetry/poetry_metadata.csv', mode='w', encoding = 'utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(header)

rootdir = '/Volumes/TARDIS/maps/'
rootcontents = os.listdir(rootdir)

for filename in rootcontents:
    if filename.endswith('.tar.gz'):
        tarpath = os.path.join(rootdir, filename)
        print(tarpath)
        drama, fiction, poetry = extractgenres(tarpath, rows, columns, table)

        sort_jsons('/Volumes/TARDIS/maps/fiction/', fiction, 'fic')
        sort_jsons('/Volumes/TARDIS/maps/poetry/', poetry, 'poe')
        sort_jsons('/Volumes/TARDIS/maps/drama/', drama, 'dra')






