import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class Indexer {
    private static Stemmer stemmer = new Stemmer();
    private static Map<String, String> stemLookup = new HashMap<>();    // to cache stemmer results

    private static String stemWord(String word) {
        String result = stemLookup.getOrDefault(word, stemmer.stem(word));
        stemLookup.putIfAbsent(word, result);
        return result;
    }

    public static SPIMI buildIndex(String folder, String outFolder, boolean useStemming) throws IOException {
        File collection = new File(folder);
        String[] files = collection.list(); // in random order
        Arrays.sort(files);

        SPIMI spimi = new SPIMI(outFolder);

        // build pipeline for lemmatization
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        int docId = 1;

        for (String file : files) {
            File doc = new File(Paths.get(folder, file).toString());
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

        spimi.createUncompressedIndex();
        spimi.createCompressedIndex(8, "gamma", false);
        spimi.createCompressedIndex(8, "delta", true);

        return spimi;
    }
}
