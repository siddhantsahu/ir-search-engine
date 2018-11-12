package search;

import java.util.*;

/**
 * Implements a sparse vector, where only a few entries are non-zero.
 */
public class SparseVector {
    private List<String> labels;
    private Map<Integer, Double> vec;

    public SparseVector(List<String> labels) {
        this.labels = labels;
        this.vec = new HashMap<>();
    }

    public void put(int i, double value) {
        if (value == 0) {
            this.vec.remove(i);
        } else {
            this.vec.put(i, value);
        }
    }

    public double get(int i) {
        return this.vec.getOrDefault(i, 0.0);
    }

    public String getLabel(int i) {
        return labels.get(i);
    }

    public double getMagnitude() {
        double res = 0;
        for (double d : this.vec.values()) {
            res += d * d;
        }
        return Math.sqrt(res);
    }

    public double dotProduct(SparseVector v) {
        double sum = 0.0;
        for (int i : this.vec.keySet()) {
            sum += this.vec.get(i) * v.get(i);
        }
        return sum;
    }

    @Override
    public String toString() {
        // format {(label, index)=weight}
        StringBuilder st = new StringBuilder();
        SortedSet<Integer> keys = new TreeSet<>(this.vec.keySet()); // to print in alphabetical order of terms
        st.append("[ ");
        for (int i : keys) {
            st.append("(" + this.getLabel(i) + ", " + i + ")=" + this.get(i) + " ");
        }
        st.append("]");
        return st.toString();
    }
}
