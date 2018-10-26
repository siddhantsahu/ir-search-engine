import java.nio.ByteBuffer;
import java.util.BitSet;

public class Utils {
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

}
