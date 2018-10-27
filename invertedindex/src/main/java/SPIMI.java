import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class SPIMI {
    private final String outFolder;
    public static final String[] SET_VALUES = new String[]{"a", "all", "an", "and", "any", "are", "as", "be", "been",
            "but", "by ", "few", "for", "have", "he", "her", "here", "him", "his", "how", "i", "in", "is", "it", "its",
            "many", "me", "my", "none", "of", "on ", "or", "our", "she", "some", "the", "their", "them", "there",
            "they", "that ", "this", "us", "was", "what", "when", "where", "which", "who", "why", "will", "with",
            "you", "your"};
    public static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(SET_VALUES));
    private Map<Integer, DocumentInfo> docInfo = new HashMap<>();
    private Map<String, PostingsEntry> invertedIndex = new TreeMap<>();

    public SPIMI(String outFolder) {
        this.outFolder = outFolder;
        // create folder if it does not exist
        File directory = new File(outFolder);
        if (!directory.exists()) {
            directory.mkdirs(); // creates parents too
        }
    }

    public Map<Integer, DocumentInfo> getDocInfo() {
        return docInfo;
    }

    public Map<String, PostingsEntry> getInvertedIndex() {
        return invertedIndex;
    }

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

    private void docInfoToDisk(Path p) throws IOException {
        try (OutputStream out = new BufferedOutputStream((Files.newOutputStream(p,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)))) {
            // format: docId, maxTf, docLen - each 4 bytes
            for (Map.Entry<Integer, DocumentInfo> entry : this.docInfo.entrySet()) {
                out.write(Utils.intToBytes(entry.getKey()));
                out.write(entry.getValue().toBytes());
            }
        }
    }

    public void createUncompressedIndex() throws IOException {
        // TODO: create output directory if it doesn't exist
        Path index = Paths.get(outFolder, "uncompressed.index");
        Path pointer = Paths.get(outFolder, "uncompressed.pointers");

        // write doc info
        docInfoToDisk(Paths.get(outFolder, "uncompressed.docinfo"));

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
                byte[] termBytes = Utils.stringToFixedWidthBytes(entry.getKey(), fixedWidth);
                byte[] postingBytes = Utils.postingListToBytes(entry.getValue().getPostingsList());
                byte[] documentFrequency = Utils.intToBytes(entry.getValue().getDocumentFrequency());
                // update references to terms and postings
                ref.write(documentFrequency);
                currentFilePosition += documentFrequency.length;
                termReference = currentFilePosition;
                ref.write(Utils.intToBytes(termReference));
                currentFilePosition += termBytes.length;
                postingsReference = currentFilePosition;
                ref.write(Utils.intToBytes(postingsReference));
                currentFilePosition += postingBytes.length;
                // actually write the terms and postings
                out.write(termBytes);
                out.write(postingBytes);
            }
        }
    }

    public void createCompressedIndex(int blockSize,
                                      String compressionCode,
                                      boolean frontCodingEnabled)
            throws IOException {
        // compressed index with blocking and gamma
        String frontCodeText = "";  // for appropriate names of files
        if (frontCodingEnabled) {
            frontCodeText = ".frontcoding";
        }
        // write doc info
        docInfoToDisk(Paths.get(outFolder, "compressed." + compressionCode + frontCodeText + ".docinfo"));

        // start writing the index
        Path index = Paths.get(outFolder, "compressed." + compressionCode + frontCodeText + ".index");
        Path pointer = Paths.get(outFolder, "compressed." + compressionCode + frontCodeText + ".pointers");

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
            List<String> blockOfTerms = new ArrayList<>(blockSize);
            for (String term : invertedIndex.keySet()) {
                if (numberOfTerms % blockSize == 0) {
                    // store term reference
                    termReferences.add(currentFilePosition);
                    // write compressed block to file
                    if (!blockOfTerms.isEmpty()) {
                        byte[] compressedBlock;
                        if (!frontCodingEnabled) {
                            compressedBlock = Utils.blockOfTermsToBytes(blockOfTerms);
                        } else {
                            compressedBlock = Utils.frontCodedBlockToBytes(blockOfTerms);
                        }
                        out.write(compressedBlock);
                        currentFilePosition += compressedBlock.length;
                        blockOfTerms.clear();
                    }
                } else {
                    blockOfTerms.add(term);
                }
                numberOfTerms += 1;
            }

            // compress postings list using gamma-encoded gaps and write to file
            for (PostingsEntry p : invertedIndex.values()) { // order of values correspond to keys, because TreeMap
                documentFrequencies.add(p.getDocumentFrequency());
                postingReferences.add(currentFilePosition);
                byte[] postingBytes = Utils.compressedPostingListToBytes(p.getPostingsList(), compressionCode);
                out.write(postingBytes);
                currentFilePosition += postingBytes.length;
            } // end writing posting list

            // write term pointers and posting pointers
            int ixTerm = 0;
            for (int i = 0; i < documentFrequencies.size(); i++) {
                ref.write(Utils.intToBytes(documentFrequencies.get(i)));
                if (i % blockSize == 0) {
                    // write term pointer
                    ref.write(Utils.intToBytes(termReferences.get(ixTerm)));
                    ixTerm += 1;
                }
                ref.write(Utils.intToBytes(postingReferences.get(i)));
            }
        }   // end writing to file
    }
}
