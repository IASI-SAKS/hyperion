package it.cnr.saks.hyperion.similarity;

public class SimilarTests {
    private String TP1;
    private String TP2;
    private double score;

    public SimilarTests(String TP1, String TP2, double score) {
        this.TP1 = TP1;
        this.TP2 = TP2;
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getTP1() {
        return TP1;
    }

    public void setTP1(String TP1) {
        this.TP1 = TP1;
    }

    public String getTP2() {
        return TP2;
    }

    public void setTP2(String TP2) {
        this.TP2 = TP2;
    }
}
