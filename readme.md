genre
=====

Public repository for documents and code associated with the project "Understanding Genre in a Collection of a Million Volumes," supported by the NEH and ACLS 2013-15.

Some of these materials were originally developed in other repositories, so you'll find that subfolders often include links pointing back to the original repo. This should only matter if you want to trace development history.

The general workflow here moves from browser -> munging-> features -> pages -> confidencefilter

browser
-------
A GUI we used to create page-level training data. Saves data in ARFF format. Written by Michael L. Black and Boris Capitanu.

confidencefilter
----------------
After page-level predictions had been produced by the code in /pages, we used these Python scripts to train models of their likely accuracy at a volume level, and used those confidence metrics to filter datasets of drama, fiction, and poetry. 

features
--------
Python code that extracted features at a page level.

munging
-------
This helpfully-named repository contains a range of Python scripts we used, for instance, to select features or to evaluate the performance of cross-validated models.

pages
-----
Contains the Java code actually used for page-level classification.

report
------
Contains the LaTeX files for the interim project report.

utilities
---------
Contains Python utilities for extracting tabular metadata from MARCxml, aligning page-level predictions with pages in a HathiTrust zip file, and so on. Note that the pagealigner utility is still under construction; check back in three days.
