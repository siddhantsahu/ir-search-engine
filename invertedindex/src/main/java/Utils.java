import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class with several helper functions to convert Java objects to bytes and other utility functions.
 */
public class Utils {
    public static byte[] stringToFixedWidthBytes(String str, int width) {
        byte[] result = new byte[width];
        System.arraycopy(str.getBytes(), 0, result, 0, str.length());
        return result;
    }

    public static String slice_start(String s, int startIndex) {
        // https://stackoverflow.com/a/17307852/2986835
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
        // r-l orientation
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

    public static String longestCommonPrefix(String[] strings) {
        // https://codereview.stackexchange.com/a/46967
        if (strings.length == 0) {
            return "";   // Or maybe return null?
        }

        for (int prefixLen = 0; prefixLen < strings[0].length(); prefixLen++) {
            char c = strings[0].charAt(prefixLen);
            for (int i = 1; i < strings.length; i++) {
                if (prefixLen >= strings[i].length() ||
                        strings[i].charAt(prefixLen) != c) {
                    // Mismatch found
                    return strings[i].substring(0, prefixLen);
                }
            }
        }
        return strings[0];
    }

    public static String gammaCode(final int n) {
        String binary = Integer.toBinaryString(n);
        String offset = Utils.slice_start(binary, 1);
        String length = Utils.unary(offset.length());
        return length + offset;
    }

    public static String deltaCode(final int n) {
        String binary = Integer.toBinaryString(n);
        String offset = Utils.slice_start(binary, 1);
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
        byte[] result = new byte[m.size() * 2 * 4];
        for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
            byte[] docIdBytes = Utils.intToBytes(entry.getKey());
            byte[] tfBytes = Utils.intToBytes(entry.getValue());
            System.arraycopy(docIdBytes, 0, result, 0, docIdBytes.length);
            System.arraycopy(tfBytes, 0, result, docIdBytes.length, tfBytes.length);
        }
        return result;
    }

    public static byte[] compressedPostingListToBytes(LinkedHashMap<Integer, Integer> m, String compressionCode)
            throws IOException {
        // https://stackoverflow.com/a/9133993/2986835
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        int previousDocId = -1; // -1 means there is no previous doc id
        // write compressed posting list for current term
        for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
            // key is doc id and value is term frequency
            byte[] docIdBytes;
            if (previousDocId == -1) {  // first doc, so write doc id instead of gaps
                docIdBytes = Utils.intToBytes(entry.getKey());
            } else {    // write gaps
                int gap = entry.getKey() - previousDocId;
                docIdBytes = Utils.gapToBytes(gap, compressionCode);
            }
            byte[] tfBytes = Utils.intToBytes(entry.getValue());
            outStream.write(docIdBytes);
            outStream.write(tfBytes);
            previousDocId = entry.getKey(); // update previous doc id for next iteration
        }
        return outStream.toByteArray();
    }

    public static byte[] blockOfTermsToBytes(List<String> block) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        for (String term : block) {
            byte len = (byte) term.length();    // max length around 25
            byte[] lenBytes = ByteBuffer.allocate(1).put(len).array();  // store length of term
            byte[] termBytes = term.getBytes();
            outStream.write(lenBytes);
            outStream.write(termBytes);
        }
        return outStream.toByteArray();
    }

    public static byte[] frontCodedBlockToBytes(List<String> block) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        String commonPrefix = Utils.longestCommonPrefix(block.toArray(new String[0]));
        int prefixLength = commonPrefix.length();
        for (int i = 0; i < block.size(); i++) {
            // for the first term, write prefix followed by a *
            byte len;
            byte[] lenBytes;
            byte[] termBytes;
            if (i == 0) {
                len = (byte) block.get(i).length();
                termBytes = new String(commonPrefix + "*" +  // * marks end of prefix
                        Utils.slice_start(block.get(i), prefixLength)).getBytes();
            } else {
                String extraCharacters = "|" + Utils.slice_start(block.get(i), prefixLength); // after stripping prefix
                len = (byte) extraCharacters.length();
                termBytes = extraCharacters.getBytes();
            }

            lenBytes = ByteBuffer.allocate(1).put(len).array();
            outStream.write(lenBytes);
            outStream.write(termBytes);
        }
        return outStream.toByteArray();
    }


}
