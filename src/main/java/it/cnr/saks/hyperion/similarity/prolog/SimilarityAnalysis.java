package it.cnr.saks.hyperion.similarity.prolog;

import it.cnr.saks.hyperion.similarity.SimilarTests;
import it.cnr.saks.hyperion.similarity.SimilarityException;
import it.cnr.saks.hyperion.symbolic.AnalyzerException;
import org.jpl7.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SimilarityAnalysis {
    private static final Logger log = LoggerFactory.getLogger(SimilarityAnalysis.class);

    public SimilarityAnalysis() throws SimilarityException {
        PrologQueryHelper.init();

        String prologProgram = Objects.requireNonNull(SimilarityAnalysis.class.getResource("/prolog/similarity_relations.pl")).getPath();
        PrologQueryHelper.load(prologProgram);
        prologProgram = Objects.requireNonNull(SimilarityAnalysis.class.getResource("/prolog/testing_similarity_relations.pl")).getPath();
        PrologQueryHelper.load(prologProgram);
    }

    public void loadPrologDataset(String pathToRegexFile, List<String> invokesList) throws SimilarityException {

        log.info("Loading REGEX file for endpoint generation: " + pathToRegexFile);
        PrologQueryHelper.load(pathToRegexFile);

        for(String invokes: invokesList) {
            log.info("Loading " + invokes);
            PrologQueryHelper.load(invokes);
        }
    }

    public SimilarTests[] computeSimilarity(String metric, String domain) {
        log.info("Running similarity analysis...");
        String[] variables = {"TP1", "TP2", "Score"};
        Map<String, Term>[] queryResults = PrologQueryHelper.query("compute_similarity_from_java", variables, domain, "trace", metric);
        System.out.println("");

        // Converto to a JSON-able object
        ArrayList<SimilarTests> results = new ArrayList<>();
        for(Map<String, Term> similarity: queryResults) {
            String TP1 = similarity.get("TP1").name();
            String TP2 = similarity.get("TP2").name();
            double score = similarity.get("Score").doubleValue();
            results.add(new SimilarTests(TP1, TP2, score));
        }

        return results.toArray(new SimilarTests[0]);
    }
}
