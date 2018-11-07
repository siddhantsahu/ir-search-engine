package search;

public class Pair implements Comparable<Pair> {
    private double score;
    private double l2NormSquared;
    private double norm;

    public Pair(double score, double len) {
        this.score = score;
        this.l2NormSquared = len;
    }

    public double getNorm() {
        return norm;
    }

    public Pair update(double score, double len) {
        // add score and length to current
        this.score += score;
        this.l2NormSquared += len;
        return this;
    }

    public void normalize(double l) {
        this.norm = this.score / Math.sqrt(this.l2NormSquared) / l;
    }

    @Override
    public int compareTo(Pair p) {
        if (p.getNorm() == this.norm) return 0;
        return p.getNorm() > this.norm ? -1 : 1;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "norm=" + norm +
                '}';
    }
}
