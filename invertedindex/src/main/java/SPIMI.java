import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

    public void writeToDisk() throws IOException, NoSuchFieldException, IllegalAccessException {
        Path index = Paths.get("uncompressed.index");
        Path ptr = Paths.get("uncompressed.pointers");
        // uncompressed index has fixed-width terms
        int fixedWidth = 0;
        for (String term : invertedIndex.keySet()) {
            if (fixedWidth < term.length()) {
                fixedWidth = term.length();
            }
        }
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(index,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE));
             OutputStream pointers = new BufferedOutputStream(Files.newOutputStream(ptr,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING,
                     StandardOpenOption.WRITE))) {
            for (String term : invertedIndex.keySet()) {
                out.write(ByteHelper.stringToFixedWidthBytes(term, fixedWidth));
            }
            for (PostingsEntry p : invertedIndex.values()) {
                out.write(ByteHelper.mapToFixedWidthBytes(p.getPostingsList()));
            }
        }
    }

    @Override
    public String toString() {
        return "SPIMI{" +
                "docInfo=" + docInfo +
                ", invertedIndex=" + invertedIndex +
                '}';
    }
}
