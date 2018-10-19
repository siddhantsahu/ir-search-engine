import os
import struct

from nltk.corpus import stopwords
from sortedcontainers import SortedDict

STOPWORDS = frozenset(stopwords.words('english'))


class SPIMI:
    def __init__(self):
        self.doc_info = {}  # to store doc_len and max_tf for docs
        self.inverted_index = SortedDict()

    def _get_max_tf(self, doc_id):
        """Helper function to get max_tf for a doc"""
        return self.doc_info[doc_id]['max_tf']

    def build_index(self, tokens):
        """Builds inverted index using single pass in-memory indexing.

        Args
        ----
        tokens: token stream
        """
        for term_doc_pair in tokens:
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

    def to_disk(self, directory):
        """Writes the dictionary to binary files."""
        ptr_file = os.path.join(directory, 'ptr')
        index_file = os.path.join(directory, 'index')
        with open(ptr_file, 'wb') as pointers, open(index_file, 'wb') as index:
            for term, val in self.inverted_index.items():
                df = val['df']  # unsigned integer, I
                term_ptr = index.tell()  # term pointer
                postings_ptr = index.tell() + index.write(
                    struct.pack('{}s'.format(len(term)), term.encode('ascii')))  # write term
                # write all postings now
                for doc_id, tf in val['postings'].items():  # in increasing order of doc_id
                    index.write(struct.pack('I', doc_id))
                    index.write(struct.pack('I', tf))
                pointers.write(struct.pack('I', df))  # write document frequency
                pointers.write(struct.pack('I', term_ptr))
                pointers.write(struct.pack('I', postings_ptr))