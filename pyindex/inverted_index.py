from nltk.corpus import stopwords
from sortedcontainers import SortedDict

STOPWORDS = frozenset(stopwords.words('english'))


class SPIMI:
    def __init__(self, ts):
        self.tokens = ts
        self.doc_info = {}  # to store doc_len and max_tf for docs
        self.inverted_index = SortedDict()

    def _get_max_tf(self, doc_id):
        """Helper function to get max_tf for a doc"""
        return self.doc_info[doc_id]['max_tf']

    def build_index(self):
        """Builds inverted index using single pass in-memory indexing."""
        for term_doc_pair in self.tokens:
            term, doc = term_doc_pair
            if doc not in self.doc_info:
                self.doc_info[doc] = {'doc_len': 1, 'max_tf': 1}
            else:
                self.doc_info[doc]['doc_len'] += 1
            # discard stopwords
            if term in STOPWORDS:
                continue
            if term not in self.inverted_index:
                # max_tf of doc wouldn't change, so do nothing
                self.inverted_index[term] = {'df': 1, 'postings': SortedDict(
                    {doc: 1})}
            else:
                val = self.inverted_index[term]
                if doc in val['postings']:
                    val['postings'][doc] += 1
                else:
                    val['postings'][doc] = 1
                # update max_tf
                self.doc_info[doc]['max_tf'] = max(val['postings'][doc], self._get_max_tf(doc))
                val['df'] = len(val['postings'].keys())
                self.inverted_index[term] = val
