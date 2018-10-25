import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

public class ByteHelper {
    public static byte[] stringToFixedWidthBytes(String str, int width) {
        byte[] result = new byte[width];
        System.arraycopy(str.getBytes(), 0, result, 0, str.length());
        return result;
    }

    public static int nextPowerOf2(int n) {
        int p = 1;
        if (n > 0 && (n & (n - 1)) == 0)
            return n;

        while (p < n)
            p <<= 1;

        return p;
    }

    public static byte[] intToBytes(final int i) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(i);
        return bb.array();
    }

    public static byte[] mapToFixedWidthBytes(LinkedHashMap<Integer, Integer> m) throws NoSuchFieldException, IllegalAccessException {
        // get size of map
        int sizeOfMap = nextPowerOf2(m.size());
        byte[] result = new byte[sizeOfMap * 2 * 4];
        for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
            byte[] docIdBytes = intToBytes(entry.getKey());
            byte[] tfBytes = intToBytes(entry.getValue());
            System.arraycopy(docIdBytes, 0, result, 0, docIdBytes.length);
            System.arraycopy(tfBytes, 0, result, docIdBytes.length, tfBytes.length);
        }
        return result;
    }
}
