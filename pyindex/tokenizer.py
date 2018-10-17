import re

import spacy

# Compiled regex for tokenizer
HTML_TAGS = re.compile(r"</*\w+>", re.IGNORECASE)
PUNCT_START_END = re.compile(r"^\W+|\W+$")
PUNCT_ANYWHERE = re.compile(r"\W")
NON_ALPHANUMERIC = re.compile(r"[^a-zA-Z0-9\-\'\.]")
ONLY_NUMBERS = re.compile(r"^(\d\W*)+$")

# spacy English model
NLP = spacy.load('en_core_web_sm')


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
    token = PUNCT_ANYWHERE.sub('', token)
    if len(token) > 1:
        return token
    elif token == 'PRON':
        raise ValueError('Not a valid token')
    else:
        raise ValueError('Not a valid token')


def lemmatize(line, doc_id):
    """Lemmatize and tokenize a line found in some document."""
    doc = NLP(line)
    for token in doc:
        try:
            yield (post_process(token.lemma_), doc_id)
        except ValueError:
            continue  # discard this token


def token_stream(files):
    """Generates a stream of tokens from a list of files."""
    for doc_id, file in enumerate(files):
        with open(file, 'r') as fp:
            for line in fp:
                line = pre_process(line)
                for term_doc in lemmatize(line, doc_id):
                    yield term_doc
