package index;

import java.io.Serializable;

/**
 * Class to store term weights. The default term weight is term frequency. However, for retrieval purposes, another
 * term weight can be stored.
 */
public class TermWeight implements Serializable {
    private int tf;
    private double tfWeighted;

    public TermWeight() {
        this.tf = 1;
        this.tfWeighted = 0.0;
    }

    public TermWeight incrementTf() {
        this.tf++;
        return this;
    }

    public void setTfWeighted(double tfWeighted) {
        this.tfWeighted = tfWeighted;
    }

    public int getTf() {
        return tf;
    }

    public double getTfWeighted() {
        return tfWeighted;
    }
}
