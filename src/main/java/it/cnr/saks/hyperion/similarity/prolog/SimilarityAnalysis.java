package it.cnr.saks.hyperion.similarity.prolog;

import it.cnr.saks.hyperion.similarity.SimilarTests;
import it.cnr.saks.hyperion.similarity.SimilarityException;
import org.jpl7.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SimilarityAnalysis {
    private static final Logger log = LoggerFactory.getLogger(SimilarityAnalysis.class);

    private String readAll(BufferedReader br) {
        StringBuilder buffer = new StringBuilder();
        while (true) {
            String line;
            try {
                line = br.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (line == null) {
                break;
            } else {
                buffer.append(line);
                buffer.append("\n");
            }
        }
        return buffer.toString();
    }

    private String toTempFile(BufferedReader br) throws SimilarityException {
        try {
            String content = readAll(br);
            File temp = Files.createTempFile("", ".hyperion").toFile();
            FileWriter fw = new FileWriter(temp);
            fw.write(content);
            fw.close();
            return temp.getPath();
        } catch (IOException e) {
            throw new SimilarityException(e.getMessage());
        }
    }

    public SimilarityAnalysis() throws SimilarityException {
        PrologQueryHelper.init();

        BufferedReader prologProgramReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(SimilarityAnalysis.class.getResourceAsStream("/prolog/similarity_relations.pl"))));
        PrologQueryHelper.load(toTempFile(prologProgramReader));
        prologProgramReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(SimilarityAnalysis.class.getResourceAsStream("/prolog/testing_similarity_relations.pl"))));
        PrologQueryHelper.load(toTempFile(prologProgramReader));
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
