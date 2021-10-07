package it.cnr.saks.hyperion.similarity.prolog;

import it.cnr.saks.hyperion.symbolic.AnalyzerException;
import org.jpl7.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SimilarityAnalysis {
    private static final Logger log = LoggerFactory.getLogger(SimilarityAnalysis.class);
    private Map<String, Term>[] similarity;

    public SimilarityAnalysis() throws AnalyzerException {
        PrologQueryHelper.init();

        String prologProgram = Objects.requireNonNull(SimilarityAnalysis.class.getResource("/prolog/similarity_relations.pl")).getPath();
        PrologQueryHelper.load(prologProgram);
        prologProgram = Objects.requireNonNull(SimilarityAnalysis.class.getResource("/prolog/testing_similarity_relations.pl")).getPath();
        PrologQueryHelper.load(prologProgram);
    }

    public void loadPrologDataset(String pathToRegexFile, List<String> invokesList) throws AnalyzerException {

        log.info("Loading REGEX file for endpoint generation: " + pathToRegexFile);
        PrologQueryHelper.load(pathToRegexFile);

        for(String invokes: invokesList) {
            log.info("Loading " + invokes);
            PrologQueryHelper.load(invokes);
        }
    }

    public void computeSimilarity(String metric) {
        log.info("Running similarity analysis...");
        String[] variables = {"TP1", "TP2", "Score"};
        this.similarity = PrologQueryHelper.query("compute_similarity_from_java", variables, "invokes", "trace", metric);
    }
}
