#!/usr/bin/env python3

# ArgumentParser

# Takes a list of arguments and returns a dict of command line options
# combined with their values.

# Basically, we assume that commands come either in the form
# -commandoption argument
#
# OR
# -commandoption
#
# In the former case, the dictionary key commandoption gets set to value argument.
# Otherwise the key commandoption just gets set to "true." String, not boolean, to
# avoid type confusion.

def simple_parse(listofargs):
    argdict = dict()

    commandoption = "none"
    for argument in listofargs[1:]:
        # We assume that the first argument in the list, sys.argv[0], is just the module
        # that was invoked.

        if argument.startswith("-") and commandoption == "none":
            commandoption = argument
        elif argument.startswith("-"):
            argdict[commandoption] = "true"
            commandoption = argument
        elif commandoption != "none":
            argdict[commandoption] = argument
            commandoption = "none"
        else:
            print("No command line option provided to govern " + argument + " — ignored.")

    # Since the last option in the list may not have been followed by an argument to 'kick' it in.

    if commandoption != "none":
        argdict[commandoption] = "true"

    return argdict





