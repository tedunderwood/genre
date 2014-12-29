features
========

Python scripts we used to count "features" at a page level, first for training models, and then when we were applying the models to 854,476 volumes.

I don't promise that the code here is very modular and well-designed. Parts of this were written before I had fully grasped the logic of object-oriented programming in Python, so there are sections that are more procedural than they ought to be. Also, much of this workflow is shaped by particular hardware affordances. So I really don't promise that this part of the project is very portable at all. I intend to refactor it and make it more so.

The central module here is **MultiNormalizeOCR.py**. You would think it would be called something like "ExtractFeatures," but the code history here is (I'm afraid) a bit baroque. Much of this code is borrowed from a workflow in the /DataMunging repo that I have used to correct OCR. The process of OCR correction and the process of feature extraction are similar in a lot of ways: you load volumes from a pairtree file structure, concatenate the pages, and then iterate through a token stream while looking ahead and back to do things like recognize phrases. So, in short, my OCR code has sort of ... evolved ... to become feature-extraction code. If you really want to understand the code history, you could compare /OCRNormalizer0.1 and /pagefeatures [in the /DataMunging repo.](https://github.com/tedunderwood/DataMunging). But I don't recommend it.

**MultiNormalizeOCR** uses the multiprocessing module to parallelize the workflow at the volume level. That might not provide much speedup on a desktop machine, since the process may end up being limited by I/O anyway. But we ran this on a cluster with a very fast file management system; I don't fully understand the details of implementation, but I/O seems to be effectively parallel, and you do in practice get a lot of speedup from multiprocessing even when your processes are all performing reads and writes.

*NormalizeOCR.py** is an older version before I parallelized it. But note that it may also differ in other ways; before using this module you would want to run a diff.

**NormalizeVolume.py** does most of the heavy lifting for a particular volume. When the module is first imported it loads a bunch of rulesets (from /rulesets) that will be used later. The key functions are as_stream and correct_stream. The first converts a list of pages to a stream of tokens, while counting structural features (like the longest sequence of capitalized lines in alphabetical order). The second function takes a stream of tokens and counts words, while converting certain categories of tokens (like arabic numbers) to features that represent a category rather than the literal character sequence '1812.'

**Volume.py** is an older version of that module; I don't think it was actually used in any way in this workflow. Why is it here at all? I'm afraid my versioning habits have not yet fully acclimated to the 21st century.

**Context.py** is a specialized version of **NormalizeVolume** that gets used for volumes before 1820 with long-S problems. In these situations there are lots of words like "fame" that could be OCR errors ("same") or legitimate words. So we do some contextual correction using two-gram probabilities (from /rulesets.)

**HeaderFinder** finds running headers and reports them, without removing them from the original pages. Our goal here is to upvote certain words if they occur in running headers. But in practice we only do this for the words listed as meaningfulheaders in **MultiNormalizeOCR**. (meaningfulheaders = {"index", "introduction", "introductory", "preface", "contents", "glossary", "notes", "poems", "ode", "stanzas", "catalog", "books", "volumes", "tale", "chapter", "canto", "advertisement", "argument", "book", "scene", "act", "comedy", "tragedy", "plays"})

**FileCabinet** and **SonicScrewdriver** are bundles of utility functions, especially used for i/o, especially related to the pairtree file structure. I don't believe **FileUtils** is currently used at all.

Again, sorry for the state of this code; it will get refactored sometime in 2015. Possibly late in 2015.
