package it.cnr.saks.hyperion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.cnr.saks.hyperion.similarity.SimilarTests;
import it.cnr.saks.hyperion.similarity.SimilarityConfiguration;
import it.cnr.saks.hyperion.similarity.prolog.SimilarityAnalysis;
import it.cnr.saks.hyperion.symbolic.AnalyzerException;
import org.jpl7.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.Map;

public class SimilarityExtractionRunnerHelper {
    private static final Logger log = LoggerFactory.getLogger(SimilarityExtractionRunnerHelper.class);

    public static int runSimilarityExtraction(File configJsonFile) throws FileNotFoundException {

        // Load the configuration
        SimilarityConfiguration configurationSimilarity = null;
        try {
            configurationSimilarity = SimilarityConfiguration.loadConfiguration(configJsonFile);
        } catch (AnalyzerException | MalformedURLException e) {
            log.error("Error while loading hyperion: " + e.getMessage());
            return 64; // EX_USAGE
        }

        SimilarTests[] results = null;

        try {
            // Perform the similarity analysis
            SimilarityAnalysis analysis = new SimilarityAnalysis();
            analysis.loadPrologDataset(configurationSimilarity.getRegex(), configurationSimilarity.getInvokes());
            results = analysis.computeSimilarity(configurationSimilarity.getMetric());

        } catch (AnalyzerException e) {
            e.printStackTrace();
        }

        // Setup the output where to generate the final representation of the
        PrintStream out;
        if(configurationSimilarity.getOutputFile() == null) {
            out = System.out;
        } else {
            out = new PrintStream(configurationSimilarity.getOutputFile());
            log.info("Writing similarity data to \"{}\"", configurationSimilarity.getOutputFile());
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
            out.println(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return 42;
        }

        return 0;
    }
}
