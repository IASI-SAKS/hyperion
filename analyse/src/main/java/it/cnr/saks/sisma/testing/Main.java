package it.cnr.saks.sisma.testing;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    private static InformationLogger inspector;
    private static MethodEnumerator methodEnumerator;

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

        long startTime = System.nanoTime();

        try {
            methodEnumerator = new MethodEnumerator(testPath, SUTPath);
        } catch (IOException | AnalyzerException e) {
            System.err.println(e.getMessage());
            System.exit(66); // EX_NOINPUT
        }

        inspector = new InformationLogger(methodEnumerator);
        inspector.setJsonOutputFile("inspection.json");

        for(MethodDescriptor method: methodEnumerator) {
            inspector.setCurrMethod(method.getClassName(), method.getMethodName());
            System.out.println("\tSymbolic execution starting from: " + method.getMethodName() + ", " + method.getMethodDescriptor());

            try {
                Analyzer a = new Analyzer(inspector)
                        .withUserClasspath(prepareFinalRuntimeClasspath(SUTPath, additionalClassPath))
                        .withMethodSignature(method.getClassName().replace(".", File.separator), method.getMethodDescriptor(), method.getMethodName())
                        .withDepthScope(5);
                a.run();
            } catch (AnalyzerException e) {
                e.printStackTrace();
            }
        }

        try {
            inspector.emitJson();
        } catch (JsonProcessingException e) {
            System.err.println(e.getMessage());
        }

        long endTime = System.nanoTime();
        double duration = (double)(endTime - startTime) / 1000000000;
        System.out.println("Analyzed " + methodEnumerator.getMethodsCount() + " methods in " + duration + " seconds.");
    }

    private static String[] prepareFinalRuntimeClasspath(String SUTPath, String[] additionalClassPath) {
        URL[] urlClassPath = methodEnumerator.getClassPath();
        ArrayList<String> listClassPath = new ArrayList<>();

        listClassPath.add(SUTPath);
        for(URL u: urlClassPath) {
            listClassPath.add(u.getPath());
        }
        listClassPath.addAll(Arrays.asList(additionalClassPath));
        
        String[] classPath = new String[listClassPath.size()];
        listClassPath.toArray(classPath);

        return classPath;
    }
}
