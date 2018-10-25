import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class SPIMI {

    private Map<String, Map> invertedIndex = new TreeMap<>();
    private Map<Integer, Map> documentInfo = new HashMap<>();

    public void invert(String term, Integer docId) {
        // if term does not exist create a new posting list of size 2 and load factor 0.95
        Map<Integer, Integer> postingList = this.invertedIndex.getOrDefault(term, new LinkedHashMap());
        postingList.putIfAbsent(docId, 1);
        postingList.computeIfPresent(docId, (k, v) -> v + 1);
        this.invertedIndex.put(term, postingList);
    }

    @Override
    public String toString() {
        return this.invertedIndex + "\n" + "Number of keys = " + this.invertedIndex.keySet().size();
    }
}
