import math
import os
from collections import OrderedDict
from struct import pack, calcsize

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
                self.inverted_index[term] = {'df': 1, 'postings': OrderedDict({doc: 1})}
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
        ptr_file = os.path.join(directory, 'uncompressed.ptr')
        dictionary_file = os.path.join(directory, 'uncompressed.dictionary')
        posting_file = os.path.join(directory, 'uncompressed.postings')

        # largest term in dictionary
        max_length = max([len(x) for x in self.inverted_index.keys()])

        with open(ptr_file, 'wb') as pointer, open(dictionary_file, 'wb') as dictionary, \
                open(posting_file, 'wb') as posting:
            for term, val in self.inverted_index.items():
                term_ptr = dictionary.tell()
                posting_ptr = posting.tell()
                dictionary.write(
                    pack('{}p'.format(max_length), term.encode('ascii')))  # pascal string
                postings_length = len(val['postings'].keys())
                # ideally, postings list would be a power of 2, for uncompressed version
                if postings_length > 1:
                    uncompressed_postings_list_length = math.ceil(math.log2(postings_length))
                else:
                    uncompressed_postings_list_length = 1
                diff = max(postings_length, uncompressed_postings_list_length - postings_length)
                for doc_id, tf in val['postings'].items():
                    posting.write(pack('I', doc_id))
                    posting.write(pack('I', tf))
                # 2 integers for each posting: doc id and term freq
                # we need to write 2*diff number of empty integers 'I'
                # or, 2*diff*size padding, where size = size of I / size of a pad
                if diff:
                    nr_of_paddings = 2 * diff * calcsize('I')
                    posting.write(pack('{}x'.format(nr_of_paddings)))
                pointer.write(pack('I', val['df']))
                pointer.write(pack('I', term_ptr))
                pointer.write(pack('I', posting_ptr))
