package it.cnr.saks.hyperion;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "hyperion", mixinStandardHelpOptions = true, version = "hyperion 1.0",
        description = "Analyzer and Orchestrator of Test Programs.")
public class Hyperion implements Callable<Integer> {
    private static final Logger log = LoggerFactory.getLogger(Hyperion.class);

    @Option(names = { "-a", "--analyze" }, paramLabel = "CONF_FILE", description = "The JSON file to configure the analysis")
    File analyzeJson;

    @Option(names = { "-c", "--coverage" }, paramLabel = "CONF_FILE", description = "The JSON file to configure the coverage analysis")
    File coverageJson;

    @Override
    public Integer call() throws Exception {
        int ret = 64; // EX_USAGE

        // Switch on the possible incarnations of the Hyperion tool
        if(this.analyzeJson != null)
            ret = AnalyzerRunner.runAnalyzer(this.analyzeJson);
        if(this.coverageJson != null)
            ret = 42; // STUB!

        return ret;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Hyperion()).execute(args);
        System.exit(exitCode);

//        try {
//            PrologQuery.init();
//            PrologQuery.load(facts);
//        } catch (AnalyzerException e) {
//            System.err.println(e.getMessage());
//            System.exit(1);
//        }

    }
}
