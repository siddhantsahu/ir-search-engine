import java.util.LinkedHashMap;

public class PostingsEntry {
    private int documentFrequency;
    private LinkedHashMap<Integer, Integer> postingsList;

    public PostingsEntry(int docId) {
        this.documentFrequency = 1;
        this.postingsList = new LinkedHashMap<>(2, 0.99f);
        this.postingsList.put(docId, 1);
    }

    public int getDocumentFrequency() {
        return documentFrequency;
    }

    public LinkedHashMap<Integer, Integer> getPostingsList() {
        return postingsList;
    }

    public PostingsEntry update(int docId) {
        this.postingsList.putIfAbsent(docId, 1);
        this.postingsList.computeIfPresent(docId, (k, v) -> v + 1);
        this.documentFrequency += 1;
        return this;
    }

    @Override
    public String toString() {
        return "PostingsEntry{" +
                "documentFrequency=" + documentFrequency +
                ", postingsList=" + postingsList +
                '}';
    }
}
