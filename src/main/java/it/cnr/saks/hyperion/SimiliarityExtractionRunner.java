package it.cnr.saks.hyperion;

import it.cnr.saks.hyperion.similarity.SimilarityConfiguration;
import it.cnr.saks.hyperion.similarity.prolog.SimilarityAnalysis;
import it.cnr.saks.hyperion.symbolic.AnalyzerException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.MalformedURLException;

public class SimiliarityExtractionRunner {

    public static int runSimilarityExtraction(File configJsonFile, File output) throws FileNotFoundException {

        // Setup the output where to generate the final representation of the
        PrintStream out;
        if(output == null) {
            out = System.out;
        } else {
            out = new PrintStream(output);
        }

        try {
            // Load the configuration
            SimilarityConfiguration configuration = SimilarityConfiguration.loadConfiguration(configJsonFile);

            // Perform the similarity analysis
            SimilarityAnalysis analysis = new SimilarityAnalysis();
            analysis.generateEndpoints(configuration.getRegex(), configuration.getInvokes());

        } catch (AnalyzerException | MalformedURLException e) {
            e.printStackTrace();
        }

        return 0;
    }
}
