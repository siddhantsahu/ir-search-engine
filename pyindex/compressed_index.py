import os
from struct import pack


def gamma(n):
    """Generate gamma code for a number"""
    binary = format(n, 'b')
    offset = binary[1:]
    length = '1' * len(offset) + '0'
    return '{}{}'.format(length, offset)


class Compressed:
    def __init__(self, index):
        self.index = index

    def to_disk(self, directory, k=8):
        """Compress the dictionary using blocking"""
        ptr_file = os.path.join(directory, 'compressed.ptr')
        dictionary_file = os.path.join(directory, 'compressed.dictionary')
        posting_file = os.path.join(directory, 'compressed.postings')
        with open(ptr_file, 'wb') as pointer, open(dictionary_file, 'wb') as dictionary, \
                open(posting_file, 'wb') as posting:
            for ix, (term, val) in enumerate(self.index.inverted_index.items()):
                new_term = '{}{}'.format(len(term), term)
                term_ptr = dictionary.tell()
                dictionary.write(pack('{}s'.format(len(new_term)), new_term.encode('ascii')))
                df = val['df']
                gap = 0  # gap can't be zero
                prev_doc_id = None
                for doc_id, tf in val['postings'].items():
                    if prev_doc_id:
                        gap = doc_id - prev_doc_id
                        # write gap as gamma
                        gamma_code = gamma(gap)
                        posting.write(
                            pack('{}s'.format(len(gamma_code)), gamma_code.encode('ascii')))
                    else:
                        # write doc_id, just for the starting doc_id
                        posting_ptr = posting.tell()
                        posting.write(pack('I', doc_id))
                    posting.write(pack('I', tf))
                pointer.write(pack('I', df))
                pointer.write(pack('I', posting_ptr))
                if ix % k == 0:  # writing every k-th term pointer
                    pointer.write(pack('I', term_ptr))
