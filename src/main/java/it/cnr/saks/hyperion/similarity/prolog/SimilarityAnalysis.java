package it.cnr.saks.hyperion.similarity.prolog;

import it.cnr.saks.hyperion.similarity.SimilarityConfiguration;
import it.cnr.saks.hyperion.symbolic.AnalyzerException;
import org.jpl7.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SimilarityAnalysis {
    private static final Logger log = LoggerFactory.getLogger(SimilarityAnalysis.class);

    public SimilarityAnalysis() throws AnalyzerException {
        PrologQuery.init();

        String prologProgram = Objects.requireNonNull(SimilarityAnalysis.class.getResource("/prolog/similarity_relations.pl")).getPath();
        PrologQuery.load(prologProgram);
        prologProgram = Objects.requireNonNull(SimilarityAnalysis.class.getResource("/prolog/testing_similarity_relations.pl")).getPath();
        PrologQuery.load(prologProgram);
    }

    public void generateEndpoints(String pathToRegexFile, List<String> invokesList) throws AnalyzerException {

        log.info("Loading REGEX file for endpoint generation: " + pathToRegexFile);
        PrologQuery.load(pathToRegexFile);

        for(String invokes: invokesList) {
            log.info("Loading " + invokes);
            PrologQuery.load(invokes);
        }
    }

    public void computeSimilarity() {
        log.info("Running similarity analysis...");

        PrologQuery.query("generate_and_assert_elems", "invokes", "trace");
        String[] variables = {"TP1", "TP2", "Es1", "Es2", "Score"};
        Map<String, Term>[] similarity = PrologQuery.query("gimme_a_try", variables, "invokes", "trace", "nonemptyEqSet");
    }
}
