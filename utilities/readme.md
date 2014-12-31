utilities
=========

**xmlparser.py** extracts tabular metadata from collections of MARCxml in the format typically provided by HathiTrust.

**pagealigner.py** aligns the pages in HathiTrust zip files with the page-level genre predictions in our JSON files.


You can use the Alignment class in this module
to create a generator that will
align a whole list of volumes with genre predictions and then
yield them back to you one by one. Because Alignment returns a
generator, you can use it to iterate across a large set
of volumes without any fear that you'll bust through
memory limitations by loading them all at once.

The generator returns a three-tiuple (volumeid, successflag, volume).
The successflag is there to tell you what went wrong, if something
goes wrong. For instance it could say "missing data file" or
"missing genre prediction" or "mismatched lengths."

The volume is represented as a list of pages.

Each page, in turn, is a twotuple, where the first element
in the tuple is the page text (in whatever format the datafile holds)
and the second is a genre code.

USAGE:
You create an Alignment between volume data and genre predictions
by giving the Alignment object a list of volume ids to get. These
are not filenames; they're just the HathiTrust volume ID. For instance,
nc01.ark+=13960=t0vq3rs0b.

These should match the filenames of the zipfiles holding pages for
each volume. Since HathiTrust sometimes doesn't prefix the namespace
to the filename of the zip file, this may require a bit of munging.

If your genre predictions are in a subfolder (relative to
your main script) called /genrepredictions, and your
data files are HathiTrust zip files in a subfolder called
/data, you don't need to pass any other parameters to the
Alignment. This becomes super simple:

    from pagealigner import Alignment
    
    alignedvols = Alignment(listofvolstoget)
    
    for volid, successflag, volume in alignedvols:

        if successflag != "success":
           
            print(successflag + " in " + volid)

            continue

        for page in volume:
    
            text = page[0]
    
            genre = page[1]
    
            if genre == genreyouwant:
    
                do stuff with text

If your genre predictions and data files are in other
folders, or if your data is in a different format, you'll
need to specify more parameters when you create
alignedvols. See the class definition of Alignment.

CAVEAT: Right now, this thing only works with HathiTrust zipfiles. Still working to expand it to HTRC feature files.
