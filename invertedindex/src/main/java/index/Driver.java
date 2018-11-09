package index;

import util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class Driver {
    public static void printTermInformation(SPIMI s, String term) throws IOException {
        Map<String, PostingsEntry> index = s.getInvertedIndex();
        // get document frequency
        int df = index.get(term).getDocumentFrequency();
        // get overall term frequency
        int tfOverall = 0;
        LinkedHashMap<Integer, Integer> postingsList = index.get(term).getPostingsList();
        for (Integer tf : postingsList.values()) {
            tfOverall += tf;
        }
        // get length of inverted list (in bytes) - for uncompressed and both versions of compressed indexes
        int lengthUncompressed = Utils.postingListToBytes(postingsList).length;
        int lengthGamma = Utils.compressedPostingListToBytes(postingsList, "gamma").length;
        int lengthDelta = Utils.compressedPostingListToBytes(postingsList, "delta").length;
        System.out.println("term = " + term + ", df = " + df + ", tf = " + tfOverall +
                ", len(uncompressed) = " + lengthUncompressed +
                ", len(gamma) = " + lengthGamma +
                ", len(delta) = " + lengthDelta);
    }

    public static void printTermDocInformation(SPIMI s, String term) {
        Map<String, PostingsEntry> index = s.getInvertedIndex();
        Map<Integer, DocumentInfo> docInfo = s.getDocInfo();
        LinkedHashMap<Integer, Integer> postingsList = index.get(term).getPostingsList();
        int i = 0;
        System.out.println("Doc info for nasa documents");
        for (Map.Entry<Integer, Integer> entry : postingsList.entrySet()) {
            // only print the first 3
            if (i == 3) {
                break;
            }
            System.out.println("docId = " + entry.getKey() +
                    ", tf = " + entry.getValue() +
                    ", max_tf = " + docInfo.get(entry.getKey()).getMaxTf() +
                    ", doc_len = " + docInfo.get(entry.getKey()).getDocLen());
            i += 1;
        }
    }

    public static void printIndexTermStats(SPIMI s) {
        Map<String, PostingsEntry> index = s.getInvertedIndex();
        int maxDf = Integer.MIN_VALUE;
        int minDf = Integer.MAX_VALUE;
        String termMaxDf = "";
        List<String> termsMinDf = new ArrayList<>();
        // iterate over terms and compute max and min df
        for (String term : index.keySet()) {
            int df = index.get(term).getDocumentFrequency();
            if (df > maxDf) {
                maxDf = df;
                termMaxDf = term;
            }
            if (df < minDf) {
                minDf = df;
            }
        }
        for (String term : index.keySet()) {
            int df = index.get(term).getDocumentFrequency();
            if (df == minDf) {
                termsMinDf.add(term);
            }
        }
        System.out.println("termWithMaxDf = " + termMaxDf +
                ", termsWithMinDf = " + termsMinDf + "");
    }

    public static void printIndexDocumentStats(SPIMI s) {
        Map<Integer, DocumentInfo> docInfo = s.getDocInfo();
        int docWithMaxTf = -1;
        int docWithMaxDocLen = -1;
        int largestMaxTfSeen = 0;
        int largestDocLenSeen = 0;
        for (Map.Entry<Integer, DocumentInfo> entry : docInfo.entrySet()) {
            int currentMaxTf = entry.getValue().getMaxTf();
            int currentDocLen = entry.getValue().getDocLen();
            if (largestMaxTfSeen < currentMaxTf) {
                largestMaxTfSeen = currentMaxTf;
                docWithMaxTf = entry.getKey();
            }
            if (largestDocLenSeen < currentDocLen) {
                largestDocLenSeen = currentDocLen;
                docWithMaxDocLen = entry.getKey();
            }
        }
        System.out.println("docWithMaxTf = " + docWithMaxTf);
        System.out.println("docWithMaxDocLen = " + docWithMaxDocLen);
    }

    public static long getSizeOfIndex(String folder, String prefix) {
        // for simplicity, assuming file name contract remains
        String[] files = {prefix + ".index", prefix + ".docinfo", prefix + ".pointers"};
        long sizeInBytes = 0;
        for (String f : files) {
            File fp = Paths.get(folder, f).toFile();
            sizeInBytes += fp.length(); // returns size in bytes
        }
        return sizeInBytes;
    }

    public static void main(String[] args) throws IOException {
        boolean useStemming = false;
        String folder = "/home/siddhant/Documents/Projects/ir_homeworks/tokenizer/Cranfield";

        SPIMI index = Indexer.buildIndex(folder, useStemming);
    }
}