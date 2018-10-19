import glob
import time

from compressed_index import Compressed
from inverted_index import SPIMI
from tokenizer import token_stream

if __name__ == '__main__':
    tokens = token_stream(glob.glob('../../tokenizer/Cranfield/cranfield*'))
    indexer = SPIMI()
    start = time.time()
    indexer.build_index(tokens)
    end = time.time()
    print('Built index in', end - start, 'sec')
    print(list(indexer.inverted_index.keys()))
    indexer.to_disk('data/')
    uncompressed_to_disk = time.time()
    print('Wrote uncompressed index to disk in', uncompressed_to_disk - end, 'sec')
    compressor = Compressed(indexer)
    compressor.to_disk('data/', k=8, front_coding=False, compression='gamma')
    compressed_to_disk = time.time()
    print('Wrote compressed index to disk in', compressed_to_disk - uncompressed_to_disk, 'sec')
