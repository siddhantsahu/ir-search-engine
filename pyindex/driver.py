import glob
import pickle
import time

from inverted_index import SPIMI
from tokenizer import token_stream

if __name__ == '__main__':
    tokens = token_stream(glob.glob('../../tokenizer/Cranfield/cranfield000*'))
    indexer = SPIMI()
    start = time.time()
    indexer.build_index(tokens)
    indexer.to_disk('data/')