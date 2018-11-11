package search;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents a query using the bag-of-words model.
 */
public class Query {
    private Map<String, Integer> parsed;

    public Query() {
        this.parsed = new TreeMap<>();
    }

    public void putWord(String word) {
        this.parsed.computeIfPresent(word, (k, v) -> v + 1);
        this.parsed.putIfAbsent(word, 1);
    }

    public Set<String> getTerms() {
        return this.parsed.keySet();
    }

    public int getMaxTf() {
        return Collections.max(this.parsed.values());
    }

    public int getTf(String term) {
        return this.parsed.getOrDefault(term, 0);
    }

    @Override
    public String toString() {
        return "Query{" +
                "parsed=" + parsed +
                '}';
    }
}
