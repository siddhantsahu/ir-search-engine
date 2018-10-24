import glob
import pickle
import time

from compressed_index import Compressed
from inverted_index import SPIMI
from tokenizer import token_stream

if __name__ == '__main__':
    tokens = token_stream(glob.glob('../../tokenizer/Cranfield/cranfield00*'))
    try:
        with open('index.pkl', 'rb') as fp:
            indexer = pickle.load(fp)
        end = time.time()
    except IOError:
        indexer = SPIMI()
        start = time.time()
        indexer.build_index(tokens)
        end = time.time()
        print('Built index in', end - start, 'sec')
        with open('index.pkl', 'wb') as fp:
            pickle.dump(indexer, fp)
    indexer.to_disk('data/')
    uncompressed_to_disk = time.time()
    print('Wrote uncompressed index to disk in', uncompressed_to_disk - end, 'sec')
    compressor = Compressed(indexer)
    compressor.blocking_gamma('data/', k=8)
    compressed_to_disk = time.time()
    print('Wrote compressed index to disk in', compressed_to_disk - uncompressed_to_disk, 'sec')
