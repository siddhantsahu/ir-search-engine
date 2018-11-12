package search;

import index.Indexer;
import index.SPIMI;
import util.ParseXMLFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

class Driver {
    private static String folder;
    private static String serializedIndexPath;

    public static void main(String[] args) throws IOException {
        boolean useStemming = false;
        folder = args[0];
        serializedIndexPath = "/tmp/lemma.index";

        // SPIMI index = Indexer.buildIndex(folder, useStemming);

        // Start serialization: helpful when testing, avoid creating the same index several times
        SPIMI index = null;
        try {
            // Reading the object from a file
            FileInputStream file = new FileInputStream(serializedIndexPath);
            ObjectInputStream in = new ObjectInputStream(file);

            // Method for deserialization of object
            index = (SPIMI) in.readObject();

            in.close();
            file.close();

            System.out.println("Read index from disk.");
        } catch (IOException ex) {
            System.out.println("Need to re-create index.");
            index = Indexer.buildIndex(folder, useStemming);
            try {
                //Saving of object in a file
                FileOutputStream file = new FileOutputStream(serializedIndexPath);
                ObjectOutputStream out = new ObjectOutputStream(file);

                // Method for serialization of object
                out.writeObject(index);

                out.close();
                file.close();

                System.out.println("Object has been serialized");

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        // end serialization

        // write to file
        String hwQueriesFile = args[1];
        List<String> hw3Queries = Files.readAllLines(Paths.get(hwQueriesFile));
        for (int i = 0; i < hw3Queries.size(); i++) {
            for (String w : Arrays.asList("w1", "w2")) {
                String out = "/tmp/" + w + "-q" + i + ".txt";
                writeSearchResults(hw3Queries.get(i), index, w, out);
            }
        }
    }

    /**
     * Convinience function that writes search result for a query to a file.
     *
     * @param text           query text
     * @param index          index to use for querying
     * @param weightFunction w1 or w2
     * @param outfile        file to write to
     * @throws IOException
     */
    private static void writeSearchResults(String text, SPIMI index, String weightFunction, String outfile)
            throws IOException {
        QueryParser search = new QueryParser(text, index);

        Map<Integer, Double> top5 = search.vectorSpaceModel(5, weightFunction);

        FileWriter fw = new FileWriter(outfile);
        try (PrintWriter pw = new PrintWriter(fw)) {
            pw.println("Query = " + text);
            // get vector representation of top 5 documents and the query
            int rank = 1;
            for (int d : top5.keySet()) {
                String filename = getExternalID(d);
                pw.println("Rank = " + rank + ", filename = " + filename + ", score = " + top5.get(d));
                pw.println("Headline = " + getHeadline(folder, filename));
                List<SparseVector> vectors = search.getVectors(d);
                pw.println("Query vector = " + vectors.get(0));
                pw.println("Document vector = " + vectors.get(1));
                pw.println("------------------------------------------------------------------------");
                rank++;
            }
        }
    }

    /**
     * Helper function to get the filename corresponding to a doc id. This is a workaround because we don't store
     * metadata.
     *
     * @param docId document id
     * @return filename corresponding to this doc id
     */
    private static String getExternalID(int docId) {
        if (docId < 10) {
            return "cranfield000" + docId;
        } else if (docId < 100) {
            return "cranfield00" + docId;
        } else if (docId < 1000) {
            return "cranfield0" + docId;
        } else {
            return "cranfield" + docId;
        }
    }

    /**
     * Parses a cranfield document and returns the headline.
     *
     * @param folder   folder to the cranfield collection
     * @param filename filename to parse
     * @return headline of the document
     * @throws IOException
     */
    private static String getHeadline(String folder, String filename) throws IOException {
        ParseXMLFile cranfield = Indexer.parseCranfieldDocument(Paths.get(folder, filename).toFile());
        return cranfield.getTitleField().toString().replaceAll("\n", " ").trim();
    }
}