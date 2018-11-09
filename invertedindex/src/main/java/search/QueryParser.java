package search;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import index.PostingsEntry;
import index.SPIMI;
import preprocess.TokenFilter;

import java.util.*;

import static java.util.Collections.max;

/**
 * A query parser, to convert the query to a vector and compute weights.
 */
public class QueryParser {
    private Properties props;
    private StanfordCoreNLP pipeline;
    private String query;

    public QueryParser(String query) {
        props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        pipeline = new StanfordCoreNLP(props);
        this.query = query;
    }

    public Map<String, Integer> parseQuery() {
        Map<String, Integer> queryTokens = new TreeMap<>();

        // annotate document
        Annotation document = new Annotation(query);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
                TokenFilter tokenFilterObj = new TokenFilter(lemma);
                for (String word : tokenFilterObj.getTokens()) {
                    if (!SPIMI.STOPWORDS.contains(word)) {
                        queryTokens.putIfAbsent(word, 1);
                        queryTokens.computeIfPresent(word, (k, v) -> v + 1);
                    }
                }
            }   // end for tokens
        }   // end for sentences

        return queryTokens;
    }

    public double maxTfWeighting(int tf, int maxTf, int collectionSize, int df) {
        return (0.4 + 0.6 * Math.log(tf + 0.5) / Math.log(maxTf + 1.0)) *
                Math.log((double) (collectionSize / df)) / Math.log(collectionSize);
    }
}