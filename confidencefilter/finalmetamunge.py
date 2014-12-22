import csv, os, json
import tarfile
import SonicScrewdriver as utils

rootdir = '/Volumes/TARDIS/maps/'

genrelabels = {'fic': 'fiction', 'dra': 'drama', 'poe': 'poetry'}

genreids = dict()

for genreabbrev, genre in genrelabels.items():
    genreids[genreabbrev] = set()
    branch = genre + '/' + genreabbrev + '_filtered.csv'
    sourcefile = os.path.join(rootdir, branch)

    with open(sourcefile, encoding = 'utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            genreids[genreabbrev].add(row['htid'])

print('We have identified the volume ids that belong to genres.\n')

# Now get
rootcontents = os.listdir(rootdir)
tarpaths = list()

for filename in rootcontents:
    if filename.endswith('.tar.gz'):
        tarpath = os.path.join(rootdir, filename)
        print(tarpath)
        tarpaths.append(tarpath)

def get_period(date):
    if date < 1700:
        return 'uncertaindate'
    elif date < 1800:
        return '1700-99'
    elif date < 1850:
        return '1800-49'
    elif date < 1875:
        return '1850-74'
    elif date < 1900:
        return '1875-99'
    elif date < 1923:
        return '1900-22'
    else:
        return 'none'

outmaps = dict()
outroot = '/Volumes/TARDIS/outmaps/'
periodsubdirs = ['1700-99', '1800-49', '1850-74', '1875-99', '1900-22', 'uncertaindate']
outbranches = ['all', 'drama', 'fiction', 'poetry']

for genre in outbranches:
    outmaps[genre] = dict()
    for subdir in periodsubdirs:
        outpath = outroot + genre + '/' + subdir + '/'
        if not os.path.exists(outpath):
            os.mkdir(outpath)
        outmaps[genre][subdir] = outpath

def writefile(jsonstring, htid, genre, period):
    global outroot, outmaps
    outfolder = outmaps[genre][period]
    outpath = os.path.join(outfolder, htid + '.json')
    with open(outpath, mode = 'w', encoding = 'utf-8') as f:
        f.write(jsonstring)

for pathtotarfile in tarpaths:

    tar = tarfile.open(pathtotarfile, 'r:gz')

    counter = 0
    for tarinfo in tar:
        counter += 1

        if tarinfo.isreg():
            # This is the name of a regular file rather than a directory.

            tardata = tar.extractfile(tarinfo.name)
            somebytes = tardata.read()
            jsonstring = somebytes.decode('utf-8', 'strict')
            jobj = json.loads(jsonstring)

            meta = jobj['hathi_metadata']
            stringdate = meta['inferred_date']
            htid = meta['htid']

            try:
                intdate = int(stringdate)
            except:
                intdate = 0
                print('Anomalous non-numeric date.')
                print(htid)
                continue

            period = get_period(intdate)

            if period == 'none':
                print(htid + " : " + str(stringdate))
                continue
            else:
                genre = 'all'
                writefile(jsonstring, htid, genre, period)

            numgenres = 0
            for genreabbrev, genre in genrelabels.items():
                if htid in genreids[genreabbrev]:
                    writefile(jsonstring, htid, genre, period)
                    numgenres += 1

            if numgenres > 1:
                print(htid + " is in more than one genre.")













