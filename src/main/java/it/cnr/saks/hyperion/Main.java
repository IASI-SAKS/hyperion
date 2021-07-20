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
//    private static final Signature hyperionLauncherEntryPoint = new Signature("it/cnr/saks/hyperion/symbolic/TestLauncher", "([Ljava/lang/String;)V", "main");
//    private static final Signature hyperionLauncherEntryPoint = new Signature("org/junit/runner/JUnitCore", "(Lorg/junit/runner/Runner;)Lorg/junit/runner/Result;", "run");
    private static final Signature hyperionLauncherEntryPoint = new Signature("org/junit/runners/ParentRunner", "(Lorg/junit/runner/notification/RunNotifier;)V", "run");

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

            inspector.setCurrMethod(method.getClassName(), method.getMethodName());
            log.info("[{}/{}] Analysing: {}:{}", analyzed, methodEnumerator.getMethodsCount(), method.getMethodName(), method.getMethodDescriptor());

            Signature testProgramSignature = new Signature(method.getClassName().replace(".", File.separator), method.getMethodDescriptor(), method.getMethodName());

            try {
                Analyzer a = new Analyzer(inspector)
                        .withUserClasspath(configuration.getClassPath())
                        .withTimeout(15)
                        .withDepthScope(500)
                        .withJbseEntryPoint(testProgramSignature)
                        .withTestProgram(testProgramSignature);

                a.setupStatic();

//                wtf(a.analizerParameters, configuration);
//                System.exit(0);

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
        log.info("Analyzed " + analyzed + " method" + (analyzed > 1 ? "s" : "") + " in " + duration + " seconds.");
    }
/*
    private static void wtf(AnalizerParameters analizerParameters, Configuration configuration) {
        final RunParameters p = new RunParameters();

        p.setJBSELibPath("/home/pellegrini/git/CNR/hyperion/target/classes/");
        p.addUserClasspath(gimmeClasspath(configuration));
        p.addSourcePath(Paths.get(System.getProperty("java.home", "") + "src.zip"));
        p.setMethodSignature(analizerParameters.getTestProgramSignature().getClassName(), analizerParameters.getTestProgramSignature().getDescriptor(), analizerParameters.getTestProgramSignature().getName());
        p.setOutputFilePath("fuuuuuu");
        p.setDecisionProcedureType(RunParameters.DecisionProcedureType.Z3);
        p.setExternalDecisionProcedurePath("z3");
        p.setStateFormatMode(RunParameters.StateFormatMode.TEXT);
        p.setStepShowMode(RunParameters.StepShowMode.METHOD);
        p.setGuided(analizerParameters.getTestProgramSignature().getClassName(), analizerParameters.getTestProgramSignature().getName());
        p.setGuidanceType(RunParameters.GuidanceType.JDI);

        final Run r = new Run(p);
        r.run();
    }

    private static String[] gimmeClasspath(Configuration configuration) {
        List<String> daList = new ArrayList<>();

        for (String p: configuration.getTestPrograms()) {
            daList.add(new File(p).getAbsolutePath());
        }
        for (String p: configuration.getSut()) {
            daList.add(new File(p).getAbsolutePath());
        }
        for (String p: configuration.getAdditionalClasspath()) {
            daList.add(new File(p).getAbsolutePath());
        }
        daList.add(new File("data/jre/rt.jar").getAbsolutePath());

        String runtimeClasspath = ManagementFactory.getRuntimeMXBean().getClassPath();
        String separator = System.getProperty("path.separator");
        String[] additionalClasspath = runtimeClasspath.split(separator);

        for (String p: additionalClasspath) {
            daList.add(new File(p).getAbsolutePath());
        }

        String[] arr = new String[daList.size()];
        daList.toArray(arr);
        return arr;
    }*/
}
