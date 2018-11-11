package search;

import index.Indexer;
import index.SPIMI;

import java.io.*;
import java.util.Map;

class Driver {
    public static void main(String[] args) throws IOException {
        boolean useStemming = false;
        String folder = "/home/siddhant/Documents/Projects/ir_homeworks/tokenizer/Cranfield";
        String serializedPath = "/tmp/lemma.index";

        // SPIMI index = Indexer.buildIndex(folder, useStemming);

        // Start serialization: helpful when testing, avoid creating the same index several times
        SPIMI index = null;
        try {
            // Reading the object from a file
            FileInputStream file = new FileInputStream(serializedPath);
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
                FileOutputStream file = new FileOutputStream(serializedPath);
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

        QueryParser search = new QueryParser("what similarity laws must be obeyed when " +
                "constructing aeroelastic models of heated high speed aircraft", index);

        Map<Integer, Double> top5 = search.vectorSpaceModel(5, "w2");

        System.out.println("top5 = " + top5);

        // get vector representation of top 5 documents and the query
        for (int d : top5.keySet()) {
            search.getVector(d);
        }
    }
}