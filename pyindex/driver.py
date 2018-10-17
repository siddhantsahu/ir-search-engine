import glob
import pickle
import time

from inverted_index import SPIMI
from tokenizer import token_stream

if __name__ == '__main__':
    tokens = token_stream(glob.glob('../../tokenizer/Cranfield/cranfield00*'))
    indexer = SPIMI(tokens)
    start = time.time()
    indexer.build_index()
    with open('index1.uncompressed', 'wb') as db:
        pickle.dump(indexer.inverted_index, db)
