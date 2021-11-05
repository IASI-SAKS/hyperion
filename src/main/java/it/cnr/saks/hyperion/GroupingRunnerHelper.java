package it.cnr.saks.hyperion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.cnr.saks.hyperion.discovery.MethodDescriptor;
import it.cnr.saks.hyperion.grouping.Grouping;
import it.cnr.saks.hyperion.grouping.GroupingConfiguration;
import it.cnr.saks.hyperion.grouping.GroupingException;
import it.cnr.saks.hyperion.grouping.TestGroup;
import it.cnr.saks.hyperion.similarity.SimilarTests;
import it.cnr.saks.hyperion.symbolic.AnalyzerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public class GroupingRunnerHelper {
    private static final Logger log = LoggerFactory.getLogger(GroupingRunnerHelper.class);

    public static int runGrouping(File configJsonFile) throws FileNotFoundException {
        GroupingConfiguration groupingConfiguration = null;
        SimilarTests[] similarTests = null;
        MethodDescriptor[] allTests = null;
        TestGroup group = null;

        try {
            groupingConfiguration = GroupingConfiguration.loadConfiguration(configJsonFile);
        } catch (AnalyzerException e) {
            log.error("Error while loading configuration: " + e.getMessage());
            return 64; // EX_USAGE
        }

        try {
            similarTests = SimilarTests.loadFromJson(new File(groupingConfiguration.getSimilarityFile()));
        } catch (AnalyzerException e) {
            log.error("Error while loading similarity data: " + e.getMessage());
            return 65; // EX_DATAERR
        }

        ObjectMapper om = new ObjectMapper();
        try {
            allTests = om.readValue(new File(groupingConfiguration.getAllTestProgramsFile()), MethodDescriptor[].class);
        } catch (IOException e) {
            log.error("Error parsing JSON file " + groupingConfiguration.getAllTestProgramsFile() + ": " + e.getMessage());
            return 65; // EX_DATAERR
        }

        try {
            Grouping g = new Grouping(groupingConfiguration.getPolicy());
            group = g.groupTests(allTests, similarTests, groupingConfiguration.getThreshold());
        } catch (GroupingException e) {
            log.error("Error while loading configuration: " + e.getMessage());
            return 64; // EX_USAGE
        }

        // Setup the output where to generate the final representation of the
        PrintStream out;
        if(groupingConfiguration.getOutputFile() == null) {
            out = System.out;
        } else {
            out = new PrintStream(groupingConfiguration.getOutputFile());
            log.info("Writing test group data to \"{}\"", groupingConfiguration.getOutputFile());
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        try {
            String json = objectMapper.writer(prettyPrinter).writeValueAsString(group);
            out.println(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return 78; // EX_CONFIG
        }

        return 0;
    }


}
