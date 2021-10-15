package it.cnr.saks.hyperion.similarity;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.cnr.saks.hyperion.grouping.GroupingConfiguration;
import it.cnr.saks.hyperion.symbolic.AnalyzerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class SimilarTests {
    private static final Logger log = LoggerFactory.getLogger(SimilarTests.class);

    private String TP1;
    private String TP2;
    private double score;

    private SimilarTests() {}

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

    public static SimilarTests[] loadFromJson(File jsonFile) throws AnalyzerException {
        SimilarTests[] similarity;

        log.info("Loading similarity information...");

        ObjectMapper om = new ObjectMapper();
        try {
            similarity = om.readValue(jsonFile, SimilarTests[].class);
        } catch (IOException e) {
            throw new AnalyzerException("Error parsing JSON configuration file " + jsonFile.getPath() + ": " + e.getMessage());
        }

        return similarity;
    }
}
