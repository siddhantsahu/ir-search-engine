package index;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.xml.sax.SAXException;
import preprocess.Stemmer;
import preprocess.TokenFilter;
import util.ParseXMLFile;
import util.Timer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * index.Indexer handles preprocessing the collection as well as building and compressing the index
 */
public class Indexer {
    private static Stemmer stemmer = new Stemmer();
    private static Map<String, String> stemLookup = new HashMap<>();    // to cache stemmer results

    /**
     * Helper function to get stem of a word. Uses memoization to speed up the process.
     *
     * @param word the word to stem
     * @return stemmed word
     */
    public static String stemWord(String word) {
        String result = stemLookup.getOrDefault(word, stemmer.stem(word));
        stemLookup.putIfAbsent(word, result);
        return result;
    }

    /**
     * Helper function to parse a cranfield document.
     *
     * @param doc File object of the document
     * @return ParseXMLFile object of the document
     * @throws IOException
     */
    public static ParseXMLFile parseCranfieldDocument(File doc) throws IOException {
        // process one file
        SAXParserFactory factory = SAXParserFactory.newInstance();
        ParseXMLFile cranfield = new ParseXMLFile();
        try {
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(doc, cranfield);
            // annotate and process terms in text
        } catch (ParserConfigurationException e1) {
            e1.printStackTrace();
        } catch (SAXException e2) {
            e2.printStackTrace();
        }
        return cranfield;
    }

    /**
     * Utility function to build the index using lemmas or stems.
     *
     * @param folder      folder containing the documents to be indexed
     * @param useStemming whether to use stemming or not, if false, only lemmas are used to build the index
     * @return the `index.SPIMI` object used to index the collection
     * @throws IOException
     */
    public static SPIMI buildIndex(String folder, boolean useStemming) throws IOException {
        File collection = new File(folder);
        String[] files = collection.list(); // in random order
        Arrays.sort(files);

        String mode = useStemming ? "stem" : "lemma";

        SPIMI spimi = new SPIMI();

        // build pipeline for lemmatization
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        Timer timer = new Timer();

        int docId = 1;

        for (String file : files) {
            File doc = Paths.get(folder, file).toFile();
            ParseXMLFile cranfield = parseCranfieldDocument(doc);

            // annotate document
            Annotation document = new Annotation(cranfield.getTextField().toString());
            pipeline.annotate(document);

            // show output
            List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

            for (CoreMap sentence : sentences) {
                for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
                    TokenFilter tokenFilterObj = new TokenFilter(lemma);
                    for (String word : tokenFilterObj.getTokens()) {
                        // build index with stems
                        String term = word;
                        if (useStemming) {
                            term = stemWord(word);
                        }
                        spimi.invert(term, docId);
                    }
                }
            }

            docId += 1;
        }

        System.out.println(timer.end());

        return spimi;
    }

    /**
     * Compress index built using SPIMI algorithm.
     *
     * @param spimi     the index
     * @param outFolder store binary compressed files in this folder
     * @throws IOException
     */
    public static void compressIndex(SPIMI spimi, String outFolder) throws IOException {
        Compression cmp = new Compression(spimi, outFolder);
        cmp.createUncompressedIndex();

        Timer tGamma = new Timer();
        cmp.createCompressedIndex(8, "gamma", false);
        System.out.println(tGamma.end());

        Timer tDelta = new Timer();
        cmp.createCompressedIndex(8, "delta", true);
        System.out.println(tDelta.end());
    }
}
