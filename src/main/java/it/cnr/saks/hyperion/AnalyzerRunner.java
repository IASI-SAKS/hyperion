package it.cnr.saks.hyperion;

import it.cnr.saks.hyperion.discovery.DiscoveryConfiguration;
import it.cnr.saks.hyperion.discovery.MethodDescriptor;
import it.cnr.saks.hyperion.discovery.MethodEnumerator;
import it.cnr.saks.hyperion.similarity.InformationLogger;
import it.cnr.saks.hyperion.symbolic.Analyzer;
import it.cnr.saks.hyperion.symbolic.AnalyzerException;
import jbse.bc.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class AnalyzerRunner {
    private static MethodEnumerator methodEnumerator;
    private static DiscoveryConfiguration discoveryConfiguration;
    private static final Logger log = LoggerFactory.getLogger(AnalyzerRunner.class);

    public static int runAnalyzer(File configJsonFile) {
        try {
            discoveryConfiguration = DiscoveryConfiguration.loadConfiguration(configJsonFile);
        } catch (AnalyzerException | MalformedURLException e) {
            log.error("Error while loading hyperion: " + e.getMessage());
            return 64; // EX_USAGE
        }

        try {
            methodEnumerator = new MethodEnumerator(discoveryConfiguration);
        } catch (IOException | AnalyzerException e) {
            log.error("Error while enumerating test programs: " + e.getMessage());
            return 70; // EX_SOFTWARE
        }

        String facts;
        if(discoveryConfiguration.getOutputFile() == null) {
            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
            df.setTimeZone(tz);
            String nowAsISO = df.format(new Date());
            facts = "inspection-" + nowAsISO + ".pl";
        } else {
            facts = discoveryConfiguration.getOutputFile();
        }

        InformationLogger inspector = new InformationLogger(discoveryConfiguration);
        inspector.setDatalogOutputFile(facts);

        int analyzed = 0;
        long startTime = System.nanoTime();

        for(MethodDescriptor method: methodEnumerator) {
            analyzed++;

            if(analyzed <= discoveryConfiguration.getSkip())
                continue;

            inspector.prepareForNewTestProgram(method.getClassName(), method.getMethodName());
            log.info("[{}/{}] Analysing: {}.{}:{}", analyzed, methodEnumerator.getMethodsCount(), method.getClassName(), method.getMethodName(), method.getMethodDescriptor());

            Signature testProgramSignature = new Signature(method.getClassName().replace(".", File.separator), method.getMethodDescriptor(), method.getMethodName());

            try {
                Analyzer a = new Analyzer(inspector)
                        .withUserClasspath(discoveryConfiguration.getClassPath())
                        .withTimeout(discoveryConfiguration.getTimeout())
                        .withDepthScope(discoveryConfiguration.getDepth())
                        .withJbseEntryPoint(testProgramSignature)
                        .withTestProgram(testProgramSignature);

                a.setupStatic();
                a.run();
            } catch (AnalyzerException | OutOfMemoryError | StackOverflowError e) {
                e.printStackTrace();
            } finally {
                try {
                    inspector.emitDatalog();
                } catch (AnalyzerException e) {
                    e.printStackTrace();
                }
            }
        }

        long endTime = System.nanoTime();
        double duration = (double)(endTime - startTime) / 1000000000;
        log.info("Analyzed " + analyzed + " method" + (analyzed > 1 ? "s" : "") + " in " + duration + " seconds.");

        return 0;
    }
}
