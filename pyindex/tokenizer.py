import re

import spacy
from tqdm import tqdm

# Compiled regex for tokenizer
HTML_TAGS = re.compile(r"</*\w+>", re.IGNORECASE)
PUNCT_START_END = re.compile(r"^\W+|\W+$")
PUNCT_ANYWHERE = re.compile(r"\W")
NON_ALPHANUMERIC = re.compile(r"[^a-zA-Z0-9\-\'\.]")
ONLY_NUMBERS = re.compile(r"^(\d\W*)+$")

# spacy English model
NLP = spacy.load('en_core_web_sm', disable=['ner', 'parser'])

def pre_process(line):
    """Pre-processes a line by stripping HTML tags and removing punctuation except dots (.),
    hyphens and apostrophes."""
    tokens = HTML_TAGS.sub('', line).split(' ')
    return ' '.join([PUNCT_START_END.sub('', word) for word in tokens])


def post_process(token):
    """Processes a token by removing punctuation, usually dots and apostrophes. Retains hyphens if
    needed."""
    token = PUNCT_START_END.sub('', token)
    token = ONLY_NUMBERS.sub('', token).strip()
    results = PUNCT_ANYWHERE.split(token)
    # filter
    for t in results:
        if len(t) > 1:
            yield t
        elif t == 'PRON':
            continue
        else:
            continue

def lemmatize(line, doc_id):
    """Lemmatize and tokenize a line found in some document."""
    doc = NLP(line)
    for token in doc:
        for el in post_process(token.lemma_):
            yield (el, doc_id)


def token_stream(files):
    """Generates a stream of tokens from a list of files."""
    for doc_id, file in tqdm(enumerate(files)):
        with open(file, 'r') as fp:
            # lines = [x.decode('utf-8') for x in fp.readlines()] # python 2.7 unicode issue
            lines = fp.readlines()  # python 3.7
            for line in lines:
                line = pre_process(line)
                for term_doc in lemmatize(line, doc_id):
                    yield term_doc

if __name__ == '__main__':
    import glob
    res = list(token_stream(glob.glob('../../tokenizer/Cranfield/*')))
    raw_lemmas = sorted(set([el[0] for el in res]))
    processed = sorted(set([el[1] for el in res]))
    with open('lemmas.txt', 'w') as fp:
        for el in raw_lemmas:
            fp.write(el)
            fp.write('\n')
    with open('processed.txt', 'w') as fp:
        for el in processed:
            fp.write(el)
            fp.write('\n')