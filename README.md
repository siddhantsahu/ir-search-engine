## Search Engine

**Indexing:** Build and compress an inverted index on a collection of documents using the single pass in-memory indexing algorithm (SPIMI).
**Retrieval:** Implements the vector space model and a variant of the popular Okapi BM25 ranking model for searching.

This search engine has been tested on the cranfield collection.

> The Cranfield collection. This was the pioneering test collection in allowing precise quantitative measures of information retrieval effectiveness, but is nowadays too small for anything but the most elementary pilot experiments. Collected in the United Kingdom starting in the late 1950s, it contains 1398 abstracts of aerodynamics journal articles, a set of 225 queries, and exhaustive relevance judgments of all (query, document) pairs.

### How does indexing work?

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

**Note:** The statistics and outputs are printed on the console with every run of the program. But, for convenience, they are annotated and attached in a separate file `stats.md`.

### Retrieval Models

Two retrieval models are implemented:
- Variant of max-tf term weighting
- Variant of Okapi BM25 retrieval model

### Usage

Requirements: Java 8 and IntelliJ Idea (import as Maven project)

Run the `search.Driver` class with cranfield collection path and query file as command line arguments. The *cranfield collection path* is the path to the directory containing the 1400 cranfield files and the query file is a file containing queries separated by newline.