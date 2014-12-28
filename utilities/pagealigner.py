# pagealigner.py

# Utility module for aligning page-level metadata predictions
# produced at the University of Illinois with HathiTrust data,
# in the form of pages concatenated in a .zip file, or
# (eventually) a file of extracted page-level features.

# This is version 1.0 of pagealigner,
# written in Python 3.3.5, in December 2014.

# It's designed to work with the first release of
# page-level metadata:
#   Ted Underwood. Page-level genre metadata for English-language 
#   volumes in HathiTrust, 1700-1922. 
#   http://dx.doi.org/10.6084/m9.figshare.1279201, December 2014.

from zipfile import ZipFile
import tarfile, json

def read_zip(filepath):
    ''' Given a path to a HathiTrust zip file, this returns
    a list of pages, each of which is a list of lines, plus
    a successflag that can be used to interpret error
    conditions. Anything other than
         successflag == 'success' is an error condition.
    '''
    pagelist = list()
    try:
        with ZipFile(file = filepath, mode='r') as zf:
            for member in zf.infolist():
                pathparts = member.filename.split("/")
                suffix = pathparts[1]
                if "_" in suffix:
                    segments = suffix.split("_")
                    page = segments[-1][0:-4]
                else:
                    page = suffix[0:-4]

                if len(page) > 0 and page[0].isdigit():
                    numericpage = True
                else:
                    numericpage = False

                if not member.filename.endswith('/') and not member.filename.endswith("_Store") and not member.filename.startswith("_") and numericpage:
                    datafile = zf.open(member, mode='r')
                    linelist = [x.decode(encoding="UTF-8") for x in datafile.readlines()]
                    pagelist.append((page, linelist))

        pagelist.sort()
        pagecount = len(pagelist)
        if pagecount > 0:
            successflag = "success"
            pagelist = [x[1] for x in pagelist]
        else:
            successflag = "missing file"

    except IOError as e:
        successflag = "missing file"
    except UnicodeError as e:
        successflag = "unicode error"

    return pagelist, successflag

def get_matching_predictions(tarpath, volidstoget):
    '''Given a tarfile containing prediction files and a list of volume IDs,
    this extracts the JSONs corresponding to volids provided, and returns
    a dict where keys are volids and values are dicts encoding the structure
    of the JSON for that volume.

    Note that nothing guarantees we will return all the volids we were 
    assigned to get. They may not all be in this tarfile.'''

    matches = dict()

    tar = tarfile.open(tarpath, 'r:gz')
    # Fix this to make it a with statement.

        for tarinfo in tar:

            name = tarinfo.name()
            pieces = name.split('/')
            thisid = pieces[-1]
            if tarinfo.isreg() and thisid.endswith('.json'):
                thisid = thisid.replace('.json', '')
            else:
                continue
                # This tarinfo object is a directory, or a file like
                # '.DS_Store,' rather than a valid JSON file.

            if thisid in volidstoget:
                tardata = tar.extractfile(tarinfo.name)
                somebytes = tardata.read()
                jsonstring = somebytes.decode('utf-8', 'strict')
                jobj = json.loads(jsonstring)
                matches[thisid] = jobj



        meta = jobj['hathi_metadata']
        date = meta['inferred_date']
        alldates.add(date)
        counts = jobj['added_metadata']['genre_counts']
