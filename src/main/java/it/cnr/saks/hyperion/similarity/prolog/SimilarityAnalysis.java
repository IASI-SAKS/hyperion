package it.cnr.saks.hyperion.similarity.prolog;

import it.cnr.saks.hyperion.similarity.SimilarityConfiguration;
import it.cnr.saks.hyperion.symbolic.AnalyzerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class SimilarityAnalysis {
    private static final Logger log = LoggerFactory.getLogger(SimilarityAnalysis.class);

    public SimilarityAnalysis() throws AnalyzerException {
        PrologQuery.init();

        String prologProgram = Objects.requireNonNull(SimilarityConfiguration.class.getResource("/prolog/similarity_relations.pl")).getPath();
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
}
