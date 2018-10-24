import os
from struct import pack

from utils import gamma, write_string_as_bytes, delta


class Compressed:
    def __init__(self, index):
        self.index = index

    def blocking_gamma(self, directory, k=8):
        """Compress the dictionary using blocking"""
        ptr_file = os.path.join(directory, 'compressed.ptr')
        dictionary_file = os.path.join(directory, 'compressed.dictionary')
        posting_file = os.path.join(directory, 'compressed.postings')

        with open(ptr_file, 'wb') as pointer, open(dictionary_file, 'wb') as dictionary, \
                open(posting_file, 'wb') as posting:
            for ix, (term, val) in enumerate(self.index.inverted_index.items()):
                term_ptr = dictionary.tell()
                # dictionary
                dictionary.write(pack('H', len(term)))
                write_string_as_bytes(dictionary, term)
                df = val['df']
                # postings
                gap = 0
                prev_doc_id = None
                for doc_id, tf in val['postings'].items():
                    if prev_doc_id:
                        gap = doc_id - prev_doc_id
                        gap_encoded = gamma(gap)
                        posting.write(pack('H', gap_encoded))  # short
                    else:
                        # write doc_id, just for the starting doc_id
                        posting_ptr = posting.tell()
                        posting.write(pack('I', doc_id))  # integer
                    posting.write(pack('I', tf))
                # write pointers
                pointer.write(pack('I', df))
                pointer.write(pack('I', posting_ptr))
                if ix % k == 0:  # writing every k-th term pointer
                    pointer.write(pack('I', term_ptr))

    def frontcoding_delta(self, directory, k=8):
        """Compress using front coding and delta codes"""
        ptr_file = os.path.join(directory, 'compressed2.ptr')
        dictionary_file = os.path.join(directory, 'compressed2.dictionary')
        posting_file = os.path.join(directory, 'compressed2.postings')

        blocks = []
        with open(ptr_file, 'wb') as pointer, open(dictionary_file, 'wb') as dictionary, \
                open(posting_file, 'wb') as posting:
            for ix, (term, val) in enumerate(self.index.inverted_index.items()):
                term_ptr = dictionary.tell()
                blocks.append(term)
                if blocks and ix % k == 0:
                    prefix = os.path.commonprefix(blocks)
                    dictionary.write(pack('H', len(blocks[0])))  # write length of first term
                    write_string_as_bytes(dictionary, prefix)
                    write_string_as_bytes(dictionary, '*')
                    write_string_as_bytes(dictionary, blocks[0][len(prefix):])
                    for t in blocks[1:]:
                        dictionary.write(pack('H', len(t) - len(prefix)))  # extra length
                        write_string_as_bytes(dictionary, '|')  # instead of diamond
                        write_string_as_bytes(dictionary, t[len(prefix):])
                    blocks = []
                # postings
                gap = 0
                prev_doc_id = None
                for doc_id, tf in val['postings'].items():
                    if prev_doc_id:
                        gap = doc_id - prev_doc_id
                        gap_encoded = delta(gap)
                        posting.write(pack('H', gap_encoded))  # short
                    else:
                        # write doc_id, just for the starting doc_id
                        posting_ptr = posting.tell()
                        posting.write(pack('I', doc_id))  # integer
                    posting.write(pack('I', tf))
                # write pointers
                pointer.write(pack('I', val['df']))
                pointer.write(pack('I', posting_ptr))
                if ix % k == 0:  # writing every k-th term pointer
                    pointer.write(pack('I', term_ptr))
