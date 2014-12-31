genre
=====

Public repository for documents and code associated with the project "Understanding Genre in a Collection of a Million Volumes," supported by the NEH and ACLS 2013-15.

Some of these materials were originally developed in other repositories, so you'll find that subfolders often include links pointing back to the original repo. This should only matter if you want to trace development history.

The workflow we followed moves from browser -> munging-> features -> pages -> confidencefilter.

But probably most users of this repo will be primarily interested in the [utilities](https://github.com/tedunderwood/genre/tree/master/utilities) subfolder, which contains Python utilities that can be used to align these genre predictions with HTRC feature files or HathiTrust zip files.

For fuller documentation, see [the interim project report.](http://figshare.com/articles/Understanding_Genre_in_a_Collection_of_a_Million_Volumes_Interim_Report/1281251)

browser
-------
A GUI we used to create page-level training data. Saves data in ARFF format. Written by Michael L. Black and Boris Capitanu.

confidencefilter
----------------
After page-level predictions had been produced by the code in /pages, we used these Python scripts to train models of their likely accuracy at a volume level, and used those confidence metrics to filter datasets of drama, fiction, and poetry. 

features
--------
Python code that extracted features at a page level. This really needs to be refactored; it's probably the least portable part of the workflow right now.

munging
-------
This helpfully-named repository contains a range of Python scripts we used, for instance, to select features or to evaluate the performance of cross-validated models.

pages
-----
Contains the Java code actually used for page-level classification.

report
------
Contains the LaTeX files for the interim project report.

[utilities](https://github.com/tedunderwood/genre/tree/master/utilities)
---------
Contains Python utilities for extracting tabular metadata from MARCxml, aligning page-level predictions with pages in a HathiTrust zip file, and so on. A very early working version of the pagealigner utility is now up, but let's call it a very early alpha! Better version on the way within a week.
