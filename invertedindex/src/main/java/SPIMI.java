import java.util.*;

public class SPIMI {
    public static final String[] SET_VALUES = new String[]{"a", "all", "an", "and", "any", "are", "as", "be", "been",
            "but", "by ", "few", "for", "have", "he", "her", "here", "him", "his", "how", "i", "in", "is", "it", "its",
            "many", "me", "my", "none", "of", "on ", "or", "our", "she", "some", "the", "their", "them", "there",
            "they", "that ", "this", "us", "was", "what", "when", "where", "which", "who", "why", "will", "with",
            "you", "your"};
    public static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(SET_VALUES));
    private Map<Integer, DocumentInfo> docInfo = new HashMap<>();
    private Map<String, PostingsEntry> invertedIndex = new TreeMap<>();

    private void addToDictionary(String term, Integer docId) {
        // update document info before proceeding
        docInfo.putIfAbsent(docId, new DocumentInfo());
        docInfo.computeIfPresent(docId, (k, v) -> v.update(1)); // term is not seen before
        // if not stopword, add to dictionary
        if (!STOPWORDS.contains(term)) {
            PostingsEntry p = new PostingsEntry(docId);
            invertedIndex.put(term, p);
        }
    }

    private PostingsEntry addToPostingList(PostingsEntry pList, int docId) {
        PostingsEntry postingList = pList.update(docId);
        docInfo.putIfAbsent(docId, new DocumentInfo());
        docInfo.computeIfPresent(docId, (k, v) -> v.update(postingList.getDocumentFrequency()));
        return postingList;
    }

    public void invert(String term, Integer docId) {
        docInfo.putIfAbsent(docId, new DocumentInfo());
        if (!invertedIndex.containsKey(term)) {
            addToDictionary(term, docId);
        }
        invertedIndex.computeIfPresent(term, (k, v) -> addToPostingList(v, docId));
    }

    @Override
    public String toString() {
        return "SPIMI{" +
                "docInfo=" + docInfo +
                ", invertedIndex=" + invertedIndex +
                '}';
    }
}
