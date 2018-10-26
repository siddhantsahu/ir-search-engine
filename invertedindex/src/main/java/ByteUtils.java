import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class ByteUtils {
    public static byte[] stringToFixedWidthBytes(String str, int width) {
        byte[] result = new byte[width];
        System.arraycopy(str.getBytes(), 0, result, 0, str.length());
        return result;
    }

    public static String slice_start(String s, int startIndex) {
        if (startIndex < 0) startIndex = s.length() + startIndex;
        return s.substring(startIndex);
    }

    public static String unary(final int i) {
        // https://stackoverflow.com/a/4903603/2986835
        String repeated = new String(new char[i]).replace("\0", "1");
        return repeated + "0";
    }

    public static BitSet bitsetFromString(String binary) {
        // https://stackoverflow.com/a/33386777/2986835
        // when read from left to right, usual scenario, can be directly applied to gamma codes
        BitSet bitset = new BitSet(binary.length());
        int len = binary.length();
        for (int i = len - 1; i >= 0; i--) {
            if (binary.charAt(i) == '1') {
                bitset.set(len - i - 1);
            }
        }
        return bitset;
    }

    public static byte[] intToBytes(final int i) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(i);
        return bb.array();
    }

    public static String gammaCode(final int n) {
        String binary = Integer.toBinaryString(n);
        String offset = ByteUtils.slice_start(binary, 1);
        String length = ByteUtils.unary(offset.length());
        return length + offset;
    }

    public static String deltaCode(final int n) {
        String binary = Integer.toBinaryString(n);
        String offset = ByteUtils.slice_start(binary, 1);
        String length = gammaCode(binary.length());
        return length + offset;
    }

    public static byte[] gapToBytes(final int gap, String code) {
        BitSet bs;
        String encodedGap;
        if (code.equals("gamma")) {
            encodedGap = gammaCode(gap);
        } else {
            encodedGap = deltaCode(gap);
        }
        bs = bitsetFromString(encodedGap);
        byte[] bytes = new byte[(bs.length() + 7) / 8]; // https://stackoverflow.com/a/6197426/2986835
        return bytes;
    }

    public static byte[] postingListToBytes(LinkedHashMap<Integer, Integer> m) {
        // times 2 is for key (doc id) and value (term frequency) and times 4 is for size of each integer
        byte[] result = new byte[m.size() * 2 * 4];
        for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
            byte[] docIdBytes = intToBytes(entry.getKey());
            byte[] tfBytes = intToBytes(entry.getValue());
            System.arraycopy(docIdBytes, 0, result, 0, docIdBytes.length);
            System.arraycopy(tfBytes, 0, result, docIdBytes.length, tfBytes.length);
        }
        return result;
    }
}
