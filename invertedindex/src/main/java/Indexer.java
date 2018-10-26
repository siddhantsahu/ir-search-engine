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
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class Indexer {

    public static void main(String args[]) throws IOException {
        String folder = "/home/siddhant/Documents/Projects/ir_homeworks/tokenizer/Cranfield";
        File collection = new File(folder);
        File[] files = collection.listFiles();

        SPIMI spimi = new SPIMI();

        // build pipeline for lemmatization
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        Set<String> terms = new TreeSet<>();

        int docId = 1;

        for (File doc : files) {
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
                    for (String l : tokenFilterObj.getTokens()) {
                        spimi.invert(l, docId);
                    }
                }
            }

            docId += 1;
        }

        spimi.createUncompressedIndex();
        spimi.createCompressedIndex(8);
    }
}
