## Inverted Index

This is a module to build and compress an inverted index on a collection of documents using the single pass in-memory indexing algorithm (SPIMI).

### How to run (tested on csgrads1 server)?
*Requires Java 8*
1. Extract the source code to an appropriate location.
2. Add CoreNLP libraries to classpath using `source /usr/local/corenlp350/classpath.sh`. After this $CLASSPATH should be set. Run `echo $CLASSPATH` to verify.
3. Build the project: `javac -cp $CLASSPATH -d target/classes/ src/main/java/*`. This puts the generated class files in `target/classes/` directory.
4. Go to `classes` directory: `cd target/classes/`
5. Then, run the main class, `Driver` as follows: `java Driver <lemma or stem> <path to cranfield collection> <output folder to store binary files>`.

Use the following two commands to generate indexes (3 indexes each) for lemma and stem versions.
`java Driver lemma /people/cs/s/sanda/cs6322/Cranfield/ ../../lemma/`
`java Driver stem /people/cs/s/sanda/cs6322/Cranfield/ ../../stem/`

### How does it work?

#### High level description

- For each document:
    1. Parse document as XML and retain non-numeric fields.
    2. Lemmatize text and then stem it, if stemming is enabled. The stemming operation is memoized.
    3. Re-tokenize the resulting token to handle acronyms, dashes, numbers and non-alphabet sequences. All preprocessing is done now.
    4. Add each term-document pair (if term isn't a stopword) to inverted index using SPIMI algorithm until no term-document pairs are left.
- Write uncompressed and compressed versions of the index to disk.

#### Some low level designs & data structures

- **The index** is designed as a `SortedMap` with term as keys and a `PostingsEntry` as the value.
    + A single `PostingsEntry` consists of the `df`, i.e. the document frequency and the postings list.
    + A postings list is implemented as a `LinkedHashMap` which preserves the order in which documents are inserted. This is one of the key ideas of the SPIMI algorithm that avoids sorting the posting list.
    + The document information such as `max_tf` (the term frequency of the term occuring the most number of times in the document) and `doc_len` (the count of words found in the document) is stored as a  `HashMap` of `doc_id` - `DocumentInfo` pairs.
- The *uncompressed* version of the *dictionary* uses fixed-width strings to store each term. The width used is the length of the longest term found in the index. The *compressed* version 1 uses **blocking** (with size 8) and the version 2 additionally uses **front-coding** to further compress the dictionary.
- **Postings list:** The *uncompressed* version of the postings list uses integer (4 bytes) to store `doc_id` and `tf`. While the *compressed versions* use **gamma** and **delta** codes (adaptive number of bytes, less than or equal to 4 bytes though) to store gaps.
- **Binary files**: Each version of the index is stored as a set of 3 binary files -- one for the document info, one for the dictionary and postings list, and a third to store the document frequency, term pointers and posting list pointers. The binary file is written in *lexicographical* order of the terms in the dictionary.
    + Uncompressed index as binary file: In the uncompressed version, the posting list immediately follows the fixed-width term. Thus, the uncompressed index file is a sequence of term and posting list pairs.
    + Compressed index as binary file: In the compressed version, all the terms are stored first followed by all the postings lists for these terms.

**Note:** The statistics and outputs are printed on the console with every run of the program. But, for convinience viewing, they are annotated and attached in a separate file `stats.md`.