package search;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import index.DocumentInfo;
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
    private StanfordCoreNLP pipeline;
    private Query query;
    private SPIMI index;
    private int collectionSize;
    private double avgDocLen;

    public QueryParser(String text, SPIMI index) {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        this.pipeline = new StanfordCoreNLP(props);
        this.query = this.parseQuery(text);
        this.index = index;
        this.collectionSize = this.index.getDocInfo().size();
        // calculate average doc length
        for (DocumentInfo d : this.index.getDocInfo().values()) {
            this.avgDocLen += d.getDocLen();
        }
        this.avgDocLen /= this.collectionSize;
    }

    /**
     * Parses the query in the same fashion as the documents.
     *
     * @param text the text of the query
     * @return a Query object
     */
    public Query parseQuery(String text) {
        Query parsedQuery = new Query();

        // annotate document
        Annotation document = new Annotation(text);
        this.pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
                TokenFilter tokenFilterObj = new TokenFilter(lemma);
                for (String word : tokenFilterObj.getTokens()) {
                    if (!SPIMI.STOPWORDS.contains(word)) {
                        parsedQuery.putWord(word);
                    }
                }
            }   // end for tokens
        }   // end for sentences

        return parsedQuery;
    }

    /**
     * A variant of the well known maxTf term weighting function.
     *
     * @param tf    term frequency of the term
     * @param maxTf maximum term frequency in document
     * @param df    document frequency
     * @return weight of the term
     */
    private double maxTfWeighting(int tf, int maxTf, int df) {
        if (tf == 0 || maxTf == 0 || df == 0) {
            return 0.0;
        }
        return (0.4 + 0.6 * Math.log(tf + 0.5) / Math.log(maxTf + 1.0)) *
                Math.log((double) (this.collectionSize / df)) / Math.log(this.collectionSize);
    }

    /**
     * A variant of the Okapi term weighting function.
     *
     * @param tf     term frequency
     * @param docLen maximum term frequency if document
     * @param df     document frequency
     * @return weight of the term
     */
    private double okapiTermWeighting(int tf, int docLen, int df) {
        if (tf == 0 || docLen == 0 || df == 0) {
            return 0.0;
        }
        return (0.4 + 0.6 * (tf / (tf + 0.5 + 1.5 *
                (docLen / this.avgDocLen))) * Math.log(this.collectionSize / df) /
                Math.log(this.collectionSize));
    }

    /**
     * Computes the term weights for all terms in the indices using the specified weighting function.
     *
     * @param weightFunction either w1 or w2
     */
    public void computeTermWeights(String weightFunction) {
        // iterate over the index, compute term weights and document length
        for (Map.Entry<String, PostingsEntry> e : this.index.getInvertedIndex().entrySet()) {
            PostingsEntry pe = e.getValue();

            for (Map.Entry<Integer, TermWeight> plEntry : pe.getPostingsList().entrySet()) {
                int docId = plEntry.getKey();
                int tf = plEntry.getValue().getTf();
                int df = pe.getDocumentFrequency();
                // compute weighted tf
                double tfWeighted = 0.0;
                if (weightFunction.equalsIgnoreCase("w1")) {
                    int maxTf = this.index.getMaxTf(docId);
                    tfWeighted = maxTfWeighting(tf, maxTf, df);
                } else {
                    int docLen = this.index.getDocLen(docId);
                    tfWeighted = okapiTermWeighting(tf, docLen, df);
                }
                // update term weight
                TermWeight tw = plEntry.getValue();
                tw.setTfWeighted(tfWeighted);
                // update l2 norm for document
                this.index.getDocInfo().get(docId).updateLenSquared(tfWeighted * tfWeighted);
            }   // end iteration over posting list
        }   // end iteration over index
    }

    /**
     * Helper function to find weight of a term in the current query
     *
     * @param term the term
     * @return weight of term in query
     */
    private double getWeightOfTermInQuery(String term) {
        int df = this.index.getDF(term);
        int maxTf = this.query.getMaxTf();
        int tf = this.query.getTf(term);
        return maxTfWeighting(tf, maxTf, df);
    }

    /**
     * Ranks the documents using the vector space model.
     *
     * @param topK           the top K documents to return
     * @param weightFunction either "w1" or "w2"
     * @return the top K documents relevant to the query
     */
    public Map<Integer, Double> vectorSpaceModel(int topK, String weightFunction) {
        this.computeTermWeights(weightFunction);
        Map<Integer, Double> scores = new HashMap<>();  // doc id is key
        double queryLengthSquared = 0.0;
        for (String term : this.query.getTerms()) {
            double wTQ = getWeightOfTermInQuery(term);
            queryLengthSquared += wTQ * wTQ;
            if (!this.index.getInvertedIndex().containsKey(term)) {
                System.out.println("Term not found in index");
                continue;
            }
            Map<Integer, TermWeight> postingList = this.index.getPostingList(term);
            for (Map.Entry<Integer, TermWeight> entry : postingList.entrySet()) {
                int docId = entry.getKey();
                double wTD = entry.getValue().getTfWeighted();
                double dot = wTD * wTQ;
                scores.computeIfPresent(docId, (k, v) -> v + dot);
                scores.putIfAbsent(docId, dot);
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

    /**
     * Computes the cosine similarity of two sparse vectors.
     *
     * @param u vector 1
     * @param v vector 2
     * @return cosine similarity, between 0 and 1.0
     */
    public double cosineScore(SparseVector u, SparseVector v) {
        return u.dotProduct(v) / u.getMagnitude() / v.getMagnitude();
    }

    /**
     * Creates a sparse vector representation, storing only non-zero entries for the query and document.
     *
     * @param docId document id
     */
    public List<SparseVector> getVectors(int docId) {
        // for simplicity, just consider all terms in dictionary
        SortedSet<String> allTerms = new TreeSet<>(this.index.getInvertedIndex().keySet());
        allTerms.addAll(this.query.getTerms());

        // query and doc vector
        List<String> labels = allTerms.stream().collect(Collectors.toList());
        SparseVector queryVector = new SparseVector(labels);
        SparseVector docVector = new SparseVector(labels);

        int i = 0;
        for (String term : allTerms) {
            double wTQ = getWeightOfTermInQuery(term);
            if (wTQ > 0.0) {
                queryVector.put(i, wTQ);
            }
            double wTD = this.index.getTFWeighted(term, docId);
            if (wTD > 0.0) {
                docVector.put(i, wTD);
            }
            i += 1;
        }

        return Arrays.asList(queryVector, docVector);
    }
}