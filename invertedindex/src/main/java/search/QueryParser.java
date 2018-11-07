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

    /**
     * Compute cosine scores of query with every document.
     *
     * @param query list of tokens in the query
     */
    public void cosineScore(Map<String, Integer> query, SPIMI index) {
        int N = index.getDocInfo().size();  // collection size (in number of documents)
        Map<Integer, Pair> scores = new HashMap<>();
        double squaredL2NormQuery = 0.0;
        double queryTermWeight;   // non-zero only if term found in collection

        int queryMaxTf = max(query.values());

        for (Map.Entry<String, Integer> queryEntry : query.entrySet()) {
            // calculate query term weight if term exists in collection
            if (index.getInvertedIndex().containsKey(queryEntry.getKey())) {
                PostingsEntry pe = index.getInvertedIndex().get(queryEntry.getKey());
                int df = pe.getDocumentFrequency();
                int queryTermTf = queryEntry.getValue();
                queryTermWeight = maxTfWeighting(queryTermTf, queryMaxTf, N, df);
                squaredL2NormQuery += queryTermWeight * queryTermWeight;

                // for each (d, tf) pair in the postings list
                for (Map.Entry<Integer, Integer> entry : pe.getPostingsList().entrySet()) {
                    int tf = entry.getValue();
                    int docId = entry.getKey();
                    int maxTf = index.getDocInfo().get(docId).getMaxTf();
                    double docWeight = maxTfWeighting(tf, maxTf, N, df);
                    double delScore = docWeight * queryTermWeight;
                    scores.putIfAbsent(docId, new Pair(delScore, docWeight * docWeight));
                    scores.computeIfPresent(docId, (k, v) -> v.update(delScore, docWeight * docWeight));
                }   // end for (d, tf) pair
            } else {
                queryTermWeight = 0.0;
            }
        }   // end for query term

        double l2NormQuery = Math.sqrt(squaredL2NormQuery);
        // normalize scores
        for (Pair p : scores.values()) {
            p.normalize(l2NormQuery);
        }

        // print scores in descending order, sort by value using java 8 stream
        scores.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(System.out::println);
    }
}