package it.cnr.saks.hyperion;

import it.cnr.saks.hyperion.similarity.SimilarityConfiguration;
import it.cnr.saks.hyperion.similarity.prolog.SimilarityAnalysis;
import it.cnr.saks.hyperion.symbolic.AnalyzerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.MalformedURLException;

public class SimilarityExtractionRunner {
    private static final Logger log = LoggerFactory.getLogger(SimilarityExtractionRunner.class);

    public static int runSimilarityExtraction(File configJsonFile) throws FileNotFoundException {

        // Load the configuration
        SimilarityConfiguration configurationSimilarity = null;
        try {
            configurationSimilarity = SimilarityConfiguration.loadConfiguration(configJsonFile);
        } catch (AnalyzerException | MalformedURLException e) {
            log.error("Error while loading hyperion: " + e.getMessage());
            return 64; // EX_USAGE
        }

        try {
            // Perform the similarity analysis
            SimilarityAnalysis analysis = new SimilarityAnalysis();
            analysis.loadPrologDataset(configurationSimilarity.getRegex(), configurationSimilarity.getInvokes());
            analysis.computeSimilarity();

        } catch (AnalyzerException e) {
            e.printStackTrace();
        }

        // Setup the output where to generate the final representation of the
        PrintStream out;
        if(configurationSimilarity.getOutputFile() == null) {
            out = System.out;
        } else {
            out = new PrintStream(configurationSimilarity.getOutputFile());
        }

        return 0;
    }
}
