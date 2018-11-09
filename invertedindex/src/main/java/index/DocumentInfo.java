package index;

import util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Represents the document information stored for every document id seen in the collection.
 */
public class DocumentInfo implements Serializable {
    /**
     * Represents the frequency of the term that is seen the most number of times in this document.
     */
    private int maxTf;

    /**
     * Total number of word occurences in this document.
     */
    private int docLen;

    private double weightedDocLenSquared;

    public DocumentInfo() {
        this.maxTf = 1;
        this.docLen = 1;
        this.weightedDocLenSquared = 0;
    }

    public int getMaxTf() {
        return maxTf;
    }

    public int getDocLen() {
        return docLen;
    }

    public double getWeightedDocLenSquared() {
        return weightedDocLenSquared;
    }

    /**
     * Updates the `maxTf` and `docLen` when a new term is seen.
     *
     * @param tf term frequency of the term seen
     * @return the current `index.DocumentInfo` object
     */
    public DocumentInfo update(int tf) {
        if (tf > this.maxTf) {
            this.maxTf = tf;
        }
        this.docLen += 1;
        return this;
    }

    public DocumentInfo updateLenSquared(double sumOfSquares) {
        this.weightedDocLenSquared += sumOfSquares;
        return this;
    }

    /**
     * Helper function to convert the contents of this object to bytes.
     *
     * @return byte array representing the content in bytes
     * @throws IOException
     */
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(Utils.intToBytes(this.maxTf));
        out.write(Utils.intToBytes(this.docLen));
        return out.toByteArray();
    }

    @Override
    public String toString() {
        return "index.DocumentInfo{" +
                "maxTf=" + maxTf +
                ", docLen=" + docLen +
                '}';
    }
}