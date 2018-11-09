package index;

import util.Utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Compression {
    /**
     * Write the index in this folder.
     */
    private final String outFolder;

    private SPIMI index;

    public Compression(SPIMI index, String outFolder) {
        this.index = index;
        this.outFolder = outFolder;
        // create folder if it does not exist
        File directory = new File(outFolder);
        if (!directory.exists()) {
            directory.mkdirs(); // creates parents too
        }
    }

    /**
     * Writes the document information to a binary file.
     *
     * @param p Path to the binary file
     * @throws IOException
     */
    private void docInfoToDisk(Path p) throws IOException {
        try (OutputStream out = new BufferedOutputStream((Files.newOutputStream(p,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)))) {
            // format: docId, maxTf, docLen - each 4 bytes
            for (Map.Entry<Integer, DocumentInfo> entry : this.index.getDocInfo().entrySet()) {
                out.write(Utils.intToBytes(entry.getKey()));
                out.write(entry.getValue().toBytes());
            }
        }
    }

    /**
     * Writes the uncompressed index to a binary file. Terms in the dictionary are stored in fixed width strings.
     * A pointer file is created for deserializing the binary file. The pointer file has document frequency and
     * references to the terms and posting list.
     *
     * @throws IOException
     */
    public void createUncompressedIndex() throws IOException {
        Path index = Paths.get(outFolder, "uncompressed.index");
        Path pointer = Paths.get(outFolder, "uncompressed.pointers");

        // write doc info
        docInfoToDisk(Paths.get(outFolder, "uncompressed.docinfo"));

        // space occupied by the longest term in the dictionary
        int fixedWidth = 0;
        for (String term : this.index.getInvertedIndex().keySet()) {
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
            for (Map.Entry<String, PostingsEntry> entry : this.index.getInvertedIndex().entrySet()) {
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

    /**
     * Compresses the dictionary and postings list, and writes them to a binary file with pointers to term and posting
     * list location.
     *
     * @param blockSize          Uses blocking to save space on storing term pointers, stores term pointer to every
     *                           `blockSize`-th term
     * @param compressionCode    Either "gamma" or "delta". Uses gamma codes and delta codes to compress the index
     * @param frontCodingEnabled Frontcoding saves additional space by not storing common term prefixes repeatedly
     * @throws IOException
     */
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
            for (String term : this.index.getInvertedIndex().keySet()) {
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
            for (PostingsEntry p : this.index.getInvertedIndex().values()) { // order of values correspond to keys, because TreeMap
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
