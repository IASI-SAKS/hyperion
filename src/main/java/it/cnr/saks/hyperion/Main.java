package it.cnr.saks.hyperion;

import it.cnr.saks.hyperion.discovery.Configuration;
import it.cnr.saks.hyperion.discovery.MethodDescriptor;
import it.cnr.saks.hyperion.discovery.MethodEnumerator;
import it.cnr.saks.hyperion.facts.InformationLogger;
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

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static MethodEnumerator methodEnumerator;
    private static Configuration configuration;

    public static void main(String[] args) {
        if(args.length < 1) {
            System.err.println("Need a path to the JSON config file");
            System.exit(64); // EX_USAGE
        }

        try {
            configuration = Configuration.loadConfiguration(args[0]);
        } catch (AnalyzerException | MalformedURLException e) {
            System.err.println("Error while loading hyperion: " + e.getMessage());
            System.exit(64); // EX_USAGE
        }

        try {
            methodEnumerator = new MethodEnumerator(configuration);
        } catch (IOException | AnalyzerException e) {
            System.err.println("Error while enumerating test programs: " + e.getMessage());
            System.exit(70); // EX_SOFTWARE
        }

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());
        String facts = "inspection-" + nowAsISO + ".pl";

        InformationLogger inspector = new InformationLogger(configuration);
        inspector.setDatalogOutputFile(facts);

        int analyzed = 0;
        long startTime = System.nanoTime();

        for(MethodDescriptor method: methodEnumerator) {
            analyzed++;

            inspector.prepareForNewTestProgram(method.getClassName(), method.getMethodName());
            log.info("[{}/{}] Analysing: {}.{}:{}", analyzed, methodEnumerator.getMethodsCount(), method.getClassName(), method.getMethodName(), method.getMethodDescriptor());

            Signature testProgramSignature = new Signature(method.getClassName().replace(".", File.separator), method.getMethodDescriptor(), method.getMethodName());

            try {
                Analyzer a = new Analyzer(inspector)
                        .withUserClasspath(configuration.getClassPath())
                        .withTimeout(configuration.getTimeout())
                        .withDepthScope(configuration.getDepth())
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

//        try {
//            PrologQuery.init();
//            PrologQuery.load(facts);
//        } catch (AnalyzerException e) {
//            System.err.println(e.getMessage());
//            System.exit(1);
//        }

        long endTime = System.nanoTime();
        double duration = (double)(endTime - startTime) / 1000000000;
        log.info("Analyzed " + analyzed + " method" + (analyzed > 1 ? "s" : "") + " in " + duration + " seconds.");
    }
}
