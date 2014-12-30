utilities
=========

**xmlparser.py** extracts tabular metadata from collections of MARCxml in the format typically provided by HathiTrust.

**pagealigner.py** aligns the pages in HathiTrust zip files with the page-level genre predictions in our JSON files.


u can use the Alignment class in this module
to create a generator that will
align a whole list of volumes with genre predictions and then
yield them back to you one by one. Because Alignment returns a
generator, you can use it to iterate across a large set
of volumes without any fear that you'll bust through
memory limitations by loading them all at once.

The generator returns volumes as a list of pages.
Each page, in turn, is a tuple, where the first element
in the tuple is the page text (in whatever format it was provided)
and the second is a genre code.

USAGE:
If your genre predictions are in a subfolder (relative to
your main script) called /genrepredictions, and your
data files are HathiTrust zip files in a subfolder called
/data, this becomes super simple:

    from pagealigner import Alignment
    
    alignedvols = Alignment(listofvolstoget)
    
    for volume in alignedvols:
    
        for page in volume:
    
            text = page[0]
    
            genre = page[1]
    
            if genre == genreyouwant:
    
                do stuff with text

If your genre predictions and data files are in other
folders, or if your data is in a different format, you'll
need to specify more parameters when you create
alignedvols. See the class definition of Alignment.

CAVEAT: Right now, this thing only works with HathiTrust zipfiles, and it also runs a little s-l-o-w-l-y. Working to address both problems (Dec 30, 2014).
