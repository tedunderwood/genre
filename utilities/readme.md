utilities
=========

At the moment both of the following utilities are written in **Python 3.3.** I hope to create a Python 2.7 version of pagealigner in the early days of 2015.

xmlparser.py
------------
Extracts tabular metadata from collections of MARCxml in the format typically provided by HathiTrust.

pagealigner.py
--------------
Aligns the pages in HathiTrust data with the page-level genre predictions in our JSON files.

It can work with the zip files provided by HathiTrust, which bundle a bunch of page files in a single zip object, or with the [extracted feature files provided by HathiTrust Research Center.](https://sandbox.htrc.illinois.edu/HTRC-UI-Portal2/Features)

You can use the **Alignment** class in this module
to create a generator that will
align a whole list of volumes with genre predictions,
yielding them back to you one by one. Because Alignment returns a
generator, you can use it to iterate across a large set
of volumes without any fear that you'll bust through
memory limitations by loading them all at once.

Each iteration of the generator returns a three-tuple

(volumeid, successflag, volume).

The successflag is there to tell you what went wrong, if something
does go wrong. For instance it could say "missing data file" or
"missing genre prediction" or "mismatched lengths." Volume IDs should
be returned to you in the same order as they were originally provided,
but volumeid is also returned for confirmation.

The volume is represented as a list of pages.

Each page, in turn, is a twotuple, where the first element
in the tuple is the page text (in whatever format the datafile holds)
and the second is a genre code.

See the report ["Understanding Genre in a Collection of a Million Volumes"](http://figshare.com/articles/Understanding_Genre_in_a_Collection_of_a_Million_Volumes_Interim_Report/1281251) for explanations of the genre codes.

**USAGE:**
This module is not designed to be run as a main script.
The Alignment class is designed to be imported into another script
where it can be iterated across.

You create an Alignment between volume data and genre predictions
by giving the Alignment object a list of volume ids to get. These
are not filenames; they're just the HathiTrust volume ID. For instance,
nc01.ark+=13960=t0vq3rs0b.

These should match the filenames of the data files holding pages for
each volume. If you're using extracted feature files from HathiTrust Research Center, that should pose no problem.If you're using zip files from HathiTrust, you may need a bit of munging, since HathiTrust sometimes doesn't prefix the namespace to the filename of the zip file.

If your genre predictions are untarred (suffix '.json') and in a subfolder (relative to
your main script) called /genrepredictions, and your
data files are HathiTrust Research Center feature files (untarred but not decompressed, suffix '.json.bz2')
in a subfolder called
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
alignedvols.

For instance,

    alignedvols = Alignment(listofvols, genrepath = '/root/genretarfiles/', datapath = '/root/hathi/', 
        datatype = 'ziptext', tarscompressed = True)

looks for genre and data files in the specified folders, and assumes the genre
predictions are still packed up as .tar.gz files, and expects the data files
to be HathiTrust zip files rather than HTRC extracted-feature files.

workassembler
-------------
Pay no attention to the scripts behind this curtain. They include header and argumentparser. These are parts of a thing that is still under development.

