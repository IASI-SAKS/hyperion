package it.cnr.saks.sisma.testing;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
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
                        .withDepthScope(5)
                        .withUninterpreted("org/springframework/util/Assert", "(Z)V", "state")
                        .withUninterpreted("org/springframework/util/Assert", "(ZLjava/lang/String;)V", "state")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/String;)V", "isAssignable")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/lang/Class;Ljava/lang/Class;)V", "isAssignable")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/lang/Class;Ljava/lang/Object;Ljava/lang/String;)V", "isInstanceOf")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/lang/Class;Ljava/lang/Object;)V", "isInstanceOf")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/util/Map;)V", "notEmpty")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/util/Map;Ljava/lang/String;)V", "notEmpty")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/util/Collection;)V", "notEmpty")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/util/Collection;Ljava/lang/String;)V", "notEmpty")
                        .withUninterpreted("org/springframework/util/Assert", "([Ljava/lang/Object;)V", "noNullElements")
                        .withUninterpreted("org/springframework/util/Assert", "([Ljava/lang/Object;Ljava/lang/String;)V", "noNullElements")
                        .withUninterpreted("org/springframework/util/Assert", "([Ljava/lang/Object;)V", "notEmpty")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/lang/String;Ljava/lang/String;)V", "doesNotContain")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "doesNotContain")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/lang/String;)V", "hasText")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/lang/String;Ljava/lang/String;)V", "hasText")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/lang/String;)V", "hasLength")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/lang/String;Ljava/lang/String;)V", "hasLength")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/lang/Object;)V", "notNull")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/lang/Object;Ljava/lang/String;)V", "notNull")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/lang/Object;)V", "isNull")
                        .withUninterpreted("org/springframework/util/Assert", "(Ljava/lang/Object;Ljava/lang/String;)V", "isNull")
                        .withUninterpreted("org/springframework/util/Assert", "(ZLjava/lang/String;)V", "isTrue")
                        .withUninterpreted("org/springframework/util/Assert", "(Z)V", "isTrue");
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

    private static String[] prepareFinalRuntimeClasspath(String SUTPath, String[] additionalClassPath) throws AnalyzerException {
        URL[] urlClassPath = methodEnumerator.getClassPath();
        ArrayList<String> listClassPath = new ArrayList<>();

        listClassPath.add(SUTPath);
        for (URL u : urlClassPath) {
            listClassPath.add(u.getPath());
        }
        listClassPath.addAll(Arrays.asList(additionalClassPath));

        String[] classPath = new String[listClassPath.size()];
        listClassPath.toArray(classPath);

        // Sanity check
        for (String path : listClassPath) {
            if(!Files.exists(Paths.get(path))) {
                throw new AnalyzerException("The path " + path + " does not exist");
            }
        }

        return classPath;
    }
}
