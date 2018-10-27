import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DocumentInfo {
    private int maxTf;
    private int docLen;

    public DocumentInfo() {
        this.maxTf = 1;
        this.docLen = 1;
    }

    public int getMaxTf() {
        return maxTf;
    }

    public int getDocLen() {
        return docLen;
    }

    public DocumentInfo update(int tf) {
        if (tf > this.maxTf) {
            this.maxTf = tf;
        }
        this.docLen += 1;
        return this;
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(Utils.intToBytes(this.maxTf));
        out.write(Utils.intToBytes(this.docLen));
        return out.toByteArray();
    }

    @Override
    public String toString() {
        return "DocumentInfo{" +
                "maxTf=" + maxTf +
                ", docLen=" + docLen +
                '}';
    }
}