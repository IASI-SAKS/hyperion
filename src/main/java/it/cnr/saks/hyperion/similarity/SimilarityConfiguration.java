package it.cnr.saks.hyperion.similarity;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.cnr.saks.hyperion.symbolic.AnalyzerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

public class SimilarityConfiguration {
    private static final Logger log = LoggerFactory.getLogger(SimilarityConfiguration.class);
    private List<String> invokes;
    private String regex;
    private String outputFile;
    private String metric;
    private String domain;

    private SimilarityConfiguration() {}

    public static SimilarityConfiguration loadConfiguration(File jsonFile) throws AnalyzerException, MalformedURLException {
        SimilarityConfiguration similarityConfiguration;

        log.info("Loading configuration...");
        ObjectMapper om = new ObjectMapper();
        try {
            similarityConfiguration = om.readValue(jsonFile, SimilarityConfiguration.class);
        } catch (IOException e) {
            throw new AnalyzerException("Error parsing JSON configuration file " + jsonFile.getPath() + ": " + e.getMessage());
        }


        return similarityConfiguration;
    }

    public List<String> getInvokes() {
        return invokes;
    }

    public void setInvokes(List<String> invokes) {
        this.invokes = invokes;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
