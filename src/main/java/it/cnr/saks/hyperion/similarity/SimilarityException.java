package it.cnr.saks.hyperion.similarity;

public class SimilarityException extends Exception {

    public SimilarityException(String s) { super(s); }

    public SimilarityException(Exception e) { super(e); }

    private static final long serialVersionUID = -2598719735414588195L;
}