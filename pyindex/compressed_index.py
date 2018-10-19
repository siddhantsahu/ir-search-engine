import os
from struct import pack


def gamma(n):
    """Generate gamma code for a number"""
    binary = format(n, 'b')
    offset = binary[1:]
    length = '1' * len(offset) + '0'
    return '{}{}'.format(length, offset)


def delta(n):
    """Generate delta code for a number"""
    binary = format(n, 'b')
    length = gamma(len(binary))
    offset = binary[1:]
    return '{}{}'.format(length, offset)


def write_string_as_bytes(file, s):
    """Helper function to write a string as bytes to a file"""
    return file.write(pack('{}s'.format(len(s)), s.encode('ascii')))


class Compressed:
    def __init__(self, index):
        self.index = index

    def to_disk(self, directory, k=8, front_coding=False, compression='gamma'):
        """Compress the dictionary using blocking"""
        ptr_file = os.path.join(directory, 'compressed.ptr')
        dictionary_file = os.path.join(directory, 'compressed.dictionary')
        posting_file = os.path.join(directory, 'compressed.postings')

        with open(ptr_file, 'wb') as pointer, open(dictionary_file, 'wb') as dictionary, \
                open(posting_file, 'wb') as posting:

            terms_in_block = []
            for ix, (term, val) in enumerate(self.index.inverted_index.items()):
                term_ptr = dictionary.tell()

                # dictionary
                if not front_coding:
                    new_term = '{}{}'.format(len(term), term)
                    dictionary.write(pack('{}s'.format(len(new_term)), new_term.encode('ascii')))
                else:
                    if ix % k == 0 and terms_in_block:
                        common_prefix = os.path.commonprefix(terms_in_block)
                        if common_prefix:
                            length_plus_prefix = '{}{}'.format(len(terms_in_block[0]),
                                                               common_prefix)
                            write_string_as_bytes(dictionary, length_plus_prefix)
                            write_string_as_bytes(dictionary, '*')
                            for i, t in enumerate(terms_in_block):
                                extra = t[len(common_prefix):]
                                if i == 0:
                                    write_string_as_bytes(dictionary, extra)
                                else:
                                    write_string_as_bytes(dictionary,
                                                          '{}~{}'.format(len(extra), extra))
                        else:  # join all terms and write to file
                            entire_block = ''.join(
                                ['{}{}'.format(len(x), x) for x in terms_in_block])
                            dictionary.write(
                                pack('{}s'.format(len(entire_block)), entire_block.encode('ascii')))
                        terms_in_block = [term]
                    else:
                        terms_in_block.append(term)

                df = val['df']

                # postings
                gap = 0
                prev_doc_id = None
                for doc_id, tf in val['postings'].items():
                    if prev_doc_id:
                        gap = doc_id - prev_doc_id
                        # write gap as gamma
                        if compression == 'gamma':
                            gap_encoded = gamma(gap)
                        else:
                            gap_encoded = delta(gap)
                        posting.write(
                            pack('{}s'.format(len(gap_encoded)), gap_encoded.encode('ascii')))
                    else:
                        # write doc_id, just for the starting doc_id
                        posting_ptr = posting.tell()
                        posting.write(pack('I', doc_id))
                    posting.write(pack('I', tf))

                # write pointers
                pointer.write(pack('I', df))
                pointer.write(pack('I', posting_ptr))
                if ix % k == 0:  # writing every k-th term pointer
                    pointer.write(pack('I', term_ptr))
