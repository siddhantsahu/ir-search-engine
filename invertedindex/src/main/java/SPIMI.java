import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
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

    public void createUncompressedIndex() throws IOException {
        Path index = Paths.get("uncompressed.index");
        Path pointer = Paths.get("uncompressed.pointers");
        // space occupied by the longest term in the dictionary
        int fixedWidth = 0;
        for (String term : invertedIndex.keySet()) {
            if (fixedWidth < term.length()) {
                fixedWidth = term.length();
            }
        }

        // write to disk and store references to the terms and postings list
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(index,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE));
             OutputStream ref = new BufferedOutputStream(Files.newOutputStream(pointer,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING,
                     StandardOpenOption.WRITE))) {
            Integer termReference;
            Integer postingsReference;
            Integer currentFilePosition = 0;
            for (Map.Entry<String, PostingsEntry> entry : invertedIndex.entrySet()) {
                byte[] termBytes = ByteUtils.stringToFixedWidthBytes(entry.getKey(), fixedWidth);
                byte[] postingBytes = ByteUtils.postingListToBytes(entry.getValue().getPostingsList());
                byte[] documentFrequency = ByteUtils.intToBytes(entry.getValue().getDocumentFrequency());
                // update references to terms and postings
                ref.write(documentFrequency);
                currentFilePosition += documentFrequency.length;
                termReference = currentFilePosition;
                ref.write(ByteUtils.intToBytes(termReference));
                currentFilePosition += termBytes.length;
                postingsReference = currentFilePosition;
                ref.write(ByteUtils.intToBytes(postingsReference));
                currentFilePosition += postingBytes.length;
                // actually write the terms and postings
                out.write(termBytes);
                out.write(postingBytes);
            }
        }
    }

    public void createCompressedIndex(int blockSize) throws IOException {
        // compressed index with blocking and gamma
        Path index = Paths.get("compressed.gamma.index");
        Path pointer = Paths.get("compressed.gamma.pointers");

        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(index,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE));
             OutputStream ref = new BufferedOutputStream(Files.newOutputStream(pointer,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING,
                     StandardOpenOption.WRITE))) {

            List<Integer> documentFrequencies = new ArrayList<>();
            List<Integer> termReferences = new ArrayList<>();
            List<Integer> postingReferences = new ArrayList<>();
            Integer currentFilePosition = 0;

            // write dictionary as a string
            int numberOfTerms = 0;
            for (String term : invertedIndex.keySet()) {
                if (numberOfTerms % blockSize == 0) {
                    // store term reference
                    termReferences.add(currentFilePosition);
                }
                // write term length as a byte and the actual term
                byte len = (byte) term.length();    // max length around 25
                byte[] lenBytes;
                lenBytes = ByteBuffer.allocate(1).put(len).array();
                byte[] termBytes = term.getBytes();
                currentFilePosition += lenBytes.length + termBytes.length;
                out.write(lenBytes);
                out.write(termBytes);
                numberOfTerms += 1;
            }

            // compress postings list using gamma-encoded gaps and write to file
            for (PostingsEntry p : invertedIndex.values()) { // order of values correspond to keys, because TreeMap
                int previousDocId = -1; // -1 means there is no previous doc id
                documentFrequencies.add(p.getDocumentFrequency());
                postingReferences.add(currentFilePosition);

                // write compressed posting list for current term
                for (Map.Entry<Integer, Integer> entry : p.getPostingsList().entrySet()) {
                    // key is doc id and value is term frequency
                    if (previousDocId == -1) {  // first doc, so write doc id instead of gaps
                        byte[] docIdBytes = ByteUtils.intToBytes(entry.getKey());
                        out.write(docIdBytes);
                        currentFilePosition += docIdBytes.length;
                    } else {    // write gaps
                        int gap = entry.getKey() - previousDocId;
                        byte[] gapBytes = ByteUtils.gapToBytes(gap, "gamma");
                        out.write(gapBytes);
                        currentFilePosition += gapBytes.length;
                    }
                    byte[] tfBytes = ByteUtils.intToBytes(entry.getValue());
                    out.write(tfBytes);
                    currentFilePosition += tfBytes.length;
                    previousDocId = entry.getKey(); // update previous doc id for next iteration
                }
            } // end writing posting list

            // write term pointers and posting pointers
            int ixTerm = 0;
            for (int i = 0; i < documentFrequencies.size(); i++) {
                ref.write(ByteUtils.intToBytes(documentFrequencies.get(i)));
                if (i % blockSize == 0) {
                    // write term pointer
                    ref.write(ByteUtils.intToBytes(termReferences.get(ixTerm)));
                    ixTerm += 1;
                }
                ref.write(ByteUtils.intToBytes(postingReferences.get(i)));
            }
        }   // end writing to file
    }
}
