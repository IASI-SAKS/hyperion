package it.cnr.saks.hyperion.grouping;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.cnr.saks.hyperion.discovery.MethodDescriptor;
import it.cnr.saks.hyperion.symbolic.AnalyzerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GroupingConfiguration {
    private static final Logger log = LoggerFactory.getLogger(GroupingConfiguration.class);
    private List<String> testPrograms;
    private List<String> invokes;
    private String similarityFile;
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

    public List<String> getInvokes() {
        return invokes;
    }

    public void setInvokes(List<String> invokes) {
        this.invokes = invokes;
    }

    public MethodDescriptor[] getAllTests() {
        List<MethodDescriptor> methods = new ArrayList<>();

        // Load all invokes facts
        for(String invokesFile : this.invokes) {
            try {
                Scanner scanner = new Scanner(new File(invokesFile));
                while (scanner.hasNextLine()) {
                    String methodFromInvokes = scanner.nextLine().split(",")[0];
                    methodFromInvokes = methodFromInvokes.replace("invokes(", "");
                    methodFromInvokes = methodFromInvokes.replaceAll("'", "");
                    String[] methodAndClass = methodFromInvokes.split(":");
                    MethodDescriptor methodDescriptor = new MethodDescriptor(methodAndClass[1], "()V", methodAndClass[0]);
                    if(!methods.contains(methodDescriptor))
                        methods.add(methodDescriptor);
                }
                scanner.close();
            } catch(FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return methods.toArray(new MethodDescriptor[0]);
    }
}
