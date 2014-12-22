browser
=======

A page-level browser that allows a user to page through text files (delimited with <pb> for pagebreaks) recording genre codes for each page. The initial version was written by Michael L. Black; the GUI was perfected by Boris Capitanu.

It requires a list of allowable genre codes (ours is provided here as codes.ini.) It saves the page-level tags produced in ARFF format (compatible with Weka).

The .jar file here contains the browser in an executable form.

**/backend** and **/gui** are folders holding the source code. But if you really want to mess with the source code, you're probably better off getting it from [the repo where it was actually developed.](https://github.com/tedunderwood/pagetagger)

The cool thing about this browser is that it allows users to do rapid page-flipping in some situations. For instance, in certain books it becomes clear around page ten that the next five hundred pages are all going to belong to the same genre (fiction, nonfiction, or what have you). So the page tagger includes a ``scan forward'' feature that advances rapidly through a page range, stopping only if it encounters a significant change in page format. (We assessed this by comparing the percentage of lines with initial capitals in the next page to pages previously scanned. In practice, this tends to catch major unexpected shifts, e.g. from prose to poetry or paratext.))
