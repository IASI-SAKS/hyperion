package it.cnr.saks.hyperion;

import javassist.NotFoundException;
import jbse.bc.Signature;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    private static MethodEnumerator methodEnumerator;
    private static TestWrapper testWrapper;

    public static void main(String[] args) {
        final String testPath = args.length > 0 ? args[0] : null;
        final String SUTPath = args.length > 1 ? args[1] : null;

        String[] additionalClassPath = null;
        if(args.length > 2) {
            additionalClassPath = Arrays.copyOfRange(args, 2, args.length);
        }

        if(testPath == null || SUTPath == null) {
            System.out.println("Need paths");
            System.exit(64); // EX_USAGE
        }

        try {
            methodEnumerator = new MethodEnumerator(testPath, SUTPath);
        } catch (IOException | AnalyzerException e) {
            System.err.println(e.getMessage());
            System.exit(66); // EX_NOINPUT
        }

        String[] runtimeClasspath = null;
        try {
            runtimeClasspath = prepareFinalRuntimeClasspath(SUTPath, additionalClassPath);
        } catch (AnalyzerException e) {
            e.printStackTrace();
            System.exit(70); // EX_SOFTWARE
        }

        Signature testWrapperSignature = new Signature("it/cnr/saks/hyperion/TestWrapper", "()V", "wrapperEntryPoint");
        try {
            testWrapper = new TestWrapper(runtimeClasspath);
        } catch (NotFoundException e) {
            e.printStackTrace();
            System.exit(70); // EX_SOFTWARE
        }

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());
        String facts = "inspection-" + nowAsISO + ".pl";

        InformationLogger inspector = new InformationLogger(methodEnumerator);
        inspector.setDatalogOutputFile(facts);

        int count = 0;

        long startTime = System.nanoTime();

        for(MethodDescriptor method: methodEnumerator) {
            inspector.setCurrMethod(method.getClassName(), method.getMethodName());
            System.out.println("Analysing: " + method.getMethodName() + ":" + method.getMethodDescriptor());

            Signature testProgramSignature = new Signature(method.getClassName().replace(".", File.separator), method.getMethodDescriptor(), method.getMethodName());

            try {
                Analyzer a = new Analyzer(inspector)
                        .withUserClasspath(runtimeClasspath)
                        .withDepthScope(60);

                a.setupStatic();

                final List<MethodDescriptor> beforeMethods = methodEnumerator.getBeforeMethods(method.getClassName());
                if(beforeMethods == null) {
                    a.withMethodSignature(testProgramSignature);
                } else {
                    System.out.println("Generating wrapper for @Before methods");
                    testWrapper.generateWrapper(testProgramSignature, beforeMethods);

//                    a.withMethodSignature(testWrapperSignature);
                    a.withMethodSignature(testWrapperSignature)
                     .withGuided(true, testProgramSignature);
                }
                a.run();

            } catch (AnalyzerException e) {
                e.printStackTrace();
                return;
            }

            inspector.emitDatalog();
            System.gc();

            if(++count == 1) // TODO: remove after debugging
                break;
        }

        try {
            PrologQuery.init();
            PrologQuery.load(facts);
        } catch (AnalyzerException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        long endTime = System.nanoTime();
        double duration = (double)(endTime - startTime) / 1000000000;
        System.out.println("Analyzed " + methodEnumerator.getMethodsCount() + " methods in " + duration + " seconds.");
    }

    private static String[] prepareFinalRuntimeClasspath(String SUTPath, String[] additionalClassPath) throws AnalyzerException {
        URL[] urlClassPath = methodEnumerator.getClassPath();
        HashSet<String> listClassPath = new HashSet<>();

        listClassPath.add("./target/classes/"); // TODO: make it general, it's used to locate the written wrapper class
        listClassPath.add(SUTPath);
        for (URL u : urlClassPath) {
            listClassPath.add(u.getPath());
        }
        if(additionalClassPath != null)
            listClassPath.addAll(Arrays.asList(additionalClassPath));

        // Add the path of this actual program
        String systemCP = System.getProperty("java.class.path");
        String[] classpathEntries = systemCP.split(File.pathSeparator);
        listClassPath.addAll(Arrays.asList(classpathEntries));

        String[] classPath = new String[listClassPath.size()];
        listClassPath.toArray(classPath);
        Arrays.sort(classPath);

        // Sanity check
        for (String path: classPath) {
//            System.out.println(path);
            if(!Files.exists(Paths.get(path))) {
                throw new AnalyzerException("The path " + path + " does not exist");
            }
        }

        return classPath;
    }
}
