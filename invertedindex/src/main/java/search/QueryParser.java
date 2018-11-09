package search;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import index.PostingsEntry;
import index.SPIMI;
import index.TermWeight;
import preprocess.TokenFilter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A query parser, to convert the query to a vector and compute weights.
 */
public class QueryParser {
    private Properties props;
    private StanfordCoreNLP pipeline;
    private String query;
    private SPIMI index;
    private int collectionSize;

    public QueryParser(String query, SPIMI index) {
        props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        pipeline = new StanfordCoreNLP(props);
        this.query = query;
        this.index = index;
        this.collectionSize = this.index.getDocInfo().size();
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

    public double maxTfWeighting(int tf, int maxTf, int df) {
        if (tf == 0 || maxTf == 0 || df == 0) {
            return 0.0;
        }
        return (0.4 + 0.6 * Math.log(tf + 0.5) / Math.log(maxTf + 1.0)) *
                Math.log((double) (this.collectionSize / df)) / Math.log(this.collectionSize);
    }

    public void computeTermWeights() {
        // iterate over the index, compute term weights and document length
        for (Map.Entry<String, PostingsEntry> e : this.index.getInvertedIndex().entrySet()) {
            String term = e.getKey();
            PostingsEntry pe = e.getValue();

            for (Map.Entry<Integer, TermWeight> plEntry : pe.getPostingsList().entrySet()) {
                int docId = plEntry.getKey();
                int maxTf = this.index.getDocInfo().get(docId).getMaxTf();
                int tf = plEntry.getValue().getTf();
                // compute weighted tf
                double tfWeighted = maxTfWeighting(tf, maxTf, pe.getDocumentFrequency());
                // update term weight
                TermWeight tw = plEntry.getValue();
                tw.setTfWeighted(tfWeighted);
                // update l2 norm for document
                this.index.getDocInfo().get(docId).updateLenSquared(tfWeighted * tfWeighted);
            }   // end iteration over posting list
        }   // end iteration over index
    }

    private double getWeightOfTermInQuery(String term, Map<String, Integer> query) {
        int df = this.index.getDF(term);
        int maxTf = Collections.max(query.values());
        int tf = query.get(term);
        return maxTfWeighting(tf, maxTf, df);
    }

    public Map<Integer, Double> vectorSpaceModel(Map<String, Integer> query, int topK) {
        this.computeTermWeights();
        Map<Integer, Double> scores = new HashMap<>();  // doc id is key
        double queryLengthSquared = 0.0;
        for (String term : query.keySet()) {
            double wTQ = getWeightOfTermInQuery(term, query);
            queryLengthSquared += wTQ * wTQ;
            for (Map.Entry<Integer, TermWeight> entry : this.index.getPostingList(term).entrySet()) {
                int docId = entry.getKey();
                double wTD = entry.getValue().getTfWeighted();
                scores.putIfAbsent(docId, wTD * wTQ);
                scores.computeIfPresent(docId, (k, v) -> v + wTD * wTQ);
            }
        }

        double queryLength = Math.sqrt(queryLengthSquared);
        // normalize scores by length
        for (Integer docId : scores.keySet()) {
            scores.computeIfPresent(docId,
                    (k, v) -> v / Math.sqrt(this.index.getDocInfo().get(docId).getWeightedDocLenSquared()) /
                            queryLength);
        }

        // return top k
        return scores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(topK)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }
}