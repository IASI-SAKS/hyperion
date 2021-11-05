package it.cnr.saks.hyperion.grouping;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.cnr.saks.hyperion.symbolic.AnalyzerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GroupingConfiguration {
    private static final Logger log = LoggerFactory.getLogger(GroupingConfiguration.class);
    private List<String> testPrograms;
    private String similarityFile;
    private String allTestProgramsFile;
    private String policy;
    private double threshold;
    private String outputFile;

    private GroupingConfiguration() {}

    public static GroupingConfiguration loadConfiguration(File jsonFile) throws AnalyzerException {
        GroupingConfiguration similarityConfiguration;

        log.info("Loading configuration...");
        ObjectMapper om = new ObjectMapper();
        try {
            similarityConfiguration = om.readValue(jsonFile, GroupingConfiguration.class);
        } catch (IOException e) {
            throw new AnalyzerException("Error parsing JSON configuration file " + jsonFile.getPath() + ": " + e.getMessage());
        }

        return similarityConfiguration;
    }

    public String getSimilarityFile() {
        return similarityFile;
    }

    public void setSimilarityFile(String similarityFile) {
        this.similarityFile = similarityFile;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public List<String> getTestPrograms() {
        return testPrograms;
    }

    public void setTestPrograms(List<String> testPrograms) {
        this.testPrograms = testPrograms;
    }

    public String getAllTestProgramsFile() {
        return allTestProgramsFile;
    }

    public void setAllTestProgramsFile(String allTestProgramsFile) {
        this.allTestProgramsFile = allTestProgramsFile;
    }
}
