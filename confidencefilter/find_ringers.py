# find_ringers.py

import csv, os

rootpath = '/Volumes/TARDIS/maps/'
# rootpath = '/Users/tunder/maps/'
genrenames = {'dra': 'drama', 'fic': 'fiction', 'poe': 'poetry'}
genrethresholds = {'dra': .66, 'fic': .50, 'poe': .70}

ringers = dict()
blockset = set()

for abbrev, name in genrenames.items():
    insubset = dict()
    threshold = genrethresholds[abbrev]
    branch = rootpath + name + '/'
    subset = branch + abbrev + '_subset.csv'
    with open(subset, encoding = 'utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            if None in row:
                row.pop(None)
            htid = row['htid']
            prob = row['prob80acc']
            if type(prob).__name__ == 'list':
                prob = prob[0]
                print(prob)
            prob = float(prob)

            if prob < threshold:
                continue
            else:
                insubset[htid] = row
    weird = list()
    ctr = 0
    filter = branch + abbrev + '_filtered.csv'

    with open(filter, encoding = 'utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            ctr += 1
            htid = row['htid']
            blockset.add(htid)
            if htid == 'uc2.ark+=13960=t15m62f6n':
                print("Zowie!")
                if 'uc2.ark+=13960=t15m62f6n' in insubset:
                    print('Double Zowie!')
                    insubset.pop('uc2.ark+=13960=t15m62f6n')
            try:
                del(insubset[htid])
            except:
                weird.append(htid)

            if htid in ringers:
                del(ringers[htid])
                print("Duplication.")

    print(abbrev + "  : "+ str(ctr))


    print(len(insubset))
    for key, value in insubset.items():
        if key not in blockset:
            ringers[key] = value

ringerids = [x for x in ringers.keys()]

fields = ['htid', 'recordid', 'locnum', 'author', 'datetype', 'startdate', 'enddate', 'textdate', 'subjects', 'genres', 'title']

outrows = list()
with open('/Volumes/TARDIS/work/metadata/MergedMonographs.tsv', encoding = 'utf-8') as f:
    for line in f:
        line = line.rstrip()
        x = line.split('\t')
        if x[0] in ringerids:
            row = [x[0], x[1], x[3], x[4], x[6], x[7], x[8], x[9], x[13], x[14], x[15]]
            outrows.append(row)

with open('/Users/tunder/work/genre/confidencefilter/ringers.csv', mode='w', encoding = 'utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(fields)
    for row in outrows:
        writer.writerow(row)





