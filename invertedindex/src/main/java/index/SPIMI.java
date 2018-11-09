package index;

import java.io.Serializable;
import java.util.*;

/**
 * Implements a variant of the single pass in-memory indexing algorithm as described in the textbook by Manning & others
 * In the original single-pass algorithm, the dictionary is sorted after the program runs out of memory. In this case,
 * we use a SortedMap instead of a HashMap to avoid sorting the dictionary at the end.
 */
public class SPIMI implements Serializable {
    private static final String[] SET_VALUES = new String[]{"a", "all", "an", "and", "any", "are", "as", "be", "been",
            "but", "by ", "few", "for", "have", "he", "her", "here", "him", "his", "how", "i", "in", "is", "it", "its",
            "many", "me", "my", "none", "of", "on ", "or", "our", "she", "some", "the", "their", "them", "there",
            "they", "that ", "this", "us", "was", "what", "when", "where", "which", "who", "why", "will", "with",
            "you", "your"};
    public static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(SET_VALUES));

    /**
     * Maps document id to a data structure storing document related information.
     */
    private Map<Integer, DocumentInfo> docInfo = new HashMap<>();

    /**
     * A SortedMap of dictionary to postings list.
     */
    private Map<String, PostingsEntry> invertedIndex = new TreeMap<>();

    public Map<Integer, DocumentInfo> getDocInfo() {
        return docInfo;
    }

    public Map<String, PostingsEntry> getInvertedIndex() {
        return invertedIndex;
    }

    public int getDF(String term) {
        if (!this.invertedIndex.containsKey(term)) {
            return 0;
        } else {
            return this.invertedIndex.get(term).getDocumentFrequency();
        }
    }

    public int getMaxTf(int docId) {
        return this.docInfo.get(docId).getMaxTf();
    }

    public double getTFWeighted(String term, int docId) {
        if (!this.getPostingList(term).containsKey(docId)) {
            return 0.0;
        } else {
            return this.getPostingList(term).get(docId).getTfWeighted();
        }
    }

    public Map<Integer, TermWeight> getPostingList(String term) {
        if (!this.invertedIndex.containsKey(term)) {
            throw new NoSuchElementException("Term not found in dictionary.");
        }
        return this.invertedIndex.get(term).getPostingsList();
    }

    /**
     * Adds an unseen term to the dictionary.
     *
     * @param term  term in collection
     * @param docId document id the term is found in
     */
    private void addToDictionary(String term, Integer docId) {
        // update document info with term before proceeding (stopwords are counted in doc length)
        docInfo.putIfAbsent(docId, new DocumentInfo());
        docInfo.computeIfPresent(docId, (k, v) -> v.update(1));
        // if not stopword, add to dictionary
        if (!STOPWORDS.contains(term)) {
            PostingsEntry p = new PostingsEntry(docId);
            invertedIndex.put(term, p);
        }
    }

    /**
     * Updates a term's posting list with the doc id seen. If the doc id is encountered for the first time, it is added
     * to the end of the posting list. Since doc ids are seen in monotonically increasing order, the posting list is
     * naturally sorted in ascending order. This is one of the key ideas of the index.SPIMI algorithm.
     *
     * @param pList the existing posting list of the term
     * @param docId the doc id seen in the (term, doc) pair
     * @return
     */
    private PostingsEntry addToPostingList(PostingsEntry pList, int docId) {
        PostingsEntry postingList = pList.update(docId);
        docInfo.putIfAbsent(docId, new DocumentInfo());
        docInfo.computeIfPresent(docId, (k, v) -> v.update(postingList.getDocumentFrequency()));
        return postingList;
    }

    /**
     * Called for every term, doc pair in the collection.
     *
     * @param term  term
     * @param docId doc id
     */
    public void invert(String term, Integer docId) {
        docInfo.putIfAbsent(docId, new DocumentInfo());
        if (!invertedIndex.containsKey(term)) {
            addToDictionary(term, docId);
        }
        invertedIndex.computeIfPresent(term, (k, v) -> addToPostingList(v, docId));
    }
}
