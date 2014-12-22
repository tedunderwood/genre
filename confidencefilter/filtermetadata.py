# filter_metadata

# The story would be neater if this weren't the case, but after our initial pass
# separating genres, it became clear that further filtering was necessary,
# both in terms of raising the underlying threshold and through various kinds of
# manual and semi-manual deselection. So:

import csv

rootpath = '/Volumes/TARDIS/maps/'
# rootpath = '/Users/tunder/maps/'
genrenames = {'dra': 'drama', 'fic': 'fiction', 'poe': 'poetry'}
genrethresholds = {'dra': .66, 'fic': .50, 'poe': .70}
genreforbids = {'fic': ['dictionary', 'bible', 'cyclopedia', 'congressional globe', 'report of', 'reports of'], 'poe': ['dictionary', 'lexicon', 'concordance', 'catalogue', 'bibliography', 'index'], 'dra': ['dictionary', 'lexicon', 'concordance', 'proceedings', 'report of', 'contested election', 'charges against', 'catalogue', 'register']}

genreabbrev = input('genre? ')
genre = genrenames[genreabbrev]
threshold = genrethresholds[genreabbrev]
forbids = genreforbids[genreabbrev]

inpath = rootpath + genre + '/' + genreabbrev + '_edited4.csv'
outpath = rootpath + genre + '/' + genreabbrev + '_filtered.csv'

with open(inpath, encoding = 'latin-1') as f:
    reader = csv.reader(f)
    header = list()
    columns = dict()
    filtered = list()

    for row in reader:

        if len(header) < 1:
            # This is the first row in the file; we use it to create a
            # column index.
            header = row
            for idx, colhead in enumerate(header):
                columns[colhead] = idx

        else:
            accuracy = float(row[columns['prob80acc']])
            if accuracy < threshold:
                continue

            discard = row[columns['discard']]
            if discard == 'x':
                print('REMOVE: ' + row[columns['htid']])
                continue

            title = row[columns['title']].lower()
            title = title.replace(',', ' ')
            title = title.replace('.', ' ')
            forbidden = False
            for forbid in forbids:
                if forbid in title:
                    forbidden = True

            if genre == 'fiction' and ('memoirs' in title or 'autobiography' in title):
                author = row[columns['author']].lower()
                author = author.replace(',', '')
                authwords = set(author.split())
                titlewords = set(title.split())
                for word in authwords:
                    if len(word) > 4 and word in titlewords and not 'novels' in titlewords:
                        print(author + ' : ' + title)
                        forbidden = True

            if forbidden:
                print(title)
                continue
            else:
                filtered.append(row)

with open(outpath, mode = 'w', encoding = 'utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(header)
    for row in filtered:
        writer.writerow(row)








