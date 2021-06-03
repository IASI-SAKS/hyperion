package it.cnr.saks.hyperion;

import javassist.NotFoundException;
import jbse.bc.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static MethodEnumerator methodEnumerator;
    private static TestWrapper testWrapper;
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

        Signature testWrapperSignature = new Signature("it/cnr/saks/hyperion/TestWrapper", "()V", "wrapperEntryPoint");
        try {
            testWrapper = new TestWrapper(configuration.getClassPath());
        } catch (NotFoundException e) {
            e.printStackTrace();
            System.exit(70); // EX_SOFTWARE
        }

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());
        String facts = "inspection-" + nowAsISO + ".pl";

        InformationLogger inspector = new InformationLogger(configuration);
        inspector.setDatalogOutputFile(facts);

        long startTime = System.nanoTime();

        /*
         * java -cp .:../../main/java/:/home/pellegrini/.m2/repository/junit/junit/4.12/junit-4.12.jar:/home/pellegrini/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar org.junit.runner.JUnitCore Course com/fullteaching/backend/unitary/course/CourseUnitaryTest
         */

        for(MethodDescriptor method: methodEnumerator) {

            if(!configuration.getIncludeTest().contains(method.getMethodName()))
                continue;
            if(configuration.getExcludeTest().contains(method.getMethodName()))
                continue;

            inspector.setCurrMethod(method.getClassName(), method.getMethodName());
            log.info("Analysing: " + method.getMethodName() + ":" + method.getMethodDescriptor());

            Signature testProgramSignature = new Signature(method.getClassName().replace(".", File.separator), method.getMethodDescriptor(), method.getMethodName());

            try {
                Analyzer a = new Analyzer(inspector)
                        .withUserClasspath(configuration.getClassPath())
                        .withTimeout(2)
                        .withDepthScope(500);

                a.setupStatic();

                final List<MethodDescriptor> beforeMethods = methodEnumerator.getBeforeMethods(method.getClassName());
                if(beforeMethods == null) {
                    a.withMethodSignature(testProgramSignature);
                } else {
                    continue;
//                    log.info("Generating wrapper for @Before methods");
//                    testWrapper.generateWrapper(testProgramSignature, beforeMethods);
//
//                    a.withMethodSignature(testWrapperSignature);
//                    a.withMethodSignature(testWrapperSignature)
//                     .withGuided(true, testProgramSignature);
                }
                a.run();

            } catch (AnalyzerException | OutOfMemoryError | StackOverflowError e) {
                e.printStackTrace();
                inspector.emitDatalog();
                System.gc();
                continue;
            }

            inspector.emitDatalog();
            System.gc();

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
        log.info("Analyzed " + methodEnumerator.getMethodsCount() + " methods in " + duration + " seconds.");
    }
}
