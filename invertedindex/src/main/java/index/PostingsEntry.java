package index;

import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * Represents a posting entry for a term in the dictionary.
 */
public class PostingsEntry implements Serializable {
    private int documentFrequency;
    private LinkedHashMap<Integer, TermWeight> postingsList;

    public PostingsEntry(int docId) {
        this.documentFrequency = 1;
        this.postingsList = new LinkedHashMap<>(2, 0.99f);
        this.postingsList.put(docId, new TermWeight());
    }

    public int getDocumentFrequency() {
        return documentFrequency;
    }

    public LinkedHashMap<Integer, TermWeight> getPostingsList() {
        return this.postingsList;
    }

    public PostingsEntry update(int docId) {
        if (!this.postingsList.containsKey(docId)) {
            this.documentFrequency += 1;
        }
        this.postingsList.computeIfPresent(docId, (k, v) -> v.incrementTf());
        this.postingsList.putIfAbsent(docId, new TermWeight());
        return this;
    }

    @Override
    public String toString() {
        return "index.PostingsEntry{" +
                "documentFrequency=" + documentFrequency +
                ", postingsList=" + postingsList +
                '}';
    }
}
